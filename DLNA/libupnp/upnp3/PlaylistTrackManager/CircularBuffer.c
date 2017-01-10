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
 * $Revision: 1.5 $
 * $Author: pdamons $
 * $Date: 2006/02/07 21:21:03 $
 *
 */

#if defined(WIN32)
	#define _CRTDBG_MAP_ALLOC
#endif

#include "DMRCommon.h"
#include "CircularBuffer.h"


struct _CircularBuffer
{
	int		_size;
	char*	_buffer;
	int		_head;
	int		_tail;
	int		_empty;
};


/* Forward References */


/* Public Methods */
CircularBuffer CircularBuffer_Create(int size)
{
	struct _CircularBuffer* instance = (struct _CircularBuffer*)malloc(sizeof(struct _CircularBuffer));
	if(instance != NULL)
	{
		memset(instance, 0, sizeof(struct _CircularBuffer));
		instance->_size = size;
		instance->_buffer = (char*)malloc((size_t)size);
		instance->_empty = 1;
	}
	return (CircularBuffer)instance;
}

void CircularBuffer_Destroy(CircularBuffer buffer)
{
	struct _CircularBuffer* instance = (struct _CircularBuffer*)buffer;
	if(instance != NULL)
	{
		free(instance->_buffer);
		free(instance);
	}
}

int CircularBuffer_GetLength(CircularBuffer buffer)
{
	struct _CircularBuffer* instance = (struct _CircularBuffer*)buffer;
	if(instance->_head == instance->_tail && instance->_empty == 0)
	{
		return instance->_size;
	}
	if(instance->_head == instance->_tail && instance->_empty == 1)
	{
		return 0;
	}
	else if(instance->_head < instance->_tail)
	{
		return instance->_tail - instance->_head;
	}
	else if(instance->_head > instance->_tail)
	{
		return instance->_size - (instance->_head - instance->_tail);
	}
	return -1;
}

int CircularBuffer_GetFreeSpace(CircularBuffer buffer)
{
	struct _CircularBuffer* instance = (struct _CircularBuffer*)buffer;
	return instance->_size - CircularBuffer_GetLength(buffer);
}

int CircularBuffer_AddBlock(CircularBuffer buffer, char* memory, int offset, int length)
{
	int startPos;
	struct _CircularBuffer* instance = (struct _CircularBuffer*)buffer;
	if(length < 1 || length > instance->_size || CircularBuffer_GetFreeSpace(buffer) < length)
	{
		return 0;
	}

	startPos = instance->_tail;
	instance->_tail = (instance->_tail + length) % instance->_size;

	if(instance->_tail == instance->_head)
	{
		instance->_empty = 0;
	}

	if(startPos + length >= instance->_size)
	{
		int len1 = instance->_size - startPos;
		int len2 = length - len1;
		memcpy(instance->_buffer + startPos, memory + offset, (size_t)len1);
		if(len2 > 0)
		{
			memcpy(instance->_buffer, memory + offset + len1, (size_t)len2);
		}
	}
	else
	{
		memcpy(instance->_buffer + startPos, memory + offset, (size_t)length);
	}

	return 1;
}

int CircularBuffer_ConsumeBytes(CircularBuffer buffer, int length)
{
	struct _CircularBuffer* instance = (struct _CircularBuffer*)buffer;
	if(length < 1 || length > instance->_size || CircularBuffer_GetLength(buffer) < length)
	{
		return 0;
	}

	instance->_head = (instance->_head + length) % instance->_size;

	if(instance->_head == instance->_tail)
	{
		instance->_empty = 1;
	}

	return 1;
}

int CircularBuffer_CopyFrom(CircularBuffer buffer, char* memory, int offset, int startIndex, int length)
{
	struct _CircularBuffer* instance = (struct _CircularBuffer*)buffer;
	int startPos = (instance->_head + startIndex) % instance->_size;
	if(length < 1 || length > instance->_size || length > CircularBuffer_GetLength(buffer))
	{
		return 0;
	}
	if(startPos + length >= instance->_size)
	{
		int len1 = instance->_size - startPos;
		int len2 = length - len1;
		memcpy(memory + offset, instance->_buffer + startPos, (size_t)len1);
		if(len2 > 0)
		{
			memcpy(memory + offset + len1, instance->_buffer, (size_t)len2);
		}
	}
	else
	{
		memcpy(memory + offset, instance->_buffer + startPos, length);
	}

	return 1;
}

int CircularBuffer_FindPattern(CircularBuffer buffer, int startIndex, char* pattern, int patternLength)
{
	int count = CircularBuffer_GetLength(buffer) - patternLength - startIndex;
	int i = 0;
	int foundIndex = -1;
	char* localBuffer = NULL;
	if(count < 0)
	{
		return foundIndex;
	}
	localBuffer = (char*)malloc(patternLength);
	for(i = 0; i < count; i++)
	{
		CircularBuffer_CopyFrom(buffer, localBuffer, 0, i + startIndex, patternLength);
		if(memcmp(pattern, localBuffer, patternLength) == 0)
		{
			foundIndex = startIndex + i;
			break;
		}
	}
	free(localBuffer);

	return foundIndex;
}

int CircularBuffer_FindLastPattern(CircularBuffer buffer, int startIndex, char* pattern, int patternLength)
{
	int count = CircularBuffer_GetLength(buffer) - patternLength - startIndex;
	int i = 0;
	int foundIndex = -1;
	char* localBuffer = NULL;
	if(count < 0)
	{
		return foundIndex;
	}
	localBuffer = (char*)malloc(patternLength);
	for(i = count - 1; i >= 0; i--)
	{
		CircularBuffer_CopyFrom(buffer, localBuffer, 0, i + startIndex, patternLength);
		if(memcmp(pattern, localBuffer, patternLength) == 0)
		{
			foundIndex = startIndex + i;
			break;
		}
	}
	free(localBuffer);

	return foundIndex;
}


/* Implementation */
