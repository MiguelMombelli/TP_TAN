// Puente JNI entre Kotlin (com.tan.data.engine.WhisperNative) y whisper.cpp.
//
// Reglas de este archivo:
//  - Nunca devuelve errores como texto: lanza excepciones de com.tan.domain.engine.
//  - Nunca deja escapar una excepción de C++ hacia Java (sería comportamiento indefinido).
//  - Un solo contexto de Whisper protegido por mutex: o se transcribe, o se carga/libera el modelo.

#include <jni.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <exception>
#include <mutex>
#include <new>
#include <string>
#include <vector>

#include "whisper.h"
#include "wav_reader.h"

#ifdef __ANDROID__
#include <android/log.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "TanWhisper", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "TanWhisper", __VA_ARGS__)
#else  // para chequear la sintaxis en una computadora común
#include <cstdio>
#define LOGI(...) (std::fprintf(stderr, __VA_ARGS__), std::fprintf(stderr, "\n"))
#define LOGE(...) (std::fprintf(stderr, __VA_ARGS__), std::fprintf(stderr, "\n"))
#endif

namespace {

// Clases Kotlin que este archivo usa por nombre. Si se renombra o mueve alguna, hay que cambiarlo acá.
constexpr const char* kInvalidAudio = "com/tan/domain/engine/InvalidAudioException";
constexpr const char* kModelLoad = "com/tan/domain/engine/ModelLoadException";
constexpr const char* kFailed = "com/tan/domain/engine/TranscriptionFailedException";
constexpr const char* kCancelled = "com/tan/domain/engine/TranscriptionCancelledException";
constexpr const char* kNativeSegment = "com/tan/data/engine/NativeSegment";
constexpr const char* kProgressListener = "com/tan/data/engine/ProgressListener";

// --- Estado global -----------------------------------------------------------

std::mutex g_mutex;  // protege g_ctx y g_model_path
whisper_context* g_ctx = nullptr;
std::string g_model_path;
std::atomic<bool> g_cancel{false};  // se escribe desde otro hilo sin tomar el mutex

// Libera el contexto actual. Hay que tener g_mutex tomado.
void free_context_locked() {
    if (g_ctx != nullptr) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
    }
    g_model_path.clear();
}

// --- Utilidades JNI ----------------------------------------------------------

void throw_java(JNIEnv* env, const char* class_name, const std::string& message) {
    if (env->ExceptionCheck()) return;  // ya hay una excepción pendiente: no la pisamos
    jclass cls = env->FindClass(class_name);
    if (cls == nullptr) return;  // FindClass ya dejó un NoClassDefFoundError pendiente
    env->ThrowNew(cls, message.c_str());
    env->DeleteLocalRef(cls);
}

std::string to_std_string(JNIEnv* env, jstring s) {
    if (s == nullptr) return {};
    const char* chars = env->GetStringUTFChars(s, nullptr);
    if (chars == nullptr) return {};
    std::string out(chars);
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

// Whisper puede cortar un carácter UTF-8 a la mitad entre segmentos, y NewStringUTF aborta (con
// CheckJNI) si recibe UTF-8 inválido o emojis de 4 bytes. Por eso decodificamos nosotros a UTF-16 y
// reemplazamos lo inválido por U+FFFD.
std::u16string utf8_to_utf16(const std::string& s) {
    std::u16string out;
    out.reserve(s.size());
    const size_t n = s.size();
    auto byte = [&](size_t k) { return static_cast<uint32_t>(static_cast<unsigned char>(s[k])); };
    auto is_cont = [&](size_t k) { return k < n && (byte(k) & 0xC0u) == 0x80u; };

    size_t i = 0;
    while (i < n) {
        const uint32_t c = byte(i);
        uint32_t cp = 0xFFFD;
        size_t len = 1;
        if (c < 0x80) {
            cp = c;
        } else if ((c & 0xE0u) == 0xC0u && is_cont(i + 1)) {
            const uint32_t v = ((c & 0x1Fu) << 6) | (byte(i + 1) & 0x3Fu);
            if (v >= 0x80) cp = v;
            len = 2;
        } else if ((c & 0xF0u) == 0xE0u && is_cont(i + 1) && is_cont(i + 2)) {
            const uint32_t v = ((c & 0x0Fu) << 12) | ((byte(i + 1) & 0x3Fu) << 6) | (byte(i + 2) & 0x3Fu);
            if (v >= 0x800 && !(v >= 0xD800 && v <= 0xDFFF)) cp = v;
            len = 3;
        } else if ((c & 0xF8u) == 0xF0u && is_cont(i + 1) && is_cont(i + 2) && is_cont(i + 3)) {
            const uint32_t v = ((c & 0x07u) << 18) | ((byte(i + 1) & 0x3Fu) << 12) |
                               ((byte(i + 2) & 0x3Fu) << 6) | (byte(i + 3) & 0x3Fu);
            if (v >= 0x10000 && v <= 0x10FFFF) cp = v;
            len = 4;
        }
        i += len;
        if (cp >= 0x10000) {  // par sustituto
            cp -= 0x10000;
            out.push_back(static_cast<char16_t>(0xD800 + (cp >> 10)));
            out.push_back(static_cast<char16_t>(0xDC00 + (cp & 0x3FF)));
        } else {
            out.push_back(static_cast<char16_t>(cp));
        }
    }
    return out;
}

jstring to_jstring(JNIEnv* env, const std::string& utf8) {
    const std::u16string u16 = utf8_to_utf16(utf8);
    return env->NewString(reinterpret_cast<const jchar*>(u16.data()), static_cast<jsize>(u16.size()));
}

std::string trim(const std::string& s) {
    const char* ws = " \t\r\n";
    const size_t a = s.find_first_not_of(ws);
    if (a == std::string::npos) return {};
    const size_t b = s.find_last_not_of(ws);
    return s.substr(a, b - a + 1);
}

// --- Callbacks de Whisper ----------------------------------------------------

// Whisper llama a estos callbacks en el mismo hilo que llamó a whisper_full, así que el JNIEnv de la
// llamada nativa sigue siendo válido.
struct ProgressCtx {
    JNIEnv* env;
    jobject listener;  // puede ser null: sin progreso
    jmethodID method;
    int last;
};

void on_progress(whisper_context*, whisper_state*, int progress, void* user_data) {
    auto* p = static_cast<ProgressCtx*>(user_data);
    if (p->listener == nullptr || progress == p->last) return;
    p->last = progress;
    p->env->CallVoidMethod(p->listener, p->method, static_cast<jint>(progress));
    if (p->env->ExceptionCheck()) g_cancel.store(true);  // si el listener falló, abortamos
}

bool on_abort(void*) {
    return g_cancel.load();
}

struct RawSegment {
    int64_t start_ms;
    int64_t end_ms;
    std::string text;
};

}  // namespace

// =============================================================================
// Funciones que ve Kotlin. El nombre sigue la regla JNI:
// Java_<paquete con _>_<clase>_<método>, y todas son miembros de `object WhisperNative`.
// =============================================================================

extern "C" JNIEXPORT jstring JNICALL
Java_com_tan_data_engine_WhisperNative_nativeSystemInfo(JNIEnv* env, jobject) {
    // Dice con qué instrucciones del procesador (NEON, FP16, DOTPROD...) se compiló ggml.
    const std::string info = std::string("whisper ") + whisper_version() + " | " + whisper_print_system_info();
    return to_jstring(env, info);
}

extern "C" JNIEXPORT void JNICALL
Java_com_tan_data_engine_WhisperNative_nativeLoadModel(JNIEnv* env, jobject, jstring jpath) {
    const std::string path = to_std_string(env, jpath);
    try {
        std::lock_guard<std::mutex> lock(g_mutex);
        if (g_ctx != nullptr && g_model_path == path) return;  // ya está cargado
        free_context_locked();

        if (FILE* probe = std::fopen(path.c_str(), "rb")) {
            std::fclose(probe);
        } else {
            throw_java(env, kModelLoad, "No se encontró el archivo del modelo.");
            return;
        }

        whisper_context_params cparams = whisper_context_default_params();
        cparams.use_gpu = false;
        const auto t0 = std::chrono::steady_clock::now();
        g_ctx = whisper_init_from_file_with_params(path.c_str(), cparams);
        if (g_ctx == nullptr) {
            throw_java(env, kModelLoad, "No se pudo cargar el modelo: el archivo está dañado o incompleto.");
            return;
        }
        g_model_path = path;
        const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - t0).count();
        LOGI("Modelo cargado en %lld ms", static_cast<long long>(ms));
    } catch (const std::bad_alloc&) {
        throw_java(env, kModelLoad, "No hay memoria suficiente para cargar el modelo.");
    } catch (const std::exception& e) {
        throw_java(env, kModelLoad, std::string("Error al cargar el modelo: ") + e.what());
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_tan_data_engine_WhisperNative_nativeFreeModel(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    free_context_locked();
}

// Pide cortar la transcripción en curso. No toma el mutex (lo tiene la transcripción).
extern "C" JNIEXPORT void JNICALL
Java_com_tan_data_engine_WhisperNative_nativeCancel(JNIEnv*, jobject) {
    g_cancel.store(true);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_tan_data_engine_WhisperNative_nativeTranscribe(JNIEnv* env, jobject, jstring jwav_path,
                                                        jint threads, jstring jlanguage,
                                                        jobject listener) {
    const std::string wav_path = to_std_string(env, jwav_path);
    const std::string language = to_std_string(env, jlanguage);

    try {
        // 1) Leer el audio. No toca el contexto, así que va antes de tomar el mutex.
        std::vector<float> pcm;
        const tan_engine::WavResult wav = tan_engine::read_wav_16k_mono(wav_path.c_str(), pcm);
        if (wav.status != tan_engine::WavStatus::Ok) {
            throw_java(env, kInvalidAudio, wav.message);
            return nullptr;
        }

        std::lock_guard<std::mutex> lock(g_mutex);
        if (g_ctx == nullptr) {
            throw_java(env, kModelLoad, "No hay un modelo cargado.");
            return nullptr;
        }
        g_cancel.store(false);

        // 2) Parámetros. "es" por defecto; "auto" hace que Whisper detecte el idioma.
        whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
        wparams.language = language.empty() ? "es" : language.c_str();
        wparams.translate = false;
        wparams.n_threads = threads > 0 ? static_cast<int>(threads) : 4;
        wparams.print_progress = false;
        wparams.print_realtime = false;
        wparams.print_timestamps = false;
        wparams.print_special = false;
        wparams.abort_callback = on_abort;
        wparams.abort_callback_user_data = nullptr;

        ProgressCtx progress{env, listener, nullptr, -1};
        if (listener != nullptr) {
            jclass listener_cls = env->FindClass(kProgressListener);
            if (listener_cls != nullptr) {
                progress.method = env->GetMethodID(listener_cls, "onProgress", "(I)V");
                env->DeleteLocalRef(listener_cls);
            }
            if (progress.method == nullptr) {
                env->ExceptionClear();
                progress.listener = nullptr;  // sin progreso, pero se puede transcribir igual
            }
        }
        if (progress.listener != nullptr) {
            wparams.progress_callback = on_progress;
            wparams.progress_callback_user_data = &progress;
        }

        // 3) Transcribir (bloquea hasta terminar o hasta que se pida cancelar).
        const double audio_s = static_cast<double>(pcm.size()) / 16000.0;
        const auto t0 = std::chrono::steady_clock::now();
        const int rc = whisper_full(g_ctx, wparams, pcm.data(), static_cast<int>(pcm.size()));
        const double took_s =
            std::chrono::duration<double>(std::chrono::steady_clock::now() - t0).count();
        LOGI("whisper_full rc=%d: %.1f s de audio en %.1f s (x%.2f tiempo real), %d hilos", rc, audio_s,
             took_s, took_s > 0 ? audio_s / took_s : 0.0, wparams.n_threads);

        if (env->ExceptionCheck()) return nullptr;  // el listener de progreso lanzó una excepción
        if (g_cancel.load()) {
            throw_java(env, kCancelled, "Transcripción cancelada.");
            return nullptr;
        }
        if (rc != 0) {
            throw_java(env, kFailed, "Whisper falló al transcribir (código " + std::to_string(rc) + ").");
            return nullptr;
        }

        // 4) Pasar los segmentos a una lista propia. Whisper mide el tiempo en unidades de 10 ms.
        std::vector<RawSegment> segments;
        const int n = whisper_full_n_segments(g_ctx);
        segments.reserve(static_cast<size_t>(n));
        for (int i = 0; i < n; ++i) {
            const char* raw = whisper_full_get_segment_text(g_ctx, i);
            std::string text = trim(raw != nullptr ? raw : "");
            if (text.empty()) continue;
            const int64_t start = whisper_full_get_segment_t0(g_ctx, i) * 10;
            int64_t end = whisper_full_get_segment_t1(g_ctx, i) * 10;
            if (end < start) end = start;
            segments.push_back({start, end, std::move(text)});
        }

        // 5) Armar el Array<NativeSegment> para Kotlin.
        jclass seg_cls = env->FindClass(kNativeSegment);
        if (seg_cls == nullptr) return nullptr;
        jmethodID ctor = env->GetMethodID(seg_cls, "<init>", "(JJLjava/lang/String;)V");
        jobjectArray result =
            ctor != nullptr ? env->NewObjectArray(static_cast<jsize>(segments.size()), seg_cls, nullptr) : nullptr;
        if (result == nullptr) {
            env->DeleteLocalRef(seg_cls);
            return nullptr;  // hay una excepción pendiente (NoSuchMethodError u OutOfMemoryError)
        }
        for (size_t i = 0; i < segments.size(); ++i) {
            jstring text = to_jstring(env, segments[i].text);
            jobject seg = text != nullptr
                              ? env->NewObject(seg_cls, ctor, static_cast<jlong>(segments[i].start_ms),
                                               static_cast<jlong>(segments[i].end_ms), text)
                              : nullptr;
            if (text != nullptr) env->DeleteLocalRef(text);
            if (seg == nullptr) {
                env->DeleteLocalRef(seg_cls);
                return nullptr;  // excepción pendiente
            }
            env->SetObjectArrayElement(result, static_cast<jsize>(i), seg);
            env->DeleteLocalRef(seg);  // evita desbordar la tabla de referencias locales
        }
        env->DeleteLocalRef(seg_cls);
        return result;
    } catch (const std::bad_alloc&) {
        throw_java(env, kFailed, "No hay memoria suficiente para transcribir este audio.");
    } catch (const std::exception& e) {
        throw_java(env, kFailed, std::string("Error inesperado al transcribir: ") + e.what());
    } catch (...) {
        throw_java(env, kFailed, "Error desconocido al transcribir.");
    }
    return nullptr;
}
