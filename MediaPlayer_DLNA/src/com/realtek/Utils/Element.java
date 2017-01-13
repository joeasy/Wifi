package com.realtek.Utils;

import java.net.URI;

public abstract interface Element
{
  public abstract String getTitle();

  public abstract int getDuration();

  public abstract URI getURI();

  public abstract boolean isEncrypted();

  public abstract boolean isPlayList();

  public abstract boolean isMedia();

  public abstract EncryptionInfo getEncryptionInfo();

  public abstract PlayListInfo getPlayListInfo();

  public abstract long getProgramDate();
}