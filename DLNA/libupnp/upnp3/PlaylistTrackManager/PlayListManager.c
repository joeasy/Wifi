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
 * $Revision: 1.7 $
 * $Author: pdamons $
 * $Date: 2006/02/07 21:21:03 $
 *
 */

#if defined(WIN32) && !defined(_WIN32_WCE)
	#define _CRTDBG_MAP_ALLOC
#endif

#include <memory.h>
#include <string.h>
#include "PlaylistManagerS.h"
//#include "PlaylistManagerV.h"
#include "PlaylistManagerPC.h"


PlayListManager PlayListManager_Create(ILibThreadPool pool, ILibWebClient_RequestManager requestManager, void* MSCPToken, DMR_MediaPlayMode mode, PlayListManagerType type, char* uri, long contentLength, int tracks, _TrackCountChangedPointer trackCountCallback, _TrackIndexChangedPointer trackIndexCallback, _ValidTrackIndexPointer validTrackIndexCallback, void* user)
{
	size_t allocLen = sizeof(struct PlayListManangerToken) + strlen(uri) + 1;
	PlayListManager token = NULL;

	if(ILibThreadPool_GetThreadCount(pool) < 2)
	{
		return token; // Must have at least 2 threads.
	}

	token = (PlayListManager)malloc(allocLen);
	memset(token, 0, allocLen);

	token->ThreadPool = pool;
	token->RequestManager = requestManager;
	token->MSCPToken = MSCPToken;
	token->User = user;
	token->OnTrackCountChanged = trackCountCallback;
	token->OnTrackIndexChanged = trackIndexCallback;
	token->OnValidTrackIndex = validTrackIndexCallback;
	token->ContentLength = contentLength;
	token->TrackCount = tracks;
	if(token->TrackCount < 0)
	{
		token->TrackCount = 0;
	}
	token->Type = type;
	token->TrackNumber = -1;
	token->URI = (char*)token + sizeof(struct PlayListManangerToken);
	strcpy(token->URI, uri);
	token->Mode = mode;
	sem_init(&token->LockObject, 0, 1);
	token->ShuffleArray = BitArray_Create(8192, 0);
	BitArray_ChangeSize(token->ShuffleArray, 0);

	srand((unsigned)time(NULL));

	if(token->Type == PLMT_DIDL_S)
	{
		if(PlayListManager_S_Create(token) == 0)
		{
			free(token);
			return NULL;
		}
		return token;
	}
	else if(token->Type == PLMT_DIDL_V)
	{
		return NULL; // TODO: Implement DIDL_V playlists.
	}
	else if(token->Type == PLMT_PLAYCONTAINER)
	{
		if(PlayListManager_PC_Create(token, uri) == 0)
		{
			free(token);
			return NULL;
		}
		return token;
	}
	else
	{
		return NULL;
	}
}

void PlayListManager_MarkTrack(PlayListManager This, int track)
{
	BitArray_SetBit(This->ShuffleArray, track - 1, 1);
}

BOOL PlayListManager_IsTrackMarked(PlayListManager This, int track)
{
	return (BitArray_GetBit(This->ShuffleArray, track - 1)==0)?FALSE:TRUE;
}

void PlayListManager_ClearTrackMarks(PlayListManager This)
{
	BitArray_Reset(This->ShuffleArray, 0);
}

BOOL PlayListManager_AreAllTracksMarked(PlayListManager This)
{
	if(BitArray_TestAllBitsEqualTo(This->ShuffleArray, 1) != 0)
	{
		return TRUE;
	}
	else
	{
		return FALSE;
	}
}

int _GetNextTrackNumber(BOOL forward, int current, int count, DMR_MediaPlayMode mode)
{
	int result = -1;

	if(mode == DMR_MPM_Normal || mode == DMR_MPM_Intro || mode == DMR_MPM_Repeat_All)
	{
		if(forward == TRUE)
		{
			result = current + 1;
			if(result == 0)
			{
				result = 1;
			}
			if(result > count)
			{
				result = 1;
			}
		}
		else
		{
			result = current - 1;
			if(result < 1)
			{
				result = count;
			}
		}
	}
	else if(mode == DMR_MPM_Random || mode == DMR_MPM_Shuffle)
	{
		result = (rand() % count) + 1;
	}
	else if(mode == DMR_MPM_Repeat_One)
	{
		result = current;
	}

	return result;
}

BOOL PlayListManager_Next(PlayListManager This, BOOL callTrackIndexCallback)
{
	BOOL done = FALSE;
	int count = 0;
	int currentCount = This->TrackCount;
	int currentTrack = This->TrackNumber;

	int nextTrack = _GetNextTrackNumber(TRUE, currentTrack, currentCount, This->Mode);
	count++;

	while(done == FALSE && count < currentCount)
	{
		if(This->Seek(This, nextTrack) == TRUE && ((This->OnValidTrackIndex != NULL && This->OnValidTrackIndex(This) == TRUE) || This->OnValidTrackIndex == NULL))
		{
			done = TRUE;
		}
		else
		{
			count++;
			nextTrack = _GetNextTrackNumber(TRUE, nextTrack, currentCount, This->Mode);
		}
	}
	if(done == TRUE)
	{
		if(callTrackIndexCallback == TRUE && This->OnTrackIndexChanged != NULL)
		{
			This->OnTrackIndexChanged(This);
		}
		return TRUE;
	}
	else
	{
		return FALSE;
	}
}

BOOL PlayListManager_Prev(PlayListManager This, BOOL callTrackIndexCallback)
{
	BOOL done = FALSE;
	int count = 0;
	int currentCount = This->TrackCount;
	int currentTrack = This->TrackNumber;

	int nextTrack = _GetNextTrackNumber(FALSE, currentTrack, currentCount, This->Mode);
	count++;

	while(done == FALSE && count < currentCount)
	{
		if(This->Seek(This, nextTrack) == TRUE && ((This->OnValidTrackIndex != NULL && This->OnValidTrackIndex(This) == TRUE) || This->OnValidTrackIndex == NULL))
		{
			done = TRUE;
		}
		else
		{
			count++;
			nextTrack = _GetNextTrackNumber(FALSE, nextTrack, currentCount, This->Mode);
		}
	}
	if(done == TRUE)
	{
		if(callTrackIndexCallback == TRUE && This->OnTrackIndexChanged != NULL)
		{
			This->OnTrackIndexChanged(This);
		}
		return TRUE;
	}
	else
	{
		return FALSE;
	}
}
