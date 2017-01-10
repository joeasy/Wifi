INCLUDE(../../../../include/config.txt)

SET(LIBNAME upnp3)
SET(LIBRARY_OUTPUT_PATH ../../../../lib/)





SET(SRCS ILibParsers.c      ILibAsyncSocket.c       ILibAsyncUDPSocket.c
ILibAsyncServerSocket.c        ILibWebClient.c      ILibWebServer.c
ILibThreadPool.c        DMR_MicroStack.c MediaServerCP_ControlPoint.c
ILibSSDPClient.c        ./HttpFiles/DlnaHttp.c ./HttpFiles/DlnaHttpClient.c
./HttpFiles/DlnaHttpServer.c ./CdsObjects/CdsDidlSerializer.c
./CdsObjects/CdsErrors.c ./CdsObjects/CdsMediaClass.c ./CdsObjects/CdsObject.c
./PlaylistTrackManager/BitArray.c ./PlaylistTrackManager/CircularBuffer.c
./MediaServerBrowser/FilteringBrowser.c
./MediaServerBrowser/MediaServerControlPoint.c ./MediaRenderer/DMR.c
./PlaySingleUri/PlaySingleUri.c ./ProtocolInfoParser/DLNAProtocolInfo.c
./StringUtils/MimeTypes.c ./StringUtils/UTF8Utils.c MyString.c
./PlaylistTrackManager/IndexBlocks.c  ./PlaylistTrackManager/PlayListManager.c
./PlaylistTrackManager/PlaylistManagerPC.c
./PlaylistTrackManager/PlaylistManagerS.c
./FileAbstractionFiles/FileIoAbstraction.c
./MediaServerAbstraction/MediaServerAbstraction.c
./DownloadController/DownloadController.c ./DmsIntegration/DmsIntegration.c
./DmsIntegration/FileSystemEnumerator/FileSystemEnumerator.c MediaServer_MicroStack.c)

SET(CC " ${MYCC}")
SET(LD " ${MYCC}")
SET(AR " ${MYAR}")

SET(OPTIMIZE_SIZE " -ffunction-sections -fdata-sections")


SET(INCLUDES ${INCLUDES} " ${CMAKE_CURRENT_SOURCE_DIR}/.")
SET(INCLUDES ${INCLUDES} " ${CMAKE_CURRENT_SOURCE_DIR}/../../../Include/Utility/upnp3/")
SET(INCLUDES  ${INCLUDES} " ${CMAKE_CURRENT_SOURCE_DIR}/../../../Include/")

SET(CFLAGS " -ggdb -Wall -DINCLUDE_FEATURE_DISPLAY  -DINCLUDE_FEATURE_VOLUME -D_POSIX -D_DEBUG -D_VERBOSE   -D_UNIX -DMMSCP_LEAN_AND_MEAN -D_MT_CONNECTION_MANAGER -D_MT_RENDERINGCONTROL -D_MT_AVTRANSPORT -DSYS_UNIX=1 -D_REENTRANT")


SET(CXXFLAGS " ${CFLAGS}")
SET(LFLAGS " -lpthread -lm  -Wl,--gc-sections -static")
SET(LFLAGS ${LFLAGS} " -L../../../../lib -L./ -lfileaccess -lpthread -lOSAL -lStreamingEngine -lMoveDataEngine")







INCLUDE_DIRECTORIES(${INCLUDES})
ADD_DEFINITIONS(${CFLAGS} ${DEBUGFLAG} ${WARNING})
ADD_LIBRARY(${LIBNAME} ${SRCS})
string(REGEX REPLACE  ";" " " APCFG_DVR  ${APCFG_DVR})
string(REGEX REPLACE  ";" " " CHIP_MODEL_DEFS ${CHIP_MODEL_DEFS})
string(REGEX REPLACE  ";" " " APCFG_DEFS ${APCFG_DEFS})
#string(REGEX REPLACE  ";" " " BUILD_OPTIONS ${BUILD_OPTIONS})
string(REGEX REPLACE  ";" " " DEFINES ${DEFINES})
string(REGEX REPLACE  ";" " " APCFG_FLAGS ${APCFG_FLAGS})
