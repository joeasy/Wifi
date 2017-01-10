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
 * $Workfile: MyString.h
 * $Revision:
 * $Author: Intel, DPA, Solution Architecture
 * $Date: 10/05/02
 * $Archive:
 */

/*
 * Implements additional string functionality.
 */

#ifndef MY_STRING_H

#include <ctype.h>
#include <stdlib.h>
#ifdef __cplusplus
extern "C" {
#endif
/*
 *	NOTE: Enabling for ISO 10646.
 *	You can enable MyString to support the full range of ISO 10646
 *	by changing the wchar_t references to a 32-bit character types.
 *	The wchar_t type is limited to 16-bit character types, so
 *	the compiler may report warnings for lines of code that work
 *	with characters wider than 16-bits.
 */

/*
 *	EndsWith()
 *		str					: the string to analyze
 *		endsWith			: the token to find at the end of str
 *		ignoreCase			: nonzero indicates case insensitive search
 *
 *	If "str" ends with "endsWith", then we return nonzero.
 */
int EndsWith(const char* str, const char* endsWith, int ignoreCase);

/*
 *	IndexOf()
 *		str					: the string to analyze
 *		findThis			: the token to find 
 *		ignoreCase			: nonzero indicates case insensitive search
 *
 *	Returns the first index where findThis can be found in str.
 *	Returns -1 if not found.
 */
int IndexOf(const char* str, const char* findThis);

/*
 *	IndexOf()
 *		str					: the string to analyze
 *		findThis			: the token to find 
 *		ignoreCase			: nonzero indicates case insensitive search
 *
 *	Returns the last index where findThis can be found in str.
 *	Returns -1 if not found.
 */
int LastIndexOf(const char* str, const char* findThis);

/*
 *	IndexOf()
 *		str					: the string to analyze
 *		startsWith			: the token to find 
 *		ignoreCase			: nonzero indicates case insensitive search
 *
 *	Returns nonzero value if the string starts with 'startsWith'.
 */
int StartsWith(const char* str, const char* startsWith, int ignoreCase);

int Utf8ToAnsi(char *dest, const char *src, int destLen);
#ifdef UNICODE
int Utf8ToWide(wchar_t *dest, const char *src, int destLen);
#else
int Utf8ToWide(unsigned short *dest, const char *src, int destLen);
#endif
int strToUtf8(char *dest, const char *src, int destSize, int isWide, int *charactersConverted);
int strUtf8Len(char *src, int isWide, int asEscapedUri);

/*
 *	Stores UTF8-compliant escaped URI in 'dest'.
 *	Returns number of bytes used in dest.
 */
int strToEscapedUri(char *dest, const char *src, int destSize, int isWide, int *charactersConverted);

/* makes a copy of the string by mallocing a new buffer and copying
 * the string into it 
 */
char *MakeStrcpy(const char *string);


/* Chomps off the trailing newline on a string much like Perl's chomp function
 */
void StrChomp(char *s);
#ifdef __cplusplus
}
#endif
#endif
