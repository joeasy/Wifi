#include <OSAL.h>
//#include "system.h"

#ifndef WIN32
#    include <stdlib.h>
#    include <semaphore.h>
#    include <pthread.h>
#    include <assert.h>
#    include <string.h>
#    include <fcntl.h>
#    include <unistd.h>
// #    include "system.h"
#else
#    include <windows.h>
#    include <sys/timeb.h>
#endif

// related to time
#define MILLI2SEC(Milli)                        ((Milli)/1000)
#define MILLI2NANO(Milli)                       (1000000* (Milli))

#define NANO2SEC(NANO)                          ((NANO)/1000000000)
#define NANO2MILLI(NANO)                        ((NANO)/1000000)

#define SEC2MILLI(SEC)                          (1000*(SEC))

#define ADD_MILLI_TIME(AbsTime, milliDelay)                                     \
{                                                                               \
    clock_gettime(CLOCK_REALTIME, &AbsTime);                                    \
    AbsTime.tv_nsec+=           MILLI2NANO(milliDelay);                         \
    AbsTime.tv_sec+=            NANO2SEC(AbsTime.tv_nsec);                      \
    AbsTime.tv_nsec%=           1000000000;                                     \
}

#ifndef __set_errno
# define __set_errno(val) (*__errno_location ()) = (val)
#endif
#ifndef SYS_ify
# define SYS_ify(syscall_name)  (__NR_##syscall_name)
#endif

#define _syscall0(type,name) \
        type name(void) \
{                                                                       \
        long err;                                                       \
        long sys_result;                                                \
        {                                                               \
        register unsigned long __v0 asm("$2");                          \
        register unsigned long __a3 asm("$7");                          \
        __asm__ volatile (                                              \
        ".set   noreorder\n\t"                                          \
        "li     $2, %2  # " #name "\n\t"                                \
        "syscall\n\t"                                                   \
        ".set reorder"                                                  \
        : "=r" (__v0), "=r" (__a3)                                      \
        : "i" (SYS_ify(name))                                           \
        : "$1", "$3", "$8", "$9", "$10", "$11", "$12", "$13",           \
                "$14", "$15", "$24", "$25", "memory");                  \
        err = __a3;                                                     \
        sys_result = __v0;                                              \
        }                                                               \
        if (err == 0)                                                   \
                return (type) sys_result;                               \
        __set_errno(sys_result);                                        \
        return (type)-1;                                                \
}

#undef __NR_fork
#define __NR_fork (4000 + 2)
#define SYS_fork __NR_fork

#ifndef ANDROID
#define __NR_myfork __NR_fork
_syscall0(pid_t, myfork);

int mysystem(const char *command)
{
        int wait_val, pid;
        __sighandler_t save_quit, save_int, save_chld;

        printf("In my system...\n");
        if (command == 0)
                return 1;

        save_quit = signal(SIGQUIT, SIG_IGN);
        save_int = signal(SIGINT, SIG_IGN);
        save_chld = signal(SIGCHLD, SIG_DFL);

        if ((pid = myfork()) < 0) {
                signal(SIGQUIT, save_quit);
                signal(SIGINT, save_int);
                signal(SIGCHLD, save_chld);
                return -1;
        }
        if (pid == 0) {
                signal(SIGQUIT, SIG_DFL);
                signal(SIGINT, SIG_DFL);
                signal(SIGCHLD, SIG_DFL);
                execl("/bin/sh", "sh", "-c", command, (char *) 0);
                _exit(127);
        }
        /* Signals are not absolutly guarenteed with vfork */
        signal(SIGQUIT, SIG_IGN);
        signal(SIGINT, SIG_IGN);

        if (wait4(pid, &wait_val, 0, 0) == -1)
                wait_val = -1;

        signal(SIGQUIT, save_quit);
        signal(SIGINT, save_int);
        signal(SIGCHLD, save_chld);
        return wait_val;
}
#endif

/*#ifndef __USE_OSAL_API__


// Critical Section
// This is the Mutex in the same process between different threads

CCritSec::CCritSec(void)
{
#ifndef WIN32
	m_pCritSec = malloc(sizeof (pthread_mutex_t));
	pthread_mutex_init((pthread_mutex_t*) m_pCritSec, NULL);
#else
	m_pCritSec = malloc(sizeof (CRITICAL_SECTION));
	InitializeCriticalSection((CRITICAL_SECTION *)m_pCritSec);
#endif
};

CCritSec::~CCritSec(void)
{
#ifndef WIN32
	pthread_mutex_destroy((pthread_mutex_t*) m_pCritSec);
#else
	DeleteCriticalSection((CRITICAL_SECTION *)m_pCritSec);
#endif
	free(m_pCritSec);
}

void CCritSec::Lock(void)
{
#ifndef WIN32
	pthread_mutex_lock((pthread_mutex_t*) m_pCritSec);
#else
	EnterCriticalSection((CRITICAL_SECTION *)m_pCritSec);
#endif
}

void CCritSec::Unlock(void)
{
#ifndef WIN32
	pthread_mutex_unlock((pthread_mutex_t*) m_pCritSec);
#else
	LeaveCriticalSection((CRITICAL_SECTION *)m_pCritSec);
#endif
}
// -------------------------------------------------------


// Critical Section

CAutoLock::CAutoLock(CCritSec* pLock): m_pLock(pLock)
{
//	DASSERT(m_pLock!= NULL);
	m_pLock->Lock();
}
CAutoLock::~CAutoLock(void)
{
	m_pLock->Unlock();
}


// Semaphore :: named or unnamed semaphore, use named semaphore for inter-process lock


CSemaphore::CSemaphore(): m_pSem(0), m_name(0)
{
}

CSemaphore::~CSemaphore()
{
#ifndef WIN32
	if (m_name== 0) {
		if (m_pSem!= 0)
			free(m_pSem);
	}
	else {
		sem_close((sem_t *)m_pSem);
		sem_unlink(m_name);		
	}
	if (m_name!=0)
		free(m_name);
#else
	CloseHandle(m_pSem);
#endif
}

#define MAXIMUM_SEM_VALUE 10000000 // @port, hard to decide this value for windows
void CSemaphore::Init(char *name, unsigned long initValue)
{
#ifndef WIN32
	if (name == 0) {
		m_pSem = (sem_t *) malloc(sizeof(sem_t));
		assert(m_pSem!=0);
		sem_init((sem_t *)m_pSem, 0, initValue);
	}
	else {
		m_name = (char*)malloc(strlen(name)+1);
		strcpy(m_name, name);
		m_pSem = (void *) sem_open(name, O_CREAT, 0644, 0);
	}
#else
	m_pSem = (HANDLE)CreateSemaphore(NULL, initValue, MAXIMUM_SEM_VALUE ,name); 
#endif

}

void  CSemaphore::Reset(unsigned long resetValue)
{
    assert(m_pSem!= NULL);
#ifndef WIN32
    sem_init((sem_t *)m_pSem, 0, resetValue);
#else
    // havn't implemented yet
    assert(0);
#endif
}

void CSemaphore::Wait() 
{
#ifndef WIN32
	sem_wait((sem_t *)m_pSem);
#else
	WaitForSingleObject( (HANDLE) m_pSem, INFINITE);
#endif
}
void CSemaphore::Post()
{
#ifndef WIN32
	sem_post((sem_t *)m_pSem);
#else
	ReleaseSemaphore((HANDLE) m_pSem, 1, NULL);
#endif

}

bool CSemaphore::TimedWait(unsigned long milliseconds)
{
#ifndef WIN32
    if ((long)milliseconds== TIME_INFINITY){
        sem_wait((sem_t *)m_pSem);
        return true;
    }

    struct timespec         AbsTime;
    ADD_MILLI_TIME(AbsTime, milliseconds);
    if (sem_trywait((sem_t *)m_pSem)!= 0){
        // return Time Out; //????
      unsigned long i;
      for (i=0; i< milliseconds; i++){
	usleep(i*1000);
	if (sem_trywait((sem_t *)m_pSem)== 0)
	  return true;
      }
        return false;
    }
#else
    if (milliseconds== TIME_INFINITY){
        WaitForSingleObject( (HANDLE) m_pSem, INFINITE);
        return true;
    }
    if (WAIT_TIMEOUT == WaitForSingleObject( (HANDLE) m_pSem, milliseconds) )
        return false;
#endif
    return true;
}

#endif
*/
/*CPolling::CPolling(
    long                    TotalTime,
    long                    SleepSlot):
    // m_msTotalTime(TotalTime),
    m_milliSleepSlot(SleepSlot)
{
#ifndef WIN32
    m_pTimeOut= malloc(sizeof(timespec));
    if (TotalTime== TIME_INFINITY){
        ((timespec*)m_pTimeOut)->tv_sec= 
	((timespec*)m_pTimeOut)->tv_nsec= -1;
    }
    else{
        clock_gettime(CLOCK_REALTIME, (timespec*)m_pTimeOut);
        ((timespec*)m_pTimeOut)->tv_nsec+= MILLI2NANO(TotalTime);
        ((timespec*)m_pTimeOut)->tv_sec+= NANO2SEC(((timespec*)m_pTimeOut)->tv_nsec);
    }
#else
    m_pTimeOut= (_timeb*)malloc(sizeof(_timeb));
    if (TotalTime== TIME_INFINITY){
        ((_timeb*)m_pTimeOut)->time= -1;
	((_timeb*)m_pTimeOut)->millitm= 0;
    }
    else{
        _ftime((_timeb*)m_pTimeOut);
        ((_timeb*)m_pTimeOut)->millitm+= (unsigned short)TotalTime;
	((_timeb*)m_pTimeOut)->time+= MILLI2SEC(((_timeb*)m_pTimeOut)->millitm);
    }
#endif
}

CPolling::~CPolling(void)
{
    free(m_pTimeOut);
}

void
CPolling::Sleep(void)
{
    // m_msTotalTime-= m_milliSleepSlot;
    osal_Sleep(m_milliSleepSlot);
}

bool
CPolling::IsTimeOut(void)
{
#ifndef WIN32
    // polling infinitely
    if (((timespec*)m_pTimeOut)->tv_sec== -1)
        return false;

    struct timespec     CurrentTime;
    clock_gettime(CLOCK_REALTIME, &CurrentTime);
    if (CurrentTime.tv_sec> ((timespec*)m_pTimeOut)->tv_sec)
        return true;
    else if (CurrentTime.tv_nsec>= ((timespec*)m_pTimeOut)->tv_nsec)
        return true;
    return false;
#else
    // polling infinitely
    if (((_timeb*)m_pTimeOut)->time== -1)
        return false;

    struct _timeb       CurrentTime;
    _ftime(&CurrentTime);
    if (CurrentTime.time> ((_timeb*)m_pTimeOut)->time)
        return true;
    else if (CurrentTime.millitm>= ((_timeb*)m_pTimeOut)->millitm)
        return true;
    return false;
#endif
}*/


//------------------------------------------------------------------------------
unsigned long g_userInputTotalCount =0;    // the count of total input keys user has pressed
unsigned long g_userInputHandledCount = 0; // the count of total input keys that have been handled by processKey
int64_t g_userInputTime = 0; // the time when the user has pressed the input key (in 90k clock)

short g_userInputKeyType;

// isKeyPressed
// function return 0 means no pending user input. Otherwise, return the number pending for process.
long osal_isKeyPressed()
{
  return g_userInputTotalCount - g_userInputHandledCount;
}

//function returns type of the key pending for handle
short osal_getKeyType()
{
	return g_userInputKeyType;
}





//------------------------------------------------------------------------------
// general functions
//

// Jefferson Mark: these functions increases code size.
// no one seems to use them.
#if 0
#ifndef WIN32 // LINUX implementation

#include <dlfcn.h>

/* load DLL/SO library */
void* osal_OpenLibrary(char* path, int bLazy)
{
    int mode = bLazy? RTLD_LAZY : RTLD_NOW;
    mode |= RTLD_GLOBAL;
    return dlopen(path, mode);
}

/* unload DLL/SO library */
HRESULT osal_CloseLibrary(void* lib_handle)
{
    return dlclose(lib_handle) == 0 ? S_OK : E_FAIL;
}

/* get function address from DLL/SO library */
void* osal_GetFuncAddress(void* lib_handle, char* func_name)
{
    return dlsym(lib_handle, func_name);
}

#else // Windows implementation

/* load DLL/SO library */
void* osal_OpenLibrary(char* path, int bLazy)
{
    return (void*)LoadLibrary(path);
}

/* unload DLL/SO library */
HRESULT osal_CloseLibrary(void* lib_handle)
{
    return FreeLibrary((HMODULE)lib_handle)? S_OK : E_FAIL; 
}

/* get function address from DLL/SO library */
void* osal_GetFuncAddress(void* lib_handle, char* func_name)
{
    return (void*)GetProcAddress((HMODULE)lib_handle, func_name);
}

#endif
#endif // if 0


/*CThread::CThread(                                                                                                                                                                        
     void (*start_address)(void*),
     void* pThreadData) 
 {        
     m_start_address = start_address;
     m_pThreadData = pThreadData;
     m_Flag = 0;
     RESET_RUN();
 }    
 
 CThread::~CThread(void)
 {    
     if(IS_RUN()) {
         //printf("CThread::~CThread: (WARNING) thread was not explicitly terminated, destructor will try ...\n");
         Exit(true);
     }
 }    
 */

bool osal_MutexCreate(
    osal_mutex_t* pMutexID)
{
    return true;
}

bool osal_MutexLock(
    osal_mutex_t* pMutexID)
{
    return true;
}

bool osal_MutexTryLock(
    osal_mutex_t* pMutexID)
{
    return true;
}

bool osal_MutexUnlock(
    osal_mutex_t* pMutexID)
{
    return true;
}
bool osal_MutexDestroy(
    osal_mutex_t* pMutexID)
{
    return true;
}
