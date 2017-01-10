/*
 * INTEL CONFIDENTIAL
 * Copyright (c) 2002 - 2006 Intel Corporation.  All rights reserved.
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
 * $File: $
 * $Revision: 1.11 $
 * $Author: jhuang $
 * $Date: 2006/11/29 00:13:20 $
 *
 */

#ifndef __DMR_H__
#define __DMR_H__
/**
    \mainpage DLNA MediaRenderer Microstack Documentation

    This is the developers documentation for the DLNA 1.5 ready DMR.
    <HR>
    INTEL CONFIDENTIAL<BR>
	Copyright©2002 - 2006 Intel Corporation.  All rights reserved.

	The source code contained or described herein and all documents
	related to the source code ("Material") are owned by Intel
	Corporation or its suppliers or licensors.  Title to the
	Material remains with Intel Corporation or its suppliers and
	licensors.  The Material contains trade secrets and proprietary
	and confidential information of Intel or its suppliers and
	licensors. The Material is protected by worldwide copyright and
	trade secret laws and treaty provisions.  No part of the Material
	may be used, copied, reproduced, modified, published, uploaded,
	posted, transmitted, distributed, or disclosed in any way without
	Intel's prior express written permission.

	No license under any patent, copyright, trade secret or other
	intellectual property right is granted to or conferred upon you
	by disclosure or delivery of the Materials, either expressly, by
	implication, inducement, estoppel or otherwise. Any license
	under such intellectual property rights must be express and
	approved by Intel in writing.
*/

/**
    \file DMR.h
    \ingroup DLNADMR
	\brief DLNA MediaRenderer API

    This file provides a basic implementation of a UPnP AV Media
    Renderer's (DMR) state with a public interface.

    \addtogroup DLNADMR DLNA MediaRenderer Microstack (DMR)
    \brief This module defines the public interface for the MediaRenderer internal state implementation.
    \{
*/

#if defined(WINSOCK2)
#include <winsock2.h>
#include <ws2tcpip.h>
#elif defined(WINSOCK1)
#include <winsock.h>
#include <wininet.h>
#endif

#if !defined(WIN32)
#include <unistd.h>
#include <pthread.h>
#endif /* WIN32 */


#include "ILibParsers.h"
#include "ILibWebServer.h"
#include "ILibAsyncSocket.h"
#include "ILibThreadPool.h"
#include "CdsDidlSerializer.h"
#include "DMR_MicroStack.h"

#include "DMRCommon.h"
#include "DMRConfiguration.h"

#ifdef __cplusplus
extern "C" {
#endif

/* Forward references. */
struct _DMR;
typedef struct _DMR *DMR;


/** \brief This is a function pointer definition for the event \ref _DMR::Event_SetAVTransportURI. */
typedef int (*DMRCallback_SetAVTransportURI)(DMR instance, void* session, char* uri, struct CdsObject* data);
/** \brief This is a function pointer definition for the event \ref _DMR::DMRCallback_GetAVProtocolInfo. */
typedef int (*DMRCallback_GetAVProtocolInfo)(DMR instance, void* session, char** protocolInfo);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_SetPlayMode. */
typedef int (*DMRCallback_SetPlayMode)(DMR instance, void* session, DMR_MediaPlayMode playMode);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_Stop. */
typedef int (*DMRCallback_Stop)(DMR instance, void* session);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_Play. */
typedef int (*DMRCallback_Play)(DMR instance, void* session, char* playSpeed);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_Pause. */
typedef int (*DMRCallback_Pause)(DMR instance, void* session);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_SeekTrack. */
typedef int (*DMRCallback_SeekTrack)(DMR instance, void* session, unsigned int trackIndex);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_SeekTrackPosition. */
typedef int (*DMRCallback_SeekTrackPosition)(DMR instance, void* session, long position);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_SeekMediaPosition. */
typedef int (*DMRCallback_SeekMediaPosition)(DMR instance, void* session, long position);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_Next. */
typedef int (*DMRCallback_Next)(DMR instance, void* session);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_Previous. */
typedef int (*DMRCallback_Previous)(DMR instance, void* session);
/** \brief This is a function pointer definition for the event \ref _DMR::Event_SelectPreset. */
typedef int (*DMRCallback_SelectPreset)(DMR instance, void* session, char* presetName);

#if defined(INCLUDE_FEATURE_VOLUME)

    /** \brief This is a function pointer definition for the event \ref _DMR::Event_SetVolume. */
	typedef int (*DMRCallback_SetVolume)(DMR instance, void* session, unsigned char volume);
    /** \brief This is a function pointer definition for the event \ref _DMR::Event_SetMute. */
	typedef int (*DMRCallback_SetMute)(DMR instance, void* session, BOOL mute);

#endif /* INCLUDE_FEATURE_VOLUME */

#if defined(INCLUDE_FEATURE_DISPLAY)

    /** \brief This is a function pointer definition for the event \ref _DMR::Event_SetContrast. */
	typedef int (*DMRCallback_SetContrast)(DMR instance, void* session, unsigned char contrast);
    /** \brief This is a function pointer definition for the event \ref _DMR::Event_SetBrightness. */
	typedef int (*DMRCallback_SetBrightness)(DMR instance, void* session, unsigned char brightness);

#endif /* INCLUDE_FEATURE_DISPLAY */

/** \brief Definition of the _DMR struct.

    Definition of the struct containing the instance state information for
    the MediaRenderer.  You must use the \ref DMR_Method_Create to create a
    reference to the _DMR structure called \ref DMR.
*/
struct _DMR
{
    /** \brief This is part of the internal working of the chain.  DO NOT ALTER! */
	void (*ILib1)(void* object,fd_set *readset, fd_set *writeset, fd_set *errorset, int* blocktime);

    /** \brief This is part of the internal working of the chain.  DO NOT ALTER! */
	void (*ILib2)(void* object,int slct, fd_set *readset, fd_set *writeset, fd_set *errorset);

	/** \brief This is part of the internal working of the chain.  DO NOT ALTER! */
	void (*ILib3)(void* object);

    /** \brief This is the internal state of the DMR instance.

        The contents of the variable are hidden from the vendor on purpose.
    */
    void* internal_state;

    /** \brief This is used solely by the application and can be used to attach
        the rendering code to the DMR instance.

        This value is never used or reset by the DMR code.
    */
    void* Tag;

    /** \brief This is used by both the vendor application and the DMR microstack.

        This value should never be NULL but can be a thread pool of 0 threads.
    */
    ILibThreadPool ThreadPool;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        SetAVTransportURI.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param uri The URI of the media item or playlist to be rendered by
        the vendor's application.
        \returns An instance of the enum \ref DMR_Error.  A value of
        \c DMR_ERROR_OK means that the function succeeded; any other
        \ref DMR_Error value is an error.

        The actual function is defined by the vendor and this function pointer
        variable is set by the vendor and called by this module.
    */
    DMRCallback_SetAVTransportURI Event_SetAVTransportURI;
	DMRCallback_SetAVTransportURI Event_SetNextAVTransportURI;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        GetConnectionInfo.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param Pointer to a char* that is the returned string.  This pointer
		will be deleted using free.
        \returns An instance of the enum \ref DMR_Error.  A value of
        \c DMR_ERROR_OK means that the function succeeded; any other
        \ref DMR_Error value is an error.

        The actual function is defined by the vendor and this function pointer
        variable is set by the vendor and called by this module.
    */
	DMRCallback_GetAVProtocolInfo Event_GetAVProtocolInfo;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        SetPlayMode.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param playMode One of \ref DMR_MediaPlayMode.  This defines how the
        application should play the media set (repeat all, shuffle, etc...).
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called
        \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_SetPlayMode Event_SetPlayMode;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Stop.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_Stop Event_Stop;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Play.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param playSpeed This is a string containing the playSpeed that the renderer
        is intended to play the media.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_Play Event_Play;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Pause.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_Pause Event_Pause;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Seek with a seekMode of TRACK_NR.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param trackIndex The track number to seek to.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_SeekTrack Event_SeekTrack;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Seek with a seekMode of REL_TIME.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param position The track time index to seek to.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_SeekTrackPosition Event_SeekTrackPosition;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Seek with a seekMode of ABS_TIME.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param position The media time index to seek to.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_SeekMediaPosition Event_SeekMediaPosition;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Next.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one
		of these 2 methods
        \b MUST be called in the event response.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called
        \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_Next Event_Next;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        Previous.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_Previous Event_Previous;

    /** \brief \b EVENT: This is in response to a UPnP action call for action
        SelectPreset.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param presetName This is the preset name to select in the application.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function
        pointer variable is set by the vendor and called by this module.
    */
    DMRCallback_SelectPreset Event_SelectPreset;

#if defined(INCLUDE_FEATURE_VOLUME)

    /** \brief \b EVENT: This is to set the volume in the vendor's application.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param volume The volume level that the application should attempt to
        set. This value cannot exceed \ref VOLUME_MAX".
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function pointer
        variable is set by the vendor and called by this module.

        This function is an optional feature.  Define \c INCLUDE_FEATURE_VOLUME in your makefile/project to enable this feature.
    */
    DMRCallback_SetVolume Event_SetVolume;

    /** \brief \b EVENT: This is to set the mute state in the vendor's application.
        \param instance The \ref DMR instance to apply this event function pointer
        to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param mute The mute state that the application should attempt to set.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function pointer
        variable is set by the vendor and called by this module.

        This function is an optional feature.  Define \c INCLUDE_FEATURE_VOLUME in your makefile/project to enable this feature.
    */
    DMRCallback_SetMute Event_SetMute;

#endif /* INCLUDE_FEATURE_VOLUME */

#if defined(INCLUDE_FEATURE_DISPLAY)

    /** \brief \b EVENT: This is to set the display contrast in the vendor's
        application.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param contrast The contrast level that the application should attempt
        to set. This value cannot exceed \ref CONTRAST_MAX.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function pointer
        variable is set by the vendor and called by this module.

        This function is an optional feature.  Define
        \c INCLUDE_FEATURE_DISPLAY in your makefile/project to enable this
        feature.
    */
    DMRCallback_SetContrast Event_SetContrast;

    /** \brief \b EVENT: This is to set the display brightness in the vendor's
        application.
        \param instance The \ref DMR instance to apply this event function
        pointer to.  This is simular to the 'this' pointer in C++.
        \param session This is the token representing the UPnP communication instance.
        It is used in the \ref DMR_Method_ErrorEventResponse method call.  NOTE: one of these 2 methods
        \b MUST be called in the event response.
        \param brightness The brightness level that the application should
        attempt to set. This value cannot exceed \ref BRIGHTNESS_MAX.
        \returns Zero (0) for normal completetion and UPnP response or negative one (-1)
        if the Event handler code called \ref DMR_Method_ErrorEventResponse on its own  Otherwise the return value is a UPnP
        error code and the microstack will respond for you with the appropriate error.

        The actual function is defined by the vendor and this function pointer
        variable is set by the vendor and called by this module.

        This function is an optional feature.  Define
        \c INCLUDE_FEATURE_DISPLAY in your makefile/project to enable this
        feature.
    */
    DMRCallback_SetBrightness Event_SetBrightness;

#endif /* INCLUDE_FEATURE_DISPLAY */
};
/** \brief This is the pointer definition for \ref _DMR structure.

    This is used in all events/methods parameters and return values.
*/
//typedef struct _DMR *DMR;


/** \brief \b CONSTRUCTOR: This is the method that creates the \ref DMR object
    used by the vendor's application.
	\param chain This is the Microstack execution chain created in the
	application and passed into the DMR constructor.
    \param port This is the port to use by the UPnP AV MediaRenderer.
    \param friendlyName This is the friendly name of the UPnP AV MediaRenderer
    device that is created.
    \param serialNumber This is the serial number of the UPnP AV MediaRenderer
    device that is created.
    \param UDN This is the UDN to be used by the UPnP AV MediaRenderer.
    \param protocolInfo This is the complete list of protocol info strings
    that designate the supported media types supported by the vendor defined
    renderers.
    \param threadPool This is the pool of threads that the application must supply
    to be used by both the application and the DMR microstack.
    \returns The created DMR object instance or \c NULL if the creation fails.

    This function is considered the constructor of the \ref DMR object.
*/
DMR DMR_Method_Create(void* chain, unsigned short port, char* friendlyName, char* serialNumber, char* UDN, char* protocolInfo, ILibThreadPool threadPool);

/* * \brief \b DESTRUCTOR: This is the method that destroys the \ref DMR object
    used by the vendor's application.
    \param instance The \ref DMR instance to destroy.  This is simular to the
    'this' pointer in C++.
	\returns Returns \ref TRUE if the DMR is destroyed or \ref FALSE if the DMR
	is running or already was destroyed.

    This function is considered the destructor of the \ref DMR object.

BOOL DMR_Method_Destroy(DMR instance);*/

/** \brief \b METHOD:  This method will return the running state of the microstack
	chain for the DMR.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
	\returns Returns \ref TRUE if the DMR microstack chain is running, \ref FALSE if
	the DMR microstack chain is not running.

    This method should be treated as a method on the \ref DMR object.
*/
BOOL DMR_Method_IsRunning(DMR instance);

/** \brief \b METHOD:  This method informs the microstack that the computers network
	interfaces have changed is some way.  This includes interfaces added, removed, or
	IP address changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.

    This method should be treated as a method on the \ref DMR object.
*/
void DMR_Method_NotifyMicrostackOfIPAddressChange(DMR instance);

/** \brief \b METHOD:  This method is optionally called to change the default behavior
    of the DMR_Event_* callbacks.  If bits are set corresponding to DMR_Event_* callbacks
    then the callback is executed through the thread pool and if there are more than zero
    threads will be executed on a seperate thread.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param bitFlags This value is the aggregate of DMR_ECS_* values that represent the
    DMR_Event_* callbacks that will be executed through the thread pool passed into the
    DMR_Method_Create method/constructor.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_Method_SetEventContextMask(DMR instance, DMR_EventContextSwitch bitFlags);

/** \brief \b METHOD: This is called in a Event callback to terminate the callback with a specific error.
    \param session This is the second parameter passed into the Event callback.
    \param errorCode The UPnP or UPnP AV error code to return to the calling control point.
    \param errorMessage The UPnP or UPnP AV error message to return to the calling control point.

    This method should be treated like as a method on the \ref DMR object.
*/
void DMR_Method_ErrorEventResponse(void* session, int errorCode, char* errorMessage);

/** \brief \b METHOD: This allows adding of preset names to the preset list in the RenderingControl.
    Only up to (DMR__StateVariable_AllowedValues_MAX - 1) can be added and they cannot be removed
    dynamically.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param name The preset name to add to the list.
    \returns TRUE is it succedded and FALSE if it fails.

    This method should be treated like as a method on the \ref DMR object.
*/
BOOL DMR_Method_AddPresetNameToList(DMR instance, const char* name);

/** \brief \b STATE \b CHANGE: This method allows the dynamic changing of the protocol
    info list advertised by the device.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param info The new comma seperated list of protocol info strings to set
    into the MediaRenderer.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_SinkProtocolInfo(DMR instance, char* info);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the TransportPlayState has changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param state The new play state that the media render should set.  If you
    pass the same state as before then this value will be ignored.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_TransportPlayState(DMR instance, DMR_PlayState state);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the TransportPlaySpeed has changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param playSpeed This is the current play speed of the transport.  See the
    UPnP AVTransport service description for details.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_TransportPlaySpeed(DMR instance, char* playSpeed);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the TransportStatus has changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param status The new transport status that the MediaRenderer should set.
    If you pass the same status as before then this value will be ignored.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_TransportStatus(DMR instance, DMR_TransportStatus status);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the CurrentTransportActions has changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param allowedActions This is a set of flags (using the
    \ref DMR_AllowedTransportActions enum values) that determines what play
    actions are allowed in the current state.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_CurrentTransportActions(DMR instance, unsigned short allowedActions);
/*
    This method will return the Allowed action for DMR
*/
unsigned short DMR_Internal_GetCurrentTransportActions(DMR instance);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the maximum number of tracks has changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param maxNumberOfTracks The current max number of tracks that the
    application has determined.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_NumberOfTracks(DMR instance, unsigned int maxNumberOfTracks);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the new track index has changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param index The current track that the application is rendering (or stopped on).
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_CurrentTrack(DMR instance, unsigned int index);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the media play mode has changed.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param mode The current media play mode that the application is in.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_CurrentPlayMode(DMR instance, DMR_MediaPlayMode mode);

/** \brief \b STATE \b CHANGE: This method is called by the application to set
    the track URI into the DMR.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param trackURI The URI of the current track in the renderer.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_CurrentTrackURI(DMR instance, char* trackURI);

/** \brief \b STATE \b CHANGE: This method is called by the application to set
    the track metadata into the DMR.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param trackMetadata The URI metadata of the current track in the renderer.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_CurrentTrackMetaData(DMR instance, struct CdsObject* trackMetadata);

/** \brief \b STATE \b CHANGE: This method is called by the application to set all of
    the track information into the DMR.
    \param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
    \param duration Duration of the track in milliseconds or 0.
    \returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_CurrentTrackDuration(DMR instance, long duration);

#if defined(INCLUDE_FEATURE_VOLUME)

	/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
        MediaRenderer that the volume has changed.
	    \param instance The \ref DMR instance to apply this method to.  This is
        simular to the 'this' pointer in C++.
	    \param volume The current application volume.
	    \returns An instance of the enum \ref DMR_Error.  A value of
        \c DMR_ERROR_OK means that the function succeeded; any other
        \ref DMR_Error value is an error.

        This function is \b optional.  To include it define the preprocessor
        \c INCLUDE_FEATURE_VOLUME in your makefile/project.

        The volume cannot be greater than \ref VOLUME_MAX.

        This method should be treated like as a method on the \ref DMR object.
    */
	DMR_Error DMR_StateChange_Volume(DMR instance, unsigned char volume);

	/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
        MediaRenderer that the mute state has changed.
	    \param instance The \ref DMR instance to apply this method to.  This is
        simular to the 'this' pointer in C++.
	    \param mute The current application mute state.
	    \returns An instance of the enum \ref DMR_Error.  A value of
        \c DMR_ERROR_OK means that the function succeeded; any other
        \ref DMR_Error value is an error.

        This function is \b optional.  To include it define the preprocessor
        \c INCLUDE_FEATURE_VOLUME in your makefile/project.

        This method should be treated like as a method on the \ref DMR object.
    */
	DMR_Error DMR_StateChange_Mute(DMR instance, BOOL mute);

#endif /* INCLUDE_FEATURE_VOLUME */

#if defined(INCLUDE_FEATURE_DISPLAY)

	/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
        MediaRenderer that the contrast has changed.
	    \param instance The \ref DMR instance to apply this method to.  This is
        simular to the 'this' pointer in C++.
	    \param contrast The current application display contrast.
	    \returns An instance of the enum \ref DMR_Error.  A value of
        \c DMR_ERROR_OK means that the function succeeded; any other
        \ref DMR_Error value is an error.

        This function is \b optional.  To include it define the preprocessor
        \c INCLUDE_FEATURE_DISPLAY in your makefile/project.

        The contrast cannot be greater than \ref CONTRAST_MAX.

        This method should be treated like as a method on the \ref DMR object.
    */
	DMR_Error DMR_StateChange_Contrast(DMR instance, unsigned char contrast);

	/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
        MediaRenderer that the brightness has changed.
	    \param instance The \ref DMR instance to apply this method to.  This is
        simular to the 'this' pointer in C++.
	    \param brightness The current application display brightness.
	    \returns An instance of the enum \ref DMR_Error.  A value of
        \c DMR_ERROR_OK means that the function succeeded; any other
        \ref DMR_Error value is an error.

        This function is \b optional.  To include it define the preprocessor
        \c INCLUDE_FEATURE_DISPLAY in your makefile/project.

        The brightness cannot be greater than \ref BRIGHTNESS_MAX.

        This method should be treated like as a method on the \ref DMR object.
    */
	DMR_Error DMR_StateChange_Brightness(DMR instance, unsigned char brightness);

#endif /* INCLUDE_FEATURE_DISPLAY */

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the AVTransportURI has changed.
	\param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
	\param uri The new URI that the application is using.
	\returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_AVTransportURI(DMR instance, char* uri);
DMR_Error DMR_StateChange_NextAVTransportURI(DMR instance, char* uri);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the AVTransportURIMetaData has changed.
	\param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
	\param metadata The new metadata that the application is using for the AVTransportURI.
	\returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_AVTransportURIMetaData(DMR instance, struct CdsObject* metadata);
DMR_Error DMR_StateChange_NextAVTransportURIMetaData(DMR instance, struct CdsObject* metadata);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the AVTransport media durations has changed.
	\param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
	\param duration The new duration of the AVTransportURI that the application is using.
	\returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_CurrentMediaDuration(DMR instance, long duration);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the AVTransportURI media position has changed.
	\param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
	\param position The new media position in the AVTransportURI that the application is using.
	\returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_AbsoluteTimePosition(DMR instance, long position);

/** \brief \b STATE \b CHANGE: This method is called by the application to inform the
    MediaRenderer that the CurrentTrackURI media position has changed.
	\param instance The \ref DMR instance to apply this method to.  This is
    simular to the 'this' pointer in C++.
	\param position The new media position in the CurrentTrackURI that the application is using.
	\returns An instance of the enum \ref DMR_Error.  A value of
    \c DMR_ERROR_OK means that the function succeeded; any other
    \ref DMR_Error value is an error.

    This method will fire a LastChange event asyncronously.

    This method should be treated like as a method on the \ref DMR object.
*/
DMR_Error DMR_StateChange_RelativeTimePosition(DMR instance, long position);
/* \} */

// added by yuyu
/**
	return the Internal State::PlayState of a DMR Instance 
*/
DMR_PlayState DMR_StateGet_PlayState(DMR instance);
/**
	return the Internal State::TransportPlaySpeed of a DMR Instance
*/
char* DMR_StateGet_TransportPlaySpeed(DMR instance);
/**
	return the Internal State::AVTransportURI of a DMR Instance
*/
char *DMR_StateGet_AVTransportURI(DMR instance);
char *DMR_StateGet_NextAVTransportURI(DMR instance);

// added by yuyu
#ifdef __cplusplus
};
#endif


#endif /* __DMR_H__ */
