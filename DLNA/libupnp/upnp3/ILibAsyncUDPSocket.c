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
 * $Workfile: ILibAsyncSocket.c
 * $Revision: #1.0.2718.23851
 * $Author:   Intel Corporation, Intel Device Builder
 * $Date:     2007å¹?æœ?æ—? *
 *
 *
 */

#ifdef MEMORY_CHECK
	#include <assert.h>
	#define MEMCHECK(x) x
#else
	#define MEMCHECK(x)
#endif

#if defined(WIN32)
	#define _CRTDBG_MAP_ALLOC
#endif

#if defined(WINSOCK2)
	#include <winsock2.h>
	#include <ws2tcpip.h>
#elif defined(WINSOCK1)
	#include <winsock.h>
	#include <wininet.h>
#endif
#include "ILibParsers.h"
#include "ILibAsyncUDPSocket.h"
#include "ILibAsyncSocket.h"

#if defined(WIN32) && !defined(_WIN32_WCE)
	#include <crtdbg.h>
#endif

struct ILibAsyncUDPSocket_Data
{
	void *user1;
	void *user2;

	ILibAsyncSocket_SocketModule UDPSocket;
	unsigned short BoundPortNumber;

	ILibAsyncUDPSocket_OnData OnData;
	ILibAsyncUDPSocket_OnSendOK OnSendOK;
};

void ILibAsyncUDPSocket_OnDataSink(ILibAsyncSocket_SocketModule socketModule,char* buffer,int *p_beginPointer, int endPointer,ILibAsyncSocket_OnInterrupt* OnInterrupt, void **user, int *PAUSE)
{
	struct ILibAsyncUDPSocket_Data *data = (struct ILibAsyncUDPSocket_Data*)*user;

	if(data->OnData!=NULL)
	{
		data->OnData(
			socketModule,
			buffer, 
			endPointer,
			ILibAsyncSocket_GetRemoteInterface(socketModule),
			ILibAsyncSocket_GetRemotePort(socketModule),
			data->user1, 
			data->user2, 
			PAUSE);
	}
	*p_beginPointer = endPointer;
}
void ILibAsyncUDPSocket_OnSendOKSink(ILibAsyncSocket_SocketModule socketModule, void *user)
{
	struct ILibAsyncUDPSocket_Data *data = (struct ILibAsyncUDPSocket_Data*)user;
	if(data->OnSendOK!=NULL)
	{
		data->OnSendOK(socketModule, data->user1, data->user2);
	}
}

void ILibAsyncUDPSocket_OnDisconnect(ILibAsyncSocket_SocketModule socketModule, void *user)
{
	free(user);
}
/*! \fn ILibAsyncUDPSocket_SocketModule ILibAsyncUDPSocket_CreateEx(void *Chain, int BufferSize, int localInterface, unsigned short localPortStartRange, unsigned short localPortEndRange, enum ILibAsyncUDPSocket_Reuse reuse, ILibAsyncUDPSocket_OnData OnData, ILibAsyncUDPSocket_OnSendOK OnSendOK, void *user)
	\brief Creates a new instance of an ILibAsyncUDPSocket module, using a random port number between \a localPortStartRange and \a localPortEndRange inclusive.
	\param Chain The chain to add this object to. (Chain must <B>not</B> not be running)
	\param BufferSize The size of the buffer to use
	\param localInterface The IP address to bind this socket to, in network order
	\param localPortStartRange The begin range to select a port number from (host order)
	\param localPortEndRange The end range to select a port number from (host order)
	\param reuse Reuse type
	\param OnData The handler to receive data
	\param OnSendOK The handler to receive notification that pending sends have completed
	\param user User object to associate with this object
	\returns The ILibAsyncUDPSocket_SocketModule handle that was created
*/
ILibAsyncUDPSocket_SocketModule ILibAsyncUDPSocket_CreateEx(void *Chain, int BufferSize, int localInterface, unsigned short localPortStartRange, unsigned short localPortEndRange, enum ILibAsyncUDPSocket_Reuse reuse, ILibAsyncUDPSocket_OnData OnData, ILibAsyncUDPSocket_OnSendOK OnSendOK, void *user)
{
	void *RetVal = NULL;
	struct ILibAsyncUDPSocket_Data *data = (struct ILibAsyncUDPSocket_Data*)malloc(sizeof(struct ILibAsyncUDPSocket_Data));
#if defined(WIN32) || defined(_WIN32_WCE)
	SOCKET newSocket;
#else
	int newSocket;
#endif
	struct sockaddr_in local;
	int ra = (int)reuse;
	int rv;

	memset(data,0,sizeof(struct ILibAsyncUDPSocket_Data));
	data->OnData = OnData;
	data->OnSendOK = OnSendOK;
	data->user1 = user;


#if !defined(__SYMBIAN32__)
	newSocket = socket(AF_INET, SOCK_DGRAM, 0);
#else
	newSocket = ILibSocketWrapper_socket(SOCK_DGRAM);
#endif
	memset(&local,0,sizeof(struct sockaddr_in));


	local.sin_addr.s_addr = localInterface;
	local.sin_family = AF_INET;
	local.sin_port = htons(localPortStartRange);

#if !defined(__SYMBIAN32__)
	rv=setsockopt(newSocket, SOL_SOCKET, SO_REUSEADDR,(char*)&ra, sizeof(ra));
#else
	rv=ILibSocketWrapper_SetReuseAddr(newSocket,1);
#endif

	if(newSocket == -1)
	{
		printf("ILibAsyncUDPSocket_CreateEx	create socket error errno=%d\n",errno);
		free(data);
		return(NULL);
	}
	
	if(localPortStartRange!=localPortEndRange)
	{
		int count=0;
		do
		{
			//try 200 max to see if can bind success
			if(count >=200)
				break;
			else
				count++;
			//
			// Choose a random port from 50000 to 65500, which is what IANA says to use
			// for non standard ports
			//
			local.sin_port =  htons((unsigned short)(localPortStartRange + ((unsigned short)rand() % (localPortEndRange-localPortStartRange))));
			
		}
		#if !defined(__SYMBIAN32__)
		while(bind(newSocket, (struct sockaddr *) &(local), sizeof(local))!=0 &&
              (*(int*)Chain != 1) /* if this chain is terminated, just break this*/);
		#else
		while(ILibSocketWrapper_bind(newSocket, (struct sockaddr *) &(local))!=0 &&
              (*(int*)Chain != 1) /* if this chain is terminated, just break this*/);
		#endif

		if (count >= 200)
                {
			#if defined(_WIN32_WCE) || defined(WIN32)
				#if defined(WINSOCK2)
					shutdown(newSocket,SD_BOTH);
				#endif
				closesocket(newSocket);
			#elif defined(_POSIX)
				shutdown(newSocket,SHUT_RDWR);
				close(newSocket);
			#elif defined(__SYMBIAN32__)
				ILibSocketWrapper_close(newSocket);
			#endif

                        free(data);
                        return NULL;
                }
	}
	#if !defined(__SYMBIAN32__)
	else if(bind(newSocket, (struct sockaddr *) &(local), sizeof(local))!=0)
	#else
	else if(ILibSocketWrapper_bind(newSocket, (struct sockaddr *) &(local))!=0)
	#endif
	{
		//
		// Could not bind to this port
		//
	      #if defined(_WIN32_WCE) || defined(WIN32)
	      #if defined(WINSOCK2)
			shutdown(newSocket,SD_BOTH);
		#endif
			closesocket(newSocket);
		#elif defined(_POSIX)
			shutdown(newSocket,SHUT_RDWR);
			close(newSocket);
		#elif defined(__SYMBIAN32__)
			ILibSocketWrapper_close(newSocket);
		#endif
		
		free(data);
		return(NULL);
	}

	data->BoundPortNumber = ntohs(local.sin_port);

	RetVal = ILibCreateAsyncSocketModule(Chain,BufferSize,&ILibAsyncUDPSocket_OnDataSink,NULL,&ILibAsyncUDPSocket_OnDisconnect,&ILibAsyncUDPSocket_OnSendOKSink);
	ILibAsyncSocket_UseThisSocket(RetVal,&newSocket,&ILibAsyncUDPSocket_OnDisconnect,data);
	return(RetVal);
}
/*! \fn int ILibAsyncUDPSocket_JoinMulticastGroup(ILibAsyncUDPSocket_SocketModule module, int localInterface, int remoteInterface)
	\brief Joins a multicast group
	\param module The ILibAsyncUDPSocket_SocketModule to join the multicast group
	\param localInterface The local IP address in network order, to join the multicast group
	\param remoteInterface The multicast ip address in network order, to join
	\returns 0 = Success, Nonzero = Failure
*/
int ILibAsyncUDPSocket_JoinMulticastGroup(ILibAsyncUDPSocket_SocketModule module, int localInterface, int remoteInterface)
{
	struct ip_mreq mreq;
#if defined(WIN32) || defined(_WIN32_WCE)
	SOCKET s = *((SOCKET*)ILibAsyncSocket_GetSocket(module));
#else
	int s = *((int*)ILibAsyncSocket_GetSocket(module));
#endif
	int RetVal;

	mreq.imr_multiaddr.s_addr = remoteInterface;
	mreq.imr_interface.s_addr = localInterface;
	
	//
	// Join the multicast group
	//
	#if !defined(__SYMBIAN32__)
	RetVal = setsockopt(s, IPPROTO_IP, IP_ADD_MEMBERSHIP,(char*)&mreq, sizeof(mreq));
	#else
	RetVal = ILibSocketWrapper_joinmulticastgroup(s, remoteInterface, localInterface);
	#endif
	if(RetVal==0)
	{
		ILibAsyncSocket_SetLocalInterface2(module,localInterface);
	}
	else
	{
		ILibAsyncSocket_SetLocalInterface2(module,0);	
	}
	return(RetVal);
}
/*! \fn int ILibAsyncUDPSocket_SetMulticastInterface(ILibAsyncUDPSocket_SocketModule module, int localInterface)
	\brief Sets the local interface to use, when multicasting
	\param module The ILibAsyncUDPSocket_SocketModule handle to set the interface on
	\param localInterface The local IP address in network order, to use when multicasting
	\returns 0 = Success, Nonzero = Failure
*/
int ILibAsyncUDPSocket_SetMulticastInterface(ILibAsyncUDPSocket_SocketModule module, int localInterface)
{
#if defined(__SYMBIAN32__)
	return(0);
#else
#if !defined(_WIN32_WCE) || (defined(_WIN32_WCE) && _WIN32_WCE>=400)
	struct in_addr interface_addr;
#if defined(WIN32) || defined(_WIN32_WCE)
	SOCKET s = *((SOCKET*)ILibAsyncSocket_GetSocket(module));
#else
	int s = *((int*)ILibAsyncSocket_GetSocket(module));
#endif
	memset((char *)&interface_addr, 0, sizeof(interface_addr));

	interface_addr.s_addr = localInterface;
	return(setsockopt(s, IPPROTO_IP, IP_MULTICAST_IF,(char*)&interface_addr, sizeof(interface_addr)));
#else
	return(1);
#endif
#endif
}
/*! \fn int ILibAsyncUDPSocket_SetMulticastTTL(ILibAsyncUDPSocket_SocketModule module, unsigned char TTL)
	\brief Sets the Multicast TTL value
	\param module The ILibAsyncUDPSocket_SocketModule handle to set the Multicast TTL value
	\param TTL The Multicast-TTL value to use
	\returns 0 = Success, Nonzero = Failure
*/
int ILibAsyncUDPSocket_SetMulticastTTL(ILibAsyncUDPSocket_SocketModule module, unsigned char TTL)
{
#if defined(__SYMBIAN32__)
	return(0);
#else
	#if defined(WIN32) || defined(_WIN32_WCE)
	SOCKET s = *((SOCKET*)ILibAsyncSocket_GetSocket(module));
#else
	int s = *((int*)ILibAsyncSocket_GetSocket(module));
#endif
	return(setsockopt(s, IPPROTO_IP, IP_MULTICAST_TTL,(char*)&TTL, sizeof(TTL)));
#endif
}
