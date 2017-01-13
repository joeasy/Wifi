package com.realtek.Utils;
enum PlayListType
{
 M3U8("UTF-8", "application/vnd.apple.mpegurl", "m3u8"), 
 M3U("US-ASCII", "audio/mpegurl", "m3u");

 final String encoding;
 final String contentType;
 final String extension;

 private PlayListType(String encoding, String contentType, String extension) { this.encoding = encoding;
   this.contentType = contentType;
   this.extension = extension; }

 public String getEncoding()
 {
   return this.encoding;
 }

 public String getContentType() {
   return this.contentType;
 }

 public String getExtension() {
   return this.extension;
 }
}