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
 * $Workfile: MyString.c
 * $Revision:
 * $Author: Intel, DPA, Solution Architecture
 * $Date: 10/05/02
 * $Archive:
 */


 /*
 * Implements additional string functionality.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "MyString.h"

#define ASSERT(x) assert(x)
#define MEMORY_CHECK(x) 
#define strnicmp strncasecmp

#if 0
int EndsWith(const char* str, const char* endsWith, int ignoreCase)
{
	int strLen, ewLen, offset;
	int cmp = 0;

	strLen = (int) strlen(str);
	ewLen = (int) strlen(endsWith);
	if (ewLen > strLen) return 0;
	offset = strLen - ewLen;

	if (ignoreCase != 0)
	{
		cmp = strnicmp(str+offset, endsWith, ewLen);
	}
	else
	{
		cmp = strncmp(str+offset, endsWith, ewLen);
	}

	return cmp == 0?1:0;
}
#endif

int IndexOf(const char* str, const char* findThis)
{
	int i,j, strLen, ftLen;
	
	strLen = (int) strlen(str);
	ftLen = (int) strlen(findThis);

	if (ftLen <= strLen)
	{
		for (i=0; i < strLen; i++)
		{
			for (j=0; j < ftLen; j++)
			{
				if (str[i+j] != findThis[j]) break;
			}
			if (j == ftLen) return i;
		}
	}

	return -1;
}

int LastIndexOf(const char* str, const char* findThis)
{
	int i,j,strLen,ftLen;

	strLen = (int) strlen(str);
	ftLen = (int) strlen(findThis);

	if (ftLen <= strLen)
	{
		for (i=strLen-ftLen; i >= 0; i--)
		{
			for (j=0; j < ftLen; j++)
			{
				if (str[i+j] != findThis[j]) break;
			}
			if (j == ftLen) return i;
		}
	}

	return -1;
}

int StartsWith(const char* str, const char* startsWith, int ignoreCase)
{
	int cmp;

	if (ignoreCase != 0)
	{
		cmp = strnicmp(str, startsWith, (int) strlen(startsWith));
	}
	else
	{
		cmp = strncmp(str, startsWith, (int) strlen(startsWith));
	}

	return cmp == 0?1:0;
}


int Utf8ToAnsi(char *dest, const char *src, int destLen)
{
	int di,si,si2,si3;
	int conv;

	conv = 0;
	di = 0;
	si = 0;
	destLen--;

	while ((src[si] != '\0') && (di < destLen))
	{
		si2 = si+1;
		si3 = si2+1;

		if ((unsigned char)src[si] <= 0x7F)
		{
			dest[di++] = src[si++];
			conv++;
		}
		else if ((unsigned char)src[si] <= 0xDF)
		{
			if (((unsigned char)src[si] & 0x1C) == 0)
			{
				dest[di++] = ((src[si] & 0x03) << 6) | (src[si2] & 0x3F);
				conv++;
			}
			si+=2;
		}
		else if ((unsigned char)src[si] <= 0xEF)
		{
			/* outside ansi char set */
			si+=3;
		}
		else if ((unsigned char)src[si] <= 0xF7)
		{
			/* outside ansi char set */
			si+=4;
		}
	}
	dest[di] = '\0';

	return conv;
}

int Utf8ToWide(unsigned short *dest, const char *src, int destLen)
{
	int di,si,si2,si3;
	int conv;

	conv = 0;
	di = 0;
	si = 0;
	destLen--;

	while ((src[si] != '\0') && (di < destLen))
	{
		si2 = si+1;
		si3 = si2+1;

		if ((unsigned char)src[si] <= 0x7F)
		{
			dest[di++] = src[si++];
			conv++;
		}
		else if ((unsigned char)src[si] <= 0xDF)
		{
			dest[di++] = ((src[si] & 0x1F) << 6) | (src[si2] & 0x3F);
			conv++;
			si+=2;
		}
		else if ((unsigned char)src[si] <= 0xEF)
		{
			dest[di++] = ((src[si] & 0x0F) << 12) | ((src[si2] & 0x3F) << 6) | (src[si3] & 0x3F);
			conv++;
			si+=3;
		}
		else if ((unsigned char)src[si] <= 0xF7)
		{
			/* code should never execute because it's for unicode characters greater than 16 bits */
			/*
			dest[di++] = ((src[si] & 0x07) << 18) | ((src[si2] & 0x3F) << 6) | ((src[si2] & 0x3F) << 6) | (src[si3] & 0x3F);
			conv++;
			 */
			si+=4;
		}
	}
	dest[di] = '\0';

	return conv;
}

/*
 *	isalnum() is not sufficient because locales can affect the return value
 *	digits:		0x30 - 0x39: 
 *	upper:		0x41 - 0x5A
 *	lower:		0x61 - 0x7A
 *	dot:		0x2E
 *	slash:		0x2F
 *	colon:		0x3A
 *	ampersand:	0x26
 *	underscore: 0x5F
 *	query:		0x3F
 */
#define _isUriPrintable(c) (((c>=0x30) && (c<=0x39)) || ((c>=0x41) && (c<=0x5A)) || ((c>=0x61) && (c<=0x7A)) || (c==0x2E) || (c==0x2F) || (c==0x3A) || (c==0x36) || (c==0x5F) || (c==0x3F))

int strUtf8Len(char *src, int isWide, int asEscapedUri)
{
	unsigned short wcv;		/* wide char scalar */
	int i,j,k=1;

	i = wcv = j = 0;
	
	do
	{
		wcv =  (unsigned char) src[i++];

		if (isWide != 0)
		{
			wcv |= (((unsigned char) src[i++]) << 8);
		}

		if (wcv < 0x80)
		{
			if ((asEscapedUri!=0) && (!(_isUriPrintable((char)wcv))))
			{
				j+=3;
			}
			else 
			{
				j++;
			}
		}
		else
		{
			if (wcv < 0x0800)
			{
				k = 2;
			}
			/*
			 *	The stuff below is for ISO 10646, which is 32-bit.
			 */
			else if (wcv < 0x10000)
			{
				k = 3;
			}
			else if (wcv < 0x200000)
			{
				k = 4;
			}
			else if (wcv < 0x4000000)
			{
				k = 5;
			}
			else if (wcv < 0x8000000)
			{
				k = 6;
			}

			if (asEscapedUri)
			{
				j += (3*k);
			}
			else
			{
				j += k;
			}
		}
	}
	while (wcv != 0);

	return j;
}

int _strToUtf8(char *dest, const char *src, int destSize, int isWide, int *charactersConverted, int uriEscape)
{
	unsigned short wcv;		/* wide char scalar */
	int i,j;
	int cc;			/* char count */
	int destLen;
	
	int numBytes, k;
	unsigned char mask;
	char shiftRight;
	int breakEarly = 0;
	
	destLen = destSize - 1;
	cc = 0;
	i = 0;
	wcv = 0;
	j=0;

	
	do
	{
		wcv =  (unsigned char) src[i++];

		if (isWide != 0)
		{
			wcv |= (((unsigned char) src[i++]) << 8);
		}

		/* 0x00000000 through 0x0000007F is: 0??????? */
		if ((wcv < 0x80) && (j < destLen))
		{
			if (uriEscape != 0)
			{
				if (_isUriPrintable((char)wcv))
				{
					dest[j++] = (char) wcv;
					cc++;
				}
				else if (j < destLen - 3)
				{
					j += sprintf(dest+j, "%%%x", wcv);
					cc++;
				}
				else
				{
					// no more room
					break;
				}
			}
			else
			{
				dest[j++] = (char) wcv;
				cc++;
			}
		}
		/* multiple-byte UTF-8 character */
		else
		{
			if (wcv < 0x0800)
			{
				numBytes = 2;
				mask = 0xC0;
				shiftRight = 6;
			}
			/*
			 *	The stuff below is for ISO 10646, which is 32-bit.
			 */
			else if (wcv < 0x10000)
			{
				numBytes = 3;
				mask = 0xE0;
				shiftRight = 12;
			}
			else if (wcv < 0x200000)
			{
				numBytes = 4;
				mask = 0xF0;
				shiftRight = 18;
			}
			else if (wcv < 0x4000000)
			{
				numBytes = 5;
				mask = 0xF8;
				shiftRight = 24;
			}
			else if (wcv < 0x8000000)
			{
				numBytes = 6;
				mask = 0xFC;
				shiftRight = 30;
			}

			// build first byte
			if ((uriEscape != 0) && (j < destLen - 3))
			{
				j += sprintf(dest+j, "%%%x", ((wcv >> shiftRight) | mask));
				cc++;
			}
			else if (j < destLen - 3)
			{
				dest[j++] = (char) ((wcv >> shiftRight) | mask);
				cc++;
			}
			else
			{
				// no more room
				break;
			}

			// build trailing sequence of 10?????? bit-sequence bytes
			if (j < destLen - numBytes)
			{
				breakEarly = 0;
			}
			else if ((uriEscape != 0) && (j < (destLen - (3 * numBytes))))
			{
				breakEarly = 0;
			}
			else
			{
				breakEarly = 1;
			}

			if (breakEarly == 0)
			{
				for (k=1; k < numBytes; k++)
				{
					shiftRight -= 6;
					ASSERT(shiftRight >= 0);

					if (uriEscape != 0)
					{
						j += sprintf(dest+j, "%%%x", (((wcv >> shiftRight) & 0x3F) | 0x80));
						cc++;
					}
					else
					{
						dest[j++] = (char) (((wcv >> shiftRight) &0x3F) | 0x80);
						cc++;
					}
				}
			}

			if (breakEarly)
			{
				break;
			}
		}
	}
	while (wcv != 0);

	//ensure properly terminated string
	j--;
	dest[j] = '\0';
	if (charactersConverted != NULL)
	{
		*charactersConverted = cc - 1;
	}

	MEMORY_CHECK(ASSERT(j < destSize));

	return j;
}


int strToUtf8(char *dest, const char *src, int destSize, int isWide, int *charactersConverted)
{
	return _strToUtf8(dest, src, destSize, isWide, charactersConverted, 0);
}

int strToEscapedUri(char *dest, const char *src, int destSize, int isWide, int *charactersConverted)
{
	return _strToUtf8(dest, src, destSize, isWide, charactersConverted, 1);
}

char *MakeStrcpy(const char *string)
{
	char * ret;
	if(string == NULL)
	{
		ret = malloc(1);
		ret[0] = '\0';
	}
	else
	{
		ret = malloc(strlen(string)+1);
		strcpy(ret, string);
	}
	return ret;
}


void StrChomp(char *s)
{
	while(*s != '\n' && *s != '\r' && *s != '\0') s++;
	*s = '\0';
}

