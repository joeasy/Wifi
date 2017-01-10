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
 * $Workfile: ILibSSDPClient.h
 * $Revision: #1.0.2718.23851
 * $Author:   Intel Corporation, Intel Device Builder
 * $Date:     2007年7月6日
 *
 *
 *
 */

#ifndef __ILibSSDPClient__
#define __ILibSSDPClient__

#include "UPnPControlPointStructs.h"

#ifdef __cplusplus
extern "C" {
#endif

void* ILibCreateSSDPClientModule(void *chain, char* DeviceURN, int DeviceURNLength, void (*FunctionCallback)(void *sender, char* UDN, int Alive, char* LocationURL, int Timeout, UPnPSSDP_MESSAGE m, void *user),void *user);
void ILibSSDP_IPAddressListChanged(void *SSDPToken);

#ifdef __cplusplus
};
#endif

#endif
