package com.realtek.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;


public final class PlayList implements Iterable<Element> {
	private final List<Element> elements;
	private final boolean endSet;
	private final int targetDuration;
	private int mediaSequenceNumber;

	public static PlayList parse(Readable playlist) throws ParseException {
		return PlayListParser.create(PlayListType.M3U8).parse(playlist);
	}
	
	public static PlayList parse(Readable playlist,PlayListType type) throws ParseException {
		return PlayListParser.create(type).parse(playlist);
	}

	public static PlayList parse(String playlist) throws ParseException {
		return parse(new StringReader(playlist));
	}

	public static PlayList parse(InputStream playlist) throws ParseException {
		return parse(new InputStreamReader(playlist));
	}

	public static PlayList parse(ReadableByteChannel playlist)
			throws ParseException {
		return parse(makeReadable(playlist));
	}

	private static Readable makeReadable(ReadableByteChannel source) {
		if (source == null)
			throw new NullPointerException("source");
		String defaultCharsetName = PlayListType.M3U8.getEncoding();
		return Channels.newReader(source, Charset.defaultCharset().name());
	}

	PlayList(List<Element> elements, boolean endSet, int targetDuration,
			int mediaSequenceNumber) {
		if (elements == null) {
			throw new NullPointerException("elements");
		}
		this.targetDuration = targetDuration;
		this.elements = elements;
		this.endSet = endSet;
		this.mediaSequenceNumber = mediaSequenceNumber;
	}

	public int getTargetDuration() {
		return this.targetDuration;
	}

	public Iterator<Element> iterator() {
		return this.elements.iterator();
	}

	public List<Element> getElements() {
		return this.elements;
	}

	public boolean isEndSet() {
		return this.endSet;
	}

	public int getMediaSequenceNumber() {
		return this.mediaSequenceNumber;
	}

	public String toString() {
		return "PlayListImpl{elements=" + this.elements + ", endSet="
				+ this.endSet + ", targetDuration=" + this.targetDuration
				+ ", mediaSequenceNumber=" + this.mediaSequenceNumber + '}';
	}
}