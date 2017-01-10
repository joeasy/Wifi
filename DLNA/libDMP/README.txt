if you want to compile by ndk, follow this:
1. build ndk environment
2. copy Android.mk.ndk
	Application.mk
	DLNA_DMP_1p5_jni.cpp
	DLNA_DMP_1p5_jni.h
	OnLoad.cpp
	include
	libupnp3 directory
   to you ndk's jni directory which is in sample project or tests project
3. move Android.mk.ndk to Android.mk