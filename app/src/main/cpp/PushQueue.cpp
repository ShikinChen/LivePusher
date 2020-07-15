//
// Created by Shiki on 2020/7/13.
//

#include "PushQueue.h"

PushQueue::PushQueue() {
    pthread_mutex_init(&mutexPacket, NULL);
    pthread_cond_init(&condPacket, NULL);
}

PushQueue::~PushQueue() {
    pthread_mutex_destroy(&mutexPacket);
    pthread_cond_destroy(&condPacket);
}

int PushQueue::putRtmpPacket(RTMPPacket *packet) {
    pthread_mutex_lock(&mutexPacket);
    queuePacket.push(packet);
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);
    return 0;
}

RTMPPacket *PushQueue::getRtmpPacket() {
    pthread_mutex_lock(&mutexPacket);
    RTMPPacket *packet = NULL;
    if (!queuePacket.empty()) {
        packet = queuePacket.front();
        queuePacket.pop();
    } else {
        pthread_cond_wait(&condPacket, &mutexPacket);
    }
    pthread_mutex_unlock(&mutexPacket);
    return packet;
}

void PushQueue::clearQueue() {
    pthread_mutex_lock(&mutexPacket);
    while (!queuePacket.empty()) {
        RTMPPacket *packet = queuePacket.front();
        queuePacket.pop();
        RTMPPacket_Free(packet);
        packet = NULL;
    }
    pthread_mutex_unlock(&mutexPacket);
}

void PushQueue::notifyQueue() {
    pthread_mutex_lock(&mutexPacket);
    pthread_cond_signal(&condPacket);
    pthread_mutex_unlock(&mutexPacket);
}
