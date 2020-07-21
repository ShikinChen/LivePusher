#include <jni.h>
#include <string>
#include "AndroidLog.h"
#include "RtmpPush.h"
#include "CallJava.h"
#include "OpenSLUtil.h"

RtmpPush *rtmpPush = NULL;
JavaVM *javaVm = NULL;
CallJava *callJava = NULL;
bool isExit = true;
OpenSLUtil *openSLUtil = NULL;

void pushAudioData(char *data, int dataLen) {
    if (rtmpPush != NULL && !isExit && data != NULL) {
        rtmpPush->pushAudioData(reinterpret_cast<char *>(data), dataLen);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_me_shiki_livepusher_push_PushVideo_initPush(JNIEnv *env, jobject thiz, jstring push_url) {
    const char *pushUrl = env->GetStringUTFChars(push_url, 0);
    if (callJava != NULL) {
        delete (callJava);
        callJava = NULL;
    }

    callJava = new CallJava(javaVm, env, &thiz);
    if (rtmpPush == NULL) {
        isExit = false;
        rtmpPush = new RtmpPush(pushUrl, callJava);
//        openSLUtil = new OpenSLUtil();
//        openSLUtil->pushAudioData = pushAudioData;
//        openSLUtil->start();
        rtmpPush->init();
    }
    env->ReleaseStringUTFChars(push_url, pushUrl);
}

extern "C"
JNIEXPORT JNICALL jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVm = vm;
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_shiki_livepusher_push_PushVideo_pushSpsAndPps(JNIEnv *env, jobject thiz, jbyteArray sps_, jint sps_len,
                                                      jbyteArray pps_, jint pps_len) {
    jbyte *sps = env->GetByteArrayElements(sps_, NULL);
    jbyte *pps = env->GetByteArrayElements(pps_, NULL);

    if (rtmpPush != NULL && !isExit) {
        rtmpPush->pushSpsAndPps(reinterpret_cast<char *>(sps), sps_len, reinterpret_cast<char *>(pps), pps_len);
    }
    env->ReleaseByteArrayElements(sps_, sps, 0);
    env->ReleaseByteArrayElements(pps_, pps, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_shiki_livepusher_push_PushVideo_pushVideoData(JNIEnv *env, jobject thiz, jbyteArray data_, jint data_len,
                                                      jboolean isKeyFrame) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    if (rtmpPush != NULL && !isExit) {
        rtmpPush->pushVideoData(reinterpret_cast<char *>(data), data_len, isKeyFrame);
    }
    env->ReleaseByteArrayElements(data_, data, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_me_shiki_livepusher_push_PushVideo_pushAudioData(JNIEnv *env, jobject thiz, jbyteArray data_, jint data_len) {

    jbyte *data = env->GetByteArrayElements(data_, NULL);
    if (rtmpPush != NULL && !isExit) {
        rtmpPush->pushAudioData(reinterpret_cast<char *>(data), data_len);
    }

    env->ReleaseByteArrayElements(data_, data, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_me_shiki_livepusher_push_PushVideo_pushStop(JNIEnv *env, jobject thiz) {
    isExit = true;
    if (rtmpPush != NULL) {
        rtmpPush->pushStop();
//        openSLUtil->finish = true;
        delete (rtmpPush);
//        delete (callJava);
//        delete (openSLUtil);
        rtmpPush = NULL;
//        callJava = NULL;
        openSLUtil = NULL;
    }
}