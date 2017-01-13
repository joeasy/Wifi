#ifdef ENABLE_ANDROID_NDK
#include "android/log.h"
#else
#include <utils/Log.h>
#endif
#ifndef ENABLE_ANDROID_NDK
#include <JNIHelp.h>
#endif
#include "DLNA_DMP.h"
#include "DLNA_DMP_1p5_jni.h"
#include "MediaType.h"
#include "MimeTypes.h"
#include "FileIoAbstraction.h"

#ifdef ENABLE_ANDROID_NDK
#define ALOGD(tag, text)  __android_log_write(ANDROID_LOG_DEBUG,tag, text)
#define ALOGE(tag, text)  __android_log_write(ANDROID_LOG_ERROR,tag, text)
#define LOG_FATAL_IF(cond, ...) ((void)0)
#endif

#define TAG "DLNA_DMP_1p5_jni"
#define UPNP_PROC_STR_LEN        ENTRIES_LENGTH
////////////////////////////////////////////////////////////////////////////////
//  global value
////////////////////////////////////////////////////////////////////////////////
static RTK_DLNA_DMP* g_pDMP = NULL;
static jobject g_DLNA_DMP_1p5_Obj = NULL;
static JavaVM * g_VM;
static jmethodID g_InvokeBrowserUpdateFunc;

static struct {
    jclass clazz;
    jmethodID init;
    jfieldID UniqueCharID;
    jfieldID UniqueCharParentID;
    jfieldID TitleName;
    jfieldID ProtocolInfo;
    jfieldID Uri;
} g_UPnPObjInfo;
////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////
//  Callback func
////////////////////////////////////////////////////////////////////////////////
bool MediaServersUpdateCallback(int i, char* addOrRemoved, char *serverName)
{   
//////To Do: implement callback
	extern JavaVM* get_VM;
	extern jclass DLNAClass;
	JNIEnv * env;
	bool attached = false;
	switch (get_VM->GetEnv((void**)&env, JNI_VERSION_1_4))
	{
	 case JNI_OK:
	 	break;
	 case JNI_EDETACHED:
	 	if (get_VM->AttachCurrentThread(&env, NULL)!=0)
		{
		 	printf("AttachCurrentThread failed");
			return false;
	   	}
	   	attached = true;
	   	break;
	 case JNI_EVERSION:
	 	printf("Invalid java version");
	 }
	jmethodID constructor = env -> GetMethodID(DLNAClass, "<init>", "()V");
	jobject myObj = env -> NewObject(DLNAClass, constructor);
	g_InvokeBrowserUpdateFunc = env -> GetMethodID(DLNAClass, "DLNADeviceAddedOrRemoved", "(Ljava/lang/String;[B)V");
	if (g_InvokeBrowserUpdateFunc != NULL)
	{
		jbyteArray	jbarray = env->NewByteArray(jsize(strlen(serverName)));
		env->SetByteArrayRegion(jbarray, 0, (jsize)strlen(serverName), (jbyte*)serverName);
		env->CallVoidMethod(myObj, g_InvokeBrowserUpdateFunc, env->NewStringUTF(addOrRemoved), jbarray);
	}
	if(attached)
    	get_VM->DetachCurrentThread();
    return true;
} 
////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////
//  GetMediaType()
////////////////////////////////////////////////////////////////////////////////
bool MimeTypeMatch(const char *protocolInfo, const char *mimeType)
{
    //protocolInfo http-get:*:mimType:*
    //                       ^       ^
    //                      ptr2    ptr1
    char *ptr1 = NULL, *ptr2 = NULL, *MType = NULL;
    bool retval = false;
	if(protocolInfo)
	{
		ptr1 = strstr(protocolInfo, "CONTENTFORMAT=");
		ptr2 = strchr(protocolInfo, ':');
		if(ptr1 && ptr2)
		{
			int len = ptr2-ptr1;
			MType = strndup(ptr1, len-1);
			MType[len-1] = '\0';
			if(mimeType && (strstr(MType, mimeType) != NULL))
				retval = true;
			if (MType) 
			{
				free(MType);
			}
			return retval;
		}
	}
    if(protocolInfo)
    {
        ptr1 = strchr(protocolInfo, ':');
        if(ptr1)
        {
            ptr2 = strchr(ptr1+1, ':');
            ptr1 = strrchr(protocolInfo, ':');
        }
    }
    if( ptr1 && ptr2)
    {
        int len = ptr1-ptr2;
        MType = strndup(ptr2+1, len-1);
        MType[len-1] = '\0';
//         ALOGD("[DLNA DMP]the get out of mime type is %s\n", MType);

        if(mimeType && strcmp(mimeType, MType)==0)
            retval = true;
    }

    if (MType) {free(MType);}
    return retval;
}

NAVLPCMINFO* GetMediaType(char *filename, SUGGESTED_MEDIA_TYPE &type)
{
    char *name = NULL;
    char *protocolInfo = NULL;
    NAVLPCMINFO *mediaInfo = NULL;
    unsigned int i = 0;
//@fixed me
//TODO: make sure the input filename....
    if(filename && strchr(filename, '/')!=NULL && filename[strlen(filename)] != '/')
        name = strrchr(filename, '/')+1;
    else if(filename)
        name = filename;

    unsigned int MediaType = g_pDMP->queryFileMediaType(name, &protocolInfo);

    const PROTOCOLINFO_ENTRY image_protocol[] = {
        {MIME_TYPE_IMAGE_JPEG, MEDIASUBTYPE_IMAGE_JPEG },	//JPEG_SM
        {MIME_TYPE_IMAGE_PNG, MEDIASUBTYPE_IMAGE_PNG}, 		//PNG_LRG, *.png
        {MIME_TYPE_IMAGE_TIF, MEDIASUBTYPE_IMAGE_TIFF},		// *.tif
        {MIME_TYPE_IMAGE_GIF, MEDIASUBTYPE_IMAGE_GIF},		// *.gif
        {MIME_TYPE_IMAGE_BMP, MEDIASUBTYPE_IMAGE_BITMAP},	// *.bmp
    };

    const PROTOCOLINFO_ENTRY audio_protocol[] = {
        {MIME_TYPE_AUDIO_LPCM, MEDIASUBTYPE_PCM},	//LPCM should be make up
        {MIME_TYPE_AUDIO_L16_SLPCM, MEDIASUBTYPE_PCM},	//LPCM should be make up
        {MIME_TYPE_AUDIO_L16_48_SLPCM, MEDIASUBTYPE_PCM},
        {MIME_TYPE_AUDIO_L16_48_LPCM, MEDIASUBTYPE_PCM},
        {MIME_TYPE_AUDIO_MPEG, MEDIASUBTYPE_MP3},	//*.mp3
        {MIME_TYPE_AUDIO_AAC, MEDIASUBTYPE_MP4_VIDEO},		//AAC_ISO_320, *.mp4
        {MIME_TYPE_AUDIO_ADTS, MEDIASUBTYPE_AAC},	//AAC_ADTS_320, *.aac
        {MIME_TYPE_AUDIO_WMA, MEDIASUBTYPE_WMA},	//WMABASE, *.wma
        {MIME_TYPE_AUDIO_WAV, MEDIASUBTYPE_WAVE},			//*.wav
    };

    const PROTOCOLINFO_ENTRY video_protocol[] = {
        {MIME_TYPE_VIDEO_MPEG2, MEDIASUBTYPE_MPEG_PROGRAM},	//MPEG_PS_NTSC, *.mpg
        {MIME_TYPE_VIDEO_AAC, MEDIASUBTYPE_MP4_VIDEO},		//AVC_MP4_BL_CIF15_AAC_520, *.mp4
        {MIME_TYPE_VIDEO_WMV, MEDIASUBTYPE_WMA},			//?? wrong _WMA, WMVMED_BASE, *.wmv
        {MIME_TYPE_VIDEO_ASF, MEDIASUBTYPE_WMA},			//MPEG4_P2_ASF_SP_G726. *.asf

    };

    type.mediaType = 0;
    type.fileType  = MEDIATYPE_None;		//?????	fill this later
    type.ioType    = MEDIATYPE_NET;            //?????	fill this later

    if( MediaType == MMSCP_CLASS_MASK_MAJOR_IMAGEITEM )
    {
        type.mediaType |= MEDIA_IMAGE;
        for( i = 0; i < sizeof(image_protocol)/sizeof(PROTOCOLINFO_ENTRY); i++)
        {
            if( MimeTypeMatch(protocolInfo, image_protocol[i].protocolInfoMimeType))
            {
                type.fileType = image_protocol[i].mediaType;
                break;
            }
        }
    }
    else if( MediaType == MMSCP_CLASS_MASK_MAJOR_AUDIOITEM )
    {
        type.mediaType |= MEDIA_AUDIO;
        for( i = 0; i < sizeof(audio_protocol)/sizeof(PROTOCOLINFO_ENTRY); i++)
        {
            if( MimeTypeMatch(protocolInfo, audio_protocol[i].protocolInfoMimeType))
            {
                type.fileType = audio_protocol[i].mediaType;
                break;
            }
        }
        
        if( type.fileType == MEDIASUBTYPE_PCM)
        {
            //since pcm is a raw data file, this should be get more mediaInfo!
            if( i == 0 ) //MIME_TYPE_AUDIO_LPCM
            {
                mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
                mediaInfo->bitsPerSample = 16;
                mediaInfo->samplingRate = 44100;
                mediaInfo->numChannels = 2;
            }
            else if( i == 1 ) //MIME_TYPE_AUDIO_L16_SLPCM
            {
                mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
                mediaInfo->bitsPerSample = 16;
                mediaInfo->samplingRate = 44100;
                mediaInfo->numChannels = 1;
            }
            else if( i == 2 ) //MIME_TYPE_AUDIO_L16_48_SLPCM
            {
                mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
                mediaInfo->bitsPerSample = 16;
                mediaInfo->samplingRate = 48000;
                mediaInfo->numChannels = 1;
            }
            else if( i == 3 ) //MIME_TYPE_AUDIO_L16_48_LPCM
            {
                mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
                mediaInfo->bitsPerSample = 16;
                mediaInfo->samplingRate = 48000;
                mediaInfo->numChannels = 2;
            }
        }
    }
    else if( MediaType == MMSCP_CLASS_MASK_MAJOR_VIDEOITEM )
    {
        type.mediaType |= MEDIA_VIDEO;
        for( i = 0; i < sizeof(video_protocol)/sizeof(PROTOCOLINFO_ENTRY); i++)
        {
            if( MimeTypeMatch(protocolInfo, video_protocol[i].protocolInfoMimeType))
            {
                type.fileType = video_protocol[i].mediaType;
                break;
            }
        }
    }
    else
    {
        ALOGD(TAG, "Warnning !!! this is NOTHING!!!\n");
    }
    return mediaInfo;
}
////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////
//  Android Native API
////////////////////////////////////////////////////////////////////////////////
static void DLNA_DMP_1p5_Init(JNIEnv* env, jobject obj)
{
    g_pDMP = RTK_DLNA_DMP_Singleton::GetInstance();
    if (g_DLNA_DMP_1p5_Obj == NULL)
        g_DLNA_DMP_1p5_Obj = env->NewGlobalRef(obj);
}

static void DLNA_DMP_1p5_UnInit(JNIEnv* env, jobject obj)
{
    if(g_pDMP)
        RTK_DLNA_DMP_Singleton::DeleteInstance();
}

static void DLNA_DMP_1p5_Start(JNIEnv* env, jobject obj)
{
    g_pDMP->Start();
}

static void DLNA_DMP_1p5_Terminate(JNIEnv* env, jobject obj)
{
    g_pDMP->Terminate();
}

static void DLNA_DMP_1p5_stopWatingResponse(JNIEnv* env, jobject obj)
{
    g_pDMP->stopWatingResponse();
}

static void DLNA_DMP_1p5_RegisterBrowserUpdateFunc(JNIEnv* env, jobject obj)
{
    g_pDMP->setMediaServersTrigger(MediaServersUpdateCallback, 0);
}

static void DLNA_DMP_1p5_UnregisterBrowserUpdateFunc(JNIEnv* env, jobject obj)
{
    g_pDMP->setMediaServersTrigger(NULL, NULL);
}

static jboolean DLNA_DMP_1p5_UPnPServiceDeviceBrowse(JNIEnv* env, jobject obj)
{
    return g_pDMP->UPnPServiceBrowse();
}

static jboolean DLNA_DMP_1p5_UPnPServiceBrowse(JNIEnv* env, jobject obj, jstring id)
{
	if(id == NULL)
		return false;
    char* pId = (char*)env->GetStringUTFChars(id, NULL);
    return g_pDMP->UPnPServiceBrowse(pId);
}

static jboolean DLNA_DMP_1p5_cdupDirectoryStack(JNIEnv* env, jobject obj, jint level)
{
    return g_pDMP->cdupDirectoryStack((int)level);
}

static void DLNA_DMP_1p5_unsetMediaServer(JNIEnv* env, jobject obj)
{
    g_pDMP->unsetMediaServer();
}

static jboolean DLNA_DMP_1p5_setMediaServerByFriendlyName(JNIEnv* env, jobject obj, jbyteArray name)
{
	char* pName = NULL;
	jsize alen = env->GetArrayLength(name);
	jbyte* ba = env->GetByteArrayElements(name, JNI_FALSE);
	if (alen > 0)
	{
		pName = (char*)malloc(alen + 1);
		memcpy(pName, ba, alen);
		pName[alen] = 0;
	}
	env->ReleaseByteArrayElements(name, ba, 0);
	
    return g_pDMP->setMediaServerByFriendlyName(pName);
}

static jboolean DLNA_DMP_1p5_setDirectoryByTitleName(JNIEnv* env, jobject obj, jstring name)
{
	if(name == NULL)
		return false;
    char* pName = (char*)env->GetStringUTFChars(name, NULL);
    return g_pDMP->setDirectoryByTitleName(pName);
}

static jint DLNA_DMP_1p5_MediaContainerObjectSizes(JNIEnv* env, jobject obj)
{
    return g_pDMP->MediaContainerObjectSizes();
}

static jint DLNA_DMP_1p5_MediaItemObjectSizes(JNIEnv* env, jobject obj)
{
    return g_pDMP->MediaItemObjectSizes();
}

static jint DLNA_DMP_1p5_MediaItemVideoSizes(JNIEnv* env, jobject obj)
{
    return g_pDMP->MediaItemVideoSizes();
}

static jint DLNA_DMP_1p5_MediaItemAudioSizes(JNIEnv* env, jobject obj)
{
    return g_pDMP->MediaItemAudioSizes();
}

static jint DLNA_DMP_1p5_MediaItemImageSizes(JNIEnv* env, jobject obj)
{
    return g_pDMP->MediaItemImageSizes();
}

static jstring DLNA_DMP_1p5_queryResourceByFile(JNIEnv* env, jobject obj, jstring filename, jint queryType)
{
	if(filename == NULL)
		return env->NewStringUTF("");
    char* pFilename = (char*)env->GetStringUTFChars(filename, NULL);
    unsigned long queryRes = 0;
    char* retString = g_pDMP->queryResourceByFile(pFilename, (UPNP_DMP_RES_TYPE)queryType, (void*)&queryRes) ? 
                      (char*)queryRes : 
                      (char*)"";

    if((UPNP_DMP_RES_TYPE)queryType == UPNP_DMP_RES_SIZE
		|| (UPNP_DMP_RES_TYPE)queryType == UPNP_DMP_RES_DURATION
		|| (UPNP_DMP_RES_TYPE)queryType == UPNP_DMP_RES_BITRATE)
    {
		char str[10];
		sprintf(str, "%ld", queryRes);
		retString = str;
	}
    return env->NewStringUTF(retString);
}

static jstring DLNA_DMP_1p5_queryResourceByID(JNIEnv* env, jobject obj, jstring id, jint queryType)
{
	if(id == NULL)
		return env->NewStringUTF("");
    	char* pId = (char*)env->GetStringUTFChars(id, NULL);
	if((UPNP_DMP_RES_TYPE)queryType != UPNP_DMP_RES_SIZE)
	{
		unsigned long queryRes = 0;
		char* retString = g_pDMP->queryResourceByID(pId, (UPNP_DMP_RES_TYPE)queryType, (void*)&queryRes) ? 
							(char*)queryRes : 
							(char*)"";

		if((UPNP_DMP_RES_TYPE)queryType == UPNP_DMP_RES_DURATION
			|| (UPNP_DMP_RES_TYPE)queryType == UPNP_DMP_RES_BITRATE)
		{
			char str[10];
			sprintf(str, "%ld", queryRes);
			retString = str;
		}
		return env->NewStringUTF(retString);
	}
	else
	{
		long long queryRes = 0;
		char* retString = g_pDMP->queryResourceByID(pId, (UPNP_DMP_RES_TYPE)queryType, (void*)&queryRes) ? 
							(char*)queryRes : 
							(char*)"";
		char str[25];
		sprintf(str, "%lld", queryRes);
		retString = str;
		return env->NewStringUTF(retString);
	}
}

static jobjectArray DLNA_DMP_1p5_querySubtitleURIListByID(JNIEnv* env, jobject obj, jstring id, jstring mimeType)
{	
	jclass StringObject = env->FindClass("java/lang/String");
	if(id == NULL || mimeType == NULL)
		return env->NewObjectArray(0, StringObject, 0);
    char* pId = (char*)env->GetStringUTFChars(id, NULL);
    char* pMineType = (char*)env->GetStringUTFChars(mimeType, NULL);
    int iListSize = 0;
    char** StringList = g_pDMP->querySubtitleURIListByID(pId, pMineType, &iListSize);
    if (!iListSize) return NULL;
    
    jobjectArray retJobjectArray = env->NewObjectArray((jint)iListSize, StringObject, 0);
    
    for (int i=0 ; i<iListSize ; i++)
    {
        env->SetObjectArrayElement(retJobjectArray, 
                                   i, 
                                   env->NewStringUTF(StringList[i]));
    }
    free(StringList);
    return retJobjectArray;
}

static jint DLNA_DMP_1p5_MediaServerSizes(JNIEnv* env, jobject obj)
{
    return (jint)g_pDMP->MediaServerSizes();
}

static jbyteArray DLNA_DMP_1p5_MediaServerName(JNIEnv* env, jobject obj, jint index)
{
    char name[UPNP_PROC_STR_LEN];
	memset(name, 0, UPNP_PROC_STR_LEN);
    g_pDMP->MediaServerName((int)index, name);
	jbyteArray	jbarray = env->NewByteArray(jsize(strlen(name)));
	env->SetByteArrayRegion(jbarray, 0, (jsize)strlen(name), (jbyte*)name);
    return jbarray;
}

static jstring DLNA_DMP_1p5_UPnPServiceContentDirectory(JNIEnv* env, jobject obj, jint index)
{
    char name[UPNP_PROC_STR_LEN];
    memset(name, 0, UPNP_PROC_STR_LEN);
    g_pDMP->UPnPServiceContentDirectory((int)index, name);
    return env->NewStringUTF(name);
}

static jobject DLNA_DMP_1p5_queryContainerObjInfoByIndex(JNIEnv* env, jobject obj, jint index)
{
    struct UPnPObjInfo* pUPnPObjInfo = (struct UPnPObjInfo *)calloc(sizeof(struct UPnPObjInfo), 1);
    memset(pUPnPObjInfo, 0, sizeof(struct UPnPObjInfo));
    jobject jUPnPObjInfo;
    
    g_pDMP->queryContainerObjInfoByIndex((int)index, pUPnPObjInfo);
    if (!pUPnPObjInfo) return NULL;
    
    jUPnPObjInfo = env->NewObject(g_UPnPObjInfo.clazz, g_UPnPObjInfo.init);
    if (jUPnPObjInfo == NULL) return NULL;
    
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.UniqueCharID, env->NewStringUTF(pUPnPObjInfo->pUniqueCharID));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.UniqueCharParentID, env->NewStringUTF(pUPnPObjInfo->pUniqueCharParentID));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.TitleName, env->NewStringUTF(pUPnPObjInfo->pTitleName));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.ProtocolInfo, env->NewStringUTF(pUPnPObjInfo->pProtocolInfo));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.Uri, env->NewStringUTF(pUPnPObjInfo->pUri));
    
    free(pUPnPObjInfo);
    
    return jUPnPObjInfo;
}

static jstring DLNA_DMP_1p5_UPnPServiceContentFile(JNIEnv* env, jobject obj, jint index)
{
    char name[UPNP_PROC_STR_LEN];
    memset(name, 0, UPNP_PROC_STR_LEN);
    g_pDMP->UPnPServiceContentFile((int)index, name);
    return env->NewStringUTF(name);
}

static jobject DLNA_DMP_1p5_queryItemObjInfoByIndex(JNIEnv* env, jobject obj, int index)
{
    struct UPnPObjInfo* pUPnPObjInfo = (struct UPnPObjInfo *)calloc(sizeof(struct UPnPObjInfo), 1);
    memset(pUPnPObjInfo, 0, sizeof(struct UPnPObjInfo));
    jobject jUPnPObjInfo;
    
    g_pDMP->queryItemObjInfoByIndex((int)index, pUPnPObjInfo);
    if (!pUPnPObjInfo) return NULL;
    
    jUPnPObjInfo = env->NewObject(g_UPnPObjInfo.clazz, g_UPnPObjInfo.init);
    if (jUPnPObjInfo == NULL) return NULL;
    
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.UniqueCharID, env->NewStringUTF(pUPnPObjInfo->pUniqueCharID));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.UniqueCharParentID, env->NewStringUTF(pUPnPObjInfo->pUniqueCharParentID));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.TitleName, env->NewStringUTF(pUPnPObjInfo->pTitleName));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.ProtocolInfo, env->NewStringUTF(pUPnPObjInfo->pProtocolInfo));
    env->SetObjectField(jUPnPObjInfo, g_UPnPObjInfo.Uri, env->NewStringUTF(pUPnPObjInfo->pUri));
    
    free(pUPnPObjInfo);
    
    return jUPnPObjInfo;
}

static jint DLNA_DMP_1p5_GetMediaType(JNIEnv* env, jobject obj, jstring filename)
{
	if(filename == NULL)
		return 0;
    char* pFilename = (char*)env->GetStringUTFChars(filename, NULL);
    SUGGESTED_MEDIA_TYPE type;
    GetMediaType(pFilename, type);
    int iRet = 0;
    if (type.mediaType&MEDIA_IMAGE)
        iRet = DEVICE_FILE_PHOTO;
    else if (type.mediaType&MEDIA_AUDIO)
        iRet = DEVICE_FILE_AUDIO;
    else if (type.mediaType&MEDIA_VIDEO)
        iRet = DEVICE_FILE_VIDEO;
    else
        iRet = DEVICE_FILE_NONE;
    return (jint)iRet;
}

static jint DLNA_DMP_1p5_GetMediaTypeByID(JNIEnv* env, jobject obj, jstring fileID)
{
	if(fileID == NULL)
		return 0;	

	char* pFileID = (char*)env->GetStringUTFChars(fileID, NULL);
	SUGGESTED_MEDIA_TYPE type;
	char *protocolInfo = NULL;
	NAVLPCMINFO *mediaInfo = NULL;
	unsigned int i = 0;	
	int iRet = 0;

	unsigned int MediaType = g_pDMP->queryFileMediaTypeByID(pFileID, &protocolInfo);

	const PROTOCOLINFO_ENTRY image_protocol[] = {
		{MIME_TYPE_IMAGE_JPEG, MEDIASUBTYPE_IMAGE_JPEG },	//JPEG_SM
		{MIME_TYPE_IMAGE_PNG, MEDIASUBTYPE_IMAGE_PNG}, 		//PNG_LRG, *.png
		{MIME_TYPE_IMAGE_TIF, MEDIASUBTYPE_IMAGE_TIFF},		// *.tif
		{MIME_TYPE_IMAGE_GIF, MEDIASUBTYPE_IMAGE_GIF},		// *.gif
		{MIME_TYPE_IMAGE_BMP, MEDIASUBTYPE_IMAGE_BITMAP},	// *.bmp
	};

	const PROTOCOLINFO_ENTRY audio_protocol[] = {
		{MIME_TYPE_AUDIO_LPCM, MEDIASUBTYPE_PCM},	//LPCM should be make up
		{MIME_TYPE_AUDIO_L16_SLPCM, MEDIASUBTYPE_PCM},	//LPCM should be make up
		{MIME_TYPE_AUDIO_L16_48_SLPCM, MEDIASUBTYPE_PCM},
		{MIME_TYPE_AUDIO_L16_48_LPCM, MEDIASUBTYPE_PCM},
		{MIME_TYPE_AUDIO_MPEG, MEDIASUBTYPE_MP3},	//*.mp3
		{MIME_TYPE_AUDIO_AAC, MEDIASUBTYPE_MP4_VIDEO},		//AAC_ISO_320, *.mp4
		{MIME_TYPE_AUDIO_ADTS, MEDIASUBTYPE_AAC},	//AAC_ADTS_320, *.aac
		{MIME_TYPE_AUDIO_WMA, MEDIASUBTYPE_WMA},	//WMABASE, *.wma
		{MIME_TYPE_AUDIO_WAV, MEDIASUBTYPE_WAVE},			//*.wav
	};

	const PROTOCOLINFO_ENTRY video_protocol[] = {
		{MIME_TYPE_VIDEO_MPEG2, MEDIASUBTYPE_MPEG_PROGRAM},	//MPEG_PS_NTSC, *.mpg
		{MIME_TYPE_VIDEO_AAC, MEDIASUBTYPE_MP4_VIDEO},		//AVC_MP4_BL_CIF15_AAC_520, *.mp4
		{MIME_TYPE_VIDEO_WMV, MEDIASUBTYPE_WMA},			//?? wrong _WMA, WMVMED_BASE, *.wmv
		{MIME_TYPE_VIDEO_ASF, MEDIASUBTYPE_WMA},			//MPEG4_P2_ASF_SP_G726. *.asf
	};

	type.mediaType = 0;
	type.fileType  = MEDIATYPE_None;		//?????	fill this later
	type.ioType    = MEDIATYPE_NET;         //?????	fill this later

	if( MediaType == MMSCP_CLASS_MASK_MAJOR_IMAGEITEM )
	{
		type.mediaType |= MEDIA_IMAGE;
		for( i = 0; i < sizeof(image_protocol)/sizeof(PROTOCOLINFO_ENTRY); i++)
		{
			if( MimeTypeMatch(protocolInfo, image_protocol[i].protocolInfoMimeType))
			{
				type.fileType = image_protocol[i].mediaType;
				break;
			}
		}
	}
	else if( MediaType == MMSCP_CLASS_MASK_MAJOR_AUDIOITEM )
	{
		type.mediaType |= MEDIA_AUDIO;
		for( i = 0; i < sizeof(audio_protocol)/sizeof(PROTOCOLINFO_ENTRY); i++)
		{
			if( MimeTypeMatch(protocolInfo, audio_protocol[i].protocolInfoMimeType))
			{
				type.fileType = audio_protocol[i].mediaType;
				break;
			}
		}

		if( type.fileType == MEDIASUBTYPE_PCM)
		{
			//since pcm is a raw data file, this should be get more mediaInfo!
			if( i == 0 ) //MIME_TYPE_AUDIO_LPCM
			{
				mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 44100;
				mediaInfo->numChannels = 2;
			}
			else if( i == 1 ) //MIME_TYPE_AUDIO_L16_SLPCM
			{
				mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 44100;
				mediaInfo->numChannels = 1;
			}
			else if( i == 2 ) //MIME_TYPE_AUDIO_L16_48_SLPCM
			{
				mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 48000;
				mediaInfo->numChannels = 1;
			}
			else if( i == 3 ) //MIME_TYPE_AUDIO_L16_48_LPCM
			{
				mediaInfo = (NAVLPCMINFO*)calloc(sizeof(NAVLPCMINFO), 1);
				mediaInfo->bitsPerSample = 16;
				mediaInfo->samplingRate = 48000;
				mediaInfo->numChannels = 2;
			}
		}
	}
	else if( MediaType == MMSCP_CLASS_MASK_MAJOR_VIDEOITEM )
	{
		type.mediaType |= MEDIA_VIDEO;
		for( i = 0; i < sizeof(video_protocol)/sizeof(PROTOCOLINFO_ENTRY); i++)
		{
			if( MimeTypeMatch(protocolInfo, video_protocol[i].protocolInfoMimeType))
			{
				type.fileType = video_protocol[i].mediaType;
				break;
			}
		}
	}
	else
	{
		ALOGD(TAG, "Warnning !!! this is NOTHING!!!\n");
	}

	if (type.mediaType&MEDIA_IMAGE)
		iRet = DEVICE_FILE_PHOTO;
	else if (type.mediaType&MEDIA_AUDIO)
		iRet = DEVICE_FILE_AUDIO;
	else if (type.mediaType&MEDIA_VIDEO)
		iRet = DEVICE_FILE_VIDEO;
	else
		iRet = DEVICE_FILE_NONE;
	
	return (jint)iRet;
}

//Add for Toshiba Live Stream
static jstring DLNA_DMP_1p5_MediaServerLocationURL(JNIEnv* env, jobject obj, jint index)
{
    char locationURL[UPNP_PROC_STR_LEN];
	memset(locationURL, 0, UPNP_PROC_STR_LEN);
    g_pDMP->MediaServerLocationURL((int)index, locationURL);
    return env->NewStringUTF(locationURL);
}

//Add for Toshiba Live Stream
static jstring DLNA_DMP_1p5_MediaServerManufacturer(JNIEnv* env, jobject obj, jint index)
{
    char manufacturer[UPNP_PROC_STR_LEN];
	memset(manufacturer, 0, UPNP_PROC_STR_LEN);
    g_pDMP->MediaServerManufacturer((int)index, manufacturer);
    return env->NewStringUTF(manufacturer);
}

//Add for Toshiba Live Stream
static jstring DLNA_DMP_1p5_MediaServerModelDescription(JNIEnv* env, jobject obj, jint index)
{
    char modelDescription[UPNP_PROC_STR_LEN];
	memset(modelDescription, 0, UPNP_PROC_STR_LEN);
    g_pDMP->MediaServerModelDescription((int)index, modelDescription);
    return env->NewStringUTF(modelDescription);
}

//Add for Toshiba Live Stream
static jstring DLNA_DMP_1p5_MediaServerRegzaApps(JNIEnv* env, jobject obj, jint index)
{
    char regzaApps[UPNP_PROC_STR_LEN];
	memset(regzaApps, 0, UPNP_PROC_STR_LEN);
    g_pDMP->MediaServerRegzaApps((int)index, regzaApps);
    return env->NewStringUTF(regzaApps);
}

//Add for Toshiba Live Stream
static jobjectArray DLNA_DMP_1p5_queryResourceListByID(JNIEnv* env, jobject obj, jstring fileID, jint queryType)
{
	jclass StringObject = env->FindClass("java/lang/String");
	if(fileID == NULL)
		return env->NewObjectArray(0, StringObject, 0);
   	char* pFileID = (char*)env->GetStringUTFChars(fileID, NULL);
    int iListSize = 0;
    char** StringList = g_pDMP->queryResourceListByID(pFileID, (UPNP_DMP_RES_TYPE)queryType, &iListSize);
    if (!iListSize) return NULL;
    jobjectArray retJobjectArray = env->NewObjectArray((jint)iListSize, StringObject, 0);
    
    for (int i=0 ; i<iListSize ; i++)
    {
    	env->SetObjectArrayElement(retJobjectArray, 
                                   i, 
                                   env->NewStringUTF(StringList[i]));
		free(StringList[i]);
	}
    free(StringList);
    return retJobjectArray;
}
static void DLNA_DMP_1p5_MediaServerSearchScan(JNIEnv* env, jobject obj)
{
    g_pDMP->MediaServerSearchScan();
}
//DLNA.ORG_OP=b
//b = (0,1) indicate the ability of ByteSeek
static jboolean QueryByteBasedSeekableofID(JNIEnv* env, jobject obj, jstring charId)
{	
	if(charId == NULL)
		return false;
    char* id = (char*)env->GetStringUTFChars(charId, NULL);
	char *protocolInfo = NULL;	
	unsigned long queryRes = 0;	
	if( g_pDMP->queryResourceByID(id, UPNP_DMP_RES_PROTOCOLINFO, (void *)&queryRes))		
		protocolInfo = (char *)queryRes;	
	if( protocolInfo != NULL && strlen(protocolInfo) != 0 )
	{		
		char *cur = NULL;		
		if( (cur = strstr( protocolInfo, "DLNA.ORG_OP"))!= NULL )		
		{			
			cur += 13;			
			if(cur[0] == '1') 
				return true;		
		}	
	}	
	return false;
}
bool QueryByteBasedSeekableofFileName(JNIEnv* env, jobject obj, jstring fileName)
{	
	if(fileName == NULL)
		return false;
	char* filename = (char*)env->GetStringUTFChars(fileName, NULL);
	char *protocolInfo = NULL;	
	unsigned long queryRes = 0;	
	if( g_pDMP->queryResourceByFile(filename, UPNP_DMP_RES_PROTOCOLINFO, (void *)&queryRes))		
		protocolInfo = (char *)queryRes;	
	if( protocolInfo != NULL && strlen(protocolInfo) != 0 )	
	{		
		char *cur = NULL;		
		if( (cur = strstr( protocolInfo, "DLNA.ORG_OP"))!= NULL )		
		{			
			cur += 13;			
			if(cur[0] == '1') return true;		
		}	
	}	
	return false;
}

static jbyteArray DLNA_DMP_1p5_getMediaInfo(JNIEnv* env, jobject obj, jstring fileName)
{
	char* pFilename = (char*)env->GetStringUTFChars(fileName, NULL);
	SUGGESTED_MEDIA_TYPE type;
	NAVLPCMINFO *mediaInfo = NULL;
	mediaInfo = GetMediaType(pFilename, type);
	int size = sizeof(NAVLPCMINFO);
	jbyteArray	jbarray = env->NewByteArray((jsize)size);
	if (mediaInfo)
	{
		env->SetByteArrayRegion(jbarray, 0, (jsize)size, (jbyte *)mediaInfo);
	}
    return jbarray;
}

static jint DLNA_DMP_1p5_getFileType(JNIEnv* env, jobject obj, jstring fileName)
{
	char* pFilename = (char*)env->GetStringUTFChars(fileName, NULL);
	SUGGESTED_MEDIA_TYPE type;
	GetMediaType(pFilename, type);
	int fileType = type.fileType;
    return (jint)fileType;
}

static void DLNA_DMP_1p5_MediaServerDelete(JNIEnv* env, jobject obj, jstring chosenUDN)
{
	if(chosenUDN == NULL)
		return;
	char* chosenUUID = (char*)env->GetStringUTFChars(chosenUDN, NULL);
    g_pDMP->MediaServerDelete(chosenUUID);
}

static jstring DLNA_DMP_1p5_MediaServerUDN(JNIEnv* env, jobject obj, jint index)
{
    char chosenUDN[UPNP_PROC_STR_LEN];
	memset(chosenUDN, 0, UPNP_PROC_STR_LEN);
    g_pDMP->MediaServerUDN((int)index, chosenUDN);
    return env->NewStringUTF(chosenUDN);
}

static jboolean DLNA_DMP_1p5_setMediaServerByUDN(JNIEnv* env, jobject obj, jstring serverUDN)
{
	if(serverUDN == NULL)
		return false;
	char* serverUUID = (char*)env->GetStringUTFChars(serverUDN, NULL);
	 return (jboolean)g_pDMP->setMediaServerByUDN(serverUUID);
}

////////////////////////////////////////////////////////////////////////////////


////////////////////////////////////////////////////////////////////////////////
//  Register Native Methods
////////////////////////////////////////////////////////////////////////////////
static JNINativeMethod sMethods[] =
{
     /* name, signature, funcPtr */
    { "DLNA_DMP_1p5_Init", "()V", (void*)DLNA_DMP_1p5_Init }, 
    { "DLNA_DMP_1p5_UnInit", "()V", (void*)DLNA_DMP_1p5_UnInit }, 
    { "DLNA_DMP_1p5_Start", "()V", (void*)DLNA_DMP_1p5_Start }, 
    { "DLNA_DMP_1p5_Terminate", "()V", (void*)DLNA_DMP_1p5_Terminate }, 
    { "DLNA_DMP_1p5_stopWatingResponse", "()V", (void*)DLNA_DMP_1p5_stopWatingResponse },
    { "DLNA_DMP_1p5_RegisterBrowserUpdateFunc", "()V", (void*)DLNA_DMP_1p5_RegisterBrowserUpdateFunc },
    { "DLNA_DMP_1p5_UnregisterBrowserUpdateFunc", "()V", (void*)DLNA_DMP_1p5_UnregisterBrowserUpdateFunc },
    { "DLNA_DMP_1p5_UPnPServiceDeviceBrowse", "()Z", (void*)DLNA_DMP_1p5_UPnPServiceDeviceBrowse },
    { "DLNA_DMP_1p5_UPnPServiceBrowse", "(Ljava/lang/String;)Z", (void*)DLNA_DMP_1p5_UPnPServiceBrowse }, 
    { "DLNA_DMP_1p5_cdupDirectoryStack", "(I)Z", (void*)DLNA_DMP_1p5_cdupDirectoryStack }, 
    { "DLNA_DMP_1p5_unsetMediaServer", "()V", (void*)DLNA_DMP_1p5_unsetMediaServer }, 
    { "DLNA_DMP_1p5_setMediaServerByFriendlyName", "([B)Z", (void*)DLNA_DMP_1p5_setMediaServerByFriendlyName }, 
    { "DLNA_DMP_1p5_setDirectoryByTitleName", "(Ljava/lang/String;)Z", (void*)DLNA_DMP_1p5_setDirectoryByTitleName }, 
    { "DLNA_DMP_1p5_MediaContainerObjectSizes", "()I", (void*)DLNA_DMP_1p5_MediaContainerObjectSizes }, 
    { "DLNA_DMP_1p5_MediaItemObjectSizes", "()I", (void*)DLNA_DMP_1p5_MediaItemObjectSizes },
    { "DLNA_DMP_1p5_MediaItemVideoSizes", "()I", (void*)DLNA_DMP_1p5_MediaItemVideoSizes }, 
    { "DLNA_DMP_1p5_MediaItemAudioSizes", "()I", (void*)DLNA_DMP_1p5_MediaItemAudioSizes }, 
    { "DLNA_DMP_1p5_MediaItemImageSizes", "()I", (void*)DLNA_DMP_1p5_MediaItemImageSizes }, 
    { "DLNA_DMP_1p5_queryResourceByFile", "(Ljava/lang/String;I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_queryResourceByFile }, 
    { "DLNA_DMP_1p5_queryResourceByID", "(Ljava/lang/String;I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_queryResourceByID },
    { "DLNA_DMP_1p5_querySubtitleURIListByID", "(Ljava/lang/String;Ljava/lang/String;)[Ljava/lang/Object;", (void*)DLNA_DMP_1p5_querySubtitleURIListByID },
    { "DLNA_DMP_1p5_MediaServerSizes", "()I", (void*)DLNA_DMP_1p5_MediaServerSizes },
    { "DLNA_DMP_1p5_MediaServerName", "(I)[B", (void*)DLNA_DMP_1p5_MediaServerName }, 
    { "DLNA_DMP_1p5_UPnPServiceContentDirectory", "(I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_UPnPServiceContentDirectory }, 
    { "DLNA_DMP_1p5_queryContainerObjInfoByIndex", "(I)Lcom/realtek/DLNA_DMP_1p5/UPnPObjInfo;", (void*)DLNA_DMP_1p5_queryContainerObjInfoByIndex }, 
    { "DLNA_DMP_1p5_UPnPServiceContentFile", "(I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_UPnPServiceContentFile }, 
    { "DLNA_DMP_1p5_queryItemObjInfoByIndex", "(I)Lcom/realtek/DLNA_DMP_1p5/UPnPObjInfo;", (void*)DLNA_DMP_1p5_queryItemObjInfoByIndex }, 
    { "DLNA_DMP_1p5_GetMediaType", "(Ljava/lang/String;)I", (void*)DLNA_DMP_1p5_GetMediaType },
    { "DLNA_DMP_1p5_GetMediaTypeByID", "(Ljava/lang/String;)I", (void*)DLNA_DMP_1p5_GetMediaTypeByID },
    { "DLNA_DMP_1p5_MediaServerLocationURL", "(I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_MediaServerLocationURL },
    { "DLNA_DMP_1p5_MediaServerManufacturer", "(I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_MediaServerManufacturer },
    { "DLNA_DMP_1p5_MediaServerModelDescription", "(I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_MediaServerModelDescription },
    { "DLNA_DMP_1p5_MediaServerRegzaApps", "(I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_MediaServerRegzaApps },
    { "DLNA_DMP_1p5_queryResourceListByID", "(Ljava/lang/String;I)[Ljava/lang/Object;", (void*)DLNA_DMP_1p5_queryResourceListByID },
    { "QueryByteBasedSeekableofID", "(Ljava/lang/String;)Z", (void*)QueryByteBasedSeekableofID},
	{ "QueryByteBasedSeekableofFileName", "(Ljava/lang/String;)Z", (void*)QueryByteBasedSeekableofFileName},
	{ "DLNA_DMP_1p5_MediaServerSearchScan", "()V", (void*)DLNA_DMP_1p5_MediaServerSearchScan },
	{ "DLNA_DMP_1p5_getMediaInfo", "(Ljava/lang/String;)[B", (void*)DLNA_DMP_1p5_getMediaInfo },
	{ "DLNA_DMP_1p5_getFileType", "(Ljava/lang/String;)I", (void*)DLNA_DMP_1p5_getFileType },
	{ "DLNA_DMP_1p5_MediaServerDelete", "(Ljava/lang/String;)V", (void*)DLNA_DMP_1p5_MediaServerDelete },
	{ "DLNA_DMP_1p5_MediaServerUDN", "(I)Ljava/lang/String;", (void*)DLNA_DMP_1p5_MediaServerUDN },
	{ "DLNA_DMP_1p5_setMediaServerByUDN", "(Ljava/lang/String;)Z", (void*)DLNA_DMP_1p5_setMediaServerByUDN }
};

#define GET_FIELD_ID(var, clazz, fieldName, fieldDescriptor) \
        var = env->GetFieldID(clazz, fieldName, fieldDescriptor); \
        LOG_FATAL_IF(! var, "Unable to find field " fieldName);

void GetUPnPObjInfoField(JNIEnv* env)
{
    g_UPnPObjInfo.clazz = env->FindClass("com/realtek/DLNA_DMP_1p5/UPnPObjInfo");
    if (g_UPnPObjInfo.clazz == NULL)
    {
        ALOGE(TAG, "Unable to find class com.realtek.DLNA_DMP_1p5.UPnPObjInfo");
        return;
    }
    
    g_UPnPObjInfo.clazz = (jclass) env->NewGlobalRef(g_UPnPObjInfo.clazz);
    g_UPnPObjInfo.init = env->GetMethodID(g_UPnPObjInfo.clazz, "<init>", "()V");
    LOG_FATAL_IF(gChannelInfo.init == NULL, "Unable to find constructor for android.app.tv.ChannelInfo");
    
    GET_FIELD_ID(g_UPnPObjInfo.UniqueCharID, g_UPnPObjInfo.clazz, "UniqueCharID", "Ljava/lang/String;");
    GET_FIELD_ID(g_UPnPObjInfo.UniqueCharParentID, g_UPnPObjInfo.clazz, "UniqueCharParentID", "Ljava/lang/String;");
    GET_FIELD_ID(g_UPnPObjInfo.TitleName, g_UPnPObjInfo.clazz, "TitleName", "Ljava/lang/String;");
    GET_FIELD_ID(g_UPnPObjInfo.ProtocolInfo, g_UPnPObjInfo.clazz, "ProtocolInfo", "Ljava/lang/String;");
    GET_FIELD_ID(g_UPnPObjInfo.Uri, g_UPnPObjInfo.clazz, "Uri", "Ljava/lang/String;");
}

#ifdef ENABLE_ANDROID_NDK
/*
* Register several native methods for one class.
*/
static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods)
{
	jclass clazz;
	clazz = env->FindClass(className);if (clazz == NULL) 
	{
		ALOGE(TAG, "Native registration unable to find class");
		return JNI_FALSE;
	}
	if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) 
	{
		ALOGE(TAG, "RegisterNatives failed");
		return JNI_FALSE;
	}
	return JNI_TRUE;
}
#endif

JavaVM* get_VM;
jclass DLNAClass;
void register_DLNA_DMP_1p5(JNIEnv* env)
{
    jclass clazz = env->FindClass("com/realtek/DLNA_DMP_1p5/DLNA_DMP_1p5");
    
    if (clazz == NULL)
        ALOGE(TAG, "Unable to find class com.realtek.DLNA_DMP_1p5.DLNA_DMP_1p5");

	DLNAClass = reinterpret_cast<jclass>(env->NewGlobalRef(clazz));
    env->GetJavaVM(&g_VM);
	get_VM = g_VM;
    //g_InvokeBrowserUpdateFunc = env->GetMethodID(clazz, "InvokeBrowserUpdateFunc", "()V");
    
    GetUPnPObjInfoField(env);
    
#ifdef ENABLE_ANDROID_NDK
	registerNativeMethods(env, "com/realtek/DLNA_DMP_1p5/DLNA_DMP_1p5", sMethods, sizeof(sMethods) / sizeof(sMethods[0]));
#else
	jniRegisterNativeMethods(env, "com/realtek/DLNA_DMP_1p5/DLNA_DMP_1p5", sMethods, NELEM(sMethods));
#endif
}
