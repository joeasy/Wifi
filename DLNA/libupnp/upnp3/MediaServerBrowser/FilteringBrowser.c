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
 * $Workfile: FilteringBrowser.c
 *
 *
 *
 */

#if defined(WIN32) && !defined(_WIN32_WCE)
	#define _CRTDBG_MAP_ALLOC
	#include <stdlib.h>
	#include <string.h>
#endif

#define MAX_KEY_LEN 32

#if defined(WINSOCK2)
	#include <winsock2.h>
	#include <ws2tcpip.h>
#elif defined(WINSOCK1)
	#include <winsock.h>
	#include <wininet.h>
#endif

#include <stdio.h>
#include "ILibParsers.h"
#include "FilteringBrowser.h"
#include "ILibWebClient.h"				
#include "MediaServerControlPoint.h"
#include "CdsDidlSerializer.h"
#include "CdsStrings.h"

#if defined(WIN32) && !defined(_WIN32_WCE)
#include <crtdbg.h>
#endif


struct FB_Server;
struct FB_FilteringBrowser;
struct FB_FilteringBrowserManager;

typedef void (*FB_Fn_FreeData) (void *data);
typedef void (*FB_Fn_ProcessFB) (struct FB_FilteringBrowser *fb, void *serviceObj, struct MSCP_BrowseArgs *args, int errorCode, /*INOUT*/struct MSCP_ResultsList **results);

/*
 *	Tracks the current state while processing results.
 *	This struct is primarily used to store state between
 *	multiple CDS:Browse requests. 
 *
 *	Essentially, the FilteringBrowser will browse a large
 *	container using multiple CDS:Browse requests. In order
 *	to properly issue a CDS:Browse request, the FilteringBrowser
 *	needs to know how much of the container has been
 *	browsed.
 */
struct FB_ProcessingState
{
	unsigned int	UpdateID;
	unsigned int	ObjectsProcessed;
	unsigned int	ObjectsMatched;
};

struct FB_SnapShot
{
	unsigned int	Processed;
	unsigned int	Matched;
};

struct FB_Indexer
{
	/* indicates if the data in here has been properly sorted and is ready to use */
	unsigned char	IsDone;

	unsigned int	Passes;
	unsigned int	HashIndex;
	unsigned int	HopSize;
	unsigned int	NumSkipped;

	/* the target page number for an entry */
	unsigned int	PageIndex[FB_INDEXTABLE_SIZE];

	/* snapshots of the FilteringBrowser's ProcessingState */
	struct FB_SnapShot	SnapShots[FB_INDEXTABLE_SIZE];

	unsigned int	TotalProcessed, TotalMatched;

	/* first valid index into the ->Objectxxx fields */
	int	FirstHashIndex;
};

struct FB_Object;
struct FB_FilteringBrowser
{
	/* the FB_Object object that points to this FilteringBrowser. */
	struct FB_Object *Wrapper;

	/*
	 *	Nonzero indicates this object should be destroyed.
	 *	This flag must be set with ->Lock acquired.
	 */
	unsigned char			Flag_Destroy;

	/* nonzero indicates this FilteringBrowser is in the middle of processing data */
	unsigned char			Flag_Processing;

	/* pagination parameters for the results */
	struct FB_PageInfo		PageInfo;

	/* execute this method when results have are ready for application */
	FB_Fn_ResultsUpdated	ResultsCallback;

	/* execute this method when we know the accurate total number of pages */
	FB_Fn_PagesCounted		PageCountCallback;

	/*
	 *	Browsing context as a stack. Each item in the stack is 
	 *	CdsObject object. With exception of the bottom element,
	 *	popping items from the stack requires us to use MSCP_DestroyMediaObject
	 *	on the object.
	 *	Top of the stack is the current container.
	 *	Bottom of the stack is FB_AllRoots.
	 */
	void					*CurrentContext;

	/*
	 *	A queue of allowed CDS containers 
	 *	(i.e. CdsObject objects).
	 */
	void					*ForwardContexts;

	/*
	 *	The UPnP server to browse
	 */
	struct FB_Server		*Server;

	/*
	 *	synchronizes access to this object
	 */
	sem_t					Lock;

	/*
	 *	Stores the filtered results
	 */
	struct FB_ResultsInfo	*Results;

	/*
	 *	Used internally to track the status of obtaining results
	 */
	struct FB_ProcessingState	ProcessingState;

	/*
	 *  Used to track the status of page turning state
	 */
	struct FB_ProcessingState	ProcessingState_Page;

	/*
	 *	Used internally to optimize subsequent page-turning Browse calls.
	 */
	struct FB_Indexer			Indexer;

	/*
	 *	Identifies the parent of the current context.
	 *	Do not directly free this field.
	 */
	struct CdsObject	*Parent;

	/* most recent pending request */
	struct MSCP_BrowseArgs		*PendingRequest;

	/* most recent page turning pending request */
	struct MSCP_BrowseArgs		*PendingRequest_Page;

	/* User Tag */
	void* Tag;
};

/*
 *	This struct was introduced to enable re-entrancy with this module.
 *	Applications hold pointers to FB_Object objects
 *	but all of the work is still done on a FB_FilteringBrowser.
 *	If an application ever executes code that would result in reentrance
 *	behavior on a FilteringBrowser, then a new FilteringBrowser is
 *	created and assigned to ->FB.
 *
 *	The underlying problem with reentrancy is caused by the fact that
 *	the FB_Fn_ResultsUpdated callback can executes in the middle when
 *	results are being processed. This type of behavior is very nice
 *	for applications because user interfaces benefit from having 
 *	results show up sooner rather than later. (Processing requires
 *	the FilteringBrowser to examine all children in a container, so
 *	waiting to execute the callback until after processing is done
 *	is not very good.) Before executing the FB_Fn_ResultsUpdated
 *	callback, it's necessary for the FilteringBrowser to release
 *	its Locks to allow the app to call methods on this module.
 */
struct FB_Object
{
	sem_t						SyncLock;
	struct FB_FilteringBrowser	*FB;
	void*						ControlPointMicroStack;
	void*						Tag; /* Added by PDA */
};

struct FB_FilteringBrowserManager
{
	void (*PreSelect)(void* object,fd_set *readset, fd_set *writeset, fd_set *errorset, int* blocktime);
	void (*PostSelect)(void* object,int slct, fd_set *readset, fd_set *writeset, fd_set *errorset);
	void (*Destroy)(void* object);

	void*				ControlPointMicroStack;

	ILibThreadPool		ThreadPool;
	/*
	 *	Hashtree of all FilteringBrowsers. 
	 *	The key is address of a FilteringBrowser.
	 *	The value is a FilterBrowser.
	 */
	void*				Browsers;

	/*
	 *	Hashtree of all FilteringBrowserWrappers. 
	 *	The key is address of a FilteringBrowserWrapper.
	 *	The value is a FilterBrowserWrapper.
	 */
	void*				Wrappers;

	/*
	 *	Hashtree of all MediaServer. 
	 *	The key is the UDN of the MediaServer.
	 *	The value is an FB_Server object.
	 */
	void*				Servers;

	/*
	 *	Hashtree of all pending Browse requests. 
	 *	The key is the memory address of a MSCP_BrowseArgs.
	 *	The value is the FilteringBrowser object.
	 */
	void*				Requests;


	/*
	 *	Hashtree of all server UDNs. 
	 *	The key is the memory address of a UPnPService object.
	 *	The value is the UDN of the MediaServer.
	 */
	void*				UDNs;

	/*
	 *	Hashtree of all root containers.
	 *	The key is the UDN.
	 *	The value is the CdsObject.
	 */
	void*				RootContainers;

	/* execute this method to report a change in the list of servers */
	FB_Fn_ServerListUpdated	ServerListCallback;

	sem_t				Lock;
};


void								*FB_TheChain = NULL;
struct FB_FilteringBrowserManager	*FB_TheManager = NULL;

/*
 *	If you add new fields to struct CdsObject,
 *	you'll need to change this initializer.
 */
struct CdsObject			_FB_AllRoots = 
{
	0,								/*DeallocateThese*/
	"-1",							/*ID*/
	"-1",							/*ParentID*/
	"All Root Containers",			/*Title*/
	NULL,							/*Creator*/
	CDS_CLASS_MASK_CONTAINER,		/*MediaClass*/
	CDS_OBJPROP_FLAGS_Restricted,	/*Flags*/
	{NULL},							/*CpInfo*/
	0,								/*DlnaManaged*/
	NULL,							/*TakeOutGroups*/
	0,								/*NumTakeOutGroups*/
	{{NULL}},						/*TypeObject*/
	{0},							/*TypeMajor*/
	{0},							/*TypeMinor1*/
	{0},							/*TypeMinor2*/
	NULL,							/*Res*/
	NULL,							/*User*/
};
const struct CdsObject		*FB_AllRoots = &_FB_AllRoots;

#define EXECUTE_RESULTS_CALLBACK(fb) if (fb->ResultsCallback != NULL) fb->ResultsCallback(fb->Wrapper, fb->Results)

/*
 *	Adds an entry to the indexer.
 */
void _FB_AddIndex (unsigned int page, unsigned int objMatched, unsigned int objProcessed, struct FB_Indexer *pi)
{
	//if ((pi->HashIndex >= 1) && (pi->PageIndex[pi->HashIndex-1] == page)) return;

	pi->NumSkipped++;
	if ((pi->HashIndex < FB_INDEXTABLE_SIZE) && (pi->NumSkipped == pi->Passes+1))
	{
		if (pi->HashIndex >= 0)
		{
			pi->NumSkipped = 0;
			pi->PageIndex[pi->HashIndex] = page;
			pi->SnapShots[pi->HashIndex].Matched = objMatched;
			pi->SnapShots[pi->HashIndex].Processed = objProcessed;

			/*
			 *	Increment the hash index.
			 *	If the hash index is out of range, then reset it to zero
			 *	and increment the passes count.
			 */
			pi->HashIndex += pi->HopSize;

			if (pi->HashIndex >= FB_INDEXTABLE_SIZE)
			{
				pi->Passes++;
				if (pi->Passes < FB_INDEXTABLE_SIZE)
				{
					pi->HopSize = pi->Passes+1;
					pi->HashIndex = pi->Passes;
				}
				else
				{
					pi->HopSize = ((pi->Passes+1) % (FB_INDEXTABLE_SIZE));
					pi->HashIndex = pi->HopSize;
				}
			}
		}
		else
		{
			fprintf(stderr, "FB_AddIndex() error: pi->HashIndex=%d\r\n", pi->HashIndex);
		}
	}
}

/*
 *	Clears the ->Objectxxx data
 *	and resets the other fields to their proper initial state.
 */
void _FB_ClearIndexer(struct FB_Indexer *pi)
{
	memset(pi, 0, sizeof(struct FB_Indexer));
	pi->HopSize = 1;
}

/* for quicksort */
void _FB_InsertSort(struct FB_Indexer *pi, unsigned int lb, unsigned int ub)
{
	unsigned int key;
	struct FB_SnapShot val;
    unsigned int i, j;

   /*
    *  Sort array pi->ObjectsProcessed and pi->ObjectsMatched for this range: [lb..ub]
	*/
    for (i = lb + 1; i <= ub; i++) 
	{ 
		key = pi->PageIndex[i];
		val = pi->SnapShots[i];

        /* Shift elements down until insertion point found. */
		for (j = i-1; j >= lb && (pi->PageIndex[j] > key); j--)
		{
			pi->PageIndex[j+1] = pi->PageIndex[j];
			pi->SnapShots[j+1] = pi->SnapShots[j];
		}

        /* insert */
		pi->PageIndex[j+1] = key;
		pi->SnapShots[j+1] = val;
    }
}

/* for quicksort */
unsigned int _FB_Partition(struct FB_Indexer *pi, unsigned int lb, unsigned int ub) 
{
    unsigned int key, pivot;
	struct FB_SnapShot pivotValue, keyValue;
    unsigned int i, j, p;

   /*
    *	Partition pi->ObjectsMatched and pi->ObjectsProcessed for: [lb..ub]
	*/

    /* select pivot and exchange with 1st element */
    p = lb + ((ub - lb)>>1);
	
	pivot = pi->PageIndex[p];
	pivotValue = pi->SnapShots[p];

	pi->PageIndex[p] = pi->PageIndex[lb];
	pi->SnapShots[p] = pi->SnapShots[lb];

    /* sort lb+1..ub based on pivot */
    i = lb+1;
    j = ub;
    while (1) {
		while (i < j && (pivot > pi->PageIndex[i])) i++;
		while (j >= i && (pi->PageIndex[j] > pivot)) j--;
        if (i >= j) break;
        
		key = pi->PageIndex[i];
		keyValue = pi->SnapShots[i];

		pi->PageIndex[i] = pi->PageIndex[j];
		pi->SnapShots[i] = pi->SnapShots[j];

		pi->PageIndex[j] = key;
		pi->SnapShots[j] = keyValue;
        
		j--;
		i++;
    }

    /* pivot belongs in pi->PageIndex[j] */
	pi->PageIndex[lb] = pi->PageIndex[j];
	pi->SnapShots[lb] = pi->SnapShots[j];

	pi->PageIndex[j] = pivot;
	pi->SnapShots[j] = pivotValue;

    return j;
}

void _FB_QuickSort(struct FB_Indexer *pi, unsigned int lb, unsigned int ub) 
{
    unsigned int m;

   /*
    *	Sort the hashtable from indices a[lb..ub]
	*/
    while (lb < ub)
	{
        /* quickly sort short lists */
        if (ub - lb <= 12) 
		{
            _FB_InsertSort(pi, lb, ub);
            return;
        }

        /* partition into two segments */
        m = _FB_Partition (pi, lb, ub);

        /* sort the smallest partition    */
        /* to minimize stack requirements */
        if (m - lb <= ub - m) 
		{
            _FB_QuickSort(pi, lb, m - 1);
            lb = m + 1;
        }
		else 
		{
            _FB_QuickSort(pi, m + 1, ub);
            ub = m - 1;
        }
    }
}

/*
 *	This method sorts the elements in the arrays
 *	according to the values in ->ObjectsMatched.
 *	The method should only be called after
 *	the playlist's contents have been completely
 *	processed.
 */
void _FB_SortIndexer(struct FB_Indexer *pi)
{
	int i;

	_FB_QuickSort(pi, 0, FB_INDEXTABLE_SIZE-1);

	pi->HashIndex = 0;
	for (i=0; i < FB_INDEXTABLE_SIZE; i++)
	{
		if (pi->PageIndex[i] > 0)
		{
			pi->FirstHashIndex = i;
			break;
		}
	}
}


void* _FB_LinkedListDeQueue(void *ll)
{
	/* return the head of the linked list and remove it */
	struct LinkedList_Node *lln = NULL;
	void *retVal = NULL;

	lln = ILibLinkedList_GetNode_Head(ll);
	if (lln != NULL)
	{
		retVal = ILibLinkedList_GetDataFromNode(lln);
		ILibLinkedList_Remove(lln);
	}

	return retVal;
}

void _FB_CopyLinkedList2(void* destll, void *ll)
{
	void *llnode;
	struct CdsObject *obj, *clone;

	ILibLinkedList_Lock(ll);
	llnode = ILibLinkedList_GetNode_Head(ll);
	while (llnode != NULL)
	{
		obj = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);
		if (obj != NULL)
		{
			/* clone the object and add a reference to it */
			clone = CDS_CloneMediaObject(obj);
//			CDS_ObjRef_Add(clone);

			/* add clone to new list */
			ILibLinkedList_AddTail(destll, clone);
		}

		/* move to next node */
		llnode = ILibLinkedList_GetNextNode(llnode);
	}
	ILibLinkedList_UnLock(ll);
}

void* _FB_CopyLinkedList(void *ll)
{
	void *retVal = NULL;

	retVal = ILibLinkedList_Create();
	_FB_CopyLinkedList2(retVal, ll);

	return retVal;
}

struct CdsObject* _FB_ContextPeekOrRemove(void *ll, int removeFlag)
{
	struct CdsObject *retVal = NULL;
	void *llnode = NULL;

	/*
	 *	Returns data that is at the head of the linked list.
	 *	If removeFlag!=0, then we remove the head of linked list also.
	 */

	/* method assumes context is already locked */
	llnode = ILibLinkedList_GetNode_Head(ll);
	if (llnode != NULL)
	{
		/* get data from the head of the list */
		retVal = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);

		if (removeFlag != 0)
		{
			/* remove if indicated to */

			if (retVal != NULL)
			{
				/* release since we AddRef in a Push operation */
				CDS_ObjRef_Release(retVal);
			}

			/* actually remove it */
			ILibLinkedList_Remove(llnode);
		}
	}

	return retVal;
}

struct CdsObject* _FB_ContextPeek(void *ll)
{
	struct CdsObject *retVal = _FB_ContextPeekOrRemove(ll, 0);
	return retVal;
}

struct CdsObject* _FB_ContextPop(void *ll)
{
	struct CdsObject *retVal = _FB_ContextPeekOrRemove(ll, 1);
	return retVal;
}

void _FB_ContextPush(void *ll, struct CdsObject *c)
{
	/* method assumes context is already locked */

	/*
	 *	pushes 'c' to the head of linked list
	 */

	CDS_ObjRef_Add(c);
	ILibLinkedList_AddHead(ll, c);
}

void _FB_ClearProcessingState(struct FB_FilteringBrowser *fb)
{

	if (fb != NULL)
	{
		sem_wait(&(fb->Lock));
		memset(&(fb->ProcessingState), 0, sizeof(struct FB_ProcessingState));
		memset(&(fb->ProcessingState_Page), 0, sizeof(struct FB_ProcessingState));
		_FB_ClearIndexer(&(fb->Indexer));
//		if (fb->PageInfo.ExamineMethod == NULL)
//		{
//			fb->Indexer.IsDone = 1;
//		}
		sem_post(&(fb->Lock));
	}
}

void _FB_FreeFilteringBrowserWrapper(struct FB_Object *fbw)
{
	sem_destroy(&(fbw->SyncLock));
	free(fbw);
}

void _FB_FreeFilteringBrowser(struct FB_FilteringBrowser *fb)
{
	void *e;
	char *key; 
	int keyLen;
	struct FB_FilteringBrowser *fb2;
	char *deleteThis = NULL;
	struct CdsObject *cdsObj;
	void *temp;

	/*
	 *	Free the server queue. Do this before
	 *	we lock fb because FB_ClearProcessingStateServers()
	 *	will lock fb too.
	 */
	_FB_ClearProcessingState(fb);

	sem_wait(&(fb->Lock));

	/*
	 *	before destroying the filter browser, 
	 *	be sure to remove it from the list of browsers 
	 *	with pending requests
	 */
	if ((FB_TheManager != NULL) && (FB_TheManager->Requests != NULL))
	{
		/*
		 *	Remove this FilteringBrowser to indicate that
		 *	it's no longer interested in processing requests
		 */
		ILibHashTree_Lock(FB_TheManager->Requests);
		e = ILibHashTree_GetEnumerator(FB_TheManager->Requests);
		while (ILibHashTree_MoveNext(e) == 0)
		{
			ILibHashTree_GetValue(e, &key, &keyLen, &temp);
			fb2 = (struct FB_FilteringBrowser*)temp;
			if (fb == fb2)
			{
				/* we have a match, so remember the key */
				deleteThis = key;
				
				break;
			}
		}

		/* remove it if appropriate */
		if (deleteThis != NULL)
		{
			ILibDeleteEntry(FB_TheManager->Requests, deleteThis, keyLen);
		}

		ILibHashTree_DestroyEnumerator(e);
		ILibHashTree_UnLock(FB_TheManager->Requests);
	}

	/* destroy context stack */
	if (fb->CurrentContext != NULL)
	{
		FB_DestroyContext(fb->CurrentContext);
		fb->CurrentContext = NULL;
	}
	
	/* destroy saved results */
	FB_DestroyResults(fb->Results);

	if (fb->PendingRequest != NULL)
	{
		free(fb->PendingRequest);
	}
	
	if (fb->PendingRequest_Page != NULL)
	{
		free(fb->PendingRequest_Page);
	}

	/* clear forward contexts */
	if (fb->ForwardContexts != NULL)
	{
		cdsObj = (struct CdsObject*) _FB_LinkedListDeQueue(fb->ForwardContexts);
		while (cdsObj != NULL)
		{
			CDS_ObjRef_Release(cdsObj);
			cdsObj = (struct CdsObject*) _FB_LinkedListDeQueue(fb->ForwardContexts);
		}
		ILibLinkedList_Destroy(fb->ForwardContexts);
		fb->ForwardContexts = NULL;
	}

	/* free the semaphore last */
	sem_post(&(fb->Lock));
	sem_destroy(&(fb->Lock));
	free (fb);
}

struct FB_ServerListCS
{
	int RemoveFlag;
	char *udn;
};

void _FB_ServerListChangeEx2(ILibThreadPool sender, void *var)
{
	struct FB_ServerListCS *csd = (struct FB_ServerListCS*) var;
	if ((FB_TheManager != NULL) && (FB_TheManager->ServerListCallback != NULL))
	{
		FB_TheManager->ServerListCallback(csd->RemoveFlag, csd->udn);
	}
	free(csd);
}

void _FB_ServerListChangeEx1(int remove_flag, char *udn)
{
	struct FB_ServerListCS *csd = (struct FB_ServerListCS*) malloc(sizeof(struct FB_ServerListCS));
	csd->RemoveFlag = remove_flag;
	csd->udn = udn;
	ILibThreadPool_QueueUserWorkItem(FB_TheManager->ThreadPool, csd, _FB_ServerListChangeEx2);
}

void _FB_PagesCountedEx2(ILibThreadPool sender, void *var)
{
	struct FB_FilteringBrowser *fb = (struct FB_FilteringBrowser *) var;
	if (fb->PageCountCallback != NULL)
	{
		fb->PageCountCallback(fb->Wrapper);

	}
}

void _FB_PagesCountedEx1(struct FB_FilteringBrowser *fb)
{
	ILibThreadPool_QueueUserWorkItem(FB_TheManager->ThreadPool, fb, _FB_PagesCountedEx2);
}

void _FB_ReportResultsEx2(ILibThreadPool sender, void *var)
{
	struct FB_FilteringBrowser *fb = (struct FB_FilteringBrowser *) var;
	EXECUTE_RESULTS_CALLBACK(fb);
}

void _FB_ReportResultsEx1(int cs_flag, struct FB_FilteringBrowser *fb)
{
	/*
		cs_flag indicates whether the context switch should actually be done.
		In some cases (e.g. when executing in _FB_ProcessBrowseResults), we
		can assume that we are not running on the microstack's thread because
		we can assume thread is from the threadpool (e.g. _FB_OnResult_BrowseEx2).
		In those cases we use cs_flag=0.

		In some cases, we have to do a context-switch (e.g. from FB_Refresh). In
		those cases, we use cs_flag=1.
	*/
	if (cs_flag == 0)
	{
		EXECUTE_RESULTS_CALLBACK(fb);
	}
	else
	{
		ILibThreadPool_QueueUserWorkItem(FB_TheManager->ThreadPool, fb, _FB_ReportResultsEx2);
	}
}

void _FB_BrowseRootContainer(struct FB_Server *server)
{
	struct MSCP_BrowseArgs* args = NULL;

	if ((FB_TheManager != NULL) && (FB_TheManager->RootContainers != NULL))
	{
		/* allocate memory for arguments */
		args = (struct MSCP_BrowseArgs*) malloc(sizeof(struct MSCP_BrowseArgs));
		memset(args, 0, sizeof(struct MSCP_BrowseArgs));

		/* set values for arguments */
		args->BrowseFlag = MSCP_BrowseFlag_Metadata;
		args->RequestedCount = 0;
		args->StartingIndex = 0;
		args->Filter = CDS_STRING_ALL_FILTER;
		args->ObjectID = "0";
		args->SortCriteria = "";

		/* issue browse request */
		MSCP_Invoke_Browse(server->Service, args);
	}
}

/* IDF#2a: incrementally browse */
int _FB_BrowseNextStep(struct FB_FilteringBrowser *fb)
{
	struct MSCP_BrowseArgs *args = NULL;
	char *containerID;
	char key[MAX_KEY_LEN];
	char *k;
	int keyLen;
	struct FB_FilteringBrowser *fb2;
	char *removeThisKey = NULL;
	void *e;
	struct UPnPService *targetServer = NULL;
	struct FB_Server *serverInfo = NULL;
	char *udn;
	int retVal = -1;
	struct CdsObject *cc;
	void *temp;

	if (
		(FB_TheManager != NULL) && 
		(FB_TheManager->Requests != NULL) && 
		(fb != NULL) && 
		(fb->Results != NULL) && 
		(fb->Results->LinkedList != NULL)
		)
	{


		sem_wait(&(fb->Lock));
		fb->Flag_Processing = 1;
		cc = _FB_ContextPeek(fb->CurrentContext);
		if (
			(cc != NULL))
		{
			/*
			 *	Pick a target server.
			 */
			/* target server is already selected */
			targetServer = fb->Server->Service;
			#ifdef _DEBUG
			if (targetServer != cc->CpInfo.Reserved.ServiceObject)
			{
				fprintf(stderr, "ERROR: #256");
			}
			#endif


			if (targetServer != NULL)
			{
				/*
				 *	A server is selected, so acquire the containerID
				 *	that we want to browse.
				 */

				/* the containerID is identified in fb->CurrentContext */
				containerID = cc->ID;

				if (containerID != NULL)
				{
					/* IDF#2b: create browse args */
					/*
					 *	Build the arguments for the next browse request.
					 */

					args = (struct MSCP_BrowseArgs*) malloc(sizeof(struct MSCP_BrowseArgs));

					/* choose the correct arguments based on the browsing mode */
					args->BrowseFlag = MSCP_BrowseFlag_Children;
					/* always request the number of CDS objects that will fit on our page */
					args->RequestedCount = FB_BROWSE_SIZE;
					args->StartingIndex = fb->ProcessingState.ObjectsProcessed;
				
					/* IDF#2c: set metadata */
					/* TODO: modify this to match the set of metadata of interest */
					args->Filter = CDS_STRING_ALL_FILTER;
					args->ObjectID = containerID;
					args->SortCriteria = "";

					/*
					 *	check the hashtree for an out-of-date request.
					 *	If an out-of-date request exists, remove it.
					 *	Things in FB_TheManager->Requests are keyed with the
					 *	memory address of 'args'. The value stored is a
					 *	FilteringBrowser object.
					 *	Also lock ->Servers, since it will be needed later
					 *	and because ->Servers is always acquired before ->Requests.
					 */
					ILibHashTree_Lock(FB_TheManager->Servers);
					ILibHashTree_Lock(FB_TheManager->Requests);
					e = ILibHashTree_GetEnumerator(FB_TheManager->Requests);
					while (ILibHashTree_MoveNext(e) == 0)
					{
						/* get the key and store it in our list */
						ILibHashTree_GetValue(e, &k, &keyLen, &temp);
						fb2 = (struct FB_FilteringBrowser*)temp;
						if (fb2 == fb)
						{
							removeThisKey = k;
							break;
						}
					}
					if (removeThisKey != NULL)
					{
						ILibDeleteEntry(FB_TheManager->Requests, removeThisKey, keyLen);
					}
					ILibHashTree_DestroyEnumerator(e);

					/*
					 *	Be sure to make sure the server still exists
					 *	before calling it.
					 */

					/*
					 *	Attempt to find the target server's UDN.
					 */	
					
					/* build a key for ->UDNs - ignore compiler warning*/
					#ifdef WIN32
					#pragma warning( disable : 4311)
					#endif
					keyLen = sprintf(key, "%p", targetServer);
					#ifdef WIN32
					#pragma warning( default : 4311)
					#endif
					key[keyLen] = '\0';
					
					ILibHashTree_Lock(FB_TheManager->UDNs);
					udn = (char*) ILibGetEntry(FB_TheManager->UDNs, key, keyLen);
					ILibHashTree_UnLock(FB_TheManager->UDNs);

					if (udn != NULL)
					{
						/*
						 *	The ->Servers hashtree should be locked, so
						 *	go ahead and look up the server info.
						 */
						serverInfo = (struct FB_Server*) ILibGetEntry(FB_TheManager->Servers, udn, (int)strlen(udn));

						if (serverInfo != NULL)
						{
							/* 
							 *	add the request into the hashtable 
							 */

							/* build a key for ->Requests - ignore compiler warning*/
							#ifdef WIN32
							#pragma warning( disable : 4311)
							#endif
							keyLen = sprintf(key, "%p", args);
							#ifdef WIN32
							#pragma warning( default : 4311)
							#endif
							key[keyLen] = '\0';

							/*
							*	Add the request to our list of pending requests.
							*/
							ILibAddEntry(FB_TheManager->Requests, key, keyLen, fb);

							/* IDF#2d: issue browse request */

							/* Issue the next browse request. */
							MSCP_Invoke_Browse(targetServer, args);
							
							if (fb->PendingRequest != NULL)
							{
								free (fb->PendingRequest);
							}
							fb->PendingRequest = args;
							retVal = 0;
						}
					}

					if (retVal != 0)
					{
						/* didn't invoke, so we need to free the args */
						free (args);
					}

					/* unlock the trees */
					ILibHashTree_UnLock(FB_TheManager->Requests);
					ILibHashTree_UnLock(FB_TheManager->Servers);
				}
			}
		}

		sem_post(&(fb->Lock));
	}

	return retVal;
}

/* issue a page browse request */
int _FB_BrowsePage(struct FB_FilteringBrowser *fb)
{
	struct MSCP_BrowseArgs *args = NULL;
	char *containerID;
	char key[MAX_KEY_LEN];
	char *k;
	int keyLen;
	struct FB_FilteringBrowser *fb2;
	char *removeThisKey = NULL;
	void *e;
	struct UPnPService *targetServer = NULL;
	struct FB_Server *serverInfo = NULL;
	char *udn;
	int retVal = -1;
	struct CdsObject *cc;
	void *temp;

	if (
		(FB_TheManager != NULL) && 
		(FB_TheManager->Requests != NULL) && 
		(fb != NULL) && 
		(fb->Results != NULL) && 
		(fb->Results->LinkedList != NULL)
		)
	{


		sem_wait(&(fb->Lock));
		fb->Flag_Processing = 1;
		cc = _FB_ContextPeek(fb->CurrentContext);
		if (
			(cc != NULL))
		{
			/*
			 *	Pick a target server.
			 */
			/* target server is already selected */
			targetServer = fb->Server->Service;
			#ifdef _DEBUG
			if (targetServer != cc->CpInfo.Reserved.ServiceObject)
			{
				fprintf(stderr, "ERROR: #256");
			}
			#endif


			if (targetServer != NULL)
			{
				/*
				 *	A server is selected, so acquire the containerID
				 *	that we want to browse.
				 */

				/* the containerID is identified in fb->CurrentContext */
				containerID = cc->ID;

				if (containerID != NULL)
				{
					/* IDF#2b: create browse args */
					/*
					 *	Build the arguments for the next browse request.
					 */

					args = (struct MSCP_BrowseArgs*) malloc(sizeof(struct MSCP_BrowseArgs));

					/* choose the correct arguments based on the browsing mode */
					args->BrowseFlag = MSCP_BrowseFlag_Children;
					/* always request the number of CDS objects that will fit on our page */
					args->RequestedCount = FB_BROWSE_SIZE;
					args->StartingIndex = fb->ProcessingState_Page.ObjectsProcessed;
				
					/* IDF#2c: set metadata */
					/* TODO: modify this to match the set of metadata of interest */
					args->Filter = CDS_STRING_ALL_FILTER;
					args->ObjectID = containerID;
					args->SortCriteria = "";

					/*
					 *	check the hashtree for an out-of-date request.
					 *	If an out-of-date request exists, remove it.
					 *	Things in FB_TheManager->Requests are keyed with the
					 *	memory address of 'args'. The value stored is a
					 *	FilteringBrowser object.
					 *	Also lock ->Servers, since it will be needed later
					 *	and because ->Servers is always acquired before ->Requests.
					 */
					ILibHashTree_Lock(FB_TheManager->Servers);
					ILibHashTree_Lock(FB_TheManager->Requests);
					e = ILibHashTree_GetEnumerator(FB_TheManager->Requests);
					while (ILibHashTree_MoveNext(e) == 0)
					{
						/* get the key and store it in our list */
						ILibHashTree_GetValue(e, &k, &keyLen, &temp);
						fb2 = (struct FB_FilteringBrowser*)temp;
						if (fb2 == fb)
						{
							removeThisKey = k;
							break;
						}
					}

					if (removeThisKey != NULL)
					{
						ILibDeleteEntry(FB_TheManager->Requests, removeThisKey, keyLen);
					}
					ILibHashTree_DestroyEnumerator(e);

					/*
					 *	Be sure to make sure the server still exists
					 *	before calling it.
					 */

					/*
					 *	Attempt to find the target server's UDN.
					 */	
					
					/* build a key for ->UDNs - ignore compiler warning*/
					#ifdef WIN32
					#pragma warning( disable : 4311)
					#endif
					keyLen = sprintf(key, "%p", targetServer);
					#ifdef WIN32
					#pragma warning( default : 4311)
					#endif
					key[keyLen] = '\0';
					
					ILibHashTree_Lock(FB_TheManager->UDNs);
					udn = (char*) ILibGetEntry(FB_TheManager->UDNs, key, keyLen);
					ILibHashTree_UnLock(FB_TheManager->UDNs);

					if (udn != NULL)
					{
						/*
						 *	The ->Servers hashtree should be locked, so
						 *	go ahead and look up the server info.
						 */
						serverInfo = (struct FB_Server*) ILibGetEntry(FB_TheManager->Servers, udn, (int)strlen(udn));

						if (serverInfo != NULL)
						{
							/* 
							 *	add the request into the hashtable 
							 */

							/* build a key for ->Requests - ignore compiler warning*/
							#ifdef WIN32
							#pragma warning( disable : 4311)
							#endif
							keyLen = sprintf(key, "%p", args);
							#ifdef WIN32
							#pragma warning( default : 4311)
							#endif
							key[keyLen] = '\0';

							/*
							*	Add the request to our list of pending requests.
							*/
							ILibAddEntry(FB_TheManager->Requests, key, keyLen, fb);

							/* IDF#2d: issue browse request */

							/* Issue the next browse request. */
							MSCP_Invoke_Browse(targetServer, args);

//							if (fb->PendingRequest_Page != NULL)
//							{
//								free (fb->PendingRequest_Page);
//							}
							fb->PendingRequest_Page = args;
							retVal = 0;
						}
					}

					if (retVal != 0)
					{
						/* didn't invoke, so we need to free the args */
						free (args);
					}

					/* unlock the trees */
					ILibHashTree_UnLock(FB_TheManager->Requests);
					ILibHashTree_UnLock(FB_TheManager->Servers);
				}
			}
		}

		sem_post(&(fb->Lock));
	}

	return retVal;
}

void _FB_DestroyFilteringBrowser(void *fbObj)
{
	struct FB_FilteringBrowser *fb = (struct FB_FilteringBrowser*) fbObj;
	char key[MAX_KEY_LEN];
	int keyLen;

	if ((FB_TheChain != NULL) && (FB_TheManager != NULL) && (fbObj != NULL))
	{
		/* make a key using the memory address */
		/* build a key - ignore compiler warning*/
		#ifdef WIN32
		#pragma warning( disable : 4311)
		#endif
		keyLen = sprintf(key, "%p", fb);
		#ifdef WIN32
		#pragma warning( default : 4311)
		#endif
		key[keyLen] = '\0';

		/* remove the FilteringBrowser */
		ILibHashTree_Lock(FB_TheManager->Browsers);
		ILibDeleteEntry(FB_TheManager->Browsers, key, keyLen);

		/* free all of the data associated with struct FB_FilteringBrowser */
		_FB_FreeFilteringBrowser(fb);

		ILibHashTree_UnLock(FB_TheManager->Browsers);
	}
}

void _FB_ProcessRootContainer(void *serviceObj, struct MSCP_BrowseArgs *args, int errorCode, /*INOUT*/struct MSCP_ResultsList **results)
{
	char key[MAX_KEY_LEN];
	int keyLen;
	int udnLen;
	struct CdsObject *obj = NULL;
	struct CdsObject *root = NULL;
	int count;
	void *llnode;
	char *udn;

	/* build a key for ->UDNs - ignore compiler warning*/
	#ifdef WIN32
	#pragma warning( disable : 4311)
	#endif
	keyLen = sprintf(key, "%p", serviceObj);
	#ifdef WIN32
	#pragma warning( default : 4311)
	#endif
	key[keyLen] = '\0';

	/* get the UDN for the UPnP service's parent device */
	ILibHashTree_Lock(FB_TheManager->UDNs);
	udn = (char*) ILibGetEntry(FB_TheManager->UDNs, key, keyLen);
	ILibHashTree_UnLock(FB_TheManager->UDNs);
	
	if (udn == NULL)
	{
		/*
		 *	We have this code because sometimes 
		 *	we get a response from a DMS that is
		 *	still there, but we've removed it
		 *	from our ->UDNs list because a new
		 *	route was found.
		 */
		return;
	}

	udnLen = (int) strlen(udn);

	ILibHashTree_Lock(FB_TheManager->RootContainers);

	count = ILibLinkedList_GetCount((*results)->LinkedList);
	if (count > 0)
	{
		llnode = ILibLinkedList_GetNode_Head((*results)->LinkedList);

		if (llnode != NULL)
		{
			root = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);
			
			if (root != NULL)
			{
				/* if we already have this root container, remove it */
				obj = (struct CdsObject*) ILibGetEntry(FB_TheManager->RootContainers, udn, udnLen);
				if (obj != NULL)
				{
					CDS_ObjRef_Release(obj);
					ILibDeleteEntry(FB_TheManager->RootContainers, udn, udnLen);
				}

				/* add the root container */
				CDS_ObjRef_Add(root);
				ILibAddEntry(FB_TheManager->RootContainers, udn, udnLen, root);
			}
		}
	}
	ILibHashTree_UnLock(FB_TheManager->RootContainers);

	if (FB_TheManager->ServerListCallback != NULL)
	{
		FB_TheManager->ServerListCallback(0, udn);
	}
}

/* IDF#4a: process browse results */
void _FB_ProcessBrowseResults(struct FB_FilteringBrowser *fb, void *serviceObj, struct MSCP_BrowseArgs *args, int errorCode, /*INOUT*/struct MSCP_ResultsList **results)
{
	struct FB_ProcessingState *ps = NULL;
	struct FB_ProcessingState *ps_page = NULL;

	struct FB_Indexer *pi = NULL;
	struct CdsObject *pt = NULL;
	unsigned int skipCount = 0;
	void *llnode = NULL;
	char resultsLimitReached = 0;
	char matched = 0;
	unsigned int targetPage;
	unsigned int count;
	unsigned int i = 0;
	unsigned int m = 0;
	unsigned int j = 0;
	char stopProcessing = 0;
	char indexFound = 0;

	//OutputDebugString("-=Processing=-\r\n");

	if ((fb != NULL) && (fb->Results != NULL))
	{
		sem_wait(&(fb->Lock));

		/*
		 *	Get pointers to the processing state and indexer.
		 *	As we process results, we will modify the processing state
		 *	so that we know which portion of the current container
		 *	we have processed so far.
		 *
		 *	As we find matches, we will update the index.
		 */
		ps = (struct FB_ProcessingState*) &(fb->ProcessingState);
		ps_page = (struct FB_ProcessingState*) &(fb->ProcessingState_Page);

		// this is a page browsing request, copy state
		if(fb->PendingRequest_Page != NULL)
		{
			ps->ObjectsMatched = ps_page->ObjectsMatched;
			ps->ObjectsProcessed = ps_page->ObjectsProcessed;

			/*
			 *	state copied
			 *	then clear its current page turning state
			 */
			memset(&(fb->ProcessingState_Page), 0, sizeof(struct FB_ProcessingState));
			fb->PendingRequest_Page = NULL;
		}


		pi = (struct FB_Indexer*) &(fb->Indexer);

		if (((*results) != NULL) && (args->StartingIndex > 0) && (ps->UpdateID != (*results)->UpdateID))
		{
			/*
			 *	If we're in the middle of browsing a container, and the update ID
			 *	has changed, then we need to start over.
			 */
			stopProcessing = 1;
		}
		else 
		{
			if ((errorCode != 0))// && (fb->Results->ErrorCode == 0))
			{
				/* A parsing error has occurred so note the error. */
				fb->Results->ErrorCode = errorCode;

				sem_post(&(fb->Lock));
				/* report the errors */
				_FB_ReportResultsEx1(0, fb);
				stopProcessing = 1;
				sem_wait(&(fb->Lock));
			}
			
			if ((*results) != NULL)
			{
				/* IDF#4b: processing algorithm */

				/*
				 *	We need to process the browse results.
				 *	Essentially, we do the following.
				 *
				 *	[1] Check to see if the object would match
				 *	our filtering requirements. If there's a match
				 *	then do [2]. Otherwise, ignore the object.
				 *
				 *	[2] We found a matching object, now we need to
				 *	figure out if it should be on the results page.
				 *	Essentially we do the following:
				 *		-Skip the first (fb->PageInfo.PageSize * fb->PageInfo.Page) matched objects.
				 *		-Save the next fb->PageInfo.PageSize matched objects.
				 *		-Ignore everything after it.
				 */


				/* skipCount == number of CDS objects in the container to skip */
				skipCount = fb->PageInfo.PageSize * fb->PageInfo.Page;

				/* note some stats about the current container */
				ps->UpdateID = fb->Results->UpdateID = (*results)->UpdateID;
				fb->Results->TotalInContainer = (*results)->TotalMatches;


				/* pt represents "process this" - as in process this CDS object*/
				pt = NULL;
				ILibLinkedList_Lock((*results)->LinkedList);

				/* get the first CDS object from the Browse response */
				llnode = ILibLinkedList_GetNode_Head((*results)->LinkedList);
				if (llnode != NULL)
				{
					pt = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);
				}

				/*
				 *	Process results until...
				 *		- we have no more results, or
				 *		- the current request has been flagged as out of date (i.e. to be destroyed), or
				 *		- we've processed all of the objects that will fit on the desired page
				 *			and the app isn't asking for an accurate page count
				 */
				while (
					/* we have a result */
					(pt != NULL) && 
					/* current request has not been flagged as out-of-date */
					(fb->Flag_Destroy == 0)
					)
				{
					if (
						(fb->PageInfo.ForceAccuratePageCount == 0) &&
						(resultsLimitReached !=0)
						)
					{
						/* we've processed everything and we don't need an accurate page count */
						break;
					}

					/*
					 *	Processing results means the following...
					 *	[1] All containers are saved as possible forward contexts.
					 *	[2] All objects are checked against the matching algorithm.
					 *		We note the index of matched objects and if appropriate,
					 *		add them to the results of the FilteringBrowser.
					 */

					/*
					 *	Save containers as possible forward context values.
					 *	Be sure to add a clone of the CDS container.
					 */
					if ((pt->MediaClass & CDS_CLASS_MASK_OBJECT_TYPE) ==  CDS_CLASS_MASK_CONTAINER)
					{
						if (fb->ForwardContexts == NULL)
						{
							fb->ForwardContexts = ILibLinkedList_Create();
						}
						CDS_ObjRef_Add(pt);
						ILibLinkedList_AddTail(fb->ForwardContexts, pt);
					}

					/* IDF#4c: filter the CDS object */
					/*
					 *	Check to see if the CDS entry is a match.
					 *	Even if we have finished results for this page,
					 *	continue asking because we'll want to count
					 *	the number of pages available.
					 */
					if ((fb->PageInfo.ExamineMethod != NULL))
					{
						matched = fb->PageInfo.ExamineMethod(fb, pt);
					}
					else
					{
						matched = 1;
					}

					if (matched)
					{
						/*
						 *	We have a matched item, so update the indexer
						 *	if we have not finished updating it at least once
						 *	for this container. 
						 *
						 *	The intent of the indexer is to capture the processing
						 *	state of the FilteringBrowser right before the first matched 
						 *	object on a target page.
						 */
						

						/* IDF#4e: update the indexer (indexer responsible for optimizing subsequent browse requests) */

						/*
						 *	We only add info to the indexer when we haven't completely
						 *	processed a container and when we're browsing children.
						 */
						if ((pi->IsDone == 0) && (args->BrowseFlag == MSCP_BrowseFlag_Children))
						{
							/* TODO: optimize the calculation of targetPage */
							targetPage = (unsigned int) floor(((float) (ps->ObjectsMatched) / (float)fb->PageInfo.PageSize));

							/* add the info only when we're starting a new page */
							if (
								((pi->HashIndex >= 1) && (pi->PageIndex[pi->HashIndex-1] != targetPage)) ||
								(pi->HashIndex == 0)
								)
							{
									/* 
										only add index if it does not already exist in the table
										so that page turning results does not interefere with index tables
									*/
									for (j=fb->Indexer.FirstHashIndex; j < FB_INDEXTABLE_SIZE; j++)
									{
										if (targetPage!=0&&targetPage == fb->Indexer.PageIndex[j])
										{
											indexFound = 1;
											break;
										}
									}
									
									if(!indexFound)
									{
										_FB_AddIndex(targetPage, ps->ObjectsMatched, ps->ObjectsProcessed, pi);
									}
							}
						}

						/*
						 *	Figure out if we need to add this object to the
						 *	FilteringBrowser's results. We only add objects
						 *	to the results when they match the pagination requirements.
						 */
						ps->ObjectsMatched++;
						count = (unsigned int)ILibLinkedList_GetCount(fb->Results->LinkedList);
						m++;
						
						/* check pagination requirements */
						if (
							(ps->ObjectsMatched > (skipCount)) &&
							(count < fb->PageInfo.PageSize)
							)
						{
							/*
							 *	Append the new CDS object to the results
							 *	and increment the counter.
							 *
							 *	TODO: You can optimize this code such that 
							 *	pt is removed from the 'results' linked list
							 *	and appended to the 'fb->Results->Results' linked list.
							 *	This consists of a few well-placed pointer reassignment
							 *	operations. Architecturally, this module isn't supposed
							 *	to have visibility into how 'results' handles its linked
							 *	list, which is why I didn't do it.
							 */

							/* IDF#4f: update list of filtered results */

							/* ensure that the CDS object isn't deallocated */
							CDS_ObjRef_Add(pt);

							/* add the object to the list */
							ILibLinkedList_Lock(fb->Results->LinkedList);
							ILibLinkedList_AddTail(fb->Results->LinkedList, pt);

							/* note the last CDS object that was processed in our results */
							fb->Results->IndexIntoContainer = args->StartingIndex + i;

							/*
							 *	Check to see if we've reached our maximum size.
							 *	If so, then set resultsLimitReached and
							 *	execute the results callback now, so that the app
							 *	doesn't have to wait until the FilteringBrowser
							 *	has processed everything.
							 */
							if ((count) == (fb->PageInfo.PageSize-1))
							{
								resultsLimitReached = 1;
								if ((fb->Flag_Destroy == 0) && (fb->ResultsCallback != NULL))
								{
									ILibLinkedList_UnLock((*results)->LinkedList);
									ILibLinkedList_UnLock(fb->Results->LinkedList);
									sem_post(&(fb->Lock));
									/* IDF#6a: report results */
									_FB_ReportResultsEx1(0, fb);
									sem_wait(&(fb->Lock));
									ILibLinkedList_Lock(fb->Results->LinkedList);
									ILibLinkedList_Lock((*results)->LinkedList);
								}
							}
							ILibLinkedList_UnLock(fb->Results->LinkedList);
						}
					}
					ps->ObjectsProcessed++;

					
					/*
					 *	We've done what we can with the CDS object,
					 *	so process the next one.
					 */
					llnode = ILibLinkedList_GetNextNode(llnode);
					if (llnode != NULL)
					{
						pt = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);
					}
					else
					{
						pt = NULL;
					}
					
					i++;
				}	/* end while() - we're done processing all objects in the Browse response */

				/*
				 *	If the indexer hasn't been populated with data from the entire container,
				 *	and we need an accurate page count, go ahead and apply some container stats.
				 *
				 *	Otherwise, just report stats - which are only accurate up to what
				 *	we've processed.
				 */
				if ((pi->IsDone == 0) && (fb->PageInfo.ForceAccuratePageCount != 0))
				{
//					pi->TotalProcessed += i;
//					pi->TotalMatched += m;
					pi->TotalProcessed = ps->ObjectsProcessed;
					pi->TotalMatched = ps->ObjectsMatched;
				}
				else if (pi->IsDone == 0)
				{
					pi->TotalProcessed = ps->ObjectsProcessed;
					pi->TotalMatched = ps->ObjectsMatched;
				}

				/*
				 *	If we are not browsing all of the root containers, AND...
				 *
				 *		(we have processed all of a container's CDS objects) OR
				 *		(we're done with the current page and we don't need an accurate page count)...
				 *
				 *	then we need to stop processing more CDS objects.
				 */
				if (
					(
						(ps->ObjectsProcessed >= (*results)->TotalMatches) ||
						(0 == (*results)->TotalMatches) ||
						(0 == (*results)->NumberReturned)
					) ||
					(
						(fb->PageInfo.ForceAccuratePageCount == 0) &&
						(resultsLimitReached != 0)
					)
					)
				{
					stopProcessing = 1;
				}

				ILibLinkedList_UnLock((*results)->LinkedList);
			}
		}

		sem_post(&(fb->Lock));

		/*
		 *	At this point, we need to figure out we're going to do next.
		 *	Choices include...
		 *		-destroy the "internal" FilteringBrowser because the context is stale
		 *		-make another Browse request to continue doing work for the current context
		 *		-finalize data in the indexer and execute results callback
		 */

		if (fb->Flag_Destroy != 0)
		{
			/* we're being told to destroy this FilteringBrowser */
			_FB_DestroyFilteringBrowser(fb);
			fb = NULL;
		}
		else
		{
			if (pi->IsDone == 0)
			{
				//if (fb->PageCountCallback != NULL)
				//{
				//	fb->PageCountCallback(fb->Wrapper);
				//}
				_FB_PagesCountedEx1(fb);
			}

			if (
					(
						/* we don't expect any more results from another browse */
						(stopProcessing == 1)
					) ||
					(
						/* IDF#6b: repeat 2-5 as needed */
						/* we might have another browse request to issue; if not, we'll get nonzero*/
						(_FB_BrowseNextStep(fb) != 0)	
					)
					)
			{
				/* 
				 *	We're done processing, so inform the app that results are ready 
				 *	unless we've already evented.
				 */
				sem_wait(&(fb->Lock));
				
				fb->Flag_Processing = 0;

				if (
					(pi->IsDone == 0)
					)
				{
					/*
					 *	The indexer has not been finalized
					 *	so go ahead and sort it and mark
					 *	it done. This will allow subsequent
					 *	page-turning operations to
					 *	optimize their browse requests.
					 */
					_FB_SortIndexer(pi);
					pi->IsDone = 1;
				}

				sem_post(&(fb->Lock));
				
				if (((*results) != NULL) && (resultsLimitReached == 0))
				{
					_FB_ReportResultsEx1(0, fb);
				}
			}
		}
	}
}

void _FB_FreeServer(struct FB_Server *server)
{
	/* free all of the data associated with struct FB_Server */
	free (server);
}

void _FB_ClearTree(void **tree, FB_Fn_FreeData freeMethod)
{
	void *e;
	char *key; 
	int keyLen;
	void *list;
	void *data;

	/* build a list where we store the keys temporarily */
	list = ILibQueue_Create();

	/* lock and get an enumerator for the browsers */
	ILibHashTree_Lock((*tree));
	e = ILibHashTree_GetEnumerator((*tree));

	if (e != NULL)
	{
		while (ILibHashTree_MoveNext(e) == 0)
		{
			/*
			*	get the key and store it in our list.
			*	Remember that key is a direct pointer into
			*	a string value in the hashtree.
			*/
			ILibHashTree_GetValue(e, &key, &keyLen, &data);
			ILibQueue_EnQueue(list, key);
		}

		/*
		*	destroy the enumerator - 
		*	need to do this before we start removing stuff
		*	from the tree. 
		*/
		ILibHashTree_DestroyEnumerator(e);
		e = NULL;
	}

	/*
	 *	We can enumerate through our list of keys
	 *	and remove stuff. Note that the tree is
	 *	still locked.
	 */
	while (ILibQueue_IsEmpty(list) == 0)
	{
		/* get the key and remove the entry */
		key = (char*) ILibQueue_DeQueue(list);
		keyLen = (int) strlen(key);
		data = ILibGetEntry((*tree), key, keyLen);
		ILibDeleteEntry((*tree), key, keyLen);

		/* if we have a method to free the data, then call it */
		if (freeMethod != NULL)
		{
			freeMethod(data);
		}
	}

	/*
	 *	At this point, we have nothing in the queue and we should
	 *	have nothing in the tree, so delete both 
	 */
	ILibHashTree_UnLock((*tree));
	ILibDestroyHashTree((*tree));
	(*tree) = NULL;
	ILibQueue_Destroy(list);
}

void _FB_OnDestroy(void *fbManagerObj)
{
	struct FB_FilteringBrowserManager *fbm = FB_TheManager;

	if (fbManagerObj == fbm)
	{
		/* set to FB_TheManager to NULL, but save fbm */
		FB_TheManager = NULL;
		FB_TheChain = NULL;

		/*
		 *	free everything on fbm
		 */

		if (fbm->Requests != NULL)
		{
			/* requests are a pointer reference to a FilteringBrowser object */
			_FB_ClearTree(&(fbm->Requests), NULL);
		}

		if (fbm->Browsers != NULL)
		{
			_FB_ClearTree(&(fbm->Browsers), (FB_Fn_FreeData)_FB_FreeFilteringBrowser);
		}

		if (fbm->Wrappers != NULL)
		{
			_FB_ClearTree(&(fbm->Wrappers), (FB_Fn_FreeData)_FB_FreeFilteringBrowserWrapper);
		}

		if (fbm->Servers != NULL)
		{
			_FB_ClearTree(&(fbm->Servers), (FB_Fn_FreeData)_FB_FreeServer);
		}

		if (fbm->UDNs != NULL)
		{
			/* UDN values are a pointer reference a UPnPDevice->UDN string */
			_FB_ClearTree(&(fbm->UDNs), NULL);
		}

		if (fbm->RootContainers != NULL)
		{
			/* UDN values are a pointer reference a UPnPDevice->UDN string */
			_FB_ClearTree(&(fbm->RootContainers), (FB_Fn_FreeData)CDS_ObjRef_Release);
		}

		/*
		 *	Intentionally do not call MSCP_ObjRef_Release() on FB_AllRoots because it's static.
		 *	This will leave outstanding references to FB_AllRoots, but that's quite ok.
		 */
		

		sem_destroy(&(_FB_AllRoots.CpInfo.Reserved.ReservedLock));
	}
	/* don't call free on fbManagerObj */
}

void _FB_OnResult_BrowseEx2(void *serviceObj, struct MSCP_BrowseArgs *args, int errorCode, struct MSCP_ResultsList *results)
{
	char key[MAX_KEY_LEN];
	int keyLen;
	struct FB_FilteringBrowser *fb;
	
	if (args->BrowseFlag == MSCP_BrowseFlag_Metadata)
	{
		/*
		 *	Browsing metadata means we're browsing a root container.
		 */
		if (results != NULL)
		{
			_FB_ProcessRootContainer(serviceObj, args, errorCode, &(results));
		}
	}
	else
	{
		/*
		 *	Otherwise, we're browsing children of a container.
		 */


		/* build a key for ->Requests - ignore compiler warning*/
		#ifdef WIN32
		#pragma warning( disable : 4311)
		#endif
		keyLen = sprintf(key, "%p", args);
		#ifdef WIN32
		#pragma warning( default : 4311)
		#endif
		key[keyLen] = '\0';

		/*
		 *	Find the FilteringBrowser object and
		 *	call its processing function to process
		 *	the requests.
		 */

		if ((FB_TheManager != NULL) && (FB_TheManager->Requests != NULL))
		{
			/*
			 *	Get the FilteringBrowser object, and remove it from the tree.
			 *
			 *	Things in FB_TheManager->Requests are keyed with the
	 		 *	memory address of 'args' - the value stored is a
			 *	FilteringBrowser.
			 */
			ILibHashTree_Lock(FB_TheManager->Requests);
			fb = ILibGetEntry(FB_TheManager->Requests, key, keyLen);
			if (fb != NULL)
			{
				ILibDeleteEntry(FB_TheManager->Requests, key, keyLen);

				/* set this to NULL because we will free 'args' instead */
				fb->PendingRequest = NULL;
			}
			ILibHashTree_UnLock(FB_TheManager->Requests);

			if (fb != NULL)
			{
				_FB_ProcessBrowseResults(fb, serviceObj, args, errorCode, &(results));
			}
		}
	}

	/*
	 *	Destroy the results and clean up the request.
	 *	None of the request's fields use dynamically allocated memory.
	 */
	MSCP_DestroyResultsList(results);
	free (args);
}

struct _FB_BrowseCS
{
	void *ServiceObj;
	struct MSCP_BrowseArgs *Args;
	int ErrorCode;
	struct MSCP_ResultsList *Results;
};

void _FB_OnResult_BrowseEx1(ILibThreadPool sender, void *var)
{
	struct _FB_BrowseCS *csd = (struct _FB_BrowseCS*) var;
	_FB_OnResult_BrowseEx2(csd->ServiceObj, csd->Args, csd->ErrorCode, csd->Results);
	free (csd);
}

void _FB_OnResult_Browse(void *serviceObj, struct MSCP_BrowseArgs *args, int errorCode, struct MSCP_ResultsList *results)
{
	struct _FB_BrowseCS *csd = (struct _FB_BrowseCS*) malloc(sizeof(struct _FB_BrowseCS));
	csd->ServiceObj = serviceObj;
	csd->Args = args;
	csd->ErrorCode = errorCode;
	csd->Results = results;
	ILibThreadPool_QueueUserWorkItem(FB_TheManager->ThreadPool, csd, _FB_OnResult_BrowseEx1);
}

void _FB_OnServerAddedRemoved(struct UPnPDevice *device, int addedFlag)
{
	char *key, *udn; 
	char key2[MAX_KEY_LEN];
	int keyLen, keyLen2;
	struct FB_Server *server = NULL;
	int udnLen = 0;
	struct CdsObject *obj = NULL;

	if (FB_TheManager != NULL)
	{
		if ((device != NULL) && (device->UDN != NULL))
		{
			/* use UDN as key */
			key = device->UDN;
			keyLen = (int) strlen(key);

			/* synchronize access */
			ILibHashTree_Lock(FB_TheManager->Servers);
			ILibHashTree_Lock(FB_TheManager->UDNs);

			/* check to see if something is there already */
			server = ILibGetEntry(FB_TheManager->Servers, key, keyLen);
			if (server != NULL)
			{
				/*
				 *	Something's there...
				 *	Remove the server from ->Servers and ->UDNs 
				 *	and ->RootContainers so that we can update 
				 *	it with more recent information.
				 */

				/* build key for ->UDNs tree */
				#ifdef WIN32
				#pragma warning( disable : 4311)
				#endif
				keyLen2 = sprintf(key2, "%p", server->Service);
				#ifdef WIN32
				#pragma warning( default : 4311)
				#endif
				key2[keyLen2] = '\0';

				udn = (char*) ILibGetEntry(FB_TheManager->UDNs, key2, keyLen2);
				if (udn != NULL)
				{
					udnLen = (int) strlen(udn);

					/* remove the root container */
					ILibHashTree_Lock(FB_TheManager->RootContainers);
					obj = (struct CdsObject*) ILibGetEntry(FB_TheManager->RootContainers, udn, udnLen);
					if (obj != NULL)
					{
						CDS_ObjRef_Release(obj);
						ILibDeleteEntry(FB_TheManager->RootContainers, udn, udnLen);
					}
					ILibHashTree_UnLock(FB_TheManager->RootContainers);

					/* remove UDN from ->UDNs */
					ILibDeleteEntry(FB_TheManager->UDNs, key2, keyLen2);
				}
			
				if(addedFlag==0 && FB_TheManager->ServerListCallback != NULL)
				{
					ILibHashTree_UnLock(FB_TheManager->Servers);
					ILibHashTree_UnLock(FB_TheManager->UDNs);
					FB_TheManager->ServerListCallback(1, udn);
					ILibHashTree_Lock(FB_TheManager->Servers);
					ILibHashTree_Lock(FB_TheManager->UDNs);
				}
				
				/* remove server info from ->Servers */
				ILibDeleteEntry(FB_TheManager->Servers, key, keyLen);
				_FB_FreeServer(server);
			}

			if (addedFlag != 0)
			{
				/* add the server */
				server = (struct FB_Server*) malloc (sizeof(struct FB_Server));
				memset(server, 0, sizeof(struct FB_Server));
				server->Device = device;
				server->Service = MSCP_GetService_ContentDirectory(device);
				ILibAddEntry(FB_TheManager->Servers, key, keyLen, server);
				
				/* build key for ->UDNs tree */
				#ifdef WIN32
				#pragma warning( disable : 4311)
				#endif
				keyLen2 = sprintf(key2, "%p", server->Service);
				#ifdef WIN32
				#pragma warning( default : 4311)
				#endif
				key2[keyLen2] = '\0';				
				ILibAddEntry(FB_TheManager->UDNs, key2, keyLen2, server->Device->UDN);
			}

			/* unlock trees */
			ILibHashTree_UnLock(FB_TheManager->UDNs);
			ILibHashTree_UnLock(FB_TheManager->Servers);

			if (addedFlag != 0)
			{
				_FB_BrowseRootContainer(server);
			}
		}
	}
}

struct FB_ResultsInfo* _FB_CreateBlankResults()
{
	struct FB_ResultsInfo* retVal = (struct FB_ResultsInfo*) malloc(sizeof(struct FB_ResultsInfo));
	memset(retVal, 0, sizeof(struct FB_ResultsInfo));
	retVal->LinkedList = ILibLinkedList_Create();
	return retVal;
}

void* _FB_CreateFilteringBrowser(FB_Fn_ResultsUpdated resultsUpdated, FB_Fn_PagesCounted pagesCounted)
{
	struct FB_FilteringBrowser *fb = NULL;
	char key[MAX_KEY_LEN];
	int keyLen;

	/* instantiate the FilteringBrowser */
	fb = (struct FB_FilteringBrowser*) malloc (sizeof(struct FB_FilteringBrowser));
	memset(fb, 0, sizeof(struct FB_FilteringBrowser));
	fb->ResultsCallback = resultsUpdated;
	fb->PageCountCallback = pagesCounted;
	sem_init(&(fb->Lock), 0, 1);

	/* initialize page info */
	memset(&(fb->PageInfo), 0, sizeof(struct FB_PageInfo));
	fb->PageInfo.PageSize = FB_DEFAULT_PAGE_SIZE;

	/* initialize processing state */
	memset(&(fb->ProcessingState), 0, sizeof(struct FB_ProcessingState));

	/* initialize processing state for page turning*/
	memset(&(fb->ProcessingState_Page), 0, sizeof(struct FB_ProcessingState));

	/* initialize context */
	fb->CurrentContext = ILibLinkedList_Create();
	_FB_ContextPush(fb->CurrentContext, &_FB_AllRoots);

	/* initialize blank results */
	fb->Results = _FB_CreateBlankResults();

	/* build a key - ignore compiler warning*/
	#ifdef WIN32
	#pragma warning( disable : 4311)
	#endif
	keyLen = sprintf(key, "%p", fb);
	#ifdef WIN32
	#pragma warning( default : 4311)
	#endif
	key[keyLen] = '\0';

	/* add the new FilteringBrowser */

	ILibHashTree_Lock(FB_TheManager->Browsers);
	ILibAddEntry(FB_TheManager->Browsers, key, keyLen, fb);
	ILibHashTree_UnLock(FB_TheManager->Browsers);

	return fb;
}

void* _FB_CopyResetFilteringBrowser(struct FB_FilteringBrowser *fb)
{
	struct FB_FilteringBrowser *newFB = NULL;

	/* create a new FilteringBroser */
	newFB = _FB_CreateFilteringBrowser(fb->ResultsCallback, fb->PageCountCallback);

	/*
	 *	Copy the applicable portions from fb to newFB.
	 *	Remember to deep copy things in lists.
	 *	Note that fb->Lock must be acquired.
	 */

	/* 
	 *	Copy the current context from fb to newFB.
	 *	Be sure to pop existing context first because
	 *	_FB_CreateFilteringBrowser() will push _FB_AllRoots.
	 */
	 _FB_ContextPop(newFB->CurrentContext);
	_FB_CopyLinkedList2(newFB->CurrentContext, fb->CurrentContext);

	/* Copy the forwards context from fb to newFB. */
	if (fb->ForwardContexts != NULL)
	{
		if (newFB->ForwardContexts == NULL)
		{
			newFB->ForwardContexts = ILibLinkedList_Create();
		}

		_FB_CopyLinkedList2(newFB->ForwardContexts, fb->ForwardContexts);
	}

	/* copy the desired pagination */
	memcpy(&(newFB->PageInfo), &(fb->PageInfo), sizeof(struct FB_PageInfo));

	/*
	 *	Have newFB have the same parent as the original.
	 *	Remember to add a reference. No need to call MSCP_ObjRef_Add()
	 *	because _FB_CopyLinkedList2() will call it when filling
	 *	adding to fb->CurrentContext.
	 */
	newFB->Parent = fb->Parent;

	/* copy the server */
	newFB->Server = fb->Server;

	/* copy the indexer, because this may simply be a pageturning request */
	memcpy(&(newFB->Indexer), &(fb->Indexer), sizeof(struct FB_Indexer));

	newFB->ProcessingState.UpdateID = fb->ProcessingState.UpdateID;

	return newFB;
}

void _FB_PreventProcessingError(struct FB_Object *fbw)
{
	struct FB_FilteringBrowser *newFB = NULL;
	struct FB_FilteringBrowser *fb = NULL;

	/*
	 *	Obtain the FilteringBrowser from the FilteringBrowserWrapper.
	 */
	if ((fbw != NULL) && (fbw->FB != NULL))
	{
		sem_wait(&(fbw->SyncLock));
		fb = fbw->FB;
		sem_wait(&(fb->Lock));

		if (
			(fb->Flag_Processing != 0) ||
			(fb->PendingRequest != NULL)
			)
		{
			/*
			 *	If the FilteringBrowser is in the middle of processing results,
			 *	then flag it to be destroyed by _FB_ProcessBrowseResults().
			 *	Then follow up by creating a new FilteringBrowser with
			 *	a similar copy of the existing FilteringBrowser.
			 */

			fb->Flag_Destroy = 1;
			newFB = _FB_CopyResetFilteringBrowser(fb);
		}
		
		sem_post(&(fb->Lock));

		if (newFB != NULL)
		{
			/*
			 *	if we have a new FilteringBrowser, then apply
			 *	it to the wrapper so that the refresh
			 *	operation will be done on the new one.
			 */
			fbw->FB = newFB;
			newFB->Wrapper = fbw;
		}

		sem_post(&(fbw->SyncLock));
	}
}

struct FB_Server* _FB_GetServer(char* udn)
{
	struct FB_Server* server;
	ILibHashTree_Lock(FB_TheManager->Servers);
	server = (struct FB_Server*)ILibGetEntry(FB_TheManager->Servers, udn, (int)strlen(udn));
	ILibHashTree_UnLock(FB_TheManager->Servers);

	return server;
}

struct UPnPDevice* FB_GetDevice(char *udn)
{
	struct FB_Server* server;
	ILibHashTree_Lock(FB_TheManager->Servers);
	server = (struct FB_Server*)ILibGetEntry(FB_TheManager->Servers, udn, (int)strlen(udn));
	ILibHashTree_UnLock(FB_TheManager->Servers);

	if(server!=NULL)
	{
		return(server->Device);
	}
	else
	{
		return(NULL);
	}
}

FB_Object FB_CreateFilteringBrowser(void *chain, ILibThreadPool thread_pool, FB_Fn_ResultsUpdated resultsUpdated, FB_Fn_ServerListUpdated serverListChanged, FB_Fn_PagesCounted pagesCounted)
{
	struct FB_Object *fbw = NULL;
	char key[MAX_KEY_LEN];
	int keyLen;

	if(FB_TheManager != NULL)
	{
		/* synchronize access the global objects, this is added to eliminate re-entrance problems*/

		sem_wait(&(FB_TheManager->Lock));

		if (FB_TheChain == NULL)
		{
			if (chain != NULL)
			{
				/* initialize control point communications */
				FB_TheManager->ControlPointMicroStack = MSCP_Init(chain, _FB_OnResult_Browse, _FB_OnServerAddedRemoved);

				/* we're done initializing stuff shared between FilteringBrowsers */
				FB_TheChain = chain;
				FB_TheManager->ThreadPool = thread_pool;

				FB_TheManager->ServerListCallback = serverListChanged;

				/*
				*	Add the manager to the chain.
				*/
				ILibAddToChain(chain, FB_TheManager);

				sem_init(&(_FB_AllRoots.CpInfo.Reserved.ReservedLock), 0, 1);

				CDS_ObjRef_Add(&(_FB_AllRoots));
			}
		}

		if ((FB_TheChain != NULL) && (FB_TheManager != NULL))
		{

			/* create a FB_Object - add it to the ->Wrappers */
			fbw = (struct FB_Object*) malloc(sizeof(struct FB_Object));
			memset(fbw, 0, sizeof(struct FB_Object));
			fbw->FB = _FB_CreateFilteringBrowser(resultsUpdated, pagesCounted);
			fbw->ControlPointMicroStack = FB_TheManager->ControlPointMicroStack;
			fbw->FB->Wrapper = fbw;
			sem_init(&(fbw->SyncLock), 0, 1);

			/* make a key using the memory address */
			/* build a key - ignore compiler warning*/
			#ifdef WIN32
			#pragma warning( disable : 4311)
			#endif
			keyLen = sprintf(key, "%p", fbw);
			#ifdef WIN32
			#pragma warning( default : 4311)
			#endif
			key[keyLen] = '\0';
			ILibHashTree_Lock(FB_TheManager->Wrappers);
			ILibAddEntry(FB_TheManager->Wrappers, key, keyLen, fbw);
			ILibHashTree_UnLock(FB_TheManager->Wrappers);
		}

		sem_post(&(FB_TheManager->Lock));		

	}

	return fbw;
}


void FB_DestroyFilteringBrowser(FB_Object fbObj)
{
	struct FB_Object *fbw = fbObj;
	int destroyImmediate = 0;
	char key[MAX_KEY_LEN];
	int keyLen;

	if (fbw != NULL)
	{
		sem_wait(&(fbw->SyncLock));

		if (fbw->FB != NULL)
		{
			/*
			 *	It's safe to lock fbw->FB->Lock because
			 *	fbw->FB can't be reassigned while we hold fbw->SyncLock
			 *	and because the FilteringBrowser is supposed to always release fb->Lock
			 *	before executing the FB_Fn_ResultsUpdated callback.
			 */

			sem_wait(&(fbw->FB->Lock));
			if (fbw->FB->Flag_Processing != 0)
			{
				/*
				 *	If the FilteringBrowser is busy processing results,
				 *	then simply mark the object to be destroyed later.
				 *
				 *	We are assured that it will be destroyed because
				 *	_FB_ProcessBrowseResults() is the onlymethod that
				 *	assigns ->Flag_Processing. 
				 *
				 *	If this call is executing
				 *	on the same thread that is running processing the results,
				 *	then we definitely don't have a problem because this call
				 *	is certainly executing through a callback.
				 *
				 *	If this call is executing on a different thread that is 
				 *	processing results, then we know that this thread is
				 *	executing during a time period when _FB_ProcessBrowseResults()
				 *	has released ->Lock to execute the ->ResultsCallback callback.
				 *	When that callback finishes and attempts to reacquire 
				 *	->Lock, it must waint until after we assign ->Flag_Destroy=1.
				 *
				 *	It is impossible for _FB_ProcessBrowseResults() to "miss"
				 *	this flag because ->Flag_Processing is changed only when
				 *	_FB_ProcessBrowseResults() has acquired ->Lock.
				 */
				fbw->FB->Flag_Destroy = 1;
			}
			else
			{
				/*
				 *	If the FilteringBrowser is not processing
				 *	stuff then go ahead and destroy it immediately.
				 */
				destroyImmediate = 1;
			}
			sem_post(&(fbw->FB->Lock));

			if (destroyImmediate != 0)
			{
				_FB_DestroyFilteringBrowser(fbw->FB);
				fbw->FB = NULL;
			}
		}

		/* make a key using the memory address */
		/* build a key - ignore compiler warning*/
		#ifdef WIN32
		#pragma warning( disable : 4311)
		#endif
		keyLen = sprintf(key, "%p", fbw);
		#ifdef WIN32
		#pragma warning( default : 4311)
		#endif
		key[keyLen] = '\0';
		ILibHashTree_Lock(FB_TheManager->Wrappers);
		ILibDeleteEntry(FB_TheManager->Wrappers, key, keyLen);
		ILibHashTree_UnLock(FB_TheManager->Wrappers);
		sem_post(&(fbw->SyncLock));

		_FB_FreeFilteringBrowserWrapper(fbw);
	}
}

void FB_DestroyResults(struct FB_ResultsInfo* results)
{
	void *llnode;
	struct CdsObject *obj;

	if ((results != NULL) && (results->LinkedList != NULL))
	{
		/*
		 *	Lock the list, iterate through it
		 *	and release all of the CDS objects.
		 *	Afterwards, unlock and destroy the list.
		 */

		ILibLinkedList_Lock(results->LinkedList);
		llnode = ILibLinkedList_GetNode_Head(results->LinkedList);
		while (llnode != NULL)
		{
			obj = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);
			if (obj != NULL)
			{
				CDS_ObjRef_Release(obj);
			}
			llnode = ILibLinkedList_GetNextNode(llnode);
		}
		ILibLinkedList_UnLock(results->LinkedList);
		ILibLinkedList_Destroy(results->LinkedList);
	}
	
	free (results);
}

/* IDF#1d: refresh */
void FB_Refresh(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	struct FB_Server *server = NULL;
	void *e;
	void *data;
	char *key;
	int keyLen;
	struct CdsObject *c = NULL;

	/* prevent reentrancy errors */
	_FB_PreventProcessingError(fbw);
	fb = fbw->FB;

	if ((fb != NULL) && (FB_TheManager != NULL))
	{
		/* clear the processing state*/
		_FB_ClearProcessingState(fb);

		/*
		 *	Lock and clear the existing forward contexts.
		 */
		sem_wait(&(fb->Lock));
		if (fb->ForwardContexts != NULL)
		{
			c = (struct CdsObject*) _FB_LinkedListDeQueue(fb->ForwardContexts);
			while (c != NULL)
			{
				CDS_ObjRef_Release(c);
				c = (struct CdsObject*) _FB_LinkedListDeQueue(fb->ForwardContexts);
			}
			ILibLinkedList_Destroy(fb->ForwardContexts);
			fb->ForwardContexts = NULL;
		}
		server = fb->Server;
		sem_post(&(fb->Lock));

		/*
		 *	Reset the processing state and clear
		 *	the results that we have.
		 */

		sem_wait(&(fb->Lock));
		/* destroy results and allocate memory for new results*/
		FB_DestroyResults(fb->Results);
		fb->Results = _FB_CreateBlankResults();

		if ((server == NULL) && (FB_TheManager->Servers != NULL))
		{
			/*
			 *	No target MediaServer was specified,
			 *	so this means that we obtain the root containers
			 *	for each MediaServer.
			 */
			ILibHashTree_Lock(FB_TheManager->Servers);


			/*
			 *	Enumerate the list of servers and add them to our 
			 *	a queue for processing.
			 */
			e = ILibHashTree_GetEnumerator(FB_TheManager->Servers);
			while (ILibHashTree_MoveNext(e) == 0)
			{
				/*
				 *	Remember that key is a direct pointer into the hashtree 
				 *	so we don't have to free it.
				 */
				ILibHashTree_GetValue(e, &key, &keyLen, &(data));

				/*
				 *	We have to make a copy of the key because
				 *	because a server from FB_TheManager->Servers might
				 *	be removed before we're done. When we actually
				 *	process the servers, we do it by looking up the
				 *	server using its key.
				 */
			}
			ILibHashTree_DestroyEnumerator(e);
			
			/* unlock stuff */
			ILibHashTree_UnLock(FB_TheManager->Servers);
		}

		sem_post(&(fb->Lock));

		if (FB_ContextIsAllRoots(fb->Wrapper) != 0)
		{
			/*
			 *	Copy known root containers into fb->Results
			 *	and execute results callback.
			 */

			fb->ForwardContexts = ILibLinkedList_Create();
			ILibHashTree_Lock(FB_TheManager->RootContainers);
			ILibLinkedList_Lock(fb->ForwardContexts);
			ILibLinkedList_Lock(fb->Results->LinkedList);
			
			e = ILibHashTree_GetEnumerator(FB_TheManager->RootContainers);
			if (e != NULL)
			{
				while (ILibHashTree_MoveNext(e) == 0)
				{
					ILibHashTree_GetValue(e, &key, &keyLen, &(data));
					if (data != NULL)
					{
						CDS_ObjRef_Add((struct CdsObject*)data);
						CDS_ObjRef_Add((struct CdsObject*)data);

						ILibLinkedList_AddTail(fb->Results->LinkedList, data);
						ILibLinkedList_AddTail(fb->ForwardContexts, data);
					}
				}
			}

			ILibLinkedList_UnLock(fb->Results->LinkedList);
			ILibLinkedList_UnLock(fb->ForwardContexts);
			ILibHashTree_UnLock(FB_TheManager->RootContainers);

			_FB_ReportResultsEx1(1, fb);
			//if (fb->PageCountCallback != NULL)
			//{
			//	fb->PageCountCallback(fb->Wrapper);
			//}
			_FB_PagesCountedEx1(fb);

			free(e);
		}
		else
		{
			/*
			 *	Tell the FilteringBrowser to go off and
			 *	browse the target MediaServer.
			 */
			_FB_BrowseNextStep(fb);
		}
	}
}

/* IDF#1b: Set target CDS container */
enum FB_SetContextErrors FB_SetContext(FB_Object fbObj, struct CdsObject *cdsContainer)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	char key[MAX_KEY_LEN];
	int keyLen;
	char *udn;
	int serverChanged = 0;
	enum FB_SetContextErrors retVal = FB_SCE_Precondition;
	struct FB_Server *serverInfo = NULL;
	struct CdsObject *c = NULL, *p = NULL;
	int targetIsAncestor = 0, targetIsForward = 0;
	void *llnode, *llnode2;
	void *fcnode;

	/* prevent reentrancy errors */
	_FB_PreventProcessingError(fbw);
	fb = fbw->FB;

	if (
		(fbObj != NULL) && 
		(cdsContainer != NULL) &&
		(cdsContainer->ID != NULL) &&
		(
			(cdsContainer->CpInfo.Reserved.ServiceObject != NULL) ||
			// Todo: set control point service object to cpinfo service object?
			((strcmp(cdsContainer->ID, FB_AllRoots->ID)==0) && (cdsContainer->CpInfo.Reserved.ServiceObject == FB_AllRoots->CpInfo.Reserved.ServiceObject))
		)
		)
	{
		{
			/*
			 *	Ensure that the desired MediaServer is still valid.
			 *	Even though we have a pointer to the UPnP service,
			 *	we need to make sure that the memory is still valid.
			 */
			retVal = FB_SCE_ServerNotExist;

			if (cdsContainer->CpInfo.Reserved.ServiceObject != NULL)
			{
				/* build a key for ->UDNs - ignore compiler warning*/
				#ifdef WIN32
				#pragma warning( disable : 4311)
				#endif
				keyLen = sprintf(key, "%p", cdsContainer->CpInfo.Reserved.ServiceObject);
				#ifdef WIN32
				#pragma warning( default : 4311)
				#endif
				key[keyLen] = '\0';

				ILibHashTree_Lock(FB_TheManager->UDNs);
				udn = (char*) ILibGetEntry(FB_TheManager->UDNs, key, keyLen);
				ILibHashTree_UnLock(FB_TheManager->UDNs);
			}
			else
			{
				/* setting context to FB_AllRoots */
				udn = NULL;
			}

			if (udn != NULL)
			{
				sem_wait(&(fb->Lock));

				ILibHashTree_Lock(FB_TheManager->Servers);
				serverInfo = (struct FB_Server*) ILibGetEntry(FB_TheManager->Servers, udn, (int)strlen(udn));

				if (serverInfo != NULL)
				{
					/*
					 *	MediaServer is still valid, let's see if we're changing Servers.
					 *	Be sure to set fb->Server so that
					 *	the FilteringBrowser knows it's browsing a particular server.
					 */
					if (fb->Server != serverInfo)
					{
						serverChanged = 1;
						fb->Server = serverInfo;
					}

					if ((serverChanged != 0) && (strcmp(cdsContainer->ID, "0") != 0))
					{
						/*
						 *	If the server changed, then container context must be the
						 *	root container to start with.
						 */
						retVal = FB_SCE_InvalidContext;
					}
					else 
					{
						retVal = FB_SCE_InvalidContext;

						/*
						 *	Check to see if the new container is
						 *	somewhere in the existing stack context.
						 *	If this is true, then pop ->CurrentContext until
						 *	the top CDS object matches the specified container.
						 *
						 *	Since fb->Lock is acquired, we can modify ->CurrentContext.
						 */

						/*
						 *	Iterate from the tail (bottom) of the stack to
						 *	the head (top) of the stack to see if the target
						 *	container is an ancestor of our current container
						 *	context. If it is an ancestor, remove the other
						 *	containers from the stack (starting from the head).
						 */
						p = NULL;
						targetIsAncestor = 0;
						ILibLinkedList_Lock(fb->CurrentContext);
						llnode = ILibLinkedList_GetNode_Tail(fb->CurrentContext);
						while (llnode != NULL)
						{
							/*
							 *	Get the container from the linked list node.
							 *	Check to see if it matches our target.
							 *	If so, note that the target is an ancestor.
							 */
							c = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);
							if (c != NULL)
							{
								if (targetIsAncestor == 0)
								{
									if (
										(c == cdsContainer) || 
										(
											(strcmp(c->ID, cdsContainer->ID) == 0) &&
											(c->CpInfo.Reserved.ServiceObject == cdsContainer->CpInfo.Reserved.ServiceObject)
										)
										)
									{
										targetIsAncestor = 1;
									}
									else
									{
										/*
										 *	Note the new parent container.
										 *	p will only be valid if targetIsAncestory!=0.
										 */
										p = c;
									}
								}
								else
								{
									/*
									 *	We know that the target is an ancestor,
									 *	so go ahead and remove the remaining
									 *	containers from the linked list.
									 */

									/* we have to release because _FB_Push() does AddRef */
									CDS_ObjRef_Release(c);

									/* get a pointer to the next node (lower in the stack) */
									llnode2 = ILibLinkedList_GetNextNode(llnode);

									/* remove the current node */
									ILibLinkedList_Remove(llnode);

									/* make llnode point to the previous node */
									llnode = llnode2;
								}
							}

							/* move up the stack - be sure to check for non-NULL because of llnode=llnode2 */
							if (llnode != NULL)
							{
								llnode = ILibLinkedList_GetPreviousNode(llnode);
							}
						}
						ILibLinkedList_UnLock(fb->CurrentContext);
						
						if (targetIsAncestor == 0)
						{
							/*
							 *	Check to see if the new container is
							 *	somewhere in the list of child containers.
							 *	If switching to a forward context, then push
							 *	the CDS object onto ->CurrentContext.
							 */

							if (fb->ForwardContexts != NULL)
							{
								ILibLinkedList_Lock(fb->ForwardContexts);
								fcnode = ILibLinkedList_GetNode_Head(fb->ForwardContexts);
								while (fcnode != NULL)
								{
									c = (struct CdsObject*) ILibLinkedList_GetDataFromNode(fcnode);
									if (c != NULL)
									{
										/*
										 *	We have a match if the target container is
										 *	the same as one of the instances in the ->ForwardContexts
										 *	list or if the objectID/MediaServer match.
										 */
										if (
											(c == cdsContainer) || 
											(
												(strcmp(c->ID, cdsContainer->ID) == 0) &&
												(c->CpInfo.Reserved.ServiceObject == cdsContainer->CpInfo.Reserved.ServiceObject)
											)
											)
										{
											targetIsForward = 1;

											/* note the new parent container and push the new context*/
											ILibLinkedList_Lock(fb->CurrentContext);
											p = _FB_ContextPeek(fb->CurrentContext);
											_FB_ContextPush(fb->CurrentContext, c);
											ILibLinkedList_UnLock(fb->CurrentContext);
											break;
										}
									}
									fcnode = ILibLinkedList_GetNextNode(fcnode);
								}
								ILibLinkedList_UnLock(fb->ForwardContexts);
							}
						}

						if (
							(p != NULL) &&
							((targetIsAncestor != 0) || (targetIsForward != 0))
							)
						{
							/*
							 *	Apply the new parent context.
							 *	As a note, we will clear the avaialble 
							 *	forward contexts in FB_Refresh().
							 */
							fb->Parent = p;

							/*
							 *	Indicate no errors.
							 *	We'll refresh after we release fb->Lock.
							 */
							retVal = FB_SCE_None;

							/*
							 *	reset to page 0
							 */
							fb->PageInfo.Page = 0;
						}
					}
				}
				ILibHashTree_UnLock(FB_TheManager->Servers);

				sem_post(&(fb->Lock));
			}

			else
			{
				/*
				 *	Setting the context to FB_AllRoots.
				 *	Pop the context to the bottom element and refresh.
				 */
				sem_wait(&(fb->Lock));
				ILibLinkedList_Lock(fb->CurrentContext);

				while (ILibLinkedList_GetCount(fb->CurrentContext) > 1)
				{
					c = _FB_ContextPop(fb->CurrentContext);
				}

				//#ifdef _DEBUG
				//c = _FB_ContextPeek(fb->CurrentContext);
				//if ((strcmp(c->ID, cdsContainer->ID)==0) && (c->CpInfo.Reserved.ServiceObject == cdsContainer->ServiceObject))
				//{
				//	fprintf(stderr, "ERROR: #1599. Expecting FB_AllRoots.\r\n");
				//}
				//#endif

				fb->Parent = NULL;
				fb->Server = NULL;
				fb->PageInfo.Page = 0;
				retVal = FB_SCE_None;

				ILibLinkedList_UnLock(fb->CurrentContext);
				sem_post(&(fb->Lock));
			}
		}
	}
	else
	{
		/* precondition error */
		retVal = FB_SCE_Precondition;
	}

	if (retVal == FB_SCE_None)
	{
		FB_Refresh(fb->Wrapper);
	}

	return retVal;
}

struct FB_ResultsInfo* FB_GetResults(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	struct FB_ResultsInfo* retVal = NULL;
	void *llnode;
	struct CdsObject *obj, *clone;

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}

	if ((fb != NULL) && (FB_TheChain != NULL) && (FB_TheManager != NULL))
	{
		/* clone all of the objects and return through retVal */
		sem_wait(&(fb->Lock));

		if (fb->Results != NULL)
		{
			ILibLinkedList_Lock(fb->Results->LinkedList);

			retVal = (struct FB_ResultsInfo*) malloc(sizeof(struct FB_ResultsInfo));
			memcpy(retVal, fb->Results, sizeof(struct FB_ResultsInfo));
			retVal->LinkedList = ILibLinkedList_Create();

			/* clone each of the results */
			llnode = ILibLinkedList_GetNode_Head(fb->Results->LinkedList);
			while (llnode)
			{
				obj = (struct CdsObject*) ILibLinkedList_GetDataFromNode(llnode);
				if (obj != NULL)
				{
					clone = CDS_CloneMediaObject(obj);
					ILibLinkedList_AddTail(retVal->LinkedList, clone);
				}
				llnode = ILibLinkedList_GetNextNode(llnode);
			}

			ILibLinkedList_UnLock(fb->Results->LinkedList);
		}

		sem_post(&(fb->Lock));
	}

	return retVal;
}

struct CdsObject* FB_GetContext_Parent(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	struct CdsObject *retVal = NULL;

	/*
	 *	Return a copy of the parent context.
	 */

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}

	if ((fb != NULL) && (fb->Parent != NULL))
	{
		sem_wait(&(fb->Lock));
		retVal = CDS_CloneMediaObject(fb->Parent);
		sem_post(&(fb->Lock));
	}

	return retVal;
}

struct CdsObject* FB_GetContext_ParentPtr(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	struct CdsObject *retVal = NULL;

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}

	if (fb != NULL)
	{
		retVal = fb->Parent;
	}

	return retVal;
}

/* IDF#1a: Set page info */
void FB_SetPageInfo (FB_Object fbObj, const struct FB_PageInfo *page)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	
	/* prevent reentrancy errors */
	_FB_PreventProcessingError(fbw);
	fb = fbw->FB;

	if (fb != NULL)
	{
		sem_wait(&(fb->Lock));
		memcpy(&(fb->PageInfo), page, sizeof(struct FB_PageInfo));
		sem_post(&(fb->Lock));
	}
}

void* FB_GetContext_EntireAsLinkedListPtr(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	void *retVal = NULL;

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}
	if (fb != NULL)
	{
		retVal = fb->CurrentContext;
	}

	return retVal;
}

void* FB_GetContext_EntireAsLinkedListCopy(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	void *retVal = NULL;

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}
	if ((fb != NULL) && (fb->CurrentContext != NULL))
	{
		sem_wait(&(fb->Lock));
		/* get a copy of the linked list */
		retVal = _FB_CopyLinkedList(fb->CurrentContext);
		sem_post(&(fb->Lock));
	}

	return retVal;
}

void FB_DestroyContext(void *context)
{
	if (context != NULL)
	{
		ILibLinkedList_Lock(context);
		while (ILibLinkedList_GetCount(context) > 0)
		{
			_FB_ContextPop(context);
		}
		ILibLinkedList_UnLock(context);
		ILibLinkedList_Destroy(context);
	}
}

int FB_ContextIsAllRoots(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	struct CdsObject *obj;
	int retVal = 0;

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}
	if (fb != NULL)
	{
		obj = _FB_ContextPeek(fb->CurrentContext);

		retVal = (
			(obj == FB_AllRoots) ||
			((strcmp(obj->ID, FB_AllRoots->ID)==0) && (obj->CpInfo.Reserved.ServiceObject == FB_AllRoots->CpInfo.ServiceObject))
			);
	}

	return retVal;
}

int FB_GetPageInfo(FB_Object fbObj, /*INOUT*/ struct FB_PageInfo *pi)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	int retVal = 0;

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}
	if (fb != NULL)
	{
		sem_wait(&(fb->Lock));
		memcpy(pi, &(fb->PageInfo), sizeof(struct FB_PageInfo));
		sem_post(&(fb->Lock));
		retVal = 1;
	}

	return retVal;
}

int FB_GetPageCount(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	int retVal = -1;
	int contextIsAllRoots = 0;

	if (fbw != NULL)
	{
		contextIsAllRoots = FB_ContextIsAllRoots(fbw);
		fb = fbw->FB;
	}
	if (fb != NULL)
	{
		sem_wait(&(fb->Lock));
		/* use ceil instead of floor() */
		if (contextIsAllRoots != 0)
		{
			retVal = 1;
		}
		else if (fb->PageInfo.ForceAccuratePageCount != 0)
		{
			/*
			 *	Possible page counting errors due to float precision errors on different platforms.
			 *	The original line is commented out and replaced by 2 different lines.
			 */
			//retVal = (unsigned int) ceil(((float) fb->Indexer.TotalMatched / (float)fb->PageInfo.PageSize));

			retVal = fb->Indexer.TotalMatched / fb->PageInfo.PageSize;
			retVal += (fb->Indexer.TotalMatched % fb->PageInfo.PageSize)? 1: 0;
		}
		else
		{
			if (fb->Results != NULL)
			{
				/*
				 *	Possible page counting errors due to float precision errors on different platforms.
				 *	The original line is commented out and replaced by 2 different lines.
				 */
				//retVal = (unsigned int) ceil(((float) fb->Results->TotalInContainer / (float)fb->PageInfo.PageSize));

				retVal = fb->Results->TotalInContainer / fb->PageInfo.PageSize;
				retVal += (fb->Results->TotalInContainer % fb->PageInfo.PageSize)? 1: 0;
			}
		}
		sem_post(&(fb->Lock));
	}

	return retVal;

}

struct CdsObject* FB_GetContext_Current(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	struct CdsObject *retVal = NULL, *obj = NULL;

	/*
	 *	Return a copy of the parent context.
	 */

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}

	if ((fb != NULL) && (fb->Parent != NULL))
	{
		sem_wait(&(fb->Lock));
		obj = _FB_ContextPeek(fb->CurrentContext);
		if (obj != NULL)
		{
			retVal = CDS_CloneMediaObject(obj);
		}
		sem_post(&(fb->Lock));
	}

	return retVal;
}

struct CdsObject* FB_GetContext_CurrentPtr(FB_Object fbObj)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;
	struct CdsObject *retVal = NULL;

	if (fbw != NULL)
	{
		fb = fbw->FB;
	}

	if ((fb != NULL) && (fb->Parent != NULL))
	{
		sem_wait(&(fb->Lock));
		retVal = _FB_ContextPeek(fb->CurrentContext);
		sem_post(&(fb->Lock));
	}

	return retVal;
}

void _FB_SetPage(struct FB_FilteringBrowser *fb, unsigned int targetPage)
{
	int i;
	struct FB_SnapShot restoreThisState;

	sem_wait(&(fb->Lock));

	/* mustdo: check for page violations */
	fb->PageInfo.Page = targetPage;

	if (fb->PageInfo.ExamineMethod != NULL)
	{
		/*
			*	The indexer has been finalized so
			*	go ahead and change the data in
			*	fb->ProcessingState. The idea is to 
			*	restore the FilteringBrowser to a state
			*	before it acquired the objects for the target page.
			*/
		
		/* find the most appropriate item index for the given target page */
		memset(&restoreThisState, 0, sizeof (struct FB_SnapShot));
		for (i=fb->Indexer.FirstHashIndex-1; i < FB_INDEXTABLE_SIZE; i++)
		{
			/* targetPage is zero-based */
			if (targetPage <= fb->Indexer.PageIndex[i])
			{
				restoreThisState = fb->Indexer.SnapShots[i];
				break;
			}
		}
		fb->ProcessingState_Page.ObjectsMatched = restoreThisState.Matched;
		fb->ProcessingState_Page.ObjectsProcessed = restoreThisState.Processed;
	}
	else
	{
		/*
			*	The app hasn't specified a method to match objects,
			*	so we know that all objects will match. Equivalently,
			*/
		fb->ProcessingState_Page.ObjectsProcessed =	fb->ProcessingState_Page.ObjectsMatched =
			targetPage * fb->PageInfo.PageSize;
	}

	/* destroy and reallocate for new results */
	FB_DestroyResults(fb->Results);
	fb->Results = _FB_CreateBlankResults();

	sem_post(&(fb->Lock));
	
	_FB_BrowsePage(fb);
}

/* IDF#1c: Set target page */
void FB_SetPage(FB_Object fbObj, unsigned int targetPage)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;

	/*
	 *	Obtain the FilteringBrowser from the FilteringBrowserWrapper.
	 */
	if ((fbw != NULL) && (fbw->FB != NULL))
	{
		sem_wait(&(fbw->SyncLock));	
		fb = fbw->FB;
		sem_wait(&(fb->Lock));
		if (
			(fb->Flag_Processing != 0) ||
			(fb->PendingRequest_Page != NULL)
			)
		{
			/*
			 *	If the FilteringBrowser is in the middle of page turning,
			 *	then clear its current page turning state
			 */
			memset(&(fb->ProcessingState_Page), 0, sizeof(struct FB_ProcessingState));
			free(fb->PendingRequest_Page);
		}
		
		sem_post(&(fb->Lock));

		_FB_SetPage(fb, targetPage);
		sem_post(&(fbw->SyncLock));
	}
}

/* IDF#1c: Set target page */
void FB_NextPreviousPage(FB_Object fbObj, int nextPrevious)
{
	struct FB_Object *fbw = (struct FB_Object*) fbObj;
	struct FB_FilteringBrowser *fb = NULL;

	int pageCount;

	fb = fbw->FB;

	pageCount = FB_GetPageCount (fbObj);

	if ((fb != NULL) && (nextPrevious != 0))
	{
		/* change the target page */
		if (nextPrevious < 0)
		{
			if (fb->PageInfo.Page > 0) {
				_FB_SetPage(fb, fb->PageInfo.Page-1);
			}
		}
		else
		{
			if ((fb->PageInfo.Page < (unsigned int)(pageCount - 1)) && (pageCount != 0)) {
				_FB_SetPage(fb, fb->PageInfo.Page+1);
			}
		}
	}
}
void FB_Init()
{
/*
 *	initialize the manager which will own the FilteringBrowsers 
 */
	if(FB_TheManager == NULL)
	{
		FB_TheManager = (struct FB_FilteringBrowserManager*) malloc (sizeof(struct FB_FilteringBrowserManager));
		memset(FB_TheManager, 0, sizeof(struct FB_FilteringBrowserManager));
		
		/* initialize hash trees*/
		FB_TheManager->Browsers = ILibInitHashTree();
		FB_TheManager->Wrappers = ILibInitHashTree();
		FB_TheManager->Servers = ILibInitHashTree();
		FB_TheManager->Requests = ILibInitHashTree();
		FB_TheManager->UDNs = ILibInitHashTree();
		FB_TheManager->RootContainers = ILibInitHashTree();

		/* setup clean up method */
		FB_TheManager->Destroy = _FB_OnDestroy;
		sem_init(&(FB_TheManager->Lock), 0, 1);
	}
}

void FB_NotifyIPAddressChange(FB_Object fbObj)
{
	if(fbObj != NULL)
	{
		struct FB_Object *fbw = (struct FB_Object*) fbObj;
		MSCP__CP_IPAddressListChanged(fbw->ControlPointMicroStack);
	}
}

void* FB_GetTag(FB_Object fbObj)
{
	if(fbObj != NULL)
	{
		struct FB_Object* fbw = (struct FB_Object*)fbObj;
		return fbw->Tag;
	}
	return NULL;
}

void FB_SetTag(FB_Object fbObj, void* tag)
{
	if(fbObj != NULL)
	{
		struct FB_Object* fbw = (struct FB_Object*)fbObj;
		fbw->Tag = tag;
	}
}
void *FB_GetCP()
{
	return(FB_TheManager->ControlPointMicroStack);
}
