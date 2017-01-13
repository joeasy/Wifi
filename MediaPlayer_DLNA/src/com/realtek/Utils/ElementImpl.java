package com.realtek.Utils;

import java.net.URI;

final class ElementImpl
  implements Element
{
  private final PlayListInfo playlistInfo;
  private final EncryptionInfo encryptionInfo;
  private final int duration;
  private final URI uri;
  private final String title;
  private final long programDate;

  public ElementImpl(PlayListInfo playlistInfo, EncryptionInfo encryptionInfo, int duration, URI uri, String title, long programDate)
  {
    if (uri == null) {
      throw new NullPointerException("uri");
    }

    if (duration < -1) {
      throw new IllegalArgumentException();
    }
    if ((playlistInfo != null) && (encryptionInfo != null)) {
      throw new IllegalArgumentException("Element cannot be a encrypted playlist.");
    }
    this.playlistInfo = playlistInfo;
    this.encryptionInfo = encryptionInfo;
    this.duration = duration;
    this.uri = uri;
    this.title = title;
    this.programDate = programDate;
  }

  public String getTitle() {
    return this.title;
  }

  public int getDuration() {
    return this.duration;
  }

  public URI getURI() {
    return this.uri;
  }

  public boolean isEncrypted() {
    return this.encryptionInfo != null;
  }

  public boolean isPlayList() {
    return this.playlistInfo != null;
  }

  public boolean isMedia() {
    return this.playlistInfo == null;
  }

  public EncryptionInfo getEncryptionInfo() {
    return this.encryptionInfo;
  }

  public PlayListInfo getPlayListInfo() {
    return this.playlistInfo;
  }

  public long getProgramDate() {
    return this.programDate;
  }

  public String toString()
  {
    return "ElementImpl{playlistInfo=" + this.playlistInfo + ", encryptionInfo=" + this.encryptionInfo + ", duration=" + this.duration + ", uri=" + this.uri + ", title='" + this.title + '\'' + '}';
  }

  static final class EncryptionInfoImpl
    implements EncryptionInfo
  {
    private final URI uri;
    private final String method;

    public EncryptionInfoImpl(URI uri, String method)
    {
      this.uri = uri;
      this.method = method;
    }

    public URI getURI() {
      return this.uri;
    }

    public String getMethod() {
      return this.method;
    }

    public String toString()
    {
      return "EncryptionInfoImpl{uri=" + this.uri + ", method='" + this.method + '\'' + '}';
    }
  }

  static final class PlayListInfoImpl
    implements PlayListInfo
  {
    private final int programId;
    private final int bandWidth;
    private final String codec;

    public PlayListInfoImpl(int programId, int bandWidth, String codec)
    {
      this.programId = programId;
      this.bandWidth = bandWidth;
      this.codec = codec;
    }

    public int getProgramId() {
      return this.programId;
    }

    public int getBandWitdh() {
      return this.bandWidth;
    }

    public String getCodecs() {
      return this.codec;
    }

    public String toString()
    {
      return "PlayListInfoImpl{programId=" + this.programId + ", bandWidth=" + this.bandWidth + ", codec='" + this.codec + '\'' + '}';
    }
  }
}