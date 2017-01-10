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
 * $Date: 2007/02/08 22:22:54 $
 *
 */

#if defined(WIN32) && !defined(_WIN32_WCE)
	#define _CRTDBG_MAP_ALLOC
#endif

#include "PlaylistManagerPC.h"
#include "CdsDidlSerializer.h"
#include "MediaServerCP_ControlPoint.h"


/* Only keep this number of objects in memory at one time. */
#define MAX_OBJECTS_IN_MEMORY	15


/* Internal State for DIDL_S */
typedef struct _PlayListManager_PC
{
	PlayListManager		Parent;			/* Pointer to the parent PlayListManager. */
	int					Error;			/* 0 means no error; greater than zero is an HTTP Error; less than zero is a different error. */

	char*				UDN;
	char*				ServiceID;
	char*				ContainerID;
	char*				FirstItemID;
	int					FirstItemIndex;
	char*				SortArgs;
	int					MaxDepth;

	sem_t				LockObject;
	struct CdsObject*	Objects[MAX_OBJECTS_IN_MEMORY];
	int					Count;
	int					StartingTrackNumber;
} *PlayListManager_PC;


/* Forward References */
void _PLMTPC_Destroy(PlayListManager manager);
BOOL _PLMTPC_Seek(PlayListManager manager, int trackNumber);
char* PC_UriUnescape(char* uri);
void ClearMetadata(PlayListManager_PC instance);
BOOL ParsePlayContainerUri(PlayListManager_PC state, char* uri);
BOOL Browse(PlayListManager_PC state, int trackIndex);
void _Callback2(struct UPnPService *sender, int ErrorCode, void *user,
			   char* Result, unsigned int NumberReturned, 
			   unsigned int TotalMatches, unsigned int UpdateID);

/* Constructor */
int PlayListManager_PC_Create(PlayListManager manager, char* uri)
{
	int i;
	int result = 0;
	PlayListManager_PC state = NULL;

	state = (PlayListManager_PC)malloc(sizeof(struct _PlayListManager_PC));
	if(state == NULL)
	{
		return result;

	}
	memset(state, 0, sizeof(struct _PlayListManager_PC));
	state->Parent = manager;
	state->UDN = NULL;
	state->ServiceID = NULL;
	state->ContainerID = NULL;
	state->FirstItemID = NULL;
	state->FirstItemIndex = -1;
	state->SortArgs = String_Create("");
	state->MaxDepth = 0;
	state->Count = 0;
	state->StartingTrackNumber = 0;
	for(i = 0; i < MAX_OBJECTS_IN_MEMORY; i++)
	{
		state->Objects[i] = NULL;
	}

	manager->InternalState = (void*)state;
	manager->Destroy = &_PLMTPC_Destroy;
	manager->Seek = &_PLMTPC_Seek;

	if(ParsePlayContainerUri(state, uri) == FALSE)
	{
		result = 0;
		return result;
	}

	sem_init(&state->LockObject, 0, 1);

	if(Browse(state, 1) == FALSE)
	{
		result = 0;
		return result;
	}

	result = 1;
	return result;
}


/* Methods */
void _PLMTPC_Destroy(PlayListManager manager)
{
	PlayListManager_PC state = (PlayListManager_PC)manager->InternalState;
	if(state != NULL)
	{
		sem_wait(&state->LockObject);
		sem_destroy(&state->LockObject);

		String_Destroy(state->UDN);
		String_Destroy(state->ServiceID);
		String_Destroy(state->ContainerID);
		String_Destroy(state->FirstItemID);
		String_Destroy(state->SortArgs);

		ClearMetadata(state);

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

BOOL _PLMTPC_Seek(PlayListManager manager, int trackNumber)
{
	PlayListManager_PC state = (PlayListManager_PC)manager->InternalState;
	BOOL result = FALSE;

	if(trackNumber >= state->StartingTrackNumber && trackNumber <= (state->StartingTrackNumber + state->Count - 1))
	{
		int index = trackNumber - state->StartingTrackNumber;
		if(manager->TrackMetaData != NULL)
		{
			CDS_ObjRef_Release(manager->TrackMetaData);
			manager->TrackMetaData = NULL;
		}
		String_Destroy(manager->TrackURI);
		manager->TrackURI = NULL;
		manager->TrackMetaData = state->Objects[index];
		CDS_ObjRef_Add(manager->TrackMetaData);
		manager->TrackNumber = trackNumber;
		manager->TrackURI = String_Create(manager->TrackMetaData->Res->Value);

		result = TRUE;
	}
	else
	{
		if(Browse(state, trackNumber) == TRUE)
		{
			if(manager->TrackMetaData != NULL)
			{
				CDS_ObjRef_Release(manager->TrackMetaData);
				manager->TrackMetaData = NULL;
			}
			String_Destroy(manager->TrackURI);
			manager->TrackURI = NULL;
			manager->TrackMetaData = state->Objects[0];
			CDS_ObjRef_Add(manager->TrackMetaData);
			manager->TrackNumber = trackNumber;
			manager->TrackURI = String_Create(manager->TrackMetaData->Res->Value);

			result = TRUE;
		}
		else
		{
			if(manager->TrackMetaData != NULL)
			{
				CDS_ObjRef_Release(manager->TrackMetaData);
				manager->TrackMetaData = NULL;
			}
			String_Destroy(manager->TrackURI);
			manager->TrackURI = NULL;

			result = FALSE;
		}
	}

	return result;
}


/* Implementation */
char* MyStrChr(const char* str, int chr)
{
	const char* pos = str;
	if(str == NULL)
	{
		return NULL;
	}
	while((int)(*pos) != 0 && (int)(*pos) != chr)
	{
		pos++;
	}
	if((int)(*pos) == chr)
	{
		return (char*)pos;
	}
	else
	{
		return NULL;
	}
}

BOOL ParsePlayContainerUri(PlayListManager_PC state, char* uri)
{
	struct parser_result_field* field;
	struct parser_result* parseResults;
	int length;
	char* pos = NULL;
	char* tmp = NULL;

	uri = PC_UriUnescape(uri);
	pos = uri + 21;

	if(ILibString_StartsWith(uri, (int)strlen(uri), "dlna-playcontainer://", 21) == 0)
	{
		free(uri);
		return FALSE;
	}
	if(ILibString_StartsWithEx(pos, (int)strlen(pos), "uuid:", 5, 0) != 0)
	{
		pos += 5;
	}
	{
		char out[4096];
		sprintf(out, "%s\n", pos);
		// fixme
		//OutputDebugString(out);
	}
	tmp = MyStrChr(pos, 0x003f);
	{
		char out[4096];
		sprintf(out, "%s\n", tmp);
		// fixme
		//OutputDebugString(out);
	}
	if(tmp == NULL)
	{
		free(uri);
		return FALSE;
	}
	length = (int)(tmp - pos);
	String_Destroy(state->UDN);
	state->UDN = String_CreateSize(length);
	strncpy(state->UDN, pos, (size_t)length);
	state->UDN[length] = 0;
	if(state->UDN[length - 1] == '/')
	{
		state->UDN[length - 1] = 0;
	}
	pos += length + 1;

	parseResults = ILibParseString(pos, 0, (int)strlen(pos), "&", 1);

	state->FirstItemIndex = -1;

	field = parseResults->FirstResult;
	while(field != NULL)
	{
		int length;
		char* val = field->data;
		if(ILibString_StartsWith(field->data, field->datalength, "sid=", 4) != 0)
		{
			length = field->datalength - 4;
			val += 4;
			String_Destroy(state->ServiceID);
			state->ServiceID = String_CreateSize(length);
			strncpy(state->ServiceID, val, (size_t)length);
			state->ServiceID[length] = 0;
		}
		else if(ILibString_StartsWith(field->data, field->datalength, "cid=", 4) != 0)
		{
			length = field->datalength - 4;
			val += 4;
			String_Destroy(state->ContainerID);
			state->ContainerID = String_CreateSize(length);
			strncpy(state->ContainerID, val, (size_t)length);
			state->ContainerID[length] = 0;
		}
		else if(ILibString_StartsWith(field->data, field->datalength, "fid=", 4) != 0)
		{
			length = field->datalength - 4;
			val += 4;
			String_Destroy(state->FirstItemID);
			state->FirstItemID = String_CreateSize(length);
			strncpy(state->FirstItemID, val, (size_t)length);
			state->FirstItemID[length] = 0;
		}
		else if(ILibString_StartsWith(field->data, field->datalength, "fii=", 4) != 0)
		{
			char* tmp = NULL;
			length = field->datalength - 4;
			val += 4;
			tmp = String_CreateSize(length);
			strncpy(tmp, val, length);
			tmp[length] = 0;
			state->FirstItemIndex = atoi(tmp);
			String_Destroy(tmp);
		}
		else if(ILibString_StartsWith(field->data, field->datalength, "sc=", 3) != 0)
		{
			length = field->datalength - 3;
			val += 3;
			String_Destroy(state->SortArgs);
			state->SortArgs = String_CreateSize(length);
			strncpy(state->SortArgs, val, (size_t)length);
			state->SortArgs[length] = 0;
		}
		else if(ILibString_StartsWith(field->data, field->datalength, "md=", 3) != 0)
		{
			char* tmp = NULL;
			length = field->datalength - 3;
			val += 3;
			tmp = String_CreateSize(length);
			strncpy(tmp, val, length);
			tmp[length] = 0;
			state->MaxDepth = atoi(tmp);
			String_Destroy(tmp);
		}
		else
		{
			String_Destroy(state->UDN);
			String_Destroy(state->ServiceID);
			String_Destroy(state->ContainerID);
			String_Destroy(state->FirstItemID);
			String_Destroy(state->SortArgs);
			ILibDestructParserResults(parseResults);

			return FALSE;
		}
		field = field->NextResult;
	}

	ILibDestructParserResults(parseResults);

	if(state->ServiceID != NULL && state->ContainerID != NULL && state->FirstItemID != NULL && state->FirstItemIndex != -1)
	{
		free(uri);
		return TRUE;
	}
	else
	{
		String_Destroy(state->UDN);
		String_Destroy(state->ServiceID);
		String_Destroy(state->ContainerID);
		String_Destroy(state->FirstItemID);
		String_Destroy(state->SortArgs);

		free(uri);
		return FALSE;
	}
}

char* GetServiceIDFromFullString(char* fullStr)
{
    char* result = fullStr;
    char* firstPos = strchr(fullStr, ':');
    if(firstPos != NULL)
    {
        char* secondPos = strchr(firstPos+1, ':');
        if(secondPos != NULL)
        {
            char* thirdPos = strchr(secondPos+1, ':');
            if(thirdPos != NULL)
            {
                if(strncmp(fullStr, "urn", 3) == 0 && strncmp(secondPos+1, "serviceId", 9) == 0)
                {
                    result = thirdPos + 1;
                }
            }
        }
    }
    return result;
}

BOOL Browse(PlayListManager_PC instance, int trackIndex)
{
	struct CdsObject* cdsObject = NULL;
	struct UPnPDevice* device = NULL;
	struct UPnPService* service = NULL;
	char* rawResult = NULL;
	int cdsIndex = instance->FirstItemIndex;
	long ticks = 0;

	if(instance->Parent->TrackCount > 0)
	{
		cdsIndex = ((cdsIndex + (trackIndex - 1)) % instance->Parent->TrackCount);
	}

	if(instance->Parent->MSCPToken == NULL)
	{
		return FALSE;
	}

	while(device == NULL && ticks < 10)
	{
		device = MSCP_GetDeviceAtUDN(instance->Parent->MSCPToken, instance->UDN);
#ifdef WIN32
		Sleep(1000);
		ticks++;
#endif
	}
	if(device == NULL)
	{
		return FALSE;
	}
	service = MSCP_GetService_ContentDirectory(device);
	if(service == NULL || service->ServiceId == NULL || ILibString_EndsWith(service->ServiceId, (int)strlen(service->ServiceId), instance->ServiceID, (int)strlen(instance->ServiceID)) == 0)
	{
		return FALSE;
	}

	sem_wait(&instance->LockObject);

	MSCP_Invoke_ContentDirectory_Browse(service, &_Callback2, instance, instance->ContainerID, "BrowseDirectChildren", "*", cdsIndex, MAX_OBJECTS_IN_MEMORY, instance->SortArgs);

	sem_wait(&instance->LockObject);
	sem_post(&instance->LockObject);

	if(instance->Error == 0)
	{
		instance->StartingTrackNumber = trackIndex;
		return TRUE;
	}

	return FALSE;
}

void _Callback2(struct UPnPService *sender, int ErrorCode, void *user,
			    char* Result, unsigned int NumberReturned, 
			    unsigned int TotalMatches, unsigned int UpdateID)
{
	PlayListManager_PC state = (PlayListManager_PC)user;
	struct ILibXMLNode* root = NULL;
	int count = (int)NumberReturned;

	state->Error = -ErrorCode;
	if(ErrorCode != 0)
	{
		sem_post(&state->LockObject);
		return;
	}

	ClearMetadata(state);

	if((int)TotalMatches != state->Parent->TrackCount)
	{
		state->Parent->TrackCount = (int)TotalMatches;
		BitArray_ChangeSize(state->Parent->ShuffleArray, state->Parent->TrackCount);

		if(state->Parent->OnTrackCountChanged != NULL)
		{
			state->Parent->OnTrackCountChanged(state->Parent);
		}
	}

	root = ILibParseXML(Result, 0, (int)strlen(Result));
	if(root != NULL)
	{
		BOOL failure = FALSE;
		if(ILibProcessXMLNodeList(root) == 0)
		{
			int i = 0;
			struct ILibXMLNode* item = root->Next;
            ILibXML_BuildNamespaceLookupTable(root);
			while(item != NULL && i < count)
			{
				struct ILibXMLAttribute* attrs = ILibGetXMLAttributes(item);
				if(attrs != NULL)
				{
					state->Objects[i] = CDS_DeserializeDidlToObject(item, attrs, 1, Result, Result + strlen(Result));
					ILibDestructXMLAttributeList(attrs);
				}
				else
				{
					failure = TRUE;
					state->Objects[i] = NULL;
				}
				i++;
				item = item->Peer;
			}
		}

		ILibDestructXMLNodeList(root);

		if(failure == TRUE)
		{
			int i;
			state->Count = 0;
			for(i = 0; i < count; i++)
			{
				if(state->Objects[i] != NULL)
				{
					CDS_ObjRef_Release(state->Objects[i]);
					state->Objects[i] = NULL;
				}
			}
			state->Error = 1;
		}

		state->Count = count;
	}
	else
	{
		state->Error = 2;
	}

	sem_post(&state->LockObject);
}

char* PC_UriUnescape(char* uri)
{
	int i;
	int j = 0;
	int length = (int)strlen(uri);
	char* result = String_CreateSize(length);
	for(i = 0; i <= length; i++)
	{
		if(uri[i] == '%')
		{
			int c1 = toupper(uri[++i]);
			int c2 = toupper(uri[++i]);
			int v1 = c1 - (int)'0';
			int v2 = c2 - (int)'0';
			if(v1 > 9)
			{
				v1 = (c1 - (int)'A') + 10;
			}
			if(v2 > 9)
			{
				v2 = (c2 - (int)'A') + 10;
			}
			v1 *= 16;
			v1 += v2;
			result[j++] = (char)v1;
		}
		else
		{
			result[j++] = uri[i];
		}
	}
	return result;
}

void ClearMetadata(PlayListManager_PC instance)
{
	int i;
	for(i = 0; i < instance->Count; i++)
	{
		if(instance->Objects[i] != NULL)
		{
			CDS_ObjRef_Release(instance->Objects[i]);
			instance->Objects[i] = NULL;
		}
	}
	instance->Count = 0;
}
