LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_C_INCLUDES = \
	$(LOCAL_PATH)/include/upnp3

LOCAL_SRC_FILES := \
	upnp3/ILibParsers.c \
	upnp3/ILibAsyncSocket.c \
	upnp3/ILibAsyncUDPSocket.c \
	upnp3/ILibAsyncServerSocket.c \
	upnp3/ILibWebClient.c \
	upnp3/ILibWebServer.c \
	upnp3/ILibThreadPool.c \
	upnp3/DMR_MicroStack.c \
	upnp3/MediaServerCP_ControlPoint.c \
	upnp3/ILibSSDPClient.c \
	upnp3/CdsObjects/CdsDidlSerializer.c \
	upnp3/CdsObjects/CdsErrors.c \
	upnp3/CdsObjects/CdsMediaClass.c \
	upnp3/CdsObjects/CdsObject.c \
	upnp3/PlaylistTrackManager/BitArray.c \
	upnp3/PlaylistTrackManager/CircularBuffer.c \
	upnp3/MediaServerBrowser/FilteringBrowser.c \
	upnp3/MediaServerBrowser/MediaServerControlPoint.c \
	upnp3/MediaRenderer/DMR.c \
	upnp3/PlaySingleUri/PlaySingleUri.c \
	upnp3/ProtocolInfoParser/DLNAProtocolInfo.c \
	upnp3/StringUtils/MimeTypes.c \
	upnp3/StringUtils/UTF8Utils.c \
	upnp3/MyString.c \
	upnp3/PlaylistTrackManager/IndexBlocks.c \
	upnp3/PlaylistTrackManager/PlayListManager.c \
	upnp3/PlaylistTrackManager/PlaylistManagerPC.c \
	upnp3/PlaylistTrackManager/PlaylistManagerS.c \
	upnp3/MediaServerAbstraction/MediaServerAbstraction.c \
	upnp3/MediaServer_MicroStack.c

LOCAL_CFLAGS := \
	-DRUN_ON_MAGELLAN \
	-DINCLUDE_FEATURE_VOLUME \
	-D_POSIX \
	-D_DEBUG \
	-D_VERBOSE \
	-D_UNIX \
	-DMMSCP_LEAN_AND_MEAN \
	-D_MT_CONNECTION_MANAGER \
	-D_MT_RENDERINGCONTROL \
	-D_MT_AVTRANSPORT \
	-DSYS_UNIX:=1 \
	-D_REENTRANT \
	-DDLNADMRCTT \
	-DANDROID_DLNA_WORKAROUND \
	-g
	
LOCAL_MODULE_TAGS := optional
LOCAL_CFLAGS += -DANDROID_PLATFORM
LOCAL_MODULE := libupnp

include $(BUILD_SHARED_LIBRARY)
