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
 * $Revision: 1.13 $
 * $Author: pdamons $
 * $Date: 2006/07/17 15:21:18 $
 *
 */

#if defined(WIN32) && !defined(_WIN32_WCE)
	#define _CRTDBG_MAP_ALLOC
#endif

#include "PlaylistManagerS.h"
#include "CircularBuffer.h"
#include "IndexBlocks.h"
#include "CdsDidlSerializer.h"

#define DIDL_S_BUFFER_SIZE	16384

#define __MIN(x,y) ((x<y)?x:y)

// PDA: Somehow this got broke would no longer build, commenting it out fixed the issue.
/*#if defined(WINSOCK2)
	#include <winsock2.h>
	#include <ws2tcpip.h>
#elif defined(WINSOCK1)
	#include <winsock.h>
	#include <wininet.h>
#endif*/

/* Internal State for DIDL_S */
typedef struct _PlayListManager_S
{
	PlayListManager	Parent;				/* Pointer to the parent PlayListManager. */
	int				Error;				/* 0 means no error; greater than zero is an HTTP Error; less than zero is a different error. */

	IndexBlocks		Blocks;				/* PlayList Block Information for fast random access. */

	sem_t			FirstBlockFinished;	/* Locked if processing first block is not done; Unlocked if processing first block is finished. */
	sem_t			BlocksFinished;		/* Locked if processing blocks is not done; Unlocked if processing blocks is finished. */

	/* Buffer State Vars */
	CircularBuffer	_buffer;			/* Circular buffer for processing a network stream. */
	int				_streamOffset;		/* Offset in the network stream for the starting index of _buffer. */
} *PlayListManager_S;


/* Forward References */
void _PLMTS_Destroy(PlayListManager manager);
BOOL _PLMTS_Seek(PlayListManager manager, int trackNumber);
void _StartPlayListProcessing(PlayListManager_S state);
void _RequestResponseCallback(ILibWebClient_StateObject WebStateObject, int InterruptFlag, struct packetheader *header, char *bodyBuffer, int *beginPointer, int endPointer, int done, void *user1, void *user2, int *PAUSE);
void _ThreadCallback(ILibThreadPool pool, void* var);
void _StartPlayListProcessingFromThread(PlayListManager_S state);
void _ProcessBuffer(PlayListManager_S state, int done);
char* _GetTrackMetadata(PlayListManager_S state, int trackNumber, int* offset, int* length);
void _GetMetadataResponseCallback(ILibWebClient_StateObject WebStateObject, int InterruptFlag, struct packetheader *header, char *bodyBuffer, int *beginPointer, int endPointer, int done, void *user1, void *user2, int *PAUSE);

/* Constructor */
int PlayListManager_S_Create(PlayListManager manager)
{
	int result = 0;
	PlayListManager_S state = NULL;

	state = (PlayListManager_S)malloc(sizeof(struct _PlayListManager_S));
	if(state == NULL)
	{
		return result;

	}
	memset(state, 0, sizeof(struct _PlayListManager_S));
	state->Parent = manager;

	sem_init(&state->FirstBlockFinished, 0, 1);
	sem_init(&state->BlocksFinished, 0, 1);

	state->_buffer = CircularBuffer_Create(DIDL_S_BUFFER_SIZE);

	state->Blocks = IndexBlocks_Create();

	_StartPlayListProcessing(state);

	manager->InternalState = (void*)state;
	manager->Destroy = &_PLMTS_Destroy;
	manager->Seek = &_PLMTS_Seek;

	result = 1;

	return result;
}


/* Methods */
void _PLMTS_Destroy(PlayListManager manager)
{
	PlayListManager_S state = (PlayListManager_S)manager->InternalState;
	if(state != NULL)
	{
		sem_wait(&state->FirstBlockFinished);
		sem_destroy(&state->FirstBlockFinished);

		sem_wait(&state->BlocksFinished);
		sem_destroy(&state->BlocksFinished);

		CircularBuffer_Destroy(state->_buffer);
		IndexBlocks_Destroy(state->Blocks);

		free(state);
	}
	if(manager->TrackURI != NULL)
	{
		free(manager->TrackURI);
	}
	if(manager->TrackMetaData != NULL)
	{
		CDS_ObjRef_Release(manager->TrackMetaData);
	}
	sem_destroy(&manager->LockObject);
	if(manager->ShuffleArray != NULL)
	{
		BitArray_Destroy(manager->ShuffleArray);
	}

	free(manager);
}

BOOL _PLMTS_Seek(PlayListManager manager, int trackNumber)
{
	char* format = "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns:dlna=\"urn:schemas-dlna-org:metadata-1-0/\">%s</DIDL-Lite>";
	char* fragment = NULL;
	char* xml = NULL;
	BOOL result = FALSE;
	char* metadata = NULL;
	int offset = 0;
	int length = 0;
	PlayListManager_S state = (PlayListManager_S)manager->InternalState;

	metadata = _GetTrackMetadata(state, trackNumber - 1, &offset, &length);

	if(metadata == NULL)
	{
		return FALSE;
	}
	fragment = ILibString_Copy(metadata + offset, length);
	xml = (char*)malloc(strlen(fragment) + strlen(format));
	sprintf(xml, format, fragment);
	free(fragment);
	free(metadata);

	if(xml != NULL)
	{
		struct ILibXMLNode* root = ILibParseXML(xml, 0, (int)strlen(xml));
		if(root != NULL)
		{
			if(ILibProcessXMLNodeList(root) == 0)
			{
				ILibXML_BuildNamespaceLookupTable(root);
				if(root->Next != NULL)
				{
					struct ILibXMLNode* item = root->Next;
					struct ILibXMLAttribute* attrs = ILibGetXMLAttributes(item);
					if(attrs != NULL)
					{
						struct CdsObject* cdsItem = CDS_DeserializeDidlToObject(item, attrs, 1, metadata + offset, metadata + offset + length);

						if(cdsItem != NULL)
						{
							if(state->Parent->TrackMetaData != NULL)
							{
								CDS_ObjRef_Release(state->Parent->TrackMetaData);
								state->Parent->TrackMetaData = NULL;
							}
							if(state->Parent->TrackURI != NULL)
							{
								free(state->Parent->TrackURI);
								state->Parent->TrackURI = NULL;
							}
							state->Parent->TrackNumber = trackNumber;
							state->Parent->TrackMetaData = cdsItem;
							state->Parent->TrackNumber = trackNumber;

							result = TRUE;
						}
						ILibDestructXMLAttributeList(attrs);
					}
				}
			}
			ILibDestructXMLNodeList(root);
		}
		free(xml);
	}

	return result;
}


/* Implementation */
void _StartPlayListProcessing(PlayListManager_S state)
{
	sem_wait(&state->FirstBlockFinished);
	sem_wait(&state->BlocksFinished);

	ILibThreadPool_QueueUserWorkItem(state->Parent->ThreadPool, (void*)state, (ILibThreadPool_Handler)&_ThreadCallback);

	sem_wait(&state->FirstBlockFinished);
	sem_post(&state->FirstBlockFinished);
}

void _ThreadCallback(ILibThreadPool pool, void* var)
{
	PlayListManager_S state = (PlayListManager_S)var;
	_StartPlayListProcessingFromThread(state);
}

void _StartPlayListProcessingFromThread(PlayListManager_S state)
{
	char* IP;
	char* Path;
	unsigned short Port;
	char *host;
	int hostLen;
	struct sockaddr_in dest;
	ILibWebClient_RequestToken token;
	char* uri = state->Parent->URI;
	struct packetheader* header = ILibCreateEmptyPacket();

	ILibParseUri(uri, &IP, &Port, &Path);
	ILibSetVersion(header, "1.1", 3);
	ILibSetDirective(header, "GET", 3, Path, (int)strlen(Path));
	host = (char*)malloc((int)strlen(IP) + 10);
	hostLen = sprintf(host, "%s:%u", IP, Port);
	ILibAddHeaderLine(header, "Host", 4, host, hostLen);
	ILibAddHeaderLine(header, "transferMode.dlna.org", 21, "Interactive", 11);

	memset(&dest, 0, sizeof(struct sockaddr_in));
	dest.sin_addr.s_addr = inet_addr(IP);
	dest.sin_port = htons(Port);

	token = ILibWebClient_PipelineRequest(state->Parent->RequestManager, &dest, header, &_RequestResponseCallback, state, NULL);

	if(IP != NULL)
	{
		free(IP);
	}
	if(Path != NULL)
	{
		free(Path);
	}
	free(host);
}

void _RequestResponseCallback(ILibWebClient_StateObject WebStateObject, int InterruptFlag, struct packetheader *header, char *bodyBuffer, int *beginPointer, int endPointer, int done, void *user1, void *user2, int *PAUSE)
{
	int length = 0;
	int need = 0;
	PlayListManager_S state = (PlayListManager_S)user1;

	// Is there an error?
	if(header->StatusCode != 200 && header->StatusCode != 206)
	{
		// Yes, unlock and either return failure (0) or signal to try again (-99) based on the error and the state object.
		state->Error = header->StatusCode;
		sem_post(&state->FirstBlockFinished);
	}

	// No Error so continue....
	// Add data to state->_buffer...
	length = endPointer - *beginPointer; /* length is NOT inclusive */
	if(length > 0)
	{
		int left = length;
		int localOffset = 0;
		while(left > 0)
		{
			int len = __MIN(CircularBuffer_GetFreeSpace(state->_buffer), left);
			CircularBuffer_AddBlock(state->_buffer, bodyBuffer, *beginPointer + localOffset, len);
			_ProcessBuffer(state, 0);
			left -= len;
			localOffset += len;
		}

		// Signal that the ILib buffer was consumed....
		*beginPointer = endPointer;
	}

	// Are we done?
	if(done != 0)
	{
		_ProcessBuffer(state, 1);
	}
}

void _ProcessBuffer(PlayListManager_S state, int done)
{
	int i;
	char* usableBuffer = NULL;
	int streamOffset = 0;
	int trackCount = 0;
	int bufferLength = CircularBuffer_GetLength(state->_buffer);
	int startOfFirstItem = CircularBuffer_FindPattern(state->_buffer, 0, "<item", 5);
	int lengthOfUsableBuffer = -1;

	if(startOfFirstItem >= 0)
	{
		lengthOfUsableBuffer = (CircularBuffer_FindLastPattern(state->_buffer, 0, "</item>", 7) + 7) - startOfFirstItem;
		trackCount++;
	}
	else
	{
		CircularBuffer_ConsumeBytes(state->_buffer, bufferLength - 4);
		state->_streamOffset += (bufferLength - 4);
		if(done == 1)
		{
			sem_post(&state->BlocksFinished);
		}
		return;
	}

	streamOffset = state->_streamOffset + startOfFirstItem;

	usableBuffer = (char*)malloc(lengthOfUsableBuffer);
	CircularBuffer_CopyFrom(state->_buffer, usableBuffer, 0, startOfFirstItem, lengthOfUsableBuffer - startOfFirstItem);
	CircularBuffer_ConsumeBytes(state->_buffer, lengthOfUsableBuffer + startOfFirstItem);
	state->_streamOffset += startOfFirstItem;
	
	for(i = 5; i < (lengthOfUsableBuffer - 5); i++)
	{
		if(memcmp(usableBuffer + i, "<item", (size_t)5) == 0)
		{
			trackCount++;
		}
	}

	IndexBlocks_AddBlock(state->Blocks, streamOffset, lengthOfUsableBuffer, trackCount);
	state->_streamOffset += lengthOfUsableBuffer;

	state->Parent->TrackCount = IndexBlocks_GetTrackCount(state->Blocks);

	BitArray_ChangeSize(state->Parent->ShuffleArray, state->Parent->TrackCount);

	free(usableBuffer);


	sem_post(&state->FirstBlockFinished);
	if(done == 1)
	{
		sem_post(&state->BlocksFinished);
	}

	if(state->Parent->OnTrackCountChanged != NULL)
	{
		state->Parent->OnTrackCountChanged(state->Parent);
	}
}

struct __TMData
{
	PlayListManager_S	State;
	sem_t				Sync;
	int					LocalTrackNumber;
	char*				Metadata;
	int					_offset;
};

char* _GetTrackMetadata(PlayListManager_S state, int trackNumber, int* offset, int* length)
{
	int trackBase;
	int rangeStart;
	int rangeLength;
	int rangeEnd;
	struct __TMData* data = (struct __TMData*)malloc(sizeof(struct __TMData));
	memset(data, 0, sizeof(struct __TMData));

	data->State = state;

	if(IndexBlocks_GetTrackRangeInfo(state->Blocks, trackNumber, &rangeStart, &rangeLength, &trackBase) == 1)
	{
		char* IP;
		char* Path;
		unsigned short Port;
		char *host;
		int hostLen;
		struct sockaddr_in dest;
		ILibWebClient_RequestToken token;
		char* uri = state->Parent->URI;
		struct packetheader* header = ILibCreateEmptyPacket();
		char rangeVal[64];
		char* metadata = NULL;

		data->LocalTrackNumber = trackNumber - trackBase;
		rangeEnd = rangeStart + rangeLength - 1;
		sprintf(rangeVal, "bytes=%d-%d", rangeStart, rangeEnd);

		ILibParseUri(uri, &IP, &Port, &Path);
		ILibSetVersion(header, "1.1", 3);
		ILibSetDirective(header, "GET", 3, Path, (int)strlen(Path));
		host = (char*)malloc((int)strlen(IP) + 10);
		hostLen = sprintf(host, "%s:%u", IP, Port);
		ILibAddHeaderLine(header, "Host", 4, host, hostLen);
		ILibAddHeaderLine(header, "Range", 5, rangeVal, (int)strlen(rangeVal));
		ILibAddHeaderLine(header, "transferMode.dlna.org", 21, "Interactive", 11);

		memset(&dest, 0, sizeof(struct sockaddr_in));
		dest.sin_addr.s_addr = inet_addr(IP);
		dest.sin_port = htons(Port);

		data->Metadata = (char*)malloc((size_t)rangeLength);
		metadata = data->Metadata;

		sem_init(&data->Sync, 0, 0);

		token = ILibWebClient_PipelineRequest(state->Parent->RequestManager, &dest, header, &_GetMetadataResponseCallback, data, NULL);

		sem_wait(&data->Sync);
		sem_destroy(&data->Sync);

		metadata = data->Metadata;

		free(host);

		free(data);

		FREE(IP);
		FREE(Path);

		if(metadata != NULL)
		{
			int i;
			int found = 0;
			int count = -1;
			int localTrack = trackNumber - trackBase;
			for(i = 0; i < (rangeLength - 5); i++)
			{
				if(memcmp(metadata + i, "<item", (size_t)5) == 0)
				{
					count++;
					found = i;
					if(count == localTrack)
					{
						break;
					}
				}
			}
			if(count < localTrack)
			{
				free(metadata);
				metadata = NULL;
			}
			else
			{
				int i;
				int newFound = 0;

				for(i = 0; i < (rangeLength - found - 6); i++)
				{
					if(memcmp(metadata + found + i, "</item>", 7) == 0)
					{
						newFound = i;
						break;
					}
				}
				if(newFound > 0)
				{
					*offset = found;
					*length = newFound - found + 7; //found + newFound + 7;
				}
				else
				{
					free(metadata);
					return NULL;
				}
			}
		}

		return metadata;
	}

	free(data);

	return NULL;
}

void _GetMetadataResponseCallback(ILibWebClient_StateObject WebStateObject, int InterruptFlag, struct packetheader *header, char *bodyBuffer, int *beginPointer, int endPointer, int done, void *user1, void *user2, int *PAUSE)
{
	struct __TMData* data = (struct __TMData*)user1;
	PlayListManager_S state = data->State;

	if(InterruptFlag != 0)
	{
		free(data->Metadata);
		data->Metadata = NULL;
		sem_post(&data->Sync);

		return;
	}
	if(header->StatusCode != 206 && header->StatusCode != 200)
	{
		free(data->Metadata);
		data->Metadata = NULL;
		sem_post(&data->Sync);

		return;
	}

	memcpy(data->Metadata + data->_offset, bodyBuffer + *beginPointer, (size_t)(endPointer - *beginPointer));
	data->_offset += (endPointer - *beginPointer);

	*beginPointer = endPointer;

	if(done != 0)
	{
		sem_post(&data->Sync);
	}
}
