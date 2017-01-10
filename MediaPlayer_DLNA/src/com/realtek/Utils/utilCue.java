package com.realtek.Utils;

import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileNotFoundException;  
import java.io.IOException;  
import java.io.InputStreamReader;  
import java.io.LineNumberReader;  
import java.io.UnsupportedEncodingException;  
import java.util.ArrayList;  
import java.util.List;  

import com.rtk.dmp.Util;
  
import android.annotation.SuppressLint;
import android.util.Log;  
/** 
 * parse cue file tool 
 * File cueFile = new File("/sdcard/test.cue"); 
 * CueFileBean bean = utilCue.parseCueFile(cueFile); 
 *  
PERFORMER "test" 
TITLE "10.Moonlight+ShadowWinner" 
FILE "10.Moonlight ShadowWinner.ape" WAVE 
   TRACK 01 AUDIO     
     TITLE "La lettre"      
     PERFORMER "Lara Fabian"    
     INDEX 01 00:00:00     
   TRACK 02 AUDIO      
     TITLE "Un ave maria"     
     PERFORMER "Lara Fabian"     
     INDEX 00 03:52:57     
     INDEX 01 03:52:99    
   TRACK 03 AUDIO 
     TITLE "Si tu n'as pas d'amour" 
     PERFORMER "Lara Fabian" 
     INDEX 00 08:50:49 
     INDEX 01 08:50:65 
   TRACK 04 AUDIO 
     TITLE "Il ne manquait que toi" 
     PERFORMER "Lara Fabian" 
     INDEX 00 12:36:17 
     INDEX 01 12:40:19 
 * @author xuweilin 
 * 
 */  
public class utilCue {  
    private static String TAG = "utilCue";  
    /** 
     * parse cue file 
     * @param cueFile file 
     * @return CueFileBean 
     */  
    @SuppressLint("DefaultLocale")
	public  CueFileBean parseCueFile(File cueFile){  
        LineNumberReader reader = null;  
        CueFileBean  cueFileBean = new utilCue().new CueFileBean();  
        List<CueSongBean> songs = new ArrayList<CueSongBean>();  
        CueSongBean cueSongBean = new utilCue().new CueSongBean();  
        boolean parseSong = false;  
        int songIndex = 0;  
        try {  
            reader = new LineNumberReader( new InputStreamReader(new FileInputStream(cueFile),"GBK"));  
            while (true) {    
                String s = new String();  
                s = reader.readLine();  
                if (s != null)   
                {  
                	if(!parseSong && s.trim().toUpperCase().contains("DATE")){
                		cueFileBean.setDate(s.substring(s.indexOf("DATE")+ 5, s.length()));
                	}
                    if(!parseSong && s.trim().toUpperCase().startsWith("PERFORMER")){  
                        cueFileBean.setPerformer(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));  
                    }  
                    if(!parseSong && s.trim().toUpperCase().startsWith("TITLE")){  
                        cueFileBean.setAlbumName(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));  
                    }  
                    if(s.trim().toUpperCase().startsWith("FILE")){  
                        cueFileBean.setFileName(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));  
                    }  
                    if(s.trim().toUpperCase().startsWith("TRACK")){  
                        parseSong = true;  
                        songIndex ++;  
                    }  
                    if(parseSong && s.trim().toUpperCase().startsWith("TITLE")){  
                        cueSongBean.setTitle(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));  
                    }  
                    if(parseSong && s.trim().toUpperCase().startsWith("PERFORMER")){  
                        cueSongBean.setPerformer(s.substring(s.indexOf("\"")+1, s.lastIndexOf("\"")));  
                    }  
                    if(songIndex == 1 && s.trim().toUpperCase().startsWith("INDEX")){  
                        cueSongBean.setIndexBegin(s.trim().split(" 01 ")[1].trim());  
                    }  
                    if(songIndex > 1 && s.trim().toUpperCase().startsWith("INDEX")){  
                        if(s.trim().contains(" 00 ")){  
                            songs.get(songIndex - 2).setIndexEnd(s.trim().split(" 00 ")[1].trim());  
                        }  
                        if(s.trim().contains(" 01 ")){  
                            cueSongBean.setIndexBegin(s.trim().split(" 01 ")[1].trim());  
                        }  
                    }  
                    if(songIndex >= 1 && s.trim().toUpperCase().startsWith("INDEX") && s.trim().contains(" 01 ")){  
                        songs.add(cueSongBean);  
                        cueSongBean = new utilCue().new CueSongBean();  
                    }  
                }else{  
                    cueFileBean.setSongs(songs);  
                    break;  
                }  
            }  
              
        } catch (UnsupportedEncodingException e) {  
            Log.e(TAG, "UnsupportedEncodingException:"+e.getMessage());  
        } catch (FileNotFoundException e) {  
            Log.e(TAG, "FileNotFoundException:"+e.getMessage());  
        }catch (IOException e) {  
            Log.e(TAG, "IOException:"+e.getMessage());  
        }finally{  
            try{  
                if(reader!=null ){  
                    reader.close();  
                }  
            }  
            catch(Exception e){  
                Log.e(TAG, "Exception:"+e.getMessage());  
            }  
        }  
        return cueFileBean;  
    }  
    /** 
     * cue bean 
     * @author xuweilin 
     * 
     */  
    public class CueFileBean{  
        private String performer; //performer  
        private String albumName; //albumName  
        private String fileName;  //fileName  
        private String date;      //add by jessie
        private List<CueSongBean> songs = null; //songs list  
        public String getPerformer() {  
            return performer;  
        }  
        public void setPerformer(String performer) {  
            this.performer = performer;  
        }  
        public String getAlbumName() {  
            return albumName;  
        }  
        public void setAlbumName(String albumName) {  
            this.albumName = albumName;  
        }  
        public String getFileName() {  
            return fileName;  
        }  
        public void setFileName(String fileName) {  
            this.fileName = fileName;  
        }  
        public List<CueSongBean> getSongs() {  
            return songs;  
        }  
        public void setSongs(List<CueSongBean> songs) {  
            this.songs = songs;  
        }
        //add by jessie
		public String getDate() {
			return date;
		}
		public void setDate(String date) {
			this.date = date;
		}  
        //add end 
    }  
    /** 
     * cue song bean 
     * @author xuweilin 
     * 
     */  
    public class CueSongBean{  
        private String title;  //title  
        private String performer;  //performer  
        private String indexBegin;  //begintime  
        private String indexEnd;   //endtime  
        public String getTitle() {  
            return title;  
        }  
        public void setTitle(String title) {  
            this.title = title;  
        }  
        public String getPerformer() {  
            return performer;  
        }  
        public void setPerformer(String performer) {  
            this.performer = performer;  
        }  
        public String getIndexBegin() {  
            return indexBegin;  
        }  
        public void setIndexBegin(String indexBegin) {  
            this.indexBegin = indexBegin;  
        }  
        public String getIndexEnd() {  
            return indexEnd;  
        }  
        public void setIndexEnd(String indexEnd) {  
            this.indexEnd = indexEnd;  
        }  
          
    }  
    
    //add by jessie
    /**
     * getPlayTime
     * @param startTime the audio's startTime which format likes this "00:00:00" --"minute:second:frame"
     * @param endTime the audio's endTime which format likes this "00:00:00" --"minute:second:frame"
     * @return playTime the audio's playTime which format likes this "00:00:00" --"hour:minute:second"
     */
    public String getPlayTime(String startTime, String endTime){
		String playTime = "00:00:00";
		String[] playTimes = playTime.split(":");
		String[] startTimes = startTime.split(":");
		String[] endTimes = endTime.split(":");
		long startTimeInt = 0, endTimeInt = 0;
		for(int i = 0; i < playTimes.length - 1; i++){
			if(i == 0){
				startTimeInt += Integer.parseInt(startTimes[i]) * 60;
				endTimeInt += Integer.parseInt(endTimes[i]) * 60;
			}else{
				startTimeInt += Integer.parseInt(startTimes[i]);
				endTimeInt += Integer.parseInt(endTimes[i]) ;
			}	
		}
		playTime = Util.toSecondTime(endTimeInt - startTimeInt);
		return playTime;
	}
    //add end
}  
