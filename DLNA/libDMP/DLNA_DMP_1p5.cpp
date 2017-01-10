#include "DLNA_DMP.h"
#include "MediaServerCP_ControlPoint.h"
#include "ILibWebClient.h"

#include "MimeTypes.h"
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <stdlib.h>
#include <assert.h>

RTK_DLNA_DMP_Singleton::AutoDMPPtr RTK_DLNA_DMP_Singleton::p;

sem_t RTK_DLNA_DMP::callbackResponseSemaphore;
sem_t RTK_DLNA_DMP::callbackRequestSemaphore;
sem_t RTK_DLNA_DMP::currCDServiceLock;
sem_t RTK_DLNA_DMP::upnpBrowserLock;
void* RTK_DLNA_DMP::DMP_Microstack_Chain;
DMR_MicroStackToken RTK_DLNA_DMP::RTKMP_microStack;
void* RTK_DLNA_DMP::DMP_Monitor;
void* RTK_DLNA_DMP::MS_KnownDevicesTable = NULL;
MMSCP_MediaObject** RTK_DLNA_DMP::r_container;
int RTK_DLNA_DMP::containerSize = 0;
MMSCP_MediaObject** RTK_DLNA_DMP::r_item;
int RTK_DLNA_DMP::itemSize = 0;
int RTK_DLNA_DMP::itemVideoSize = 0;
int RTK_DLNA_DMP::itemAudioSize = 0;
int RTK_DLNA_DMP::itemImageSize = 0;
struct UPnPService *RTK_DLNA_DMP::currCDService;
struct MMSCP_ResultsList *RTK_DLNA_DMP::mostRecentBrowseResults;
bool (*(RTK_DLNA_DMP::deviceTriggerUpdate))(int, void *);
bool (*(RTK_DLNA_DMP::deviceAddedOrRemovedTrigger))(int, char *, char *);
void* RTK_DLNA_DMP::deviceTriggerUpdateData;

pthread_t RTK_DLNA_DMP::DMPthread = 0;
void* RTK_DLNA_DMP::MS_ControlPoint;
void* RTK_DLNA_DMP::currBrowseDirectoryStack = 0;
int RTK_DLNA_DMP::m_state;
int RTK_DLNA_DMP::m_bIsStopBrowsing;
int RTK_DLNA_DMP::m_bIsBrowsingState;

#define MMSC_Error_XmlNotWellFormed 1000
#define UPNP_OBJECT_SIZE_LIMITED    4096 

#if IS_CHIP(MARS) || IS_CHIP(JUPITER) || IS_CHIP(SATURN) || defined(IS_TV_CHIP)
#define RENDERER_SINKPROTOCOLINFO "http-get:*:*:*"
#else
#define RENDERER_SINKPROTOCOLINFO "http-get:*:audio/mpeg:*,http-get:*:audio/x-ms-wma:*,http-get:*:audio/wav:*,http-get:*:audio/lpcm:*,http-get:*:video/mpeg2:*,http-get:*:audio/x-mpegurl:*,http-get:*:image/jpeg:*"
#endif
#define CDS_STRING_ALL_FILTER                                   "@dlna:dlnaManaged,dc:title,dc:creator,upnp:class,dc:date,upnp:album,upnp:artist,upnp:genre,res,res@bitrate,res@bitsPerSample,res@colorDepth,res@duration,res@protection,res@resolution,res@sampleFrequency,res@nrAudioChannels,res@size,res@importUri,res@dlna:ifoFileURI,res@importIfoFileUri,res@dlna:resumeUpload,res@dlna:uploadedSize,res@dlna:trackTotal"

#if 0
static void threadLauncher(void *parm) {
/* yuyu
	RTK_DLNA_DMP *instance= (RTK_DLNA_DMP *)parm;
	instance->Run();
yuyu */
	return;
}
#endif

RTK_DLNA_DMP::RTK_DLNA_DMP() {
	// refer to CLShell:Init()

	// create chain
    if(DMPthread == 0)
    {
	DMP_Microstack_Chain = ILibCreateChain();

	//RTKMP_microStack = DMR_CreateMicroStack(DMP_Microstack_Chain ,"yuyu UPnP Player","6401ab56-6067-4530-96d6-06603d5c5b90","0000001",600,0);
//	RTKMP_microStack = DMR_CreateMicroStack(DMP_Microstack_Chain ,"Sony Playstation 3","25fa8952-e706-4d74-bdf1-ff44a3222cb7","0000001",600,0);
/* TODO: Define available protocolInfo */
//#define RENDERER_SINKPROTOCOLINFO "http-get:*:audio/mpeg:*,http-get:*:audio/x-ms-wma:*,http-get:*:audio/wav:*,http-get:*:audio/lpcm:*,http-get:*:video/mpeg2:*,http-get:*:audio/x-mpegurl:*,http-get:*:image/jpeg:*"

#define RENDERER_SOURCEPROTOCOLINFO RENDERER_SINKPROTOCOLINFO

//        RTKMR_SetState_ConnectionManager_SourceProtocolInfo(RTKMP_microStack,RENDERER_SOURCEPROTOCOLINFO);
//        RTKMR_SetState_ConnectionManager_SinkProtocolInfo(RTKMP_microStack,RENDERER_SINKPROTOCOLINFO);
//        RTKMR_SetState_ConnectionManager_CurrentConnectionIDs(RTKMP_microStack,"0");
//	RTKMR_SetState_RenderingControl_LastChange(RTKMP_microStack,"<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/RCS/\"/>");
//        RTKMR_SetState_AVTransport_LastChange(RTKMP_microStack,"<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT/\"/>");


	// following code copied from CommandLineShell.c
	// initialize thread protection semaphores

	sem_init(&callbackResponseSemaphore, 0, 0);
	sem_init(&callbackRequestSemaphore, 0, 0);
	sem_init(&currCDServiceLock, 0, 1);
	sem_init(&upnpBrowserLock, 0, 1);
	//Initially there is no server
	currCDService = NULL;

	// create browsing-related variables

	ILibCreateStack(&currBrowseDirectoryStack);
	ILibPushStack(&currBrowseDirectoryStack, MakeStrcpy("0"));
	mostRecentBrowseResults = NULL;
	MS_KnownDevicesTable = ILibInitHashTree();
    r_container = NULL;
    r_item = NULL;
    containerSize = 0;
    itemSize = 0;
	itemVideoSize = 0;
	itemAudioSize = 0;
	itemImageSize = 0;

	deviceTriggerUpdate = NULL;
	deviceAddedOrRemovedTrigger = NULL;
	deviceTriggerUpdateData = NULL;

	// Initialize Media Server Control Point(MMSCP), and register call back functions
	MS_ControlPoint = MMSCP_Init(DMP_Microstack_Chain, MS_BrowseResponded, MS_ServerAddedOrRemoved);
	m_state = 0;
	m_bIsStopBrowsing  = false;
	m_bIsBrowsingState = false;

/*yuyu*/pthread_create(&DMPthread, NULL, &Run, NULL);
/*yuyu*///pthread_detach(DMPthread);
	DMP_Monitor = (void *)ILibCreateLifeTime(DMP_Microstack_Chain);
//TODO	ILibLifeTime_Add(DMP_Monitor, NULL, 4, (void*)&DMP_Network_Monitor, NULL);
//TODO	ILibLifeTime_Add(DMP_Monitor, NULL, 4, (ILibLifeTime_OnCallback*)&DMP_Network_Monitor, NULL);
	ILibLifeTime_Add(DMP_Monitor, NULL, 4, &DMP_Network_Monitor, NULL);
    }
    else
    {
        Terminate();
        ReInit();
        m_state = 0;
    }

	return;
}

RTK_DLNA_DMP::~RTK_DLNA_DMP() {
	// refer to CLShell:Uninit()
	Terminate();
	pthread_join(DMPthread, 0);
	sem_destroy(&callbackResponseSemaphore);
	sem_destroy(&callbackRequestSemaphore);
	sem_destroy(&currCDServiceLock);
	sem_destroy(&upnpBrowserLock);
	if(currBrowseDirectoryStack != NULL)
	{
		popDirectoryStackToRoot();
		free(ILibPopStack(&currBrowseDirectoryStack));
		ILibClearStack(&currBrowseDirectoryStack);
	}

	if(NULL != currCDService)
	{
                //MSCP_UnSubscribeUPnPEvents(currCDService);
		MSCP_Release(currCDService->Parent);
		currCDService = NULL;
	}

	if(mostRecentBrowseResults != NULL) 
	{
		free(mostRecentBrowseResults);
		mostRecentBrowseResults = NULL;
	}

	if(MS_KnownDevicesTable!=NULL)
		ILibDestroyHashTree(MS_KnownDevicesTable);
	MS_KnownDevicesTable = NULL;
        if( r_container )
        {
            free(r_container);
            r_container = NULL;
        }
        if( r_item )
        {
            free(r_item);
            r_item = NULL;
        }

#ifndef ANDROID_PLATFORM
    pthread_cancel(DMPthread);
#endif
       DMPthread = 0;
//yuyu	delete DMPthread;
}

void RTK_DLNA_DMP::ReInit(void){
	// semaphore
	sem_destroy(&callbackResponseSemaphore);
	sem_destroy(&callbackRequestSemaphore);
	sem_destroy(&currCDServiceLock);
	sem_destroy(&upnpBrowserLock);
	sem_init(&callbackResponseSemaphore, 0, 0);
	sem_init(&callbackRequestSemaphore, 0, 0);
	sem_init(&currCDServiceLock, 0, 1);
	sem_init(&upnpBrowserLock, 0, 1);
	popDirectoryStackToRoot();

	if(NULL != currCDService)
	{
                //MSCP_UnSubscribeUPnPEvents(currCDService);
		MSCP_Release(currCDService->Parent);
		currCDService = NULL;
	}
	if(mostRecentBrowseResults != NULL) 
	{
		free(mostRecentBrowseResults);
		mostRecentBrowseResults = NULL;
	}
        ILibDestroyHashTree(MS_KnownDevicesTable);
	MS_KnownDevicesTable = ILibInitHashTree();
        if( r_container )
        {
            free(r_container);
            r_container = NULL;
        }
        if( r_item )
        {
            free(r_item);
            r_item = NULL;
        }
	containerSize = 0;
	itemSize  = 0;
	itemVideoSize = 0;
	itemAudioSize = 0;
	itemImageSize = 0;

//	bool (RTK_DLNA_DMP::*deviceTriggerUpdate)(void *) = NULL;
	deviceTriggerUpdate = NULL;
	deviceAddedOrRemovedTrigger = NULL;
	deviceTriggerUpdateData = NULL;

	// create chain
	DMP_Microstack_Chain = ILibCreateChain();
//	RTKMP_microStack = DMR_CreateMicroStack(DMP_Microstack_Chain ,"Sony Playstation 3","25fa8952-e706-4d74-bdf1-ff44a3222cb7","0000001",600,0);
	//RTKMP_microStack = DMR_CreateMicroStack(DMP_Microstack_Chain ,"yuyu UPnP Player","6401ab56-6067-4530-96d6-06603d5c5b90","0000001",600,0);
	// Initialize Media Server Control Point(MMSCP), and register call back functions
	MS_ControlPoint = MMSCP_Init(DMP_Microstack_Chain, MS_BrowseResponded, MS_ServerAddedOrRemoved);
	DMP_Monitor = (void *)ILibCreateLifeTime(DMP_Microstack_Chain);
//TODO	ILibLifeTime_Add(DMP_Monitor, NULL, 4, (void *)&DMP_Network_Monitor, NULL);
	ILibLifeTime_Add(DMP_Monitor, NULL, 4, &DMP_Network_Monitor, NULL);
}

//yuyu void RTK_DLNA_DMP::Run(void){
void *RTK_DLNA_DMP::Run(void* arg) {
	while(1)
	{
		if(m_state == 1)
		{
			ILibStartChain(DMP_Microstack_Chain);
		#ifndef ANDROID_PLATFORM
			ReInit();
		#endif
		}else if(m_state == -1){
		#ifdef ANDROID_PLATFORM
			break;
		#endif
		}
		usleep(100*1000);
	}
    return 0;
}

void RTK_DLNA_DMP::Start(void) {
	// start the chain
		m_state = 1;
}

void RTK_DLNA_DMP::Terminate(void) {
		if( m_state == 1 )
			ILibStopChain(DMP_Microstack_Chain);
		m_state = -1;
}

int RTK_DLNA_DMP::MediaServerSizes()
{
    int serverCount = 0;
    void *enumeration;
    char *key;
    int keyLength;
    void *data;
	
	if(MS_KnownDevicesTable == NULL)
		return 0;
	
    ILibHashTree_Lock(MS_KnownDevicesTable);
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);
    do {
        key = NULL;
        keyLength = 0;
        data = NULL;

        ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

        if(0 != keyLength) {
            serverCount++;
        }
    } while(!ILibHashTree_MoveNext(enumeration));
    ILibHashTree_DestroyEnumerator(enumeration);
    ILibHashTree_UnLock(MS_KnownDevicesTable);

    return serverCount;
}

int RTK_DLNA_DMP::MediaContainerObjectSizes()
{
    return containerSize;
}

int RTK_DLNA_DMP::MediaItemObjectSizes()
{
    return itemSize;
}

int RTK_DLNA_DMP::MediaItemVideoSizes()
{
    return itemVideoSize;
}

int RTK_DLNA_DMP::MediaItemAudioSizes()
{
    return itemAudioSize;
}

int RTK_DLNA_DMP::MediaItemImageSizes()
{
    return itemImageSize;
}

int RTK_DLNA_DMP::queryMediaServers(struct MediaServerEntry **result) {
	void *enumeration;
	char *key;
	int keyLength;
	void *data;
	char *charData;
	int serverCount = 0;

	struct MediaServerEntry **test;

	// step.0 : start temporary hash tree to store (key, value) = (UDN, friendlyName)
	void *namesTable = ILibInitHashTree();

	// step.1 : lock hash tree
	ILibHashTree_Lock(MS_KnownDevicesTable);

	enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);
	do {
		key = NULL;
		keyLength = 0;
		data = NULL;

		ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

		if(0 != keyLength) {
			ILibAddEntry(namesTable, key, keyLength, ((struct UPnPDevice*)data)->FriendlyName);
			serverCount++;
		}
	} while(!ILibHashTree_MoveNext(enumeration));
	ILibHashTree_DestroyEnumerator(enumeration);

	if(serverCount == 0)
		return serverCount;

	// step.2 : get data from hashtree and put it into result array
	//*result = (struct MediaServerEntry *)malloc(sizeof(struct MediaServerEntry)*serverCount);
	*test = (struct MediaServerEntry *)malloc(sizeof(struct MediaServerEntry)*serverCount);

	enumeration = ILibHashTree_GetEnumerator(namesTable);
	serverCount = 0;

	do {
		key = NULL;
		keyLength = 0;
		data = NULL;
		charData = NULL;

		ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);
		charData = (char*)data;

		if(0 != keyLength) {
			//((*result)[serverCount]).udn = (char *)malloc(sizeof(char)*(keyLength+1));
			((*test)[serverCount]).udn = (char *)malloc(sizeof(char)*(keyLength+1));
			//memcpy((*result)[serverCount].udn, key, keyLength);
			memcpy((*test)[serverCount].udn, key, keyLength);
			//((*result)[serverCount]).friendlyName = strdup(charData);
			((*test)[serverCount]).friendlyName = strdup(charData);
		}
		serverCount++;
	} while(!ILibHashTree_MoveNext(enumeration));

	ILibHashTree_DestroyEnumerator(enumeration);

	// step.3 : release HashTree lock
	ILibHashTree_UnLock(MS_KnownDevicesTable);
	ILibDestroyHashTree(namesTable);

	return serverCount;
}

char *RTK_DLNA_DMP::MediaServerName(int index, char *name)
{
    void *enumeration;
    char *key;
    int keyLength;
    void *data;
    int count = 0;

    if( index < 0)
    {
        if( name )
            name[0] = '\0';
        return NULL;
    }

//printf("[DLNA DMP] file:%s, func:%s, line:%d\n", __FILE__, __func__, __LINE__);
    ILibHashTree_Lock(MS_KnownDevicesTable);
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);

    do{
	key = NULL;
	data = NULL;
	keyLength = 0;

	ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

	if( 0 != keyLength )
	    count++;
    }while((count<=index) && !ILibHashTree_MoveNext(enumeration) );

    if( name )
    {
        if( count <= index )
	    name[0] = '\0';
        else if( data == NULL )
            name[0] = '\0';
        else
        {
        	char *tmpFriendlyName = NULL;
			tmpFriendlyName = ((struct UPnPDevice*)data)->FriendlyName;
			if(tmpFriendlyName != NULL)
				sprintf(name, "%s", tmpFriendlyName);
			else
				name[0] = '\0';
		}
    }

    ILibHashTree_DestroyEnumerator(enumeration);
    ILibHashTree_UnLock(MS_KnownDevicesTable);
    
    if( data == NULL)
        return NULL;

    return name;
}

//Add for Toshiba Live Stream
char *RTK_DLNA_DMP::MediaServerLocationURL(int index, char *locationURL)
{
    void *enumeration;
    char *key;
    int keyLength;
    void *data;
    int count = 0;

    if( index < 0)
    {
        if( locationURL )
            locationURL[0] = '\0';
        return NULL;
    }

    ILibHashTree_Lock(MS_KnownDevicesTable);
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);

    do{
	key = NULL;
	data = NULL;
	keyLength = 0;

	ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

	if( 0 != keyLength )
	    count++;
    }while((count<=index) && !ILibHashTree_MoveNext(enumeration) );

    if( locationURL )
    {
        if( count <= index )
	    locationURL[0] = '\0';
        else if( data == NULL )
            locationURL[0] = '\0';
        else
        {
        	char *tmpURLValue = NULL;
			tmpURLValue = ((struct UPnPDevice*)data)->LocationURL;
			if(tmpURLValue != NULL)
				sprintf(locationURL, "%s", tmpURLValue);
			else
				locationURL[0] = '\0';
        }
    }

    ILibHashTree_DestroyEnumerator(enumeration);
    ILibHashTree_UnLock(MS_KnownDevicesTable);
    
    if( data == NULL)
        return NULL;

    return locationURL;
}

//Add for Toshiba Live Stream
char *RTK_DLNA_DMP::MediaServerModelDescription(int index, char *modelDescription)
{
    void *enumeration;
    char *key;
    int keyLength;
    void *data;
    int count = 0;

    if( index < 0)
    {
        if( modelDescription )
            modelDescription[0] = '\0';
        return NULL;
    }

    ILibHashTree_Lock(MS_KnownDevicesTable);
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);

    do{
	key = NULL;
	data = NULL;
	keyLength = 0;

	ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

	if( 0 != keyLength )
	    count++;
    }while((count<=index) && !ILibHashTree_MoveNext(enumeration) );

    if( modelDescription )
    {
        if( count <= index )
	    	modelDescription[0] = '\0';
        else if( data == NULL )
            modelDescription[0] = '\0';
        else
        {
        	char *tmpDescriptionValue = NULL;
			tmpDescriptionValue = ((struct UPnPDevice*)data)->ModelDescription;
			if(tmpDescriptionValue != NULL)
				sprintf(modelDescription, "%s", tmpDescriptionValue);
			else
				modelDescription[0] = '\0';
		}
    }

    ILibHashTree_DestroyEnumerator(enumeration);
    ILibHashTree_UnLock(MS_KnownDevicesTable);

    if( data == NULL)
        return NULL;

    return modelDescription;
}

//Add for Toshiba Live Stream
char *RTK_DLNA_DMP::MediaServerManufacturer(int index, char *manufacturer)
{
    void *enumeration;
    char *key;
    int keyLength;
    void *data;
    int count = 0;

    if( index < 0)
    {
        if( manufacturer )
            manufacturer[0] = '\0';
        return NULL;
    }

    ILibHashTree_Lock(MS_KnownDevicesTable);
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);

    do{
	key = NULL;
	data = NULL;
	keyLength = 0;

	ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

	if( 0 != keyLength )
	    count++;
    }while((count<=index) && !ILibHashTree_MoveNext(enumeration) );

    if( manufacturer )
    {
        if( count <= index )
	    manufacturer[0] = '\0';
        else if( data == NULL )
            manufacturer[0] = '\0';
        else
        {
        	char *tmpManufacturerValue = NULL;
			tmpManufacturerValue = ((struct UPnPDevice*)data)->ManufacturerName;
			if(tmpManufacturerValue != NULL)
				sprintf(manufacturer, "%s", tmpManufacturerValue);
			else
				 manufacturer[0] = '\0';
        }
    }

    ILibHashTree_DestroyEnumerator(enumeration);
    ILibHashTree_UnLock(MS_KnownDevicesTable);

    if( data == NULL)
        return NULL;

    return manufacturer;
}

//Add for Toshiba Live Stream
char *RTK_DLNA_DMP::MediaServerRegzaApps(int index, char *appscap)
{
    void *enumeration;
    char *key;
    int keyLength;
    void *data;
    int count = 0;

    if( index < 0)
    {
        if( appscap )
            appscap[0] = '\0';
        return NULL;
    }

    ILibHashTree_Lock(MS_KnownDevicesTable);
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);

    do{
	key = NULL;
	data = NULL;
	keyLength = 0;

	ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

	if( 0 != keyLength )
	    count++;
    }while((count<=index) && !ILibHashTree_MoveNext(enumeration) );

    if( appscap )
    {
        if( count <= index )
	    appscap[0] = '\0';
        else if( data == NULL )
            appscap[0] = '\0';
        else
		{
			char *tmpTagValue = NULL;
			tmpTagValue = MSCP_GetCustomXML_X_REGZAAPPS((struct UPnPDevice*)data);
			if(tmpTagValue != NULL)
				sprintf(appscap, "%s", tmpTagValue);
			else
				appscap[0] = '\0';
        }
    }

    ILibHashTree_DestroyEnumerator(enumeration);
    ILibHashTree_UnLock(MS_KnownDevicesTable);

    if( data == NULL)
        return NULL;

    return appscap;
}

char *RTK_DLNA_DMP::MediaServerUDN(int index, char *UDN)
{
    void *enumeration;
    char *key;
    int keyLength;
    void *data;
    int count = 0;

    if( index < 0)
    {
        if( UDN )
            UDN[0] = '\0';
        return NULL;
    }

    ILibHashTree_Lock(MS_KnownDevicesTable);
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);

    do{
		key = NULL;
		data = NULL;
		keyLength = 0;

		ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);

		if( 0 != keyLength )
		    count++;
    }while((count<=index) && !ILibHashTree_MoveNext(enumeration) );

    if( UDN )
    {
        if( count <= index )
	    	UDN[0] = '\0';
        else if( data == NULL )
            UDN[0] = '\0';
        else
		{
			char *tmpUDN = NULL;
			tmpUDN = ((struct UPnPDevice*)data)->UDN;
			if(tmpUDN != NULL)
				sprintf(UDN, "%s", tmpUDN);
			else
				 tmpUDN[0] = '\0';
        }
    }

    ILibHashTree_DestroyEnumerator(enumeration);
    ILibHashTree_UnLock(MS_KnownDevicesTable);

    if( data == NULL)
        return NULL;

    return UDN;
}


char *RTK_DLNA_DMP::UPnPServiceContentDirectory(int index, char *name)
{
    if( name )
        name[0] = '\0';

    if( index < 0 || index >= containerSize )
        return NULL;

    if( r_container && r_container[index] && r_container[index]->Title!=NULL)
    {
        if(name)
            snprintf(name, 256, "%s", (char *)r_container[index]->Title);
        return r_container[index]->Title;
    }

    return NULL;
}

char *RTK_DLNA_DMP::UPnPServiceContentFile(int index, char *name)
{
    if(name)
        name[0] = '\0';

    if( index < 0 || index >= itemSize )
        return NULL;

    if( r_item && r_item[index] && r_item[index]->Title!=NULL)
    {
        if(name)
            snprintf(name, 256, "%s", (char *)r_item[index]->Title);
        return r_item[index]->Title;
    }

    return NULL;
}

/* should be in MSCP_ControlPoint.c */
#if 0
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

    sem_t DeviceLock;
    void* SIDTable;
    void* DeviceTable;

    void *Chain;
    int RecheckFlag;
    int AddressListLength;
    int *AddressList;
};
#else
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
#endif

void RTK_DLNA_DMP::DMP_Network_Monitor(void *){
    if(m_state == 1)
    {
        int length;
        int *list;
        MSCP_CP* cp = (MSCP_CP*)MS_ControlPoint;

        length = ILibGetLocalIPAddressList(&list);
        if(length!=cp->AddressListLength|| memcmp((void*)list,(void*)(cp->AddressList),sizeof(int)*length)!=0)
        {
            printf("%s, %s, %d System IPs changes!!!\n", __FILE__, __func__, __LINE__);
            MSCP__CP_IPAddressListChanged(MS_ControlPoint);
            //cp->RecheckFlag = 1;
            //ILibForceUnBlockChain(DMP_Microstack_Chain);
        }
        free(list);

//TODO        ILibLifeTime_Add(DMP_Monitor, NULL, 4, (void *)&DMP_Network_Monitor, NULL);
        ILibLifeTime_Add(DMP_Monitor, NULL, 4, &DMP_Network_Monitor, NULL);
    }
}

void RTK_DLNA_DMP::MediaServerSearchScan(void)
{

printf("%s, %s, %d\n", __FILE__, __func__, __LINE__);
printf("%s, %s, %d\n", __FILE__, __func__, __LINE__);
    struct MSCP_CP *CP = (struct MSCP_CP*)MS_ControlPoint;
    if( CP && CP->SSDP )
	ILibSSDP_IPAddressListChanged(CP->SSDP);
    else
    {
printf("%s, %s, %d\n", __FILE__, __func__, __LINE__);
printf("[DLNA DMP] SearchScan....wrong....\n");
printf("%s, %s, %d\n", __FILE__, __func__, __LINE__);
    }
}

void ProcessDeviceRemoval(struct MSCP_CP* CP, struct UPnPDevice *device)
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
      ProcessDeviceRemoval(CP,temp);
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

void RTK_DLNA_DMP::MediaServerDelete(char *chosenUDN)
{
	struct UPnPDevice *theMServer;
	if(chosenUDN[0] != '\0')
	{
		theMServer = (struct UPnPDevice *)ILibGetEntry(MS_KnownDevicesTable, chosenUDN, (int)strlen(chosenUDN));
		if(theMServer != NULL)
		{
			//delete deviceUDN from MS_KnownDevicesTable
			ILibHashTree_Lock(MS_KnownDevicesTable);
			ILibDeleteEntry(MS_KnownDevicesTable,
				theMServer->UDN,
				(int)strlen(theMServer->UDN));
			ILibHashTree_UnLock(MS_KnownDevicesTable);

			//delete entry(deviceUDN & LocationURL) from MediaServer_ControlPoint(MSCP_CP)
			while(theMServer->Parent!=NULL)
         	{
            	theMServer = theMServer->Parent;
         	}
	         //
	         // Remove the timed event, checking the refreshing of notify packets
	         //
	         ProcessDeviceRemoval((struct MSCP_CP*)MS_ControlPoint,theMServer);
	         //
	         // If the app above subscribed to events, there will be extra references
	         // that we can delete, otherwise, the device ain't ever going away
	         //
	        int i = theMServer->ReferenceTiedToEvents;
	        while(i!=0)
	        {
	            MSCP_Release(theMServer);
	            --i;
	         }
	         MSCP_Release(theMServer);
		}
	}
		
}
bool RTK_DLNA_DMP::UPnPServiceBrowse(void)
{
	char *browseID = (char *)ILibPeekStack(&currBrowseDirectoryStack);
	return UPnPServiceBrowse(browseID);
}

bool RTK_DLNA_DMP::UPnPServiceBrowse(char *id)
{
	sem_wait(&upnpBrowserLock);
    bool retval = false;
    int browseCount = 1000, startingIndex = 0;
    struct MMSCP_BrowseArgs browseArgs;
    struct UPnPService *cds = getCurrentCDService();
    struct MMSCP_ResultsList *RecentBrowseResults = NULL;

    printf("[DLNA DMP] file:%s, func:%s, line:%d\n", __FILE__, __func__, __LINE__);
	if( m_bIsStopBrowsing == true) {printf("[DLNA DMP] file:%s, func:%s, line:%d\nSomething error with m_bIsStopBrowsing\n",__FILE__, __func__, __LINE__);}

    if(mostRecentBrowseResults != NULL)
    {
		MMSCP_DestroyResultsList(mostRecentBrowseResults);
		mostRecentBrowseResults = NULL;
    }

	if( r_container )
	{
		free(r_container);
		r_container = NULL;
		containerSize = 0;
	}
	if( r_item )
	{
		free(r_item);
		r_item = NULL;
		itemSize = 0;
		itemVideoSize = 0;
		itemAudioSize = 0;
		itemImageSize = 0;
	}
    if( cds!=NULL )
    {
    	while( startingIndex >= 0 && browseCount--)
    	{
			browseArgs.BrowseFlag = MMSCP_BrowseFlag_Children;
//			browseArgs.Filter = "*"; //"res";
			browseArgs.Filter = CDS_STRING_ALL_FILTER; //"res";
//			browseArgs.ObjectID = (char *)ILibPeekStack(&currBrowseDirectoryStack);
			browseArgs.ObjectID = id;
			browseArgs.RequestedCount = 256;
			browseArgs.SortCriteria = "";
			browseArgs.StartingIndex = startingIndex;
			browseArgs.UserObject = NULL;

			MMSCP_Invoke_Browse(cds, &browseArgs);

//			MSCP_Release(cds->Parent);	//remove out for the while loop, one MSCP_AddRef in getCurrentCDService matches one MSCP_Release.

			m_bIsBrowsingState = true;
			if( waitForResponse() == 0)
				retval = true;
			else
			{
				retval = false;
				break;
			}
        //if the return results are not enough, use while loop to get the resident objects.
        	if( mostRecentBrowseResults != NULL && mostRecentBrowseResults->TotalMatches != mostRecentBrowseResults->NumberReturned )
			{
            	if( RecentBrowseResults == NULL )
            	{
                	RecentBrowseResults = mostRecentBrowseResults;
                	startingIndex =  mostRecentBrowseResults->NumberReturned;
                	mostRecentBrowseResults = NULL;
            	}
            	else
            	{
                	// Just move MMSCP_MdeiaObject object from mostRecentBrowseResults(returned)
                	// to RecentBrowseResults(temp)
                	struct MMSCP_MediaObject *newObj = NULL;
                	void *node = NULL;
                	if(mostRecentBrowseResults->LinkedList!=NULL)
                    	node = ILibLinkedList_GetNode_Head(mostRecentBrowseResults->LinkedList);
                	if( node != NULL )
                    	newObj = (struct MMSCP_MediaObject*)ILibLinkedList_GetDataFromNode(node);
                	while(newObj != NULL)
                	{
                    	ILibLinkedList_AddTail(RecentBrowseResults->LinkedList, newObj);
                    	newObj = NULL;
                    	node = ILibLinkedList_GetNextNode(node);
                    	if(node!=NULL)
                        	newObj = (struct MMSCP_MediaObject*)ILibLinkedList_GetDataFromNode(node);
                	}

                	startingIndex +=  mostRecentBrowseResults->NumberReturned;

                	// All the match objects are collected. Break the while loop
                	if( mostRecentBrowseResults->TotalMatches == (unsigned int)startingIndex )
                    {
                            if( RecentBrowseResults->TotalMatches != mostRecentBrowseResults->TotalMatches)
                                RecentBrowseResults->TotalMatches = mostRecentBrowseResults->TotalMatches;
                		startingIndex = -1;
                    }

                	// The number of objects is over 1024!!! Break the while loop
                	if( startingIndex != -1 && (unsigned int)startingIndex >= UPNP_OBJECT_SIZE_LIMITED )
                    {
                            RecentBrowseResults->TotalMatches = startingIndex;
                		startingIndex = -1;
                    }

                	if(mostRecentBrowseResults->LinkedList)
                	    ILibLinkedList_Destroy(mostRecentBrowseResults->LinkedList); 
                    free(mostRecentBrowseResults);
    				mostRecentBrowseResults = NULL;
				}
			}
			else
			startingIndex = -1;
		}
    	if( RecentBrowseResults != NULL)
			mostRecentBrowseResults = RecentBrowseResults;
		MSCP_Release(cds->Parent);
    }

    if( retval )
    {
    printf("[DLNA DMP] file:%s, func:%s, line:%d\n[DLNA DMP] Browse Responsed\n", __FILE__, __func__, __LINE__);


		if(mostRecentBrowseResults != NULL)
		{
	    	void *node;
	    	struct MMSCP_MediaObject *currObject = NULL;
	    	unsigned int currObjectClass;
            unsigned int currObjectMediaType = 0;

			if( mostRecentBrowseResults->NumberReturned == 0)
			{
				sem_post(&upnpBrowserLock);
				return retval;
			}

            r_container = (MMSCP_MediaObject **)calloc(sizeof(MMSCP_MediaObject*), mostRecentBrowseResults->TotalMatches);
            r_item = (MMSCP_MediaObject **)calloc(sizeof(MMSCP_MediaObject*), mostRecentBrowseResults->TotalMatches);
	    	node = ILibLinkedList_GetNode_Head(mostRecentBrowseResults->LinkedList);
	    	if( node!=NULL )
				currObject = (struct MMSCP_MediaObject*)ILibLinkedList_GetDataFromNode(node);

	    	while(currObject!=NULL)
	    	{
				currObjectClass = MMSCP_CLASS_MASK_OBJECTTYPE & currObject->MediaClass;
				if(MMSCP_CLASS_MASK_CONTAINER == currObjectClass)
                {
                    r_container[containerSize] = currObject;
                    containerSize++;
                }
				else if(MMSCP_CLASS_MASK_ITEM == currObjectClass)
				{
		    		MMSCP_MediaResource *cp = currObject->Res;
                    currObjectMediaType = currObject->MediaClass & MMSCP_CLASS_MASK_MAJOR;
		  			if(cp!=NULL)
		  			{
                    	r_item[itemSize] = currObject;
                    	itemSize++;
                        switch(currObjectMediaType)
                        {
                            case MMSCP_CLASS_MASK_MAJOR_IMAGEITEM:
                                itemImageSize++;
                                break;
                            case MMSCP_CLASS_MASK_MAJOR_AUDIOITEM:
                                itemAudioSize++;
                                break;
                            case MMSCP_CLASS_MASK_MAJOR_VIDEOITEM:
                                itemVideoSize++;
                                break;
                            default:
                                break;
                        }
		  			}
		  			else
		  printf("***\n------[DLNA DMP] [Warnning!!!!]\tthere is a miss MediaResource ,object title is : %s ------\n***\n", currObject->Title);
				}
				else
		    printf("***\n------[DLNA DMP]:Bad Class....------\n***\n");

				currObject = NULL;
				node = ILibLinkedList_GetNextNode(node);
				if(node!=NULL)
		    		currObject = (struct MMSCP_MediaObject*)ILibLinkedList_GetDataFromNode(node);
			}

		}
		else
		{
//	    if(!mostRecentBrowseResults)
//	    {
	    printf("[DLNA DMP] file:%s, func:%s, line:%d\n", __FILE__, __func__, __LINE__);
	    printf("***\n------[DLNA DMP] Browse Responsed, but NO results!!------\n***\n");
		MMSCP_DestroyResultsList(mostRecentBrowseResults);
		mostRecentBrowseResults = NULL;
//	    }
		}
    }
	sem_post(&upnpBrowserLock);
    return retval;
}

bool RTK_DLNA_DMP::UPnPServiceBrowse(char *id, int startingIndex, int requestedCount, int *returnedCount, int *totalCount)
{
	bool retval = false;
	struct MMSCP_BrowseArgs browseArgs;
	struct UPnPService *cds = getCurrentCDService();

	if( returnedCount != NULL ) *returnedCount = 0;
	if( totalCount != NULL )    *totalCount    = 0;

	printf("[DLNA DMP] func:%s, line:%d, requestedCount:%d\n", __func__, __LINE__, requestedCount);
	if( m_bIsStopBrowsing == true) {printf("[DLNA DMP] file:%s, func:%s, line:%d\nSomething error with m_bIsStopBrowsing\n",__FILE__, __func__, __LINE__);}
	if(mostRecentBrowseResults != NULL)
	{
		MMSCP_DestroyResultsList(mostRecentBrowseResults);
		mostRecentBrowseResults = NULL;
	}

	if( cds != NULL && id != NULL)
	{
		browseArgs.BrowseFlag = MMSCP_BrowseFlag_Children;
//		browseArgs.Filter = "*"; //"res";
		browseArgs.Filter = CDS_STRING_ALL_FILTER; //"res";
//		browseArgs.ObjectID = (char *)ILibPeekStack(&currBrowseDirectoryStack);
		browseArgs.ObjectID = id;
		browseArgs.RequestedCount = requestedCount;
		browseArgs.SortCriteria = "";
		browseArgs.StartingIndex = startingIndex;
		browseArgs.UserObject = NULL;

		MMSCP_Invoke_Browse(cds, &browseArgs);

		MSCP_Release(cds->Parent);	

		m_bIsBrowsingState = true;
		if( waitForResponse() == 0)
			retval = true;
	}

	if( retval )
	{
		if( r_container )
		{
			free(r_container);
			r_container = NULL;
			containerSize = 0;
		}
		if( r_item )
		{
			free(r_item);
			r_item = NULL;
			itemSize = 0;
			itemVideoSize = -1;
			itemAudioSize = -1;
			itemImageSize = -1;
		}

		if(mostRecentBrowseResults != NULL)
		{
			void *node;
			struct MMSCP_MediaObject *currObject = NULL;
			unsigned int currObjectClass;
			//unsigned int currObjectMediaType = 0;

			if( returnedCount != NULL ) *returnedCount = mostRecentBrowseResults->NumberReturned;
			if( totalCount != NULL )    *totalCount    = mostRecentBrowseResults->TotalMatches;

			if( mostRecentBrowseResults->NumberReturned == 0 )
				return retval;

			r_container = (MMSCP_MediaObject **)calloc(sizeof(MMSCP_MediaObject*), mostRecentBrowseResults->NumberReturned);
			r_item      = (MMSCP_MediaObject **)calloc(sizeof(MMSCP_MediaObject*), mostRecentBrowseResults->NumberReturned);

			node = ILibLinkedList_GetNode_Head(mostRecentBrowseResults->LinkedList);
			if( node!=NULL )
				currObject = (struct MMSCP_MediaObject*)ILibLinkedList_GetDataFromNode(node);

			while(currObject!=NULL)
			{
				currObjectClass = MMSCP_CLASS_MASK_OBJECTTYPE & currObject->MediaClass;
				if(MMSCP_CLASS_MASK_CONTAINER == currObjectClass)
				{
					r_container[containerSize] = currObject;
					containerSize++;
				}
				else if(MMSCP_CLASS_MASK_ITEM == currObjectClass)
				{
					MMSCP_MediaResource *cp = currObject->Res;
					//currObjectMediaType = currObject->MediaClass & MMSCP_CLASS_MASK_MAJOR;
					r_item[itemSize] = currObject;
					itemSize++;
					if(cp==NULL)
						printf("***\n------[DLNA DMP] [Warnning!!!!]\tthere is a miss MediaResource ,object title is : %s ------\n***\n", currObject->Title);
				}
				else
					printf("***\n------[DLNA DMP]:Bad Class....------\n***\n");

				currObject = NULL;
				node = ILibLinkedList_GetNextNode(node);
				if(node!=NULL)
					currObject = (struct MMSCP_MediaObject*)ILibLinkedList_GetDataFromNode(node);
			}
		}
		else
		{
			printf("[DLNA DMP] file:%s, func:%s, line:%d\n", __FILE__, __func__, __LINE__);
			printf("***\n------[DLNA DMP] Browse Responsed, but NO results!!------\n***\n");
			MMSCP_DestroyResultsList(mostRecentBrowseResults);
			mostRecentBrowseResults = NULL;
		}
	}
	return retval;
}

bool RTK_DLNA_DMP::setMediaServerByUDN(char *chosenUDN) {
	// setting Media Server = choosing a valid Content Directory Service
	bool retVal = false;
	struct UPnPDevice *theMServer;

	ILibHashTree_Lock(MS_KnownDevicesTable);
	sem_wait(&currCDServiceLock);

	// main target: set currCDService correctly
	if(chosenUDN[0] != '\0')
	{
		theMServer = (struct UPnPDevice *)ILibGetEntry(MS_KnownDevicesTable, chosenUDN, (int)strlen(chosenUDN));
		if(theMServer != NULL)
		currCDService = MSCP_GetService_MediaServer_ContentDirectory(theMServer);
		if((NULL == currCDService) ||(theMServer == NULL))
			printf("[DLNA DMP]: UPNP-ERROR:  A server without a content directory service was chosen\n");
		else {
			MSCP_AddRef(theMServer);
			retVal = true;
		}
	}
	sem_post(&currCDServiceLock);
	ILibHashTree_UnLock(MS_KnownDevicesTable);
	return retVal;
}

bool RTK_DLNA_DMP::setDirectoryByTitleName(char *name)
{
    int i = 0;

    if(name)
    {
        for( i = 0; i < containerSize; i++)
        {
            if( r_container && r_container[i]->Title && strcmp(name, r_container[i]->Title)==0)
            {
                int keyLength = strlen(r_container[i]->ID);
                char *ID = (char *)malloc((keyLength+1)*sizeof(char));
                memcpy(ID, r_container[i]->ID, keyLength);
                ID[keyLength] = '\0';
                ILibPushStack(&currBrowseDirectoryStack, ID);
                return true;
            }
        }
    }
    return false;
}

bool RTK_DLNA_DMP::setDirectoryByID(char *id)
{
	if( id != NULL && strlen(id) != 0 )
	{
		int keyLength = strlen(id);
		char *ID = (char *)malloc((keyLength+1)*sizeof(char));
		memcpy(ID, id, keyLength);
		ID[keyLength] = '\0';
		ILibPushStack(&currBrowseDirectoryStack, ID);
		return true;
	}
	return false;
}

bool RTK_DLNA_DMP::setMediaServerByFriendlyName(char *name)
{
    bool retVal = false;
    struct UPnPDevice *theMServer;
    void *enumeration, *data;
    char *key, *udn = NULL;
    int keyLength;

    ILibHashTree_Lock(MS_KnownDevicesTable);
    sem_wait(&currCDServiceLock);

// reset
    if( currCDService != NULL)
    {
        //MSCP_UnSubscribeUPnPEvents(currCDService);
	MSCP_Release(currCDService->Parent);
	currCDService = NULL;
    }
    popDirectoryStackToRoot();

// find out the server's udn by friendlyName
    enumeration = ILibHashTree_GetEnumerator(MS_KnownDevicesTable);
    do
    {
	key = NULL;
	keyLength = 0;
	data = NULL;

	ILibHashTree_GetValue(enumeration, &key, &keyLength, &data);
	if( keyLength )
	{
	    if( !strcmp(((struct UPnPDevice*)data)->FriendlyName, name))
	    {
		udn = (char *)key;
	    printf("[DLNA DMP]:the select sever, udn:%s, friendlyName:%s\n", udn, ((struct UPnPDevice*)data)->FriendlyName);
		break;
	    }
	}
    }
    while(!ILibHashTree_MoveNext(enumeration));
    ILibHashTree_DestroyEnumerator(enumeration);

// set the mediaServer by the udn
    if( udn != NULL && udn[0] != '\0')
    {
	theMServer = (struct UPnPDevice*)ILibGetEntry(MS_KnownDevicesTable, udn, (int)strlen(udn));
	currCDService = MSCP_GetService_MediaServer_ContentDirectory(theMServer);
	if( NULL == currCDService )
	    printf("***\n------[DLNA DMP]:%s, %s, %d\nUPNP-ERROR:  A server without a content directory service was chosen ------\n***\n", __FILE__, __func__, __LINE__);
	else
	{
	    MSCP_AddRef(theMServer);
	    retVal = true;
            //MSCP_SubscribeForUPnPEvents(currCDService, NULL);
	}
    }
    else
	printf("***\n------[DLNA DMP]:the select server udn is NULL ------\n***\n");

    sem_post(&currCDServiceLock);
    ILibHashTree_UnLock(MS_KnownDevicesTable);

    if(!retVal)
	printf("***\n------[DLNA DMP]: Can not find the server by the name ------\n***\n");

    return retVal;
}

void RTK_DLNA_DMP::unsetMediaServer()
{
    if( currCDService != NULL)
    {
        //MSCP_UnSubscribeUPnPEvents(currCDService);
        MSCP_Release(currCDService->Parent);
        currCDService = NULL;
    }
}

#if 0
MMSCP_MediaResource *RTK_DLNA_DMP::queryObjectResourceByFilename(char *filename)
{
//    printf("[DLNA DMP] file:%s, func:%s, line:%d\n", __FILE__, __func__, __LINE__);
    int i = 0;

    if(currCDService == NULL)
    {
       printf("***\n------[DLNA DMP]: The selected media server disappear!!!------\n***\n");
       return NULL;
    }

    if(mostRecentBrowseResults== NULL)
    {
       printf("***\n------[DLNA DMP]: The most recent browse result disappear!!!------\n***\n");
       return NULL;
    }

    if( filename )
    {
        for( i = 0; i < itemSize; i++)
        {
            if(r_item && r_item[i]->Title && strcmp(filename, r_item[i]->Title)==0)
            {
                struct MMSCP_MediaResource *obj = MMSCP_SelectBestIpNetworkResource(r_item[i],RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);
                return obj;
            }
        }
    }
    return NULL;
}
#endif

//return unsigned int, success
//return negative int, failed
int RTK_DLNA_DMP::queryItemIndexByFilename(char *id)
{
	int i = 0;
	if( currCDService == NULL )
	{
       printf("***\n------[DLNA DMP]: The selected media server disappear!!!------\n***\n");
       return -1;
	}

    if(mostRecentBrowseResults== NULL)
    {
       printf("***\n------[DLNA DMP]: The most recent browse result disappear!!!------\n***\n");
       return -1;
    }

	if( id )
	{
		for( i = 0; i < itemSize; i++)
		{
			if(r_item && r_item[i]->Title && strcmp(id, r_item[i]->Title)==0)
				return i;
		}
	}
	
	return -1;
}

//return unsigned int, success
//return negative int, failed
int RTK_DLNA_DMP::queryItemIndexByID(char *id)
{
	int i = 0;
	if( currCDService == NULL )
	{
       printf("***\n------[DLNA DMP]: The selected media server disappear!!!------\n***\n");
       return -1;
	}

    if(mostRecentBrowseResults== NULL)
    {
       printf("***\n------[DLNA DMP]: The most recent browse result disappear!!!------\n***\n");
       return -1;
    }

	if( id )
	{
		for( i = 0; i < itemSize; i++)
		{
			if(r_item && r_item[i]->ID && strcmp(id, r_item[i]->ID)==0)
				return i;
		}
	}
    printf("***\n------[DLNA DMP, %d]: Can not find appropriate ID index !!!------\n***\n", __LINE__);
	
	return -1;
}

bool RTK_DLNA_DMP::MimeTypeMatch(const char *protocolInfo, const char *mimeType)
{
//protocolInfo http-get:*:mimType:*
//                       ^       ^
//                      ptr2    ptr1
    char *ptr1 = NULL, *ptr2 = NULL, *MType = NULL;
    bool retval = false;
    if(protocolInfo)
    {
        ptr1 = strchr(protocolInfo, ':');
        if(ptr1)
        {
            ptr2 = strchr(ptr1+1, ':');
            ptr1 = strrchr(protocolInfo, ':');
        }
    }
    if( ptr1 && ptr2)
    {
        int len = ptr1-ptr2;
        MType = strndup(ptr2+1, len-1);
        MType[len-1] = '\0';

        if(mimeType && strcmp(mimeType, MType)==0)
            retval = true;

        free(MType);
    }

    return retval;

}

bool RTK_DLNA_DMP::queryResourceByFile(char *filename, UPNP_DMP_RES_TYPE queryType, void *ret)
{
	int index = queryItemIndexByFilename(filename);
	struct MMSCP_MediaResource *obj = NULL, *obj2 = NULL;
	if( index >= 0 && index < itemSize )
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);

	// try to select a non-transcoding file
	// only replace the object pointer when selected obj->Size > 0
	obj2 = obj;
	while( obj2 != NULL )
	{
		if( obj2->Size > 0 )
		{
			obj = obj2;
			break;
		}
		obj2 = obj2->Next;
	}
	// try to select a non-transcoding file

	if( obj != NULL )
	{
		switch ( queryType ){
			case UPNP_DMP_RES_PROTOCOLINFO:
				*(unsigned long *)ret = (unsigned long)(obj->ProtocolInfo);
				return true;
			case UPNP_DMP_RES_URI:
				*(unsigned long *)ret = (unsigned long)(obj->Uri);
				return true;
			case UPNP_DMP_RES_SIZE:
				*(long long*)ret = (long long)(obj->Size);
				return true;
			case UPNP_DMP_RES_DURATION:
				*(unsigned long*)ret = (unsigned long)(obj->Duration);
				return true;
			case UPNP_DMP_RES_BITRATE:
				*(unsigned long*)ret = (unsigned long)(obj->Bitrate);
				return true;
			case UPNP_DMP_RES_RESOLUTION:
				*(unsigned long*)ret = (unsigned long)(obj->Resolution);
				return true;
			case UPNP_DMP_RES_CREATOR:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Creator);
				return true;
			case UPNP_DMP_RES_GENRE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Genre);
				return true;
			case UPNP_DMP_RES_ALBUM:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Album);
				return true;
			case UPNP_DMP_RES_ARTIST:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Artist);
				return true;
			case UPNP_DMP_RES_DATE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Date);
				return true;
			//case UPNP_DMP_RES_ID:
			//case UPNP_DMP_RES_PARENT_ID:
			default:
				break;
		}
	}
		
	return false;
}

//Add for Toshiba Live Stream
char** RTK_DLNA_DMP::queryResourceListByID(char *fileID, UPNP_DMP_RES_TYPE queryType, int *rlistSize)
{
	int index = queryItemIndexByID(fileID);
	struct MMSCP_MediaResource *res = NULL;
	int listSize = 0;
	int protocolInfoSize = 0;
	int uriSize = 0;
	int resolutionSize = 0;
	int bitSize = 0;
	int durationSize = 0;
	int resSize = 0;
	*rlistSize = 0;
	char **ret = NULL;
	if( index < 0 || index >= itemSize )
		return NULL;
	res = r_item[index]->Res;
	while(res != NULL) //find the resList's num
	{
		if( res->ProtocolInfo )
		{
			listSize++;
		}
		res = res->Next;
	}
	if( listSize == 0)
		return NULL;
	ret = (char **)malloc(listSize*sizeof(char **));
	memset(ret, 0, listSize*sizeof(char **));
	res = r_item[index]->Res;
	listSize = 0;
	while( res != NULL )
	{
		switch ( queryType ){
			case UPNP_DMP_RES_PROTOCOLINFO:
				protocolInfoSize = strlen(res->ProtocolInfo);
				ret[listSize] = (char *)malloc(protocolInfoSize*sizeof(char *));
				memset(ret[listSize], 0, protocolInfoSize*sizeof(char *));
				memcpy(ret[listSize], res->ProtocolInfo, protocolInfoSize);
				listSize++;
				break;
			case UPNP_DMP_RES_URI: 
				uriSize = strlen(res->Uri);
				ret[listSize] = (char *)malloc(uriSize*sizeof(char *));
				memset(ret[listSize], 0, uriSize*sizeof(char *)); 
				memcpy(ret[listSize], res->Uri, uriSize);
				listSize++;
				break;
			case UPNP_DMP_RES_RESOLUTION:
				resolutionSize = strlen(res->Resolution);
				ret[listSize] = (char *)malloc(resolutionSize*sizeof(char *));
				memset(ret[listSize], 0, resolutionSize*sizeof(char *)); 
				memcpy(ret[listSize], res->Resolution, resolutionSize);
				listSize++;
				break;
			case UPNP_DMP_RES_BITRATE:
				char bitrate[10];
				sprintf(bitrate, "%ld", res->Bitrate);
				bitSize = strlen(bitrate);
				ret[listSize] = (char *)malloc(bitSize*sizeof(char *));
				memset(ret[listSize], 0, bitSize*sizeof(char *)); 
				strcpy(ret[listSize], bitrate);
				listSize++;
				break;
			case UPNP_DMP_RES_DURATION:
				char duration[10];
				sprintf(duration, "%d", res->Duration);
				durationSize = strlen(duration);
				ret[listSize] = (char *)malloc(durationSize*sizeof(char *));
				memset(ret[listSize], 0, durationSize*sizeof(char *)); 
				strcpy(ret[listSize], duration);
				listSize++;
				break;
			case UPNP_DMP_RES_SIZE:
				char resContentLen[20];
				sprintf(resContentLen, "%lld", res->Size);
				resSize = strlen(resContentLen);
				ret[listSize] = (char *)malloc(resSize*sizeof(char *));
				memset(ret[listSize], 0, resSize*sizeof(char *)); 
				strcpy(ret[listSize], resContentLen);
				listSize++;
				break;
			default:
				break;
		}
		res = res->Next;
	}
	
	*rlistSize = listSize;
	return ret;
}

char **RTK_DLNA_DMP::querySubtitleURIListByID(char *id, char *mimeType, int *rlistSize)
{
	int index = queryItemIndexByID(id);
	struct MMSCP_MediaResource *res = NULL;
	int listSize = 0;
	*rlistSize = 0;
	char **ret = NULL;
	if( index < 0 || index >= itemSize || mimeType == NULL)
		return NULL;

        res = r_item[index]->Res; 
	// the first time, get the count
	while( res )
	{
		if( res->ProtocolInfo )
		{
			if( MimeTypeMatch(res->ProtocolInfo, mimeType) )
			{
				listSize++;
			}
		}
		res = res->Next;
	}
	if( listSize == 0)
		return NULL;
	ret = (char **)malloc(listSize*sizeof(char **));
	memset(ret, 0, sizeof(ret));
	res = r_item[index]->Res;
	listSize = 0;
	// the second time, save the address
	while( res )
	{
		if( res->ProtocolInfo )
		{
			if( MimeTypeMatch(res->ProtocolInfo, mimeType) )
			{
				ret[listSize] = res->Uri;
				listSize++;
			}
		}
		res = res->Next;
	}
	
	*rlistSize = listSize;
	return ret;
}

bool RTK_DLNA_DMP::queryResourceByID(char *id, UPNP_DMP_RES_TYPE queryType, void *ret)
{
	int index = queryItemIndexByID(id);
	struct MMSCP_MediaResource *obj = NULL, *obj2 = NULL; 
	if( index >= 0 && index < itemSize)
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);

	// try to select a non-transcoding file
	// only replace the object pointer when selected obj->Size > 0
	obj2 = obj;
	while( obj2 != NULL )
	{
		if( obj2->Size > 0 )
		{
			obj = obj2;
			break;
		}
		obj2 = obj2->Next;
	}
	// try to select a non-transcoding file

	if( obj != NULL )
	{
		switch ( queryType ){
			case UPNP_DMP_RES_PROTOCOLINFO:
				*(unsigned long *)ret = (unsigned long)(obj->ProtocolInfo);
				return true;
			case UPNP_DMP_RES_URI:
				*(unsigned long *)ret = (unsigned long)(obj->Uri);
				return true;
			case UPNP_DMP_RES_SIZE:
				*(long long*)ret = (long long)(obj->Size);
				return true;
			case UPNP_DMP_RES_DURATION:
				*(unsigned long*)ret = (unsigned long)(obj->Duration);
				return true;
			case UPNP_DMP_RES_BITRATE:
				*(unsigned long*)ret = (unsigned long)(obj->Bitrate);
				return true;
			case UPNP_DMP_RES_RESOLUTION:
				*(unsigned long*)ret = (unsigned long)(obj->Resolution);
				return true;
			case UPNP_DMP_RES_TITLE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Title);
				return true;
			case UPNP_DMP_RES_PARENT_ID:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->ParentID);
				return true;
			case UPNP_DMP_RES_CREATOR:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Creator);
				return true;
			case UPNP_DMP_RES_GENRE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Genre);
				return true;
			case UPNP_DMP_RES_ALBUM:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Album);
				return true;
			case UPNP_DMP_RES_ARTIST:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Artist);
				return true;
			case UPNP_DMP_RES_DATE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Date);
				return true;
			default:
				break;
		}
	}
    printf("***\n------[DLNA DMP, %d]: NO appropriate obj !!!------\n***\n", __LINE__);
	return false;
}

bool RTK_DLNA_DMP::queryResourceByIndex(int index, UPNP_DMP_RES_TYPE queryType, void *ret)
{
	struct MMSCP_MediaResource *obj = NULL, *obj2 = NULL; 
	if( index >= 0 && index < itemSize)
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);

	// try to select a non-transcoding file
	// only replace the object pointer when selected obj->Size > 0
	obj2 = obj;
	while( obj2 != NULL )
	{
		if( obj2->Size > 0 )
		{
			obj = obj2;
			break;
		}
		obj2 = obj2->Next;
	}
	// try to select a non-transcoding file

	if( obj != NULL )
	{
		switch ( queryType ){
			case UPNP_DMP_RES_PROTOCOLINFO:
				*(unsigned long *)ret = (unsigned long)(obj->ProtocolInfo);
				return true;
			case UPNP_DMP_RES_URI:
				*(unsigned long *)ret = (unsigned long)(obj->Uri);
				return true;
			case UPNP_DMP_RES_SIZE:
				*(long long*)ret = (long long)(obj->Size);
				return true;
			case UPNP_DMP_RES_DURATION:
				*(unsigned long*)ret = (unsigned long)(obj->Duration);
				return true;
			case UPNP_DMP_RES_BITRATE:
				*(unsigned long*)ret = (unsigned long)(obj->Bitrate);
				return true;
			case UPNP_DMP_RES_RESOLUTION:
				*(unsigned long*)ret = (unsigned long)(obj->Resolution);
				return true;
			case UPNP_DMP_RES_TITLE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Title);
				return true;
			case UPNP_DMP_RES_ID:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->ID);
				return true;
			case UPNP_DMP_RES_PARENT_ID:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->ParentID);
				return true;
			case UPNP_DMP_RES_CREATOR:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Creator);
				return true;
			case UPNP_DMP_RES_GENRE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Genre);
				return true;
			case UPNP_DMP_RES_ALBUM:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Album);
				return true;
			case UPNP_DMP_RES_ARTIST:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Artist);
				return true;
			case UPNP_DMP_RES_DATE:
				*(unsigned long*)ret = (unsigned long)(r_item[index]->Date);
				return true;
			default:
				break;
		}
	}
	return false;
}

bool RTK_DLNA_DMP::queryContainerObjInfoByIndex(int index, struct UPnPObjInfo *pInfo)
{
	if( index >= 0 && index < containerSize && pInfo != NULL)
	{
		pInfo->pUniqueCharID = r_container[index]->ID;
		pInfo->pUniqueCharParentID = r_container[index]->ParentID;
		pInfo->pProtocolInfo = NULL;
		pInfo->pTitleName = r_container[index]->Title;
		pInfo->pUri = NULL;
		return true;
	}

	return false;
}

bool RTK_DLNA_DMP::queryItemObjInfoByIndex(int index, struct UPnPObjInfo *pInfo)
{
	struct MMSCP_MediaResource *obj = NULL, *obj2 = NULL; 
	if( index >= 0 && index < itemSize)
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);

	// try to select a non-transcoding file
	// only replace the object pointer when selected obj->Size > 0
	obj2 = obj;
	while( obj2 != NULL )
	{
		if( obj2->Size > 0 )
		{
			obj = obj2;
			break;
		}
		obj2 = obj2->Next;
	}
	// try to select a non-transcoding file

	if( obj != NULL && pInfo != NULL)
	{
		pInfo->pUniqueCharID = r_item[index]->ID;
		pInfo->pUniqueCharParentID = r_item[index]->ParentID;
		pInfo->pProtocolInfo = obj->ProtocolInfo;
		pInfo->pTitleName = r_item[index]->Title;
		pInfo->pUri = obj->Uri;
		return true;
	}

	return false;
}

char *RTK_DLNA_DMP::queryFileURI(char *filename)
{
    //    struct MMSCP_MediaResource *obj = queryObjectResourceByFilename(filename);
	int index = queryItemIndexByFilename(filename);
	struct MMSCP_MediaResource *obj = NULL, *obj2 = NULL;
	if( index >= 0 && index < itemSize )
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);

	// try to select a non-transcoding file
	// only replace the object pointer when selected obj->Size > 0
	obj2 = obj;
	while( obj2 != NULL )
	{
		if( obj2->Size > 0 )
		{
			obj = obj2;
			break;
		}
		obj2 = obj2->Next;
	}
	// try to select a non-transcoding file

	if( obj != NULL)
		return (obj->Uri);
	return NULL;
}

#if 0
long RTK_DLNA_DMP::queryFileSize(char *filename)
{
    //    struct MMSCP_MediaResource *obj = queryObjectResourceByFilename(filename);
	int index = queryItemIndexByFilename(filename);
	struct MMSCP_MediaResource *obj = NULL;
	if( index >= 0 && index < itemSize )
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);
	if( obj != NULL)
		return (obj->Size);
	return -1;
}

int RTK_DLNA_DMP::queryFileDuration(char *filename)
{
    //    struct MMSCP_MediaResource *obj = queryObjectResourceByFilename(filename);
	int index = queryItemIndexByFilename(filename);
	struct MMSCP_MediaResource *obj = NULL;
	if( index >= 0 && index < itemSize )
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);
	if( obj != NULL)
		return (obj->Duration);
	return 0;
}

long RTK_DLNA_DMP::queryFileBitrate(char *filename)
{
    //    struct MMSCP_MediaResource *obj = queryObjectResourceByFilename(filename);
	int index = queryItemIndexByFilename(filename);
	struct MMSCP_MediaResource *obj = NULL;
	if( index >= 0 && index < itemSize )
		obj = MMSCP_SelectBestIpNetworkResource(r_item[index], RENDERER_SINKPROTOCOLINFO, ((struct MSCP_CP*)MS_ControlPoint)->AddressList, ((struct MSCP_CP*)MS_ControlPoint)->AddressListLength);
	if( obj != NULL)
		return (obj->Bitrate);
	return 0;
}
#endif

//TODO, modified later
unsigned int RTK_DLNA_DMP::queryFileMediaType(char *filename, char **ProtocolInfo)
{
    int i = 0;

    *ProtocolInfo = NULL;
    if(currCDService == NULL)
    {
//       printf("***\n------[DLNA DMP]: The selected media server disappear!!!------\n***\n");
       return 0;
    }

    if(mostRecentBrowseResults== NULL)
    {
       printf("***\n------[DLNA DMP]: The most recent browse result disappear!!!------\n***\n");
       return 0;
    }

    if( filename )
    {
        for( i = 0; i < itemSize; i++)
        {
            if(r_item && r_item[i]->Title && strcmp(filename, r_item[i]->Title)==0)
            {
                *ProtocolInfo = r_item[i]->Res->ProtocolInfo;
                return (r_item[i]->MediaClass & MMSCP_CLASS_MASK_MAJOR);
            }
        }
//        printf("***\n------[DLNA DMP]: The missing filename is %s!!!------\n***\n", filename);
    }
//    printf("***\n------[DLNA DMP]: No match filename!!!------\n***\n");

    return 0;
}
unsigned int RTK_DLNA_DMP::queryFileMediaTypeByID(char *pFileID, char **ProtocolInfo)
{
	int index = queryItemIndexByID(pFileID);

	*ProtocolInfo = NULL;
	if(currCDService == NULL)
		return 0;

	if(mostRecentBrowseResults== NULL)
		return 0;

	if(index>=0 && index<itemSize)
	{
		*ProtocolInfo = r_item[index]->Res->ProtocolInfo;
		return (r_item[index]->MediaClass & MMSCP_CLASS_MASK_MAJOR);
	}
	return 0;
}
#if 0 //TODO, should be remove
char *RTK_DLNA_DMP::queryPresentContentTitle()
{
    if(strcmp("0", (char *)ILibPeekStack(&currBrowseDirectoryStack)))
	 return ((char *)ILibPeekStack(&currBrowseDirectoryStack)); 
    else
         return NULL;
}
#endif


bool RTK_DLNA_DMP::queryMediaResource(struct MMSCP_BrowseArgs *criteria, 
				struct MMSCP_MediaObject **resultArray, 
				int *resultLength) {

	bool success = false;
	struct UPnPService *cds = getCurrentCDService();
	if(cds == NULL)	// if there is no valid Content Directory Service
		return false;

	// invoke async. browse request;
	MMSCP_Invoke_Browse(cds, criteria);

	// release reference (which is increased by getCurrentCDS()
	MSCP_Release(cds->Parent);

	if(waitForResponse() == 0) {
	    success = true;
		// put result into arrays
		*resultLength = parseBrowseResult(resultArray);
	}

	return success;
}

int RTK_DLNA_DMP::parseBrowseResult(struct MMSCP_MediaObject **resultArray) {
	int i = 0;
	int retVal = 0;

	if(mostRecentBrowseResults != NULL) {
		void *node = ILibLinkedList_GetNode_Head(mostRecentBrowseResults->LinkedList);
		struct MMSCP_MediaObject *currObject = NULL;
		if (node != NULL)
		{
			currObject = (struct MMSCP_MediaObject*) ILibLinkedList_GetDataFromNode(node);
		}
		if((retVal = ILibLinkedList_GetCount(mostRecentBrowseResults->LinkedList)) == 0)
			return retVal;

		// allocate memory for resultArray
		*resultArray = (struct MMSCP_MediaObject *)malloc(sizeof(struct MMSCP_MediaObject)*retVal);

		// start parsing result
		while(currObject != NULL) {
			// copy result to array 
			memcpy((&(*resultArray)[i++]), currObject, sizeof(struct MMSCP_MediaObject));

			node = ILibLinkedList_GetNextNode(node);
			currObject = NULL;
			if (node != NULL)
				currObject = (struct MMSCP_MediaObject*) ILibLinkedList_GetDataFromNode(node);
		}
	}
	return retVal;
}

bool RTK_DLNA_DMP::cdupDirectoryStack(int level)
{
    int i;
    for( i = 0; i < level; i++)
	if(strcmp("0", (char *)ILibPeekStack(&currBrowseDirectoryStack)))
	    free(ILibPopStack(&currBrowseDirectoryStack));
        else
            return false;

    return true;
}

int RTK_DLNA_DMP::getDirectoryLevel()
{
    void *tempStack;
    int count = 0;
    ILibCreateStack(&tempStack);
    while(ILibPeekStack(&currBrowseDirectoryStack))
        ILibPushStack(&tempStack, ILibPopStack(&currBrowseDirectoryStack));

    while(ILibPeekStack(&tempStack))
    {
        ILibPushStack(&currBrowseDirectoryStack, ILibPopStack(&tempStack));
        count++;
    }
    ILibClearStack(&tempStack);
    return count;
}

void RTK_DLNA_DMP::freeMediaServerResultArray(struct MediaServerEntry **resultArray, int n) {
	if(n<=0) 
		return;
	for(int i=0 ; i<n ; i++) {
		free((*resultArray)[i].udn);
		free((*resultArray)[i].friendlyName);
	}
	free(*resultArray);
}

void RTK_DLNA_DMP::freeMediaObjectResultArray(struct MMSCP_MediaObject **resultArray, int n) {
	free(*resultArray);
}


// static function (registered as MMSCP callback function pointer)
void RTK_DLNA_DMP::MS_BrowseResponded(void *serviceObj, struct MMSCP_BrowseArgs *args, int errorCode, struct MMSCP_ResultsList *results) {

	printf("[DLNA DMP] file:%s, func:%s, line:%d\n", __FILE__, __func__, __LINE__);

	// the resulte is out of date
	if( sem_trywait(&callbackRequestSemaphore) != 0) 
		MMSCP_DestroyResultsList(results);
	else
	{
		sem_wait(&currCDServiceLock);
		if((currCDService == serviceObj) ){
			if(0 == errorCode) {
				if(mostRecentBrowseResults != NULL) {
					// prevent memory leaks leave the old results, and
					// destroy the new results because there might be 
					// other references to the old stuff.
					MMSCP_DestroyResultsList(results);
				}
				else {
						mostRecentBrowseResults = results;
						printf("[DLNA DMP] MS_BrowseResponded success....\n"); 
				}
			}
			else
			{
				MMSCP_DestroyResultsList(results);
				printf("***\n------[DLNA DMP] MS_BrowseResponsed error code:%x ------\n***\n", errorCode);
			}
		}
		else /* the response was for a different cds service... */
		{
			MMSCP_DestroyResultsList(results);
			printf("***\n------[DLNA DMP] ERROR:  A browse response came in for a media server other ------\n");
			printf("***\n------[DLNA DMP] than the currently selected one.                           ------\n***\n");
		}
		sem_post(&callbackResponseSemaphore);
		sem_post(&currCDServiceLock);
	}
}

// static function (registered as MMSCP callback function pointer)
void RTK_DLNA_DMP::MS_ServerAddedOrRemoved(struct UPnPDevice *device, int added) {
	if(added)
	{
		ILibHashTree_Lock(MS_KnownDevicesTable);

		MSCP_AddRef(device);
		ILibAddEntry(MS_KnownDevicesTable,
			device->UDN,
			(int)strlen(device->UDN),
			device);

		ILibHashTree_UnLock(MS_KnownDevicesTable);
		if(deviceAddedOrRemovedTrigger != NULL)
			deviceAddedOrRemovedTrigger(deviceTriggerUpdateId, "added", device->FriendlyName);
	}
	else /* removed */
	{
		ILibHashTree_Lock(MS_KnownDevicesTable);

		ILibDeleteEntry(MS_KnownDevicesTable,
			device->UDN,
			(int)strlen(device->UDN));

		ILibHashTree_UnLock(MS_KnownDevicesTable);

		if(deviceAddedOrRemovedTrigger != NULL)
			deviceAddedOrRemovedTrigger(deviceTriggerUpdateId, "removed", device->FriendlyName);
		/* if it's the currently selected one, we have to get rid of it */
		sem_wait(&currCDServiceLock);
		
		if(currCDService == MSCP_GetService_MediaServer_ContentDirectory(device))
		{
                        //MSCP_UnSubscribeUPnPEvents(currCDService);
			MSCP_Release(device);
			currCDService = NULL;
		}
	
		sem_post(&currCDServiceLock);	
	
		MSCP_Release(device);
	}
	if(deviceTriggerUpdate != NULL)
		deviceTriggerUpdate(deviceTriggerUpdateId, deviceTriggerUpdateData);
}

void RTK_DLNA_DMP::popDirectoryStackToRoot(void) {
	void *temp;
	while(strcmp("0", (char *)ILibPeekStack(&currBrowseDirectoryStack)))
	{
		temp = ILibPopStack(&currBrowseDirectoryStack);
		free(temp);
	}
}

struct UPnPService * RTK_DLNA_DMP::getCurrentCDService() {
	struct UPnPService *ret;
	sem_wait(&currCDServiceLock);
	if(NULL == currCDService)
	{
		printf("***\n------[DLNA DMP] No server selected, select one with setms ------\n***\n");
		ret = NULL;
	}
	else
	{
		ret = currCDService;
		MSCP_AddRef(currCDService->Parent);
	}
	sem_post(&currCDServiceLock);
	return ret;
}

int RTK_DLNA_DMP::waitForResponse() {
	/* this will break if the response comes back after the timeout goes off */
    time_t t1, t2;

	/* TODO: Add handler that will release the semaphore if device does not respond. */
	t2 = t1 = time(NULL);	
	sem_post(&callbackRequestSemaphore);
	do{	
	  if( sem_trywait(&callbackResponseSemaphore)==0 )
	  {
	      //printf("***\n------[DLNA DMP] this browse cost %d sec(s) ------\n***\n", time(NULL)-t1);
		  m_bIsBrowsingState = false;
          if( m_bIsStopBrowsing )
		  {
		  	m_bIsStopBrowsing = false;
			return 1;
          }
	      return 0;
	  }
	  t2 = time(NULL);
	  usleep(10000);
	}while(((t2-t1)<15)); //&& (!m_bIsStopBrowsing));
	
	m_bIsBrowsingState = false;
	if( m_bIsStopBrowsing ) m_bIsStopBrowsing = false;
	//else:time out
	  
	sem_wait(&callbackRequestSemaphore);
	return 1;      
}

void RTK_DLNA_DMP::stopWatingResponse()
{
	if( m_bIsBrowsingState )
		m_bIsStopBrowsing = true;
}

void RTK_DLNA_DMP::setMediaServersUpdate( bool (*updateFuncPtr)(int, void*), void *pParam )
{
	deviceTriggerUpdate= updateFuncPtr;
	deviceTriggerUpdateData = pParam;
}

void RTK_DLNA_DMP::setMediaServersTrigger( bool (*updateFuncPtr)(int, char*, char*), void *pParam )
{
	deviceAddedOrRemovedTrigger = updateFuncPtr;
	deviceTriggerUpdateData = pParam;
}
