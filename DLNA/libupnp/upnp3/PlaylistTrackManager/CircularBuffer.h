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
 * $Revision: 1.4 $
 * $Author: pdamons $
 * $Date: 2006/11/17 22:11:39 $
 *
 */

#ifndef __CIRCULARBUFFER_H__
#define __CIRCULARBUFFER_H__

/**
    \file CircularBuffer.h
    \ingroup DLNADMR
	\brief Utility API for creating a circular buffer.

    \addtogroup DLNADMR DLNA MediaRenderer Microstack (DMR)

    \{
*/

/** \brief The type 'CircularBuffer' is a 'void*'.
*/
typedef void *CircularBuffer;

/** \brief Create a CircularBuffer object with a given byte size.
	\param size Size in bytes of the circular buffer.
	\returns A CircularBuffer instance.
*/
CircularBuffer CircularBuffer_Create(int size);

/** \brief Destroys a CircularBuffer instance.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer instance.
*/
void CircularBuffer_Destroy(CircularBuffer buffer);

/** \brief Returns the free space in the buffer.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer
		   instance.
	\returns the number of bytes that can be inserted into the \ref CircularBuffer.
*/
int CircularBuffer_GetFreeSpace(CircularBuffer buffer);

/** \brief Writes bytes into the buffer consuming buffer space.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer
		   instance.
	\param memory Source buffer to write the bytes from.
	\param offset Offset in bytes from the beginning in the source buffer to copy
		   the bytes from.
	\param length The number of bytes to copy from the source buffer.
	\returns Zero (0) if the method fails or 1 if the method succeeds.

	\warning This method will fail if you attempt to insert more bytes than
			 there is free space (see \ref CircularBuffer_GetFreeSpace).
*/
int CircularBuffer_AddBlock(CircularBuffer buffer, char* memory, int offset, int length);

/** \brief Throw away a specific number of bytes from the circular buffer.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer
		   instance.
	\param length This is the number of bytes to discard from the circular
	       buffer.  This is alway relative to the beginning of the buffer.
	\returns Zero (0) if the method fails (length < 1 or
		   length > \ref CircularBuffer_GetLength.
*/
int CircularBuffer_ConsumeBytes(CircularBuffer buffer, int length);

/** \brief Reads (without consuming bytes) from the \ref CircularBuffer.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer
		   instance.
	\param memory The destination buffer.
	\param offset The offset in bytes in the destination buffer to copy to.
	\param startIndex The offset in the source \ref CircularBuffer to copy
		   from.
	\param length The number of bytes to copy from the the source to the
		   destination.
	\returns Zero (0) if the method fails or 1 if the method succeeds.
*/
int CircularBuffer_CopyFrom(CircularBuffer buffer, char* memory, int offset, int startIndex, int length);

/** \brief Finds a byte pattern in the \ref CircularBuffer from the beginning.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer
		   instance.
	\param startIndex The start index in the circular buffer instance from
		   the beginning to start the pattern search.
	\param pattern The byte pattern to look for in the \ref CircularBuffer.
	\param patternLength The byte length of the pattern to search for.
	\returns The index of the first occurance of the pattern in the instance or
			 -1 if the instance was not found.
*/
int CircularBuffer_FindPattern(CircularBuffer buffer, int startIndex, char* pattern, int patternLength);

/** \brief Finds a byte pattern in the \ref CircularBuffer from the end.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer
		   instance.
	\param startIndex The start index in the circular buffer instance from
		   the end to start the pattern search.
	\param pattern The byte pattern to look for in the \ref CircularBuffer.
	\param patternLength The byte length of the pattern to search for.
	\returns The index of the last occurance of the pattern in the instance or
			 -1 if the instance was not found.
*/
int CircularBuffer_FindLastPattern(CircularBuffer buffer, int startIndex, char* pattern, int patternLength);

/** \brief Gets the used length of the \ref CircularBuffer.
	\param buffer This is the 'this' pointer to the \ref CircularBuffer
		   instance.
	\returns The number of bytes currently in the instance or -1 if the
			 method fails.
*/
int CircularBuffer_GetLength(CircularBuffer buffer);

/* \} */
#endif /* __CIRCULARBUFFER_H__ */
