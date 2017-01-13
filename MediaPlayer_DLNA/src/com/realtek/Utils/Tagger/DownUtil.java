package com.realtek.Utils.Tagger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jaudiotagger.audio.asf.data.GUID;
import org.jaudiotagger.audio.asf.io.AsfHeaderReader;
import org.jaudiotagger.audio.asf.io.CountingInputStream;
import org.jaudiotagger.audio.asf.util.Utils;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.flac.FlacStreamReader;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataStreamInfo;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockHeader;
import org.jaudiotagger.audio.mp4.Mp4NotMetaFieldKey;
import org.jaudiotagger.audio.mp4.atom.Mp4BoxHeader;
import org.jaudiotagger.audio.ogg.OggVorbisTagReader;
import org.jaudiotagger.audio.ogg.util.OggPageHeader;
import org.jaudiotagger.audio.ogg.util.VorbisHeader;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;

import android.os.Environment;

public class DownUtil
{

	public String path;

	private String targetFile;

	private long fileSize;
	long startdex;
	public DownUtil(String path, String targetFile)
	{
		this.path = path;
		this.targetFile = targetFile;
	}

	public void download() throws Exception
	{
		URL url = new URL(path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(5 * 1000);
		conn.setRequestMethod("GET");
		conn.setRequestProperty(
			"Accept",
			"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
		conn.setRequestProperty("Accept-Language", "zh-CN");
		conn.setRequestProperty("Charset", "UTF-8");
		conn.setRequestProperty(
			"User-Agent",
			"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
		conn.setRequestProperty("Connection", "Keep-Alive");

		fileSize = conn.getContentLength();
		conn.disconnect();
		String path = Environment.getExternalStorageDirectory().getPath();

	    if(targetFile.endsWith(".mp3"))
		{
	    	RandomAccessFile file1 = new RandomAccessFile(targetFile, "rw");
			file1.setLength(10);
			file1.close();	
			RandomAccessFile currentPart1 = new RandomAccessFile(targetFile,"rw");
		    currentPart1.seek(0);
			DownloadThread(0,10,currentPart1);
			
			File file2 = new File(path+"/a.mp3");
			startdex = AbstractID3v2Tag.getV2TagSizeIfExists(file2);
			
			RandomAccessFile file3 = new RandomAccessFile(path+"/b.mp3", "rw");
			file3.setLength(startdex+1024+256);
			file3.close();
			
			RandomAccessFile currentPart2 = new RandomAccessFile(path+"/b.mp3","rw");
		    currentPart2.seek(0);
			DownloadThread(0,startdex+1024,currentPart2);
			
			RandomAccessFile currentPart3 = new RandomAccessFile(path+"/b.mp3","rw");
			currentPart3.seek(startdex+1024);
			DownloadThread(fileSize-256,256,currentPart3);	
		}
	    else if(targetFile.endsWith(".wma"))
	    {
	    	RandomAccessFile wmaFile1 = new RandomAccessFile(path+"/c.wma","rw");
	    	wmaFile1.setLength(256);
	    	wmaFile1.close();

	    	RandomAccessFile wmaFile2 = new RandomAccessFile(path+"/c.wma","rw");
	    	wmaFile2.seek(0);
	    	DownloadThread(0,256,wmaFile2);
	    	
	    	RandomAccessFile wmaFile = new RandomAccessFile(path+"/c.wma","r");
	    	wmaFile.seek(0);
	    	InputStream stream = AsfHeaderReader.createStream(wmaFile);
	    	GUID guid = Utils.readGUID(stream);
	    	CountingInputStream cis = new CountingInputStream(stream);
	    	BigInteger Chunk = Utils.readBig64(cis);
	    	wmaFile.close();
	    	
	    	long startbyte = Chunk.longValue();
	    	RandomAccessFile wmaFile3 = new RandomAccessFile(path+"/c.wma","rw");
	    	wmaFile3.setLength(startbyte+1024);
	    	wmaFile3.close();
	    	RandomAccessFile wmaFile4 = new RandomAccessFile(path+"/c.wma","rw");
	    	wmaFile4.seek(0);
	    	DownloadThread(0,startbyte+1024,wmaFile4);
	    }
	    else if(targetFile.endsWith(".mp4"))
	    {	
	    	RandomAccessFile mp4File1 = new RandomAccessFile(path+"/d.mp4","rw");
	    	mp4File1.setLength(1024);
	    	mp4File1.seek(0);
	    	DownloadThread(0,1024,mp4File1);
	    	
	    	mp4Ftyp mp4ftyp = new mp4Ftyp();
	    	int len1 = mp4ftyp.getlength();
	    	
	    	
	    	RandomAccessFile mp4File2 = new RandomAccessFile(path+"/d.mp4","rw");
	    	mp4File2.setLength(1024+len1);
	    	mp4File2.seek(0);
	    	DownloadThread(0,1024+len1,mp4File2);
	    	
	    	mp4Moov mp4moov = new mp4Moov(len1);
	    	int len2 = mp4moov.getlength();
	    	
	    	RandomAccessFile mp4File3 = new RandomAccessFile(path+"/d.mp4","rw");
	    	long filelen = len2+len1+1024;
	    	mp4File3.setLength(filelen);
	    	mp4File3.seek(0);
	    	DownloadThread(0,filelen,mp4File3);
	    	
	    	RandomAccessFile mp4File4 = new RandomAccessFile(path+"/d.mp4","rw");
	    	mp4File4.seek(len1);
	    	Mp4BoxHeader moovHeader = Mp4BoxHeader.seekWithinLevel(mp4File4, Mp4NotMetaFieldKey.MOOV.getFieldName());
	    	long len3 = moovHeader.getLength();

	    	RandomAccessFile mp4File5 = new RandomAccessFile(path+"/d.mp4","rw");
	    	filelen = filelen+len3;
	    	mp4File5.setLength(filelen);
	    	mp4File5.seek(0);
	    	DownloadThread(0,filelen,mp4File5);
	    }
	    else if(targetFile.endsWith(".flac"))
	    {	
	    	long length = 0;
	    	RandomAccessFile flacFile1 = new RandomAccessFile(path+"/e.flac","rw");
	    	flacFile1.setLength(20);
	    	flacFile1.seek(0);
	    	DownloadThread(0,20,flacFile1);
	    	
	    	RandomAccessFile raf = new RandomAccessFile(path+"/e.flac","rw");
	    	FlacStreamReader flacStream = new FlacStreamReader(raf);
	        flacStream.findStream();

	        MetadataBlockDataStreamInfo mbdsi = null;
	        boolean isLastBlock = false;
	        while (!isLastBlock)
	        {
	            MetadataBlockHeader mbh = MetadataBlockHeader.readHeader(raf);
	            raf.seek(raf.getFilePointer() + mbh.getDataLength());
	            length = raf.getFilePointer() + mbh.getDataLength();
	            isLastBlock = mbh.isLastBlock();

		        RandomAccessFile flacFile= new RandomAccessFile(path+"/e.flac","rw");
		        flacFile.setLength(length+20);
		        flacFile.seek(0);
			    DownloadThread(0,length+20,flacFile);
			    mbh = null; //Free memory
	        }
	    }
	    else if(targetFile.endsWith(".ogg"))
	    {
	    	RandomAccessFile oggfile1= new RandomAccessFile(path+"/f.ogg","rw");
	    	oggfile1.setLength(1024);//cannot find the size of first pageheader
	    	oggfile1.seek(0);
	    	DownloadThread(0,1024,oggfile1);
	    	
	    	RandomAccessFile raf = new RandomAccessFile(path+"/f.ogg","rw");
	    	raf.seek(0);
	    	long length = 0;
	    	OggPageHeader pageHeader = OggPageHeader.read(raf); 
	        long idexforpage = raf.getFilePointer() + pageHeader.getPageLength();
	        raf.seek(idexforpage);
	        pageHeader = OggPageHeader.read(raf);
	        long indexfornext = raf.getFilePointer();
	        if(pageHeader.getPageLength()>raf.length()-idexforpage)
            {
            	length = idexforpage+pageHeader.getPageLength();
            	RandomAccessFile oggfile = new RandomAccessFile(path+"/f.ogg","rw");
	        	oggfile.setLength(1024+length);//cannot find the size of first pageheader
	        	oggfile.seek(0);
	        	DownloadThread(0,length+1024,oggfile);
            }
	        raf.close();
	        RandomAccessFile rafnext = new RandomAccessFile(path+"/f.ogg","rw");
             rafnext.seek(indexfornext);
	        byte[] b1 = new byte[VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH];
	        rafnext.read(b1);
	        
	        OggVorbisTagReader vtr = new OggVorbisTagReader();
	        if (! vtr.isVorbisCommentHeader(b1))
	        {
	            throw new CannotReadException("Cannot find comment block (no vorbiscomment header)");
	        }
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        byte[] b = new byte[pageHeader.getPacketList().get(0).getLength() - (VorbisHeader.FIELD_PACKET_TYPE_LENGTH + VorbisHeader.FIELD_CAPTURE_PATTERN_LENGTH)];
	        rafnext.read(b);
	        baos.write(b);
	        if ((pageHeader.getPacketList().size() <= 1)&&pageHeader.isLastPacketIncomplete())
	        {
	        	
		        while (true)
		        {
		            OggPageHeader nextPageHeader = OggPageHeader.read(rafnext);
		            b = new byte[nextPageHeader.getPacketList().get(0).getLength()];
		            rafnext.read(b);
		            baos.write(b);
		            
		            if (nextPageHeader.getPacketList().size() > 1)
		            {
		                break;
		            }
		            if (!nextPageHeader.isLastPacketIncomplete())
		            {
		            	break;
		            }
	            	length = length+pageHeader.getPageLength();
	            	RandomAccessFile oggfile = new RandomAccessFile(path+"/f.ogg","rw");
		        	oggfile.setLength(length);//cannot find the size of first pageheader
		        	oggfile.seek(0);
		        	DownloadThread(0,length,oggfile);
		        }
	        }
	        long len = rafnext.length();
	        rafnext.close();
	        RandomAccessFile oggfile = new RandomAccessFile(path+"/f.ogg","rw");
	        oggfile.setLength(len+7168);//cannot find the size of first pageheader
	        oggfile.seek(len);
        	DownloadThread(fileSize-7168,7168,oggfile);
        	rafnext = new RandomAccessFile(path+"/f.ogg","rw");
	        rafnext.seek(rafnext.length() - 2);
	        
	        int i = 2;
	        while (rafnext.getFilePointer() >= 4)
	        {
	        	if (rafnext.read() == OggPageHeader.CAPTURE_PATTERN[3])
	            {
	                rafnext.seek(rafnext.getFilePointer() - OggPageHeader.FIELD_CAPTURE_PATTERN_LENGTH);
	                byte[] ogg = new byte[3];
	                rafnext.readFully(ogg);
	                if (ogg[0] == OggPageHeader.CAPTURE_PATTERN[0] && ogg[1] == OggPageHeader.CAPTURE_PATTERN[1] && ogg[2] == OggPageHeader.CAPTURE_PATTERN[2])
	                {
	                    rafnext.seek(rafnext.getFilePointer() - 3);

	                    long oldPos = rafnext.getFilePointer();
	                    rafnext.seek(rafnext.getFilePointer() + OggPageHeader.FIELD_PAGE_SEGMENTS_POS);
	                    int pageSegments = rafnext.readByte() & 0xFF; //Unsigned
	                    rafnext.seek(oldPos);

	                    b = new byte[OggPageHeader.OGG_PAGE_HEADER_FIXED_LENGTH + pageSegments];
	                    rafnext.readFully(b);

	                    OggPageHeader pageHeaderEnd = new OggPageHeader(b);
	                    rafnext.seek(0);
	                    break;
	                }
	            }
	        	long pos = rafnext.getFilePointer();
	        	if(pos < len)
	        	{
	        		long len3 = rafnext.length();
	        		if(len3>fileSize)
	        			break;
	    	        RandomAccessFile oggfile2 = new RandomAccessFile(path+"/f.ogg","rw");
	            	oggfile2.setLength(len3+7168);//cannot find the size of first pageheader
	            	oggfile2.seek(len);
	            	long startpos = fileSize-(len3+7168-len); 
	            	DownloadThread(startpos,len3+7168-len,oggfile2);
	            	i++;
	            	rafnext.seek(rafnext.getFilePointer()+7168);
	        	}
	            rafnext.seek(rafnext.getFilePointer() - 2);
	        }
	        rafnext.close();
	    }
	    
	}

	public void DownloadThread(long startPos1, long currentPartSize1,
			RandomAccessFile currentPart1) throws IOException
	{

		long startPos;

		long currentPartSize;

		RandomAccessFile currentPart;

		int length = 0;
			startPos = startPos1;
			currentPartSize = currentPartSize1;
			currentPart = currentPart1;
			
			URL url = new URL(path);
	          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	            conn.setConnectTimeout(5 * 1000);
				conn.setRequestMethod("GET");
				conn.setRequestProperty(
					"Accept",
					"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
				conn.setRequestProperty("Accept-Language", "zh-CN");
				conn.setRequestProperty("Charset", "UTF-8");
				InputStream inStream = conn.getInputStream();

				inStream.skip(startPos);
				System.out.print(inStream);
				byte[] buffer = new byte[(int)currentPartSize];
				int hasRead = 0;

				while (length < currentPartSize
					&& (hasRead = inStream.read(buffer)) != -1)
				{
					currentPart.write(buffer, 0, hasRead);

					length += hasRead;
				}
				currentPart.close();
				inStream.close();
			}
		}

