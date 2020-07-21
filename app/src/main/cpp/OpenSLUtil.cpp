//
// Created by Shiki on 2020/7/16.
//

#include "OpenSLUtil.h"
#include "AndroidLog.h"

void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {

    OpenSLUtil *openSLUtil = static_cast<OpenSLUtil *>(context);
    if (openSLUtil->pushAudioData != NULL) {
        openSLUtil->pushAudioData(openSLUtil->audioBuffer->getNowBuffer(), openSLUtil->BUFFER_SIZE);
    }
    if (openSLUtil->finish) {
        (*openSLUtil->recordItf)->SetRecordState(openSLUtil->recordItf, SL_RECORDSTATE_STOPPED);
        //
        (*openSLUtil->recordObj)->Destroy(openSLUtil->recordObj);
        openSLUtil->recordObj = NULL;
        openSLUtil->recordItf = NULL;
        (*openSLUtil->slObjectEngine)->Destroy(openSLUtil->slObjectEngine);
        openSLUtil->slObjectEngine = NULL;
        openSLUtil->engineItf = NULL;
        delete (openSLUtil->audioBuffer);
    } else {
        (*openSLUtil->recorderBufferQueue)->Enqueue(openSLUtil->recorderBufferQueue,
                                                    openSLUtil->audioBuffer->getAudioBuffer(), openSLUtil->BUFFER_SIZE);
    }
}

void OpenSLUtil::start() {
    slCreateEngine(&slObjectEngine, 0, NULL, 0, NULL, NULL);
    (*slObjectEngine)->Realize(slObjectEngine, SL_BOOLEAN_FALSE);
    (*slObjectEngine)->GetInterface(slObjectEngine, SL_IID_ENGINE, &engineItf);


    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE,
                                      SL_IODEVICE_AUDIOINPUT,
                                      SL_DEFAULTDEVICEID_AUDIOINPUT,
                                      NULL};
    SLDataSource audioSrc = {&loc_dev, NULL};


    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            2
    };


    SLDataFormat_PCM format_pcm = {
            SL_DATAFORMAT_PCM, 2, SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, SL_BYTEORDER_LITTLEENDIAN
    };

    SLDataSink audioSnk = {&loc_bq, &format_pcm};

    const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};

    (*engineItf)->CreateAudioRecorder(engineItf, &recordObj, &audioSrc, &audioSnk, 1, id, req);
    (*recordObj)->Realize(recordObj, SL_BOOLEAN_FALSE);
    (*recordObj)->GetInterface(recordObj, SL_IID_RECORD, &recordItf);

    (*recordObj)->GetInterface(recordObj, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &recorderBufferQueue);


    (*recorderBufferQueue)->Enqueue(recorderBufferQueue, audioBuffer->getAudioBuffer(), BUFFER_SIZE);

    (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue, bqRecorderCallback, this);

    (*recordItf)->SetRecordState(recordItf, SL_RECORDSTATE_RECORDING);
}

OpenSLUtil::OpenSLUtil() {
    audioBuffer = new AudioBuffer(4096);
}

OpenSLUtil::~OpenSLUtil() {
    if (audioBuffer != NULL) {
        delete audioBuffer;
        audioBuffer = NULL;
    }
}
