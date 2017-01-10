#define LOG_TAG "AndroidRenderer"

#include <jni.h>
#include "DLNALog.h"
#include "DLNA_DMR.h"
#include "DMRAndroidRenderer.h"

//TODO: before each command active, check the playback state firstly
//TODO, move statis into member variable

#ifdef DLNADMRCTT
#include "DMRAndroidPlaylist.h"
extern int g_bplaycontainer;
#endif
extern bool DMR_PlayCompleted;
extern jobject gDMRServiceObj; 
extern JavaVM *gVM;
static jclass gDMRclass;
static int nextIndex;
static int RenderType;
static pthread_t   DMRPlayMonitor;
static int playTime = 0;
static int inittime = 0;
static bool playflag = false;
char* AndroidRenderer::uuid; 
bool AndroidRenderer::force_stop_thread;
int AndroidRenderer::status;
static jmethodID gRenderer_preParse;
static jmethodID gRenderer_loadMedia;
static jmethodID gRenderer_Play;
static jmethodID gRenderer_Stop;
static jmethodID gRenderer_Pause;
static jmethodID gRenderer_Restart;
static jmethodID gRenderer_QueryForConnection;
static jmethodID gRenderer_SetBrightness;
static jmethodID gRenderer_SetContrast;
static jmethodID gRenderer_ShowVolumeStatus;
static jmethodID gRenderer_SetMute;
static jmethodID gRenderer_SeekMediaPosition;
static jmethodID gRenderer_GetUUID;
static jmethodID gRenderer_SetRate;

AndroidRenderer::AndroidRenderer(SUBRENDERER_TYPE renderType)
{
       ALOGE("AndroidRender constructed  !");
	status = 0;
	playTime = 0;
	inittime = 0;
	#ifdef UCTT
	playflag = false;
	#endif
       #ifdef DLNADMRCTT
	force_stop_thread = false;
	if(g_bplaycontainer){
		pthread_create(&DMRPlayMonitor, NULL, &PlayMonitor, NULL);
		nextIndex = 1;
	}
	#endif
	Init();
	m_subRenderer_type = renderType;
	RenderType = renderType;
	m_protocolInfo = NULL;
	ALOGE("AndroidRender m_subRenderer_type %d ",m_subRenderer_type);
}
int AndroidRenderer::Init()
{
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_preParse = env->GetMethodID(gDMRclass, "preParse", "()V");
	gRenderer_loadMedia = env->GetMethodID(gDMRclass, "loadMedia", "(C)V");
	gRenderer_Play = env->GetMethodID(gDMRclass, "Play", "(Ljava/lang/String;IIIZ)V");
	gRenderer_Pause = env->GetMethodID(gDMRclass, "Pause", "(Z)V");
	gRenderer_Stop = env->GetMethodID(gDMRclass, "Stop", "()V");
	gRenderer_SeekMediaPosition = env->GetMethodID(gDMRclass, "SeekMediaPosition", "(I)V");
	DMRDetachCurrentThread();
	return S_OK;
}

AndroidRenderer::~AndroidRenderer()
{
	ALOGE("AndroidRenderer destructed !!!\n");
	Stop();
	if(m_protocolInfo)
	{
		free(m_protocolInfo);
		m_protocolInfo = NULL;
	}
	#ifdef DLNADMRCTT
	force_stop_thread = true;
	if(g_bplaycontainer)
		pthread_join(DMRPlayMonitor, 0);
	#endif
}

int AndroidRenderer::GetUUID()
{
    	ALOGE("DMR in jni GetUUIDCallBack\n"); 
	JNIEnv *env;
	gVM->GetEnv((void**)&env,JNI_VERSION_1_4);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_GetUUID = env->GetMethodID(gDMRclass, "SetUUID", "()Ljava/lang/String;");
	jstring UUID = (jstring)env->CallObjectMethod(gDMRServiceObj, gRenderer_GetUUID);
	uuid= (char*)env->GetStringUTFChars(UUID,NULL);
	env->DeleteLocalRef(UUID);
	return S_OK;
}
int AndroidRenderer::loadMedia(char *filename)
{
	//initialize, this should be double check
	//TODO, see if this is an LPCM file !!!
	bool Continue = true;
	return S_OK;
}

int AndroidRenderer::preParse(char *filename, unsigned int *NumberOfTracks, char ***MediaTrackURI, SUBRENDERER_TYPE **MediaType, long *TotalTime,char ***ProtocalInfo)
{
	unsigned int i = 0;
	char tmp_filename[1024];
	long filelength = RTK_DLNA_DMR::GetRenderMediaFilesize();
       ALOGE("DMR in jni preParseCallBack\n");

	*TotalTime = 0;
	*NumberOfTracks = 1;
	//TODO, if the input file is not single track, here should be modified !!!
	*MediaTrackURI = new char*[*NumberOfTracks];	// NumberOfTracks = 1, temporary;
	if( filename != NULL)
		*MediaTrackURI[0] = strdup(filename);
	else
		*MediaTrackURI[0] = NULL;
	*MediaType = new SUBRENDERER_TYPE[*NumberOfTracks];
	for( i = 0; i < *NumberOfTracks; i++)
		*MediaType[i] = m_subRenderer_type;

	#ifdef PPTV
	if( (filename != NULL) && (!strncmp(filename,"pptv://",strlen("pptv://"))) )
	{
		DMR_StateChange_CurrentMediaDuration(RTK_DLNA_DMR::m_pDMR_MicroStack, *TotalTime);
		DMR_StateChange_CurrentTrackDuration(RTK_DLNA_DMR::m_pDMR_MicroStack, *TotalTime);
	}
	#endif
	
	return S_OK;

}

void* AndroidRenderer::PlayMonitor(void *)
{
	//ALOGE("PlayMonitor begin !!!!!!!!");
	while(1)
	{
		//ALOGE("PlayMonitor ing !!!!!!!!!!!!!!!!!!");
		if(RenderType == 4)
		{
				usleep(1000);
				DMR_PlayCompleted = true;
				ALOGE("playback_monitor photo!");
		}
		if (!RTK_DLNA_DMR::checkDMRIsBusy()) {
		if(DMR_PlayCompleted){
				RTK_DLNA_DMR::RTKMR_AVTransport_PlayNext(RTK_DLNA_DMR::m_pDMR_MicroStack , NULL);
				DMR_PlayCompleted = false;
				ALOGE("playback_monitor  calling playnext\n");
			}
	      }
		if(force_stop_thread){
				force_stop_thread = false;
				break;
			}
		usleep(1000000);
	}
	//ALOGE("PlayMonitor exit !!!!!!!!");
	return NULL;	
}
int AndroidRenderer::Play(char *filename, int speed)
{
	ALOGE("DMR in jni PlayCallBack SPEED = %d \n", speed);

	char tmp_filename[1024] = {0};
	sprintf(tmp_filename,"%s",filename);

	char *pExtraString = NULL;
#ifndef DLNADMRCTT
	pExtraString = " forcerange";
#endif
	if (pExtraString)
	{
		strcat(tmp_filename, pExtraString);
	}

	long filelength = RTK_DLNA_DMR::GetRenderMediaFilesize();
	if(filelength > 0)
	{
		char tmp_string[64];
		sprintf(tmp_string, " contentlength=%ld", filelength);
		strcat(tmp_filename, tmp_string);
	}

	long duration = RTK_DLNA_DMR::GetRenderMediaDuration();
	if(duration > 0)
	{
		char tmp_string[64];
		sprintf(tmp_string, " duration=%ld", duration);
		strcat(tmp_filename, tmp_string);
	}

	if(m_protocolInfo)
	{
		strcat(tmp_filename, " protocolinfo=");
		strcat(tmp_filename, m_protocolInfo);
	}
	ALOGE("DMR Playback uri = %s\n", tmp_filename);

	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	jstring Filename =env->NewStringUTF(tmp_filename);
	env->CallVoidMethod(gDMRServiceObj, gRenderer_Play,Filename,speed,m_subRenderer_type,inittime,RTK_DLNA_DMR::isSupport);
	inittime = 0;//inittime for fix seek upnp certification
	env->DeleteLocalRef(Filename);
	DMRDetachCurrentThread();
	playflag = true;
	ALOGE("DMR Play exit");
	return S_OK;
}

int AndroidRenderer::Pause(bool Pause)
{
	ALOGE("DMR in jni PauseCallBack\n");
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	env->CallVoidMethod(gDMRServiceObj, gRenderer_Pause, Pause);
       DMRDetachCurrentThread();
	return S_OK;
}

void AndroidRenderer::Stop()
{
	ALOGE("DMR in jni StopCallBack\n");
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	env->CallVoidMethod(gDMRServiceObj, gRenderer_Stop);
	DMRDetachCurrentThread();
	ALOGE("DMRservice send Stop message to java done\n\n\n\n\n");
}

int AndroidRenderer::Restart(int Restart, void* start)
{
	ALOGE("DMR in jni RestartCallBack\n");
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_Restart = env->GetMethodID(gDMRclass, "Restart", "()V");
	env->CallVoidMethod(gDMRServiceObj, gRenderer_Restart);
       DMRDetachCurrentThread();
	ALOGD("DMRservice send Restart message to java done\n\n\n\n\n");
	return S_OK;
}
int AndroidRenderer::SeekMediaPosition(int titleNum, long position)
{
	ALOGE("DMR in jni SeekMediaPositionCallBack\n");
	JNIEnv *env;
	#ifdef UCTT
	if(!playflag){
		inittime = position;
		setSeekPosition(inittime*1000);
		return S_OK;
	}
	#endif
	DMRAttachCurrentThread((void**)&env);
	playTime = position;
	env->CallVoidMethod(gDMRServiceObj, gRenderer_SeekMediaPosition,playTime);
       DMRDetachCurrentThread();
	return S_OK;
}

int AndroidRenderer::setSeekPosition(int position)
{	
	long elapsedTime = (long)position;
	DMR_StateChange_AbsoluteTimePosition(RTK_DLNA_DMR::m_pDMR_MicroStack, elapsedTime);
	DMR_StateChange_RelativeTimePosition(RTK_DLNA_DMR::m_pDMR_MicroStack, elapsedTime);
	ALOGE("setSeekPosition  %d",position);
	return S_OK;
}

int AndroidRenderer::QueryForConnection(int Restart, void* start)
{
	ALOGE("DMR in jni QueryForConnectionCallBack\n");
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_QueryForConnection = env->GetMethodID(gDMRclass, "QueryForConnection", "()I");
	jint result = env->CallIntMethod(gDMRServiceObj, gRenderer_QueryForConnection);
       DMRDetachCurrentThread();
	if(result ==1)
		return E_FAIL;
	else
	return S_OK;
}
int AndroidRenderer::SetBrightness(int Bright , void* bright)
{
	ALOGE("DMR in jni SetBrightnessCallBack\n");
	JNIEnv *env;
	int* brt = (int*)bright;
	ALOGE("brightness is %d\n",*brt);
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_SetBrightness = env->GetMethodID(gDMRclass, "SetBrightness", "(I)V");
	env->CallVoidMethod(gDMRServiceObj, gRenderer_SetBrightness,*brt);
	DMRDetachCurrentThread();
	return S_OK;
}
int AndroidRenderer::SetContrast(int Contrast, void * contrast)
{
	ALOGE("DMR in jni SetContrastCallBack\n");
	JNIEnv *env;
	int* con = (int*)contrast;
	ALOGE("contrast is %d\n",*con);
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_SetContrast = env->GetMethodID(gDMRclass, "SetContrast", "(I)V");
	env->CallVoidMethod(gDMRServiceObj, gRenderer_SetContrast,*con);
	DMRDetachCurrentThread();
	return S_OK;
}
int   AndroidRenderer::ShowVolumeStatus(int Volume, void* volume)
{
	ALOGE("DMR in jni ShowVolumeStatusCallBack\n");
	int* vol = (int*)volume;
	ALOGE("volume is %d\n",*vol);
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_ShowVolumeStatus = env->GetMethodID(gDMRclass, "SetVolume", "(I)V");
	env->CallVoidMethod(gDMRServiceObj, gRenderer_ShowVolumeStatus,*vol);
	DMRDetachCurrentThread();
	return S_OK;
}
int AndroidRenderer::SetMute(int Mute, void* mute)
{
	ALOGE("DMR in jni SetMuteCallBack\n");
	bool* Mt = (bool*)mute;
	ALOGE("Mute is %d\n",*Mt);
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_SetMute = env->GetMethodID(gDMRclass, "SetMute", "(Z)V");
	env->CallVoidMethod(gDMRServiceObj, gRenderer_SetMute,*Mt);
	DMRDetachCurrentThread();
	return S_OK;
}
void AndroidRenderer::SetProtocolInfo(char *pinfo)
{
    if(m_protocolInfo)
    {
        free(m_protocolInfo);
        m_protocolInfo = NULL;
    }

    if(pinfo)
        m_protocolInfo = strdup(pinfo);
}
int AndroidRenderer::SetRate(int rate)
{
	ALOGE("DMR in jni SetRateCallBack \n rate = %d\n",rate);
	JNIEnv *env;
	DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_SetRate = env->GetMethodID(gDMRclass, "SetRate", "(I)V");
	env->CallVoidMethod(gDMRServiceObj, gRenderer_SetRate,rate);
	DMRDetachCurrentThread();
	return S_OK;
}
void AndroidRenderer::DMRAttachCurrentThread(void** env)
{
       status = gVM->GetEnv(env,JNI_VERSION_1_4);
       if(status<0)
              gVM->AttachCurrentThread((JNIEnv**)env, NULL);   
}
void AndroidRenderer::DMRDetachCurrentThread()
{
	if(status<0)
		gVM->DetachCurrentThread();
}

