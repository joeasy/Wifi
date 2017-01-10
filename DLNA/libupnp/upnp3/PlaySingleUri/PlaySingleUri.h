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
 * $Revision: 1.6 $
 * $Author: pdamons $
 * $Date: 2006/02/07 21:17:33 $
 *
 */

#ifndef __PLAYSINGLEURI_H__
#define __PLAYSINGLEURI_H__

#include "DMRCommon.h"
#include "CdsObject.h"
/**
    \file PlaySingleUri.h
    \ingroup DLNADMR
	\brief DLNA DMR PlaySingleUri API

    This file provides a API to resolve a playsingle URI to a struct CdsObject*
	that contains a list of resources that the caller can pick from to get
	a real media URI.

    \addtogroup DLNADMR DLNA MediaRenderer Microstack (DMR)
    \brief This module defines the component to resolve PlaySingleUris.
    \{
*/


/** \brief This method will take a resource that specified a PlaySingleUri
	and return the same resource with the resolved URI for the media.
	\param MSCPToken [in] This is the token returned from the
		MediaServerCP_CreateControlPoint method in the media server control
		point code.
	\param playSingleUri [in] This is the playSingle Uri as specified in the DLNA spec.
	\returns The struct CdsObject* for the resolved PlaySingle Uri of the if this
	    function succeeds or NULL if the function fails.  The caller must pick the
		appropriate URI from the resources in the struct CdsObject*.

	\warning This is a syncronous method that does nework communications.  It
	    may take quite a few seconds to execute.
*/
struct CdsObject* ResolvePlaySingleURI(void* MSCPToken, char* playSingleUri);

/* \} */
#endif /* __PLAYSINGLEURI_H__ */
