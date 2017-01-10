#ifndef _PLI_H_
#define _PLI_H_

#include "Types.h"

#ifdef KERNEL_2_6_34
#define DEF_MEM_SIZE    0x20000000      // 512  MB
#else
#define DEF_MEM_SIZE    0x10000000      // 256  MB
#endif

//#define TARGET_BOARD
#define DEBUG_MODE

#ifdef	DEBUG_MODE
#define pli_allocContinuousMemory(a, b, c) pli_allocContinuousMemoryMesg("", (a), (b), (c))
#define pli_freeContinuousMemory(a) pli_freeContinuousMemoryMesg("", (a))
#define MESG_LENGTH 10

#define AUDIO_FLAG	0x10000000
#define VIDEO_FLAG	0x20000000
#define CPU_MASK	0xf0000000

typedef struct alloc_record
{
        int                     addr;
	int			flag;
	int			requested_size;
	int			allocated_size;
        char                    mesg[MESG_LENGTH];
        struct alloc_record     *next;
} alloc_record;
#else
#define pli_allocContinuousMemoryMesg(m, a, b, c) pli_allocContinuousMemory((a), (b), (c))
#define pli_freeContinuousMemoryMesg(m, a) pli_freeContinuousMemory((a))
#endif

#ifdef	TARGET_BOARD
#define DNAME_INLINE_LEN_MIN	36

typedef struct {
        unsigned long           addr;
        unsigned long           prot;
} watch_struct;

typedef struct {
        int                     fd;
	char                    name[DNAME_INLINE_LEN_MIN];
        unsigned long           addr;
} dirty_page_struct;

typedef struct {
        unsigned long           addr;
        unsigned long           size;
} sched_log_struct;

#define WATCH_IOC_MAGIC                         'j'
#define WATCH_IOQGETASID                        _IO(WATCH_IOC_MAGIC, 1)
#define WATCH_IOSSETWATCH                       _IOW(WATCH_IOC_MAGIC, 2, watch_struct)
#define WATCH_IOTCLRWATCH                       _IO(WATCH_IOC_MAGIC, 3)

#define WATCH_W		0x1
#define WATCH_R 	0x2
#define WATCH_I		0x4

#define RPC_IOC_MAGIC  'k'
#define RPC_IOCTRESET _IO(RPC_IOC_MAGIC,  2)
#endif

#ifdef __cplusplus
extern "C" {
#endif

  typedef char COMM_STR[16];

  typedef struct _tagTIME {
    int    tm_sec  ; /* seconds [0,61]                 */
    int    tm_min  ; /* minutes [0,59]                 */
    int    tm_hour ; /* hour [0,23]                    */
    int    tm_mday ; /* day of month [1,31]            */
    int    tm_mon  ; /* month of year [0,11]           */
    int    tm_year ; /* years since 1900               */
    int    tm_wday ; /* day of week [0,6] (Sunday = 0) */
    int    tm_yday ; /* day of year [0,365]            */
    int    tm_isdst; /* daylight savings flag          */
  } TIME;

  // *** Timer
  int64_t pli_getSCR();            /* 27MHz */
  int64_t pli_getPTS();            /* 90KHz */
  int     pli_getTime(TIME *time);
  int64_t pli_getMilliseconds();   /* a monotonically increasing system clock */

  // *** memory access
#ifdef	TARGET_BOARD
  // initialize the system
  // it will return 0 if it succeeds
  int     pli_init();

  // close the pli interface
  int     pli_close();

  void    pli_showInfo();

  // free all memory allocated by pli interface
  void    pli_freeAllMemory();
  
  // free all memory cache in kernel
  void    pli_clearCacheMemory();

  // start to detect if one thread occupy CPU too long
  void    pli_startDetectOccupy(int ticks);

  // disable all interrupt
  void    pli_disableInterrupt();
  
  // enable all interrupt
  void    pli_enableInterrupt();
  
  // get the address of the memory mapped io registers
  int*    pli_getIOAddress(int addr);

  // set the watchpoint register
  // return zero means it fail to set watchpoint 
  int     pli_setWatchPoint(unsigned long addr, unsigned long prot);

  // clear the watchpoint register
  // return zero means it fail to clear watchpoint 
  int     pli_clrWatchPoint();

  // get the watchpoint register
  // return zero means it fail to get watchpoint 
  int     pli_getWatchPoint(unsigned long *addr, unsigned long *prot);

  // reset remote procedure call 
  void    pli_resetRPC();

  // alloc the log buffer and initialize the scheduling log mechanism
  int     pli_initLog(int bufsize);

  // end the log mechanism and free the log buffer
  int     pli_freeLog();

  // start the scheduling log mechanism
  int     pli_startLog();

  // stop the scheduling log mechanism
  int     pli_stopLog(char *logfile);

  // log the happening of events
  int     pli_logEvent(int event);

  // change the name of calling thread
  int     pli_setThreadName(char *);

  // start the mechanism of logging dirty pages
  int     pli_startRecordDirty(dirty_page_struct *dirty);

  // end the mechanism of logging dirty pages
  int     pli_endRecordDirty();

  int     pli_syncMetaData(int fd_dev);
#endif	//	TARGET_BOARD

  // list the information of the memory allocated by pli interface
  void    pli_listAllMemory();
  
  // get the start virtual address of pli
  int     pli_getStartAddress();
  // jason added : get end of graphic virtual address
  int     pli_getGraphicEndAddress();

#ifdef	DEBUG_MODE
  // function return cached virtual address
  // phyAddress will be used while passing this address to other CPU
  void*   pli_allocContinuousMemoryMesg(char *str, size_t size, BYTE** nonCachedAddr, unsigned long *phyAddr); // allocate from system reserved memory block
	#ifdef MEM_MANAGE  
	  void pli_freeContinuousMemoryManage(char *str,void *mem_addr);
	  void *pli_allocContinuousMemoryManage(char *str,size_t size,unsigned long * nonCachedAddr,unsigned long *pPhyAddr);
	#else
  	  #define   pli_freeContinuousMemoryManage pli_freeContinuousMemoryMesg
	  #define   pli_allocContinuousMemoryManage(a,b,c,d) pli_allocContinuousMemoryMesg(a,b,(BYTE**)c,d)
	#endif  
#else
  // function return cached virtual address. nonCachedAddr will be used for single access when flushing is not efficient
  // phyAddress will be used while passing this address to other CPU
  void*   pli_allocContinuousMemory(size_t size, BYTE** nonCachedAddr, unsigned long *phyAddr); // allocate from system reserved memory block
#endif
  
  // flush the cached virtual address. There is no need to call this function if use non-cached virtual address 
  void    pli_flushMemory(void* ptr, long size);

  // flush the cached whose physical address fell between phyStart and phyEnd
  void    pli_flushRange(unsigned long phyStart, unsigned long phyEnd);
  void    pli_flushRangeEx(unsigned long* addr, unsigned long *size, int count); //+JT

  // prefetch the virtual address data. There is no need to call this function if use non-cached virtual address 
  void    pli_prefetchCache(void* ptr, long size);

  // hardware accelerated copy from DDR to DDR. Note, this function will not flush the cache
  // function accept physical address only 
  // function return serial number, user can use this number to check whether the copy complete or not
  // return zero means the command is not issued or issued but failed
  uint64_t pli_ddrCopy(void *dest, void* src, long size);

  // Check task has been finished or or not by serial number.
  // if task has been finished, return 1, else return 0. 
  // You can test the completion by check return value > 0.
  long    pli_checkTaskCompletion(uint64_t serialNumber);

#ifdef	DEBUG_MODE
  // use cached virtual address to free the memory
  void    pli_freeContinuousMemoryMesg(char *str, void *ptr);
#else
  // use cached virtual address to free the memory
  void    pli_freeContinuousMemory(void *ptr);
#endif

  // Inter-processor write memroy, will perform swap 4 bytes (endian swap) and write to the destination if cross CPU with different endianess
  void    pli_IPCWriteULONG(BYTE* des, unsigned long value);
#ifndef WIN32
  void    pli_IPCWriteULONGLONG(BYTE* des, unsigned long long value);
#endif

  // Inter-processor write memroy, will perform swap 4 bytes (endian swap) and write to the destination if cross CPU with different endianess
  unsigned long         pli_IPCReadULONG(BYTE* src);
#ifndef WIN32
  unsigned long long    pli_IPCReadULONGLONG(BYTE* src);
#endif


#ifdef TARGET_BOARD
  // Inter-processor copy memory, will perform swap each 4 bytes (endian swap) and write to the destination if cross CPU with different endianess
  void    pli_IPCCopyMemory(BYTE* src, BYTE* des, unsigned long len);

  // function return virtual memory of framebuffer
  void*   pli_allocGraphicMemory(size_t size, unsigned long *phyAddr);

  // use graphic virtual memroy to free the memory
  void    pli_freeGraphicMemory(void *ptr);
#endif	//	TARGET_BOARD

  // get hardware semaphore, return 1 when get the semaphore, otherwise return 0 
  long pli_lockHWSem();

  // release the hardware semaphore
  void pli_unlockHWSem();

  // This function should be called from application to initial hardware
  long pli_initHWSem();

  // get hardware semaphore, return 1 when get the semaphore, otherwise return 0 
  long pli_hwLock();

  // release the hardware semaphore
  void pli_hwUnlock();

  // This function should be called from application to initial hardware
  long pli_hwInit();

#ifdef __cplusplus
}
#endif

#endif 
