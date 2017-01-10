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
 * $File: $
 * $Revision: 1.9 $
 * $Author: pdamons $
 * $Date: 2006/09/21 04:31:27 $
 *
 */

#if defined(WIN32)
	#define _CRTDBG_MAP_ALLOC
#endif

#include "DMRCommon.h"
#include "CdsDidlSerializer.h"
#include "PlaySingleUri.h"
#include "MediaServerCP_ControlPoint.h"
#include "DLNAProtocolInfo.h"


struct _PlaySingleUri
{
	char* Method;
	char* UDN;
	char* ServiceID;
	char* ItemID;
};

struct _BrowseState
{
	sem_t Lock;		/* in */
	char* Result;	/* out */
};

/*int PS_InPlaceUriUnescape(char* data)
{
	char hex[3];
	char *stp;
	int src_x=0;
	int dst_x=0;

	int length = (int)strlen(data);
	hex[2]=0;

	while(src_x<length)
	{
		if(strncmp(data+src_x,"%",1)==0)
		{
			//
			// Since we encountered a '%' we know this is an escaped character
			//
			hex[0] = data[src_x+1];
			hex[1] = data[src_x+2];
			data[dst_x] = (char)strtol(hex,&stp,16);
			dst_x += 1;
			src_x += 3;
		}
		else if(src_x!=dst_x)
		{
			//
			// This doesn't need to be unescaped. If we didn't unescape anything previously
			// there is no need to copy the string either
			//
			data[dst_x] = data[src_x];
			src_x += 1;
			dst_x += 1;
		}
		else
		{
			//
			// This doesn't need to be unescaped, however we need to copy the string
			//
			src_x += 1;
			dst_x += 1;
		}
	}
	return(dst_x);
}

char* PS_UriUnescape(char* uri)
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
}*/

struct _PlaySingleUri* _ParsePlaySingleUri(char* playSingleURI)
{
	struct _PlaySingleUri* result = NULL;
	size_t allocSize = 0;
	char* pos = NULL;
	
	if(playSingleURI == NULL || ILibString_StartsWith(playSingleURI, (int)strlen(playSingleURI), "dlna-playsingle://", 18) == 0)
	{
		return NULL;
	}
	allocSize = strlen(playSingleURI) + sizeof(struct _PlaySingleUri) + 1;
	result = (struct _PlaySingleUri*)malloc(strlen(playSingleURI) + sizeof(struct _PlaySingleUri) + 1);
	memset(result, 0, allocSize);
	result->Method = (char*)result + sizeof(struct _PlaySingleUri);
	strcpy(result->Method, playSingleURI);

	pos = strchr(result->Method, (int)':');
	if(pos == NULL)
	{
		free(result);
		return NULL;
	}
	*pos = '\0';
	pos += 3;
	result->UDN = pos;
	pos = strchr(pos, (int)'?');
	if(pos == NULL)
	{
		free(result);
		return NULL;
	}
	*pos = '\0';
	if(*(pos-1) == '/')
	{
		*(pos-1) = '\0';
	}
	pos++;
	result->ServiceID = pos;
	pos = strchr(pos, (int)'&');
	if(pos == NULL)
	{
		free(result);
		return NULL;
	}
	*pos = '\0';
	pos++;

	result->ItemID = pos;

	result->ServiceID = strchr(result->ServiceID, '=');
	result->ItemID = strchr(result->ItemID, '=');

	if(result->ItemID == NULL || result->ServiceID == NULL)
	{
		free(result);
		return NULL;
	}

	result->ItemID++;
	result->ServiceID++;

	ILibInPlaceHTTPUnEscape(result->ItemID);
	ILibInPlaceHTTPUnEscape(result->UDN);
	ILibInPlaceHTTPUnEscape(result->ServiceID);

	return result;
}

void _Callback(struct UPnPService *sender, int ErrorCode, void *user,
			   char* Result, unsigned int NumberReturned, 
			   unsigned int TotalMatches, unsigned int UpdateID)
{
	struct _BrowseState* state = (struct _BrowseState*)user;

	if(ErrorCode == 0)
	{
		state->Result = (char*)malloc(strlen(Result) + 1);
		strcpy(state->Result, Result);
	}

	sem_post(&state->Lock);
}

struct CdsObject* ResolvePlaySingleURI(void* MSCPToken, char* playSingleUri)
{
	struct CdsObject* result = NULL;
	struct _PlaySingleUri* psUri = NULL;
	struct _BrowseState* state = NULL;
	struct UPnPDevice* device = NULL;
	struct UPnPService* service = NULL;
	char* rawResult = NULL;

	if(MSCPToken == NULL || playSingleUri == NULL)
	{
		return NULL;
	}
	psUri = _ParsePlaySingleUri(playSingleUri);
	if(psUri == NULL)
	{
		return NULL;
	}
	device = MSCP_GetDeviceAtUDN(MSCPToken, psUri->UDN);
	if(device == NULL)
	{
		free(psUri);
		return NULL;
	}
	service = MSCP_GetService_ContentDirectory(device);
	if(service == NULL)
	{
		free(psUri);
		return NULL;
	}

	state = (struct _BrowseState*)malloc(sizeof(struct _BrowseState));
	sem_init(&state->Lock, 0, 1);
	state->Result = NULL;

	sem_wait(&state->Lock);

	MSCP_Invoke_ContentDirectory_Browse(service, &_Callback, state, psUri->ItemID, "BrowseMetadata", "res", 0, 0, "");

	sem_wait(&state->Lock);
	sem_destroy(&state->Lock);
//	state->Lock = 0;
	sem_init(&(state->Lock), 0, 1);

	rawResult = state->Result;

	free(state);
	free(psUri);

	if(rawResult != NULL)
	{
		struct ILibXMLNode* root = ILibParseXML(rawResult, 0, (int)strlen(rawResult));
		if(root != NULL)
		{
			if(ILibProcessXMLNodeList(root) == 0)
			{
				struct ILibXMLNode* item = root->Next;
				if(item != NULL && strncmp(item->Name, "item", 4) == 0)
				{
					struct ILibXMLAttribute* attrs = ILibGetXMLAttributes(item);
					if(attrs != NULL)
					{
						result = CDS_DeserializeDidlToObject(item, attrs, 1, rawResult, rawResult + strlen(rawResult));
						ILibDestructXMLAttributeList(attrs);
					}
				}
			}
			ILibDestructXMLNodeList(root);
		}
		free(rawResult);
	}

	return result;
}
