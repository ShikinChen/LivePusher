//
// Created by Shiki on 2020/7/16.
//

#ifndef LIVEPUSHER_AUDIOBUFFER_H
#define LIVEPUSHER_AUDIOBUFFER_H


class AudioBuffer {
public:
    char **buffer;
    int index = -1;

    AudioBuffer(int bufferSize);

    virtual ~AudioBuffer();

    char *getAudioBuffer();

    char *getNowBuffer();
};


#endif //LIVEPUSHER_AUDIOBUFFER_H
