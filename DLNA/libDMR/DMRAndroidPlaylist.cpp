#include "DLNA_DMP.h"
#include "DLNA_DMR.h"
#include "DMRAndroidPlaylist.h"
#include "DMRAndroidRenderer.h"
#include "DLNALog.h"
#include "jni.h"

//TODO the value should be double checked
#define DMR_HTTP_ERROR_NONE    0
#define DMR_HTTP_ERROR_GENERIC -10
#define DMR_HTTP_ERROR_EOF     -20

#define DMR_HTTP_MAX_RETRY 5
static int bPlayContainer = false;
#ifdef DLNADMRCTT
extern char g_playcontaiercid[128];
#endif
extern bool DMR_PlayCompleted;
extern jobject gDMRServiceObj; 
extern JavaVM *gVM;
static jclass gDMRclass;
static jmethodID gRenderer_loadMedia;
static jmethodID gRenderer_getfilelen;
static char* tempbuffer;
static int m_contentlength;

playlistRenderer::playlistRenderer()
{
	JNIEnv *env;
	AndroidRenderer::DMRAttachCurrentThread((void**)&env);
	gDMRclass = env->GetObjectClass(gDMRServiceObj);
	gRenderer_loadMedia = env->GetMethodID(gDMRclass, "Download",  "(Ljava/lang/String;)Ljava/lang/String;");
	gRenderer_getfilelen = env->GetMethodID(gDMRclass,"getfilelen","(Ljava/lang/String;)I");
	AndroidRenderer::DMRDetachCurrentThread();
}

playlistRenderer::~playlistRenderer()
{
}

int playlistRenderer::loadMedia(char *filename)
{
	JNIEnv *env;
	AndroidRenderer::DMRAttachCurrentThread((void**)&env);
	jstring Filename =env->NewStringUTF(filename);
	jint tmplen = env->CallIntMethod(gDMRServiceObj,gRenderer_getfilelen,Filename);
	jstring tmp = (jstring)env->CallObjectMethod(gDMRServiceObj, gRenderer_loadMedia,Filename);
	m_contentlength = (int)tmplen;
	tempbuffer =  (char *)calloc(m_contentlength+1, sizeof(char));
	tempbuffer = (char*)env->GetStringUTFChars(tmp,NULL);
	env->DeleteLocalRef(Filename);
	env->DeleteLocalRef(tmp);
	AndroidRenderer::DMRDetachCurrentThread();
	return S_OK;
}

int playlistRenderer::preParse(char *filename, unsigned int *NumberOfTracks, char ***MediaTrackURI, SUBRENDERER_TYPE **MediaType, long *TotalTime,char ***ProtocalInfo)
{
	ALOGE("playlistRenderer::preParse begin!!");
	int bSuccess = E_FAIL;
	bPlayContainer = false;
	#ifdef DLNADMRCTT
	if(filename && !strncmp(filename,"dlna-playcontainer://",strlen("dlna-playcontainer://")))
	{
		ALOGE("playlistRenderer::preParse begin 11");
		bPlayContainer = true;
		printf("dlna-playcontainer preParse\n");
		char uuid[64];
		char cid[128];
		char fii[128];
		char *cur = NULL;
		char *next = NULL;
		int startindex = 0;
		unsigned long ret;
        if( (cur = strstr( filename, "uuid:"))!= NULL )
        {
        	memset(uuid,0,64);
            cur += 5;
            strncpy(uuid,cur,36);
        }	
		else if( (cur = strstr( filename, "uuid%3A"))!= NULL )
        {
        	memset(uuid,0,64);
            cur += 7;
            strncpy(uuid,cur,36);
        }
		else if( (cur = strstr( filename, "uuid%3a"))!= NULL )
        {
        	memset(uuid,0,64);
            cur += 7;
            strncpy(uuid,cur,36);
        }
		
		cur = NULL;
		next = NULL;
		if( (cur = strstr( filename, "cid="))!= NULL )
        {
        	memset(cid,0,128);
            cur += 4;
			if( (next = strchr( cur, '&'))!= NULL )
        	{
            	strncpy(cid,cur,next-cur);
				strcpy(g_playcontaiercid,cid);
        	}	
			else if( (next = strstr( cur, "%26"))!= NULL )
        	{
            	strncpy(cid,cur,next-cur);
				strcpy(g_playcontaiercid,cid);
        	}	
		}
		
		cur = NULL;
		next = NULL;
		if( (cur = strstr( filename, "fii="))!= NULL )
        {
        	memset(fii,0,128);
            cur += 4;
			if( (next = strchr( cur, '&'))!= NULL )
        	{
            	strncpy(fii,cur,next-cur);
        	}	
			else
				strcpy(fii,cur);

			startindex = atoi(fii);
		}
		ALOGE("renderer  uuid=%s cid=%s startindex=%d\n",uuid,cid,startindex);
		printf("uuid=%s cid=%s startindex=%d\n",uuid,cid,startindex);
		RTK_DLNA_DMP_Singleton::DeleteInstance();
		RTK_DLNA_DMP *m_pUpnpDmp = RTK_DLNA_DMP_Singleton::GetInstance();
		if(m_pUpnpDmp)m_pUpnpDmp->unsetMediaServer();
		if(m_pUpnpDmp)m_pUpnpDmp->Start();
		sleep(5);
		
		if(m_pUpnpDmp && !m_pUpnpDmp->setMediaServerByUDN(uuid))
		{
			ALOGE("renderer DMP setMediaServerByUDN Error\n");
			printf("DMP setMediaServerByUDN Error\n");
			if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
			sleep(10);
			return bSuccess;
		}
			
		if(!m_pUpnpDmp->UPnPServiceBrowse(cid))
		{
			printf("DMP UPnPServiceBrowse Error\n");
			ALOGE("renderer DMP UPnPServiceBrowse Error\n");
			if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
			return bSuccess;
		}	
			
		
		//get all item
		int totalcount=m_pUpnpDmp->MediaItemObjectSizes();
		printf("DMP item totalcount=%d\n",totalcount);
		ALOGE("renderer   DMP item totalcount=%d\n",totalcount);
		if(startindex < totalcount){
			*NumberOfTracks =	totalcount - startindex;
			*TotalTime = 0;
			*MediaTrackURI = new char*[*NumberOfTracks];
			*ProtocalInfo  = new char*[*NumberOfTracks];
			*MediaType = new SUBRENDERER_TYPE[*NumberOfTracks];
			
			for(int i=startindex;i<totalcount;i++){
				(*MediaType)[i-startindex] = DMR_SUBR_UNKNOWN;
				if(m_pUpnpDmp->queryResourceByIndex(i, UPNP_DMP_RES_URI, &ret))
				{
					(*MediaTrackURI)[i-startindex] = strdup((char*)ret);
				}
				if(m_pUpnpDmp->queryResourceByIndex(i, UPNP_DMP_RES_PROTOCOLINFO, &ret))
				{
					(*ProtocalInfo)[i-startindex] = strdup((char*)ret);
				}
			}
			if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
			sleep(10);
			ALOGE("playlistRenderer::preParse begin 12!!");
			return S_OK;
		}
		else
			if(m_pUpnpDmp)m_pUpnpDmp->Terminate();
			ALOGE("playlistRenderer::preParse begin 13!!");
			return bSuccess;
	}
	#endif	
	//initialze the values;
	*NumberOfTracks = 0;
	*TotalTime = 0;

	if(filename!=NULL)
		bSuccess = loadMedia(filename);

	if( bSuccess == S_OK )
	{
		//TODO, if content length missing ?
		//if content length missing, then return E_FAIL !!!
		int index = 0;
		char *cur = tempbuffer;
		int lineLength = 0;
		tempbuffer[m_contentlength] = '\0';
		ALOGE("\t===> [DMR][Bugtracking] %s %d ==> read ret %s\n", __func__, __LINE__, tempbuffer);
	
		*NumberOfTracks = getListLength(tempbuffer);
		*TotalTime = 0;
		if( *NumberOfTracks == 0 )
		{
			return E_FAIL;
		}
		else
		{
			*MediaTrackURI = new char*[*NumberOfTracks];
			*MediaType = new SUBRENDERER_TYPE[*NumberOfTracks];
		}

		while( *cur != '\0')
		{
			lineLength = getLineLength(cur);
			if( *cur != '#' && *cur != '\n')
			{
				//TODO, notice, end of string?
				(*MediaTrackURI)[index] = (char *)strndup(cur, lineLength);
				(*MediaTrackURI)[index][lineLength] = '\0';
				(*MediaTrackURI)[index][lineLength-1] = '\0';
				(*MediaTrackURI)[index][lineLength-2] = '\0';
				(*MediaType)[index] = DMR_SUBR_UNKNOWN;
				ALOGE("\t===> [DMR][Bugtracking] %s %d \n\t==> track uri:%s\n", __func__, __LINE__, (*MediaTrackURI)[index]);
				if( (*MediaTrackURI)[index][0] == 'h' ) printf("\t===> [DMR][Bugtracking] yes, this is h\n");
				else ALOGE("\t===> [DMR][Bugtracking]  Oh~~~no~~~~~~~\n");
				index++;
			}
			cur += lineLength;
		}
		return S_OK;
	}
	else{
		return E_FAIL;
	}
	ALOGE("playlistRenderer::preParse begin 2222!!");
	return S_OK;
}

int playlistRenderer::getListLength(char *pFileBuffer)
{
	int count = 0;
	if( pFileBuffer != NULL )
	{
		char *cur = pFileBuffer;
		while( *cur != '\0' )
		{
			if( *cur != '#' && *cur != '\n')
				count++;
			cur += (getLineLength(cur));
		}
	}
	printf("[DMR] there are %d media url in the playlist\n", count);
	return count;
}

int playlistRenderer::getLineLength(char *pFileBuffer)
{
	int count = 0;
	if( pFileBuffer != NULL )
	{
		while( pFileBuffer[count] != '\0' && pFileBuffer[count] != '\n')
			count++;
	}
	if( pFileBuffer[count] == '\0' ) count--;
	return (count+1);
}
