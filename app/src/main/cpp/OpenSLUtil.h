//
// Created by Shiki on 2020/7/16.
//

#ifndef LIVEPUSHER_OPENSLUTIL_H
#define LIVEPUSHER_OPENSLUTIL_H

#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include "AudioBuffer.h"

class OpenSLUtil {
public:

    SLObjectItf slObjectEngine = NULL;
    SLEngineItf engineItf = NULL;

    SLObjectItf recordObj = NULL;
    SLRecordItf recordItf = NULL;

    SLAndroidSimpleBufferQueueItf recorderBufferQueue = NULL;

    bool finish = false;

    AudioBuffer *audioBuffer = NULL;

    const SLuint32 BUFFER_SIZE = 4096;


    typedef void (*PushAudioData)(char *data, int dataLen);

    PushAudioData pushAudioData = NULL;

    OpenSLUtil();

    virtual ~OpenSLUtil();

    void start();


};


#endif //LIVEPUSHER_OPENSLUTIL_H
