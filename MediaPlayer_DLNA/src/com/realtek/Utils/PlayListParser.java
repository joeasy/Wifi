package com.realtek.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PlayListParser {
	private Logger log = Logger.getLogger(getClass().getName());
	private PlayListType type;

	static PlayListParser create(PlayListType type) {
		return new PlayListParser(type);
	}

	public PlayListParser(PlayListType type) {
		if (type == null) {
			throw new NullPointerException("type");
		}
		this.type = type;
	}

	public PlayList parse(Readable source) throws ParseException {
		Scanner scanner = new Scanner(source);

		boolean firstLine = true;

		int lineNumber = 0;

		List elements = new ArrayList(10);
		ElementBuilder builder = new ElementBuilder();
		boolean endListSet = false;
		int targetDuration = -1;
		int mediaSequenceNumber = -1;

		EncryptionInfo currentEncryption = null;

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();

			if (null != line && line.length() > 0) {
				/**
				 * EXTM3U 
				 */
				if (line.startsWith(M3uConstants.EX_PREFIX)) {
					if (firstLine) {
						checkFirstLine(lineNumber, line);
						firstLine = false;
					} else if (line.startsWith(M3uConstants.EXTINF)) {
						parseExtInf(line, lineNumber, builder);
					} else if (line.startsWith(M3uConstants.EXT_X_ENDLIST)) {
						endListSet = true;
					} else if (line
							.startsWith(M3uConstants.EXT_X_TARGET_DURATION)) {
						if (targetDuration != -1) {
							throw new ParseException(line, lineNumber,
									"#EXT-X-TARGETDURATION duplicated");
						}
						targetDuration = parseTargetDuration(line, lineNumber);
					} else if (line
							.startsWith(M3uConstants.EXT_X_MEDIA_SEQUENCE)) {
						if (mediaSequenceNumber != -1) {
							throw new ParseException(line, lineNumber,
									"#EXT-X-MEDIA-SEQUENCE duplicated");
						}
						mediaSequenceNumber = parseMediaSequence(line,
								lineNumber);
					} else if (line
							.startsWith(M3uConstants.EXT_X_PROGRAM_DATE_TIME)) {
						long programDateTime = parseProgramDateTime(line,
								lineNumber);
						builder.programDate(programDateTime);
					} else if (line.startsWith(M3uConstants.EXT_X_KEY)) {
						currentEncryption = parseEncryption(line, lineNumber);
					} else {
						this.log.log(Level.FINE, "Unknown: '" + line + "'");
					}
				} else if (line.startsWith(M3uConstants.COMMENT_PREFIX)) {
					if (this.log.isLoggable(Level.FINEST))
						this.log.log(Level.FINEST, "----- Comment: " + line);
				} else {
					/**
					 * 
					 */
					if (firstLine) {
						checkFirstLine(lineNumber, line);
					}

					builder.encrypted(currentEncryption);

					builder.uri(toURI(line));
					elements.add(builder.create());

					builder.reset();
				}
			}

			lineNumber++;
		}

		return new PlayList(Collections.unmodifiableList(elements), endListSet,
				targetDuration, mediaSequenceNumber);
	}

	private URI toURI(String line) {
		try {
			return URI.create(line);
		} catch (IllegalArgumentException e) {
		}
		return new File(line).toURI();
	}

	private long parseProgramDateTime(String line, int lineNumber)
			throws ParseException {
		return M3uConstants.Patterns.toDate(line, lineNumber);
	}

	private int parseTargetDuration(String line, int lineNumber)
			throws ParseException {
		return (int) parseNumberTag(line, lineNumber,
				M3uConstants.Patterns.EXT_X_TARGET_DURATION,
				"#EXT-X-TARGETDURATION");
	}

	private int parseMediaSequence(String line, int lineNumber)
			throws ParseException {
		return (int) parseNumberTag(line, lineNumber,
				M3uConstants.Patterns.EXT_X_MEDIA_SEQUENCE,
				"#EXT-X-MEDIA-SEQUENCE");
	}

	private long parseNumberTag(String line, int lineNumber, Pattern patter,
			String property) throws ParseException {
		Matcher matcher = patter.matcher(line);
		if ((!matcher.find()) && (!matcher.matches())
				&& (matcher.groupCount() < 1)) {
			throw new ParseException(line, lineNumber, property
					+ " must specify duration");
		}
		try {
			return Long.valueOf(matcher.group(1)).longValue();
		} catch (NumberFormatException e) {
			throw new ParseException(line, lineNumber, e);
		}
	}

	private void checkFirstLine(int lineNumber, String line)
			throws ParseException {
		if ((this.type == PlayListType.M3U8 || this.type == PlayListType.M3U)
				&& (!line.startsWith("#EXTM3U")))
			throw new ParseException(line, lineNumber, "PlayList type '"
					+ this.type + "' must start with " + "#EXTM3U");
	}

	private void parseExtInf(String line, int lineNumber, ElementBuilder builder)
			throws ParseException {
		Matcher matcher = M3uConstants.Patterns.EXTINF.matcher(line);

		if ((!matcher.find()) || (!matcher.matches())
				|| (matcher.groupCount() < 1)) {
			// gmaui
			builder.duration(Integer.valueOf(0).intValue()).title("");
			// throw new ParseException(line, lineNumber,
			// "EXTINF must specify at least the duration");
		} else {
			String duration = matcher.group(1);
			String title = matcher.groupCount() > 1 ? matcher.group(2) : "";
			// gmaui
			title = (null == title) ? "" : title;
			try {
				builder.duration(Integer.valueOf(duration).intValue()).title(
						title);
			} catch (NumberFormatException e) {
				throw new ParseException(line, lineNumber, e);
			}
		}
	}

	private EncryptionInfo parseEncryption(String line, int lineNumber)
			throws ParseException {
		Matcher matcher = M3uConstants.Patterns.EXT_X_KEY.matcher(line);

		if ((!matcher.find()) || (!matcher.matches())
				|| (matcher.groupCount() < 1)) {
			throw new ParseException(line, lineNumber, "illegal input: " + line);
		}

		String method = matcher.group(1);
		String uri = matcher.group(3);

		if (method.equalsIgnoreCase("none")) {
			return null;
		}

		return new ElementImpl.EncryptionInfoImpl(uri != null ? toURI(uri)
				: null, method);
	}
}