//
// Created by Shiki on 2020/7/13.
//

#ifndef LIVEPUSHER_PUSHQUEUE_H
#define LIVEPUSHER_PUSHQUEUE_H

#include <pthread.h>
#include "AndroidLog.h"
#include <queue>

extern "C" {
#include "librtmp/rtmp.h"
};

using namespace std;

class PushQueue {
public:
    queue<RTMPPacket *> queuePacket;
    pthread_mutex_t mutexPacket;
    pthread_cond_t condPacket;


    PushQueue();

    virtual ~PushQueue();

    int putRtmpPacket(RTMPPacket *packet);

    RTMPPacket *getRtmpPacket();

    void clearQueue();

    void notifyQueue();
};


#endif //LIVEPUSHER_PUSHQUEUE_H
