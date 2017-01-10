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
 * $Workfile: FilteringBrowser.h
 *
 *
 *
 */


#ifndef FILTERINGBROWSER_H
#define FILTERINGBROWSER_H

#include "MediaServerCP_ControlPoint.h"
#include "limits.h"
#include "ILibThreadPool.h"
#include "CdsObject.h"

/*! \file FilteringBrowser.h
	\brief Provides the ability to browse a MediaServer.
*/

/*! \defgroup FilteringBrowser DLNA - Filtering Browser
	\brief This module allows browsing of a
	MediaServer's (e.g. DMS, M-DMS) content. Also works for DMS and M-DMS devices.
	
	\ref FilteringBrowser is built on top of the generated DeviceBuilder API.
	This module essentially provides a level of abstraction from 
	the UPnP communications, such that upper application layers
	can work with structs that represent content. 

	The \ref FilteringBrowser works in the following way.
	- <b>Browsing Contexts</b>
		- A \ref FilteringBrowser is designed to a browse 
			UPnP AV MediaServers (e.g. DMS, M-DMS devices).
		- A single \ref FilteringBrowser is capable of browsing
			one CDS container of a UPnP AV MediaServer at a time.
		- The CDS container that is being browsed is a <i>browsing context</i>.
		- A <i>browsing context</i> is more formally defined as the exact path
			to the CDS container that is being browsed. For example,
			<i>FB_AllRoots/Root Container of DMS/Container1</i> is an example
			of a complete browsing context.
	- <b>Results (Filtering & Pagination)</b>
		- When a \ref FilteringBrowser has a browsing context, it reports its results
			as a single <i>page</i> of <i>matched</i> (a.k.a. <i>post-filtered</i>)
			results.
		- <i>Matched/post-filtered</i> means that each CDS object (in the page of 
			results) has been matched against a filtering method that is
			assigned by the application layer.
		- Results are divided into <i>pages</i>. Each <i>page</i> contains only
			<i>matched/post-filtered</i> CDS objects.
		- Each page has a size. The page size is the maximum number of CDS objects
			that are associated with a single page of results.
		- A \ref FilteringBrowser has one page size for all of its results, although
			separate \ref FilteringBrowser instances can have different page sizes.
		- A \ref FilteringBrowser can change the page size of its results dynamically,
			although this requires the \ref FilteringBrowser to rebrowse the
			current context.
	- <b>Accurate vs Estimated Page Counting</b>
		- A \ref FilteringBrowser can be configured to estimate or accurately calculate the 
			number of pages for the current context+pagination+filtering configuration.
		- Estimating the number of pages uses the number of CDS objects in the container
			(which is obtained in every CDS:Browse request) divided by the page size.
		- Accurately counting the number of pages forces the \ref FilteringBrowser
			to count the number of CDS object in the container and divide by the page size.
	- <b>Browsing \ref FB_AllRoots</b>
		- The \ref FB_AllRoots container is a logical container that represents 
			the parent of all root containers of MediaServers.
	- <b> Setup of the FilteringBrowser </b>
		- Use \ref FB_CreateFilteringBrowser() to create a \ref FilteringBrowser
			instance.
		- After creating the \ref FilteringBrowser instance, applications use
			\ref FB_SetPageInfo() to specify the pagination/filtering configuration.
	- <b> Changing the browse context </b>
		- Use \ref FB_SetContext() to change the browsing context
			(i.e. the CDS container that is being browsed).
	- <b> Changing pages </b>
		- Use \ref FB_NextPreviousPage and \ref FB_SetPage to change
			target page.
	- <b> Accessing the results </b>
		- The callback for reporting results is excuted on a thread using the
			threadpool so that app layer doesn't have to worry
			about timing sensitivities. 
		executes on a thread that is 
			somewhat timing	sensitive. Control points monitor the
			SSDP multicast channel and using the thread too long (~3 seconds?)
			can cause an inconvenience of a missing a MediaServer
			advertisement or bye-bye message. 
			Generally, using the thread to update a GUI is fine and rarely
			causes problems, although having sufficient network buffers
			is usually limiting platform requirement.
		- To prevent the CDS objects in the results from being deallocated,
			the application layer will need to call \ref FB_GetResults() once,
			\ref CDS_CloneMediaObject() once for each CDS object, or 
			\ref CDS_ObjRef_Add() once for each CDS object.
			See \ref FB_Fn_ResultsUpdated for more information.

	\{
*/


/* forward declaration */
struct FB_ResultsInfo;

/*!	\brief Represents a \ref FilteringBrowser instance.
*/
typedef void* FB_Object;

/*!	\brief This method type is used when the \ref FilteringBrowser instance
	observes a change in the set of UPnP AV MediaServers that are available
	on the network.
*/
typedef void (*FB_Fn_ServerListUpdated) (int RemoveFlag, char * udn);

/*! \brief This method type is used when the \ref FilteringBrowser
	has results to report.

	When the \ref FilteringBrowser executes its callback for reporting
	results, the results actually represent the post-filtered set of
	CDS objects. Specifically, all of the CDS objects in the \a results
	argument have already been filtered through the 
	\ref FB_PageInfo::ExamineMethod (setting this to NULL means that
	all CDS objects are matched).

	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\param[in] results_page	A single page of matched/post-filtered CDS objects.
							
							Application access to \a results_page is thread-safe during
							execution of the callback because the callback will 
							execute on the microstack/control-point's thread.

							Applications that want to have thread-safe access the \a results_page
							after thread execution has returned to the \ref FilteringBrowser
							need to do one of the following things:
								-	call \ref FB_GetResults() to get a deep copy of each object
								-	call \ref CDS_CloneMediaObject() to get a deep copy of
									each objects of interest (in \a results_page)
								-	call \ref CDS_ObjRef_Add() to ensure that 
									objects of interest (in \a results_page) are not freed
							
							That being said, the only time the \ref FilteringBrowser will modify
							the memory associated with \a results_page will be if the application
							calls any of the methods that modify the state of the
							\ref FilteringBrowser. 
 */
typedef void (*FB_Fn_ResultsUpdated) (FB_Object fb, struct FB_ResultsInfo* results_page);

/*!	\brief This method type is used when the \ref FilteringBrowser
	is filtering CDS:Browse responses so that the application gets
	only the CDS objects of interest in the \ref FB_PageInfo::ExamineMethod
	callback.

	Returns nonzero if the \ref FilteringBrowser should include the CDS object
	in the <i>results_page</i> argument of the the \ref FB_PageInfo::ExamineMethod
	callback.

    This callback does not allow reentrant behavior with FilteringBrowser methods. 
	Your application WILL deadlock if it calls certain \ref FilteringBrowser
	(FB_) methods. The proper way to use this callback is to examine the 
	metadata on \a cds_obj and return a zero or nonzero value.

	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\param[in] cds_obj		The CDS object to analyze. Applications must 
							avoid access to the following fields.
*/
typedef int (*FB_Fn_ExamineObject) (FB_Object fb, struct CdsObject *cds_obj);

/*!	\brief This method type is used when the \ref FilteringBrowser
	reports an update to the number of results pages.

	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().
*/
typedef void (*FB_Fn_PagesCounted) (FB_Object fb);

/*!	\brief The default page size is the number of matched/post-filtered 
	CDS objects that are reported in the \ref FB_Fn_ResultsUpdated
	callback is executed. 

	<b>TODO:</b> You can change use this to specify the default page size.
 */
#define FB_DEFAULT_PAGE_SIZE 15

	
/*!	\brief This value is also the number of CDS objects that
	are requested in individual CDS:Browse requests. DLNA recommends a value of 15.
*/
#define FB_BROWSE_SIZE 15

/*!	\brief UINT_MAX is the maximum value that can be used for a page size.
 
	<b>TODO:</b> This is largely for app-developers to specify an
	upper bound for the number of CDS objects in a set of results,
	when they are using multiple pages of different sizes.
*/
#define FB_MAX_PAGE_SIZE UINT_MAX

/*!	\brief When the \ref FilteringBrowser rebrowses a CDS container
	(such as when changing the page number), the \ref FilteringBrowser
	will use an indexer to reduce the number of CDS:Browse requests
	needed to acquire the desired results.

	The indexer's size is bytes is roughly (\ref FB_INDEXTABLE_SIZE * 12). 
	- A value of 100 will result in behavior where subsequent page-turning 
		browse requests result with browsing in increments of 1% of the 
		number of CDS objects in the container.
	- A value of 10 will result with browsing in 10% increments
		of the number of CDS objects in the container.
	
	<b>TODO:</b> Change this value to increase the size of the indexer
	so that subsequent browse requests (resulting from a new target page) 
	results in fewer incremental browse requests. 
*/
#define FB_INDEXTABLE_SIZE 100

/*!	\brief These are the errors that can occur when attempting to set
	a new browsing context.
*/
enum FB_SetContextErrors
{
	/*!	\brief no error */
	FB_SCE_None				= 0,		

	/*! \brief precondition failed */
	FB_SCE_Precondition		= 1,		

	/*!	\brief MediaServer for this container is no longer valid */
	FB_SCE_ServerNotExist	= 2,		

	/* \brief specified container change is not allowed */
	FB_SCE_InvalidContext	= 3			
};

/*!	\brief Represents a UPnP AV MediaServer (e.g. DLNA DMS device).
	
	The struct identifies a UPnP MediaServer and the ContentDirectory
	Service that is associated with it.

	This struct is primarily used to identify which MediaServers
	are available on the network.
*/
struct FB_Server
{
	/*!	\brief represents the UPnP MediaServer */
	struct UPnPDevice *Device;

	/*!	\brief represents the ContentDirectory service associated with the 
		\ref FB_Server::Device
	*/
	struct UPnPService *Service;
};

/*!	\brief Represents the page information instructions that
	the \ref FilteringBrowser will use when reporting results
	to an application.
*/
struct FB_PageInfo
{
	/*!	\brief Results are viewed on pages. Each page has 
		this number of CdsObject objects.

		Use UINT_MAX to request all results on one page.
	*/
	unsigned int			PageSize;

	/*!	\brief Applications can scroll through the results
		by changing the page number. Value is zero-based.

		Consider this example:
		- a CDS container has 100 child CDS objects
		- if we filtered the entire container only 70 CDS objects are of interest
		- the final product only has a screen that can display 10 objects at once
		- If the the page size is 10, then the number of pages for the results is 7.
		- Page 0 has CDS objects [0,9], page 1 has CDS objects [10,19], ...
	*/
	unsigned int			Page;

	/*!	\brief Indicates if the \ref FilteringBrowser will
		calculate the number of total pages using an accurate
		formula or by using an estimation formula.

		- If this value is nonzero, then the \ref FilteringBrowser
			will process all objects in a container in order
			to acquire an accurate page count (from
			\ref FB_GetPageCount()).
		- If the value is zero, then the \ref FilteringBrowser 
			estimates the page count using the number of total
			objects in the counter. The advantage of this is
			that fewer browse requests are issued by the control point.
	*/
	unsigned char			ForceAccuratePageCount;

	/*!	\brief This is the method that will filter for CDS objects of interest
		for the application.

		The assigned method is VERY performance-sensitive. This method essentially
		executes O(<i>n</i>), where <i>n</i> is the number of CDS objects in the
		current browsing context (i.e. current CDS container that is being browsed).
	*/
	FB_Fn_ExamineObject		ExamineMethod;
};

/*!	\brief Represents a single page of results from the \ref FilteringBrowser.
*/
struct FB_ResultsInfo
{
	/*!	\brief The CDS objects that match the filtering and page requirements. */
	void *LinkedList;

	/*!	\brief The container's UpdateID, acquired from the CDS:Browse response. */
	unsigned int UpdateID;

	/*!	\brief The total number of CDS objects in the container, 
		acquired from the CDS:Browse response. 
	*/
	unsigned int TotalInContainer;

	/* \brief The last CDS object in the \ref FB_ResultsInfo::LinkedList set
		has this index in the current browsing context (i.e. the CDS container 
		that is being browsed.)
	*/
	unsigned int IndexIntoContainer;

	/*!	\brief A nonzero value indicates that an error occurred when browsing 
		the MediaServer. The error code maps to the UPnP AV error code.
	*/
	int ErrorCode;
};

/*!	\brief An instance of a logical CDS object that is parent
	to all root containers on the network.

	Setting the browsing context to this instance essentially
	tells the \ref FilteringBrowser to report all MediaServers
	on the network.

	A memcmp() or comparison by address will indicate if a 
	CdsObject object is actually the logical "all roots" container.
*/
extern const struct CdsObject *FB_AllRoots;

/*!	\brief Initialize all the neccessary resources for creating FilterBrowser instance.

	You will need to call this method only once at the begining of your application.  After this, you
	can call FB_CreateFilteringBrowser() to create multiple filtering browsers.  
*/
void FB_Init();

/*!	\brief Creates a \ref FilteringBrowser instance.

	You will need to call ILibStartChain() after calling this method
	to activate the browsing behavior of the \ref FilteringBrowser.

	\param[in] chain			Obtained from ILibCreateChain(). 
	
								You can call \ref FB_CreateFilteringBrowser() multiple times,
								but each \ref FilteringBrowser instance will share a 
								common instance of the Microstack object. 
								Therefore, subsequent calls should have \a chain ==NULL. 
								
								Please note that calls to \ref FB_CreateFilteringBrowser() 
								supports re-entrance. So that you can call this function on different
								threads.
	
	\param[in] thread_pool		Ensures that callbacks do not execute on the timing-sensitive
								Microstack thread, which is used for UPnP communications.

								You can call \ref FB_CreateFilteringBrowser() multiple times,
								but each \ref FilteringBrowser instance will share a 
								common instance of the threadpool object. 
								Therefore, subsequent calls should have \a thread_pool ==NULL. 

	\param[in] results_updated	This callback is executed whenever the \ref FilteringBrowser's list of
								matched/post-filtered CDS objects changes. 

								In other words, the \ref FilteringBrowser keeps a 
								a single page of results in memory, and this callback
								executes when that page of results changes.
 
	\param[in] server_list_changed
								This callback is executed whenever the Microstack
								detects a change in the list of UPnP AV MediaServer
								devices on the network. The callback can only be set once,
								given a set of \ref FilteringBrowser instances in
								a single application process.
 
	\param[in] pages_counted	This callback is executed whenever the 
								\ref FilteringBrowser determines the number of result pages
								for the current \ref FB_PageInfo and browsing context.

	\returns The new \ref FilteringBrowser instance.
*/
FB_Object FB_CreateFilteringBrowser(void *chain, ILibThreadPool thread_pool, FB_Fn_ResultsUpdated results_updated, FB_Fn_ServerListUpdated server_list_changed, FB_Fn_PagesCounted pages_counted);

/*!	\brief Destroy a \ref FilteringBrowser instance. 

	If you do not call this, then the module will automatically 
	clean up the \ref FilteringBrowser instances when ILibStopChain() is called.
	
	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().
*/
void FB_DestroyFilteringBrowser(FB_Object fb);

/*!	\brief Destroys a page of results.

	\param[in] result_page	The page of results to free, obtained from 
							\ref FB_GetResults().
*/
void FB_DestroyResults(struct FB_ResultsInfo* result_page);

/*!	\brief Refreshes the state of the \ref FilteringBrowser instance.

	This method modifies the state of the \ref FilteringBrowser instance,
	so do not call this method (on the same thread) when executing the 
	\ref FB_Fn_ExamineObject callback.
*/
void FB_Refresh(FB_Object fb);

/*!	\brief Sets the \ref FilteringBrowser instance's context to the 
	specified CDS container.

	This method will call \ref FB_Refresh(),
	so do not call this method (on the same thread) when executing the 
	\ref FB_Fn_ExamineObject callback.
 
	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().
							
	\param[in] cds_container 
							The CDS container to browse.
							Use (\ref CdsObject::MediaClass BITWISE-AND \ref CDS_CLASS_MASK_CONTAINER)
							to determine if the CDS object is a CDS container.

	\returns A success or failure indication.
 */
enum FB_SetContextErrors FB_SetContext(FB_Object fb, struct CdsObject *cds_container);

/*!	\brief Get a deep-copy of \ref FilteringBrowser instance's current page of results.

	If used, this method is usually called in the callback handler for
	\ref FB_Fn_ResultsUpdated. 
	Use \ref FB_DestroyResults() to free the associated memory. 

	This method clones each CDS object in the current results page. This can be
	memory intensive, but it is a mechanism to actually keep the entire set
	of CDS objects and the results information together. 
	
	Applications can use \ref CDS_ObjRef_Add() to increment a reference counter
	on individual CDS objects. This is generally sufficient for most applications
	when they only want to prevent CDS objects from being deallocated until
	the application releases its reference.
	
	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns A deep-copy of the current page of results. 
*/
struct FB_ResultsInfo* FB_GetResults(FB_Object fb);

/*!	\brief Returns a copy of the container context. 
	
	Must call \ref CDS_ObjRef_Release() on the returned object.

	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns a copy of the current container context
*/
struct CdsObject* FB_GetContext_Current(FB_Object fb);

/*! \brief Returns a reference to the container context. 

	Applications are supposed to call \ref CDS_ObjRef_Add()
	and \ref CDS_ObjRef_Release() on the returned object
	to ensure that the instance is not destroyed
	while the app has an oustanding reference.

	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns a pointer reference to the current browsing/container context. 
*/
struct CdsObject* FB_GetContext_CurrentPtr(FB_Object fb);

/*!	\brief Returns a copy of the parent container context. 

	Must call \ref CDS_ObjRef_Release() on the returned object.
	
	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns a copy of the parent CDS container (of the current browsing context). 
*/
struct CdsObject* FB_GetContext_Parent(FB_Object fb);

/*!	\brief Returns a reference of the parent container context. 

	Applications are recommended to call \ref CDS_ObjRef_Add()
	and \ref CDS_ObjRef_Release() on the returned object
	to ensure that the instance is not destroyed
	while the app has an oustanding reference.
	
	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns a pointer reference to the parent CDS container (of the current browsing context). 
*/
struct CdsObject* FB_GetContext_ParentPtr(FB_Object fb);

/*!	\brief Instructs the \ref FilteringBrowser instance 
	to find CDS objects according to these
	pagination and filtering rules.
	
	Applications get notifications from the method specified in 
	the <i>page->ResultsCallback</i>.
 
	Applications must call \ref FB_Refresh() in order for the 
	\ref FilteringBrowser instance to rebrowse and acquire its new state.

	\param[in] fb			The FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\param[in] page			Specifies the filtering and pagination 
							requirements for the desired page of results.
*/
void FB_SetPageInfo (FB_Object fb, const struct FB_PageInfo *page);

/*!	\brief Returns a pointer to the linked list that represents the
	context of the \ref FilteringBrowser.
 
	The tail of the linked list
	represents the bottom, which should be a pointer to \ref FB_AllRoots.
	The head of the linked list represents the top of the context
	stack, equivalently the current container that is being browsed.

	Do not use ILibLinkedList_Destroy(), FB_DestroyContext(), or 
	any ILibLinkedList_xxx method that modifies the 
	returned linked list because the application doesn't own the memory. 
	Applications are encouraged to use ILibLinkedList_Lock() and 
	ILibLinkedList_UnLock() for thread-safe access to the 
	returned context. 

	\param[in] fb			The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns A pointer to memory created by ILibLinkedList_Create().
*/
void* FB_GetContext_EntireAsLinkedListPtr(FB_Object fb);

/*!	\brief Returns a copy of the linked list that represents the
	context of the \ref FilteringBrowser. 
	
	The tail of the linked list
	represents the bottom, which should be a pointer to \ref FB_AllRoots.
	The head of the linked list represents the top of the context
	stack, equivalently the current container that is being browsed.

	Applications must use \ref FB_DestroyContext() to free the memory.

	\param[in] fb			The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns A copy of a linked list that represents the current browsing context.
*/
void* FB_GetContext_EntireAsLinkedListCopy(FB_Object fb);

/*!	\brief Destroys the specified context.

	\param[in] context		As obtained from \ref FB_GetContext_EntireAsLinkedListCopy().
 */
void FB_DestroyContext(void *context);

/*!	\brief Indicates if the current context is at the \ref FB_AllRoots.

	\param[in] fb			The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\returns A nonzero value if the \ref FilteringBrowser instance is browsing \ref FB_AllRoots.
*/
int FB_ContextIsAllRoots(FB_Object fb);

/*!	\brief Acquires the pagination information of the \ref FilteringBrowser instance.

	The method populates \a pi with the current pagination info.

	\param[in] fb			The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\param[in,out] pi			An allocated \ref FB_PageInfo where the 
							pagination information is copied.

	\returns A nonzero value if successful.
*/
int FB_GetPageInfo(FB_Object fb, /*INOUT*/ struct FB_PageInfo *pi);

/*!	\brief Acquires the number of results pages for the current
	browsing context.

	The returned value is affected by whether or not the 
	\ref FB_PageInfo::ForceAccuratePageCount field is set to true.

	\param[in] fb			The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().
	
	\returns An error or the number of pages.
		- Returns negative value if an error occurred.
		- Returns non-negative to indicate the number of pages associated
			with the current results set.
 */
int FB_GetPageCount(FB_Object fb);

/*!	\brief Explicitly set the target page for the \ref FilteringBrowser instance.

	Applications can use this method 
	(instead of \ref FB_SetPageInfo() and FB_Refresh())
	to change the current page of the results. 
	
	The advantage of using
	this method is that the \ref FilteringBrowser will not rebrowse the
	entire container. Instead, it will browse only what is necessary.

	\param[in] fb			The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\param[in] target_page	The target result page, 0-based value.
*/
void FB_SetPage(FB_Object fb, unsigned int target_page);

/*!	\brief Instruct the \ref FilteringBrowser instance to browse to the
	next or previous page of results.

	Applications can use this method 
	(instead of \ref FB_SetPageInfo() and \ref FB_Refresh())
	to change the current page of the results.
 
	The advantage of using this method is that the \ref FilteringBrowser 
	will not rebrowse the entire container. Instead, it will browse 
	only what is necessary.

	\param[in] fb			The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

	\param[in] next_prev	Indicates next or previous page.
								- Use a negative value indicates to go to the previous page.
								- Use a positive value indicates to go to the next page.
*/
void FB_NextPreviousPage(FB_Object fb, int next_prev);

/*!	\brief Notifiy the underlying UPnP control point stack that the IP address is changed. It will trigger
	it to send M-SEARCH out to scan the network for all devices.

	\param[in] fbObj		The \ref FilteringBrowser object, acquired from
							\ref FB_CreateFilteringBrowser().

*/
void FB_NotifyIPAddressChange(FB_Object fbObj);

/*! \brief Given a specified UDN return the associated UPnPDevice structure.
	\param[in] udn The UDN to return the UPnPDevice structure for.
	\returns The UPnPDevice structure or NULL.
*/
struct UPnPDevice* FB_GetDevice(char *udn);

/*! \brief Gets the tag accociated with the user created FB instance.   
	\param[in] fbObj The FB instance returned by the 
					 \ref FB_CreateFilteringBrowser function.
	\returns The user tag value or NULL is not set.
	\warning This \b not the tag used in FB_*Ex functions!  That tag is a session tag!
*/
void* FB_GetTag(FB_Object fbObj);

/*! \brief Sets the tag accociated with the user created FB instance.   
	\param[in] fbObj The FB instance returned by the 
					 \ref FB_CreateFilteringBrowser function.
	\param[in] tag The user defined tag associated with the filtering browser instance.
	\warning This \b not the tag used in FB_*Ex functions!  That tag is a session tag!
*/
void FB_SetTag(FB_Object fbObj, void* tag);

void *FB_GetCP();

/*! \} */

#endif //FILTERINGBROWSER_H

