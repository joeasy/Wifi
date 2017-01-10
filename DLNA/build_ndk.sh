#!/bin/bash
NDK_ROOT_LOCAL=/home/jacob_shen/android/android-ndk-r9b
MODULE_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cp $MODULE_PATH/jni/Android.mk.ndk $MODULE_PATH/jni/Android.mk
cp $MODULE_PATH/libDMP/Android.mk.ndk $MODULE_PATH/libDMP/Android.mk
cp $MODULE_PATH/libDMR/Android.mk.ndk $MODULE_PATH/libDMR/Android.mk
cp $MODULE_PATH/libupnp/Android.mk.ndk $MODULE_PATH/libupnp/Android.mk
$NDK_ROOT_LOCAL/ndk-build -C . $* NDK_MODULE_PATH=$MODULE_PATH
