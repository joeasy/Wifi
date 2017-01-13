package com.realtek.Utils.Tagger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.jaudiotagger.audio.generic.Utils;
import org.jaudiotagger.audio.mp4.atom.Mp4BoxHeader;

public class mp4Ftyp
{
	public mp4Ftyp() throws IOException
	{
		
	}
	public int getlength() throws IOException
	{
	RandomAccessFile mp4File3;
	try {
		mp4File3 = new RandomAccessFile("/mnt/sdcard/d.mp4","rw");
		mp4File3.seek(0);
	    ByteBuffer headerBuffer = ByteBuffer.allocate(Mp4BoxHeader.HEADER_LENGTH);
	    mp4File3.getChannel().read(headerBuffer);
	    headerBuffer.rewind();
	    byte[] b = new byte[Mp4BoxHeader.HEADER_LENGTH];
	    headerBuffer.get(b);
	    //Keep reference to copy of RawData
	    headerBuffer = ByteBuffer.wrap(b);
	    //Calculate box size OFFSET_LENGTH
	    int length = Utils.getIntBE(b, 0, 3);
	    return length;
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return 0;
	}
}