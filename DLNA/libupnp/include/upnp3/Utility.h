/*
 * INTEL CONFIDENTIAL
 * Copyright (c) 2002, 2003 Intel Corporation.  All rights reserved.
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
 *
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by Intel in writing.
 *  
 * $Workfile: Utility.h
 * $Revision:
 * $Author: Intel, DPA, Solution Architecture
 * $Date: 10/05/02
 * $Archive:
 */


#ifndef UTILITY_H
#define UTILITY_H
#ifdef __cplusplus
extern "C" {
#endif

#define UTL_MALLOC malloc
#define UTL_FREE   free
////#define DEBUGONLY(x)



/*
 *	Defines an empty string.
 */
#define EMPTY_STRING ""

/*
 *	Used to prevent warnings on assinign NULL
 *	to a char*
 */
#define NULL_CHAR '\0'

/*
 *	Copies memory from one location to a new location 
 *	and returns the pointer.
 */
void* CopyArray(int elementSize, int numElements, const void* data);

/*
 *	Does a normal free on freeThis, except
 *	that it checks for non-NULL value first.
 */
void _SafeFree (void* freeThis);

/*
 *	This macro calls _SafeFree and then assigns
 *	the pointer to NULL, for extra safety.
 */
//#define SafeFree(x) _SafeFree(x); x = NULL;
void SafeFree(void** freeThis);

/*
 *	Copies a string safely. 
 *	If str is NULL returned value is an empty string.
 *
 *	If storeHere is NULL, then memory is allocated
 *	by the method. Use SafeFree() to deallocate
 *	that memory.
 *
 *	Returns the copy of str.
 */
char* SafeStringCopy (char* storeHere, const char* str);

/*
 *	Spawns a normal thread that is detached
 *	from the calling thread.
 */
void* SpawnDetachedThread(void* method, void* arg);
#ifdef __cplusplus
}
#endif
#endif
