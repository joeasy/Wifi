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
 * $Workfile: Main.c
 * $Revision: #1.0.2718.23851
 * $Author:   Intel Corporation, Intel Device Builder
 * $Date:     2007年9月12日
 *
 *
 *
 */

#if defined(WIN32)
	#ifndef MICROSTACK_NO_STDAFX
		#include "stdafx.h"
	#endif
	#define _CRTDBG_MAP_ALLOC
	#include <TCHAR.h>
#endif

#if defined(WINSOCK2)
	#include <winsock2.h>
	#include <ws2tcpip.h>
#elif defined(WINSOCK1)
	#include <winsock.h>
	#include <wininet.h>
#endif

#include "ILibParsers.h"
#include "MediaServerCP_ControlPoint.h"

#include "DMR_MicroStack.h"
#include "ILibWebServer.h"
#include "ILibAsyncSocket.h"



#include "ILibThreadPool.h"
#include <pthread.h>
#include "DMR.h"

#if defined(WIN32)
	#include <crtdbg.h>
#endif


void *MicroStackChain;







void *ILib_Pool;

DMR dmrObject;
const char* ProtocolInfoList[] = {
	/* Image Formats */
"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM;DLNA.ORG_FLAGS=31200000000000000000000000000000",
"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED;DLNA.ORG_FLAGS=3120000000000000000000000000000",
"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_LRG;DLNA.ORG_FLAGS=3120000000000000000000000000000",
#ifdef INCLUDE_FEATURE_PLAYSINGLEURI
	"playsingle-http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_MED;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_LRG;DLNA.ORG_FLAGS=2120000000000000000000000000000",
#endif
	/* Audio Formats */
"http-get:*:audio/L16;channels=1;rate=44100:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=3120000000000000000000000000000",
"http-get:*:audio/L16;channels=2;rate=44100:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=3120000000000000000000000000000",
"http-get:*:audio/L16;channels=1;rate=48000:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=3120000000000000000000000000000",
"http-get:*:audio/L16;channels=2;rate=48000:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=3120000000000000000000000000000",
#ifdef INCLUDE_FEATURE_PLAYSINGLEURI
	"playsingle-http-get:*:audio/L16;channels=1;rate=44100:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:audio/L16;channels=2;rate=44100:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:audio/L16;channels=1;rate=48000:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:audio/L16;channels=2;rate=48000:DLNA.ORG_PN=LPCM;DLNA.ORG_FLAGS=2120000000000000000000000000000",
#endif
	/* VIDEO: PS */
"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC;DLNA.ORG_FLAGS=3120000000000000000000000000000",
#ifdef INCLUDE_FEATURE_PLAYSINGLEURI
	"playsingle-http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC;DLNA.ORG_FLAGS=2120000000000000000000000000000",
#endif
    /* VIDEO: TS NA */
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_NA;DLNA.ORG_FLAGS=3120000000000000000000000000000",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_NA_T;DLNA.ORG_FLAGS=3120000000000000000000000000000",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_NA;DLNA.ORG_FLAGS=3120000000000000000000000000000",
"http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_NA_T;DLNA.ORG_FLAGS=3120000000000000000000000000000",
#ifdef INCLUDE_FEATURE_PLAYSINGLEURI
	"playsingle-http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_NA;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_SD_NA_T;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_NA;DLNA.ORG_FLAGS=2120000000000000000000000000000",
	"playsingle-http-get:*:video/vnd.dlna.mpeg-tts:DLNA.ORG_PN=MPEG_TS_HD_NA_T;DLNA.ORG_FLAGS=2120000000000000000000000000000",
#endif
"\0"
};




void *ILib_Monitor;
int ILib_IPAddressLength;
int *ILib_IPAddressList;




//typedef int (*DMRCallback_SetAVTransportURI)(DMR instance, void* session, char* uri, struct CdsObject* data);
int RTKMR_AVTransport_SetAVTransportURI(DMR instance, void* session, char* uri, struct CdsObject* data)
{
	printf("\t\t[yuyu] this is just a test still, now\n");
	return 0;
	//return 402;
}




















void ILib_IPAddressMonitor(void *data)
{
	int length;
	int *list;
	
	length = ILibGetLocalIPAddressList(&list);
	if(length!=ILib_IPAddressLength || memcmp((void*)list,(void*)ILib_IPAddressList,sizeof(int)*length)!=0)
	{
		
		




		
		
		
		free(ILib_IPAddressList);
		ILib_IPAddressList = list;
		ILib_IPAddressLength = length;
	}
	else
	{
		free(list);
	}
	
	
	ILibLifeTime_Add(ILib_Monitor,NULL,4,(void (*)(void*))&ILib_IPAddressMonitor,NULL);
}





void BreakSink(int s)
{
	
	if(MicroStackChain!=NULL)
	{
		ILibStopChain(MicroStackChain);
		MicroStackChain = NULL;
	}
	
	

}





void* ILibPoolThread(void *args)
{
	ILibThreadPool_AddThread(ILib_Pool);
	return(0);
}


char* BuildProtocolInfo(const char* infoList[])
{
	int counter;
    int length = 0;
    char* result = NULL;
    char* p;

    if(infoList == NULL)
    {
        return NULL;
    }

    counter = 0;
    p = (char*)infoList[counter];
    while(p[0] != '\0')
    {
        length += ((int)strlen(p) + 1);
        p = (char*)infoList[++counter];
    }

    result = (char*)malloc(length);
    result[0] = 0;

    counter = 0;
    p = (char*)infoList[counter];
    while(p[0] != '\0')
    {
        if(result[0] != '\0')
        {
            strcat(result, ",");
        }
        strcat(result, p);
        p = (char*)infoList[++counter];
    }

    return result;
}


int main(void) 
{
	char *protocolInfo;

	
	int x;
	

	struct sigaction setup_action;
    sigset_t block_mask;
	pthread_t t;
     
		
	

	
	MicroStackChain = ILibCreateChain();
	
	

	/* TODO: Each device must have a unique device identifier (UDN) */
	
		
	
	/* All evented state variables MUST be initialized before DMR_Start is called. */
	
	

	
	printf("Intel MicroStack 1.0 - Intel DLNA DMR,\r\n\r\n");

	
	ILib_Pool = ILibThreadPool_Create();
	for(x=0;x<3;++x)
	{
		
		pthread_create(&t,NULL,&ILibPoolThread,NULL);
	}
	
	DMR_GetConfiguration()->Manufacturer = "RTK";
	DMR_GetConfiguration()->ManufacturerURL = "http://www.realtek.com.tw";
	DMR_GetConfiguration()->ModelName = "RTKDMR";
	DMR_GetConfiguration()->ModelDescription = "RTKDMRs";
	DMR_GetConfiguration()->ModelNumber = "111";
	DMR_GetConfiguration()->ModelURL = "http://RTKDMR";
	protocolInfo = BuildProtocolInfo(ProtocolInfoList);
dmrObject = DMR_Method_Create(MicroStackChain, 0, "Intel Code Wizard Generated DMR", "serialNumber", "f04888fd-fff8-4ad9-9806-a495d5c8b544", protocolInfo, ILib_Pool);

dmrObject->Event_SetAVTransportURI = &RTKMR_AVTransport_SetAVTransportURI;


	

	
	
	

	
	ILib_Monitor = ILibCreateLifeTime(MicroStackChain);
	
	ILib_IPAddressLength = ILibGetLocalIPAddressList(&ILib_IPAddressList);
	ILibLifeTime_Add(ILib_Monitor,NULL,4,(void (*)(void*))&ILib_IPAddressMonitor,NULL);
	
	



	sigemptyset (&block_mask);
    /* Block other terminal-generated signals while handler runs. */
    sigaddset (&block_mask, SIGINT);
    sigaddset (&block_mask, SIGQUIT);
    setup_action.sa_handler = BreakSink;
    setup_action.sa_mask = block_mask;
    setup_action.sa_flags = 0;
    sigaction (SIGINT, &setup_action, NULL);


	
	ILibStartChain(MicroStackChain);
free(protocolInfo);


	
	

	
	if(ILib_Pool!=NULL)
	{
		printf("Stopping Thread Pool...\r\n");
		ILibThreadPool_Destroy(ILib_Pool);
		printf("Thread Pool Destroyed...\r\n");
	}
	

	
	free(ILib_IPAddressList);
	
	
	return 0;
}

