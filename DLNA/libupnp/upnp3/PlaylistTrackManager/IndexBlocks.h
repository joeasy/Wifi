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
 * $Revision: 1.3 $
 * $Author: pdamons $
 * $Date: 2006/02/07 21:21:03 $
 *
 */

#ifndef __INDEXBLOCKS_H__
#define __INDEXBLOCKS_H__


typedef void *IndexBlocks;


IndexBlocks IndexBlocks_Create();
void IndexBlocks_Destroy(IndexBlocks blocks);

int IndexBlocks_AddBlock(IndexBlocks blocks, int streamOffset, int length, int trackCount);

int IndexBlocks_GetTrackCount(IndexBlocks blocks);
int IndexBlocks_GetTrackRangeInfo(IndexBlocks blocks, int trackNumber, int* byteOffset, int* length, int* trackOffset);

#endif /* __INDEXBLOCKS_H__ */
