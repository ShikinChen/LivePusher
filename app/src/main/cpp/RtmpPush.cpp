//
// Created by Shiki on 2020/7/13.
//

#include "RtmpPush.h"
#include <malloc.h>
#include <string>
#include "AndroidLog.h"

RtmpPush::RtmpPush(const char *url, CallJava *callJava) {
    this->url = static_cast<char *>(malloc(strlen(url)));
    strcpy(this->url, url);
    queue = new PushQueue();
    this->callJava = callJava;
}

RtmpPush::~RtmpPush() {
    free(url);
    url = NULL;
    queue->notifyQueue();
    queue->clearQueue();
    queue = NULL;
    callJava = NULL;
}

void *callBackPush(void *data) {
    RtmpPush *rtmpPush = static_cast<RtmpPush *>(data);
    rtmpPush->startPushing = false;
    rtmpPush->rtmp = RTMP_Alloc();
    RTMP_Init(rtmpPush->rtmp);
    rtmpPush->rtmp->Link.timeout = 10;
    rtmpPush->rtmp->Link.lFlags |= RTMP_LF_LIVE;
    RTMP_SetupURL(rtmpPush->rtmp, rtmpPush->url);
    RTMP_EnableWrite(rtmpPush->rtmp);

    if (!RTMP_Connect(rtmpPush->rtmp, NULL)) {
        LOGE("RTMP connect fail")
        rtmpPush->callJava->onConnectFail("RTMP connect fail");
        goto end;
    }


    if (!RTMP_ConnectStream(rtmpPush->rtmp, 0)) {
        LOGE("RTMP connect stream fail")
        rtmpPush->callJava->onConnectFail("RTMP connect stream fail");
        goto end;
    }
    rtmpPush->callJava->onConnectSuccess();
    rtmpPush->startPushing = true;
    rtmpPush->startTime = RTMP_GetTime();
    LOGE("RTMP 成功")
    while (rtmpPush->startPushing) {
        RTMPPacket *packet = rtmpPush->queue->getRtmpPacket();
        if (packet != NULL) {
            int result = RTMP_SendPacket(rtmpPush->rtmp, packet, 1);
            RTMPPacket_Free(packet);
            free(packet);
            packet = NULL;
        }
    }

    end:
    RTMP_Close(rtmpPush->rtmp);
    RTMP_Free(rtmpPush->rtmp);
    rtmpPush->rtmp = NULL;
    return 0;
}

void RtmpPush::init() {
    this->callJava->onConnecting(Main);
    pthread_create(&pushThread, NULL, callBackPush, this);
}

void RtmpPush::pushSpsAndPps(char *sps, int spsLen, char *pps, int ppsLen) {
    int bodySize = spsLen + ppsLen + 16;

    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodySize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;
    int i = 0;

    //frame type : 1关键帧、2非关键帧 (4 bit) CodecID : 7表示AVC (4 bit) 0x17 和frametype组合成一个字节
    body[i++] = 0x17;

    //fixed : 0x00（AVCDecoderConfigurationRecord） 0x00 0x00 0x00 (4 byte)
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    //configurationVersion (1 byte)		0x01 版本
    body[i++] = 0x01;

    //AVCProfileIndication (1 byte)		sps[1] Profile
    body[i++] = sps[1];
    //profile_compatibility (1 byte)		sps[2] 兼容性
    body[i++] = sps[2];
    //AVCLevelIndication (1 byte)		sps[3] Profile level
    body[i++] = sps[3];

    //lengthSizeMinusOne (1 byte)		0xff 包长数据所使用的字节数
    body[i++] = 0xff;
    //sps number (1 byte)			0xe1 sps个数
    body[i++] = 0xe1;
    //sps data length (2 byte)			sps长度
    body[i++] = (spsLen >> 8) & 0xff;
    body[i++] = spsLen & 0xff;

    //sps data					sps实际内容
    memcpy(&body[i], sps, spsLen);
    i += spsLen;

    //pps number (1 byte)			0x01 pps的个数
    body[i++] = 0x01;
    //pps data length (2 byte)			pps长度
    body[i++] = (ppsLen >> 8) & 0xff;
    body[i++] = ppsLen & 0xff;
    //pps data					pps内容
    memcpy(&body[i], pps, ppsLen);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;

    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    queue->putRtmpPacket(packet);
}

void RtmpPush::pushVideoData(char *data, int dataLen, bool isKeyFrame) {
    int bodySize = dataLen + 9;

    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodySize);
    RTMPPacket_Reset(packet);


    char *body = packet->m_body;
    int i = 0;

    //frame type : 1关键帧、2非关键帧 (4 bit) CodecID : 7表示AVC (4 bit) 0x17 和frametype组合成一个字节
    if (isKeyFrame) {
        body[i++] = 0x17;
    } else {
        body[i++] = 0x27;
    }


    //fixed : 0x01（AVCDecoderConfigurationRecord） 0x00 0x00 0x00 (4 byte)
    body[i++] = 0x01;
    body[i++] = 0x00;
    body[i++] = 0x00;
    body[i++] = 0x00;

    body[i++] = (dataLen >> 24) & 0xff;
    body[i++] = (dataLen >> 16) & 0xff;
    body[i++] = (dataLen >> 8) & 0xff;
    body[i++] = dataLen & 0xff;

    memcpy(&body[i], data, dataLen);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = bodySize;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_hasAbsTimestamp = 0;

    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    queue->putRtmpPacket(packet);
}

void RtmpPush::pushAudioData(char *data, int dataLen) {
    int bodySize = dataLen + 2;

    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, bodySize);
    RTMPPacket_Reset(packet);

    char *body = packet->m_body;

    body[0] = 0xAF;
    body[1] = 0x01;

    memcpy(&body[2], data, dataLen);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = bodySize;
    packet->m_nTimeStamp = RTMP_GetTime() - startTime;
    packet->m_hasAbsTimestamp = 0;

    packet->m_nChannel = 0x04;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    packet->m_nInfoField2 = rtmp->m_stream_id;

    queue->putRtmpPacket(packet);
}

void RtmpPush::pushStop() {
    startPushing = false;
    queue->notifyQueue();
    pthread_join(pushThread, NULL);
}
