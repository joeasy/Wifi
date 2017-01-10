/*
 * INTEL CONFIDENTIAL
 * Copyright (c) 2002 - 2006 Intel Corporation.  All rights reserved.
 * 
 * The source code contained or described herein and all documents
 * related to the source code ("Material") are owned by Intel
 * Corporation or its suppliers or licensors.  Title to the
 * Material remains with Intel Corporation or its suppliers and
 * licensors.  The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and
 * licensors. The Material is protected by worldwide copyright and
 * trade secret laws and treaty provisions.  No part of the Material
 * may be used, copied, reproduced, modified, published, uploaded,
 * posted, transmitted, distributed, or disclosed in any way without
 * Intel's prior express written permission.
 
 * No license under any patent, copyright, trade secret or other
 * intellectual property right is granted to or conferred upon you
 * by disclosure or delivery of the Materials, either expressly, by
 * implication, inducement, estoppel or otherwise. Any license
 * under such intellectual property rights must be express and
 * approved by Intel in writing.
 * 
 * $Workfile: MimeTypes.h
 *
 *
 *
 */

#ifndef MIMETYPES_H

#include "CdsMediaClass.h"

/*! \file MimeTypes.h 
	\brief Provides basic mapping for media formats using extension/mimetypes/DLNA profiles.
*/

/*! \defgroup Mimetypes DLNA - File Metadata
	\brief 
	This module provides the CDS metadata of media formats using a File-System-based metadata backend.  It is used only for
	deriving ProtocolInfo values solely by the file extension of the file without actually parsing the file formats.
	It is a helper library used for the DMS module.  Vendor are advised to replace this component with their own media parser
	library.

	\{
*/

#define CLASS_ITEM				"object.item"
#define CLASS_AUDIO_ITEM		"object.item.audioItem"
#define CLASS_PLAYLIST_M3U		"object.container.playlistContainer"
#define CLASS_PLAYLIST_ASX		"object.container.playlistContainer"
#define CLASS_PLAYLIST_DIDLS	"object.item.playlistItem"
#define CLASS_VIDEO_ITEM		"object.item.videoItem"
#define CLASS_IMAGE_ITEM		"object.item.imageItem"

/* mandatory by DLNA */
#define EXTENSION_IMAGE_JPG			".jpg"
#define EXTENSION_IMAGE_JPG_W		L".jpg"
#define EXTENSION_IMAGE_JPEG		".jpeg"
#define EXTENSION_IMAGE_JPEG_W		L".jpeg"
#define MIME_TYPE_IMAGE_JPEG		"image/jpeg"
#define PROTINFO_IMAGE_JPEG			"http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_SM"

#define EXTENSION_AUDIO_LPCM		".pcm"
#define EXTENSION_AUDIO_LPCM_W		L".pcm"
#define MIME_TYPE_AUDIO_LPCM		"audio/L16;rate=44100;channels=2"
#define PROTINFO_AUDIO_LPCM			"http-get:*:audio/L16;rate=44100;channels=2:DLNA.ORG_PN=LPCM"

#define EXTENSION_VIDEO_MPEG2		".mpg"
#define EXTENSION_VIDEO_MPEG2_W		L".mpg"
#define MIME_TYPE_VIDEO_MPEG2		"video/mpeg"
#define PROTINFO_VIDEO_MPEG2		"http-get:*:video/mpeg:DLNA.ORG_PN=MPEG_PS_NTSC"

#define EXTENSION_AUDIO_MPEG		".mp3"
#define EXTENSION_AUDIO_MPEG_W		L".mp3"
#define MIME_TYPE_AUDIO_MPEG		"audio/mpeg"
#define PROTINFO_AUDIO_MPEG			"http-get:*:audio/mpeg:DLNA.ORG_PN=MP3"

#define EXTENSION_AUDIO_3GPP		".3gp"
#define EXTENSION_AUDIO_3GPP_W		L".3gp"
#define MIME_TYPE_AUDIO_3GPP		"audio/3gpp"
#define PROTINFO_AUDIO_3GPP			"http-get:*:audio/3gpp:DLNA.ORG_PN=AMR_3GPP"

#define EXTENSION_VIDEO_AAC			".mp4"
#define EXTENSION_VIDEO_AAC_W		L".mp4"
#define MIME_TYPE_VIDEO_AAC			"video/mp4"
#define PROTINFO_VIDEO_AAC			"http-get:*:video/mp4:DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC_520"

#define EXTENSION_AUDIO_AAC			".mp4"
#define EXTENSION_AUDIO_AAC_W		L".mp4"
#define MIME_TYPE_AUDIO_AAC			"audio/mp4"
#define PROTINFO_AUDIO_AAC			"http-get:*:audio/mp4:DLNA.ORG_PN=AAC_ISO_320"

#define EXTENSION_AUDIO_ADTS		".aac"
#define EXTENSION_AUDIO_ADTS_W		L".aac"

#define MIME_TYPE_AUDIO_ADTS		"audio/aac"
#define PROTINFO_AUDIO_ADTS			"http-get:*:audio/aac:DLNA.ORG_PN=AAC_ADTS_320"

/* optional*/
#define EXTENSION_VIDEO_WMV			".wmv"
#define EXTENSION_VIDEO_WMV_W		L".wmv"
#define MIME_TYPE_VIDEO_WMV			"video/x-ms-wmv"
#define PROTINFO_VIDEO_WMV			"http-get:*:video/x-ms-wmv:DLNA.ORG_PN=WMVMED_BASE"

#define EXTENSION_AUDIO_WMA			".wma"
#define EXTENSION_AUDIO_WMA_W		L".wma"
#define MIME_TYPE_AUDIO_WMA			"audio/x-ms-wma"
#define PROTINFO_AUDIO_WMA			"http-get:*:audio/x-ms-wma:DLNA.ORG_PN=WMABASE"

#define EXTENSION_VIDEO_ASF			".asf"
#define EXTENSION_VIDEO_ASF_W		L".asf"
#define MIME_TYPE_VIDEO_ASF			"video/x-ms-asf"
#define PROTINFO_VIDEO_ASF			"http-get:*:video/x-ms-asf:DLNA.ORG_PN=MPEG4_P2_ASF_SP_G726"

#define EXTENSION_IMAGE_PNG			".png"
#define EXTENSION_IMAGE_PNG_W		L".png"
#define MIME_TYPE_IMAGE_PNG			"image/png"
#define PROTINFO_IMAGE_PNG			"http-get:*:image/png:DLNA.ORG_PN=PNG_LRG"

#define EXTENSION_AUDIO_WAV			".wav"
#define EXTENSION_AUDIO_WAV_W		L".wav"
#define MIME_TYPE_AUDIO_WAV			"audio/wav"
#define PROTINFO_AUDIO_WAV			"http-get:*:audio/wav:*"

#ifdef ENABLE_APE
#define PROTINFO_AUDIO_APE				"http-get:*:audio/x-ape:*"
#endif

#define EXTENSION_PLAYLIST_ASX		".asx"
#define EXTENSION_PLAYLIST_ASX_W	L".asx"
#define MIME_TYPE_PLAYLIST_ASX		"audio/x-ms-asx"
#define PROTINFO_PLAYLIST_ASX		"http-get:*:audio/x-ms-asx:*"

#define EXTENSION_IMAGE_TIF			".tif"
#define EXTENSION_IMAGE_TIF_W		L".tif"
#define MIME_TYPE_IMAGE_TIF			"image/tif"
#define PROTINFO_IMAGE_TIF			"http-get:*:image/tif:*"

#define EXTENSION_IMAGE_GIF			".gif"
#define EXTENSION_IMAGE_GIF_W		L".gif"
#define MIME_TYPE_IMAGE_GIF			"image/gif"
#define PROTINFO_IMAGE_GIF			"http-get:*:image/gif:*"

#define EXTENSION_IMAGE_BMP			".bmp"
#define EXTENSION_IMAGE_BMP_W		L".bmp"
#define MIME_TYPE_IMAGE_BMP			"image/bmp"
#define PROTINFO_IMAGE_BMP			"http-get:*:image/bmp:*"

#define EXTENSION_TXT				".txt"
#define EXTENSION_TXT_W				L".txt"
#define MIME_TYPE_TXT				"text/plain"
#define PROTINFO_TXT				"http-get:*:text/plain:*"

#define EXTENSION_DIDLS				".didls"
#define EXTENSION_DIDLS_W			L".didls"
#define MIME_TYPE_DIDLS				"text/xml"
#define PROTINFO_DIDLS				"http-get:*:text/xml:DLNA.ORG_PN=DIDL_S"

// -----------> yuyu added v
#define EXTENSION_AUDIO_L16_SLPCM		".pcm"
#define EXTENSION_AUDIO_L16_SLPCM_W		L".pcm"
#define MIME_TYPE_AUDIO_L16_SLPCM		"audio/L16;rate=44100;channels=1"
#define PROTINFO_AUDIO_L16_SLPCM			"http-get:*:audio/L16;rate=44100;channels=1:DLNA.ORG_PN=LPCM"

#define EXTENSION_AUDIO_L16_48_SLPCM		".pcm"
#define EXTENSION_AUDIO_L16_48_SLPCM_W		L".pcm"
#define MIME_TYPE_AUDIO_L16_48_SLPCM		"audio/L16;rate=48000;channels=1"
#define PROTINFO_AUDIO_L16_48_SLPCM			"http-get:*:audio/L16;rate=48000;channels=1:DLNA.ORG_PN=LPCM"

#define EXTENSION_AUDIO_L16_48_LPCM		".pcm"
#define EXTENSION_AUDIO_L16_48_LPCM_W		L".pcm"
#define MIME_TYPE_AUDIO_L16_48_LPCM		"audio/L16;rate=48000;channels=2"
#define PROTINFO_AUDIO_L16_48_LPCM			"http-get:*:audio/L16;rate=48000;channels=2:DLNA.ORG_PN=LPCM"

#if 0
#define EXTENSION_PLAYLIST_M3U		".m3u"
#define EXTENSION_PLAYLIST_M3U_W	L".m3u"
#define MIME_TYPE_PLAYLIST_M3U		"audio/m3u"			//TODO, is this correct?
#define PROTINFO_PLAYLIST_M3U		"http-get:*:audio/m3u:*"	//TODO, is this correct?
#endif

// -----------> yuyu added ^

#define MIMETYPE_OCTETSTREAM		"application/octet-stream"
#define PROTINFO_OCTETSTREAM		"http-get:*:application/octet-stream:*"

/* mandatory by DLNA */
#define	DLNAPROFILE_JPEG_SM						"JPEG_SM"
#define	DLNAPROFILE_JPEG_SM_W					L"JPEG_SM"
#define	DLNAPROFILE_JPEG_MED					"JPEG_MED"
#define	DLNAPROFILE_JPEG_MED_W					L"JPEG_MED"
#define	DLNAPROFILE_JPEG_LRG					"JPEG_LRG"
#define	DLNAPROFILE_JPEG_LRG_W					L"JPEG_LRG"

#define	DLNAPROFILE_LPCM						"LPCM"
#define	DLNAPROFILE_LPCM_W						L"LPCM"
#define	DLNAPROFILE_MPEG_PS_NTSC				"MPEG_PS_NTSC"
#define	DLNAPROFILE_MPEG_PS_NTSC_W				L"MPEG_PS_NTSC"
#define	DLNAPROFILE_MP3							"MP3"
#define	DLNAPROFILE_MP3_W						L"MP3"
#define DLNAPROFILE_AMR_3GPP					"AMR_3GPP"
#define DLNAPROFILE_AMR_3GPP_W					L"AMR_3GPP"
#define DLNAPROFILE_AVC_MP4_BL_CIF15_AAC_520	"AVC_MP4_BL_CIF15_AAC_520"
#define DLNAPROFILE_AVC_MP4_BL_CIF15_AAC_520_W	L"AVC_MP4_BL_CIF15_AAC_520"
#define DLNAPROFILE_AAC_ISO_320					"AAC_ISO_320"
#define DLNAPROFILE_AAC_ISO_320_W				L"AAC_ISO_320"
#define DLNAPROFILE_AAC_ADTS_320				"AAC_ADTS_320"
#define DLNAPROFILE_AAC_ADTS_320_W				L"AAC_ADTS_320"
/* optional */
#define	DLNAPROFILE_WMVMED_BASE					"WMVMED_BASE"
#define	DLNAPROFILE_WMVMED_BASE_W				L"WMVMED_BASE"
#define	DLNAPROFILE_WMABASE						"WMABASE"
#define	DLNAPROFILE_WMABASE_W					L"WMABASE"
#define	DLNAPROFILE_MPEG4_P2_ASF_SP_G726		"MPEG4_P2_ASF_SP_G726"
#define	DLNAPROFILE_MPEG4_P2_ASF_SP_G726_W		L"MPEG4_P2_ASF_SP_G726"
#define	DLNAPROFILE_PNG_LRG						"PNG_LRG"
#define	DLNAPROFILE_PNG_LRG_W					L"PNG_LRG"

#define DLNAPROFILE_DIDL_S						"DIDL_S"
#define DLNAPROFILE_DIDL_S_W					L"DIDL_S"



/*!	\brief Parses the file extension of a given file path.

	\param[in] file_path The file path or file name.
	\param[in] wide Input string is wide character.

	\returns A file extension starts with '.', must be freed.
*/
char* FilePathToFileExtension(char* file_path, int wide);

/*!	\brief Returns the corresponding DLNA Profile mapping to a given file extension.

	\param[in] extension The file extension starts with '.'.
	\param[in] wide Input string is wide character.

	\returns DLNA Profile in static string, NULL if mapping cannot be determined.
*/
const char* FileExtensionToDlnaProfile(const char* extension, int wide);

/*!	\brief Returns the corresponding mimetype mapping to a given file extension.

	\param[in] extension The file extension starts with '.'.
	\param[in] wide Input string is wide character.

	\returns Mimetype in static string, NULL if mapping cannot be determined.
*/
const char* FileExtensionToMimeType (const char* extension, int wide);

/*!	\brief Returns the corresponding media class mapping to a given file extension, as described by CdsMediaClass.h.

	\param[in] extension The file extension starts with '.'.
	\param[in] wide Input string is wide character.

	\returns Media class code value.
*/
unsigned int FileExtensionToClassCode (const char* extension, int wide);

/*! \} */

#endif
