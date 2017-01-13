#include <jni.h>
#ifndef ENABLE_ANDROID_NDK
#include <JNIHelp.h>
#endif
#include "DLNA_DMP_1p5_jni.h"

#ifdef ENABLE_ANDROID_NDK
#include "android/log.h"
#define NULL 0
#define ALOGE(tag, text ) __android_log_write(ANDROID_LOG_ERROR,tag, text)
#define LOG_FATAL_IF(cond, ...) ((void)0)
#define ALOG_ASSERT(cond, ...) LOG_FATAL_IF(!(cond), ## __VA_ARGS__)
#else
#include <utils/Log.h>
#endif


#define TAG "JNI_OnLoad"
extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;
    
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE(TAG, "GetEnv failed!");
        return result;
    }
    
    ALOG_ASSERT(env, "Could not retrieve the env!");
    
    register_DLNA_DMP_1p5(env);
    return JNI_VERSION_1_4;
}
