#ifndef __VENUS_DLNA_DMR_HEADER_FILE__
#define __VENUS_DLNA_DMR_HEADER_FILE__

#include <OSAL.h>
#include <semaphore.h>
#include <pthread.h>

#ifdef ANDROID_PLATFORM
#include <EType.h>
#else
#include <Application/AppClass/VideoPlayback.h>
#endif
#include "MediaType.h"
#include "subRenderer.h"
#include "DMR.h"
#include "ILibParsers.h"
#include "DMRCommon.h"
#include "CdsObject.h"
#include "CdsMediaClass.h"
#include "MimeTypes.h"
#include <sys/stat.h>

#ifndef HAS_FLAG
 #define HAS_FLAG(flag, bit)    ((flag) & (bit))
 #define SET_FLAG(flag, bit)    ((flag)|= (bit))
 #define RESET_FLAG(flag, bit)  ((flag)&= (~(bit)))
#endif 

typedef enum
{
	DLNA_DMR_CB_NONE = 0,
	DLNA_DMR_CB_QUERYFORCONNECT,
	DLNA_DMR_CB_PREPAREFORCONNECT,
	DLNA_DMR_CB_PREPAREFORDISCONNECT,
	DLNA_DMR_CB_SETBRIGHTNESS,
	DLNA_DMR_CB_SETCONTRAST,
	DLNA_DMR_CB_SHOWSTATUS,
	DLNA_DMR_CB_UPDATEINFO,
	DLNA_DMR_CB_SHOWVOLUMESTATUS,
	DLNA_DMR_CB_SETMUTE,
	DLNA_DMR_CB_SHOWDIALOG,
	DLNA_DMR_CB_RESTART,
} DLNA_DMR_CB_FUNC;
typedef enum
{
       DLNA_DMR_VOL_NONE = 0,
       DLNA_DMR_VOL_NUM,
       DLNA_DMR_VOL_MUTE,
}DLNA_DMR_VOL_STATUS;
	
class RTK_DLNA_DMR {

private:
	static void             *m_pDMR_MicroStack_Chain;
	static void             *m_pDMR_ILib_Pool;
	static char             *m_pDMR_ProtocolInfo;	// save in DMR.c, DMR_InternalState
	static char             *m_pDMR_mediaProtocolInfo;
	static char             *m_pDMR_mediaMimeTypeProtocol;
	static pthread_t         m_DMRthread;
	static pthread_t         m_DMRMonitorthread;
//	static osal_mutex_t      m_mutexRegisterFunc;
	static osal_mutex_t      m_mutexSubRenderer;
	static osal_mutex_t		 *m_LoadMediaMutex;
	static bool              m_bIsInternalStop;
	static bool              m_bIsDMRBusy;
	static bool              m_bIsDMRChainAlive;
	static bool				 m_bIsDMRMonitorThreadRunning;
	static bool              m_bIsDMRSupportPause;
	static bool              m_bIsDMRSupportSeek;
	static bool              m_bIsDMRSupportStalling;
 
	static char*             m_pSetAVURI;
	static SUBRENDERER_TYPE  m_SETAVType;
	static ENUM_MEDIA_TYPE   m_mediaType;
	static int				 m_dialogState;
	static unsigned int      m_NumberOfTracks;
	static unsigned int      m_CurrentTrack;
	static char**            m_ppMediaTrackURI;
	static char**			 m_ppProtocolInfo;
	static SUBRENDERER_TYPE* m_pMediaType;
	static long              m_totalTime;
	static struct CdsObject *m_pMediaMetadata;

//	static int m_state;
	
	static DMR_MediaPlayMode m_PlayMode;
	static int m_Speed;
	// sem_t and sync ...
#ifndef ANDROID_PLATFORM	
	static VideoPlayback *m_pPlayback;
#endif
	static void          *m_pDMR_Monitor;

	static int m_ipAddressLength;
	static int *m_pIpAddressList;

	//FOR PCM usage
	static int 	nBitPerSample;
	static int  nChannel;
	static int 	nSampleRate;

private:
	// type 0 strExtension  1 ProtocolInfo
	static SUBRENDERER_TYPE RendererType(char *strExtensionProtocolInfo,int type = 0);
	static SUBRENDERER_TYPE RendererType(struct CdsObject *data);
//	static void UpdateDMRState();
	static void    playback_monitor(void *param);
	//static void    DMR_Network_Monitor(void *);
	static void*   DMR_Network_Monitor(void *);
	static void*   PlayTrackThread(void *);
	static void*   Run(void *arg);
	static inline bool checkDMRIsInternalCommand(){return m_bIsInternalStop;};
	static inline void SetDMRInternalCommandInit(){m_bIsInternalStop = true;};
	static inline void SetDMRInternalCommandDone(){m_bIsInternalStop = false;};
	static inline void SetDMRBlock()           {osal_MutexLock(&m_mutexSubRenderer);m_bIsDMRBusy      = true;};
	static inline void SetDMRUnBlock()         {m_bIsDMRBusy      = false;osal_MutexUnlock(&m_mutexSubRenderer);};
public:
	static inline bool checkDMRIsBusy()        {return m_bIsDMRBusy;};
	static DMR_PlayState m_PlayState;
	static bool              isSupport;
	// event functions R
public:
	static int RTKMR_AVTransport_Stop(DMR instance, void *session);
	static void RTKMR_AVTransport_PlayNext(DMR instance, void *session);
private:
	static int RTKMR_AVTransport_SetAVTransportURI(DMR instance, void *session, char *uri, struct CdsObject *data);
	static int RTKMR_AVTransport_SetNextAVTransportURI(DMR instance, void *session, char *uri, struct CdsObject *data);
	static int RTKMR_ConnectionManager_GetAVProtocolInfo(DMR instance, void *session, char **protocolInfo);
	static int RTKMR_AVTransport_Play(DMR instance, void *session, char *playSpeed);
	static int RTKMR_AVTransport_SeekTrack(DMR instance, void *session, unsigned int trackIndex);
	static int RTKMR_AVTransport_SeekTrackPosition(DMR instance, void *session, long position);
	static int RTKMR_AVTransport_SeekMediaPosition(DMR instance, void *session, long position);
	static int RTKMR_AVTransport_Next(DMR instance, void *session);
	static int RTKMR_AVTransport_Previous(DMR instance, void *session);
	static int RTKMR_RenderingControl_SelectPreset(DMR instance, void *session, char *presetName);

	// event function O
	static int RTKMR_AVTransport_SetPlayMode(DMR instance, void *session, DMR_MediaPlayMode playMode);
	static int RTKMR_AVTransport_Pause(DMR instance, void *session);
#if defined(INCLUDE_FEATURE_VOLUME)
	static int RTKMR_RenderingControl_SetVolume(DMR instance, void *session, unsigned char volume);
	static int RTKMR_RenderingControl_SetMute(DMR instance, void *session, bool mute);
#endif /* INCLUDE_FEATURE_VOLUME*/
#if defined(INCLUDE_FEATURE_DISPLAY)
	static int RTKMR_RenderingControl_SetContrast(DMR instance, void *session, unsigned char contrast);
	static int RTKMR_RenderingControl_SetBrightness(DMR instance, void *session, unsigned char brightness);
#endif /* INCLUDE_FEATURE_DISPLAY */
	//self added
	

public:
	static struct _DMR *m_pDMR_MicroStack;
	static subRenderer *m_psubRenderer;
	static char* m_DMRname;
	static char* m_UUID;
	
#ifdef ANDROID_PLATFORM	
	RTK_DLNA_DMR(osal_mutex_t* mt);
#else
	RTK_DLNA_DMR(VideoPlayback *playback_app_class,osal_mutex_t *mt);
#endif
	~RTK_DLNA_DMR();

	void Start();		// daemon-mode Start
	void Terminate();

	SUBRENDERER_TYPE GetRendererType();//{return m_psubRenderer->GetRendererType();};
	static long      GetRenderMediaFileDate();
	char*            GetRenderMediaFilename();
	char*            GetRenderMediaFullname();
	static long             GetRenderMediaFilesize();
	static long             GetRenderMediaDuration();
	int              GetRenderMediaResolutinoX();
	int              GetRenderMediaResolutionY();
	int              GetRenderMediaColorDepth();
	static int  GetPCMSampleRate(){ return nSampleRate;};
	static int  GetPCMBitPerSample(){ return nBitPerSample;};
	static int  GetPCMChannel(){ return nChannel;};
	static char *GetMediaMimeTypeProtocol(){ return m_pDMR_mediaMimeTypeProtocol;};
	static ENUM_MEDIA_TYPE GetMediaTpye(){return m_mediaType;};
	static bool CheckPlayContainerURI();
	static int SetDialogState(int dstate){m_dialogState = dstate; return 0;};
	static int GetDialogState(){return m_dialogState;};

public:
	//register callback functions from AP
	void RegisterDlnaDmrCallbackFunc( int (*updateFuncPtr)(int, void*), void *pParam ,DLNA_DMR_CB_FUNC func_type);
	void DlnaDmrInternalStop();
	void DlnaDmrInternalPause();
	void DlnaDmrInternalUnPause();
	void DlnaDmrInternalPlayNextURI();
	static int  DlnaDmrInternalSeek(int seconds);
	void DlnaDmrSyncRendererVar(unsigned char brightness, unsigned char contrast, unsigned char volume, bool mute);
//	void UnregisterDlnaDmrCallbackFunc( int (*updateFuncPtr)(int, void*), void *pParam ,DLNA_DMR_CB_FUNC  func_type);
private:
	// callback functions
	static int (*QueryForConnection)(int, void *); // (int eventID, void* eventData)
	static int (*PrepareForConnection)(int, void *); // (int eventID, void* eventData)
	static int (*PrepareForDisconnection)(int, void *); // (int eventID, void* eventData)
	static int (*SetBrightness)(int , void*);
	static int (*SetContrast)(int, void *);
	static int (*ShowStatus)(int, void*);
	static int (*UpdateMediaInfo)(int, void*);
	static int (*ShowVolumeStatus)(int, void*);
	static int (*SetMute)(int, void*);
	static int (*RestartDMR)(int, void*);
public:
	static int (*ShowDialog)(int,void*);
};

#endif
