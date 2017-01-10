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
 * $Workfile: MediaServerControlPoint.h
 *
 *
 *
 */

#ifndef _MEDIASERVERCONTROLPOINT_H
#define _MEDIASERVERCONTROLPOINT_H

#include "UPnPControlPointStructs.h"
#include "CdsObject.h"

/*! \file MediaServerControlPoint.h 
	\brief This is the underlying control point for the FilteringBrowser and UploadController components for browsing
	CDS objects on a DMS.  If an application needs to issue a custom browse request, it can use this module
	to send browse requests directly to the media server and pass in the callback where the reponse should go to.
*/

/*! \defgroup MSCP DLNA - Media Server Control Point
	\brief 
	This module provides the functionality for issuing browse request and passes the results 
	from the DMS back to the FilteringBrowser. 

	\{
*/



/*!	\brief Control Point error codes. None of these error codes are allowed to overlap
 *	with the UPnP, UPnP-AV error code ranges.
 */
enum MSCP_NonstandardErrorCodes
{
	MSCP_Error_XmlNotWellFormed		= 1000,
};

/*!	\brief This flag to indicate whether the browse request is a <b>BrowseMetadata</b> or <b>BrowseDirectChilren</b>.
 */
enum MSCP_Enum_BrowseFlag
{
	MSCP_BrowseFlag_Metadata = 0,
	MSCP_BrowseFlag_Children
};

/*! \brief Struct that encapsulated the browse results.
 */
struct MSCP_ResultsList
{
	void*		 LinkedList;
	unsigned int NumberReturned;
	unsigned int TotalMatches;
	unsigned int UpdateID;

	int NumberParsed;
};

/*! \brief Struct that represents a browse request.
 */
struct MSCP_BrowseArgs
{
	char *ObjectID;
	enum MSCP_Enum_BrowseFlag BrowseFlag;
	char *Filter;
	unsigned int StartingIndex;
	unsigned int RequestedCount;
	char *SortCriteria;

	/* browse request initiator can attach a misc field for use in results processing */
	void *UserObject;
};

typedef void (*MSCP_Fn_Result_Browse) (void *serviceObj, struct MSCP_BrowseArgs *args, int errorCode, struct MSCP_ResultsList *results);
typedef void (*MSCP_Fn_Device_AddRemove) (struct UPnPDevice *device, int added);

/*! \brief Use this method to destroy the results of a Browse request.

	\param[in] resultsList The browse results that needs to be destroyed.	
 */
void MSCP_DestroyResultsList (struct MSCP_ResultsList *resultsList);

/*! \brief Must call this method once at the very beginning.

	Caller should registers callbacks for Browse responses and when MediaServers enter/leave the UPnP network.
	\param[in] chain The thread chain, obtained from ILibCreateChain.
 	\param[in] callbackBrowse	The callback to execute when results for a browse request are received.
 	\param[in] callbackDeviceAddRemove The callback to execute when a MediaServer leaves/enters the UPnP network.
 
	\returns a control point object.
*/
void *MSCP_Init(void *chain, MSCP_Fn_Result_Browse callbackBrowse, MSCP_Fn_Device_AddRemove callbackDeviceAddRemove);

/*! \brief Call this method to perform a browse request.

	\param[in] serviceObj The CDS service object for the MediaServer.
 	\param[in] args	The arguments of the browse request.
 */
void MSCP_Invoke_Browse(void *serviceObj, struct MSCP_BrowseArgs *args);

void MSCP_Invoke_BrowseEx(void *serviceObj, struct MSCP_BrowseArgs *args, MSCP_Fn_Result_Browse callbackBrowse);

/*! \brief Use this method to select the best matched MSCP_MediaResource.
	 The resource object's IP-based URI can then be used to actually acquire the content.

	 \param[in] mediaObj The CDS object with zero or more resources
	 \param[in] protocolInfoSet	A comma-delimited set of protocolInfo, sorted with target's preferred formats frist
	 \param[in] ipAddressList The desired ipAddress, in network byte order form.
	 \param[in] ipAddressListLen The length of the ipAddressList.
	 \returns NULL if no acceptable resource was found.
 */
struct CdsResource* MSCP_SelectBestIpNetworkResource(const struct CdsObject *mediaObj, const char *protocolInfoSet, int *ipAddressList, int ipAddressListLen);

/*! \brief Call this method for cleanup after the control points shutsdown.
 */
void MSCP_Uninit();

/*! \brief Call this method increment a reference to the UPnP Service object.
 */
void MSCP_AddRefRootDevice(void* upnp_service);

/*! \brief Call this method decrement a reference to the UPnP Service object.
 */
void MSCP_ReleaseRootDevice(void* upnp_service);

/*! \} */

#endif
