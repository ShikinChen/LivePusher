//
// Created by Shiki on 2020/7/16.
//

#include "AudioBuffer.h"

AudioBuffer::AudioBuffer(int bufferSize) {
    buffer = new char *[2];
    for (int i = 0; i < 2; i++) {
        buffer[i] = new char[bufferSize];
    }
}

AudioBuffer::~AudioBuffer() {
    for (int i = 0; i < 2; i++) {
        delete buffer[i];
    }
    delete buffer;
}

char *AudioBuffer::getAudioBuffer() {
    index++;
    if (index > 1) {
        index = 1;
    }
    return buffer[index];
}

char *AudioBuffer::getNowBuffer() {
    return buffer[index];
}
