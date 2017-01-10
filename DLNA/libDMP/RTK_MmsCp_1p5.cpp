#include <stdio.h>
#include <string.h>

//#include "MSCP_ControlPoint.h"
#include "ILibParsers.h"
#include "MediaServerCP_ControlPoint.h"
#include "RTK_MmsCp.h"
//#include "MediaServerControlPoint.h"
#include "MyString.h"
#include <assert.h>

#include <netinet/in.h>

#define strnicmp strncasecmp
#define stricmp strcasecmp

#define UNSUPPORTED_BY_CP printf("Action is not supported by this implementation.\n"); ASSERT(1);

#define MMSCP_DEVICE_ADDED 1
#define MMSCP_DEVICE_REMOVED 0

#define MMSCP_BROWSE_FLAG_METADATA_STRING "BrowseMetadata"
#define MMSCP_BROWSE_FLAG_CHILDREN_STRING "BrowseDirectChildren"

/* CDS normative tag names and attributes */
#define MMSCP_ATTRIB_ID				"id"
#define MMSCP_ATTRIB_PARENTID		"parentID"
#define MMSCP_ATTRIB_REFID			"refID"
#define MMSCP_ATTRIB_RESTRICTED		"restricted"
#define MMSCP_ATTRIB_SEARCHABLE		"searchable"
#define MMSCP_ATTRIB_PROTOCOLINFO	"protocolInfo"

#define MMSCP_ATTRIB_SAMPLEFREQUENCY "sampleFrequency"
#define MMSCP_ATTRIB_BITSPERSAMPLE  "bitsPerSample"
#define MMSCP_ATTRIB_PROTECTION     "protection"

#define MMSCP_ATTRIB_RESOLUTION		"resolution"
#define MMSCP_ATTRIB_DURATION		"duration"
#define MMSCP_ATTRIB_BITRATE		"bitrate"
#define MMSCP_ATTRIB_COLORDEPTH		"colorDepth"
#define MMSCP_ATTRIB_SIZE			"size"

#define MMSCP_TAG_DIDL				"DIDL-Lite"
#define MMSCP_TAG_CONTAINER			"container"
#define MMSCP_TAG_ITEM				"item"
#define MMSCP_TAG_RESOURCE			"res"

#define MMSCP_TAG_CREATOR			"dc:creator"
#define MMSCP_TAG_TITLE				"dc:title"
#define MMSCP_TAG_MEDIACLASS		"upnp:class"
#define MMSCP_TAG_GENRE				"upnp:genre"
#define MMSCP_TAG_ALBUM				"upnp:album"

#define MMSCP_TAG_ARTIST			"upnp:artist"
#define MMSCP_TAG_DATE				"dc:date"

#ifdef _DEBUG
#define	ASSERT(x) assert (x)
#define MMSCP_MALLOC(x) malloc(x)
//MMSCP_malloc(x)
#define MMSCP_FREE(x) free(x)
//MMSCP_free(x)
#define DEBUGONLY(x) x
#else
#ifndef UNDER_CE
#define ASSERT(x)
#endif
#define MMSCP_MALLOC(x) malloc(x)
#define MMSCP_FREE(x) free(x)
#define DEBUGONLY(x)
#endif

#ifdef _TEMPDEBUG
#define TEMPDEBUGONLY(x) x
#else
#define TEMPDEBUGONLY(x) 
#endif

/***********************************************************************************************************************
 *	BEGIN: MMSCP static values
 ***********************************************************************************************************************/

/*
 *	The relative order of strings within these arrays must correspond to the MMSCP_CLASS_MASK_xxx bitmask mappings.
 */
const char* MMSCP_CLASS_OBJECT_TYPE[] = {"object", "item", "container"};
const char* MMSCP_CLASS_MAJOR_TYPE[] = {"", "imageItem", "audioItem", "videoItem", "playlistItem", "textItem", "person", "playlistContainer", "album", "genre", "storageSystem", "storageVolume", "storageFolder"};
const char* MMSCP_CLASS_MINOR_TYPE[] = {"", "photo", "musicTrack", "audioBroadcast", "audioBook", "movie", "videoBroadcast", "musicVideClip", "musicArtist", "musicAlbum", "photoAlbum", "musicGenre", "movieGenre"};

#define MMSCP_CLASS_FIRST_MAJOR_TYPE	MMSCP_CLASS_MASK_MAJOR_IMAGEITEM
/*
 *	Maximum length of a string in 
 *	MMSCP_CLASS_OBJECT_TYPE, MMSCP_CLASS_MAJOR_TYPE, and MMSCP_CLASS_MINOR_TYPE.
 *	Size includes null terminator.
 */
#define MMSCP_MAX_CLASS_FRAGMENT_LEN	17
#define MMSCP_MAX_CLASS_FRAGMENT_SIZE	18

#define MIN(X, Y)  ((X) < (Y) ? (X) : (Y))

/*
 *	These are the types of strings that can be searched.
 */
enum MMSCP_SearchableStringTypes
{
	MMSCP_STRTYPE_ID,
	MMSCP_STRTYPE_CREATOR,
	MMSCP_STRTYPE_PROTOCOLINFO
};

const char* MMSCP_TRUE_STRINGS[] = {"1", "true", "yes"};
const char* MMSCP_FALSE_STRINGS[] = {"0", "false", "no"};
#define MMSCP_TRUE_STRINGS_LEN 3
#define MMSCP_FALSE_STRINGS_LEN 3

/***********************************************************************************************************************
 *	END: MMSCP static values
 ***********************************************************************************************************************/

//TODO, review!
/*! \brief Struct that encapsulated the browse args and callbacks.
 */
//typedef void (*MMSCP_Fn_Result_Browse) (void *serviceObj, struct MMSCP_BrowseArgs *args, int errorCode, struct MMSCP_ResultsList *results);
struct BrowseInfo
{
        struct MMSCP_BrowseArgs *args;
        MMSCP_Fn_Result_Browse callbackBrowse;
};



/***********************************************************************************************************************
 *	BEGIN: MMSCP state variables
 ***********************************************************************************************************************/

/* Function pointer for sending Browse results back to caller */
MMSCP_Fn_Result_Browse				MMSCP_Callback_Browse;
MMSCP_Fn_Device_AddRemove			MMSCP_Callback_DeviceAddRemove;

int MMSCP_malloc_counter = 0;

/***********************************************************************************************************************
 *	END: MMSCP state variables
 ***********************************************************************************************************************/




/***********************************************************************************************************************
 *	BEGIN: Helper methods
 ***********************************************************************************************************************/

/* TODO: debug malloc/MMSCP_FREE is not thread safe */

#ifdef _DEBUG
void* MMSCP_malloc(int sz)
{
	++MMSCP_malloc_counter;
	return((void*)malloc(sz));
}
void MMSCP_free(void* ptr)
{
	--MMSCP_malloc_counter;
	free(ptr);	
}
int MMSCP_malloc_GetCount()
{
	return(MMSCP_malloc_counter);
}

#endif

/*
 *	Copies bytes from copyFrom to copyHere.
 *	Will not copy more than copyMaxChars bytes.
 *	Stops copying when ., <, null, or " char is found.
 */
void MMSCP_CopyUntilClassFragmentTerminator(char *copyHere, const char *copyFrom, int copyMaxChars)
{
	int i;
	char c;
	
	for (i=0; i < copyMaxChars; i++)
	{
		c = copyFrom[i];
		
		if (c == '.' || c == '<' || c == '\0' || c == '"')
		{
			copyHere[i] = '\0';
			return;
		}
		else
		{
			copyHere[i] = c;
		}
	}
}

/*
 *	Given an array of strings, finds the index in that array with a matching string.
 */
int MMSCP_FindStringInArray(const char* str,const char** strarray,const int strarraylen)
{
	int i;
	for (i=0;i<strarraylen;i++) {if (stricmp(str,strarray[i]) == 0) {return i;}}
	return -1;
}


void MMSCP_StringFixup(char **fixThis, char** di, char *emptyStr, const char *data, const char *rangeStart, const char *rangeEnd)
{
	int len;

	if (data != NULL)
	{
		if ((rangeStart <= data) && (data <= rangeEnd))
		{
			/* store an XML-unescaped representation */

			*fixThis = *di;
			len = (int) strlen(data);
			memcpy(*di, data, len);

			ILibInPlaceXmlUnEscape(*di);

			*di = *di + len + 1;
		}
		else
		{
			*fixThis = (char*)data;
		}
	}
	else
	{
		*fixThis = emptyStr;
	}
}


int MMSCP_GetRequiredSizeForMediaObject(struct MMSCP_MediaObject *obj, struct MMSCP_MediaObject *obj2)
{
	int retVal;
	struct MMSCP_MediaResource *res;
	struct MMSCP_MediaResource *res2;
	unsigned char addProtInfo;
	unsigned char addResolution;
	struct MMSCP_MediaResource *resProt;
	struct MMSCP_MediaResource *resRes;

	retVal = 0;

	if (obj->ID != NULL)
	{
		retVal += ((int) strlen(obj->ID) +1);
	}

	if (obj->ParentID != NULL)
	{
		if ((obj2 != NULL) && (strcmp(obj2->ParentID, obj->ParentID) == 0))
		{
			obj->ParentID = obj2->ParentID;
		}
		else
		{
			retVal += ((int) strlen(obj->ParentID) +1);
		}
	}

	if (obj->RefID != NULL)
	{
		retVal += ((int) strlen(obj->RefID) +1);
	}

	if (obj->Title != NULL)
	{
		retVal += ((int) strlen(obj->Title) +1);
	}

	if (obj->Creator != NULL)
	{
		if ((obj2 != NULL) && (strcmp(obj2->Creator, obj->Creator) == 0))
		{
			obj->Creator = obj2->Creator;
		}
		else
		{
			retVal += ((int) strlen(obj->Creator) +1);
		}
	}

	if (obj->Genre != NULL)
	{
		if ((obj2 != NULL) && (strcmp(obj2->Genre, obj->Genre) == 0))
		{
			obj->Genre = obj2->Genre;
		}
		else
		{
			retVal += ((int) strlen(obj->Genre) +1);
		}
	}

	if (obj->Album != NULL)
	{
		if ((obj2 != NULL) && (strcmp(obj2->Album, obj->Album) == 0))
		{
			obj->Album = obj2->Album;
		}
		else
		{
			retVal += ((int) strlen(obj->Album) +1);
		}
	}

	if (obj->Artist != NULL)
	{
		if ((obj2 != NULL) && (strcmp(obj2->Artist, obj->Artist) == 0))
		{
			obj->Artist= obj2->Artist;
		}
		else
		{
			retVal += ((int) strlen(obj->Artist) +1);
		}
	}

	if (obj->Date != NULL)
	{
		if ((obj2 != NULL) && (strcmp(obj2->Date, obj->Date) == 0))
		{
			obj->Date = obj2->Date;
		}
		else
		{
			retVal += ((int) strlen(obj->Date) +1);
		}
	}

	res = obj->Res;
	res2 = NULL;
	while (res != NULL)
	{
		//if (res->ProtocolInfo != NULL)
		addProtInfo = (res->ProtocolInfo != NULL);
		addResolution = (res->Resolution != NULL);
		{
			if (obj2 != NULL)
			{
				res2 = obj2->Res;
			}

			resProt = NULL;
			resRes = NULL;
			while (res2 != NULL)
			{
				if (addProtInfo && (res2->ProtocolInfo != NULL) && (strcmp(res2->ProtocolInfo, res->ProtocolInfo) == 0))
				{
					addProtInfo = 0;
					resProt = res2;
				}
				if (addResolution && (res2->Resolution != NULL) && (strcmp(res2->Resolution, res->Resolution) == 0))
				{
					addResolution = 0;
					resRes = res2;
				}

				if ((addProtInfo != 0) || (addResolution != 0))
				{
					res2 = res2->Next;
				}
				else
				{
					res2 = NULL;
				}
			}

			if (addProtInfo != 0)
			{
				retVal += ((int) strlen(res->ProtocolInfo) +1);
			}
			else if (resProt != NULL)
			{
				res->ProtocolInfo = resProt->ProtocolInfo;
			}
			
			if (addResolution != 0)
			{
				retVal += ((int) strlen(res->Resolution) +1);
			}
			else if (resRes != NULL)
			{
				res->Resolution = resRes->Resolution;
			}
		}

		if (res->Uri != NULL)
		{
			retVal += ((int) strlen(res->Uri) +1);
		}

		res = res->Next;
	}

	return retVal;
}

#if 0 // HIWU
void MMSCP_RemoveQuotFromAttribValue(struct ILibXMLAttribute *att)
{
	/*if ((att->Value[0] == '"') || (att->Value[0] == '\''))
	{
		att->Value++;
		att->ValueLength -= 2;
	}*/
}
#endif // HIWU

struct MMSCP_MediaObject* MMSCP_CreateMediaObject(struct ILibXMLNode *node, struct ILibXMLAttribute *attribs, int isItem, const char *rangeStart, const char *rangeEnd)
{
	struct ILibXMLNode *startNode;
	struct ILibXMLAttribute *att;

	struct MMSCP_MediaObject tempObj;
	struct MMSCP_MediaObject* newObj;

	struct MMSCP_MediaResource *res = NULL;

	char *innerXml;
	int innerXmlLen;
	char classFragment[MMSCP_MAX_CLASS_FRAGMENT_SIZE];
	int indexIntoArray;

	int dataSize;
	int mallocSize;

	char *di;
	char *emptyDI;
	
	char c;
	int l;
	int i;
	char *index;
	char *start;
	char duration[3][16];

	#ifdef _DEBUG
	/* PRECONDITION: node is a start node*/
	if (node->StartTag == 0)
	{
		printf("MMSCP_CreateMediaObject requires node->StartTag!=0.\r\n");
		ASSERT(0);
	}
	
	/* PRECONDITION: node->Name is null terminated and this node is a container or item */
	if (!(
		(stricmp(node->Name, MMSCP_TAG_CONTAINER) == 0) ||
		(stricmp(node->Name, MMSCP_TAG_ITEM) == 0)
		))
	{
		printf("MMSCP_CreateMediaObject requires item or container node.\r\n");
		ASSERT(0);
	}
	#endif

	/* initialize temp obj to zero; init flags appropriately */
	memset(&tempObj, 0, sizeof(struct MMSCP_MediaObject));
	tempObj.Flags |= MMSCP_Flags_Restricted;	/* assume object is restricted */
	if (isItem == 0)
	{
		tempObj.Flags |= MMSCP_Flags_Searchable;/* assume container is searchable */
	}

	/*
	 *
	 *	Parse the item/container node and set the pointers in tempObj
	 *	to point into the memory referenced by node.
	 *
	 */

	/* Parse the attributes of the item/container */
	att = attribs;
	while (att != NULL)
	{
		/* [DONOTREPARSE] null terminate name and value. */
		att->Name[att->NameLength] = '\0';
		////MMSCP_RemoveQuotFromAttribValue(att); // HIWU
		att->Value[att->ValueLength] = '\0';
		if (stricmp(att->Name, MMSCP_ATTRIB_ID) == 0)
		{
			tempObj.ID = att->Value;
		}
		else if (stricmp(att->Name, MMSCP_ATTRIB_PARENTID) == 0)
		{
			tempObj.ParentID = att->Value;
		}
		else if (stricmp(att->Name, MMSCP_ATTRIB_RESTRICTED) == 0)
		{
			if (MMSCP_FindStringInArray(att->Value, MMSCP_TRUE_STRINGS, MMSCP_TRUE_STRINGS_LEN) >= 0)
			{
				/* set the restricted flag. */
				tempObj.Flags |= MMSCP_Flags_Restricted;
			}
			else
			{
				tempObj.Flags &= (~MMSCP_Flags_Restricted);
			}
		}
		else if ((isItem == 0) && (stricmp(att->Name, MMSCP_ATTRIB_SEARCHABLE) == 0))
		{
			if (MMSCP_FindStringInArray(att->Value, MMSCP_TRUE_STRINGS, MMSCP_TRUE_STRINGS_LEN) >= 0)
			{
				/* set the searchable flag. */
				tempObj.Flags |= MMSCP_Flags_Searchable;
			}
			else
			{
				tempObj.Flags &= (~MMSCP_Flags_Searchable);
			}
		}
		else if ((isItem != 0) && (stricmp(att->Name, MMSCP_ATTRIB_REFID) == 0))
		{
			tempObj.RefID = att->Value;
		}
		att = att->Next;
	}

	/*
	 *
	 *	Iterate through the child nodes of the startNode
	 *	and set the title, creator, and resources for
	 *	the media object.
	 *
	 */

	startNode = node;
	node = startNode->Next;
	while (node != startNode->ClosingTag)
	{
		/* [DONOTREPARSE] null terminate name */	
////		attribs = ILibGetXMLAttributes(node);	
////		att = attribs;
////		node->Name[node->NameLength] = '\0';

		if (node->StartTag != 0)
		{
		    /* [DONOTREPARSE] null terminate name */	
		    attribs = ILibGetXMLAttributes(node);	
		    att = attribs;
		    node->Name[node->NameLength] = '\0';		    
		    
			if (stricmp(node->Name, MMSCP_TAG_RESOURCE) == 0)
			{
				/*
				 *
				 *	Create a new resource element and add it
				 *	to the existing list of resources for the
				 *	media object. The resource will point to 
				 *	memory in XML, but we'll change where they
				 *	point at the very end.
				 *
				 */
				
				if (tempObj.Res == NULL)
				{
					tempObj.Res = (struct MMSCP_MediaResource*) MMSCP_MALLOC (sizeof(struct MMSCP_MediaResource));
					res = tempObj.Res;
				}
				else
				{
					res->Next = (struct MMSCP_MediaResource*) MMSCP_MALLOC (sizeof(struct MMSCP_MediaResource));
					res = res->Next;					
				}

				/* initialize everything to zero */
				memset(res, 0, sizeof(struct MMSCP_MediaResource));
				res->Duration = res->Bitrate = res->ColorDepth = res->Size = -1;

				/* Extract the protocolInfo from the element */
				while (att != NULL)
				{
					/* [DONOTREPARSE] */
					att->Name[att->NameLength] = '\0';
					////MMSCP_RemoveQuotFromAttribValue(att); // HIWU
					att->Value[att->ValueLength] = '\0';					
					if (stricmp(att->Name, MMSCP_ATTRIB_PROTOCOLINFO) == 0)
					{
						res->ProtocolInfo = (att->Value);							
						////break;
					}
					/* HIWU: add this 
                    else if (stricmp(att->Name, MMSCP_ATTRIB_SAMPLEFREQUENCY) == 0)
                    {
                        
                    }
                    else if (stricmp(att->Name, MMSCP_ATTRIB_BITSPERSAMPLE) == 0)
                    {
                    }            
                    else if (stricmp(att->Name, MMSCP_ATTRIB_PROTECTION) == 0)
                    {
                        
                    }
                    */					
					else if (stricmp(att->Name, MMSCP_ATTRIB_RESOLUTION) == 0)
					{
						res->Resolution = (att->Value);						
					}
					else if (stricmp(att->Name, MMSCP_ATTRIB_DURATION) == 0)
					{
						l = 0;
						start = att->Value;
						while((index = strstr(start, ":")) != NULL && l < 2)
						{
							strncpy(duration[l], start, (index-start));
							duration[l][index-start] = 0;
							start = index + strlen(":");
							l++;
						}
						strncpy(duration[l], start, strlen(start));
						duration[l][strlen(start)] = 0;
						l++;
						res->Duration = 0;
						if(l <= 3)
						{
							for(i = l-1; i >= 0; i-- )
							{
								switch((l-1)-i)
								{
									case 0:
										res->Duration += atoi(duration[i]);
										break;
									case 1:
										res->Duration += atoi(duration[i])*60;
										break;
									case 2:
										res->Duration += atoi(duration[i])*60*60;
										break;
									default:
										break;
								}
							}
						}
						else
							res->Duration = -1;
					}
					else if (stricmp(att->Name, MMSCP_ATTRIB_BITRATE) == 0)
					{
						ILibGetLong(att->Value, att->ValueLength, &(res->Bitrate));
					}
					else if (stricmp(att->Name, MMSCP_ATTRIB_COLORDEPTH) == 0)
					{
						ILibGetLong(att->Value, att->ValueLength, &(res->ColorDepth));
					}
					else if (stricmp(att->Name, MMSCP_ATTRIB_SIZE) == 0)
					{
						ILibGetLongLong(att->Value, att->ValueLength, &(res->Size));
					}				
					att = att->Next;
				}

				/* grab the URI */
				innerXmlLen = ILibReadInnerXML(node, &innerXml);
				innerXml[innerXmlLen] = '\0';
				res->Uri = innerXml;

			}
			else if ( node->NSTag && stricmp(node->NSTag, MMSCP_TAG_MEDIACLASS) == 0) // HIWU
			{
				/* Figure out proper enum value given the specified media class */
				innerXmlLen = ILibReadInnerXML(node, &innerXml);

				/* initialize to bad class */
				tempObj.MediaClass = MMSCP_CLASS_MASK_BADCLASS;
							
				/* determine object type */
				MMSCP_CopyUntilClassFragmentTerminator(classFragment, innerXml, MIN(innerXmlLen, MMSCP_MAX_CLASS_FRAGMENT_LEN));
				indexIntoArray = MMSCP_FindStringInArray(classFragment, MMSCP_CLASS_OBJECT_TYPE, MMSCP_CLASS_OBJECT_TYPE_LEN);

				if (indexIntoArray == 0)
				{
					innerXml += ((int) strlen(MMSCP_CLASS_OBJECT_TYPE[indexIntoArray]) + 1);
					MMSCP_CopyUntilClassFragmentTerminator(classFragment, innerXml, MIN(innerXmlLen, MMSCP_MAX_CLASS_FRAGMENT_LEN));
					indexIntoArray = MMSCP_FindStringInArray(classFragment, MMSCP_CLASS_OBJECT_TYPE, MMSCP_CLASS_OBJECT_TYPE_LEN);

					if (indexIntoArray > 0)
					{
						innerXml += ((int) strlen(MMSCP_CLASS_OBJECT_TYPE[indexIntoArray]) + 1);
						tempObj.MediaClass = indexIntoArray;
									
						/* Determine major type */
						MMSCP_CopyUntilClassFragmentTerminator(classFragment, innerXml, MIN(innerXmlLen, MMSCP_MAX_CLASS_FRAGMENT_LEN));
						indexIntoArray = MMSCP_FindStringInArray(classFragment, MMSCP_CLASS_MAJOR_TYPE, MMSCP_CLASS_MAJOR_TYPE_LEN);
						if (indexIntoArray > 0)
						{
							innerXml += ((int) strlen(MMSCP_CLASS_MAJOR_TYPE[indexIntoArray]) + 1);
							tempObj.MediaClass |= (indexIntoArray << MMSCP_SHIFT_MAJOR_TYPE);

							/* Determine minor type */
							MMSCP_CopyUntilClassFragmentTerminator(classFragment, innerXml, MIN(innerXmlLen, MMSCP_MAX_CLASS_FRAGMENT_LEN));
							indexIntoArray = MMSCP_FindStringInArray(classFragment, MMSCP_CLASS_MAJOR_TYPE, MMSCP_CLASS_MAJOR_TYPE_LEN);
							if (indexIntoArray > 0)
							{
								tempObj.MediaClass |= (indexIntoArray << MMSCP_SHIFT_MINOR1_TYPE);
								/* TODO : Add vendor-specific supported minor types parsing here */
							}
						}
					}
				}
			}
			else if (node->NSTag && stricmp(node->NSTag, MMSCP_TAG_CREATOR) == 0)
			{// HIWU
				innerXmlLen = ILibReadInnerXML(node, &innerXml);
				innerXml[innerXmlLen] = '\0';
				tempObj.Creator = innerXml;
			}
			else if (node->NSTag && stricmp(node->NSTag, MMSCP_TAG_TITLE) == 0)
			{// HIWU
				innerXmlLen = ILibReadInnerXML(node, &innerXml);
				innerXml[innerXmlLen] = '\0';
				tempObj.Title = innerXml;
			}
			else if (node->NSTag && stricmp(node->NSTag, MMSCP_TAG_GENRE) == 0)
			{// HIWU
				innerXmlLen = ILibReadInnerXML(node, &innerXml);
				innerXml[innerXmlLen] = '\0';
				tempObj.Genre = innerXml;
			}
			else if (node->NSTag && stricmp(node->NSTag, MMSCP_TAG_ALBUM) == 0)
			{// HIWU
				innerXmlLen = ILibReadInnerXML(node, &innerXml);
				innerXml[innerXmlLen] = '\0';
				tempObj.Album = innerXml;
			}	
			else if (node->NSTag && stricmp(node->NSTag, MMSCP_TAG_ARTIST) == 0)
			{// yuyu
				innerXmlLen = ILibReadInnerXML(node, &innerXml);
				innerXml[innerXmlLen] = '\0';
				tempObj.Artist = innerXml;
			}	
			else if (node->NSTag && stricmp(node->NSTag, MMSCP_TAG_DATE) == 0)
			{// yuyu
				innerXmlLen = ILibReadInnerXML(node, &innerXml);
				innerXml[innerXmlLen] = '\0';
				tempObj.Date = innerXml;
			}	

		/* free attribute mapping */
		ILibDestructXMLAttributeList(attribs); //HIWU			
	
		}

		node = node->Next;
		#ifdef _DEBUG
		if (node == NULL)
		{
			printf("MMSCP_CreateMediaObject: Unexpected null node.\r\n");
			ASSERT(0);
		}
		#endif
		/* free attribute mapping */
		////ILibDestructXMLAttributeList(attribs); //HIWU
	}

	/*
	 *
	 *	At this point, we have a temp media object and possibly some media resources.
	 *	All string data is simply a pointer into the XML string. In order to
	 *	maximize on efficient memory usage, we do the following.
	 *
	 *	1)	Determine size needed for all new strings in results set. Also note which strings need to be copied in this step.
	 *	2)	Create a new media object, with additional memory for storing new string data.
	 *	3)	Point new media object's fields to either the new memory or to existing memory from a previous media object.
	 *	4)	Connect new media object to resource objects (attached to temp)
	 *	5)	Point each field of each resource to memory in new memory to existing memory from a previous media object.
	 *
	 *
	 */
	 

	/*
	 *	Create the new media object, with additional memory for string data appended at the end.
	 */
	dataSize = MMSCP_GetRequiredSizeForMediaObject(&tempObj, NULL);
	mallocSize = dataSize + sizeof(struct MMSCP_MediaObject) + 1;
	newObj = (struct MMSCP_MediaObject*) MMSCP_MALLOC(mallocSize);
	memset(newObj, 0, mallocSize);
	newObj->MallocSize = mallocSize;
	newObj->RefCount = 0;
	sem_init(&(newObj->Lock), 0, 1);

	newObj->MediaClass = tempObj.MediaClass;
	newObj->Flags = tempObj.Flags;

	/* di will point to where it's safe to write string data */
	di = (char*)newObj;
	di += sizeof(struct MMSCP_MediaObject);
	emptyDI = di;
	di ++;

	MMSCP_StringFixup(&(newObj->ID),		&di, emptyDI, tempObj.ID,		rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->ParentID),	&di, emptyDI, tempObj.ParentID,	rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->RefID),		&di, emptyDI, tempObj.RefID,	rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->Title),		&di, emptyDI, tempObj.Title,	rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->Creator),	&di, emptyDI, tempObj.Creator,	rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->Genre),		&di, emptyDI, tempObj.Genre,	rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->Album),		&di, emptyDI, tempObj.Album,	rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->Artist),	&di, emptyDI, tempObj.Artist,	rangeStart, rangeEnd);
	MMSCP_StringFixup(&(newObj->Date),		&di, emptyDI, tempObj.Date,		rangeStart, rangeEnd);

	newObj->Res = tempObj.Res;
	res = newObj->Res;
	while (res != NULL)
	{
		/*
		 *	Since resources are already allocated, we send the same parameters
		 *	for arg1 and arg3.
		 */
		MMSCP_StringFixup(&(res->ProtocolInfo), &di, emptyDI, res->ProtocolInfo,	rangeStart, rangeEnd);
		MMSCP_StringFixup(&(res->Resolution),	&di, emptyDI, res->Resolution,		rangeStart, rangeEnd);
		MMSCP_StringFixup(&(res->Uri),			&di, emptyDI, res->Uri,				rangeStart, rangeEnd);
		res = res->Next;
	}

	/* prevent memory corruption in debug version */
	ASSERT(di <= ((char*)newObj) + mallocSize);

	return newObj;
}


/***********************************************************************************************************************
 *	END: Helper methods
 ***********************************************************************************************************************/

 
 
 
/***********************************************************************************************************************
 *	BEGIN: UPnP Callback Sinks
 *	These methods are callback sinks that are wired to the underlying UPNP stack.
 ***********************************************************************************************************************/

#ifndef MMSCP_LEAN_AND_MEAN
void MMSCP_ProcessResponse_GetCurrentConnectionInfo(struct UPnPService* Service,int ErrorCode,void *User,int RcsID,int AVTransportID,char* ProtocolInfo,char* PeerConnectionManager,int PeerConnectionID,char* Direction,char* Status)
{
	printf("MSCP Invoke Response: ConnectionManager/GetCurrentConnectionInfo(%d,%d,%s,%s,%d,%s,%s)\r\n",RcsID,AVTransportID,ProtocolInfo,PeerConnectionManager,PeerConnectionID,Direction,Status);
	UNSUPPORTED_BY_CP;
}

void MMSCP_ProcessResponse_GetProtocolInfo(struct UPnPService* Service,int ErrorCode,void *User,char* Source,char* Sink)
{
	printf("MSCP Invoke Response: ConnectionManager/GetProtocolInfo(%s,%s)\r\n",Source,Sink);
	UNSUPPORTED_BY_CP;
}

void MMSCP_ProcessResponse_GetCurrentConnectionIDs(struct UPnPService* Service,int ErrorCode,void *User,char* ConnectionIDs)
{
	printf("MSCP Invoke Response: ConnectionManager/GetCurrentConnectionIDs(%s)\r\n",ConnectionIDs);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_ExportResource(struct UPnPService* Service,int ErrorCode,void *User,unsigned int TransferID)
{
	printf("MSCP Invoke Response: ContentDirectory/ExportResource(%u)\r\n",TransferID);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_StopTransferResource(struct UPnPService* Service,int ErrorCode,void *User)
{
	printf("MSCP Invoke Response: ContentDirectory/StopTransferResource()\r\n");
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_DestroyObject(struct UPnPService* Service,int ErrorCode,void *User)
{
	printf("MSCP Invoke Response: ContentDirectory/DestroyObject()\r\n");
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_UpdateObject(struct UPnPService* Service,int ErrorCode,void *User)
{
	printf("MSCP Invoke Response: ContentDirectory/UpdateObject()\r\n");
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_GetSystemUpdateID(struct UPnPService* Service,int ErrorCode,void *User,unsigned int Id)
{
	printf("MSCP Invoke Response: ContentDirectory/GetSystemUpdateID(%u)\r\n",Id);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_GetTransferProgress(struct UPnPService* Service,int ErrorCode,void *User,char* TransferStatus,char* TransferLength,char* TransferTotal)
{
	printf("MSCP Invoke Response: ContentDirectory/GetTransferProgress(%s,%s,%s)\r\n",TransferStatus,TransferLength,TransferTotal);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_GetSortCapabilities(struct UPnPService* Service,int ErrorCode,void *User,char* SortCaps)
{
	printf("MSCP Invoke Response: ContentDirectory/GetSortCapabilities(%s)\r\n",SortCaps);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_GetSearchCapabilities(struct UPnPService* Service,int ErrorCode,void *User,char* SearchCaps)
{
	printf("MSCP Invoke Response: ContentDirectory/GetSearchCapabilities(%s)\r\n",SearchCaps);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_CreateObject(struct UPnPService* Service,int ErrorCode,void *User,char* ObjectID,char* Result)
{
	printf("MSCP Invoke Response: ContentDirectory/CreateObject(%s,%s)\r\n",ObjectID,Result);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_Search(struct UPnPService* Service,int ErrorCode,void *User,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID)
{
	printf("MSCP Invoke Response: ContentDirectory/Search(%s,%u,%u,%u)\r\n",Result,NumberReturned,TotalMatches,UpdateID);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_ImportResource(struct UPnPService* Service,int ErrorCode,void *User,unsigned int TransferID)
{
	printf("MSCP Invoke Response: ContentDirectory/ImportResource(%u)\r\n",TransferID);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_CreateReference(struct UPnPService* Service,int ErrorCode,void *User,char* NewID)
{
	printf("MSCP Invoke Response: ContentDirectory/CreateReference(%s)\r\n",NewID);
	UNSUPPORTED_BY_CP;
}

void MSCPResponseSink_ContentDirectory_DeleteResource(struct UPnPService* Service,int ErrorCode,void *User)
{
	printf("MSCP Invoke Response: ContentDirectory/DeleteResource()\r\n");
	UNSUPPORTED_BY_CP;
}

void MMSCPEventSink_ConnectionManager_SourceProtocolInfo(struct UPnPService* Service,char* SourceProtocolInfo)
{
	printf("MSCP Event from %s/ConnectionManager/SourceProtocolInfo: %s\r\n",Service->Parent->FriendlyName,SourceProtocolInfo);
	UNSUPPORTED_BY_CP;
}

void MMSCPEventSink_ConnectionManager_SinkProtocolInfo(struct UPnPService* Service,char* SinkProtocolInfo)
{
	printf("MSCP Event from %s/ConnectionManager/SinkProtocolInfo: %s\r\n",Service->Parent->FriendlyName,SinkProtocolInfo);
	UNSUPPORTED_BY_CP;
}

void MMSCPEventSink_ConnectionManager_CurrentConnectionIDs(struct UPnPService* Service,char* CurrentConnectionIDs)
{
	printf("MSCP Event from %s/ConnectionManager/CurrentConnectionIDs: %s\r\n",Service->Parent->FriendlyName,CurrentConnectionIDs);
	UNSUPPORTED_BY_CP;
}

void MMSCPEventSink_ContentDirectory_TransferIDs(struct UPnPService* Service,char* TransferIDs)
{
	printf("MSCP Event from %s/ContentDirectory/TransferIDs: %s\r\n",Service->Parent->FriendlyName,TransferIDs);
	UNSUPPORTED_BY_CP;
}

#endif

void MMSCP_AddRefRootDevice(struct UPnPService* Service)
{
	struct UPnPDevice *d = Service->Parent;
	//char str[1024];

	//sprintf(str, "RF+: %s(%d)\r\n", d->UDN, d->ReferenceCount);
	//OutputDebugString(str);	
	MSCP_AddRef(d);
}

void MMSCP_ReleaseRootDevice(struct UPnPService* Service)
{
	struct UPnPDevice *d = Service->Parent;
	//char str[1024];

	//sprintf(str, "RF-: %s(%d)\r\n", d->UDN, d->ReferenceCount);
	//OutputDebugString(str);	
	MSCP_Release(d);
}

void MSCPResponseSink_ContentDirectory_Browse(struct UPnPService* Service,int ErrorCode,void *User,char* Result,unsigned int NumberReturned,unsigned int TotalMatches,unsigned int UpdateID)
{
	struct ILibXMLNode* nodeList;
	struct ILibXMLNode* node;
	struct MMSCP_ResultsList *resultsList;
	struct ILibXMLAttribute *attribs;
	
	int resultLen;
	int parsePeerResult = 0;
	char *lastResultPos;

	struct MMSCP_MediaObject *newObj;

	TEMPDEBUGONLY(printf("MSCP Invoke Response: ContentDirectory/Browse(%s,%u,%u,%u)\r\n",Result,NumberReturned,TotalMatches,UpdateID);)

	if ((ErrorCode == 0) && (Result != NULL))
	{
		newObj = NULL;
		//resultLen = ILibInPlaceXmlUnEscape(Result);
		resultLen = strlen(Result);
		resultsList = (struct MMSCP_ResultsList*) MMSCP_MALLOC (sizeof(struct MMSCP_ResultsList));
		memset(resultsList, 0, sizeof(struct MMSCP_ResultsList));
		resultsList->LinkedList = ILibLinkedList_Create();

		lastResultPos = Result + resultLen;
		nodeList = ILibParseXML(Result, 0, resultLen);
		parsePeerResult = ILibProcessXMLNodeList(nodeList);

		if (parsePeerResult != 0)
		{
			MMSCP_Callback_Browse(Service, (struct MMSCP_BrowseArgs *)User, (int)MMSC_Error_XmlNotWellFormed, NULL);
		}
		else if (resultLen == 0)
		{
			MMSCP_Callback_Browse(Service, (struct MMSCP_BrowseArgs *)User, ErrorCode, NULL);
		}
		else
		{
			node = nodeList;
			while (node != NULL)
			{
				if (node->StartTag != 0)
				{
					/*[DONOTREPARSE] null terminate string */
					attribs = ILibGetXMLAttributes(node);
					node->Name[node->NameLength] = '\0';

					newObj = NULL;
					if (stricmp(node->Name, MMSCP_TAG_CONTAINER) == 0)
					{
						newObj = MMSCP_CreateMediaObject(node, attribs, 0, Result, lastResultPos);
						node = node->Next;
					}
					else if (stricmp(node->Name, MMSCP_TAG_ITEM) == 0)
					{					    
						newObj = MMSCP_CreateMediaObject(node, attribs, 1, Result, lastResultPos);
						node = node->Next;
					}
					else if (stricmp(node->Name, MMSCP_TAG_DIDL) == 0)
					{
						/* this is didl-lite root node, go to first child */
						node = node->Next;
					}
					else
					{
						/* this node is not supported, go to next sibling/peer */
						if (node->Peer != NULL)
						{
							node = node->Peer;
						}
						else
						{
							node = node->Parent->Peer;
						}
					}

					if (newObj != NULL)
					{
						/* set reminder of which CDS provided this object */
						MMSCP_AddRefRootDevice(Service);
						newObj->ServiceObject = Service;
						if (resultsList->LinkedList != NULL)
						{
							MMSCP_ObjRef_Add(newObj);
							ILibLinkedList_AddTail(resultsList->LinkedList, newObj);
						}
					}

					/* free attribute mappings */
					ILibDestructXMLAttributeList(attribs);
				}
				else
				{
					node = node->Next;
				}
			}
		}

		resultsList->NumberReturned = NumberReturned;
		resultsList->TotalMatches = TotalMatches;
		resultsList->UpdateID = UpdateID;

		/* validate number of parsed objects against returned count */
		resultsList->NumberParsed = ILibLinkedList_GetCount(resultsList->LinkedList);
		if ((int)resultsList->NumberParsed != (int)resultsList->NumberReturned)
		{
			printf("MSCPResponseSink_ContentDirectory_Browse: Detected mismatch with number of objects returned=%u and parsed=%d.\r\n", resultsList->NumberReturned, resultsList->NumberParsed);
		}

		/* free resources from XML parsing */
		ILibDestructXMLNodeList(nodeList);

		/* execute callback with results */
		MMSCP_Callback_Browse(Service, (struct MMSCP_BrowseArgs *)User, ErrorCode, resultsList);
	}
	else
	{
		MMSCP_Callback_Browse(Service, (struct MMSCP_BrowseArgs *)User, ErrorCode, NULL);
	}
}

#if 1
void MMSCPEventSink_ContentDirectory_ContainerUpdateIDs(struct UPnPService* Service,char* ContainerUpdateIDs)
{
	printf("MSCP Event from %s/ContentDirectory/ContainerUpdateIDs: %s\r\n",Service->Parent->FriendlyName,ContainerUpdateIDs);
	UNSUPPORTED_BY_CP;
}

void MMSCPEventSink_ContentDirectory_SystemUpdateID(struct UPnPService* Service,unsigned int SystemUpdateID)
{
	printf("MSCP Event from %s/ContentDirectory/SystemUpdateID: %u\r\n",Service->Parent->FriendlyName,SystemUpdateID);
	UNSUPPORTED_BY_CP;
}
#endif // 0

/* Called whenever a new device on the correct type is discovered */
void MMSCP_UPnPSink_DeviceAdd(struct UPnPDevice *device)
{
	printf("MSCP Device Added: %s\r\n", device->FriendlyName);
	
	if (MMSCP_Callback_DeviceAddRemove != NULL)
	{
		MMSCP_Callback_DeviceAddRemove(device, MMSCP_DEVICE_ADDED);
	}
}

/* Called whenever a discovered device was removed from the network */
void MMSCP_UPnPSink_DeviceRemove(struct UPnPDevice *device)
{
	printf("MSCP Device Removed: %s\r\n", device->FriendlyName);

	if (MMSCP_Callback_DeviceAddRemove != NULL)
	{
		MMSCP_Callback_DeviceAddRemove(device, MMSCP_DEVICE_REMOVED);
	}
}

/***********************************************************************************************************************
 *	END: UPnP Callback Sinks
 ***********************************************************************************************************************/





/***********************************************************************************************************************
 *	BEGIN: API method implementations
 ***********************************************************************************************************************/
void MMSCP_DestroyResultsList (struct MMSCP_ResultsList *resultsList)
{
	void *h;
	struct MMSCP_MediaObject *obj;

	if ((resultsList != NULL) && (resultsList->LinkedList))
	{
		ILibLinkedList_Lock(resultsList->LinkedList);
		h = ILibLinkedList_GetNode_Head(resultsList->LinkedList);
		while (h != NULL)
		{
			obj = (struct MMSCP_MediaObject*) ILibLinkedList_GetDataFromNode(h);
			if (obj != NULL)
			{
				MMSCP_ObjRef_Release(obj);
			}

			ILibLinkedList_Remove(h);
			h = ILibLinkedList_GetNode_Head(resultsList->LinkedList);
		}
		ILibLinkedList_UnLock(resultsList->LinkedList);
		ILibLinkedList_Destroy(resultsList->LinkedList);
		MMSCP_FREE (resultsList);
	}
}


void *MMSCP_Init(void *chain, MMSCP_Fn_Result_Browse callbackBrowse, MMSCP_Fn_Device_AddRemove callbackDeviceAddRemove)
{
	MMSCP_Callback_Browse = callbackBrowse;
	MMSCP_Callback_DeviceAddRemove = callbackDeviceAddRemove;

	/* Event callback function registration code */

#ifndef MMSCP_LEAN_AND_MEAN
	/* These require that the generated stack monitors and reports these state variables. */
	/* TODO: Provide a BrowseOnly DeviceBuilder settings file that provides these other state variables. */
	MSCP_EventCallback_ConnectionManager_SourceProtocolInfo=&MMSCPEventSink_ConnectionManager_SourceProtocolInfo;
	MSCP_EventCallback_ConnectionManager_SinkProtocolInfo=&MMSCPEventSink_ConnectionManager_SinkProtocolInfo;
	MSCP_EventCallback_ConnectionManager_CurrentConnectionIDs=&MMSCPEventSink_ConnectionManager_CurrentConnectionIDs;
	MSCP_EventCallback_ContentDirectory_TransferIDs=&MMSCPEventSink_ContentDirectory_TransferIDs;
#endif

	MSCP_EventCallback_ContentDirectory_ContainerUpdateIDs=&MMSCPEventSink_ContentDirectory_ContainerUpdateIDs;
	MSCP_EventCallback_ContentDirectory_SystemUpdateID=&MMSCPEventSink_ContentDirectory_SystemUpdateID;

	/* create the underlying UPnP control point stack */
	return MSCP_CreateControlPoint(chain, &MMSCP_UPnPSink_DeviceAdd, &MMSCP_UPnPSink_DeviceRemove);
}

void MMSCP_Invoke_Browse(void *serviceObj, struct MMSCP_BrowseArgs *args)
{
	char *browseFlagString;
//TODO, who free?
        struct BrowseInfo* bInfo = (struct BrowseInfo*) malloc(sizeof(struct BrowseInfo));
        bInfo->args = args;
        bInfo->callbackBrowse = MMSCP_Callback_Browse;

	if (args->BrowseFlag == MMSCP_BrowseFlag_Metadata)
	{
		browseFlagString = MMSCP_BROWSE_FLAG_METADATA_STRING;
	}
	else
	{
		browseFlagString = MMSCP_BROWSE_FLAG_CHILDREN_STRING;
	}

	////MSCP_Invoke_ContentDirectory_Browse
//	MSCP_Invoke_MediaServer_ContentDirectory_Browse
	MSCP_Invoke_ContentDirectory_Browse
		(
		(struct UPnPService *)serviceObj, 
		MSCPResponseSink_ContentDirectory_Browse, 
		bInfo, 
		args->ObjectID, 
		browseFlagString, 
		args->Filter, 
		args->StartingIndex, 
		args->RequestedCount, 
		args->SortCriteria
		);
}

struct MMSCP_MediaResource* MMSCP_SelectBestIpNetworkResource(const struct MMSCP_MediaObject *mediaObj, const char *protocolInfoSet, int *ipAddressList, int ipAddressListLen)
{
	struct MMSCP_MediaResource* retVal = NULL, *res;
	long ipMatch = 0xFFFFFFFF, protInfoMatch = 0, bestIpMatch = 0xFFFFFFFF, bestProtInfoMatch = 0;
	int protInfoCount = 0;
	char *protInfoSet;
	int protInfoSetStringSize, protInfoSetStringLen;
	int i, pi;
	short finding;
	char **protInfos;
	char *protocol, *network, *mimeType, *info;
	int protocolLen, networkLen, mimeTypeLen, infoLen;
	char *resprotocol, *resnetwork, *resmimeType, *resinfo;
	int resprotocolLen, resnetworkLen, resmimeTypeLen, resinfoLen;
	int posIpByteStart, posIpByteLength;
	long ipq[4];
	int qi;
	unsigned long ip;//,reverseThis;
	char *rp;
	int cmpIp1, cmpIp2;
	int badUri;

	/*
	 *	copy the list of protocolInfo strings into protInfoSet
	 */
	protInfoSetStringLen = (int) strlen(protocolInfoSet);
	protInfoSetStringSize = protInfoSetStringLen + 1;
	protInfoSet = (char*) malloc (protInfoSetStringSize);
	memcpy(protInfoSet, protocolInfoSet, protInfoSetStringSize);
	protInfoSet[protInfoSetStringLen] = '\0';

	/*
	 *	Replace all commas in protInfoSet to NULL chars
	 *	and count the number of protocolInfo strings in the set.
	 *	Method only works if the protocolInfo are listed in form: A,B,...Z.
	 *	If we receive malformed sets like A,,B then results are undefined.
	 */
	protInfoCount = 1;
	for (i=0; i < protInfoSetStringLen; i++)
	{
		if (protInfoSet[i] == ',')
		{
			protInfoSet[i] = '\0';
			protInfoCount++;
		}
	}

	/*
	 *	create an array of char** that will allow us easy access 
	 *	to individual protocolInfo. Also redo the count
	 *	in case of inaccuracies due to bad formatting.
	 */
	protInfos = (char**) malloc (sizeof(char*) * protInfoCount);
	pi = 0;
	finding = 0;
	for (i=0; i < protInfoSetStringLen; i++)
	{
		if ((finding == 0) && (protInfoSet[i] != '\0'))
		{
			protInfos[pi] = &(protInfoSet[i]);
			pi++;
			finding = 1;
			protInfoCount++;
		}
		else if ((finding == 1) && (protInfoSet[i] == '\0'))
		{
			finding = 0;
		}
	}
	if (pi < protInfoCount) { protInfoCount = pi; }


	/*
	 *	Iterate through the different resources and track the best match.
	 */
	res = mediaObj->Res;
	while (res != NULL)
	{
		/* the protocolInfo strings listed first have higher precedence */
		protInfoMatch = protInfoCount + 1;

		/* calculate a match value against protocolInfo */
		for (i=0; i < protInfoCount; i++)
		{
			protInfoMatch--;
			
			/*
			 * get pointers and lengths for the fields in the protocolInfo 
			 */
			protocol = protInfos[i];
			protocolLen = IndexOf(protocol, ":");

			network = protocol + protocolLen + 1;
			networkLen = IndexOf(network, ":");

			mimeType = network + networkLen + 1;
			mimeTypeLen = IndexOf(mimeType, ":");

			info = mimeType + mimeTypeLen + 1;
			infoLen = (int) strlen(info);

			/*
			 * get pointers and lengths for the fields in the resource's protocolInfo 
			 */
			resprotocol = res->ProtocolInfo;
			resprotocolLen = IndexOf(resprotocol, ":");

			resnetwork = resprotocol + resprotocolLen + 1;
			resnetworkLen = IndexOf(resnetwork, ":");

			resmimeType = resnetwork + resnetworkLen + 1;
			resmimeTypeLen = IndexOf(resmimeType, ":");

			resinfo = resmimeType + resmimeTypeLen + 1;
			resinfoLen = (int) strlen(resinfo);
			
			/* compare each of the fields */

			if (strnicmp(protocol, resprotocol, MIN(protocolLen, resprotocolLen)) == 0)
			{
				if (strnicmp(network, resnetwork, MIN(networkLen, resnetworkLen)) == 0)
				{
					if (
						((mimeType[0] == '*') && (mimeType[1] == ':'))
						|| (strnicmp(mimeType, resmimeType, MIN(mimeTypeLen, resmimeTypeLen)) == 0)
						)
					{
						/*
						 *	DHWG guidelines require the DLNA.ORG_PN parameter to
						 *	show up first, so this code will work provided 
						 *	the protocolInfo values in protocolInfoSet only
						 *	have the DLNA.ORG_PN parameter in it. It's OK if
						 *	the CdsMediaResource has other parameters because
						 *	we only compare MIN(infoLen,resinfoLen).
						 */

						if (
							((info[0] == '*') && (info[1] == '\0'))
							|| (strnicmp(info, resinfo, MIN(infoLen, resinfoLen)) == 0)
							)
						{
							/*
							 *	If we get here then protocolInfo matches.
							 *	Go ahead and break since protInfoMatch is
							 *	set on every iteration.
							 */
							break;
						}
					}
				}
			}
		}

		/*
		 *	At this point, we have calculated the protInfoMatch value,
		 *	but we still need to determine if the resource has a good
		 *	chance of being routable given a particular target
		 *	IP address.
		 */

		ipMatch = 0xFFFFFFFF;
		ip = 0;

		/*
		 *	Convert text-based IP address to in-order int form.
		 *	Since the res->URI is assumed to be a valid DLNA URI,
		 *	it will have the form scheme://[ip address]:....
		 *
		 *	If by chance the URI is not in quad-notation, then we
		 *	use an IP address value of 0.0.0.0 because it's impossible
		 *	to match with host/domain names.
		 */
		posIpByteStart = IndexOf(res->Uri, "://") + 3;
		if (posIpByteStart > 0)
		{
			rp = res->Uri + posIpByteStart;
			qi = 0;
			badUri = 0;
			while ((qi < 4) && (badUri == 0))
			{
				posIpByteLength = 0;
				
				/* loop until we don't find a digit or until we know it's a bad URI*/
				while ((isdigit(rp[posIpByteLength]) != 0) || (badUri != 0))
				{
					posIpByteLength++;
					if (posIpByteLength > 3)
					{
						badUri = 1;
					}
				}

				if (posIpByteLength == 0) badUri = 1;

				if (badUri == 0)
				{
					ILibGetLong(rp, posIpByteLength, &(ipq[qi]));
					if (ipq[qi] > 255) badUri = 1;
					rp += (posIpByteLength + 1);
					qi++;
				}
			}
			
			if (badUri != 0)
			{
				ipq[0] = ipq[1] = ipq[2] = ipq[3] = 0;
			}
		}

		/*
		 *	Convert each network byte into a 32-bit integer,
		 *	then perform a bit mask comparison against the target ip address.
		 */
		ip = (int) (ipq[0] | (ipq[1] << 8) | (ipq[2] << 16) | (ipq[3] << 24));

		cmpIp1 = htonl(ip);

		for (i=0; i < ipAddressListLen; i++)
		{
			cmpIp2 = htonl(ipAddressList[i]);
			ipMatch = (cmpIp2 ^ cmpIp1);
/*
			reverseThis = (cmpIp2 ^ cmpIp1);
			ipMatch = 
				((reverseThis & 0x000000ff) << 24) |
				((reverseThis & 0x0000ff00) << 8) |
				((reverseThis & 0x00ff0000) >> 8) |
				((reverseThis & 0xff000000) >> 24);
*/

			if (
				((unsigned)ipMatch < (unsigned)bestIpMatch) ||
				((ipMatch == bestIpMatch) && (protInfoMatch > bestProtInfoMatch))
				)
			{
				retVal = res;
				bestIpMatch = ipMatch;
				bestProtInfoMatch = protInfoMatch;
			}
		}

		res = res->Next;
	}

	free (protInfos);
	free (protInfoSet);

	return retVal;
}

void MMSCP_Uninit()
{
	MMSCP_Callback_Browse = NULL;
	MMSCP_Callback_DeviceAddRemove = NULL;
}

struct MMSCP_MediaObject *MMSCP_CloneMediaObject(const struct MMSCP_MediaObject *cloneThis)
{
	/*
	 *	This code is written for both 64-bit and 32-bit address spaces.
	 */

	#if (ADDRESS_SPACE==64)
	#define MEMTYPE __int64
	#else
	#define MEMTYPE long
	#endif

	struct MMSCP_MediaObject *retVal = NULL;
	const struct MMSCP_MediaObject *ct;
	struct MMSCP_MediaObject **ah;
	MEMTYPE diff;
	struct MMSCP_MediaResource *ctr, **clr;
	int dataSize, mallocSize = 0;
	char *di, *emptyDI;
	//char dbgstr[1024];
	
	ah = &(retVal);
	ct = cloneThis;
	if (ct != NULL)
	{
		if (ct->MallocSize == 0)
		{
			dataSize = MMSCP_GetRequiredSizeForMediaObject((struct MMSCP_MediaObject *)ct, NULL);

			mallocSize = dataSize + sizeof(struct MMSCP_MediaObject) + 1;
			(*ah) = (struct MMSCP_MediaObject*) malloc(mallocSize);
			memset((*ah), 0, mallocSize);
			(*ah)->MallocSize = mallocSize;
			(*ah)->RefCount = 0;
			sem_init(&((*ah)->Lock), 0, 1);		
			
			//sprintf(dbgstr, "CL1: %x(%s)\r\n", (*ah), ct->ID);
			//OutputDebugString(dbgstr);

			(*ah)->MediaClass = ct->MediaClass;
			(*ah)->Flags = ct->Flags;		
			
			/* di will point to where it's safe to write string data */
			di = (char*)(*ah);
			di += sizeof(struct MMSCP_MediaObject);
			emptyDI = di;
			di ++;

			MMSCP_StringFixup(&((*ah)->ID),			&di, emptyDI, ct->ID,		ct->ID,			ct->ID);
			MMSCP_StringFixup(&((*ah)->ParentID),	&di, emptyDI, ct->ParentID,	ct->ParentID,	ct->ParentID);
			MMSCP_StringFixup(&((*ah)->RefID),		&di, emptyDI, ct->RefID,	ct->RefID,		ct->RefID);
			MMSCP_StringFixup(&((*ah)->Title),		&di, emptyDI, ct->Title,	ct->Title,		ct->Title);
			MMSCP_StringFixup(&((*ah)->Creator),	&di, emptyDI, ct->Creator,	ct->Creator,	ct->Creator);
			MMSCP_StringFixup(&((*ah)->Genre),		&di, emptyDI, ct->Genre,	ct->Genre,		ct->Genre);
			MMSCP_StringFixup(&((*ah)->Album),		&di, emptyDI, ct->Album,	ct->Album,		ct->Album);
			MMSCP_StringFixup(&((*ah)->Artist),		&di, emptyDI, ct->Artist,	ct->Artist,		ct->Artist);
			MMSCP_StringFixup(&((*ah)->Date),		&di, emptyDI, ct->Date,	ct->Date,		ct->Date);

			ctr = ct->Res;
			clr = &((*ah)->Res);
			while (ctr != NULL)
			{
				(*clr) = (struct MMSCP_MediaResource*) malloc (sizeof(struct MMSCP_MediaResource));
				memcpy((*clr), ctr, sizeof(struct MMSCP_MediaResource));

				MMSCP_StringFixup(&((*clr)->ProtocolInfo),	&di, emptyDI, ctr->ProtocolInfo,	ctr->ProtocolInfo, ctr->ProtocolInfo);
				MMSCP_StringFixup(&((*clr)->Resolution),		&di, emptyDI, ctr->Resolution,		ctr->Resolution, ctr->Resolution);
				MMSCP_StringFixup(&((*clr)->Uri),			&di, emptyDI, ctr->Uri,				ctr->Uri, ctr->Uri);
				ctr = ctr->Next;
			}

			/* prevent memory corruption in debug version */
			ASSERT(di <= ((char*)(*ah)) + mallocSize);		}
		else
		{
			/* allocate memory for the cloned CDS object and copy its state*/
			(*ah) = (struct MMSCP_MediaObject*) malloc(ct->MallocSize);
			diff = ((unsigned MEMTYPE)retVal - (unsigned MEMTYPE)cloneThis);
			memcpy((*ah), ct, ct->MallocSize);

			/*
			 *	Initialize stuff that is unique to this
			 *	particular instance - notably, the reserved fields.
			 */
			if ((*ah)->ServiceObject != NULL)
			{
				MMSCP_AddRefRootDevice((*ah)->ServiceObject);
			}
			(*ah)->RefCount = 0;
			sem_init(&((*ah)->Lock), 0, 1);

			/* we will clone resources in another step */
			(*ah)->Res = NULL;

			/*
			 *	Adjust the pointer addresses so that they fit for the new CDS object.
			 *	ignore compiler warnings
			 */
#if 1 // HIWU			 
			(*ah)->Album		= (char *)(((unsigned MEMTYPE) ((*ah)->Album))		+ (diff));			
			(*ah)->Creator		= (char *)(((unsigned MEMTYPE) ((*ah)->Creator))	+ (diff));
			(*ah)->Genre		= (char *)(((unsigned MEMTYPE) ((*ah)->Genre))		+ (diff));
			(*ah)->Artist		= (char *)(((unsigned MEMTYPE) ((*ah)->Artist))		+ (diff));
			(*ah)->Date 		= (char *)(((unsigned MEMTYPE) ((*ah)->Date))		+ (diff));
			(*ah)->ID			= (char *)(((unsigned MEMTYPE) ((*ah)->ID))			+ (diff));
			(*ah)->ParentID		= (char *)(((unsigned MEMTYPE) ((*ah)->ParentID))	+ (diff));
			(*ah)->RefID		= (char *)(((unsigned MEMTYPE) ((*ah)->RefID))		+ (diff));
			(*ah)->Title		= (char *)(((unsigned MEMTYPE) ((*ah)->Title))		+ (diff));
printf("===>HIWU debug ...\n");
#else
			(*ah)->Album		= (((unsigned MEMTYPE) ((*ah)->Album))		+ (diff));			
			(*ah)->Creator		= (((unsigned MEMTYPE) ((*ah)->Creator))	+ (diff));
			(*ah)->Genre		= (((unsigned MEMTYPE) ((*ah)->Genre))		+ (diff));
			(*ah)->Artist 		= (((unsigned MEMTYPE) ((*ah)->Artist))		+ (diff));			
			(*ah)->Date			= (((unsigned MEMTYPE) ((*ah)->Date))		+ (diff));			
			(*ah)->ID			= (((unsigned MEMTYPE) ((*ah)->ID))			+ (diff));
			(*ah)->ParentID		= (((unsigned MEMTYPE) ((*ah)->ParentID))	+ (diff));
			(*ah)->RefID		= (((unsigned MEMTYPE) ((*ah)->RefID))		+ (diff));
			(*ah)->Title		= (((unsigned MEMTYPE) ((*ah)->Title))		+ (diff));
#endif // HIWU
			/*
			 *	Clone resources.
			 *	Note that memory for resource strings is part of the CDS object
			 *	because some of those strings may repeat. Thus, all we need
			 *	to do is allocate memory for the CDS resource, copy as is,
			 *	and then adjust the string pointers.
			 */
			ctr = ct->Res;
			clr = &((*ah)->Res);

			while (ctr != NULL)
			{
				/* allocate and copy */
				(*clr) = (struct MMSCP_MediaResource*) malloc (sizeof(struct MMSCP_MediaResource));
				memcpy((*clr), ctr, sizeof(struct MMSCP_MediaResource));

				/* adjust string pointers - remember that they point to memory that trails the CDS object */
#if 1 // HIWU				
				(*clr)->Uri				= (char *)(((unsigned MEMTYPE) ((*clr)->Uri))			+ (diff));
				(*clr)->ProtocolInfo	= (char *)(((unsigned MEMTYPE) ((*clr)->ProtocolInfo))	+ (diff));
				(*clr)->Resolution		= (char *)(((unsigned MEMTYPE) ((*clr)->Resolution))	+ (diff));
printf("===>HIWU convert\n");
#else
				(*clr)->Uri				= (((unsigned MEMTYPE) ((*clr)->Uri))			+ (diff));
				(*clr)->ProtocolInfo	= (((unsigned MEMTYPE) ((*clr)->ProtocolInfo))	+ (diff));
				(*clr)->Resolution		= (((unsigned MEMTYPE) ((*clr)->Resolution))	+ (diff));
#endif // HIWU
				clr = &((*clr)->Next);
				ctr = ctr->Next;
			}
		}
	}

	return retVal;
}

void MMSCP_ObjRef_Add(struct MMSCP_MediaObject *refThis)
{
	//char dbgstr[1024];

	//sprintf(dbgstr, "OB+: %x\t(%s)(%d)\r\n", refThis, refThis->ID, refThis->RefCount);
	//OutputDebugString(dbgstr);

	sem_wait(&(refThis->Lock));
	refThis->RefCount++;
	sem_post(&(refThis->Lock));
}

void MMSCP_ObjRef_Release(struct MMSCP_MediaObject *releaseThis)
{
	struct MMSCP_MediaResource *res;
	struct MMSCP_MediaResource *nextRes;
	//char dbgstr[1024];

	/*
	 *	Remember that the ResultsList is stored in such a way that
	 *	each MediaObject and MediaResource pointer is allocated
	 *	with additional trailinng memory that stores the strings associated
	 *	with the object. Therefore, when we free a MediaResource or
	 *	a MediaObject, we're also freeing the memory for the string metadata.
	 */

	if (releaseThis != NULL)
	{
		//sprintf(dbgstr, "OB-: %x\t(%s)(%d)\r\n", releaseThis, releaseThis->ID, releaseThis->RefCount);
		//OutputDebugString(dbgstr);

		sem_wait(&(releaseThis->Lock));
		releaseThis->RefCount--;

		if (releaseThis->RefCount <= 0)
		{
			res = releaseThis->Res;
			while (res != NULL)
			{
				nextRes = res->Next;
				MMSCP_FREE(res);
				res = nextRes;
			}

			if (releaseThis->ServiceObject != NULL)
			{
				/* release the UPnP device */
				MMSCP_ReleaseRootDevice(releaseThis->ServiceObject);
			}

			sem_post(&(releaseThis->Lock));

			/* destroy the object */
			MMSCP_FREE(releaseThis);
		}
		else
		{
			sem_post(&(releaseThis->Lock));
		}
	}
}

/***********************************************************************************************************************
 *	END: API method implementations
 ***********************************************************************************************************************/
