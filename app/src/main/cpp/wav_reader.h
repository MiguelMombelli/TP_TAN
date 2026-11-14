// Lector de WAV para el motor de transcripción.
//
// No usa JNI a propósito: así se puede probar en cualquier computadora con un
// compilador de C++, sin Android.
#pragma once

#include <string>
#include <vector>

namespace tan_engine {

enum class WavStatus {
    Ok,
    CannotOpen,         // el archivo no existe o no se puede leer
    NotWav,             // no empieza con RIFF/WAVE
    NoFmtChunk,         // no se encontró el bloque "fmt " antes de los datos
    NoDataChunk,        // no se encontró el bloque "data"
    UnsupportedFormat,  // no es PCM 16-bit, mono, 16 kHz
    Empty,              // el audio no tiene muestras
};

struct WavResult {
    WavStatus status = WavStatus::Ok;
    std::string message;  // descripción legible (en español); vacía si todo salió bien
};

// Lee un WAV PCM de 16 bits, mono, a 16 kHz (el formato que pide Whisper) y
// devuelve las muestras normalizadas a float en [-1, 1).
//
// - Salta los bloques que no interesan (LIST, INFO, etc.).
// - Si el tamaño declarado del bloque "data" es 0, 0xFFFFFFFF o mayor que el
//   archivo (encabezado sin cerrar, por ejemplo si la app murió grabando),
//   usa lo que realmente hay en el archivo.
// - Lee por bloques directo a float: no duplica el audio en memoria.
WavResult read_wav_16k_mono(const char* path, std::vector<float>& out);

}  // namespace tan_engine
