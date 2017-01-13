package com.realtek.Utils;

import java.net.URI;

class ElementBuilder
{
  private int duration;
  private URI uri;
  private PlayListInfo playlistInfo;
  private EncryptionInfo encryptionInfo;
  private String title;
  private long programDate = -1L;

  public long programDate()
  {
    return this.programDate;
  }

  public ElementBuilder programDate(long programDate) {
    this.programDate = programDate;
    return this;
  }

  public String getTitle() {
    return this.title;
  }

  public ElementBuilder title(String title) {
    this.title = title;
    return this;
  }

  public int getDuration() {
    return this.duration;
  }

  public ElementBuilder duration(int duration) {
    this.duration = duration;
    return this;
  }

  public URI getUri() {
    return this.uri;
  }

  public ElementBuilder uri(URI uri) {
    this.uri = uri;
    return this;
  }

  public ElementBuilder playList(int programId, int bandWidth, String codec) {
    this.playlistInfo = new ElementImpl.PlayListInfoImpl(programId, bandWidth, codec);
    return this;
  }

  public ElementBuilder resetPlatListInfo() {
    this.playlistInfo = null;
    return this;
  }

  public ElementBuilder resetEncryptedInfo() {
    this.encryptionInfo = null;
    return this;
  }

  public ElementBuilder reset() {
    this.duration = 0;
    this.uri = null;
    this.title = null;
    this.programDate = -1L;
    resetEncryptedInfo();
    resetPlatListInfo();
    return this;
  }

  public ElementBuilder encrypted(EncryptionInfo info)
  {
    this.encryptionInfo = info;
    return this;
  }

  public ElementBuilder encrypted(URI uri, String method) {
    this.encryptionInfo = new ElementImpl.EncryptionInfoImpl(uri, method);
    return this;
  }

  public Element create() {
    return new ElementImpl(this.playlistInfo, this.encryptionInfo, this.duration, this.uri, this.title, this.programDate);
  }
}