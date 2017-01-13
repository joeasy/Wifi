package com.realtek.Utils.Tagger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;


import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.asf.AsfTag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.jaudiotagger.tag.flac.FlacTag;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.mp4.Mp4FieldKey;
import org.jaudiotagger.tag.mp4.Mp4Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import android.annotation.SuppressLint;
import android.os.Environment;


@SuppressLint("DefaultLocale")
public class ParserSingleton
{
    String FileName;
	String lowcast;
	ID3v1Tag id3v1tag ;
	AbstractID3v2Tag id3v2tag;
	AsfTag wmatag;
	Mp4Tag mp4tag;
	FlacTag flactag;
	VorbisCommentTag oggtag;
	 private static ParserSingleton singleton ; 
	 
	public static class AudioInfo{
		public String Title;
		public String Artist;
		public String Album;
		public String Gener;
		public String Year;
		public Artwork artwork;
	 }
	
	public static ParserSingleton getInstance(){  
        synchronized(ParserSingleton.class){  
        	singleton = singleton == null ? new ParserSingleton() : singleton ;  
        }  
        return singleton ;   
    }  
	
	public  ParserSingleton()
	{
	}
	public AudioInfo Parser(String url)
	{
		clearfile();
		FileName = url;
		lowcast = url.toLowerCase();
		AudioInfo Audio = new AudioInfo();
       String returntype = preParser();
       if(returntype ==null)
       {
    	if(lowcast.contains(".mp3"))
    	{
    	   if(MP3Parser().booleanValue())
			{
				if(id3v2tag !=null)
				{
					Audio.Title = id3v2tag.getFirst(FieldKey.TITLE);
					Audio.Artist = id3v2tag.getFirst(FieldKey.ARTIST);
					Audio.Album = id3v2tag.getFirst(FieldKey.ALBUM);
					Audio.Gener = id3v2tag.getFirst(FieldKey.GENRE);
					Audio.Year = id3v2tag.getFirst(FieldKey.YEAR);
					Audio.artwork= id3v2tag.getFirstArtwork();
				}
				else if(id3v1tag != null)
				{
					Audio.Title = id3v1tag.getFirst(FieldKey.TITLE);
					Audio.Artist = id3v1tag.getFirst(FieldKey.ARTIST);
					Audio.Album = id3v1tag.getFirst(FieldKey.ALBUM);
					Audio.Gener = id3v1tag.getFirst(FieldKey.GENRE);
					Audio.Year = id3v1tag.getFirst(FieldKey.YEAR);
					Audio.artwork= id3v1tag.getFirstArtwork();
				}
			}
    	}
    	   if(lowcast.contains(".wma"))
    	   {
    		   if(WMAParser().booleanValue())
			  {
				if(wmatag != null)
				{
					Audio.Artist =  wmatag.getFirst(FieldKey.ARTIST);
					Audio.Title = wmatag.getFirst(FieldKey.TITLE);
					Audio.Gener = wmatag.getFirst(FieldKey.GENRE);
		        	Audio.Album = wmatag.getFirst(FieldKey.ALBUM);
		        	Audio.Year = wmatag.getFirst(FieldKey.YEAR);
		        	Audio.artwork = wmatag.getFirstArtwork();
				}
			 }
    	   }
    	   if(lowcast.contains(".ogg"))
    	   {
				 if(OGGParser().booleanValue())
				{
					if(oggtag != null)
					{
						Audio.Artist =  oggtag.getFirst(FieldKey.ARTIST);
						Audio.Title = oggtag.getFirst(FieldKey.TITLE);
						Audio.Gener = oggtag.getFirst(FieldKey.GENRE);
						Audio.Album = oggtag.getFirst(FieldKey.ALBUM);
						Audio.artwork = oggtag.getFirstArtwork(); 
					}
				}
    	   }
		   if(lowcast.contains(".flac"))
		   {
				 if(FLACParser().booleanValue())
				{
					if(flactag != null)
					{
						Audio.Artist= flactag.getFirst(FieldKey.ARTIST);
						Audio.Title = flactag.getFirst(FieldKey.TITLE);
						Audio.Gener = flactag.getFirst(FieldKey.GENRE);
						Audio.Album = flactag.getFirst(FieldKey.ALBUM);
						Audio.artwork = flactag.getFirstArtwork();
					}
				}
		   }
		   if(lowcast.contains(".mp4"))
		   {
				if(MP4Parser().booleanValue())
				{
					if(mp4tag != null)
					{
						Audio.Artist =  mp4tag.getFirst(Mp4FieldKey.ARTIST);
						Audio.Title = mp4tag.getFirst(Mp4FieldKey.TITLE);
						Audio.Gener = mp4tag.getFirst(Mp4FieldKey.GENRE);
						Audio.Album = mp4tag.getFirst(Mp4FieldKey.ALBUM);
						Audio.artwork = mp4tag.getFirstArtwork();
					}
				}
		   }
    	}else if(returntype.equals("truemp3"))
		{
			if(id3v2tag !=null)
			{
				Audio.Title = id3v2tag.getFirst(FieldKey.TITLE);
				Audio.Artist = id3v2tag.getFirst(FieldKey.ARTIST);
				Audio.Album = id3v2tag.getFirst(FieldKey.ALBUM);
				Audio.Gener = id3v2tag.getFirst(FieldKey.GENRE);
				Audio.Year = id3v2tag.getFirst(FieldKey.YEAR);
				Audio.artwork= id3v2tag.getFirstArtwork(); 
			}
			else if(id3v1tag != null)
			{
				Audio.Title = id3v1tag.getFirst(FieldKey.TITLE);
				Audio.Artist = id3v1tag.getFirst(FieldKey.ARTIST);
				Audio.Album = id3v1tag.getFirst(FieldKey.ALBUM);
				Audio.Gener = id3v1tag.getFirst(FieldKey.GENRE);
				Audio.Year = id3v1tag.getFirst(FieldKey.YEAR);
				Audio.artwork= id3v1tag.getFirstArtwork();
			}
		}else if(returntype.equals("truewma"))
		{
			if(wmatag != null)
			{
				Audio.Artist =  wmatag.getFirst(FieldKey.ARTIST);
				Audio.Title = wmatag.getFirst(FieldKey.TITLE);
				Audio.Gener = wmatag.getFirst(FieldKey.GENRE);
	        	Audio.Album = wmatag.getFirst(FieldKey.ALBUM);
	        	Audio.Year = wmatag.getFirst(FieldKey.YEAR);
	        	Audio.artwork = wmatag.getFirstArtwork();
			}
		}else if(returntype.equals("trueogg"))
		{
			if(oggtag != null)
			{
				Audio.Artist =  oggtag.getFirst(FieldKey.ARTIST);
				Audio.Title = oggtag.getFirst(FieldKey.TITLE);
				Audio.Gener = oggtag.getFirst(FieldKey.GENRE);
				Audio.Album = oggtag.getFirst(FieldKey.ALBUM);
				Audio.artwork = oggtag.getFirstArtwork();
			}
 		}else if(returntype.equals("trueflac"))
		{
 			if(flactag != null)
			{
				Audio.Artist= flactag.getFirst(FieldKey.ARTIST);
				Audio.Title = flactag.getFirst(FieldKey.TITLE);
				Audio.Gener = flactag.getFirst(FieldKey.GENRE);
				Audio.Album = flactag.getFirst(FieldKey.ALBUM);
				Audio.artwork = flactag.getFirstArtwork();
			}
	 	}else if(returntype.equals("truemp4"))
		{
	 		if(mp4tag != null)
			{
				Audio.Artist =  mp4tag.getFirst(Mp4FieldKey.ARTIST);
				Audio.Title = mp4tag.getFirst(Mp4FieldKey.TITLE);
				Audio.Gener = mp4tag.getFirst(Mp4FieldKey.GENRE);
				Audio.Album = mp4tag.getFirst(Mp4FieldKey.ALBUM);
				Audio.artwork = mp4tag.getFirstArtwork();
			}
		}
		clearfile();
		return Audio;
	}
	public String preParser()
	{
		if(lowcast.contains(".mp3"))
			return MP3Parser().toString()+"mp3";
		else if(lowcast.contains(".wma"))
			return WMAParser().toString()+"wma";
		else if(lowcast.contains(".ogg"))
	    	return OGGParser().toString()+"ogg";
		else if(lowcast.contains(".flac"))
	    	return FLACParser().toString()+"flac";
		else if(lowcast.contains(".mp4"))
	    	return MP4Parser().toString()+"mp4";
		else
		    return null;	
	}
	
	@SuppressLint("SdCardPath")
	public Boolean MP3Parser()
	{
		String path = Environment.getExternalStorageDirectory().getPath();
		MultiThreadDown ThreadDown = new MultiThreadDown(FileName,"mp3");
		MP3File mp3file;
		File file2 = new File(path+"/b.mp3");
			try {
				mp3file = new MP3File(file2, MP3File.LOAD_ALL, true);
				id3v1tag = mp3file.getID3v1Tag();
				id3v2tag = mp3file.getID3v2Tag();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (TagException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (ReadOnlyFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (InvalidAudioFrameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return true;
	}
	public Boolean WMAParser()
	{
		String path = Environment.getExternalStorageDirectory().getPath();
		MultiThreadDown ThreadDown = new MultiThreadDown(FileName,"wma");
		File file = new File(path+"/c.wma");
		try {
			AudioFile f;
			f = AudioFileIO.read(file);
			if(f.getTag() instanceof AsfTag){
				wmatag = (AsfTag) f.getTag();
	        }
		} catch (CannotReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (ReadOnlyFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InvalidAudioFrameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public Boolean MP4Parser()
	{
		String path = Environment.getExternalStorageDirectory().getPath();
		MultiThreadDown ThreadDown = new MultiThreadDown(FileName,"mp4");
		File file = new File(path+"/d.mp4");
		try {
			AudioFile f;
			f = AudioFileIO.read(file);
	        mp4tag = (Mp4Tag)f.getTag();
        	
		} catch (CannotReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (ReadOnlyFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InvalidAudioFrameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public Boolean FLACParser()
	{
		String path = Environment.getExternalStorageDirectory().getPath();
		MultiThreadDown ThreadDown = new MultiThreadDown(FileName,"flac");
		 File file = new File(path + "/e.flac");
        AudioFile f;
		try {
			f = AudioFileIO.read(file);
			flactag = (FlacTag)f.getTag();
		} catch (CannotReadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (TagException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (ReadOnlyFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (InvalidAudioFrameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public Boolean OGGParser()
		{
		    String path = Environment.getExternalStorageDirectory().getPath();
			MultiThreadDown ThreadDown = new MultiThreadDown(FileName,"ogg");
			 File file = new File(path + "/f.ogg");					
	        AudioFile f;
			try {
				f = AudioFileIO.read(file);
				oggtag = (VorbisCommentTag)f.getTag();
			} catch (CannotReadException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (TagException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (ReadOnlyFileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			} catch (InvalidAudioFrameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return true;
	}
	
	public void clearfile()
	{		
		RandomAccessFile file;
		try {
			String path = Environment.getExternalStorageDirectory().getPath();
			file = new RandomAccessFile(path+"/b.mp3", "rw");
			try {
				file.setLength(0);
				file.close();
			    file = new RandomAccessFile(path+"/c.wma", "rw");
			    file.setLength(0);
				file.close();
				file = new RandomAccessFile(path+"/d.mp4", "rw");
			    file.setLength(0);
				file.close();
				file = new RandomAccessFile(path+"/e.flac", "rw");
			    file.setLength(0);
				file.close();
				file = new RandomAccessFile(path+"/f.ogg", "rw");
			    file.setLength(0);
				file.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}