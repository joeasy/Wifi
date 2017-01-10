#include <DLNA_DMR.h>
#ifdef ANDROID_PLATFORM
#include <arpa/inet.h>
//#include <file_access.h>
#else
#include <Application/AppClass/SetupClass.h>
#endif
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <stdlib.h>
#include <assert.h>

#include "DMR_mapping_table.h"

#ifdef ANDROID_PLATFORM
#include <utils/Log.h>
#include "DMRAndroidRenderer.h"
#include "DMRAndroidPlaylist.h"
static int lastspeed = 256;
#else
#include <Application/AppClass/DMRLinuxRenderer.h>
#include "Application/AppClass/AudioUtil.h"
#endif

#ifdef DLNADMRCTT
#include <DLNA_DMP.h>
extern int DLNADMR_bPlayingAV;
extern int DLNADMR_bSupportSF;
extern int DLNADMR_bSupportTrickmode;
extern int DLNADMR_bSupportFBFFSBSF;
char g_playcontaiercid[128];
int g_bplaycontainer;
#endif

static struct CdsObject *m_pNextURIMetadata = NULL;

//TODO: check the protocol info list
//TODO: there may be "DMR_SUBR_NOTHING" in playlist, this should be considered!
//      SeekTrack !!!
//      RendererType with extension should be modify !!!
//TODO: fix warning
//TODO: LPCM
//TODO: all DMR_StateChange_XXX input values modify by (Cds_Object*)data->Res->.....
//TODO: re design the architecture member variables!!!
//TODO: re design the state machine
//TODO: output flows about DMR!!!
//TODO: think about the display information
const char* ProtocolInfoList[] = {
"http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC_520",
"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_PAL",
"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_JP_T",
"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_BASE",
"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_FULL",
"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVHIGH_FULL",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=AVC_TS_JP_AAC_T",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=AVC_TS_HD_60_AC3_T",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=AVC_TS_HD_24_AC3_T",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_JP_MPEG1_L2_T",
"http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_MP_HD_1080i_AAC",
"http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_L32_HD_AAC",
"http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_L31_HD_AAC",
"http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_L3L_SD_AAC",
"http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_CIF30_AAC_940",
"http-get:*:application/x-dtcp1;CONTENTFORMAT=\"video/vnd.dlna.mpeg-tts\":DLNA.ORG_PN=DTCP_MPEG_TS_JP_T",
"http-get:*:application/x-dtcp1;CONTENTFORMAT=\"video/vnd.dlna.mpeg-tts\":DLNA.ORG_PN=DTCP_AVC_TS_JP_AAC_T",
"http-get:*:application/x-dtcp1;CONTENTFORMAT=\"video/vnd.dlna.mpeg-tts\":DLNA.ORG_PN=DTCP_MPEG_TS_SD_JP_MPEG1_L2_T",
"http-get:*:application/x-dtcp1;CONTENTFORMAT=\"video/vnd.dlna.mpeg-tts\":DLNA.ORG_PN=DTCP_AVC_TS_HD_60_AC3_T",
"http-get:*:audio/L16;rate=44100;channels=1:DLNA.ORG_PN=LPCM",
"http-get:*:audio/L16;rate=44100;channels=2:DLNA.ORG_PN=LPCM",
"http-get:*:audio/L16;rate=48000;channels=1:DLNA.ORG_PN=LPCM",
"http-get:*:audio/L16;rate=48000;channels=2:DLNA.ORG_PN=LPCM",
"http-get:*:audio/mpeg:DLNA.ORG_PN=MP3",
"http-get:*:audio/vnd.dlna.adts:DLNA.ORG_PN=AAC_ADTS_320",
"http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO_320",
"http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMABASE",
"http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMAFULL",
"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM",
"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM_ICO",
"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED",
"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_LRG",
"http-get:*:application/x-dtcp1;CONTENTFORMAT=\"video/vnd.dlna.mpeg-tts\":ARIB.OR.JP_PN=MPEG_TTS_CP",
"http-get:*:video/vnd.dlna.mpeg-tts:TOSHIBA.CO.JP_PN=AVC_TS_DC_AAC_T",
"http-get:*:video/vnd.dlna.mpeg-tts:TOSHIBA.CO.JP_PN=AVC_TS_SQ_AAC_T",
"http-get:*:application/x-dtcp1;CONTENTFORMAT=\"video/vnd.dlna.mpeg-tts\": TOSHIBA.CO.JP_PN=DTCP_AVC_TS_DC_AAC_T",
"http-get:*:application/x-dtcp1;CONTENTFORMAT=\"video/vnd.dlna.mpeg-tts\": TOSHIBA.CO.JP_PN=DTCP_AVC_TS_SQ_AAC_T",
"http-get:*:video/3gpp:DIGION.COM_PN=AVC_3GP_HP_L4_AAC",
"http-get:*:video/3gpp:DIGION.COM_PN=AVC_3GP_HP_L4",
"http-get:*:video/3gpp:DIGION.COM_PN=MPEG4_P2_3GP_SP_L6_AAC",
"http-get:*:video/3gpp:DIGION.COM_PN=AVC_3GP_BP_L4_AAC",
"http-get:*:video/3gpp:DIGION.COM_PN=AVC_3GP_BP_L4",

"\0"
};

const char* TypeList[] = {
"AVC_MP4_BL_CIF15_AAC_520",
"MPEG_PS_PAL",
"MPEG_PS_NTSC",
"MPEG_TS_JP_T",
"WMVMED_BASE",
"WMVMED_FULL",
"WMVHIGH_FULL",
"AVC_TS_JP_AAC_T",
"AVC_TS_HD_60_AC3_T",
"AVC_TS_HD_24_AC3_T",
"MPEG_TS_SD_JP_MPEG1_L2_T",
"AVC_MP4_MP_HD_1080i_AAC",
"AVC_MP4_BL_L32_HD_AAC",
"AVC_MP4_BL_L31_HD_AAC",
"AVC_MP4_BL_L3L_SD_AAC",
"AVC_MP4_BL_CIF30_AAC_940",
"DTCP_MPEG_TS_JP_T",
"DTCP_AVC_TS_JP_AAC_T",
"DTCP_MPEG_TS_SD_JP_MPEG1_L2_T",
"DTCP_AVC_TS_HD_60_AC3_T",
"LPCM",
"MP3",
"AAC_ADTS_320",
"AAC_ISO_320",
"WMABASE",
"WMAFULL",
"JPEG_SM",
"JPEG_SM_ICO",
"JPEG_MED",
"JPEG_LRG",
"MPEG_TTS_CP",
"AVC_TS_DC_AAC_T",
"AVC_TS_SQ_AAC_T",
"DTCP_AVC_TS_DC_AAC_T",
"DTCP_AVC_TS_SQ_AAC_T",
"AVC_3GP_HP_L4_AAC",
"AVC_3GP_HP_L4",
"MPEG4_P2_3GP_SP_L6_AAC",
"AVC_3GP_BP_L4_AAC",
"AVC_3GP_BP_L4",
"\0"
};


#define FREE_MEMORY(memory) \
	if(memory != NULL) { \
        free(memory) ; \
        memory = NULL ; \
	}

#define DELETE_OBJECT(object) \
	if(object != NULL) { \
        delete object ; \
        object = NULL ; \
	}

//TODO, becareful racing codition, mutex_lock m_PlayState
#define RETURN_ERROR(error) \
	{ \
		if( m_PlayState != DMR_PS_NoMedia ) \
			m_PlayState = DMR_PS_Stopped; \
		if( ShowStatus != NULL ) \
			ShowStatus(m_PlayState, NULL); \
		DMR_StateChange_TransportPlayState(instance, m_PlayState); \
		printf("DMR RETURN_ERROR (%d) in %s:%d\n", error, __FILE__, __LINE__); \
		return error; \
	}

#define RETURN_ERROR_STOP(error) \
	{ \
		if( m_psubRenderer ) \
			m_psubRenderer->Stop(); \
		m_PlayState = DMR_PS_Stopped; \
		if( ShowStatus != NULL ) \
			ShowStatus(m_PlayState, NULL); \
		DMR_StateChange_TransportPlayState(instance, m_PlayState); \
		printf("DMR RETURN_ERROR_STOP (%d) in %s:%d\n", error, __FILE__, __LINE__); \
		return error; \
	}

#define DELETE_CDS_OBJECT(object)	\
	if (object) {					\
    	CDS_ObjRef_Release(object);	\
    	object = NULL;				\
	}

//#define NORMALIZE_VOLUME(volume) ((int)MAX_VOLUME_NUMBER*volume/100)

static inline bool GetBitVal(char *flagStr, int n) 
{
	char ch = flagStr[7-((n>>2)&0x7)];
	char target = 1<<(n&3);

	if ((ch>='0'&&ch<='9'&&((ch-'0')&target))||
	    (ch>='a'&&ch<='f'&&((ch-'a'+10)&target))||
	    (ch>='A'&&ch<='F'&&((ch-'A'+10)&target)))
	{
		return true;
	}
	return false;
}

static inline float Stringtof(char *playspeed)
{
	int i = 0;
	float speed = 0;
	int denominator;
	char * nstart = playspeed;
	char *nend = strstr(playspeed, "/");
	char numerator[16];
	if(nend)
	{
		memset(numerator, 0, 16);
		memcpy(numerator, nstart, (nend-nstart));
		char *dstart = nend + 1;
		speed = ((float)atoi(numerator)/(float)atoi(dstart));
	}else{
		speed = (float)atoi(playspeed);
	}
	return speed;
}

char* BuildProtocolInfo(const char* infoList[])
{
        int counter;
    int length = 0;
    char* result = NULL;
    char* p;
#ifdef ENABLE_DTCP_IP
#define DTCP_MIMETYPE_PREFIX  "application/x-dtcp1;CONTENTFORMAT=\""
#define DTCP_MIMETYPE_POSTFIX "\""
#define DTCP_PROFILEID_PREFIX "DTCP_"
#define DTCP_PROFILEID_NAME "DLNA.ORG_PN="
#define DLNA_ORG_FLAGS      "DLNA.ORG_FLAGS="
#endif

    if(infoList == NULL)
    {
        return NULL;
    }

    counter = 0;
    p = (char*)infoList[counter];
    while(p[0] != '\0')
    {
    	int pinfo_len = strlen(p)+1;
        length += pinfo_len;

//#ifdef ENABLE_DTCP_IP
#if 0
		if(strstr(p, DTCP_PROFILEID_NAME))
			length += pinfo_len + strlen(DTCP_PROFILEID_PREFIX) + strlen(DTCP_MIMETYPE_PREFIX) + strlen(DTCP_MIMETYPE_POSTFIX);
		else
			length += pinfo_len + strlen(DTCP_MIMETYPE_PREFIX) + strlen(DTCP_MIMETYPE_POSTFIX);
#endif

        p = (char*)infoList[++counter];
    }

	if(length == 0)
		return NULL;

    result = (char*)malloc(length);
    result[0] = 0;

    counter = 0;
    p = (char*)infoList[counter];

	char* curPos = result;
	char* startPos;
	while(1)
	{
		length = strlen(p);

		memcpy(curPos, p, length);
		curPos += length;

//#ifdef ENABLE_DTCP_IP
#if 0
		/* add DTCP protocolinfo here
		 * e.g.
		 *   add
		 *     http-get:*:application/x-dtcp1;CONTENTFORMAT="video/vnd.dlna.mpeg-tts":DLNA.ORG_PN=DTCP_MPEG_TS_HD_KO_T
		 *   for
		 *     http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_KO_T
		 *
		 * Seg1=> http-get:*:
		 * MimeType=> video/vnd.dlna.mpeg-tts
		 * Seg2=> :DLNA.ORG_PN=
		 * Seg3=> MPEG_TS_HD_KO_T
		 */
		do{
			char *p_mimetype = strstr(p, ":*:");
			startPos = curPos;
			if(p_mimetype)
			{
				int SegLen1, SegLen2, SegLen3, MimeTypeLen;
				char *p_Seg2, *p_Seg3, *p_flags;

				p_mimetype += strlen(":*:");

				SegLen1 = p_mimetype-p;

				p_Seg2 = strstr(p_mimetype, ":");
				if(!p_Seg2)
				{
					// should not enter here...
					// protocolinfo format error??

					// skip this protocolinfo
					break;
				}
				MimeTypeLen = p_Seg2 - p_mimetype;

				p_Seg3 = strstr(p_Seg2, DTCP_PROFILEID_NAME);
				if(p_Seg3)
				{
					p_Seg3 += strlen(DTCP_PROFILEID_NAME);
					SegLen2 = p_Seg3 - p_Seg2;
					SegLen3 = length - SegLen1 - MimeTypeLen - SegLen2;
				}
				else
				{
					SegLen2 = length - SegLen1 - MimeTypeLen;
					SegLen3 = 0;
				}


				// delimiter
				*curPos = ',';
				curPos++;

				// Seg1
				memcpy(curPos, p, SegLen1);
				curPos += SegLen1;

				// MimeType
				memcpy(curPos, DTCP_MIMETYPE_PREFIX, strlen(DTCP_MIMETYPE_PREFIX));
				curPos += strlen(DTCP_MIMETYPE_PREFIX);
				memcpy(curPos, p_mimetype, MimeTypeLen);
				curPos += MimeTypeLen;
				memcpy(curPos, DTCP_MIMETYPE_POSTFIX, strlen(DTCP_MIMETYPE_POSTFIX));
				curPos += strlen(DTCP_MIMETYPE_POSTFIX);;

				// Seg2
				memcpy(curPos, p_Seg2, SegLen2);
				curPos += SegLen2;

				// Seg3
				if(p_Seg3)
				{
					memcpy(curPos, DTCP_PROFILEID_PREFIX, strlen(DTCP_PROFILEID_PREFIX));
					curPos += strlen(DTCP_PROFILEID_PREFIX);
					memcpy(curPos, p_Seg3, SegLen3);
					curPos += SegLen3;
				}
				*curPos = 0;

				//set flags in  DLNA.ORG_FLAGS
				p_flags = strstr(startPos, DLNA_ORG_FLAGS);
				if(p_flags)
				{
					char val;
					// LP flag, bit 16
					p_flags += strlen(DLNA_ORG_FLAGS) + 3; // bit 16 is in the 4th byte, mask = 0x1
					val = *p_flags;
					//printf("[steve] got DLNA_ORG_FLAGS(LP), %c %c %c\n", *(p_flags-1), *p_flags, *(p_flags+1));
					if((val >= '0' && val <= '9' && ((val-'0')&1)==0) ||
					   (val >= 'a' && val <= 'e' && ((val-'a'+10)&1)==0) ||
					   (val >= 'A' && val <= 'E' && ((val-'A'+10)&1)==0))
					{
						//printf("[steve] bit 16 from %c to %c\n", val, val+1);
						*p_flags = val+1;
					}

					// cleartextbyteseek-full flag, bit 15
					p_flags++; // bit 15 is in the 5th byte, mask = 0x8
					val = *p_flags;
					if(val >= '0' && val <= '7')
					{
						val -= '0';
						//printf("[steve] bit 15 from %X to %X\n", val, val+8);
						val += 8;
						if(val <= 9)
							*p_flags = val+'0';
						else
							*p_flags = val-10+'A';
					}
				}
			}
		}while(0);

#endif

		p = (char*)infoList[++counter];
		if(p[0] == '\0')
			break;

		*curPos = ',';
		curPos++;
	}
	*curPos = 0;
    return result;
}

bool IsSurportProtocolInfo(char* portocolInfo)
{
	int i = 0;
	char* flag1;
	char* flag2;
	char* flag3;
	char* flag4;
	char* prostart;

	flag1 = strstr(portocolInfo,"DLNA.ORG_PN");
	flag2 = strstr(portocolInfo,"ARIB.OR.JP_PN");
	flag3 = strstr(portocolInfo,"TOSHIBA.CO.JP_PN");
	flag4 = strstr(portocolInfo,"DIGION.COM_PN");
	//ALOGE("portocolInfo = %s,flag1 = %d,flag2 = %d,flag3 = %d,flag4= %d",portocolInfo,flag1,flag2,flag3,flag3);
	if(flag1 || flag2 || flag3 || flag4)
	{
		while(strcmp(TypeList[i],"\0")!=0)
		{
			prostart = strstr(portocolInfo,TypeList[i]);
			i++;
			//ALOGE("TypeList[%d] = %s, prostart = %d",i,TypeList[i]),prostart;
			if(prostart)
				return true;
		}
		return false;
	}else
		return false;
}
bool IsMPEG_TS_JP_T(char* portocolInfo)
{
	char* isIn;
	char* flagStart = strstr(portocolInfo,"DLNA.ORG_PN");
	if(flagStart)
	{
		isIn = strstr(portocolInfo,"MPEG_TS_JP_T");
		if(isIn)
			return true;
		else 
			return false;
	}else
	return false;
}

void* RTK_DLNA_DMR::m_pDMR_MicroStack_Chain;
void* RTK_DLNA_DMR::m_pDMR_ILib_Pool;
char* RTK_DLNA_DMR::m_pDMR_mediaProtocolInfo;
char* RTK_DLNA_DMR::m_pDMR_ProtocolInfo;
char* RTK_DLNA_DMR::m_pDMR_mediaMimeTypeProtocol;
struct _DMR*      RTK_DLNA_DMR::m_pDMR_MicroStack;
char*             RTK_DLNA_DMR::m_pSetAVURI;
char*  		RTK_DLNA_DMR::m_DMRname;
char*  		RTK_DLNA_DMR::m_UUID;
SUBRENDERER_TYPE  RTK_DLNA_DMR::m_SETAVType;
ENUM_MEDIA_TYPE   RTK_DLNA_DMR::m_mediaType;
int				  RTK_DLNA_DMR::m_dialogState;
unsigned int      RTK_DLNA_DMR::m_NumberOfTracks;
unsigned int      RTK_DLNA_DMR::m_CurrentTrack;
char**            RTK_DLNA_DMR::m_ppMediaTrackURI;
char**			  RTK_DLNA_DMR::m_ppProtocolInfo;

SUBRENDERER_TYPE* RTK_DLNA_DMR::m_pMediaType;
long              RTK_DLNA_DMR::m_totalTime;
struct CdsObject *RTK_DLNA_DMR::m_pMediaMetadata;

pthread_t         RTK_DLNA_DMR::m_DMRthread;
pthread_t         RTK_DLNA_DMR::m_DMRMonitorthread;
DMR_PlayState     RTK_DLNA_DMR::m_PlayState;
DMR_MediaPlayMode RTK_DLNA_DMR::m_PlayMode;
int               RTK_DLNA_DMR::m_Speed;
//osal_mutex_t 	  RTK_DLNA_DMR::m_mutexRegisterFunc;
osal_mutex_t 	  RTK_DLNA_DMR::m_mutexSubRenderer;
osal_mutex_t *    RTK_DLNA_DMR::m_LoadMediaMutex;
bool              RTK_DLNA_DMR::m_bIsInternalStop;
bool              RTK_DLNA_DMR::m_bIsDMRBusy;
bool              RTK_DLNA_DMR::m_bIsDMRChainAlive;
bool			  RTK_DLNA_DMR::m_bIsDMRMonitorThreadRunning;
bool              RTK_DLNA_DMR::m_bIsDMRSupportPause;
bool              RTK_DLNA_DMR::isSupport;
bool              RTK_DLNA_DMR::m_bIsDMRSupportSeek;
bool              RTK_DLNA_DMR::m_bIsDMRSupportStalling;
subRenderer* RTK_DLNA_DMR::m_psubRenderer;

#ifndef ANDROID_PLATFORM
VideoPlayback* RTK_DLNA_DMR::m_pPlayback;
#endif
void*          RTK_DLNA_DMR::m_pDMR_Monitor;
int            RTK_DLNA_DMR::m_ipAddressLength;
int *          RTK_DLNA_DMR::m_pIpAddressList;

int				RTK_DLNA_DMR::nBitPerSample;
int				RTK_DLNA_DMR::nChannel;
int				RTK_DLNA_DMR::nSampleRate;


int (*(RTK_DLNA_DMR::QueryForConnection))     (int, void *); // (int eventID, void* eventData)
int (*(RTK_DLNA_DMR::PrepareForConnection))   (int, void *); // (int eventID, void* eventData)
int (*(RTK_DLNA_DMR::PrepareForDisconnection))(int, void *);
int (*(RTK_DLNA_DMR::SetBrightness))          (int , void*);
int (*(RTK_DLNA_DMR::SetContrast))            (int, void *);
int (*(RTK_DLNA_DMR::ShowStatus))             (int, void *);
int (*(RTK_DLNA_DMR::UpdateMediaInfo))        (int, void *);

int (*(RTK_DLNA_DMR::ShowVolumeStatus))        (int, void *);
int (*(RTK_DLNA_DMR::SetMute))        		   (int, void *);
int (*(RTK_DLNA_DMR::ShowDialog))             (int, void *);
int (*(RTK_DLNA_DMR::RestartDMR))             (int, void *);

#define STATUS_SET_AV_TRANSPORT_URI		(1L << 0)
#define STATUS_MESSAGE_DATA_EXIST		(1L << 1)

long			statusFlag;

static int fake_DMR_Start = 0;
#ifdef ANDROID_PLATFORM
RTK_DLNA_DMR::RTK_DLNA_DMR(osal_mutex_t* mt)//to test DMR pass by ruili
#else
RTK_DLNA_DMR::RTK_DLNA_DMR(VideoPlayback *playback_app_class,osal_mutex_t* mt)
#endif
{
 #ifdef ANDROID_PLATFORM
  	char *friendlyName ;
 	if(m_DMRname!=NULL) {
	      	 friendlyName = (char *)malloc(strlen(m_DMRname)+4);
	     	 strcpy(friendlyName,m_DMRname);
 	}
	else{
		friendlyName = (char *)malloc(strlen("T30")+4);
	      strcpy(friendlyName,"T30");
	}
  #else
	char *friendlyName = (char *)malloc(strlen(": Realtek Media Render")+strlen(SetupClass::GetInstance()->GetSystemDeviceFriendlyName())+1);
    if(strlen(SetupClass::GetInstance()->GetSystemDeviceFriendlyName()) != 0)
        snprintf(friendlyName, strlen(": Realtek Media Render")+strlen(SetupClass::GetInstance()->GetSystemDeviceFriendlyName())+1, "%s: Realtek Media Render", SetupClass::GetInstance()->GetSystemDeviceFriendlyName());
    else
        snprintf(friendlyName, strlen("Realtek Media Render")+strlen(SetupClass::GetInstance()->GetSystemDeviceFriendlyName())+1, "%sRealtek Media Render", SetupClass::GetInstance()->GetSystemDeviceFriendlyName());
  #endif
//	char *m_pDMR_ProtocolInfo = BuildProtocolInfo(ProtocolInfoList);
	m_pDMR_ProtocolInfo     = BuildProtocolInfo(ProtocolInfoList);
	m_pDMR_MicroStack_Chain = ILibCreateChain();
	m_pDMR_ILib_Pool        = ILibThreadPool_Create();
   #ifdef ANDROID_PLATFORM
	m_pDMR_MicroStack       = DMR_Method_Create(m_pDMR_MicroStack_Chain, 0, friendlyName, "0000002", m_UUID, m_pDMR_ProtocolInfo, m_pDMR_ILib_Pool);
   #else
	m_pDMR_MicroStack       = DMR_Method_Create(m_pDMR_MicroStack_Chain, 0, friendlyName, "0000001", SetupClass::GetInstance()->GetSystemDeviceUUID(), m_pDMR_ProtocolInfo, m_pDMR_ILib_Pool);
   #endif
	free(friendlyName);
	
	m_pDMR_mediaProtocolInfo     = NULL;
	m_pDMR_mediaMimeTypeProtocol = NULL;

	osal_MutexCreate(&m_mutexSubRenderer);
	m_LoadMediaMutex = mt;
	#ifndef ANDROID_PLATFORM
	m_pPlayback = playback_app_class;
	#endif

	m_psubRenderer = NULL;
	m_SETAVType    = DMR_SUBR_NOTHING;
	m_mediaType    = MEDIATYPE_None;

	m_PlayState    = DMR_PS_NoMedia;
	m_PlayMode     = DMR_MPM_Normal;
	m_Speed        = 1;

	m_pSetAVURI       = NULL;
	m_NumberOfTracks  = 0;
	m_CurrentTrack    = 1;
	m_ppMediaTrackURI = NULL;
	m_ppProtocolInfo  = NULL;
	m_pMediaType      = NULL;
	m_totalTime       = 0;
	m_pMediaMetadata  = NULL;

// set the event functions
	m_pDMR_MicroStack->Event_SetAVTransportURI = &RTKMR_AVTransport_SetAVTransportURI;
	m_pDMR_MicroStack->Event_SetNextAVTransportURI = &RTKMR_AVTransport_SetNextAVTransportURI;

	m_pDMR_MicroStack->Event_GetAVProtocolInfo = &RTKMR_ConnectionManager_GetAVProtocolInfo;
	m_pDMR_MicroStack->Event_SetPlayMode       = &RTKMR_AVTransport_SetPlayMode;
	m_pDMR_MicroStack->Event_Stop              = &RTKMR_AVTransport_Stop;
	m_pDMR_MicroStack->Event_Play              = &RTKMR_AVTransport_Play;
	m_pDMR_MicroStack->Event_Pause             = &RTKMR_AVTransport_Pause;
	m_pDMR_MicroStack->Event_SeekTrack         = &RTKMR_AVTransport_SeekTrack;
	m_pDMR_MicroStack->Event_SeekTrackPosition = &RTKMR_AVTransport_SeekTrackPosition;
	m_pDMR_MicroStack->Event_SeekMediaPosition = &RTKMR_AVTransport_SeekMediaPosition;
	m_pDMR_MicroStack->Event_Next              = &RTKMR_AVTransport_Next;
	m_pDMR_MicroStack->Event_Previous          = &RTKMR_AVTransport_Previous;
	m_pDMR_MicroStack->Event_SelectPreset      = &RTKMR_RenderingControl_SelectPreset;
#if defined(INCLUDE_FEATURE_VOLUME)
	m_pDMR_MicroStack->Event_SetVolume         = &RTKMR_RenderingControl_SetVolume;
	m_pDMR_MicroStack->Event_SetMute           = &RTKMR_RenderingControl_SetMute;
#endif /* INCLUDE_FEATURE_VOLUME */
#if defined(INCLUDE_FEATURE_DISPLAY)
	m_pDMR_MicroStack->Event_SetContrast       = &RTKMR_RenderingControl_SetContrast;
	m_pDMR_MicroStack->Event_SetBrightness     = &RTKMR_RenderingControl_SetBrightness;
#endif /* INCLUDE_FEATURE_DISPLAY */
	QueryForConnection      = NULL;
	PrepareForConnection    = NULL;
	PrepareForDisconnection = NULL;
	SetBrightness           = NULL;
	SetContrast             = NULL;
	ShowStatus              = NULL;
	ShowDialog				= NULL;
	UpdateMediaInfo         = NULL;
	ShowVolumeStatus        = NULL;
	SetMute					= NULL;
	RestartDMR              = NULL;
	m_bIsInternalStop       = false;
	m_bIsDMRBusy            = false;
	m_bIsDMRChainAlive      = false;
	m_bIsDMRMonitorThreadRunning = false;	
	m_bIsDMRSupportPause    = false;

	m_ipAddressLength = 0;
	m_pIpAddressList  = NULL;
    fake_DMR_Start    = 0;
	m_ipAddressLength = ILibGetLocalIPAddressList(&m_pIpAddressList);
	printf("m_ipaddesslist leng=%d\n",m_ipAddressLength);
	for(int i=0;i<m_ipAddressLength;i++){
		struct in_addr DMR__inaddr;
		DMR__inaddr.s_addr = m_pIpAddressList[i];
		printf("m_ipaddr[%d]=%s\n",i, inet_ntoa(DMR__inaddr));
	}
	//m_pDMR_Monitor    = (void *)ILibCreateLifeTime(m_pDMR_MicroStack_Chain);

	//ILibLifeTime_Add(m_pDMR_Monitor, NULL, 4, &DMR_Network_Monitor, NULL);

	if(m_ipAddressLength == 0)
    {
        fake_DMR_Start = 1;
        printf("DMR Fake Start Only create Monitor Thread\n");
    }

	pthread_create(&m_DMRthread, NULL, &Run, NULL);
	while(!m_bIsDMRChainAlive)
		usleep(100000);
	
	pthread_create(&m_DMRMonitorthread, NULL, &DMR_Network_Monitor, NULL);
	while(!m_bIsDMRMonitorThreadRunning)
		usleep(100000);	

	statusFlag = 0;
}

RTK_DLNA_DMR::~RTK_DLNA_DMR()
{
	DlnaDmrInternalStop();
	
	m_bIsDMRMonitorThreadRunning = false;
	pthread_join(m_DMRMonitorthread, 0);

	Terminate();

	FREE_MEMORY(m_pDMR_mediaProtocolInfo);
	FREE_MEMORY(m_pDMR_mediaMimeTypeProtocol);

	//TODO, check ....
	if( m_pDMR_ILib_Pool )
	{
		while(m_bIsDMRChainAlive)
		{
			usleep(100);
		}

		ILibThreadPool_Destroy( m_pDMR_ILib_Pool );
	}

	pthread_join(m_DMRthread, 0);
	//pthread_join(m_DMRMonitorthread, 0);

	if( m_pSetAVURI != NULL )
	{
		free(m_pSetAVURI);
		m_pSetAVURI = NULL;
	}
	if( m_DMRname != NULL )
	{
		free(m_DMRname);
		m_DMRname = NULL;
	}
	if( m_UUID != NULL )
	{
		free(m_UUID);
		m_UUID = NULL;
	}
	if( m_ppMediaTrackURI != NULL )	//m_ppMediaTrackURI[m_NumberOfTracks];
	{
		unsigned int i = 0;
		for( i = 0; i < m_NumberOfTracks; i++)
		{
			if( m_ppMediaTrackURI[i] != NULL )
			{
				free(m_ppMediaTrackURI[i]);
				m_ppMediaTrackURI[i] = NULL;
			}
		}
		delete [] m_ppMediaTrackURI;
		m_ppMediaTrackURI = NULL;
	}
	
	if( m_ppProtocolInfo != NULL ) 
	{
		unsigned int i = 0;
		for( i = 0; i < m_NumberOfTracks; i++)
		{
			if( m_ppProtocolInfo[i] != NULL )
			{
				free(m_ppProtocolInfo[i]);
				m_ppProtocolInfo[i] = NULL;
			}
		}
		delete [] m_ppProtocolInfo;
		m_ppProtocolInfo = NULL;
	}

	
		
	if( m_pMediaType != NULL )	//m_pMediaType[m_NumberOfTracks];
	{
		delete [] m_pMediaType;
		m_pMediaType = NULL;
	}

	if(m_ipAddressLength != 0)
	{
		free(m_pIpAddressList);
		m_pIpAddressList = NULL;
	}

	if(m_pDMR_ProtocolInfo != 0)
	{
		free(m_pDMR_ProtocolInfo);
		m_pDMR_ProtocolInfo = NULL;
	}

	DELETE_CDS_OBJECT(m_pMediaMetadata);
    DELETE_CDS_OBJECT(m_pNextURIMetadata);

	osal_MutexDestroy(&m_mutexSubRenderer);
}

void *RTK_DLNA_DMR::Run(void* arg) {
	m_bIsDMRChainAlive = true;
    if(fake_DMR_Start == 1){
        printf("####DMR Fake Start\n");
        return NULL;
    }
#ifndef ANDROID_PLATFORM
	pli_setThreadName("DLNA_DMR");
#endif
	ILibStartChain(m_pDMR_MicroStack_Chain);
	m_bIsDMRChainAlive = false;
	return NULL;
}

/*
void RTK_DLNA_DMR::DMR_Network_Monitor(void *)
{
        int length;
        int *list;
        length = ILibGetLocalIPAddressList(&list);
//        if(length!=cp->AddressListLength|| memcmp((void*)list,(void*)(cp->AddressList),sizeof(int)*length)!=0)

	printf("m_ipaddessleng=%d newleng=%d\n",m_ipAddressLength,length);
	for(int i=0;i<m_ipAddressLength;i++)
		printf("m_ipaddr[%d]=%d\n",i,m_pIpAddressList[i]);
	
	for(int j=0;j<length;j++)
		printf("newaddr[%d]=%d\n",j,list[j]);

	if( length != m_ipAddressLength || memcmp((void*)list, (void*)m_pIpAddressList, sizeof(int)*length)!=0)
	{
		if(m_ipAddressLength != 0)
		{
			free(m_pIpAddressList);
			m_pIpAddressList = NULL;
		}
        	m_ipAddressLength = ILibGetLocalIPAddressList(&m_pIpAddressList);
		//m_ipAddressLength = length;
		//m_pIpAddressList = list;
		DMR_Method_NotifyMicrostackOfIPAddressChange(m_pDMR_MicroStack);
		printf("%s, %s, %d System IPs changes!!!\n", __FILE__, __func__, __LINE__);
		
		if(RestartDMR != NULL)RestartDMR(0,NULL);

	}
        free(list);
	ILibLifeTime_Add(m_pDMR_Monitor, NULL, 4, &DMR_Network_Monitor, NULL);
}
*/
void* RTK_DLNA_DMR::DMR_Network_Monitor(void *)
{
    int length;
    int *list;
	int count=0;

	m_bIsDMRMonitorThreadRunning = true;
	while(m_bIsDMRMonitorThreadRunning)
	{
		if(count <= 4){
			count++;
			sleep(1);
		}
		else{
			count = 0;
        length = ILibGetLocalIPAddressList(&list);
		
	if( length != m_ipAddressLength || memcmp((void*)list, (void*)m_pIpAddressList, sizeof(int)*length)!=0)
	{
		struct in_addr DMR__inaddr;
		printf("m_ipaddessleng=%d newleng=%d\n",m_ipAddressLength,length);
		for(int i=0;i<m_ipAddressLength;i++){
			DMR__inaddr.s_addr = m_pIpAddressList[i];
			printf("m_ipaddr[%d]=%s\n",i,inet_ntoa(DMR__inaddr));
		}
		
		for(int j=0;j<length;j++){
			DMR__inaddr.s_addr = list[j];
			printf("newaddr[%d]=%s\n",j,inet_ntoa(DMR__inaddr));
		}
			int bNeedRestart = 1;
			for(int i=0;i<m_ipAddressLength;i++)
			for(int j=0;j<length;j++){
				if(m_pIpAddressList[i] == list[j])
				{
					DMR__inaddr.s_addr = m_pIpAddressList[i];
					if(m_pSetAVURI != NULL)
					{
						char ipaddr[64];
						char *p,*tmp,*tmp1,*tmp2,*tmp3;
						if(p = strstr(m_pSetAVURI,"http://")){
							tmp = p + 7;
							if(tmp1 = strchr(tmp,'.')){
								if(tmp2 = strchr(tmp1+1,'.')){
									if(tmp3 = strchr(tmp2+1,'.')){
										memset(ipaddr,0,64);
										strncpy(ipaddr,tmp,tmp3-tmp);
										if(strstr(inet_ntoa(DMR__inaddr),ipaddr))
										{
											printf("%s contains  URI addr=%s\n",inet_ntoa(DMR__inaddr),ipaddr);
											bNeedRestart = 0;
										}
										
									}					
								}
							}
						}

					}
				}
			}
			
			if(m_ipAddressLength != 0)
			{
				free(m_pIpAddressList);
				m_pIpAddressList = NULL;
			}
	        	m_ipAddressLength = ILibGetLocalIPAddressList(&m_pIpAddressList);
			//m_ipAddressLength = length;
			//m_pIpAddressList = list;
			if(!bNeedRestart)
			DMR_Method_NotifyMicrostackOfIPAddressChange(m_pDMR_MicroStack);
			printf("%s, %s, %d System IPs changes!!!\n", __FILE__, __func__, __LINE__);

			if(bNeedRestart){
				printf("DMR Need to Restart\n");
				if(RestartDMR != NULL)RestartDMR(0,NULL);
			}
		}
	        free(list);
		}
	}
	printf("DMR Monitor Thread Exit\n");
	return NULL;	
}

void* RTK_DLNA_DMR::PlayTrackThread(void *arg)
{
	SetDMRBlock();
	DELETE_OBJECT(m_psubRenderer);
 #ifdef ANDROID_PLATFORM
	if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_AUDIO )
		m_psubRenderer = new AndroidRenderer(DMR_SUBR_AUDIO);
	else if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_VIDEO ) 	
		m_psubRenderer = new AndroidRenderer(DMR_SUBR_VIDEO);
	else if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_IMAGE )
		m_psubRenderer = new AndroidRenderer(DMR_SUBR_IMAGE);
	else
		assert(0);
  #else 
	if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_AUDIO )
		m_psubRenderer = new avRenderer(m_pPlayback, DMR_SUBR_AUDIO);
	else
	if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_VIDEO ) 
		m_psubRenderer = new avRenderer(m_pPlayback, DMR_SUBR_VIDEO);
	else
	if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_IMAGE )
		m_psubRenderer = new imageRenderer(m_pPlayback );
	else
		assert(0);	// this should not happen !!!
  #endif
	SetDMRUnBlock();

	m_PlayState = DMR_PS_Transitioning;

	if(m_ppProtocolInfo)
		m_pMediaType[m_CurrentTrack-1] = RendererType(m_ppProtocolInfo[m_CurrentTrack-1],1);

	if(m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_NOTHING)
	{
		char *extension = strrchr(m_ppMediaTrackURI[m_CurrentTrack-1], '.');
		if( extension != NULL )
			m_pMediaType[m_CurrentTrack-1] = RendererType(extension);
	}
	
	RTKMR_AVTransport_Play(m_pDMR_MicroStack, NULL, NULL);
	return NULL;
}

void RTK_DLNA_DMR::Start()
{
	;
}

void RTK_DLNA_DMR::Terminate()
{
    if(fake_DMR_Start)
	{
        printf("#########Fake DMR Stop\n");
        m_bIsDMRChainAlive = false;
        return;
       }
	printf("#############Stoping DMR Chain\n");
	ILibStopChain(m_pDMR_MicroStack_Chain);
}


int RTK_DLNA_DMR::RTKMR_AVTransport_SetAVTransportURI(DMR instance, void *session, char *uri, struct CdsObject *data)
{
/**
		!! lock the device !!
		!! check uri simply, loadMedia already yet...!!
	0. check the dmr instance
	1. check the uri
	2. reset the related variables
	3. Get the AVTURI type and AVProtocolInfo
	4. set NumberOfTracsks, m_CurrentTrack, m_ppMediaTrackURI[][], m_pMediatype[],
 	   according to m_SETAVType
	5. store URI, and update related variables to DMR.c

		!! TODO racing concerned!! / Sync ?? 
		!!                                !!
		!! TODO check session is the same ??
*/
	printf("[DMR event] AVTransport SetAVTransportURI [%s] at %ld\n", uri, time(NULL));
	unsigned int i = 0;
	int duration = 0;

#ifdef PPTV
	if( (!strncmp(uri,"pptv://",strlen("pptv://"))) && (DMR_StateGet_PlayState(instance) != DMR_PS_Stopped) ){
		RTKMR_AVTransport_Stop(instance,session);
	}
#endif

	SetDMRBlock();
	statusFlag = 0;
    SET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);

    if( checkDMRIsInternalCommand() ) {
        RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
		SetDMRUnBlock();
		RETURN_ERROR(402);
	}

	if( m_pDMR_MicroStack != instance )
	{
		printf("[DMR event] AVTransport SetAVTransportURI\n\tInvalid instanceID\n");
		ALOGE("[DMR event] m_pDMR_MicroStack = %d \n instance = %d\n",m_pDMR_MicroStack,instance);
        	RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
		SetDMRUnBlock();
		RETURN_ERROR(718);// Invalid InstanceID //return DMR_ERROR_BADTHIS;
	}

	if( uri == NULL || strlen(uri) == 0 || strcmp(uri, "")==0)
	{
		printf("[DMR event] AVTransport SetAVTransportURI\n\tNULL uri\n");
		//TODO, this should be re-check
		m_PlayState = DMR_PS_NoMedia;	// <--- this is strange, UPnP define only initialization state set to DMR_PS_NoMedia state...., but DLNA CTT DMR, MM part expected set to DMR_PS_NoMedia
		m_SETAVType = DMR_SUBR_NOTHING;
		m_mediaType = MEDIATYPE_None;
		FREE_MEMORY(m_pDMR_mediaProtocolInfo);
		FREE_MEMORY(m_pDMR_mediaMimeTypeProtocol);
		FREE_MEMORY(m_pSetAVURI);
		DELETE_OBJECT(m_psubRenderer);
		DELETE_OBJECT(m_pMediaType);
        	DELETE_CDS_OBJECT(m_pMediaMetadata);
		if( m_ppMediaTrackURI != NULL )
		{
			for( i = 0; i < m_NumberOfTracks; i++)
			{
				if( m_ppMediaTrackURI[i] != NULL )
				{
					free(m_ppMediaTrackURI[i]);
					m_ppMediaTrackURI[i] = NULL;
				}
			}
			delete [] m_ppMediaTrackURI;
			m_ppMediaTrackURI = NULL;
		}

		if( m_ppProtocolInfo != NULL ) 
		{
			unsigned int i = 0;
			for( i = 0; i < m_NumberOfTracks; i++)
			{
				if( m_ppProtocolInfo[i] != NULL )
				{
					free(m_ppProtocolInfo[i]);
					m_ppProtocolInfo[i] = NULL;
				}
			}
			delete [] m_ppProtocolInfo;
			m_ppProtocolInfo = NULL;
		}
			
		DMR_StateChange_AVTransportURI(instance, uri);
		DMR_StateChange_AVTransportURIMetaData(instance, data);
		DMR_StateChange_TransportPlayState(instance, m_PlayState);
		DMR_StateChange_NumberOfTracks(instance, 0);
		DMR_StateChange_CurrentTrack(instance, 0);
		DMR_StateChange_CurrentTrackURI(instance, NULL);
		DMR_StateChange_CurrentMediaDuration(instance, duration);
		DMR_StateChange_CurrentTrackDuration(instance, duration);
		DMR_StateChange_CurrentTransportActions(instance, DMR_ATS_Reset);

		RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
		SetDMRUnBlock();

		if(UpdateMediaInfo != NULL)
			UpdateMediaInfo(0, NULL);

		return DMR_ERROR_OK;
		//goto parse_done;
		//RETURN_ERROR(402);
		//RETURN_ERROR(DMR_ERROR_BADURI);
	}


	//reset something
	m_SETAVType = DMR_SUBR_NOTHING;
	m_mediaType = MEDIATYPE_None;
	FREE_MEMORY(m_pDMR_mediaProtocolInfo);
	FREE_MEMORY(m_pDMR_mediaMimeTypeProtocol);
	FREE_MEMORY(m_pSetAVURI);
	DELETE_OBJECT(m_psubRenderer);
	DELETE_OBJECT(m_pMediaType);
	DELETE_CDS_OBJECT(m_pMediaMetadata);

	if( m_ppMediaTrackURI != NULL )
	{
		for( i = 0; i < m_NumberOfTracks; i++)
		{
			if( m_ppMediaTrackURI[i] != NULL )
			{
				free(m_ppMediaTrackURI[i]);
				m_ppMediaTrackURI[i] = NULL;
			}
		}
		delete [] m_ppMediaTrackURI;
		m_ppMediaTrackURI = NULL;
	}
	m_CurrentTrack = -1;
	if( m_ppProtocolInfo != NULL ) 
	{
		unsigned int i = 0;
		for( i = 0; i < m_NumberOfTracks; i++)
		{
			if( m_ppProtocolInfo[i] != NULL )
			{
				free(m_ppProtocolInfo[i]);
				m_ppProtocolInfo[i] = NULL;
			}
		}
		delete [] m_ppProtocolInfo;
		m_ppProtocolInfo = NULL;
	}

	nChannel = 1;
	nSampleRate = 44100;
	nBitPerSample = 16;
//	printf("m_pDMR_MicroStack :%x\t dmr instance:%x\n", m_pDMR_MicroStack , instance);

	// a. Get the renderer type
	// b. Get the AVProtocolInfo
	if( data != NULL)	// just fetch the info
	{
		m_SETAVType = RendererType(data);
		if(data->Res){
			nChannel = data->Res->NrAudioChannels;
			nSampleRate = data->Res->SampleFrequency;
			nBitPerSample = data->Res->BitsPerSample;
		}
	}
	
	#ifdef DLNADMRCTT
	g_bplaycontainer = 0;
	#endif
	if( m_SETAVType == DMR_SUBR_NOTHING )	// while (data==NULL), parsing the file extension
	{
		printf("[DMR event] the metadata input is null\n");
		#ifdef DLNADMRCTT
		if(!strncmp(uri,"dlna-playcontainer://",strlen("dlna-playcontainer://")))
		{
				printf("dlna-playcontainer uri\n");
				g_bplaycontainer = 1;
				m_SETAVType = DMR_SUBR_PLAYLIST;
				#ifdef ANDROID_PLATFORM
				SetDMRUnBlock();
				RETURN_ERROR(714);
				#endif
		}
		else
		#endif

		{
			char *extension = strrchr(uri, '.');

			if( extension == NULL )
			{
                		RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
				SetDMRUnBlock();
				//RETURN_ERROR(DMR_ERROR_BADURI)
				RETURN_ERROR(714);
			}

			m_SETAVType = RendererType(extension);
		}
	}
	else if(m_mediaType == MEDIATYPE_None){
			char *extension = strrchr(uri, '.');
			if((extension != NULL) && !strcmp(extension,".pcm"))
				m_mediaType = MEDIASUBTYPE_PCM;
			
	}

	#ifdef PPTV
		if(!strncmp(uri,"pptv://",strlen("pptv://")))
		{
				m_SETAVType = DMR_SUBR_VIDEO;
				m_mediaType = MEDIATYPE_PPTV;
		}
	#endif

	

	if( QueryForConnection == NULL || QueryForConnection(0, NULL)!= S_OK)
	{
        	RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
		SetDMRUnBlock();
		RETURN_ERROR(602);
	}
	
	// new renderer by renderer type
	// and related settings: NumberOfTracsks, m_CurrentTrack, m_ppMediaTrackURI[][], m_pMediatype[]
	// loadMedia for setting
	switch( m_SETAVType )
	{
	#ifdef ANDROID_PLATFORM
		 case DMR_SUBR_AUDIO:
			m_psubRenderer = new AndroidRenderer(DMR_SUBR_AUDIO);
			break;
		case DMR_SUBR_VIDEO:
			m_psubRenderer = new AndroidRenderer(DMR_SUBR_VIDEO);
			break;
		case DMR_SUBR_IMAGE:
			m_psubRenderer = new AndroidRenderer(DMR_SUBR_IMAGE);
			break;
		case DMR_SUBR_TEXT:
          		 RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
			SetDMRUnBlock();
			RETURN_ERROR(602);
		case DMR_SUBR_PLAYLIST:
			m_psubRenderer = new playlistRenderer();
			break;
	#else
		case DMR_SUBR_AUDIO:
			m_psubRenderer = new avRenderer(m_pPlayback, DMR_SUBR_AUDIO);
			break;
		case DMR_SUBR_VIDEO:
			m_psubRenderer = new avRenderer(m_pPlayback, DMR_SUBR_VIDEO);
			break;
		case DMR_SUBR_IMAGE:
			m_psubRenderer = new imageRenderer(m_pPlayback );
			break;
		case DMR_SUBR_TEXT:
			printf("[DMR event] this is text file\n");
            		RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
			SetDMRUnBlock();
			RETURN_ERROR(602);//"Optional Action Not Implemented"
		case DMR_SUBR_PLAYLIST:
			printf("[DMR event] this is a playlist...\n");
			m_psubRenderer = new playlistRenderer(m_pPlayback );
			break;
	#endif
		default:{
			printf("[DMR event] this is nothing, or not support format.....\n");
            		RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
			SetDMRUnBlock();

			//RETURN_ERROR(716);// NotExist
			RETURN_ERROR(714);//"Illegal MIME-Type"
			break;
			}
	}

#ifdef ENABLE_DTCP_IP
		if(data && data->Res && data->Res->ProtocolInfo && m_psubRenderer)
		{
			bool bDTCP = false;
			if(strstr(data->Res->ProtocolInfo, "DTCP1HOST") != NULL)
				bDTCP = true;
			else
			{
				char *p = strstr(uri, "?");
				if(p && strstr(p, "CONTENTPROTECTIONTYPE=DTCP1") != NULL)
					bDTCP = true;
			}

			if(bDTCP)
			{
				char *pinfo_tmp, *pEnd, *pinfo;
				int len, sub=0;

				pinfo = data->Res->ProtocolInfo;
				len = strlen(pinfo);

				pinfo_tmp = strdup(pinfo);

				if(pinfo_tmp)
				{
					pEnd = pinfo_tmp+len;

					while(pEnd > pinfo_tmp && *pEnd != ':')
					{
						pEnd--;
						sub++;
					}
					if(sub>=2)
					{
						*(pEnd+1) = '*';
						*(pEnd+2) = 0;
						len -= sub-2;
						pinfo = pinfo_tmp;
						printf("protocolinfo = %s\n", pinfo);
					}
				}
				m_psubRenderer->SetProtocolInfo(pinfo);

				if(pinfo_tmp)
					free(pinfo_tmp);
			}
		}
#endif /* ENABLE_DTCP_IP */

	m_bIsDMRSupportPause = false;
	m_bIsDMRSupportSeek = true;
	DLNADMR_bSupportSF = 0;
	DLNADMR_bSupportTrickmode = 1;
	DLNADMR_bSupportFBFFSBSF = 1;
	
	if(data && data->Res && data->Res->ProtocolInfo)
	{
		char *flags = NULL;
		if((flags = strstr(data->Res->ProtocolInfo, "DLNA.ORG_FLAGS")) != NULL)
		{
			flags += 15; // 15 = strlen("DLNA.ORG_FLAGS=")
			if(GetBitVal(flags, 21) == false)
				m_bIsDMRSupportStalling = false;
			else
				m_bIsDMRSupportStalling = true;
		}
		else
			m_bIsDMRSupportStalling = false;

		if(m_bIsDMRSupportStalling && IsMPEG_TS_JP_T(data->Res->ProtocolInfo))
		{
			m_bIsDMRSupportSeek = false;
			DLNADMR_bSupportTrickmode = 0;
			DLNADMR_bSupportSF = 1;
		}
		
		if((flags = strstr(data->Res->ProtocolInfo, "DLNA.ORG_OP")) != NULL)
		{
			flags += 12; // 12 = strlen("DLNA.ORG_OP=")
			if(flags[0] == '1' || flags[1] == '1')
				m_bIsDMRSupportPause = true;
			else
				m_bIsDMRSupportSeek = false;
			if((flags[0] == '0' && flags[1] == '1')&& IsMPEG_TS_JP_T(data->Res->ProtocolInfo))
			{
				m_bIsDMRSupportSeek = false;
				m_bIsDMRSupportPause = false;
				DLNADMR_bSupportTrickmode = 0;
				DLNADMR_bSupportFBFFSBSF = 0;
			}
			if(flags[0] == '1')
			{
				m_bIsDMRSupportSeek = true;
				DLNADMR_bSupportTrickmode = 1;
		}
		}else
		{
			m_bIsDMRSupportSeek = false;
		}
		
		// for DTCP-IP cleartext byte seek
		if((flags = strstr(data->Res->ProtocolInfo, "DLNA.ORG_FLAGS")) != NULL)
		{
			flags += 15; // 15 = strlen("DLNA.ORG_FLAGS=")
			if((GetBitVal(flags, 16) == 1) && 
			((GetBitVal(flags, 15) == 1) ||((GetBitVal(flags, 14) == 1) && (GetBitVal(flags, 23) != 1))))
			{
				m_bIsDMRSupportPause = true;
				m_bIsDMRSupportSeek = true;
	}
		}
		if(!m_bIsDMRSupportStalling && IsMPEG_TS_JP_T(data->Res->ProtocolInfo))
		{
			DLNADMR_bSupportTrickmode = 0;
			DLNADMR_bSupportSF = 0;
		}
	}
	isSupport = true;
	if(data && data->Res && data->Res->ProtocolInfo)
	{
		isSupport = IsSurportProtocolInfo(data->Res->ProtocolInfo);
	}
	ALOGE("isSupport = %d",isSupport);

	osal_MutexLock(m_LoadMediaMutex);
	int hr = m_psubRenderer->preParse(uri, &m_NumberOfTracks, &m_ppMediaTrackURI, &m_pMediaType, &m_totalTime,&m_ppProtocolInfo);
	osal_MutexUnlock(m_LoadMediaMutex);
	if(hr == S_OK)
	{
		printf("preparse succeed\n");

		//unsigned int i = 0;
		m_CurrentTrack = 1;
		m_pSetAVURI = strdup(uri);
		
		// for Playlist....
		// fill up MediaType
		if(m_SETAVType == DMR_SUBR_PLAYLIST)
		{
			//m_pSetAVURI = strdup(m_ppMediaTrackURI[m_CurrentTrack-1]);
			#if 1
			for( i = 0; i < m_NumberOfTracks; i++)
			{
				#ifdef DLNADMRCTT
				if(!strncmp(m_ppMediaTrackURI[i],"dlna-playsingle://",strlen("dlna-playsingle://")))
				{
					char iid[128];
					char uuid[64];
					char*	cur = NULL;
					char*	next= NULL;
					unsigned long ret;
					printf("Starting playsingle URL convert\n");

			        if( (cur = strstr( m_ppMediaTrackURI[i], "uuid:"))!= NULL )
			        {
			        	memset(uuid,0,64);
			            cur += 5;
			            strncpy(uuid,cur,36);
			        }	
					else if( (cur = strstr( m_ppMediaTrackURI[i], "uuid%3A"))!= NULL )
			        {
			        	memset(uuid,0,64);
			            cur += 7;
			            strncpy(uuid,cur,36);
			        }
					else if( (cur = strstr( m_ppMediaTrackURI[i], "uuid%3a"))!= NULL )
			        {
			        	memset(uuid,0,64);
			            cur += 7;
			            strncpy(uuid,cur,36);
			        }
	
					cur = NULL;
					next = NULL;
					if( (cur = strstr(m_ppMediaTrackURI[i], "iid="))!= NULL )
			        {
			        	memset(iid,0,128);
			            cur += 4;
						if( (next = strchr( cur, '&'))!= NULL )
			        	{
			            	strncpy(iid,cur,next-cur);
			        	}	
						else
							strcpy(iid,cur);
				   }
					else{
                        			RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
						SetDMRUnBlock();
						RETURN_ERROR(704);
						}

					printf("xxxxxxxxxxxxxxxxx uuid=%s iid=%s\n",uuid,iid);
					RTK_DLNA_DMP *m_pUpnpDmp = RTK_DLNA_DMP_Singleton::GetInstance();
					if(m_pUpnpDmp)m_pUpnpDmp->unsetMediaServer();
					if(m_pUpnpDmp)m_pUpnpDmp->Start();
					sleep(5);

					if(m_pUpnpDmp && !m_pUpnpDmp->setMediaServerByUDN(uuid))
					{
						printf("DMP setMediaServerByUDN Error\n");
                        			RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
						SetDMRUnBlock();
						if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
						RETURN_ERROR(704);
					}
		
					if(!m_pUpnpDmp->UPnPServiceBrowse("0"))
					{
						printf("DMP UPnPServiceBrowse iid Error\n");
						RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
                       			SetDMRUnBlock();
						if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
						RETURN_ERROR(704);
					}	

					//get all item
					int totalcount=m_pUpnpDmp->MediaContainerObjectSizes();
					printf("DMP container totalcount=%d\n",totalcount);

					struct UPnPObjInfo *pInfo = (struct UPnPObjInfo *)malloc(sizeof(struct UPnPObjInfo));
		                	if(!pInfo)
		                	{
		                        	RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
		                		SetDMRUnBlock();
						if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
							RETURN_ERROR(704);
		                	}
					for(int i=0;i<totalcount;i++)
					{
						memset(pInfo, 0, sizeof(struct UPnPObjInfo));
						m_pUpnpDmp->queryContainerObjInfoByIndex(i, pInfo);
						if(strcmp(pInfo->pUniqueCharID,g_playcontaiercid))
							break;
					}
					
					printf("DMP get container id=%s\n",pInfo->pUniqueCharID);

					if(!m_pUpnpDmp->UPnPServiceBrowse(pInfo->pUniqueCharID))
					{
						printf("DMP UPnPServiceBrowse id Error\n");
                        			RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
						SetDMRUnBlock();
						if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
						RETURN_ERROR(704);
					}	
					
					free(pInfo);
					
					if(m_pUpnpDmp->queryResourceByID(iid,  UPNP_DMP_RES_URI, &ret))
					{
						free(m_ppMediaTrackURI[i]);
						m_ppMediaTrackURI[i] = strdup((char*)ret);
						
					}
					else
					{
                        			RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
						SetDMRUnBlock();
						if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
						RETURN_ERROR(704);
					}	
					if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
					
				}
				#endif
				
				if(m_ppProtocolInfo)
				m_pMediaType[i] = RendererType(m_ppProtocolInfo[i],1);
				if((m_pMediaType[i] == DMR_SUBR_NOTHING) ||(m_pMediaType[i] == DMR_SUBR_UNKNOWN))
				{
					char *extension = strrchr(m_ppMediaTrackURI[i], '.');
					if( extension != NULL )
						m_pMediaType[i] = RendererType(extension);
				}
			}
			if (m_NumberOfTracks > 0) // Set the current play item to fist one
                        {
				if(m_ppProtocolInfo)
					m_pMediaType[0] = RendererType(m_ppProtocolInfo[0], 1);
				if((m_pMediaType[0] == DMR_SUBR_NOTHING) ||(m_pMediaType[0] == DMR_SUBR_UNKNOWN))
				{
					char *extension = strrchr(m_ppMediaTrackURI[i], '.');
					if( extension != NULL )
						m_pMediaType[0] = RendererType(extension);
				}
                        }
			#endif

			
			
		}

		
		
		//TODO, search a appropriate track index
		if( m_SETAVType != m_pMediaType[m_CurrentTrack-1] )
		{
			DELETE_OBJECT(m_psubRenderer);
		#ifdef ANDROID_PLATFORM
			 if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_AUDIO )
				m_psubRenderer = new AndroidRenderer(DMR_SUBR_AUDIO);
			else if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_VIDEO )
				m_psubRenderer = new AndroidRenderer(DMR_SUBR_VIDEO);
			else if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_IMAGE )
				m_psubRenderer = new AndroidRenderer(DMR_SUBR_IMAGE);
		#else
			if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_AUDIO )
				m_psubRenderer = new avRenderer(m_pPlayback, DMR_SUBR_AUDIO);
			else
			if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_VIDEO )
				m_psubRenderer = new avRenderer(m_pPlayback, DMR_SUBR_VIDEO);
			else
			if( m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_IMAGE )
				m_psubRenderer = new imageRenderer(m_pPlayback );
		#endif
			else{
				printf("playlist DMR_SUBR_Nothing\n");
                		RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
				SetDMRUnBlock();
				RETURN_ERROR(704);
			}

//			if( m_psubRenderer != NULL )
//				m_psubRenderer->loadMedia(m_ppMediaTrackURI[m_CurrentTrack-1]);
		}
		
		#ifdef DLNADMRCTT
		if((m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_AUDIO)||
		   (m_pMediaType[m_CurrentTrack-1] == DMR_SUBR_VIDEO))
			DLNADMR_bPlayingAV = 1;
		else
			DLNADMR_bPlayingAV = 0;
		#endif

	}
	else
	{
		printf("preparse failed\n");
        	RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
		SetDMRUnBlock();
		RETURN_ERROR(704);
	}

	if( PrepareForConnection == NULL || PrepareForConnection(0, NULL)!= S_OK)
	{
        	RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
		SetDMRUnBlock();
		if(UpdateMediaInfo != NULL)
			UpdateMediaInfo(0, NULL);

		RETURN_ERROR(602);
	}
	
	#ifndef DLNADMRCTT
	if( m_PlayState == DMR_PS_NoMedia )
		m_PlayState = DMR_PS_Stopped;
	else
	if( m_PlayState == DMR_PS_Playing)
		m_PlayState = DMR_PS_Transitioning;
	#else
		m_PlayState = DMR_PS_Stopped;	
	#endif

	DMR_StateChange_AVTransportURI(instance, uri);
	DMR_StateChange_AVTransportURIMetaData(instance, data);
	DMR_StateChange_CurrentTrackMetaData(instance, m_pMediaMetadata);
	DMR_StateChange_TransportPlayState(instance, m_PlayState);
	
	DMR_StateChange_NumberOfTracks(instance, m_NumberOfTracks);
	
	#ifdef DLNADMRCTT
	if(g_bplaycontainer)
		DMR_StateChange_CurrentTrack(instance, m_CurrentTrack-1);//for DCTT 7.3.100.3
	else
	#endif
		DMR_StateChange_CurrentTrack(instance, m_CurrentTrack);

	DMR_StateChange_CurrentTrackURI(instance, m_ppMediaTrackURI[m_CurrentTrack-1]);
	if( data != NULL && data->Res != NULL && data->Res->Duration >= 0 )
		duration = data->Res->Duration*1000;
    else
        printf("[DMR event] duration does not exist !!!\n");


	printf("m_PlayState=%d m_CurrentTrack=%d NumberofTrack=%d trackURI=%s duration=%d totaltime=%d\n",m_PlayState,m_CurrentTrack,m_NumberOfTracks,m_ppMediaTrackURI[m_CurrentTrack-1],duration,m_totalTime);

	if(duration <= 0 && m_totalTime > 0 )
        	duration = m_totalTime;
	
	if(duration != m_totalTime )
        	m_totalTime = duration;
	
	DMR_StateChange_CurrentMediaDuration(instance, duration);
	DMR_StateChange_CurrentTrackDuration(instance, duration);
	
	unsigned short allowedActions = DMR_ATS_Play|DMR_ATS_Stop|DMR_ATS_Pause|DMR_ATS_Next|DMR_ATS_Previous|DMR_ATS_Seek;
	 if(!m_bIsDMRSupportPause)
	 {
		 allowedActions &= ~DMR_ATS_Pause;
	 }
	 if(!m_bIsDMRSupportSeek)
	 {
		 allowedActions &= ~DMR_ATS_Seek;
	 }

	DMR_StateChange_CurrentTransportActions(instance, allowedActions);
	if(UpdateMediaInfo != NULL) UpdateMediaInfo(0, NULL);

    	RESET_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
	SetDMRUnBlock();

	printf("[DMR event] AVTransport SetAVTransportURI DONE at %ld\n", time(NULL));

	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_SetNextAVTransportURI(DMR instance, void *session, char *uri, struct CdsObject *data)
{
	printf("[DMR event] AVTransport SetNextAVTransportURI\n");

	if( m_pDMR_MicroStack != instance )
	{
		printf("[DMR event] AVTransport SetNextAVTransportURI\n\tInvalid instanceID\n");
		return 718;
	}
	
	if( uri != NULL && strcmp(uri, "")!=0)
	{
		printf("uri=%s\n", uri);
		char *extension = strrchr(uri, '.');
		if( extension == NULL )
		{
		    return 714;
		}
		SUBRENDERER_TYPE type = RendererType(extension);
		if(type == DMR_SUBR_NOTHING)
			return 714;
			
		DMR_StateChange_NextAVTransportURI(instance, uri);
		DMR_StateChange_NextAVTransportURIMetaData(instance, data);
		
		DELETE_CDS_OBJECT(m_pNextURIMetadata);
		m_pNextURIMetadata = data;
	}
	return DMR_ERROR_OK;
}



int RTK_DLNA_DMR::RTKMR_ConnectionManager_GetAVProtocolInfo(DMR instance, void *session, char **protocolInfo)
{
	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_mediaProtocolInfo != NULL)
//		protocolInfo = &m_pDMR_mediaProtocolInfo;
		*protocolInfo = strdup(m_pDMR_mediaProtocolInfo);
//	if( m_pDMR_ProtocolInfo != NULL )//BuildProtocolInfo(ProtocolInfoList);
//		protocolInfo = &m_pDMR_ProtocolInfo;
//	*protocolInfo = BuildProtocolInfo(ProtocolInfoList);
	return DMR_ERROR_OK;
//	return 0;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_Stop(DMR instance, void *session)
{
	printf("[DMR event] AVTransport Stop...\n");

	if( m_pDMR_MicroStack != instance )
		RETURN_ERROR(718);// Invalid InstanceID

	SetDMRBlock();

	if( checkDMRIsInternalCommand() )
	{
		RETURN_ERROR(402);
	}

	if( m_psubRenderer ){
		m_psubRenderer->Stop();
	}
	else
	{
		SetDMRUnBlock();
		RETURN_ERROR(701);//"Transition Not Available"
	}
	
	m_PlayState = DMR_PS_Stopped;
	DMR_StateChange_TransportPlayState(instance, m_PlayState);
	if( ShowStatus != NULL )
		ShowStatus((int)m_PlayState, NULL);

	SetDMRUnBlock();
	
	if(m_pSetAVURI)
	{
		free(m_pSetAVURI);
		m_pSetAVURI = NULL;
	}

	#ifndef DLNADMRCTT
	DMR_StateChange_AbsoluteTimePosition(instance,0xffffffff);
	DMR_StateChange_RelativeTimePosition(instance,0);
	#else
	DMR_StateChange_AbsoluteTimePosition(instance,0);
    	DMR_StateChange_RelativeTimePosition(instance,0);
	#endif
	
	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_Play(DMR instance, void *session, char *playSpeed)
{
/**
	0. check the dmr instance
	 . check the PlayState
	1. translate the playSpeed
	2  check if there is AVTransportURI ?
	3. Run
	4. Update the CurrentTransportAction, and PlayState
*/
	printf("\t\t[DMR event] AVTransport Play speed=%s\n",playSpeed);

	int speed = 0;
	float f_speed = 0;
	if( m_pDMR_MicroStack != instance )
		RETURN_ERROR(718);// Invalid InstanceID

	if( (DMR_StateGet_PlayState(instance) != DMR_PS_Stopped) &&
		(DMR_StateGet_PlayState(instance) != DMR_PS_Paused) &&
		(DMR_StateGet_PlayState(instance) != DMR_PS_Transitioning) &&
		(DMR_StateGet_PlayState(instance) != DMR_PS_Playing)
	  )
		RETURN_ERROR(701);//"Transition Not Available"
	
	SetDMRBlock();

	if( checkDMRIsInternalCommand() )
		RETURN_ERROR(402);

	if( m_psubRenderer == NULL )
	{
	  do {
			if (m_CurrentTrack > 0 && m_ppMediaTrackURI) {
				char * uri = strdup(m_ppMediaTrackURI[m_CurrentTrack-1]);
				RTKMR_AVTransport_Stop(m_pDMR_MicroStack,NULL);
				if(uri && *uri != '\0' && RTKMR_AVTransport_SetAVTransportURI(m_pDMR_MicroStack, NULL, uri, NULL) == DMR_ERROR_OK)
					break;
				}
		SetDMRUnBlock();
		RETURN_ERROR(702);// "No Contents"
	     } while(1);
	}

	if( playSpeed != NULL )
	{
		f_speed = Stringtof(playSpeed);
		if( (f_speed <= 32.0) && (f_speed >= 0.0625) )
			speed = (int)(f_speed*256);
		else
		if( (f_speed >= -32.0) && (f_speed <= -0.0625) )
			speed = (int)(f_speed*256);
		else
		{
			SetDMRUnBlock();
			RETURN_ERROR(717);//"Play Speed Not Supported"
		}
	}
	else
		speed = m_Speed*256;


	if(m_PlayState == DMR_PS_Playing)   //for DIXIM DMC 
	{
		/*printf("[DMR event] Come in Pause()\n");
		if((speed == 256)&&(lastspeed == speed))
		{
			m_psubRenderer->Pause(true);
			m_PlayState = DMR_PS_Paused;
			DMR_StateChange_TransportPlayState(instance, m_PlayState);
			if( ShowStatus != NULL )
				ShowStatus((int)m_PlayState, NULL);
			SetDMRUnBlock();
			return DMR_ERROR_OK;
		}else{*/
			m_psubRenderer->SetRate(speed);
			lastspeed = speed;
			if( playSpeed ){
				DMR_StateChange_TransportPlaySpeed(instance, playSpeed);
			}
			SetDMRUnBlock();
			return DMR_ERROR_OK;
		//}
		
		//RETURN_ERROR(716);//"Resource Not Found"
	}else if(m_PlayState == DMR_PS_Paused)// && m_psubRenderer->Pause() != S_OK)
	{
		printf("[DMR event] Come in de-Pause()\n");
		m_psubRenderer->Pause(false);
		m_psubRenderer->SetRate(speed);
		//RETURN_ERROR(716);//"Resource Not Found"
	}else
	if( m_psubRenderer->Play(m_ppMediaTrackURI[m_CurrentTrack-1], speed) != S_OK)
//	if( m_psubRenderer->Play(speed) != S_OK)
	{
		#ifdef DLNADMRCTT
		// for LPTT 7.3.10.1
		#ifdef ENABLE_DTCP_IP
		//if(bDtcpIpContent)
		{
			struct stat st;
			if(stat("/tmp/http.error.403",&st) == 0)
			{
				SetDMRUnBlock();
				RETURN_ERROR(722);
			}
		}
		#endif /* ENABLE_DTCP_IP */
		#else
		printf("Play Return Error\n");
		SetDMRUnBlock();
		RETURN_ERROR(716);//"Resource Not Found"
		#endif
	}
	lastspeed = speed;
	if( playSpeed )
		DMR_StateChange_TransportPlaySpeed(instance, playSpeed);
	m_PlayState = DMR_PS_Playing;
	//DMR_StateChange_TransportPlayState(instance, DMR_PS_Playing);
	DMR_StateChange_TransportPlayState(instance, m_PlayState);
	
	if(m_bIsDMRSupportPause){//to allow pause action for GoldenDMC
		unsigned short Allowedaction = DMR_Internal_GetCurrentTransportActions(instance);
		Allowedaction |= Allowedaction | DMR_ATS_Pause;
		DMR_StateChange_CurrentTransportActions(instance, Allowedaction);
	}
	
	#ifdef DLNADMRCTT
	if(g_bplaycontainer)
		DMR_StateChange_CurrentTrack(instance, m_CurrentTrack-1); //for DCTT 7.3.100.3
	else
	#endif
		DMR_StateChange_CurrentTrack(instance, m_CurrentTrack);
	DMR_StateChange_CurrentTrackURI(instance, m_ppMediaTrackURI[m_CurrentTrack-1]);
	DMR_StateChange_CurrentTrackMetaData(instance, m_pMediaMetadata); //?? how to generate the MetaData??

	if( ShowStatus != NULL )
		ShowStatus((int)DMR_PS_Playing, NULL);
	SetDMRUnBlock();
	return DMR_ERROR_OK;
}

void RTK_DLNA_DMR::RTKMR_AVTransport_PlayNext(DMR instance, void *session)
{
	unsigned int nextTrack = 0;

	#ifdef PPTV
	if( ( MEDIATYPE_PPTV == m_mediaType ) && (DMR_StateGet_PlayState(instance) != DMR_PS_Stopped) ){
		printf("%s %d \n",__FILE__,__LINE__);

		RTKMR_AVTransport_Stop(instance,session);
	}
	#endif


	if( checkDMRIsInternalCommand() )
		return;

	switch( m_PlayMode )
	{
		case DMR_MPM_Normal:
			nextTrack = m_CurrentTrack+1;
			break;
		case DMR_MPM_Repeat_All:
			nextTrack = m_CurrentTrack+1;
			if( nextTrack > m_NumberOfTracks )
			nextTrack = 1;
			break;
		case DMR_MPM_Repeat_One:
			nextTrack = m_CurrentTrack;
			break;
		case DMR_MPM_Random:
		case DMR_MPM_Shuffle:
		case DMR_MPM_Intro:
			//TODO
		case DMR_MPM_Direct_One:
		default:
			nextTrack = m_CurrentTrack+1;
			break;
	}
	
	printf("PlayNext nextTrack=%d\n",nextTrack);

	if( nextTrack > m_NumberOfTracks )
		RTKMR_AVTransport_Stop(instance, session);
	else
	{
		// determinate if subRenderer_Type is the same?!
		// change the subRenderer
		//TODO, search a appropriate track index
		//deadlock bug using a self terminated thread to do it
		if(m_PlayMode != DMR_MPM_Repeat_One)
		RTKMR_AVTransport_Stop(instance, session);
		
		m_CurrentTrack = nextTrack;
		
		pthread_t tid;
		pthread_attr_t  attr;
		pthread_attr_init(&attr);
		pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
		pthread_create(&tid, &attr, PlayTrackThread, (void*)NULL);
		pthread_attr_destroy(&attr);
		
		printf("PlayNext nextTracktype=%d\n",m_pMediaType[nextTrack-1]);
		
	}
}

int RTK_DLNA_DMR::RTKMR_AVTransport_SeekTrack(DMR instance, void *session, unsigned int trackIndex)
{
	printf("[DMR event] %s trackIndex:%d \n", __func__, trackIndex);
// think of if there is some error, should "RTKMR_AVTransport_Stop()" ??!!!
//	RETURN_ERROR(710);// Seek mode not supported


	
	int speed = 0;
	if( m_pDMR_MicroStack != instance )
		RETURN_ERROR(718);// Invalid InstanceID

	SetDMRBlock();

	if( checkDMRIsInternalCommand() )
		RETURN_ERROR(402);

	if( m_psubRenderer == NULL )
	{
		SetDMRUnBlock();
		RETURN_ERROR(702);// "No Contents"
	}

	if( m_NumberOfTracks == 1 )
	{
		if( trackIndex == 0 )		// fast forward
			speed = 2;
		else
		if( trackIndex == 1 )		// fast reverse
			speed = -2;
		else
		{
			SetDMRUnBlock();
			RETURN_ERROR(711);// Illegal seek target
		}
		if( S_OK == m_psubRenderer->SetRate(speed*256))
		{
			char strSpeed[8] = {0};
			m_Speed = speed;	// ?? or do not need to stored?
			snprintf(strSpeed, 8, "%d", m_Speed);
			DMR_StateChange_TransportPlaySpeed(instance, strSpeed);
		}
		else
		{
			SetDMRUnBlock();
			RETURN_ERROR(710);// Seek mode not supported
		}
		SetDMRUnBlock();
	}
	else
	{
		SetDMRUnBlock();
		if( trackIndex < 1 || trackIndex > m_NumberOfTracks )
			RETURN_ERROR(711);// Illegal seek target 
		//TODO, search a appropriate track index
		//mediaType may be not the same, check first, and find the appropriate next one
		m_CurrentTrack = trackIndex;
		/*
		DMR_StateChange_CurrentTrack(instance, m_CurrentTrack);
		DMR_StateChange_CurrentTrackURI(instance, m_ppMediaTrackURI[m_CurrentTrack-1]);
		if((DMR_StateGet_PlayState(instance) == DMR_PS_Paused) ||
		(DMR_StateGet_PlayState(instance) == DMR_PS_Playing))
		{
			RTKMR_AVTransport_Play(instance, session, NULL);
		}
		*/
		pthread_t tid;
		pthread_attr_t  attr;
		pthread_attr_init(&attr);
		pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
		pthread_create(&tid, &attr, PlayTrackThread, (void*)NULL);
		pthread_attr_destroy(&attr);		
	}

	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_SeekTrackPosition(DMR instance, void *session, long position)
{
	printf("[DMR event] %s position:%ld \n", __func__, position);
//	RETURN_ERROR(710);// Seek mode not supported
	int ret;
	if( m_pDMR_MicroStack != instance )
		RETURN_ERROR(718);// Invalid InstanceID

	SetDMRBlock();

	if( checkDMRIsInternalCommand() )
		RETURN_ERROR(402);

	if( m_psubRenderer == NULL )
	{
		SetDMRUnBlock();
		RETURN_ERROR(702);// "No Contents"
	}

	if( !m_bIsDMRSupportSeek )
	{
		SetDMRUnBlock();
		RETURN_ERROR(710);// "Seek mode not supported"
	}
	if(m_totalTime >0)
	{
	if( m_totalTime/1000 < position )
	{
		SetDMRUnBlock();
		RETURN_ERROR(711);// Illegal seek target
	}
	}

	
	//TODO: see if position is out of range
	if( m_NumberOfTracks == 1)
	{
		ret = m_psubRenderer->SeekMediaPosition(-1, position);
	}
	else
		ret = m_psubRenderer->SeekMediaPosition(m_CurrentTrack-1, position);

	SetDMRUnBlock();

	if(ret == S_OK)
		return DMR_ERROR_OK;
	
	printf("[DMR event] SeekTrackPosition error!!!\n");
	
	//RETURN_ERROR(710);	// Illegal seek target
	return 711;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_SeekMediaPosition(DMR instance, void *session, long position)
{

	printf("[DMR event] %s position:%ld \n", __func__, position);

/**
	TODO: This edition temporarily considers "1 title" !!!
*/

//	RETURN_ERROR(710);// Seek mode not supported

	int ret;
	if( m_pDMR_MicroStack != instance )
		RETURN_ERROR(718);// Invalid InstanceID

	SetDMRBlock();

	if( checkDMRIsInternalCommand() )
		RETURN_ERROR(402);

	if( m_psubRenderer == NULL )
	{
		SetDMRUnBlock();
		RETURN_ERROR(702);// "No Contents"
	}
	
	printf(">>>>>>>>>>>>>>>>>> seek position=%d\n",position);
	//TODO: see if position is out of range
	ret = m_psubRenderer->SeekMediaPosition(-1, position);
	SetDMRUnBlock();

	if(ret == S_OK)
		return DMR_ERROR_OK;

	printf("[DMR event] SeekMediaPosition error!!!\n");
	
	return 711;	// Illegal seek target
}

int RTK_DLNA_DMR::RTKMR_AVTransport_Next(DMR instance, void *session)
{
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		RETURN_ERROR(718);// Invalid InstanceID

	if( m_NumberOfTracks != 1 && m_CurrentTrack < m_NumberOfTracks)
		return RTKMR_AVTransport_SeekTrack(instance, session, m_CurrentTrack+1);
	
	if(m_PlayMode == DMR_MPM_Repeat_One)
			RTK_DLNA_DMR::RTKMR_AVTransport_PlayNext(instance, session);

	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_Previous(DMR instance, void *session)
{
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		RETURN_ERROR(718);// Invalid InstanceID

	if( m_NumberOfTracks != 1 && m_CurrentTrack > 1)
		return RTKMR_AVTransport_SeekTrack(instance, session, m_CurrentTrack-1);

	if(m_PlayMode == DMR_MPM_Repeat_One)
		RTK_DLNA_DMR::RTKMR_AVTransport_PlayNext(instance, session);	

	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_RenderingControl_SelectPreset(DMR instance, void *session, char *presetName)
{
#if 0
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		return 718;// Invalid InstanceID

	if( presetName != NULL && 
		( strncmp(presetName, "FactoryDefaults", strlen("FactoryDefaults")) ||
		  strncmp(presetName, "InstallationDefaults", strlen("InstallationDefaults")) ) )
	{
		// reset
		// brightness, contrast, 
		// Mute, Volume, VolumeDB, Loudness
		// there are not in our system:
		// sharpness, colorTemperature, HorizontalKeystone, VerticalKeystone
		SetupClass::GetInstance()->SetBrightness(video_default.brightness);
		DMR_StateChange_Brightness(instance, video_default.brightness);
		if( SetBrightness != NULL )
		{
			SetBrightness(0, (void*)&video_default.brightness);
		}

		SetupClass::GetInstance()->SetContrast(video_default.contrast);
		DMR_StateChange_Contrast(instance, video_default.contrast);
		if( SetContrast != NULL )
		{
			SetContrast (0, (void*)&video_default.contrast);
		}

		SetupClass::GetInstance()->SetVolume(misc_default.m_volume);
		DMR_StateChange_Volume(instance, (int)(misc_default.m_volume*100/MAX_VOLUME_NUMBER));
		SetAudioVolume(misc_default.m_volume);
	}
#endif
	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_Pause(DMR instance, void *session)
{
	
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		return 718;// Invalid InstanceID

	SetDMRBlock();

	if( checkDMRIsInternalCommand() )
		RETURN_ERROR(402);

	if( m_psubRenderer == NULL )
	{
		SetDMRUnBlock();
		RETURN_ERROR(702);// "No Contents"
	}

	//yunfeng_han
	if((m_PlayState == DMR_PS_Paused)||(m_PlayState == DMR_PS_Stopped) || (m_bIsDMRSupportPause == false))
	{
		SetDMRUnBlock();
		printf("m_playstate=%d\n",m_PlayState);
		return 701;//Transition Not Available
	}

	
	if( m_psubRenderer->Pause() == S_OK )
	{
		m_PlayState = DMR_PS_Paused;
		DMR_StateChange_TransportPlayState(instance, m_PlayState);
		if( ShowStatus != NULL )
			ShowStatus((int)m_PlayState, NULL);
		SetDMRUnBlock();
		return DMR_ERROR_OK;
	}

	SetDMRUnBlock();
	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_AVTransport_SetPlayMode(DMR instance, void *session, DMR_MediaPlayMode playMode)
{
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		return 718;// Invalid InstanceID

	m_PlayMode = playMode;
	switch( playMode )
	{
		case DMR_MPM_Normal:
		case DMR_MPM_Repeat_One:
		case DMR_MPM_Repeat_All:
		case DMR_MPM_Random:
		case DMR_MPM_Shuffle:
		case DMR_MPM_Intro:
			break;
		case DMR_MPM_Direct_One:
		default:
			m_PlayMode = DMR_MPM_Normal;
			DMR_StateChange_CurrentPlayMode(instance, m_PlayMode);
			return 712;// play mode not supported
	}
	DMR_StateChange_CurrentPlayMode(instance, m_PlayMode);
	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_RenderingControl_SetVolume(DMR instance, void *session, unsigned char volume)
{
#ifdef ANDROID_PLATFORM
        int vol = (int)volume;
	if((vol >VOLUME_MAX) ||(vol<0))
		return 601;
	if( ShowVolumeStatus != NULL )
	{
		ShowVolumeStatus(DLNA_DMR_VOL_NUM,(void*)&vol);
		DMR_StateChange_Volume(instance, volume);
		DMR_StateChange_Mute(instance, false);
	}
#else
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	int vol = (int)(volume*MAX_VOLUME_NUMBER/100);
	if( m_pDMR_MicroStack != instance )
		return 718;// Invalid InstanceID

	if( (vol > MAX_VOLUME_NUMBER) || (vol < MIN_VOLUME_NUMBER) )
		return 601;// Argument Value Out of Range

	if( ShowVolumeStatus != NULL )
	{
		ShowVolumeStatus(DLNA_DMR_VOL_NUM,(void*)&vol);
		DMR_StateChange_Volume(instance, volume);
		DMR_StateChange_Mute(instance, false);
	}
	else{
	(SetupClass::GetInstance())->SetVolume((int)vol);
	DMR_StateChange_Volume(instance, volume);
	SetAudioVolume((int)vol);
	}
#endif
	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_RenderingControl_SetMute(DMR instance, void *session, bool mute)
{
#ifdef ANDROID_PLATFORM
        int Mute = (int)mute;
	DMR_StateChange_Mute(instance, mute);
	if( ShowVolumeStatus != NULL )
		SetMute(DLNA_DMR_VOL_MUTE,(void*)&Mute);
#else
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		return 718;// Invalid InstanceID

	DMR_StateChange_Mute(instance, mute);
	if( ShowVolumeStatus != NULL )
		ShowVolumeStatus(DLNA_DMR_VOL_MUTE,(void*)&mute);
	else
#if defined(IS_TV_CHIP) 
	SetAudioMute(AUDIO_MUTE_ID_AP, (bool)mute, AUDIO_MUTE_PORT_SPEAKER);
#else
	SetAudioMute((bool)mute);
#endif
#endif

	return DMR_ERROR_OK;
}

#if defined(INCLUDE_FEATURE_DISPLAY)
int RTK_DLNA_DMR::RTKMR_RenderingControl_SetContrast(DMR instance, void *session, unsigned char contrast)
{
#ifdef ANDROID_PLATFORM
        int Contrast = (int)contrast;
	if( SetContrast != NULL )
		SetContrast (0, (void*)&Contrast);
	//State update
	DMR_StateChange_Contrast(instance, contrast); 
#else
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		return 718;// Invalid InstanceID

	if( contrast > MAX_CONTRAST_VALUE )
		return 601;// Argument Value Out of Range

	SetupClass::GetInstance()->SetContrast((int)contrast);
	if( SetContrast != NULL )
		SetContrast (0, (void*)&contrast);
	//State update
	DMR_StateChange_Contrast(instance, contrast);
#endif
	return DMR_ERROR_OK;
}

int RTK_DLNA_DMR::RTKMR_RenderingControl_SetBrightness(DMR instance, void *session, unsigned char brightness)
{
#ifdef ANDROID_PLATFORM
        int Brightness = (int)brightness;
	if( SetBrightness != NULL )
		SetBrightness(0, (void*)&Brightness);
	DMR_StateChange_Brightness(instance, brightness);
#else
//	printf("[DMR event] %s Method Not Implemented\n", __func__);
	if( m_pDMR_MicroStack != instance )
		return 718;// Invalid InstanceID

	if( brightness > MAX_BRIGHTNESS_VALUE )
		return 601;// Argument Value Out of Range

	SetupClass::GetInstance()->SetBrightness((unsigned int)brightness);
	if( SetBrightness != NULL )
		SetBrightness(0, (void*)&brightness);
	//State update
	DMR_StateChange_Brightness(instance, brightness);
#endif
	return DMR_ERROR_OK;
}
#endif


SUBRENDERER_TYPE RTK_DLNA_DMR::RendererType(char *str,int type)
{
	unsigned int i = 0;
	if(type == 0){//extension
		for( i = 0; i < sizeof(extension_protocolInfo_mapping)/sizeof(EXTTORENDERERTYPE); i++)
		{
			if( !strncasecmp( str, extension_protocolInfo_mapping[i].mediaExtension, strlen(extension_protocolInfo_mapping[i].mediaExtension)) )
			{
				FREE_MEMORY(m_pDMR_mediaProtocolInfo);
				m_pDMR_mediaProtocolInfo = strdup(extension_protocolInfo_mapping[i].mediaProtocolInfo);
				FREE_MEMORY(m_pDMR_mediaMimeTypeProtocol);
				m_pDMR_mediaMimeTypeProtocol = strdup(extension_protocolInfo_mapping[i].mimeTypeProtocolInfo);
				m_mediaType = extension_protocolInfo_mapping[i].mediaType;
		printf("\t-------------------------------\n");
		printf("\t[%d] media protocol info is set by %s, index:%d\n", __LINE__, __func__, i);
		printf("\t-------------------------------\n");
				return extension_protocolInfo_mapping[i].subrendererType;
			}
			
		}
	}
	else{//protocolinfo
		for( i = 0; i < sizeof(extension_protocolInfo_mapping)/sizeof(EXTTORENDERERTYPE); i++)
		{
			if( strstr(str, extension_protocolInfo_mapping[i].mimeTypeProtocolInfo))
			{
				FREE_MEMORY(m_pDMR_mediaProtocolInfo);
				m_pDMR_mediaProtocolInfo = strdup(str);
				FREE_MEMORY(m_pDMR_mediaMimeTypeProtocol);
				m_pDMR_mediaMimeTypeProtocol = strdup(extension_protocolInfo_mapping[i].mimeTypeProtocolInfo);
				m_mediaType = extension_protocolInfo_mapping[i].mediaType;
		printf("\t-------------------------------\n");
		printf("\t[%d] media protocol info is set by %s, index:%d\n", __LINE__, __func__, i);
		printf("\t-------------------------------\n");
				return extension_protocolInfo_mapping[i].subrendererType;
			}
		}
	
	}
	return DMR_SUBR_NOTHING;
}


bool RTK_DLNA_DMR::CheckPlayContainerURI()
{
	bool ret = false;
	if(m_pSetAVURI && !strncmp(m_pSetAVURI,"dlna-playcontainer://",strlen("dlna-playcontainer://")))
		ret = true;
	return ret;
}



void RTK_DLNA_DMR::RegisterDlnaDmrCallbackFunc( int (*updateFuncPtr)(int, void*), void *pParam ,DLNA_DMR_CB_FUNC func_type)
{
	switch( func_type )
	{
		case DLNA_DMR_CB_QUERYFORCONNECT:
			QueryForConnection      = updateFuncPtr;
		case DLNA_DMR_CB_PREPAREFORCONNECT:
			PrepareForConnection    = updateFuncPtr;
			break;
		case DLNA_DMR_CB_PREPAREFORDISCONNECT:
			PrepareForDisconnection = updateFuncPtr;
			break;
		case DLNA_DMR_CB_SETBRIGHTNESS:
			SetBrightness           = updateFuncPtr;
			break;
		case DLNA_DMR_CB_SETCONTRAST:
			SetContrast             = updateFuncPtr;
			break;
		case DLNA_DMR_CB_SHOWSTATUS:
			ShowStatus              = updateFuncPtr;
			break;
		case DLNA_DMR_CB_UPDATEINFO:
			UpdateMediaInfo         = updateFuncPtr;
			break;

		case DLNA_DMR_CB_SHOWVOLUMESTATUS:
			ShowVolumeStatus        = updateFuncPtr;
			break;
		case DLNA_DMR_CB_SETMUTE:
			SetMute       		    = updateFuncPtr;
			break;
		case DLNA_DMR_CB_SHOWDIALOG:
			ShowDialog				= updateFuncPtr;
		case DLNA_DMR_CB_RESTART:
			RestartDMR              = updateFuncPtr;
			break;
		case DLNA_DMR_CB_NONE:
		default:
			break;
	}
}

void RTK_DLNA_DMR::DlnaDmrInternalStop()
{
    bool bDestroyRender = !HAS_FLAG(statusFlag, STATUS_SET_AV_TRANSPORT_URI);
    osal_MutexLock(&m_mutexSubRenderer);

	SetDMRInternalCommandInit();

	if( m_psubRenderer )
	{
		m_psubRenderer->Stop();
		m_PlayState = DMR_PS_Stopped; DMR_StateChange_TransportPlayState(m_pDMR_MicroStack, m_PlayState);
	}
	
    if (bDestroyRender) {
        printf("subRenderer destroy in DlnaDmrInternalStop\n");
        DELETE_OBJECT(m_psubRenderer);
        FREE_MEMORY(m_pSetAVURI);
    }
	DMR_StateChange_AbsoluteTimePosition(m_pDMR_MicroStack, 0);
       DMR_StateChange_RelativeTimePosition(m_pDMR_MicroStack, 0);
	SetDMRInternalCommandDone();

    osal_MutexUnlock(&m_mutexSubRenderer);
	//TODO, reset state
}

void RTK_DLNA_DMR::DlnaDmrInternalPause()
{
	if((m_PlayState == DMR_PS_Paused)||(m_PlayState == DMR_PS_Stopped))
		return;
	
	if(checkDMRIsBusy())
		return;

	SetDMRInternalCommandInit();
	if( m_psubRenderer != NULL && m_psubRenderer->Pause() == S_OK )
	{
		m_PlayState = DMR_PS_Paused;
		DMR_StateChange_TransportPlayState(m_pDMR_MicroStack, m_PlayState);
	}
	SetDMRInternalCommandDone();
	return;
}

void RTK_DLNA_DMR::DlnaDmrInternalPlayNextURI()
{
	if(checkDMRIsBusy())
		return;

	char * uri = DMR_StateGet_NextAVTransportURI(m_pDMR_MicroStack);
	RTKMR_AVTransport_Stop(m_pDMR_MicroStack,NULL);
	if(uri && *uri != '\0' && RTKMR_AVTransport_SetAVTransportURI(m_pDMR_MicroStack,NULL,uri,m_pNextURIMetadata) == DMR_ERROR_OK)
		RTKMR_AVTransport_Play(m_pDMR_MicroStack,NULL,"1");	
}



void RTK_DLNA_DMR::DlnaDmrInternalUnPause()
{
	if((m_PlayState != DMR_PS_Paused))
		return;

	if(checkDMRIsBusy())
		return;

	SetDMRInternalCommandInit();
	if( m_psubRenderer != NULL )
	{
		m_psubRenderer->Pause(false);
		m_PlayState = DMR_PS_Playing;
		DMR_StateChange_TransportPlayState(m_pDMR_MicroStack, m_PlayState);
	}
	SetDMRInternalCommandDone();
}
int RTK_DLNA_DMR::DlnaDmrInternalSeek(int seconds)
{
	if((m_PlayState != DMR_PS_Playing))
        return -1;

	if(checkDMRIsBusy())
        return -1;

    SetDMRInternalCommandInit();
    if( m_psubRenderer != NULL )
    {
		int ret = m_psubRenderer->SeekMediaPosition(-1, seconds);
		if(ret != S_OK){
			SetDMRInternalCommandDone();
        	return -1;
		}

        m_PlayState = DMR_PS_Playing;
        DMR_StateChange_TransportPlayState(m_pDMR_MicroStack, m_PlayState);
    }
    SetDMRInternalCommandDone();
	return 0;
}

void RTK_DLNA_DMR::DlnaDmrSyncRendererVar(unsigned char brightness, unsigned char contrast, unsigned char volume, bool mute)
{
	//TODO, check if m_pDMR_MicroStack->state is NULL ???
#if defined(INCLUDE_FEATURE_DISPLAY)
	DMR_StateChange_Brightness(m_pDMR_MicroStack, brightness);
	DMR_StateChange_Contrast(m_pDMR_MicroStack, contrast);
#endif
	DMR_StateChange_Volume(m_pDMR_MicroStack, volume);
	//TODO: check DMR_StateChange_Volume(instance, (int)(misc_default.m_volume*100/MAX_VOLUME_NUMBER));
	DMR_StateChange_Mute(m_pDMR_MicroStack, mute);
}

#if 0
int RTK_DLNA_DMR::UnregisterDlnaDmrCallbackFunc( bool (*updateFuncPtr)(int, void*), void *pParam ,DLNA_DMR_CB_FUNC  func_type)
{
	switch( func_type )
	{
		case DLNA_DMR_CB_PREPAREFORCONNECT:
			PrepareForConnection = NULL;
			break;
		case DLNA_DMR_CB_PREPAREFORDISCONNECT:
			PrepareForDisconnection = NULL;
			break;
		case DLNA_DMR_CB_NONE:
		default:
			break;
	}
}
#endif

SUBRENDERER_TYPE RTK_DLNA_DMR::RendererType(struct CdsObject *data)
{
	unsigned int i = 0;
	m_pMediaMetadata = data;

	printf("\t-------------------------------\n");
	printf("\t%s %d %s\n", __FILE__, __LINE__, __func__);
	printf("\t-------------------------------\n");
//	if( data->Res != NULL && data->Res->ProtocolInfo != NULL )
//	printf("\t-------------------------------\n");
//	printf("\tmetadata->Res->protocolInfo:%s\n", data->Res->ProtocolInfo);
//	printf("\t-------------------------------\n");

	if( data->Res != NULL && data->Res->ProtocolInfo != NULL && strlen(data->Res->ProtocolInfo)!= 0)
	{
	FREE_MEMORY(m_pDMR_mediaProtocolInfo);
		m_pDMR_mediaProtocolInfo = strdup(data->Res->ProtocolInfo);
	printf("\t-------------------------------\n");
	printf("\t[%d] media protocol info is set by %s\n\tprotocolInfo:%s", __LINE__, __func__, m_pDMR_mediaProtocolInfo);
	printf("\t-------------------------------\n");
		for( i = 0; i < sizeof(extension_protocolInfo_mapping)/sizeof(EXTTORENDERERTYPE); i++)
		{
//			if( strstr(data->Res->ProtocolInfo, extension_protocolInfo_mapping[i].mediaProtocolInfo) != NULL)
			if( strstr(data->Res->ProtocolInfo, extension_protocolInfo_mapping[i].mimeTypeProtocolInfo))
			{
				FREE_MEMORY(m_pDMR_mediaMimeTypeProtocol);
				m_pDMR_mediaMimeTypeProtocol = strdup(extension_protocolInfo_mapping[i].mimeTypeProtocolInfo);
				m_mediaType = extension_protocolInfo_mapping[i].mediaType;
				return extension_protocolInfo_mapping[i].subrendererType;
			}
		}
	}
#if 1
	unsigned int objectMajorType = CDS_CLASS_MASK_MAJOR & data->MediaClass;
	for( i = 0; i < sizeof(cds_renderertype_mapping)/sizeof(CDSTORENDERERTYPE); i++)
	{
		if( cds_renderertype_mapping[i].objectMajorType == objectMajorType )
		{
			if( data->Res != NULL && data->Res->ProtocolInfo != NULL ){
				FREE_MEMORY(m_pDMR_mediaProtocolInfo);
				m_pDMR_mediaProtocolInfo = strdup(data->Res->ProtocolInfo);
			}
			//TODO, fill up media type
			return cds_renderertype_mapping[i].subrendererType;
		}
	}
#endif


	return DMR_SUBR_NOTHING;
}


SUBRENDERER_TYPE RTK_DLNA_DMR::GetRendererType()//{return m_psubRenderer->GetRendererType();};
{
	if( m_psubRenderer != NULL )
		return m_psubRenderer->GetRendererType();
	else
		return DMR_SUBR_NOTHING;
}

char* RTK_DLNA_DMR::GetRenderMediaFilename()
{
	if(m_pMediaMetadata != NULL)
	{
		return m_pMediaMetadata->Title;
	}
	return NULL;
}

long RTK_DLNA_DMR::GetRenderMediaFileDate()
{
    if( m_pMediaMetadata != NULL)
    {                       
        return m_pMediaMetadata->TypeMajor.VideoItem.Date;
    }
    return NULL;
}

char* RTK_DLNA_DMR::GetRenderMediaFullname()
{
	if(m_ppMediaTrackURI && (m_NumberOfTracks >= 1) && (m_CurrentTrack >= 1) && (m_CurrentTrack <= m_NumberOfTracks))
	{
		return m_ppMediaTrackURI[m_CurrentTrack -1];
	}
    else if(m_pSetAVURI)
    {
        return m_pSetAVURI;
    }
    return NULL;
}

long RTK_DLNA_DMR::GetRenderMediaFilesize()
{
	if(m_pMediaMetadata != NULL && m_pMediaMetadata->Res != NULL)
	{
		return m_pMediaMetadata->Res->Size;
	}
	return 0;
}

long RTK_DLNA_DMR::GetRenderMediaDuration()
{
	if(m_pMediaMetadata != NULL && m_pMediaMetadata->Res != NULL)
    {
		if(m_pMediaMetadata->Res->Duration >= 0)
            return m_pMediaMetadata->Res->Duration;
        else
            return 0;
    }
    return 0;
}

int RTK_DLNA_DMR::GetRenderMediaResolutinoX()
{
	if(m_pMediaMetadata != NULL && m_pMediaMetadata->Res != NULL)
	{
		if((m_pMediaMetadata->Res->ResolutionX < 0) && m_psubRenderer)
		{
			int x,y;
			if(m_psubRenderer->GetResolution(&x,&y) == S_OK)
				return x;
		}
		return m_pMediaMetadata->Res->ResolutionX;
	}
	return 0;
}

int RTK_DLNA_DMR::GetRenderMediaResolutionY()
{
	if(m_pMediaMetadata != NULL && m_pMediaMetadata->Res != NULL)
	{
		if((m_pMediaMetadata->Res->ResolutionY < 0) && m_psubRenderer)
		{
			int x,y;
			if(m_psubRenderer->GetResolution(&x,&y) == S_OK)
				return y;
		}
		return m_pMediaMetadata->Res->ResolutionY;
	}
	return 0;
}

int RTK_DLNA_DMR::GetRenderMediaColorDepth()
{
	if(m_pMediaMetadata != NULL && m_pMediaMetadata->Res != NULL)
	{
		return m_pMediaMetadata->Res->ColorDepth;
	}
	return 0;
}

/*bool RTK_DLNA_DMR::isURIAlive()
{
	if(m_pSetAVURI && strncmp(m_pSetAVURI, "http://", strlen("http://"))==0)
	{
        IOPLUGIN IOPlugin;
        if(openIOPlugin_Http(&IOPlugin) == S_OK)
        {
			char tmp_filename[1024];
			sprintf(tmp_filename,"%s readTimeout=10 connectTimeout=10", m_pSetAVURI);

            void* fh = IOPlugin.open(tmp_filename, 0);
			bool isExist = fh != NULL;
          	if (fh) {
                IOPlugin.close(fh);
            }
            IOPlugin.dispose(&IOPlugin);

            return isExist;
        }
	}

	return false;
}

void RTK_DLNA_DMR::SetDMRMessageData(DMRMessageData &msgData)
{
	SetDMRBlock();
	SET_FLAG(statusFlag, STATUS_MESSAGE_DATA_EXIST);
	g_DMRMessageData = msgData;
	SetDMRUnBlock();
}

bool RTK_DLNA_DMR::GetDMRMessageData(DMRMessageData &msgData)
{
	bool res = false;
	if(!checkDMRIsBusy())
	{
		SetDMRBlock();
		if (HAS_FLAG(statusFlag, STATUS_MESSAGE_DATA_EXIST))
		{
			msgData = g_DMRMessageData;
			RESET_FLAG(statusFlag, STATUS_MESSAGE_DATA_EXIST);
			res= true;

		}
		SetDMRUnBlock();
	}

	return res;
}

void RTK_DLNA_DMR::SetDMRRendererVar(DLNA_DMR_RENDERERVAR type, unsigned char value)
{
	switch (type)
	{
	case DLNA_DMR_RENDERERVAR_VOL_NUMBER:
		DMR_StateChange_Volume(m_pDMR_MicroStack, value);
		break;
	case DLNA_DMR_RENDERERVAR_VOL_MUTE:
		DMR_StateChange_Mute(m_pDMR_MicroStack, (BOOL)value);
		break;
	}
}*/
