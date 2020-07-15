//
// Created by Shiki on 2020/4/30.
//

#include "CallJava.h"


CallJava::CallJava(JavaVM *javaVm, JNIEnv *jniEnv, jobject *obj) : javaVm(javaVm), jniEnv(jniEnv) {
    this->jobj = jniEnv->NewGlobalRef(*obj);

    jclass jclz = jniEnv->GetObjectClass(jobj);
    if (!jclz) {
        if (LOG_DEBUG) {
            LOGE("获取class失败");
        }
        return;
    }
    jmid_onConnecting = jniEnv->GetMethodID(jclz, "onConnecting", "()V");
    jmid_onConnectSuccess = jniEnv->GetMethodID(jclz, "onConnectSuccess", "()V");
    jmid_onConnectFail = jniEnv->GetMethodID(jclz, "onConnectFail", "(Ljava/lang/String;)V");
}

CallJava::~CallJava() {
    jniEnv->DeleteGlobalRef(jobj);
    javaVm = NULL;
    jniEnv = NULL;
    jobj = NULL;
}

void CallJava::onConnecting(ThreadType threadType) {
    if (threadType == Main) {
        jniEnv->CallVoidMethod(jobj, jmid_onConnecting);
    } else if (threadType == Child) {
        JNIEnv *jniEnv;
        javaVm->AttachCurrentThread(&jniEnv, NULL);
        if (!jniEnv) {
            if (LOG_DEBUG) {
                LOGE("获取子线程env失败");
            }
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_onConnecting);
        javaVm->DetachCurrentThread();
    }
}

void CallJava::onConnectSuccess(ThreadType threadType) {
    if (threadType == Main) {
        jniEnv->CallVoidMethod(jobj, jmid_onConnectSuccess);
    } else if (threadType == Child) {
        JNIEnv *jniEnv;
        javaVm->AttachCurrentThread(&jniEnv, NULL);
        if (!jniEnv) {
            if (LOG_DEBUG) {
                LOGE("获取子线程env失败");
            }
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_onConnectSuccess);
        javaVm->DetachCurrentThread();
    }
}

void CallJava::onConnectFail(const char *msg, ThreadType threadType) {
    if (threadType == Main) {
        jstring jmsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jobj, jmid_onConnectFail, jmsg);
        jniEnv->DeleteLocalRef(jmsg);
    } else if (threadType == Child) {
        JNIEnv *jniEnv;
        javaVm->AttachCurrentThread(&jniEnv, NULL);
        if (!jniEnv) {
            if (LOG_DEBUG) {
                LOGE("获取子线程env失败");
            }
            return;
        }
        jstring jmsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jobj, jmid_onConnectFail, jmsg);
        jniEnv->DeleteLocalRef(jmsg);
        javaVm->DetachCurrentThread();
    }
}

