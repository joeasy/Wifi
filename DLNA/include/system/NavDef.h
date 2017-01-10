#ifndef __NAV_DEF_H__
#define __NAV_DEF_H__

/* Nav Command/Property/Constant Definitions */

#include <EType.h>

#ifdef __cplusplus
extern "C" {
#endif

    /* Special HRESULT Definitions Used Only By Nav ***************************/

    #define S_FEEDBACK_PRIVATE_INFO ((H_FLAG_SUCCESS) | (0x0000FFFF))

    /* Nav Command Identifiers ************************************************/

    typedef enum {

        NAV_COMMAND_INVALID,
        NAV_COMMAND_PLAYTITLE,
        NAV_COMMAND_PLAYCHAPTER,
        NAV_COMMAND_PLAYNEXTCHAPTER,
        NAV_COMMAND_PLAYPREVCHAPTER,
        NAV_COMMAND_PLAYAUTOCHAPTER,
        NAV_COMMAND_PLAYATTIME,
#ifndef DISABLEPLAYATPOS
		NAV_COMMAND_PLAYATPOS,
#endif        
        NAV_COMMAND_PLAYSEGMENT,
        NAV_COMMAND_MENUSHOW,
        NAV_COMMAND_MENUESCAPE,
        NAV_COMMAND_BUTTONSELECTNUMERIC,
        NAV_COMMAND_BUTTONSELECTPOINT,
        NAV_COMMAND_BUTTONACTIVATENUMERIC,
        NAV_COMMAND_BUTTONACTIVATEPOINT,
        NAV_COMMAND_BUTTONACTIVATE,
        NAV_COMMAND_BUTTONMOVESELECTION,
        NAV_COMMAND_GOUP,
        NAV_COMMAND_STILLESCAPE,
        NAV_COMMAND_SETAUDIOSTREAM,
        NAV_COMMAND_SETSUBPICTURESTREAM,
        NAV_COMMAND_SETANGLE,
        NAV_COMMAND_SETGETPROPERTY_ASYNC,
        NAV_COMMAND_RUN,          /* 0 parameters */
        NAV_COMMAND_PAUSE,        /* 0 parameters */
        NAV_COMMAND_STOP,         /* 0 parameters */
        NAV_COMMAND_SETSPEED,     /* 2 parameters: 1. speed (4 bytes) 2. skip (4 bytes) */
        NAV_COMMAND_SETSPEED_POS, /* 1 parameters: pts offset (8 bytes) */
        NAV_COMMAND_STILL,        /* 0 parameters */

    } NAV_CMD_ID;

    /* Nav Command Blocking Flags *********************************************/

    typedef enum {

        NAV_CMDBLK_PLAYTITLE                = 0x00000001,
        NAV_CMDBLK_PLAYCHAPTER              = 0x00000002,
        NAV_CMDBLK_PLAYNEXTCHAPTER          = 0x00000004,
        NAV_CMDBLK_PLAYPREVCHAPTER          = 0x00000008,
        NAV_CMDBLK_PLAYATTIME               = 0x00000010,
        NAV_CMDBLK_MENUESCAPE               = 0x00000020,
        NAV_CMDBLK_BUTTON                   = 0x00000040,
        NAV_CMDBLK_GOUP                     = 0x00000080,
        NAV_CMDBLK_STILLESCAPE              = 0x00000100,
        NAV_CMDBLK_SETVIDEOPRESENTATIONMODE = 0x00000200,
        NAV_CMDBLK_SETAUDIOSTREAM           = 0x00000400,
        NAV_CMDBLK_SETAUDIODOWNMIXMODE      = 0x00000800,
        NAV_CMDBLK_SETSUBPICTURESTREAM      = 0x00001000,
        NAV_CMDBLK_SETANGLE                 = 0x00002000,
        NAV_CMDBLK_PAUSE                    = 0x00004000,
        NAV_CMDBLK_STOP                     = 0x00008000,
        NAV_CMDBLK_FORWARDSCAN              = 0x00010000,
        NAV_CMDBLK_BACKWARDSCAN             = 0x00020000

    } NAV_CMD_BLK;

    /* Nav Property Identifiers ***********************************************/

    typedef enum {

        /* general property get/set for every input plugin */

        NAVPROP_INPUT_GET_PLAYBACK_STATUS, /* [out] NAVPLAYBACKSTATUS                     */
        NAVPROP_INPUT_GET_VIDEO_STATUS,    /* [out] NAVVIDEOSTATUS                        */
        NAVPROP_INPUT_GET_AUDIO_STATUS,    /* [out] NAVAUDIOSTATUS                        */
        NAVPROP_INPUT_GET_SPIC_STATUS,     /* [out] NAVSPICSTATUS                         */
        NAVPROP_INPUT_GET_MENU_STATUS,     /* [out] NAVMENUSTATUS                         */
        NAVPROP_INPUT_GET_DISC_STATUS,     /* [out] NAVDISCSTATUS                         */
        NAVPROP_INPUT_GET_TITLE_STATUS,    /* [out] NAVTITLESTATUS [in] int (title index) */
        NAVPROP_INPUT_GET_NAV_STATE,       /* [out] variable size block                   */
        NAVPROP_INPUT_GET_CMD_BLK_FLAGS,   /* [out] int, bit mask of NAV_CMD_BLK members  */
        NAVPROP_INPUT_GET_MEDIA_SIGNATURE, /* [out] 16 unsigned char's                    */
        NAVPROP_INPUT_GET_PRIVATE_INFO_FB, /* [out] NAVBUF to carry private info feedback */
        NAVPROP_INPUT_GET_TIME_POSITION,   /* [out] int64_t (file offset) [in] unsigned int (elapsed seconds) */

        NAVPROP_INPUT_SET_NAV_STATE,        /* [in] variable size block              */
        NAVPROP_INPUT_SET_NAV_STATE_FORCED, /* [in] variable size block              */
        NAVPROP_INPUT_SET_REPEAT_MODE,      /* [in] NAV_REPEAT_MODE                  */
        NAVPROP_INPUT_SET_RATE_INFO,        /* [in] long (playback rate information) */
        NAVPROP_INPUT_SET_VIDEO_PRES_MODE,  /* [in] NAV_VIDEO_PRES_MODE              */
        NAVPROP_INPUT_SET_RINGBUFFER_INFO,  /* [in] NAVRINGBUFFERINFO                */
        NAVPROP_INPUT_SET_FLOW_INTERFACE,   /* [in] IFlowManager*                    */
        
        NAVPROP_INPUT_SET_THUMBNAIL_TIME_POS,	/* [in] unsigned int, the time position(in seconds) of the thumbnail */
		NAVPROP_INPUT_SET_DECODE_THUMBNAIL,		/* [in] NAVTHUMBNAILPARA */
		NAVPROP_INPUT_SET_DECODE_MULTI_THUMBNAIL, /*[in] NAVMULTITHUMBNAILPARA */
	
		NAVPROP_INPUT_SET_PLAYBACK_MODE,			/* [in] NAV_PLAYBACK_MODE*/		
        NAVPROP_INPUT_SET_LPCM_INFO,        /* [in] NAVLPCMINFO     LPCM info setting*/

		NAVPROP_INPUT_SET_SNDAVORBIS_CHANNEL,
		NAVPROP_INPUT_SET_ERROR_CONCEALMENT_LEVEL,	/* [in] long (0 ~ 256, 0 will show as much decoded stuff as possible), 
													 * this only has effect on normal speed
													 */
		NAVPROP_INPUT_SET_AUDIO_PREFERENCE,			/* [in] ENUM_MEDIA_TYPE */
		NAVPROP_AUDIO_GET_PTS,					/* [out] int64_t */
						
		NAVPROP_INPUT_SET_EIO_LEVEL,			/* [in] long (0: fatal error, 1: read error) */
			
        /* special property get/set for DVD-Video nav input plugin */

        NAVPROP_DVDV_GET_BUTTON_JUMP_TARGET, /* [out] int (title index)                         */
        NAVPROP_DVDV_GET_PLAYBACK_STATUS_EX, /* [out] NAVPLAYBACKSTATUS_DVDV                    */
        NAVPROP_DVDV_SET_PARENTAL_CONTROL,   /* [in]  NAVPARENTALCONTROL                        */
        NAVPROP_DVDV_SET_PLAYER_REGION,      /* [in]  int (player region code 1~8)              */
        NAVPROP_DVDV_SET_LANG_PREFERENCE,    /* [in]  NAVLANGPREFERENCE                         */
        NAVPROP_DVDV_APPROVE_TMP_PARENTAL,   /* [in]  bool, non-zero to approve, zero to reject */

        /* special property get/set for DVD-VR nav input plugin */

        NAVPROP_VR_GET_RTR_VMGI,                 /* [out] RTR_VMGI pointer                            */
        NAVPROP_VR_DECODE_KEYFRAME,              /* [in]  decode the key frame                        */
        NAVPROP_VR_SET_BOOKMARK,                 /* [in]  set bookmark                                */
        NAVPROP_VR_RESTORE_BOOKMARK,             /* [out] restore bookmark                            */
        NAVPROP_VR_SET_PLAYDOMAIN,               /* [in]  set playback domain (0:program, 1:playlist) */
        NAVPROP_VR_DECODE_MULTIPLE_KEYFRAME,     /* [in]  decode multiple key frame                   */
        NAVPROP_VR_IS_NEW_REC_PG,                /* [out] get the pg state does ever played           */
        NAVPROP_VR_DISABLE_AUTO_RESET_NEW_TITLE, /* [in]  turn off auto reset new title flag          */
        NAVPROP_VR_CAN_SPEED,                    /* [out] get the playback can be fast forward or not */

        /* special property get/set for VCD nav input plugin */

        NAVPROP_VCD_GET_PBC_ON_OFF, /* [out] NAVVCDPBCINFO */
        NAVPROP_VCD_SET_PBC_ON_OFF, /* [in]  unsigned int, non-zero if PBC is on, zero if PBC is off */

        /* special property get/set for I-Frame only mode rate control */
        NAVPROP_ISCAN_WITH_RATE_AWARE, /* [in] int, for I scan mode, send with rate awareness (non-zero) or not(zero) */

        /* special property get/set for DivX Digital Right Management */

        NAVPROP_DIVX_DRM_QUERY,          /* [out] NAVDIVXDRMINFO                      */
        NAVPROP_DIVX_DRM_APPROVE_RENTAL, /* [in]  to use one of the reantal available */
		NAVPROP_DIVX_EDITIONNUM_QUERY,
		NAVPROP_DIVX_EDITIONNAME_QUERY,
		NAVPROP_DIVX_EDITIONLAWRATE_QUERY,
		NAVPROP_DIVX_METADATA_QUERY,
		NAVPROP_DIVX_LAWRATE_QUERY,
		NAVPROP_DIVX_TITLENUM_QUERY,
		NAVPROP_DIVX_CHAPTERTYPE_QUERY,
		NAVPROP_DIVX_LASTPLAYPATH,

        /* special property get/set for CDDA */

        NAVPROP_CDDA_DISCINFO, /* [out] NAVCDDAINFO output disc information for CDDA */

        /* special property get/set for DV nav input plugin */

        NAVPROP_DV_CTRL_CMD,                    /* [in] NAV_DV_ID */
        NAVPROP_DV_DISPLAY_CTRL,                /* [in] unsigned int DV Display control */

        /* special property get/set for DTV nav input plugin */

        NAVPROP_DTV_SET_TUNER_HANDLE,      /* [in]  unsigned char, the tuner handle */
        NAVPROP_DTV_GET_SI_ENGINE_HANDLE,  /* [out] unsigned int, the SI engine handle */ /* FIXME: OBSELETE */
        NAVPROP_DTV_SET_SI_ENGINE_HANDLE,  /* [in]  unsigned int, the SI engine handle */
        NAVPROP_DTV_SET_PREVIEW_MODE,		/* [in] NAV_PREVIEW_MODE */	
        NAVPROP_DTV_SET_RECORD_MODE,       /* [in]  NAVRECORDMODE, recording mode */
        NAVPROP_DTV_GET_RECORD_STATUS,     /* [out] NAVRECORDSTATUS, recording status */
        NAVPROP_DTV_GET_EDITING_INTERFACE, /* [out] IFileEditing*, pointer to editing interface */
		NAVPROP_DTV_AB_FAST_COPY,			/* [in] NAVEDITINGPARA, editing parameters */
		NAVPROP_DTV_GET_SI_STATE,		   /* [out] the SI state whose size would be set in outDataSize */
		NAVPROP_DTV_GET_AUDIO_FORMAT_FB,	/* [in] SI_CODEC_TYPE [out] privateData in SI_FILTER_DESC */

#ifdef ENABLE_MHEG5
        NAVPROP_DTV_FORCE_UPDATE_DEMUX_TARGET, /*update demux setting*/
        NAVPROP_DTV_UPDATE_MHEG5_MEDIA_SOURCE,        /*update MHEG5 media source (audio clip/iframe/streaming)*/
#endif
		NAVPROP_GET_IOPLUGIN_SUBTITLE_INFO,/*for hls subtitle*/
		NAVPROP_GET_IOPLUGIN_THUMBNAIL_INFO,/*for hls thumbnail*/
		
        /* general property get/set for every demux plugin */

        /*
        NAVPROP_DEMUX_xxxxxxxx,
        */

        /* general property get/set for nav filter */

        NAVPROP_NAV_SET_TIMEOUT_LIMITS,   /* [in] NAVTIMEOUTLIMITS */
        NAVPROP_NAV_SET_STARTUP_FULLNESS, /* [in] long, av sync start-up fullness requirement in bytes */

        /*Subtitle fonts Get for mkv internlal Subtitle*/
        NAVPROP_SUBTITLE_GET_FONTS,       /* [in] char *, for filename */
		
        /*Allow input plugin to set its favorable fullness timeout.
		  It is internally used by navigator */
		NAVPROP_INPUT_GET_TIMEOUT_LIMITS,	/* [out] NAVTIMEOUTLIMITS */

#ifndef DISABLE_GETPLAYBACK_PTSINFO
		NAVPROP_NAV_GET_PLAYBACK_PTSINFO,	/* [out] NAVPLAYBACKPTSINFO */
#endif

		
        /* misc */

        NAVPROP_MISC_HACK_UOP = 12345678,
        
        //for flash
        NAVPROP_INPUT_FLASH_SET_BUFFER,	/* [in] FLASH_BUF_NODE */
		NAVPROP_INPUT_FLASH_SET_CALLBACK_FUN,
		NAVPROP_INPUT_FLASH_FLUSH_BUFFER,
		NAVPROP_INPUT_FLASH_SET_DECODE_CMD,	/* [in] NAVFLASHDECODECMD */
		NAVPROP_INPUT_FLASH_SET_STREAMPLAYER,	/*[in] void* */
		NAVPROP_INPUT_FLASH_SET_MEDIA_TYPE,		/*[in] ENUM_MEDIA_TYPE */

        //For NRD2.X
        NAVPROP_INPUT_NRD2_SET_HEADER,
	NAVPROP_INPUT_NRD2_SET_DRM_HEADER,
        NAVPROP_INPUT_NRD2_GET_CHALLENGE,
        NAVPROP_INPUT_NRD2_STORE_LICENSE,
        NAVPROP_INPUT_NRD2_CLEAR_LICENSE,	

	//added by baili_feng for getting Content Description of ASF
	NAVPROP_INPUT_GET_CONTENT_DESCRIPTION,
	//add by kuyo.chang for getting date info
	NAVPROP_INPUT_GET_DATE_INFO,

        //For ACETREX PLAYBACK
        NAVPROP_INPUT_SET_ACETREX_DATA,
        
        //To enable/disable menu and config menu size. 
        NAVPROP_INPUT_SET_MENU_CONFIG,	/* [in] NAVMENUCONFIG*/

		//for skype to open audio or video stream
		NAVPROP_INPUT_OPEN_SUB_STREAM,	/* [in] path, [out] fd of this substream 
										 * Note : Should use sync SetGetProperty
										 */
		NAVPROP_INPUT_CLOSE_SUB_STREAM, /* [in] fd to be closed 
										 * Note : Should use sync SetGetProperty
										 */
										 
		NAVPROP_INPUT_SET_STEREOSCOPIC_OUTPUT_MODE, /*[in] bool */								 
		NAVPROP_INPUT_GET_STEREOSCOPIC_OUTPUT_CAP, /*[out] bool */	
		
		NAVPROP_NRD3_SET_DROP_COMMAND, /* [in] NAVUSERDROP */
		
		NAVPROP_INPUT_SET_ASSOCIATE_AUDIO_MIXING_LEVEL, /* [in] NAVAUDIOMIXINGLEVEL*/
		NAVPROP_DTV_GET_PCR_STATUS,				/*[out] NAVDTVPCRINFO */
		NAVPROP_DTV_SET_TUNER_DESCRAMBLE_INFO, /*[in] NAVDTVDESINFO*/
    } NAV_PROP_ID;

    /* Nav DV Control Commands ************************************************/

    typedef enum {

        NAV_DV_CTRL_PLAY,
        NAV_DV_CTRL_STOP,
        NAV_DV_CTRL_PAUSE,
        NAV_DV_CTRL_FF,
        NAV_DV_CTRL_FR,
        NAV_DV_CTRL_SF,
        NAV_DV_CTRL_SR,
        NAV_DV_CTRL_R,
        NAV_DV_CTRL_WIND_FF,
        NAV_DV_CTRL_WIND_FR,
        NAV_DV_CTRL_PASSTHROUGH,
        NAV_DV_CTRL_GET_PLAYBACK_STATUS,
        NAV_DV_QUERY_SIGNAL_MODE,

    } NAV_DV_ID;

    /* Nav DV Camcorder Device Status *****************************************/

    typedef enum {

        DV_PLAY,
        DV_STOP,
        DV_PAUSE,
        DV_FF, // fast forward
        DV_FR, // fast reverse
        DV_SF, // slow forward
        DV_SR, // slow reverse
        DV_R,  // reverse playback
        DV_WF,
        DV_WR,
        DV_INIT,
        DV_NOT_CONNECTED,
        DV_REC,
        DV_LOAD_MEDIUM,

    } DV_STATE;

    /* Nav DV Display Control Bits ************************************************/

    typedef enum {

        NAV_DV_DISPLAY_RECORD_DATE_TIME = 1,

    } NAV_DV_DISPLAY_CTRL_BIT;

    /* Nav Button Directions **************************************************/

    typedef enum {

        NAV_BTNDIR_UP,
        NAV_BTNDIR_DOWN,
        NAV_BTNDIR_LEFT,
        NAV_BTNDIR_RIGHT

    } NAV_BTNDIR_ID;

    /* Nav Display States *****************************************************/

    typedef enum {

        NAV_DISPLAY_OFF,
        NAV_DISPLAY_ON,
        NAV_DISPLAY_UNCHANGED

    } NAV_DISPLAY_STATE;

    /* Nav Repeat Modes *******************************************************/

    typedef enum {

        NAV_REPEAT_NONE,
        NAV_REPEAT_TITLE,
        NAV_REPEAT_CHAPTER,
        NAV_REPEAT_AB,
        NAV_REPEAT_TITLE_ONCE

    } NAV_REPEAT_MODE;

    /* Nav Video Presentation Mode ********************************************/

    typedef enum {

        NAV_VPM_NORMAL,
        NAV_VPM_WIDE,
        NAV_VPM_PAN_SCAN,
        NAV_VPM_LETTER_BOX

    } NAV_VIDEO_PRES_MODE;

    /* Nav TV Systems *********************************************************/

    typedef enum {

        NAV_TVSYSTEM_NTSC,
        NAV_TVSYSTEM_PAL,
        NAV_TVSYSTEM_CUSTOM,
        NAV_TVSYSTEM_UNKNOWN

    } NAV_TV_SYSTEM;

    /* Nav Source Letterbox Modes *********************************************/

    typedef enum {

        NAV_SRCLTRBOX_NONE,
        NAV_SRCLTRBOX_NORMAL,
        NAV_SRCLTRBOX_14_9_CENTER,
        NAV_SRCLTRBOX_14_9_TOP,
        NAV_SRCLTRBOX_16_9_CENTER,
        NAV_SRCLTRBOX_16_9_TOP,
        NAV_SRCLTRBOX_16_9_PLUS_CENTER,
        NAV_SRCLTRBOX_14_9_FULL_FORMAT

    } NAV_SRC_LTRBOX;

    /* Nav Audio Description **************************************************/

    typedef enum {

        NAV_AUDIODESC_NOT_SPECIFIED,
        NAV_AUDIODESC_NORMAL,
        NAV_AUDIODESC_FOR_VISUALLY_IMPAIRED,
        NAV_AUDIODESC_DIRECTOR_COMMENT

    } NAV_AUDIO_DESC;

    /* Nav Subpicture Description *********************************************/

    typedef enum {

        NAV_SPICDESC_NOT_SPECIFIED,
        NAV_SPICDESC_NORMAL,
        NAV_SPICDESC_BIGGER,
        NAV_SPICDESC_CHILDREN,
        NAV_SPICDESC_CC_NORMAL,
        NAV_SPICDESC_CC_BIGGER,
        NAV_SPICDESC_CC_CHILDREN,
        NAV_SPICDESC_FORCED,
        NAV_SPICDESC_DIRECTOR_COMMENT_NORMAL,
        NAV_SPICDESC_DIRECTOR_COMMENT_BIGGER,
        NAV_SPICDESC_DIRECTOR_COMMENT_CHILDREN

    } NAV_SPIC_DESC;

    /* Nav Domains ************************************************************/

    typedef enum {

        NAV_DOMAIN_STOP,
        NAV_DOMAIN_TITLE,
        NAV_DOMAIN_MENU,
        NAV_DOMAIN_VR_PG,
        NAV_DOMAIN_VR_PL

    } NAV_DOMAIN;

    /* Nav AV Sync Modes ******************************************************/

    typedef enum {

        NAV_AVSYNC_AUDIO_MASTER_AUTO,      /* the new auto no skip mode */
        NAV_AVSYNC_AUDIO_MASTER_AUTO_SKIP, /* the old auto mode */
        NAV_AVSYNC_AUDIO_MASTER_AUTO_AF,   /* auto no skip mode, but use audio master first at the start */
        NAV_AVSYNC_AUDIO_MASTER,
        NAV_AVSYNC_SYSTEM_MASTER,
        NAV_AVSYNC_AUDIO_ONLY,
        NAV_AVSYNC_VIDEO_ONLY,
        NAV_AVSYNC_VIDEO_ONLY_SLIDESHOW,
        NAV_AVSYNC_SLIDESHOW,
        NAV_AVSYNC_VIDEO_MASTER_FIRST,
        NAV_AVSYNC_AUDIO_LOW_DELAY,
        NAV_AVSYNC_SYSTEM_MASTER_BY_PCR,

    } NAV_AVSYNC_MODE;

    /* Nav Stream Types *******************************************************/

    typedef enum {

        NAV_STREAM_VIDEO,
        NAV_STREAM_AUDIO,
        NAV_STREAM_SPIC,
        NAV_STREAM_TELETEXT

    } NAV_STREAM_TYPE;

    /* Nav Private Data ID ****************************************************/

    typedef enum {

        NAV_PRIVATE_DVD_PCI     = 0,
        NAV_PRIVATE_DVD_DSI     = 1,
        NAV_PRIVATE_SPU_STR_ID	= 2,
        NAV_PRIVATE_DVR_RDI     = 0x50,
        NAV_PRIVATE_DTV_PSI     = 0x10000000,
        NAV_PRIVATE_DTV_PCR     = 0x10000001,
        NAV_PRIVATE_DEMUX_COUNT = 0x10000002,
 		NAV_PRIVATE_DTV_SCRAMBLE_CHECK  = 0x10000003,
 		NAV_PRIVATE_HDMV_GT_PACKET  = 0x10000004,	//NAVGTPACKET

    } NAV_PRIVATE_DATA_ID;

    /* Nav content_description_type********************************************/
    //added by baili_feng
    typedef enum
    {
	    NAV_CONTENT_DESCRIPTION_NONE,
	    NAV_CONTENT_DESCRIPTION_ASF,
    } NAV_CONTENT_DESCRIPTION_TYPE;

    /* Nav CONTENTDESCRIPTION *************************************************/
    //added by baili_feng
    typedef struct _tagNAVCONTENTDESCRIPTION
    {
	    NAV_CONTENT_DESCRIPTION_TYPE type;
	    void* data;
	    int size;
    } NAVCONTENTDESCRIPTION;

    /* Nav Timeout Limits *****************************************************/

    typedef struct _tagNAVTIMEOUTLIMITS {

        long maxTimeout; /* in units of 90,000 ticks per second */
        long minTimeout; /* in units of 90,000 ticks per second */

    } NAVTIMEOUTLIMITS;

    /* Nav Media Info *********************************************************/

    typedef struct _tagNAVMEDIAINFO {

        ENUM_MEDIA_TYPE mediaType;
        ENUM_MEDIA_TYPE videoType;
        ENUM_MEDIA_TYPE audioType;
        ENUM_MEDIA_TYPE spicType;
        NAV_AVSYNC_MODE AVSyncMode;
        long            AVSyncStartupFullness; /* in bytes; 0 means no startup fullness requirement */
        long            minForwardSkip;
        long            maxForwardSkip;
        long            minReverseSkip;
        long            maxReverseSkip;
        bool            bSmoothReverseCapable;
        bool            bUseDDRCopy;
        bool            bFlushBeforeDelivery;
        
        long            AVSyncStartupRPTS;
        long            AVSyncStartupFullnessUpper;

    } NAVMEDIAINFO;

    typedef struct _tagAudioCodecInfo {
        ENUM_MEDIA_TYPE formatType;
        uint16_t        nChannels;
        uint32_t        nSamplesPerSec;
        uint32_t        nAvgBytesPerSec;
        uint16_t        nBlockAlign; 
        uint16_t        wBitsPerSample;
        uint16_t        cbSize;         
        unsigned char   *cbData; 
    } AUDIOCODECINFO; 



    typedef struct _tagVideoCodecInfo {
        ENUM_MEDIA_TYPE formatType; 
        uint16_t        width; 
        uint16_t        height; 
        uint16_t        cbSize;
        unsigned char   *cbData;
    } VIDEOCODECINFO;


    /* Nav Media Info Ex. for RTSP w/RTP cases */
    typedef struct _tagNAVMEDIAINFOEX {
        ENUM_MEDIA_TYPE mediaType; 
        ENUM_MEDIA_TYPE videoType;               /* for Codec Type */
        ENUM_MEDIA_TYPE audioType;               /* for Codec Type */
        ENUM_MEDIA_TYPE spicType;                /* for Codec Type */

        void*           audioPrivateData;        
        unsigned int    audioPrivateDataSize;    /* AUDIOCODECINFO */

        void*           videoPrivateData;
        unsigned int    videoPrivateDataSize;    /* VIDEOCODECINFO */

        void*           spicPrivateData;
        unsigned int    spicPrivateDataSize;

    } NAVMEDIAINFOEX;

    /* Nav IO Info ******i*****************************************************/

/****sub define for file info in filelink*******************************************/
#ifdef ENABLE_FILELIST_PLAYBACK
	    typedef struct _tagFILELINKINFO {
			long long filesize;
			long long begin_pts;
			long long end_pts;
		}FILELINKINFO;
#endif
    typedef struct _tagNAVIOINFO {

        int64_t         totalBytes;               /* -1 means unknown */
        int             totalSeconds;             /* -1 means unknown */
        int             averageBytesPerSecond;    /* -1 means unknown */
        int             seekAlignmentInBytes;     /* -1 means no restriction */
        int             readSizeAlignmentInBytes; /* -1 means no restriction */
        bool            bSeekable;                /* true: support seeking within media */
        bool            bSlowSeek;                /* true: seeking is slow */
        bool            bStreaming;               /* true: media can grow bigger */
        bool            bDirectIOCapable;         /* true: support direct IO */
		bool			bOpenMany;                /* true: this stream can support openN */
        ENUM_MEDIA_TYPE ioType;
        void*           ioInternalHandle;
        ENUM_MEDIA_TYPE	preDeterminedType;        /* for RTSP w/RTP cases */
        NAVMEDIAINFOEX  mediaInfo;
        bool            bSeekByTime;              /* true: Calling IOPLUGIN.seekByTime() to do the seeking jobs */
    	
    	bool			bPrepareBuf;			  /* true : io plugin will prepare buffer and input plguin gets read pointer by getReadPtr()*/
    	bool			bUseDDRCopy;			  /* It is meaningless if bPrepareBuf = false. true : buffer prepared by IO plugin is contiguous and be able to use DDRCopy */
	bool            bRedirect;                            /* true : Have to redirect to the new io plugin. */
	const char*     pRedirectURL;             /* valid if bRedirect is true. */
#ifdef ENABLE_FILELIST_PLAYBACK
		bool			bIsFileLink;
		int			numFilesInLink;
		FILELINKINFO* pFileInfoList;
#endif
    } NAVIOINFO;

    /* Nav Streaming Info *****************************************************/

    typedef struct _tagNAVSTREAMINGINFO {

        int     totalSecondsPrepared;             /* -1 means unknown */
        int64_t totalBytesPrepared;               /* -1 means unknown */
        int     totalSeconds;                     /* -1 means unknown */
        int     secondsNeededToFinishPreparation; /* -1 means unknown */
        NAV_STREAM_TYPE bitstreamType;            /* for RTSP w/RTP, to indicate stream type */
        int64_t pts;                              /* for RTSP w/RTP, for timestamp */
    } NAVSTREAMINGINFO;

    /* Nav Information for Auto Stop ******************************************/

    typedef struct _tagNAVAUTOSTOPINFO {

        unsigned char bActive;
        unsigned char bHasVideo;
        unsigned char bHasAudio;
        unsigned char bHasIndexGen;

    } NAVAUTOSTOPINFO;

    /* Nav Latest Demux'ed PTS Info *******************************************/

    typedef struct _tagNAVDEMUXPTSINFO {

        int64_t videoPTS;
        int64_t audioPTS;
        int64_t spicPTS;

        long    videoFullness[2];
        long    audioFullness[2];
        long    videoBufSize[2];
        long    audioBufSize[2];

    } NAVDEMUXPTSINFO;

#ifndef DISABLE_GETPLAYBACK_PTSINFO
	typedef struct _tagNAVPLAYBACKPTSINFO {
		int64_t presentVideoPTS;
		int64_t presentAudioPTS;
		int64_t demuxVideoPTS;
		int64_t demuxAudioPTS;
	} NAVPLAYBACKPTSINFO;
#endif

    /* Nav Time ***************************************************************/

    typedef struct _tagNAVTIME {

        int seconds;
        int frameIndex;

    } NAVTIME;

    /* Nav Video Attribute ****************************************************/

    typedef struct _tagNAVVIDEOATTR {

        ENUM_MEDIA_TYPE type;
        NAV_TV_SYSTEM   tvSystem;
        int             aspectRatioX;
        int             aspectRatioY;
        int             frameSizeX;
        int             frameSizeY;
        int             frameRate; // PTS per frame
        NAV_SRC_LTRBOX  srcLtrBox;
        bool            line21Switch1;
        bool            line21Switch2;
        bool            bAllowPanScanMode;
        bool            bAllowLetterBoxMode;

        unsigned int    languageCode; // to meet DivX requirement
        unsigned int    countryCode;  // to meet DivX requirement
        unsigned int    typeCode;     // to meet DivX requirement

		int				bitRate;
    } NAVVIDEOATTR;

    /* Nav Audio Attribute ****************************************************/

    typedef struct _tagNAVAUDIOATTR {

        ENUM_MEDIA_TYPE type;
        unsigned int    languageCode; // ISO-639 language code, two lowercase letters
        NAV_AUDIO_DESC  description;
        int             bitsPerSample;
        int             samplingRate;
        int             numChannels;
        bool            bEnabled;

        unsigned int    countryCode;  // to meet DivX requirement
        unsigned int    typeCode;     // to meet DivX requirement
	unsigned short  pid;

    } NAVAUDIOATTR;

    /* Nav Subpicture Attribute ***********************************************/

    typedef struct _tagNAVSPICATTR {

        ENUM_MEDIA_TYPE type;
        unsigned int    languageCode; // ISO-639 language code, two lowercase letters
        NAV_SPIC_DESC   description;
        bool            bEnabled;

        unsigned int    countryCode;  // to meet DivX requirement
        unsigned int    typeCode;     // to meet DivX requirement

    } NAVSPICATTR;

    /* Nav Playback Status ****************************************************/

    typedef struct _tagNAVPLAYBACKSTATUS {

        NAV_DOMAIN      domain;
        int             numTitles;
        int             currentTitle;
        int             numChapters;
        int             currentChapter;
        int             numAngles;
        int             currentAngle;
        int             numButtons;
        NAVTIME         elapsedTime;
        NAVTIME         totalTime;
        NAVTIME         currChapterStartTime;
        NAVTIME         currChapterEndTime;
        bool            bPaused;
        bool            bStill;
        bool            bInAngleBlock;
        NAV_REPEAT_MODE repeatMode;
        ENUM_MEDIA_TYPE mediaType;
        unsigned int    languageCode;
        int             bufferingSecondsLeftPrepare;
        int             bufferingSecondsLeftPlayback;
        int64_t			elapsedPTS;	//for SRT
        int64_t			basePTS;
        NAVTIME         transmittedTime; // this is used to indicate BS depth which has been transmitted to decoders

    } NAVPLAYBACKSTATUS;

    typedef struct _tagNAVPLAYBACKSTATUS_DVDV {

        NAVPLAYBACKSTATUS status;
        int               vtsN;
        int               pgcN;
        int               pgN;
        int               cellN;
        uint32_t          vobuStartAddress;
        int               frameIdxInVOBU;
        int               currentButtonNumber;
        int               currentButtonTargetTitle;
        int               currentButton_X_Start;
        int               currentButton_X_End;
        int               currentButton_Y_Start;
        int               currentButton_Y_End;

    } NAVPLAYBACKSTATUS_DVDV;

    #define NAV_MAX_VIDEO_STREAMS 1
    #define NAV_MAX_AUDIO_STREAMS 16
    #define NAV_MAX_SPIC_STREAMS  32
    #define NAV_MAX_MENUS         8

    /* Nav Video Status *******************************************************/

    typedef struct _tagNAVVIDEOSTATUS {

        int                 numStreams;
        int                 indexCurrentStream;
        NAVVIDEOATTR        streamAttr[NAV_MAX_VIDEO_STREAMS];
        NAV_VIDEO_PRES_MODE presentationMode;

    } NAVVIDEOSTATUS;

    /* Nav Audio Status *******************************************************/

    typedef struct _tagNAVAUDIOSTATUS {

        int          numStreams;
        int          indexCurrentStream;
        NAVAUDIOATTR streamAttr[NAV_MAX_AUDIO_STREAMS];

    } NAVAUDIOSTATUS;

    /* Nav Subpicture Status **************************************************/

    typedef struct _tagNAVSPICSTATUS {

        int          numStreams;
        int          indexCurrentStream;
        NAVSPICATTR  streamAttr[NAV_MAX_SPIC_STREAMS];
        bool         bDummyStream;
        bool         bDisplay;

    } NAVSPICSTATUS;

    /* Nav Menu Status ********************************************************/

    typedef struct _tagNAVMENUSTATUS {

        int          numMenus;
        unsigned int menuID[NAV_MAX_MENUS];

    } NAVMENUSTATUS;

    /* Nav Disc Status ********************************************************/

    typedef struct _tagNAVDISCSTATUS {

        unsigned int    allowedRegions;
        ENUM_MEDIA_TYPE discType;
        ENUM_MEDIA_TYPE discSubtype;
        int             isEncrypt;

    } NAVDISCSTATUS;

    /* Nav Title Status *******************************************************/

    typedef struct _tagNAVTITLESTATUS {

        int     numChapters;
        int     numAngles;
        NAVTIME totalTime;

    } NAVTITLESTATUS;

    /* Nav Command Status *****************************************************/

    typedef struct _tagNAVCMDSTATUS {

        NAV_CMD_ID   executedCmdType;
        unsigned int executedCmdID;
        HRESULT      executedCmdResult;
        int          numOfPendingCmds;

    } NAVCMDSTATUS;

    /* Nav File Session Information *******************************************/

    typedef struct _tagNAVFILESESSIONINFO {

        int64_t         startAddress;
        int64_t         endAddress;
        int64_t         stcOffset;
        int             channelIndex;
        ENUM_MEDIA_TYPE mediaType;

    } NAVFILESESSIONINFO;

    /* Nav Ring Buffer Information ********************************************/

    typedef struct _tagNAVRINGBUFFERINFO {

        unsigned long  bufferBeginAddr_physical;
        BYTE*          bufferBeginAddr_cached;
        BYTE*          bufferBeginAddr_noncached;
        unsigned long  bufferSize;
        unsigned long* pWritePointer;
        unsigned long* pNumOfReadPointers;
        unsigned long* pReadPointers[4];

    } NAVRINGBUFFERINFO;

    /* Nav Parental Control Settings ******************************************/

    typedef struct _tagNAVPARENTALCONTROL {

        char countryCode[2]; // ISO 3166 Alpha-2 code
        int  level;          // from 1 (kid-safe) to 8 (adult-level), or -1 to turn off parental control

    } NAVPARENTALCONTROL;

    /* Nav Language Preference Settings ***************************************/

    typedef struct _tagNAVLANGPREFERENCE {

        uint16_t       prefMenuLangCode;  // ISO-639 language code, two lowercase letters
        uint16_t       prefAudioLangCode; // ISO-639 language code, two lowercase letters
        NAV_AUDIO_DESC prefAudioLangDesc;
        uint16_t       prefSpicLangCode;  // ISO-639 language code, two lowercase letters
        NAV_SPIC_DESC  prefSpicLangDesc;

    } NAVLANGPREFERENCE;

    /* CDDA disc information **************************************************/

    typedef struct _tagNAVCDDAINFO {

        int     trackNum;
        NAVTIME discLength;
        NAVTIME trackLength[99];

    } NAVCDDAINFO;

    /* VCD PBC mode information ***********************************************/

    typedef struct _tagNAVVCDPBCINFO {

        unsigned int pbcCapability; /* non-zero if it can be played with PBC ON, zero if it only can be play with PBC OFF */
        unsigned int pbcState;      /* non-zero if PBC is on, zero if PBC is off */

    } NAVVCDPBCINFO;

    /* DivX DRM Information ***************************************************/

    typedef enum {

        NAV_DIVX_DRM_NONE,
        NAV_DIVX_DRM_NOT_AUTH_USER,
        NAV_DIVX_DRM_RENTAL_EXPIRED,
        NAV_DIVX_DRM_REQUEST_RENTAL,
        NAV_DIVX_DRM_AUTHORIZED,
		NAV_DIVX_DRM_RENTAL_AUTHORIZED,

    } NAV_DIVX_DRM_ID;

    /* DivX DRM information ***************************************************/

    typedef struct _tagDIVXDRM {

        NAV_DIVX_DRM_ID drmStatus;
        unsigned char useLimit;
        unsigned char useCount;
        unsigned char digitalProtection;
        unsigned char cgms_a;
        unsigned char acpt_b;
        unsigned char ict;

    } NAVDIVXDRMINFO;

	
	typedef enum {
		
		DIVX_METADATA_TITLE,
		DIVX_METADATA_AUDIO,
		DIVX_METADATA_SUBTITLE,	
		DIVX_METADATA_CHAPTER,
		
	} DIVX_METADATA_TYPE;
	
	typedef struct __DIVX_METADATA_QUERYPARAM
	{
		DIVX_METADATA_TYPE type;
		char language[32];
		
	}DIVX_METADATA_QUERYPARAM;
	

    /* Nav Preview Mode ********************************************************/
    typedef enum {

        NAV_PREVIEW_OFF,
        NAV_PREVIEW_ON,
        
    } NAV_PREVIEW_MODE;
    
    /* Nav Record Mode ********************************************************/

    typedef enum {

        NAV_RECORD_OFF,
        NAV_RECORD_ON,
        NAV_RECORD_PAUSE,
        NAV_RECORD_RESUME,

    } NAV_RECORD_MODE;

#if defined(ENABLE_MHEG5) || defined(ENABLE_TCLHAL)
    /* MHEG5 i-frame/audio clip/ICS use ********************************************************/

    typedef enum {
        NAV_AVSYNC_OFF,/*no set av sync mode*/
        NAV_AVSYNC_ON,/*enable av sync*/
        NAV_AVSYNC_FREERUN/*free run*/

    } NAV_AVSYNC_TYPE;

#endif


    typedef struct _tagNAVRECORDMODE {

        NAV_RECORD_MODE mode;
        char*           path;         /* path of the file to-be-created for recording */
        unsigned int    handle;       /* if 'path' is NULL, use 'handle' as the recording target instead */
        unsigned char   bCircular;    /* TRUE (non-zero) if using circular file; FALSE (zero) if using normal file */
        unsigned char   bLinkingList; /* TRUE (non-zero) if using linking-list file; FALSE (zero) if using array file */
        int64_t         maxFileSize;  /* maximal file size (circular or non-circular) in bytes, use -1 for normal file if there is no limit */

    } NAVRECORDMODE;

    /* Nav Record Status ******************************************************/

    typedef struct _tagNAVRECORDSTATUS {

        unsigned int elapsedTime;     /* recording elapsed time in seconds */
        unsigned int recordedTime;    /* amount of time recorded in file */
        unsigned int recordedBlocks;  /* number of blocks recorded in file */
        unsigned int totalFileBlocks; /* number of blocks the file can store */

    } NAVRECORDSTATUS;

	/* Nav Editing Parameters *********************************************/
	
	typedef struct _tagNAVEDITINGPARA
	{
		char*		 pDesPathName;  /* path name of the Target */
		unsigned int startTime;		/* the start time to execute the editing operation */
		unsigned int endTime;		/* the end time to execute the editing operation */
		
	} NAVEDITINGPARA;
	
	/* Nav Thumbnail Decode Command **************************************/
	
	typedef struct _tagNAVTHUMBNAILPARA
	{
		unsigned int colorFormat;    /* 0: IC_RGB332 , 1: IC_RGB565, 2: IC_RGBA8888, 3: IC_ARGB8888, 4:IC_YUV  */
		unsigned int pTargetLuma;    //Linear address of Luman buffer.
		unsigned int pTargetChroma;  //Linear address of Chroma buffer.
		unsigned int pitch;
		unsigned int targetRectX;
		unsigned int targetRectY;
		unsigned int targetRectWidth;
		unsigned int targetRectHeight;

		unsigned int timePosition;	/* time position (in seconds) of the thumbanil.
									 * this field is reserved for AP to specify time postion
									 * in case this information is stored in database.
									 */
	} NAVTHUMBNAILPARA;
	
	typedef struct _tagNAVMULTITHUMBNAILPARA
	{
		unsigned int numThumbnail;
		unsigned int maxDecFrameCount;		// a threshold to prevent video from taking too much time to find the best thumbnail
		unsigned char** pFileName;
		NAVTHUMBNAILPARA* pThumbnailPara;
		
	} NAVMULTITHUMBNAILPARA;

	typedef enum {

        NAV_PLAYBACK_NORMAL,
        NAV_PLAYBACK_KARAOKE,
        NAV_PLAYBACK_SINGLE_TRACK,	//For Audio CD

    } NAV_PLAYBACK_MODE;

/*    typedef struct _tagNAVLPCMINFO
    {
        int             bitsPerSample;
        int             samplingRate;
        int             numChannels;
        int64_t 	fileSize;
    } NAVLPCMINFO;*/

    typedef enum {
        NAV_FAILURE_UNIDENTIFIED,
        NAV_FAILURE_UNSUPPORTED_VIDEO_CODEC,
        NAV_FAILURE_UNSUPPORTED_AUDIO_CODEC,
        NAV_FAILURE_VIDEO_PIN_UPDATE_FAILURE,
        NAV_FAILURE_UNSUPPORTED_DIVX_DRM_VERSION,
		NAV_FAILURE_NO_LOAD_DRM,
    } NAV_FAILURE_REASON;

    typedef struct _tagNAVLOADFAILURE
    {
        NAV_FAILURE_REASON  reason;
        ENUM_MEDIA_TYPE     mediaType;
        ENUM_MEDIA_TYPE     videoType;
        unsigned int        fourCC; // would be zero if not available, for video only
        ENUM_MEDIA_TYPE     audioType;
    } NAVLOADFAILURE;

	//////////////////////////////////////////////////////////////
	typedef struct _tagNAVFLASHDECODECMD
	{
		int64_t				relativePTS;		//don't display video until pts is greater than or equal to relativePTS
		bool				bPauseAtDecodeTime; //if video should be paused at the time specified by relativePTS
	}NAVFLASHDECODECMD;
	
	typedef struct _tagNAVACETREXDATA
	{
		void * pAceTrexDeviceID;
		void * pAceTrexPurchaseID;
		void * pAceTrexRetailerID;
	}NAVACETREXDATA;


	////////////////////////////////////////////////////////////////////
	//For PrivateDataFeedback id = NAV_PRIVATE_HDMV_GT_PACKET
	typedef struct _tagNAVIGPACKET
	{
		int pid;
		unsigned char* pData;
		int             pDataSize;
		int             headerLen;
		int64_t         pts;
	} NAVGTPACKET;	//Graphic or Text Subtitle packets

	typedef enum{
		MENU_TYPE_HOME,
		MENU_TYPE_POP_UP,
		
	} NAV_MENU_TYPE;
	
	
	typedef struct _tagNAVMENUCONFIG
	{
		bool bEnable;	//if false, don't care menu display window config
		long x;			//menu display window position x
		long y;			//menu display window position y
		long width;		//menu display window width
		long height;	//menu display window height
		
	} NAVMENUCONFIG;
	
	typedef struct _tagNAVUSERDROP
	{
		long dataType;	//0 : all data, 1 : video data, 2 : audio data, 3 : spic data
		long stamp;
		long bResetKey; // 1 : reset IV and odd/even key
	} NAVUSERDROP;
	
	typedef struct _tagNAVSIHANDLE
	{
		unsigned int siHandle;
		void*        pAttachFunc;
		void*        pDetachFunc;
	} NAVSIHANDLE;

	typedef struct
	{
		int pid;
		int codecType;
	} DTV_AUDIO_FORMAT_FB;
	
	typedef struct
	{
		int level;		//mixing level. should fall in the rage [min ... max] 
		int min;		//min mixing level
		int max;		//max mixing level
		
	} NAVAUDIOMIXINGLEVEL;
	
	typedef struct
	{
		int64_t pcrBase;	//the first pcr
		int64_t stcBase;	//the time when receive the first pcr
		int64_t pcr;		//current pcr
		int64_t stc;		//current time
		
	} NAVDTVPCRINFO;

	typedef struct _tagNAVDTVDESINFO
	{
		int mode;       //refert to Platform_Lib/DTVFrontendControl/tv_fe_types.h : DESCRAMBLE_MODE
		int algo;       //refert to Platform_Lib/DTVFrontendControl/tv_fe_types.h : DESCRAMBLE_ALGORITHM_ID          
		int round;      //valid only if algo = DESCRAMBLE_ALGO_MULTI2

	} NAVDTVDESINFO;

#ifdef __cplusplus
}
#endif

#endif /*__NAV_DEF_H__*/
