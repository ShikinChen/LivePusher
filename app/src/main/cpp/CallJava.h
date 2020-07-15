//
// Created by Shiki on 2020/4/30.
//

#ifndef LIVEPUSHER_CALLJAVA_H
#define LIVEPUSHER_CALLJAVA_H
enum ThreadType {
    Main,
    Child
};

#include <jni.h>
#include "AndroidLog.h"

class CallJava {
public:
    JavaVM *javaVm = NULL;
    JNIEnv *jniEnv = NULL;
    jobject jobj;


    CallJava(JavaVM *javaVm, JNIEnv *jniEnv, jobject *obj);

    virtual  ~CallJava();

    void onConnecting(ThreadType threadType = Child);

    void onConnectSuccess(ThreadType threadType = Child);

    void onConnectFail(const char *msg, ThreadType threadType = Child);


private:
    jmethodID jmid_onConnecting;
    jmethodID jmid_onConnectSuccess;
    jmethodID jmid_onConnectFail;

};


#endif //LIVEPUSHER_CALLJAVA_H
