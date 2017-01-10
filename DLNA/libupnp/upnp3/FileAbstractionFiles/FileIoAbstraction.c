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
 * $Workfile: FileIoAbstraction.c
 * $Revision: #1.0.2201.28945
 * $Author:   Intel Corporation, Intel Device Builder
 * $Date:     Tuesday, January 10, 2006
 *
 *
 *
 */


#if defined(WIN32)
	#define _CRTDBG_MAP_ALLOC
	#include <stdlib.h>
	#if !defined(_WIN32_WCE)
		#include <crtdbg.h>
	#endif
#else
	#include <stdlib.h>
#endif

#include <stdio.h>
#include <string.h>


/* Windows 32 */
#if defined(WIN32)
	#include <windows.h>
	#include <time.h>
	#include <direct.h>
	#define getcwd(path, sizeOfBuffers) _getcwd(path,sizeOfBuffers)
#endif


/* POSIX */
#if defined(_POSIX)
	#include <sys/stat.h>
	#include <sys/types.h>
	#include <dirent.h>
	#include <sys/time.h>
	#include <unistd.h>
	#include <assert.h>
	//modified by yunfeng_han@realsil.com.cn
	#ifndef off64_t
	#define off64_t long long
	#endif
	#include <io/GeneralFileAccess/file_access.h>
//	char entries[ENTRIES_COUNT][ENTRIES_LENGTH];
    int entry_counter = 0;
	char **entries=NULL;
#endif

#if defined(WIN32) || defined(_WIN32_WCE)
	#define strncasecmp(x,y,z) _strnicmp(x,y,z)
	#define strcasecmp(x,y) _stricmp(x,y)
#endif


#if defined(_WIN32_WCE)
	#include <Winbase.h>
#endif
#include "FileIoAbstraction.h"

#if defined(__SYMBIAN32__)
#include "ILibSymbianFileIO.h"
#endif

int EndsWith(/*INOUT*/ const char* str, const char* endsWith, int ignoreCase)
{
	int strLen, ewLen, offset;
	int cmp = 0;

	strLen = (int) strlen(str);
	ewLen = (int) strlen(endsWith);
	if (ewLen > strLen) return 0;
	offset = strLen - ewLen;

	if (ignoreCase != 0)
	{
		cmp = strncasecmp(str+offset, endsWith, ewLen);
	}
	else
	{
		cmp = strncmp(str+offset, endsWith, ewLen);
	}

	return cmp == 0?1:0;
}

void ILibFileDir_CloseDir(void* handle)
{
#if defined(WIN32) || defined(_WIN32_WCE)
	FindClose(*((HANDLE*)handle));
	free(handle);
#elif defined(_POSIX)
	DIR* dirObj = (DIR*) handle;
	closedir(dirObj);
#elif defined(__SYMBIAN32__)
	ILibSymbian_FileDir_CloseDir(handle);
#endif
}


void* ILibFileDir_GetDirFirstFile(const char* directory, /*INOUT*/ char* filename, int filenamelength, /*INOUT*/ int* filesize)
{
#if defined(WIN32) || defined(_WIN32_WCE)
	WIN32_FIND_DATA FileData;
	HANDLE* hSearch;
	char* direx;
	#if defined(_WIN32_WCE) || defined(UNICODE)
		wchar_t *tempChar;
		int tempCharLength;
	#endif


	hSearch = malloc(sizeof(HANDLE));
	direx = malloc(filenamelength + 5);

	if (directory[(int) strlen(directory) - 1] == '\\')
	{
		sprintf(direx,"%s*.*",directory);
	}
	else
	{
		sprintf(direx,"%s\\*.*",directory);
	}

	#if defined(_WIN32_WCE) || defined(UNICODE)
		tempCharLength = MultiByteToWideChar(CP_UTF8,0,direx,-1,NULL,0);
		tempChar = (wchar_t*)malloc(sizeof(wchar_t)*tempCharLength);
		MultiByteToWideChar(CP_UTF8,0,direx,-1,tempChar,tempCharLength);
		*hSearch = FindFirstFile(tempChar, &FileData);
		free(direx);
		free(tempChar);
	#else
		*hSearch = FindFirstFile(direx, &FileData);
		free(direx);
	#endif

	if (*hSearch == INVALID_HANDLE_VALUE)
	{
		free(hSearch);
		hSearch = NULL;
	}
	else
	{
		if (filename != NULL)
		{
#if defined(_WIN32_WCE) || defined(UNICODE)
			WideCharToMultiByte(CP_UTF8,0,(LPCWSTR)FileData.cFileName,-1,filename,filenamelength,NULL,NULL);
#else
			memcpy(filename,FileData.cFileName,1+(int)strlen(FileData.cFileName));
#endif
		}

		if (filesize != NULL)
		{
			*filesize = FileData.nFileSizeLow;
		}
	}

	return hSearch;
#elif defined(_POSIX)
	DIR* dirObj;
	struct dirent* dirEntry;	/* dirEntry is a pointer to static memory in the C runtime lib for readdir()*/
	struct stat _si;
	char fullPath[1024];
	char translatedFileName [256];
	char tmpstr[256];
	gfile_t * fh;
	int i;
	entry_counter = 0;
	if(entries==NULL){
		printf("11111111111111111111111111111111111111111111111111111111111111\n");
		entries = (char *) malloc(ENTRIES_COUNT * sizeof(char *));
    	for (i = 0; i < ENTRIES_COUNT; ++i)
    	{
        	entries[i]=  (char *) malloc(ENTRIES_LENGTH);
    	}
	}
	
	dirObj = opendir(directory);
//	printf("#######################In FIRST File\n");
	if (dirObj != NULL)
	{
		dirEntry = readdir(dirObj);

		if ((dirEntry != NULL) && ((int) strlen(dirEntry->d_name) < filenamelength))
		{
			if (filename != NULL)
			{
                                bool tmp;
				strcpy(filename,dirEntry->d_name);
				sprintf(fullPath, "%s%s", directory, dirEntry->d_name);
				
				//modified by yunfeng_han@realsil.com.cn
				strcpy(tmpstr,directory);
				GGetFileName (tmpstr, dirEntry->d_name, translatedFileName, ENTRIES_LENGTH-1, &tmp);
				sprintf(fullPath, "%s%s", directory, translatedFileName);
			//	printf("In Get First File %s   %s  \n",translatedFileName,fullPath);
        		fh = GFileOpen(fullPath, O_RDONLY | 0x20000000, GFILE_TYPE_ARRAY);
				assert(fh !=NULL);
				//if(fh ==NULL)return dirObj;
				strcpy(entries[entry_counter++], translatedFileName);
				strcpy(filename,translatedFileName);
				if (filesize != NULL)
				{
					sprintf(fullPath, "%s%s", directory, dirEntry->d_name);
					if (stat(fullPath, &_si) != -1)
					{
						if ((_si.st_mode & S_IFDIR) == S_IFDIR)
						{
							*filesize = 0;
						}
						else
						{
    						*filesize = GgetFileSize(fh);
							//*filesize = _si.st_size;
						}
					}
					
				}
				if(fh != NULL) GFileClose(fh);
			}
		}
	}

	return dirObj;
#elif defined(__SYMBIAN32__)
	return (ILibSymbian_FileDir_GetDirFirstFile(directory, filename, filenamelength, filesize));
#endif
}




// Windows Version
// 0 = No More Files
// 1 = Next File
int ILibFileDir_GetDirNextFile(void* handle, const char* dirName, char* filename, int filenamelength, int* filesize)
{
#if defined(WIN32) || defined(_WIN32_WCE)
	WIN32_FIND_DATA FileData;
	
	if (FindNextFile(*((HANDLE*)handle), &FileData) == 0) {return 0;}
	#if defined(_WIN32_WCE) || defined(UNICODE)
		WideCharToMultiByte(CP_UTF8,0,(LPCWSTR)FileData.cFileName,-1,filename,filenamelength,NULL,NULL);
	#else
		memcpy(filename,FileData.cFileName,1+(int)strlen(FileData.cFileName));
	#endif
	if (filesize != NULL)
	{
		*filesize = FileData.nFileSizeLow;
	}

	return 1;
#elif defined(_POSIX)
	DIR* dirObj;
	struct dirent* dirEntry;	/* dirEntry is a pointer to static memory in the C runtime lib for readdir()*/
	struct stat _si;
	char fullPath[1024];
	char translatedFileName [256];
	char tmpstr[256];
	gfile_t * fh;
	int i;
	dirObj = (DIR*) handle;
	while((dirEntry = readdir(dirObj)) != NULL) {

	if ((dirEntry != NULL) && ((int) strlen(dirEntry->d_name) < filenamelength))
	{
                bool tmp;
		strcpy(filename,dirEntry->d_name);
		sprintf(fullPath, "%s%s", dirName, dirEntry->d_name);
		//printf("%s   %s  \n",filename,fullPath);
		//modified by yunfeng_han@realsil.com.cn
		strcpy(tmpstr,dirName);
		GGetFileName (tmpstr, dirEntry->d_name, translatedFileName, ENTRIES_LENGTH-1, &tmp);
		//printf("%s   %s  \n",translatedFileName,dirName);
		for (i = 0; i < entry_counter; ++i)
		{
    		if (0 == strcmp (entries[i], translatedFileName))
    		{
    			break;
    		}
		}


		
	//	printf("11111111   i = %d count = %d\n",i,entry_counter);					
		if (i == entry_counter)
		{
			sprintf(fullPath, "%s%s", dirName, translatedFileName);
		//	printf("In Get Next File %s   %s  \n",translatedFileName,fullPath);
        	fh = GFileOpen(fullPath, O_RDONLY | 0x20000000, GFILE_TYPE_ARRAY);
			assert(fh !=NULL);
			strcpy(entries[entry_counter++], translatedFileName);
			i++;
			strcpy(filename,translatedFileName);
			
		}
		if ((filesize != NULL)&&(i == entry_counter))
		{
			/* ? Cygwin has a memory leak with stat. */
			sprintf(fullPath, "%s%s", dirName, dirEntry->d_name);
			if (stat(fullPath, &_si) != -1)
			{
				if ((_si.st_mode & S_IFDIR) == S_IFDIR)
				{
					if(fh!=NULL){
						*filesize = 0;
						GFileClose(fh);
						fh =NULL;
					}
					return 1;
				}
				else
				{
					if(fh!=NULL){
						*filesize = GgetFileSize(fh);
						GFileClose(fh);
						fh =NULL;
					}
					//*filesize = _si.st_size;
					return 1;
				}
					
				
			}
		}
		//printf("2222222222   i = %d count = %d\n",i,entry_counter);
		if (i == entry_counter){
				if(fh!=NULL){
					GFileClose(fh);
					fh =NULL;
					return 1;
				}
		}
		
	}
}
	return 0;
#elif defined(__SYMBIAN32__)
	return(ILibSymbian_FileDir_GetDirNextFile(handle, dirName, filename, filenamelength, filesize));
#endif
}

enum ILibFileDir_Type ILibFileDir_GetType(char* directory)
{
#if defined(WIN32) || defined(_WIN32_WCE)
	DWORD _si;
	int dirLen,dirSize;
	#if defined(_WIN32_WCE) || defined(UNICODE)
		wchar_t *tempChar;
		int tempCharLength;
	#endif

	dirLen = (int) strlen(directory);
	dirSize = dirLen+1;

	#if defined(_WIN32_WCE) || defined(UNICODE)
		tempCharLength = MultiByteToWideChar(CP_UTF8,0,directory,-1,NULL,0);
		tempChar = (wchar_t*)malloc(sizeof(wchar_t)*tempCharLength);
		MultiByteToWideChar(CP_UTF8,0,directory,-1,tempChar,tempCharLength);
		_si = GetFileAttributes(tempChar);
		free(tempChar);
	#else
		_si = GetFileAttributes(directory);
	#endif
	
	if (_si == 0xFFFFFFFF)
	{
		return ILibFileDir_Type_NOT_FOUND_ERROR;
	}

	if ((_si & FILE_ATTRIBUTE_DIRECTORY) == 0)
	{
		return ILibFileDir_Type_FILE;
	}
	else 
	{
		return ILibFileDir_Type_DIRECTORY;
	}
#elif defined(_POSIX)
	struct stat _si;

	int dirLen,dirSize;
	//char *fullpath; //remove since no use, 20090513, yuyu
	int pathExists;
	gfile_t * fh;
	enum ILibFileDir_Type retVal = ILibFileDir_Type_NOT_FOUND_ERROR;

	dirLen = (int) strlen(directory);
	dirSize = dirLen+1;
	//fullpath = (char*) malloc(dirSize); //remove since no use, 20090513, yuyu

	pathExists = stat (directory, &_si);

	if (pathExists != -1)
	{
		if ((_si.st_mode & S_IFDIR) == S_IFDIR)
		{
			retVal = ILibFileDir_Type_DIRECTORY;
			return retVal;
		}
		else
		{
			retVal = ILibFileDir_Type_FILE;
			return retVal;
		}
	}
	//modified by yunfeng_han@realsil.com.cn
	//try general file access
	//printf("In Get File Type %s\n",directory);
	fh = GFileOpen(directory, O_RDONLY | 0x20000000, GFILE_TYPE_ARRAY);
	if(fh !=NULL){
		retVal = ILibFileDir_Type_FILE;
		GFileClose(fh);
		fh =NULL;
		return retVal;
	}
	return retVal;
#elif defined(__SYMBIAN32__)
	switch(ILibSymbian_FileDir_GetType(directory))
	{
		case ILibSymbian_FileDir_Type_FILE:
			return(ILibFileDir_Type_FILE);
			break;
		case ILibSymbian_FileDir_Type_DIR:
			return(ILibFileDir_Type_DIRECTORY);
			break;
		default:
			return(ILibFileDir_Type_NOT_FOUND_ERROR);
			break;
	}
#endif
}

int ILibFileDir_GetDirEntryCount(const char* fullPath, char *dirDelimiter)
{
	char fn[MAX_PATH_LENGTH];
	void *dirObj;
	int retVal = 0;
	int ewDD;
	int nextFile;

//	printf("#########################before ILibFileDir_GetDirEntryCount\n");
	dirObj = ILibFileDir_GetDirFirstFile(fullPath, fn, MAX_PATH_LENGTH, NULL);
	if (dirObj != NULL)
	{
		ewDD = EndsWith(fullPath, dirDelimiter, 0);

		do
		{
			retVal++;
			nextFile = ILibFileDir_GetDirNextFile(dirObj,fullPath,fn,MAX_PATH_LENGTH, NULL);
		}
		while (nextFile != 0);

		ILibFileDir_CloseDir(dirObj);
	}
//	printf("############################after ILibFileDir_GetDirEntryCount\n");
	return retVal;
}

char* ILibFileDir_GetWorkingDir(char *path, size_t sizeOfBuf)
{
#if defined(_WIN32_WCE)
	if(path==NULL)
	{
		path = (char*)malloc(2);
	}
	sprintf(path,"/");
	return(path);
#elif defined(__SYMBIAN32__)
	return(ILibSymbian_FileDir_GetWorkingDir(path,(int)sizeOfBuf));
#else
	return getcwd(path, (int)sizeOfBuf);
#endif
}
int ILibFileDir_DeleteFile(char *FileName)
{

#if defined(_WIN32_WCE) || (defined(WIN32) && defined(UNICODE))
	int retVal;
	wchar_t* wfile;		/* must MMS_FREE */
	int wFileLen;
	int wFileSize;
	int mbFileLen;
	int mbFileSize;

	mbFileLen = (int) strlen(FileName);
	mbFileSize = mbFileLen+1;
	wFileLen = mbFileLen * 2;
	wFileSize = mbFileSize * 2;
	wfile = (wchar_t*)malloc(wFileSize);

	if (mbstowcs(wfile,FileName,wFileSize) == -1)
	{
		retVal = 0;
	}
	else	
	{
		retVal = DeleteFile(wfile);
	}

	free(wfile);
	return retVal;
#elif defined(WIN32) && !defined(_WIN32_WCE) && !defined(UNICODE)
	return(!DeleteFile((LPCTSTR)FileName));
#elif defined(POSIX)
	//return(remove(FileName));
	//modified by yunfeng_han@realsil.com.cn
	return (GFileDelete(FileName));
#elif defined(__SYMBIAN32__)
	return(ILibSymbian_FileDir_DeleteFile(FileName));
#endif
	return 0;
}
int ILibFileDir_DeleteDir(char *path)
{

#if defined(_WIN32_WCE) || (defined(WIN32) && defined(UNICODE))
	int retVal;
	wchar_t* wdirectory;		/* must MMS_FREE */
	int wDirLen;
	int wDirSize;
	int mbDirLen;
	int mbDirSize;
    DWORD  nError;

	mbDirLen = (int) strlen(path);
	mbDirSize = mbDirLen+1;
	wDirLen = mbDirLen * 2;
	wDirSize = mbDirSize * 2;

	wdirectory = (wchar_t*)malloc(wDirSize);

	if (mbstowcs(wdirectory,path,wDirSize) == -1)
	{
		retVal = 0;
	}
	else	
	{
		retVal = !RemoveDirectory(wdirectory);
	    if(retVal==0)
		{
			nError= GetLastError();
		}
	}

	free(wdirectory);
	return retVal;
#elif defined(WIN32) && !defined(_WIN32_WCE) && !defined(UNICODE)
	return(!RemoveDirectory((LPCTSTR)path));
#elif defined(POSIX)
	return(rmdir(path));
#elif defined(__SYMBIAN32__)
	return(ILibSymbian_FileDir_DeleteDir(path));	
#endif
	return 0;
}

int ILibFileDir_Delete(char* path)
{
	int count = 0;
	enum ILibFileDir_Type type = ILibFileDir_GetType(path);
	if( type == ILibFileDir_Type_FILE )
	{
		if(ILibFileDir_DeleteFile(path) == 0)
			count++;
	}
	else if(type == ILibFileDir_Type_DIRECTORY)
	{
		char filename[MAX_PATH_LENGTH];
		int filesize;
		void* handle = ILibFileDir_GetDirFirstFile(path, filename, MAX_PATH_LENGTH, &filesize);
		if(handle == NULL)
		{
			ILibFileDir_DeleteDir(path);
			count++;
		}else if ( !((strcmp(filename, ".") == 0) ||
					(strcmp(filename, "..") == 0)))
		{
            
            
            char fullpath[MAX_PATH_LENGTH];
            do{
                memset(fullpath,0,MAX_PATH_LENGTH);
                strcpy(fullpath,path);
                if(path[strlen(path) - 1] != '\\')
                    strcat(fullpath,"\\");
                strcat(fullpath,filename);
                count += ILibFileDir_Delete(fullpath);
            }while(ILibFileDir_GetDirNextFile(handle, path, filename, MAX_PATH_LENGTH, &filesize));

            ILibFileDir_CloseDir(handle);
			ILibFileDir_DeleteDir(path);
			count++;
         
		}
	}
	return count;
}



int ILibFileDir_CreateDir(char *path)
{
#if defined(_WIN32_WCE) || (defined(WIN32) && defined(UNICODE))
	int retVal;
	wchar_t* wdirectory;		/* must MMS_FREE */
	int wDirLen;
	int wDirSize;
	int mbDirLen;
	int mbDirSize;
	
	mbDirLen = (int) strlen(path);
	mbDirSize = mbDirLen+1;
	wDirLen = mbDirLen * 2;
	wDirSize = mbDirSize * 2;
	wdirectory = (wchar_t*)malloc(wDirSize);

	if (mbstowcs(wdirectory,path,wDirSize) == -1)
	{
		retVal = 0;
	}
	else	
	{
		retVal = !CreateDirectory(wdirectory, NULL);
	}

	free(wdirectory);
	return retVal;
#elif defined(WIN32) && !defined(_WIN32_WCE) && !defined(UNICODE)
	return(!CreateDirectory((LPCTSTR)path,NULL));
#elif defined(POSIX)
	return(mkdir(path,0));
#elif defined(__SYMBIAN32__)
	return(ILibSymbian_FileDir_CreateDir(path));
#endif
	return 0;
}

int ILibFileDir_MoveFile(char *OldFileName, char *NewFileName)
{
#if defined(_WIN32_WCE) || (defined(WIN32) && defined(UNICODE))
	int retVal;
	wchar_t* woldfile;		/* must MMS_FREE */
	int wOldFileLen;
	int wOldFileSize;
	int mbOldFileLen;
	int mbOldFileSize;
	wchar_t* wnewfile;		/* must MMS_FREE */
	int wNewFileLen;
	int wNewFileSize;
	int mbNewFileLen;
	int mbNewFileSize;


	mbOldFileLen = (int) strlen(OldFileName);
	mbOldFileSize = mbOldFileLen+1;
	wOldFileLen = mbOldFileLen * 2;
	wOldFileSize = mbOldFileSize * 2;
	woldfile = (wchar_t*)malloc(wOldFileSize);

	mbNewFileLen = (int) strlen(NewFileName);
	mbNewFileSize = mbNewFileLen+1;
	wNewFileLen = mbNewFileLen * 2;
	wNewFileSize = mbNewFileSize * 2;
	wnewfile = (wchar_t*)malloc(wNewFileSize);

	if (mbstowcs(woldfile,OldFileName,wOldFileSize) == -1)
	{
		retVal = 0;
	}
	else if(mbstowcs(wnewfile,NewFileName,wNewFileSize) == -1)
	{
		retVal = 0;
	}
	else	
	{
		retVal = !MoveFile(woldfile, wnewfile);
	}

	free(woldfile);
	free(wnewfile);
	return retVal;
#elif defined(WIN32) && !defined(_WIN32_WCE) && !defined(UNICODE)
	return(!MoveFile((LPCTSTR)OldFileName, (LPCTSTR)NewFileName));
#elif defined(POSIX)
	//modified by yunfeng_han@realsil.com.cn
	//return(rename(OldFileName, NewFileName));
	return (GFileRename(OldFileName, NewFileName));
#elif defined(__SYMBIAN32__)
	return(ILibSymbian_FileDir_MoveFile(OldFileName,NewFileName));
#endif
	return 0;
}

long ILibFileDir_GetFileSize(char *FileName)
{
	/*
	long SourceFileLength;
	FILE *SourceFile = fopen(FileName,"rb");

	if(SourceFile==NULL)
	{
		return(-1);
	}

	fseek(SourceFile,0,SEEK_END);
	
	SourceFileLength = ftell(SourceFile);
	fclose(SourceFile);
	return(SourceFileLength);
	*/

	//modified by yunfeng_han@realsil.com.cn
	gfile_t * fh;
	long SourceFileLength;
//	printf("In Get File Size %s\n",FileName);
	fh = GFileOpen(FileName, O_RDONLY | 0x20000000, GFILE_TYPE_ARRAY);
	if(fh ==NULL)return -1;
	SourceFileLength = GgetFileSize(fh);
	GFileClose(fh);
	return(SourceFileLength);
}

time_t ILibFileDir_GetFileTimeStamp(char *FileName)
{	
#if defined(WIN32) || defined(_WIN32_WCE)
	HANDLE* hFile;
	WIN32_FIND_DATA FileData;
	LONGLONG time;

	#if defined(_WIN32_WCE) || (defined(WIN32) && defined(UNICODE))
	wchar_t *tempChar;
	int tempCharLength;

	tempCharLength = MultiByteToWideChar(CP_UTF8,0,FileName,-1,NULL,0);
	tempChar = (wchar_t*)malloc(sizeof(wchar_t)*tempCharLength);
	MultiByteToWideChar(CP_UTF8,0,FileName,-1,tempChar,tempCharLength);
	hFile = FindFirstFile(tempChar, &FileData);
	free(tempChar);
	#else
	hFile = FindFirstFile(FileName, &FileData);
	#endif

	if(hFile == NULL) return -1;
    // Retrieve the last modified timestamp for the file.

	time = (__int64) FileData.ftLastWriteTime.dwHighDateTime << 32;;
	time = ((time + FileData.ftLastWriteTime.dwLowDateTime - 116444736000000000) / 10000000);

	FindClose(hFile);
	return (time_t) time; 
#else
	return -1;
#endif
}
