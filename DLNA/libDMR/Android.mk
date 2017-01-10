LOCAL_PATH := $(my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	./OSAL.cpp \
	./DLNA_DMR.cpp \
	./com_android_server_DMRService.cpp \
	./DMR_Android.cpp \
	./DMRAndroidPlaylist.cpp

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../include/DMR \
	$(LOCAL_PATH)/../include/system \
	$(LOCAL_PATH)/../include/DMP \
	$(LOCAL_PATH)/../libupnp/include/upnp3

LOCAL_CFLAGS := \
	-DMMSCP_LEAN_AND_MEAN \
	-DUSE_NEW_NAV=YES \
	-DINCLUDE_FEATURE_VOLUME \
	-DENABLE_DTCP_IP \
	-g

LOCAL_CFLAGS += "-DIS_CHIP(MODEL)=0"
LOCAL_CFLAGS += -DIS_MAGELLAN
LOCAL_CFLAGS += -DANDROID_PLATFORM \
		-DDLNADMRCTT
LOCAL_CFLAGS += -DUCTT

LOCAL_SHARED_LIBRARIES := libupnp libDMP libcutils libnativehelper
LOCAL_MODULE := libDMR
LOCAL_MODULE_TAGS := optional
LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
