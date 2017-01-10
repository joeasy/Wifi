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
 * $Workfile: MediaServerControlPoint.c
 *
 *
 *
 */


#if defined(WIN32) && !defined(_WIN32_WCE)
	#ifdef _DEBUG
	#define _CRTDBG_MAP_ALLOC
	#endif
#endif
#include <stdlib.h>
#if defined(WINSOCK2)
	#include <winsock2.h>
	#include <ws2tcpip.h>
#elif defined(WINSOCK1)
	#include <winsock.h>
	#include <wininet.h>
#endif

#include "ILibParsers.h"
#include "MediaServerCP_ControlPoint.h"
#include "MediaServerControlPoint.h"
#include "CdsStrings.h"
#include "CdsObject.h"
#include "CdsDidlSerializer.h"
#include <stdio.h>
#include <string.h>
#include <ctype.h>

#ifndef UNDER_CE
#include "assert.h"
#endif

#ifdef _POSIX
#define strnicmp strncasecmp
#define stricmp strcasecmp
#endif
#include <stdlib.h>
#if defined(WIN32) && !defined(_WIN32_WCE)
#include <crtdbg.h>
#endif

#define UNSUPPORTED_BY_CP printf("Action is not supported by this implementation."); ASSERT(1);

#define MSCP_DEVICE_ADDED 1
#define MSCP_DEVICE_REMOVED 0

#ifdef _DEBUG
#define	ASSERT(x) assert (x)
#define MSCP_MALLOC(x) malloc(x)
#define MSCP_FREE(x) free(x)
#define DEBUGONLY(x) x
#else
#ifndef UNDER_CE
#define ASSERT(x)
#endif
#define MSCP_MALLOC(x) malloc(x)
#define MSCP_FREE(x) free(x)
#define DEBUGONLY(x)
#endif

#ifdef _TEMPDEBUG
#define TEMPDEBUGONLY(x) x
#else
#define TEMPDEBUGONLY(x) 
#endif

#if defined(WIN32) && !defined(_WIN32_WCE)
#include <crtdbg.h>
#endif

/***********************************************************************************************************************
 *	BEGIN: MSCP static values
 ***********************************************************************************************************************/

#define MIN(X, Y)  ((X) < (Y) ? (X) : (Y))

/***********************************************************************************************************************
 *	END: MSCP static values
 ***********************************************************************************************************************/

/*! \brief Struct that encapsulated the browse args and callbacks.
 */
struct BrowseInfo
{
	struct MSCP_BrowseArgs *args;
	MSCP_Fn_Result_Browse callbackBrowse;
};

/***********************************************************************************************************************
 *	BEGIN: MSCP state variables
 ***********************************************************************************************************************/

/* Function pointer for sending Browse results back to caller */
MSCP_Fn_Result_Browse				MSCP_Callback_Browse;
MSCP_Fn_Device_AddRemove			MSCP_Callback_DeviceAddRemove;

int MSCP_malloc_counter = 0;

/***********************************************************************************************************************
 *	END: MSCP state variables
 ***********************************************************************************************************************/




/***********************************************************************************************************************
 *	BEGIN: Helper methods
 ***********************************************************************************************************************/

/* TODO: debug malloc/MSCP_FREE is not thread safe */

#ifdef _DEBUG
void* MSCP_malloc(int sz)
{
	++MSCP_malloc_counter;
	return((void*)malloc(sz));
}
void MSCP_free(void* ptr)
{
	--MSCP_malloc_counter;
	free(ptr);	
}
int MSCP_malloc_GetCount()
{
	return(MSCP_malloc_counter);
}
#endif 
 
/***********************************************************************************************************************
 *	BEGIN: UPnP Callback Sinks
 *	These methods are callback sinks that are wired to the underlying UPNP stack.
 ***********************************************************************************************************************/

#ifdef MSCP_INCLUDE_FEATURE_CMS
void MSCP_ProcessResponse_GetCurrentConnectionInfo(struct UPnPService* Service,int ErrorCode,void *User,int RcsID,int AVTransportID,char* ProtocolInfo,char* PeerConnectionManager,int PeerConnectionID,char* Direction,char* Status)
{
	printf("MSCP Invoke Response: ConnectionManager/GetCurrentConnectionInfo(%d,%d,%s,%s,%d,%s,%s)\r\n",RcsID,AVTransportID,ProtocolInfo,PeerConnectionManager,PeerConnectionID,Direction,Status);
	UNSUPPORTED_BY_CP;
}

void MSCP_ProcessResponse_GetProtocolInfo(struct UPnPService* Service,int ErrorCode,void *User,char* Source,char* Sink)
{
	printf("MSCP Invoke Response: ConnectionManager/GetProtocolInfo(%s,%s)\r\n",Source,Sink);
	UNSUPPORTED_BY_CP;
}

void MSCP_ProcessResponse_GetCurrentConnectionIDs(struct UPnPService* Service,int ErrorCode,void *User,char* ConnectionIDs)
{
	printf("MSCP Invoke Response: ConnectionManager/GetCurrentConnectionIDs(%s)\r\n",ConnectionIDs);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_ExportResource(struct UPnPService* Service,int ErrorCode,void *User,unsigned int TransferID)
{
	printf("MSCP Invoke Response: ContentDirectory/ExportResource(%u)\r\n",TransferID);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_StopTransferResource(struct UPnPService* Service,int ErrorCode,void *User)
{
	printf("MSCP Invoke Response: ContentDirectory/StopTransferResource()\r\n");
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_UpdateObject(struct UPnPService* Service,int ErrorCode,void *User)
{
	printf("MSCP Invoke Response: ContentDirectory/UpdateObject()\r\n");
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_GetSystemUpdateID(struct UPnPService* Service,int ErrorCode,void *User,unsigned int Id)
{
	printf("MSCP Invoke Response: ContentDirectory/GetSystemUpdateID(%u)\r\n",Id);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_GetTransferProgress(struct UPnPService* Service,int ErrorCode,void *User,char* TransferStatus,char* TransferLength,char* TransferTotal)
{
	printf("MSCP Invoke Response: ContentDirectory/GetTransferProgress(%s,%s,%s)\r\n",TransferStatus,TransferLength,TransferTotal);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_GetSortCapabilities(struct UPnPService* Service,int ErrorCode,void *User,char* SortCaps)
{
	printf("MSCP Invoke Response: ContentDirectory/GetSortCapabilities(%s)\r\n",SortCaps);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_GetSearchCapabilities(struct UPnPService* Service,int ErrorCode,void *User,char* SearchCaps)
{
	printf("MSCP Invoke Response: ContentDirectory/GetSearchCapabilities(%s)\r\n",SearchCaps);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_Search(struct UPnPService* Service,int ErrorCode,void *User,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID)
{
	printf("MSCP Invoke Response: ContentDirectory/Search(%s,%u,%u,%u)\r\n",Result,NumberReturned,TotalMatches,UpdateID);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_ImportResource(struct UPnPService* Service,int ErrorCode,void *User,unsigned int TransferID)
{
	printf("MSCP Invoke Response: ContentDirectory/ImportResource(%u)\r\n",TransferID);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_CreateReference(struct UPnPService* Service,int ErrorCode,void *User,char* NewID)
{
	printf("MSCP Invoke Response: ContentDirectory/CreateReference(%s)\r\n",NewID);
	UNSUPPORTED_BY_CP;
}

void MSCP_ResponseSink_ContentDirectory_DeleteResource(struct UPnPService* Service,int ErrorCode,void *User)
{
	printf("MSCP Invoke Response: ContentDirectory/DeleteResource()\r\n");
	UNSUPPORTED_BY_CP;
}

void MSCP_EventSink_ConnectionManager_SourceProtocolInfo(struct UPnPService* Service,char* SourceProtocolInfo)
{
	printf("MSCP Event from %s/ConnectionManager/SourceProtocolInfo: %s\r\n",Service->Parent->FriendlyName,SourceProtocolInfo);
	UNSUPPORTED_BY_CP;
}

void MSCP_EventSink_ConnectionManager_SinkProtocolInfo(struct UPnPService* Service,char* SinkProtocolInfo)
{
	printf("MSCP Event from %s/ConnectionManager/SinkProtocolInfo: %s\r\n",Service->Parent->FriendlyName,SinkProtocolInfo);
	UNSUPPORTED_BY_CP;
}

void MSCP_EventSink_ConnectionManager_CurrentConnectionIDs(struct UPnPService* Service,char* CurrentConnectionIDs)
{
	printf("MSCP Event from %s/ConnectionManager/CurrentConnectionIDs: %s\r\n",Service->Parent->FriendlyName,CurrentConnectionIDs);
	UNSUPPORTED_BY_CP;
}

void MSCP_EventSink_ContentDirectory_TransferIDs(struct UPnPService* Service,char* TransferIDs)
{
	printf("MSCP Event from %s/ContentDirectory/TransferIDs: %s\r\n",Service->Parent->FriendlyName,TransferIDs);
	UNSUPPORTED_BY_CP;
}

#endif

void MSCP_AddRefRootDevice(void* upnp_service)
{
	struct UPnPService* Service = (struct UPnPService*) upnp_service;
	struct UPnPDevice *d = Service->Parent;
	MSCP_AddRef(d);
}

void MSCP_ReleaseRootDevice(void* upnp_service)
{
	struct UPnPService* Service = (struct UPnPService*) upnp_service;
	struct UPnPDevice *d = Service->Parent;	
	MSCP_Release(d);
}

/* IDF#3a: receive browse response */
void MSCPResponseSink_ContentDirectory_Browse(struct UPnPService* Service,int ErrorCode,void *User,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID)
{
	struct ILibXMLNode* nodeList;
	struct ILibXMLNode* node;
	struct MSCP_ResultsList *resultsList;
	struct ILibXMLAttribute *attribs;
	struct BrowseInfo *bInfo;

	int error = 0;
	int resultLen;
	int parsePeerResult = 0;

	char *lastResultPos;

	struct CdsObject *newObj;

	TEMPDEBUGONLY(printf("MSCP Invoke Response: ContentDirectory/Browse(%s,%u,%u,%u)\r\n",Result,NumberReturned,TotalMatches,UpdateID);)
	bInfo = (struct BrowseInfo*) User;

  	if ((ErrorCode == 0) && (Result != NULL))
	{
		newObj = NULL;
		resultLen = (int) strlen(Result);
		resultsList = (struct MSCP_ResultsList*) MSCP_MALLOC (sizeof(struct MSCP_ResultsList));
		memset(resultsList, 0, sizeof(struct MSCP_ResultsList));
		resultsList->LinkedList = ILibLinkedList_Create();

		lastResultPos = Result + resultLen;
		nodeList = ILibParseXML(Result, 0, resultLen);
		ILibXML_BuildNamespaceLookupTable(nodeList);
		parsePeerResult = ILibProcessXMLNodeList(nodeList);

		if (parsePeerResult != 0)
		{
			error = (int) MSCP_Error_XmlNotWellFormed;
		}
		else if (resultLen == 0)
		{
			error = (int) MSCP_Error_XmlNotWellFormed;
		}
		else
		{
			node = nodeList;
			while (node != NULL)
			{
				if (node->StartTag != 0)
				{
					/*[DONOTREPARSE] null terminate string */
					attribs = ILibGetXMLAttributes(node);
					node->Name[node->NameLength] = '\0';

					/* IDF#3b: build CDS objects out of browse response */
					newObj = NULL;
					if (stricmp(node->Name, CDS_TAG_CONTAINER) == 0)
					{
						newObj = CDS_DeserializeDidlToObject(node, attribs, 0, Result, lastResultPos);
						node = node->Next;
					}
					else if (stricmp(node->Name, CDS_TAG_ITEM) == 0)
					{
						newObj = CDS_DeserializeDidlToObject(node, attribs, 1, Result, lastResultPos);
						node = node->Next;
					}
					else if (stricmp(node->Name, CDS_TAG_DIDL) == 0)
					{
						/* this is didl-lite root node, go to first child */
						node = node->Next;
					}
					else
					{
						/* this node is not supported, go to next sibling/peer */
						if (node->Peer != NULL)
						{
							node = node->Peer;
						}
						else if(node->Parent!=NULL)
						{
							node = node->Parent->Peer;
						}
						else
						{
							node = NULL;
						}
					}

					/* IDF#3f: CDS object built... add it to list (refcount too!) */
					if (newObj != NULL)
					{
						/* set reminder of which CDS provided this object */
						MSCP_AddRefRootDevice(Service);
						newObj->CpInfo.Reserved.ServiceObject = Service;
						if (resultsList->LinkedList != NULL)
						{
							ILibLinkedList_AddTail(resultsList->LinkedList, newObj);
						}
					}

					/* free attribute mappings */
					ILibDestructXMLAttributeList(attribs);
				}
				else
				{
					node = node->Next;
				}
			}
		}

		resultsList->NumberReturned = NumberReturned;
		resultsList->TotalMatches = TotalMatches;
		resultsList->UpdateID = UpdateID;

		/* validate number of parsed objects against returned count */
		resultsList->NumberParsed = ILibLinkedList_GetCount(resultsList->LinkedList);
		if ((int)resultsList->NumberParsed != (int)resultsList->NumberReturned)
		{
			printf("MSCPResponseSink_ContentDirectory_Browse: Detected mismatch with number of objects returned=%u and parsed=%d.\r\n", resultsList->NumberReturned, resultsList->NumberParsed);
		}

		/* free resources from XML parsing */
		ILibDestructXMLNodeList(nodeList);

		/* IDF#3g: report results to FilteringBrowser */
		/* execute callback with results */
		if(error == 0)
		{
			if(bInfo->callbackBrowse != NULL)
			{
				bInfo->callbackBrowse(Service, bInfo->args, ErrorCode, resultsList);
			}
		}	
		else
		{
			if(bInfo->callbackBrowse != NULL)
			{
				bInfo->callbackBrowse(Service, bInfo->args, error, resultsList);
			}
		}
	}
	else
	{
		bInfo->callbackBrowse(Service, bInfo->args, ErrorCode, NULL);
	}

	free(bInfo);
}

void MSCPEventSink_ContentDirectory_ContainerUpdateIDs(struct UPnPService* Service,char* ContainerUpdateIDs)
{
	printf("MSCP Event from %s/ContentDirectory/ContainerUpdateIDs: %s\r\n",Service->Parent->FriendlyName,ContainerUpdateIDs);
	UNSUPPORTED_BY_CP;
}

void MSCPEventSink_ContentDirectory_SystemUpdateID(struct UPnPService* Service,unsigned int SystemUpdateID)
{
	printf("MSCP Event from %s/ContentDirectory/SystemUpdateID: %u\r\n",Service->Parent->FriendlyName,SystemUpdateID);
	UNSUPPORTED_BY_CP;
}


/* Called whenever a new device on the correct type is discovered */
void MSCP_UPnPSink_DeviceAdd(struct UPnPDevice *device)
{
	printf("MSCP Device Added: %s \r\nUDN=%s\r\n\r\n", device->FriendlyName,device->UDN);
	
	if (MSCP_Callback_DeviceAddRemove != NULL)
	{
		MSCP_Callback_DeviceAddRemove(device, MSCP_DEVICE_ADDED);
	}
}

/* Called whenever a discovered device was removed from the network */
void MSCP_UPnPSink_DeviceRemove(struct UPnPDevice *device)
{
	printf("MSCP Device Removed: %s\r\n", device->FriendlyName);

	if (MSCP_Callback_DeviceAddRemove != NULL)
	{
		MSCP_Callback_DeviceAddRemove(device, MSCP_DEVICE_REMOVED);
	}
}

/***********************************************************************************************************************
 *	END: UPnP Callback Sinks
 ***********************************************************************************************************************/





/***********************************************************************************************************************
 *	BEGIN: API method implementations
 ***********************************************************************************************************************/
void MSCP_DestroyResultsList (struct MSCP_ResultsList *resultsList)
{
	void *h;
	struct CdsObject *obj;

	if ((resultsList != NULL) && (resultsList->LinkedList))
	{
		ILibLinkedList_Lock(resultsList->LinkedList);
		h = ILibLinkedList_GetNode_Head(resultsList->LinkedList);
		while (h != NULL)
		{
			obj = (struct CdsObject*) ILibLinkedList_GetDataFromNode(h);
			if (obj != NULL)
			{
				CDS_ObjRef_Release(obj);
			}

			ILibLinkedList_Remove(h);
			h = ILibLinkedList_GetNode_Head(resultsList->LinkedList);
		}
		ILibLinkedList_UnLock(resultsList->LinkedList);
		ILibLinkedList_Destroy(resultsList->LinkedList);
		MSCP_FREE (resultsList);
	}
}


void *MSCP_Init(void *chain, MSCP_Fn_Result_Browse callbackBrowse, MSCP_Fn_Device_AddRemove callbackDeviceAddRemove)
{
	MSCP_Callback_Browse = callbackBrowse;
	MSCP_Callback_DeviceAddRemove = callbackDeviceAddRemove;

	/* Event callback function registration code */

#ifdef MSCP_INCLUDE_FEATURE_CMS
	/* These require that the generated stack monitors and reports these state variables. */
	/* TODO: Provide a BrowseOnly DeviceBuilder settings file that provides these other state variables. */
	MSCP_EventCallback_ConnectionManager_SourceProtocolInfo=&MSCPEventSink_ConnectionManager_SourceProtocolInfo;
	MSCP_EventCallback_ConnectionManager_SinkProtocolInfo=&MSCPEventSink_ConnectionManager_SinkProtocolInfo;
	MSCP_EventCallback_ConnectionManager_CurrentConnectionIDs=&MSCPEventSink_ConnectionManager_CurrentConnectionIDs;
	MSCP_EventCallback_ContentDirectory_TransferIDs=&MSCPEventSink_ContentDirectory_TransferIDs;
#endif

	MSCP_EventCallback_ContentDirectory_ContainerUpdateIDs=&MSCPEventSink_ContentDirectory_ContainerUpdateIDs;
	MSCP_EventCallback_ContentDirectory_SystemUpdateID=&MSCPEventSink_ContentDirectory_SystemUpdateID;

	/* create the underlying UPnP control point stack */
	return MSCP_CreateControlPoint(chain, &MSCP_UPnPSink_DeviceAdd, &MSCP_UPnPSink_DeviceRemove);
}

void MSCP_Invoke_Browse(void *serviceObj, struct MSCP_BrowseArgs *args)
{
	MSCP_Invoke_BrowseEx(serviceObj, args, MSCP_Callback_Browse);
}

void MSCP_Invoke_BrowseEx(void *serviceObj, struct MSCP_BrowseArgs *args, MSCP_Fn_Result_Browse callbackBrowse)
{
	char *browseFlagString;
	struct BrowseInfo* bInfo = (struct BrowseInfo*) malloc(sizeof(struct BrowseInfo));
	bInfo->args = args;
	bInfo->callbackBrowse = callbackBrowse;

	if (args->BrowseFlag == MSCP_BrowseFlag_Metadata)
	{
		browseFlagString = CDS_STRING_BROWSEMETADATA;
	}
	else
	{
		browseFlagString = CDS_STRING_BROWSE_DIRECT_CHILDREN;
	}

	MSCP_Invoke_ContentDirectory_Browse
		(
		serviceObj, 
		MSCPResponseSink_ContentDirectory_Browse, 
		bInfo, 
		args->ObjectID, 
		browseFlagString, 
		args->Filter, 
		args->StartingIndex, 
		args->RequestedCount, 
		args->SortCriteria
		);
}

struct CdsResource* MSCP_SelectBestIpNetworkResource(const struct CdsObject *mediaObj, const char *protocolInfoSet, int *ipAddressList, int ipAddressListLen)
{
	struct CdsResource* retVal = NULL, *res;
	long ipMatch = 0xFFFFFFFF, protInfoMatch = 0, bestIpMatch = 0xFFFFFFFF, bestProtInfoMatch = 0;
	int protInfoCount = 0;
	char *protInfoSet;
	int protInfoSetStringSize, protInfoSetStringLen;
	int i, pi;
	short finding;
	char **protInfos;
	char *protocol, *network, *mimeType, *info;
	int protocolLen, networkLen, mimeTypeLen, infoLen;
	char *resprotocol, *resnetwork, *resmimeType, *resinfo;
	int resprotocolLen, resnetworkLen, resmimeTypeLen, resinfoLen;
	int posIpByteStart, posIpByteLength;
	long ipq[4];
	int qi;
	unsigned long ip,reverseThis;
	char *rp;
	int cmpIp1, cmpIp2;
	int badUri;

	/*
	 *	copy the list of protocolInfo strings into protInfoSet
	 */
	protInfoSetStringLen = (int) strlen(protocolInfoSet);
	protInfoSetStringSize = protInfoSetStringLen + 1;
	protInfoSet = (char*) malloc (protInfoSetStringSize);
	memcpy(protInfoSet, protocolInfoSet, protInfoSetStringSize);
	protInfoSet[protInfoSetStringLen] = '\0';

	/*
	 *	Replace all commas in protInfoSet to NULL chars
	 *	and count the number of protocolInfo strings in the set.
	 *	Method only works if the protocolInfo are listed in form: A,B,...Z.
	 *	If we receive malformed sets like A,,B then results are undefined.
	 */
	protInfoCount = 1;
	ipq[0] = ipq[1] = ipq[2] = ipq[3] = 0;    // make up the initial value set, 20090525, yuyu
	for (i=0; i < protInfoSetStringLen; i++)
	{
		if (protInfoSet[i] == ',')
		{
			protInfoSet[i] = '\0';
			protInfoCount++;
		}
	}

	/*
	 *	create an array of char** that will allow us easy access 
	 *	to individual protocolInfo. Also redo the count
	 *	in case of inaccuracies due to bad formatting.
	 */
	protInfos = (char**) malloc (sizeof(char*) * protInfoCount);
	pi = 0;
	finding = 0;
	for (i=0; i < protInfoSetStringLen; i++)
	{
		if ((finding == 0) && (protInfoSet[i] != '\0'))
		{
			protInfos[pi] = &(protInfoSet[i]);
			pi++;
			finding = 1;
			protInfoCount++;
		}
		else if ((finding == 1) && (protInfoSet[i] == '\0'))
		{
			finding = 0;
		}
	}
	if (pi < protInfoCount) { protInfoCount = pi; }


	/*
	 *	Iterate through the different resources and track the best match.
	 */
	res = mediaObj->Res;
	while ((res != NULL) && (res->Value !=NULL))
	{
		/* the protocolInfo strings listed first have higher precedence */
		protInfoMatch = protInfoCount + 1;

		/* calculate a match value against protocolInfo */
		for (i=0; i < protInfoCount; i++)
		{
			protInfoMatch--;
			
			/*
			 * get pointers and lengths for the fields in the protocolInfo 
			 */
			protocol = protInfos[i];
			protocolLen = ILibString_IndexOf(protocol, (int) strlen(protocol), ":", 1);

			network = protocol + protocolLen + 1;
			networkLen = ILibString_IndexOf(network, (int) strlen(network), ":", 1);

			mimeType = network + networkLen + 1;
			mimeTypeLen = ILibString_IndexOf(mimeType, (int) strlen(mimeType), ":", 1);

			info = mimeType + mimeTypeLen + 1;
			infoLen = (int) strlen(info);

			
			/*
			 * get pointers and lengths for the fields in the resource's protocolInfo 
			 */
			if(res->ProtocolInfo == NULL)
			{
				free (protInfos);
				free (protInfoSet);
				return NULL;
			}

			resprotocol = res->ProtocolInfo;

			resprotocolLen = ILibString_IndexOf(resprotocol, (int) strlen(resprotocol), ":", 1);

			resnetwork = resprotocol + resprotocolLen + 1;
			resnetworkLen = ILibString_IndexOf(resnetwork, (int) strlen(resnetwork), ":", 1);

			resmimeType = resnetwork + resnetworkLen + 1;
			resmimeTypeLen = ILibString_IndexOf(resmimeType, (int) strlen(resmimeType), ":", 1);

			resinfo = resmimeType + resmimeTypeLen + 1;
			resinfoLen = (int) strlen(resinfo);
			
			/* compare each of the fields */

			if (strnicmp(protocol, resprotocol, MIN(protocolLen, resprotocolLen)) == 0)
			{
				if (
					((network[0] == '*') && (network[1] == ':'))
					|| (strnicmp(network, resnetwork, MIN(networkLen, resnetworkLen)) == 0)
					)
				{
					if (
						((mimeType[0] == '*') && (mimeType[1] == ':'))
						|| (strnicmp(mimeType, resmimeType, MIN(mimeTypeLen, resmimeTypeLen)) == 0)
						)
					{
						/*
						 *	DHWG guidelines require the DLNA.ORG_PN parameter to
						 *	show up first, so this code will work provided 
						 *	the protocolInfo values in protocolInfoSet only
						 *	have the DLNA.ORG_PN parameter in it. It's OK if
						 *	the CdsMediaResource has other parameters because
						 *	we only compare MIN(infoLen,resinfoLen).
						 */

						if (
							((info[0] == '*') && (info[1] == '\0'))
							|| (strnicmp(info, resinfo, MIN(infoLen, resinfoLen)) == 0)
							)
						{
							/*
							 *	If we get here then protocolInfo matches.
							 *	Go ahead and break since protInfoMatch is
							 *	set on every iteration.
							 */
							break;
						}
					}
				}
			}
		}

		/*
		 *	At this point, we have calculated the protInfoMatch value,
		 *	but we still need to determine if the resource has a good
		 *	chance of being routable given a particular target
		 *	IP address.
		 */

		ipMatch = 0xFFFFFFFF;
		ip = 0;

		/*
		 *	Convert text-based IP address to in-order int form.
		 *	Since the res->URI is assumed to be a valid DLNA URI,
		 *	it will have the form scheme://[ip address]:....
		 *
		 *	If by chance the URI is not in quad-notation, then we
		 *	use an IP address value of 0.0.0.0 because it's impossible
		 *	to match with host/domain names.
		 */
		posIpByteStart = ILibString_IndexOf(res->Value, (int) strlen(res->Value), "://", 3) + 3;
		if (posIpByteStart > 0)
		{
			rp = res->Value + posIpByteStart;
			qi = 0;
			badUri = 0;
			while ((qi < 4) && (badUri == 0))
			{
				posIpByteLength = 0;
				
				/* loop until we don't find a digit or until we know it's a bad URI*/
				while ((isdigit(rp[posIpByteLength]) != 0) || (badUri != 0))
				{
					posIpByteLength++;
					if (posIpByteLength > 3)
					{
						badUri = 1;
					}
				}

				if (posIpByteLength == 0) badUri = 1;

				if (badUri == 0)
				{
					ILibGetLong(rp, posIpByteLength, &(ipq[qi]));
					if (ipq[qi] > 255) badUri = 1;
					rp += (posIpByteLength + 1);
					qi++;
				}
			}
			
			if (badUri != 0)
			{
				ipq[0] = ipq[1] = ipq[2] = ipq[3] = 0;
			}
		}

		/*
		 *	Convert each network byte into a 32-bit integer,
		 *	then perform a bit mask comparison against the target ip address.
		 */
		ip = (int) (ipq[0] | (ipq[1] << 8) | (ipq[2] << 16) | (ipq[3] << 24));

		cmpIp1 = (int) ip;

		for (i=0; i < ipAddressListLen; i++)
		{
			cmpIp2 = ipAddressList[i];
			reverseThis = (cmpIp2 ^ cmpIp1);
			ipMatch = 
				((reverseThis & 0x000000ff) << 24) |
				((reverseThis & 0x0000ff00) << 8) |
				((reverseThis & 0x00ff0000) >> 8) |
				((reverseThis & 0xff000000) >> 24);

			if (
				((unsigned)ipMatch < (unsigned)bestIpMatch) ||
				((ipMatch == bestIpMatch) && (protInfoMatch > bestProtInfoMatch))
				)
			{
				retVal = res;
				bestIpMatch = ipMatch;
				bestProtInfoMatch = protInfoMatch;
			}
		}

		res = res->Next;
	}

	free (protInfos);
	free (protInfoSet);

	return retVal;
}

void MSCP_Uninit()
{
	MSCP_Callback_Browse = NULL;
	MSCP_Callback_DeviceAddRemove = NULL;
}
/***********************************************************************************************************************
 *	END: API method implementations
 ***********************************************************************************************************************/
