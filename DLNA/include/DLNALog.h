#ifndef _DLNA_LOG_H__
#define _DLNA_LOG_H__

#ifdef ENABLE_ANDROID_NDK
#include <android/log.h>
//#define  LOG_TAG    "JniHelper"
#define  ALOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#else
#include <utils/Log.h>
#endif

#endif // _DLNA_LOG_H__
