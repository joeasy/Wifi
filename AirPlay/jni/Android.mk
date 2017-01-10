LOCAL_PATH:= $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MODULE := libalac_decoder

LOCAL_SRC_FILES := alac_decoder.cpp alac.cpp
LOCAL_CFLAGS := -O3 -Wall -DBUILD_STANDALONE -DCPU_ARM -finline-functions -fPIC
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
#-DDBG_TIME
#LOCAL_ARM_MODE := arm

include $(BUILD_SHARED_LIBRARY)
