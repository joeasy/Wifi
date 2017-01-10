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
 * $Revision: 1.21 $
 * $Author: byroe $
 * $Date: 2007/03/16 21:00:31 $
 *
 */

#if defined(WIN32)
	#define _CRTDBG_MAP_ALLOC
#endif

/* DMR related includes */
#include "DMR.h"
#include "CdsDidlSerializer.h"


/* Optimization: No code for the printfs is generated in RELEASE mode builds */
//#if defined(WIN32) && defined(_DEBUG)
#if 0
#define ERROROUT1    printf
#define ERROROUT2    printf
#define ERROROUT3    printf
#define ERROROUT4    printf
#define ERROROUT5    printf
#define ERROROUT6    printf
#define ERROROUT7    printf
#define ERROROUT8    printf
#define ERROROUT9    printf
#else
#define ERROROUT1(p1)
#define ERROROUT2(p1,p2)
#define ERROROUT3(p1,p2,p3)
#define ERROROUT4(p1,p2,p3,p4) 
#define ERROROUT5(p1,p2,p3,p4,p5)
#define ERROROUT6(p1,p2,p3,p4,p5,p6)
#define ERROROUT7(p1,p2,p3,p4,p5,p6,p7)
#define ERROROUT8(p1,p2,p3,p4,p5,p6,p7,p8)
#define ERROROUT9(p1,p2,p3,p4,p5,p6,p7,p8,p9)
#endif /* WIN32 && !DEBUG */

#define OutputDebugString(x)

/****************************************************************************/
/* Global Fields */
/****************************************************************************/

#ifdef DLNADMRCTT
int DLNADMR_bPlayingAV;
int DLNADMR_bSupportSF;
int DLNADMR_bSupportTrickmode;
int DLNADMR_bSupportFBFFSBSF;
#endif

/****************************************************************************/
/* Event Mask Fields */
#define EVENT_CONTRAST                          0x00000001
#define EVENT_BRIGHTNESS                        0x00000002
#define EVENT_VOLUME                            0x00000004
#define EVENT_MUTE                              0x00000008
#define EVENT_TRANSPORTSTATE                    0x00000010
#define EVENT_TRANSPORTSTATUS                   0x00000020
#define EVENT_CURRENTPLAYMODE                   0x00000040
#define EVENT_TRANSPORTPLAYSPEED                0x00000080
#define EVENT_NUMBEROFTRACKS                    0x00000100
#define EVENT_CURRENTTRACK                      0x00000200
#define EVENT_CURRENTTRACKDURATION              0x00000400
#define EVENT_CURRENTMEDIADURATION              0x00000800
#define EVENT_CURRENTTRACKURI                   0x00001000
#define EVENT_CURRENTTRACKMETADATA              0x00002000
#define EVENT_AVTRANSPORTURI                    0x00004000
#define EVENT_AVTRANSPORTURIMETADATA            0x00008000
#define EVENT_CURRENTTRANSPORTACTIONS           0x00010000
#define EVENT_ABSOLUTETIMEPOSITION              0x00020000
#define EVENT_RELATIVETIMEPOSITION              0x00040000
#define EVENT_PRESETNAMELIST                    0x00080000// Added by yuyu for DLNA 1.5 CTT 7.3.93.1
#define EVENT_AVTOTHERS                         0x00100000// Added by yuyu for DLNA 1.5 CTT 7.3.93.1
#define EVENT_NEXTAVTRANSPORTURI                0x00200000// add by yunfeng for SetNextAVTransportURI
#define EVENT_NEXTAVTRANSPORTURIMETADATA        0x00400000// add by yunfeng for SetNextAVTransportURI


#define EVENT_ALL_RENDERINGCONTROL              0x0000000F
#define EVENT_ALL_AVTRANSPORT                   0x0077FFF0 // 0x0017FFF0  
#define EVENT_MASK_RENDERINGCONTROL             EVENT_ALL_RENDERINGCONTROL
#define EVENT_MASK_AVTRANSPORT                  EVENT_ALL_AVTRANSPORT                   
/****************************************************************************/


/****************************************************************************/
/* Internal State of an DMR Instance */
typedef struct _internalState
{
	/* UPnP Internal State */
    void *DMR_microStackChain;
    void *DMR_microStack;
    void *DMR_Monitor;

    /* Private UPnP fields */
    char* udn;
    unsigned short seconds;
    unsigned short port;

	/* Static for given instance of DMR. */
    char* FriendlyName;
    char* SerialNumber;
    char* ProtocolInfo;
    DMR_EventContextSwitch  EventsOnThreadBitMask;

    /**********************************************************************/
	/* Private State Variable Storage Fields for LastChange Eventing Only */
    unsigned long LastChangeMask;
    struct timeval LastChangeTime;
#if defined(INCLUDE_FEATURE_DISPLAY)
    unsigned short Brightness;
    unsigned short Contrast;
#endif /* INCLUDE_FEATURE_DISPLAY */
#if defined(INCLUDE_FEATURE_VOLUME)
    unsigned short Volume;
    BOOL Mute;
#endif /* INCLUDE_FEATURE_DISPLAY */
    DMR_MediaPlayMode CurrentPlayMode;
    DMR_PlayState TransportState;
    DMR_TransportStatus TransportStatus;
    char* TransportPlaySpeed;
    char* AVTransportURI;
    char* AVTransportURIMetaData;
	
	//add for SetNextAVTransportURI
	char* NextAVTransportURI;
	char* NextAVTransportURIMetaData;
	
    unsigned short CurrentTransportActions;
    long CurrentMediaDuration; /* in ms. */
    unsigned int NumberOfTracks;
    unsigned int CurrentTrack;
    long CurrentTrackDuration; /* in ms. */
    char* CurrentTrackMetaData;
    char* CurrentTrackURI;
    long AbsoluteTimePosition; /* in ms. */
    long RelativeTimePosition; /* in ms. */
    char* PresetNames[DMR__StateVariable_AllowedValues_MAX - 1];
    /**********************************************************************/

    /* Used to allow single access to this struct. */
    sem_t   resourceLock;
} *DMR_InternalState;
/****************************************************************************/


/****************************************************************************/
/* Internal Structure for calling methods through the thread pool.          */
#ifdef WIN32
typedef unsigned __int64 METHOD_PARAM;
#else
typedef void *METHOD_PARAM;
#endif
typedef struct _contextMethodCall
{
    DMR                     dmr;
    DMR_SessionToken        session;
    DMR_EventContextSwitch  method;
    int                     parameterCount;
    METHOD_PARAM            parameters[16];
} *ContextMethodCall;
/****************************************************************************/


/****************************************************************************/
/* Forward references for functions */
void DMR_LastChangeTimerEvent(void* object);
void FireGenaLastChangeEvent(DMR instance);
DMR_Error CallMethodThroughThreadPool(DMR instance, ContextMethodCall method);
/****************************************************************************/


/****************************************************************************/
/* milliseconds to a time string */
BOOL _addMethodParameter(ContextMethodCall This, METHOD_PARAM parameter)
{
    if(This->parameterCount == 16)
    {
        return FALSE;
    }

    This->parameters[This->parameterCount++] = parameter;

    return TRUE;
}
ContextMethodCall _createMethod(DMR_EventContextSwitch methodID, DMR instance, DMR_SessionToken session)
{
    ContextMethodCall result = MALLOC(sizeof(struct _contextMethodCall));
    if(result != NULL)
    {
        result->session = session;
        result->method = methodID;
        result->dmr = instance;
    }
    return result;
}
/****************************************************************************/


/****************************************************************************/
/* Convert a metadata string to a CdsObject. */
struct CdsObject* _metadataToCDS(char* metadata)
{
	struct CdsObject* result = NULL;
    struct ILibXMLNode* root = NULL;
    if(metadata == NULL)
    {
        return NULL;
    }
    if(strlen(metadata) == 0)
    {
        return NULL;
    }
    root = ILibParseXML(metadata, 0, (int)strlen(metadata));
    if(root != NULL)
    {
        if(ILibProcessXMLNodeList(root) == 0)
        {
			struct ILibXMLNode* item = root->Next;
            if(item != NULL && strncmp(item->Name, "item", item->NameLength) == 0)
            {
                struct CdsObject* CDS = NULL;
                struct ILibXMLAttribute* attrs = ILibGetXMLAttributes(item);

				// >>> Not sure why I need to do this???  Possible bug in the 'CDS_DeserializeDidlToObject'.
                //item->Name[item->NameLength] = '\0';
				// <<< end block

                CDS = CDS_DeserializeDidlToObject(item, attrs, 1, metadata, metadata + strlen(metadata));

                if(attrs != NULL)
                {
                    ILibDestructXMLAttributeList(attrs);
                }

                result = CDS;
            }
        }
        ILibDestructXMLNodeList(root);
    }
    return result;
}
/****************************************************************************/


/****************************************************************************/
/* milliseconds to a time string */
char* MillisecondsToTimeString(long milliseconds)
{
    int hours;
    int minutes;
    int seconds;
    int ms;
    char* result = String_CreateSize(16);

    ms = milliseconds % 1000;
    milliseconds /= 1000;
    seconds = milliseconds % 60;
    milliseconds /= 60;
    minutes = milliseconds % 60;
    milliseconds /= 60;
    hours = milliseconds;

    // handle the abnormal condition where the string will be overrun.
    if(hours > 99999)
    {
        hours = 99999;
    }

	sprintf(result, "%02d:%02d:%02d", hours, minutes, seconds);
    //sprintf(result, "%d:%02d:%02d.%03d", hours, minutes, seconds, ms);


    return result;
}
/****************************************************************************/


/****************************************************************************/
/* enum to string functions */
#define TESTBIT(x,y)   (((x & y) == y)?TRUE:FALSE)

char* FromPlayStateToString(DMR_PlayState state)
{
    char* result = NULL;
    switch(state)
    {
        case DMR_PS_NoMedia:
            result = String_Create("NO_MEDIA_PRESENT");
            break;
        case DMR_PS_Stopped:
            result = String_Create("STOPPED");
            break;
        case DMR_PS_Paused:
            result = String_Create("PAUSED_PLAYBACK");
            break;
        case DMR_PS_Playing:
            result = String_Create("PLAYING");
            break;
        case DMR_PS_Transitioning:
            result = String_Create("TRANSITIONING");
            break;
    }

    return result;
}

char* FromTransportStatusToString(DMR_TransportStatus status)
{
    char* result = NULL;
    switch(status)
    {
        case DMR_TS_OK:
            result = String_Create("OK");
            break;
        case DMR_TS_ERROR_OCCURRED:
            result = String_Create("ERROR_OCCURRED");
            break;
    }
    return result;
}

char* FromTransportActionsToString(unsigned short flags)
{
    int strLength = 1;
    char* result = NULL;

    if(TESTBIT(flags, DMR_ATS_Play) == TRUE)
    {
        strLength += 4;
    }
    if(TESTBIT(flags, DMR_ATS_Stop) == TRUE)
    {
        strLength += 5;
    }
    if(TESTBIT(flags, DMR_ATS_Pause) == TRUE)
    {
        strLength += 6;
    }
    if(TESTBIT(flags, DMR_ATS_Seek) == TRUE)
    {
        strLength += 5;
    }
    if(TESTBIT(flags, DMR_ATS_Next) == TRUE)
    {
        strLength += 5;
    }
    if(TESTBIT(flags, DMR_ATS_Previous) == TRUE)
    {
        strLength += 9;
    }

    result = String_CreateSize(strLength);

    if(TESTBIT(flags, DMR_ATS_Play) == TRUE)
    {
        strcpy(result, "Play");
    }
    if(TESTBIT(flags, DMR_ATS_Stop) == TRUE)
    {
        if(strlen(result) > 0)
        {
            strcat(result, ",Stop");
        }
        else
        {
            strcpy(result, "Stop");
        }
    }
    if(TESTBIT(flags, DMR_ATS_Pause) == TRUE)
    {
        if(strlen(result) > 0)
        {
            strcat(result, ",Pause");
        }
        else
        {
            strcpy(result, "Pause");
        }
    }
    if(TESTBIT(flags, DMR_ATS_Seek) == TRUE)
    {
        if(strlen(result) > 0)
        {
            strcat(result, ",Seek");
        }
        else
        {
            strcpy(result, "Seek");
        }
    }
    if(TESTBIT(flags, DMR_ATS_Next) == TRUE)
    {
        if(strlen(result) > 0)
        {
            strcat(result, ",Next");
        }
        else
        {
            strcpy(result, "Next");
        }
    }
    if(TESTBIT(flags, DMR_ATS_Previous) == TRUE)
    {
        if(strlen(result) > 0)
        {
            strcat(result, ",Previous");
        }
        else
        {
            strcpy(result, "Previous");
        }
    }
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.T.2 vvv */
    if(TESTBIT(flags, DMR_ATS_Reset) == TRUE)
    {
        strcpy(result, "");
    }
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.T.2 ^^^ */

    return result;
}

DMR_MediaPlayMode FromStringToMediaPlayMode(char* playMode)
{
    DMR_MediaPlayMode result = (DMR_MediaPlayMode)-1;

    if(strcmp(playMode, "NORMAL") == 0)
    {
        result = DMR_MPM_Normal;
    }
    else if(strcmp(playMode, "SHUFFLE") == 0)
    {
        result = DMR_MPM_Shuffle;
    }
    else if(strcmp(playMode, "REPEAT_ONE") == 0)
    {
        result = DMR_MPM_Repeat_One;
    }
    else if(strcmp(playMode, "REPEAT_ALL") == 0)
    {
        result = DMR_MPM_Repeat_All;
    }
    else if(strcmp(playMode, "RANDOM") == 0)
    {
        result = DMR_MPM_Random;
    }
    else if(strcmp(playMode, "DIRECT_1") == 0)
    {
        result = DMR_MPM_Direct_One;
    }
    else if(strcmp(playMode, "INTRO") == 0)
    {
        result = DMR_MPM_Intro;
    }
    return result;
}

char* FromMediaPlayModeToString(DMR_MediaPlayMode mode)
{
    char* result = NULL;

    switch(mode)
    {
        case DMR_MPM_Normal:
            result = String_Create("NORMAL");
            break;
        case DMR_MPM_Shuffle:
            result = String_Create("SHUFFLE");
            break;
        case DMR_MPM_Repeat_One:
            result = String_Create("REPEAT_ONE");
            break;
        case DMR_MPM_Repeat_All:
            result = String_Create("REPEAT_ALL");
            break;
        case DMR_MPM_Random:
            result = String_Create("RANDOM");
            break;
        case DMR_MPM_Direct_One:
            result = String_Create("DIRECT_1");
            break;
        case DMR_MPM_Intro:
            result = String_Create("INTRO");
            break;
    }

    return result;
}
/****************************************************************************/


/****************************************************************************/
/* Check the basic stability of the a DMR instance */
DMR_Error CheckThis(DMR instance)
{
    if(instance != NULL)
    {
        DMR_InternalState state = (DMR_InternalState)instance->internal_state;
        if(state != NULL)
        {
            return DMR_ERROR_OK;
        }
        else
        {
            return DMR_ERROR_BADINTERNALSTATE;
        }
    }
    else
    {
        return DMR_ERROR_BADTHIS;
    }
}
/****************************************************************************/


/****************************************************************************/
/* Gets the DMR object from the DMR_SessionToken object in the callbacks from
   the UPnP stack.*/
DMR GetDMRFromSessionToken(DMR_SessionToken upnptoken)
{
    return (DMR)DMR_GetTag(((struct ILibWebServer_Session*)upnptoken)->User);
}
/****************************************************************************/


/****************************************************************************/
/* Multi thread locking mechanisms for the DMR (for thread safety) */
void DMR_Lock(DMR instance)
{
    if(CheckThis(instance) == DMR_ERROR_OK)
    {
        DMR_InternalState state = (DMR_InternalState)instance->internal_state;
		sem_wait(&state->resourceLock);
    }
}

void DMR_Unlock(DMR instance)
{
    if(CheckThis(instance) == DMR_ERROR_OK)
    {
        DMR_InternalState state = (DMR_InternalState)instance->internal_state;
		sem_post(&state->resourceLock);
    }
}
/****************************************************************************/


/****************************************************************************/
/* ConnectionManager SOAP Action Callbacks */
void DMR_ConnectionManager_GetCurrentConnectionIDs(DMR_SessionToken upnptoken)
{
	ERROROUT1("Invoke: DMR_ConnectionManager_GetCurrentConnectionIDs();\r\n");

	DMR_Response_ConnectionManager_GetCurrentConnectionIDs(upnptoken, "0");
}

void DMR_ConnectionManager_GetCurrentConnectionInfo(DMR_SessionToken upnptoken, int ConnectionID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

    ERROROUT2("Invoke: DMR_ConnectionManager_GetCurrentConnectionInfo(%d);\r\n",ConnectionID);

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

	if(ConnectionID != 0)
	{
		DMR_Response_Error(upnptoken, 706, "Invalid connection reference");
        return;
    }

	if(state->AVTransportURI != NULL && state->CurrentTrackURI != NULL)
	{
	    ContextMethodCall method = NULL;
	    method = _createMethod(DMR_ECS_GETAVPROTOCOLINFO, instance, upnptoken);
	    CallMethodThroughThreadPool(instance, method);
	}
	else
	{
		DMR_Response_ConnectionManager_GetCurrentConnectionInfo(upnptoken, 0, 0, "", "", -1, "Input", "OK");
	}
}

void DMR_ConnectionManager_GetProtocolInfo(DMR_SessionToken upnptoken)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

    ERROROUT1("Invoke: DMR_ConnectionManager_GetProtocolInfo();\r\n");
    
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }
	
    DMR_Response_ConnectionManager_GetProtocolInfo(upnptoken, "", state->ProtocolInfo);
	
}
/****************************************************************************/


//vvv modified by yuyu for DLNA 1.5 CTT 7.3.104.2/7.3.104.4
char* GetTransportActions(unsigned short actions);
//^^^ modified by yuyu for DLNA 1.5 CTT 7.3.104.2/7.3.104.4
/****************************************************************************/
/* AVTransport SOAP Action Callbacks */
void DMR_AVTransport_GetCurrentTransportActions(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    char* actions = NULL;
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

	ERROROUT2("Invoke: DMR_AVTransport_GetCurrentTransportActions(%u);\r\n",InstanceID);

    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    DMR_Lock(instance);
//vvv modified by yuyu for DLNA 1.5 CTT 7.3.104.2/7.3.104.4
    //actions = FromTransportActionsToString(state->CurrentTransportActions);
    actions = GetTransportActions(state->CurrentTransportActions);
//^^^ modified by yuyu for DLNA 1.5 CTT 7.3.104.2/7.3.104.4
    DMR_Unlock(instance);

	DMR_Response_AVTransport_GetCurrentTransportActions(upnptoken, (const char*)actions);

    String_Destroy(actions);
}

void DMR_AVTransport_GetDeviceCapabilities(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu

    ERROROUT2("Invoke: DMR_AVTransport_GetDeviceCapabilities(%u);\r\n",InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

#ifdef PPTV
	DMR_Response_AVTransport_GetDeviceCapabilities(upnptoken, "None,Network,PPVOD,PPLIVE", "NOT_IMPLEMENTED", "NOT_IMPLEMENTED");
#else
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.104.7 vvv */
	DMR_Response_AVTransport_GetDeviceCapabilities(upnptoken, "None,Network", "NOT_IMPLEMENTED", "NOT_IMPLEMENTED");
	//DMR_Response_AVTransport_GetDeviceCapabilities(upnptoken, "NOT_IMPLEMENTED", "NOT_IMPLEMENTED", "NOT_IMPLEMENTED");
#endif
    
	/* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.104.7 ^^^ */
}

int _MakeCdsObjectConformant(struct CdsObject* metadata)
{
	int result = 0;
	struct CdsResource* res = metadata->Res;
	int length = 0;

	if(metadata->Creator != NULL)
	{
		length = ILibXmlEscapeLength(metadata->Creator);
		if(length >= 1024)
		{
			int tmpLen = 0;
			char* tmp = NULL;
			tmpLen = ILibXmlEscape(tmp, metadata->Creator);
			tmp[1024] = 0;
			tmpLen = ILibInPlaceXmlUnEscape(tmp);
			ILibString_Copy(metadata->Creator, tmpLen);
			metadata->Creator[tmpLen] = 0;
			free(tmp);
		}
	}

	if(metadata->ParentID != NULL)
	{
		length = ILibXmlEscapeLength(metadata->ParentID);
		if(length >= 1024)
		{
			int tmpLen = 0;
			char* tmp = NULL;
			tmpLen = ILibXmlEscape(tmp, metadata->ParentID);
			tmp[1024] = 0;
			tmpLen = ILibInPlaceXmlUnEscape(tmp);
			ILibString_Copy(metadata->ParentID, tmpLen);
			metadata->ParentID[tmpLen] = 0;
			free(tmp);
			result = 1;
		}
	}

	if(metadata->Title != NULL)
	{
		length = ILibXmlEscapeLength(metadata->Title);
		if(length >= 256)
		{
			int tmpLen = 0;
			char* tmp = NULL;
			tmpLen = ILibXmlEscape(tmp, metadata->Title);
			tmp[256] = 0;
			tmpLen = ILibInPlaceXmlUnEscape(tmp);
			ILibString_Copy(metadata->Title, tmpLen);
			metadata->Title[tmpLen] = 0;
			free(tmp);
		}
	}

	while(res != NULL)
	{
		if(res->IfoFileUri != NULL)
		{
			length = ILibXmlEscapeLength(res->IfoFileUri);
			if(length >= 1024)
			{
				int tmpLen = 0;
				char* tmp = NULL;
				tmpLen = ILibXmlEscape(tmp, res->IfoFileUri);
				tmp[1024] = 0;
				tmpLen = ILibInPlaceXmlUnEscape(tmp);
				ILibString_Copy(res->IfoFileUri, tmpLen);
				res->IfoFileUri[tmpLen] = 0;
				free(tmp);
				result = 1;
			}
		}
		if(res->ImportIfoFileUri != NULL)
		{
			length = ILibXmlEscapeLength(res->ImportIfoFileUri);
			if(length >= 1024)
			{
				int tmpLen = 0;
				char* tmp = NULL;
				tmpLen = ILibXmlEscape(tmp, res->ImportIfoFileUri);
				tmp[1024] = 0;
				tmpLen = ILibInPlaceXmlUnEscape(tmp);
				ILibString_Copy(res->ImportIfoFileUri, tmpLen);
				res->ImportIfoFileUri[tmpLen] = 0;
				free(tmp);
				result = 1;
			}
		}
		if(res->ImportUri != NULL)
		{
			length = ILibXmlEscapeLength(res->ImportUri);
			if(length >= 1024)
			{
				int tmpLen = 0;
				char* tmp = NULL;
				tmpLen = ILibXmlEscape(tmp, res->ImportUri);
				tmp[1024] = 0;
				tmpLen = ILibInPlaceXmlUnEscape(tmp);
				ILibString_Copy(res->ImportUri, tmpLen);
				res->ImportUri[tmpLen] = 0;
				free(tmp);
				result = 1;
			}
		}
		if(res->ProtocolInfo != NULL)
		{
			length = ILibXmlEscapeLength(res->ImportUri);
			if(length >= 1024)
			{
				int tmpLen = 0;
				char* tmp = NULL;
				tmpLen = ILibXmlEscape(tmp, res->ImportUri);
				tmp[1024] = 0;
				tmpLen = ILibInPlaceXmlUnEscape(tmp);
				ILibString_Copy(res->ImportUri, tmpLen);
				res->ImportUri[tmpLen] = 0;
				free(tmp);
				result = 1;
			}
		}
		if(res->Value != NULL)
		{
			length = ILibXmlEscapeLength(res->Value);
			if(length >= 1024)
			{
				int tmpLen = 0;
				char* tmp = NULL;
				tmpLen = ILibXmlEscape(tmp, res->Value);
				tmp[1024] = 0;
				tmpLen = ILibInPlaceXmlUnEscape(tmp);
				ILibString_Copy(res->Value, tmpLen);
				res->Value[tmpLen] = 0;
				free(tmp);
				result = 1;
			}
		}
		res = res->Next;
	}

	return result;
}

char* _MakeMetadataConformant(char* original)
{
	char* result = NULL; 
	if(original != NULL)
	{
		int length = (int)strlen(original);
		struct ILibXMLNode* root = NULL;
		
		root = ILibParseXML(original, 0, length);
		if(root != NULL && ILibProcessXMLNodeList(root) == 0)
		{
			ILibXML_BuildNamespaceLookupTable(root);
			if(root->Next != NULL)
			{
				struct ILibXMLAttribute* attrs = ILibGetXMLAttributes(root->Next);
				int item = 0;
				struct CdsObject* object = NULL;
				if(root->Next->NameLength == 4 && strncmp(root->Next->Name, "item", root->Next->NameLength) == 0)
				{
					item = 1;
				}

				object = CDS_DeserializeDidlToObject(root->Next, attrs, item, original, original + length);

				if(attrs != NULL)
				{
					ILibDestructXMLAttributeList(attrs);
				}
				ILibDestructXMLNodeList(root);

				if(object != NULL)
				{
					int error = 0;

					if(_MakeCdsObjectConformant(object) == 0)
					{
						result = CDS_SerializeObjectToDidl(object, 0, CDS_ConvertCsvStringToBitString("*"), 1, &error); 
						if(result == NULL)
						{
							result = String_Create("");
						}
					}
					else
					{
						result = String_Create("");
					}
				}
				else
				{
					result = String_Create("");
				}
			}
			else
			{
				ILibDestructXMLNodeList(root);
				result = String_Create("");
			}
		}
		else
		{
			result = String_Create("");
			if(root!=NULL)
			{
				ILibDestructXMLNodeList(root);
			}
		}
	}
	else
	{
		result = String_Create("");
	}

	return result;
}

void DMR_AVTransport_GetMediaInfo(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    unsigned int maxTracks;
    char* mediaDuration = NULL;
    char* uri = NULL;
    char* metadata = NULL;
	char* nexturi = NULL;
	char* nextmetadata = NULL;
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

	ERROROUT2("Invoke: DMR_AVTransport_GetMediaInfo(%u);\r\n", InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    DMR_Lock(instance);
    maxTracks = state->NumberOfTracks;
    mediaDuration = MillisecondsToTimeString(state->CurrentMediaDuration);
    uri = String_Create((const char*)state->AVTransportURI);
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 vvv */
    //metadata = _MakeMetadataConformant(state->AVTransportURIMetaData); //String_Create((const char*)state->AVTransportURIMetaData);
    if( state->AVTransportURIMetaData != NULL || strlen(state->AVTransportURIMetaData) != 0)
        metadata = String_Create(state->AVTransportURIMetaData);
    else
        metadata = String_Create("NOT_IMPLEMENTED");		
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 ^^^ */

	if( state->NextAVTransportURI != NULL || strlen(state->NextAVTransportURI) != 0)
		nexturi = String_Create(state->NextAVTransportURI);
    else
		nexturi = String_Create("NOT_IMPLEMENTED");
	   
	if( state->NextAVTransportURIMetaData != NULL || strlen(state->NextAVTransportURIMetaData) != 0)
		nextmetadata = String_Create(state->NextAVTransportURIMetaData);
    else
		nextmetadata = String_Create("NOT_IMPLEMENTED"); 	   

    DMR_Unlock(instance);

    DMR_Response_AVTransport_GetMediaInfo(upnptoken,
        maxTracks,
        (const char*)mediaDuration,
		(const char*)uri,
		(const char*)metadata,
		(const char*)nexturi,	//"NOT_IMPLEMENTED",
		(const char*)nextmetadata,//"NOT_IMPLEMENTED",
		"NOT_IMPLEMENTED",
		"NOT_IMPLEMENTED",
		"NOT_IMPLEMENTED");

    String_Destroy(mediaDuration);
    String_Destroy(uri);
    String_Destroy(metadata);
	String_Destroy(nexturi);
    String_Destroy(nextmetadata);
}

void DMR_AVTransport_GetPositionInfo(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    char* trackDuration = NULL;
    char* trackMetaData = NULL;
    char* trackURI = NULL;
    char* relTime = NULL;
    char* absTime = NULL;
    unsigned int track = 0;
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

	ERROROUT2("Invoke: DMR_AVTransport_GetPositionInfo(%u);\r\n",InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    DMR_Lock(instance);
    track = state->CurrentTrack;
    if( state->CurrentTrackDuration < 0 )
    {
        trackDuration =  String_Create("NOT_IMPLEMENTED");
    }
    else
    {
        trackDuration = MillisecondsToTimeString(state->CurrentTrackDuration);
    }
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.100.6A,B,C vvv */
    //trackMetaData = _MakeMetadataConformant(state->CurrentTrackMetaData); //String_Create((const char*)state->CurrentTrackMetaData);
    if( state->CurrentTrackMetaData == NULL || strlen(state->CurrentTrackMetaData) == 0 )
        trackMetaData = String_Create("NOT_IMPLEMENTED");
    else
        trackMetaData = String_Create((const char*)state->CurrentTrackMetaData);
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.100.6A,B,C ^^^ */
    trackURI = String_Create((const char*)state->CurrentTrackURI);
	if(state->RelativeTimePosition < 0)
	{
		relTime = String_Create("NOT_IMPLEMENTED");
	}
	else
	{
		relTime = MillisecondsToTimeString(state->RelativeTimePosition);
	}
	if(state->AbsoluteTimePosition < 0)
	{
		absTime = String_Create("NOT_IMPLEMENTED");
	}
	else
	{
		absTime = MillisecondsToTimeString(state->AbsoluteTimePosition);
	}
    DMR_Unlock(instance);

	printf("relTime=%s absTime=%s CurrentTrackDuration=%s\n",relTime,absTime,trackDuration);
	
	DMR_Response_AVTransport_GetPositionInfo(upnptoken, track, (const char*)trackDuration, (const char*)trackMetaData, (const char*)trackURI, (const char*)relTime, (const char*)absTime, 0xffffffff, 0xffffffff);

    String_Destroy(trackDuration);
    String_Destroy(trackMetaData);
    String_Destroy(trackURI);
    String_Destroy(relTime);
    String_Destroy(absTime);
}

void DMR_AVTransport_GetTransportInfo(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    char* transportState = NULL;
    char* transportStatus = NULL;
    char* transportSpeed = NULL;
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

    ERROROUT2("Invoke: DMR_AVTransport_GetTransportInfo(%u);\r\n", InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    DMR_Lock(instance);
    transportState = FromPlayStateToString(state->TransportState);
    transportStatus = FromTransportStatusToString(state->TransportStatus);
    transportSpeed = String_Create((const char*)state->TransportPlaySpeed);
    DMR_Unlock(instance);

	printf("transportState=%s transportStatus=%s transportSpeed=%s\n", transportState, transportStatus, transportSpeed);

    DMR_Response_AVTransport_GetTransportInfo(upnptoken, (const char*)transportState, (const char*)transportStatus, (const char*)transportSpeed);

    String_Destroy(transportState);
    String_Destroy(transportStatus);
    String_Destroy(transportSpeed);
}

void DMR_AVTransport_GetTransportSettings(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    char* playMode = NULL;
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

    ERROROUT2("Invoke: DMR_AVTransport_GetTransportSettings(%u);\r\n",InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    DMR_Lock(instance);
    playMode = FromMediaPlayModeToString(state->CurrentPlayMode);
    DMR_Unlock(instance);

	DMR_Response_AVTransport_GetTransportSettings(upnptoken, (const char*)playMode, "NOT_IMPLEMENTED");

    String_Destroy(playMode);
}

void DMR_AVTransport_Next(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT2("Invoke: DMR_AVTransport_Next(%u);\r\n",InstanceID);

    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    method = _createMethod(DMR_ECS_NEXT, instance, upnptoken);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_AVTransport_Pause(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT2("Invoke: DMR_AVTransport_Pause(%u);\r\n",InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    method = _createMethod(DMR_ECS_PAUSE, instance, upnptoken);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_AVTransport_Play(DMR_SessionToken upnptoken,unsigned int InstanceID,char* Speed)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT3("Invoke: DMR_AVTransport_Play(%u,%s);\r\n",InstanceID,Speed);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    method = _createMethod(DMR_ECS_PLAY, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)Speed);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_AVTransport_Previous(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT2("Invoke: DMR_AVTransport_Previous(%u);\r\n",InstanceID);

    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    method = _createMethod(DMR_ECS_PREVIOUS, instance, upnptoken);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_AVTransport_Seek(DMR_SessionToken upnptoken,unsigned int InstanceID,char* Unit,char* Target)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

    ERROROUT4("Invoke: DMR_AVTransport_Seek(%u,%s,%s);\r\n", InstanceID, Unit, Target);

	if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }
    if(strcmp(Unit, "TRACK_NR") == 0)
    {
        int target = (unsigned int)atoi(Target);
        method = _createMethod(DMR_ECS_SEEKTRACK, instance, upnptoken);
        _addMethodParameter(method, (METHOD_PARAM)target);

        CallMethodThroughThreadPool(instance, method);
    }
    else if(strcmp(Unit, "ABS_TIME") == 0)
    {
        //long target = atol(Target);
        int hh,mm,ss;
        sscanf(Target,"%d:%d:%d",&hh,&mm,&ss);
		long target = (3600*hh+mm*60+ss);
        method = _createMethod(DMR_ECS_SEEKMEDIATIME, instance, upnptoken);
        _addMethodParameter(method, (METHOD_PARAM)target);

        CallMethodThroughThreadPool(instance, method);
    }
    else if(strcmp(Unit, "REL_TIME") == 0)
    {
        //long target = atol(Target);
        int hh,mm,ss;
        sscanf(Target,"%d:%d:%d",&hh,&mm,&ss);
		long target = (3600*hh+mm*60+ss);
        method = _createMethod(DMR_ECS_SEEKTRACKTIME, instance, upnptoken);
        _addMethodParameter(method, (METHOD_PARAM)target);

        CallMethodThroughThreadPool(instance, method);
    }
    else
    {
        DMR_Response_Error(upnptoken, 710, "Seek Mode Not Supported");
    }
}

void DMR_AVTransport_SetAVTransportURI(DMR_SessionToken upnptoken, unsigned int InstanceID, char* CurrentURI, char* CurrentURIMetaData)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    struct CdsObject* CDS = NULL;
    ContextMethodCall method = NULL;

	ERROROUT4("Invoke: DMR_AVTransport_SetAVTransportURI(%u,%s,%s);\r\n", InstanceID, CurrentURI, CurrentURIMetaData);

    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }
	
    method = _createMethod(DMR_ECS_SETAVTRANSPORTURI, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)CurrentURI);
    CDS = _metadataToCDS(CurrentURIMetaData);
    _addMethodParameter(method, (METHOD_PARAM)CDS);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_AVTransport_SetNextAVTransportURI(DMR_SessionToken upnptoken, unsigned int InstanceID, char* NextURI, char* NextURIMetaData)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    struct CdsObject* CDS = NULL;
    ContextMethodCall method = NULL;

	ERROROUT4("Invoke: DMR_AVTransport_SetNextAVTransportURI(%u,%s,%s);\r\n", InstanceID, NextURI, NextURIMetaData);

    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }
	
    method = _createMethod(DMR_ECS_SETNEXTAVTRANSPORTURI, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)NextURI);
    CDS = _metadataToCDS(NextURIMetaData);
    _addMethodParameter(method, (METHOD_PARAM)CDS);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_AVTransport_SetPlayMode(DMR_SessionToken upnptoken,unsigned int InstanceID,char* NewPlayMode)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
	DMR_MediaPlayMode mode = -1;
    ContextMethodCall method = NULL;

	ERROROUT3("Invoke: DMR_AVTransport_SetPlayMode(%u,%s);\r\n",InstanceID,NewPlayMode);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }

    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

	mode = FromStringToMediaPlayMode(NewPlayMode);

    method = _createMethod(DMR_ECS_SETPLAYMODE, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)mode);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_AVTransport_Stop(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT2("Invoke: DMR_AVTransport_Stop(%u);\r\n",InstanceID);
	
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 718, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }
			
    method = _createMethod(DMR_ECS_STOP, instance, upnptoken);

    CallMethodThroughThreadPool(instance, method);
}
/****************************************************************************/


/****************************************************************************/
/* RenderingControl SOAP Action Callbacks */
void DMR_RenderingControl_ListPresets(DMR_SessionToken upnptoken, unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;
    char* list;
    int listLen = 0;
    int i;

	ERROROUT2("Invoke: DMR_RenderingControl_ListPresets(%u);\r\n",InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    listLen = (int)strlen("FactoryDefaults") + 1;
    for(i = 0; i < (DMR__StateVariable_AllowedValues_MAX - 1); i++)
    {
        if(state->PresetNames[i] != NULL)
        {
            listLen += (int)strlen((const char*)state->PresetNames[i]) + 1;
        }
    }
    list = String_CreateSize(listLen + 1);
    strcpy(list, "FactoryDefaults");
    for(i = 0; i < (DMR__StateVariable_AllowedValues_MAX - 1); i++)
    {
        if(state->PresetNames[i] != NULL)
        {
            strcat(list, ",");
            strcat(list, (const char*)state->PresetNames[i]);
        }
    }

	DMR_Response_RenderingControl_ListPresets(upnptoken, (const char*)list);

    String_Destroy(list);
}

void DMR_RenderingControl_SelectPreset(DMR_SessionToken upnptoken, unsigned int InstanceID, char* PresetName)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT3("Invoke: DMR_RenderingControl_SelectPreset(%u,%s);\r\n", InstanceID, PresetName);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    method = _createMethod(DMR_ECS_SELECTPRESET, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)PresetName);

    CallMethodThroughThreadPool(instance, method);
}

#if defined(INCLUDE_FEATURE_DISPLAY)
void DMR_RenderingControl_GetBrightness(DMR_SessionToken upnptoken, unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

    ERROROUT2("Invoke: DMR_RenderingControl_GetBrightness(%u);\r\n", InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }
	
    DMR_Response_RenderingControl_GetBrightness(upnptoken, state->Brightness);
}

void DMR_RenderingControl_GetContrast(DMR_SessionToken upnptoken,unsigned int InstanceID)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

	ERROROUT2("Invoke: DMR_RenderingControl_GetContrast(%u);\r\n", InstanceID);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }
	
    DMR_Response_RenderingControl_GetContrast(upnptoken, state->Contrast);
}

void DMR_RenderingControl_SetBrightness(DMR_SessionToken upnptoken, unsigned int InstanceID, unsigned short DesiredBrightness)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT3("Invoke: DMR_RenderingControl_SetBrightness(%u,%u);\r\n", InstanceID, DesiredBrightness);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    method = _createMethod(DMR_ECS_SETBRIGHTNESS, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)DesiredBrightness);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_RenderingControl_SetContrast(DMR_SessionToken upnptoken, unsigned int InstanceID, unsigned short DesiredContrast)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

	ERROROUT3("Invoke: DMR_RenderingControl_SetContrast(%u,%u);\r\n", InstanceID, DesiredContrast);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    method = _createMethod(DMR_ECS_SETCONTRAST, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)DesiredContrast);

    CallMethodThroughThreadPool(instance, method);
}
#endif /* INCLUDE_FEATURE_DISPLAY */

#if defined(INCLUDE_FEATURE_VOLUME)
void DMR_RenderingControl_GetMute(DMR_SessionToken upnptoken, unsigned int InstanceID, char* Channel)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

	ERROROUT3("Invoke: DMR_RenderingControl_GetMute(%u,%s);\r\n",InstanceID,Channel);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    if(strcmp(Channel, "Master") != 0)
    {
		DMR_Response_Error(upnptoken, 600, "Argument Value Invalid");
        return;
    }
	
    DMR_Response_RenderingControl_GetMute(upnptoken, state->Mute?1:0);
}

void DMR_RenderingControl_GetVolume(DMR_SessionToken upnptoken, unsigned int InstanceID, char* Channel)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;

	ERROROUT3("Invoke: DMR_RenderingControl_GetVolume(%u,%s);\r\n", InstanceID, Channel);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    if(strcmp(Channel, "Master") != 0)
    {
		DMR_Response_Error(upnptoken, 600, "Argument Value Invalid");
        return;
    }
	
    DMR_Response_RenderingControl_GetVolume(upnptoken, state->Volume);
}

void DMR_RenderingControl_SetMute(DMR_SessionToken upnptoken,unsigned int InstanceID,char* Channel,int DesiredMute)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

    ERROROUT4("Invoke: DMR_RenderingControl_SetMute(%u,%s,%d);\r\n", InstanceID, Channel, DesiredMute);
	
    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }
	
    if(CheckThis(instance) != DMR_ERROR_OK)
    {
    	DMR_Response_Error(upnptoken, 501, "Action Failed");
        return;
    }

    if(strcmp(Channel, "Master") != 0)
    {
    	//DMR_Response_Error(upnptoken, 600, "Argument Value Invalid");
		DMR_Response_Error(upnptoken, 703, "Argument Value Invalid");
        return;
    }

    method = _createMethod(DMR_ECS_SETMUTE, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)DesiredMute);

    CallMethodThroughThreadPool(instance, method);
}

void DMR_RenderingControl_SetVolume(DMR_SessionToken upnptoken,unsigned int InstanceID,char* Channel,unsigned short DesiredVolume)
{
    DMR instance = GetDMRFromSessionToken(upnptoken);
    //DMR_InternalState state = (DMR_InternalState)instance->internal_state;//remove since no use, 20090513, yuyu
    ContextMethodCall method = NULL;

    if(InstanceID != 0)
    {
		DMR_Response_Error(upnptoken, 702, "Invalid InstanceID");
        return;
    }

    if(strcmp(Channel, "Master") != 0)
    {
		DMR_Response_Error(upnptoken, 600, "Argument Value Invalid");
        return;
    }
	
    method = _createMethod(DMR_ECS_SETVOLUME, instance, upnptoken);
    _addMethodParameter(method, (METHOD_PARAM)DesiredVolume);

    CallMethodThroughThreadPool(instance, method);
}
#endif /* INCLUDE_FEATURE_VOLUME */
/****************************************************************************/

void DMRDestroyFromChain(DMR instance)
{
    if(instance != NULL)
    {
        DMR_InternalState state = (DMR_InternalState)instance->internal_state;
        if(state != NULL)
        {
            int i;

            /* Destroy the strings in the internal state */
            String_Destroy(state->udn);
            String_Destroy(state->FriendlyName);
            String_Destroy(state->SerialNumber);
            String_Destroy(state->ProtocolInfo);
            String_Destroy(state->TransportPlaySpeed);
            String_Destroy(state->AVTransportURI);
            String_Destroy(state->AVTransportURIMetaData);
			String_Destroy(state->NextAVTransportURI);
            String_Destroy(state->NextAVTransportURIMetaData);
            String_Destroy(state->CurrentTrackMetaData);
            String_Destroy(state->CurrentTrackURI);

            for(i = 0; i < (DMR__StateVariable_AllowedValues_MAX - 1); i++)
            {
                String_Destroy(state->PresetNames[i]);
            }

            sem_destroy(&state->resourceLock);

            /* Destroy the internal state structure */
            FREE(instance->internal_state);

            /* REMOVED: The ILibChain will destroy this object */
            /* FREE(instance); */
        }
    }
}


/****************************************************************************/
/* DMR Public Methods */
DMR DMR_Method_Create(void* chain, unsigned short port, char* friendlyName, char* serialNumber, char* UDN, char* protocolInfo, ILibThreadPool threadPool)
{
    /* Build the DMR objects. */
    DMR dmr = NULL;
    DMR_InternalState state = NULL;
    int i;
    int count;
    struct DMR__Device_MediaRenderer* device = NULL;

    /* Allocate the DMR and internal state structures */
    dmr = (DMR)MALLOC(sizeof(struct _DMR));
	if(dmr == NULL)
	{
		return NULL;
	}
    state = (DMR_InternalState)MALLOC(sizeof(struct _internalState));
	if(state == NULL)
	{
		if(dmr) FREE(dmr);//make up, 20090513, yuyu
		return NULL;
	}
	state->DMR_microStackChain = chain;
    dmr->internal_state = (void*)state;
    dmr->ThreadPool = threadPool;

	/* Let the chain destroy the object. */
	dmr->ILib3 = (void*)DMRDestroyFromChain;
	ILibAddToChain(chain, dmr);
    
    /* Set the default values for the DMR_InternalState. */
    state->FriendlyName = String_Create(friendlyName);
    state->SerialNumber = String_Create(serialNumber);
    state->udn = String_Create(UDN);
    state->ProtocolInfo = String_Create(protocolInfo);
    state->port = port;
    state->seconds = ADVERTISEMENT_PERIOD_SECONDS;
    state->EventsOnThreadBitMask = DMR_ECS_DEFAULT;
    state->TransportPlaySpeed = String_Create("1");
    state->TransportState = DMR_PS_NoMedia;
    state->TransportStatus = DMR_TS_OK;
    state->CurrentPlayMode = DMR_MPM_Normal;
    state->AVTransportURI = String_Create("");
    state->AVTransportURIMetaData = String_Create("");
	state->NextAVTransportURI = String_Create("");
    state->NextAVTransportURIMetaData = String_Create("");
    state->CurrentTrackURI = String_Create("");
    state->CurrentTrackMetaData = String_Create("");
#if defined(INCLUDE_FEATURE_VOLUME)
    state->Volume = (unsigned char)VOLUME_FACTORYDEFAULT;
    state->Mute = FALSE;
#endif /* INCLUDE_FEATURE_VOLUME */
#if defined(INCLUDE_FEATURE_DISPLAY)
    state->Contrast = (unsigned char)CONTRAST_FACTORYDEFAULT;
    state->Brightness = (unsigned char)BRIGHTNESS_FACTORYDEFAULT;
#endif /* INCLUDE_FEATURE_DISPLAY */
	//TODO
//#ifdef DLNADMRCTT //For DLNA DMR CTT 7.3.100.4A,B,C 7.3.100.5A,B,C 
    state->RelativeTimePosition = 0;	//0xffffffff;
    state->CurrentTrackDuration = 0;	//0xffffffff;
    //state->AbsoluteTimePosition = 0xffffffff;
//#endif  //For DLNA DMR CTT 7.3.100.4A,B,C 7.3.100.5A,B,C 
    state->PresetNames[0] = String_Create("Brightness");

    sem_init(&state->resourceLock, 0, 1);

    /* Create the microstack state structures */
    state->DMR_microStack = DMR_CreateMicroStack(state->DMR_microStackChain, state->FriendlyName, state->udn, state->SerialNumber, state->seconds, state->port);
    DMR_SetTag(state->DMR_microStack, (void*)dmr);

    device = DMR_GetConfiguration();

    // Add any added preset names that the applicatin may have added...
    count = 1;
    for(i = 0; i < (DMR__StateVariable_AllowedValues_MAX - 1); i++)
    {
        if(state->PresetNames[i] != NULL)
        {
            device->RenderingControl->StateVar_A_ARG_TYPE_PresetName->AllowedValues[count++] = state->PresetNames[i];
        }
    }

    /* Optionally remove the display and volume features */
#if !defined(INCLUDE_FEATURE_DISPLAY)
    device->RenderingControl->GetBrightness = NULL;
    device->RenderingControl->GetContrast = NULL;
    device->RenderingControl->SetBrightness = NULL;
    device->RenderingControl->SetContrast = NULL;
    device->RenderingControl->StateVar_Brightness = NULL;
    device->RenderingControl->StateVar_Contrast = NULL;
#endif /* INCLUDE_FEATURE_DISPLAY */

#if !defined(INCLUDE_FEATURE_VOLUME)
    device->RenderingControl->GetMute = NULL;
    device->RenderingControl->GetVolume = NULL;
    device->RenderingControl->SetMute = NULL;
    device->RenderingControl->SetVolume = NULL;
    device->RenderingControl->StateVar_Mute = NULL;
    device->RenderingControl->StateVar_Volume = NULL;
#endif /* INCLUDE_FEATURE_VOLUME */

#if !defined(INCLUDE_FEATURE_PLAYCONTAINERURI)
	// TODO: FUTURE FEATURE for <dlna:X_DLNACAP>playcontainer-0-1</dlna:X_DLNACAP>
#endif

    /* Setup internal funcation callbacks for SOAP actions optionally
        ignoring the display and volume features */
    DMR_FP_AVTransport_GetCurrentTransportActions=(DMR__ActionHandler_AVTransport_GetCurrentTransportActions)&DMR_AVTransport_GetCurrentTransportActions;
    DMR_FP_AVTransport_GetDeviceCapabilities=(DMR__ActionHandler_AVTransport_GetDeviceCapabilities)&DMR_AVTransport_GetDeviceCapabilities;
    DMR_FP_AVTransport_GetMediaInfo=(DMR__ActionHandler_AVTransport_GetMediaInfo)&DMR_AVTransport_GetMediaInfo;
    DMR_FP_AVTransport_GetPositionInfo=(DMR__ActionHandler_AVTransport_GetPositionInfo)&DMR_AVTransport_GetPositionInfo;
    DMR_FP_AVTransport_GetTransportInfo=(DMR__ActionHandler_AVTransport_GetTransportInfo)&DMR_AVTransport_GetTransportInfo;
    DMR_FP_AVTransport_GetTransportSettings=(DMR__ActionHandler_AVTransport_GetTransportSettings)&DMR_AVTransport_GetTransportSettings;
    DMR_FP_AVTransport_Next=(DMR__ActionHandler_AVTransport_Next)&DMR_AVTransport_Next;
    DMR_FP_AVTransport_Pause=(DMR__ActionHandler_AVTransport_Pause)&DMR_AVTransport_Pause;
    DMR_FP_AVTransport_Play=(DMR__ActionHandler_AVTransport_Play)&DMR_AVTransport_Play;
    DMR_FP_AVTransport_Previous=(DMR__ActionHandler_AVTransport_Previous)&DMR_AVTransport_Previous;
    DMR_FP_AVTransport_Seek=(DMR__ActionHandler_AVTransport_Seek)&DMR_AVTransport_Seek;
    DMR_FP_AVTransport_SetAVTransportURI=(DMR__ActionHandler_AVTransport_SetAVTransportURI)&DMR_AVTransport_SetAVTransportURI;
	DMR_FP_AVTransport_SetNextAVTransportURI=(DMR__ActionHandler_AVTransport_SetNextAVTransportURI)&DMR_AVTransport_SetNextAVTransportURI;
	DMR_FP_AVTransport_SetPlayMode=(DMR__ActionHandler_AVTransport_SetPlayMode)&DMR_AVTransport_SetPlayMode;
    DMR_FP_AVTransport_Stop=(DMR__ActionHandler_AVTransport_Stop)&DMR_AVTransport_Stop;
    DMR_FP_ConnectionManager_GetCurrentConnectionIDs=(DMR__ActionHandler_ConnectionManager_GetCurrentConnectionIDs)&DMR_ConnectionManager_GetCurrentConnectionIDs;
    DMR_FP_ConnectionManager_GetCurrentConnectionInfo=(DMR__ActionHandler_ConnectionManager_GetCurrentConnectionInfo)&DMR_ConnectionManager_GetCurrentConnectionInfo;
    DMR_FP_ConnectionManager_GetProtocolInfo=(DMR__ActionHandler_ConnectionManager_GetProtocolInfo)&DMR_ConnectionManager_GetProtocolInfo;
    DMR_FP_RenderingControl_ListPresets=(DMR__ActionHandler_RenderingControl_ListPresets)&DMR_RenderingControl_ListPresets;
    DMR_FP_RenderingControl_SelectPreset=(DMR__ActionHandler_RenderingControl_SelectPreset)&DMR_RenderingControl_SelectPreset;
#if defined(INCLUDE_FEATURE_DISPLAY)
    DMR_FP_RenderingControl_GetBrightness=(DMR__ActionHandler_RenderingControl_GetBrightness)&DMR_RenderingControl_GetBrightness;
    DMR_FP_RenderingControl_GetContrast=(DMR__ActionHandler_RenderingControl_GetContrast)&DMR_RenderingControl_GetContrast;
    DMR_FP_RenderingControl_SetBrightness=(DMR__ActionHandler_RenderingControl_SetBrightness)&DMR_RenderingControl_SetBrightness;
    DMR_FP_RenderingControl_SetContrast=(DMR__ActionHandler_RenderingControl_SetContrast)&DMR_RenderingControl_SetContrast;
#endif /* INCLUDE_FEATURE_DISPLAY */
#if defined(INCLUDE_FEATURE_VOLUME)
    DMR_FP_RenderingControl_GetMute=(DMR__ActionHandler_RenderingControl_GetMute)&DMR_RenderingControl_GetMute;
    DMR_FP_RenderingControl_GetVolume=(DMR__ActionHandler_RenderingControl_GetVolume)&DMR_RenderingControl_GetVolume;
    DMR_FP_RenderingControl_SetMute=(DMR__ActionHandler_RenderingControl_SetMute)&DMR_RenderingControl_SetMute;
    DMR_FP_RenderingControl_SetVolume=(DMR__ActionHandler_RenderingControl_SetVolume)&DMR_RenderingControl_SetVolume;
#endif /* INCLUDE_FEATURE_VOLUME */
    
	/* All evented state variables MUST be initialized before UPnPStart is called. */
	//DMR_SetState_AVTransport_LastChange(state->DMR_microStack, "&lt;Event xmlns=&quot;urn:schemas-upnp-org:metadata-1-0/AVT/&quot;/&gt;");
	//DMR_SetState_AVTransport_LastChange(state->DMR_microStack, "<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT/\"/ >");
	DMR_SetState_ConnectionManager_SourceProtocolInfo(state->DMR_microStack, "");
    DMR_SetState_ConnectionManager_SinkProtocolInfo(state->DMR_microStack, state->ProtocolInfo);
	DMR_SetState_ConnectionManager_CurrentConnectionIDs(state->DMR_microStack, "0");
//TODO, CTT 7.3.93.1 !!!
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1 vvv */
	state->LastChangeMask = EVENT_MASK_RENDERINGCONTROL|EVENT_MASK_AVTRANSPORT|EVENT_PRESETNAMELIST|EVENT_AVTOTHERS;
	FireGenaLastChangeEvent(dmr);
	state->LastChangeMask = 0;
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.93.1 ^^^ */
	//DMR_SetState_RenderingControl_LastChange(state->DMR_microStack, "&lt;Event xmlns=&quot;urn:schemas-upnp-org:metadata-1-0/RCS/&quot;/&gt;");
	//DMR_SetState_RenderingControl_LastChange(state->DMR_microStack, "<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/RCS/\"/ >");
        	
    /* Setup the microstack execution chain */
    state->DMR_Monitor = ILibCreateLifeTime(state->DMR_microStackChain);
    ILibLifeTime_AddEx(state->DMR_Monitor, dmr, 200, &DMR_LastChangeTimerEvent, NULL);

    /* Return the DMR object */
    return dmr;
}

BOOL DMR_Method_IsRunning(DMR instance)
{
	if(instance != NULL)
	{
		DMR_InternalState state = (DMR_InternalState)instance->internal_state;
		if(state != NULL)
		{
			if(state->DMR_microStackChain != NULL)
			{
				return (ILibIsChainRunning(state->DMR_microStackChain))?TRUE:FALSE;
			}
		}
	}
	return FALSE;
}

DMR_Error DMR_Method_SetEventContextMask(DMR instance, DMR_EventContextSwitch bitFlags)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }

    DMR_Lock(instance);
    istate->EventsOnThreadBitMask = bitFlags;
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

void DMR_Method_NotifyMicrostackOfIPAddressChange(DMR instance)
{
	if(instance != NULL)
	{
		DMR_InternalState state = (DMR_InternalState)instance->internal_state;
		if(state != NULL)
		{
			if(state->DMR_microStack != NULL)
			{
				DMR_IPAddressListChanged(state->DMR_microStack);
			}
		}
	}
}

BOOL DMR_Method_AddPresetNameToList(DMR instance, const char* name)
{
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;
    int i;
    int found = -1;
    if(DMR_Method_IsRunning(instance) == TRUE)
    {
        return FALSE;
    }

    for(i = 0; i < (DMR__StateVariable_AllowedValues_MAX - 1); i++)
    {
        if(state->PresetNames[i] == NULL)
        {
            found = i;
            break;
        }
    }
    if(found == -1)
    {
        return FALSE;
    }
    else
    {
        state->PresetNames[found] = String_Create(name);
    }
    return TRUE;
}

void DMR_Method_ErrorEventResponse(void* session, int errorCode, char* errorMessage)
{
    DMR_Response_Error(session, errorCode, errorMessage);
}
/****************************************************************************/


/****************************************************************************/
/* Handles the LastChange Gena Eventing for both Services */
int GetTransportActionsLength(unsigned short actions)
{
	unsigned long lActions = (unsigned long)actions;
	int result = 1;

	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Play) == TRUE)
	{
		result += 5;
		#ifdef DLNADMRCTT
		if(DLNADMR_bSupportTrickmode)
		{
		if(DLNADMR_bPlayingAV)
		result += 62;
		}else
		{
			if(DLNADMR_bSupportSF)
			result += 20;
			else if(DLNADMR_bSupportFBFFSBSF)
			result += 51;
		}
		#endif
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Stop) == TRUE)
	{
		result += 5;
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Pause) == TRUE)
	{
		result += 6;
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Next) == TRUE)
	{
		result += 5;
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Previous) == TRUE)
	{
		result += 9;
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Seek) == TRUE)
	{
		result += 5;
		#ifdef DLNADMRCTT
		if(DLNADMR_bPlayingAV)
		result += 16;
		#endif
		
	}

	return result;
}

char* GetTransportActions(unsigned short actions)
{
	unsigned long lActions = (unsigned long)actions;
	char* result = String_CreateSize(GetTransportActionsLength(actions));
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Play) == TRUE)
	{
		strcat(result, "Play");
		#ifdef DLNADMRCTT
		if(DLNADMR_bSupportTrickmode)
		{
		if(DLNADMR_bPlayingAV)
		strcat(result, ",X_DLNA_PS=-32\\,-16\\,-8\\,-2\\,-1/4\\,-1/16\\,1/16\\,1/4\\,8\\,16\\,32");
		}else
		{
			if(DLNADMR_bSupportSF)
			strcat(result, ",X_DLNA_PS=1/16\\,1/4");
			else if(DLNADMR_bSupportFBFFSBSF)
			strcat(result, ",X_DLNA_PS=-32\\,-16\\,-8\\,-2\\,-1/4\\,-1/16\\,8\\,16\\,32");
		}
		#endif
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Stop) == TRUE)
	{
		if(strlen(result) > 0)
		{
			strcat(result, ",");
		}
		strcat(result, "Stop");
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Pause) == TRUE)
	{
		if(strlen(result) > 0)
		{
			strcat(result, ",");
		}
		strcat(result, "Pause");
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Next) == TRUE)
	{
		if(strlen(result) > 0)
		{
			strcat(result, ",");
		}
		strcat(result, "Next");
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Previous) == TRUE)
	{
		if(strlen(result) > 0)
		{
			strcat(result, ",");
		}
		strcat(result, "Previous");
	}
	if(TESTBIT(lActions, (unsigned long)DMR_ATS_Seek) == TRUE)
	{
		if(strlen(result) > 0)
		{
			strcat(result, ",");
		}
		strcat(result, "Seek");
		#ifdef DLNADMRCTT
		if(DLNADMR_bPlayingAV)
		strcat(result, ",X_DLNA_SeekTime");
		#endif
	}

	return result;
}

void DMR_LastChangeTimerEvent(void* object)
{
    DMR dmr = (DMR)object;
    DMR_InternalState state = (DMR_InternalState)dmr->internal_state;
    int shift = 0;

    {
        if(state->LastChangeMask != 0)
        {
            struct timeval now;
            //gettimeofday(&(now), NULL);
	//CurrentTick = (tv.tv_sec*1000) + (tv.tv_usec/1000);
	//printf("[%d]\t\tnow:%lld, LastChangeTime:%lld, diff:%d\n", __LINE__, (now.tv_usec*1000+now.tv_sec/1000), (state->LastChangeTime.tv_usec*1000+state->LastChangeTime.tv_sec/1000), ((now.tv_usec*1000+now.tv_sec/1000) - (state->LastChangeTime.tv_usec*1000+state->LastChangeTime.tv_sec/1000)));
            //if( ((now.tv_usec*1000+now.tv_sec/1000) - (state->LastChangeTime.tv_usec*1000+state->LastChangeTime.tv_sec/1000)) < 180 )
            //    shift = (now.tv_usec*1000+now.tv_sec/1000) - (state->LastChangeTime.tv_usec*1000+state->LastChangeTime.tv_sec/1000);
            //else
            //if( ((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000)) < 190 )
            //{
            //    shift = 190 - ((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000));
            //    ILibLifeTime_Remove(state->DMR_Monitor, dmr);
            //    ILibLifeTime_AddEx(state->DMR_Monitor, dmr, shift, &DMR_LastChangeTimerEvent, NULL);
            //    return;
            //}
            //else
    //printf("%s stamp time, sec:%d, usec:%d, sum:%x\n", __func__, state->LastChangeTime.tv_sec, state->LastChangeTime.tv_usec, state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000);
    //printf("%s stamp time, sec:%d, usec:%d, sum:%x\n", __func__, now.tv_sec, now.tv_usec, now.tv_sec*1000+now.tv_usec/1000);
    //printf("diff:%d\n", ((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000)));
            {
                state->LastChangeMask = (EVENT_MASK_RENDERINGCONTROL|EVENT_MASK_AVTRANSPORT|EVENT_PRESETNAMELIST|EVENT_AVTOTHERS);
                FireGenaLastChangeEvent(dmr);
                state->LastChangeMask = 0;
            }
        }
    }

    ILibLifeTime_AddEx(state->DMR_Monitor, dmr, 200+shift, &DMR_LastChangeTimerEvent, NULL);
}

/* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1A vvv */
void DMR_LastChangeUpdateImmediate(void *object)
{
    struct timeval now;
    DMR dmr = (DMR)object;
    DMR_InternalState state = (DMR_InternalState)dmr->internal_state;

	//ltms->ExpirationTick = (tv.tv_sec*1000) + (tv.tv_usec/1000) + ms; 
    gettimeofday(&(now), NULL);
    //printf("[%d]\t\tnow:%lld, LastChangeTime:%lld, diff:%d\n", __LINE__, (now.tv_sec*1000+now.tv_usec/1000), (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000), ((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000)));
	//ltms->ExpirationTick = (tv.tv_sec*1000) + (tv.tv_usec/1000) + ms; 
    //printf("%s stamp time, sec:%d, usec:%d, sum:%x\n", __func__, state->LastChangeTime.tv_sec, state->LastChangeTime.tv_usec, state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000);
    //printf("%s stamp time, sec:%d, usec:%d, sum:%x\n", __func__, now.tv_sec, now.tv_usec, now.tv_sec*1000+now.tv_usec/1000);
    //printf("diff:%d\n", ((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000)));
    if( ((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000)) >= 180 )
    //if(1)//yunfeng  modify
	{
		printf("calling DMR_LastChangeUpdateImmediate %d",((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000)));
	
        state->LastChangeMask = (EVENT_MASK_RENDERINGCONTROL|EVENT_MASK_AVTRANSPORT|EVENT_PRESETNAMELIST|EVENT_AVTOTHERS);
        FireGenaLastChangeEvent(dmr);
        state->LastChangeMask = 0;
        ILibLifeTime_Remove(state->DMR_Monitor, dmr);
        ILibLifeTime_AddEx(state->DMR_Monitor, dmr, 200, &DMR_LastChangeTimerEvent, NULL);
    }
	else
	{
		printf("Will  not call FireGenaLastChangeEvent\n");
	}
}
/* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.93.1A ^^^ */

void FireGenaLastChangeEvent(DMR instance)
{
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1A vvv */
    //        struct timeval now;
    //        gettimeofday(&(now), NULL);
    //printf("%s stamp time, sec:%d, usec:%d, sum:%x\n", __func__, state->LastChangeTime.tv_sec, state->LastChangeTime.tv_usec, state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000);
    //printf("diff:%d\n", ((now.tv_sec*1000+now.tv_usec/1000) - (state->LastChangeTime.tv_sec*1000+state->LastChangeTime.tv_usec/1000)));
    gettimeofday(&(state->LastChangeTime), NULL);
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.93.1A ^^^ */
    char* tmp = (char*)malloc(128);
    { /* RenderingControl */
        int renderingDataLen = 123;
        char* renderingData = NULL;

        DMR_Lock(instance);
#if defined(INCLUDE_FEATURE_DISPLAY)
        if(TESTBIT(state->LastChangeMask, EVENT_CONTRAST) == TRUE)
        {
            renderingDataLen += 28;
        }
        if(TESTBIT(state->LastChangeMask, EVENT_BRIGHTNESS) == TRUE)
        {
            renderingDataLen += 30;
        }
#endif /* INCLUDE_FEATURE_DISPLAY */
#if defined(INCLUDE_FEATURE_VOLUME)
        if(TESTBIT(state->LastChangeMask, EVENT_VOLUME) == TRUE)
        {
            renderingDataLen += 43;
        }
        if(TESTBIT(state->LastChangeMask, EVENT_MUTE) == TRUE)
        {
            renderingDataLen += 38;
        }
#endif /* INCLUDE_FEATURE_VOLUME */
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1 vvv */
        if(TESTBIT(state->LastChangeMask, EVENT_PRESETNAMELIST) == TRUE)
        {
			renderingDataLen += (int)strlen("<PresetNameList val=") + 4;
            int i = 0;
            for(i = 0; i < (DMR__StateVariable_AllowedValues_MAX - 1); i++)
            {
                if(state->PresetNames[i] != NULL)
                {
                    renderingDataLen += (int)strlen((const char*)state->PresetNames[i]) + 1;
                }
            }
        }
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.93.1 ^^^ */

        renderingData = (char*)malloc(renderingDataLen);
        strcpy(renderingData, "<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/RCS/\"><InstanceID val=\"0\">");

#if defined(INCLUDE_FEATURE_DISPLAY)
        if(TESTBIT(state->LastChangeMask, EVENT_CONTRAST) == TRUE)
        {
            sprintf(tmp, "%d", (int)state->Contrast);
            strcat(renderingData, "<Contrast val=\"");
            strcat(renderingData, tmp);
            strcat(renderingData, "\"/>");
        }
        if(TESTBIT(state->LastChangeMask, EVENT_BRIGHTNESS) == TRUE)
        {
            sprintf(tmp, "%d", (int)state->Brightness);
            strcat(renderingData, "<Brightness val=\"");
            strcat(renderingData, tmp);
            strcat(renderingData, "\"/>");
        }
#endif /* INCLUDE_FEATURE_DISPLAY */
#if defined(INCLUDE_FEATURE_VOLUME)
        if(TESTBIT(state->LastChangeMask, EVENT_VOLUME) == TRUE)
        {
            sprintf(tmp, "%d", (int)state->Volume);
            strcat(renderingData, "<Volume channel=\"Master\" val=\"");
            strcat(renderingData, tmp);
            strcat(renderingData, "\"/>");
        }
        if(TESTBIT(state->LastChangeMask, EVENT_MUTE) == TRUE)
        {
            strcat(renderingData, "<Mute channel=\"Master\" val=\"");
            if(state->Mute == TRUE)
            {
                strcat(renderingData, "1");
            }
            else
            {
                strcat(renderingData, "0");
            }
            strcat(renderingData, "\"/>");
        }
#endif /* INCLUDE_FEATURE_VOLUME */
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1 vvv */
        if(TESTBIT(state->LastChangeMask, EVENT_PRESETNAMELIST) == TRUE)
        {
            int i = 0;
            strcat(renderingData, "<PresetNameList val=\"");
            for(i = 0; i < (DMR__StateVariable_AllowedValues_MAX - 1); i++)
            {
                if(state->PresetNames[i] != NULL)
                {
                    strcat(renderingData, ",");
                    strcat(renderingData, (const char*)state->PresetNames[i]);
                }
            }
            strcat(renderingData, "\"/>");
        }
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.93.1 ^^^ */

        strcat(renderingData, "</InstanceID></Event>");
        DMR_Unlock(instance);
        DMR_SetState_RenderingControl_LastChange(state->DMR_microStack, renderingData);
		OutputDebugString("RenderingControl: Gena Event Fired!\n");
        
        free(renderingData);
    }

    { /* AVTransport */
        char* transportDidl = NULL;
        //int transportDidlLen = 0;//remove since no use, 20090513, yuyu
        char* trackDidl = NULL;
        //int trackDidlLen = 0;//remove since no use, 20090513, yuyu
        int AVTransportDataLen = 126;
        char* AVTransportData = NULL;
        DMR_Lock(instance);

        if(TESTBIT(state->LastChangeMask, EVENT_ABSOLUTETIMEPOSITION) == TRUE)
        {
            char* t = MillisecondsToTimeString(state->AbsoluteTimePosition);
			int length = (int)strlen(t);
			String_Destroy(t);
            AVTransportDataLen += 36;
			AVTransportDataLen += (length < 15)?15:length;
        }
        if(TESTBIT(state->LastChangeMask, EVENT_RELATIVETIMEPOSITION) == TRUE)
        {
            char* t = MillisecondsToTimeString(state->RelativeTimePosition);
			int length = (int)strlen(t);
			String_Destroy(t);
            AVTransportDataLen += 36;
			AVTransportDataLen += (length < 15)?15:length;
        }
        if(TESTBIT(state->LastChangeMask, EVENT_TRANSPORTSTATE) == TRUE)
        {
            AVTransportDataLen += 30;
            AVTransportDataLen += 16;
        }

        if(TESTBIT(state->LastChangeMask, EVENT_TRANSPORTSTATUS) == TRUE)
        {
            AVTransportDataLen += 31;
			AVTransportDataLen += 14;
        }

        if(TESTBIT(state->LastChangeMask, EVENT_CURRENTPLAYMODE) == TRUE)
        {
            AVTransportDataLen += 31;
            AVTransportDataLen += 15;
        }

        if(TESTBIT(state->LastChangeMask, EVENT_TRANSPORTPLAYSPEED) == TRUE)
        {
            AVTransportDataLen += 34;
            AVTransportDataLen += (int)strlen(state->TransportPlaySpeed);
        }

        if(TESTBIT(state->LastChangeMask, EVENT_NUMBEROFTRACKS) == TRUE)
        {
            AVTransportDataLen += 30;
            AVTransportDataLen += 16;
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACK) == TRUE)
        {
            AVTransportDataLen += 28;
            AVTransportDataLen += 16;
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACKDURATION) == TRUE)
        {
            char* t = MillisecondsToTimeString(state->CurrentTrackDuration);
			int length = (int)strlen(t);
			String_Destroy(t);
            AVTransportDataLen += 36;
			AVTransportDataLen += (length < 15)?15:length;
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTMEDIADURATION) == TRUE)
        {
            char* t = MillisecondsToTimeString(state->CurrentMediaDuration);
			int length = (int)strlen(t);
			String_Destroy(t);
            AVTransportDataLen += 36;
			AVTransportDataLen += (length < 15)?15:length;
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACKMETADATA) == TRUE)
        {
            if(state->CurrentTrackMetaData != NULL && strlen(state->CurrentTrackMetaData) != 0 )
			{
                /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.100.6A,B,C vvv */
				//char* l1metadata = _MakeMetadataConformant(state->CurrentTrackMetaData);
    			AVTransportDataLen += 36;
                AVTransportDataLen += strlen(state->CurrentTrackMetaData);
    			//AVTransportDataLen += ILibXmlEscapeLength(l1metadata);
				//String_Destroy(l1metadata);
                /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.100.6A,B,C ^^^ */
			}
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACKURI) == TRUE)
        {
            if(state->CurrentTrackURI != NULL)
			{
				char* localUri = String_CreateSize(ILibXmlEscapeLength(state->CurrentTrackURI));
				ILibXmlEscape(localUri, state->CurrentTrackURI);
				AVTransportDataLen += 31;
				AVTransportDataLen += ILibXmlEscapeLength(localUri);
				String_Destroy(localUri);
			}
        }

		if(TESTBIT(state->LastChangeMask, EVENT_AVTRANSPORTURI) == TRUE)
        {
            if(state->AVTransportURI)
			{
				char* localUri = String_CreateSize(ILibXmlEscapeLength(state->AVTransportURI));
				ILibXmlEscape(localUri, state->AVTransportURI);
				AVTransportDataLen += 30;
				AVTransportDataLen += ILibXmlEscapeLength(localUri);
				String_Destroy(localUri);
			}
        }
		
		if(TESTBIT(state->LastChangeMask, EVENT_NEXTAVTRANSPORTURI) == TRUE)
		{
			if(state->NextAVTransportURI)
			{
				char* nextUri = String_CreateSize(ILibXmlEscapeLength(state->NextAVTransportURI));
				ILibXmlEscape(nextUri, state->NextAVTransportURI);
				AVTransportDataLen += 34;
				AVTransportDataLen += ILibXmlEscapeLength(nextUri);
				String_Destroy(nextUri);
			}
		}

		if(TESTBIT(state->LastChangeMask, EVENT_AVTRANSPORTURIMETADATA) == TRUE)
        {
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 vvv */
            if(state->AVTransportURIMetaData)
			{
				//char* l2metadata = _MakeMetadataConformant(state->AVTransportURIMetaData);
    			AVTransportDataLen += 38;
    			AVTransportDataLen += strlen(state->AVTransportURIMetaData);
    			//AVTransportDataLen += ILibXmlEscapeLength(l2metadata);
				//String_Destroy(l2metadata);
			}
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 ^^^ */
        }

		if(TESTBIT(state->LastChangeMask, EVENT_NEXTAVTRANSPORTURIMETADATA) == TRUE)
        {
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 vvv */
            if(state->NextAVTransportURIMetaData)
			{
				//char* l2metadata = _MakeMetadataConformant(state->AVTransportURIMetaData);
    			AVTransportDataLen += 42;
    			AVTransportDataLen += strlen(state->NextAVTransportURIMetaData);
    			//AVTransportDataLen += ILibXmlEscapeLength(l2metadata);
				//String_Destroy(l2metadata);
			}
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 ^^^ */
        }
	
		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRANSPORTACTIONS) == TRUE)
        {
            AVTransportDataLen += 39;
            AVTransportDataLen += GetTransportActionsLength(state->CurrentTransportActions);
        }

    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1 vvv */
        if(TESTBIT(state->LastChangeMask, EVENT_AVTOTHERS) == TRUE)
        {
            AVTransportDataLen += (int)strlen("<CurrentRecordQualityMode val=\"NOT_IMPLEMENTED\"/>");
            //AVTransportDataLen += (int)strlen("<NextAVTransportURI val=\"NOT_IMPLEMENTED\"/>");
            AVTransportDataLen += (int)strlen("<PossibleRecordQualityModes val=\"NOT_IMPLEMENTED\"/>");
            //AVTransportDataLen += (int)strlen("<NextAVTransportURIMetaData val=\"NOT_IMPLEMENTED\"/>");
            AVTransportDataLen += (int)strlen("<PlaybackStorageMedium val=\"NOT_IMPLEMENTED\"/>");
            AVTransportDataLen += (int)strlen("<RecordMediumWriteStatus val=\"NOT_IMPLEMENTED\"/>");
            AVTransportDataLen += (int)strlen("<PossiblePlaybackStorageMedia val=\"NOT_IMPLEMENTED\"/>");
            AVTransportDataLen += (int)strlen("<PossibleRecordStorageMedia val=\"NOT_IMPLEMENTED\"/>");
			AVTransportDataLen += (int)strlen("<RecordStorageMedium val=\"NOT_IMPLEMENTED\"/>");
        }
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.93.1 ^^^ */

		AVTransportData = (char*)String_CreateSize(AVTransportDataLen);
        strcpy(AVTransportData, "<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT/\"><InstanceID val=\"0\">");

        if(TESTBIT(state->LastChangeMask, EVENT_ABSOLUTETIMEPOSITION) == TRUE)
        {
            char* atp = NULL;
			if(state->AbsoluteTimePosition < 0)
			{
				atp = String_Create("NOT_IMPLEMENTED");
			}
			else
			{
	            atp = MillisecondsToTimeString(state->AbsoluteTimePosition);
			}
            strcat(AVTransportData, "<AbsoluteTimePosition val=\"");
            strcat(AVTransportData, (const char*)atp);
            strcat(AVTransportData, "\"/>");
            String_Destroy(atp);
        }
        if(TESTBIT(state->LastChangeMask, EVENT_RELATIVETIMEPOSITION) == TRUE)
        {
            char* atp = NULL;
			if(state->RelativeTimePosition < 0)
			{
				atp = String_Create("NOT_IMPLEMENTED");
			}
			else
			{
	            atp = MillisecondsToTimeString(state->RelativeTimePosition);
			}
            strcat(AVTransportData, "<RelativeTimePosition val=\"");
            strcat(AVTransportData, (const char*)atp);
            strcat(AVTransportData, "\"/>");
            String_Destroy(atp);
        }
        if(TESTBIT(state->LastChangeMask, EVENT_TRANSPORTSTATE) == TRUE)
        {
            strcat(AVTransportData, "<TransportState val=\"");
            if(state->TransportState == DMR_PS_Stopped)
            {
                strcat(AVTransportData, "STOPPED");
            }
            else if(state->TransportState == DMR_PS_Playing)
            {
                strcat(AVTransportData, "PLAYING");
            }
            else if(state->TransportState == DMR_PS_Transitioning)
            {
                strcat(AVTransportData, "TRANSITIONING");
            }
            else if(state->TransportState == DMR_PS_Paused)
            {
                strcat(AVTransportData, "PAUSED_PLAYBACK");
            }
            else if(state->TransportState == DMR_PS_NoMedia)
            {
                strcat(AVTransportData, "NO_MEDIA_PRESENT");
            }
            strcat(AVTransportData, "\"/>");
        }

        if(TESTBIT(state->LastChangeMask, EVENT_TRANSPORTSTATUS) == TRUE)
        {
            strcat(AVTransportData, "<TransportStatus val=\"");
            if(state->TransportStatus == DMR_TS_OK)
            {
                strcat(AVTransportData, "OK");
            }
            else if(state->TransportStatus == DMR_TS_ERROR_OCCURRED)
            {
                strcat(AVTransportData, "ERROR_OCCURRED");
            }
            strcat(AVTransportData, "\"/>");
        }

        if(TESTBIT(state->LastChangeMask, EVENT_CURRENTPLAYMODE) == TRUE)
        {
            strcat(AVTransportData, "<CurrentPlayMode val=\"");
            if(state->CurrentPlayMode == DMR_MPM_Normal)
            {
                strcat(AVTransportData, "NORMAL");
            }
            else if(state->CurrentPlayMode == DMR_MPM_Shuffle)
            {
                strcat(AVTransportData, "SHUFFLE");
            }
            else if(state->CurrentPlayMode == DMR_MPM_Repeat_One)
            {
                strcat(AVTransportData, "REPEAT_ONE");
            }
            else if(state->CurrentPlayMode == DMR_MPM_Repeat_All)
            {
                strcat(AVTransportData, "REPEAT_ALL");
            }
            else if(state->CurrentPlayMode == DMR_MPM_Random)
            {
                strcat(AVTransportData, "RANDOM");
            }
            else if(state->CurrentPlayMode == DMR_MPM_Direct_One)
            {
                strcat(AVTransportData, "DIRECT_1");
            }
            else if(state->CurrentPlayMode == DMR_MPM_Intro)
            {
                strcat(AVTransportData, "INTRO");
            }
            strcat(AVTransportData, "\"/>");
        }

        if(TESTBIT(state->LastChangeMask, EVENT_TRANSPORTPLAYSPEED) == TRUE)
        {
            strcat(AVTransportData, "<TransportPlaySpeed val=\"");
            strcat(AVTransportData, state->TransportPlaySpeed);
            strcat(AVTransportData, "\"/>");
        }

        if(TESTBIT(state->LastChangeMask, EVENT_NUMBEROFTRACKS) == TRUE)
        {
            sprintf(tmp, "%d", state->NumberOfTracks);
			strcat(AVTransportData, "<NumberOfTracks val=\"");
			strcat(AVTransportData, tmp);
			strcat(AVTransportData, "\"/>");
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACK) == TRUE)
        {
            sprintf(tmp, "%d", state->CurrentTrack);
			strcat(AVTransportData, "<CurrentTrack val=\"");
			strcat(AVTransportData, tmp);
			strcat(AVTransportData, "\"/>");
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACKDURATION) == TRUE)
        {
            char* str = NULL;
			if(state->CurrentTrackDuration < 0L)
			{
				str = String_Create("NOT_IMPLEMENTED");
			}
			else
			{
				str = MillisecondsToTimeString(state->CurrentTrackDuration);
			}
			strcat(AVTransportData, "<CurrentTrackDuration val=\"");
            strcat(AVTransportData, str);
			strcat(AVTransportData, "\"/>");
            String_Destroy(str);
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTMEDIADURATION) == TRUE)
        {
            char* str = NULL;
			if(state->CurrentMediaDuration < 0)
			{
				str = String_Create("NOT_IMPLEMENTED");
			}
			else
			{
				str = MillisecondsToTimeString(state->CurrentMediaDuration);
			}
			strcat(AVTransportData, "<CurrentMediaDuration val=\"");
            strcat(AVTransportData, str);
			strcat(AVTransportData, "\"/>");
            String_Destroy(str);
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACKMETADATA) == TRUE)
        {
            if(state->CurrentTrackMetaData != NULL)
			{
                /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.100.6A,B,C vvv */
				//char* l1metadata = _MakeMetadataConformant(state->CurrentTrackMetaData);
				strcat(AVTransportData, "<CurrentTrackMetaData val=\"");
                strcat(AVTransportData, state->CurrentTrackMetaData);
                //sprintf(AVTransportData, "%s%s", AVTransportData, state->CurrentTrackMetaData);
                //strcat(AVTransportData, l1metadata);
				strcat(AVTransportData, "\"/>");
				//String_Destroy(l1metadata);
                /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.100.6A,B,C ^^^ */
			}
        }

		if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRACKURI) == TRUE)
        {
            if(state->CurrentTrackURI != NULL)
			{
				//char* uri = NULL; //remove since no use, 20090513, yuyu
				char* localUri = String_CreateSize(ILibXmlEscapeLength(state->CurrentTrackURI));
				ILibXmlEscape(localUri, state->CurrentTrackURI);
				strcat(AVTransportData, "<CurrentTrackURI val=\"");
				strcat(AVTransportData, localUri);
				strcat(AVTransportData, "\"/>");
				String_Destroy(localUri);
			}
        }

		if(TESTBIT(state->LastChangeMask, EVENT_AVTRANSPORTURI) == TRUE)
        {
            if(state->AVTransportURI != NULL)
			{
				//char* uri = NULL; //remove since no use, 20090513, yuyu
				char* localUri = String_CreateSize(ILibXmlEscapeLength(state->AVTransportURI));
				ILibXmlEscape(localUri, state->AVTransportURI);
				strcat(AVTransportData, "<AVTransportURI val=\"");
				strcat(AVTransportData, localUri);
				strcat(AVTransportData, "\"/>");
				String_Destroy(localUri);
			}
        }
		if(TESTBIT(state->LastChangeMask, EVENT_NEXTAVTRANSPORTURI) == TRUE)
		{
			if(state->NextAVTransportURI != NULL)
			{
				//char* uri = NULL; //remove since no use, 20090513, yuyu
				char* nextUri = String_CreateSize(ILibXmlEscapeLength(state->NextAVTransportURI));
				ILibXmlEscape(nextUri, state->NextAVTransportURI);
				strcat(AVTransportData, "<NextAVTransportURI val=\"");
				strcat(AVTransportData, nextUri);
				strcat(AVTransportData, "\"/>");
				String_Destroy(nextUri);
			}
		}

		if(TESTBIT(state->LastChangeMask, EVENT_AVTRANSPORTURIMETADATA) == TRUE)
        {
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 vvv */
            if(state->AVTransportURIMetaData != NULL)
			{
				//char* l2metadata = _MakeMetadataConformant(state->AVTransportURIMetaData);
				strcat(AVTransportData, "<AVTransportURIMetaData val=\"");
                strcat(AVTransportData, state->AVTransportURIMetaData );
                //strcat(AVTransportData, l2metadata);
				strcat(AVTransportData, "\"/>");
				//String_Destroy(l2metadata);
			}
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 ^^^ */
        }
		if(TESTBIT(state->LastChangeMask, EVENT_NEXTAVTRANSPORTURIMETADATA) == TRUE)
		{
	/* vvv Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 vvv */
			if(state->NextAVTransportURIMetaData != NULL)
			{
				//char* l2metadata = _MakeMetadataConformant(state->AVTransportURIMetaData);
				strcat(AVTransportData, "<NextAVTransportURIMetaData val=\"");
				strcat(AVTransportData, state->NextAVTransportURIMetaData );
				//strcat(AVTransportData, l2metadata);
				strcat(AVTransportData, "\"/>");
				//String_Destroy(l2metadata);
			}
	/* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.101.0,0A,4/7.3.101.7 ^^^ */
		}

        if(TESTBIT(state->LastChangeMask, EVENT_CURRENTTRANSPORTACTIONS) == TRUE)
        {
            char* tmp2 = GetTransportActions(state->CurrentTransportActions);
			strcat(AVTransportData, "<CurrentTransportActions val=\"");
			strcat(AVTransportData, tmp2);
			strcat(AVTransportData, "\"/>");
			String_Destroy(tmp2);
        }

    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1 vvv */
        if(TESTBIT(state->LastChangeMask, EVENT_AVTOTHERS) == TRUE)
        {
            strcat(AVTransportData, "<CurrentRecordQualityMode val=\"NOT_IMPLEMENTED\"/>");
            //strcat(AVTransportData, "<NextAVTransportURI val=\"NOT_IMPLEMENTED\"/>");
            strcat(AVTransportData, "<PossibleRecordQualityModes val=\"NOT_IMPLEMENTED\"/>");
            //strcat(AVTransportData, "<NextAVTransportURIMetaData val=\"NOT_IMPLEMENTED\"/>");
            strcat(AVTransportData, "<PlaybackStorageMedium val=\"NOT_IMPLEMENTED\"/>");
            strcat(AVTransportData, "<RecordMediumWriteStatus val=\"NOT_IMPLEMENTED\"/>");
            strcat(AVTransportData, "<PossiblePlaybackStorageMedia val=\"NOT_IMPLEMENTED\"/>");
            strcat(AVTransportData, "<PossibleRecordStorageMedia val=\"NOT_IMPLEMENTED\"/>");
			strcat(AVTransportData, "<RecordStorageMedium val=\"NOT_IMPLEMENTED\"/>");
        }
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.93.1 ^^^ */

        DMR_Unlock(instance);
        strcat(AVTransportData, "</InstanceID></Event>");
		{
			int actualLength = (int)strlen((const char*)AVTransportData);
			int allocatedLength = AVTransportDataLen;
			if(allocatedLength < actualLength)
			{
				OutputDebugString("FATAL MEMORY ERROR!");
			}
		}
        DMR_SetState_AVTransport_LastChange(state->DMR_microStack, AVTransportData);
		OutputDebugString("AVTransport: Gena Event Fired!\n");
        
        String_Destroy(AVTransportData);
        FREE(transportDidl);
        FREE(trackDidl);
    }
    free(tmp);
}
/****************************************************************************/


/****************************************************************************/
/* Methods Called by the Application to Set the LastChange evented variable.*/
DMR_Error DMR_StateChange_SinkProtocolInfo(DMR instance, char* info)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    if(info != NULL)
    {
        char* newValue = NULL;
        char* saved = NULL;
        if(strcmp((const char*)istate->ProtocolInfo, (const char*)info) == 0)
        {
            return DMR_ERROR_OK;
        }
        DMR_Lock(instance);
        saved = istate->ProtocolInfo;
        DMR_Unlock(instance);
        newValue = String_Create((char*)info);
        if(newValue != NULL)
        {
            DMR_Lock(instance);
            istate->ProtocolInfo = newValue;
            DMR_Unlock(instance);
            DMR_SetState_ConnectionManager_SinkProtocolInfo(istate->DMR_microStack, (char*)info);
            String_Destroy(saved);
            return DMR_ERROR_OK;
        }
        else
        {
            return DMR_ERROR_OUTOFMEMORY;
        }
    }
    else
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
}

DMR_Error DMR_StateChange_TransportPlayState(DMR instance, DMR_PlayState state)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.98.4B vvv */
    //if((int)state < 1 || (int)state > 63)
    if((int)state < 0 || (int)state > 63)
    /* ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.98.4B ^^^ */
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
    DMR_Lock(instance);
    if(istate->TransportState != state)
    {
        istate->TransportState = state;
        istate->LastChangeMask |= EVENT_TRANSPORTSTATE;
    }
    DMR_Unlock(instance);

	#ifdef DLNADMRCTT
    if(state == DMR_PS_Paused)
       DMR_StateChange_CurrentTransportActions(instance,(unsigned short)(istate->CurrentTransportActions & 0x3b));
  #else
		if(state == DMR_PS_Playing)
		   DMR_StateChange_CurrentTransportActions(instance,(unsigned short)(istate->CurrentTransportActions & ~DMR_ATS_Play));
		else
		   DMR_StateChange_CurrentTransportActions(instance,(unsigned short)(istate->CurrentTransportActions | DMR_ATS_Play));
    #endif

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_TransportPlaySpeed(DMR instance, char* playSpeed)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    DMR_Lock(instance);
    if(strcmp((const char*)istate->TransportPlaySpeed, (const char*)playSpeed) != 0)
    {
        String_Destroy(istate->TransportPlaySpeed);
        istate->TransportPlaySpeed = String_Create((const char*)playSpeed);
        istate->LastChangeMask |= EVENT_TRANSPORTPLAYSPEED;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_TransportStatus(DMR instance, DMR_TransportStatus status)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    if((int)status < 0 || (int)status > 4)
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
    DMR_Lock(instance);
    if(istate->TransportStatus != status)
    {
        istate->TransportStatus = status;
        istate->LastChangeMask |= EVENT_TRANSPORTSTATUS;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_CurrentTransportActions(DMR instance, unsigned short allowedActions)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    if(allowedActions < 0 || allowedActions > 63)
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
    DMR_Lock(instance);
    if(istate->CurrentTransportActions != allowedActions)
    {
        istate->CurrentTransportActions = allowedActions;
        istate->LastChangeMask |= EVENT_CURRENTTRANSPORTACTIONS;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

unsigned short DMR_Internal_GetCurrentTransportActions(DMR instance)
{
	DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    return istate->CurrentTransportActions;
}

DMR_Error DMR_StateChange_NumberOfTracks(DMR instance, unsigned int maxNumberOfTracks)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    DMR_Lock(instance);
    if(istate->NumberOfTracks != maxNumberOfTracks)
    {
        istate->NumberOfTracks = maxNumberOfTracks;
        istate->LastChangeMask |= EVENT_NUMBEROFTRACKS;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_CurrentTrack(DMR instance, unsigned int index)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    if(index > istate->NumberOfTracks)
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
    DMR_Lock(instance);
    if(istate->CurrentTrack != index)
    {
        istate->CurrentTrack = index;
        istate->LastChangeMask |= EVENT_CURRENTTRACK;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_CurrentPlayMode(DMR instance, DMR_MediaPlayMode mode)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    if(mode < 0 || mode > 6)
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
    DMR_Lock(instance);
    if(istate->CurrentPlayMode != mode)
    {
        istate->CurrentPlayMode = mode;
        istate->LastChangeMask |= EVENT_CURRENTPLAYMODE;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_CurrentTrackURI(DMR instance, char* trackURI)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    DMR_Lock(instance);
    if(trackURI != NULL && strcmp(trackURI, istate->CurrentTrackURI) != 0)
    {
        String_Destroy(istate->CurrentTrackURI);
        istate->CurrentTrackURI = String_Create(trackURI);
        istate->LastChangeMask |= EVENT_CURRENTTRACKURI;
    }
    else if(trackURI == NULL)
    {
        String_Destroy(istate->CurrentTrackURI);
        istate->CurrentTrackURI = String_Create("");
        istate->LastChangeMask |= EVENT_CURRENTTRACKURI;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_CurrentTrackMetaData(DMR instance, struct CdsObject* trackMetadata)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }

    DMR_Lock(instance);
    if(trackMetadata != NULL)
    {
        int didlSize = 0;
        char* didl = CDS_SerializeObjectToDidl(trackMetadata, 1, CdsFilter_ResAllAttribs, 1, &didlSize);
        if(didl != NULL)
        {
            if(strcmp(istate->CurrentTrackMetaData, didl) != 0)
            {
                String_Destroy(istate->CurrentTrackMetaData);
                istate->CurrentTrackMetaData = String_Create(didl);
                istate->LastChangeMask |= EVENT_CURRENTTRACKMETADATA;
            }
            free(didl);
        }
        else
        {
            DMR_Unlock(instance);
            return DMR_ERROR_BADMETADATA;
        }
    }
    else
    {
        String_Destroy(istate->CurrentTrackMetaData);
        istate->CurrentTrackMetaData = String_Create("NOT_IMPLEMENTED");
        istate->LastChangeMask |= EVENT_CURRENTTRACKMETADATA;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_CurrentTrackDuration(DMR instance, long duration)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    if(duration < 0 
	//v modified for DLNA 1.5 CTT DMR, MM 7.3.100.6A,B,C
	   && duration != -1)
	//^ modified for DLNA 1.5 CTT DMR, MM 7.3.100.6A,B,C
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
    DMR_Lock(instance);
    if(istate->CurrentTrackDuration != duration)
    {
        istate->CurrentTrackDuration = duration;
		printf("xxxxxxxxxxxxx CurrentTrackDuration=%d\n",duration);
        istate->LastChangeMask |= EVENT_CURRENTTRACKDURATION;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

#if defined(INCLUDE_FEATURE_VOLUME)

	DMR_Error DMR_StateChange_Volume(DMR instance, unsigned char volume)
    {
        DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
        DMR_Error err = CheckThis(instance);
        if(err != DMR_ERROR_OK)
        {
            return err;
        }
        if(volume > VOLUME_MAX)
        {
            return DMR_ERROR_VOLUMEOUTOFRANGE;
        }
        DMR_Lock(instance);
        if(istate->Volume != volume)
        {
            istate->Volume = volume;
            istate->LastChangeMask |= EVENT_VOLUME;
        }
        DMR_Unlock(instance);

        return DMR_ERROR_OK;
    }

	DMR_Error DMR_StateChange_Mute(DMR instance, BOOL mute)
    {
        DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
        DMR_Error err = CheckThis(instance);
        if(err != DMR_ERROR_OK)
        {
            return err;
        }
        if(mute != FALSE && mute != TRUE)
        {
            return DMR_ERROR_INVALIDARGUMENT;
        }
        DMR_Lock(instance);
        if(istate->Mute != mute)
        {
            istate->Mute = mute;
            istate->LastChangeMask |= EVENT_MUTE;
        }
        DMR_Unlock(instance);

        return DMR_ERROR_OK;
    }

#endif /* INCLUDE_FEATURE_VOLUME */

#if defined(INCLUDE_FEATURE_DISPLAY)

	DMR_Error DMR_StateChange_Contrast(DMR instance, unsigned char contrast)
    {
        DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
        DMR_Error err = CheckThis(instance);
        if(err != DMR_ERROR_OK)
        {
            return err;
        }
        if(contrast > CONTRAST_MAX)
        {
            return DMR_ERROR_CONTRASTOUTOFRANGE;
        }
        DMR_Lock(instance);
        if(istate->Contrast != contrast)
            //if(TESTBIT(istate->LastChangeMask, EVENT_CONTRAST) == TRUE)
            if(0)
            {
                struct timeval now;
                gettimeofday(&(now), NULL);
	printf("[%d]\t\tnow:%lld, LastChangeTime:%lld, diff:%d\n", __LINE__, (now.tv_usec*1000+now.tv_sec/1000), (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000), ((now.tv_usec*1000+now.tv_sec/1000) - (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000)));
                if( ((now.tv_usec*1000+now.tv_sec/1000) - (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000)) < 180 )
                    usleep( 180-((now.tv_usec*1000+now.tv_sec/1000) - (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000)) );
                DMR_Unlock(instance);
                FireGenaLastChangeEvent(instance);
            }
            else
                DMR_Unlock(instance);
        else
            DMR_Unlock(instance);

        DMR_Lock(instance);
        if(istate->Contrast != contrast)
        {
            istate->Contrast = contrast;
            istate->LastChangeMask |= EVENT_CONTRAST;
        }
        DMR_Unlock(instance);

        return DMR_ERROR_OK;
    }

	DMR_Error DMR_StateChange_Brightness(DMR instance, unsigned char brightness)
    {
        DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
        DMR_Error err = CheckThis(instance);
        if(err != DMR_ERROR_OK)
        {
            return err;
        }
        if(brightness > BRIGHTNESS_MAX)
        {
            return DMR_ERROR_BRIGHTNESSOUTOFRANGE;
        }
        DMR_Lock(instance);
        if(istate->Brightness != brightness)
            //if(TESTBIT(istate->LastChangeMask, EVENT_BRIGHTNESS) == TRUE)
            if(0)
            {
                struct timeval now;
                gettimeofday(&(now), NULL);
	printf("[%d]\t\tnow:%lld, LastChangeTime:%lld, diff:%d\n", __LINE__, (now.tv_usec*1000+now.tv_sec/1000), (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000), ((now.tv_usec*1000+now.tv_sec/1000) - (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000)));
                if( ((now.tv_usec*1000+now.tv_sec/1000) - (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000)) < 180 )
                    usleep( 180-((now.tv_usec*1000+now.tv_sec/1000) - (istate->LastChangeTime.tv_usec*1000+istate->LastChangeTime.tv_sec/1000)) );
                DMR_Unlock(instance);
                FireGenaLastChangeEvent(instance);
            }
            else
                DMR_Unlock(instance);
        else
            DMR_Unlock(instance);

        DMR_Lock(instance);
        if(istate->Brightness != brightness)
        {
            istate->Brightness = brightness;
            istate->LastChangeMask |= EVENT_BRIGHTNESS;
        }
        DMR_Unlock(instance);

        return DMR_ERROR_OK;
    }

#endif /* INCLUDE_FEATURE_DISPLAY */

DMR_Error DMR_StateChange_AVTransportURI(DMR instance, char* uri)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    DMR_Lock(instance);
    if(uri != NULL && strcmp(uri, istate->AVTransportURI) != 0)
    {
        String_Destroy(istate->AVTransportURI);
        istate->AVTransportURI = String_Create(uri);
        istate->LastChangeMask |= EVENT_AVTRANSPORTURI;
    }
    else if(uri == NULL)
    {
        String_Destroy(istate->AVTransportURI);
        istate->AVTransportURI = String_Create("");
        istate->LastChangeMask |= EVENT_AVTRANSPORTURI;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}
DMR_Error DMR_StateChange_NextAVTransportURI(DMR instance, char* uri)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    DMR_Lock(instance);
    if(uri != NULL && strcmp(uri, istate->NextAVTransportURI) != 0)
    {
        String_Destroy(istate->NextAVTransportURI);
        istate->NextAVTransportURI = String_Create(uri);
        istate->LastChangeMask |= EVENT_NEXTAVTRANSPORTURI;
    }
    else if(uri == NULL)
    {
        String_Destroy(istate->NextAVTransportURI);
        istate->NextAVTransportURI = String_Create("");
        istate->LastChangeMask |= EVENT_NEXTAVTRANSPORTURI;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_AVTransportURIMetaData(DMR instance, struct CdsObject* metadata)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }

    DMR_Lock(instance);
    if(metadata != NULL)
    {
        int didlSize = 0;
        char* didl = CDS_SerializeObjectToDidl(metadata, 1, CdsFilter_ResAllAttribs, 1, &didlSize);
        if(didl != NULL)
        {
            if(strcmp(istate->AVTransportURIMetaData, didl) != 0)
            {
                String_Destroy(istate->AVTransportURIMetaData);
                istate->AVTransportURIMetaData = String_Create(didl);
                istate->LastChangeMask |= EVENT_AVTRANSPORTURIMETADATA;
            }
            free(didl);
        }
        else
        {
            DMR_Unlock(instance);
            return DMR_ERROR_BADMETADATA;
        }
    }
    else
    {
        String_Destroy(istate->AVTransportURIMetaData);
        istate->AVTransportURIMetaData = String_Create("NOT_IMPLEMENTED");
        istate->LastChangeMask |= EVENT_AVTRANSPORTURIMETADATA;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}
DMR_Error DMR_StateChange_NextAVTransportURIMetaData(DMR instance, struct CdsObject* metadata)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }

    DMR_Lock(instance);
    if(metadata != NULL)
    {
        int didlSize = 0;
        char* didl = CDS_SerializeObjectToDidl(metadata, 1, CdsFilter_ResAllAttribs, 1, &didlSize);
        if(didl != NULL)
        {
            if(strcmp(istate->NextAVTransportURIMetaData, didl) != 0)
            {
                String_Destroy(istate->NextAVTransportURIMetaData);
                istate->NextAVTransportURIMetaData = String_Create(didl);
                istate->LastChangeMask |= EVENT_NEXTAVTRANSPORTURIMETADATA;
            }
            free(didl);
        }
        else
        {
            DMR_Unlock(instance);
            return DMR_ERROR_BADMETADATA;
        }
    }
    else
    {
        String_Destroy(istate->NextAVTransportURIMetaData);
        istate->NextAVTransportURIMetaData = String_Create("");
        istate->LastChangeMask |= EVENT_NEXTAVTRANSPORTURIMETADATA;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_CurrentMediaDuration(DMR instance, long duration)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    DMR_Lock(instance);
    if(istate->CurrentMediaDuration != duration)
    {
        istate->CurrentMediaDuration = duration;
        istate->LastChangeMask |= EVENT_CURRENTMEDIADURATION;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_AbsoluteTimePosition(DMR instance, long position)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    if(istate->CurrentMediaDuration > 0 && position > istate->CurrentMediaDuration)
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }
    DMR_Lock(instance);
    if(istate->AbsoluteTimePosition != position)
    {
        istate->AbsoluteTimePosition = position;
        //istate->LastChangeMask |= EVENT_ABSOLUTETIMEPOSITION;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}

DMR_Error DMR_StateChange_RelativeTimePosition(DMR instance, long position)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
	
	/* sometimes maybe can not get totaltime  delete this CurrentTrackDuration  check
    if(position > istate->CurrentTrackDuration)
    {
        return DMR_ERROR_INVALIDARGUMENT;
    }*/

    DMR_Lock(instance);
    if(istate->RelativeTimePosition != position)
    {
        istate->RelativeTimePosition = position;
        //istate->LastChangeMask |= EVENT_RELATIVETIMEPOSITION;
    }
    DMR_Unlock(instance);

    return DMR_ERROR_OK;
}
/****************************************************************************/


/****************************************************************************/
/* Functions to handle the context switch of the callbacks.                 */
char* UPnPErrorMessageLookup(int code)
{
    switch(code)
    {
        case 401:
            return String_Create("Invalid Action");
        case 402:
            return String_Create("Invalid Args");
        case 404:
            return String_Create("Invalid Var");
        case 501:
            return String_Create("Action Failed");
        case 600:
            return String_Create("Argument Value Invalid");
        case 601:
            return String_Create("Argument Value Out of Range");
        case 602:
            return String_Create("Optional Action Not Implemented");
        case 603:
            return String_Create("Out Of Memory");
        case 604:
            return String_Create("Human Intervention Required");
        case 605:
            return String_Create("String Argument Too Long");
        case 606:
            return String_Create("Action Not Authorized");
        case 607:
            return String_Create("Signature Failure");
        case 608:
            return String_Create("Signature Missing");
        case 609:
            return String_Create("Not Encrypted");
        case 610:
            return String_Create("Invalid Sequence");
        case 611:
            return String_Create("Invalid Control URL");
        case 612:
            return String_Create("No Such Session");
        default:
            {
                char buffer[40];
                sprintf(buffer, "Unknown UPnP Error Code: %d", code);
                return String_Create(buffer);
            }
            break;
    }
}

char* UPnPErrorMessageLookupAVS(int code)
{
    switch(code)
    {
        case 701:
            return String_Create("Transition Not Available");
        case 702:
            return String_Create("No Contents");
        case 703:
            return String_Create("Read Error");
        case 704:
            return String_Create("Format Not Supported For Playback");
        case 705:
            return String_Create("Transport Is Locked");
        case 706:
            return String_Create("Write Error");
        case 707:
            return String_Create("Media Is Protected Or Not Writable");
        case 708:
            return String_Create("Format Not Supported For Recording");
        case 709:
            return String_Create("Media Is Full");
        case 710:
            return String_Create("Seek Mode Not Supported");
        case 711:
            return String_Create("Illegal Seek Target");
        case 712:
            return String_Create("Play Mode Not Supported");
        case 713:
            return String_Create("Record Quality Not Supported");
        case 714:
            return String_Create("Illegal MIME-Type");
        case 715:
            return String_Create("Content 'BUSY'");
        case 716:
            return String_Create("Resource Not Found");
        case 717:
            return String_Create("Play Speed Not Supported");
        case 718:
            return String_Create("Invalid InstanceID");
        case 722:
            return String_Create("Can't Determine Allowed Uses");
        default:
            return UPnPErrorMessageLookup(code);
    }
}

char* UPnPErrorMessageLookupRCS(int code)
{
    switch(code)
    {
        case 701:
            return String_Create("Invalid Name");
        case 702:
            return String_Create("Invalid InstanceID");
        default:
            return UPnPErrorMessageLookup(code);
    }
}

void CallbackFromThreadPool(ILibThreadPool threadPool, void* oMethod)
{
    DMR instance = NULL;
    ContextMethodCall method = (ContextMethodCall)oMethod;
    if(oMethod == NULL)
    {
        return;
    }
    instance = method->dmr;
    switch(method->method)
    {
		case DMR_ECS_GETAVPROTOCOLINFO:
			{
                if(instance->Event_GetAVProtocolInfo != NULL && method->parameterCount == 0)
                {
					char* protocolInfo = NULL;
                    int result = instance->Event_GetAVProtocolInfo(instance, method->session, &protocolInfo);
                    if(result == 0)
                    {
						if(protocolInfo == NULL)
						{
							DMR_Response_ConnectionManager_GetCurrentConnectionInfo(method->session, 0, 0, "", "", -1, "Input", "OK");
						}
						else
						{
							DMR_Response_ConnectionManager_GetCurrentConnectionInfo(method->session, 0, 0, protocolInfo, "", -1, "Input", "OK");
							free(protocolInfo);
						}
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
				}
                else if(instance->Event_GetAVProtocolInfo == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
			}
			break;
        case DMR_ECS_SETAVTRANSPORTURI:
            {
                if(instance->Event_SetAVTransportURI != NULL && method->parameterCount == 2)
                {
                    int result;
                    char* uri = (char*)method->parameters[0];
                    struct CdsObject* metadata = (struct CdsObject*)method->parameters[1];
                    result = instance->Event_SetAVTransportURI(instance, method->session, uri, metadata);
					String_Destroy(uri);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_SetAVTransportURI((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SetAVTransportURI == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
		 case DMR_ECS_SETNEXTAVTRANSPORTURI:
            {
                if(instance->Event_SetNextAVTransportURI != NULL && method->parameterCount == 2)
                {
                    int result;
                    char* uri = (char*)method->parameters[0];
                    struct CdsObject* metadata = (struct CdsObject*)method->parameters[1];
                    result = instance->Event_SetNextAVTransportURI(instance, method->session, uri, metadata);
					String_Destroy(uri);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_SetNextAVTransportURI((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SetNextAVTransportURI == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_STOP:
            {
                if(instance->Event_Stop != NULL && method->parameterCount == 0)
                {
                    int result = instance->Event_Stop(instance, method->session);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Stop((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_Stop == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_PLAY:
            {
                if(instance->Event_Play != NULL && method->parameterCount == 1)
                {
                    int result;
                    char* playSpeed = (char*)method->parameters[0];
                    result = instance->Event_Play(instance, method->session, playSpeed);
					String_Destroy(playSpeed);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Play((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_Play == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_PAUSE:
            {
                if(instance->Event_Pause != NULL && method->parameterCount == 0)
                {
                    int result = instance->Event_Pause(instance, method->session);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Pause((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_Pause == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_SEEKTRACK:
            {
                if(instance->Event_SeekTrack != NULL && method->parameterCount == 1)
                {
                    int result;
                    unsigned int track = (unsigned int)(unsigned long)method->parameters[0];
                    result = instance->Event_SeekTrack(instance, method->session, track);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Seek((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SeekTrack == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_SEEKTRACKTIME:
            {
                if(instance->Event_SeekTrackPosition != NULL && method->parameterCount == 1)
                {
                    int result;
                    long position = (long)method->parameters[0];
                    result = instance->Event_SeekTrackPosition(instance, method->session, position);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Seek((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SeekTrackPosition == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_SEEKMEDIATIME:
            {
                if(instance->Event_SeekMediaPosition != NULL && method->parameterCount == 1)
                {
                    int result;
                    long position = (long)method->parameters[0];
                    result = instance->Event_SeekMediaPosition(instance, method->session, position);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Seek((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SeekMediaPosition == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_NEXT:
            {
                if(instance->Event_Next != NULL && method->parameterCount == 0)
                {
                    int result = instance->Event_Next(instance, method->session);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Next((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_Next == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_PREVIOUS:
            {
                if(instance->Event_Previous != NULL && method->parameterCount == 0)
                {
                    int result = instance->Event_Previous(instance, method->session);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_Previous((const DMR_SessionToken)method->session);
					}
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_Previous == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_SETPLAYMODE:
            {
                if(instance->Event_SetPlayMode != NULL && method->parameterCount == 1)
                {
                    int result;
                    DMR_MediaPlayMode mode = (DMR_MediaPlayMode)method->parameters[0];
                    result = instance->Event_SetPlayMode(instance, method->session, mode);
                    if(result == 0)
                    {
                        DMR_Response_AVTransport_SetPlayMode((const DMR_SessionToken)method->session);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SetPlayMode == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_SELECTPRESET:
            {
                if(instance->Event_SelectPreset != NULL && method->parameterCount == 1)
                {
                    int result;
                    char* preset = (char*)method->parameters[0];
                    result = instance->Event_SelectPreset(instance, method->session, preset);
					String_Destroy(preset);
                    if(result == 0)
                    {
                        DMR_Response_RenderingControl_SelectPreset((const DMR_SessionToken)method->session);
						DMR_LastChangeUpdateImmediate(instance);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupAVS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SelectPreset == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
#if defined(INCLUDE_FEATURE_DISPLAY)
        case DMR_ECS_SETBRIGHTNESS:
            {
                if(instance->Event_SetBrightness != NULL && method->parameterCount == 1)
                {
                    int result;
                    unsigned char val = (unsigned char)method->parameters[0];
                    result = instance->Event_SetBrightness(instance, method->session, val);
                    if(result == 0)
                    {
                        DMR_Response_RenderingControl_SetBrightness((const DMR_SessionToken)method->session);
                        /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1A vvv */
                        DMR_LastChangeUpdateImmediate(instance);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupRCS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SetBrightness == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_SETCONTRAST:
            {
                if(instance->Event_SetContrast != NULL && method->parameterCount == 1)
                {
                    int result;
                    unsigned char val = (unsigned char)method->parameters[0];
                    result = instance->Event_SetContrast(instance, method->session, val);
                    if(result == 0)
                    {
                        DMR_Response_RenderingControl_SetContrast((const DMR_SessionToken)method->session);
                        /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1A vvv */
                        DMR_LastChangeUpdateImmediate(instance);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupRCS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SetContrast == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
#endif /* INCLUDE_FEATURE_DISPLAY */
#if defined(INCLUDE_FEATURE_VOLUME)
        case DMR_ECS_SETVOLUME:
            {
                if(instance->Event_SetVolume != NULL && method->parameterCount == 1)
                {
                    int result;
                    unsigned char val = (unsigned char)method->parameters[0];
                    result = instance->Event_SetVolume(instance, method->session, val);
                    if(result == 0)
                    {
                        DMR_Response_RenderingControl_SetVolume((const DMR_SessionToken)method->session);
                        /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1A vvv */
                        DMR_LastChangeUpdateImmediate(instance);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupRCS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SetVolume == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
        case DMR_ECS_SETMUTE:
            {
                if(instance->Event_SetMute != NULL && method->parameterCount == 1)
                {
                    int result;
                    BOOL val = (BOOL)method->parameters[0];
                    result = instance->Event_SetMute(instance, method->session, val);
                    if(result == 0)
                    {
                        DMR_Response_RenderingControl_SetMute((const DMR_SessionToken)method->session);
                        /* vvv Added by yuyu for DLNA 1.5 CTT 7.3.93.1A vvv */
                        DMR_LastChangeUpdateImmediate(instance);
                    }
                    else if(result > 0)
                    {
                        char* msg = UPnPErrorMessageLookupRCS(result);
                        DMR_Response_Error((const DMR_SessionToken)method->session, (const int)result, (const char*)msg);
                        String_Destroy(msg);
                    }
                }
                else if(instance->Event_SetMute == NULL)
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Action Not Implemented");
                }
                else
                {
                    DMR_Response_Error((const DMR_SessionToken)method->session, 501, "Action Failed: Program Error");
                }
            }
            break;
#endif /* INCLUDE_FEATURE_VOLUME */
        default:
            //ILibWebServer_Release((struct ILibWebServer_Session*)method->session);
            FREE(oMethod);
            return;
    }
    //ILibWebServer_Release((struct ILibWebServer_Session*)method->session);
    FREE(oMethod);
}

#if defined(WIN32) && !defined(_WIN32_WCE)
#pragma warning(disable: 4047)
#endif

DMR_Error CallMethodThroughThreadPool(DMR instance, ContextMethodCall method)
{
    DMR_InternalState state = (DMR_InternalState)instance->internal_state;
    BOOL contextSwitch = FALSE;
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }
    contextSwitch = TESTBIT(state->EventsOnThreadBitMask, method->method);
    switch(method->method)
    {
		case DMR_ECS_GETAVPROTOCOLINFO:
			break;
		case DMR_ECS_SETAVTRANSPORTURI:
            if(instance->Event_SetAVTransportURI != NULL && method->parameterCount == 2)
            {
                method->parameters[0] = (void*)String_Create((const char*)method->parameters[0]);
            }
            break;
		case DMR_ECS_SETNEXTAVTRANSPORTURI:
            if(instance->Event_SetNextAVTransportURI != NULL && method->parameterCount == 2)
            {
                method->parameters[0] = (void*)String_Create((const char*)method->parameters[0]);
            }
            break;
        case DMR_ECS_STOP:
            break;
        case DMR_ECS_PLAY:
            if(instance->Event_Play != NULL && method->parameterCount == 1)
            {
                method->parameters[0] = (void*)String_Create((const char*)method->parameters[0]);
            }
            break;
        case DMR_ECS_PAUSE:
            break;
        case DMR_ECS_SEEKTRACK:
            break;
        case DMR_ECS_SEEKTRACKTIME:
            break;
        case DMR_ECS_SEEKMEDIATIME:
            break;
        case DMR_ECS_NEXT:
            break;
        case DMR_ECS_PREVIOUS:
            break;
        case DMR_ECS_SETPLAYMODE:
            break;
        case DMR_ECS_SELECTPRESET:
            if(instance->Event_SelectPreset != NULL && method->parameterCount == 1)
            {
                method->parameters[0] = (void*)String_Create((const char*)method->parameters[0]);
            }
            break;
#if defined(INCLUDE_FEATURE_DISPLAY)
        case DMR_ECS_SETBRIGHTNESS:
            break;
        case DMR_ECS_SETCONTRAST:
            break;
#endif /* INCLUDE_FEATURE_DISPLAY */
#if defined(INCLUDE_FEATURE_VOLUME)
        case DMR_ECS_SETVOLUME:
            break;
        case DMR_ECS_SETMUTE:
            break;
#endif /* INCLUDE_FEATURE_VOLUME */
        default:
            return DMR_ERROR_INVALIDARGUMENT;
    }

    //ILibWebServer_AddRef((struct ILibWebServer_Session*)method->session);
    if(contextSwitch == TRUE)
    {
        ILibThreadPool_QueueUserWorkItem(instance->ThreadPool, (void*)method, &CallbackFromThreadPool);
    }
    else
    {
        CallbackFromThreadPool(NULL, (void*)method);
    }

    return DMR_ERROR_OK;
}

/****************************************************************************/
// added by yuyu
DMR_PlayState DMR_StateGet_PlayState(DMR instance)
{
    DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
	return istate->TransportState;

#if 0
    DMR_Error err = CheckThis(instance);
    if(err != DMR_ERROR_OK)
    {
        return err;
    }

    return DMR_ERROR_OK;
#endif
}

char* DMR_StateGet_TransportPlaySpeed(DMR instance)
{
	DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
	return istate->TransportPlaySpeed;
}

char *DMR_StateGet_AVTransportURI(DMR instance)
{
	DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
	return istate->AVTransportURI;
}

char *DMR_StateGet_NextAVTransportURI(DMR instance)
{
	DMR_InternalState istate = (DMR_InternalState)instance->internal_state;
	return istate->NextAVTransportURI;
}


// added by yuyu
