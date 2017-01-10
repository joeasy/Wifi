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
* $Workfile: DMR_MicroStack.c
* $Revision: #1.0.2718.23851
* $Author:   Intel Corporation, Intel Device Builder
* $Date:     2007å¹????
*
*
*
*/


#if defined(WIN32) || defined(_WIN32_WCE)
#	ifndef MICROSTACK_NO_STDAFX
#		include "stdafx.h"
#	endif
char* DMR_PLATFORM = "WINDOWS";
#elif defined(__SYMBIAN32__)
char* DMR_PLATFORM = "SYMBIAN";
#else
char* DMR_PLATFORM = "REALTEK";
#endif

#if defined(WIN32)
#define _CRTDBG_MAP_ALLOC
#endif

#if defined(WINSOCK2)
#	include <winsock2.h>
#	include <ws2tcpip.h>
#elif defined(WINSOCK1)
#	include <winsock.h>
#	include <wininet.h>
#endif

#include <time.h>
#include "ILibParsers.h"
#include "DMR_MicroStack.h"
#include "ILibWebServer.h"
#include "ILibWebClient.h"
#include "ILibAsyncSocket.h"
#include "ILibAsyncUDPSocket.h"

#if defined(WIN32) && !defined(_WIN32_WCE)
#include <crtdbg.h>
#endif


#define UPNP_SSDP_TTL 4
#define UPNP_HTTP_MAXSOCKETS 5
#define UPNP_MAX_SSDP_HEADER_SIZE 4096
#define UPNP_PORT 1900

#define UPNP_GROUP "239.255.255.250"
#define DMR__MAX_SUBSCRIPTION_TIMEOUT 300
#define DMR_MIN(a,b) (((a)<(b))?(a):(b))

#define LVL3DEBUG(x)

struct DMR__StateVariableTable_AVTransport DMR__StateVariableTable_AVTransport_Impl =
{
   {
      (char)0x36,(char)0x3C,(char)0x73,(char)0x74,(char)0x61,(char)0x74,(char)0x65,(char)0x56,(char)0x61,(char)0x72
      ,(char)0x69,(char)0x61,(char)0x62,(char)0x6C,(char)0x65,(char)0x20,(char)0x73,(char)0x65,(char)0x6E,(char)0x64
      ,(char)0x45,(char)0x76,(char)0x65,(char)0x6E,(char)0x74,(char)0x73,(char)0x3D,(char)0x22,(char)0x6E,(char)0x6F
      ,(char)0x22,(char)0x3E,(char)0x3C,(char)0x6E,(char)0x61,(char)0x6D,(char)0x65,(char)0x3E,(char)0x43,(char)0x75
      ,(char)0x72,(char)0x72,(char)0x65,(char)0x6E,(char)0x74,(char)0x50,(char)0x6C,(char)0x61,(char)0x79,(char)0x4D
      ,(char)0x6F,(char)0x64,(char)0x65,(char)0x3C,(char)0x2F,(char)0x85,(char)0x05,(char)0x12,(char)0x3C,(char)0x64
      ,(char)0x61,(char)0x74,(char)0x61,(char)0x54,(char)0x79,(char)0x70,(char)0x65,(char)0x3E,(char)0x73,(char)0x74
      ,(char)0x72,(char)0x69,(char)0x6E,(char)0x67,(char)0x3C,(char)0x2F,(char)0x49,(char)0x04,(char)0x11,(char)0x3C
      ,(char)0x61,(char)0x6C,(char)0x6C,(char)0x6F,(char)0x77,(char)0x65,(char)0x64,(char)0x56,(char)0x61,(char)0x6C
      ,(char)0x75,(char)0x65,(char)0x4C,(char)0x69,(char)0x73,(char)0x74,(char)0x8E,(char)0x04,(char)0x09,(char)0x3E
      ,(char)0x4E,(char)0x4F,(char)0x52,(char)0x4D,(char)0x41,(char)0x4C,(char)0x3C,(char)0x2F,(char)0x4D,(char)0x05
      ,(char)0x00,(char)0xCE,(char)0x08,(char)0x07,(char)0x53,(char)0x48,(char)0x55,(char)0x46,(char)0x46,(char)0x4C
      ,(char)0x45,(char)0x1D,(char)0x09,(char)0x09,(char)0x52,(char)0x45,(char)0x50,(char)0x45,(char)0x41,(char)0x54
      ,(char)0x5F,(char)0x4F,(char)0x4E,(char)0xE5,(char)0x09,(char)0x02,(char)0x41,(char)0x4C,(char)0x9E,(char)0x1C
      ,(char)0x06,(char)0x52,(char)0x41,(char)0x4E,(char)0x44,(char)0x4F,(char)0x4D,(char)0x5D,(char)0x25,(char)0x08
      ,(char)0x44,(char)0x49,(char)0x52,(char)0x45,(char)0x43,(char)0x54,(char)0x5F,(char)0x31,(char)0x9D,(char)0x2E
      ,(char)0x05,(char)0x49,(char)0x4E,(char)0x54,(char)0x52,(char)0x4F,(char)0x10,(char)0x37,(char)0x01,(char)0x2F
      ,(char)0x92,(char)0x44,(char)0x07,(char)0x64,(char)0x65,(char)0x66,(char)0x61,(char)0x75,(char)0x6C,(char)0x74
      ,(char)0x8E,(char)0x44,(char)0x00,(char)0x4D,(char)0x05,(char)0x02,(char)0x3C,(char)0x2F,(char)0x8D,(char)0x67
      ,(char)0x01,(char)0x3E,(char)0x65,(char)0x6B,(char)0x13,(char)0x52,(char)0x65,(char)0x63,(char)0x6F,(char)0x72
      ,(char)0x64,(char)0x53,(char)0x74,(char)0x6F,(char)0x72,(char)0x61,(char)0x67,(char)0x65,(char)0x4D,(char)0x65
      ,(char)0x64,(char)0x69,(char)0x75,(char)0x6D,(char)0x7F,(char)0x6C,(char)0x00,(char)0x45,(char)0x6C,(char)0x0D
      ,(char)0x54,(char)0x5F,(char)0x49,(char)0x4D,(char)0x50,(char)0x4C,(char)0x45,(char)0x4D,(char)0x45,(char)0x4E
      ,(char)0x54,(char)0x45,(char)0x44,(char)0xB2,(char)0x37,(char)0x00,(char)0xCF,(char)0x0F,(char)0x00,(char)0xF8
      ,(char)0x39,(char)0x03,(char)0x79,(char)0x65,(char)0x73,(char)0x48,(char)0xA5,(char)0x09,(char)0x4C,(char)0x61
      ,(char)0x73,(char)0x74,(char)0x43,(char)0x68,(char)0x61,(char)0x6E,(char)0x67,(char)0x24,(char)0xA4,(char)0x00
      ,(char)0x76,(char)0x52,(char)0x12,(char)0x6C,(char)0x61,(char)0x74,(char)0x69,(char)0x76,(char)0x65,(char)0x54
      ,(char)0x69,(char)0x6D,(char)0x65,(char)0x50,(char)0x6F,(char)0x73,(char)0x69,(char)0x74,(char)0x69,(char)0x6F
      ,(char)0x6E,(char)0xE3,(char)0xBE,(char)0x00,(char)0xCD,(char)0x75,(char)0x0A,(char)0x32,(char)0x31,(char)0x34
      ,(char)0x37,(char)0x34,(char)0x38,(char)0x33,(char)0x36,(char)0x34,(char)0x37,(char)0xD0,(char)0x76,(char)0x00
      ,(char)0x8C,(char)0xC8,(char)0x01,(char)0x52,(char)0x84,(char)0x31,(char)0x09,(char)0x3E,(char)0x3C,(char)0x6D
      ,(char)0x69,(char)0x6E,(char)0x69,(char)0x6D,(char)0x75,(char)0x6D,(char)0x4D,(char)0x0D,(char)0x00,(char)0x08
      ,(char)0x05,(char)0x04,(char)0x3C,(char)0x6D,(char)0x61,(char)0x78,(char)0x52,(char)0x07,(char)0x00,(char)0x07
      ,(char)0x05,(char)0x02,(char)0x3C,(char)0x2F,(char)0x93,(char)0x13,(char)0x00,(char)0x34,(char)0x8F,(char)0x00
      ,(char)0x47,(char)0xFA,(char)0x08,(char)0x54,(char)0x72,(char)0x61,(char)0x63,(char)0x6B,(char)0x55,(char)0x52
      ,(char)0x49,(char)0x7F,(char)0x56,(char)0x00,(char)0xA4,(char)0x19,(char)0x04,(char)0x44,(char)0x75,(char)0x72
      ,(char)0x61,(char)0x67,(char)0x56,(char)0x00,(char)0x7B,(char)0x34,(char)0x00,(char)0x06,(char)0xC5,(char)0x0B
      ,(char)0x51,(char)0x75,(char)0x61,(char)0x6C,(char)0x69,(char)0x74,(char)0x79,(char)0x4D,(char)0x6F,(char)0x64
      ,(char)0x65,(char)0xBF,(char)0xC4,(char)0x00,(char)0xBF,(char)0xC4,(char)0x00,(char)0xBF,(char)0xC4,(char)0x00
      ,(char)0x5E,(char)0x6F,(char)0x00,(char)0xC4,(char)0xFC,(char)0x01,(char)0x61,(char)0xFF,(char)0x55,(char)0x00
      ,(char)0xE0,(char)0xC6,(char)0x0F,(char)0x41,(char)0x62,(char)0x73,(char)0x6F,(char)0x6C,(char)0x75,(char)0x74
      ,(char)0x65,(char)0x43,(char)0x6F,(char)0x75,(char)0x6E,(char)0x74,(char)0x65,(char)0x72,(char)0x99,(char)0xC7
      ,(char)0x02,(char)0x69,(char)0x34,(char)0x7F,(char)0xE1,(char)0x00,(char)0x49,(char)0xE1,(char)0x00,(char)0xAE
      ,(char)0x1A,(char)0x00,(char)0x3F,(char)0xE1,(char)0x00,(char)0x3F,(char)0xE1,(char)0x00,(char)0x3E,(char)0xE1
      ,(char)0x15,(char)0x41,(char)0x5F,(char)0x41,(char)0x52,(char)0x47,(char)0x5F,(char)0x54,(char)0x59,(char)0x50
      ,(char)0x45,(char)0x5F,(char)0x49,(char)0x6E,(char)0x73,(char)0x74,(char)0x61,(char)0x6E,(char)0x63,(char)0x65
      ,(char)0x49,(char)0x44,(char)0x91,(char)0xE2,(char)0x01,(char)0x75,(char)0x5B,(char)0x3C,(char)0x01,(char)0x30
      ,(char)0xBF,(char)0x93,(char)0x00,(char)0xC6,(char)0x78,(char)0x0D,(char)0x56,(char)0x54,(char)0x72,(char)0x61
      ,(char)0x6E,(char)0x73,(char)0x70,(char)0x6F,(char)0x72,(char)0x74,(char)0x55,(char)0x52,(char)0x49,(char)0xFF
      ,(char)0xE7,(char)0x00,(char)0xD8,(char)0xE7,(char)0x00,(char)0xC9,(char)0x18,(char)0x04,(char)0x53,(char)0x74
      ,(char)0x61,(char)0x74,(char)0x7F,(char)0xE5,(char)0x00,(char)0x44,(char)0xE5,(char)0x05,(char)0x53,(char)0x54
      ,(char)0x4F,(char)0x50,(char)0x50,(char)0x52,(char)0xE3,(char)0x00,(char)0x4D,(char)0xEE,(char)0x07,(char)0x50
      ,(char)0x4C,(char)0x41,(char)0x59,(char)0x49,(char)0x4E,(char)0x47,(char)0x1D,(char)0x09,(char)0x0A,(char)0x54
      ,(char)0x52,(char)0x41,(char)0x4E,(char)0x53,(char)0x49,(char)0x54,(char)0x49,(char)0x4F,(char)0x4E,(char)0xA0
      ,(char)0x0A,(char)0x07,(char)0x50,(char)0x41,(char)0x55,(char)0x53,(char)0x45,(char)0x44,(char)0x5F,(char)0x44
      ,(char)0x15,(char)0x04,(char)0x42,(char)0x41,(char)0x43,(char)0x4B,(char)0x9D,(char)0x1E,(char)0x10,(char)0x4E
      ,(char)0x4F,(char)0x5F,(char)0x4D,(char)0x45,(char)0x44,(char)0x49,(char)0x41,(char)0x5F,(char)0x50,(char)0x52
      ,(char)0x45,(char)0x53,(char)0x45,(char)0x4E,(char)0x54,(char)0xD0,(char)0x29,(char)0x01,(char)0x2F,(char)0x92
      ,(char)0x37,(char)0x00,(char)0x4D,(char)0xB2,(char)0x00,(char)0x12,(char)0x10,(char)0x00,(char)0xFF,(char)0x79
      ,(char)0x17,(char)0x6D,(char)0x65,(char)0x3E,(char)0x43,(char)0x75,(char)0x72,(char)0x72,(char)0x65,(char)0x6E
      ,(char)0x74,(char)0x54,(char)0x72,(char)0x61,(char)0x63,(char)0x6B,(char)0x4D,(char)0x65,(char)0x74,(char)0x61
      ,(char)0x44,(char)0x61,(char)0x74,(char)0x61,(char)0x7F,(char)0x7B,(char)0x00,(char)0xD8,(char)0xF2,(char)0x04
      ,(char)0x4E,(char)0x65,(char)0x78,(char)0x74,(char)0xBF,(char)0x95,(char)0x00,(char)0xA6,(char)0xD0,(char)0x1A
      ,(char)0x50,(char)0x6F,(char)0x73,(char)0x73,(char)0x69,(char)0x62,(char)0x6C,(char)0x65,(char)0x52,(char)0x65
      ,(char)0x63,(char)0x6F,(char)0x72,(char)0x64,(char)0x51,(char)0x75,(char)0x61,(char)0x6C,(char)0x69,(char)0x74
      ,(char)0x79,(char)0x4D,(char)0x6F,(char)0x64,(char)0x65,(char)0x73,(char)0xBF,(char)0x98,(char)0x00,(char)0x05
      ,(char)0x71,(char)0x0D,(char)0x54,(char)0x5F,(char)0x49,(char)0x4D,(char)0x50,(char)0x4C,(char)0x45,(char)0x4D
      ,(char)0x45,(char)0x4E,(char)0x54,(char)0x45,(char)0x44,(char)0xF2,(char)0x70,(char)0x00,(char)0xCF,(char)0x0F
      ,(char)0x00,(char)0xBF,(char)0x70,(char)0x00,(char)0x8F,(char)0x70,(char)0x00,(char)0xD1,(char)0xE9,(char)0x03
      ,(char)0x75,(char)0x69,(char)0x34,(char)0x0C,(char)0xE9,(char)0x00,(char)0xCD,(char)0x93,(char)0x01,(char)0x30
      ,(char)0x3F,(char)0x90,(char)0x00,(char)0x85,(char)0xF0,(char)0x14,(char)0x41,(char)0x62,(char)0x73,(char)0x6F
      ,(char)0x6C,(char)0x75,(char)0x74,(char)0x65,(char)0x54,(char)0x69,(char)0x6D,(char)0x65,(char)0x50,(char)0x6F
      ,(char)0x73,(char)0x69,(char)0x74,(char)0x69,(char)0x6F,(char)0x6E,(char)0x3F,(char)0x90,(char)0x00,(char)0x2A
      ,(char)0x90,(char)0x00,(char)0x7F,(char)0xAC,(char)0x00,(char)0x21,(char)0x92,(char)0x14,(char)0x6C,(char)0x61
      ,(char)0x79,(char)0x62,(char)0x61,(char)0x63,(char)0x6B,(char)0x53,(char)0x74,(char)0x6F,(char)0x72,(char)0x61
      ,(char)0x67,(char)0x65,(char)0x4D,(char)0x65,(char)0x64,(char)0x69,(char)0x75,(char)0x6D,(char)0xFF,(char)0x90
      ,(char)0x00,(char)0xFF,(char)0x90,(char)0x00,(char)0xFF,(char)0x90,(char)0x00,(char)0xE1,(char)0x90,(char)0x00
      ,(char)0xC6,(char)0xE6,(char)0x02,(char)0x41,(char)0x63,(char)0xC4,(char)0x71,(char)0x01,(char)0x73,(char)0xFF
      ,(char)0xE7,(char)0x00,(char)0xD8,(char)0xE7,(char)0x00,(char)0xC6,(char)0xE5,(char)0x00,(char)0x86,(char)0x53
      ,(char)0x0A,(char)0x57,(char)0x72,(char)0x69,(char)0x74,(char)0x65,(char)0x53,(char)0x74,(char)0x61,(char)0x74
      ,(char)0x75,(char)0x3F,(char)0xE7,(char)0x00,(char)0x3F,(char)0xE7,(char)0x00,(char)0x3F,(char)0xE7,(char)0x00
      ,(char)0x99,(char)0x90,(char)0x07,(char)0x6F,(char)0x73,(char)0x73,(char)0x69,(char)0x62,(char)0x6C,(char)0x65
      ,(char)0x93,(char)0x92,(char)0x01,(char)0x61,(char)0x7F,(char)0x92,(char)0x00,(char)0x7F,(char)0x92,(char)0x00
      ,(char)0x7F,(char)0x92,(char)0x00,(char)0xD7,(char)0xE8,(char)0x00,(char)0xFF,(char)0xE7,(char)0x00,(char)0xEE
      ,(char)0xE7,(char)0x0D,(char)0x4E,(char)0x75,(char)0x6D,(char)0x62,(char)0x65,(char)0x72,(char)0x4F,(char)0x66
      ,(char)0x54,(char)0x72,(char)0x61,(char)0x63,(char)0x6B,(char)0x52,(char)0xAB,(char)0x03,(char)0x75,(char)0x69
      ,(char)0x34,(char)0x4C,(char)0xE5,(char)0x00,(char)0x0D,(char)0xD1,(char)0x01,(char)0x30,(char)0x7F,(char)0x3B
      ,(char)0x00,(char)0x46,(char)0x3B,(char)0x12,(char)0x5F,(char)0x41,(char)0x52,(char)0x47,(char)0x5F,(char)0x54
      ,(char)0x59,(char)0x50,(char)0x45,(char)0x5F,(char)0x53,(char)0x65,(char)0x65,(char)0x6B,(char)0x4D,(char)0x6F
      ,(char)0x64,(char)0x65,(char)0x3F,(char)0xB1,(char)0x0B,(char)0x75,(char)0x65,(char)0x3E,(char)0x54,(char)0x52
      ,(char)0x41,(char)0x43,(char)0x4B,(char)0x5F,(char)0x4E,(char)0x52,(char)0x63,(char)0xAF,(char)0x00,(char)0x3F
      ,(char)0x2D,(char)0x00,(char)0x04,(char)0x2D,(char)0x06,(char)0x54,(char)0x61,(char)0x72,(char)0x67,(char)0x65
      ,(char)0x74,(char)0x3F,(char)0xFA,(char)0x00,(char)0x60,(char)0xBF,(char)0x00,(char)0x06,(char)0xFC,(char)0x00
      ,(char)0xFF,(char)0xBE,(char)0x00,(char)0xFF,(char)0xFA,(char)0x00,(char)0xFF,(char)0xFA,(char)0x00,(char)0xE3
      ,(char)0xFA,(char)0x00,(char)0x49,(char)0xBE,(char)0x06,(char)0x53,(char)0x74,(char)0x61,(char)0x74,(char)0x75
      ,(char)0x73,(char)0xBF,(char)0xF7,(char)0x05,(char)0x75,(char)0x65,(char)0x3E,(char)0x4F,(char)0x4B,(char)0x50
      ,(char)0xF4,(char)0x00,(char)0x4D,(char)0xFF,(char)0x0C,(char)0x45,(char)0x52,(char)0x52,(char)0x4F,(char)0x52
      ,(char)0x20,(char)0x4F,(char)0x43,(char)0x43,(char)0x55,(char)0x52,(char)0x52,(char)0x32,(char)0xFF,(char)0x02
      ,(char)0x4F,(char)0x4B,(char)0x3F,(char)0x3D,(char)0x00,(char)0x0E,(char)0x3D,(char)0x09,(char)0x50,(char)0x6C
      ,(char)0x61,(char)0x79,(char)0x53,(char)0x70,(char)0x65,(char)0x65,(char)0x64,(char)0x7F,(char)0xC0,(char)0x04
      ,(char)0x75,(char)0x65,(char)0x3E,(char)0x31,(char)0x30,(char)0x73,(char)0x01,(char)0x31,(char)0x1F,(char)0xF3

   },
   1010,
   5346
};
struct DMR__StateVariable_AVTransport_CurrentPlayMode DMR__StateVariable_AVTransport_CurrentPlayMode_Impl =
{
   0,
   86,
   86,
   18,
   359,
   19,
   //{"NORMAL","SHUFFLE","REPEAT_ONE","REPEAT_ALL","RANDOM","DIRECT_1","INTRO",NULL},
   {"NORMAL",NULL},
   378,
   14,
   398,
   15,
   "NORMAL",
   413,
   16
};
struct DMR__StateVariable_AVTransport_RecordStorageMedium DMR__StateVariable_AVTransport_RecordStorageMedium_Impl =
{
   429,
   90,
   519,
   18,
   581,
   19,
   {"NOT_IMPLEMENTED","NOT_IMPLEMENTED","NOT_IMPLEMENTED",NULL},
   600,
   14,
   629,
   15,
   "NOT_IMPLEMENTED",
   644,
   16
};
struct DMR__StateVariable_AVTransport_LastChange DMR__StateVariable_AVTransport_LastChange_Impl =
{
   660,
   82,
   742,
   16
};
struct DMR__StateVariable_AVTransport_RelativeTimePosition DMR__StateVariable_AVTransport_RelativeTimePosition_Impl =
{
   758,
   91,
   888,
   19,
   965,
   20,
   {"2147483647","2147483647",NULL},
   849,
   14,
   873,
   15,
   "2147483647",
   985,
   16
};
struct DMR__StateVariable_AVTransport_CurrentTrackURI DMR__StateVariable_AVTransport_CurrentTrackURI_Impl =
{
   1001,
   86,
   1087,
   16
};
struct DMR__StateVariable_AVTransport_CurrentTrackDuration DMR__StateVariable_AVTransport_CurrentTrackDuration_Impl =
{
   1103,
   91,
   1194,
   16
};
struct DMR__StateVariable_AVTransport_CurrentRecordQualityMode DMR__StateVariable_AVTransport_CurrentRecordQualityMode_Impl =
{
   1210,
   95,
   1305,
   18,
   1367,
   19,
   {"NOT_IMPLEMENTED",NULL},
   1386,
   14,
   1415,
   15,
   "NOT_IMPLEMENTED",
   1430,
   16
};
struct DMR__StateVariable_AVTransport_CurrentMediaDuration DMR__StateVariable_AVTransport_CurrentMediaDuration_Impl =
{
   1446,
   91,
   1537,
   16
};
struct DMR__StateVariable_AVTransport_AbsoluteCounterPosition DMR__StateVariable_AVTransport_AbsoluteCounterPosition_Impl =
{
   1553,
   90,
   1643,
   16
};
struct DMR__StateVariable_AVTransport_RelativeCounterPosition DMR__StateVariable_AVTransport_RelativeCounterPosition_Impl =
{
   1659,
   90,
   1788,
   19,
   1865,
   20,
   {"2147483647","2147483647",NULL},
   1749,
   14,
   1773,
   15,
   NULL,//"2147483647",
   1885,
   16
};
struct DMR__StateVariable_AVTransport_A_ARG_TYPE_InstanceID DMR__StateVariable_AVTransport_A_ARG_TYPE_InstanceID_Impl =
{
   1901,
   89,
   1990,
   14,
   2005,
   15,
   "0",
   2020,
   16
};
struct DMR__StateVariable_AVTransport_AVTransportURI DMR__StateVariable_AVTransport_AVTransportURI_Impl =
{
   2036,
   85,
   2121,
   16
};
struct DMR__StateVariable_AVTransport_TransportState DMR__StateVariable_AVTransport_TransportState_Impl =
{
   2137,
   85,
   2222,
   18,
   2443,
   19,
   {"STOPPED","PLAYING","TRANSITIONING","PAUSED_PLAYBACK","NO_MEDIA_PRESENT",NULL},
   2462,
   14,
   2492,
   15,
   "NO_MEDIA_PRESENT",
   2507,
   16
};
struct DMR__StateVariable_AVTransport_CurrentTrackMetaData DMR__StateVariable_AVTransport_CurrentTrackMetaData_Impl =
{
   2523,
   91,
   2614,
   16
};
struct DMR__StateVariable_AVTransport_NextAVTransportURI DMR__StateVariable_AVTransport_NextAVTransportURI_Impl =
{
   2630,
   89,
   2719,
   16
};
struct DMR__StateVariable_AVTransport_PossibleRecordQualityModes DMR__StateVariable_AVTransport_PossibleRecordQualityModes_Impl =
{
   2735,
   97,
   2832,
   18,
   2894,
   19,
   {"NOT_IMPLEMENTED",NULL},
   2913,
   14,
   2942,
   15,
   "NOT_IMPLEMENTED",
   2957,
   16
};
struct DMR__StateVariable_AVTransport_CurrentTrack DMR__StateVariable_AVTransport_CurrentTrack_Impl =
{
   2973,
   80,
   3053,
   14,
   3068,
   15,
   "0",
   {"0", "1024", "1"}, /* Added by yuyu for DLNA 1.5 CTT 7.3.6.1 */
   3083,
   16,
};
struct DMR__StateVariable_AVTransport_AbsoluteTimePosition DMR__StateVariable_AVTransport_AbsoluteTimePosition_Impl =
{
   3099,
   91,
   3190,
   16
};
struct DMR__StateVariable_AVTransport_NextAVTransportURIMetaData DMR__StateVariable_AVTransport_NextAVTransportURIMetaData_Impl =
{
   3206,
   97,
   3303,
   16
};
struct DMR__StateVariable_AVTransport_PlaybackStorageMedium DMR__StateVariable_AVTransport_PlaybackStorageMedium_Impl =
{
   3319,
   92,
   3411,
   18,
   3473,
   19,
   {"NOT_IMPLEMENTED",NULL},
   3492,
   14,
   3521,
   15,
   "NOT_IMPLEMENTED",
   3536,
   16
};
struct DMR__StateVariable_AVTransport_CurrentTransportActions DMR__StateVariable_AVTransport_CurrentTransportActions_Impl =
{
   3552,
   94,
   3646,
   16
};
struct DMR__StateVariable_AVTransport_RecordMediumWriteStatus DMR__StateVariable_AVTransport_RecordMediumWriteStatus_Impl =
{
   3662,
   94,
   3756,
   18,
   3818,
   19,
   {"NOT_IMPLEMENTED",NULL},
   3837,
   14,
   3866,
   15,
   "NOT_IMPLEMENTED",
   3881,
   16
};
struct DMR__StateVariable_AVTransport_PossiblePlaybackStorageMedia DMR__StateVariable_AVTransport_PossiblePlaybackStorageMedia_Impl =
{
   3897,
   99,
   3996,
   18,
   4058,
   19,
   {"NOT_IMPLEMENTED","NONE","NETWORK",NULL},
   4077,
   14,
   4106,
   15,
   "NOT_IMPLEMENTED",
   4121,
   16
};
struct DMR__StateVariable_AVTransport_AVTransportURIMetaData DMR__StateVariable_AVTransport_AVTransportURIMetaData_Impl =
{
   4137,
   93,
   4230,
   16
};
struct DMR__StateVariable_AVTransport_NumberOfTracks DMR__StateVariable_AVTransport_NumberOfTracks_Impl =
{
   4246,
   82,
   4328,
   14,
   4343,
   15,
   NULL,//"0",
   {"0", "1024"}, /* Added by yuyu for DLNA 1.5 CTT 7.3.6.1 */
   4358,
   16
};
struct DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekMode DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekMode_Impl =
{
   4374,
   90,
   4464,
   18,
   4519,
   19,
   // vvv Modified by yuyu for DLNA 1.5 CTT 7.3.Y.8
   //{"TRACK_NR",NULL},
   //{"TRACK_NR", "REL_TIME", "ABS_TIME", "X_DLNA_REL_BYTE", NULL},
   {"TRACK_NR", "REL_TIME", "ABS_TIME", NULL},
   //{"TRACK_NR", "X_DLNA_REL_BYTE", NULL},
   // ^^^ Modified by yuyu for DLNA 1.5 CTT 7.3.Y.8
   4538,
   16
};
struct DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekTarget DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekTarget_Impl =
{
   4554,
   92,
   4646,
   16
};
struct DMR__StateVariable_AVTransport_PossibleRecordStorageMedia DMR__StateVariable_AVTransport_PossibleRecordStorageMedia_Impl =
{
   4662,
   97,
   4759,
   18,
   4821,
   19,
   {"NOT_IMPLEMENTED",NULL},
   4840,
   14,
   4869,
   15,
   "NOT_IMPLEMENTED",
   4884,
   16
};
struct DMR__StateVariable_AVTransport_TransportStatus DMR__StateVariable_AVTransport_TransportStatus_Impl =
{
   4900,
   86,
   4986,
   18,
   5078,
   19,
   {"OK","ERROR_OCCURRED",NULL},
   5097,
   14,
   5113,
   15,
   "OK",
   5128,
   16
};
struct DMR__StateVariable_AVTransport_TransportPlaySpeed DMR__StateVariable_AVTransport_TransportPlaySpeed_Impl =
{
   5144,
   89,
   5233,
   18,
   5281,
   19,
   {"-32","-16","-8","-2","-1/4","-1/16","1/16","1/4","1","8","16","32",NULL},
   5300,
   14,
   5315,
   15,
   "1",
   5330,
   16
};
struct DMR__StateVariableTable_ConnectionManager DMR__StateVariableTable_ConnectionManager_Impl =
{
   {
      (char)0x3E,(char)0x3C,(char)0x73,(char)0x74,(char)0x61,(char)0x74,(char)0x65,(char)0x56,(char)0x61,(char)0x72
      ,(char)0x69,(char)0x61,(char)0x62,(char)0x6C,(char)0x65,(char)0x20,(char)0x73,(char)0x65,(char)0x6E,(char)0x64
      ,(char)0x45,(char)0x76,(char)0x65,(char)0x6E,(char)0x74,(char)0x73,(char)0x3D,(char)0x22,(char)0x6E,(char)0x6F
      ,(char)0x22,(char)0x3E,(char)0x3C,(char)0x6E,(char)0x61,(char)0x6D,(char)0x65,(char)0x3E,(char)0x41,(char)0x5F
      ,(char)0x41,(char)0x52,(char)0x47,(char)0x5F,(char)0x54,(char)0x59,(char)0x50,(char)0x45,(char)0x5F,(char)0x50
      ,(char)0x72,(char)0x6F,(char)0x74,(char)0x6F,(char)0x63,(char)0x6F,(char)0x6C,(char)0x49,(char)0x6E,(char)0x66
      ,(char)0x6F,(char)0x3C,(char)0x2F,(char)0x85,(char)0x07,(char)0x12,(char)0x3C,(char)0x64,(char)0x61,(char)0x74
      ,(char)0x61,(char)0x54,(char)0x79,(char)0x70,(char)0x65,(char)0x3E,(char)0x73,(char)0x74,(char)0x72,(char)0x69
      ,(char)0x6E,(char)0x67,(char)0x3C,(char)0x2F,(char)0x49,(char)0x04,(char)0x02,(char)0x3C,(char)0x2F,(char)0xCD
      ,(char)0x17,(char)0x01,(char)0x3E,(char)0xB0,(char)0x1B,(char)0x10,(char)0x43,(char)0x6F,(char)0x6E,(char)0x6E
      ,(char)0x65,(char)0x63,(char)0x74,(char)0x69,(char)0x6F,(char)0x6E,(char)0x53,(char)0x74,(char)0x61,(char)0x74
      ,(char)0x75,(char)0x73,(char)0xA3,(char)0x1C,(char)0x10,(char)0x61,(char)0x6C,(char)0x6C,(char)0x6F,(char)0x77
      ,(char)0x65,(char)0x64,(char)0x56,(char)0x61,(char)0x6C,(char)0x75,(char)0x65,(char)0x4C,(char)0x69,(char)0x73
      ,(char)0x74,(char)0x8E,(char)0x04,(char)0x05,(char)0x3E,(char)0x4F,(char)0x4B,(char)0x3C,(char)0x2F,(char)0x4D
      ,(char)0x04,(char)0x00,(char)0xCE,(char)0x07,(char)0x15,(char)0x43,(char)0x6F,(char)0x6E,(char)0x74,(char)0x65
      ,(char)0x6E,(char)0x74,(char)0x46,(char)0x6F,(char)0x72,(char)0x6D,(char)0x61,(char)0x74,(char)0x4D,(char)0x69
      ,(char)0x73,(char)0x6D,(char)0x61,(char)0x74,(char)0x63,(char)0x68,(char)0x9D,(char)0x0C,(char)0x14,(char)0x49
      ,(char)0x6E,(char)0x73,(char)0x75,(char)0x66,(char)0x66,(char)0x69,(char)0x63,(char)0x69,(char)0x65,(char)0x6E
      ,(char)0x74,(char)0x42,(char)0x61,(char)0x6E,(char)0x64,(char)0x77,(char)0x69,(char)0x64,(char)0x74,(char)0x9E
      ,(char)0x0C,(char)0x05,(char)0x55,(char)0x6E,(char)0x72,(char)0x65,(char)0x6C,(char)0xC5,(char)0x5B,(char)0x07
      ,(char)0x43,(char)0x68,(char)0x61,(char)0x6E,(char)0x6E,(char)0x65,(char)0x6C,(char)0x9F,(char)0x0B,(char)0x05
      ,(char)0x6B,(char)0x6E,(char)0x6F,(char)0x77,(char)0x6E,(char)0x90,(char)0x2D,(char)0x01,(char)0x2F,(char)0x12
      ,(char)0x3A,(char)0x00,(char)0x3F,(char)0x5B,(char)0x0D,(char)0x41,(char)0x56,(char)0x54,(char)0x72,(char)0x61
      ,(char)0x6E,(char)0x73,(char)0x70,(char)0x6F,(char)0x72,(char)0x74,(char)0x49,(char)0x44,(char)0xD1,(char)0x76
      ,(char)0x02,(char)0x69,(char)0x34,(char)0xCC,(char)0x75,(char)0x07,(char)0x64,(char)0x65,(char)0x66,(char)0x61
      ,(char)0x75,(char)0x6C,(char)0x74,(char)0xC6,(char)0x54,(char)0x04,(char)0x2D,(char)0x31,(char)0x3C,(char)0x2F
      ,(char)0x4D,(char)0x04,(char)0x00,(char)0xBF,(char)0x7D,(char)0x04,(char)0x5F,(char)0x52,(char)0x63,(char)0x73
      ,(char)0xBF,(char)0x20,(char)0x00,(char)0x3F,(char)0x9E,(char)0x00,(char)0x0B,(char)0x9E,(char)0x00,(char)0xEE
      ,(char)0x42,(char)0x01,(char)0x30,(char)0x3F,(char)0x22,(char)0x00,(char)0x1A,(char)0xC0,(char)0x07,(char)0x4D
      ,(char)0x61,(char)0x6E,(char)0x61,(char)0x67,(char)0x65,(char)0x72,(char)0xFF,(char)0xDC,(char)0x00,(char)0x4E
      ,(char)0xF8,(char)0x03,(char)0x79,(char)0x65,(char)0x73,(char)0x88,(char)0xF8,(char)0x06,(char)0x53,(char)0x6F
      ,(char)0x75,(char)0x72,(char)0x63,(char)0x65,(char)0x7F,(char)0xF7,(char)0x00,(char)0xA6,(char)0x1A,(char)0x03
      ,(char)0x69,(char)0x6E,(char)0x6B,(char)0x3F,(char)0x1A,(char)0x00,(char)0x6F,(char)0xB6,(char)0x03,(char)0x44
      ,(char)0x69,(char)0x72,(char)0x06,(char)0x73,(char)0x00,(char)0x63,(char)0x4F,(char)0x00,(char)0x92,(char)0xD5
      ,(char)0x00,(char)0xCD,(char)0xFE,(char)0x06,(char)0x4F,(char)0x75,(char)0x74,(char)0x70,(char)0x75,(char)0x74
      ,(char)0x9D,(char)0xF7,(char)0x05,(char)0x49,(char)0x6E,(char)0x70,(char)0x75,(char)0x74,(char)0xBF,(char)0xEB
      ,(char)0x00,(char)0xD9,(char)0x69,(char)0x07,(char)0x43,(char)0x75,(char)0x72,(char)0x72,(char)0x65,(char)0x6E
      ,(char)0x74,(char)0xCC,(char)0xA7,(char)0x01,(char)0x73,(char)0xF2,(char)0x84
   },
   377,
   1524
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ProtocolInfo DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ProtocolInfo_Impl =
{
   0,
   94,
   94,
   16
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionStatus DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionStatus_Impl =
{
   110,
   98,
   208,
   18,
   439,
   19,
   {"OK","ContentFormatMismatch","InsufficientBandwidth","UnreliableChannel","Unknown",NULL},
   458,
   16
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_AVTransportID DMR__StateVariable_ConnectionManager_A_ARG_TYPE_AVTransportID_Impl =
{
   474,
   91,
   565,
   14,
   581,
   15,
   "-1",
   596,
   16
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_RcsID DMR__StateVariable_ConnectionManager_A_ARG_TYPE_RcsID_Impl =
{
   612,
   83,
   695,
   14,
   711,
   15,
   "-1",
   726,
   16
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionID DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionID_Impl =
{
   742,
   90,
   832,
   14,
   847,
   15,
   "0",
   862,
   16
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionManager DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionManager_Impl =
{
   878,
   99,
   977,
   16
};
struct DMR__StateVariable_ConnectionManager_SourceProtocolInfo DMR__StateVariable_ConnectionManager_SourceProtocolInfo_Impl =
{
   993,
   90,
   1083,
   16
};
struct DMR__StateVariable_ConnectionManager_SinkProtocolInfo DMR__StateVariable_ConnectionManager_SinkProtocolInfo_Impl =
{
   1099,
   88,
   1187,
   16
};
struct DMR__StateVariable_ConnectionManager_A_ARG_TYPE_Direction DMR__StateVariable_ConnectionManager_A_ARG_TYPE_Direction_Impl =
{
   1203,
   91,
   1294,
   18,
   1381,
   19,
   {"Output","Input",NULL},
   1400,
   16
};
struct DMR__StateVariable_ConnectionManager_CurrentConnectionIDs DMR__StateVariable_ConnectionManager_CurrentConnectionIDs_Impl =
{
   1416,
   92,
   1508,
   16
};
struct DMR__StateVariableTable_RenderingControl DMR__StateVariableTable_RenderingControl_Impl =
{
   {
      (char)0x2F,(char)0x3C,(char)0x73,(char)0x74,(char)0x61,(char)0x74,(char)0x65,(char)0x56,(char)0x61,(char)0x72
      ,(char)0x69,(char)0x61,(char)0x62,(char)0x6C,(char)0x65,(char)0x20,(char)0x73,(char)0x65,(char)0x6E,(char)0x64
      ,(char)0x45,(char)0x76,(char)0x65,(char)0x6E,(char)0x74,(char)0x73,(char)0x3D,(char)0x22,(char)0x6E,(char)0x6F
      ,(char)0x22,(char)0x3E,(char)0x3C,(char)0x6E,(char)0x61,(char)0x6D,(char)0x65,(char)0x3E,(char)0x43,(char)0x6F
      ,(char)0x6E,(char)0x74,(char)0x72,(char)0x61,(char)0x73,(char)0x74,(char)0x3C,(char)0x2F,(char)0xC5,(char)0x03
      ,(char)0x0F,(char)0x3C,(char)0x64,(char)0x61,(char)0x74,(char)0x61,(char)0x54,(char)0x79,(char)0x70,(char)0x65
      ,(char)0x3E,(char)0x75,(char)0x69,(char)0x32,(char)0x3C,(char)0x2F,(char)0x89,(char)0x03,(char)0x1F,(char)0x3C
      ,(char)0x61,(char)0x6C,(char)0x6C,(char)0x6F,(char)0x77,(char)0x65,(char)0x64,(char)0x56,(char)0x61,(char)0x6C
      ,(char)0x75,(char)0x65,(char)0x52,(char)0x61,(char)0x6E,(char)0x67,(char)0x65,(char)0x3E,(char)0x3C,(char)0x6D
      ,(char)0x69,(char)0x6E,(char)0x69,(char)0x6D,(char)0x75,(char)0x6D,(char)0x3E,(char)0x30,(char)0x3C,(char)0x2F
      ,(char)0xC8,(char)0x02,(char)0x04,(char)0x3C,(char)0x6D,(char)0x61,(char)0x78,(char)0x05,(char)0x05,(char)0x05
      ,(char)0x31,(char)0x30,(char)0x30,(char)0x3C,(char)0x2F,(char)0x48,(char)0x03,(char)0x02,(char)0x3C,(char)0x2F
      ,(char)0x93,(char)0x0F,(char)0x01,(char)0x2F,(char)0x8D,(char)0x27,(char)0x01,(char)0x3E,(char)0x5B,(char)0x2B
      ,(char)0x03,(char)0x79,(char)0x65,(char)0x73,(char)0x88,(char)0x2B,(char)0x06,(char)0x4C,(char)0x61,(char)0x73
      ,(char)0x74,(char)0x43,(char)0x68,(char)0xC4,(char)0x1F,(char)0x00,(char)0x11,(char)0x2C,(char)0x06,(char)0x73
      ,(char)0x74,(char)0x72,(char)0x69,(char)0x6E,(char)0x67,(char)0xCC,(char)0x2C,(char)0x00,(char)0xAA,(char)0x18
      ,(char)0x00,(char)0xCA,(char)0x43,(char)0x0A,(char)0x42,(char)0x72,(char)0x69,(char)0x67,(char)0x68,(char)0x74
      ,(char)0x6E,(char)0x65,(char)0x73,(char)0x73,(char)0x7F,(char)0x44,(char)0x00,(char)0x7F,(char)0x44,(char)0x00
      ,(char)0xE7,(char)0x2B,(char)0x06,(char)0x56,(char)0x6F,(char)0x6C,(char)0x75,(char)0x6D,(char)0x65,(char)0xFF
      ,(char)0x2A,(char)0x00,(char)0xFF,(char)0x2A,(char)0x00,(char)0xA7,(char)0x56,(char)0x14,(char)0x41,(char)0x5F
      ,(char)0x41,(char)0x52,(char)0x47,(char)0x5F,(char)0x54,(char)0x59,(char)0x50,(char)0x45,(char)0x5F,(char)0x50
      ,(char)0x72,(char)0x65,(char)0x73,(char)0x65,(char)0x74,(char)0x4E,(char)0x61,(char)0x6D,(char)0xA4,(char)0x71
      ,(char)0x00,(char)0x4C,(char)0x9E,(char)0x04,(char)0x4C,(char)0x69,(char)0x73,(char)0x74,(char)0xCE,(char)0xA2
      ,(char)0x10,(char)0x3E,(char)0x46,(char)0x61,(char)0x63,(char)0x74,(char)0x6F,(char)0x72,(char)0x79,(char)0x44
      ,(char)0x65,(char)0x66,(char)0x61,(char)0x75,(char)0x6C,(char)0x74,(char)0x73,(char)0xCE,(char)0x9A,(char)0x03
      ,(char)0x3E,(char)0x3C,(char)0x2F,(char)0xD2,(char)0x0F,(char)0x01,(char)0x64,(char)0xC6,(char)0x0A,(char)0x00
      ,(char)0xD7,(char)0x0F,(char)0x00,(char)0x8D,(char)0x07,(char)0x00,(char)0xF5,(char)0x90,(char)0x00,(char)0x8A
      ,(char)0x37,(char)0x02,(char)0x4C,(char)0x69,(char)0x13,(char)0xD6,(char)0x00,(char)0x3F,(char)0xAA,(char)0x00
      ,(char)0xC7,(char)0xED,(char)0x03,(char)0x4D,(char)0x75,(char)0x74,(char)0xD2,(char)0xC0,(char)0x07,(char)0x62
      ,(char)0x6F,(char)0x6F,(char)0x6C,(char)0x65,(char)0x61,(char)0x6E,(char)0xCC,(char)0xED,(char)0x00,(char)0x4D
      ,(char)0x3B,(char)0x01,(char)0x30,(char)0xFF,(char)0x37,(char)0x00,(char)0x10,(char)0x72,(char)0x0A,(char)0x49
      ,(char)0x6E,(char)0x73,(char)0x74,(char)0x61,(char)0x6E,(char)0x63,(char)0x65,(char)0x49,(char)0x44,(char)0x53
      ,(char)0xCB,(char)0x01,(char)0x34,(char)0xFF,(char)0x21,(char)0x00,(char)0xEA,(char)0x93,(char)0x07,(char)0x43
      ,(char)0x68,(char)0x61,(char)0x6E,(char)0x6E,(char)0x65,(char)0x6C,(char)0x3F,(char)0x93,(char)0x09,(char)0x75
      ,(char)0x65,(char)0x3E,(char)0x4D,(char)0x61,(char)0x73,(char)0x74,(char)0x65,(char)0x72,(char)0xF0,(char)0x90
      ,(char)0x00,(char)0x88,(char)0x0D,(char)0x00,(char)0x9D,(char)0x8E
   },
   356,
   1420
};
struct DMR__StateVariable_RenderingControl_Contrast DMR__StateVariable_RenderingControl_Contrast_Impl =
{
   0,
   76,
   76,
   19,
   137,
   20,
   {"0","64","1"},
   157,
   16
};
struct DMR__StateVariable_RenderingControl_LastChange DMR__StateVariable_RenderingControl_LastChange_Impl =
{
   173,
   82,
   255,
   16
};
struct DMR__StateVariable_RenderingControl_Brightness DMR__StateVariable_RenderingControl_Brightness_Impl =
{
   271,
   78,
   349,
   19,
   410,
   20,
   {"0","64","1"},
   430,
   16
};
struct DMR__StateVariable_RenderingControl_Volume DMR__StateVariable_RenderingControl_Volume_Impl =
{
   446,
   74,
   520,
   19,
   581,
   20,
   {"0","100","1"}, /* Modified by yuyu for DLNA 1.5 CTT 7.3.108.1 */
   601,
   16
};
struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_PresetName DMR__StateVariable_RenderingControl_A_ARG_TYPE_PresetName_Impl =
{
   617,
   92,
   709,
   18,
   771,
   19,
   {"FactoryDefaults",NULL},
   790,
   14,
   819,
   15,
   "FactoryDefaults",
   834,
   16
};
struct DMR__StateVariable_RenderingControl_PresetNameList DMR__StateVariable_RenderingControl_PresetNameList_Impl =
{
   850,
   85,
   935,
   16
};
struct DMR__StateVariable_RenderingControl_Mute DMR__StateVariable_RenderingControl_Mute_Impl =
{
   951,
   76,
   1027,
   14,
   1042,
   15,
   "0",
   1057,
   16
};
struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_InstanceID DMR__StateVariable_RenderingControl_A_ARG_TYPE_InstanceID_Impl =
{
   1073,
   89,
   1162,
   14,
   1177,
   15,
   "0",
   1192,
   16
};
struct DMR__StateVariable_RenderingControl_A_ARG_TYPE_Channel DMR__StateVariable_RenderingControl_A_ARG_TYPE_Channel_Impl =
{
   1208,
   89,
   1297,
   18,
   1350,
   19,
   {"Master",NULL},
   1369,
   14,
   1389,
   15,
   "Master",
   1404,
   16
};
struct DMR__ActionTable_AVTransport DMR__ActionTable_AVTransport_Impl =
{
   {
      (char)0x22,(char)0x3C,(char)0x61,(char)0x63,(char)0x74,(char)0x69,(char)0x6F,(char)0x6E,(char)0x3E,(char)0x3C
      ,(char)0x6E,(char)0x61,(char)0x6D,(char)0x65,(char)0x3E,(char)0x47,(char)0x65,(char)0x74,(char)0x43,(char)0x75
      ,(char)0x72,(char)0x72,(char)0x65,(char)0x6E,(char)0x74,(char)0x54,(char)0x72,(char)0x61,(char)0x6E,(char)0x73
      ,(char)0x70,(char)0x6F,(char)0x72,(char)0x74,(char)0x41,(char)0x05,(char)0x08,(char)0x03,(char)0x73,(char)0x3C
      ,(char)0x2F,(char)0x45,(char)0x08,(char)0x0D,(char)0x3C,(char)0x61,(char)0x72,(char)0x67,(char)0x75,(char)0x6D
      ,(char)0x65,(char)0x6E,(char)0x74,(char)0x4C,(char)0x69,(char)0x73,(char)0x74,(char)0x8A,(char)0x03,(char)0x00
      ,(char)0xC7,(char)0x0F,(char)0x0A,(char)0x49,(char)0x6E,(char)0x73,(char)0x74,(char)0x61,(char)0x6E,(char)0x63
      ,(char)0x65,(char)0x49,(char)0x44,(char)0xC8,(char)0x0B,(char)0x04,(char)0x64,(char)0x69,(char)0x72,(char)0x65
      ,(char)0x46,(char)0x18,(char)0x04,(char)0x69,(char)0x6E,(char)0x3C,(char)0x2F,(char)0x8A,(char)0x03,(char)0x21
      ,(char)0x3C,(char)0x72,(char)0x65,(char)0x6C,(char)0x61,(char)0x74,(char)0x65,(char)0x64,(char)0x53,(char)0x74
      ,(char)0x61,(char)0x74,(char)0x65,(char)0x56,(char)0x61,(char)0x72,(char)0x69,(char)0x61,(char)0x62,(char)0x6C
      ,(char)0x65,(char)0x3E,(char)0x41,(char)0x5F,(char)0x41,(char)0x52,(char)0x47,(char)0x5F,(char)0x54,(char)0x59
      ,(char)0x50,(char)0x45,(char)0x5F,(char)0xCC,(char)0x12,(char)0x00,(char)0x15,(char)0x0B,(char)0x02,(char)0x3C
      ,(char)0x2F,(char)0x4A,(char)0x1F,(char)0x00,(char)0xCF,(char)0x21,(char)0x00,(char)0xCF,(char)0x2C,(char)0x00
      ,(char)0x0A,(char)0x21,(char)0x03,(char)0x6F,(char)0x75,(char)0x74,(char)0x62,(char)0x21,(char)0x00,(char)0x59
      ,(char)0x40,(char)0x00,(char)0xE1,(char)0x21,(char)0x01,(char)0x2F,(char)0x4E,(char)0x47,(char)0x01,(char)0x2F
      ,(char)0xC8,(char)0x56,(char)0x00,(char)0xD0,(char)0x58,(char)0x11,(char)0x44,(char)0x65,(char)0x76,(char)0x69
      ,(char)0x63,(char)0x65,(char)0x43,(char)0x61,(char)0x70,(char)0x61,(char)0x62,(char)0x69,(char)0x6C,(char)0x69
      ,(char)0x74,(char)0x69,(char)0x65,(char)0xBF,(char)0x57,(char)0x00,(char)0xBF,(char)0x57,(char)0x00,(char)0xAF
      ,(char)0x57,(char)0x09,(char)0x50,(char)0x6C,(char)0x61,(char)0x79,(char)0x4D,(char)0x65,(char)0x64,(char)0x69
      ,(char)0x61,(char)0x37,(char)0x58,(char)0x08,(char)0x50,(char)0x6F,(char)0x73,(char)0x73,(char)0x69,(char)0x62
      ,(char)0x6C,(char)0x65,(char)0x04,(char)0x12,(char)0x0B,(char)0x62,(char)0x61,(char)0x63,(char)0x6B,(char)0x53
      ,(char)0x74,(char)0x6F,(char)0x72,(char)0x61,(char)0x67,(char)0x65,(char)0xC7,(char)0x14,(char)0x00,(char)0x30
      ,(char)0x7B,(char)0x03,(char)0x52,(char)0x65,(char)0x63,(char)0x7F,(char)0x23,(char)0x00,(char)0x45,(char)0x23
      ,(char)0x06,(char)0x52,(char)0x65,(char)0x63,(char)0x6F,(char)0x72,(char)0x64,(char)0xFF,(char)0x22,(char)0x0E
      ,(char)0x65,(char)0x63,(char)0x51,(char)0x75,(char)0x61,(char)0x6C,(char)0x69,(char)0x74,(char)0x79,(char)0x4D
      ,(char)0x6F,(char)0x64,(char)0x65,(char)0x73,(char)0xBF,(char)0x24,(char)0x00,(char)0x86,(char)0x24,(char)0x00
      ,(char)0x4E,(char)0x14,(char)0x00,(char)0xBF,(char)0xA0,(char)0x00,(char)0x4A,(char)0xF9,(char)0x00,(char)0x05
      ,(char)0x70,(char)0x04,(char)0x49,(char)0x6E,(char)0x66,(char)0x6F,(char)0xFF,(char)0xF5,(char)0x00,(char)0xFF
      ,(char)0xF5,(char)0x00,(char)0xEE,(char)0xF5,(char)0x07,(char)0x4E,(char)0x72,(char)0x54,(char)0x72,(char)0x61
      ,(char)0x63,(char)0x6B,(char)0x38,(char)0xF6,(char)0x08,(char)0x4E,(char)0x75,(char)0x6D,(char)0x62,(char)0x65
      ,(char)0x72,(char)0x4F,(char)0x66,(char)0x48,(char)0x11,(char)0x00,(char)0x30,(char)0xBE,(char)0x00,(char)0x05
      ,(char)0xBD,(char)0x04,(char)0x44,(char)0x75,(char)0x72,(char)0x61,(char)0x44,(char)0xF5,(char)0x00,(char)0x37
      ,(char)0xBF,(char)0x07,(char)0x43,(char)0x75,(char)0x72,(char)0x72,(char)0x65,(char)0x6E,(char)0x74,(char)0xCF
      ,(char)0x12,(char)0x00,(char)0xB0,(char)0xE0,(char)0x00,(char)0x87,(char)0x11,(char)0x03,(char)0x55,(char)0x52
      ,(char)0x49,(char)0xF7,(char)0xE0,(char)0x0A,(char)0x41,(char)0x56,(char)0x54,(char)0x72,(char)0x61,(char)0x6E
      ,(char)0x73,(char)0x70,(char)0x6F,(char)0x72,(char)0x46,(char)0x11,(char)0x00,(char)0x7A,(char)0x20,(char)0x08
      ,(char)0x4D,(char)0x65,(char)0x74,(char)0x61,(char)0x44,(char)0x61,(char)0x74,(char)0x61,(char)0x7F,(char)0x22
      ,(char)0x02,(char)0x6F,(char)0x72,(char)0x4E,(char)0x13,(char)0x00,(char)0xF1,(char)0x86,(char)0x02,(char)0x65
      ,(char)0x78,(char)0xFB,(char)0x43,(char)0x00,(char)0x84,(char)0x0F,(char)0x00,(char)0xFF,(char)0x44,(char)0x00
      ,(char)0x88,(char)0x20,(char)0x00,(char)0x3F,(char)0x44,(char)0x00,(char)0x92,(char)0x22,(char)0x00,(char)0x3A
      ,(char)0x45,(char)0x04,(char)0x50,(char)0x6C,(char)0x61,(char)0x79,(char)0x04,(char)0xFA,(char)0x02,(char)0x75
      ,(char)0x6D,(char)0x77,(char)0xCC,(char)0x00,(char)0x44,(char)0x10,(char)0x0B,(char)0x62,(char)0x61,(char)0x63
      ,(char)0x6B,(char)0x53,(char)0x74,(char)0x6F,(char)0x72,(char)0x61,(char)0x67,(char)0x65,(char)0x08,(char)0x13
      ,(char)0x00,(char)0xF0,(char)0xED,(char)0x06,(char)0x52,(char)0x65,(char)0x63,(char)0x6F,(char)0x72,(char)0x64
      ,(char)0xBD,(char)0x22,(char)0x00,(char)0xC6,(char)0x10,(char)0x00,(char)0x3F,(char)0x22,(char)0x05,(char)0x57
      ,(char)0x72,(char)0x69,(char)0x74,(char)0x65,(char)0x84,(char)0xFB,(char)0x02,(char)0x75,(char)0x73,(char)0xFD
      ,(char)0x21,(char)0x00,(char)0x06,(char)0x55,(char)0x00,(char)0x8D,(char)0x13,(char)0x00,(char)0x61,(char)0xF0
      ,(char)0x00,(char)0x09,(char)0xF3,(char)0x03,(char)0x4C,(char)0x69,(char)0x73,(char)0xC5,(char)0x03,(char)0x00
      ,(char)0x87,(char)0xE7,(char)0x00,(char)0x08,(char)0x02,(char)0x00,(char)0xC5,(char)0xF5,(char)0x07,(char)0x47
      ,(char)0x65,(char)0x74,(char)0x50,(char)0x6F,(char)0x73,(char)0x69,(char)0xC4,(char)0xF1,(char)0x04,(char)0x49
      ,(char)0x6E,(char)0x66,(char)0x6F,(char)0x08,(char)0xF7,(char)0x00,(char)0xCE,(char)0x0E,(char)0x00,(char)0x8F
      ,(char)0xE2,(char)0x0A,(char)0x49,(char)0x6E,(char)0x73,(char)0x74,(char)0x61,(char)0x6E,(char)0x63,(char)0x65
      ,(char)0x49,(char)0x44,(char)0x92,(char)0xE0,(char)0x02,(char)0x69,(char)0x6E,(char)0x63,(char)0xE0,(char)0x0A
      ,(char)0x5F,(char)0x41,(char)0x52,(char)0x47,(char)0x5F,(char)0x54,(char)0x59,(char)0x50,(char)0x45,(char)0x5F
      ,(char)0xCC,(char)0x12,(char)0x00,(char)0x30,(char)0xE0,(char)0x05,(char)0x54,(char)0x72,(char)0x61,(char)0x63
      ,(char)0x6B,(char)0xB7,(char)0xDF,(char)0x07,(char)0x43,(char)0x75,(char)0x72,(char)0x72,(char)0x65,(char)0x6E
      ,(char)0x74,(char)0xC7,(char)0x10,(char)0x00,(char)0xB5,(char)0x1E,(char)0x04,(char)0x44,(char)0x75,(char)0x72
      ,(char)0x61,(char)0xC4,(char)0xFB,(char)0x00,(char)0xBF,(char)0x20,(char)0x00,(char)0xCE,(char)0x12,(char)0x00
      ,(char)0x35,(char)0x41,(char)0x00,(char)0xCA,(char)0xEB,(char)0x00,(char)0x3F,(char)0x43,(char)0x02,(char)0x63
      ,(char)0x6B,(char)0xBA,(char)0xFE,(char)0x00,(char)0x85,(char)0x63,(char)0x03,(char)0x55,(char)0x52,(char)0x49
      ,(char)0x7F,(char)0x64,(char)0x00,(char)0x89,(char)0x11,(char)0x00,(char)0xB2,(char)0xFC,(char)0x05,(char)0x6C
      ,(char)0x54,(char)0x69,(char)0x6D,(char)0x65,(char)0x79,(char)0xFB,(char)0x06,(char)0x6C,(char)0x61,(char)0x74
      ,(char)0x69,(char)0x76,(char)0x65,(char)0xC4,(char)0x10,(char)0x00,(char)0x08,(char)0xC4,(char)0x00,(char)0xB2
      ,(char)0xFB,(char)0x03,(char)0x41,(char)0x62,(char)0x73,(char)0x3B,(char)0x21,(char)0x07,(char)0x41,(char)0x62
      ,(char)0x73,(char)0x6F,(char)0x6C,(char)0x75,(char)0x74,(char)0x3F,(char)0x21,(char)0x08,(char)0x52,(char)0x65
      ,(char)0x6C,(char)0x43,(char)0x6F,(char)0x75,(char)0x6E,(char)0x74,(char)0x7F,(char)0x42,(char)0x00,(char)0x05
      ,(char)0x11,(char)0x02,(char)0x65,(char)0x72,(char)0x3D,(char)0x43,(char)0x00,(char)0x3C,(char)0x22,(char)0x00
      ,(char)0x48,(char)0x43,(char)0x00,(char)0x32,(char)0x22,(char)0x00,(char)0xC9,(char)0xED,(char)0x03,(char)0x4C
      ,(char)0x69,(char)0x73,(char)0xC5,(char)0x03,(char)0x00,(char)0x87,(char)0xE1,(char)0x00,(char)0x08,(char)0x02
      ,(char)0x00,(char)0x85,(char)0xF0,(char)0x02,(char)0x47,(char)0x65,(char)0x04,(char)0xFF,(char)0x0A,(char)0x6E
      ,(char)0x73,(char)0x70,(char)0x6F,(char)0x72,(char)0x74,(char)0x49,(char)0x6E,(char)0x66,(char)0x6F,(char)0x48
      ,(char)0xF1,(char)0x00,(char)0x0E,(char)0x0F,(char)0x00,(char)0xCF,(char)0xFD,(char)0x0A,(char)0x49,(char)0x6E
      ,(char)0x73,(char)0x74,(char)0x61,(char)0x6E,(char)0x63,(char)0x65,(char)0x49,(char)0x44,(char)0x12,(char)0xFD
      ,(char)0x02,(char)0x69,(char)0x6E,(char)0x63,(char)0x78,(char)0x0A,(char)0x5F,(char)0x41,(char)0x52,(char)0x47
      ,(char)0x5F,(char)0x54,(char)0x59,(char)0x50,(char)0x45,(char)0x5F,(char)0xCC,(char)0x12,(char)0x00,(char)0x30
      ,(char)0xFD,(char)0x00,(char)0x0A,(char)0xEC,(char)0x00,(char)0x06,(char)0x30,(char)0x00,(char)0x85,(char)0xF3
      ,(char)0x00,(char)0x37,(char)0xFF,(char)0x00,(char)0x50,(char)0x11,(char)0x00,(char)0x3F,(char)0x23,(char)0x00
      ,(char)0x05,(char)0x23,(char)0x02,(char)0x75,(char)0x73,(char)0x7F,(char)0x23,(char)0x00,(char)0x89,(char)0x11
      ,(char)0x00,(char)0xB7,(char)0x46,(char)0x05,(char)0x53,(char)0x70,(char)0x65,(char)0x65,(char)0x64,(char)0x7F
      ,(char)0x44,(char)0x05,(char)0x74,(char)0x50,(char)0x6C,(char)0x61,(char)0x79,(char)0x47,(char)0x12,(char)0x00
      ,(char)0xFF,(char)0x9C,(char)0x00,(char)0xD3,(char)0x9C,(char)0x08,(char)0x53,(char)0x65,(char)0x74,(char)0x74
      ,(char)0x69,(char)0x6E,(char)0x67,(char)0x73,(char)0xFF,(char)0x9D,(char)0x00,(char)0xFF,(char)0x9D,(char)0x00
      ,(char)0x6E,(char)0xF4,(char)0x00,(char)0x44,(char)0x44,(char)0x03,(char)0x4D,(char)0x6F,(char)0x64,(char)0xB8
      ,(char)0x9A,(char)0x00,(char)0x87,(char)0xAD,(char)0x00,(char)0x8A,(char)0x11,(char)0x00,(char)0xF0,(char)0xBD
      ,(char)0x09,(char)0x52,(char)0x65,(char)0x63,(char)0x51,(char)0x75,(char)0x61,(char)0x6C,(char)0x69,(char)0x74
      ,(char)0xBF,(char)0x21,(char)0x00,(char)0x04,(char)0xCF,(char)0x06,(char)0x52,(char)0x65,(char)0x63,(char)0x6F
      ,(char)0x72,(char)0x64,(char)0xCD,(char)0x13,(char)0x00,(char)0x7F,(char)0x79,(char)0x00,(char)0x07,(char)0xE7
      ,(char)0x04,(char)0x4E,(char)0x65,(char)0x78,(char)0x74,(char)0x7F,(char)0x75,(char)0x00,(char)0x7F,(char)0x75
      ,(char)0x00,(char)0xFF,(char)0xAA,(char)0x00,(char)0xC6,(char)0x7A,(char)0x04,(char)0x61,(char)0x75,(char)0x73
      ,(char)0x65,(char)0xFF,(char)0x31,(char)0x00,(char)0xFF,(char)0x31,(char)0x00,(char)0xFF,(char)0x31,(char)0x00
      ,(char)0x89,(char)0xAC,(char)0x00,(char)0xBF,(char)0xD8,(char)0x00,(char)0xBF,(char)0xD8,(char)0x00,(char)0xAE
      ,(char)0xD8,(char)0x05,(char)0x53,(char)0x70,(char)0x65,(char)0x65,(char)0x64,(char)0x36,(char)0xF9,(char)0x08
      ,(char)0x54,(char)0x72,(char)0x61,(char)0x6E,(char)0x73,(char)0x70,(char)0x6F,(char)0x72,(char)0x05,(char)0xD8
      ,(char)0x00,(char)0x07,(char)0x12,(char)0x00,(char)0x3F,(char)0x83,(char)0x00,(char)0xC8,(char)0xFD,(char)0x07
      ,(char)0x72,(char)0x65,(char)0x76,(char)0x69,(char)0x6F,(char)0x75,(char)0x73,(char)0xBF,(char)0xB5,(char)0x00
      ,(char)0xBF,(char)0xB5,(char)0x00,(char)0x3F,(char)0xE7,(char)0x00,(char)0xC6,(char)0x57,(char)0x03,(char)0x65
      ,(char)0x65,(char)0x6B,(char)0xFF,(char)0x83,(char)0x00,(char)0xFF,(char)0x83,(char)0x00,(char)0xEE,(char)0x83
      ,(char)0x04,(char)0x55,(char)0x6E,(char)0x69,(char)0x74,(char)0xBF,(char)0xD5,(char)0x02,(char)0x45,(char)0x5F
      ,(char)0x44,(char)0x3D,(char)0x04,(char)0x4D,(char)0x6F,(char)0x64,(char)0x65,(char)0xB2,(char)0xA3,(char)0x05
      ,(char)0x54,(char)0x61,(char)0x72,(char)0x67,(char)0x65,(char)0x7F,(char)0x20,(char)0x00,(char)0x47,(char)0x20
      ,(char)0x00,(char)0xC8,(char)0x12,(char)0x00,(char)0x3F,(char)0x72,(char)0x00,(char)0x09,(char)0x72,(char)0x03
      ,(char)0x74,(char)0x41,(char)0x56,(char)0x49,(char)0xBC,(char)0x03,(char)0x55,(char)0x52,(char)0x49,(char)0x3F
      ,(char)0xF9,(char)0x00,(char)0x3F,(char)0xF9,(char)0x00,(char)0x2E,(char)0xF9,(char)0x06,(char)0x43,(char)0x75
      ,(char)0x72,(char)0x72,(char)0x65,(char)0x6E,(char)0x8C,(char)0x2D,(char)0x00,(char)0xAF,(char)0xC8,(char)0x00
      ,(char)0x8F,(char)0x3E,(char)0x00,(char)0x3A,(char)0x20,(char)0x08,(char)0x4D,(char)0x65,(char)0x74,(char)0x61
      ,(char)0x44,(char)0x61,(char)0x74,(char)0x61,(char)0x3F,(char)0x22,(char)0x01,(char)0x72,(char)0x0E,(char)0x13
      ,(char)0x00,(char)0xFF,(char)0x78,(char)0x00,(char)0xCA,(char)0x78,(char)0x04,(char)0x50,(char)0x6C,(char)0x61
      ,(char)0x79,(char)0x46,(char)0xAE,(char)0x00,(char)0xBF,(char)0xEC,(char)0x00,(char)0xBF,(char)0xEC,(char)0x00
      ,(char)0xAC,(char)0xEC,(char)0x03,(char)0x4E,(char)0x65,(char)0x77,(char)0xD0,(char)0x2D,(char)0x00,(char)0x6E
      ,(char)0xEE,(char)0x00,(char)0x87,(char)0x87,(char)0x00,(char)0x0A,(char)0x3F,(char)0x00,(char)0xBF,(char)0xCC
      ,(char)0x00,(char)0x88,(char)0xCC,(char)0x03,(char)0x74,(char)0x6F,(char)0x70,(char)0x7F,(char)0xC9,(char)0x00
      ,(char)0x7F,(char)0xC9,(char)0x00,(char)0x36,(char)0xFE
   },
   1125,
   7208
};
struct DMR__Action_AVTransport_GetCurrentTransportActions DMR__Action_AVTransport_GetCurrentTransportActions_Impl =
{
   0,
   355
};
struct DMR__Action_AVTransport_GetDeviceCapabilities DMR__Action_AVTransport_GetDeviceCapabilities_Impl =
{
   355,
   642
};
struct DMR__Action_AVTransport_GetMediaInfo DMR__Action_AVTransport_GetMediaInfo_Impl =
{
   997,
   1432
};
struct DMR__Action_AVTransport_GetPositionInfo DMR__Action_AVTransport_GetPositionInfo_Impl =
{
   2429,
   1271
};
struct DMR__Action_AVTransport_GetTransportInfo DMR__Action_AVTransport_GetTransportInfo_Impl =
{
   3700,
   627
};
struct DMR__Action_AVTransport_GetTransportSettings DMR__Action_AVTransport_GetTransportSettings_Impl =
{
   4327,
   485
};
struct DMR__Action_AVTransport_Next DMR__Action_AVTransport_Next_Impl =
{
   4812,
   198
};
struct DMR__Action_AVTransport_Pause DMR__Action_AVTransport_Pause_Impl =
{
   5010,
   199
};
struct DMR__Action_AVTransport_Play DMR__Action_AVTransport_Play_Impl =
{
   5209,
   325
};
struct DMR__Action_AVTransport_Previous DMR__Action_AVTransport_Previous_Impl =
{
   5534,
   202
};
struct DMR__Action_AVTransport_Seek DMR__Action_AVTransport_Seek_Impl =
{
   5736,
   456
};
struct DMR__Action_AVTransport_SetAVTransportURI DMR__Action_AVTransport_SetAVTransportURI_Impl =
{
   6192,
   483
};
struct DMR__Action_AVTransport_SetPlayMode DMR__Action_AVTransport_SetPlayMode_Impl =
{
   6675,
   335
};
struct DMR__Action_AVTransport_Stop DMR__Action_AVTransport_Stop_Impl =
{
   7010,
   198
};
struct DMR__ActionTable_ConnectionManager DMR__ActionTable_ConnectionManager_Impl =
{
   {
      (char)0x1D,(char)0x3C,(char)0x61,(char)0x63,(char)0x74,(char)0x69,(char)0x6F,(char)0x6E,(char)0x3E,(char)0x3C
      ,(char)0x6E,(char)0x61,(char)0x6D,(char)0x65,(char)0x3E,(char)0x47,(char)0x65,(char)0x74,(char)0x43,(char)0x75
      ,(char)0x72,(char)0x72,(char)0x65,(char)0x6E,(char)0x74,(char)0x43,(char)0x6F,(char)0x6E,(char)0x6E,(char)0x65
      ,(char)0xC5,(char)0x06,(char)0x05,(char)0x49,(char)0x44,(char)0x73,(char)0x3C,(char)0x2F,(char)0x85,(char)0x07
      ,(char)0x0D,(char)0x3C,(char)0x61,(char)0x72,(char)0x67,(char)0x75,(char)0x6D,(char)0x65,(char)0x6E,(char)0x74
      ,(char)0x4C,(char)0x69,(char)0x73,(char)0x74,(char)0x8A,(char)0x03,(char)0x00,(char)0x07,(char)0x0F,(char)0x00
      ,(char)0x95,(char)0x0C,(char)0x03,(char)0x64,(char)0x69,(char)0x72,(char)0x86,(char)0x11,(char)0x06,(char)0x3E
      ,(char)0x6F,(char)0x75,(char)0x74,(char)0x3C,(char)0x2F,(char)0xCA,(char)0x03,(char)0x16,(char)0x3C,(char)0x72
      ,(char)0x65,(char)0x6C,(char)0x61,(char)0x74,(char)0x65,(char)0x64,(char)0x53,(char)0x74,(char)0x61,(char)0x74
      ,(char)0x65,(char)0x56,(char)0x61,(char)0x72,(char)0x69,(char)0x61,(char)0x62,(char)0x6C,(char)0x65,(char)0x3E
      ,(char)0x56,(char)0x1F,(char)0x00,(char)0xD5,(char)0x0A,(char)0x02,(char)0x3C,(char)0x2F,(char)0x0A,(char)0x20
      ,(char)0x01,(char)0x2F,(char)0x4E,(char)0x26,(char)0x01,(char)0x2F,(char)0x08,(char)0x35,(char)0x00,(char)0x22
      ,(char)0x37,(char)0x03,(char)0x6E,(char)0x66,(char)0x6F,(char)0x71,(char)0x37,(char)0x00,(char)0x12,(char)0x37
      ,(char)0x02,(char)0x69,(char)0x6E,(char)0xE2,(char)0x36,(char)0x0B,(char)0x41,(char)0x5F,(char)0x41,(char)0x52
      ,(char)0x47,(char)0x5F,(char)0x54,(char)0x59,(char)0x50,(char)0x45,(char)0x5F,(char)0x4E,(char)0x13,(char)0x00
      ,(char)0xA1,(char)0x37,(char)0x00,(char)0x0F,(char)0x5A,(char)0x05,(char)0x52,(char)0x63,(char)0x73,(char)0x49
      ,(char)0x44,(char)0x37,(char)0x58,(char)0x00,(char)0x4B,(char)0x21,(char)0x03,(char)0x52,(char)0x63,(char)0x73
      ,(char)0xB4,(char)0x1F,(char)0x0B,(char)0x41,(char)0x56,(char)0x54,(char)0x72,(char)0x61,(char)0x6E,(char)0x73
      ,(char)0x70,(char)0x6F,(char)0x72,(char)0x74,(char)0xBF,(char)0x21,(char)0x00,(char)0xC5,(char)0x42,(char)0x00
      ,(char)0xCF,(char)0x13,(char)0x00,(char)0x30,(char)0x43,(char)0x08,(char)0x50,(char)0x72,(char)0x6F,(char)0x74
      ,(char)0x6F,(char)0x63,(char)0x6F,(char)0x6C,(char)0x0C,(char)0x72,(char)0x00,(char)0xFA,(char)0x44,(char)0x00
      ,(char)0x8E,(char)0x13,(char)0x00,(char)0x31,(char)0x23,(char)0x03,(char)0x65,(char)0x65,(char)0x72,(char)0x8A
      ,(char)0xCD,(char)0x07,(char)0x4D,(char)0x61,(char)0x6E,(char)0x61,(char)0x67,(char)0x65,(char)0x72,(char)0x3F
      ,(char)0x6A,(char)0x03,(char)0x50,(char)0x45,(char)0x5F,(char)0xD3,(char)0x14,(char)0x00,(char)0xBE,(char)0x26
      ,(char)0x00,(char)0x7F,(char)0x8F,(char)0x00,(char)0xBF,(char)0xB0,(char)0x00,(char)0x84,(char)0xE2,(char)0x01
      ,(char)0x44,(char)0x48,(char)0xCE,(char)0x00,(char)0xBF,(char)0xB1,(char)0x03,(char)0x50,(char)0x45,(char)0x5F
      ,(char)0xCB,(char)0x12,(char)0x00,(char)0x30,(char)0xD2,(char)0x00,(char)0xC4,(char)0xE7,(char)0x02,(char)0x75
      ,(char)0x73,(char)0x7F,(char)0x68,(char)0x00,(char)0x8D,(char)0xF3,(char)0x00,(char)0x88,(char)0x14,(char)0x00
      ,(char)0xA1,(char)0xF4,(char)0x00,(char)0x49,(char)0xF7,(char)0x03,(char)0x4C,(char)0x69,(char)0x73,(char)0xC5
      ,(char)0x03,(char)0x00,(char)0x07,(char)0xED,(char)0x00,(char)0x08,(char)0x02,(char)0x00,(char)0x05,(char)0xFA
      ,(char)0x03,(char)0x47,(char)0x65,(char)0x74,(char)0xD4,(char)0xB7,(char)0x00,(char)0xCE,(char)0x0E,(char)0x00
      ,(char)0x10,(char)0x35,(char)0x05,(char)0x6F,(char)0x75,(char)0x72,(char)0x63,(char)0x65,(char)0xF7,(char)0xE5
      ,(char)0x00,(char)0x46,(char)0x0F,(char)0x00,(char)0x7E,(char)0xC1,(char)0x04,(char)0x53,(char)0x69,(char)0x6E
      ,(char)0x6B,(char)0xF8,(char)0x1F,(char)0x03,(char)0x69,(char)0x6E,(char)0x6B,(char)0xAF,(char)0xE0,(char)0x00
      ,(char)0x17,(char)0x52
   },
   342,
   1748
};
struct DMR__Action_ConnectionManager_GetCurrentConnectionIDs DMR__Action_ConnectionManager_GetCurrentConnectionIDs_Impl =
{
   0,
   220
};
struct DMR__Action_ConnectionManager_GetCurrentConnectionInfo DMR__Action_ConnectionManager_GetCurrentConnectionInfo_Impl =
{
   220,
   1200
};
struct DMR__Action_ConnectionManager_GetProtocolInfo DMR__Action_ConnectionManager_GetProtocolInfo_Impl =
{
   1420,
   328
};
struct DMR__ActionTable_RenderingControl DMR__ActionTable_RenderingControl_Impl =
{
   {
      (char)0x1D,(char)0x3C,(char)0x61,(char)0x63,(char)0x74,(char)0x69,(char)0x6F,(char)0x6E,(char)0x3E,(char)0x3C
      ,(char)0x6E,(char)0x61,(char)0x6D,(char)0x65,(char)0x3E,(char)0x47,(char)0x65,(char)0x74,(char)0x42,(char)0x72
      ,(char)0x69,(char)0x67,(char)0x68,(char)0x74,(char)0x6E,(char)0x65,(char)0x73,(char)0x73,(char)0x3C,(char)0x2F
      ,(char)0x05,(char)0x05,(char)0x0D,(char)0x3C,(char)0x61,(char)0x72,(char)0x67,(char)0x75,(char)0x6D,(char)0x65
      ,(char)0x6E,(char)0x74,(char)0x4C,(char)0x69,(char)0x73,(char)0x74,(char)0x8A,(char)0x03,(char)0x00,(char)0x87
      ,(char)0x0C,(char)0x0A,(char)0x49,(char)0x6E,(char)0x73,(char)0x74,(char)0x61,(char)0x6E,(char)0x63,(char)0x65
      ,(char)0x49,(char)0x44,(char)0xC8,(char)0x0B,(char)0x04,(char)0x64,(char)0x69,(char)0x72,(char)0x65,(char)0x06
      ,(char)0x15,(char)0x04,(char)0x69,(char)0x6E,(char)0x3C,(char)0x2F,(char)0x8A,(char)0x03,(char)0x21,(char)0x3C
      ,(char)0x72,(char)0x65,(char)0x6C,(char)0x61,(char)0x74,(char)0x65,(char)0x64,(char)0x53,(char)0x74,(char)0x61
      ,(char)0x74,(char)0x65,(char)0x56,(char)0x61,(char)0x72,(char)0x69,(char)0x61,(char)0x62,(char)0x6C,(char)0x65
      ,(char)0x3E,(char)0x41,(char)0x5F,(char)0x41,(char)0x52,(char)0x47,(char)0x5F,(char)0x54,(char)0x59,(char)0x50
      ,(char)0x45,(char)0x5F,(char)0xCC,(char)0x12,(char)0x00,(char)0x15,(char)0x0B,(char)0x02,(char)0x3C,(char)0x2F
      ,(char)0x4A,(char)0x1F,(char)0x00,(char)0xCF,(char)0x21,(char)0x06,(char)0x43,(char)0x75,(char)0x72,(char)0x72
      ,(char)0x65,(char)0x6E,(char)0x53,(char)0x2F,(char)0x00,(char)0x8A,(char)0x23,(char)0x03,(char)0x6F,(char)0x75
      ,(char)0x74,(char)0xE2,(char)0x23,(char)0x00,(char)0x8C,(char)0x3F,(char)0x00,(char)0x21,(char)0x21,(char)0x01
      ,(char)0x2F,(char)0x8E,(char)0x46,(char)0x01,(char)0x2F,(char)0xC8,(char)0x52,(char)0x00,(char)0xD0,(char)0x54
      ,(char)0x08,(char)0x43,(char)0x6F,(char)0x6E,(char)0x74,(char)0x72,(char)0x61,(char)0x73,(char)0x74,(char)0x7F
      ,(char)0x54,(char)0x00,(char)0x7F,(char)0x54,(char)0x00,(char)0x75,(char)0x54,(char)0x00,(char)0xD0,(char)0x2E
      ,(char)0x00,(char)0xEF,(char)0x53,(char)0x00,(char)0x8A,(char)0x3E,(char)0x00,(char)0x7F,(char)0x53,(char)0x00
      ,(char)0x0A,(char)0xA8,(char)0x04,(char)0x4D,(char)0x75,(char)0x74,(char)0x65,(char)0xBF,(char)0xA6,(char)0x00
      ,(char)0xBF,(char)0xA6,(char)0x00,(char)0xAF,(char)0xA6,(char)0x06,(char)0x68,(char)0x61,(char)0x6E,(char)0x6E
      ,(char)0x65,(char)0x6C,(char)0xBF,(char)0xC7,(char)0x02,(char)0x45,(char)0x5F,(char)0x09,(char)0x12,(char)0x00
      ,(char)0xF7,(char)0xC6,(char)0x00,(char)0x0C,(char)0x4E,(char)0x00,(char)0x6F,(char)0xC5,(char)0x00,(char)0xC6
      ,(char)0x5C,(char)0x00,(char)0xFF,(char)0xC3,(char)0x00,(char)0xCA,(char)0xC3,(char)0x05,(char)0x56,(char)0x6F
      ,(char)0x6C,(char)0x75,(char)0x6D,(char)0x3F,(char)0x71,(char)0x00,(char)0x3F,(char)0x71,(char)0x00,(char)0x3F
      ,(char)0x71,(char)0x00,(char)0x3F,(char)0x71,(char)0x00,(char)0xB9,(char)0xE3,(char)0x00,(char)0x8E,(char)0x4E
      ,(char)0x00,(char)0x2F,(char)0xE3,(char)0x00,(char)0xC8,(char)0x5D,(char)0x00,(char)0xBF,(char)0xE2,(char)0x00
      ,(char)0x87,(char)0xE2,(char)0x00,(char)0x84,(char)0xE9,(char)0x07,(char)0x50,(char)0x72,(char)0x65,(char)0x73
      ,(char)0x65,(char)0x74,(char)0x73,(char)0xBF,(char)0xE3,(char)0x00,(char)0xBF,(char)0xE3,(char)0x00,(char)0x75
      ,(char)0xC3,(char)0x00,(char)0x86,(char)0x2E,(char)0x04,(char)0x4E,(char)0x61,(char)0x6D,(char)0x65,(char)0x04
      ,(char)0xAB,(char)0x00,(char)0xF7,(char)0xC5,(char)0x00,(char)0x50,(char)0x11,(char)0x00,(char)0x7F,(char)0xC8
      ,(char)0x00,(char)0xC7,(char)0xEB,(char)0x05,(char)0x53,(char)0x65,(char)0x6C,(char)0x65,(char)0x63,(char)0xC7
      ,(char)0x56,(char)0x00,(char)0x3F,(char)0xC9,(char)0x00,(char)0x3F,(char)0xC9,(char)0x00,(char)0x2E,(char)0xC9
      ,(char)0x00,(char)0xCA,(char)0x54,(char)0x00,(char)0xFF,(char)0xEA,(char)0x02,(char)0x45,(char)0x5F,(char)0xCC
      ,(char)0x12,(char)0x00,(char)0x7F,(char)0x55,(char)0x00,(char)0x49,(char)0x55,(char)0x0A,(char)0x74,(char)0x42
      ,(char)0x72,(char)0x69,(char)0x67,(char)0x68,(char)0x74,(char)0x6E,(char)0x65,(char)0x73,(char)0x3F,(char)0xAC
      ,(char)0x00,(char)0x3F,(char)0xAC,(char)0x00,(char)0x6F,(char)0xFE,(char)0x07,(char)0x44,(char)0x65,(char)0x73
      ,(char)0x69,(char)0x72,(char)0x65,(char)0x64,(char)0x52,(char)0x2F,(char)0x00,(char)0xAE,(char)0xCF,(char)0x00
      ,(char)0x4C,(char)0x3F,(char)0x00,(char)0xBF,(char)0x54,(char)0x00,(char)0x8A,(char)0x54,(char)0x07,(char)0x43
      ,(char)0x6F,(char)0x6E,(char)0x74,(char)0x72,(char)0x61,(char)0x73,(char)0xBF,(char)0xA9,(char)0x00,(char)0x3F
      ,(char)0x54,(char)0x00,(char)0x36,(char)0x54,(char)0x00,(char)0xD0,(char)0x2E,(char)0x00,(char)0xAE,(char)0xCC
      ,(char)0x00,(char)0x4A,(char)0x3E,(char)0x00,(char)0xBF,(char)0xA7,(char)0x00,(char)0x8A,(char)0xA7,(char)0x04
      ,(char)0x4D,(char)0x75,(char)0x74,(char)0x65,(char)0xBF,(char)0xFB,(char)0x00,(char)0xBF,(char)0xFB,(char)0x00
      ,(char)0xAE,(char)0xFB,(char)0x07,(char)0x43,(char)0x68,(char)0x61,(char)0x6E,(char)0x6E,(char)0x65,(char)0x6C
      ,(char)0xFF,(char)0xFA,(char)0x02,(char)0x45,(char)0x5F,(char)0x09,(char)0x12,(char)0x00,(char)0x77,(char)0xC6
      ,(char)0x00,(char)0x0C,(char)0x4E,(char)0x00,(char)0x6E,(char)0xE8,(char)0x00,(char)0x86,(char)0x5C,(char)0x00
      ,(char)0x7F,(char)0xC3,(char)0x00,(char)0x4A,(char)0xC3,(char)0x05,(char)0x56,(char)0x6F,(char)0x6C,(char)0x75
      ,(char)0x6D,(char)0xFF,(char)0x70,(char)0x00,(char)0xFF,(char)0x70,(char)0x00,(char)0xFF,(char)0x70,(char)0x00
      ,(char)0xFF,(char)0x70,(char)0x00,(char)0x39,(char)0xE3,(char)0x00,(char)0x8E,(char)0x4E,(char)0x00,(char)0xAE
      ,(char)0xE2,(char)0x00,(char)0x88,(char)0x5D,(char)0x00,(char)0x38,(char)0xE2
   },
   497,
   3838
};
struct DMR__Action_RenderingControl_GetBrightness DMR__Action_RenderingControl_GetBrightness_Impl =
{
   0,
   339
};
struct DMR__Action_RenderingControl_GetContrast DMR__Action_RenderingControl_GetContrast_Impl =
{
   339,
   333
};
struct DMR__Action_RenderingControl_GetMute DMR__Action_RenderingControl_GetMute_Impl =
{
   672,
   450
};
struct DMR__Action_RenderingControl_GetVolume DMR__Action_RenderingControl_GetVolume_Impl =
{
   1122,
   456
};
struct DMR__Action_RenderingControl_ListPresets DMR__Action_RenderingControl_ListPresets_Impl =
{
   1578,
   345
};
struct DMR__Action_RenderingControl_SelectPreset DMR__Action_RenderingControl_SelectPreset_Impl =
{
   1923,
   341
};
struct DMR__Action_RenderingControl_SetBrightness DMR__Action_RenderingControl_SetBrightness_Impl =
{
   2264,
   338
};
struct DMR__Action_RenderingControl_SetContrast DMR__Action_RenderingControl_SetContrast_Impl =
{
   2602,
   332
};
struct DMR__Action_RenderingControl_SetMute DMR__Action_RenderingControl_SetMute_Impl =
{
   2934,
   449
};
struct DMR__Action_RenderingControl_SetVolume DMR__Action_RenderingControl_SetVolume_Impl =
{
   3383,
   455
};
struct DMR__Service_AVTransport DMR__Service_AVTransport_Impl =
{
   &DMR__Action_AVTransport_GetCurrentTransportActions_Impl,
   &DMR__Action_AVTransport_GetDeviceCapabilities_Impl,
   &DMR__Action_AVTransport_GetMediaInfo_Impl,
   &DMR__Action_AVTransport_GetPositionInfo_Impl,
   &DMR__Action_AVTransport_GetTransportInfo_Impl,
   &DMR__Action_AVTransport_GetTransportSettings_Impl,
   &DMR__Action_AVTransport_Next_Impl,
   &DMR__Action_AVTransport_Pause_Impl,
   &DMR__Action_AVTransport_Play_Impl,
   &DMR__Action_AVTransport_Previous_Impl,
   &DMR__Action_AVTransport_Seek_Impl,
   &DMR__Action_AVTransport_SetAVTransportURI_Impl,
   &DMR__Action_AVTransport_SetPlayMode_Impl,
   &DMR__Action_AVTransport_Stop_Impl,
   &DMR__StateVariable_AVTransport_CurrentPlayMode_Impl,
   &DMR__StateVariable_AVTransport_RecordStorageMedium_Impl,
   &DMR__StateVariable_AVTransport_LastChange_Impl,
   &DMR__StateVariable_AVTransport_RelativeTimePosition_Impl,
   &DMR__StateVariable_AVTransport_CurrentTrackURI_Impl,
   &DMR__StateVariable_AVTransport_CurrentTrackDuration_Impl,
   &DMR__StateVariable_AVTransport_CurrentRecordQualityMode_Impl,
   &DMR__StateVariable_AVTransport_CurrentMediaDuration_Impl,
   &DMR__StateVariable_AVTransport_AbsoluteCounterPosition_Impl,
   &DMR__StateVariable_AVTransport_RelativeCounterPosition_Impl,
   &DMR__StateVariable_AVTransport_A_ARG_TYPE_InstanceID_Impl,
   &DMR__StateVariable_AVTransport_AVTransportURI_Impl,
   &DMR__StateVariable_AVTransport_TransportState_Impl,
   &DMR__StateVariable_AVTransport_CurrentTrackMetaData_Impl,
   &DMR__StateVariable_AVTransport_NextAVTransportURI_Impl,
   &DMR__StateVariable_AVTransport_PossibleRecordQualityModes_Impl,
   &DMR__StateVariable_AVTransport_CurrentTrack_Impl,
   &DMR__StateVariable_AVTransport_AbsoluteTimePosition_Impl,
   &DMR__StateVariable_AVTransport_NextAVTransportURIMetaData_Impl,
   &DMR__StateVariable_AVTransport_PlaybackStorageMedium_Impl,
   &DMR__StateVariable_AVTransport_CurrentTransportActions_Impl,
   &DMR__StateVariable_AVTransport_RecordMediumWriteStatus_Impl,
   &DMR__StateVariable_AVTransport_PossiblePlaybackStorageMedia_Impl,
   &DMR__StateVariable_AVTransport_AVTransportURIMetaData_Impl,
   &DMR__StateVariable_AVTransport_NumberOfTracks_Impl,
   &DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekMode_Impl,
   &DMR__StateVariable_AVTransport_A_ARG_TYPE_SeekTarget_Impl,
   &DMR__StateVariable_AVTransport_PossibleRecordStorageMedia_Impl,
   &DMR__StateVariable_AVTransport_TransportStatus_Impl,
   &DMR__StateVariable_AVTransport_TransportPlaySpeed_Impl,
   {
      (char)0x09,(char)0x3C,(char)0x73,(char)0x65,(char)0x72,(char)0x76,(char)0x69,(char)0x63,(char)0x65,(char)0x3E
      ,(char)0x48,(char)0x02,(char)0x1A,(char)0x54,(char)0x79,(char)0x70,(char)0x65,(char)0x3E,(char)0x75,(char)0x72
      ,(char)0x6E,(char)0x3A,(char)0x73,(char)0x63,(char)0x68,(char)0x65,(char)0x6D,(char)0x61,(char)0x73,(char)0x2D
      ,(char)0x75,(char)0x70,(char)0x6E,(char)0x70,(char)0x2D,(char)0x6F,(char)0x72,(char)0x67,(char)0x3A,(char)0x87
      ,(char)0x0A,(char)0x10,(char)0x3A,(char)0x41,(char)0x56,(char)0x54,(char)0x72,(char)0x61,(char)0x6E,(char)0x73
      ,(char)0x70,(char)0x6F,(char)0x72,(char)0x74,(char)0x3A,(char)0x31,(char)0x3C,(char)0x2F,(char)0x0C,(char)0x0E
      ,(char)0x00,(char)0x88,(char)0x13,(char)0x02,(char)0x49,(char)0x64,(char)0xC5,(char)0x10,(char)0x00,(char)0xD0
      ,(char)0x0E,(char)0x02,(char)0x49,(char)0x64,(char)0x4C,(char)0x0F,(char)0x27,(char)0x5F,(char)0x31,(char)0x46
      ,(char)0x38,(char)0x38,(char)0x42,(char)0x37,(char)0x37,(char)0x41,(char)0x2D,(char)0x38,(char)0x32,(char)0x33
      ,(char)0x36,(char)0x2D,(char)0x34,(char)0x39,(char)0x62,(char)0x39,(char)0x2D,(char)0x42,(char)0x33,(char)0x34
      ,(char)0x34,(char)0x2D,(char)0x39,(char)0x37,(char)0x34,(char)0x39,(char)0x36,(char)0x39,(char)0x34,(char)0x31
      ,(char)0x32,(char)0x39,(char)0x33,(char)0x30,(char)0x3C,(char)0x2F,(char)0xCA,(char)0x14,(char)0x09,(char)0x3C
      ,(char)0x53,(char)0x43,(char)0x50,(char)0x44,(char)0x55,(char)0x52,(char)0x4C,(char)0x3E,(char)0x8B,(char)0x20
      ,(char)0x0B,(char)0x2F,(char)0x73,(char)0x63,(char)0x70,(char)0x64,(char)0x2E,(char)0x78,(char)0x6D,(char)0x6C
      ,(char)0x3C,(char)0x2F,(char)0x88,(char)0x07,(char)0x08,(char)0x3C,(char)0x63,(char)0x6F,(char)0x6E,(char)0x74
      ,(char)0x72,(char)0x6F,(char)0x6C,(char)0x90,(char)0x0A,(char)0x00,(char)0xC7,(char)0x05,(char)0x02,(char)0x3C
      ,(char)0x2F,(char)0x0B,(char)0x08,(char)0x09,(char)0x3C,(char)0x65,(char)0x76,(char)0x65,(char)0x6E,(char)0x74
      ,(char)0x53,(char)0x75,(char)0x62,(char)0xD0,(char)0x15,(char)0x00,(char)0x05,(char)0x06,(char)0x02,(char)0x3C
      ,(char)0x2F,(char)0xCC,(char)0x07,(char)0x00,(char)0xC9,(char)0x3A,(char)0x01,(char)0x3E,(char)0x00,(char)0x00

   },
   190,
   309,
};
struct DMR__Service_ConnectionManager DMR__Service_ConnectionManager_Impl =
{
   &DMR__Action_ConnectionManager_GetCurrentConnectionIDs_Impl,
   &DMR__Action_ConnectionManager_GetCurrentConnectionInfo_Impl,
   &DMR__Action_ConnectionManager_GetProtocolInfo_Impl,
   &DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ProtocolInfo_Impl,
   &DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionStatus_Impl,
   &DMR__StateVariable_ConnectionManager_A_ARG_TYPE_AVTransportID_Impl,
   &DMR__StateVariable_ConnectionManager_A_ARG_TYPE_RcsID_Impl,
   &DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionID_Impl,
   &DMR__StateVariable_ConnectionManager_A_ARG_TYPE_ConnectionManager_Impl,
   &DMR__StateVariable_ConnectionManager_SourceProtocolInfo_Impl,
   &DMR__StateVariable_ConnectionManager_SinkProtocolInfo_Impl,
   &DMR__StateVariable_ConnectionManager_A_ARG_TYPE_Direction_Impl,
   &DMR__StateVariable_ConnectionManager_CurrentConnectionIDs_Impl,
   {
      (char)0x09,(char)0x3C,(char)0x73,(char)0x65,(char)0x72,(char)0x76,(char)0x69,(char)0x63,(char)0x65,(char)0x3E
      ,(char)0x48,(char)0x02,(char)0x1A,(char)0x54,(char)0x79,(char)0x70,(char)0x65,(char)0x3E,(char)0x75,(char)0x72
      ,(char)0x6E,(char)0x3A,(char)0x73,(char)0x63,(char)0x68,(char)0x65,(char)0x6D,(char)0x61,(char)0x73,(char)0x2D
      ,(char)0x75,(char)0x70,(char)0x6E,(char)0x70,(char)0x2D,(char)0x6F,(char)0x72,(char)0x67,(char)0x3A,(char)0x87
      ,(char)0x0A,(char)0x16,(char)0x3A,(char)0x43,(char)0x6F,(char)0x6E,(char)0x6E,(char)0x65,(char)0x63,(char)0x74
      ,(char)0x69,(char)0x6F,(char)0x6E,(char)0x4D,(char)0x61,(char)0x6E,(char)0x61,(char)0x67,(char)0x65,(char)0x72
      ,(char)0x3A,(char)0x31,(char)0x3C,(char)0x2F,(char)0x8C,(char)0x0F,(char)0x00,(char)0x08,(char)0x15,(char)0x02
      ,(char)0x49,(char)0x64,(char)0x45,(char)0x12,(char)0x00,(char)0x50,(char)0x10,(char)0x02,(char)0x49,(char)0x64
      ,(char)0xD2,(char)0x10,(char)0x27,(char)0x5F,(char)0x45,(char)0x31,(char)0x31,(char)0x34,(char)0x41,(char)0x36
      ,(char)0x31,(char)0x36,(char)0x2D,(char)0x39,(char)0x42,(char)0x31,(char)0x42,(char)0x2D,(char)0x34,(char)0x39
      ,(char)0x30,(char)0x34,(char)0x2D,(char)0x42,(char)0x32,(char)0x44,(char)0x34,(char)0x2D,(char)0x41,(char)0x31
      ,(char)0x44,(char)0x41,(char)0x30,(char)0x32,(char)0x45,(char)0x34,(char)0x43,(char)0x34,(char)0x33,(char)0x39
      ,(char)0x3C,(char)0x2F,(char)0x4A,(char)0x16,(char)0x09,(char)0x3C,(char)0x53,(char)0x43,(char)0x50,(char)0x44
      ,(char)0x55,(char)0x52,(char)0x4C,(char)0x3E,(char)0x91,(char)0x23,(char)0x0B,(char)0x2F,(char)0x73,(char)0x63
      ,(char)0x70,(char)0x64,(char)0x2E,(char)0x78,(char)0x6D,(char)0x6C,(char)0x3C,(char)0x2F,(char)0x08,(char)0x09
      ,(char)0x08,(char)0x3C,(char)0x63,(char)0x6F,(char)0x6E,(char)0x74,(char)0x72,(char)0x6F,(char)0x6C,(char)0x16
      ,(char)0x0C,(char)0x00,(char)0x47,(char)0x07,(char)0x02,(char)0x3C,(char)0x2F,(char)0x8B,(char)0x09,(char)0x09
      ,(char)0x3C,(char)0x65,(char)0x76,(char)0x65,(char)0x6E,(char)0x74,(char)0x53,(char)0x75,(char)0x62,(char)0xD6
      ,(char)0x18,(char)0x00,(char)0x85,(char)0x07,(char)0x02,(char)0x3C,(char)0x2F,(char)0x4C,(char)0x09,(char)0x00
      ,(char)0xC9,(char)0x40,(char)0x01,(char)0x3E,(char)0x00,(char)0x00
   },
   196,
   339,
};
struct DMR__Service_RenderingControl DMR__Service_RenderingControl_Impl =
{
   &DMR__Action_RenderingControl_GetBrightness_Impl,
   &DMR__Action_RenderingControl_GetContrast_Impl,
   &DMR__Action_RenderingControl_GetMute_Impl,
   &DMR__Action_RenderingControl_GetVolume_Impl,
   &DMR__Action_RenderingControl_ListPresets_Impl,
   &DMR__Action_RenderingControl_SelectPreset_Impl,
   &DMR__Action_RenderingControl_SetBrightness_Impl,
   &DMR__Action_RenderingControl_SetContrast_Impl,
   &DMR__Action_RenderingControl_SetMute_Impl,
   &DMR__Action_RenderingControl_SetVolume_Impl,
   &DMR__StateVariable_RenderingControl_Contrast_Impl,
   &DMR__StateVariable_RenderingControl_LastChange_Impl,
   &DMR__StateVariable_RenderingControl_Brightness_Impl,
   &DMR__StateVariable_RenderingControl_Volume_Impl,
   &DMR__StateVariable_RenderingControl_A_ARG_TYPE_PresetName_Impl,
   &DMR__StateVariable_RenderingControl_PresetNameList_Impl,
   &DMR__StateVariable_RenderingControl_Mute_Impl,
   &DMR__StateVariable_RenderingControl_A_ARG_TYPE_InstanceID_Impl,
   &DMR__StateVariable_RenderingControl_A_ARG_TYPE_Channel_Impl,
   {
      (char)0x09,(char)0x3C,(char)0x73,(char)0x65,(char)0x72,(char)0x76,(char)0x69,(char)0x63,(char)0x65,(char)0x3E
      ,(char)0x48,(char)0x02,(char)0x1A,(char)0x54,(char)0x79,(char)0x70,(char)0x65,(char)0x3E,(char)0x75,(char)0x72
      ,(char)0x6E,(char)0x3A,(char)0x73,(char)0x63,(char)0x68,(char)0x65,(char)0x6D,(char)0x61,(char)0x73,(char)0x2D
      ,(char)0x75,(char)0x70,(char)0x6E,(char)0x70,(char)0x2D,(char)0x6F,(char)0x72,(char)0x67,(char)0x3A,(char)0x87
      ,(char)0x0A,(char)0x15,(char)0x3A,(char)0x52,(char)0x65,(char)0x6E,(char)0x64,(char)0x65,(char)0x72,(char)0x69
      ,(char)0x6E,(char)0x67,(char)0x43,(char)0x6F,(char)0x6E,(char)0x74,(char)0x72,(char)0x6F,(char)0x6C,(char)0x3A
      ,(char)0x31,(char)0x3C,(char)0x2F,(char)0x4C,(char)0x0F,(char)0x00,(char)0xC8,(char)0x14,(char)0x02,(char)0x49
      ,(char)0x64,(char)0x05,(char)0x12,(char)0x00,(char)0x10,(char)0x10,(char)0x02,(char)0x49,(char)0x64,(char)0x91
      ,(char)0x10,(char)0x27,(char)0x5F,(char)0x46,(char)0x30,(char)0x32,(char)0x37,(char)0x37,(char)0x36,(char)0x37
      ,(char)0x42,(char)0x2D,(char)0x46,(char)0x41,(char)0x31,(char)0x30,(char)0x2D,(char)0x34,(char)0x65,(char)0x39
      ,(char)0x38,(char)0x2D,(char)0x39,(char)0x37,(char)0x43,(char)0x36,(char)0x2D,(char)0x44,(char)0x38,(char)0x30
      ,(char)0x45,(char)0x36,(char)0x45,(char)0x41,(char)0x34,(char)0x42,(char)0x34,(char)0x31,(char)0x39,(char)0x3C
      ,(char)0x2F,(char)0x0A,(char)0x16,(char)0x09,(char)0x3C,(char)0x53,(char)0x43,(char)0x50,(char)0x44,(char)0x55
      ,(char)0x52,(char)0x4C,(char)0x3E,(char)0x10,(char)0x23,(char)0x0B,(char)0x2F,(char)0x73,(char)0x63,(char)0x70
      ,(char)0x64,(char)0x2E,(char)0x78,(char)0x6D,(char)0x6C,(char)0x3C,(char)0x2F,(char)0xC8,(char)0x08,(char)0x02
      ,(char)0x3C,(char)0x63,(char)0xC6,(char)0x29,(char)0x00,(char)0xD5,(char)0x0B,(char)0x00,(char)0x07,(char)0x07
      ,(char)0x02,(char)0x3C,(char)0x2F,(char)0x4B,(char)0x09,(char)0x09,(char)0x3C,(char)0x65,(char)0x76,(char)0x65
      ,(char)0x6E,(char)0x74,(char)0x53,(char)0x75,(char)0x62,(char)0x55,(char)0x18,(char)0x00,(char)0x45,(char)0x07
      ,(char)0x02,(char)0x3C,(char)0x2F,(char)0x0C,(char)0x09,(char)0x00,(char)0xC9,(char)0x3F,(char)0x01,(char)0x3E
      ,(char)0x00,(char)0x00
   },
   192,
   334,
};
struct DMR__Device_MediaRenderer DMR__Device_MediaRenderer_Impl =
{
   &DMR__Service_AVTransport_Impl,
   &DMR__Service_ConnectionManager_Impl,
   &DMR__Service_RenderingControl_Impl,

   NULL,
   NULL,
   NULL,
   NULL,
   NULL,
   NULL,
   NULL,
   NULL,
   NULL,
   NULL,
   {
      (char)0x61,(char)0x3C,(char)0x3F,(char)0x78,(char)0x6D,(char)0x6C,(char)0x20,(char)0x76,(char)0x65,(char)0x72
      ,(char)0x73,(char)0x69,(char)0x6F,(char)0x6E,(char)0x3D,(char)0x22,(char)0x31,(char)0x2E,(char)0x30,(char)0x22
      ,(char)0x20,(char)0x65,(char)0x6E,(char)0x63,(char)0x6F,(char)0x64,(char)0x69,(char)0x6E,(char)0x67,(char)0x3D
      ,(char)0x22,(char)0x75,(char)0x74,(char)0x66,(char)0x2D,(char)0x38,(char)0x22,(char)0x3F,(char)0x3E,(char)0x0D
      ,(char)0x0A,(char)0x3C,(char)0x72,(char)0x6F,(char)0x6F,(char)0x74,(char)0x20,(char)0x78,(char)0x6D,(char)0x6C
      ,(char)0x6E,(char)0x73,(char)0x3D,(char)0x22,(char)0x75,(char)0x72,(char)0x6E,(char)0x3A,(char)0x73,(char)0x63
      ,(char)0x68,(char)0x65,(char)0x6D,(char)0x61,(char)0x73,(char)0x2D,(char)0x75,(char)0x70,(char)0x6E,(char)0x70
      ,(char)0x2D,(char)0x6F,(char)0x72,(char)0x67,(char)0x3A,(char)0x64,(char)0x65,(char)0x76,(char)0x69,(char)0x63
      ,(char)0x65,(char)0x2D,(char)0x31,(char)0x2D,(char)0x30,(char)0x22,(char)0x3E,(char)0x0D,(char)0x0A,(char)0x20
      ,(char)0x20,(char)0x20,(char)0x3C,(char)0x73,(char)0x70,(char)0x65,(char)0x63,(char)0x56,(char)0x86,(char)0x16
      ,(char)0x00,(char)0x86,(char)0x04,(char)0x00,(char)0x44,(char)0x05,(char)0x09,(char)0x6D,(char)0x61,(char)0x6A
      ,(char)0x6F,(char)0x72,(char)0x3E,(char)0x31,(char)0x3C,(char)0x2F,(char)0x46,(char)0x02,(char)0x00,(char)0x0A
      ,(char)0x06,(char)0x0B,(char)0x69,(char)0x6E,(char)0x6F,(char)0x72,(char)0x3E,(char)0x30,(char)0x3C,(char)0x2F
      ,(char)0x6D,(char)0x69,(char)0x6E,(char)0x08,(char)0x06,(char)0x02,(char)0x3C,(char)0x2F,(char)0xD1,(char)0x10
      ,(char)0x01,(char)0x3C,(char)0xC6,(char)0x19,(char)0x00,(char)0x0A,(char)0x14,(char)0x00,(char)0xC6,(char)0x1D
      ,(char)0x05,(char)0x54,(char)0x79,(char)0x70,(char)0x65,(char)0x3E,(char)0xDB,(char)0x25,(char)0x12,(char)0x3A
      ,(char)0x4D,(char)0x65,(char)0x64,(char)0x69,(char)0x61,(char)0x52,(char)0x65,(char)0x6E,(char)0x64,(char)0x65
      ,(char)0x72,(char)0x65,(char)0x72,(char)0x3A,(char)0x31,(char)0x3C,(char)0x2F,(char)0x0B,(char)0x0E,(char)0x00
      ,(char)0x0A,(char)0x13,(char)0x0D,(char)0x6C,(char)0x6E,(char)0x61,(char)0x3A,(char)0x58,(char)0x5F,(char)0x44
      ,(char)0x4C,(char)0x4E,(char)0x41,(char)0x43,(char)0x41,(char)0x50,(char)0x86,(char)0x3B,(char)0x01,(char)0x3A
      ,(char)0x44,(char)0x05,(char)0x00,(char)0xCE,(char)0x3C,(char)0x00,(char)0xC4,(char)0x09,(char)0x00,(char)0xD1
      ,(char)0x3C,(char)0x13,(char)0x70,(char)0x6C,(char)0x61,(char)0x79,(char)0x63,(char)0x6F,(char)0x6E,(char)0x74
      ,(char)0x61,(char)0x69,(char)0x6E,(char)0x65,(char)0x72,(char)0x2D,(char)0x30,(char)0x2D,(char)0x31,(char)0x3C
      ,(char)0x2F,(char)0xCE,(char)0x13,(char)0x00,(char)0xD5,(char)0x19,(char)0x03,(char)0x44,(char)0x4F,(char)0x43
      ,(char)0xEE,(char)0x19,(char)0x0A,(char)0x44,(char)0x4D,(char)0x52,(char)0x2D,(char)0x31,(char)0x2E,(char)0x35
      ,(char)0x30,(char)0x3C,(char)0x2F,(char)0x8E,(char)0x11,(char)0x00,(char)0x4A,(char)0x58,(char)0x11,(char)0x66
      ,(char)0x72,(char)0x69,(char)0x65,(char)0x6E,(char)0x64,(char)0x6C,(char)0x79,(char)0x4E,(char)0x61,(char)0x6D
      ,(char)0x65,(char)0x3E,(char)0x25,(char)0x73,(char)0x3C,(char)0x2F,(char)0x4D,(char)0x04,(char)0x00,(char)0x0B
      ,(char)0x62,(char)0x0A,(char)0x6E,(char)0x75,(char)0x66,(char)0x61,(char)0x63,(char)0x74,(char)0x75,(char)0x72
      ,(char)0x65,(char)0x72,(char)0xC5,(char)0x09,(char)0x00,(char)0x4D,(char)0x04,(char)0x00,(char)0xD5,(char)0x09
      ,(char)0x03,(char)0x55,(char)0x52,(char)0x4C,(char)0x91,(char)0x0A,(char)0x03,(char)0x55,(char)0x52,(char)0x4C
      ,(char)0x0B,(char)0x77,(char)0x0F,(char)0x6F,(char)0x64,(char)0x65,(char)0x6C,(char)0x44,(char)0x65,(char)0x73
      ,(char)0x63,(char)0x72,(char)0x69,(char)0x70,(char)0x74,(char)0x69,(char)0x6F,(char)0x6E,(char)0x06,(char)0x16
      ,(char)0x00,(char)0x50,(char)0x05,(char)0x00,(char)0xCE,(char)0x0B,(char)0x00,(char)0xC9,(char)0x29,(char)0x00
      ,(char)0x8A,(char)0x03,(char)0x00,(char)0x4F,(char)0x08,(char)0x05,(char)0x75,(char)0x6D,(char)0x62,(char)0x65
      ,(char)0x72,(char)0xCB,(char)0x08,(char)0x03,(char)0x75,(char)0x6D,(char)0x62,(char)0x8D,(char)0x28,(char)0x00
      ,(char)0x44,(char)0x1D,(char)0x00,(char)0xC4,(char)0x26,(char)0x0B,(char)0x68,(char)0x74,(char)0x74,(char)0x70
      ,(char)0x3A,(char)0x2F,(char)0x2F,(char)0x32,(char)0x35,(char)0x35,(char)0x2E,(char)0x04,(char)0x01,(char)0x00
      ,(char)0x07,(char)0x02,(char)0x07,(char)0x3A,(char)0x32,(char)0x35,(char)0x35,(char)0x2F,(char)0x3C,(char)0x2F
      ,(char)0x89,(char)0x09,(char)0x00,(char)0x49,(char)0xA2,(char)0x05,(char)0x73,(char)0x65,(char)0x72,(char)0x69
      ,(char)0x61,(char)0x8C,(char)0x17,(char)0x00,(char)0x4D,(char)0x04,(char)0x00,(char)0x09,(char)0xAC,(char)0x09
      ,(char)0x55,(char)0x44,(char)0x4E,(char)0x3E,(char)0x75,(char)0x75,(char)0x69,(char)0x64,(char)0x3A,(char)0xC4
      ,(char)0x52,(char)0x03,(char)0x55,(char)0x44,(char)0x4E,(char)0x88,(char)0xA6,(char)0x00,(char)0x09,(char)0xA2
      ,(char)0x02,(char)0x3C,(char)0x2F,(char)0xC4,(char)0xC6,(char)0x01,(char)0x3E,(char)0x00,(char)0x00
   },
   439,
   841,
   NULL,
   NULL
};

DMR_MicroStackToken DMR_CreateMicroStack(void *Chain, const char* FriendlyName,const char* UDN, const char* SerialNumber, const int NotifyCycleSeconds, const unsigned short PortNum);
/* UPnP Set Function Pointers Methods */
void (*DMR_FP_PresentationPage) (void* upnptoken,struct packetheader *packet);
/*! \var DMR_FP_AVTransport_GetCurrentTransportActions
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetCurrentTransportActions
*/
DMR__ActionHandler_AVTransport_GetCurrentTransportActions DMR_FP_AVTransport_GetCurrentTransportActions;
/*! \var DMR_FP_AVTransport_GetDeviceCapabilities
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetDeviceCapabilities
*/
DMR__ActionHandler_AVTransport_GetDeviceCapabilities DMR_FP_AVTransport_GetDeviceCapabilities;
/*! \var DMR_FP_AVTransport_GetMediaInfo
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetMediaInfo
*/
DMR__ActionHandler_AVTransport_GetMediaInfo DMR_FP_AVTransport_GetMediaInfo;
/*! \var DMR_FP_AVTransport_GetPositionInfo
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetPositionInfo
*/
DMR__ActionHandler_AVTransport_GetPositionInfo DMR_FP_AVTransport_GetPositionInfo;
/*! \var DMR_FP_AVTransport_GetTransportInfo
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetTransportInfo
*/
DMR__ActionHandler_AVTransport_GetTransportInfo DMR_FP_AVTransport_GetTransportInfo;
/*! \var DMR_FP_AVTransport_GetTransportSettings
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetTransportSettings
*/
DMR__ActionHandler_AVTransport_GetTransportSettings DMR_FP_AVTransport_GetTransportSettings;
/*! \var DMR_FP_AVTransport_Next
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Next
*/
DMR__ActionHandler_AVTransport_Next DMR_FP_AVTransport_Next;
/*! \var DMR_FP_AVTransport_Pause
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Pause
*/
DMR__ActionHandler_AVTransport_Pause DMR_FP_AVTransport_Pause;
/*! \var DMR_FP_AVTransport_Play
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Play
*/
DMR__ActionHandler_AVTransport_Play DMR_FP_AVTransport_Play;
/*! \var DMR_FP_AVTransport_Previous
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Previous
*/
DMR__ActionHandler_AVTransport_Previous DMR_FP_AVTransport_Previous;
/*! \var DMR_FP_AVTransport_Seek
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Seek
*/
DMR__ActionHandler_AVTransport_Seek DMR_FP_AVTransport_Seek;
/*! \var DMR_FP_AVTransport_SetAVTransportURI
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> SetAVTransportURI
*/
DMR__ActionHandler_AVTransport_SetAVTransportURI DMR_FP_AVTransport_SetAVTransportURI;
DMR__ActionHandler_AVTransport_SetNextAVTransportURI DMR_FP_AVTransport_SetNextAVTransportURI;

/*! \var DMR_FP_AVTransport_SetPlayMode
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> SetPlayMode
*/
DMR__ActionHandler_AVTransport_SetPlayMode DMR_FP_AVTransport_SetPlayMode;
/*! \var DMR_FP_AVTransport_Stop
	\brief Dispatch Pointer for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Stop
*/
DMR__ActionHandler_AVTransport_Stop DMR_FP_AVTransport_Stop;
/*! \var DMR_FP_ConnectionManager_GetCurrentConnectionIDs
	\brief Dispatch Pointer for ConnectionManager >> urn:schemas-upnp-org:service:ConnectionManager:1 >> GetCurrentConnectionIDs
*/
DMR__ActionHandler_ConnectionManager_GetCurrentConnectionIDs DMR_FP_ConnectionManager_GetCurrentConnectionIDs;
/*! \var DMR_FP_ConnectionManager_GetCurrentConnectionInfo
	\brief Dispatch Pointer for ConnectionManager >> urn:schemas-upnp-org:service:ConnectionManager:1 >> GetCurrentConnectionInfo
*/
DMR__ActionHandler_ConnectionManager_GetCurrentConnectionInfo DMR_FP_ConnectionManager_GetCurrentConnectionInfo;
/*! \var DMR_FP_ConnectionManager_GetProtocolInfo
	\brief Dispatch Pointer for ConnectionManager >> urn:schemas-upnp-org:service:ConnectionManager:1 >> GetProtocolInfo
*/
DMR__ActionHandler_ConnectionManager_GetProtocolInfo DMR_FP_ConnectionManager_GetProtocolInfo;
/*! \var DMR_FP_RenderingControl_GetBrightness
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetBrightness
*/
DMR__ActionHandler_RenderingControl_GetBrightness DMR_FP_RenderingControl_GetBrightness;
/*! \var DMR_FP_RenderingControl_GetContrast
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetContrast
*/
DMR__ActionHandler_RenderingControl_GetContrast DMR_FP_RenderingControl_GetContrast;
/*! \var DMR_FP_RenderingControl_GetMute
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetMute
*/
DMR__ActionHandler_RenderingControl_GetMute DMR_FP_RenderingControl_GetMute;
/*! \var DMR_FP_RenderingControl_GetVolume
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetVolume
*/
DMR__ActionHandler_RenderingControl_GetVolume DMR_FP_RenderingControl_GetVolume;
/*! \var DMR_FP_RenderingControl_ListPresets
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> ListPresets
*/
DMR__ActionHandler_RenderingControl_ListPresets DMR_FP_RenderingControl_ListPresets;
/*! \var DMR_FP_RenderingControl_SelectPreset
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SelectPreset
*/
DMR__ActionHandler_RenderingControl_SelectPreset DMR_FP_RenderingControl_SelectPreset;
/*! \var DMR_FP_RenderingControl_SetBrightness
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetBrightness
*/
DMR__ActionHandler_RenderingControl_SetBrightness DMR_FP_RenderingControl_SetBrightness;
/*! \var DMR_FP_RenderingControl_SetContrast
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetContrast
*/
DMR__ActionHandler_RenderingControl_SetContrast DMR_FP_RenderingControl_SetContrast;
/*! \var DMR_FP_RenderingControl_SetMute
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetMute
*/
DMR__ActionHandler_RenderingControl_SetMute DMR_FP_RenderingControl_SetMute;
/*! \var DMR_FP_RenderingControl_SetVolume
	\brief Dispatch Pointer for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetVolume
*/
DMR__ActionHandler_RenderingControl_SetVolume DMR_FP_RenderingControl_SetVolume;





struct DMR_DataObject;

//
// It should not be necessary to expose/modify any of these structures. They
// are used by the internal stack
//

struct SubscriberInfo
{
   char* SID;		// Subscription ID
   int SIDLength;
   int SEQ;


   int Address;
   unsigned short Port;
   char* Path;
   int PathLength;
   int RefCount;
   int Disposing;

   struct timeval RenewByTime;

   struct SubscriberInfo *Next;
   struct SubscriberInfo *Previous;
};
struct DMR_DataObject
{
   //
   // Absolutely DO NOT put anything above these 3 function pointers
   //
   ILibChain_PreSelect PreSelect;
   ILibChain_PostSelect PostSelect;
   ILibChain_Destroy Destroy;

   void *EventClient;
   void *Chain;
   int UpdateFlag;

   /* Network Poll */
   unsigned int NetworkPollTime;

   int ForceExit;
   char *UUID;
   char *UDN;
   char *Serial;
   void *User;
   void *User2;

   void *WebServerTimer;
   void *HTTPServer;

   int InitialNotify;

   char* AVTransport_LastChange;
	char* ConnectionManager_SourceProtocolInfo;
	char* ConnectionManager_SinkProtocolInfo;
	char* ConnectionManager_CurrentConnectionIDs;
	char* RenderingControl_LastChange;


   struct sockaddr_in addr;
   int addrlen;

   struct ip_mreq mreq;
   int *AddressList;
   int AddressListLength;

   int _NumEmbeddedDevices;
   int WebSocketPortNumber;

   void **NOTIFY_RECEIVE_socks;
   void **NOTIFY_SEND_socks;



   struct timeval CurrentTime;
   struct timeval NotifyTime;

   int SID;
   int NotifyCycleTime;



   sem_t EventLock;
   struct SubscriberInfo *HeadSubscriberPtr_AVTransport;
	int NumberOfSubscribers_AVTransport;
	struct SubscriberInfo *HeadSubscriberPtr_ConnectionManager;
	int NumberOfSubscribers_ConnectionManager;
	struct SubscriberInfo *HeadSubscriberPtr_RenderingControl;
	int NumberOfSubscribers_RenderingControl;

};

struct MSEARCH_state
{
   char *ST;
   int STLength;
   void *upnp;
   struct sockaddr_in dest_addr;
   int localIPAddress;
   void *Chain;
//vvv modified by yuyu, fixed socket not close leakage
   void *SubChain;
   //void *Socket;
//^^^ modified by yuyu, fixed socket not close leakage
};
struct DMR_FragmentNotifyStruct
{
   struct DMR_DataObject *upnp;
   int packetNumber;
};

static int bIsDestroied = 0;

extern sem_t Destroied;
/* Pre-declarations */
//yunfeng add
char *Rfc1123_DateTimeNow();
void DMR_StreamDescriptionDocument(struct ILibWebServer_Session *session,struct packetheader* header);

void DMR_FragmentedSendNotify(void *data);
void DMR_SendNotify(const struct DMR_DataObject *upnp);
void DMR_SendByeBye(const struct DMR_DataObject *upnp);
void DMR_MainInvokeSwitch();
void DMR_SendDataXmlEscaped(const void* DMR_Token, const char* Data, const int DataLength, const int Terminate);
void DMR_SendData(const void* DMR_Token, const char* Data, const int DataLength, const int Terminate);
int DMR_PeriodicNotify(struct DMR_DataObject *upnp);
void DMR_SendEvent_Body(void *upnptoken, char *body, int bodylength, struct SubscriberInfo *info);
void DMR_ProcessMSEARCH(struct DMR_DataObject *upnp, struct packetheader *packet);
struct in_addr DMR__inaddr;

/*! \fn DMR_GetWebServerToken(const DMR_MicroStackToken MicroStackToken)
\brief Converts a MicroStackToken to a WebServerToken
\par
\a MicroStackToken is the void* returned from a call to DMR_CreateMicroStack. The returned token, is the server token
not the session token.
\param MicroStackToken MicroStack Token
\returns WebServer Token
*/
void* DMR_GetWebServerToken(const DMR_MicroStackToken MicroStackToken)
{
   return(((struct DMR_DataObject*)MicroStackToken)->HTTPServer);
}





#define DMR_BuildSsdpResponsePacket(outpacket,outlength,ipaddr,port,EmbeddedDeviceNumber,USN,USNex,ST,NTex,NotifyTime)\
{\
   DMR__inaddr.s_addr = ipaddr;\
   char* date= Rfc1123_DateTimeNow();\
   *outlength = sprintf(outpacket,"HTTP/1.1 200 OK\r\nLOCATION: http://%s:%d/\r\nDATE: %s\r\nEXT:\r\nSERVER: %s, UPnP/1.0, Intel MicroStack/1.0.2718\r\nUSN: uuid:%s%s\r\nCACHE-CONTROL: max-age=%d\r\nST: %s%s\r\n\r\n" ,inet_ntoa(DMR__inaddr),port,date,DMR_PLATFORM,USN,USNex,NotifyTime,ST,NTex);\
	free(date);\
}
#define DMR_BuildSsdpNotifyPacket(outpacket,outlength,ipaddr,port,EmbeddedDeviceNumber,USN,USNex,NT,NTex,NotifyTime)\
{\
   DMR__inaddr.s_addr = ipaddr;\
   *outlength = sprintf(outpacket,"NOTIFY * HTTP/1.1\r\nLOCATION: http://%s:%d/\r\nHOST: 239.255.255.250:1900\r\nSERVER: %s, UPnP/1.0, Intel MicroStack/1.0.2718\r\nNTS: ssdp:alive\r\nUSN: uuid:%s%s\r\nCACHE-CONTROL: max-age=%d\r\nNT: %s%s\r\n\r\n",inet_ntoa(DMR__inaddr),port,DMR_PLATFORM,USN,USNex,NotifyTime,NT,NTex);\
}




void DMR_SetDisconnectFlag(DMR_SessionToken token,void *flag)
{
   ((struct ILibWebServer_Session*)token)->Reserved10=flag;
}


/*! \fn DMR_IPAddressListChanged(DMR_MicroStackToken MicroStackToken)
\brief Tell the underlying MicroStack that an IPAddress may have changed
\param MicroStackToken Microstack
*/
void DMR_IPAddressListChanged(DMR_MicroStackToken MicroStackToken)
{
   ((struct DMR_DataObject*)MicroStackToken)->UpdateFlag = 1;
   ILibForceUnBlockChain(((struct DMR_DataObject*)MicroStackToken)->Chain);
}


void DMR_SSDPSink(ILibAsyncUDPSocket_SocketModule socketModule,char* buffer, int bufferLength, int remoteInterface, unsigned short remotePort, void *user, void *user2, int *PAUSE)
{
   struct packetheader *packet;
   struct sockaddr_in addr;
   memset(&addr,0,sizeof(struct sockaddr_in));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = remoteInterface;
   addr.sin_port = htons(remotePort);

   packet = ILibParsePacketHeader(buffer,0,bufferLength);
   if(packet!=NULL)
   {
      packet->Source = &addr;
      packet->ReceivingAddress = ILibAsyncUDPSocket_GetLocalInterface(socketModule);
      if(packet->StatusCode==-1 && memcmp(packet->Directive,"M-SEARCH",8)==0 && packet->ReceivingAddress!=0)
      {
         //
         // Process the search request with our Multicast M-SEARCH Handler
         //
         DMR_ProcessMSEARCH(user, packet);
      }
      ILibDestructPacket(packet);
   }
}

//
//	Internal underlying Initialization, that shouldn't be called explicitely
//
// <param name="state">State object</param>
// <param name="NotifyCycleSeconds">Cycle duration</param>
// <param name="PortNumber">Port Number</param>
void DMR_Init(struct DMR_DataObject *state, void *chain, const int NotifyCycleSeconds,const unsigned short PortNumber)
{
   int i;

   state->Chain = chain;

   /* Setup Notification Timer */
   state->NotifyCycleTime = NotifyCycleSeconds;


   gettimeofday(&(state->CurrentTime),NULL);
   (state->NotifyTime).tv_sec = (state->CurrentTime).tv_sec  + (state->NotifyCycleTime/2);

   memset((char *)&(state->addr), 0, sizeof(state->addr));
   state->addr.sin_family = AF_INET;
   state->addr.sin_addr.s_addr = htonl(INADDR_ANY);
   state->addr.sin_port = (unsigned short)htons(UPNP_PORT);
   state->addrlen = sizeof(state->addr);


   /* Set up socket */
   state->AddressListLength = ILibGetLocalIPAddressList(&(state->AddressList));
   state->NOTIFY_SEND_socks = (void**)malloc(sizeof(void*)*(state->AddressListLength));
   state->NOTIFY_RECEIVE_socks = (void**)malloc(sizeof(void*)*(state->AddressListLength));

   //
   // Iterate through all the current IP Addresses
   //
   for(i=0;i<state->AddressListLength;++i)
   {
      state->NOTIFY_SEND_socks[i] = ILibAsyncUDPSocket_Create(
      chain,
      UPNP_MAX_SSDP_HEADER_SIZE,
      state->AddressList[i],
      0,
      ILibAsyncUDPSocket_Reuse_SHARED,
      NULL,
      NULL,
      state);
      ILibAsyncUDPSocket_JoinMulticastGroup(
      state->NOTIFY_SEND_socks[i],
      state->AddressList[i],
      inet_addr(UPNP_GROUP));

      ILibAsyncUDPSocket_SetMulticastTTL(state->NOTIFY_SEND_socks[i],UPNP_SSDP_TTL);

      state->NOTIFY_RECEIVE_socks[i] = ILibAsyncUDPSocket_Create(
      state->Chain,
      UPNP_MAX_SSDP_HEADER_SIZE,
      0,
      UPNP_PORT,
      ILibAsyncUDPSocket_Reuse_SHARED,
      &DMR_SSDPSink,
      NULL,
      state);
      ILibAsyncUDPSocket_JoinMulticastGroup(
      state->NOTIFY_RECEIVE_socks[i],
      state->AddressList[i],
      inet_addr(UPNP_GROUP));


   }

}
void DMR_PostMX_Destroy(void *object)
{
   struct MSEARCH_state *mss = (struct MSEARCH_state*)object;
   free(mss->ST);
   free(mss);
}

void DMR_OnPostMX_MSEARCH_SendOK(ILibAsyncUDPSocket_SocketModule socketModule, void *user1, void *user2)
{
   struct MSEARCH_state *mss = (struct MSEARCH_state*)user1;
//vvv modified by yuyu, fixed socket not close leakage
   ILibChain_SafeRemove_SubChain(mss->Chain, mss->SubChain);
   //ILibChain_SafeRemove(mss->Chain, mss->Socket);
//^^^ modified by yuyu, fixed socket not close leakage
   free(mss->ST);
   free(mss);
}
void DMR_PostMX_MSEARCH(void *object)
{
   struct MSEARCH_state *mss = (struct MSEARCH_state*)object;

   char *b = (char*)malloc(sizeof(char)*5000);
   int packetlength;
   void *response_socket;
   void *subChain;

   char *ST = mss->ST;
   int STLength = mss->STLength;
   struct DMR_DataObject *upnp = (struct DMR_DataObject*)mss->upnp;

   int rcode=0;
//vvv modified by yuyu, fixed socket not close leakage
   subChain = ILibCreateChain();
//^^^ modified by yuyu, fixed socket not close leakage

   response_socket = ILibAsyncUDPSocket_Create(
subChain,
//mss->Chain, //subChain, modified by yuyu, fixed socket not close leakage
   UPNP_MAX_SSDP_HEADER_SIZE,
   mss->localIPAddress,
   0,
   ILibAsyncUDPSocket_Reuse_SHARED,
   NULL,
   DMR_OnPostMX_MSEARCH_SendOK,
   mss);
//vvv modified by yuyu, fixed socket not close leakage
   ILibChain_SafeAdd_SubChain(mss->Chain,subChain);

   mss->SubChain = subChain;
   //mss->Socket = response_socket;
//^^^ modified by yuyu, fixed socket not close leakage

   //
   // Search for root device
   //
   if(STLength==15 && memcmp(ST,"upnp:rootdevice",15)==0)
   {

      DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::upnp:rootdevice","upnp:rootdevice","",upnp->NotifyCycleTime);


      rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
   }
   //
   // Search for everything
   //
   else if(STLength==8 && memcmp(ST,"ssdp:all",8)==0)
   {
      DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::upnp:rootdevice","upnp:rootdevice","",upnp->NotifyCycleTime);
							rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
							usleep(100*1000);
							DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"",upnp->UUID,"",upnp->NotifyCycleTime);
							rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
							usleep(100*1000);
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:device:MediaRenderer:1","urn:schemas-upnp-org:device:MediaRenderer:1","",upnp->NotifyCycleTime);
							rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
							usleep(100*1000);
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:AVTransport:1","urn:schemas-upnp-org:service:AVTransport:1","",upnp->NotifyCycleTime);
						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
							usleep(100*1000);
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:ConnectionManager:1","urn:schemas-upnp-org:service:ConnectionManager:1","",upnp->NotifyCycleTime);
						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
							usleep(100*1000);
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:RenderingControl:1","urn:schemas-upnp-org:service:RenderingControl:1","",upnp->NotifyCycleTime);

						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);

   }
   if(STLength==(int)strlen(upnp->UUID) && memcmp(ST,upnp->UUID,(int)strlen(upnp->UUID))==0)
				{
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"",upnp->UUID,"",upnp->NotifyCycleTime);
						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
				}
				if(STLength>=42 && memcmp(ST,"urn:schemas-upnp-org:device:MediaRenderer:",42)==0 && atoi(ST+42)<=1)
				{
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:device:MediaRenderer:1",ST,"",upnp->NotifyCycleTime);
						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
				}
				if(STLength>=41 && memcmp(ST,"urn:schemas-upnp-org:service:AVTransport:",41)==0 && atoi(ST+41)<=1)
				{
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:AVTransport:1",ST,"",upnp->NotifyCycleTime);
						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
				}
				if(STLength>=47 && memcmp(ST,"urn:schemas-upnp-org:service:ConnectionManager:",47)==0 && atoi(ST+47)<=1)
				{
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:ConnectionManager:1",ST,"",upnp->NotifyCycleTime);
						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
				}
				if(STLength>=46 && memcmp(ST,"urn:schemas-upnp-org:service:RenderingControl:",46)==0 && atoi(ST+46)<=1)
				{
						DMR_BuildSsdpResponsePacket(b,&packetlength,mss->localIPAddress,(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:RenderingControl:1",ST,"",upnp->NotifyCycleTime);
						rcode = ILibAsyncUDPSocket_SendTo(response_socket,mss->dest_addr.sin_addr.s_addr, ntohs(mss->dest_addr.sin_port), b, packetlength, ILibAsyncSocket_MemoryOwnership_USER);
				}

   if(rcode==0)
   {
//vvv modified by yuyu, fixed socket not close leakage
      ILibChain_SafeRemove_SubChain(mss->Chain, subChain);
      //ILibChain_SafeRemove(mss->Chain, response_socket);
//^^^ modified by yuyu, fixed socket not close leakage
      free(mss->ST);
      free(mss);
   }
   free(b);
}
void DMR_ProcessMSEARCH(struct DMR_DataObject *upnp, struct packetheader *packet)
{
   char* ST = NULL;
   int STLength = 0;
   struct packetheader_field_node *node;
   int MANOK = 0;
   unsigned long MXVal;
   int MXOK = 0;
   int MX;
   struct MSEARCH_state *mss = NULL;

   if(memcmp(packet->DirectiveObj,"*",1)==0)
   {
      if(memcmp(packet->Version,"1.1",3)==0)
      {
         node = packet->FirstField;
         while(node!=NULL)
         {
            if(node->FieldLength==2 && strncasecmp(node->Field,"ST",2)==0)
            {
               //
               // This is what is being searched for
               //
               ST = (char*)malloc(1+node->FieldDataLength);
               memcpy(ST,node->FieldData,node->FieldDataLength);
               ST[node->FieldDataLength] = 0;
               STLength = node->FieldDataLength;
            }
            else if(node->FieldLength==3 && strncasecmp(node->Field,"MAN",3)==0 && memcmp(node->FieldData,"\"ssdp:discover\"",15)==0)
            {
               //
               // This is a required header field
               //
               MANOK = 1;
            }
            else if(node->FieldLength==2 && strncasecmp(node->Field,"MX",2)==0 && ILibGetULong(node->FieldData,node->FieldDataLength,&MXVal)==0)
            {
               //
               // If the timeout value specified is greater than 10 seconds, just force it
               // down to 10 seconds
               //
               MXOK = 1;
               MXVal = MXVal>10?10:MXVal;
            }
            node = node->NextField;
         }
         if(MANOK!=0 && MXOK!=0)
         {
            if(MXVal==0)
            {
               MX = 0;
            }
            else
            {
               //
               // The timeout value should be a random number between 0 and the
               // specified value
               //
               MX = (int)(0 + ((unsigned short)rand() % MXVal));
            }
            mss = (struct MSEARCH_state*)malloc(sizeof(struct MSEARCH_state));
            mss->ST = ST;
            mss->STLength = STLength;
            mss->upnp = upnp;
            memset((char *)&(mss->dest_addr), 0, sizeof(mss->dest_addr));
            mss->dest_addr.sin_family = AF_INET;
            mss->dest_addr.sin_addr = packet->Source->sin_addr;
            mss->dest_addr.sin_port = packet->Source->sin_port;
            mss->localIPAddress = packet->ReceivingAddress;
            mss->Chain = upnp->Chain;

			//printf("#####################ProcessMSEARCH IP=%s\n",inet_ntoa(packet->Source->sin_addr));

			//
            // Register for a timed callback, so we can respond later
            //
            //ILibLifeTime_Add(upnp->WebServerTimer,mss,MX,&DMR_PostMX_MSEARCH,&DMR_PostMX_Destroy);
            DMR_PostMX_MSEARCH(mss);
         }
         else
         {
            free(ST);
         }
      }
   }
}
void DMR_Dispatch_AVTransport_GetCurrentTransportActions(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==26 && memcmp(xnode->Name,"GetCurrentTransportActions",26)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_GetCurrentTransportActions == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_GetCurrentTransportActions((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_GetDeviceCapabilities(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==21 && memcmp(xnode->Name,"GetDeviceCapabilities",21)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_GetDeviceCapabilities == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_GetDeviceCapabilities((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_GetMediaInfo(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==12 && memcmp(xnode->Name,"GetMediaInfo",12)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_GetMediaInfo == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_GetMediaInfo((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_GetPositionInfo(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==15 && memcmp(xnode->Name,"GetPositionInfo",15)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_GetPositionInfo == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_GetPositionInfo((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_GetTransportInfo(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==16 && memcmp(xnode->Name,"GetTransportInfo",16)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_GetTransportInfo == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_GetTransportInfo((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_GetTransportSettings(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==20 && memcmp(xnode->Name,"GetTransportSettings",20)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_GetTransportSettings == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_GetTransportSettings((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_Next(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Next",4)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_Next == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_Next((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_Pause(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==5 && memcmp(xnode->Name,"Pause",5)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_Pause == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_Pause((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_Play(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_Speed = NULL;
	int p_SpeedLength = 0;
	char* _Speed = "";
	int _SpeedLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Play",4)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==5 && memcmp(xnode->Name,"Speed",5)==0)
								{
									p_SpeedLength = ILibReadInnerXML(xnode,&p_Speed);
									p_Speed[p_SpeedLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 3)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_SpeedLength = ILibInPlaceXmlUnEscape(p_Speed);
	_Speed = p_Speed;
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {
      if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->AllowedValues[OK]!=NULL)
      {
         if(strcmp(_Speed,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }
   if(OK!=0)
   {
		//DMR_Response_Error(ReaderObject,402,"Illegal value");
		DMR_Response_Error(ReaderObject,717,"Play Speed Not Supported");
		return;
	}
	if(DMR_FP_AVTransport_Play == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_Play((void*)ReaderObject,_InstanceID,_Speed);
}

void DMR_Dispatch_AVTransport_Previous(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Previous",8)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_Previous == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_Previous((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_AVTransport_Seek(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_Unit = NULL;
	int p_UnitLength = 0;
	char* _Unit = "";
	int _UnitLength;
	char *p_Target = NULL;
	int p_TargetLength = 0;
	char* _Target = "";
	int _TargetLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}

	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Seek",4)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==4 && memcmp(xnode->Name,"Unit",4)==0)
								{
									p_UnitLength = ILibReadInnerXML(xnode,&p_Unit);
									p_Unit[p_UnitLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength==6 && memcmp(xnode->Name,"Target",6)==0)
								{
									p_TargetLength = ILibReadInnerXML(xnode,&p_Target);
									p_Target[p_TargetLength]=0;
										OK |= 4;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 7)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_UnitLength = ILibInPlaceXmlUnEscape(p_Unit);
	_Unit = p_Unit;
	//printf("Seek _Unit=%s\n",_Unit);
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {

      if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->AllowedValues[OK]!=NULL)
      {
      	if(strcmp(_Unit,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }

   if(OK!=0)
   {
		//DMR_Response_Error(ReaderObject,402,"Illegal value");
		DMR_Response_Error(ReaderObject,710,"Seek Mode Not Supported");
		return;
	}
	_TargetLength = ILibInPlaceXmlUnEscape(p_Target);
	_Target = p_Target;

	if(DMR_FP_AVTransport_Seek == NULL)
	{
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	}
	else
	{
		DMR_FP_AVTransport_Seek((void*)ReaderObject,_InstanceID,_Unit,_Target);
	}
}

void DMR_Dispatch_AVTransport_SetAVTransportURI(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_CurrentURI = NULL;
	int p_CurrentURILength = 0;
	char* _CurrentURI = "";
	int _CurrentURILength;
	char *p_CurrentURIMetaData = NULL;
	int p_CurrentURIMetaDataLength = 0;
	char* _CurrentURIMetaData = "";
	int _CurrentURIMetaDataLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==17 && memcmp(xnode->Name,"SetAVTransportURI",17)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==10 && memcmp(xnode->Name,"CurrentURI",10)==0)
								{
									p_CurrentURILength = ILibReadInnerXML(xnode,&p_CurrentURI);
									p_CurrentURI[p_CurrentURILength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength==18 && memcmp(xnode->Name,"CurrentURIMetaData",18)==0)
								{
									p_CurrentURIMetaDataLength = ILibReadInnerXML(xnode,&p_CurrentURIMetaData);
									p_CurrentURIMetaData[p_CurrentURIMetaDataLength]=0;
										OK |= 4;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 7)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_CurrentURILength = ILibInPlaceXmlUnEscape(p_CurrentURI);
	_CurrentURI = p_CurrentURI;
	_CurrentURIMetaDataLength = ILibInPlaceXmlUnEscape(p_CurrentURIMetaData);
	_CurrentURIMetaData = p_CurrentURIMetaData;
	if(DMR_FP_AVTransport_SetAVTransportURI == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_SetAVTransportURI((void*)ReaderObject,_InstanceID,_CurrentURI,_CurrentURIMetaData);
}

void DMR_Dispatch_AVTransport_SetNextAVTransportURI(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_CurrentURI = NULL;
	int p_CurrentURILength = 0;
	char* _CurrentURI = "";
	int _CurrentURILength;
	char *p_CurrentURIMetaData = NULL;
	int p_CurrentURIMetaDataLength = 0;
	char* _CurrentURIMetaData = "";
	int _CurrentURIMetaDataLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==21 && memcmp(xnode->Name,"SetNextAVTransportURI",21)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==7 && memcmp(xnode->Name,"NextURI",7)==0)
								{
									p_CurrentURILength = ILibReadInnerXML(xnode,&p_CurrentURI);
									p_CurrentURI[p_CurrentURILength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength==15 && memcmp(xnode->Name,"NextURIMetaData",15)==0)
								{
									p_CurrentURIMetaDataLength = ILibReadInnerXML(xnode,&p_CurrentURIMetaData);
									p_CurrentURIMetaData[p_CurrentURIMetaDataLength]=0;
										OK |= 4;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 7)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_CurrentURILength = ILibInPlaceXmlUnEscape(p_CurrentURI);
	_CurrentURI = p_CurrentURI;
	_CurrentURIMetaDataLength = ILibInPlaceXmlUnEscape(p_CurrentURIMetaData);
	_CurrentURIMetaData = p_CurrentURIMetaData;
	if(DMR_FP_AVTransport_SetNextAVTransportURI == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_SetNextAVTransportURI((void*)ReaderObject,_InstanceID,_CurrentURI,_CurrentURIMetaData);
}



void DMR_Dispatch_AVTransport_SetPlayMode(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_NewPlayMode = NULL;
	int p_NewPlayModeLength = 0;
	char* _NewPlayMode = "";
	int _NewPlayModeLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;

	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==11 && memcmp(xnode->Name,"SetPlayMode",11)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==11 && memcmp(xnode->Name,"NewPlayMode",11)==0)
								{
									p_NewPlayModeLength = ILibReadInnerXML(xnode,&p_NewPlayMode);
									p_NewPlayMode[p_NewPlayModeLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 3)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}


/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_NewPlayModeLength = ILibInPlaceXmlUnEscape(p_NewPlayMode);
	_NewPlayMode = p_NewPlayMode;
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {
      if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->AllowedValues[OK]!=NULL)
      {
         if(strcmp(_NewPlayMode,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }
   if(OK!=0)
   {
		//DMR_Response_Error(ReaderObject,402,"Illegal value");
		printf("Play mode not supported\n");
		DMR_Response_Error(ReaderObject,712,"Play mode not supported");
		return;
	}
	if(DMR_FP_AVTransport_SetPlayMode == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_SetPlayMode((void*)ReaderObject,_InstanceID,_NewPlayMode);
}

void DMR_Dispatch_AVTransport_Stop(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Stop",4)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}


	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_AVTransport_Stop == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_AVTransport_Stop((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_ConnectionManager_GetCurrentConnectionIDs(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	int OK = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==23 && memcmp(xnode->Name,"GetCurrentConnectionIDs",23)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength != 0 && xnode->Name!= NULL && xnode->Parent != NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x1;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	if(DMR_FP_ConnectionManager_GetCurrentConnectionIDs == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_ConnectionManager_GetCurrentConnectionIDs((void*)ReaderObject);
}

void DMR_Dispatch_ConnectionManager_GetCurrentConnectionInfo(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	long TempLong;
	int OK = 0;
	char *p_ConnectionID = NULL;
	int p_ConnectionIDLength = 0;
	int _ConnectionID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==24 && memcmp(xnode->Name,"GetCurrentConnectionInfo",24)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==12 && memcmp(xnode->Name,"ConnectionID",12)==0)
								{
									p_ConnectionIDLength = ILibReadInnerXML(xnode,&p_ConnectionID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetLong(p_ConnectionID,p_ConnectionIDLength, &TempLong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_ConnectionID = (int)TempLong;
	if(DMR_FP_ConnectionManager_GetCurrentConnectionInfo == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_ConnectionManager_GetCurrentConnectionInfo((void*)ReaderObject,_ConnectionID);
}

void DMR_Dispatch_ConnectionManager_GetProtocolInfo(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{

	int OK = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==15 && memcmp(xnode->Name,"GetProtocolInfo",15)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength != 0 && xnode->Name!= NULL && xnode->Parent != NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x1;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

	if(DMR_FP_ConnectionManager_GetProtocolInfo == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_ConnectionManager_GetProtocolInfo((void*)ReaderObject);
}

void DMR_Dispatch_RenderingControl_GetBrightness(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==13 && memcmp(xnode->Name,"GetBrightness",13)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_RenderingControl_GetBrightness == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_GetBrightness((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_RenderingControl_GetContrast(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==11 && memcmp(xnode->Name,"GetContrast",11)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_RenderingControl_GetContrast == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_GetContrast((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_RenderingControl_GetMute(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_Channel = NULL;
	int p_ChannelLength = 0;
	char* _Channel = "";
	int _ChannelLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==7 && memcmp(xnode->Name,"GetMute",7)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==7 && memcmp(xnode->Name,"Channel",7)==0)
								{
									p_ChannelLength = ILibReadInnerXML(xnode,&p_Channel);
									p_Channel[p_ChannelLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 3)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_ChannelLength = ILibInPlaceXmlUnEscape(p_Channel);
	_Channel = p_Channel;
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK]!=NULL)
      {
         if(strcmp(_Channel,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }
   if(OK!=0)
   {
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	if(DMR_FP_RenderingControl_GetMute == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_GetMute((void*)ReaderObject,_InstanceID,_Channel);
}

void DMR_Dispatch_RenderingControl_GetVolume(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_Channel = NULL;
	int p_ChannelLength = 0;
	char* _Channel = "";
	int _ChannelLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==9 && memcmp(xnode->Name,"GetVolume",9)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==7 && memcmp(xnode->Name,"Channel",7)==0)
								{
									p_ChannelLength = ILibReadInnerXML(xnode,&p_Channel);
									p_Channel[p_ChannelLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 3)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_ChannelLength = ILibInPlaceXmlUnEscape(p_Channel);
	_Channel = p_Channel;
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK]!=NULL)
      {
         if(strcmp(_Channel,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }
   if(OK!=0)
   {
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	if(DMR_FP_RenderingControl_GetVolume == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_GetVolume((void*)ReaderObject,_InstanceID,_Channel);
}

void DMR_Dispatch_RenderingControl_ListPresets(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==11 && memcmp(xnode->Name,"ListPresets",11)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 1)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	if(DMR_FP_RenderingControl_ListPresets == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_ListPresets((void*)ReaderObject,_InstanceID);
}

void DMR_Dispatch_RenderingControl_SelectPreset(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_PresetName = NULL;
	int p_PresetNameLength = 0;
	char* _PresetName = "";
	int _PresetNameLength;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==12 && memcmp(xnode->Name,"SelectPreset",12)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==10 && memcmp(xnode->Name,"PresetName",10)==0)
								{
									p_PresetNameLength = ILibReadInnerXML(xnode,&p_PresetName);
									p_PresetName[p_PresetNameLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 3)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_PresetNameLength = ILibInPlaceXmlUnEscape(p_PresetName);
	_PresetName = p_PresetName;
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->AllowedValues[OK]!=NULL)
      {
         if(strcmp(_PresetName,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }
   if(OK!=0)
   {
		//DMR_Response_Error(ReaderObject,402,"Illegal value");
		DMR_Response_Error(ReaderObject,701,"Invalid Name");
		return;
	}
	if(DMR_FP_RenderingControl_SelectPreset == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_SelectPreset((void*)ReaderObject,_InstanceID,_PresetName);
}

void DMR_Dispatch_RenderingControl_SetBrightness(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	unsigned long TempULong2;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_DesiredBrightness = NULL;
	int p_DesiredBrightnessLength = 0;
	unsigned short _DesiredBrightness = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==13 && memcmp(xnode->Name,"SetBrightness",13)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==17 && memcmp(xnode->Name,"DesiredBrightness",17)==0)
								{
									p_DesiredBrightnessLength = ILibReadInnerXML(xnode,&p_DesiredBrightness);
										OK |= 2;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 3)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	OK = ILibGetULong(p_DesiredBrightness,p_DesiredBrightnessLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	else
	{
	OK = 0;
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[0]!=NULL)
      {
         ILibGetULong(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[0]), &TempULong2);
		if(TempULong<TempULong2){OK=1;}
      }
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[1]!=NULL)
      {
         ILibGetULong(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[1]), &TempULong2);
		if(TempULong>TempULong2){OK=1;}
      }
      if(OK!=0)
      {
		  DMR_Response_Error(ReaderObject,402,"Illegal value");
		  return;
		}
	_DesiredBrightness = (unsigned short)TempULong;
 }
	if(DMR_FP_RenderingControl_SetBrightness == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_SetBrightness((void*)ReaderObject,_InstanceID,_DesiredBrightness);
}

void DMR_Dispatch_RenderingControl_SetContrast(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	unsigned long TempULong2;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_DesiredContrast = NULL;
	int p_DesiredContrastLength = 0;
	unsigned short _DesiredContrast = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==11 && memcmp(xnode->Name,"SetContrast",11)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==15 && memcmp(xnode->Name,"DesiredContrast",15)==0)
								{
									p_DesiredContrastLength = ILibReadInnerXML(xnode,&p_DesiredContrast);
										OK |= 2;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 3)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	OK = ILibGetULong(p_DesiredContrast,p_DesiredContrastLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	else
	{
	OK = 0;
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[0]!=NULL)
      {
         ILibGetULong(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[0]), &TempULong2);
		if(TempULong<TempULong2){OK=1;}
      }
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[1]!=NULL)
      {
         ILibGetULong(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[1]), &TempULong2);
		if(TempULong>TempULong2){OK=1;}
      }
      if(OK!=0)
      {
		  DMR_Response_Error(ReaderObject,402,"Illegal value");
		  return;
		}
	_DesiredContrast = (unsigned short)TempULong;
 }
	if(DMR_FP_RenderingControl_SetContrast == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_SetContrast((void*)ReaderObject,_InstanceID,_DesiredContrast);
}

void DMR_Dispatch_RenderingControl_SetMute(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_Channel = NULL;
	int p_ChannelLength = 0;
	char* _Channel = "";
	int _ChannelLength;
	char *p_DesiredMute = NULL;
	int p_DesiredMuteLength = 0;
	int _DesiredMute = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==7 && memcmp(xnode->Name,"SetMute",7)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==7 && memcmp(xnode->Name,"Channel",7)==0)
								{
									p_ChannelLength = ILibReadInnerXML(xnode,&p_Channel);
									p_Channel[p_ChannelLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength==11 && memcmp(xnode->Name,"DesiredMute",11)==0)
								{
									p_DesiredMuteLength = ILibReadInnerXML(xnode,&p_DesiredMute);
										OK |= 4;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 7)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_ChannelLength = ILibInPlaceXmlUnEscape(p_Channel);
	_Channel = p_Channel;
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK]!=NULL)
      {
         if(strcmp(_Channel,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }
   if(OK!=0)
   {
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	OK=0;
	if(p_DesiredMuteLength==2)
	{
		if(strncasecmp(p_DesiredMute,"no",2)==0)
		{
			OK = 1;
			_DesiredMute = 0;
		}
	}
	if(p_DesiredMuteLength==3)
	{
		if(strncasecmp(p_DesiredMute,"yes",3)==0)
		{
			OK = 1;
			_DesiredMute = 1;
		}
	}
	if(p_DesiredMuteLength==4)
	{
		if(strncasecmp(p_DesiredMute,"true",4)==0)
		{
			OK = 1;
			_DesiredMute = 1;
		}
	}
	if(p_DesiredMuteLength==5)
	{
		if(strncasecmp(p_DesiredMute,"false",5)==0)
		{
			OK = 1;
			_DesiredMute = 0;
		}
	}
	if(p_DesiredMuteLength==1)
	{
		if(memcmp(p_DesiredMute,"0",1)==0)
		{
			OK = 1;
			_DesiredMute = 0;
		}
		if(memcmp(p_DesiredMute,"1",1)==0)
		{
			OK = 1;
			_DesiredMute = 1;
		}
	}
	if(OK==0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	if(DMR_FP_RenderingControl_SetMute == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_SetMute((void*)ReaderObject,_InstanceID,_Channel,_DesiredMute);
}

void DMR_Dispatch_RenderingControl_SetVolume(char *buffer, int offset, int bufferLength, struct ILibWebServer_Session *ReaderObject)
{
	unsigned long TempULong;
	unsigned long TempULong2;
	int OK = 0;
	char *p_InstanceID = NULL;
	int p_InstanceIDLength = 0;
	unsigned int _InstanceID = 0;
	char *p_Channel = NULL;
	int p_ChannelLength = 0;
	char* _Channel = "";
	int _ChannelLength;
	char *p_DesiredVolume = NULL;
	int p_DesiredVolumeLength = 0;
	unsigned short _DesiredVolume = 0;
	struct ILibXMLNode *xnode = ILibParseXML(buffer,offset,bufferLength);
	struct ILibXMLNode *root = xnode;
	if(ILibProcessXMLNodeList(root)!=0)
	{
/* The XML is not well formed! */
      ILibDestructXMLNodeList(root);
	DMR_Response_Error(ReaderObject,501,"Invalid XML");
	return;
	}
	while(xnode!=NULL)
	{
		if(xnode->StartTag!=0 && xnode->NameLength==8 && memcmp(xnode->Name,"Envelope",8)==0)
		{
			// Envelope
			xnode = xnode->Next;
			while(xnode!=NULL)
			{
				if(xnode->StartTag!=0 && xnode->NameLength==4 && memcmp(xnode->Name,"Body",4)==0)
				{
					// Body
					xnode = xnode->Next;
					while(xnode!=NULL)
					{
						if(xnode->StartTag!=0 && xnode->NameLength==9 && memcmp(xnode->Name,"SetVolume",9)==0)
						{
							// Inside the interesting part of the SOAP
							xnode = xnode->Next;
							while(xnode!=NULL)
							{
								if(xnode->NameLength==10 && memcmp(xnode->Name,"InstanceID",10)==0)
								{
									p_InstanceIDLength = ILibReadInnerXML(xnode,&p_InstanceID);
										OK |= 1;
								}
								else if(xnode->NameLength==7 && memcmp(xnode->Name,"Channel",7)==0)
								{
									p_ChannelLength = ILibReadInnerXML(xnode,&p_Channel);
									p_Channel[p_ChannelLength]=0;
										OK |= 2;
								}
								else if(xnode->NameLength==13 && memcmp(xnode->Name,"DesiredVolume",13)==0)
								{
									p_DesiredVolumeLength = ILibReadInnerXML(xnode,&p_DesiredVolume);
										OK |= 4;
								}
								else if(xnode->NameLength != 0 && xnode->Name!= NULL)
								{
									printf("invalid Argument %s\n",__FUNCTION__);
									OK  |= 0x8000;
								}
								if(xnode->Peer==NULL)
								{
									xnode = xnode->Parent;
									break;
								}
								else
								{
									xnode = xnode->Peer;
								}
							}
						}
						if(xnode!=NULL)
						{
							if(xnode->Peer==NULL)
							{
								xnode = xnode->Parent;
								break;
							}
							else
							{
								xnode = xnode->Peer;
							}
						}
					}
				}
				if(xnode!=NULL)
				{
					if(xnode->Peer==NULL)
					{
						xnode = xnode->Parent;
						break;
					}
					else
					{
						xnode = xnode->Peer;
					}
				}
			}
		}
		if(xnode!=NULL){xnode = xnode->Peer;}
	}
	ILibDestructXMLNodeList(root);
	if (OK != 7)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}

/* Type Checking */
   OK = ILibGetULong(p_InstanceID,p_InstanceIDLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	_InstanceID = (unsigned int)TempULong;
	_ChannelLength = ILibInPlaceXmlUnEscape(p_Channel);
	_Channel = p_Channel;
	for(OK=0;OK<DMR__StateVariable_AllowedValues_MAX;++OK)
   {
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK]!=NULL)
      {
         if(strcmp(_Channel,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[OK])==0)
         {
            OK=0;
            break;
         }
      }
      else
      {
         break;
      }
   }
   if(OK!=0)
   {
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	OK = ILibGetULong(p_DesiredVolume,p_DesiredVolumeLength, &TempULong);
	if(OK!=0)
	{
		DMR_Response_Error(ReaderObject,402,"Illegal value");
		return;
	}
	else
	{
	OK = 0;
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[0]!=NULL)
      {
         ILibGetULong(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[0]), &TempULong2);
		if(TempULong<TempULong2){OK=1;}
      }
      if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[1]!=NULL)
      {
         ILibGetULong(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[1]), &TempULong2);
		if(TempULong>TempULong2){OK=1;}
      }
      if(OK!=0)
      {
		  DMR_Response_Error(ReaderObject,402,"Illegal value");
		  return;
		}
	_DesiredVolume = (unsigned short)TempULong;
 }
	if(DMR_FP_RenderingControl_SetVolume == NULL)
		DMR_Response_Error(ReaderObject,501,"No Function Handler");
	else
		DMR_FP_RenderingControl_SetVolume((void*)ReaderObject,_InstanceID,_Channel,_DesiredVolume);
}


int DMR_ProcessPOST(struct ILibWebServer_Session *session, struct packetheader* header, char *bodyBuffer, int offset, int bodyBufferLength)
{
   struct packetheader_field_node *f = header->FirstField;
   char* HOST;
   char* SOAPACTION = NULL;
   int SOAPACTIONLength = 0;
   struct parser_result *r,*r2;
   struct parser_result_field *prf;

   int RetVal = 0;

   //printf("#############ProcessPOST IP=%s\n",inet_ntoa(header->Source->sin_addr));
   //
   // Iterate through all the HTTP Headers
   //
   while(f!=NULL)
   {
      if(f->FieldLength==4 && strncasecmp(f->Field,"HOST",4)==0)
      {
         HOST = f->FieldData;
      }
      else if(f->FieldLength==10 && strncasecmp(f->Field,"SOAPACTION",10)==0)
      {
         r = ILibParseString(f->FieldData,0,f->FieldDataLength,"#",1);
         SOAPACTION = r->LastResult->data;
         SOAPACTIONLength = r->LastResult->datalength-1;
         ILibDestructParserResults(r);
      }
      else if(f->FieldLength==10 && strncasecmp(f->Field,"USER-AGENT",10)==0)
      {
         // Check UPnP version of the Control Point which invoked us
         r = ILibParseString(f->FieldData,0,f->FieldDataLength," ",1);
         prf = r->FirstResult;
         while(prf!=NULL)
         {
            if(prf->datalength>5 && memcmp(prf->data,"UPnP/",5)==0)
            {
               r2 = ILibParseString(prf->data+5,0,prf->datalength-5,".",1);
               r2->FirstResult->data[r2->FirstResult->datalength]=0;
               r2->LastResult->data[r2->LastResult->datalength]=0;
               if(atoi(r2->FirstResult->data)==1 && atoi(r2->LastResult->data)>0)
               {
                  session->Reserved9=1;
               }
               ILibDestructParserResults(r2);
            }
            prf = prf->NextResult;
         }
         ILibDestructParserResults(r);
      }
      f = f->NextField;
   }

   if(header->DirectiveObjLength==20 && memcmp((header->DirectiveObj)+1,"AVTransport/control",19)==0)
	{
		 if(SOAPACTIONLength==26 && memcmp(SOAPACTION,"GetCurrentTransportActions",26)==0)
		{
			DMR_Dispatch_AVTransport_GetCurrentTransportActions(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==21 && memcmp(SOAPACTION,"GetDeviceCapabilities",21)==0)
		{
			DMR_Dispatch_AVTransport_GetDeviceCapabilities(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==12 && memcmp(SOAPACTION,"GetMediaInfo",12)==0)
		{
			DMR_Dispatch_AVTransport_GetMediaInfo(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==15 && memcmp(SOAPACTION,"GetPositionInfo",15)==0)
		{
			DMR_Dispatch_AVTransport_GetPositionInfo(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==16 && memcmp(SOAPACTION,"GetTransportInfo",16)==0)
		{
			DMR_Dispatch_AVTransport_GetTransportInfo(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==20 && memcmp(SOAPACTION,"GetTransportSettings",20)==0)
		{
			DMR_Dispatch_AVTransport_GetTransportSettings(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==4 && memcmp(SOAPACTION,"Next",4)==0)
		{
			DMR_Dispatch_AVTransport_Next(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==5 && memcmp(SOAPACTION,"Pause",5)==0)
		{
			DMR_Dispatch_AVTransport_Pause(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==4 && memcmp(SOAPACTION,"Play",4)==0)
		{
			DMR_Dispatch_AVTransport_Play(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==8 && memcmp(SOAPACTION,"Previous",8)==0)
		{
			DMR_Dispatch_AVTransport_Previous(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==4 && memcmp(SOAPACTION,"Seek",4)==0)
		{
			DMR_Dispatch_AVTransport_Seek(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==17 && memcmp(SOAPACTION,"SetAVTransportURI",17)==0)
		{
			DMR_Dispatch_AVTransport_SetAVTransportURI(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==21 && memcmp(SOAPACTION,"SetNextAVTransportURI",21)==0)
		{
			#ifndef DLNADMRCTT
			DMR_Dispatch_AVTransport_SetNextAVTransportURI(bodyBuffer, offset, bodyBufferLength, session);
			#else
			RetVal=1;
			#endif
		}
		else if(SOAPACTIONLength==11 && memcmp(SOAPACTION,"SetPlayMode",11)==0)
		{
			DMR_Dispatch_AVTransport_SetPlayMode(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==4 && memcmp(SOAPACTION,"Stop",4)==0)
		{
			DMR_Dispatch_AVTransport_Stop(bodyBuffer, offset, bodyBufferLength, session);
		}
		else
		{
			RetVal=1;
		}
	}
	else if(header->DirectiveObjLength==26 && memcmp((header->DirectiveObj)+1,"ConnectionManager/control",25)==0)
	{
		 if(SOAPACTIONLength==23 && memcmp(SOAPACTION,"GetCurrentConnectionIDs",23)==0)
		{
			DMR_Dispatch_ConnectionManager_GetCurrentConnectionIDs(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==24 && memcmp(SOAPACTION,"GetCurrentConnectionInfo",24)==0)
		{
			DMR_Dispatch_ConnectionManager_GetCurrentConnectionInfo(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==15 && memcmp(SOAPACTION,"GetProtocolInfo",15)==0)
		{
			DMR_Dispatch_ConnectionManager_GetProtocolInfo(bodyBuffer, offset, bodyBufferLength, session);
		}
		else
		{
			RetVal=1;
		}
	}
	else if(header->DirectiveObjLength==25 && memcmp((header->DirectiveObj)+1,"RenderingControl/control",24)==0)
	{
		if(SOAPACTIONLength==7 && memcmp(SOAPACTION,"GetMute",7)==0)
		{
			DMR_Dispatch_RenderingControl_GetMute(bodyBuffer, offset, bodyBufferLength, session);
		}
#if defined(INCLUDE_FEATURE_DISPLAY)
		else if(SOAPACTIONLength==13 && memcmp(SOAPACTION,"GetBrightness",13)==0)
		{
			DMR_Dispatch_RenderingControl_GetBrightness(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==11 && memcmp(SOAPACTION,"GetContrast",11)==0)
		{
			DMR_Dispatch_RenderingControl_GetContrast(bodyBuffer, offset, bodyBufferLength, session);
		}
#endif
		else if(SOAPACTIONLength==9 && memcmp(SOAPACTION,"GetVolume",9)==0)
		{
			DMR_Dispatch_RenderingControl_GetVolume(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==11 && memcmp(SOAPACTION,"ListPresets",11)==0)
		{
			DMR_Dispatch_RenderingControl_ListPresets(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==12 && memcmp(SOAPACTION,"SelectPreset",12)==0)
		{
			DMR_Dispatch_RenderingControl_SelectPreset(bodyBuffer, offset, bodyBufferLength, session);
		}
#if defined(INCLUDE_FEATURE_DISPLAY)
		else if(SOAPACTIONLength==13 && memcmp(SOAPACTION,"SetBrightness",13)==0)
		{
			DMR_Dispatch_RenderingControl_SetBrightness(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==11 && memcmp(SOAPACTION,"SetContrast",11)==0)
		{
			DMR_Dispatch_RenderingControl_SetContrast(bodyBuffer, offset, bodyBufferLength, session);
		}
#endif
		else if(SOAPACTIONLength==7 && memcmp(SOAPACTION,"SetMute",7)==0)
		{
			DMR_Dispatch_RenderingControl_SetMute(bodyBuffer, offset, bodyBufferLength, session);
		}
		else if(SOAPACTIONLength==9 && memcmp(SOAPACTION,"SetVolume",9)==0)
		{
			DMR_Dispatch_RenderingControl_SetVolume(bodyBuffer, offset, bodyBufferLength, session);
		}
		else
		{
			RetVal=1;
		}
	}
	else
	{
		RetVal=1;
	}


   return(RetVal);
}

struct SubscriberInfo* DMR_RemoveSubscriberInfo(struct SubscriberInfo **Head, int *TotalSubscribers,char* SID, int SIDLength)
{
   struct SubscriberInfo *info = *Head;
   while(info!=NULL)
   {
      if(info->SIDLength==SIDLength && memcmp(info->SID,SID,SIDLength)==0)
      {
         if ( info->Previous )
         info->Previous->Next = info->Next;
         else
         *Head = info->Next;
         if ( info->Next )
         info->Next->Previous = info->Previous;
         break;
      }
      info = info->Next;

   }
   if(info!=NULL)
   {
      info->Previous = NULL;
      info->Next = NULL;
      --(*TotalSubscribers);
   }
   return(info);
}

#define SAFEFREE(obj)	if(obj) {free(obj); obj=0;}

#define DMR_DestructSubscriberInfo(info)\
{\
   SAFEFREE(info->Path);\
   SAFEFREE(info->SID);\
   SAFEFREE(info);\
}

#define DMR_DestructEventObject(EvObject)\
{\
   SAFEFREE(EvObject->PacketBody);\
   SAFEFREE(EvObject);\
}

#define DMR_DestructEventDataObject(EvData)\
{\
   SAFEFREE(EvData);\
}
void DMR_ExpireSubscriberInfo(struct DMR_DataObject *d, struct SubscriberInfo *info)
{
   struct SubscriberInfo *t = info;
   while(t->Previous!=NULL)
   {
      t = t->Previous;
   }
   if(d->HeadSubscriberPtr_AVTransport==t)
	{
		--(d->NumberOfSubscribers_AVTransport);
	}
	else if(d->HeadSubscriberPtr_ConnectionManager==t)
	{
		--(d->NumberOfSubscribers_ConnectionManager);
	}
	else if(d->HeadSubscriberPtr_RenderingControl==t)
	{
		--(d->NumberOfSubscribers_RenderingControl);
	}


   if(info->Previous!=NULL)
   {
      // This is not the Head
      info->Previous->Next = info->Next;
      if(info->Next!=NULL)
      {
         info->Next->Previous = info->Previous;
      }
   }
   else
   {
      // This is the Head
      if(d->HeadSubscriberPtr_AVTransport==info)
	{
		d->HeadSubscriberPtr_AVTransport = info->Next;
		if(info->Next!=NULL)
		{
			info->Next->Previous = NULL;
		}
	}
	else if(d->HeadSubscriberPtr_ConnectionManager==info)
	{
		d->HeadSubscriberPtr_ConnectionManager = info->Next;
		if(info->Next!=NULL)
		{
			info->Next->Previous = NULL;
		}
	}
	else if(d->HeadSubscriberPtr_RenderingControl==info)
	{
		d->HeadSubscriberPtr_RenderingControl = info->Next;
		if(info->Next!=NULL)
		{
			info->Next->Previous = NULL;
		}
	}
	else
	{
		// Error
		return;
	}

   }
   --info->RefCount;
   if(info->RefCount==0)
   {
      DMR_DestructSubscriberInfo(info);
   }
}

int DMR_SubscriptionExpired(struct SubscriberInfo *info)
{
   int RetVal = 0;

   struct timeval tv;
   gettimeofday(&tv,NULL);
   if((info->RenewByTime).tv_sec < tv.tv_sec) {RetVal = -1;}

   return(RetVal);
}

void DMR_GetInitialEventBody_AVTransport(struct DMR_DataObject *DMR_Object,char ** body, int *bodylength)
{
	int TempLength;
	TempLength = (int)(25+(int)strlen(DMR_Object->AVTransport_LastChange));
	*body = (char*)malloc(sizeof(char)*TempLength);
	*bodylength = sprintf(*body,"LastChange>%s</LastChange",DMR_Object->AVTransport_LastChange);
}
void DMR_GetInitialEventBody_ConnectionManager(struct DMR_DataObject *DMR_Object,char ** body, int *bodylength)
{
	int TempLength;
	TempLength = (int)(177+(int)strlen(DMR_Object->ConnectionManager_SourceProtocolInfo)+(int)strlen(DMR_Object->ConnectionManager_SinkProtocolInfo)+(int)strlen(DMR_Object->ConnectionManager_CurrentConnectionIDs));
	*body = (char*)malloc(sizeof(char)*TempLength);
	*bodylength = sprintf(*body,"SourceProtocolInfo>%s</SourceProtocolInfo></e:property><e:property><SinkProtocolInfo>%s</SinkProtocolInfo></e:property><e:property><CurrentConnectionIDs>%s</CurrentConnectionIDs",DMR_Object->ConnectionManager_SourceProtocolInfo,DMR_Object->ConnectionManager_SinkProtocolInfo,DMR_Object->ConnectionManager_CurrentConnectionIDs);
}
void DMR_GetInitialEventBody_RenderingControl(struct DMR_DataObject *DMR_Object,char ** body, int *bodylength)
{
	int TempLength;
	TempLength = (int)(25+(int)strlen(DMR_Object->RenderingControl_LastChange));
	*body = (char*)malloc(sizeof(char)*TempLength);
	*bodylength = sprintf(*body,"LastChange>%s</LastChange",DMR_Object->RenderingControl_LastChange);
}


void DMR_ProcessUNSUBSCRIBE(struct packetheader *header, struct ILibWebServer_Session *session)
{
   char* SID = NULL;
   int SIDLength = 0;
   struct SubscriberInfo *Info;
   struct packetheader_field_node *f;
   char* packet = (char*)malloc(sizeof(char)*50);
   int packetlength;

   char* Timeout = NULL;
   int TimeoutLength = 0;
   char* URL = NULL;
   int URLLength = 0;
   char* NotificationType = NULL;
   int NotificationTypeLength;


   //
   // Iterate through all the HTTP headers
   //
   f = header->FirstField;
   while(f!=NULL)
   {
      if(f->FieldLength==3 && strncasecmp(f->Field,"SID",3)==0)
      {
         //
         // Get the Subscription ID
         //
         SID = f->FieldData;
         SIDLength = f->FieldDataLength;
      }
      //yunfeng add
      else if(f->FieldLength==2 && strncasecmp(f->Field,"NT",2)==0)
      {
         //
         // Get the Notificatio Type
         //
         NotificationType = f->FieldData;
         NotificationTypeLength = f->FieldDataLength;
      }
      else if(f->FieldLength==8 && strncasecmp(f->Field,"Callback",8)==0)
      {
         //
         // Get the Callback URL
         //
         URL = f->FieldData;
         URLLength = f->FieldDataLength;
      }
      else if(f->FieldLength==7 && strncasecmp(f->Field,"Timeout",7)==0)
      {
         //
         // Get the requested timeout value
         //
         Timeout = f->FieldData;
         TimeoutLength = f->FieldDataLength;
      }

      f = f->NextField;
   }
   sem_wait(&(((struct DMR_DataObject*)session->User)->EventLock));
   if(header->DirectiveObjLength==18 && memcmp(header->DirectiveObj + 1,"AVTransport/event",17)==0)
	{
		Info = DMR_RemoveSubscriberInfo(&(((struct DMR_DataObject*)session->User)->HeadSubscriberPtr_AVTransport),&(((struct DMR_DataObject*)session->User)->NumberOfSubscribers_AVTransport),SID,SIDLength);
		if(Info!=NULL)
		{
			--Info->RefCount;
			if(Info->RefCount==0)
			{
				DMR_DestructSubscriberInfo(Info);
			}
			if(NotificationType || URL)
      		{
         		printf("NotificationType or URL != NULL\n");
				packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",400,"Bad Request");
         		ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
      		}else{

				packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",200,"OK");
				ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
			}
		}
		else
		{
			packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",412,"Invalid SID");
			ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
		}
	}
	else if(header->DirectiveObjLength==24 && memcmp(header->DirectiveObj + 1,"ConnectionManager/event",23)==0)
	{
		Info = DMR_RemoveSubscriberInfo(&(((struct DMR_DataObject*)session->User)->HeadSubscriberPtr_ConnectionManager),&(((struct DMR_DataObject*)session->User)->NumberOfSubscribers_ConnectionManager),SID,SIDLength);
		if(Info!=NULL)
		{
			--Info->RefCount;
			if(Info->RefCount==0)
			{
				DMR_DestructSubscriberInfo(Info);
			}
			if(NotificationType || URL)
            {
                printf("NotificationType or URL != NULL\n");
                packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",400,"Bad Request");
                ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
            }else{

                packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",200,"OK");
                ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
            }
		}
		else
		{
			packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",412,"Invalid SID");
			ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
		}
	}
	else if(header->DirectiveObjLength==23 && memcmp(header->DirectiveObj + 1,"RenderingControl/event",22)==0)
	{
		Info = DMR_RemoveSubscriberInfo(&(((struct DMR_DataObject*)session->User)->HeadSubscriberPtr_RenderingControl),&(((struct DMR_DataObject*)session->User)->NumberOfSubscribers_RenderingControl),SID,SIDLength);
		if(Info!=NULL)
		{
			--Info->RefCount;
			if(Info->RefCount==0)
			{
				DMR_DestructSubscriberInfo(Info);
			}
			if(NotificationType || URL)
            {
                printf("NotificationType or URL != NULL\n");
                packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",400,"Bad Request");
                ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
            }else{

                packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",200,"OK");
                ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
            }
		}
		else
		{
			packetlength = sprintf(packet,"HTTP/1.1 %d %s\r\nContent-Length: 0\r\n\r\n",412,"Invalid SID");
			ILibWebServer_Send_Raw(session,packet,packetlength,0,1);
		}
	}

   sem_post(&(((struct DMR_DataObject*)session->User)->EventLock));
}



char *Rfc1123_DateTimeNow()
{
	static const char *DAY_NAMES[] =
  { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	static const char *MONTH_NAMES[] =
  { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
    const int RFC1123_TIME_LEN = 29;
    time_t t;
    struct tm *tm;
    char * buf = malloc(RFC1123_TIME_LEN+1);

    time(&t);
    tm = gmtime(&t);

    strftime(buf, RFC1123_TIME_LEN+1, "---, %d --- %Y %H:%M:%S GMT", tm);
#ifdef ANDROID_DLNA_WORKAROUND
    memcpy((void *)buf, (void *)DAY_NAMES[tm->tm_wday], 3);
    memcpy((void *)(buf+8), (void *)MONTH_NAMES[tm->tm_mon], 3);
#else
#ifndef RUN_ON_MAGELLAN //FIX ME for arm tool chain
    memcpy((void *)buf, (void *)DAY_NAMES[tm->tm_wday], 3);
    memcpy((void *)(buf+8), (void *)MONTH_NAMES[tm->tm_mon], 3);
#endif
#endif
    return buf;
}

void DMR_TryToSubscribe(char* ServiceName, long Timeout, char* URL, int URLLength,struct ILibWebServer_Session *session)
{
   int *TotalSubscribers = NULL;
   struct SubscriberInfo **HeadPtr = NULL;
   struct SubscriberInfo *NewSubscriber,*TempSubscriber;
   int SIDNumber,rnumber;
   char *SID;
   char *TempString;
   int TempStringLength;
   char *TempString2;
   long TempLong;
   char *packet;
   int packetlength;
   char* path;

   char* escapedURI;
   int escapedURILength;

   char *packetbody = NULL;
   int packetbodyLength;

   struct parser_result *p;
   struct parser_result *p2;

   char* date;


   struct DMR_DataObject *dataObject = (struct DMR_DataObject*)session->User;

   if(strncmp(ServiceName,"AVTransport",11)==0)
	{
		TotalSubscribers = &(dataObject->NumberOfSubscribers_AVTransport);
		HeadPtr = &(dataObject->HeadSubscriberPtr_AVTransport);
	}
	if(strncmp(ServiceName,"ConnectionManager",17)==0)
	{
		TotalSubscribers = &(dataObject->NumberOfSubscribers_ConnectionManager);
		HeadPtr = &(dataObject->HeadSubscriberPtr_ConnectionManager);
	}
	if(strncmp(ServiceName,"RenderingControl",16)==0)
	{
		TotalSubscribers = &(dataObject->NumberOfSubscribers_RenderingControl);
		HeadPtr = &(dataObject->HeadSubscriberPtr_RenderingControl);
	}


   if(*HeadPtr!=NULL)
   {
      NewSubscriber = *HeadPtr;
      while(NewSubscriber!=NULL)
      {
         if(DMR_SubscriptionExpired(NewSubscriber)!=0)
         {
            TempSubscriber = NewSubscriber->Next;
            NewSubscriber = DMR_RemoveSubscriberInfo(HeadPtr,TotalSubscribers,NewSubscriber->SID,NewSubscriber->SIDLength);
            DMR_DestructSubscriberInfo(NewSubscriber);
            NewSubscriber = TempSubscriber;
         }
         else
         {
            NewSubscriber = NewSubscriber->Next;
         }
      }
   }
   //
   // The Maximum number of subscribers can be bounded
   //
   if(*TotalSubscribers<10)
   {
      NewSubscriber = (struct SubscriberInfo*)malloc(sizeof(struct SubscriberInfo));
      memset(NewSubscriber,0,sizeof(struct SubscriberInfo));


      //
      // The SID must be globally unique, so lets generate it using
      // a bunch of random hex characters
      //
      SID = (char*)malloc(43);
      memset(SID,0,43);    // modified the memset length from 38 to 43, 20090518, yuyu
      sprintf(SID,"uuid:");
      for(SIDNumber=5;SIDNumber<=12;++SIDNumber)
      {
         rnumber = rand()%16;
         sprintf(SID+SIDNumber,"%x",rnumber);
      }
      sprintf(SID+SIDNumber,"-");
      for(SIDNumber=14;SIDNumber<=17;++SIDNumber)
      {
         rnumber = rand()%16;
         sprintf(SID+SIDNumber,"%x",rnumber);
      }
      sprintf(SID+SIDNumber,"-");
      for(SIDNumber=19;SIDNumber<=22;++SIDNumber)
      {
         rnumber = rand()%16;
         sprintf(SID+SIDNumber,"%x",rnumber);
      }
      sprintf(SID+SIDNumber,"-");
      for(SIDNumber=24;SIDNumber<=27;++SIDNumber)
      {
         rnumber = rand()%16;
         sprintf(SID+SIDNumber,"%x",rnumber);
      }
      sprintf(SID+SIDNumber,"-");
      for(SIDNumber=29;SIDNumber<=40;++SIDNumber)
      {
         rnumber = rand()%16;
         sprintf(SID+SIDNumber,"%x",rnumber);
      }

      p = ILibParseString(URL,0,URLLength,"://",3);
      if(p->NumResults==1)
      {
         ILibWebServer_Send_Raw(session,"HTTP/1.1 412 Precondition Failed\r\nContent-Length: 0\r\n\r\n",55,1,1);
         ILibDestructParserResults(p);
         free(SID);           //make up memory release, 20090518, yuyu
         free(NewSubscriber); //make up memory release, 20090518, yuyu
         return;
      }
      TempString = p->LastResult->data;
      TempStringLength = p->LastResult->datalength;
      ILibDestructParserResults(p);
      p = ILibParseString(TempString,0,TempStringLength,"/",1);
      p2 = ILibParseString(p->FirstResult->data,0,p->FirstResult->datalength,":",1);
      TempString2 = (char*)malloc(1+sizeof(char)*p2->FirstResult->datalength);
      memcpy(TempString2,p2->FirstResult->data,p2->FirstResult->datalength);
      TempString2[p2->FirstResult->datalength] = '\0';
      NewSubscriber->Address = inet_addr(TempString2);
      if(p2->NumResults==1)
      {
         NewSubscriber->Port = 80;
	 if(TempStringLength==p2->FirstResult->datalength)
	 {
		 path = (char*)malloc(2);
		 memcpy(path,"/",1);
		 path[1] = '\0';
	 }
	 else
	 {
		 path = (char*)malloc(1+TempStringLength - p2->FirstResult->datalength -1);
		 memcpy(path,TempString + p2->FirstResult->datalength,TempStringLength - p2->FirstResult->datalength -1);
		 path[TempStringLength - p2->FirstResult->datalength - 1] = '\0';
	 }
         NewSubscriber->Path = path;
         NewSubscriber->PathLength = (int)strlen(path);
      }
      else
      {
         ILibGetLong(p2->LastResult->data,p2->LastResult->datalength,&TempLong);
         NewSubscriber->Port = (unsigned short)TempLong;
         if(TempStringLength==p->FirstResult->datalength)
         {
            path = (char*)malloc(2);
            memcpy(path,"/",1);
            path[1] = '\0';
         }
         else
         {
            path = (char*)malloc(1+TempStringLength - p->FirstResult->datalength -1);
            memcpy(path,TempString + p->FirstResult->datalength,TempStringLength - p->FirstResult->datalength -1);
            path[TempStringLength - p->FirstResult->datalength -1] = '\0';
         }
         NewSubscriber->Path = path;
         NewSubscriber->PathLength = (int)strlen(path);
      }
      ILibDestructParserResults(p);
      ILibDestructParserResults(p2);
      free(TempString2);


      escapedURI = (char*)malloc(ILibHTTPEscapeLength(NewSubscriber->Path));
      escapedURILength = ILibHTTPEscape(escapedURI,NewSubscriber->Path);

      free(NewSubscriber->Path);
      NewSubscriber->Path = escapedURI;
      NewSubscriber->PathLength = escapedURILength;


      NewSubscriber->RefCount = 1;
      NewSubscriber->Disposing = 0;
      NewSubscriber->Previous = NULL;
      NewSubscriber->SID = SID;
      NewSubscriber->SIDLength = (int)strlen(SID);
      NewSubscriber->SEQ = 0;

      //
      // Determine what the subscription renewal cycle is
      //

      gettimeofday(&(NewSubscriber->RenewByTime),NULL);
      (NewSubscriber->RenewByTime).tv_sec += (int)Timeout;

      NewSubscriber->Next = *HeadPtr;
      if(*HeadPtr!=NULL) {(*HeadPtr)->Previous = NewSubscriber;}
      *HeadPtr = NewSubscriber;
      ++(*TotalSubscribers);
      LVL3DEBUG(printf("\r\n\r\nSubscribed [%s] %d.%d.%d.%d:%d FOR %d Duration\r\n",NewSubscriber->SID,(NewSubscriber->Address)&0xFF,(NewSubscriber->Address>>8)&0xFF,(NewSubscriber->Address>>16)&0xFF,(NewSubscriber->Address>>24)&0xFF,NewSubscriber->Port,Timeout);)

      LVL3DEBUG(printf("TIMESTAMP: %d <%d>\r\n\r\n",(NewSubscriber->RenewByTime).tv_sec-Timeout,NewSubscriber);)

     // packet = (char*)malloc(134 + (int)strlen(SID) + (int)strlen(DMR_PLATFORM) + 4);
	  date = Rfc1123_DateTimeNow();
	  packet = (char*)malloc(134  + 8 + (int)strlen(date) + (int)strlen(SID) + (int)strlen(DMR_PLATFORM) + 4);
      packetlength = sprintf(packet,"HTTP/1.1 200 OK\r\nDATE: %s\r\nSERVER: %s, UPnP/1.0, Intel MicroStack/1.0.2718\r\nSID: %s\r\nTIMEOUT: Second-%ld\r\nContent-Length: 0\r\n\r\n",date,DMR_PLATFORM,SID,Timeout);
	  free(date);
      if(strcmp(ServiceName,"AVTransport")==0)
	{
		DMR_GetInitialEventBody_AVTransport(dataObject,&packetbody,&packetbodyLength);
	}
	else if(strcmp(ServiceName,"ConnectionManager")==0)
	{
		DMR_GetInitialEventBody_ConnectionManager(dataObject,&packetbody,&packetbodyLength);
	}
	else if(strcmp(ServiceName,"RenderingControl")==0)
	{
		DMR_GetInitialEventBody_RenderingControl(dataObject,&packetbody,&packetbodyLength);
	}

      if (packetbody != NULL)	    {
         ILibWebServer_Send_Raw(session,packet,packetlength,0,1);

         DMR_SendEvent_Body(dataObject,packetbody,packetbodyLength,NewSubscriber);
         free(packetbody);
      }
   }
   else
   {
      /* Too many subscribers */
      ILibWebServer_Send_Raw(session,"HTTP/1.1 412 Too Many Subscribers\r\nContent-Length: 0\r\n\r\n",56,1,1);
   }
}

void DMR_SubscribeEvents(char* path,int pathlength,char* Timeout,int TimeoutLength,char* URL,int URLLength,struct ILibWebServer_Session* session)
{
   long TimeoutVal;
   char* buffer = (char*)malloc(1+sizeof(char)*pathlength);

   //yunfeng
   int ret = ILibGetLong(Timeout,TimeoutLength,&TimeoutVal);
   if(ret != -1)
   {
		#ifndef DLNADMRCTT
   		if(TimeoutVal <= 1800)
			TimeoutVal	= 7200;
		#endif
   }
   else
   {
   		TimeoutVal = 7200;
   }
   memcpy(buffer,path,pathlength);
   buffer[pathlength] = '\0';
   free(buffer);

  // if(TimeoutVal>DMR__MAX_SUBSCRIPTION_TIMEOUT) {TimeoutVal=DMR__MAX_SUBSCRIPTION_TIMEOUT;}

   if(pathlength==18 && memcmp(path+1,"AVTransport/event",17)==0)
	{
		DMR_TryToSubscribe("AVTransport",TimeoutVal,URL,URLLength,session);
	}
else if(pathlength==24 && memcmp(path+1,"ConnectionManager/event",23)==0)
	{
		DMR_TryToSubscribe("ConnectionManager",TimeoutVal,URL,URLLength,session);
	}
else if(pathlength==23 && memcmp(path+1,"RenderingControl/event",22)==0)
	{
		DMR_TryToSubscribe("RenderingControl",TimeoutVal,URL,URLLength,session);
	}
	else
	{
		ILibWebServer_Send_Raw(session,"HTTP/1.1 412 Invalid Service Name\r\nContent-Length: 0\r\n\r\n",56,1,1);
	}

}

void DMR_RenewEvents(char* path,int pathlength,char *_SID,int SIDLength, char* Timeout, int TimeoutLength, struct ILibWebServer_Session *ReaderObject)
{
   struct SubscriberInfo *info = NULL;
   long TimeoutVal;

   struct timeval tv;

   char* packet;
   int packetlength;
   char* SID = (char*)malloc(SIDLength+1);
   memcpy(SID,_SID,SIDLength);
   SID[SIDLength] ='\0';

   LVL3DEBUG(gettimeofday(&tv,NULL);)
   LVL3DEBUG(printf("\r\n\r\nTIMESTAMP: %d\r\n",tv.tv_sec);)

   LVL3DEBUG(printf("SUBSCRIBER [%s] attempting to Renew Events for %s Duration [",SID,Timeout);)

   if(pathlength==18 && memcmp(path+1,"AVTransport/event",17)==0)
	{
		info = ((struct DMR_DataObject*)ReaderObject->User)->HeadSubscriberPtr_AVTransport;
	}
else if(pathlength==24 && memcmp(path+1,"ConnectionManager/event",23)==0)
	{
		info = ((struct DMR_DataObject*)ReaderObject->User)->HeadSubscriberPtr_ConnectionManager;
	}
else if(pathlength==23 && memcmp(path+1,"RenderingControl/event",22)==0)
	{
		info = ((struct DMR_DataObject*)ReaderObject->User)->HeadSubscriberPtr_RenderingControl;
	}


   //
   // Find this SID in the subscriber list, and recalculate
   // the expiration timeout
   //
   while(info!=NULL && strcmp(info->SID,SID)!=0)
   {
      info = info->Next;
   }
   if(info!=NULL)
   {
      ILibGetLong(Timeout,TimeoutLength,&TimeoutVal);
      if(TimeoutVal>DMR__MAX_SUBSCRIPTION_TIMEOUT) {TimeoutVal=DMR__MAX_SUBSCRIPTION_TIMEOUT;}

      gettimeofday(&tv,NULL);

	  printf("tvsec=%d RenewbyTime=%d\n",tv.tv_sec, (info->RenewByTime).tv_sec);
	  if(tv.tv_sec > (info->RenewByTime).tv_sec )
	  {
		printf("FAILED]\r\n\r\n");
      	ILibWebServer_Send_Raw(ReaderObject,"HTTP/1.1 412 Precondition Failed\r\nContent-Length: 0\r\n\r\n",55,1,1);
	  	free(SID);
		return;
	  }

      (info->RenewByTime).tv_sec = tv.tv_sec + TimeoutVal;

      packet = (char*)malloc(134 + (int)strlen(SID) + 4);
      packetlength = sprintf(packet,"HTTP/1.1 200 OK\r\nSERVER: %s, UPnP/1.0, Intel MicroStack/1.0.2718\r\nSID: %s\r\nTIMEOUT: Second-%ld\r\nContent-Length: 0\r\n\r\n",DMR_PLATFORM,SID,TimeoutVal);
      ILibWebServer_Send_Raw(ReaderObject,packet,packetlength,0,1);
      LVL3DEBUG(printf("OK] {%d} <%d>\r\n\r\n",TimeoutVal,info);)
   }
   else
   {
      LVL3DEBUG(printf("FAILED]\r\n\r\n");)
      ILibWebServer_Send_Raw(ReaderObject,"HTTP/1.1 412 Precondition Failed\r\nContent-Length: 0\r\n\r\n",55,1,1);
   }
   free(SID);
}

void DMR_ProcessSUBSCRIBE(struct packetheader *header, struct ILibWebServer_Session *session)
{
   char* SID = NULL;
   int SIDLength = 0;
   char* Timeout = NULL;
   int TimeoutLength = 0;
   char* URL = NULL;
   int URLLength = 0;
   struct parser_result *p;
   char* NotificationType = NULL;
   int NotificationTypeLength;

   struct packetheader_field_node *f;

   //
   // Iterate through all the HTTP Headers
   //
   f = header->FirstField;
   while(f!=NULL)
   {
      if(f->FieldLength==3 && strncasecmp(f->Field,"SID",3)==0)
      {
         //
         // Get the Subscription ID
         //
         SID = f->FieldData;
         SIDLength = f->FieldDataLength;
      }
	  //yunfeng add
	  else if(f->FieldLength==2 && strncasecmp(f->Field,"NT",2)==0)
      {
         //
         // Get the Notificatio Type
         //
         NotificationType = f->FieldData;
         NotificationTypeLength = f->FieldDataLength;
      }
      else if(f->FieldLength==8 && strncasecmp(f->Field,"Callback",8)==0)
      {
         //
         // Get the Callback URL
         //
         URL = f->FieldData;
         URLLength = f->FieldDataLength;
      }
      else if(f->FieldLength==7 && strncasecmp(f->Field,"Timeout",7)==0)
      {
         //
         // Get the requested timeout value
         //
         Timeout = f->FieldData;
         TimeoutLength = f->FieldDataLength;
      }

      f = f->NextField;
   }
   if(Timeout==NULL)
   {
      //
      // It a timeout wasn't specified, force it to a specific value
      //
      Timeout = "7200";
      TimeoutLength = 4;
   }
   else
   {
      p = ILibParseString(Timeout,0,TimeoutLength,"-",1);
      if(p->NumResults==2)
      {
         Timeout = p->LastResult->data;
         TimeoutLength = p->LastResult->datalength;
         if(TimeoutLength==8 && strncasecmp(Timeout,"INFINITE",8)==0)
         {
            //
            // Infinite timeouts will cause problems, so we don't allow it
            //
            Timeout = "7200";
            TimeoutLength = 4;
         }
      }
      else
      {
         Timeout = "7200";
         TimeoutLength = 4;
      }
      ILibDestructParserResults(p);
   }

   if(SID==NULL)
   {
      //
      // If not SID was specified, this is a subscription request
      //
      /* yunfeng
	  check if it is a valid subscription
	  if NotificationType!= upnp:event
	  this is  a invalid subscription request
	  */
      if(!NotificationType || strncasecmp(NotificationType,"upnp:event",10))
      {
		 ILibWebServer_Send_Raw(session,"HTTP/1.1 412 Precondition Failed\r\nContent-Length: 0\r\n\r\n",55,1,1);
		 return;
      }

      /* Subscribe */
      DMR_SubscribeEvents(header->DirectiveObj,header->DirectiveObjLength,Timeout,TimeoutLength,URL,URLLength,session);
   }
   else
   {
      //
      // If a SID was specified, it is a renewal request for an existing subscription
      //


	  /* yunfeng
	  check if it is a valid subscription
	  if NotificationType or Callback != NULL
	  this is  a invalid subscription request
	  */
      if(NotificationType || URL)
      {
      	 printf("NotificationType or URL != NULL\n");
      	 ILibWebServer_Send_Raw(session,"HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n",55-8,1,1);
         return;
      }




      /* Renew */
      DMR_RenewEvents(header->DirectiveObj,header->DirectiveObjLength,SID,SIDLength,Timeout,TimeoutLength,session);
   }
}


void DMR_StreamDescriptionDocument_SCPD(struct ILibWebServer_Session *session, int StartActionList, char *buffer, int offset, int length, int DoneActionList, int Done)
{
   if(StartActionList)
   {
      ILibWebServer_StreamBody(session,"<?xml version=\"1.0\" encoding=\"utf-8\" ?><scpd xmlns=\"urn:schemas-upnp-org:service-1-0\"><specVersion><major>1</major><minor>0</minor></specVersion><actionList>",157,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   }
   if(buffer!=NULL)
   {
      ILibWebServer_StreamBody(session,buffer+offset,length,ILibAsyncSocket_MemoryOwnership_USER,0);
   }
   if(DoneActionList)
   {
      ILibWebServer_StreamBody(session,"</actionList><serviceStateTable>",32,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   }
   if(Done)
   {
      ILibWebServer_StreamBody(session,"</serviceStateTable></scpd>",27,ILibAsyncSocket_MemoryOwnership_STATIC,1);
   }
}


void DMR_ProcessHTTPPacket(struct ILibWebServer_Session *session, struct packetheader* header, char *bodyBuffer, int offset, int bodyBufferLength)
{


   int i;
   struct packetheader_field_node *f;
   char* RequiredLang = NULL;
   int RequiredLangLen;

   char *packet;
   char *date;
   #if defined(WIN32) || defined(_WIN32_WCE)
   char *responseHeader = "\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #elif defined(__SYMBIAN32__)
   char *responseHeader = "\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #else
   char *responseHeader = "\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #endif


	#if defined(WIN32) || defined(_WIN32_WCE)
   char *responseHeader2 = "\r\nCONTENT-LANGUAGE:  en\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #elif defined(__SYMBIAN32__)
   char *responseHeader2 = "\r\nCONTENT-LANGUAGE:  en\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #else
   char *responseHeader2 = "\r\nCONTENT-LANGUAGE:  en\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #endif

   char *errorTemplate = "HTTP/1.1 %d %s\r\nServer: %s, UPnP/1.0, Intel MicroStack/1.0.2718\r\nContent-Length: 0\r\n\r\n";
   char *errorPacket;
   int errorPacketLength;
   char *buffer;


   LVL3DEBUG(errorPacketLength=ILibGetRawPacket(header,&errorPacket);)
   LVL3DEBUG(printf("%s\r\n",errorPacket);)
   LVL3DEBUG(free(errorPacket);)


   if(header->DirectiveLength==4 && memcmp(header->Directive,"HEAD",4)==0)
   {
   	  printf("Http Head\r\n");
      if(header->DirectiveObjLength==1 && memcmp(header->DirectiveObj,"/",1)==0)
      {
         //
         // A HEAD request for the device description document.
         // We stream the document back, so we don't return content length or anything
         // because the actual response won't have it either
         //
         ILibWebServer_StreamHeader_Raw(session,200,"OK",responseHeader,1);
         ILibWebServer_StreamBody(session,NULL,0,ILibAsyncSocket_MemoryOwnership_STATIC,1);
      }

      else if(header->DirectiveObjLength==21 && memcmp((header->DirectiveObj)+1,"AVTransport/scpd.xml",20)==0)
	{
		ILibWebServer_StreamHeader_Raw(session,200,"OK",responseHeader,1);
		ILibWebServer_StreamBody(session,NULL,0,ILibAsyncSocket_MemoryOwnership_STATIC,1);
	}
	else if(header->DirectiveObjLength==27 && memcmp((header->DirectiveObj)+1,"ConnectionManager/scpd.xml",26)==0)
	{
		ILibWebServer_StreamHeader_Raw(session,200,"OK",responseHeader,1);
		ILibWebServer_StreamBody(session,NULL,0,ILibAsyncSocket_MemoryOwnership_STATIC,1);
	}
	else if(header->DirectiveObjLength==26 && memcmp((header->DirectiveObj)+1,"RenderingControl/scpd.xml",25)==0)
	{
		ILibWebServer_StreamHeader_Raw(session,200,"OK",responseHeader,1);
		ILibWebServer_StreamBody(session,NULL,0,ILibAsyncSocket_MemoryOwnership_STATIC,1);
	}

      else
      {
         //
         // A HEAD request for something we don't have
         //
         errorPacket = (char*)malloc(128);
         errorPacketLength = sprintf(errorPacket,errorTemplate,404,"File Not Found",DMR_PLATFORM);
         ILibWebServer_Send_Raw(session,errorPacket,errorPacketLength,0,1);
      }
   }
   else if(header->DirectiveLength==3 && memcmp(header->Directive,"GET",3)==0)
   {

      //
      // Iterate through all the HTTP Headers
      //
      f = header->FirstField;
      while(f!=NULL)
      {
      	//printf("f->Field=%s:%s\n",f->Field,f->FieldData);
        if(f->FieldLength==15 && strncasecmp(f->Field,"ACCEPT-LANGUAGE",15)==0)
        {
           //
           // Get the ACCEPT-LANGUAGE string
           //
           RequiredLang = f->FieldData;
           RequiredLangLen = f->FieldDataLength;
      	   //printf("accept lang=%s\n",RequiredLang);
        }
        f = f->NextField;
      }

	  //printf("header->Version=%s\r\n",header->Version);

      if(!((header->VersionLength==3) && (memcmp(header->Version,"1.0",3)==0)))
      {
      	//not http 1.0  Response need to chunk ,no need content Length
        date = Rfc1123_DateTimeNow();
        if((RequiredLang != NULL) && (RequiredLangLen != 0))
        {
       		packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader2) + 11+36);
			if(header->DirectiveObjLength==21 && memcmp((header->DirectiveObj)+1,"AVTransport/scpd.xml",20)==0)
				sprintf(packet,"%s\r\nDATE: %s",responseHeader2,date);
			else if(header->DirectiveObjLength==27 && memcmp((header->DirectiveObj)+1,"ConnectionManager/scpd.xml",26)==0)
				sprintf(packet,"%s\r\nDATE: %s",responseHeader2,date);
			else  if(header->DirectiveObjLength==26 && memcmp((header->DirectiveObj)+1,"RenderingControl/scpd.xml",25)==0)
				sprintf(packet,"%s\r\nDATE: %s",responseHeader2,date);


        }
        else
        {
        	packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader) + 11+36);
      		if(header->DirectiveObjLength==21 && memcmp((header->DirectiveObj)+1,"AVTransport/scpd.xml",20)==0)
				sprintf(packet,"%s\r\nDATE: %s",responseHeader,date);
			else if(header->DirectiveObjLength==27 && memcmp((header->DirectiveObj)+1,"ConnectionManager/scpd.xml",26)==0)
				sprintf(packet,"%s\r\nDATE: %s",responseHeader,date);
			else  if(header->DirectiveObjLength==26 && memcmp((header->DirectiveObj)+1,"RenderingControl/scpd.xml",25)==0)
				sprintf(packet,"%s\r\nDATE: %s",responseHeader,date);

		}
        free(date);
      }
      else
      {
      	 date = Rfc1123_DateTimeNow();
        if((RequiredLang != NULL) && (RequiredLangLen != 0))
        {
       		packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader2) + 11+36);
			if(header->DirectiveObjLength==21 && memcmp((header->DirectiveObj)+1,"AVTransport/scpd.xml",20)==0)
			{
			#ifndef DLNADMRCTT
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader2,13111+489,date);//change 13096->13111 for adding playspeed allowedvalue 
			#else
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader2,13111,date);
			#endif
			}
			else if(header->DirectiveObjLength==27 && memcmp((header->DirectiveObj)+1,"ConnectionManager/scpd.xml",26)==0)
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader2,3488,date);
			else  if(header->DirectiveObjLength==26 && memcmp((header->DirectiveObj)+1,"RenderingControl/scpd.xml",25)==0)
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader2,5553,date);
		}
        else
        {
        	packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader) + 11+36);
      		if(header->DirectiveObjLength==21 && memcmp((header->DirectiveObj)+1,"AVTransport/scpd.xml",20)==0)
			{
			#ifndef DLNADMRCTT
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader,13111+489,date);
			#else
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader,13111+15,date);
			#endif
			}
			else if(header->DirectiveObjLength==27 && memcmp((header->DirectiveObj)+1,"ConnectionManager/scpd.xml",26)==0)
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader,3488,date);
			else  if(header->DirectiveObjLength==26 && memcmp((header->DirectiveObj)+1,"RenderingControl/scpd.xml",25)==0)
				sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader,5553,date);
		}
        free(date);
      }

      if(header->DirectiveObjLength==1 && memcmp(header->DirectiveObj,"/",1)==0)
      {
         //
         // A GET Request for the device description document, so lets stream
         // it back to the client
         //

         DMR_StreamDescriptionDocument(session,header);
         free(packet);

      }

      else if(header->DirectiveObjLength==21 && memcmp((header->DirectiveObj)+1,"AVTransport/scpd.xml",20)==0)
	  {
		//printf("\r\nAVTransport packet=%s\r\n",packet);
		ILibWebServer_StreamHeader_Raw(session,200,"OK",packet,0);
		//free(packet);
		DMR_StreamDescriptionDocument_SCPD(session,1,NULL,0,0,0,0);
		buffer = ILibDecompressString((unsigned char*)DMR__ActionTable_AVTransport_Impl.Reserved,DMR__ActionTable_AVTransport_Impl.ReservedXL,DMR__ActionTable_AVTransport_Impl.ReservedUXL);
		if(DMR__Device_MediaRenderer_Impl.AVTransport->GetCurrentTransportActions!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->GetCurrentTransportActions->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->GetCurrentTransportActions->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->GetDeviceCapabilities!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->GetDeviceCapabilities->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->GetDeviceCapabilities->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->GetMediaInfo!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->GetMediaInfo->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->GetMediaInfo->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->GetPositionInfo!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->GetPositionInfo->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->GetPositionInfo->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->GetTransportInfo!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->GetTransportInfo->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->GetTransportInfo->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->GetTransportSettings!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->GetTransportSettings->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->GetTransportSettings->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->Next!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->Next->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->Next->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->Pause!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->Pause->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->Pause->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->Play!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->Play->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->Play->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->Previous!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->Previous->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->Previous->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->Seek!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->Seek->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->Seek->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->SetAVTransportURI!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->SetAVTransportURI->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->SetAVTransportURI->Reserved2,0,0);}

		//to DO  add nexturi description
		#ifndef DLNADMRCTT
		DMR_StreamDescriptionDocument_SCPD(session,0,"<action><name>SetNextAVTransportURI</name><argumentList><argument><name>InstanceID</name><direction>in</direction><relatedStateVariable>A_ARG_TYPE_InstanceID</relatedStateVariable></argument><argument><name>NextURI</name><direction>in</direction><relatedStateVariable>NextAVTransportURI</relatedStateVariable></argument><argument><name>NextURIMetaData</name><direction>in</direction><relatedStateVariable>NextAVTransportURIMetaData</relatedStateVariable></argument></argumentList></action>",0,489,0,0);
		#endif

		if(DMR__Device_MediaRenderer_Impl.AVTransport->SetPlayMode!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->SetPlayMode->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->SetPlayMode->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->Stop!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.AVTransport->Stop->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->Stop->Reserved2,0,0);}
		free(buffer);
		DMR_StreamDescriptionDocument_SCPD(session,0,NULL,0,0,1,0);
		buffer = ILibDecompressString((unsigned char*)DMR__StateVariableTable_AVTransport_Impl.Reserved,DMR__StateVariableTable_AVTransport_Impl.ReservedXL,DMR__StateVariableTable_AVTransport_Impl.ReservedUXL);
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentPlayMode->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordStorageMedium->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_LastChange!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_LastChange->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_LastChange->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_LastChange->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_LastChange->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
#if 0
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved4,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved4L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[0]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<minimum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[0]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</minimum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[1]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<maximum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[1]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</maximum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[2]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<step>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[2],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->MinMaxStep[2]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</step>",7,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved5,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved5L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
#endif
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeTimePosition->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackURI!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackURI->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackURI->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackURI->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackURI->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackDuration!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackDuration->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackDuration->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackDuration->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackDuration->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentRecordQualityMode->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentMediaDuration!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentMediaDuration->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentMediaDuration->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentMediaDuration->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentMediaDuration->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteCounterPosition!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteCounterPosition->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteCounterPosition->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteCounterPosition->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteCounterPosition->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved4,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved4L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[0]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<minimum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[0]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</minimum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[1]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<maximum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[1]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</maximum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[2]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<step>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[2],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->MinMaxStep[2]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</step>",7,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved5,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved5L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RelativeCounterPosition->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);

		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_InstanceID->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURI!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURI->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURI->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURI->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURI->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportState->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackMetaData!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackMetaData->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackMetaData->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackMetaData->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrackMetaData->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURI!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURI->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURI->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURI->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURI->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordQualityModes->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			// vvv Added by yuyu for DLNA 1.5 CTT 7.3.6.1
			ILibWebServer_StreamBody(session,"<allowedValueRange>",19,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[0]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<minimum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[0]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</minimum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[1]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<maximum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[1]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</maximum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[2]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<step>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[2],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->MinMaxStep[2]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</step>",7,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			ILibWebServer_StreamBody(session,"</allowedValueRange>",20,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			// ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.6.1
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTrack->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteTimePosition!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteTimePosition->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteTimePosition->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteTimePosition->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AbsoluteTimePosition->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURIMetaData!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURIMetaData->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURIMetaData->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURIMetaData->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NextAVTransportURIMetaData->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PlaybackStorageMedium->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTransportActions!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTransportActions->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTransportActions->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTransportActions->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_CurrentTransportActions->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_RecordMediumWriteStatus->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossiblePlaybackStorageMedia->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURIMetaData!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURIMetaData->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURIMetaData->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURIMetaData->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_AVTransportURIMetaData->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			// vvv Added by yuyu for DLNA 1.5 CTT 7.3.6.1
			ILibWebServer_StreamBody(session,"<allowedValueRange>",19,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->MinMax[0]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<minimum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->MinMax[0],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->MinMax[0]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</minimum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->MinMax[1]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<maximum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->MinMax[1],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->MinMax[1]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</maximum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			ILibWebServer_StreamBody(session,"</allowedValueRange>",20,ILibAsyncSocket_MemoryOwnership_USER,0);
			// ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.6.1
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_NumberOfTracks->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			// vvv Added by yuyu for DLNA 1.5 CTT 7.3.T.0/7.3.T.1/7.3.T.2
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			//for(i=0;i<DMR__StateVaribale_SeekMode_Limitation;++i)
			// ^^^ Added by yuyu for DLNA 1.5 CTT 7.3.T.0/7.3.T.1/7.3.T.2
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekMode->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekTarget!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekTarget->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekTarget->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekTarget->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_A_ARG_TYPE_SeekTarget->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_PossibleRecordStorageMedia->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportStatus->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved1,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved6,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved7,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved2,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved3,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved8,DMR__Device_MediaRenderer_Impl.AVTransport->StateVar_TransportPlaySpeed->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		free(buffer);
		DMR_StreamDescriptionDocument_SCPD(session,0,NULL,0,0,0,1);
	}
	else if(header->DirectiveObjLength==27 && memcmp((header->DirectiveObj)+1,"ConnectionManager/scpd.xml",26)==0)
	{
		//printf("\r\nConnecttion Manager packet=%s\r\n",packet);
		ILibWebServer_StreamHeader_Raw(session,200,"OK",packet,0);
		//free(packet);
		DMR_StreamDescriptionDocument_SCPD(session,1,NULL,0,0,0,0);
		buffer = ILibDecompressString((unsigned char*)DMR__ActionTable_ConnectionManager_Impl.Reserved,DMR__ActionTable_ConnectionManager_Impl.ReservedXL,DMR__ActionTable_ConnectionManager_Impl.ReservedUXL);
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->GetCurrentConnectionIDs!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.ConnectionManager->GetCurrentConnectionIDs->Reserved,DMR__Device_MediaRenderer_Impl.ConnectionManager->GetCurrentConnectionIDs->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->GetCurrentConnectionInfo!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.ConnectionManager->GetCurrentConnectionInfo->Reserved,DMR__Device_MediaRenderer_Impl.ConnectionManager->GetCurrentConnectionInfo->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->GetProtocolInfo!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.ConnectionManager->GetProtocolInfo->Reserved,DMR__Device_MediaRenderer_Impl.ConnectionManager->GetProtocolInfo->Reserved2,0,0);}
		free(buffer);
		DMR_StreamDescriptionDocument_SCPD(session,0,NULL,0,0,1,0);
		buffer = ILibDecompressString((unsigned char*)DMR__StateVariableTable_ConnectionManager_Impl.Reserved,DMR__StateVariableTable_ConnectionManager_Impl.ReservedXL,DMR__StateVariableTable_ConnectionManager_Impl.ReservedUXL);
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ProtocolInfo!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ProtocolInfo->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ProtocolInfo->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ProtocolInfo->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ProtocolInfo->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved2,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved3,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionStatus->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved6,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved7,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_AVTransportID->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved6,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved7,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_RcsID->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved6,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved7,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionID->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionManager!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionManager->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionManager->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionManager->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_ConnectionManager->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SourceProtocolInfo!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SourceProtocolInfo->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SourceProtocolInfo->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SourceProtocolInfo->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SourceProtocolInfo->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SinkProtocolInfo!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SinkProtocolInfo->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SinkProtocolInfo->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SinkProtocolInfo->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_SinkProtocolInfo->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved2,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved3,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_A_ARG_TYPE_Direction->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_CurrentConnectionIDs!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_CurrentConnectionIDs->Reserved1,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_CurrentConnectionIDs->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_CurrentConnectionIDs->Reserved8,DMR__Device_MediaRenderer_Impl.ConnectionManager->StateVar_CurrentConnectionIDs->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		free(buffer);
		DMR_StreamDescriptionDocument_SCPD(session,0,NULL,0,0,0,1);
	}
	else if(header->DirectiveObjLength==26 && memcmp((header->DirectiveObj)+1,"RenderingControl/scpd.xml",25)==0)
	{
		//printf("\r\nRendering Control packet=%s\r\n",packet);
		ILibWebServer_StreamHeader_Raw(session,200,"OK",packet,0);
		//free(packet);
		DMR_StreamDescriptionDocument_SCPD(session,1,NULL,0,0,0,0);
		buffer = ILibDecompressString((unsigned char*)DMR__ActionTable_RenderingControl_Impl.Reserved,DMR__ActionTable_RenderingControl_Impl.ReservedXL,DMR__ActionTable_RenderingControl_Impl.ReservedUXL);
#if defined(INCLUDE_FEATURE_DISPLAY)
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->GetBrightness!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->GetBrightness->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->GetBrightness->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->GetContrast!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->GetContrast->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->GetContrast->Reserved2,0,0);}
#endif
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->GetMute!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->GetMute->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->GetMute->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->GetVolume!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->GetVolume->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->GetVolume->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->ListPresets!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->ListPresets->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->ListPresets->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->SelectPreset!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->SelectPreset->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->SelectPreset->Reserved2,0,0);}
#if defined(INCLUDE_FEATURE_DISPLAY)
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->SetBrightness!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->SetBrightness->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->SetBrightness->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->SetContrast!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->SetContrast->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->SetContrast->Reserved2,0,0);}
#endif
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->SetMute!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->SetMute->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->SetMute->Reserved2,0,0);}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->SetVolume!=NULL){DMR_StreamDescriptionDocument_SCPD(session,0,buffer,DMR__Device_MediaRenderer_Impl.RenderingControl->SetVolume->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->SetVolume->Reserved2,0,0);}
		free(buffer);
		DMR_StreamDescriptionDocument_SCPD(session,0,NULL,0,0,1,0);
		buffer = ILibDecompressString((unsigned char*)DMR__StateVariableTable_RenderingControl_Impl.Reserved,DMR__StateVariableTable_RenderingControl_Impl.ReservedXL,DMR__StateVariableTable_RenderingControl_Impl.ReservedUXL);
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved4,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved4L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[0]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<minimum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[0]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</minimum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[1]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<maximum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[1]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</maximum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[2]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<step>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[2],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->MinMaxStep[2]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</step>",7,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved5,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved5L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Contrast->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_LastChange!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_LastChange->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_LastChange->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_LastChange->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_LastChange->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved4,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved4L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[0]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<minimum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[0]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</minimum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[1]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<maximum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[1]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</maximum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[2]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<step>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[2],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->MinMaxStep[2]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</step>",7,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved5,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved5L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Brightness->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved4,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved4L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[0]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<minimum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[0],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[0]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</minimum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[1]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<maximum>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[1],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[1]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</maximum>",10,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[2]!=NULL)
			{
				ILibWebServer_StreamBody(session,"<step>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[2],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->MinMaxStep[2]),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,"</step>",7,ILibAsyncSocket_MemoryOwnership_STATIC,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved5,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved5L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Volume->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved6,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved7,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved2,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved3,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_PresetName->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_PresetNameList!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_PresetNameList->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_PresetNameList->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_PresetNameList->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_PresetNameList->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved6,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved7,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_Mute->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}

		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved6,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved7,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_InstanceID->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel!=NULL)
		{
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved1,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved1L,ILibAsyncSocket_MemoryOwnership_USER,0);
			if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->DefaultValue!=NULL)
			{
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved6,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved6L,ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->DefaultValue,(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->DefaultValue),ILibAsyncSocket_MemoryOwnership_USER,0);
				ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved7,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved7L,ILibAsyncSocket_MemoryOwnership_USER,0);
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved2,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved2L,ILibAsyncSocket_MemoryOwnership_USER,0);
			for(i=0;i<DMR__StateVariable_AllowedValues_MAX;++i)
			{
				if(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[i]!=NULL)
				{
					ILibWebServer_StreamBody(session,"<allowedValue>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
					ILibWebServer_StreamBody(session,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[i],(int)strlen(DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->AllowedValues[i]),ILibAsyncSocket_MemoryOwnership_USER,0);
					ILibWebServer_StreamBody(session,"</allowedValue>",15,ILibAsyncSocket_MemoryOwnership_STATIC,0);
				}
			}
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved3,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved3L,ILibAsyncSocket_MemoryOwnership_USER,0);
			ILibWebServer_StreamBody(session,buffer+DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved8,DMR__Device_MediaRenderer_Impl.RenderingControl->StateVar_A_ARG_TYPE_Channel->Reserved8L,ILibAsyncSocket_MemoryOwnership_USER,0);
		}
		free(buffer);
		DMR_StreamDescriptionDocument_SCPD(session,0,NULL,0,0,0,1);
	}

      else
      {
         //
         // A GET Request for something we don't have
         //
         errorPacket = (char*)malloc(128);
         errorPacketLength = sprintf(errorPacket,errorTemplate,404,"File Not Found",DMR_PLATFORM);
         ILibWebServer_Send_Raw(session,errorPacket,errorPacketLength,0,1);
      }
   }
   else if(header->DirectiveLength==4 && memcmp(header->Directive,"POST",4)==0)
   {
      //
      // Defer Control to the POST Handler
      //
      if(DMR_ProcessPOST(session,header,bodyBuffer,offset,bodyBufferLength)!=0)
      {
         //
         // A POST for an action that doesn't exist
         //
         DMR_Response_Error(session,401,"Invalid Action");
      }
   }

   else if(header->DirectiveLength==9 && memcmp(header->Directive,"SUBSCRIBE",9)==0)
   {
      //
      // Subscription Handler
      //
      DMR_ProcessSUBSCRIBE(header,session);
   }
   else if(header->DirectiveLength==11 && memcmp(header->Directive,"UNSUBSCRIBE",11)==0)
   {
      //
      // UnSubscribe Handler
      //
      DMR_ProcessUNSUBSCRIBE(header,session);
   }
   else
   {
      //
      // The client tried something we didn't expect/support
      //
      errorPacket = (char*)malloc(128);
      errorPacketLength = sprintf(errorPacket,errorTemplate,400,"Bad Request",DMR_PLATFORM);
      ILibWebServer_Send_Raw(session,errorPacket,errorPacketLength,ILibAsyncSocket_MemoryOwnership_CHAIN,1);
   }
}
void DMR_FragmentedSendNotify_Destroy(void *data);
void DMR_MasterPreSelect(void* object,void *socketset, void *writeset, void *errorset, int* blocktime)
{
   int i;
   struct DMR_DataObject *DMR_Object = (struct DMR_DataObject*)object;
   struct DMR_FragmentNotifyStruct *f;
   int timeout;

   if(DMR_Object->InitialNotify==0)
   {
      //
      // The initial "HELLO" packets were not sent yet, so lets send them
      //
      DMR_Object->InitialNotify = -1;
      //
      // In case we were interrupted, we need to flush out the caches of
      // all the control points by sending a "byebye" first, to insure
      // control points don't ignore our "hello" packets thinking they are just
      // periodic re-advertisements.
      //
      DMR_SendByeBye(DMR_Object);

      //
      // PacketNumber 0 is the controller, for the rest of the packets. Send
      // one of these to send out an advertisement "group"
      //
      f = (struct DMR_FragmentNotifyStruct*)malloc(sizeof(struct DMR_FragmentNotifyStruct));
      f->packetNumber=0;
      f->upnp = DMR_Object;
      //
      // We need to inject some delay in these packets to space them out,
      // otherwise we could overflow the inbound buffer of the recipient, causing them
      // to lose packets. And UPnP/1.0 control points are not as robust as UPnP/1.1 control points,
      // so they need all the help they can get ;)
      //
      timeout = (int)(0 + ((unsigned short)rand() % (500)));
      do
      {
         f->upnp->InitialNotify = rand();
      }while(f->upnp->InitialNotify==0);
      //
      // Register for the timed callback, to actually send the packet
      //
      //ILibLifeTime_AddEx(f->upnp->WebServerTimer,f,timeout,&DMR_FragmentedSendNotify,&DMR_FragmentedSendNotify_Destroy);
      DMR_FragmentedSendNotify(f);

   }
   if(DMR_Object->UpdateFlag!=0)
   {
      //
      // Somebody told us that we should recheck our IP Address table,
      // as one of them may have changed
      //
      DMR_Object->UpdateFlag = 0;



      /* Clear Sockets */


      //
      // Iterate through all the currently bound IP addresses
      // and release the sockets
      //
      for(i=0;i<DMR_Object->AddressListLength;++i)
      {
         ILibChain_SafeRemove(DMR_Object->Chain,DMR_Object->NOTIFY_SEND_socks[i]);
      }
      free(DMR_Object->NOTIFY_SEND_socks);

      for(i=0;i<DMR_Object->AddressListLength;++i)
      {
         ILibChain_SafeRemove(DMR_Object->Chain,DMR_Object->NOTIFY_RECEIVE_socks[i]);
      }
      free(DMR_Object->NOTIFY_RECEIVE_socks);


      //
      // Fetch a current list of ip addresses
      //
      free(DMR_Object->AddressList);
      DMR_Object->AddressListLength = ILibGetLocalIPAddressList(&(DMR_Object->AddressList));


      //
      // Re-Initialize our SEND socket
      //
      DMR_Object->NOTIFY_SEND_socks = (void**)malloc(sizeof(void*)*(DMR_Object->AddressListLength));
      DMR_Object->NOTIFY_RECEIVE_socks = (void**)malloc(sizeof(void*)*(DMR_Object->AddressListLength));

      //
      // Now that we have a new list of IP addresses, re-initialise everything
      //
      for(i=0;i<DMR_Object->AddressListLength;++i)
      {
         DMR_Object->NOTIFY_SEND_socks[i] = ILibAsyncUDPSocket_Create(
         DMR_Object->Chain,
         UPNP_MAX_SSDP_HEADER_SIZE,
         DMR_Object->AddressList[i],
         0,
         ILibAsyncUDPSocket_Reuse_SHARED,
         NULL,
         NULL,
         DMR_Object);
         ILibAsyncUDPSocket_JoinMulticastGroup(
         DMR_Object->NOTIFY_SEND_socks[i],
         DMR_Object->AddressList[i],
         inet_addr(UPNP_GROUP));

         ILibAsyncUDPSocket_SetMulticastTTL(DMR_Object->NOTIFY_SEND_socks[i],UPNP_SSDP_TTL);

         DMR_Object->NOTIFY_RECEIVE_socks[i] = ILibAsyncUDPSocket_Create(
         DMR_Object->Chain,
         UPNP_MAX_SSDP_HEADER_SIZE,
         0,
         UPNP_PORT,
         ILibAsyncUDPSocket_Reuse_SHARED,
         &DMR_SSDPSink,
         NULL,
         DMR_Object);

         ILibAsyncUDPSocket_JoinMulticastGroup(
         DMR_Object->NOTIFY_RECEIVE_socks[i],
         DMR_Object->AddressList[i],
         inet_addr(UPNP_GROUP));


      }


      //
      // Iterate through all the packet types, and re-broadcast
      //
      for(i=1;i<=6;++i)
      {
         f = (struct DMR_FragmentNotifyStruct*)malloc(sizeof(struct DMR_FragmentNotifyStruct));
         f->packetNumber=i;
         f->upnp = DMR_Object;
         //
         // Inject some random delay, to spread these packets out, to help prevent
         // the inbound buffer of the recipient from overflowing, causing dropped packets.
         //
         timeout = (int)(0 + ((unsigned short)rand() % (500)));
         //ILibLifeTime_AddEx(f->upnp->WebServerTimer,f,timeout,&DMR_FragmentedSendNotify,&DMR_FragmentedSendNotify_Destroy);
         DMR_FragmentedSendNotify(f);
      }
   }
}

void DMR_FragmentedSendNotify_Destroy(void *data)
{
   free(data);
}
void DMR_FragmentedSendNotify(void *data)
{
   struct DMR_FragmentNotifyStruct *FNS = (struct DMR_FragmentNotifyStruct*)data;
   int timeout,timeout2;
   int subsetRange;
   int packetlength;
   char* packet = (char*)malloc(5000);
   int i,i2;
   struct DMR_FragmentNotifyStruct *f;

   if(FNS->packetNumber==0)
   {
      subsetRange = 800; // Make sure all our packets will get out within 5 seconds

      // Send the first "group"
      for(i2=0;i2<6;++i2)
      {
         f = (struct DMR_FragmentNotifyStruct*)malloc(sizeof(struct DMR_FragmentNotifyStruct));
         f->packetNumber=i2+1;
         f->upnp = FNS->upnp;
         timeout2 = (rand() % subsetRange);
         //ILibLifeTime_AddEx(FNS->upnp->WebServerTimer,f,timeout2,&DMR_FragmentedSendNotify,&DMR_FragmentedSendNotify_Destroy);
         DMR_FragmentedSendNotify(f);
      }

      // Now Repeat this "group" after 5 seconds, to insure there is no overlap
      for(i2=0;i2<6;++i2)
      {
         f = (struct DMR_FragmentNotifyStruct*)malloc(sizeof(struct DMR_FragmentNotifyStruct));
         f->packetNumber=i2+1;
         f->upnp = FNS->upnp;
         timeout2 = 5000 + (rand() % subsetRange);
         ILibLifeTime_AddEx(FNS->upnp->WebServerTimer,f,timeout2,&DMR_FragmentedSendNotify,&DMR_FragmentedSendNotify_Destroy);
         //DMR_FragmentedSendNotify(f);
      }

      // Calculate the next transmission window and spread the packets
      timeout = (int)((FNS->upnp->NotifyCycleTime/4) + ((unsigned short)rand() % (FNS->upnp->NotifyCycleTime/2 - FNS->upnp->NotifyCycleTime/4)));
      ILibLifeTime_Add(FNS->upnp->WebServerTimer,FNS,timeout,&DMR_FragmentedSendNotify,&DMR_FragmentedSendNotify_Destroy);
   }

   for(i=0;i<FNS->upnp->AddressListLength;++i)
   {
      ILibAsyncUDPSocket_SetMulticastInterface(FNS->upnp->NOTIFY_SEND_socks[i],FNS->upnp->AddressList[i]);
      switch(FNS->packetNumber)
      {
		case 1:
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,FNS->upnp->AddressList[i],(unsigned short)FNS->upnp->WebSocketPortNumber,0,FNS->upnp->UDN,"::upnp:rootdevice","upnp:rootdevice","",FNS->upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(FNS->upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			break;
		case 2:
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,FNS->upnp->AddressList[i],(unsigned short)FNS->upnp->WebSocketPortNumber,0,FNS->upnp->UDN,"","uuid:",FNS->upnp->UDN,FNS->upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(FNS->upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			break;
		case 3:
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,FNS->upnp->AddressList[i],(unsigned short)FNS->upnp->WebSocketPortNumber,0,FNS->upnp->UDN,"::urn:schemas-upnp-org:device:MediaRenderer:1","urn:schemas-upnp-org:device:MediaRenderer:1","",FNS->upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(FNS->upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			break;
		case 4:
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,FNS->upnp->AddressList[i],(unsigned short)FNS->upnp->WebSocketPortNumber,0,FNS->upnp->UDN,"::urn:schemas-upnp-org:service:AVTransport:1","urn:schemas-upnp-org:service:AVTransport:1","",FNS->upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(FNS->upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			break;
		case 5:
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,FNS->upnp->AddressList[i],(unsigned short)FNS->upnp->WebSocketPortNumber,0,FNS->upnp->UDN,"::urn:schemas-upnp-org:service:ConnectionManager:1","urn:schemas-upnp-org:service:ConnectionManager:1","",FNS->upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(FNS->upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			break;
		case 6:
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,FNS->upnp->AddressList[i],(unsigned short)FNS->upnp->WebSocketPortNumber,0,FNS->upnp->UDN,"::urn:schemas-upnp-org:service:RenderingControl:1","urn:schemas-upnp-org:service:RenderingControl:1","",FNS->upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(FNS->upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			break;
	   }

   }
   free(packet);
   if(FNS->packetNumber!=0)
   {
      free(FNS);
   }
}
void DMR_SendNotify(const struct DMR_DataObject *upnp)
{
   int packetlength;
   char* packet = (char*)malloc(5000);
   int i,i2;
   for(i=0;i<upnp->AddressListLength;++i)
   {
        ILibAsyncUDPSocket_SetMulticastInterface(upnp->NOTIFY_SEND_socks[i],upnp->AddressList[i]);
		for (i2=0;i2<2;i2++)
		{
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,upnp->AddressList[i],(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::upnp:rootdevice","upnp:rootdevice","",upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,upnp->AddressList[i],(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"","uuid:",upnp->UDN,upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,upnp->AddressList[i],(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:device:MediaRenderer:1","urn:schemas-upnp-org:device:MediaRenderer:1","",upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,upnp->AddressList[i],(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:AVTransport:1","urn:schemas-upnp-org:service:AVTransport:1","",upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,upnp->AddressList[i],(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:ConnectionManager:1","urn:schemas-upnp-org:service:ConnectionManager:1","",upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
			DMR_BuildSsdpNotifyPacket(packet,&packetlength,upnp->AddressList[i],(unsigned short)upnp->WebSocketPortNumber,0,upnp->UDN,"::urn:schemas-upnp-org:service:RenderingControl:1","urn:schemas-upnp-org:service:RenderingControl:1","",upnp->NotifyCycleTime);
			ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
		}
   }
   free(packet);
}

#define DMR_BuildSsdpByeByePacket(outpacket,outlength,USN,USNex,NT,NTex,DeviceID)\
{\
   if(DeviceID==0)\
   {\
		*outlength = sprintf(outpacket,"NOTIFY * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nNTS: ssdp:byebye\r\nUSN: uuid:%s%s\r\nNT: %s%s\r\n\r\n",USN,USNex,NT,NTex);\
   }\
   else\
   {\
      if(memcmp(NT,"uuid:",5)==0)\
      {\
			*outlength = sprintf(outpacket,"NOTIFY * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nNTS: ssdp:byebye\r\nUSN: uuid:%s_%d%s\r\nNT: %s%s_%d\r\n\r\n",USN,DeviceID,USNex,NT,NTex,DeviceID);\
	  }\
      else\
      {\
			*outlength = sprintf(outpacket,"NOTIFY * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nNTS: ssdp:byebye\r\nUSN: uuid:%s_%d%s\r\nNT: %s%s\r\nContent-Length: 0\r\n\r\n",USN,DeviceID,USNex,NT,NTex);\
	  }\
   }\
}


void DMR_SendByeBye(const struct DMR_DataObject *upnp)
{

   int packetlength;
   char* packet = (char*)malloc(5000);
   int i, i2;

   for(i=0;i<upnp->AddressListLength;++i)
   {
      ILibAsyncUDPSocket_SetMulticastInterface(upnp->NOTIFY_SEND_socks[i],upnp->AddressList[i]);

      for (i2=0;i2<2;i2++)
      {
	      DMR_BuildSsdpByeByePacket(packet,&packetlength,upnp->UDN,"::upnp:rootdevice","upnp:rootdevice","",0);
		  ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
	      DMR_BuildSsdpByeByePacket(packet,&packetlength,upnp->UDN,"","uuid:",upnp->UDN,0);
	      ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
	      DMR_BuildSsdpByeByePacket(packet,&packetlength,upnp->UDN,"::urn:schemas-upnp-org:device:MediaRenderer:1","urn:schemas-upnp-org:device:MediaRenderer:1","",0);
	      ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
	      DMR_BuildSsdpByeByePacket(packet,&packetlength,upnp->UDN,"::urn:schemas-upnp-org:service:AVTransport:1","urn:schemas-upnp-org:service:AVTransport:1","",0);
	      ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
	      DMR_BuildSsdpByeByePacket(packet,&packetlength,upnp->UDN,"::urn:schemas-upnp-org:service:ConnectionManager:1","urn:schemas-upnp-org:service:ConnectionManager:1","",0);
	      ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
	      DMR_BuildSsdpByeByePacket(packet,&packetlength,upnp->UDN,"::urn:schemas-upnp-org:service:RenderingControl:1","urn:schemas-upnp-org:service:RenderingControl:1","",0);
	      ILibAsyncUDPSocket_SendTo(upnp->NOTIFY_SEND_socks[i],inet_addr(UPNP_GROUP),UPNP_PORT,packet,packetlength,ILibAsyncSocket_MemoryOwnership_USER);
		  //yunfeng send second byebye ssdp  sleep sometime to avoid network congestion
		  if(i2 == 0)sleep(1);
	  }
   }
   free(packet);
}

/*! \fn DMR_Response_Error(const DMR_SessionToken DMR_Token, const int ErrorCode, const char* ErrorMsg)
\brief Responds to the client invocation with a SOAP Fault
\param DMR_Token UPnP token
\param ErrorCode Fault Code
\param ErrorMsg Error Detail
*/
void DMR_Response_Error(const DMR_SessionToken DMR_Token, const int ErrorCode, const char* ErrorMsg)
{
   char* body;
   int bodylength;
   char* head;
   int headlength;
   body = (char*)malloc(395 + (int)strlen(ErrorMsg));
   bodylength = sprintf(body,"<s:Envelope\r\n xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body><s:Fault><faultcode>s:Client</faultcode><faultstring>UPnPError</faultstring><detail><UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\"><errorCode>%d</errorCode><errorDescription>%s</errorDescription></UPnPError></detail></s:Fault></s:Body></s:Envelope>",ErrorCode,ErrorMsg);
   head = (char*)malloc(1024);
   char *date = Rfc1123_DateTimeNow();
   headlength = sprintf(head,"HTTP/1.1 500 Internal\r\nEXT:\r\nCONTENT-TYPE: text/xml; charset=\"utf-8\"\r\nSERVER: REALTEK, UPnP/1.0, Intel MicroStack/1.0.2718\r\nDATE: %s\r\nContent-Length: %d\r\n\r\n",date,bodylength);
   free(date);
   ILibWebServer_Send_Raw((struct ILibWebServer_Session*)DMR_Token,head,headlength,0,0);
   ILibWebServer_Send_Raw((struct ILibWebServer_Session*)DMR_Token,body,bodylength,0,1);
}

/*! \fn DMR_GetLocalInterfaceToHost(const DMR_SessionToken DMR_Token)
\brief When a UPnP request is dispatched, this method determines which ip address actually received this request
\param DMR_Token UPnP token
\returns IP Address
*/
int DMR_GetLocalInterfaceToHost(const DMR_SessionToken DMR_Token)
{
   return(ILibWebServer_GetLocalInterface((struct ILibWebServer_Session*)DMR_Token));
}

void DMR_ResponseGeneric(const DMR_MicroStackToken DMR_Token,const char* ServiceURI,const char* MethodName,const char* Params)
{
   char* packet;
   int packetlength;
   struct ILibWebServer_Session *session = (struct ILibWebServer_Session*)DMR_Token;
   int RVAL=0;

   packet = (char*)malloc(239+strlen(ServiceURI)+strlen(Params)+(strlen(MethodName)*2));
   packetlength = sprintf(packet,"<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:%sResponse xmlns:u=\"%s\">%s</u:%sResponse></s:Body></s:Envelope>",MethodName,ServiceURI,Params,MethodName);
   LVL3DEBUG(printf("SendBody: %s\r\n",packet);)
   #if defined(WIN32) || defined(_WIN32_WCE)
   RVAL=ILibWebServer_StreamHeader_Raw(session,200,"OK","\r\nEXT:\r\nCONTENT-TYPE: text/xml; charset=\"utf-8\"\r\nSERVER: WINDOWS, UPnP/1.0, Intel MicroStack/1.0.2718",1);
   #elif defined(__SYMBIAN32__)
   RVAL=ILibWebServer_StreamHeader_Raw(session,200,"OK","\r\nEXT:\r\nCONTENT-TYPE: text/xml; charset=\"utf-8\"\r\nSERVER: SYMBIAN, UPnP/1.0, Intel MicroStack/1.0.2718",1);
   #else
   char* date =  Rfc1123_DateTimeNow();
   char* headresponse = "\r\nEXT:\r\nCONTENT-TYPE: text/xml; charset=\"utf-8\"\r\nSERVER: REALTEK, UPnP/1.0, Intel MicroStack/1.0.2718";
   char *data = (char*)malloc((int)strlen(date) + (int)strlen(headresponse) + 11 + 24);
#ifndef DLNADMRCTT
   // Don't use the Transfer-Encoding: chunked to send the packet
   struct packetheader *hdr = ILibWebClient_GetHeaderFromDataObject(session->Reserved3);
   if (hdr && hdr->VersionLength==3 && memcmp(hdr->Version,"1.1",3)==0) {
      hdr->Version[2] = '0';
   }

   sprintf(data,"%s\r\nDATE: %s\r\nContent-Length: %d",headresponse,date, packetlength);
#else
   sprintf(data,"%s\r\nDATE: %s",headresponse,date);
#endif
   free(date);
   RVAL=ILibWebServer_StreamHeader_Raw(session,200,"OK",data,0);

   #endif
   if(RVAL!=ILibAsyncSocket_SEND_ON_CLOSED_SOCKET_ERROR && RVAL != ILibWebServer_SEND_RESULTED_IN_DISCONNECT)
   {
      RVAL=ILibWebServer_StreamBody(session,packet,packetlength,0,1);
   }
}

/*! \fn DMR_Response_AVTransport_GetCurrentTransportActions(const DMR_SessionToken DMR_Token, const char* unescaped_Actions)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetCurrentTransportActions
	\param DMR_Token MicroStack token
 \param unescaped_Actions Value of argument Actions \b     Note: Automatically Escaped
*/
void DMR_Response_AVTransport_GetCurrentTransportActions(const DMR_SessionToken DMR_Token, const char* unescaped_Actions)
{
  char* body;
	char *Actions = (char*)malloc(1+ILibXmlEscapeLength(unescaped_Actions));

	ILibXmlEscape(Actions,unescaped_Actions);
  body = (char*)malloc(20+strlen(Actions));
  sprintf(body,"<Actions>%s</Actions>",Actions);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","GetCurrentTransportActions",body);
  free(body);
	free(Actions);
}

/*! \fn DMR_Response_AVTransport_GetDeviceCapabilities(const DMR_SessionToken DMR_Token, const char* unescaped_PlayMedia, const char* unescaped_RecMedia, const char* unescaped_RecQualityModes)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetDeviceCapabilities
	\param DMR_Token MicroStack token
 \param unescaped_PlayMedia Value of argument PlayMedia \b     Note: Automatically Escaped
 \param unescaped_RecMedia Value of argument RecMedia \b     Note: Automatically Escaped
 \param unescaped_RecQualityModes Value of argument RecQualityModes \b     Note: Automatically Escaped
*/
void DMR_Response_AVTransport_GetDeviceCapabilities(const DMR_SessionToken DMR_Token, const char* unescaped_PlayMedia, const char* unescaped_RecMedia, const char* unescaped_RecQualityModes)
{
  char* body;
	char *PlayMedia = (char*)malloc(1+ILibXmlEscapeLength(unescaped_PlayMedia));
	char *RecMedia = (char*)malloc(1+ILibXmlEscapeLength(unescaped_RecMedia));
	char *RecQualityModes = (char*)malloc(1+ILibXmlEscapeLength(unescaped_RecQualityModes));

	ILibXmlEscape(PlayMedia,unescaped_PlayMedia);
	ILibXmlEscape(RecMedia,unescaped_RecMedia);
	ILibXmlEscape(RecQualityModes,unescaped_RecQualityModes);
  body = (char*)malloc(80+strlen(PlayMedia)+strlen(RecMedia)+strlen(RecQualityModes));
  sprintf(body,"<PlayMedia>%s</PlayMedia><RecMedia>%s</RecMedia><RecQualityModes>%s</RecQualityModes>",PlayMedia,RecMedia,RecQualityModes);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","GetDeviceCapabilities",body);
  free(body);
	free(PlayMedia);
	free(RecMedia);
	free(RecQualityModes);
}

/*! \fn DMR_Response_AVTransport_GetMediaInfo(const DMR_SessionToken DMR_Token, const unsigned int NrTracks, const char* unescaped_MediaDuration, const char* unescaped_CurrentURI, const char* unescaped_CurrentURIMetaData, const char* unescaped_NextURI, const char* unescaped_NextURIMetaData, const char* unescaped_PlayMedium, const char* unescaped_RecordMedium, const char* unescaped_WriteStatus)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetMediaInfo
	\param DMR_Token MicroStack token
 \param NrTracks Value of argument NrTracks
 \param unescaped_MediaDuration Value of argument MediaDuration \b     Note: Automatically Escaped
 \param unescaped_CurrentURI Value of argument CurrentURI \b     Note: Automatically Escaped
 \param unescaped_CurrentURIMetaData Value of argument CurrentURIMetaData \b     Note: Automatically Escaped
 \param unescaped_NextURI Value of argument NextURI \b     Note: Automatically Escaped
 \param unescaped_NextURIMetaData Value of argument NextURIMetaData \b     Note: Automatically Escaped
 \param unescaped_PlayMedium Value of argument PlayMedium \b     Note: Automatically Escaped
 \param unescaped_RecordMedium Value of argument RecordMedium \b     Note: Automatically Escaped
 \param unescaped_WriteStatus Value of argument WriteStatus \b     Note: Automatically Escaped
*/
void DMR_Response_AVTransport_GetMediaInfo(const DMR_SessionToken DMR_Token, const unsigned int NrTracks, const char* unescaped_MediaDuration, const char* unescaped_CurrentURI, const char* unescaped_CurrentURIMetaData, const char* unescaped_NextURI, const char* unescaped_NextURIMetaData, const char* unescaped_PlayMedium, const char* unescaped_RecordMedium, const char* unescaped_WriteStatus)
{
  char* body;
	char *MediaDuration = (char*)malloc(1+ILibXmlEscapeLength(unescaped_MediaDuration));
	char *CurrentURI = (char*)malloc(1+ILibXmlEscapeLength(unescaped_CurrentURI));
	//char *CurrentURIMetaData = (char*)malloc(1+ILibXmlEscapeLength(unescaped_CurrentURIMetaData));
	char *NextURI = (char*)malloc(1+ILibXmlEscapeLength(unescaped_NextURI));
	char *NextURIMetaData = (char*)malloc(1+ILibXmlEscapeLength(unescaped_NextURIMetaData));
	char *PlayMedium = (char*)malloc(1+ILibXmlEscapeLength(unescaped_PlayMedium));
	char *RecordMedium = (char*)malloc(1+ILibXmlEscapeLength(unescaped_RecordMedium));
	char *WriteStatus = (char*)malloc(1+ILibXmlEscapeLength(unescaped_WriteStatus));


	ILibXmlEscape(MediaDuration,unescaped_MediaDuration);
	ILibXmlEscape(CurrentURI,unescaped_CurrentURI);
	//ILibXmlEscape(CurrentURIMetaData,unescaped_CurrentURIMetaData);
	ILibXmlEscape(NextURI,unescaped_NextURI);
	ILibXmlEscape(NextURIMetaData,unescaped_NextURIMetaData);
	ILibXmlEscape(PlayMedium,unescaped_PlayMedium);
	ILibXmlEscape(RecordMedium,unescaped_RecordMedium);
	ILibXmlEscape(WriteStatus,unescaped_WriteStatus);
  body = (char*)malloc(265+strlen(MediaDuration)+strlen(CurrentURI)+strlen(unescaped_CurrentURIMetaData)+strlen(NextURI)+strlen(NextURIMetaData)+strlen(PlayMedium)+strlen(RecordMedium)+strlen(WriteStatus));
  sprintf(body,"<NrTracks>%u</NrTracks><MediaDuration>%s</MediaDuration><CurrentURI>%s</CurrentURI><CurrentURIMetaData>%s</CurrentURIMetaData><NextURI>%s</NextURI><NextURIMetaData>%s</NextURIMetaData><PlayMedium>%s</PlayMedium><RecordMedium>%s</RecordMedium><WriteStatus>%s</WriteStatus>",NrTracks,MediaDuration,CurrentURI,unescaped_CurrentURIMetaData,NextURI,NextURIMetaData,PlayMedium,RecordMedium,WriteStatus);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","GetMediaInfo",body);
  free(body);
	free(MediaDuration);
	free(CurrentURI);
	//free(CurrentURIMetaData);
	free(NextURI);
	free(NextURIMetaData);
	free(PlayMedium);
	free(RecordMedium);
	free(WriteStatus);
}

/*! \fn DMR_Response_AVTransport_GetPositionInfo(const DMR_SessionToken DMR_Token, const unsigned int Track, const char* unescaped_TrackDuration, const char* unescaped_TrackMetaData, const char* unescaped_TrackURI, const char* unescaped_RelTime, const char* unescaped_AbsTime, const int RelCount, const int AbsCount)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetPositionInfo
	\param DMR_Token MicroStack token
 \param Track Value of argument Track
 \param unescaped_TrackDuration Value of argument TrackDuration \b     Note: Automatically Escaped
 \param unescaped_TrackMetaData Value of argument TrackMetaData \b     Note: Automatically Escaped
 \param unescaped_TrackURI Value of argument TrackURI \b     Note: Automatically Escaped
 \param unescaped_RelTime Value of argument RelTime \b     Note: Automatically Escaped
 \param unescaped_AbsTime Value of argument AbsTime \b     Note: Automatically Escaped
 \param RelCount Value of argument RelCount
 \param AbsCount Value of argument AbsCount
*/
void DMR_Response_AVTransport_GetPositionInfo(const DMR_SessionToken DMR_Token, const unsigned int Track, const char* unescaped_TrackDuration, const char* unescaped_TrackMetaData, const char* unescaped_TrackURI, const char* unescaped_RelTime, const char* unescaped_AbsTime, const int RelCount, const int AbsCount)
{
  char* body;
	//char *TrackDuration = (char*)malloc(1+ILibXmlEscapeLength(unescaped_TrackDuration));
	char *TrackMetaData = (char*)malloc(1+ILibXmlEscapeLength(unescaped_TrackMetaData));
	char *TrackURI = (char*)malloc(1+ILibXmlEscapeLength(unescaped_TrackURI));
	char *RelTime = (char*)malloc(1+ILibXmlEscapeLength(unescaped_RelTime));
	char *AbsTime = (char*)malloc(1+ILibXmlEscapeLength(unescaped_AbsTime));

	//ILibXmlEscape(TrackDuration,unescaped_TrackDuration);
	ILibXmlEscape(TrackMetaData,unescaped_TrackMetaData);
	ILibXmlEscape(TrackURI,unescaped_TrackURI);
	ILibXmlEscape(RelTime,unescaped_RelTime);
	ILibXmlEscape(AbsTime,unescaped_AbsTime);
  body = (char*)malloc(212+strlen(unescaped_TrackDuration)+strlen(unescaped_TrackMetaData)+strlen(TrackURI)+strlen(RelTime)+strlen(AbsTime));
  sprintf(body,"<Track>%u</Track><TrackDuration>%s</TrackDuration><TrackMetaData>%s</TrackMetaData><TrackURI>%s</TrackURI><RelTime>%s</RelTime><AbsTime>%s</AbsTime><RelCount>%d</RelCount><AbsCount>%d</AbsCount>",Track,unescaped_TrackDuration,unescaped_TrackMetaData,TrackURI,RelTime,AbsTime,RelCount,AbsCount);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","GetPositionInfo",body);
  free(body);
	//free(TrackDuration);
	free(TrackMetaData);
	free(TrackURI);
	free(RelTime);
	free(AbsTime);
}

/*! \fn DMR_Response_AVTransport_GetTransportInfo(const DMR_SessionToken DMR_Token, const char* unescaped_CurrentTransportState, const char* unescaped_CurrentTransportStatus, const char* unescaped_CurrentSpeed)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetTransportInfo
	\param DMR_Token MicroStack token
 \param unescaped_CurrentTransportState Value of argument CurrentTransportState \b     Note: Automatically Escaped
 \param unescaped_CurrentTransportStatus Value of argument CurrentTransportStatus \b     Note: Automatically Escaped
 \param unescaped_CurrentSpeed Value of argument CurrentSpeed \b     Note: Automatically Escaped
*/
void DMR_Response_AVTransport_GetTransportInfo(const DMR_SessionToken DMR_Token, const char* unescaped_CurrentTransportState, const char* unescaped_CurrentTransportStatus, const char* unescaped_CurrentSpeed)
{
  char* body;
	char *CurrentTransportState = (char*)malloc(1+ILibXmlEscapeLength(unescaped_CurrentTransportState));
	char *CurrentTransportStatus = (char*)malloc(1+ILibXmlEscapeLength(unescaped_CurrentTransportStatus));
	char *CurrentSpeed = (char*)malloc(1+ILibXmlEscapeLength(unescaped_CurrentSpeed));

	ILibXmlEscape(CurrentTransportState,unescaped_CurrentTransportState);
	ILibXmlEscape(CurrentTransportStatus,unescaped_CurrentTransportStatus);
	ILibXmlEscape(CurrentSpeed,unescaped_CurrentSpeed);
  body = (char*)malloc(126+strlen(CurrentTransportState)+strlen(CurrentTransportStatus)+strlen(CurrentSpeed));
  sprintf(body,"<CurrentTransportState>%s</CurrentTransportState><CurrentTransportStatus>%s</CurrentTransportStatus><CurrentSpeed>%s</CurrentSpeed>",CurrentTransportState,CurrentTransportStatus,CurrentSpeed);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","GetTransportInfo",body);
  free(body);
	free(CurrentTransportState);
	free(CurrentTransportStatus);
	free(CurrentSpeed);
}

/*! \fn DMR_Response_AVTransport_GetTransportSettings(const DMR_SessionToken DMR_Token, const char* unescaped_PlayMode, const char* unescaped_RecQualityMode)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> GetTransportSettings
	\param DMR_Token MicroStack token
 \param unescaped_PlayMode Value of argument PlayMode \b     Note: Automatically Escaped
 \param unescaped_RecQualityMode Value of argument RecQualityMode \b     Note: Automatically Escaped
*/
void DMR_Response_AVTransport_GetTransportSettings(const DMR_SessionToken DMR_Token, const char* unescaped_PlayMode, const char* unescaped_RecQualityMode)
{
  char* body;
	char *PlayMode = (char*)malloc(1+ILibXmlEscapeLength(unescaped_PlayMode));
	char *RecQualityMode = (char*)malloc(1+ILibXmlEscapeLength(unescaped_RecQualityMode));

	ILibXmlEscape(PlayMode,unescaped_PlayMode);
	ILibXmlEscape(RecQualityMode,unescaped_RecQualityMode);
  body = (char*)malloc(55+strlen(PlayMode)+strlen(RecQualityMode));
  sprintf(body,"<PlayMode>%s</PlayMode><RecQualityMode>%s</RecQualityMode>",PlayMode,RecQualityMode);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","GetTransportSettings",body);
  free(body);
	free(PlayMode);
	free(RecQualityMode);
}

/*! \fn DMR_Response_AVTransport_Next(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Next
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_Next(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","Next","");
}

/*! \fn DMR_Response_AVTransport_Pause(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Pause
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_Pause(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","Pause","");
}

/*! \fn DMR_Response_AVTransport_Play(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Play
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_Play(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","Play","");
}

/*! \fn DMR_Response_AVTransport_Previous(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Previous
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_Previous(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","Previous","");
}

/*! \fn DMR_Response_AVTransport_Seek(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Seek
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_Seek(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","Seek","");
}

/*! \fn DMR_Response_AVTransport_SetAVTransportURI(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> SetAVTransportURI
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_SetAVTransportURI(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","SetAVTransportURI","");
}
void DMR_Response_AVTransport_SetNextAVTransportURI(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","SetNextAVTransportURI","");
}

/*! \fn DMR_Response_AVTransport_SetPlayMode(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> SetPlayMode
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_SetPlayMode(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","SetPlayMode","");
}

/*! \fn DMR_Response_AVTransport_Stop(const DMR_SessionToken DMR_Token)
	\brief Response Method for AVTransport >> urn:schemas-upnp-org:service:AVTransport:1 >> Stop
	\param DMR_Token MicroStack token
*/
void DMR_Response_AVTransport_Stop(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:AVTransport:1","Stop","");
}

/*! \fn DMR_Response_ConnectionManager_GetCurrentConnectionIDs(const DMR_SessionToken DMR_Token, const char* unescaped_ConnectionIDs)
	\brief Response Method for ConnectionManager >> urn:schemas-upnp-org:service:ConnectionManager:1 >> GetCurrentConnectionIDs
	\param DMR_Token MicroStack token
 \param unescaped_ConnectionIDs Value of argument ConnectionIDs \b     Note: Automatically Escaped
*/
void DMR_Response_ConnectionManager_GetCurrentConnectionIDs(const DMR_SessionToken DMR_Token, const char* unescaped_ConnectionIDs)
{
  char* body;
	char *ConnectionIDs = (char*)malloc(1+ILibXmlEscapeLength(unescaped_ConnectionIDs));

	ILibXmlEscape(ConnectionIDs,unescaped_ConnectionIDs);
  body = (char*)malloc(32+strlen(ConnectionIDs));
  sprintf(body,"<ConnectionIDs>%s</ConnectionIDs>",ConnectionIDs);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:ConnectionManager:1","GetCurrentConnectionIDs",body);
  free(body);
	free(ConnectionIDs);
}

/*! \fn DMR_Response_ConnectionManager_GetCurrentConnectionInfo(const DMR_SessionToken DMR_Token, const int RcsID, const int AVTransportID, const char* unescaped_ProtocolInfo, const char* unescaped_PeerConnectionManager, const int PeerConnectionID, const char* unescaped_Direction, const char* unescaped_Status)
	\brief Response Method for ConnectionManager >> urn:schemas-upnp-org:service:ConnectionManager:1 >> GetCurrentConnectionInfo
	\param DMR_Token MicroStack token
 \param RcsID Value of argument RcsID
 \param AVTransportID Value of argument AVTransportID
 \param unescaped_ProtocolInfo Value of argument ProtocolInfo \b     Note: Automatically Escaped
 \param unescaped_PeerConnectionManager Value of argument PeerConnectionManager \b     Note: Automatically Escaped
 \param PeerConnectionID Value of argument PeerConnectionID
 \param unescaped_Direction Value of argument Direction \b     Note: Automatically Escaped
 \param unescaped_Status Value of argument Status \b     Note: Automatically Escaped
*/
void DMR_Response_ConnectionManager_GetCurrentConnectionInfo(const DMR_SessionToken DMR_Token, const int RcsID, const int AVTransportID, const char* unescaped_ProtocolInfo, const char* unescaped_PeerConnectionManager, const int PeerConnectionID, const char* unescaped_Direction, const char* unescaped_Status)
{
  char* body;
	char *ProtocolInfo = (char*)malloc(1+ILibXmlEscapeLength(unescaped_ProtocolInfo));
	char *PeerConnectionManager = (char*)malloc(1+ILibXmlEscapeLength(unescaped_PeerConnectionManager));
	char *Direction = (char*)malloc(1+ILibXmlEscapeLength(unescaped_Direction));
	char *Status = (char*)malloc(1+ILibXmlEscapeLength(unescaped_Status));

	ILibXmlEscape(ProtocolInfo,unescaped_ProtocolInfo);
	ILibXmlEscape(PeerConnectionManager,unescaped_PeerConnectionManager);
	ILibXmlEscape(Direction,unescaped_Direction);
	ILibXmlEscape(Status,unescaped_Status);
  body = (char*)malloc(233+strlen(ProtocolInfo)+strlen(PeerConnectionManager)+strlen(Direction)+strlen(Status));
  sprintf(body,"<RcsID>%d</RcsID><AVTransportID>%d</AVTransportID><ProtocolInfo>%s</ProtocolInfo><PeerConnectionManager>%s</PeerConnectionManager><PeerConnectionID>%d</PeerConnectionID><Direction>%s</Direction><Status>%s</Status>",RcsID,AVTransportID,ProtocolInfo,PeerConnectionManager,PeerConnectionID,Direction,Status);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:ConnectionManager:1","GetCurrentConnectionInfo",body);
  free(body);
	free(ProtocolInfo);
	free(PeerConnectionManager);
	free(Direction);
	free(Status);
}

/*! \fn DMR_Response_ConnectionManager_GetProtocolInfo(const DMR_SessionToken DMR_Token, const char* unescaped_Source, const char* unescaped_Sink)
	\brief Response Method for ConnectionManager >> urn:schemas-upnp-org:service:ConnectionManager:1 >> GetProtocolInfo
	\param DMR_Token MicroStack token
 \param unescaped_Source Value of argument Source \b     Note: Automatically Escaped
 \param unescaped_Sink Value of argument Sink \b     Note: Automatically Escaped
*/
void DMR_Response_ConnectionManager_GetProtocolInfo(const DMR_SessionToken DMR_Token, const char* unescaped_Source, const char* unescaped_Sink)
{
  char* body;
	char *Source = (char*)malloc(1+ILibXmlEscapeLength(unescaped_Source));
	char *Sink = (char*)malloc(1+ILibXmlEscapeLength(unescaped_Sink));
	printf("DMR_MicroStack.c Invoke: DMR_ConnectionManager_GetProtocolInfo();\r\n");
	printf("DMR_MicroStack.c unescaped_Sink:%s\r\n", unescaped_Sink);

	ILibXmlEscape(Source,unescaped_Source);
	ILibXmlEscape(Sink,unescaped_Sink);
  body = (char*)malloc(31+strlen(Source)+strlen(Sink));
  sprintf(body,"<Source>%s</Source><Sink>%s</Sink>",Source,Sink);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:ConnectionManager:1","GetProtocolInfo",body);
  free(body);
	free(Source);
	free(Sink);
}

/*! \fn DMR_Response_RenderingControl_GetBrightness(const DMR_SessionToken DMR_Token, const unsigned short CurrentBrightness)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetBrightness
	\param DMR_Token MicroStack token
 \param CurrentBrightness Value of argument CurrentBrightness
*/
void DMR_Response_RenderingControl_GetBrightness(const DMR_SessionToken DMR_Token, const unsigned short CurrentBrightness)
{
  char* body;

  body = (char*)malloc(46);
  sprintf(body,"<CurrentBrightness>%u</CurrentBrightness>",CurrentBrightness);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","GetBrightness",body);
  free(body);
}

/*! \fn DMR_Response_RenderingControl_GetContrast(const DMR_SessionToken DMR_Token, const unsigned short CurrentContrast)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetContrast
	\param DMR_Token MicroStack token
 \param CurrentContrast Value of argument CurrentContrast
*/
void DMR_Response_RenderingControl_GetContrast(const DMR_SessionToken DMR_Token, const unsigned short CurrentContrast)
{
  char* body;

  body = (char*)malloc(42);
  sprintf(body,"<CurrentContrast>%u</CurrentContrast>",CurrentContrast);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","GetContrast",body);
  free(body);
}

/*! \fn DMR_Response_RenderingControl_GetMute(const DMR_SessionToken DMR_Token, const int CurrentMute)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetMute
	\param DMR_Token MicroStack token
 \param CurrentMute Value of argument CurrentMute
*/
void DMR_Response_RenderingControl_GetMute(const DMR_SessionToken DMR_Token, const int CurrentMute)
{
  char* body;

  body = (char*)malloc(29);
  sprintf(body,"<CurrentMute>%d</CurrentMute>",(CurrentMute!=0?1:0));
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","GetMute",body);
  free(body);
}

/*! \fn DMR_Response_RenderingControl_GetVolume(const DMR_SessionToken DMR_Token, const unsigned short CurrentVolume)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> GetVolume
	\param DMR_Token MicroStack token
 \param CurrentVolume Value of argument CurrentVolume
*/
void DMR_Response_RenderingControl_GetVolume(const DMR_SessionToken DMR_Token, const unsigned short CurrentVolume)
{
  char* body;

  body = (char*)malloc(38);
  sprintf(body,"<CurrentVolume>%u</CurrentVolume>",CurrentVolume);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","GetVolume",body);
  free(body);
}

/*! \fn DMR_Response_RenderingControl_ListPresets(const DMR_SessionToken DMR_Token, const char* unescaped_CurrentPresetNameList)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> ListPresets
	\param DMR_Token MicroStack token
 \param unescaped_CurrentPresetNameList Value of argument CurrentPresetNameList \b     Note: Automatically Escaped
*/
void DMR_Response_RenderingControl_ListPresets(const DMR_SessionToken DMR_Token, const char* unescaped_CurrentPresetNameList)
{
  char* body;
	char *CurrentPresetNameList = (char*)malloc(1+ILibXmlEscapeLength(unescaped_CurrentPresetNameList));

	ILibXmlEscape(CurrentPresetNameList,unescaped_CurrentPresetNameList);
  body = (char*)malloc(48+strlen(CurrentPresetNameList));
  sprintf(body,"<CurrentPresetNameList>%s</CurrentPresetNameList>",CurrentPresetNameList);
  DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","ListPresets",body);
  free(body);
	free(CurrentPresetNameList);
}

/*! \fn DMR_Response_RenderingControl_SelectPreset(const DMR_SessionToken DMR_Token)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SelectPreset
	\param DMR_Token MicroStack token
*/
void DMR_Response_RenderingControl_SelectPreset(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","SelectPreset","");
}

/*! \fn DMR_Response_RenderingControl_SetBrightness(const DMR_SessionToken DMR_Token)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetBrightness
	\param DMR_Token MicroStack token
*/
void DMR_Response_RenderingControl_SetBrightness(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","SetBrightness","");
}

/*! \fn DMR_Response_RenderingControl_SetContrast(const DMR_SessionToken DMR_Token)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetContrast
	\param DMR_Token MicroStack token
*/
void DMR_Response_RenderingControl_SetContrast(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","SetContrast","");
}

/*! \fn DMR_Response_RenderingControl_SetMute(const DMR_SessionToken DMR_Token)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetMute
	\param DMR_Token MicroStack token
*/
void DMR_Response_RenderingControl_SetMute(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","SetMute","");
}

/*! \fn DMR_Response_RenderingControl_SetVolume(const DMR_SessionToken DMR_Token)
	\brief Response Method for RenderingControl >> urn:schemas-upnp-org:service:RenderingControl:1 >> SetVolume
	\param DMR_Token MicroStack token
*/
void DMR_Response_RenderingControl_SetVolume(const DMR_SessionToken DMR_Token)
{
DMR_ResponseGeneric(DMR_Token,"urn:schemas-upnp-org:service:RenderingControl:1","SetVolume","");
}



void DMR_SendEventSink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *subscriber,
void *upnp,
int *PAUSE)
{
	sem_wait(&Destroied);
	if(bIsDestroied == 1)
	{
		sem_post(&Destroied);
		return;
	}
   if(done!=0 && ((struct SubscriberInfo*)subscriber)->Disposing==0)
   {
      sem_wait(&(((struct DMR_DataObject*)upnp)->EventLock));
      --((struct SubscriberInfo*)subscriber)->RefCount;
      if(((struct SubscriberInfo*)subscriber)->RefCount==0)
      {
         LVL3DEBUG(printf("\r\n\r\nSubscriber at [%s] %d.%d.%d.%d:%d was/did UNSUBSCRIBE while trying to send event\r\n\r\n",((struct SubscriberInfo*)subscriber)->SID,(((struct SubscriberInfo*)subscriber)->Address&0xFF),((((struct SubscriberInfo*)subscriber)->Address>>8)&0xFF),((((struct SubscriberInfo*)subscriber)->Address>>16)&0xFF),((((struct SubscriberInfo*)subscriber)->Address>>24)&0xFF),((struct SubscriberInfo*)subscriber)->Port);)
         struct SubscriberInfo* sInfo = ((struct SubscriberInfo*)subscriber);
		 DMR_DestructSubscriberInfo(sInfo);
      }
      else if(header==NULL)
      {
         LVL3DEBUG(printf("\r\n\r\nCould not deliver event for [%s] %d.%d.%d.%d:%d UNSUBSCRIBING\r\n\r\n",((struct SubscriberInfo*)subscriber)->SID,(((struct SubscriberInfo*)subscriber)->Address&0xFF),((((struct SubscriberInfo*)subscriber)->Address>>8)&0xFF),((((struct SubscriberInfo*)subscriber)->Address>>16)&0xFF),((((struct SubscriberInfo*)subscriber)->Address>>24)&0xFF),((struct SubscriberInfo*)subscriber)->Port);)
         // Could not send Event, so unsubscribe the subscriber
         ((struct SubscriberInfo*)subscriber)->Disposing = 1;
         if( bIsDestroied == 0 )
         	DMR_ExpireSubscriberInfo(upnp,subscriber);
      }
      sem_post(&(((struct DMR_DataObject*)upnp)->EventLock));
   }
      sem_post(&Destroied);
}
void DMR_SendEvent_Body(void *upnptoken,char *body,int bodylength,struct SubscriberInfo *info)
{
   struct DMR_DataObject* DMR_Object = (struct DMR_DataObject*)upnptoken;
   struct sockaddr_in dest;
   int packetLength;
   char *packet;
   int ipaddr;

#define MAX_SUBSCRIPTION	30
   if ((info->RefCount+1) > MAX_SUBSCRIPTION)
   {
   		return;
   }

   memset(&dest,0,sizeof(dest));
   dest.sin_addr.s_addr = info->Address;
   dest.sin_port = htons(info->Port);
   dest.sin_family = AF_INET;
   ipaddr = info->Address;

   packet = (char*)malloc(info->PathLength + bodylength + 483);
   packetLength = sprintf(packet,"NOTIFY %s HTTP/1.1\r\nSERVER: %s, UPnP/1.0, Intel MicroStack/1.0.2718\r\nHOST: %s:%d\r\nContent-Type: text/xml; charset=\"utf-8\"\r\nNT: upnp:event\r\nNTS: upnp:propchange\r\nSID: %s\r\nSEQ: %d\r\nContent-Length: %d\r\n\r\n<?xml version=\"1.0\" encoding=\"utf-8\"?><e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\"><e:property><%s></e:property></e:propertyset>",info->Path,DMR_PLATFORM,inet_ntoa(dest.sin_addr),info->Port,info->SID,info->SEQ,bodylength+137,body);
   ++info->SEQ;

   ++info->RefCount;
   ILibWebClient_PipelineRequestEx(DMR_Object->EventClient,&dest,packet,packetLength,0,NULL,0,0,&DMR_SendEventSink,info,upnptoken);
}
void DMR_SendEvent(void *upnptoken, char* body, const int bodylength, const char* eventname)
{
   struct SubscriberInfo *info = NULL;
   struct DMR_DataObject* DMR_Object = (struct DMR_DataObject*)upnptoken;
   struct sockaddr_in dest;
   LVL3DEBUG(struct timeval tv;)

   if(DMR_Object==NULL)
   {
      free(body);
      return;
   }
   sem_wait(&(DMR_Object->EventLock));
   if(strncmp(eventname,"AVTransport",11)==0)
	{
		info = DMR_Object->HeadSubscriberPtr_AVTransport;
	}
	if(strncmp(eventname,"ConnectionManager",17)==0)
	{
		info = DMR_Object->HeadSubscriberPtr_ConnectionManager;
	}
	if(strncmp(eventname,"RenderingControl",16)==0)
	{
		info = DMR_Object->HeadSubscriberPtr_RenderingControl;
	}

   memset(&dest,0,sizeof(dest));
   while(info!=NULL)
   {
      if(!DMR_SubscriptionExpired(info))
      {
         DMR_SendEvent_Body(upnptoken,body,bodylength,info);
      }
      else
      {
         //Remove Subscriber
         LVL3DEBUG(gettimeofday(&tv,NULL);)
         LVL3DEBUG(printf("\r\n\r\nTIMESTAMP: %d\r\n",tv.tv_sec);)
         LVL3DEBUG(printf("Did not renew [%s] %d.%d.%d.%d:%d UNSUBSCRIBING <%d>\r\n\r\n",((struct SubscriberInfo*)info)->SID,(((struct SubscriberInfo*)info)->Address&0xFF),((((struct SubscriberInfo*)info)->Address>>8)&0xFF),((((struct SubscriberInfo*)info)->Address>>16)&0xFF),((((struct SubscriberInfo*)info)->Address>>24)&0xFF),((struct SubscriberInfo*)info)->Port,info);)
      }

      info = info->Next;
   }

   sem_post(&(DMR_Object->EventLock));
}

/*! \fn DMR_SetState_AVTransport_LastChange(DMR_MicroStackToken upnptoken, char* val)
	\brief Sets the state of LastChange << urn:schemas-upnp-org:service:AVTransport:1 << AVTransport \par
	\b Note: Must be called at least once prior to start
	\param upnptoken The MicroStack token
	\param val The new value of the state variable
*/
void DMR_SetState_AVTransport_LastChange(DMR_MicroStackToken upnptoken, char* val)
{
	struct DMR_DataObject *DMR_Object = (struct DMR_DataObject*)upnptoken;
  char* body;
  int bodylength;
  char* valstr;
  valstr = (char*)malloc(ILibXmlEscapeLength(val)+1);
  ILibXmlEscape(valstr,val);
  if (DMR_Object->AVTransport_LastChange != NULL) free(DMR_Object->AVTransport_LastChange);
  DMR_Object->AVTransport_LastChange = valstr;
  body = (char*)malloc(30 + (int)strlen(valstr));
  bodylength = sprintf(body,"%s>%s</%s","LastChange",valstr,"LastChange");
  //printf("body=%s\n",body);
  DMR_SendEvent(upnptoken,body,bodylength,"AVTransport");
  free(body);
}

/*! \fn DMR_SetState_ConnectionManager_SourceProtocolInfo(DMR_MicroStackToken upnptoken, char* val)
	\brief Sets the state of SourceProtocolInfo << urn:schemas-upnp-org:service:ConnectionManager:1 << ConnectionManager \par
	\b Note: Must be called at least once prior to start
	\param upnptoken The MicroStack token
	\param val The new value of the state variable
*/
void DMR_SetState_ConnectionManager_SourceProtocolInfo(DMR_MicroStackToken upnptoken, char* val)
{
	struct DMR_DataObject *DMR_Object = (struct DMR_DataObject*)upnptoken;
  char* body;
  int bodylength;
  char* valstr;
  valstr = (char*)malloc(ILibXmlEscapeLength(val)+1);
  ILibXmlEscape(valstr,val);
  if (DMR_Object->ConnectionManager_SourceProtocolInfo != NULL) free(DMR_Object->ConnectionManager_SourceProtocolInfo);
  DMR_Object->ConnectionManager_SourceProtocolInfo = valstr;
  body = (char*)malloc(46 + (int)strlen(valstr));
  bodylength = sprintf(body,"%s>%s</%s","SourceProtocolInfo",valstr,"SourceProtocolInfo");
  DMR_SendEvent(upnptoken,body,bodylength,"ConnectionManager");
  free(body);
}

/*! \fn DMR_SetState_ConnectionManager_SinkProtocolInfo(DMR_MicroStackToken upnptoken, char* val)
	\brief Sets the state of SinkProtocolInfo << urn:schemas-upnp-org:service:ConnectionManager:1 << ConnectionManager \par
	\b Note: Must be called at least once prior to start
	\param upnptoken The MicroStack token
	\param val The new value of the state variable
*/
void DMR_SetState_ConnectionManager_SinkProtocolInfo(DMR_MicroStackToken upnptoken, char* val)
{
	struct DMR_DataObject *DMR_Object = (struct DMR_DataObject*)upnptoken;
  char* body;
  int bodylength;
  char* valstr;
  valstr = (char*)malloc(ILibXmlEscapeLength(val)+1);
  ILibXmlEscape(valstr,val);
  if (DMR_Object->ConnectionManager_SinkProtocolInfo != NULL) free(DMR_Object->ConnectionManager_SinkProtocolInfo);
  DMR_Object->ConnectionManager_SinkProtocolInfo = valstr;
  body = (char*)malloc(42 + (int)strlen(valstr));
  bodylength = sprintf(body,"%s>%s</%s","SinkProtocolInfo",valstr,"SinkProtocolInfo");
  DMR_SendEvent(upnptoken,body,bodylength,"ConnectionManager");
  free(body);
}

/*! \fn DMR_SetState_ConnectionManager_CurrentConnectionIDs(DMR_MicroStackToken upnptoken, char* val)
	\brief Sets the state of CurrentConnectionIDs << urn:schemas-upnp-org:service:ConnectionManager:1 << ConnectionManager \par
	\b Note: Must be called at least once prior to start
	\param upnptoken The MicroStack token
	\param val The new value of the state variable
*/
void DMR_SetState_ConnectionManager_CurrentConnectionIDs(DMR_MicroStackToken upnptoken, char* val)
{
	struct DMR_DataObject *DMR_Object = (struct DMR_DataObject*)upnptoken;
  char* body;
  int bodylength;
  char* valstr;
  valstr = (char*)malloc(ILibXmlEscapeLength(val)+1);
  ILibXmlEscape(valstr,val);
  if (DMR_Object->ConnectionManager_CurrentConnectionIDs != NULL) free(DMR_Object->ConnectionManager_CurrentConnectionIDs);
  DMR_Object->ConnectionManager_CurrentConnectionIDs = valstr;
  body = (char*)malloc(50 + (int)strlen(valstr));
  bodylength = sprintf(body,"%s>%s</%s","CurrentConnectionIDs",valstr,"CurrentConnectionIDs");
  DMR_SendEvent(upnptoken,body,bodylength,"ConnectionManager");
  free(body);
}

/*! \fn DMR_SetState_RenderingControl_LastChange(DMR_MicroStackToken upnptoken, char* val)
	\brief Sets the state of LastChange << urn:schemas-upnp-org:service:RenderingControl:1 << RenderingControl \par
	\b Note: Must be called at least once prior to start
	\param upnptoken The MicroStack token
	\param val The new value of the state variable
*/
void DMR_SetState_RenderingControl_LastChange(DMR_MicroStackToken upnptoken, char* val)
{
	struct DMR_DataObject *DMR_Object = (struct DMR_DataObject*)upnptoken;
  char* body;
  int bodylength;
  char* valstr;
  valstr = (char*)malloc(ILibXmlEscapeLength(val)+1);
  ILibXmlEscape(valstr,val);
  if (DMR_Object->RenderingControl_LastChange != NULL) free(DMR_Object->RenderingControl_LastChange);
  DMR_Object->RenderingControl_LastChange = valstr;
  body = (char*)malloc(30 + (int)strlen(valstr));
  bodylength = sprintf(body,"%s>%s</%s","LastChange",valstr,"LastChange");
  DMR_SendEvent(upnptoken,body,bodylength,"RenderingControl");
  free(body);
}



void DMR_DestroyMicroStack(void *object)
{
   struct DMR_DataObject *upnp = (struct DMR_DataObject*)object;
   struct SubscriberInfo  *sinfo,*sinfo2;
   DMR_SendByeBye(upnp);

   bIsDestroied = 1;

   sem_wait(&Destroied);
   sem_destroy(&(upnp->EventLock));

   free(upnp->AVTransport_LastChange);
	free(upnp->ConnectionManager_SourceProtocolInfo);
	free(upnp->ConnectionManager_SinkProtocolInfo);
	free(upnp->ConnectionManager_CurrentConnectionIDs);
	free(upnp->RenderingControl_LastChange);


   free(upnp->AddressList);
   free(upnp->NOTIFY_SEND_socks);
   free(upnp->NOTIFY_RECEIVE_socks);
   free(upnp->UUID);
   free(upnp->Serial);


   sinfo = upnp->HeadSubscriberPtr_AVTransport;
	while(sinfo!=NULL)
	{
		sinfo2 = sinfo->Next;
		DMR_DestructSubscriberInfo(sinfo);
		sinfo = sinfo2;
	}
	sinfo = upnp->HeadSubscriberPtr_ConnectionManager;
	while(sinfo!=NULL)
	{
		sinfo2 = sinfo->Next;
		DMR_DestructSubscriberInfo(sinfo);
		sinfo = sinfo2;
	}
	sinfo = upnp->HeadSubscriberPtr_RenderingControl;
	while(sinfo!=NULL)
	{
		sinfo2 = sinfo->Next;
		DMR_DestructSubscriberInfo(sinfo);
		sinfo = sinfo2;
	}
   sem_post(&Destroied);

}
int DMR_GetLocalPortNumber(DMR_SessionToken token)
{
   return(ILibWebServer_GetPortNumber(((struct ILibWebServer_Session*)token)->Parent));
}
void DMR_SessionReceiveSink(
struct ILibWebServer_Session *sender,
int InterruptFlag,
struct packetheader *header,
char *bodyBuffer,
int *beginPointer,
int endPointer,
int done)
{

   char *txt;
   if(header!=NULL && sender->User3==NULL && done==0)
   {
      sender->User3 = (void*)~0;
      txt = ILibGetHeaderLine(header,"Expect",6);
      if(txt!=NULL)
      {
         if(strcasecmp(txt,"100-Continue")==0)
         {
            //
            // Expect Continue
            //
            ILibWebServer_Send_Raw(sender,"HTTP/1.1 100 Continue\r\n\r\n",25,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
         else
         {
            //
            // Don't understand
            //
            ILibWebServer_Send_Raw(sender,"HTTP/1.1 417 Expectation Failed\r\n\r\n",35,ILibAsyncSocket_MemoryOwnership_STATIC,1);
            ILibWebServer_DisconnectSession(sender);
            return;
         }
      }
   }

   if(header!=NULL && done !=0 && InterruptFlag==0)
   {
      DMR_ProcessHTTPPacket(sender,header,bodyBuffer,beginPointer==NULL?0:*beginPointer,endPointer);
      if(beginPointer!=NULL) {*beginPointer = endPointer;}
   }
}
void DMR_SessionSink(struct ILibWebServer_Session *SessionToken, void *user)
{
   SessionToken->OnReceive = &DMR_SessionReceiveSink;
   SessionToken->User = user;
}
void DMR_SetTag(const DMR_MicroStackToken token, void *UserToken)
{
   ((struct DMR_DataObject*)token)->User = UserToken;
}
void *DMR_GetTag(const DMR_MicroStackToken token)
{
   return(((struct DMR_DataObject*)token)->User);
}
DMR_MicroStackToken DMR_GetMicroStackTokenFromSessionToken(const DMR_SessionToken token)
{
   return(((struct ILibWebServer_Session*)token)->User);
}
DMR_MicroStackToken DMR_CreateMicroStack(void *Chain, const char* FriendlyName,const char* UDN, const char* SerialNumber, const int NotifyCycleSeconds, const unsigned short PortNum)

{
   struct DMR_DataObject* RetVal = (struct DMR_DataObject*)malloc(sizeof(struct DMR_DataObject));

   bIsDestroied = 0;

   struct timeval tv;
   gettimeofday(&tv,NULL);
   srand((int)tv.tv_sec);

   DMR__Device_MediaRenderer_Impl.FriendlyName = FriendlyName;
	DMR__Device_MediaRenderer_Impl.UDN = UDN;
	DMR__Device_MediaRenderer_Impl.Serial = SerialNumber;
	if(DMR__Device_MediaRenderer_Impl.Manufacturer == NULL) {DMR__Device_MediaRenderer_Impl.Manufacturer = "TOSHIBA";}
   if(DMR__Device_MediaRenderer_Impl.ManufacturerURL == NULL) {DMR__Device_MediaRenderer_Impl.ManufacturerURL = " ";}
   if(DMR__Device_MediaRenderer_Impl.ModelDescription == NULL) {DMR__Device_MediaRenderer_Impl.ModelDescription = " ";}
   if(DMR__Device_MediaRenderer_Impl.ModelName == NULL) {DMR__Device_MediaRenderer_Impl.ModelName = "T30_01";}
   if(DMR__Device_MediaRenderer_Impl.ModelNumber == NULL) {DMR__Device_MediaRenderer_Impl.ModelNumber = " ";}
   if(DMR__Device_MediaRenderer_Impl.ModelURL == NULL) {DMR__Device_MediaRenderer_Impl.ModelURL = " ";}
   if(DMR__Device_MediaRenderer_Impl.ProductCode == NULL) {DMR__Device_MediaRenderer_Impl.ProductCode = " ";}


   /* Complete State Reset */
   memset(RetVal,0,sizeof(struct DMR_DataObject));

   RetVal->ForceExit = 0;
   RetVal->PreSelect = &DMR_MasterPreSelect;
   RetVal->PostSelect = NULL;
   RetVal->Destroy = &DMR_DestroyMicroStack;
   RetVal->InitialNotify = 0;
   if (UDN != NULL)
   {
      RetVal->UUID = (char*)malloc((int)strlen(UDN)+6);
      sprintf(RetVal->UUID,"uuid:%s",UDN);
      RetVal->UDN = RetVal->UUID + 5;
   }
   if (SerialNumber != NULL)
   {
      RetVal->Serial = (char*)malloc((int)strlen(SerialNumber)+1);
      strcpy(RetVal->Serial,SerialNumber);
   }



   RetVal->WebServerTimer = ILibCreateLifeTime(Chain);

   RetVal->HTTPServer = ILibWebServer_Create(Chain,UPNP_HTTP_MAXSOCKETS,PortNum,&DMR_SessionSink,RetVal);
   RetVal->WebSocketPortNumber=(int)ILibWebServer_GetPortNumber(RetVal->HTTPServer);



   ILibAddToChain(Chain,RetVal);
   DMR_Init(RetVal,Chain,NotifyCycleSeconds,PortNum);

   RetVal->EventClient = ILibCreateWebClient(5,Chain);
   RetVal->UpdateFlag = 0;



   sem_init(&(RetVal->EventLock),0,1);
   DMR_GetConfiguration()->MicrostackToken=RetVal;
   return(RetVal);
}








void DMR_StreamDescriptionDocument(struct ILibWebServer_Session *session,struct packetheader* header)
{
   struct packetheader_field_node *f;
   char* RequiredLang = NULL;
   int RequiredLangLen;
   char *packet;
   char *date;
   int contentlen=0;
   #if defined(WIN32) || defined(_WIN32_WCE)
   char *responseHeader = "\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #elif defined(__SYMBIAN32__)
   char *responseHeader = "\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #else
   char *responseHeader = "\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #endif


	#if defined(WIN32) || defined(_WIN32_WCE)
   char *responseHeader2 = "\r\nCONTENT-LANGUAGE:  en\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #elif defined(__SYMBIAN32__)
   char *responseHeader2 = "\r\nCONTENT-LANGUAGE:  en\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #else
   char *responseHeader2 = "\r\nCONTENT-LANGUAGE:  en\r\nCONTENT-TYPE:  text/xml; charset=\"utf-8\"";
   #endif

   char *tempString;
   int tempStringLength;
   char *xString,*xString2;
   char *yuyuTemp = NULL, *yuyuInsertP = NULL;

//TODO !!!!
   char *yuyuAdd = strdup(" xmlns:pnpx=\"http://schemas.microsoft.com/windows/pnpx/2005/11\"\n\txmlns:df=\"http://schemas.microsoft.com/windows/2008/09/devicefoundation\">\n\t\t<pnpx:X_hardwareId>PnPX_RealtekMediaRender_HWID_1</pnpx:X_hardwareId>\n\t\t<pnpx:X_compatibleId>MS_DigitalMediaDeviceClass_DMR_V001</pnpx:X_compatibleId>\n\t\t<pnpx:X_deviceCategory>Multimedia.DMR</pnpx:X_deviceCategory>\n\t\t<df:X_deviceCategory>Multimedia.DMR</df:X_deviceCategory>\n\t\t<df:X_modelId>ABCC3F95-1DE6-4e99-9526-3F74F5C5D195</df:X_modelId>\n\t\t<pnpx:X_singularDescription>\n\t\t\tDigital media renderer\n\t\t</pnpx:X_singularDescription");
  // char *yuyuAdd = strdup(" xmlns:pnpx=\"http://schemas.microsoft.com/windows/pnpx/2005/11\"\n\txmlns:df=\"http://schemas.microsoft.com/windows/2008/09/devicefoundation\"");



   //xString2 = ILibDecompressString((unsigned char*)DMR__Device_MediaRenderer_Impl.Reserved,DMR__Device_MediaRenderer_Impl.ReservedXL,DMR__Device_MediaRenderer_Impl.ReservedUXL);
 #ifdef ANDROID_PLATFORM
  xString2 = strdup("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n\t<specVersion>\n\t\t<major>1</major>\n\t\t<minor>0</minor>\n\t</specVersion>\n\t<device>\n\t\t<deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>\n\t\t<dlna:X_DLNADOC xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">DMR-1.50</dlna:X_DLNADOC>\n\t\t<friendlyName>%s</friendlyName>\n\t\t<manufacturer>%s</manufacturer>\n\t\t<manufacturerURL>%s</manufacturerURL>\n\t\t<modelDescription>%s</modelDescription>\n\t\t<modelName>%s</modelName>\n\t\t<modelNumber>%s</modelNumber>\n\t\t<modelURL>http://255.255.255.255:255/</modelURL>\n\t\t<serialNumber />\n\t\t<UDN>uuid:%s</UDN>\n\t</device>\n</root>\n");
 #else
   xString2 = strdup("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n\t<specVersion>\n\t\t<major>1</major>\n\t\t<minor>0</minor>\n\t</specVersion>\n\t<device>\n\t\t<deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>\n\t\t<dlna:X_DLNADOC xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">DMR-1.50</dlna:X_DLNADOC>\n\t\t<dlna:X_DLNACAP xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\">playcontainer-1-0</dlna:X_DLNACAP>\n\t\t<friendlyName>%s</friendlyName>\n\t\t<manufacturer>%s</manufacturer>\n\t\t<manufacturerURL>%s</manufacturerURL>\n\t\t<modelDescription>%s</modelDescription>\n\t\t<modelName>%s</modelName>\n\t\t<modelNumber>%s</modelNumber>\n\t\t<modelURL>http://255.255.255.255:255/</modelURL>\n\t\t<serialNumber>%s</serialNumber>\n\t\t<UDN>uuid:%s</UDN>\n\t</device>\n</root>\n");
 #endif
  //xString2 = strdup("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n\t<specVersion>\n\t\t<major>1</major>\n\t\t<minor>0</minor>\n\t</specVersion>\n\t<device>\n\t\t<deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>\n\t\t<friendlyName>%s</friendlyName>\n\t\t<manufacturer>%s</manufacturer>\n\t\t<manufacturerURL>%s</manufacturerURL>\n\t\t<modelDescription>%s</modelDescription>\n\t\t<modelName>%s</modelName>\n\t\t<modelNumber>%s</modelNumber>\n\t\t<modelURL>http://255.255.255.255:255/</modelURL>\n\t\t<serialNumber>%s</serialNumber>\n\t\t<UDN>uuid:%s</UDN>\n\t</device>\n</root>\n");

   xString = ILibString_Replace(xString2,(int)strlen(xString2),"http://255.255.255.255:255/",27,"%s",2);
//printf("\n\n\t===================================\n");
//printf("DecompresssString:\n");
//printf("%s\n", xString2);
//printf("\t===================================\n\n");
   free(xString2);

   yuyuTemp = (char *)malloc(strlen(xString)+strlen(yuyuAdd)+1);
   memset(yuyuTemp, 0, strlen(xString)+strlen(yuyuAdd)+1);
   yuyuInsertP = strstr(xString, "device");
   yuyuInsertP = strstr(yuyuInsertP+1, "device");
   if( yuyuInsertP != NULL )
   {
       yuyuInsertP += (strlen("device"));
       snprintf(yuyuTemp, yuyuInsertP-xString+1, "%s", xString);
       sprintf(yuyuTemp, "%s%s%s", yuyuTemp, yuyuAdd, yuyuInsertP);
   }
   else
   {
       memset(yuyuTemp, 0, strlen(xString)+strlen(yuyuAdd)+1);
       snprintf(yuyuTemp, strlen(xString), "%s", xString);
   }
   free(yuyuAdd);

   tempStringLength = (int)(strlen(yuyuTemp)+strlen(DMR__Device_MediaRenderer_Impl.Manufacturer)+strlen(DMR__Device_MediaRenderer_Impl.ManufacturerURL)+strlen(DMR__Device_MediaRenderer_Impl.ModelDescription)+strlen(DMR__Device_MediaRenderer_Impl.ModelName)+strlen(DMR__Device_MediaRenderer_Impl.ModelNumber)+strlen(DMR__Device_MediaRenderer_Impl.ModelURL)+strlen(DMR__Device_MediaRenderer_Impl.ProductCode)+strlen(DMR__Device_MediaRenderer_Impl.FriendlyName)+strlen(DMR__Device_MediaRenderer_Impl.UDN));
   tempString = (char*)malloc(tempStringLength);
   tempStringLength = sprintf(tempString,yuyuTemp,
   DMR__Device_MediaRenderer_Impl.FriendlyName,
   DMR__Device_MediaRenderer_Impl.Manufacturer,
   DMR__Device_MediaRenderer_Impl.ManufacturerURL,
   DMR__Device_MediaRenderer_Impl.ModelDescription,
   DMR__Device_MediaRenderer_Impl.ModelName,
   DMR__Device_MediaRenderer_Impl.ModelNumber,
   DMR__Device_MediaRenderer_Impl.ModelURL,
   DMR__Device_MediaRenderer_Impl.UDN);
   free(xString);
   free(yuyuTemp);

	contentlen=0;
	contentlen+= tempStringLength-19;
	contentlen += 13;
	if(DMR__Device_MediaRenderer_Impl.AVTransport!=NULL)
   		contentlen += strlen("<service><serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType><serviceId>urn:upnp-org:serviceId:AVTransport</serviceId><SCPDURL>AVTransport/scpd.xml</SCPDURL><controlURL>AVTransport/control</controlURL><eventSubURL>AVTransport/event</eventSubURL></service>");
	if(DMR__Device_MediaRenderer_Impl.ConnectionManager!=NULL)
		contentlen += strlen("<service><serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType><serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId><SCPDURL>ConnectionManager/scpd.xml</SCPDURL><controlURL>ConnectionManager/control</controlURL><eventSubURL>ConnectionManager/event</eventSubURL></service>");
	if(DMR__Device_MediaRenderer_Impl.RenderingControl!=NULL)
	 	contentlen += strlen("<service><serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType><serviceId>urn:upnp-org:serviceId:RenderingControl</serviceId><SCPDURL>RenderingControl/scpd.xml</SCPDURL><controlURL>RenderingControl/control</controlURL><eventSubURL>RenderingControl/event</eventSubURL></service>");
   contentlen += (14+9+7);

	f = header->FirstField;
    while(f!=NULL)
    {
       if(f->FieldLength==15 && strncasecmp(f->Field,"ACCEPT-LANGUAGE",15)==0)
       {
          //
          // Get the ACCEPT-LANGUAGE string
          //
          RequiredLang = f->FieldData;
          RequiredLangLen = f->FieldDataLength;
       }
       f = f->NextField;
    }

 	if(!(header->VersionLength==3 && memcmp(header->Version,"1.0",3)==0))
 	{
 		//1.1 no need content Length
 	  date = Rfc1123_DateTimeNow();
 	  if((RequiredLang != NULL) && (RequiredLangLen != 0))
 	  {
 	 	packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader2) + 11+36);
 		sprintf(packet,"%s\r\nDATE: %s",responseHeader2,date);
		//sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader2,contentlen,date);
	  }
 	  else
 	  {

 	  	packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader) + 11+36);
		sprintf(packet,"%s\r\nDATE: %s",responseHeader,date);
 		//sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader,contentlen,date);
 	  }
 	  free(date);
 	}
 	else
 	{
 		 date = Rfc1123_DateTimeNow();
 	  if((RequiredLang != NULL) && (RequiredLangLen != 0))
 	  {
 	 	packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader2) + 11+36);
 		sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader2,contentlen,date);
 	  }
 	  else
 	  {
 	  	packet = (char*)malloc((int)strlen(date) + (int)strlen(responseHeader) + 11+36);
 		sprintf(packet,"%s\r\nCONTENT-LENGTH: %d\r\nDATE: %s",responseHeader,contentlen,date);
 	  }
 	  free(date);
 	}

    //printf("Device packet=%s\n",packet);


   //
   // Device
   //
   ILibWebServer_StreamHeader_Raw(session,200,"OK",packet,0);



   ILibWebServer_StreamBody(session,tempString,tempStringLength-19,ILibAsyncSocket_MemoryOwnership_CHAIN,0);
//printf("[%s][%d] preview the string....tempString\n%s\ndone\n", __func__, __LINE__, tempString);

   //
   // Embedded Services
   //
   ILibWebServer_StreamBody(session,"<serviceList>",13,ILibAsyncSocket_MemoryOwnership_STATIC,0);

   if(DMR__Device_MediaRenderer_Impl.AVTransport!=NULL)
   {
// vvv modified by yuyu for UPnP CTT
      //xString = ILibDecompressString((unsigned char*)DMR__Device_MediaRenderer_Impl.AVTransport->Reserved,DMR__Device_MediaRenderer_Impl.AVTransport->ReservedXL,DMR__Device_MediaRenderer_Impl.AVTransport->ReservedUXL);
      //ILibWebServer_StreamBody(session,xString,DMR__Device_MediaRenderer_Impl.AVTransport->ReservedUXL,ILibAsyncSocket_MemoryOwnership_CHAIN,0);
      ILibWebServer_StreamBody(session,"<service><serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType><serviceId>urn:upnp-org:serviceId:AVTransport</serviceId><SCPDURL>AVTransport/scpd.xml</SCPDURL><controlURL>AVTransport/control</controlURL><eventSubURL>AVTransport/event</eventSubURL></service>",strlen("<service><serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType><serviceId>urn:upnp-org:serviceId:AVTransport</serviceId><SCPDURL>AVTransport/scpd.xml</SCPDURL><controlURL>AVTransport/control</controlURL><eventSubURL>AVTransport/event</eventSubURL></service>"),ILibAsyncSocket_MemoryOwnership_STATIC,0);
// ^^^ modified by yuyu for UPnP CTT
   }

   if(DMR__Device_MediaRenderer_Impl.ConnectionManager!=NULL)
   {
// vvv modified by yuyu for UPnP CTT
      //xString = ILibDecompressString((unsigned char*)DMR__Device_MediaRenderer_Impl.ConnectionManager->Reserved,DMR__Device_MediaRenderer_Impl.ConnectionManager->ReservedXL,DMR__Device_MediaRenderer_Impl.ConnectionManager->ReservedUXL);
      //ILibWebServer_StreamBody(session,xString,DMR__Device_MediaRenderer_Impl.ConnectionManager->ReservedUXL,ILibAsyncSocket_MemoryOwnership_CHAIN,0);
      ILibWebServer_StreamBody(session,"<service><serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType><serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId><SCPDURL>ConnectionManager/scpd.xml</SCPDURL><controlURL>ConnectionManager/control</controlURL><eventSubURL>ConnectionManager/event</eventSubURL></service>",strlen("<service><serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType><serviceId>urn:upnp-org:serviceId:ConnectionManager</serviceId><SCPDURL>ConnectionManager/scpd.xml</SCPDURL><controlURL>ConnectionManager/control</controlURL><eventSubURL>ConnectionManager/event</eventSubURL></service>"),ILibAsyncSocket_MemoryOwnership_STATIC,0);
// ^^^ modified by yuyu for UPnP CTT
   }

   if(DMR__Device_MediaRenderer_Impl.RenderingControl!=NULL)
   {
// vvv modified by yuyu for UPnP CTT
      //xString = ILibDecompressString((unsigned char*)DMR__Device_MediaRenderer_Impl.RenderingControl->Reserved,DMR__Device_MediaRenderer_Impl.RenderingControl->ReservedXL,DMR__Device_MediaRenderer_Impl.RenderingControl->ReservedUXL);
      //ILibWebServer_StreamBody(session,xString,DMR__Device_MediaRenderer_Impl.RenderingControl->ReservedUXL,ILibAsyncSocket_MemoryOwnership_CHAIN,0);
      ILibWebServer_StreamBody(session,"<service><serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType><serviceId>urn:upnp-org:serviceId:RenderingControl</serviceId><SCPDURL>RenderingControl/scpd.xml</SCPDURL><controlURL>RenderingControl/control</controlURL><eventSubURL>RenderingControl/event</eventSubURL></service>",strlen("<service><serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType><serviceId>urn:upnp-org:serviceId:RenderingControl</serviceId><SCPDURL>RenderingControl/scpd.xml</SCPDURL><controlURL>RenderingControl/control</controlURL><eventSubURL>RenderingControl/event</eventSubURL></service>"),ILibAsyncSocket_MemoryOwnership_STATIC,0);
// ^^^ modified by yuyu for UPnP CTT
   }

   ILibWebServer_StreamBody(session,"</serviceList>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);

   ILibWebServer_StreamBody(session,"</device>",9,ILibAsyncSocket_MemoryOwnership_STATIC,0);


   ILibWebServer_StreamBody(session,"</root>",7,ILibAsyncSocket_MemoryOwnership_STATIC,1);
}
struct DMR__Device_MediaRenderer* DMR_GetConfiguration()
{
	return(&(DMR__Device_MediaRenderer_Impl));
}



