LOCAL_PATH := $(my-dir)
include $(CLEAR_VARS)


LOCAL_SRC_FILES := \
    OnLoad.cpp  \
    DLNA_DMP_1p5.cpp \
    RTK_MmsCp_1p5.cpp   \
    DLNA_DMP_1p5_jni.cpp
    


LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../include/DMP \
	$(LOCAL_PATH)/../include/system \
    $(LOCAL_PATH)/../libupnp/include/upnp3


LOCAL_CFLAGS := \
	"-DIS_CHIP(MODEL)=0" \
	-DIS_TV_CHIP \
	-DMMSCP_LEAN_AND_MEAN \
	-DANDROID_PLATFORM \
	-DDLNADMRCTT

LOCAL_SHARED_LIBRARIES := libupnp libcutils libnativehelper

LOCAL_MODULE := libDMP

LOCAL_MODULE_TAGS := optional

#LOCAL_CFLAGS += -DENABLE_ANDROID

# build library

LOCAL_PRELINK_MODULE := false

include $(BUILD_SHARED_LIBRARY)
