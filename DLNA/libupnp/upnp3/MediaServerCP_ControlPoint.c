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
* $Workfile: MediaServerCP_ControlPoint.c
* $Revision: #1.0.2718.23851
* $Author:   Intel Corporation, Intel Device Builder
* $Date:     2007年7月6日
*
*
*
*/


#if defined(WIN32) || defined(_WIN32_WCE)
#	ifndef MICROSTACK_NO_STDAFX
#		include "stdafx.h"
#	endif
char* MSCP_PLATFORM = "WINDOWS UPnP/1.0 Intel MicroStack/1.0.2718";
#elif defined(__SYMBIAN32__)
char* MSCP_PLATFORM = "SYMBIAN UPnP/1.0 Intel MicroStack/1.0.2718";
#else
char* MSCP_PLATFORM = "POSIX UPnP/1.0 Intel MicroStack/1.0.2718, RealtekMediaCenter, DLNADOC/1.50";
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
#include "ILibSSDPClient.h"
#include "ILibWebServer.h"
#include "ILibWebClient.h"
#include "ILibAsyncSocket.h"
#include "MediaServerCP_ControlPoint.h"

#if defined(WIN32) && !defined(_WIN32_WCE)
#include <crtdbg.h>
#endif

#define UPNP_PORT 1900
#define UPNP_GROUP "239.255.255.250"
#define MSCP_MIN(a,b) (((a)<(b))?(a):(b))

#define MSCP_InvocationPriorityLevel ILibAsyncSocket_QOS_CONTROL

#define INVALID_DATA 0
#define DEBUGSTATEMENT(x)
#define LVL3DEBUG(x)

static const char *UPNPCP_SOAP_Header = "POST %s HTTP/1.1\r\nHost: %s:%d\r\nUser-Agent: %s\r\nSOAPACTION: \"%s#%s\"\r\nContent-Type: text/xml; charset=\"utf-8\"\r\nContent-Length: %d\r\n\r\n";
static const char *UPNPCP_SOAP_BodyHead = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:";
static const char *UPNPCP_SOAP_BodyTail = "></s:Body></s:Envelope>";

void MSCP_Renew(void *state);
void MSCP_SSDP_Sink(void *sender, char* UDN, int Alive, char* LocationURL, int Timeout,UPnPSSDP_MESSAGE m, void *cp);

struct CustomUserData
{
   int Timeout;
   char* buffer;
   
   char *UDN;
   char *LocationURL;
};
struct MSCP_CP
{
   void (*PreSelect)(void* object,fd_set *readset, fd_set *writeset, fd_set *errorset, int* blocktime);
   void (*PostSelect)(void* object,int slct, fd_set *readset, fd_set *writeset, fd_set *errorset);
   void (*Destroy)(void* object);
   void (*DiscoverSink)(struct UPnPDevice *device);
   void (*RemoveSink)(struct UPnPDevice *device);
   
   //ToDo: Check to see if this is necessary
   void (*EventCallback_DimmingService_LoadLevelStatus)(struct UPnPService* Service,unsigned char value);
   void (*EventCallback_SwitchPower_Status)(struct UPnPService* Service,int value);
   
   struct LifeTimeMonitorStruct *LifeTimeMonitor;
   
   void *HTTP;
   void *SSDP;
   void *WebServer;
   void *User;
   
   sem_t DeviceLock;
   void* SIDTable;
   void* DeviceTable_UDN;
   void* DeviceTable_Tokens;
   void* DeviceTable_URI;
   
   void *Chain;
   int RecheckFlag;
   int AddressListLength;
   int *AddressList;
   UPnPDeviceDiscoveryErrorHandler ErrorDispatch;
};

void (*MSCP_EventCallback_ConnectionManager_SourceProtocolInfo)(struct UPnPService* Service,char* value);
void (*MSCP_EventCallback_ConnectionManager_SinkProtocolInfo)(struct UPnPService* Service,char* value);
void (*MSCP_EventCallback_ConnectionManager_CurrentConnectionIDs)(struct UPnPService* Service,char* value);
void (*MSCP_EventCallback_ContentDirectory_ContainerUpdateIDs)(struct UPnPService* Service,char* value);
void (*MSCP_EventCallback_ContentDirectory_SystemUpdateID)(struct UPnPService* Service,unsigned int value);


struct InvokeStruct
{
   struct UPnPService *Service;
   void *CallbackPtr;
   void *User;
};
struct UPnPServiceInfo
{
   char* serviceType;
   char* SCPDURL;
   char* controlURL;
   char* eventSubURL;
   char* serviceId;
   struct UPnPServiceInfo *Next;
};
struct MSCP__Stack
{
   void *data;
   struct MSCP__Stack *next;
};


void MSCP_SetUser(void *token, void *user)
{
   ((struct MSCP_CP*)token)->User = user;
}
void* MSCP_GetUser(void *token)
{
   return(((struct MSCP_CP*)token)->User);
}
void MSCP_CP_ProcessDeviceRemoval(struct MSCP_CP* CP, struct UPnPDevice *device);



void MSCP_DestructUPnPService(struct UPnPService *service)
{
   struct UPnPAction *a1,*a2;
   struct UPnPStateVariable *sv1,*sv2;
   int i;
   
   a1 = service->Actions;
   while(a1!=NULL)
   {
      a2 = a1->Next;
      free(a1->Name);
      free(a1);
      a1 = a2;
   }
   
   sv1 = service->Variables;
   while(sv1!=NULL)
   {
      sv2 = sv1->Next;
      free(sv1->Name);
      if(sv1->Min!=NULL) {free(sv1->Min);}
      if(sv1->Max!=NULL) {free(sv1->Max);}
      if(sv1->Step!=NULL) {free(sv1->Step);}
      for(i=0;i<sv1->NumAllowedValues;++i)
      {
         free(sv1->AllowedValues[i]);
      }
      if(sv1->AllowedValues!=NULL) {free(sv1->AllowedValues);}
      free(sv1);
      sv1 = sv2;
   }
   if(service->ControlURL!=NULL) {free(service->ControlURL);}
   if(service->SCPDURL!=NULL) {free(service->SCPDURL);}
   if(service->ServiceId!=NULL) {free(service->ServiceId);}
   if(service->ServiceType!=NULL) {free(service->ServiceType);}
   if(service->SubscriptionURL!=NULL) {free(service->SubscriptionURL);}
   if(service->SubscriptionID!=NULL)
   {
      ILibLifeTime_Remove(((struct MSCP_CP*)service->Parent->CP)->LifeTimeMonitor,service);
      ILibDeleteEntry(((struct MSCP_CP*)service->Parent->CP)->SIDTable,service->SubscriptionID,(int)strlen(service->SubscriptionID));
      free(service->SubscriptionID);
      service->SubscriptionID = NULL;
   }
   
   free(service);
}
void MSCP_DestructUPnPDevice(struct UPnPDevice *device)
{
   struct UPnPDevice *d1,*d2;
   struct UPnPService *s1,*s2;
   int iconIndex;
   
   void *en,*en2;
   char *key;
   int keyLen;
   void *val,*ht2;
   
   
   d1 = device->EmbeddedDevices;
   while(d1!=NULL)
   {
      d2 = d1->Next;
      MSCP_DestructUPnPDevice(d1);
      d1 = d2;
   }
   
   s1 = device->Services;
   while(s1!=NULL)
   {
      s2 = s1->Next;
      MSCP_DestructUPnPService(s1);
      s1 = s2;
   }
   
   LVL3DEBUG(printf("\r\n\r\nDevice Destructed\r\n");)
   if(device->PresentationURL!=NULL) {free(device->PresentationURL);}
   if(device->ManufacturerName!=NULL) {free(device->ManufacturerName);}
   if(device->ManufacturerURL!=NULL) {free(device->ManufacturerURL);}
   if(device->ModelName!=NULL) {free(device->ModelName);}
   if(device->ModelNumber!=NULL) {free(device->ModelNumber);}
   if(device->ModelURL!=NULL) {free(device->ModelURL);}
   if(device->ModelDescription!=NULL) {free(device->ModelDescription);}
   if(device->DeviceType!=NULL) {free(device->DeviceType);}
   if(device->FriendlyName!=NULL) {free(device->FriendlyName);}
   if(device->LocationURL!=NULL) {free(device->LocationURL);}
   if(device->UDN!=NULL) {free(device->UDN);}
   if(device->InterfaceToHost!=NULL) {free(device->InterfaceToHost);}
   if(device->Reserved3!=NULL) {free(device->Reserved3);}
   if(device->IconsLength>0)
   {
      for(iconIndex=0;iconIndex<device->IconsLength;++iconIndex)
      {
         if(device->Icons[iconIndex].mimeType!=NULL){free(device->Icons[iconIndex].mimeType);}
         if(device->Icons[iconIndex].url!=NULL){free(device->Icons[iconIndex].url);}
      }
      free(device->Icons);
   }
   
   if(device->CustomTagTable != NULL) 
   {
      en = ILibHashTree_GetEnumerator(device->CustomTagTable);
      while(ILibHashTree_MoveNext(en)==0)
      {
         ILibHashTree_GetValue(en,&key,&keyLen,&ht2);
         en2 = ILibHashTree_GetEnumerator(ht2);
         while(ILibHashTree_MoveNext(en2)==0)
         {
            ILibHashTree_GetValue(en2,&key,&keyLen,&val);
            if(val!=NULL)
            {
               free(val);
            }
         }
         ILibHashTree_DestroyEnumerator(en2);
         ILibDestroyHashTree(ht2);
      }
      ILibHashTree_DestroyEnumerator(en);
      ILibDestroyHashTree(device->CustomTagTable);
   }
   
   
   free(device);
}


/*! \fn MSCP_AddRef(struct UPnPDevice *device)
\brief Increments the reference counter for a UPnP device
\param device UPnP device
*/
void MSCP_AddRef(struct UPnPDevice *device)
{
   struct MSCP_CP *CP = (struct MSCP_CP*)device->CP;
   struct UPnPDevice *d = device;
   sem_wait(&(CP->DeviceLock));
   while(d->Parent!=NULL) {d = d->Parent;}
   ++d->ReferenceCount;
   sem_post(&(CP->DeviceLock));
}

void MSCP_CheckfpDestroy(struct UPnPDevice *device)
{
   struct UPnPDevice *ed = device->EmbeddedDevices;
   if(device->fpDestroy!=NULL) {device->fpDestroy(device);}
   while(ed!=NULL)
   {
      MSCP_CheckfpDestroy(ed);
      ed = ed->Next;
   }
}
/*! \fn MSCP_Release(struct UPnPDevice *device)
\brief Decrements the reference counter for a UPnP device.
\para Device will be disposed when the counter becomes zero.
\param device UPnP device
*/
void MSCP_Release(struct UPnPDevice *device)
{
   struct MSCP_CP *CP = (struct MSCP_CP*)device->CP;
   struct UPnPDevice *d = device;
   sem_wait(&(CP->DeviceLock));
   while(d->Parent!=NULL) {d = d->Parent;}
   --d->ReferenceCount;
   if(d->ReferenceCount<=0)
   {
      MSCP_CheckfpDestroy(device);
      MSCP_DestructUPnPDevice(d);
   }
   sem_post(&(CP->DeviceLock));
}
//void UPnPDeviceDescriptionInterruptSink(void *sender, void *user1, void *user2)
//{
   //	struct CustomUserData *cd = (struct CustomUserData*)user1;
   //	free(cd->buffer);
   //	free(user1);
   //}
int MSCP_IsLegacyDevice(struct packetheader *header)
{
   struct packetheader_field_node *f;
   struct parser_result_field *prf;
   struct parser_result *r,*r2;
   int Legacy=1;
   // Check version of Device
   f = header->FirstField;
   while(f!=NULL)
   {
      if(f->FieldLength==6 && strncasecmp(f->Field,"SERVER",6)==0)
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
                  Legacy=0;
               }
               ILibDestructParserResults(r2);
            }
            prf = prf->NextResult;
         }
         ILibDestructParserResults(r);
      }
      if(Legacy)
      {
         f = f->NextField;
      }
      else
      {
         break;
      }
   }
   return(Legacy);
}
void MSCP_Push(struct MSCP__Stack **pp_Top, void *data)
{
   struct MSCP__Stack *frame = (struct MSCP__Stack*)malloc(sizeof(struct MSCP__Stack));
   frame->data = data;
   frame->next = *pp_Top;
   *pp_Top = frame;
}
void *MSCP_Pop(struct MSCP__Stack **pp_Top)
{
   struct MSCP__Stack *frame = *pp_Top;
   void *RetVal = NULL;
   
   if(frame!=NULL)
   {
      *pp_Top = frame->next;
      RetVal = frame->data;
      free(frame);
   }
   return(RetVal);
}
void *MSCP_Peek(struct MSCP__Stack **pp_Top)
{
   struct MSCP__Stack *frame = *pp_Top;
   void *RetVal = NULL;
   
   if(frame!=NULL)
   {
      RetVal = (*pp_Top)->data;
   }
   return(RetVal);
}
void MSCP_Flush(struct MSCP__Stack **pp_Top)
{
   while(MSCP_Pop(pp_Top)!=NULL) {}
   *pp_Top = NULL;
}

/*! \fn MSCP_GetDeviceAtUDN(void *v_CP,char* UDN)
\brief Fetches a device with a particular UDN
\param v_CP Control Point Token
\param UDN Unique Device Name
\returns UPnP device
*/
struct UPnPDevice* MSCP_GetDeviceAtUDN(void *v_CP,char* UDN)
{
   struct UPnPDevice *RetVal = NULL;
   struct MSCP_CP* CP = (struct MSCP_CP*)v_CP;
   
   ILibHashTree_Lock(CP->DeviceTable_UDN);
   RetVal = (struct UPnPDevice*)ILibGetEntry(CP->DeviceTable_UDN,UDN,(int)strlen(UDN));
   ILibHashTree_UnLock(CP->DeviceTable_UDN);
   return(RetVal);
}
struct packetheader *MSCP_BuildPacket(char* IP, int Port, char* Path, char* cmd)
{
   struct packetheader *RetVal = ILibCreateEmptyPacket();
   char* HostLine = (char*)malloc((int)strlen(IP)+7);
   int HostLineLength = sprintf(HostLine,"%s:%d",IP,Port);
   
   ILibSetVersion(RetVal,"1.1",3);
   ILibSetDirective(RetVal,cmd,(int)strlen(cmd),Path,(int)strlen(Path));
   ILibAddHeaderLine(RetVal,"Host",4,HostLine,HostLineLength);
   ILibAddHeaderLine(RetVal,"User-Agent",10,MSCP_PLATFORM,(int)strlen(MSCP_PLATFORM));
   free(HostLine);
   return(RetVal);
}

void MSCP_RemoveServiceFromDevice(struct UPnPDevice *device, struct UPnPService *service)
{
   struct UPnPService *s = device->Services;
   
   if(s==service)
   {
      device->Services = s->Next;
      MSCP_DestructUPnPService(service);
      return;
   }
   while(s->Next!=NULL)
   {
      if(s->Next == service)
      {
         s->Next = s->Next->Next;
         MSCP_DestructUPnPService(service);
         return;
      }
      s = s->Next;
   }
}

void MSCP_ProcessDevice(void *v_cp,struct UPnPDevice *device)
{
   struct MSCP_CP* cp = (struct MSCP_CP*)v_cp;
   struct UPnPDevice *EmbeddedDevice = device->EmbeddedDevices;
   
   // Copy the LocationURL if necessary
   if(device->LocationURL==NULL && device->Parent!=NULL && device->Parent->LocationURL!=NULL)
   {
      device->LocationURL = (char*)malloc(1+strlen(device->Parent->LocationURL));
      strcpy(device->LocationURL,device->Parent->LocationURL);
   }
   while(EmbeddedDevice!=NULL)
   {
      MSCP_ProcessDevice(v_cp,EmbeddedDevice);
      EmbeddedDevice = EmbeddedDevice->Next;
   }
   
   // Create a table entry for each Unique Device Name, for easy mapping
   // of ssdp:byebye packets. This way any byebye packet from the device
   // heirarchy will result in the device being removed.
   
   ILibHashTree_Lock(cp->DeviceTable_UDN);
   ILibAddEntry(cp->DeviceTable_UDN,device->UDN,(int)strlen(device->UDN),device);
   ILibHashTree_UnLock(cp->DeviceTable_UDN);
}

/*! fn MSCP_PrintUPnPDevice(int indents, struct UPnPDevice *device)
\brief Debug method that displays the device object structure
\param indents The number of spaces to indent each sub-entry
\param device The device to display
*/
void MSCP_PrintUPnPDevice(int indents, struct UPnPDevice *device)
{
   struct UPnPService *s;
   struct UPnPDevice *d;
   struct UPnPAction *a;
   int x=0;
   
   for(x=0;x<indents;++x) {printf(" ");}
   printf("Device: %s\r\n",device->DeviceType);
   
   for(x=0;x<indents;++x) {printf(" ");}
   printf("Friendly: %s\r\n",device->FriendlyName);
   
   s = device->Services;
   while(s!=NULL)
   {
      for(x=0;x<indents;++x) {printf(" ");}
      printf("   Service: %s\r\n",s->ServiceType);
      a = s->Actions;
      while(a!=NULL)
      {
         for(x=0;x<indents;++x) {printf(" ");}
         printf("      Action: %s\r\n",a->Name);
         a = a->Next;
      }
      s = s->Next;
   }
   
   d = device->EmbeddedDevices;
   while(d!=NULL)
   {
      MSCP_PrintUPnPDevice(indents+5,d);
      d = d->Next;
   }
}

/*! \fn MSCP_GetService(struct UPnPDevice *device, char* ServiceName, int length)
\brief Obtains a specific UPnP service instance of appropriate version, from within a device
\para
This method returns services who's version is greater than or equal to that specified within \a ServiceName
\param device UPnP device
\param ServiceName Service Type
\param length Length of \a ServiceName
\returns UPnP service
*/
struct UPnPService *MSCP_GetService(struct UPnPDevice *device, char* ServiceName, int length)
{
   struct UPnPService *RetService = NULL;
   struct UPnPService *s = device->Services;
   struct parser_result *pr,*pr2;
   
   pr = ILibParseString(ServiceName,0,length,":",1);
   while(s!=NULL)
   {
      pr2 = ILibParseString(s->ServiceType,0,(int)strlen(s->ServiceType),":",1);
      if(length-pr->LastResult->datalength >= (int)strlen(s->ServiceType)-pr2->LastResult->datalength && memcmp(ServiceName,s->ServiceType,length-pr->LastResult->datalength)==0)
      {
         if(atoi(pr->LastResult->data)<=atoi(pr2->LastResult->data))
         {
            RetService = s;
            ILibDestructParserResults(pr2);
            break;
         }
      }
      ILibDestructParserResults(pr2);
      s = s->Next;
   }
   ILibDestructParserResults(pr);
   
   return(RetService);
}

/*! \fn MSCP_GetService_ConnectionManager(struct UPnPDevice *device)
\brief Returns the ConnectionManager service from the specified device	\par
Service Type = urn:schemas-upnp-org:service:ConnectionManager<br>
Version >= 1
\param device The device object to query
\returns A pointer to the service object
*/
struct UPnPService *MSCP_GetService_MediaServer_ContentDirectory(struct UPnPDevice *device)
{
   return(MSCP_GetService(device,"urn:schemas-upnp-org:service:ContentDirectory:1",47));
}
/*! \fn MSCP_GetService_ConnectionManager(struct UPnPDevice *device)
\brief Returns the ConnectionManager service from the specified device	\par
Service Type = urn:schemas-upnp-org:service:ConnectionManager<br>
Version >= 1
\param device The device object to query
\returns A pointer to the service object
*/
struct UPnPService *MSCP_GetService_ConnectionManager(struct UPnPDevice *device)
{
   return(MSCP_GetService(device,"urn:schemas-upnp-org:service:ConnectionManager:1",48));
}
/*! \fn MSCP_GetService_ContentDirectory(struct UPnPDevice *device)
\brief Returns the ContentDirectory service from the specified device	\par
Service Type = urn:schemas-upnp-org:service:ContentDirectory<br>
Version >= 1
\param device The device object to query
\returns A pointer to the service object
*/
struct UPnPService *MSCP_GetService_ContentDirectory(struct UPnPDevice *device)
{
   return(MSCP_GetService(device,"urn:schemas-upnp-org:service:ContentDirectory:1",47));
}


struct UPnPDevice *MSCP_GetDevice2(struct UPnPDevice *device, int index, int *c_Index)
{
   struct UPnPDevice *RetVal = NULL;
   struct UPnPDevice *e_Device = NULL;
   int currentIndex = *c_Index;
   
   if(strncmp(device->DeviceType,"urn:schemas-upnp-org:device:MediaServer:",40)==0 && atoi(device->DeviceType+40)>=1)
   
   {
      ++currentIndex;
      if(currentIndex==index)
      {
         *c_Index = currentIndex;
         return(device);
      }
   }
   
   e_Device = device->EmbeddedDevices;
   while(e_Device!=NULL)
   {
      RetVal = MSCP_GetDevice2(e_Device,index,&currentIndex);
      if(RetVal!=NULL)
      {
         break;
      }
      e_Device = e_Device->Next;
   }
   
   *c_Index = currentIndex;
   return(RetVal);
}
struct UPnPDevice* MSCP_GetDevice1(struct UPnPDevice *device,int index)
{
   int c_Index = -1;
   return(MSCP_GetDevice2(device,index,&c_Index));
}
int MSCP_GetDeviceCount(struct UPnPDevice *device)
{
   int RetVal = 0;
   struct UPnPDevice *e_Device = device->EmbeddedDevices;
   
   while(e_Device!=NULL)
   {
      RetVal += MSCP_GetDeviceCount(e_Device);
      e_Device = e_Device->Next;
   }
   if(strncmp(device->DeviceType,"urn:schemas-upnp-org:device:MediaServer:1",41)==0)
   {
      ++RetVal;
   }
   
   return(RetVal);
}

//
// Internal method to parse the SOAP Fault
//
int MSCP_GetErrorCode(char *buffer, int length)
{
   int RetVal = 500;
   struct ILibXMLNode *xml,*rootXML;
   
   char *temp;
   int tempLength;
   
   if(buffer==NULL) {return(RetVal);}
   
   rootXML = xml = ILibParseXML(buffer,0,length);
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==8 && memcmp(xml->Name,"Envelope",8)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==4 && memcmp(xml->Name,"Body",4)==0)
               {
                  xml = xml->Next;
                  while(xml!=NULL)
                  {
                     if(xml->NameLength==5 && memcmp(xml->Name,"Fault",5)==0)
                     {
                        xml = xml->Next;
                        while(xml!=NULL)
                        {
                           if(xml->NameLength==6 && memcmp(xml->Name,"detail",6)==0)
                           {
                              xml = xml->Next;
                              while(xml!=NULL)
                              {
                                 if(xml->NameLength==9 && memcmp(xml->Name,"MSCP_Error",9)==0)
                                 {
                                    xml = xml->Next;
                                    while(xml!=NULL)
                                    {
                                       if(xml->NameLength==9 && memcmp(xml->Name,"errorCode",9)==0)
                                       {
                                          tempLength = ILibReadInnerXML(xml,&temp);
                                          temp[tempLength] = 0;
                                          RetVal =atoi(temp);
                                          xml = NULL;
                                       }
                                       if(xml!=NULL) {xml = xml->Peer;}
                                    }
                                 }
                                 if(xml!=NULL) {xml = xml->Peer;}
                              }
                           }
                           if(xml!=NULL) {xml = xml->Peer;}
                        }
                     }
                     if(xml!=NULL) {xml = xml->Peer;}
                  }
               }
               if(xml!=NULL) {xml = xml->Peer;}
            }
         }
         if(xml!=NULL) {xml = xml->Peer;}
      }
   }
   ILibDestructXMLNodeList(rootXML);
   return(RetVal);
}

//
// Internal method to parse the SCPD document
//
void MSCP_ProcessSCPD(char* buffer, int length, struct UPnPService *service)
{
   struct UPnPAction *action;
   struct UPnPStateVariable *sv = NULL;
   struct UPnPAllowedValue *av = NULL;
   struct UPnPAllowedValue *avs = NULL;
   
   struct ILibXMLNode *xml,*rootXML;
   int index = 0;
   
   char* tempString;
   int tempStringLength;
   
   struct UPnPDevice *root = service->Parent;
   while(root->Parent!=NULL) {root = root->Parent;}
   
   rootXML = xml = ILibParseXML(buffer,0,length);
   if(ILibProcessXMLNodeList(xml)!=0)
   {
      //
      // The XML Document was not well formed
      //
      root->SCPDError=UPNP_ERROR_SCPD_NOT_WELL_FORMED;
      ILibDestructXMLNodeList(rootXML);
      return;
   }
   
   while(xml!=NULL && strncmp(xml->Name,"!",1)==0)
   {
      xml = xml->Next;
   }
   xml = xml->Next;
   while(xml!=NULL)
   {
      //
      // Iterate all the actions in the actionList element
      //
      if(xml->NameLength==10 && memcmp(xml->Name,"actionList",10)==0)
      {
         xml = xml->Next;
         while(xml != NULL)
         {
            if(xml->NameLength==6 && memcmp(xml->Name,"action",6)==0)
            {
               //
               // Action element
               //
               action = (struct UPnPAction*)malloc(sizeof(struct UPnPAction));
               action->Name = NULL;
               action->Next = service->Actions;
               service->Actions = action;
               
               xml = xml->Next;
               while(xml != NULL)
               {
                  if(xml->NameLength==4 && memcmp(xml->Name,"name",4)==0)
                  {
                     tempStringLength = ILibReadInnerXML(xml,&tempString);
                     action->Name = (char*)malloc(1+tempStringLength);
                     memcpy(action->Name,tempString,tempStringLength);
                     action->Name[tempStringLength] = '\0';
                  }
                  if(xml->Peer==NULL)
                  {
					 if(xml->Parent != NULL)
					 {
						 xml = xml->Parent;
					 }
					 else
					 {
						 xml = xml->Next;
					 }
					 break;
                  }
                  else
                  {
                     xml = xml->Peer;
                  }
               }
            }
			if(xml != NULL)
			{
				if(xml->Peer==NULL)
				{
					if(xml->Parent != NULL)
					{
						xml = xml->Parent;
					}
					else
					{
						xml = xml->Next;
					}
					break;
				}
				else
				{
					xml = xml->Peer;
				}
			}
         }
      }
      //
      // Iterate all the StateVariables in the state table
      //
      if(xml->NameLength==17 && memcmp(xml->Name,"serviceStateTable",17)==0)
      {
         if(xml->Next->StartTag!=0)
         {
            xml = xml->Next;
            while(xml != NULL)
            {
               if(xml->NameLength==13 && memcmp(xml->Name,"stateVariable",13)==0)
               {
                  //
                  // Initialize a new state variable
                  //
                  sv = (struct UPnPStateVariable*)malloc(sizeof(struct UPnPStateVariable));
                  sv->AllowedValues = NULL;
                  sv->NumAllowedValues = 0;
                  sv->Max = NULL;
                  sv->Min = NULL;
                  sv->Step = NULL;
                  sv->Name = NULL;
                  sv->Next = service->Variables;
                  service->Variables = sv;
                  sv->Parent = service;
                  
                  xml = xml->Next;
                  while(xml != NULL)
                  {
                     if(xml->NameLength==4 && memcmp(xml->Name,"name",4)==0)
                     {
                        //
                        // Populate the name
                        //
                        tempStringLength = ILibReadInnerXML(xml,&tempString);
                        sv->Name = (char*)malloc(1+tempStringLength);
                        memcpy(sv->Name,tempString,tempStringLength);
                        sv->Name[tempStringLength] = '\0';
                     }
                     if(xml->NameLength==16 && memcmp(xml->Name,"allowedValueList",16)==0)
                     {
                        //
                        // This state variable defines an allowed value list
                        //
                        if(xml->Next->StartTag!=0)
                        {
                           avs = NULL;
                           xml = xml->Next;
                           while(xml != NULL)
                           {
                              //
                              // Iterate through all the allowed values, and reference them
                              //
                              if(xml->NameLength==12 && memcmp(xml->Name,"allowedValue",12)==0)
                              {
                                 av = (struct UPnPAllowedValue*)malloc(sizeof(struct UPnPAllowedValue));
                                 av->Next = avs;
                                 avs = av;
                                 
                                 tempStringLength = ILibReadInnerXML(xml,&tempString);
                                 av->Value = (char*)malloc(1+tempStringLength);
                                 memcpy(av->Value,tempString,tempStringLength);
                                 av->Value[tempStringLength] = '\0';
                              }
                              if(xml->Peer!=NULL)
                              {
                                 xml = xml->Peer;
                              }
                              else
                              {
								  if(xml->Parent != NULL)
								  {
									  xml = xml->Parent;
								  }
								  else
								  {
									  xml = xml->Next;								  
								  }
								  break;
                              }
                           }
                           av = avs;
                           while(av!=NULL)
                           {
                              ++sv->NumAllowedValues;
                              av = av->Next;
                           }
                           av = avs;
                           sv->AllowedValues = (char**)malloc(sv->NumAllowedValues*sizeof(char*));
                           for(index=0;index<sv->NumAllowedValues;++index)
                           {
                              sv->AllowedValues[index] = av->Value;
                              av = av->Next;
                           }
                           av = avs;
                           while(av!=NULL)
                           {
                              avs = av->Next;
                              free(av);
                              av = avs;
                           }
                        }
                     }
                     if(xml->NameLength==17 && memcmp(xml->Name,"allowedValueRange",17)==0)
                     {
                        //
                        // The state variable defines a range
                        //
                        if(xml->Next->StartTag!=0)
                        {
                           xml = xml->Next;
                           while(xml != NULL)
                           {
                              if(xml->NameLength==7)
                              {
                                 if(memcmp(xml->Name,"minimum",7)==0)
                                 {
                                    //
                                    // Set the minimum range
                                    //
                                    tempStringLength = ILibReadInnerXML(xml,&tempString);
                                    sv->Min = (char*)malloc(1+tempStringLength);
                                    memcpy(sv->Min,tempString,tempStringLength);
                                    sv->Min[tempStringLength] = '\0';
                                 }
                                 else if(memcmp(xml->Name,"maximum",7)==0)
                                 {
                                    //
                                    // Set the maximum range
                                    //
                                    tempStringLength = ILibReadInnerXML(xml,&tempString);
                                    sv->Max = (char*)malloc(1+tempStringLength);
                                    memcpy(sv->Max,tempString,tempStringLength);
                                    sv->Max[tempStringLength] = '\0';
                                 }
                              }
                              if(xml->NameLength==4 && memcmp(xml->Name,"step",4)==0)
                              {
                                 //
                                 // Set the stepping
                                 //
                                 tempStringLength = ILibReadInnerXML(xml,&tempString);
                                 sv->Step = (char*)malloc(1+tempStringLength);
                                 memcpy(sv->Step,tempString,tempStringLength);
                                 sv->Step[tempStringLength] = '\0';
                              }
                              if(xml->Peer!=NULL)
                              {
                                 xml = xml->Peer;
                              }
                              else
                              {
								  if(xml->Parent != NULL)
								  {
									  xml = xml->Parent;
								  }
								  else
								  {
									  xml = xml->Next;
								  }
								  break;
                              }
                           }
                        }
                     }
					 if(xml != NULL)
					 {
						 if(xml->Peer!=NULL)
						 {
							 xml = xml->Peer;
						 }
						 else
						 {
							 if(xml->Parent != NULL)
							 {
								 xml = xml->Parent;
							 }
							 else
							 {
								 xml = xml->Next;
							 }
							 break;
						 }
					 }
                  }
               }
			   if(xml != NULL)
			   {
				   if(xml->Peer!=NULL)
				   {
					   xml = xml->Peer;
				   }
				   else
				   {
					   if(xml->Parent != NULL)
					   {
						   xml = xml->Parent;
					   }
					   else
					   {
						   xml = xml->Next;
					   }
					   break;
				   }
			   }
			}
         }
      }
	  if(xml != NULL)
		  xml = xml->Peer;
   }
   
   ILibDestructXMLNodeList(rootXML);
}


//
// Internal method called from SSDP dispatch, when the
// IP Address for a device has changed
//
void MSCP_InterfaceChanged(struct UPnPDevice *device)
{
   void *cp = device->CP;
   char *UDN;
   char *LocationURL;
   int Timeout;
   
   Timeout = device->CacheTime;
   UDN = (char*)malloc((int)strlen(device->UDN)+1);
   strcpy(UDN,device->UDN);
   LocationURL = device->Reserved3;
   device->Reserved3 = NULL;
   
   MSCP_SSDP_Sink(NULL, device->UDN, 0, NULL, 0,UPnPSSDP_NOTIFY,device->CP);
   MSCP_SSDP_Sink(NULL, UDN, -1, LocationURL, Timeout, UPnPSSDP_NOTIFY,cp);
   
   free(UDN);
   free(LocationURL);
}

//
// Internal Timed Event Sink, that is called when a device
// has failed to refresh it's NOTIFY packets. So
// we'll assume the device is no longer available
//
void MSCP_ExpiredDevice(struct UPnPDevice *device)
{
   LVL3DEBUG(printf("Device[%s] failed to re-advertise in a timely manner\r\n",device->FriendlyName);)
   while(device->Parent!=NULL) {device = device->Parent;}
   MSCP_SSDP_Sink(NULL, device->UDN, 0, NULL, 0,UPnPSSDP_NOTIFY,device->CP);
}

//
// The discovery process for the device is complete. Just need to 
// set the reference counters, and notify the layers above us
//
void MSCP_FinishProcessingDevice(struct MSCP_CP* CP, struct UPnPDevice *RootDevice)
{
   int Timeout = RootDevice->CacheTime;
   struct UPnPDevice *RetDevice;
   int i=0;
   
   RootDevice->ReferenceCount = 1;
   do
   {
      RetDevice = MSCP_GetDevice1(RootDevice,i++);
      if(RetDevice!=NULL)
      {
         //
         // Set Reserved to non-zero to indicate that we are passing
         // this instance up the stack to the app layer above. Add a reference
         // to the device while we're at it.
         //
         RetDevice->Reserved=1;
         MSCP_AddRef(RetDevice);
         if(CP->DiscoverSink!=NULL)
         {
            //
            // Notify the app layer above
            //
            CP->DiscoverSink(RetDevice);
         }
      }
   }while(RetDevice!=NULL);
   //
   // Set a timed callback for the refresh cycle of the device. If the
   // device doesn't refresh by then, we'll remove this device.
   //
   ILibLifeTime_Add(CP->LifeTimeMonitor,RootDevice,Timeout,(void*)&MSCP_ExpiredDevice,NULL);
}

//
// Internal HTTP Sink for fetching the SCPD document
//
void MSCP_SCPD_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *dv,
void *sv,
int *PAUSE)
{
   struct UPnPDevice *device;
   struct UPnPService *service = (struct UPnPService*)sv;
   struct MSCP_CP *CP = service->Parent->CP;
   
   
   //
   // header==NULL if there was an error connecting to the device
   // StatusCode!=200 if there was an HTTP error in fetching the SCPD
   // done!=0 when the GET is complete
   //
   if(!(header==NULL || !ILibWebClientIsStatusOk(header->StatusCode)) && done!=0)
   {
      MSCP_ProcessSCPD(buffer,EndPointer, service);
      
      //
      // Fetch the root device
      //
      device = service->Parent;
      while(device->Parent!=NULL)
      {
         device = device->Parent;
      }
      //
      // Decrement the counter indicating how many
      // SCPD documents are left to fetch
      //
      --device->SCPDLeft;
      
      //
      // Check to see that we have all the SCPD documents we were
      // looking for
      //
      if(device->SCPDLeft==0)
      {
         if(device->SCPDError==0)
         {
            //
            // No errors, complete processing
            //
            MSCP_FinishProcessingDevice(CP,device);
         }
         else if(IsInterrupt==0)
         {
            //
            // Errors occured, so free all the resources, of this 
            // stale device
            //
            MSCP_CP_ProcessDeviceRemoval(CP,device);
            MSCP_DestructUPnPDevice(device);
         }
      }
   }
   else
   {
      //
      // Errors happened while trying to fetch the SCPD
      //
      if(done!=0 && (header==NULL || !ILibWebClientIsStatusOk(header->StatusCode)))
      {
         //
         // Get the root device
         //
         device = service->Parent;
         while(device->Parent!=NULL)
         {
            device = device->Parent;
         }
         
         //
         // Decrement the counter indicating how many
         // SCPD documents are left to fetch
         //
         --device->SCPDLeft;
         
         //
         // Set the flag indicating that an error has occured
         //
         device->SCPDError=1;
         if(device->SCPDLeft==0 && IsInterrupt==0)
         {
            //
            // If all the SCPD requests have been attempted, free
            // all the resources of this stale device
            //
            MSCP_CP_ProcessDeviceRemoval(CP,device);
            MSCP_DestructUPnPDevice(device);
         }
      }
   }
}

//
// Internal method used to calculate how many SCPD
// documents will need to be fetched
//
void MSCP_CalculateSCPD_FetchCount(struct UPnPDevice *device)
{
   int count = 0;
   struct UPnPDevice *root;
   struct UPnPDevice *e_Device = device->EmbeddedDevices;
   struct UPnPService *s;
   
   while(e_Device!=NULL)
   {
      MSCP_CalculateSCPD_FetchCount(e_Device);
      e_Device = e_Device->Next;
   }
   
   s = device->Services;
   while(s!=NULL)
   {
      ++count;
      s = s->Next;
   }
   
   root = device;
   while(root->Parent!=NULL)
   {
      root = root->Parent;
   }
   root->SCPDLeft += count;
}

//
// Internal method used to actually make the HTTP
// requests to obtain all the Service Description Documents.
//
void MSCP_SCPD_Fetch(struct UPnPDevice *device)
{
   struct UPnPDevice *e_Device = device->EmbeddedDevices;
   struct UPnPService *s;
   char *IP,*Path;
   unsigned short Port;
   struct packetheader *p;
   struct sockaddr_in addr;
   
   while(e_Device!=NULL)
   {
      //
      // We need to recursively call this on all the embedded devices,
      // to insure all of those services are accounted for
      //
      MSCP_SCPD_Fetch(e_Device);
      e_Device = e_Device->Next;
   }
   
   //
   // Iterate through all of the services contained in this device
   //
   s = device->Services;
   while(s!=NULL)
   {
      //
      // Parse the SCPD URL, and then build the request packet
      //
      ILibParseUri(s->SCPDURL,&IP,&Port,&Path);
      DEBUGSTATEMENT(printf("SCPD: %s Port: %d Path: %s\r\n",IP,Port,Path));
      p = MSCP_BuildPacket(IP,Port,Path,"GET");
      
      memset((char *)&addr, 0,sizeof(addr));
      addr.sin_family = AF_INET;
      addr.sin_addr.s_addr = inet_addr(IP);
      addr.sin_port = htons(Port);
      
      //
      // Actually make the HTTP Request
      //
      ILibWebClient_PipelineRequest(
      ((struct MSCP_CP*)device->CP)->HTTP,
      &addr,
      p,
      &MSCP_SCPD_Sink,
      device,
      s);
      
      //
      // Free the resources from our ILibParseURI() method call
      //
      free(IP);
      free(Path);
      s = s->Next;
   }
}

/*! \fn MSCP_GetCustomTagFromDevice(struct UPnPDevice *d, char* FullNameSpace, char* Name)
\brief Returns the value of the specified custom XML element
\param d The UPnP device to query
\param FullNameSpace The fully resolved namespace URI of the custom element
\param Name The local name of the custom element
\returns The value of the specified XML element. NULL if it doesn't exist.
*/
char *MSCP_GetCustomTagFromDevice(struct UPnPDevice *d, char* FullNameSpace, char* Name)
{
   if(ILibHasEntry(d->CustomTagTable,FullNameSpace,(int)strlen(FullNameSpace))!=0)
   {
      return(ILibGetEntry(ILibGetEntry(d->CustomTagTable,FullNameSpace,(int)strlen(FullNameSpace)),Name,(int)strlen(Name)));
   }
   else
   {
      return(NULL);
   }
}


void MSCP_ProcessDeviceXML_iconList(struct UPnPDevice *device,const char *BaseURL,struct ILibXMLNode *xml)
{
   struct ILibXMLNode *x2;
   int tempStringLength;
   char *tempString;
   struct parser_result *tpr;
   int numIcons = 0;
   struct UPnPIcon *Icons = NULL;
   char *iconURL=NULL;
   
   //
   // Count how many icons we have
   //
   x2 = xml;
   while(x2!=NULL)
   {
      if(x2->NameLength==4 && memcmp(x2->Name,"icon",4)==0)
      {
         ++numIcons;
      }
      x2 = x2->Peer;
   }
   Icons = (struct UPnPIcon*)malloc(numIcons*sizeof(struct UPnPIcon));
   memset(Icons,0,numIcons*sizeof(struct UPnPIcon));
   device->IconsLength = numIcons;
   device->Icons = Icons;
   numIcons = 0;
   
   while(xml!=NULL)
   {
      if(xml->NameLength==4 && memcmp(xml->Name,"icon",4)==0)
      {
         x2 = xml->Next;
         while(x2!=NULL)
         {
            if(x2->NameLength==5 && memcmp(x2->Name,"width",5)==0)
            {
               tempStringLength = ILibReadInnerXML(x2,&tempString);
               tempString[tempStringLength] = 0;
               Icons[numIcons].width = atoi(tempString);
            }
            if(x2->NameLength==6 && memcmp(x2->Name,"height",6)==0)
            {
               tempStringLength = ILibReadInnerXML(x2,&tempString);
               tempString[tempStringLength] = 0;
               Icons[numIcons].height = atoi(tempString);
            }
            if(x2->NameLength==5 && memcmp(x2->Name,"depth",5)==0)
            {
               tempStringLength = ILibReadInnerXML(x2,&tempString);
               tempString[tempStringLength] = 0;
               Icons[numIcons].colorDepth = atoi(tempString);
            }
            if(x2->NameLength==8 && memcmp(x2->Name,"mimetype",8)==0)
            {
               tempStringLength = ILibReadInnerXML(x2,&tempString);
               Icons[numIcons].mimeType = ILibString_Copy(tempString,tempStringLength);
            }
            if(x2->NameLength==3 && memcmp(x2->Name,"url",3)==0)
            {
               tempStringLength = ILibReadInnerXML(x2,&tempString);
               tempString[tempStringLength] = 0;
               tpr = ILibParseString(tempString,0,tempStringLength,"://",3);
               if(tpr->NumResults==1)
               {
                  /* RelativeURL */
                  if(tempString[0]=='/')
                  {
                     iconURL = (char*)malloc(1+strlen(BaseURL)+tempStringLength);
                     memcpy(iconURL,BaseURL,strlen(BaseURL));
                     strcpy(iconURL+strlen(BaseURL),tempString+1);
                  }
                  else
                  {
                     iconURL = (char*)malloc(2+strlen(BaseURL)+tempStringLength);
                     memcpy(iconURL,BaseURL,strlen(BaseURL));
                     strcpy(iconURL+strlen(BaseURL),tempString);
                  }
               }
               else
               {
                  /* AbsoluteURL */
                  iconURL = (char*)malloc(1+tempStringLength);
                  memcpy(iconURL,tempString,tempStringLength);
                  iconURL[tempStringLength] = '\0';
               }
               ILibDestructParserResults(tpr);
               Icons[numIcons].url = iconURL;
            }
            x2 = x2->Peer;
         }
         ++numIcons;
      }
      xml = xml->Peer;
   }
}
struct UPnPDevice* MSCP_ProcessDeviceXML_device(struct ILibXMLNode *xml, void *v_CP,const char *BaseURL, const char *RelativeBaseURL, int Timeout, int RecvAddr)
{
   struct ILibXMLNode *tempNode;
   struct ILibXMLAttribute *a,*root_a;
   char *tempString;
   int tempStringLength;
   
   char *tempString2;
   
   struct parser_result *tpr;
   
   char *ServiceId = NULL;
   int ServiceIdLength = 0;
   char* ServiceType = NULL;
   int ServiceTypeLength = 0;
   char* SCPDURL = NULL;
   int SCPDURLLength = 0;
   char* EventSubURL = NULL;
   int EventSubURLLength = 0;
   char* ControlURL = NULL;
   int ControlURLLength = 0;
   int ServiceMaxVersion;
   
   struct in_addr raddr;
   
   struct UPnPDevice *tempDevice;
   struct UPnPService *TempService;
   struct UPnPDevice *device = (struct UPnPDevice*)malloc(sizeof(struct UPnPDevice));
   memset(device,0,sizeof(struct UPnPDevice));
   
   device->CustomTagTable = ILibInitHashTree();
   
   
   device->MaxVersion=1;
   device->CP = v_CP;
   device->CacheTime = Timeout;
   device->InterfaceToHost = (char*)malloc(16);
   raddr.s_addr = RecvAddr;
   sprintf(device->InterfaceToHost,"%s",inet_ntoa(raddr));
   
   root_a = a = ILibGetXMLAttributes(xml);
   if(a!=NULL)
   {
      while(a!=NULL)
      {
         a->Name[a->NameLength]=0;
         if(strcasecmp(a->Name,"MaxVersion")==0)
         {
            a->Value[a->ValueLength]=0;
            device->MaxVersion = atoi(a->Value);
            break;
         }
         a = a->Next;
      }
      ILibDestructXMLAttributeList(root_a);
   }
   
   xml = xml->Next;
   while(xml!=NULL)
   {
      if(xml->NameLength==10 && memcmp(xml->Name,"deviceList",10)==0)
      {
         if(xml->Next->StartTag!=0)
         {
            //
            // Iterate through all the device elements contained in the
            // deviceList element
            //
            xml = xml->Next;
            while(xml != NULL)
            {
               if(xml->NameLength==6 && memcmp(xml->Name,"device",6)==0)
               {
                  //
                  // If this device contains other devices, then we can recursively call ourselves
                  //
                  tempDevice = MSCP_ProcessDeviceXML_device(xml,v_CP,BaseURL,RelativeBaseURL,Timeout, RecvAddr);
                  tempDevice->Parent = device;
                  tempDevice->Next = device->EmbeddedDevices;
                  device->EmbeddedDevices = tempDevice;
               }
               if(xml->Peer==NULL)
               {
				  if(xml->Parent != NULL)
				  {
					  xml = xml->Parent;
				  }
				  else
				  {
					  xml = xml->Next;
				  }
				  break;
               }
               else
               {
                  xml = xml->Peer;
               }
            }
         }
      } else
      if(xml->NameLength==3 && memcmp(xml->Name,"UDN",3)==0)
      {
         //
         // Copy the UDN out of the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         if(tempStringLength>5)
         {
            if(memcmp(tempString,"uuid:",5)==0)
            {
               tempString += 5;
               tempStringLength -= 5;
            }
            device->UDN = (char*)malloc(tempStringLength+1);
            memcpy(device->UDN,tempString,tempStringLength);
            device->UDN[tempStringLength] = '\0';
         }
      } else
      if(xml->NameLength==10 && memcmp(xml->Name,"deviceType",10) == 0)
      {
         //
         // Copy the device type out of the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         
         device->DeviceType = (char*)malloc(tempStringLength+1);
         memcpy(device->DeviceType,tempString,tempStringLength);
         device->DeviceType[tempStringLength] = '\0';
      } else
      if(xml->NameLength==12 && memcmp(xml->Name,"friendlyName",12) == 0)
      {
         //
         // Copy the friendly name out of the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         device->FriendlyName = (char*)malloc(1+tempStringLength);
         memcpy(device->FriendlyName,tempString,tempStringLength);
         device->FriendlyName[tempStringLength] = '\0';
         ILibInPlaceXmlUnEscape(device->FriendlyName);
      } else
      if(xml->NameLength==12 && memcmp(xml->Name,"manufacturer",12) == 0)
      {
         //
         // Copy the Manufacturer from the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         device->ManufacturerName = (char*)malloc(1+tempStringLength);
         memcpy(device->ManufacturerName,tempString,tempStringLength);
         device->ManufacturerName[tempStringLength] = '\0';
      } else
      if(xml->NameLength==15 && memcmp(xml->Name,"manufacturerURL",15) == 0)
      {
         //
         // Copy the Manufacturer's URL from the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         device->ManufacturerURL = (char*)malloc(1+tempStringLength);
         memcpy(device->ManufacturerURL,tempString,tempStringLength);
         device->ManufacturerURL[tempStringLength] = '\0';
      } else
      if(xml->NameLength==16 && memcmp(xml->Name,"modelDescription",16) == 0)
      {
         //
         // Copy the model meta data from the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         device->ModelDescription = (char*)malloc(1+tempStringLength);
         memcpy(device->ModelDescription,tempString,tempStringLength);
         device->ModelDescription[tempStringLength] = '\0';
      } else
      if(xml->NameLength==9 && memcmp(xml->Name,"modelName",9) == 0)
      {
         //
         // Copy the model meta data from the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         device->ModelName = (char*)malloc(1+tempStringLength);
         memcpy(device->ModelName,tempString,tempStringLength);
         device->ModelName[tempStringLength] = '\0';
      } else
      if(xml->NameLength==11 && memcmp(xml->Name,"modelNumber",11) == 0)
      {
         //
         // Copy the model meta data from the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         device->ModelNumber = (char*)malloc(1+tempStringLength);
         memcpy(device->ModelNumber,tempString,tempStringLength);
         device->ModelNumber[tempStringLength] = '\0';
      } else
      if(xml->NameLength==8 && memcmp(xml->Name,"modelURL",8) == 0)
      {
         //
         // Copy the model meta data from the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         device->ModelURL = (char*)malloc(1+tempStringLength);
         memcpy(device->ModelURL,tempString,tempStringLength);
         device->ModelURL[tempStringLength] = '\0';
      } else
      if(xml->NameLength==8 && memcmp(xml->Name,"iconList",8) == 0)
      {
         MSCP_ProcessDeviceXML_iconList(device,BaseURL,xml->Next);
      } else
      if(xml->NameLength==15 && memcmp(xml->Name,"presentationURL",15) == 0)
      {
         //
         // Copy the presentation URL from the description document
         //
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         tempString[tempStringLength] = 0;
         tpr = ILibParseString(tempString,0,tempStringLength,"://",3);
         if(tpr->NumResults==1)
         {
            /* RelativeURL */
            if(tempString[0]=='/')
            {
               device->PresentationURL = (char*)malloc(1+strlen(BaseURL)+tempStringLength);
               memcpy(device->PresentationURL,BaseURL,strlen(BaseURL));
               strcpy(device->PresentationURL+strlen(BaseURL),tempString+1);
            }
            else
            {
               device->PresentationURL = (char*)malloc(2+strlen(RelativeBaseURL)+tempStringLength);
               memcpy(device->PresentationURL,RelativeBaseURL,strlen(RelativeBaseURL));
               strcpy(device->PresentationURL+strlen(RelativeBaseURL),tempString);
            }
         }
         else
         {
            /* AbsoluteURL */
            device->PresentationURL = (char*)malloc(1+tempStringLength);
            memcpy(device->PresentationURL,tempString,tempStringLength);
            device->PresentationURL[tempStringLength] = '\0';
         }
         ILibDestructParserResults(tpr);
      } else
      if(xml->NameLength==9 && memcmp(xml->Name,"X_DLNACAP",9)==0)
      {
         tempString = ILibXML_LookupNamespace(xml,xml->NSTag,xml->NSLength);
         if(strcmp(tempString,"urn:schemas-dlna-org:device-1-0")==0)
         {
            tempStringLength = ILibReadInnerXML(xml,&tempString);
            tempString2 = (char*)malloc(tempStringLength+1);
            memcpy(tempString2,tempString,tempStringLength);
            tempString2[tempStringLength]=0;
            if(ILibGetEntry(device->CustomTagTable,"urn:schemas-dlna-org:device-1-0",31)==NULL)
            {
               ILibAddEntry(device->CustomTagTable,"urn:schemas-dlna-org:device-1-0",31,ILibInitHashTree());
            }
            ILibAddEntry(ILibGetEntry(device->CustomTagTable,"urn:schemas-dlna-org:device-1-0",31),xml->Name,xml->NameLength,tempString2);
         }
      } 
      else
      if(xml->NameLength==9 && memcmp(xml->Name,"X_DLNADOC",9)==0)
      {
         tempString = ILibXML_LookupNamespace(xml,xml->NSTag,xml->NSLength);
         if(strcmp(tempString,"urn:schemas-dlna-org:device-1-0")==0)
         {
            tempStringLength = ILibReadInnerXML(xml,&tempString);
            tempString2 = (char*)malloc(tempStringLength+1);
            memcpy(tempString2,tempString,tempStringLength);
            tempString2[tempStringLength]=0;
            if(ILibGetEntry(device->CustomTagTable,"urn:schemas-dlna-org:device-1-0",31)==NULL)
            {
               ILibAddEntry(device->CustomTagTable,"urn:schemas-dlna-org:device-1-0",31,ILibInitHashTree());
            }
            ILibAddEntry(ILibGetEntry(device->CustomTagTable,"urn:schemas-dlna-org:device-1-0",31),xml->Name,xml->NameLength,tempString2);
         }
      } 
      else
	  if(xml->NameLength==11 && memcmp(xml->Name,"X_REGZAAPPS",11)==0)  		  // Add for Toshiba Live Stream	  
	  {
	  	 tempString = ILibXML_LookupNamespace(xml,xml->NSTag,xml->NSLength);
         if(strcmp(tempString,"urn:schemas-toshiba-co-jp:regzadata-1-0")==0)
         {
            tempStringLength = ILibReadInnerXML(xml,&tempString);
            tempString2 = (char*)malloc(tempStringLength+1);
            memcpy(tempString2,tempString,tempStringLength);
            tempString2[tempStringLength]=0;
            if(ILibGetEntry(device->CustomTagTable,"urn:schemas-toshiba-co-jp:regzadata-1-0",39)==NULL)
            {
               ILibAddEntry(device->CustomTagTable,"urn:schemas-toshiba-co-jp:regzadata-1-0",39,ILibInitHashTree());
            }
            ILibAddEntry(ILibGetEntry(device->CustomTagTable,"urn:schemas-toshiba-co-jp:regzadata-1-0",39),xml->Name,xml->NameLength,tempString2);
         }
	  }
      else
      
      if(xml->NameLength==11 && memcmp(xml->Name,"serviceList",11)==0)
      {
         //
         // Iterate through all the services contained in the serviceList element
         //
         tempNode = xml;
         xml = xml->Next;
         while(xml!=NULL)
         {
            if(xml->NameLength==7 && memcmp(xml->Name,"service",7)==0)
            {
               ServiceType = NULL;
               ServiceTypeLength = 0;
               SCPDURL = NULL;
               SCPDURLLength = 0;
               EventSubURL = NULL;
               EventSubURLLength = 0;
               ControlURL = NULL;
               ControlURLLength = 0;
               ServiceMaxVersion = 1;
               
               root_a = a = ILibGetXMLAttributes(xml);
               if(a!=NULL)
               {
                  while(a!=NULL)
                  {
                     a->Name[a->NameLength]=0;
                     if(strcasecmp(a->Name,"MaxVersion")==0)
                     {
                        a->Value[a->ValueLength]=0;
                        ServiceMaxVersion = atoi(a->Value);
                        break;
                     }
                     a = a->Next;
                  }
                  ILibDestructXMLAttributeList(root_a);
               }
               
               xml = xml->Next;
               while(xml != NULL)
               {
                  //
                  // Fetch the URIs associated with this service
                  //
                  if(xml->NameLength==11 && memcmp(xml->Name,"serviceType",11)==0)
                  {
                     ServiceTypeLength = ILibReadInnerXML(xml,&ServiceType);
                  } else
                  if(xml->NameLength==9 && memcmp(xml->Name,"serviceId",9)==0)
                  {
                     ServiceIdLength = ILibReadInnerXML(xml,&ServiceId);
                  } else
                  if(xml->NameLength==7 && memcmp(xml->Name,"SCPDURL",7) == 0)
                  {
                     SCPDURLLength = ILibReadInnerXML(xml,&SCPDURL);
                  } else
                  if(xml->NameLength==10 && memcmp(xml->Name,"controlURL",10) == 0)
                  {
                     ControlURLLength = ILibReadInnerXML(xml,&ControlURL);
                  } else
                  if(xml->NameLength==11 && memcmp(xml->Name,"eventSubURL",11) == 0)
                  {
                     EventSubURLLength = ILibReadInnerXML(xml,&EventSubURL);
                  }
                  
                  if(xml->Peer!=NULL)
                  {
                     xml = xml->Peer;
                  }
                  else
                  {
					 if(xml->Parent != NULL)
					 {
						 xml = xml->Parent;
					 }
					 else
					 {
						 xml = xml->Next;
					 }
					 break;
                  }
               }
               
               /* Finished Parsing the ServiceSection, build the Service */
               ServiceType[ServiceTypeLength] = '\0';
               SCPDURL[SCPDURLLength] = '\0';
               EventSubURL[EventSubURLLength] = '\0';
               ControlURL[ControlURLLength] = '\0';
               
               TempService = (struct UPnPService*)malloc(sizeof(struct UPnPService));
               TempService->SubscriptionID = NULL;
               TempService->ServiceId = (char*)malloc(ServiceIdLength+1);
               TempService->ServiceId[ServiceIdLength]=0;
               memcpy(TempService->ServiceId,ServiceId,ServiceIdLength);
               
               TempService->Actions = NULL;
               TempService->Variables = NULL;
               TempService->Next = NULL;
               TempService->Parent = device;
               TempService->MaxVersion = ServiceMaxVersion;
               if(EventSubURLLength>=7 && memcmp(EventSubURL,"http://",6)==0)
               {
                  /* Explicit */
                  TempService->SubscriptionURL = (char*)malloc(EventSubURLLength+1);
                  memcpy(TempService->SubscriptionURL,EventSubURL,EventSubURLLength);
                  TempService->SubscriptionURL[EventSubURLLength] = '\0';
               }
               else
               {
                  /* Relative */
                  if(memcmp(EventSubURL,"/",1)!=0)
                  {
                     TempService->SubscriptionURL = (char*)malloc(EventSubURLLength+(int)strlen(RelativeBaseURL)+1);
                     memcpy(TempService->SubscriptionURL,RelativeBaseURL,(int)strlen(RelativeBaseURL));
                     memcpy(TempService->SubscriptionURL+(int)strlen(RelativeBaseURL),EventSubURL,EventSubURLLength);
                     TempService->SubscriptionURL[EventSubURLLength+(int)strlen(RelativeBaseURL)] = '\0';
                  }
                  else
                  {
                     TempService->SubscriptionURL = (char*)malloc(EventSubURLLength+(int)strlen(BaseURL)+1);
                     memcpy(TempService->SubscriptionURL,BaseURL,(int)strlen(BaseURL));
                     memcpy(TempService->SubscriptionURL+(int)strlen(BaseURL),EventSubURL+1,EventSubURLLength-1);
                     TempService->SubscriptionURL[EventSubURLLength+(int)strlen(BaseURL)-1] = '\0';
                  }
               }
               if(ControlURLLength>=7 && memcmp(ControlURL,"http://",6)==0)
               {
                  /* Explicit */
                  TempService->ControlURL = (char*)malloc(ControlURLLength+1);
                  memcpy(TempService->ControlURL,ControlURL,ControlURLLength);
                  TempService->ControlURL[ControlURLLength] = '\0';
               }
               else
               {
                  /* Relative */
                  if(memcmp(ControlURL,"/",1)!=0)
                  {
                     TempService->ControlURL = (char*)malloc(ControlURLLength+(int)strlen(RelativeBaseURL)+1);
                     memcpy(TempService->ControlURL,RelativeBaseURL,(int)strlen(RelativeBaseURL));
                     memcpy(TempService->ControlURL+(int)strlen(RelativeBaseURL),ControlURL,ControlURLLength);
                     TempService->ControlURL[ControlURLLength+(int)strlen(RelativeBaseURL)] = '\0';
                  }
                  else
                  {
                     TempService->ControlURL = (char*)malloc(ControlURLLength+(int)strlen(BaseURL)+1);
                     memcpy(TempService->ControlURL,BaseURL,(int)strlen(BaseURL));
                     memcpy(TempService->ControlURL+(int)strlen(BaseURL),ControlURL+1,ControlURLLength-1);
                     TempService->ControlURL[ControlURLLength+(int)strlen(BaseURL)-1] = '\0';
                  }
               }
               if(SCPDURLLength>=7 && memcmp(SCPDURL,"http://",6)==0)
               {
                  /* Explicit */
                  TempService->SCPDURL = (char*)malloc(SCPDURLLength+1);
                  memcpy(TempService->SCPDURL,SCPDURL,SCPDURLLength);
                  TempService->SCPDURL[SCPDURLLength] = '\0';
               }
               else
               {
                  /* Relative */
                  if(memcmp(SCPDURL,"/",1)!=0)
                  {
                     TempService->SCPDURL = (char*)malloc(SCPDURLLength+(int)strlen(RelativeBaseURL)+1);
                     memcpy(TempService->SCPDURL,RelativeBaseURL,(int)strlen(RelativeBaseURL));
                     memcpy(TempService->SCPDURL+(int)strlen(RelativeBaseURL),SCPDURL,SCPDURLLength);
                     TempService->SCPDURL[SCPDURLLength+(int)strlen(RelativeBaseURL)] = '\0';
                  }
                  else
                  {
                     TempService->SCPDURL = (char*)malloc(SCPDURLLength+(int)strlen(BaseURL)+1);
                     memcpy(TempService->SCPDURL,BaseURL,(int)strlen(BaseURL));
                     memcpy(TempService->SCPDURL+(int)strlen(BaseURL),SCPDURL+1,SCPDURLLength-1);
                     TempService->SCPDURL[SCPDURLLength+(int)strlen(BaseURL)-1] = '\0';
                  }
               }
               
               TempService->ServiceType = (char*)malloc(ServiceTypeLength+1);
               sprintf(TempService->ServiceType,ServiceType,ServiceTypeLength);
               TempService->Next = device->Services;
               device->Services = TempService;
               
               DEBUGSTATEMENT(printf("ServiceType: %s\r\nSCPDURL: %s\r\nEventSubURL: %s\r\nControl URL: %s\r\n",ServiceType,SCPDURL,EventSubURL,ControlURL));
            }
			if(xml != NULL)
				xml = xml->Peer;
         }
         xml = tempNode;
      } // End of ServiceList
	  if(xml != NULL)
		  xml = xml->Peer;
   } // End of While
   
   return(device);
}

//
// Internal method used to parse the Device Description XML Document
//
int MSCP_ProcessDeviceXML(void *v_CP,char* buffer, int BufferSize, char* LocationURL, int RecvAddr, int Timeout)
{
   struct UPnPDevice *RootDevice = NULL;
   
   char* IP;
   unsigned short Port;
   char* Path;
   
   char* BaseURL = NULL;
   char* RelativeBaseURL = NULL;
   
   struct ILibXMLNode *rootXML;
   struct ILibXMLNode *xml;
   char* tempString;
   int tempStringLength;
   
   //
   // Parse the XML, check that it's wellformed, and build the namespace lookup table
   //
   rootXML = ILibParseXML(buffer,0,BufferSize);
   if(ILibProcessXMLNodeList(rootXML)!=0)
   {
      ILibDestructXMLNodeList(rootXML);
      return(1);
   }
   ILibXML_BuildNamespaceLookupTable(rootXML);
   
   //
   // We need to figure out if this particular device uses
   // relative URLs using the URLBase element.
   //
   xml = rootXML;
   xml = xml->Next;
   while(xml!=NULL)
   {
      if(xml->NameLength==7 && memcmp(xml->Name,"URLBase",7)==0)
      {
         tempStringLength = ILibReadInnerXML(xml,&tempString);
         if(tempString[tempStringLength-1]!='/')
         {
            BaseURL = (char*)malloc(2+tempStringLength);
            memcpy(BaseURL,tempString,tempStringLength);
            BaseURL[tempStringLength] = '/';
            BaseURL[tempStringLength+1] = '\0';
         }
         else
         {
            BaseURL = (char*)malloc(1+tempStringLength);
            memcpy(BaseURL,tempString,tempStringLength);
            BaseURL[tempStringLength] = '\0';
         }
         break;
      }
      xml = xml->Peer;
   }
   
   //
   // If the URLBase was not specified, then we need force the
   // base url to be that of the base path that we used to fetch
   // the description document from.
   //
   
   if(BaseURL==NULL)
   {
      char *relatedURL = NULL;
      char *cur = NULL;
      char *lastCur = strrchr(LocationURL, '/');

      ILibParseUri(LocationURL,&IP,&Port,&Path);
      BaseURL = (char*)malloc(18+(int)strlen(IP));
      sprintf(BaseURL,"http://%s:%d/",IP,Port);

      // for DLNA certification modified!! yuyu
      // find http://$IP:port/
      //                     ^
      cur = strstr(LocationURL, IP);
      if( cur != NULL )
        cur = strchr(cur, '/');

      // something should be append!!
      if( cur != NULL && lastCur != NULL && cur!=lastCur)
      {
          int appendLength = (lastCur-cur);
          relatedURL = (char *)malloc(appendLength+1);
          memset(relatedURL, 0, appendLength+1);
          snprintf(relatedURL, appendLength, "%s", cur+1);
      }
      //printf("\t\t[%s] relatedURL:%s.\n", __func__, relatedURL);

      if(relatedURL)
      {
        RelativeBaseURL = (char*)malloc(18+(int)strlen(IP)+strlen(relatedURL));
        memset(RelativeBaseURL, 0, 18+(int)strlen(IP)+strlen(relatedURL));
        sprintf(RelativeBaseURL,"http://%s:%d/%s/",IP,Port, relatedURL);
        //printf("\t\t[%s] RelativeBaseURL:%s\n", __func__, RelativeBaseURL);
        //printf("\t\t[%s] alloca legnth:%d, strlen(RelativeBaseURL):%d\n", __func__, 18+(int)strlen(IP)+strlen(relatedURL), strlen(RelativeBaseURL));
        free(relatedURL);
      }
      else
      {
        RelativeBaseURL = (char*)malloc(18+(int)strlen(IP));
        sprintf(RelativeBaseURL,"http://%s:%d/",IP,Port);
        //printf("\t\t[%s] BaseURL:%s\n", __func__, BaseURL);
      }

      free(IP);
      free(Path);
   }
   else
      RelativeBaseURL = strdup(BaseURL);
   
   DEBUGSTATEMENT(printf("BaseURL: %s\r\n",BaseURL));
   
   //
   // Now that we have the base path squared away, we can actually parse this thing!
   // Let's start by looking for the device element
   //
   xml = rootXML;
   xml = xml->Next;
   while(xml!=NULL && xml->NameLength!=6 && memcmp(xml->Name,"device",6)!=0)
   {
      xml = xml->Peer;
   }
   if(xml==NULL)
   {
      /* Error */
      ILibDestructXMLNodeList(rootXML);
      free(BaseURL); // make up memory release, 20090513, yuyu
      free(RelativeBaseURL); // make up memory release, 20090513, yuyu
      return(1);
   }
   
   //
   // Process the Device Element. If the device element contains other devices,
   // it will be recursively called, so we don't need to worry
   //
   RootDevice = MSCP_ProcessDeviceXML_device(xml,v_CP,BaseURL,RelativeBaseURL,Timeout,RecvAddr);
   free(BaseURL);
   free(RelativeBaseURL);
   ILibDestructXMLNodeList(rootXML);
   
   
   /* Save reference to LocationURL in the RootDevice */
   RootDevice->LocationURL = (char*)malloc(strlen(LocationURL)+1);
   sprintf(RootDevice->LocationURL,"%s",LocationURL);
   
   //
   // Now that we processed the device XML document, we need to fetch
   // and parse the service description documents.
   //
   MSCP_ProcessDevice(v_CP,RootDevice);
   RootDevice->SCPDLeft = 0;
   MSCP_CalculateSCPD_FetchCount(RootDevice);
   if(RootDevice->SCPDLeft==0)
   {
      //
      // If this device doesn't contain any services, than we don't
      // need to bother with fetching any SCPD documents
      //
      MSCP_FinishProcessingDevice(v_CP,RootDevice);
   }
   else
   {
      //
      // Fetch the SCPD documents
      //
      MSCP_SCPD_Fetch(RootDevice);
   }
   return(0);
}


//
// The internal sink for obtaining the Device Description Document
//
void MSCP_HTTP_Sink_DeviceDescription(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *user,
void *cp,
int *PAUSE)
{
   struct CustomUserData *customData = (struct CustomUserData*)user;
   struct MSCP_CP* CP = (struct MSCP_CP*)cp;
   
   if(done!=0)
   {
      ILibDeleteEntry(CP->DeviceTable_Tokens,customData->UDN,(int)strlen(customData->UDN));
      ILibDeleteEntry(CP->DeviceTable_URI,customData->LocationURL,(int)strlen(customData->LocationURL));
   }
   
   if(header!=NULL && ILibWebClientIsStatusOk(header->StatusCode) && done!=0 && EndPointer > 0)
   {
      if(MSCP_ProcessDeviceXML(cp,buffer,EndPointer-(*p_BeginPointer),customData->buffer,header->ReceivingAddress,customData->Timeout)!=0)
      {
         ILibDeleteEntry(CP->DeviceTable_UDN,customData->UDN,(int)strlen(customData->UDN));
         ILibDeleteEntry(CP->DeviceTable_URI,customData->LocationURL,(int)strlen(customData->LocationURL));
      }
   }
   else if((header==NULL) || (header!=NULL && !ILibWebClientIsStatusOk(header->StatusCode)))
   {
      if(done!=0 && CP->ErrorDispatch!=NULL)
      {
         CP->ErrorDispatch(customData->UDN,customData->LocationURL,header!=NULL?header->StatusCode:-1);
      }
      ILibDeleteEntry(CP->DeviceTable_UDN,customData->UDN,(int)strlen(customData->UDN));
      ILibDeleteEntry(CP->DeviceTable_URI,customData->LocationURL,(int)strlen(customData->LocationURL));
   }
   
   if(done!=0)
   {
      free(customData->buffer);
      free(customData->UDN);
      free(customData->LocationURL);
      free(user);
   }
}
void MSCP__FlushRequest(struct UPnPDevice *device)
{
   struct UPnPDevice *ed = device->EmbeddedDevices;
   struct UPnPService *s = device->Services;
   
   while(ed!=NULL)
   {
      MSCP__FlushRequest(ed);
      ed = ed->Next;
   }
   while(s!=NULL)
   {
      s = s->Next;
   }
}

//
// An internal method used to recursively release all the references to a device.
// While doing this, we'll also check to see if we gave any of these to the app layer
// above, and if so, trigger a removal event.
//
void MSCP_CP_RecursiveReleaseAndEventDevice(struct MSCP_CP* CP, struct UPnPDevice *device)
{
   struct UPnPDevice *temp = device->EmbeddedDevices;
   
   ILibDeleteEntry(CP->DeviceTable_URI,device->LocationURL,(int)strlen(device->LocationURL));
   ILibAddEntry(CP->DeviceTable_UDN,device->UDN,(int)strlen(device->UDN),NULL);
   
   while(temp!=NULL)
   {
      MSCP_CP_RecursiveReleaseAndEventDevice(CP,temp);
      temp = temp->Next;
   }
   
   if(device->Reserved!=0)
   {
      //
      // We gave this to the app layer above
      //
      if(CP->RemoveSink!=NULL)
      {
         CP->RemoveSink(device);
      }
      MSCP_Release(device);
   }
}
void MSCP_CP_ProcessDeviceRemoval(struct MSCP_CP* CP, struct UPnPDevice *device)
{
   struct UPnPDevice *temp = device->EmbeddedDevices;
   struct UPnPService *s;
   char *IP;
   unsigned short port;
   char *PATH;
   struct UPnPDevice *dTemp = device;
   
   if(dTemp->Parent!=NULL)
   {
      dTemp = dTemp->Parent;
   }
   ILibLifeTime_Remove(CP->LifeTimeMonitor,dTemp);
   
   while(temp!=NULL)
   {
      MSCP_CP_ProcessDeviceRemoval(CP,temp);
      temp = temp->Next;
   }
   
   s = device->Services;
   while(s!=NULL)
   {
      //
      // Remove all the pending requests
      //
      ILibParseUri(s->ControlURL,&IP,&port,&PATH);
      ILibWebClient_DeleteRequests(((struct MSCP_CP*)device->CP)->HTTP,IP,(int)port);
      free(IP);
      free(PATH);
      
      ILibLifeTime_Remove(CP->LifeTimeMonitor,s);
      s = s->Next;
   }
   
   if(device->Reserved!=0)
   {
      // device was flagged, and given to the user
      if(CP->RemoveSink!=NULL)
      {
         CP->RemoveSink(device);
      }
      MSCP_Release(device);
   }
   
   ILibHashTree_Lock(CP->DeviceTable_UDN);
   ILibDeleteEntry(CP->DeviceTable_UDN,device->UDN,(int)strlen(device->UDN));
   if(device->LocationURL!=NULL)
   {
      ILibDeleteEntry(CP->DeviceTable_URI,device->LocationURL,(int)strlen(device->LocationURL));
   }
   ILibHashTree_UnLock(CP->DeviceTable_UDN);
}

//
// The internal sink called by our SSDP Module
//
void MSCP_SSDP_Sink(void *sender, char* UDN, int Alive, char* LocationURL, int Timeout,UPnPSSDP_MESSAGE m,void *cp)
{
   struct CustomUserData *customData;
   char* buffer;
   char* IP;
   unsigned short Port;
   char* Path;
   struct packetheader *p;
   struct sockaddr_in addr;
   
   struct UPnPDevice *device;
   int i=0;
   struct MSCP_CP *CP = (struct MSCP_CP*)cp;
   struct timeval t;
   
   ILibWebClient_RequestToken RT;
   void *v;
   
   if(Alive!=0)
   {
      /* Hello */
      
      //
      // A device should never advertise it's timeout value being zero. But if
      // it does, let's not waste time processing stuff
      //
      if(Timeout==0) {return;}
      DEBUGSTATEMENT(printf("BinaryLight Hello\r\n"));
      DEBUGSTATEMENT(printf("LocationURL: %s\r\n",LocationURL));
      
      //
      // Lock the table
      //
      ILibHashTree_Lock(CP->DeviceTable_UDN);
      
      //
      // We have never seen this location URL, nor have we ever seen this UDN before, 
      // so let's try and find it
      //
#ifdef ANDROID_DLNA_WORKAROUND   //for CTT 7.4.73.1
      if(ILibHasEntry(CP->DeviceTable_UDN,UDN,(int)strlen(UDN))==0)
#else	
      if(ILibHasEntry(CP->DeviceTable_URI,LocationURL,(int)strlen(LocationURL))==0 && ILibHasEntry(CP->DeviceTable_UDN,UDN,(int)strlen(UDN))==0)
#endif
      {
         //
         // Parse the location uri of the device description document,
         // and build the request packet
         //
         ILibParseUri(LocationURL,&IP,&Port,&Path);
         DEBUGSTATEMENT(printf("IP: %s Port: %d Path: %s\r\n",IP,Port,Path));
         p = MSCP_BuildPacket(IP,Port,Path,"GET");
         
         memset((char *)&addr, 0,sizeof(addr));
         addr.sin_family = AF_INET;
         addr.sin_addr.s_addr = inet_addr(IP);
         addr.sin_port = htons(Port);
         
         buffer = (char*)malloc((int)strlen(LocationURL)+1);
         strcpy(buffer,LocationURL);
         
         customData = (struct CustomUserData*)malloc(sizeof(struct CustomUserData));
         customData->Timeout = Timeout;
         customData->buffer = buffer;
         
         customData->LocationURL = (char*)malloc(strlen(LocationURL)+1);
         strcpy(customData->LocationURL,LocationURL);
         customData->UDN = (char*)malloc(strlen(UDN)+1);
         strcpy(customData->UDN,UDN);
         
         //
         // Add these items into our table, so we don't try to find it multiple times
         //
         ILibAddEntry(CP->DeviceTable_URI,LocationURL,(int)strlen(LocationURL),customData->UDN);
         ILibAddEntry(CP->DeviceTable_UDN,UDN,(int)strlen(UDN),NULL);
         
         //
         // Make the HTTP request to fetch the Device Description Document
         //
         RT = ILibWebClient_PipelineRequest(
         
         ((struct MSCP_CP*)cp)->HTTP,
         &addr,
         p,
         &MSCP_HTTP_Sink_DeviceDescription,
         customData, 
         cp);
         
         free(IP);
         free(Path);
         
         ILibAddEntry(CP->DeviceTable_Tokens,UDN,(int)strlen(UDN),RT);
      }
      else
      {
         //
         // We have seen this device before, so thse packets are
         // Periodic Notify Packets
         
         //
         // Fetch the device, this packet is advertising
         //
         device = (struct UPnPDevice*)ILibGetEntry(CP->DeviceTable_UDN,UDN,(int)strlen(UDN));
         if(device!=NULL && device->ReservedID==0 && m==UPnPSSDP_NOTIFY)
         {
            //
            // Get the root device
            //
            while(device->Parent!=NULL)
            {
               device = device->Parent;
            }
            
            //
            // Is this device on the same IP Address?
            //
            if(strcmp(device->LocationURL,LocationURL)==0)
            {
               //Extend LifetimeMonitor duration
               gettimeofday(&t,NULL);
               device->Reserved2 = t.tv_sec;
               ILibLifeTime_Remove(((struct MSCP_CP*)cp)->LifeTimeMonitor,device);
               ILibLifeTime_Add(((struct MSCP_CP*)cp)->LifeTimeMonitor,device,Timeout,(void*)&MSCP_ExpiredDevice,NULL);
            }
            else
            {
               //
               // Same device, different Interface
               // Wait up to 7 seconds to see if the old interface is still
               // valid.
               //
               gettimeofday(&t,NULL);
               if(t.tv_sec-device->Reserved2>10)
               {
                  device->Reserved2 = t.tv_sec;
                  if(device->Reserved3!=NULL) {free(device->Reserved3);}
                  device->Reserved3 = (char*)malloc(strlen(LocationURL)+1);
                  strcpy(device->Reserved3,LocationURL);
                  ILibLifeTime_Remove(((struct MSCP_CP*)cp)->LifeTimeMonitor,device);
                  ILibLifeTime_Add(((struct MSCP_CP*)cp)->LifeTimeMonitor,device,7,(void*)&MSCP_InterfaceChanged,NULL);
               }
            }
         }
      }
      ILibHashTree_UnLock(CP->DeviceTable_UDN);
   }
   else
   {
      /* Bye Bye */
      ILibHashTree_Lock(CP->DeviceTable_UDN);
     
      if(UDN != NULL)
      {
      v = ILibGetEntry(CP->DeviceTable_Tokens,UDN,(int)strlen(UDN));
      if(v!=NULL)
      {
         //ILibWebClient_CancelRequest((ILibWebClient_RequestToken)v);
         ILibDeleteEntry(CP->DeviceTable_Tokens,UDN,(int)strlen(UDN));
         //ILibDeleteEntry(CP->DeviceTable_UDN,UDN,(int)strlen(UDN));
      }
      device = (struct UPnPDevice*)ILibGetEntry(CP->DeviceTable_UDN,UDN,(int)strlen(UDN));
      }
      else
	 device = NULL;
      
      //
      // Find the device that is going away
      //
      ILibHashTree_UnLock(CP->DeviceTable_UDN);
      
      
      if(device!=NULL)
      {
         //
         // Get the root device
         //
         while(device->Parent!=NULL)
         {
            device = device->Parent;
         }
         //
         // Remove the timed event, checking the refreshing of notify packets
         //
         ILibLifeTime_Remove(((struct MSCP_CP*)cp)->LifeTimeMonitor,device);
         MSCP_CP_ProcessDeviceRemoval(CP,device);
         //
         // If the app above subscribed to events, there will be extra references
         // that we can delete, otherwise, the device ain't ever going away
         //
         i = device->ReferenceTiedToEvents;
         while(i!=0)
         {
            MSCP_Release(device);
            --i;
         }
         MSCP_Release(device);
      }
   }
}

void MSCP_ConnectionManager_EventSink(char* buffer, int bufferlength, struct UPnPService *service)
{
   struct ILibXMLNode *xml,*rootXML;
   char *tempString;
   int tempStringLength;
   
   char* SourceProtocolInfo = 0;
   char* SinkProtocolInfo = 0;
   char* CurrentConnectionIDs = 0;
   
   /* Parse SOAP */
   rootXML = xml = ILibParseXML(buffer,0,bufferlength);
   ILibProcessXMLNodeList(xml);
   
   while(xml!=NULL)
   {
      if(xml->NameLength==11 && memcmp(xml->Name,"propertyset",11)==0)
      {
         if(xml->Next->StartTag!=0)
         {
            xml = xml->Next;
            while(xml != NULL)
            {
               if(xml->NameLength==8 && memcmp(xml->Name,"property",8)==0)
               {
                  xml = xml->Next;
                  while(xml != NULL)
                  {
                     if(xml->NameLength==18 && memcmp(xml->Name,"SourceProtocolInfo",18) == 0)
                     {
                        tempStringLength = ILibReadInnerXML(xml,&tempString);
                        tempString[tempStringLength] = '\0';
                        SourceProtocolInfo = tempString;
                        if(MSCP_EventCallback_ConnectionManager_SourceProtocolInfo != NULL)
                        {
                           MSCP_EventCallback_ConnectionManager_SourceProtocolInfo(service,SourceProtocolInfo);
                        }
                     }
                     if(xml->NameLength==16 && memcmp(xml->Name,"SinkProtocolInfo",16) == 0)
                     {
                        tempStringLength = ILibReadInnerXML(xml,&tempString);
                        tempString[tempStringLength] = '\0';
                        SinkProtocolInfo = tempString;
                        if(MSCP_EventCallback_ConnectionManager_SinkProtocolInfo != NULL)
                        {
                           MSCP_EventCallback_ConnectionManager_SinkProtocolInfo(service,SinkProtocolInfo);
                        }
                     }
                     if(xml->NameLength==20 && memcmp(xml->Name,"CurrentConnectionIDs",20) == 0)
                     {
                        tempStringLength = ILibReadInnerXML(xml,&tempString);
                        tempString[tempStringLength] = '\0';
                        CurrentConnectionIDs = tempString;
                        if(MSCP_EventCallback_ConnectionManager_CurrentConnectionIDs != NULL)
                        {
                           MSCP_EventCallback_ConnectionManager_CurrentConnectionIDs(service,CurrentConnectionIDs);
                        }
                     }
                     if(xml->Peer!=NULL)
                     {
                        xml = xml->Peer;
                     }
                     else
                     {
						if(xml->Parent != NULL)
						{
							xml = xml->Parent;
						}
						else
						{
							xml = xml->Next;
						}
						break;
                     }
                  }
               }
			   if(xml != NULL)
			   {
				   if(xml->Peer!=NULL)
				   {
					   xml = xml->Peer;
				   }
				   else
				   {
					   if(xml->Parent != NULL)
					   {
						   xml = xml->Parent;
					   }
					   else
					   {
						   xml = xml->Next;
					   }
					   break;
				   }
			   }
            }
         }
      }
	  if(xml != NULL)
		  xml = xml->Peer;
   }
   
   ILibDestructXMLNodeList(rootXML);
}
void MSCP_ContentDirectory_EventSink(char* buffer, int bufferlength, struct UPnPService *service)
{
   struct ILibXMLNode *xml,*rootXML;
   char *tempString;
   int tempStringLength;
   
   char* ContainerUpdateIDs = 0;
   unsigned int SystemUpdateID = 0;
   unsigned long TempULong;
   
   /* Parse SOAP */
   rootXML = xml = ILibParseXML(buffer,0,bufferlength);
   ILibProcessXMLNodeList(xml);
   
   while(xml!=NULL)
   {
      if(xml->NameLength==11 && memcmp(xml->Name,"propertyset",11)==0)
      {
         if(xml->Next->StartTag!=0)
         {
            xml = xml->Next;
            while(xml != NULL)
            {
               if(xml->NameLength==8 && memcmp(xml->Name,"property",8)==0)
               {
                  xml = xml->Next;
                  while(xml != NULL)
                  {
                     if(xml->NameLength==18 && memcmp(xml->Name,"ContainerUpdateIDs",18) == 0)
                     {
                        tempStringLength = ILibReadInnerXML(xml,&tempString);
                        tempString[tempStringLength] = '\0';
                        ContainerUpdateIDs = tempString;
                        if(MSCP_EventCallback_ContentDirectory_ContainerUpdateIDs != NULL)
                        {
                           MSCP_EventCallback_ContentDirectory_ContainerUpdateIDs(service,ContainerUpdateIDs);
                        }
                     }
                     if(xml->NameLength==14 && memcmp(xml->Name,"SystemUpdateID",14) == 0)
                     {
                        tempStringLength = ILibReadInnerXML(xml,&tempString);
                        if(ILibGetULong(tempString,tempStringLength,&TempULong)==0)
                        {
                           SystemUpdateID = (unsigned int) TempULong;
                        }
                        if(MSCP_EventCallback_ContentDirectory_SystemUpdateID != NULL)
                        {
                           MSCP_EventCallback_ContentDirectory_SystemUpdateID(service,SystemUpdateID);
                        }
                     }
                     if(xml->Peer!=NULL)
                     {
                        xml = xml->Peer;
                     }
                     else
                     {
						if(xml->Parent != NULL)
						{
							xml = xml->Parent;
						}
						else
						{
							xml = xml->Next;
						}
						break;
                     }
                  }
               }
			   if(xml != NULL)
			   {
				   if(xml->Peer!=NULL)
				   {
					   xml = xml->Peer;
				   }
				   else
				   {
					   if(xml->Parent != NULL)
					   {
						   xml = xml->Parent;
					   }
					   else
					   {
						   xml = xml->Next;
					   }
					   break;
				   }
			   }
            }
         }
      }
	  if(xml != NULL)
		  xml = xml->Peer;
   }
   
   ILibDestructXMLNodeList(rootXML);
}



//
// Internal HTTP Sink, called when an event is delivered
//
void MSCP_OnEventSink(
struct ILibWebServer_Session *sender,
int InterruptFlag,
struct packetheader *header,
char *buffer,
int *BeginPointer,
int BufferSize,
int done)
{
   int type_length;
   char* sid = NULL;
   void* value = NULL;
   struct UPnPService *service = NULL;
   struct packetheader_field_node *field = NULL;
   struct packetheader *resp;
   
   
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
   
   
   if(done!=0)
   {
      //
      // We recieved the event, let's prepare the response
      //
      resp = ILibCreateEmptyPacket();
      ILibSetVersion(resp,"1.1",3);
      ILibAddHeaderLine(resp,"Server",6,MSCP_PLATFORM,(int)strlen(MSCP_PLATFORM));
      ILibAddHeaderLine(resp,"Content-Length",14,"0",1);
      field = header->FirstField;
      while(field!=NULL)
      {
         if(field->FieldLength==3)
         {
            if(strncasecmp(field->Field,"SID",3)==0)
            {
               //
               // We need to determine who this event is for, by looking
               // at the subscription id
               //
               sid = (char*)malloc(field->FieldDataLength+1);
               sprintf(sid,"%s",field->FieldData);
               
               //
               // Do we know about this SID?
               //
               value = ILibGetEntry(((struct MSCP_CP*)sender->User)->SIDTable,field->FieldData,field->FieldDataLength);
               break;
            }
         }
         field = field->NextField;
      }
      
      if(value==NULL)
      {
         /* Not a valid SID */
         ILibSetStatusCode(resp,412,"Failed",6);
      }
      else
      {
         ILibSetStatusCode(resp,200,"OK",2);
         service = (struct UPnPService*)value;
         
         type_length = (int)strlen(service->ServiceType);
         if(type_length>47 && strncmp("urn:schemas-upnp-org:service:ConnectionManager:",service->ServiceType,47)==0)
         {
            MSCP_ConnectionManager_EventSink(buffer, BufferSize, service);
         }
         else
         if(type_length>46 && strncmp("urn:schemas-upnp-org:service:ContentDirectory:",service->ServiceType,46)==0)
         {
            MSCP_ContentDirectory_EventSink(buffer, BufferSize, service);
         }
         
      }
      ILibWebServer_Send(sender,resp);
      if(sid!=NULL){free(sid);}
   }
}

//
// Internal sink called when our attempt to unregister for events
// has gone through
//
void MSCP_OnUnSubscribeSink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *BeginPointer,
int BufferSize,
int done,
void *user,
void *vcp,
int *PAUSE)
{
   struct UPnPService *s;
   //struct MSCP_CP *cp = (struct MSCP_CP*)vcp;
   
   if(done!=0)
   {
      s = (struct UPnPService*)user;
      if(header!=NULL)
      {
         if(ILibWebClientIsStatusOk(header->StatusCode))
         {
            /* Successful */
         }
      }
      MSCP_Release(s->Parent);
   }
}


//
// Internal sink called when our attempt to register for events
// has gone through
//
void MSCP_OnSubscribeSink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *BeginPointer,
int BufferSize,
int done,
void *user,
void *vcp,
int *PAUSE)
{
   struct UPnPService *s;
   struct UPnPDevice *d;
   struct packetheader_field_node *field;
   struct parser_result *p;
   struct MSCP_CP *cp = (struct MSCP_CP*)vcp;
   
   if(done!=0)
   {
      s = (struct UPnPService*)user;
      if(header!=NULL)
      {
         if(ILibWebClientIsStatusOk(header->StatusCode))
         {
            /* Successful */
            field = header->FirstField;
            while(field!=NULL)
            {
               if(field->FieldLength==3 && strncasecmp(field->Field,"SID",3)==0 && s->SubscriptionID==NULL)
               {
                  //
                  // Determine what subscription id was assigned to us
                  //
                  s->SubscriptionID = (char*)malloc(1+field->FieldDataLength);
                  strcpy(s->SubscriptionID,field->FieldData);
                  //
                  // Make a mapping from this SID to our service, to make our lives easier
                  //
                  ILibAddEntry(cp->SIDTable,field->FieldData,field->FieldDataLength,s);
               } else
               if(field->FieldLength==7 && strncasecmp(field->Field,"TIMEOUT",7)==0)
               {
                  //
                  // Determine what refresh cycle the device wants us to enforce
                  //
                  p = ILibParseString(field->FieldData,0,field->FieldDataLength,"-",1);
                  p->LastResult->data[p->LastResult->datalength] = '\0';
                  MSCP_AddRef(s->Parent);
                  d = s->Parent;
                  while(d->Parent!=NULL) {d = d->Parent;}
                  ++d->ReferenceTiedToEvents;
                  ILibLifeTime_Add(cp->LifeTimeMonitor,s,atoi(p->LastResult->data)/2,(void*)&MSCP_Renew,NULL);
                  ILibDestructParserResults(p);
               }
               field = field->NextField;
            }
         }
      }
      MSCP_Release(s->Parent);
   }
}

//
// Internal Method used to renew our event subscription with a device
//
void MSCP_Renew(void *state)
{
   struct UPnPService *service = (struct UPnPService*)state;
   struct UPnPDevice *d = service->Parent;
   char *IP;
   char *Path;
   unsigned short Port;
   struct packetheader *p;
   char* TempString;
   struct sockaddr_in destaddr;
   
   //
   // Determine where this renewal should go
   //
   ILibParseUri(service->SubscriptionURL,&IP,&Port,&Path);
   p = ILibCreateEmptyPacket();
   ILibSetVersion(p,"1.1",3);
   
   ILibSetDirective(p,"SUBSCRIBE",9,Path,(int)strlen(Path));
   
   TempString = (char*)malloc((int)strlen(IP)+7);
   sprintf(TempString,"%s:%d",IP,Port);
   
   ILibAddHeaderLine(p,"HOST",4,TempString,(int)strlen(TempString));
   free(TempString);
   
   ILibAddHeaderLine(p,"SID",3,service->SubscriptionID,(int)strlen(service->SubscriptionID));
   ILibAddHeaderLine(p,"TIMEOUT",7,"Second-180",10);
   ILibAddHeaderLine(p,"User-Agent",10,MSCP_PLATFORM,(int)strlen(MSCP_PLATFORM));
   
   memset((char *)&destaddr, 0,sizeof(destaddr));
   destaddr.sin_family = AF_INET;
   destaddr.sin_addr.s_addr = inet_addr(IP);
   destaddr.sin_port = htons(Port);
   
   
   //
   // Try to refresh our subscription
   //
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequest(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &destaddr,
   p,
   &MSCP_OnSubscribeSink,
   (void*)service, service->Parent->CP);
   
   while(d->Parent!=NULL) {d = d->Parent;}
   --d->ReferenceTiedToEvents;
   free(IP);
   free(Path);
}

struct UPnPDevice* MSCP_GetDeviceEx(struct UPnPDevice *device, char* DeviceType, int counter, int number)
{
   struct UPnPDevice *RetVal = NULL;
   struct UPnPDevice *d = device->EmbeddedDevices;
   struct parser_result *pr,*pr2;
   int DeviceTypeLength = (int)strlen(DeviceType);
   int TempLength = (int)strlen(device->DeviceType);
   
   while(d != NULL && RetVal==NULL)
   {
      RetVal = MSCP_GetDeviceEx(d,DeviceType,counter,number);
      d = d->Next;
   }
   
   if(RetVal==NULL)
   {
      pr = ILibParseString(DeviceType,0,DeviceTypeLength,":",1);
      pr2 = ILibParseString(device->DeviceType,0,TempLength,":",1);
      
      if(DeviceTypeLength-pr->LastResult->datalength == TempLength - pr2->LastResult->datalength && atoi(pr->LastResult->data) >= atoi(pr2->LastResult->data) && memcmp(DeviceType,device->DeviceType,DeviceTypeLength-pr->LastResult->datalength)==0)
      {
         ILibDestructParserResults(pr);
         ILibDestructParserResults(pr2);
         if(number == (++counter)) return(device);
      }
      ILibDestructParserResults(pr);
      ILibDestructParserResults(pr2);
      return(NULL);
   }
   else
   {
      return(RetVal);
   }
}

/*! \fn MSCP_HasAction(struct UPnPService *s, char* action)
\brief Determines if an action exists on a service
\param s UPnP service to query
\param action action name
\returns Non-zero if it exists
*/
int MSCP_HasAction(struct UPnPService *s, char* action)
{
   struct UPnPAction *a = s->Actions;
   
   while(a!=NULL)
   {
      if(strcmp(action,a->Name)==0) return(-1);
      a = a->Next;
   }
   return(0);
}

//
// Internal Trigger called when the chain is cleaning up
//
void MSCP_StopCP(void *v_CP)
{
   int i;
   struct UPnPDevice *Device;
   struct MSCP_CP *CP= (struct MSCP_CP*)v_CP;
   void *en;
   char *key;
   int keyLength;
   void *data;
   
   
   en = ILibHashTree_GetEnumerator(CP->DeviceTable_UDN);
   while(ILibHashTree_MoveNext(en)==0)
   {
      ILibHashTree_GetValue(en,&key,&keyLength,&data);
      if(data!=NULL)
      {
         // This is a UPnPDevice
         Device = (struct UPnPDevice*)data;
         if(Device->ReservedID==0 && Device->Parent==NULL) // This is a UPnPDevice if ReservedID==0
         {
            // This is the Root Device (Which is only in the table once)
            MSCP_CP_RecursiveReleaseAndEventDevice(CP,Device);
            i = Device->ReferenceTiedToEvents;
            while(i!=0)
            {
               MSCP_Release(Device);
               --i;
            }
            MSCP_Release(Device);
         }
      }
   }
   ILibHashTree_DestroyEnumerator(en);
   ILibDestroyHashTree(CP->SIDTable);
   ILibDestroyHashTree(CP->DeviceTable_UDN);
   ILibDestroyHashTree(CP->DeviceTable_URI);
   ILibDestroyHashTree(CP->DeviceTable_Tokens);
   
   free(CP->AddressList);
   
   sem_destroy(&(CP->DeviceLock));
}

/*! \fn MSCP__CP_IPAddressListChanged(void *CPToken)
\brief Notifies the underlying microstack that one of the ip addresses may have changed
\param CPToken Control Point Token
*/
void MSCP__CP_IPAddressListChanged(void *CPToken)
{
   if(ILibIsChainBeingDestroyed(((struct MSCP_CP*)CPToken)->Chain)==0)
   {
      ((struct MSCP_CP*)CPToken)->RecheckFlag = 1;
      ILibForceUnBlockChain(((struct MSCP_CP*)CPToken)->Chain);
   }
}

void MSCP_CP_PreSelect(void *CPToken,fd_set *readset, fd_set *writeset, fd_set *errorset, int *blocktime)
{
   struct MSCP_CP *CP= (struct MSCP_CP*)CPToken;
   void *en;
   
   struct UPnPDevice *device;
   char *key;
   int keyLength;
   void *data;
   void *q;
   
   int *IPAddressList;
   int NumAddressList;
   int i;
   int found;
   
   //
   // Do we need to recheck IP Addresses?
   //
   if(CP->RecheckFlag!=0)
   {
      CP->RecheckFlag = 0;
      
      //
      // Get the current IP Address list
      //
      NumAddressList = ILibGetLocalIPAddressList(&IPAddressList);
      
      //
      // Create a Queue, to add devices that need to be removed
      //
      q = ILibQueue_Create();
      
      //
      // Iterate through all the devices we are aware of
      //
      ILibHashTree_Lock(CP->DeviceTable_UDN);
      en = ILibHashTree_GetEnumerator(CP->DeviceTable_UDN);
      while(ILibHashTree_MoveNext(en)==0)
      {
         ILibHashTree_GetValue(en,&key,&keyLength,&data);
         if(data!=NULL)
         {
            // This is a UPnP Device
            device = (struct UPnPDevice*)data;
            if(device->ReservedID==0 && device->Parent==NULL)
            {
               // This is the root device, which is in the table exactly once
               
               //
               // Iterate through all the current IP addresses, and check to 
               // see if there are any devices that aren't on one of these
               //
               found = 0;
               for(i=0;i<NumAddressList;++i)
               {
                  if(IPAddressList[i]==inet_addr(device->InterfaceToHost))
                  {
                     found = 1;
                     break;
                  }
               }
               //
               // If the device wasn't bound to any of the current IP addresses, than
               // it is no longer reachable, so we should get rid of it, and hope
               // to find it later
               //
               if(found==0)
               {
                  // Queue Device to be removed, so we can process it
                  // outside of the lock
                  
                  ILibQueue_EnQueue(q,device);
               }
            }
         }
      }
      
      ILibHashTree_DestroyEnumerator(en);
      ILibHashTree_UnLock(CP->DeviceTable_UDN);
      
      //
      // Get rid of the devices that are no longer reachable
      //
      while(ILibQueue_PeekQueue(q)!=NULL)
      {
         device = ILibQueue_DeQueue(q);
         MSCP_CP_ProcessDeviceRemoval(CP,device);
         MSCP_Release(device);
      }
      ILibQueue_Destroy(q);
      ILibSSDP_IPAddressListChanged(CP->SSDP);
      free(CP->AddressList);
      CP->AddressListLength = NumAddressList;
      CP->AddressList = IPAddressList;
   }
}
void MSCP_OnSessionSink(struct ILibWebServer_Session *session, void *User)
{
   session->OnReceive = &MSCP_OnEventSink;
   session->User = User;
}


#ifdef UPNP_DEBUG

void MSCP_Debug(struct ILibWebServer_Session *sender, struct UPnPDevice *dv)
{
   char tmp[25];
   
   ILibWebServer_StreamBody(sender,"<b>",3,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   ILibWebServer_StreamBody(sender,dv->FriendlyName,(int)strlen(dv->FriendlyName),ILibAsyncSocket_MemoryOwnership_USER,0);
   ILibWebServer_StreamBody(sender,"</b>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   
   ILibWebServer_StreamBody(sender,"<br>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   
   ILibWebServer_StreamBody(sender,"  LocationURL: <A HREF=\"",24,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   ILibWebServer_StreamBody(sender,dv->LocationURL,(int)strlen(dv->LocationURL),ILibAsyncSocket_MemoryOwnership_USER,0);
   ILibWebServer_StreamBody(sender,"\">",2,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   ILibWebServer_StreamBody(sender,dv->LocationURL,(int)strlen(dv->LocationURL),ILibAsyncSocket_MemoryOwnership_USER,0);
   ILibWebServer_StreamBody(sender,"</A>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   
   ILibWebServer_StreamBody(sender,"<br>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   ILibWebServer_StreamBody(sender,"  UDN: <A HREF=\"/UDN/",21,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   ILibWebServer_StreamBody(sender,dv->UDN,(int)strlen(dv->UDN),ILibAsyncSocket_MemoryOwnership_USER,0);
   ILibWebServer_StreamBody(sender,"\">",2,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   ILibWebServer_StreamBody(sender,dv->UDN,(int)strlen(dv->UDN),ILibAsyncSocket_MemoryOwnership_USER,0);
   ILibWebServer_StreamBody(sender,"</A>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   
   
   while(dv->Parent!=NULL)
   {
      dv = dv->Parent;
   }
   ILibWebServer_StreamBody(sender,"<br><i>  Reference Counter: ",28,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   sprintf(tmp,"%d",dv->ReferenceCount);
   ILibWebServer_StreamBody(sender,tmp,(int)strlen(tmp),ILibAsyncSocket_MemoryOwnership_USER,0);
   ILibWebServer_StreamBody(sender,"</i>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   
   
   
   ILibWebServer_StreamBody(sender,"<br><br>",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
}
void MSCP_DebugOnQueryWCDO(struct ILibWebServer_Session *sender, char *w)
{
   struct MSCP_CP *cp = (struct MSCP_CP*)sender->User3;
   struct packetheader *p = ILibCreateEmptyPacket();
   char *t;
   
   ILibSetStatusCode(p,200,"OK",2);
   ILibSetVersion(p,"1.1",3);
   ILibAddHeaderLine(p,"Content-Type",12,"text/html",9);
   ILibWebServer_StreamHeader(sender,p);
   
   ILibWebServer_StreamBody(sender,"<HTML>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   
   if(w!=NULL)
   {
      t = ILibWebClient_QueryWCDO(cp->HTTP,w);
      ILibWebServer_StreamBody(sender,t,(int)strlen(t),ILibAsyncSocket_MemoryOwnership_CHAIN,0);
   }
   
   ILibWebServer_StreamBody(sender,"</HTML>",7,ILibAsyncSocket_MemoryOwnership_STATIC,1);
}
void MSCP_DebugOnQuery(struct ILibWebServer_Session *sender, char *UDN, char *URI, char *TOK)
{
   struct MSCP_CP *cp = (struct MSCP_CP*)sender->User3;
   struct packetheader *p = ILibCreateEmptyPacket();
   void *en;
   char *key;
   int keyLen;
   void *data;
   int rmv = 0;
   
   char *tmp;
   
   struct UPnPDevice *dv;
   
   ILibSetStatusCode(p,200,"OK",2);
   ILibSetVersion(p,"1.1",3);
   ILibAddHeaderLine(p,"Content-Type",12,"text/html",9);
   ILibWebServer_StreamHeader(sender,p);
   
   ILibWebServer_StreamBody(sender,"<HTML>",6,ILibAsyncSocket_MemoryOwnership_STATIC,0);
   
   ILibHashTree_Lock(cp->DeviceTable_UDN);
   
   if(UDN!=NULL && stricmp(UDN,"*")==0)
   {
      // Look in the DeviceTable_UDN
      
      en = ILibHashTree_GetEnumerator(cp->DeviceTable_UDN);
      while(ILibHashTree_MoveNext(en)==0)
      {
         ILibHashTree_GetValue(en,&key,&keyLen,&data);
         if(data!=NULL)
         {
            dv = (struct UPnPDevice*)data;
            MSCP_Debug(sender, dv);
         }
         else
         {
            tmp = (char*)malloc(keyLen+1);
            tmp[keyLen]=0;
            memcpy(tmp,key,keyLen);
            ILibWebServer_StreamBody(sender,"<b>UDN: ",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            ILibWebServer_StreamBody(sender,tmp,keyLen,ILibAsyncSocket_MemoryOwnership_CHAIN,0);
            ILibWebServer_StreamBody(sender,"</b><br><i>Not created yet</i><br>",34,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
      }
      
      ILibHashTree_DestroyEnumerator(en);
   }
   else if(UDN!=NULL)
   {
      if(memcmp(UDN,"K",1)==0)
      {
         rmv=1;
         UDN = UDN+1;
      }
      dv = (struct UPnPDevice*)ILibGetEntry(cp->DeviceTable_UDN,UDN,(int)strlen(UDN));
      if(ILibHasEntry(cp->DeviceTable_UDN,UDN,(int)strlen(UDN))==0)
      {
         ILibWebServer_StreamBody(sender,"<b>NOT FOUND</b>",16,ILibAsyncSocket_MemoryOwnership_STATIC,0);
      }
      else
      {
         if(dv==NULL)
         {
            ILibWebServer_StreamBody(sender,"<b>UDN exists, but device not created yet.</b><br>",50,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
         else
         {
            MSCP_Debug(sender, dv);
         }
         if(rmv)
         {
            ILibDeleteEntry(cp->DeviceTable_UDN,UDN,(int)strlen(UDN));
            ILibWebServer_StreamBody(sender,"<i>DELETED</i>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
      }
   }
   else if(URI!=NULL)
   {
      if(stricmp(URI,"*")!=0)
      {
         if(memcmp(URI,"K",1)==0)
         {
            rmv = 1;
            URI = URI+1;
         }
         key = (char*)ILibGetEntry(cp->DeviceTable_URI,URI,(int)strlen(URI));
         if(key==NULL)
         {
            ILibWebServer_StreamBody(sender,"<b>NOT FOUND</b>",16,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
         else
         {
            ILibWebServer_StreamBody(sender,"<b>UDN: ",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            ILibWebServer_StreamBody(sender,key,(int)strlen(key),ILibAsyncSocket_MemoryOwnership_USER,0);
            ILibWebServer_StreamBody(sender,"</b><br>",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            if(rmv)
            {
               ILibDeleteEntry(cp->DeviceTable_URI,URI,(int)strlen(URI));
               ILibWebServer_StreamBody(sender,"<i>DELETED</i>",14,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            }
         }
      }
      else
      {
         en = ILibHashTree_GetEnumerator(cp->DeviceTable_URI);
         while(ILibHashTree_MoveNext(en)==0)
         {
            ILibHashTree_GetValue(en,&key,&keyLen,&data);
            ILibWebServer_StreamBody(sender,"<b>URI: ",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            ILibWebServer_StreamBody(sender,key,(int)strlen(key),ILibAsyncSocket_MemoryOwnership_USER,0);
            ILibWebServer_StreamBody(sender,"</b><br>",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            if(data==NULL)
            {
               ILibWebServer_StreamBody(sender,"<i>No UDN</i><br>",17,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            }
            else
            {
               ILibWebServer_StreamBody(sender,"UDN: ",5,ILibAsyncSocket_MemoryOwnership_STATIC,0);
               ILibWebServer_StreamBody(sender,(char*)data,(int)strlen((char*)data),ILibAsyncSocket_MemoryOwnership_USER,0);
               ILibWebServer_StreamBody(sender,"<br>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            }
            ILibWebServer_StreamBody(sender,"<br>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
         ILibHashTree_DestroyEnumerator(en);
      }
   }
   else if(TOK!=NULL)
   {
      if(stricmp(TOK,"*")!=0)
      {
         key = (char*)ILibGetEntry(cp->DeviceTable_Tokens,TOK,(int)strlen(TOK));
         if(key==NULL)
         {
            ILibWebServer_StreamBody(sender,"<b>NOT FOUND</b>",16,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
         else
         {
            ILibWebServer_StreamBody(sender,"<i>Outstanding Requests</i><br>",31,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
      }
      else
      {
         en = ILibHashTree_GetEnumerator(cp->DeviceTable_URI);
         while(ILibHashTree_MoveNext(en)==0)
         {
            ILibHashTree_GetValue(en,&key,&keyLen,&data);
            ILibWebServer_StreamBody(sender,"<b>UDN: ",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            ILibWebServer_StreamBody(sender,key,(int)strlen(key),ILibAsyncSocket_MemoryOwnership_USER,0);
            ILibWebServer_StreamBody(sender,"</b><br>",8,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            if(data==NULL)
            {
               ILibWebServer_StreamBody(sender,"<i>No Tokens?</i><br>",21,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            }
            else
            {
               ILibWebServer_StreamBody(sender,"<i>Outstanding Requests</i><br>",31,ILibAsyncSocket_MemoryOwnership_STATIC,0);
            }
            ILibWebServer_StreamBody(sender,"<br>",4,ILibAsyncSocket_MemoryOwnership_STATIC,0);
         }
      }
   }
   
   
   ILibHashTree_UnLock(cp->DeviceTable_UDN);
   ILibWebServer_StreamBody(sender,"</HTML>",7,ILibAsyncSocket_MemoryOwnership_STATIC,1);
}
void MSCP_OnDebugReceive(struct ILibWebServer_Session *sender, int InterruptFlag, struct packetheader *header, char *bodyBuffer, int *beginPointer, int endPointer, int done)
{
   struct packetheader *r = NULL;
   if(!done){return;}
   
   header->DirectiveObj[header->DirectiveObjLength]=0;
   header->Directive[header->DirectiveLength]=0;
   
   if(stricmp(header->Directive,"GET")==0)
   {
      if(memcmp(header->DirectiveObj,"/UDN/",5)==0)
      {
         MSCP_DebugOnQuery(sender,header->DirectiveObj+5,NULL,NULL);
      }
      else if(memcmp(header->DirectiveObj,"/URI/",5)==0)
      {
         MSCP_DebugOnQuery(sender,NULL,header->DirectiveObj+5,NULL);
      }
      else if(memcmp(header->DirectiveObj,"/TOK/",5)==0)
      {
         MSCP_DebugOnQuery(sender,NULL,NULL,header->DirectiveObj+5);
      }
      else if(memcmp(header->DirectiveObj,"/WCDO/",6)==0)
      {
         MSCP_DebugOnQueryWCDO(sender,header->DirectiveObj+6);
      }
      else
      {
         r = ILibCreateEmptyPacket();
         ILibSetStatusCode(r,404,"Bad Request",11);
         ILibWebServer_Send(sender,r);
      }
   }
}

void MSCP_OnDebugSessionSink(struct ILibWebServer_Session *sender, void *user)
{
   sender->OnReceive = &MSCP_OnDebugReceive;
   sender->User3 = user;
}
#endif
void MSCP_ControlPoint_AddDiscoveryErrorHandler(void *cpToken, UPnPDeviceDiscoveryErrorHandler callback)
{
   struct MSCP_CP *cp = (struct MSCP_CP*)cpToken;
   cp->ErrorDispatch = callback;
}
/*! \fn MSCP_CreateControlPoint(void *Chain, void(*A)(struct UPnPDevice*),void(*R)(struct UPnPDevice*))
\brief Initalizes the control point
\param Chain The chain to attach this CP to
\param A AddSink Function Pointer
\param R RemoveSink Function Pointer
\returns ControlPoint Token
*/
void *MSCP_CreateControlPoint(void *Chain, void(*A)(struct UPnPDevice*),void(*R)(struct UPnPDevice*))
{
   struct MSCP_CP *cp = (struct MSCP_CP*)malloc(sizeof(struct MSCP_CP));
   memset(cp, 0, sizeof(struct MSCP_CP));
   
   cp->Destroy = &MSCP_StopCP;
   cp->PostSelect = NULL;
   cp->PreSelect = &MSCP_CP_PreSelect;
   cp->DiscoverSink = A;
   cp->RemoveSink = R;
   
   sem_init(&(cp->DeviceLock),0,1);
   cp->WebServer = ILibWebServer_Create(Chain,5,0,&MSCP_OnSessionSink,cp);
   cp->SIDTable = ILibInitHashTree();
   cp->DeviceTable_UDN = ILibInitHashTree();
   cp->DeviceTable_URI = ILibInitHashTree();
   cp->DeviceTable_Tokens = ILibInitHashTree();
   #ifdef UPNP_DEBUG
   ILibWebServer_Create(Chain,2,7575,&MSCP_OnDebugSessionSink,cp);
   #endif
   
   cp->SSDP = ILibCreateSSDPClientModule(Chain,"urn:schemas-upnp-org:device:MediaServer:1", 41, &MSCP_SSDP_Sink,cp);
   
   cp->HTTP = ILibCreateWebClient(5,Chain);
   ILibAddToChain(Chain,cp);
   cp->LifeTimeMonitor = ILibCreateLifeTime(Chain);
   
   cp->Chain = Chain;
   cp->RecheckFlag = 0;
   cp->AddressListLength = ILibGetLocalIPAddressList(&(cp->AddressList));
   return((void*)cp);
}

void MSCP_Invoke_ConnectionManager_GetCurrentConnectionIDs_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 1;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   char* ConnectionIDs = NULL;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==31 && memcmp(xml->Name,"GetCurrentConnectionIDsResponse",31)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==13 && memcmp(xml->Name,"ConnectionIDs",13) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     ConnectionIDs = tempBuffer;
                     ILibInPlaceXmlUnEscape(ConnectionIDs);
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,ConnectionIDs);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ConnectionManager_GetCurrentConnectionInfo_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 7;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   long TempLong;
   int RcsID = 0;
   int AVTransportID = 0;
   char* ProtocolInfo = NULL;
   char* PeerConnectionManager = NULL;
   int PeerConnectionID = 0;
   char* Direction = NULL;
   char* Status = NULL;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,int,int,char*,char*,int,char*,char*))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,int,int,char*,char*,int,char*,char*))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==32 && memcmp(xml->Name,"GetCurrentConnectionInfoResponse",32)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==5 && memcmp(xml->Name,"RcsID",5) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetLong(tempBuffer,tempBufferLength,&TempLong)==0)
                  {
                     RcsID = (int) TempLong;
                  }
               }
               else 
               if(xml->NameLength==13 && memcmp(xml->Name,"AVTransportID",13) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetLong(tempBuffer,tempBufferLength,&TempLong)==0)
                  {
                     AVTransportID = (int) TempLong;
                  }
               }
               else 
               if(xml->NameLength==12 && memcmp(xml->Name,"ProtocolInfo",12) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     ProtocolInfo = tempBuffer;
                     ILibInPlaceXmlUnEscape(ProtocolInfo);
                  }
               }
               else 
               if(xml->NameLength==21 && memcmp(xml->Name,"PeerConnectionManager",21) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     PeerConnectionManager = tempBuffer;
                     ILibInPlaceXmlUnEscape(PeerConnectionManager);
                  }
               }
               else 
               if(xml->NameLength==16 && memcmp(xml->Name,"PeerConnectionID",16) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetLong(tempBuffer,tempBufferLength,&TempLong)==0)
                  {
                     PeerConnectionID = (int) TempLong;
                  }
               }
               else 
               if(xml->NameLength==9 && memcmp(xml->Name,"Direction",9) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     Direction = tempBuffer;
                     ILibInPlaceXmlUnEscape(Direction);
                  }
               }
               else 
               if(xml->NameLength==6 && memcmp(xml->Name,"Status",6) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     Status = tempBuffer;
                     ILibInPlaceXmlUnEscape(Status);
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,int,int,char*,char*,int,char*,char*))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,int,int,char*,char*,int,char*,char*))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,RcsID,AVTransportID,ProtocolInfo,PeerConnectionManager,PeerConnectionID,Direction,Status);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ConnectionManager_GetProtocolInfo_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 2;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   char* Source = NULL;
   char* Sink = NULL;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==23 && memcmp(xml->Name,"GetProtocolInfoResponse",23)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==6 && memcmp(xml->Name,"Source",6) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     Source = tempBuffer;
                     ILibInPlaceXmlUnEscape(Source);
                  }
               }
               else 
               if(xml->NameLength==4 && memcmp(xml->Name,"Sink",4) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     Sink = tempBuffer;
                     ILibInPlaceXmlUnEscape(Sink);
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,Source,Sink);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ContentDirectory_Browse_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 4;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   unsigned long TempULong;
   char* Result = NULL;
   unsigned int NumberReturned = 0;
   unsigned int TotalMatches = 0;
   unsigned int UpdateID = 0;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         printf("[%s][%d] Connection failed!!!\n", __func__, __LINE__);
         ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         printf("[%s][%d] Soap failed!!!\n", __func__, __LINE__);
         ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==14 && memcmp(xml->Name,"BrowseResponse",14)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==6 && memcmp(xml->Name,"Result",6) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     Result = tempBuffer;
                     ILibInPlaceXmlUnEscape(Result);
                  }
               }
               else 
               if(xml->NameLength==14 && memcmp(xml->Name,"NumberReturned",14) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetULong(tempBuffer,tempBufferLength,&TempULong)==0)
                  {
                     NumberReturned = (unsigned int) TempULong;
                  }
               }
               else 
               if(xml->NameLength==12 && memcmp(xml->Name,"TotalMatches",12) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetULong(tempBuffer,tempBufferLength,&TempULong)==0)
                  {
                     TotalMatches = (unsigned int) TempULong;
                  }
               }
               else 
               if(xml->NameLength==8 && memcmp(xml->Name,"UpdateID",8) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetULong(tempBuffer,tempBufferLength,&TempULong)==0)
                  {
                     UpdateID = (unsigned int) TempULong;
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      printf("[%s][%d] something wrong !!!\n", __func__, __LINE__);
      ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,Result,NumberReturned,TotalMatches,UpdateID);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ContentDirectory_CreateObject_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 2;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   char* ObjectID = NULL;
   char* Result = NULL;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==20 && memcmp(xml->Name,"CreateObjectResponse",20)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==8 && memcmp(xml->Name,"ObjectID",8) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     ObjectID = tempBuffer;
                     ILibInPlaceXmlUnEscape(ObjectID);
                  }
               }
               else 
               if(xml->NameLength==6 && memcmp(xml->Name,"Result",6) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     Result = tempBuffer;
                     ILibInPlaceXmlUnEscape(Result);
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,char*,char*))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,ObjectID,Result);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ContentDirectory_DestroyObject_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   if(done==0){return;}
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*))_InvokeData->CallbackPtr)(Service,-1,_InvokeData->User);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   ((void (*)(struct UPnPService*,int,void*))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User);
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ContentDirectory_GetSearchCapabilities_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 1;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   char* SearchCaps = NULL;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==29 && memcmp(xml->Name,"GetSearchCapabilitiesResponse",29)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==10 && memcmp(xml->Name,"SearchCaps",10) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     SearchCaps = tempBuffer;
                     ILibInPlaceXmlUnEscape(SearchCaps);
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,SearchCaps);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ContentDirectory_GetSortCapabilities_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 1;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   char* SortCaps = NULL;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==27 && memcmp(xml->Name,"GetSortCapabilitiesResponse",27)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==8 && memcmp(xml->Name,"SortCaps",8) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     SortCaps = tempBuffer;
                     ILibInPlaceXmlUnEscape(SortCaps);
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,char*))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,SortCaps);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ContentDirectory_GetSystemUpdateID_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 1;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   unsigned long TempULong;
   unsigned int Id = 0;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,unsigned int))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,unsigned int))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==25 && memcmp(xml->Name,"GetSystemUpdateIDResponse",25)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==2 && memcmp(xml->Name,"Id",2) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetULong(tempBuffer,tempBufferLength,&TempULong)==0)
                  {
                     Id = (unsigned int) TempULong;
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,unsigned int))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,unsigned int))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,Id);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}
void MSCP_Invoke_ContentDirectory_Search_Sink(
void *WebReaderToken,
int IsInterrupt,
struct packetheader *header,
char *buffer,
int *p_BeginPointer,
int EndPointer,
int done,
void *_service,
void *state,
int *PAUSE)
{
   struct UPnPService *Service = (struct UPnPService*)_service;
   struct InvokeStruct *_InvokeData = (struct InvokeStruct*)state;
   int ArgLeft = 4;
   struct ILibXMLNode *xml;
   struct ILibXMLNode *__xml;
   char *tempBuffer;
   int tempBufferLength;
   unsigned long TempULong;
   char* Result = NULL;
   unsigned int NumberReturned = 0;
   unsigned int TotalMatches = 0;
   unsigned int UpdateID = 0;
   LVL3DEBUG(char *DebugBuffer;)
   
   if(done==0){return;}
   LVL3DEBUG(DebugBuffer = (char*)malloc(EndPointer+1);)
   LVL3DEBUG(memcpy(DebugBuffer,buffer,EndPointer);)
   LVL3DEBUG(DebugBuffer[EndPointer]=0;)
   LVL3DEBUG(printf("\r\n SOAP Recieved:\r\n%s\r\n",DebugBuffer);)
   LVL3DEBUG(free(DebugBuffer);)
   if(_InvokeData->CallbackPtr==NULL)
   {
      MSCP_Release(Service->Parent);
      free(_InvokeData);
      return;
   }
   else
   {
      if(header==NULL)
      {
         /* Connection Failed */
         ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,IsInterrupt==0?-1:IsInterrupt,_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
      else if(!ILibWebClientIsStatusOk(header->StatusCode))
      {
         /* SOAP Fault */
         ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,MSCP_GetErrorCode(buffer,EndPointer-(*p_BeginPointer)),_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
         MSCP_Release(Service->Parent);
         free(_InvokeData);
         return;
      }
   }
   
   __xml = xml = ILibParseXML(buffer,0,EndPointer-(*p_BeginPointer));
   if(ILibProcessXMLNodeList(xml)==0)
   {
      while(xml!=NULL)
      {
         if(xml->NameLength==14 && memcmp(xml->Name,"SearchResponse",14)==0)
         {
            xml = xml->Next;
            while(xml!=NULL)
            {
               if(xml->NameLength==6 && memcmp(xml->Name,"Result",6) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(tempBufferLength!=0)
                  {
                     tempBuffer[tempBufferLength] = '\0';
                     Result = tempBuffer;
                     ILibInPlaceXmlUnEscape(Result);
                  }
               }
               else 
               if(xml->NameLength==14 && memcmp(xml->Name,"NumberReturned",14) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetULong(tempBuffer,tempBufferLength,&TempULong)==0)
                  {
                     NumberReturned = (unsigned int) TempULong;
                  }
               }
               else 
               if(xml->NameLength==12 && memcmp(xml->Name,"TotalMatches",12) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetULong(tempBuffer,tempBufferLength,&TempULong)==0)
                  {
                     TotalMatches = (unsigned int) TempULong;
                  }
               }
               else 
               if(xml->NameLength==8 && memcmp(xml->Name,"UpdateID",8) == 0)
               {
                  --ArgLeft;
                  tempBufferLength = ILibReadInnerXML(xml,&tempBuffer);
                  if(ILibGetULong(tempBuffer,tempBufferLength,&TempULong)==0)
                  {
                     UpdateID = (unsigned int) TempULong;
                  }
               }
               xml = xml->Peer;
            }
         }
         if(xml!=NULL) {xml = xml->Next;}
      }
      ILibDestructXMLNodeList(__xml);
   }
   
   if(ArgLeft!=0)
   {
      ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,-2,_InvokeData->User,INVALID_DATA,INVALID_DATA,INVALID_DATA,INVALID_DATA);
   }
   else
   {
      ((void (*)(struct UPnPService*,int,void*,char*,unsigned int,unsigned int,unsigned int))_InvokeData->CallbackPtr)(Service,0,_InvokeData->User,Result,NumberReturned,TotalMatches,UpdateID);
   }
   MSCP_Release(Service->Parent);
   free(_InvokeData);
}

/*! \fn MSCP_Invoke_ConnectionManager_GetCurrentConnectionIDs(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* ConnectionIDs), void *_user)
\brief Invokes the GetCurrentConnectionIDs action in the ConnectionManager service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
*/
void MSCP_Invoke_ConnectionManager_GetCurrentConnectionIDs(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* ConnectionIDs), void* user)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+278);
   SoapBodyTemplate = "%sGetCurrentConnectionIDs xmlns:u=\"%s\"></u:GetCurrentConnectionIDs%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(186 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"GetCurrentConnectionIDs",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ConnectionManager_GetCurrentConnectionIDs_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ConnectionManager_GetCurrentConnectionInfo(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,int RcsID,int AVTransportID,char* ProtocolInfo,char* PeerConnectionManager,int PeerConnectionID,char* Direction,char* Status), void *_user, int ConnectionID)
\brief Invokes the GetCurrentConnectionInfo action in the ConnectionManager service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
\param ConnectionID Value of the ConnectionID parameter. */
void MSCP_Invoke_ConnectionManager_GetCurrentConnectionInfo(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,int RcsID,int AVTransportID,char* ProtocolInfo,char* PeerConnectionManager,int PeerConnectionID,char* Direction,char* Status), void* user, int ConnectionID)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+320);
   SoapBodyTemplate = "%sGetCurrentConnectionInfo xmlns:u=\"%s\"><ConnectionID>%d</ConnectionID></u:GetCurrentConnectionInfo%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType, ConnectionID,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(187 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"GetCurrentConnectionInfo",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ConnectionManager_GetCurrentConnectionInfo_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ConnectionManager_GetProtocolInfo(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* Source,char* Sink), void *_user)
\brief Invokes the GetProtocolInfo action in the ConnectionManager service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
*/
void MSCP_Invoke_ConnectionManager_GetProtocolInfo(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* Source,char* Sink), void* user)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+262);
   SoapBodyTemplate = "%sGetProtocolInfo xmlns:u=\"%s\"></u:GetProtocolInfo%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(178 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"GetProtocolInfo",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ConnectionManager_GetProtocolInfo_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ContentDirectory_Browse(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID), void *_user, char* ObjectID, char* BrowseFlag, char* Filter, unsigned int StartingIndex, unsigned int RequestedCount, char* SortCriteria)
\brief Invokes the Browse action in the ContentDirectory service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
\param ObjectID Value of the ObjectID parameter.  <b>MUST</b> be properly escaped.
\param BrowseFlag Value of the BrowseFlag parameter.  <b>MUST</b> be properly escaped.
\param Filter Value of the Filter parameter.  <b>MUST</b> be properly escaped.
\param StartingIndex Value of the StartingIndex parameter. 	\param RequestedCount Value of the RequestedCount parameter. 	\param SortCriteria Value of the SortCriteria parameter.  <b>MUST</b> be properly escaped.
*/
void MSCP_Invoke_ContentDirectory_Browse(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID), void* user, char* ObjectID, char* BrowseFlag, char* Filter, unsigned int StartingIndex, unsigned int RequestedCount, char* SortCriteria)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+(int)strlen(ObjectID)+(int)strlen(BrowseFlag)+(int)strlen(Filter)+(int)strlen(SortCriteria)+420);
   SoapBodyTemplate = "%sBrowse xmlns:u=\"%s\"><ObjectID>%s</ObjectID><BrowseFlag>%s</BrowseFlag><Filter>%s</Filter><StartingIndex>%u</StartingIndex><RequestedCount>%u</RequestedCount><SortCriteria>%s</SortCriteria></u:Browse%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType, ObjectID, BrowseFlag, Filter, StartingIndex, RequestedCount, SortCriteria,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(169 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"Browse",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ContentDirectory_Browse_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ContentDirectory_CreateObject(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* ObjectID,char* Result), void *_user, char* ContainerID, char* Elements)
\brief Invokes the CreateObject action in the ContentDirectory service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
\param ContainerID Value of the ContainerID parameter.  <b>MUST</b> be properly escaped.
\param Elements Value of the Elements parameter.  <b>MUST</b> be properly escaped.
*/
void MSCP_Invoke_ContentDirectory_CreateObject(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* ObjectID,char* Result), void* user, char* ContainerID, char* Elements)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+(int)strlen(ContainerID)+(int)strlen(Elements)+304);
   SoapBodyTemplate = "%sCreateObject xmlns:u=\"%s\"><ContainerID>%s</ContainerID><Elements>%s</Elements></u:CreateObject%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType, ContainerID, Elements,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(175 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"CreateObject",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ContentDirectory_CreateObject_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ContentDirectory_DestroyObject(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user), void *_user, char* unescaped_ObjectID)
\brief Invokes the DestroyObject action in the ContentDirectory service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
\param unescaped_ObjectID Value of the ObjectID parameter.  <b>Automatically</b> escaped
*/
void MSCP_Invoke_ContentDirectory_DestroyObject(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user), void* user, char* unescaped_ObjectID)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   char* ObjectID;
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   ObjectID = (char*)malloc(1+ILibXmlEscapeLength(unescaped_ObjectID));
   ILibXmlEscape(ObjectID,unescaped_ObjectID);
   buffer = (char*)malloc((int)strlen(service->ServiceType)+(int)strlen(ObjectID)+279);
   SoapBodyTemplate = "%sDestroyObject xmlns:u=\"%s\"><ObjectID>%s</ObjectID></u:DestroyObject%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType, ObjectID,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   free(ObjectID);
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(176 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"DestroyObject",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ContentDirectory_DestroyObject_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ContentDirectory_GetSearchCapabilities(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* SearchCaps), void *_user)
\brief Invokes the GetSearchCapabilities action in the ContentDirectory service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
*/
void MSCP_Invoke_ContentDirectory_GetSearchCapabilities(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* SearchCaps), void* user)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+274);
   SoapBodyTemplate = "%sGetSearchCapabilities xmlns:u=\"%s\"></u:GetSearchCapabilities%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(184 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"GetSearchCapabilities",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ContentDirectory_GetSearchCapabilities_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ContentDirectory_GetSortCapabilities(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* SortCaps), void *_user)
\brief Invokes the GetSortCapabilities action in the ContentDirectory service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
*/
void MSCP_Invoke_ContentDirectory_GetSortCapabilities(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* SortCaps), void* user)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+270);
   SoapBodyTemplate = "%sGetSortCapabilities xmlns:u=\"%s\"></u:GetSortCapabilities%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(182 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"GetSortCapabilities",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ContentDirectory_GetSortCapabilities_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ContentDirectory_GetSystemUpdateID(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,unsigned int Id), void *_user)
\brief Invokes the GetSystemUpdateID action in the ContentDirectory service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
*/
void MSCP_Invoke_ContentDirectory_GetSystemUpdateID(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,unsigned int Id), void* user)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+266);
   SoapBodyTemplate = "%sGetSystemUpdateID xmlns:u=\"%s\"></u:GetSystemUpdateID%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(180 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"GetSystemUpdateID",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ContentDirectory_GetSystemUpdateID_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}
/*! \fn MSCP_Invoke_ContentDirectory_Search(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID), void *_user, char* ContainerID, char* SearchCriteria, char* Filter, unsigned int StartingIndex, unsigned int RequestedCount, char* SortCriteria)
\brief Invokes the Search action in the ContentDirectory service
\param service The UPnPService instance to invoke the action on
\param CallbackPtr The function pointer to be called when the invocation returns
\param ContainerID Value of the ContainerID parameter.  <b>MUST</b> be properly escaped.
\param SearchCriteria Value of the SearchCriteria parameter.  <b>MUST</b> be properly escaped.
\param Filter Value of the Filter parameter.  <b>MUST</b> be properly escaped.
\param StartingIndex Value of the StartingIndex parameter. 	\param RequestedCount Value of the RequestedCount parameter. 	\param SortCriteria Value of the SortCriteria parameter.  <b>MUST</b> be properly escaped.
*/
void MSCP_Invoke_ContentDirectory_Search(struct UPnPService *service,void (*CallbackPtr)(struct UPnPService *sender,int ErrorCode,void *user,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID), void* user, char* ContainerID, char* SearchCriteria, char* Filter, unsigned int StartingIndex, unsigned int RequestedCount, char* SortCriteria)
{
   int headerLength;
   char *headerBuffer;
   char *SoapBodyTemplate;
   char* buffer;
   int bufferLength;
   char* IP;
   unsigned short Port;
   char* Path;
   struct sockaddr_in addr;
   struct InvokeStruct *invoke_data = (struct InvokeStruct*)malloc(sizeof(struct InvokeStruct));
   
   if(service==NULL)
   {
      free(invoke_data);
      return;
   }
   
   
   buffer = (char*)malloc((int)strlen(service->ServiceType)+(int)strlen(ContainerID)+(int)strlen(SearchCriteria)+(int)strlen(Filter)+(int)strlen(SortCriteria)+434);
   SoapBodyTemplate = "%sSearch xmlns:u=\"%s\"><ContainerID>%s</ContainerID><SearchCriteria>%s</SearchCriteria><Filter>%s</Filter><StartingIndex>%u</StartingIndex><RequestedCount>%u</RequestedCount><SortCriteria>%s</SortCriteria></u:Search%s";
   bufferLength = sprintf(buffer,SoapBodyTemplate,UPNPCP_SOAP_BodyHead,service->ServiceType, ContainerID, SearchCriteria, Filter, StartingIndex, RequestedCount, SortCriteria,UPNPCP_SOAP_BodyTail);
   LVL3DEBUG(printf("\r\n SOAP Sent: \r\n%s\r\n",buffer);)
   
   MSCP_AddRef(service->Parent);
   ILibParseUri(service->ControlURL,&IP,&Port,&Path);
   
   headerBuffer = (char*)malloc(169 + (int)strlen(MSCP_PLATFORM) + (int)strlen(Path) + (int)strlen(IP) + (int)strlen(service->ServiceType));
   headerLength = sprintf(headerBuffer,UPNPCP_SOAP_Header,Path,IP,Port,MSCP_PLATFORM,service->ServiceType,"Search",bufferLength);
   
   memset((char *)&addr, 0, sizeof(addr));
   addr.sin_family = AF_INET;
   addr.sin_addr.s_addr = inet_addr(IP);
   addr.sin_port = htons(Port);
   
   invoke_data->CallbackPtr = CallbackPtr;
   invoke_data->User = user;
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequestEx(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &addr,
   headerBuffer,
   headerLength,
   0,
   buffer,
   bufferLength,
   0,
   &MSCP_Invoke_ContentDirectory_Search_Sink,
   service,
   invoke_data);
   
   free(IP);
   free(Path);
}


/*! \fn MSCP_UnSubscribeUPnPEvents(struct UPnPService *service)
\brief Unregisters for UPnP events
\param service UPnP service to unregister from
*/
void MSCP_UnSubscribeUPnPEvents(struct UPnPService *service)
{
   char *IP;
   char *Path;
   unsigned short Port;
   struct packetheader *p;
   char* TempString;
   struct sockaddr_in destaddr;
   
   if(service->SubscriptionID==NULL) {return;}
   ILibParseUri(service->SubscriptionURL,&IP,&Port,&Path);
   p = ILibCreateEmptyPacket();
   ILibSetVersion(p,"1.1",3);
   
   ILibSetDirective(p,"UNSUBSCRIBE",11,Path,(int)strlen(Path));
   
   TempString = (char*)malloc((int)strlen(IP)+7);
   sprintf(TempString,"%s:%d",IP,Port);
   
   ILibAddHeaderLine(p,"HOST",4,TempString,(int)strlen(TempString));
   free(TempString);
   
   ILibAddHeaderLine(p,"User-Agent",10,MSCP_PLATFORM,(int)strlen(MSCP_PLATFORM));
   ILibAddHeaderLine(p,"SID",3,service->SubscriptionID,(int)strlen(service->SubscriptionID));
   
   memset((char *)&destaddr, 0,sizeof(destaddr));
   destaddr.sin_family = AF_INET;
   destaddr.sin_addr.s_addr = inet_addr(IP);
   destaddr.sin_port = htons(Port);
   
   MSCP_AddRef(service->Parent);
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequest(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &destaddr,
   p,
   &MSCP_OnUnSubscribeSink,
   (void*)service,
   service->Parent->CP);
   
   free(IP);
   free(Path);
}

/*! \fn MSCP_SubscribeForUPnPEvents(struct UPnPService *service, void(*callbackPtr)(struct UPnPService* service,int OK))
\brief Registers for UPnP events
\param service UPnP service to register with
\param callbackPtr Function Pointer triggered on completion
*/
void MSCP_SubscribeForUPnPEvents(struct UPnPService *service, void(*callbackPtr)(struct UPnPService* service,int OK))
{
   char* callback;
   char *IP;
   char *Path;
   unsigned short Port;
   struct packetheader *p;
   char* TempString;
   struct sockaddr_in destaddr;
   
   ILibParseUri(service->SubscriptionURL,&IP,&Port,&Path);
   p = ILibCreateEmptyPacket();
   ILibSetVersion(p,"1.1",3);
   
   ILibSetDirective(p,"SUBSCRIBE",9,Path,(int)strlen(Path));
   
   TempString = (char*)malloc((int)strlen(IP)+7);
   sprintf(TempString,"%s:%d",IP,Port);
   
   ILibAddHeaderLine(p,"HOST",4,TempString,(int)strlen(TempString));
   free(TempString);
   
   ILibAddHeaderLine(p,"NT",2,"upnp:event",10);
   ILibAddHeaderLine(p,"TIMEOUT",7,"Second-180",10);
   ILibAddHeaderLine(p,"User-Agent",10,MSCP_PLATFORM,(int)strlen(MSCP_PLATFORM));
   
   callback = (char*)malloc(10+(int)strlen(service->Parent->InterfaceToHost)+6+(int)strlen(Path));
   sprintf(callback,"<http://%s:%d%s>",service->Parent->InterfaceToHost,ILibWebServer_GetPortNumber(((struct MSCP_CP*)service->Parent->CP)->WebServer),Path);
   
   ILibAddHeaderLine(p,"CALLBACK",8,callback,(int)strlen(callback));
   free(callback);
   
   memset((char *)&destaddr, 0,sizeof(destaddr));
   destaddr.sin_family = AF_INET;
   destaddr.sin_addr.s_addr = inet_addr(IP);
   destaddr.sin_port = htons(Port);
   
   MSCP_AddRef(service->Parent);
   ILibWebClient_SetQosForNextRequest(((struct MSCP_CP*)service->Parent->CP)->HTTP,MSCP_InvocationPriorityLevel);
   ILibWebClient_PipelineRequest(
   ((struct MSCP_CP*)service->Parent->CP)->HTTP,
   &destaddr,
   p,
   &MSCP_OnSubscribeSink,
   (void*)service,
   service->Parent->CP);
   
   free(IP);
   free(Path);
}

/*! \fn MSCP_GetCustomXML_X_DLNACAP(struct UPnPDevice *d)
\brief Obtains the meta data associated with \a X_DLNACAP and X_DLNACAP_NAMESPACE
\param d The UPnPDevice to query
\returns The associated meta-data. NULL if this tag was not present on the device
*/
char* MSCP_GetCustomXML_X_DLNACAP(struct UPnPDevice *d)
{
   return(MSCP_GetCustomTagFromDevice(d,X_DLNACAP_NAMESPACE, X_DLNACAP));
}
/*! \fn MSCP_GetCustomXML_X_DLNADOC(struct UPnPDevice *d)
\brief Obtains the meta data associated with \a X_DLNADOC and X_DLNADOC_NAMESPACE
\param d The UPnPDevice to query
\returns The associated meta-data. NULL if this tag was not present on the device
*/
char* MSCP_GetCustomXML_X_DLNADOC(struct UPnPDevice *d)
{
   return(MSCP_GetCustomTagFromDevice(d,X_DLNADOC_NAMESPACE, X_DLNADOC));
}

//Add for Toshiba Live Stream
char* MSCP_GetCustomXML_X_REGZAAPPS(struct UPnPDevice *d)
{
   return(MSCP_GetCustomTagFromDevice(d,X_REGZAAPPS_NAMESPACE, X_REGZAAPPS));
}









