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
 * $Revision: 1.5 $
 * $Author: pdamons $
 * $Date: 2006/02/07 21:21:03 $
 *
 */

#if defined(WIN32)
	#define _CRTDBG_MAP_ALLOC
#endif

#include "DMRCommon.h"
#include "ILibParsers.h"
#include "IndexBlocks.h"


struct _IndexBlockNode;
struct _IndexBlockNode
{
	unsigned int			ByteOffset;			// 2GB limit
	unsigned short			ByteLength;			// Max is DIDL_S_BUFFER_SIZE
	unsigned short			FirstTrackNumber;	// Max Track Number is 65535
	unsigned char			TrackCount;			// Max Tracks per Block = 256
	struct _IndexBlockNode*	Next;				// Next Block or NULL.
}; /* Packed size == 104bits (13bytes); Unpacked size = 160 bits (20) */

struct _IndexBlocks
{
	sem_t					_sync;
	int 					_bIsSetSem;
	struct _IndexBlockNode*	_firstNode;
	int						_blockCount;
};


/* Forward references */
struct _IndexBlockNode* _GetLastNode(struct _IndexBlockNode* firstNode);
struct _IndexBlockNode* _FindTrackBlock(struct _IndexBlockNode* firstNode, int track);


/* Public methods */
IndexBlocks IndexBlocks_Create()
{
	struct _IndexBlocks* instance = (struct _IndexBlocks*)malloc(sizeof(struct _IndexBlocks));
	if(instance != NULL)
	{
		instance->_blockCount = 0;
		instance->_firstNode = NULL;
		sem_init(&instance->_sync, 0, 1);
	}
	return (IndexBlocks)instance;
}

void IndexBlocks_Destroy(IndexBlocks blocks)
{
	struct _IndexBlocks* instance = (struct _IndexBlocks*)blocks;
	struct _IndexBlockNode* node = NULL;
	
//yuyu	if(blocks == NULL || instance->_sync == NULL)
	if(blocks == NULL || instance->_bIsSetSem == 0)
	{
		return;
	}

	sem_wait(&instance->_sync);
	sem_destroy(&instance->_sync);
//yuyu	instance->_sync = NULL;
	instance->_bIsSetSem = 0;

	node = instance->_firstNode;

	while(node != NULL)
	{
		struct _IndexBlockNode* next = node->Next;
		free(node);
		node = next;
	}

	free(instance);
}

int IndexBlocks_AddBlock(IndexBlocks blocks, int streamOffset, int length, int trackCount)
{
	int trackOffset = 0;
	struct _IndexBlocks* instance = (struct _IndexBlocks*)blocks;
	struct _IndexBlockNode* node = NULL;
	struct _IndexBlockNode* lastNode = NULL;
	if(instance == NULL || blocks == NULL || length >= 65536 || trackCount >= 256)
	{
		return 0;
	}
//yuyu	if(instance->_sync == NULL)
	if(instance->_bIsSetSem == 0)
	{
		return 0;
	}
	sem_wait(&instance->_sync);

	lastNode = _GetLastNode(instance->_firstNode);
	if(lastNode != NULL)
	{
		trackOffset = (int)lastNode->FirstTrackNumber + (int)lastNode->TrackCount;
	}
	if((trackOffset + trackCount) >= 65536)
	{
		sem_post(&instance->_sync);
		return 0;
	}

	node = (struct _IndexBlockNode*)malloc(sizeof(struct _IndexBlockNode));
	if(node == NULL)
	{
		sem_post(&instance->_sync);
		return 0;
	}

	node->Next = NULL;
	node->ByteOffset = streamOffset;
	node->ByteLength = (unsigned short)length;
	node->FirstTrackNumber = (unsigned short)trackOffset;
	node->TrackCount = (unsigned char)trackCount;

	if(lastNode == NULL)
	{
		instance->_firstNode = node;
	}
	else
	{
		lastNode->Next = node;
	}

	instance->_blockCount++;

	sem_post(&instance->_sync);

	return 1;
}

int IndexBlocks_GetTrackCount(IndexBlocks blocks)
{
	int result = -1;
	struct _IndexBlocks* instance = (struct _IndexBlocks*)blocks;
	if(instance != NULL)
	{
//yuyu		if(instance->_sync == NULL)
		if(instance->_bIsSetSem == 0)
		{
			return result;
		}
		sem_wait(&instance->_sync);
		if(instance->_firstNode == NULL)
		{
			result = 0;
		}
		else
		{
			struct _IndexBlockNode* lastNode = _GetLastNode(instance->_firstNode);
			result = (int)lastNode->FirstTrackNumber + (int)lastNode->TrackCount;
		}
		sem_post(&instance->_sync);
	}
	return result;
}

int IndexBlocks_GetTrackRangeInfo(IndexBlocks blocks, int trackNumber, int* byteOffset, int* length, int* trackOffset)
{
	struct _IndexBlockNode* node = NULL;
	int trackCount = 0;
	struct _IndexBlocks* instance = (struct _IndexBlocks*)blocks;
	if(instance == NULL || trackNumber < 0 || byteOffset == NULL || length == NULL || trackOffset == NULL)
	{
		return 0;
	}
	if(instance->_blockCount == 0)
	{
		return 0;
	}
	trackCount = IndexBlocks_GetTrackCount(blocks);
	if(trackNumber >= trackCount)
	{
		return 0;
	}
//yuyu	if(instance->_sync == NULL)
	if(instance->_bIsSetSem == 0)
	{
		return 0;
	}
	sem_wait(&instance->_sync);

	node = _FindTrackBlock(instance->_firstNode, trackNumber);

	sem_post(&instance->_sync);

	if(node == NULL)
	{
		return 0;
	}

	*byteOffset = node->ByteOffset;
	*length = (int)node->ByteLength;
	*trackOffset = (int)node->FirstTrackNumber;

	return 1;
}


/* Implementation */
struct _IndexBlockNode* _GetLastNode(struct _IndexBlockNode* firstNode)
{
	struct _IndexBlockNode* lastNode = firstNode;
	while(firstNode != NULL)
	{
		lastNode = firstNode;
		firstNode = firstNode->Next;
	}
	return lastNode;
}

struct _IndexBlockNode* _FindTrackBlock(struct _IndexBlockNode* firstNode, int track)
{
	struct _IndexBlockNode* foundNode = NULL;
	if(track < 0)
	{
		return foundNode;
	}
	while(firstNode != NULL)
	{
		int lastTrack = (int)firstNode->FirstTrackNumber + (int)firstNode->TrackCount - 1;
		if(track >= (int)firstNode->FirstTrackNumber && track <= lastTrack)
		{
			foundNode = firstNode;
			break;
		}
		firstNode = firstNode->Next;
	}
	return foundNode;
}
