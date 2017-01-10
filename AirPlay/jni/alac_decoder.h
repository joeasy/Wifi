#ifndef __ALAC__DECOMP_JNI_H
#define __ALAC__DECOMP_JNI_H

#include <jni.h>
#include "alac.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_realtek_cast_util_Alac_createAlac(JNIEnv*, jclass, jint, jint);

JNIEXPORT void JNICALL Java_com_realtek_cast_util_Alac_setupAlac(JNIEnv*, jclass, jint, jint, jint, jint, jint, jint, jint, jint, jint, jint, jint);

JNIEXPORT jint JNICALL Java_com_realtek_cast_util_Alac_decodeFrame(JNIEnv*, jclass, jbyteArray, jint, jintArray);

JNIEXPORT jint JNICALL Java_com_realtek_cast_util_Alac_decodeByteFrame(JNIEnv*, jclass, jbyteArray, jint, jbyteArray);

JNIEXPORT jint JNICALL Java_com_realtek_cast_util_Alac_readBit(JNIEnv *env, jclass clazz);

JNIEXPORT jint JNICALL Java_com_realtek_cast_util_Alac_readBits(JNIEnv *env, jclass clazz, jint bits);

JNIEXPORT void JNICALL Java_com_realtek_cast_util_Alac_unreadBits(JNIEnv *env, jclass clazz, jint bits);

#ifdef __cplusplus
}
#endif
#endif /* __ALAC__DECOMP_JNI_H */

