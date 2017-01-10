#ifndef __VENUS_DLNA_DMP_HEADER_FILE__
#define __VENUS_DLNA_DMP_HEADER_FILE__

#if 0 //AP_CONFIG_WITH(APCFG_APP_UPNP)
#include "ILibParsers.h"
#include "MSCP_ControlPoint.h"
#include "ILibWebServer.h"
#include "ILibAsyncSocket.h"
#include "ILibSSDPClient.h"
#include "Utility.h"
#include "MyString.h"
#include "RTK_MmsCp.h"
#include "RTKMR_MicroStack.h"
#include <OSAL.h>
#include <semaphore.h>
#include <pthread.h>
#else
//#include <OSAL.h>
#include <semaphore.h>
#include <pthread.h>
#include "ILibParsers.h"
#include "ILibAsyncSocket.h"
#include "ILibWebServer.h"
#include "ILibSSDPClient.h"
#include "RTK_MmsCp.h"
#include "DMR_MicroStack.h"
#include "MyString.h"   //TODO, double check?
#endif


struct MediaServerEntry {
	char *udn;
	char *friendlyName;
};

struct UPnPObjInfo {
    char *pUniqueCharID;
    char *pUniqueCharParentID;
    char *pTitleName;
    char *pProtocolInfo;
    char *pUri;
};

typedef enum {
	UPNP_DMP_RES_ID,              //char *
	UPNP_DMP_RES_PARENT_ID,       //char *
	UPNP_DMP_RES_PROTOCOLINFO,    //char *
	UPNP_DMP_RES_TITLE,           //char *
	UPNP_DMP_RES_URI,             //char *
	UPNP_DMP_RES_SIZE,            //long
	UPNP_DMP_RES_DURATION,        //int
	UPNP_DMP_RES_BITRATE,         //long 
	UPNP_DMP_RES_RESOLUTION,      //char *
	UPNP_DMP_RES_CREATOR,         //char *
	UPNP_DMP_RES_GENRE,           //char *
	UPNP_DMP_RES_ALBUM,           //char *
	UPNP_DMP_RES_ARTIST,          //char *
	UPNP_DMP_RES_DATE,            //char *
} UPNP_DMP_RES_TYPE;

class RTK_DLNA_DMP {
public:
		RTK_DLNA_DMP();										// reviewed
		~RTK_DLNA_DMP();									// reviewed

		void Start();										// daemon-mode Start
		void Terminate();									// reviewed

		int queryMediaServers(struct MediaServerEntry **); 	// reviewed

		bool setMediaServerByUDN(char *chosenUDN);				// reviewed

		bool queryMediaResource(struct MMSCP_BrowseArgs *criteria, 
						struct MMSCP_MediaObject **resultArray,
						int *resultLength);					// reviewed

		void freeMediaServerResultArray(struct MediaServerEntry **, int n);

		void freeMediaObjectResultArray(struct MMSCP_MediaObject **resultArray, int n);

//yuyu		void Run();
		static void *Run(void *arg);											// reviewed
		static void ReInit(void);

//yuyu
public:
	char *MediaServerName(int index, char *name = NULL);
	// Add for Toshiba Live Stream
	char *MediaServerLocationURL(int index, char *LocationURL = NULL);
	char *MediaServerManufacturer(int index, char *manufacturer = NULL);
	char *MediaServerRegzaApps(int index, char *regzaApps = NULL);
	char *MediaServerUDN(int index, char *chosenUDN = NULL);
	char *MediaServerModelDescription(int index, char *name = NULL);
	char *UPnPServiceContentDirectory(int index, char *name = NULL);
	// char *name malloc here, and upper ap free.
	void UPnPServiceContentDirectory(char *id, char *name);
	char *UPnPServiceContentFile(int index, char *name = NULL);
	// char *name malloc here, and upper ap free.
	void UPnPServiceContentFile(char *id, char *name);
	int MediaServerSizes();
	int MediaContainerObjectSizes();
	int MediaItemObjectSizes();
        int MediaItemVideoSizes();
        int MediaItemAudioSizes();
        int MediaItemImageSizes();
	//TODO, remove it?
	char *queryFileURI(char *filename);
	//long queryFileSize(char *filename);
	//int queryFileDuration(char *filename);
	//long queryFileBitrate(char *filename);
	// Just prepare first
	bool queryResourceByFile(char *filename, UPNP_DMP_RES_TYPE queryType, void *ret);
	// Add for Toshiba Live Stream
	char** queryResourceListByID(char *fileID, UPNP_DMP_RES_TYPE queryType, int *rlistSize);
		//char *queryIDURI(char *id);
		//long queryIDSize(char *id);
		//int queryIDDruation(char *id);
		//long queryIDBitrate(char *id);
		//char *queryIDParentID(char *id);
	bool queryResourceByID(char *id, UPNP_DMP_RES_TYPE queryType, void *ret);
	char **querySubtitleURIListByID(char *id, char *mimeType, int *rlistSize);
	bool queryResourceByIndex(int index, UPNP_DMP_RES_TYPE queryType, void *ret);
	bool queryContainerObjInfoByIndex(int index, struct UPnPObjInfo *pInfo);
	bool queryItemObjInfoByIndex(int index, struct UPnPObjInfo *pInfo);
	//bool queryResourceByIndex(char *id, UPNP_DMP_RES_TYPE queryType, void *ret);
	// Just prepare first
	////TODO, modified later
	unsigned int queryFileMediaType(char *filename, char **ProtocolInfo);
	unsigned int queryFileMediaTypeByID(char *pFileID, char **ProtocolInfo);
	//unsigned int queryIDFileMediaType(char *id, char **ProtocolInfo);
	//TODO, remove this, queryPresentContentTitle,queryContentTitleName
	//char *queryPresentContentTitle();
	//char *queryContentTitleName(char *id, int *len);
	void MediaServerSearchScan(void);
	void MediaServerDelete(char *chosenUDN);
	// Browse id picked up from Stack, this is for GBrowserAP only
	bool UPnPServiceBrowse(void);
	// Browse by input id, for others except GBrowserAP
	bool UPnPServiceBrowse(char *id);
	// For partial browsing
	bool UPnPServiceBrowse(char *id, int startingIndex, int requestedCount, int *returnedCount, int *totalCount);
	bool setMediaServerByFriendlyName(char *name);
	void unsetMediaServer();
	bool setDirectoryByTitleName(char *name);
	bool setDirectoryByID(char *id);
	bool cdupDirectoryStack(int level);
	int getDirectoryLevel();
	void stopWatingResponse();

	//callback function(s)
	void setMediaServersUpdate( bool (*updateFuncPtr)(int, void*), void *pParam );//{deviceTriggerUpdate= updateFuncPtr;}
	void setMediaServersTrigger( bool (*updateFuncPtr)(int, char*, char*), void *pParam );//{deviceAddedOrRemovedTrigger= updateFuncPtr;}
private:
	char *findIDFromTable(void *theILibHashTable, char* name);
	//MMSCP_MediaResource *queryObjectResourceByFilename(char *filename);
	int queryItemIndexByFilename(char *id);
	int queryItemIndexByID(char *id);
	bool MimeTypeMatch(const char *protocolInfo, const char *mimeType);

        static MMSCP_MediaObject** r_container;
		static int containerSize;
        static MMSCP_MediaObject** r_item;
		static int itemSize;
		static int itemVideoSize;
		static int itemAudioSize;
		static int itemImageSize;

#if 0 //AP_CONFIG_WITH(APCFG_APP_UPNP)
	static RTKMR_MicroStackToken RTKMP_microStack;
#else
	// The stack is for GBrowserAP only
	static DMR_MicroStackToken RTKMP_microStack;
#endif
	static void *DMP_Monitor;
	// 1. check if the IP Address list changed
	// 2. check if the network connected

private:

		// must-have functions for registering as callback
		static void MS_BrowseResponded(void *serviceObj,
						struct MMSCP_BrowseArgs *args, 
						int errorCode, 
						struct MMSCP_ResultsList *results);

		static void MS_ServerAddedOrRemoved(struct UPnPDevice *device,
						int added);

		// convert "result Linklist" into "result Array"
		int parseBrowseResult(struct MMSCP_MediaObject **resultArray);	// reviewed

		// default helper function copied from CommandLineShell.c
		static void 				popDirectoryStackToRoot(void);
		struct UPnPService * 	getCurrentCDService();
		int 					waitForResponse();

		// monitor the network enviornment status
		static void DMP_Network_Monitor(void *);
		// 1. check if the IP Address list changed
		// 2. check if the network connected

private:
		static int m_state;	// -1:stop, 0:init, 1:run
		static int m_bIsBrowsingState;
		static int m_bIsStopBrowsing;

		// internal variables
	
		// variables supporting MMSCP
		static sem_t callbackResponseSemaphore;
		static sem_t callbackRequestSemaphore;
		static sem_t currCDServiceLock;
		static sem_t upnpBrowserLock;
		static void *currBrowseDirectoryStack;
		
		static void *MS_KnownDevicesTable;	// hashtree storing (key, value) = (UDN, UPNPDevice *)

		static void *MS_ControlPoint;

		static void *DMP_Microstack_Chain;
//yuyu		CThread *DMPthread;
		static pthread_t DMPthread;

		// pointer to current "Content Directory Service"
		static struct UPnPService *currCDService;
		static struct MMSCP_ResultsList *mostRecentBrowseResults;
	static bool (*deviceTriggerUpdate)(int, void *);
	static bool (*deviceAddedOrRemovedTrigger)(int, char *, char *);
		//                                (int eventId, void *pData);
		static const int deviceTriggerUpdateId = 0;
		static void *deviceTriggerUpdateData;

};

class RTK_DLNA_DMP_Singleton
{
friend class AutoDMPPtr;

private:
	class AutoDMPPtr
	{
		public:
			RTK_DLNA_DMP *m_ptr;
			AutoDMPPtr(){m_ptr = NULL;}
			~AutoDMPPtr(){if(m_ptr) delete m_ptr; m_ptr = NULL;}
	};
	static AutoDMPPtr p;

public:
	static RTK_DLNA_DMP *GetInstance()
	{
		if(p.m_ptr == NULL)
		{
			p.m_ptr = new RTK_DLNA_DMP();
		}
		return p.m_ptr;
	}

	static void DeleteInstance()
	{
		if(p.m_ptr)
			delete p.m_ptr;
		p.m_ptr = NULL;
	}
}; 

#endif
