/*
 * ALAC (Apple Lossless Audio Codec) decoder
 * Copyright (c) 2005 David Hammerton
 * All rights reserved.
 *
 * This is the actual decoder.
 *
 * http://crazney.net/programs/itunes/alac.html
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 */


#include <jni.h>
#include <string.h>
#include "alac_decoder.h"

static alac_file *s_alac;

JNIEXPORT void JNICALL Java_com_realtek_cast_util_Alac_createAlac(JNIEnv *env, jclass clazz, jint sampleSize, jint numChannels) {
	if (s_alac != NULL) {
		alac_free(s_alac);
	}
	s_alac = alac_create(sampleSize, numChannels);
}

JNIEXPORT void JNICALL Java_com_realtek_cast_util_Alac_setupAlac(JNIEnv* env, jclass clazz,
		jint frameSize, jint i7a, jint sampleSize, jint riceHistoryMult, jint riceInitHistory, jint riceKModifier, jint i7f, jint i80, jint i82, jint i86, jint i8a) {
	s_alac->setinfo_max_samples_per_frame = (uint32_t)frameSize;
	s_alac->setinfo_7a = (uint8_t)i7a;
	s_alac->setinfo_sample_size = (uint8_t)sampleSize;
	s_alac->setinfo_rice_historymult = (uint8_t)riceHistoryMult;
	s_alac->setinfo_rice_initialhistory = (uint8_t)riceInitHistory;
	s_alac->setinfo_rice_kmodifier = (uint8_t)riceKModifier;
	s_alac->setinfo_7f = (uint8_t)i7f;
	s_alac->setinfo_80 = (uint16_t)i80;
	s_alac->setinfo_82 = (uint32_t)i82;
	s_alac->setinfo_86 = (uint32_t)i86;
	s_alac->setinfo_8a_rate = (uint32_t)i8a;

	alac_allocate_buffers(s_alac);
}

JNIEXPORT jint JNICALL Java_com_realtek_cast_util_Alac_decodeFrame(JNIEnv *env, jclass clazz, jbyteArray in, jint size, jintArray out) {
	static jbyte bufIn[1024*16];
	static jint bufOut[1024*16];
	static jint tmp[1024*16];

	env->GetByteArrayRegion(in, 0, size, bufIn);

	int outputsize = 0;
	alac_decode_frame(s_alac, (unsigned char*)bufIn, bufOut, &outputsize);

//	int value;
	int16_t *tmpout = (int16_t*) bufOut;
	for (int i = 0; i < outputsize/2; i++){
		tmp[i] = (int16_t) tmpout[i];
	}
	env->SetIntArrayRegion(out, 0, outputsize/2, tmp);

	return outputsize;
}

JNIEXPORT jint JNICALL Java_com_realtek_cast_util_Alac_decodeByteFrame(JNIEnv *env, jclass clazz, jbyteArray in, jint size, jbyteArray out) {
	static jbyte bufIn[1024*16];
	static jbyte bufOut[1024*16];

	env->GetByteArrayRegion(in, 0, size, bufIn);

	int outputsize = 0;
	alac_decode_frame(s_alac, (unsigned char*)bufIn, bufOut, &outputsize);
	if (outputsize >= 0) {
		env->SetByteArrayRegion(out, 0, outputsize, bufOut);
	}

	return outputsize;
}
