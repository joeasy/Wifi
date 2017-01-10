/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "DMRServiceJNI"

#include <jni.h>
#ifndef ENABLE_ANDROID_NDK
#include <JNIHelp.h>
#endif
#include "DLNALog.h"

#include <DLNA_DMR.h>
#include "DMRAndroidRenderer.h"

static osal_mutex_t gMutex;	
static RTK_DLNA_DMR* gRTK_DLNA_DMR;
bool DMR_PlayCompleted;	
jobject gDMRServiceObj;
JavaVM *gVM;


static void RTK_DLNA_DMR_callinit(JNIEnv* env, jobject obj, jstring DMRname)
{
	if (gDMRServiceObj == NULL)
		gDMRServiceObj = env->NewGlobalRef(obj);

	if (gDMRServiceObj == NULL)
		ALOGE("gDMRServiceObj is NULL\n");	
	char* jniname =  (char*)env->GetStringUTFChars(DMRname,NULL);
	ALOGD(LOG_TAG, "jniname  %s",jniname);

	osal_MutexCreate(&gMutex);
	gRTK_DLNA_DMR->m_DMRname = strdup(jniname);
	AndroidRenderer::GetUUID();
	gRTK_DLNA_DMR->m_UUID = strdup(AndroidRenderer::uuid);
	gRTK_DLNA_DMR = new RTK_DLNA_DMR(&gMutex);
	gRTK_DLNA_DMR->RegisterDlnaDmrCallbackFunc(&AndroidRenderer::Restart, NULL, DLNA_DMR_CB_RESTART);
	gRTK_DLNA_DMR->RegisterDlnaDmrCallbackFunc(&AndroidRenderer::QueryForConnection, NULL, DLNA_DMR_CB_QUERYFORCONNECT);
	gRTK_DLNA_DMR->RegisterDlnaDmrCallbackFunc(&AndroidRenderer::SetBrightness, NULL, DLNA_DMR_CB_SETBRIGHTNESS);
	gRTK_DLNA_DMR->RegisterDlnaDmrCallbackFunc(&AndroidRenderer::SetContrast, NULL, DLNA_DMR_CB_SETCONTRAST);
	gRTK_DLNA_DMR->RegisterDlnaDmrCallbackFunc(&AndroidRenderer::ShowVolumeStatus, NULL, DLNA_DMR_CB_SHOWVOLUMESTATUS);
	gRTK_DLNA_DMR->RegisterDlnaDmrCallbackFunc(&AndroidRenderer::SetMute, NULL, DLNA_DMR_CB_SETMUTE);
	env->ReleaseStringUTFChars(DMRname, jniname);
	env->DeleteLocalRef(DMRname);
	ALOGD("\n\n\n\n\n\n\n\n RTK_DLNA_DMR_init \n\n\n\n\n\n\n\n");		
}	

static void RTK_DLNA_DMR_calldeinit(JNIEnv* env, jobject obj)
{
	
    if (gDMRServiceObj == NULL)
        gDMRServiceObj = env->NewGlobalRef(obj);
	if (gDMRServiceObj == NULL)
		ALOGE("gDMRServiceObj is NULL\n");
	ALOGD("\n\n\n\n\n\n\n\n DMR is over !!! deinit...\n\n\n\n\n\n\n\n");
	if(1)
	{
		if(gRTK_DLNA_DMR)
		{
			delete gRTK_DLNA_DMR;
			gRTK_DLNA_DMR = NULL;
		}
	}	
}
static void setStateStop(JNIEnv* env, jobject obj)
{
	ALOGE("Send stop commend JNI !");
	DMR_PlayCompleted = true;
	//sleep(10);
	gRTK_DLNA_DMR->m_PlayState = DMR_PS_Stopped;
	DMR_StateChange_TransportPlayState(gRTK_DLNA_DMR->m_pDMR_MicroStack, DMR_PS_Stopped);
}
static void SetStatePlay(JNIEnv* env, jobject obj,jboolean isplay)
{
	ALOGE("Set Playing state  JNI !");
	#ifdef DLNADMRCTT
	//gRTK_DLNA_DMR->m_PlayState = DMR_PS_Playing;
	//DMR_StateChange_TransportPlayState(gRTK_DLNA_DMR->m_pDMR_MicroStack, gRTK_DLNA_DMR->m_PlayState);
	#endif
}

static void setSeekPosition(JNIEnv* env, jobject obj,jint position)
{
	AndroidRenderer::setSeekPosition(position);
}
static jstring getfileTitle(JNIEnv* env, jobject obj)
{
	ALOGE("title = %s",gRTK_DLNA_DMR->GetRenderMediaFilename());
	return env->NewStringUTF(gRTK_DLNA_DMR->GetRenderMediaFilename());
}
static jint getfileDate(JNIEnv* env, jobject obj)
{
	ALOGE("Date = %ld",gRTK_DLNA_DMR->GetRenderMediaFileDate());
	return (jint)gRTK_DLNA_DMR->GetRenderMediaFileDate();
}
static jbyteArray getMediaInfo(JNIEnv* env, jobject obj)
{
	NAVLPCMINFO* mediaInfo;
	if(gRTK_DLNA_DMR->GetMediaTpye() == MEDIASUBTYPE_PCM)
		{
			mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
			if(!gRTK_DLNA_DMR->GetMediaMimeTypeProtocol())
			{
				ALOGE("[DMR subRenderer][Audio/Video] Play PCM 6()\n");
				mediaInfo->bitsPerSample = gRTK_DLNA_DMR->GetPCMBitPerSample();
				mediaInfo->samplingRate = gRTK_DLNA_DMR->GetPCMSampleRate();
				mediaInfo->numChannels = gRTK_DLNA_DMR->GetPCMChannel();
			}
			else if ( strcmp(gRTK_DLNA_DMR->GetMediaMimeTypeProtocol(), MIME_TYPE_AUDIO_LPCM) == 0)
			{
				ALOGE("[DMR subRenderer][Audio/Video] Play PCM 1()\n");
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 44100;
				mediaInfo->numChannels = 2;
			}
			else
			if ( strcmp(gRTK_DLNA_DMR->GetMediaMimeTypeProtocol(), MIME_TYPE_AUDIO_L16_SLPCM) == 0)
			{
				ALOGE("[DMR subRenderer][Audio/Video] Play PCM 2()\n");
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 44100;
				mediaInfo->numChannels = 1;
			}
			else
			if ( strcmp(gRTK_DLNA_DMR->GetMediaMimeTypeProtocol(), MIME_TYPE_AUDIO_L16_48_SLPCM) == 0)
			{
				ALOGE("[DMR subRenderer][Audio/Video] Play PCM 3()\n");
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 48000;
				mediaInfo->numChannels = 1;
			}
			else
			if ( strcmp(gRTK_DLNA_DMR->GetMediaMimeTypeProtocol(), MIME_TYPE_AUDIO_L16_48_LPCM) == 0)
			{
				ALOGE("[DMR subRenderer][Audio/Video] Play PCM 4()\n");
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 48000;
				mediaInfo->numChannels = 2;
			}
			else
			{
				ALOGE("[DMR subRenderer][Audio/Video] Play PCM 5()\n");
				mediaInfo->bitsPerSample = gRTK_DLNA_DMR->GetPCMBitPerSample();
				mediaInfo->samplingRate = gRTK_DLNA_DMR->GetPCMSampleRate();
				mediaInfo->numChannels = gRTK_DLNA_DMR->GetPCMChannel();
			}
		}

	int size = sizeof(NAVLPCMINFO);
	jbyteArray	jbarray = env->NewByteArray((jsize)size);
	if (mediaInfo)
	{
		env->SetByteArrayRegion(jbarray, 0, (jsize)size, (jbyte *)mediaInfo);
	}
    return jbarray;
}

static jint getFileType(JNIEnv* env, jobject obj)
{
	ENUM_MEDIA_TYPE type;
	type = gRTK_DLNA_DMR->GetMediaTpye();
       return (jint)type;
}

static void setfilePlayspeed(JNIEnv* env, jobject obj,jstring playspeed)
{
	char* speed = (char*)env->GetStringUTFChars(playspeed,NULL);
	if(speed)
		DMR_StateChange_TransportPlaySpeed(gRTK_DLNA_DMR->m_pDMR_MicroStack, speed);
	env->ReleaseStringUTFChars(playspeed,speed);
}

static JNINativeMethod sMethods[] = {
     /* name, signature, funcPtr */   
    { "RTK_DLNA_DMR_init", "(Ljava/lang/String;)V", (void*)RTK_DLNA_DMR_callinit },   
    { "RTK_DLNA_DMR_deinit", "()V", (void*)RTK_DLNA_DMR_calldeinit },
    { "setSeekPosition", "(I)V", (void*)setSeekPosition},
    { "setStateStop", "()V", (void*)setStateStop},
    { "getMediaInfo", "()[B", (void*)getMediaInfo },
    { "getFileType", "()I", (void*)getFileType },
    { "getfileTitle", "()Ljava/lang/String;", (void*)getfileTitle },
    { "getfileDate", "()I", (void*)getfileDate },
    { "setfilePlayspeed", "(Ljava/lang/String;)V", (void*)setfilePlayspeed },
}; 



int register_android_server_DMRservice(JNIEnv* env)	
{	
	 jclass clazz = env->FindClass("com/android/server/RtkDMRService");
	if (clazz == NULL)
    {
        ALOGE("Can't find com/android/server/RtkDMRService");
        return -1;
    }
	env->GetJavaVM(&gVM);

#ifdef ENABLE_ANDROID_NDK
	return env->RegisterNatives(clazz, sMethods, sizeof(sMethods) / sizeof(sMethods[0]));
#else
	return jniRegisterNativeMethods(env, "com/android/server/RtkDMRService",
                                    sMethods, NELEM(sMethods));
#endif
}
extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
       ALOGE("GetEnv failed!");
        return result;
    }

    register_android_server_DMRservice(env);
    return JNI_VERSION_1_4;
}
