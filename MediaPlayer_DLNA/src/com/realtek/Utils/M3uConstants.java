package com.realtek.Utils;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class M3uConstants
{
  static final String COMMENT_PREFIX = "#";
  static final String EX_PREFIX = "#EXT";
  static final String EXTM3U = "#EXTM3U";
  static final String EXTINF = "#EXTINF";

  static final String EXT_X_BYTERANGE = "#EXT-X-BYTERANGE";
  
  static final String EXT_X_TARGET_DURATION = "#EXT-X-TARGETDURATION";
  static final String EXT_X_MEDIA_SEQUENCE = "#EXT-X-MEDIA-SEQUENCE";
  static final String EXT_X_KEY = "#EXT-X-KEY";
  static final String EXT_X_PROGRAM_DATE_TIME = "#EXT-X-PROGRAM-DATE-TIME";
  static final String EXT_X_ALLOW_CACHE = "#EXT-X-ALLOW-CACHE";
  

  static final String EXT_X_PLAYLIST_TYPE = "#EXT-X-PLAYLIST-TYPE";
  static final String EXT_X_MEDIA = "#EXT-X-MEDIA";
  
  static final String EXT_X_STREAM_INF = "#EXT-X-STREAM-INF";
  static final String EXT_X_ENDLIST = "#EXT-X-ENDLIST";
  static final String EXT_X_DISCONTINUITY = "#EXT-X-DISCONTINUITY";


  static final String EXT_X_I_FRAMES_ONLY = "#EXT-X-I-FRAMES-ONLY";
  static final String EXT_X_MAP = "#EXT-X-MAP";
  static final String EXT_X_I_FRAME_STREAM_INF = "#EXT-X-I-FRAME-STREAM-INF";
  static final String EXT_X_VERSION = "#EXT-X-VERSION";
  private M3uConstants()
  {
    throw new AssertionError("Not allowed");
  }

  static class Patterns
  {
    static final Pattern EXTINF = Pattern.compile(tagPattern("#EXTINF") + "\\s*(-1|[0-9]*)\\s*(?:,((.*)))?");

    static final Pattern EXT_X_KEY = Pattern.compile(tagPattern("#EXT-X-KEY") + "METHOD=([0-9A-Za-z\\-]*)(,URI=\"(([^\\\\\"]*.*))\")?");

    static final Pattern EXT_X_TARGET_DURATION = Pattern.compile(tagPattern("#EXT-X-TARGETDURATION") + "([0-9]*)");
    static final Pattern EXT_X_MEDIA_SEQUENCE = Pattern.compile(tagPattern("#EXT-X-MEDIA-SEQUENCE") + "([0-9]*)");

    static final Pattern EXT_X_PROGRAM_DATE_TIME = Pattern.compile(tagPattern("#EXT-X-PROGRAM-DATE-TIME") + "(.*)");

    private Patterns()
    {
      throw new AssertionError();
    }

    private static String tagPattern(String tagName)
    {
      return "\\s*" + tagName + "\\s*:\\s*";
    }

    static long toDate(String line, int lineNumber)
      throws ParseException
    {
      Matcher matcher = EXT_X_PROGRAM_DATE_TIME.matcher(line);

      if ((!matcher.find()) || (!matcher.matches()) || (matcher.groupCount() < 1)) {
        throw new ParseException(line, lineNumber, " must specify date-time");
      }

      SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
      System.out.println(ISO8601FORMAT.format(new Date()));
      String dateTime = matcher.group(1);
      try {
        return ISO8601FORMAT.parse(dateTime).getTime();
      } catch (java.text.ParseException e) {
        throw new ParseException(line, lineNumber, e);
      }
    }
  }
}