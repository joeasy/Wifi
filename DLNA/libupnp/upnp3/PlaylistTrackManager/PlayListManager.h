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
 * $Revision: 1.9 $
 * $Author: pdamons $
 * $Date: 2006/11/17 22:11:39 $
 *
 */

#if !defined(__PLAYLISTMANAGER_H__)
#define __PLAYLISTMANAGER_H__

/**
    \file PlayListManager.h
    \ingroup DLNADMR
	\brief An optional DMR API for managing a playlist (DIDL_S or PlayContainer).

    This file provides a API to resolve a playsingle URI to a struct CdsObject*
	that contains a list of resources that the caller can pick from to get
	a real media URI.

    \addtogroup DLNADMR DLNA MediaRenderer Microstack (DMR)
    \brief This module defines the component to resolve PlaySingleUris.
    \{
*/


#include "DMRCommon.h"
#include "CdsObject.h"
#include "ILibWebClient.h"
#include "ILibThreadPool.h"
#include "BitArray.h"
#include "DMRCommon.h"

/** \brief Enum defining the type of playlist to create.
	\warning The PlayContainerUri functionality supported here only supports a
			 strict=1 with a maxDepth=0 type of playlist.  It will not recurse
			 into sub-containers!
*/
typedef enum 
{
	/** \brief Do \b not use to in the \ref PlayListManager_Create constructor. */
	PLMT_Unknown = 0,
	/** \brief Creates a DIDL_S compliant playlist manager. */
	PLMT_DIDL_S,
	/** \brief This type of playlist is currently \b not supported! */
	PLMT_DIDL_V,
	/** \brief Creates a PlayContainerUri playlist manager. */
	PLMT_PLAYCONTAINER
} PlayListManagerType;

/** \brief Defines a typedef for the PlayListManager instance. */
typedef struct PlayListManangerToken *PlayListManager;

/** \brief Function pointer for a derived playlist manager that will destroy
           the \ref PlayListManager instance.
	\warning Is is the responsibility of the derived play list manager
			 implemention to destroy the memory and sem_t used in the
			 PlayListManangerToken structure!  If you implement a new play
			 list manager then you must implement this in your destroy
			 code.
*/
typedef void (*_DestroyFunctionPointer)(PlayListManager mananger);


/** \brief Function pointer for a derived playlist manager that will seek
           to the specific track index.
	\warning The track index is one based!
*/
typedef BOOL (*_SeekFunctionPointer)(PlayListManager mananger, int trackNumber);

/** \brief Function pointer for a callback (event) to the creators code that
		   informs that the play list track count has changed.
*/
typedef void (*_TrackCountChangedPointer)(PlayListManager mananger);

/** \brief Function pointer for a callback (event) to the creators code that
		   asks the caller if the current track is a valid track.  This event
		   is fired before \ref _TrackIndexChangedPointer.  If the users code
		   returns FALSE then the play list manager tried a different track.
*/
typedef BOOL (*_ValidTrackIndexPointer)(PlayListManager mananger);

/** \brief Function pointer for a callback (event) to the creators code that
		   informs the users code that the a new track number has been choosen.
*/
typedef void (*_TrackIndexChangedPointer)(PlayListManager mananger);

/** \brief Base class structure implementation. */
typedef struct PlayListManangerToken
{
	/** \brief This is the sem_t as a void* that controls access to the private implementaion.*/
	void* LockObject;

	/** \brief Storage for the \ref ILibWebClient_RequestManager passed into
			   the constructor of the \ref PlayListManager. */
	ILibWebClient_RequestManager RequestManager;

	/** \brief Storage for the MSCPToken passed into the constructor of the
			   \ref PlayListManager. */
	void* MSCPToken;

	/** \brief Type of the playlist manager. */
	PlayListManagerType	Type;

	/** \brief This is the AVTransportURI of the playlist. */
	char* URI;

	/** \brief This is an optional contentLength of the AVTransportURI media. */
	long ContentLength;

	/** \brief This is the track duration of the current track in milliseconds. */
	long TrackDuration;

	/** \brief This is the current track count for the playlist. */
	int TrackCount;

	/** \brief This is the currently selected track number (1 based).*/
	int TrackNumber;

	/** \brief This is the current track URI (usually set by the application in the
			   \ref _ValidTrackIndexPointer callback). */
	char* TrackURI;

	/** \brief This is the current track URI metadata (this is usually used by the
			   application to select a track URI). */
	struct CdsObject* TrackMetaData;

	/** \brief This is the current playmode (from the renderer) that is used in the
			   \ref PlayListManager_Next and \ref PlayListManager_Prev track
			   calculations. */
	DMR_MediaPlayMode Mode;

	/** \brief Storage for the thead pool instance passed into the constructor
			   of the \ref PlayListManager. */
	ILibThreadPool ThreadPool;

	/** \brief This is currently used to mark tracks played or invalid as the
			   renderer runs.  When all bits are marked then all tracks are
			   played or cannot be played. */
	BitArray ShuffleArray;

	/** \brief Function defined by the derived play list manager that is
			   called to destroy both the derived and base play list manager
			   resources.
		\param manager The 'this' pointer for the \ref PlayListManager instance.
		\warning Is is the responsibility of the derived play list manager
			     implemention to destroy the memory and sem_t used in the
				 PlayListManangerToken structure!  If you implement a new play
				 list manager then you must implement this in your destroy
				 code.
	*/
	_DestroyFunctionPointer Destroy;

	/** \brief Function defined by the derived play list manager that is called
			   by \ref PlayListManager_Next, \ref PlayListManager_Prev, or the
			   application to change the track number.
		\param manager The 'this' pointer for the \ref PlayListManager instance.
		\param trackNumber The 1-based index of the track to seek to.
		\returns TRUE (1)if the seek succeeds or FALSE (0) if the seek fails.
	*/
	_SeekFunctionPointer Seek;

	/** \brief Event function pointer called to inform the subscriber that the
			   track count has changed.  Check the
			   \ref PlayListManangerToken::TrackCount.
		\param manager The 'this' pointer for the \ref PlayListManager instance.
	*/
	_TrackCountChangedPointer	OnTrackCountChanged;

	/** \brief Event function pointer called to query the application as to the
			   validity of the current track number.  The
			   track metadata value should be used to
			   decide if the current track is valid.
		\param manager The 'this' pointer for the \ref PlayListManager instance.
		\returns The applicatin should return TRUE to accept the track or FALSE to
				 reject the track.
	*/
	_ValidTrackIndexPointer		OnValidTrackIndex;

	/** \brief Event function pointer called to inform the subscriber that the
			   \ref TrackNumber has changed.
		\param manager The 'this' pointer for the \ref PlayListManager instance.
	*/
	_TrackIndexChangedPointer	OnTrackIndexChanged;

	/** \brief Value is defined by the application. */
	void* User;

	/** \brief Private internal state of the derived play list manager. */
	void* InternalState;
};
//} *PlayListManager;	//comment this out to workaround 3.4.4 compiler error

/** \brief Creates an instance of the playlist manager of a specific type.
	\param pool \b Required. The thread pool that the app is using (there should be at
		   least 3 threads created in the pool).
	\param requestManager \b Required. The request manager being used by the application.
	\param MSCPToken \b Required. The media server control point used in the application.
	\param mode \b Required. The current play mode of the renderer.
	\param type \b Required. The type of the play list manager to create (Only \ref PLMT_DIDL_S or
		   \ref PLMT_PLAYCONTAINER are currently allowed).
	\param uri \b Required. The AVTransportUri of the play list.
	\param contentLength \b Optional. contentLength Either -1 or the content length of the play list media item
		   (DIDL_S only; not used).
	\param tracks \b Optional. Either -1 or the number of tracks in the playlist (not used).
	\param trackCountCallback \b Required. Function pointer to the application function to call when
		   the track count has changed.
    \param trackIndexCallback \b Required. Function pointer to the application function to call when
		   the track index has changed.
	\param validTrackIndexCallback \b Required. Function pointer to the application function to call
		   when the track manager needs the applicatin to verify if the current track is valid.
    \param user Tag used by the user. Can be anything.
    \returns The new \ref PlayListManager instance or NULL if it fails.
*/
PlayListManager PlayListManager_Create(ILibThreadPool pool,
									   ILibWebClient_RequestManager requestManager,
									   void* MSCPToken,
									   DMR_MediaPlayMode mode,
									   PlayListManagerType type,
									   char* uri,
									   long contentLength,
									   int tracks,
									   _TrackCountChangedPointer trackCountCallback,
									   _TrackIndexChangedPointer trackIndexCallback,
									   _ValidTrackIndexPointer validTrackIndexCallback,
									   void* user);

/** \brief Selects the next track number based on the play mode.
	\param This This is the 'this' pointer for the manager instance.
	\param callTrackIndexCallback determines if the callback is called.  TRUE
	       calls the callback and FALSE doesn't.
	\returns TRUE is a track is selected or FALSE if a track cannot be selected.
*/
BOOL PlayListManager_Next(PlayListManager This, BOOL callTrackIndexCallback);

/** \brief Selects the previous (or next) track number based on the play mode.
	\param This This is the 'this' pointer for the manager instance.
	\param callTrackIndexCallback determines if the callback is called.  TRUE
	       calls the callback and FALSE doesn't.
	\returns TRUE is a track is selected or FALSE if a track cannot be selected.
*/
BOOL PlayListManager_Prev(PlayListManager This, BOOL callTrackIndexCallback);

/** \brief Marks a specific track (1-based) as played or invalid.
	\param This This is the 'this' pointer for the manager instance.
	\param track The 1-based track index that is to be marked.
*/
void PlayListManager_MarkTrack(PlayListManager This, int track);

/** \brief Checks to see if a track (1-based) is marked as played or invalid.
	\param This This is the 'this' pointer for the manager instance.
	\param track The 1-based track index that is to be checked.
	\returns TRUE if the track is marked or FALSE if it is not marked.
*/
BOOL PlayListManager_IsTrackMarked(PlayListManager This, int track);

/** \brief Clears ALL tracks to unmarked state.
	\param This This is the 'this' pointer for the manager instance.
*/
void PlayListManager_ClearTrackMarks(PlayListManager This);

/** \brief Checks to see if ALL tracks in the playlist are marked as played or invalid.
	\param This This is the 'this' pointer for the manager instance.
	\returns TRUE if ALL tracks are played (or invalid) or FALSE otherwise.
*/
BOOL PlayListManager_AreAllTracksMarked(PlayListManager This);

/* \} */
#endif
