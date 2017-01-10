#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += libnetty libdd-plist libjald

LOCAL_JNI_SHARED_LIBRARIES := libalac_decoder
#LOCAL_REQUIRED_MODULES := alac_decoder

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.cfg

LOCAL_SRC_FILES := $(call all-java-files-under, src)

# Link against the current Android SDK.
LOCAL_SDK_VERSION := current

# This is the target being built.
LOCAL_PACKAGE_NAME := RealtekAirPlay

include $(BUILD_PACKAGE)
##################################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libnetty:libs/netty-all-4.0.18.Final.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libdd-plist:libs/dd-plist.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += libjald:libs/jald.jar

#include $(call all-makefiles-under, jni)

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
