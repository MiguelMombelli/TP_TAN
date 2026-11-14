cmake_minimum_required(VERSION 3.22.1)
project("whisper_jni")

# Portado del TAN viejo (Flutter). Compila a mano los fuentes de ggml y whisper.cpp v1.9.0:
# si se actualiza el submodule y aparece un archivo nuevo o cambia de lugar, hay que ajustar esta lista.

set(WHISPER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)
set(GGML_DIR    ${WHISPER_DIR}/ggml)

# --- Núcleo de ggml (ggml-base + registro de backends) ---
set(GGML_SRCS
    ${GGML_DIR}/src/ggml.c
    ${GGML_DIR}/src/ggml.cpp
    ${GGML_DIR}/src/ggml-alloc.c
    ${GGML_DIR}/src/ggml-backend.cpp
    ${GGML_DIR}/src/ggml-backend-meta.cpp
    ${GGML_DIR}/src/ggml-backend-dl.cpp
    ${GGML_DIR}/src/ggml-backend-reg.cpp
    ${GGML_DIR}/src/ggml-opt.cpp
    ${GGML_DIR}/src/ggml-threading.cpp
    ${GGML_DIR}/src/ggml-quants.c
    ${GGML_DIR}/src/gguf.cpp
)

# --- Backend de CPU (amx.cpp está protegido por #ifdef __AMX_INT8__) ---
set(GGML_CPU_SRCS
    ${GGML_DIR}/src/ggml-cpu/ggml-cpu.c
    ${GGML_DIR}/src/ggml-cpu/ggml-cpu.cpp
    ${GGML_DIR}/src/ggml-cpu/ops.cpp
    ${GGML_DIR}/src/ggml-cpu/binary-ops.cpp
    ${GGML_DIR}/src/ggml-cpu/unary-ops.cpp
    ${GGML_DIR}/src/ggml-cpu/vec.cpp
    ${GGML_DIR}/src/ggml-cpu/quants.c
    ${GGML_DIR}/src/ggml-cpu/repack.cpp
    ${GGML_DIR}/src/ggml-cpu/traits.cpp
    ${GGML_DIR}/src/ggml-cpu/hbm.cpp
    ${GGML_DIR}/src/ggml-cpu/amx/amx.cpp
    ${GGML_DIR}/src/ggml-cpu/amx/mmq.cpp
)

# arch/arm solo para arm64-v8a (celulares reales); arch/x86 solo para x86_64 (emulador).
if(ANDROID_ABI STREQUAL "arm64-v8a")
    list(APPEND GGML_CPU_SRCS
        ${GGML_DIR}/src/ggml-cpu/arch/arm/repack.cpp
        ${GGML_DIR}/src/ggml-cpu/arch/arm/quants.c
    )
elseif(ANDROID_ABI STREQUAL "x86_64")
    list(APPEND GGML_CPU_SRCS
        ${GGML_DIR}/src/ggml-cpu/arch/x86/repack.cpp
        ${GGML_DIR}/src/ggml-cpu/arch/x86/quants.c
    )
endif()

add_compile_definitions(
    GGML_VERSION="unknown"
    GGML_COMMIT="unknown"
    WHISPER_VERSION="1.9.0"   # actualizar si cambia el submodule
    WHISPER_COMMIT="unknown"
)

add_library(
    whisper_jni
    SHARED
    whisper_jni.cpp      # puente JNI (nuestro)
    wav_reader.cpp       # lector de WAV (nuestro, sin JNI)
    ${WHISPER_DIR}/src/whisper.cpp
    ${GGML_SRCS}
    ${GGML_CPU_SRCS}
)

target_include_directories(whisper_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${WHISPER_DIR}/include
    ${GGML_DIR}/include
    ${GGML_DIR}/src
    ${GGML_DIR}/src/ggml-cpu
)

target_compile_features(whisper_jni PRIVATE c_std_11 cxx_std_17)

target_compile_definitions(whisper_jni PRIVATE
    _GNU_SOURCE
    _XOPEN_SOURCE=600
    GGML_USE_CPU
    NDEBUG
)

# Optimización SIEMPRE, también en las compilaciones debug de Android Studio.
# Sin esto el motor corre ~12 veces más lento (en el TAN viejo: 25 s en debug contra 2 s en release).
target_compile_options(whisper_jni PRIVATE
    -O3
    -fvisibility=hidden          # los símbolos JNI usan JNIEXPORT, que los vuelve visibles igual
    -ffunction-sections
    -fdata-sections
)
target_link_options(whisper_jni PRIVATE -Wl,--gc-sections)

# EXPERIMENTO (apagado por defecto): instrucciones modernas de ARM para acelerar el producto de matrices.
# Probable mejora en el Snapdragon 8 Gen 1 del Motorola, pero un celular arm64 viejo se cae con SIGILL.
# Para probarlo: agregar "-DTAN_ARM_DOTPROD=ON" en los argumentos de cmake del build.gradle.kts y mirar
# que nativeSystemInfo() muestre DOTPROD = 1.
option(TAN_ARM_DOTPROD "Compilar ggml con armv8.2-a+dotprod+fp16" OFF)
if(TAN_ARM_DOTPROD AND ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(whisper_jni PRIVATE -march=armv8.2-a+dotprod+fp16)
endif()

find_library(log-lib log)

target_link_libraries(
    whisper_jni
    ${log-lib}
    dl
    m
)
