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
 * $Workfile: DMR_MicroStack.h
 * $Revision: #1.0.2718.23851
 * $Author:   Intel Corporation, Intel Device Builder
 * $Date:     2007年7月6日
 *
 *
 *
 */
#ifndef __DMR_Microstack__
#define __DMR_Microstack__


#include "ILibAsyncSocket.h"

#ifdef __cplusplus
extern "C" {
#endif

/*! \file DMR_MicroStack.h 
	\brief MicroStack APIs for Device Implementation
*/

/*! \defgroup MicroStack MicroStack Module
	\{
*/

struct DMR_DataObject;
struct packetheader;

typedef void* DMR_MicroStackToken;
typedef void* DMR_SessionToken;




/* Complex Type Parsers */


/* Complex Type Serializers */



/* DMR_ Stack Management */
DMR_MicroStackToken DMR_CreateMicroStack(void *Chain, const char* FriendlyName,const char* UDN, const char* SerialNumber, const int NotifyCycleSeconds, const unsigned short PortNum);


void DMR_IPAddressListChanged(DMR_MicroStackToken MicroStackToken);
int DMR_GetLocalPortNumber(DMR_SessionToken token);
int   DMR_GetLocalInterfaceToHost(const DMR_SessionToken DMR_Token);
void* DMR_GetWebServerToken(const DMR_MicroStackToken MicroStackToken);
void DMR_SetTag(const DMR_MicroStackToken token, void *UserToken);
void *DMR_GetTag(const DMR_MicroStackToken token);
DMR_MicroStackToken DMR_GetMicroStackTokenFromSessionToken(const DMR_SessionToken token);

typedef void(*DMR__ActionHandler_AVTransport_GetCurrentTransportActions) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_GetDeviceCapabilities) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_GetMediaInfo) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_GetPositionInfo) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_GetTransportInfo) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_GetTransportSettings) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_Next) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_Pause) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_Play) (void* upnptoken,unsigned int InstanceID,char* Speed);
typedef void(*DMR__ActionHandler_AVTransport_Previous) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_AVTransport_Seek) (void* upnptoken,unsigned int InstanceID,char* Unit,char* Target);
typedef void(*DMR__ActionHandler_AVTransport_SetAVTransportURI) (void* upnptoken,unsigned int InstanceID,char* CurrentURI,char* CurrentURIMetaData);
typedef void(*DMR__ActionHandler_AVTransport_SetNextAVTransportURI) (void* upnptoken,unsigned int InstanceID,char* NextURI,char* NextURIMetaData);
typedef void(*DMR__ActionHandler_AVTransport_SetPlayMode) (void* upnptoken,unsigned int InstanceID,char* NewPlayMode);
typedef void(*DMR__ActionHandler_AVTransport_Stop) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_ConnectionManager_GetCurrentConnectionIDs) (void* upnptoken);
typedef void(*DMR__ActionHandler_ConnectionManager_GetCurrentConnectionInfo) (void* upnptoken,int ConnectionID);
typedef void(*DMR__ActionHandler_ConnectionManager_GetProtocolInfo) (void* upnptoken);
typedef void(*DMR__ActionHandler_RenderingControl_GetBrightness) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_RenderingControl_GetContrast) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_RenderingControl_GetMute) (void* upnptoken,unsigned int InstanceID,char* Channel);
typedef void(*DMR__ActionHandler_RenderingControl_GetVolume) (void* upnptoken,unsigned int InstanceID,char* Channel);
typedef void(*DMR__ActionHandler_RenderingControl_ListPresets) (void* upnptoken,unsigned int InstanceID);
typedef void(*DMR__ActionHandler_RenderingControl_SelectPreset) (void* upnptoken,unsigned int InstanceID,char* PresetName);
typedef void(*DMR__ActionHandler_RenderingControl_SetBrightness) (void* upnptoken,unsigned int InstanceID,unsigned short DesiredBrightness);
typedef void(*DMR__ActionHandler_RenderingControl_SetContrast) (void* upnptoken,unsigned int InstanceID,unsigned short DesiredContrast);
typedef void(*DMR__ActionHandler_RenderingControl_SetMute) (void* upnptoken,unsigned int InstanceID,char* Channel,int DesiredMute);
typedef void(*DMR__ActionHandler_RenderingControl_SetVolume) (void* upnptoken,unsigned int InstanceID,char* Channel,unsigned short DesiredVolume);
/* DMR_ Set Function Pointers Methods */
extern void (*DMR_FP_PresentationPage) (void* upnptoken,struct packetheader *packet);
extern DMR__ActionHandler_AVTransport_GetCurrentTransportActions DMR_FP_AVTransport_GetCurrentTransportActions;
extern DMR__ActionHandler_AVTransport_GetDeviceCapabilities DMR_FP_AVTransport_GetDeviceCapabilities;
extern DMR__ActionHandler_AVTransport_GetMediaInfo DMR_FP_AVTransport_GetMediaInfo;
extern DMR__ActionHandler_AVTransport_GetPositionInfo DMR_FP_AVTransport_GetPositionInfo;
extern DMR__ActionHandler_AVTransport_GetTransportInfo DMR_FP_AVTransport_GetTransportInfo;
extern DMR__ActionHandler_AVTransport_GetTransportSettings DMR_FP_AVTransport_GetTransportSettings;
extern DMR__ActionHandler_AVTransport_Next DMR_FP_AVTransport_Next;
extern DMR__ActionHandler_AVTransport_Pause DMR_FP_AVTransport_Pause;
extern DMR__ActionHandler_AVTransport_Play DMR_FP_AVTransport_Play;
extern DMR__ActionHandler_AVTransport_Previous DMR_FP_AVTransport_Previous;
extern DMR__ActionHandler_AVTransport_Seek DMR_FP_AVTransport_Seek;
extern DMR__ActionHandler_AVTransport_SetAVTransportURI DMR_FP_AVTransport_SetAVTransportURI;
extern DMR__ActionHandler_AVTransport_SetNextAVTransportURI DMR_FP_AVTransport_SetNextAVTransportURI;

extern DMR__ActionHandler_AVTransport_SetPlayMode DMR_FP_AVTransport_SetPlayMode;
extern DMR__ActionHandler_AVTransport_Stop DMR_FP_AVTransport_Stop;
extern DMR__ActionHandler_ConnectionManager_GetCurrentConnectionIDs DMR_FP_ConnectionManager_GetCurrentConnectionIDs;
extern DMR__ActionHandler_ConnectionManager_GetCurrentConnectionInfo DMR_FP_ConnectionManager_GetCurrentConnectionInfo;
extern DMR__ActionHandler_ConnectionManager_GetProtocolInfo DMR_FP_ConnectionManager_GetProtocolInfo;
extern DMR__ActionHandler_RenderingControl_GetBrightness DMR_FP_RenderingControl_GetBrightness;
extern DMR__ActionHandler_RenderingControl_GetContrast DMR_FP_RenderingControl_GetContrast;
extern DMR__ActionHandler_RenderingControl_GetMute DMR_FP_RenderingControl_GetMute;
extern DMR__ActionHandler_RenderingControl_GetVolume DMR_FP_RenderingControl_GetVolume;
extern DMR__ActionHandler_RenderingControl_ListPresets DMR_FP_RenderingControl_ListPresets;
extern DMR__ActionHandler_RenderingControl_SelectPreset DMR_FP_RenderingControl_SelectPreset;
extern DMR__ActionHandler_RenderingControl_SetBrightness DMR_FP_RenderingControl_SetBrightness;
extern DMR__ActionHandler_RenderingControl_SetContrast DMR_FP_RenderingControl_SetContrast;
extern DMR__ActionHandler_RenderingControl_SetMute DMR_FP_RenderingControl_SetMute;
extern DMR__ActionHandler_RenderingControl_SetVolume DMR_FP_RenderingControl_SetVolume;


void DMR_SetDisconnectFlag(DMR_SessionToken token,void *flag);

/* Invocation Response Methods */
void DMR_Response_Error(const DMR_SessionToken DMR_Token, const int ErrorCode, const char* ErrorMsg);
void DMR_ResponseGeneric(const DMR_SessionToken DMR_Token,const char* ServiceURI,const char* MethodName,const char* Params);
void DMR_Response_AVTransport_GetCurrentTransportActions(const DMR_SessionToken DMR_Token, const char* Actions);
void DMR_Response_AVTransport_GetDeviceCapabilities(const DMR_SessionToken DMR_Token, const char* PlayMedia, const char* RecMedia, const char* RecQualityModes);
void DMR_Response_AVTransport_GetMediaInfo(const DMR_SessionToken DMR_Token, const unsigned int NrTracks, const char* MediaDuration, const char* CurrentURI, const char* CurrentURIMetaData, const char* NextURI, const char* NextURIMetaData, const char* PlayMedium, const char* RecordMedium, const char* WriteStatus);
void DMR_Response_AVTransport_GetPositionInfo(const DMR_SessionToken DMR_Token, const unsigned int Track, const char* TrackDuration, const char* TrackMetaData, const char* TrackURI, const char* RelTime, const char* AbsTime, const int RelCount, const int AbsCount);
void DMR_Response_AVTransport_GetTransportInfo(const DMR_SessionToken DMR_Token, const char* CurrentTransportState, const char* CurrentTransportStatus, const char* CurrentSpeed);
void DMR_Response_AVTransport_GetTransportSettings(const DMR_SessionToken DMR_Token, const char* PlayMode, const char* RecQualityMode);
void DMR_Response_AVTransport_Next(const DMR_SessionToken DMR_Token);
void DMR_Response_AVTransport_Pause(const DMR_SessionToken DMR_Token);
void DMR_Response_AVTransport_Play(const DMR_SessionToken DMR_Token);
void DMR_Response_AVTransport_Previous(const DMR_SessionToken DMR_Token);
void DMR_Response_AVTransport_Seek(const DMR_SessionToken DMR_Token);
void DMR_Response_AVTransport_SetAVTransportURI(const DMR_SessionToken DMR_Token);
void DMR_Response_AVTransport_SetPlayMode(const DMR_SessionToken DMR_Token);
void DMR_Response_AVTransport_Stop(const DMR_SessionToken DMR_Token);
void DMR_Response_ConnectionManager_GetCurrentConnectionIDs(const DMR_SessionToken DMR_Token, const char* ConnectionIDs);
void DMR_Response_ConnectionManager_GetCurrentConnectionInfo(const DMR_SessionToken DMR_Token, const int RcsID, const int AVTransportID, const char* ProtocolInfo, const char* PeerConnectionManager, const int PeerConnectionID, const char* Direction, const char* Status);
void DMR_Response_ConnectionManager_GetProtocolInfo(const DMR_SessionToken DMR_Token, const char* Source, const char* Sink);
void DMR_Response_RenderingControl_GetBrightness(const DMR_SessionToken DMR_Token, const unsigned short CurrentBrightness);
void DMR_Response_RenderingControl_GetContrast(const DMR_SessionToken DMR_Token, const unsigned short CurrentContrast);
void DMR_Response_RenderingControl_GetMute(const DMR_SessionToken DMR_Token, const int CurrentMute);
void DMR_Response_RenderingControl_GetVolume(const DMR_SessionToken DMR_Token, const unsigned short CurrentVolume);
void DMR_Response_RenderingControl_ListPresets(const DMR_SessionToken DMR_Token, const char* CurrentPresetNameList);
void DMR_Response_RenderingControl_SelectPreset(const DMR_SessionToken DMR_Token);
void DMR_Response_RenderingControl_SetBrightness(const DMR_SessionToken DMR_Token);
void DMR_Response_RenderingControl_SetContrast(const DMR_SessionToken DMR_Token);
void DMR_Response_RenderingControl_SetMute(const DMR_SessionToken DMR_Token);
void DMR_Response_RenderingControl_SetVolume(const DMR_SessionToken DMR_Token);

/* State Variable Eventing Methods */
void DMR_SetState_AVTransport_LastChange(DMR_MicroStackToken microstack,char* val);
void DMR_SetState_ConnectionManager_SourceProtocolInfo(DMR_MicroStackToken microstack,char* val);
void DMR_SetState_ConnectionManager_SinkProtocolInfo(DMR_MicroStackToken microstack,char* val);
void DMR_SetState_ConnectionManager_CurrentConnectionIDs(DMR_MicroStackToken microstack,char* val);
void DMR_SetState_RenderingControl_LastChange(DMR_MicroStackToken microstack,char* val);


#define DMR__StateVariable_AllowedValues_MAX 12
#define DMR__StateVaribale_SeekMode_Limitation 1
struct DMR__StateVariableTable_AVTransport
{
	char Reserved[1010];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__StateVariable_AVTransport_CurrentPlayMode
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_RecordStorageMedium
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_LastChange
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_RelativeTimePosition
{
	int Reserved1;
	int Reserved1L;
	int Reserved4;
	int Reserved4L;
	int Reserved5;
	int Reserved5L;
	char *MinMaxStep[3];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_CurrentTrackURI
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_CurrentTrackDuration
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_CurrentRecordQualityMode
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_CurrentMediaDuration
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_AbsoluteCounterPosition
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_RelativeCounterPosition
{
	int Reserved1;
	int Reserved1L;
	int Reserved4;
	int Reserved4L;
	int Reserved5;
	int Reserved5L;
	char *MinMaxStep[3];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_A_ARG_TYPE_InstanceID
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_AVTransportURI
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_TransportState
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_CurrentTrackMetaData
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_NextAVTransportURI
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_PossibleRecordQualityModes
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_CurrentTrack
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	char *MinMaxStep[3]; /* Added by yuyu for DLNA 1.5 CTT 7.3.6.1 */
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_AbsoluteTimePosition
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_NextAVTransportURIMetaData
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_PlaybackStorageMedium
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_CurrentTransportActions
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_RecordMediumWriteStatus
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_PossiblePlaybackStorageMedia
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_AVTransportURIMetaData
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_NumberOfTracks
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	char *MinMax[2]; /* Added by yuyu for DLNA 1.5 CTT 7.3.6.1 */
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekMode
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekTarget
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_PossibleRecordStorageMedia
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_TransportStatus
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_AVTransport_TransportPlaySpeed
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariableTable_ConnectionManager
{
	char Reserved[377];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ProtocolInfo
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionStatus
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_AVTransportID
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_RcsID
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionID
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionManager
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_SourceProtocolInfo
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_SinkProtocolInfo
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_Direction
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_ConnectionManager_CurrentConnectionIDs
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariableTable_RenderingControl
{
	char Reserved[356];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__StateVariable_RenderingControl_Contrast
{
	int Reserved1;
	int Reserved1L;
	int Reserved4;
	int Reserved4L;
	int Reserved5;
	int Reserved5L;
	char *MinMaxStep[3];
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_LastChange
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_Brightness
{
	int Reserved1;
	int Reserved1L;
	int Reserved4;
	int Reserved4L;
	int Reserved5;
	int Reserved5L;
	char *MinMaxStep[3];
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_Volume
{
	int Reserved1;
	int Reserved1L;
	int Reserved4;
	int Reserved4L;
	int Reserved5;
	int Reserved5L;
	char *MinMaxStep[3];
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_PresetName
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_PresetNameList
{
	int Reserved1;
	int Reserved1L;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_Mute
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_InstanceID
{
	int Reserved1;
	int Reserved1L;
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_Channel
{
	int Reserved1;
	int Reserved1L;
	int Reserved2;
	int Reserved2L;
	int Reserved3;
	int Reserved3L;
	char *AllowedValues[DMR__StateVariable_AllowedValues_MAX];
	int Reserved6;
	int Reserved6L;
	int Reserved7;
	int Reserved7L;
	char *DefaultValue;
	int Reserved8;
	int Reserved8L;
};
struct DMR__ActionTable_AVTransport
{
	char Reserved[1125];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__Action_AVTransport_GetCurrentTransportActions
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_GetDeviceCapabilities
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_GetMediaInfo
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_GetPositionInfo
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_GetTransportInfo
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_GetTransportSettings
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_Next
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_Pause
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_Play
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_Previous
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_Seek
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_SetAVTransportURI
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_SetPlayMode
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_AVTransport_Stop
{
	int Reserved;
	int Reserved2;
};
struct DMR__ActionTable_ConnectionManager
{
	char Reserved[342];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__Action_ConnectionManager_GetCurrentConnectionIDs
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_ConnectionManager_GetCurrentConnectionInfo
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_ConnectionManager_GetProtocolInfo
{
	int Reserved;
	int Reserved2;
};
struct DMR__ActionTable_RenderingControl
{
	char Reserved[497];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__Action_RenderingControl_GetBrightness
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_GetContrast
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_GetMute
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_GetVolume
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_ListPresets
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_SelectPreset
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_SetBrightness
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_SetContrast
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_SetMute
{
	int Reserved;
	int Reserved2;
};
struct DMR__Action_RenderingControl_SetVolume
{
	int Reserved;
	int Reserved2;
};
struct DMR__Service_AVTransport
{
	struct DMR__Action_AVTransport_GetCurrentTransportActions *GetCurrentTransportActions;
	struct DMR__Action_AVTransport_GetDeviceCapabilities *GetDeviceCapabilities;
	struct DMR__Action_AVTransport_GetMediaInfo *GetMediaInfo;
	struct DMR__Action_AVTransport_GetPositionInfo *GetPositionInfo;
	struct DMR__Action_AVTransport_GetTransportInfo *GetTransportInfo;
	struct DMR__Action_AVTransport_GetTransportSettings *GetTransportSettings;
	struct DMR__Action_AVTransport_Next *Next;
	struct DMR__Action_AVTransport_Pause *Pause;
	struct DMR__Action_AVTransport_Play *Play;
	struct DMR__Action_AVTransport_Previous *Previous;
	struct DMR__Action_AVTransport_Seek *Seek;
	struct DMR__Action_AVTransport_SetAVTransportURI *SetAVTransportURI;
	struct DMR__Action_AVTransport_SetPlayMode *SetPlayMode;
	struct DMR__Action_AVTransport_Stop *Stop;
	
	struct DMR__StateVariable_AVTransport_CurrentPlayMode *StateVar_CurrentPlayMode;
	struct DMR__StateVariable_AVTransport_RecordStorageMedium *StateVar_RecordStorageMedium;
	struct DMR__StateVariable_AVTransport_LastChange *StateVar_LastChange;
	struct DMR__StateVariable_AVTransport_RelativeTimePosition *StateVar_RelativeTimePosition;
	struct DMR__StateVariable_AVTransport_CurrentTrackURI *StateVar_CurrentTrackURI;
	struct DMR__StateVariable_AVTransport_CurrentTrackDuration *StateVar_CurrentTrackDuration;
	struct DMR__StateVariable_AVTransport_CurrentRecordQualityMode *StateVar_CurrentRecordQualityMode;
	struct DMR__StateVariable_AVTransport_CurrentMediaDuration *StateVar_CurrentMediaDuration;
	struct DMR__StateVariable_AVTransport_AbsoluteCounterPosition *StateVar_AbsoluteCounterPosition;
	struct DMR__StateVariable_AVTransport_RelativeCounterPosition *StateVar_RelativeCounterPosition;
	struct DMR__StateVariable_AVTransport_A_ARG_TYPE_InstanceID *StateVar_A_ARG_TYPE_InstanceID;
	struct DMR__StateVariable_AVTransport_AVTransportURI *StateVar_AVTransportURI;
	struct DMR__StateVariable_AVTransport_TransportState *StateVar_TransportState;
	struct DMR__StateVariable_AVTransport_CurrentTrackMetaData *StateVar_CurrentTrackMetaData;
	struct DMR__StateVariable_AVTransport_NextAVTransportURI *StateVar_NextAVTransportURI;
	struct DMR__StateVariable_AVTransport_PossibleRecordQualityModes *StateVar_PossibleRecordQualityModes;
	struct DMR__StateVariable_AVTransport_CurrentTrack *StateVar_CurrentTrack;
	struct DMR__StateVariable_AVTransport_AbsoluteTimePosition *StateVar_AbsoluteTimePosition;
	struct DMR__StateVariable_AVTransport_NextAVTransportURIMetaData *StateVar_NextAVTransportURIMetaData;
	struct DMR__StateVariable_AVTransport_PlaybackStorageMedium *StateVar_PlaybackStorageMedium;
	struct DMR__StateVariable_AVTransport_CurrentTransportActions *StateVar_CurrentTransportActions;
	struct DMR__StateVariable_AVTransport_RecordMediumWriteStatus *StateVar_RecordMediumWriteStatus;
	struct DMR__StateVariable_AVTransport_PossiblePlaybackStorageMedia *StateVar_PossiblePlaybackStorageMedia;
	struct DMR__StateVariable_AVTransport_AVTransportURIMetaData *StateVar_AVTransportURIMetaData;
	struct DMR__StateVariable_AVTransport_NumberOfTracks *StateVar_NumberOfTracks;
	struct DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekMode *StateVar_A_ARG_TYPE_SeekMode;
	struct DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekTarget *StateVar_A_ARG_TYPE_SeekTarget;
	struct DMR__StateVariable_AVTransport_PossibleRecordStorageMedia *StateVar_PossibleRecordStorageMedia;
	struct DMR__StateVariable_AVTransport_TransportStatus *StateVar_TransportStatus;
	struct DMR__StateVariable_AVTransport_TransportPlaySpeed *StateVar_TransportPlaySpeed;
	
	char Reserved[190];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__Service_ConnectionManager
{
	struct DMR__Action_ConnectionManager_GetCurrentConnectionIDs *GetCurrentConnectionIDs;
	struct DMR__Action_ConnectionManager_GetCurrentConnectionInfo *GetCurrentConnectionInfo;
	struct DMR__Action_ConnectionManager_GetProtocolInfo *GetProtocolInfo;
	
	struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ProtocolInfo *StateVar_A_ARG_TYPE_ProtocolInfo;
	struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionStatus *StateVar_A_ARG_TYPE_ConnectionStatus;
	struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_AVTransportID *StateVar_A_ARG_TYPE_AVTransportID;
	struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_RcsID *StateVar_A_ARG_TYPE_RcsID;
	struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionID *StateVar_A_ARG_TYPE_ConnectionID;
	struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionManager *StateVar_A_ARG_TYPE_ConnectionManager;
	struct DMR__StateVariable_ConnectionManager_SourceProtocolInfo *StateVar_SourceProtocolInfo;
	struct DMR__StateVariable_ConnectionManager_SinkProtocolInfo *StateVar_SinkProtocolInfo;
	struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_Direction *StateVar_A_ARG_TYPE_Direction;
	struct DMR__StateVariable_ConnectionManager_CurrentConnectionIDs *StateVar_CurrentConnectionIDs;
	
	char Reserved[196];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__Service_RenderingControl
{
	struct DMR__Action_RenderingControl_GetBrightness *GetBrightness;
	struct DMR__Action_RenderingControl_GetContrast *GetContrast;
	struct DMR__Action_RenderingControl_GetMute *GetMute;
	struct DMR__Action_RenderingControl_GetVolume *GetVolume;
	struct DMR__Action_RenderingControl_ListPresets *ListPresets;
	struct DMR__Action_RenderingControl_SelectPreset *SelectPreset;
	struct DMR__Action_RenderingControl_SetBrightness *SetBrightness;
	struct DMR__Action_RenderingControl_SetContrast *SetContrast;
	struct DMR__Action_RenderingControl_SetMute *SetMute;
	struct DMR__Action_RenderingControl_SetVolume *SetVolume;
	
	struct DMR__StateVariable_RenderingControl_Contrast *StateVar_Contrast;
	struct DMR__StateVariable_RenderingControl_LastChange *StateVar_LastChange;
	struct DMR__StateVariable_RenderingControl_Brightness *StateVar_Brightness;
	struct DMR__StateVariable_RenderingControl_Volume *StateVar_Volume;
	struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_PresetName *StateVar_A_ARG_TYPE_PresetName;
	struct DMR__StateVariable_RenderingControl_PresetNameList *StateVar_PresetNameList;
	struct DMR__StateVariable_RenderingControl_Mute *StateVar_Mute;
	struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_InstanceID *StateVar_A_ARG_TYPE_InstanceID;
	struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_Channel *StateVar_A_ARG_TYPE_Channel;
	
	char Reserved[192];
	int ReservedXL;
	int ReservedUXL;
};
struct DMR__Device_MediaRenderer
{
	struct DMR__Service_AVTransport *AVTransport;
	struct DMR__Service_ConnectionManager *ConnectionManager;
	struct DMR__Service_RenderingControl *RenderingControl;
	
	const char *FriendlyName;
	const char *UDN;
	const char *Serial;
	const char *Manufacturer;
	const char *ManufacturerURL;
	const char *ModelDescription;
	const char *ModelName;
	const char *ModelNumber;
	const char *ModelURL;
	const char *ProductCode;
	char Reserved[439];
	int ReservedXL;
	int ReservedUXL;
	void *User;
	void *MicrostackToken;
};

struct DMR__Device_MediaRenderer* DMR_GetConfiguration();

#ifdef __cplusplus
};
#endif

/*! \} */
#endif
