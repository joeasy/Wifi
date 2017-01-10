/*
 * INTEL CONFIDENTIAL
 * Copyright (c) 2002 - 2005 Intel Corporation.  All rights reserved.
 * 
 * The source code contained or described herein and all documents
 * related to the source code ("Material") are owned by Intel
 * Corporation or its suppliers or licensors.  Title to the
 * Material remains with Intel Corporation or its suppliers and
 * licensors.  The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and
 * licensors. The Material is protected by worldwide copyright and
 * trade secret laws and treaty provisions.  No part of the Material
 * may be used, copied, reproduced, modified, published, uploaded,
 * posted, transmitted, distributed, or disclosed in any way without
 * Intel's prior express written permission.
 
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by Intel in writing.
 * 
 * $Workfile: $
 * $Revision: 1.6 $
 * $Author: pdamons $
 * $Date: 2006/11/17 22:11:39 $
 *
 */

#if !defined(__DMRCOMMON_H__)
#define __DMRCOMMON_H__

#include "ILibParsers.h"

/**
    \file DMRCommon.h
    \ingroup DLNADMR
	\brief DLNA MediaRenderer Common APIs

    This file provides the enums and typedefs used in multiple *.c and *.h files.

    \addtogroup DLNADMR DLNA MediaRenderer Microstack (DMR)
    \{
*/

#if !defined(WIN32)
/** \brief Type for boolean type in C. */
#ifdef __cplusplus

#ifndef BOOL
#define BOOL bool
#endif

#ifndef TRUE
#define TRUE true
#endif

#ifndef FALSE
#define FALSE false
#endif

#else

#ifndef BOOL
#define BOOL int
#endif

/** \brief This is a true boolean value (1). */
#ifndef TRUE
#define TRUE	1
#endif

/** \brief This is a false boolean value (0). */
#ifndef FALSE
#define FALSE	0
#endif

#endif
#endif

/** \brief Play state commands send from the DMR microstack to the vendor's renderer.
*/
typedef enum
{
    /** \brief Request the STOPPED state. */
	DMR_PC_Stop = 0,
    /** \brief Request the PLAY state. */
	DMR_PC_Play = 1,
    /** \brief Request the PAUSED state. */
	DMR_PC_Pause = 2
} DMR_PlayCommand;


/** \brief Play state flags sent from the vendor's application to the DMR microstack.

    Treat this as a set of flags stored in an \c int.
*/
typedef enum
{
	/** \brief Added by yuyu for DLNA 1.5 CTT 7.3.T.2 */
	DMR_ATS_Reset = 0,
    /** \brief The Play PlayCommand will be accepted. */
	DMR_ATS_Play = 1,
    /** \brief The Stop PlayCommand will be accepted. */
	DMR_ATS_Stop = 2,
    /** \brief The Pause PlayCommand will be accepted. */
	DMR_ATS_Pause = 4,
    /** \brief The Seek transition will be accepted. */
	DMR_ATS_Seek = 8,
    /** \brief The Next transition will be accepted. */
	DMR_ATS_Next = 16,
    /** \brief The Previous transition will be accepted. */
	DMR_ATS_Previous = 32
} DMR_AllowedTransportActions;


/** \brief Play states sent from the vendor's renderer implementation to the DMR microstack.
*/
typedef enum
{
    /** \brief PlayState representing the NO_MEDIA_PRESENT state. */
	DMR_PS_NoMedia = 0,
    /** \brief PlayState representing the STOPPED state. */
	DMR_PS_Stopped = 1,
    /** \brief PlayState representing the PAUSED_PLAYBACK state. */
	DMR_PS_Paused = 2,
    /** \brief PlayState representing the PLAYING state. */
	DMR_PS_Playing = 3,
    /** \brief PlayState representing the TRANSITIONING state. */
	DMR_PS_Transitioning = 4
} DMR_PlayState;


/** \brief Media play modes sent from the vendor's renderer implementation
    to the DMR microstack or from the UPnP level to the vendor.
*/
typedef enum
{
    /** \brief PlayMode representing the NORMAL play mode. */
    DMR_MPM_Normal = 0,
    /** \brief PlayMode representing the REPEAT_ONE play mode. */
    DMR_MPM_Repeat_One = 1,
    /** \brief PlayMode representing the REPEAT_ALL play mode. */
    DMR_MPM_Repeat_All = 2,
    /** \brief PlayMode representing the RANDOM play mode. */
    DMR_MPM_Random = 3,
    /** \brief PlayMode representing the SHUFFLE play mode. */
    DMR_MPM_Shuffle = 4,
    /** \brief PlayMode representing the DIRECT_1 play mode. */
    DMR_MPM_Direct_One = 5,
    /** \brief PlayMode representing the INTRO play mode. */
    DMR_MPM_Intro = 6
} DMR_MediaPlayMode;


/** \brief Transport status sent from the vendor's renderer implementation
    to the DMR microstack.
*/
typedef enum
{
    /** \brief The state of the AV transport is OK. */
    DMR_TS_OK = 0,
    /** \brief The state of the AV transport is ERROR_OCCURRED. */
    DMR_TS_ERROR_OCCURRED = 1
} DMR_TransportStatus;


/** \brief Errors returned from the vendor defined callbacks.
*/
typedef enum
{
    /** \brief There is no error. */
    DMR_ERROR_OK = 0,
    /** \brief The This pointer for the pseudo class is not valid. */
    DMR_ERROR_BADTHIS = -1,
    /** \brief The internal pointer for the pseudo class's private variables is not valid. */
    DMR_ERROR_BADINTERNALSTATE = -2,
    /** \brief The DMR is not currently running. */
    DMR_ERROR_NOTRUNNING = -3,
    /** \brief The play list is bad and cannot be played. */
    DMR_ERROR_BADPLAYLIST = -4,
    /** \brief The play list is too large and cannot be played. */
    DMR_ERROR_PLAYLISTTOOLARGE = -5,
    /** \brief The specified track index is out of range. */
    DMR_ERROR_TRACKINDEXOUTOFRANGE = -6,
    /** \brief The specified new play state of transition is not supported from this state. */
    DMR_ERROR_TRANSITIONNOTSUPPORTED = -7,
    /** \brief The specified new volume is out of range. */
    DMR_ERROR_VOLUMEOUTOFRANGE = -8,
    /** \brief The specified new contrast is out of range. */
    DMR_ERROR_CONTRASTOUTOFRANGE = -9,
    /** \brief The specified new brightness is out of range. */
    DMR_ERROR_BRIGHTNESSOUTOFRANGE = -10,
    /** \brief The specified transport URI was rejected by the application. */
    DMR_ERROR_REJECTURI = -11,
    /** \brief The specified transport URI metadata was rejected by the application. */
    DMR_ERROR_REJECTMETADATA = -12,
    /** \brief The specified transport URI is not a valid URI. */
    DMR_ERROR_BADURI = -13,
    /** \brief The specified transport URI metadata is not a valid. */
    DMR_ERROR_BADMETADATA = -14,
    /** \brief The specified play state is not a valid play state. */
    DMR_ERROR_BADPLAYSTATE = -15,
    /** \brief The specified record state is not a valid record state. */
    DMR_ERROR_BADRECORDSTATE = -16,
    /** \brief The specified media play mode command is not a valid. */
    DMR_ERROR_BADMEDIAPLAYMODE = -17,
    /** \brief The specified transport status is not valid. */
    DMR_ERROR_BADTRANSPORTSTATUS = -18,
    /** \brief The specified play command is not a valid play command. */
    DMR_ERROR_BADPLAYCOMMAND = -19,
    /** \brief The specified record command is not a valid record command. */
    DMR_ERROR_BADRECORDCOMMAND = -20,
    /** \brief No thread pool was supplied by the application for use in the DMR. */
    DMR_ERROR_NOTHREADPOOL = -21,
    /** \brief A passed argument was invalid. */
    DMR_ERROR_INVALIDARGUMENT = -22,
    /** \brief The DMR failed to allocate memory on the heap. */
    DMR_ERROR_OUTOFMEMORY = -23,
    /** \brief The playspeed was invalid with the given play command */
    DMR_ERROR_BADPLAYSPEED = -24
} DMR_Error;

/** \brief Bit masks for determining if the DMR event should be executed
    on a seperate thread.
*/
typedef enum
{
    DMR_ECS_SETAVTRANSPORTURI   =   0x0001,
    DMR_ECS_STOP                =   0x0002,
    DMR_ECS_PLAY                =   0x0004,
    DMR_ECS_PAUSE               =   0x0008,
    DMR_ECS_SEEKTRACK           =   0x0010,
    DMR_ECS_SEEKTRACKTIME       =   0x0020,
    DMR_ECS_SEEKMEDIATIME       =   0x0040,
    DMR_ECS_NEXT                =   0x0080,
    DMR_ECS_PREVIOUS            =   0x0100,
    DMR_ECS_SETPLAYMODE         =   0x0200,
    DMR_ECS_SELECTPRESET        =   0x0400,
#if defined(INCLUDE_FEATURE_DISPLAY)
    DMR_ECS_SETBRIGHTNESS       =   0x0800,
    DMR_ECS_SETCONTRAST         =   0x1000,
#endif /* INCLUDE_FEATURE_DISPLAY */
#if defined(INCLUDE_FEATURE_VOLUME)
    DMR_ECS_SETVOLUME           =   0x2000,
    DMR_ECS_SETMUTE             =   0x4000,
#endif /* INCLUDE_FEATURE_VOLUME */
	DMR_ECS_GETAVPROTOCOLINFO	=	0x8000,
	DMR_ECS_SETNEXTAVTRANSPORTURI   =   0x10000,
    DMR_ECS_DEFAULT             =   0xffffffff /* ALL events are context switched! */

} DMR_EventContextSwitch;

/* Memory allocation macros that clear memory to zero and destroy memory iff the pointer is not NULL. */
/** \brief allocate memory and clear it. */
#define MALLOC(x) memset(malloc((size_t)x),0,(size_t)x)
/** \brief free memory if the pointer is not NULL. */
#define FREE(x) if(x!=NULL)free(x)

/* String creation and destruction macros. */
/** \brief Create a clone of the specified char* string. */
#define String_Create(x)		(char*)strcpy((char*)malloc(strlen(x)+1),x)
/** \brief Create a empty char* string adding 1 for the '\\0' value. */
#define String_CreateSize(x)	(char*)memset(malloc((size_t)(x+1)),0,1)
/** \brief Destroys a string if it is not NULL. */
#define String_Destroy(x)		FREE(x)

/**
\}
*/
#endif /* __DMRCOMMON_H__ */
