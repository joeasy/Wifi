
#
# Intel's UPnP MicroStack/1.0, Sample MakeFile */
# Intel Device Builder - Build#1.0.2718
#

# List all object files here

-include ../../../src/Tools/MConfig/.config
include ../../../include/MakeConfig

UPNP_LIB_OBJ_FILES = \
	ILibParsers.o\
	ILibAsyncSocket.o\
	ILibAsyncUDPSocket.o\
	ILibAsyncServerSocket.o\
	ILibWebClient.o\
	ILibWebServer.o\
	ILibThreadPool.o\
	DMR_MicroStack.o\
MediaServerCP_ControlPoint.o\
ILibSSDPClient.o\
	DlnaHttp.o \
DlnaHttpClient.o \
DlnaHttpServer.o \
CdsDidlSerializer.o \
CdsErrors.o \
CdsMediaClass.o \
CdsObject.o \
BitArray.o \
CircularBuffer.o \
FilteringBrowser.o \
MediaServerControlPoint.o \
DMR.o \
PlaySingleUri.o \
DLNAProtocolInfo.o \
MimeTypes.o \
UTF8Utils.o \
MyString.o \
IndexBlocks.o \
PlayListManager.o \
PlaylistManagerPC.o \
PlaylistManagerS.o \
FileIoAbstraction.o \
MediaServerAbstraction.o \
DownloadController.o \
DmsIntegration.o \
FileSystemEnumerator.o \
MediaServer_MicroStack.o\


OTHER_OBJS =\
Main.o \

# Compiler command name
#CC = mipsel-linux-gcc
CC = $(MYCC)
LD = $(MYCC)
AR = $(MYAR)

# Compiler flags applied to all files
# Optional flags: -D_VERBOSE -D_DEBUG -D_DEBUG_CODECWRAPPER -D_TEMPDEBUG
# If enabling the TRANSITION state, then you need the _USE_TRANSIT_STATE #define.
# If enabling M3U caching, then you need the _M3U_BUFFER #define.
# If debugging playlist parser, use -D_PLAYLIST_TRACE
# -g puts debug symbols
#

OPTIMIZE_SIZE = -ffunction-sections -fdata-sections

#INCLUDE_PATH = -I. -I../  -I./HttpFiles  -I./CdsObjects  -I./MediaServerBrowser  -I./MediaRenderer  -I./PlaySingleUri  -I./ProtocolInfoParser  -I./StringUtils -I./PlaylistTrackManager \
#-I../../../Include/Utility/upnp1.5/
INCLUDE_PATH = -I. -I../../../src/Include/Utility/upnp3/ -I../../../src/Include/

CFLAGS = -ggdb -Wall \
	-DINCLUDE_FEATURE_DISPLAY  -DINCLUDE_FEATURE_VOLUME -D_POSIX -D_DEBUG -D_VERBOSE \
	-D_UNIX -DMMSCP_LEAN_AND_MEAN -D_MT_CONNECTION_MANAGER -D_MT_RENDERINGCONTROL -D_MT_AVTRANSPORT -DSYS_UNIX=1 -D_REENTRANT \
	$(INCLUDE_PATH)	$(DEFINES) #$(OPTIMIZE_SIZE)	-Os 

CXXFLAGS = $(CFLAGS)

LFLAGS = -lpthread -lm  -Wl,--gc-sections -static
LFLAGS += -L../../../lib_release/prebuilt_$(TOOLCHAIN_VER) -L../../../lib_release -L./ -lfileaccess -lpthread -lOSAL -lStreamingEngine -lMoveDataEngine
#LIBS = -lpthread -lm 
# Name of the executable
#VPATH =  ./ ../  ./HttpFiles  ./CdsObjects  ./MediaServerBrowser  ./MediaRenderer  ./PlaySingleUri  ./ProtocolInfoParser  ./StringUtils ./PlaylistTrackManager  
VPATH =  ./  ./HttpFiles  ./CdsObjects  ./MediaServerBrowser  ./MediaRenderer  ./PlaySingleUri  ./ProtocolInfoParser  ./StringUtils ./PlaylistTrackManager ./MediaServerAbstraction ./DownloadController  ./DmsIntegration  ./DmsIntegration/FileSystemEnumerator ./FileAbstractionFiles
EXENAME = Main

# Builds all object files and executable
${EXENAME} : $(OTHER_OBJS) $(UPNP_LIB_OBJ_FILES) 
	$(AR) cru libupnp3.a $(UPNP_LIB_OBJ_FILES)
	cp libupnp3.a ../../../lib_release/prebuilt_$(TOOLCHAIN_VER)/libupnp3.a
	#$(CC) -o ${EXENAME} $(UPNP_LIB_OBJ_FILES)$(OTHER_OBJS) $(LFLAGS)
	#$(CC) $(CFLAGS) -o ${EXENAME} $(UPNP_LIB_OBJ_FILES) $(LIBS)


# Macro rule for all object files.
#$(UPNP_LIB_OBJ_FILES) : \
#	DMR_MicroStack.h\
#MediaServerCP_ControlPoint.h\
#ILibSSDPClient.h\


# Clean up
clean :
	rm -f $(UPNP_LIB_OBJ_FILES) ${EXENAME} ${EXENAME}.exe libupnp3.a $(OTHER_OBJS)
	rm -f ../../../lib_release/prebuilt_$(TOOLCHAIN_VER)/libupnp3.a

