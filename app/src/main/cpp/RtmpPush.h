//
// Created by Shiki on 2020/7/13.
//

#ifndef LIVEPUSHER_RTMPPUSH_H
#define LIVEPUSHER_RTMPPUSH_H

#include "PushQueue.h"
#include <pthread.h>
#include "CallJava.h"

extern "C" {
#include "librtmp/rtmp.h"
};

class RtmpPush {
public:

    RTMP *rtmp = NULL;
    char *url = NULL;

    PushQueue *queue = NULL;

    pthread_t initThread;

    CallJava *callJava;

    bool startPushing = false;

    long startTime = 0;

    RtmpPush(const char *url, CallJava *callJava);

    virtual ~RtmpPush();

    void init();

    void pushSpsAndPps(char *sps, int spsLen, char *pps, int ppsLen);

    void pushVideoData(char *data, int dataLen, bool isKeyFrame);
};


#endif //LIVEPUSHER_RTMPPUSH_H
