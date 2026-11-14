#include "wav_reader.h"

#include <algorithm>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <memory>

namespace tan_engine {
namespace {

constexpr uint32_t kSampleRate = 16000;
constexpr uint16_t kChannels = 1;
constexpr uint16_t kBitsPerSample = 16;
constexpr uint16_t kFormatPcm = 1;
constexpr size_t kBlockSamples = 1 << 16;  // 65536 muestras = 128 KB por lectura

struct FileCloser {
    void operator()(FILE* f) const {
        if (f != nullptr) std::fclose(f);
    }
};
using FilePtr = std::unique_ptr<FILE, FileCloser>;

// Los WAV son little-endian; se arma cada número byte a byte para no depender
// del procesador donde corre.
uint16_t le16(const uint8_t* p) {
    return static_cast<uint16_t>(p[0] | (p[1] << 8));
}

uint32_t le32(const uint8_t* p) {
    return static_cast<uint32_t>(p[0]) | (static_cast<uint32_t>(p[1]) << 8) |
           (static_cast<uint32_t>(p[2]) << 16) | (static_cast<uint32_t>(p[3]) << 24);
}

bool read_exact(FILE* f, void* dst, size_t n) {
    return std::fread(dst, 1, n, f) == n;
}

WavResult fail(WavStatus status, std::string message) {
    WavResult r;
    r.status = status;
    r.message = std::move(message);
    return r;
}

}  // namespace

WavResult read_wav_16k_mono(const char* path, std::vector<float>& out) {
    out.clear();

    FilePtr file(std::fopen(path, "rb"));
    if (!file) {
        return fail(WavStatus::CannotOpen, "No se pudo abrir el archivo de audio.");
    }
    FILE* f = file.get();

    if (fseeko(f, 0, SEEK_END) != 0) {
        return fail(WavStatus::CannotOpen, "No se pudo leer el archivo de audio.");
    }
    const long long file_size = ftello(f);
    if (file_size < 0 || fseeko(f, 0, SEEK_SET) != 0) {
        return fail(WavStatus::CannotOpen, "No se pudo leer el archivo de audio.");
    }

    // Encabezado RIFF: "RIFF" <tamaño> "WAVE"
    uint8_t riff[12];
    if (!read_exact(f, riff, sizeof(riff)) || std::memcmp(riff, "RIFF", 4) != 0 ||
        std::memcmp(riff + 8, "WAVE", 4) != 0) {
        return fail(WavStatus::NotWav, "El archivo no es un WAV válido.");
    }

    // Recorre los bloques: cada uno es <id de 4 bytes> <tamaño de 4 bytes> <datos>.
    bool have_fmt = false;
    uint16_t format_tag = 0, channels = 0, bits = 0;
    uint32_t sample_rate = 0;
    long long data_offset = -1;
    uint32_t data_declared = 0;

    while (true) {
        uint8_t chunk[8];
        if (!read_exact(f, chunk, sizeof(chunk))) break;  // fin del archivo
        const uint32_t chunk_size = le32(chunk + 4);
        const long long body = ftello(f);

        if (std::memcmp(chunk, "fmt ", 4) == 0) {
            uint8_t fmt[16];
            if (chunk_size < sizeof(fmt) || !read_exact(f, fmt, sizeof(fmt))) {
                return fail(WavStatus::NoFmtChunk, "El bloque de formato del WAV está dañado.");
            }
            format_tag = le16(fmt);
            channels = le16(fmt + 2);
            sample_rate = le32(fmt + 4);
            bits = le16(fmt + 14);
            have_fmt = true;
        } else if (std::memcmp(chunk, "data", 4) == 0) {
            data_offset = body;
            data_declared = chunk_size;
            break;
        }

        // Los bloques se alinean a 2 bytes: si el tamaño es impar hay un byte de relleno.
        const long long next = body + static_cast<long long>(chunk_size) + (chunk_size & 1u);
        if (next >= file_size || fseeko(f, next, SEEK_SET) != 0) break;
    }

    if (data_offset < 0) {
        return fail(WavStatus::NoDataChunk, "El WAV no contiene datos de audio.");
    }
    if (!have_fmt) {
        return fail(WavStatus::NoFmtChunk, "El WAV no declara su formato.");
    }

    if (format_tag != kFormatPcm || channels != kChannels || sample_rate != kSampleRate ||
        bits != kBitsPerSample) {
        char msg[200];
        std::snprintf(msg, sizeof(msg),
                      "Formato WAV no soportado (frecuencia %u Hz, canales %u, %u bits, código %u). "
                      "Se necesita PCM de 16000 Hz, mono, 16 bits.",
                      static_cast<unsigned>(sample_rate), static_cast<unsigned>(channels),
                      static_cast<unsigned>(bits), static_cast<unsigned>(format_tag));
        return fail(WavStatus::UnsupportedFormat, msg);
    }

    // Cuántos bytes de audio hay de verdad. El tamaño declarado no es de fiar si el
    // encabezado quedó sin cerrar.
    const unsigned long long available = static_cast<unsigned long long>(file_size - data_offset);
    unsigned long long n_bytes = data_declared;
    if (data_declared == 0 || data_declared == 0xFFFFFFFFu || n_bytes > available) {
        n_bytes = available;
    }
    const size_t n_samples = static_cast<size_t>(n_bytes / 2);  // un byte suelto al final se descarta
    if (n_samples == 0) {
        return fail(WavStatus::Empty, "El audio no tiene muestras.");
    }

    out.resize(n_samples);
    if (fseeko(f, data_offset, SEEK_SET) != 0) {
        out.clear();
        return fail(WavStatus::CannotOpen, "No se pudo leer el archivo de audio.");
    }

    // Lee por bloques y convierte directo a float: así no hace falta una copia int16 del audio entero.
    std::vector<uint8_t> buffer(kBlockSamples * 2);
    size_t done = 0;
    while (done < n_samples) {
        const size_t want = std::min(kBlockSamples, n_samples - done);
        const size_t got = std::fread(buffer.data(), 2, want, f);
        if (got == 0) break;
        for (size_t i = 0; i < got; ++i) {
            const int16_t s = static_cast<int16_t>(le16(buffer.data() + 2 * i));
            out[done + i] = static_cast<float>(s) / 32768.0f;
        }
        done += got;
    }

    if (done == 0) {
        out.clear();
        return fail(WavStatus::Empty, "El audio no tiene muestras.");
    }
    out.resize(done);  // el archivo pudo quedar más corto de lo previsto
    return WavResult{};
}

}  // namespace tan_engine
