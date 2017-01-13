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
ifeq ($(filter rtd298x_tv013,$(TARGET_PRODUCT)),)
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

# This is the target being built.
LOCAL_PACKAGE_NAME := RTKMiracast

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src/com)
#LOCAL_STATIC_JAVA_LIBRARIES := libandroidsupportv4
LOCAL_CERTIFICATE := shared
# Link against the current Android SDK.
# LOCAL_SDK_VERSION := current
#LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libandroidsupportv4:libs/android-support-v4.jar
include $(BUILD_MULTI_PREBUILT)
include $(call all-makefiles-under,$(LOCAL_PATH))
endif