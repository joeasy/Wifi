package com.realtek.Utils;

import java.util.Comparator;

public class DLNAFileInfoComparator implements Comparator<DLNAFileInfo>{

	protected int mode;
	private int type;
	public DLNAFileInfoComparator(int mode){
		this.mode = mode;
	}
	public DLNAFileInfoComparator(int type,int mode){
		this.mode = mode;
		this.type = type;
	}
	
	@Override
	public int compare(DLNAFileInfo object1, DLNAFileInfo object2) {
		String  m1=object1.getFileName();
        String  m2=object2.getFileName();
        if(type == 1){
    		m1=object1.getFileDate();
            m2=object2.getFileDate();
        }
        int result=0;
        if(m1.compareToIgnoreCase(m2)>0)
        {
            result=mode;
        }
        if(m1.compareToIgnoreCase(m2)<0)
        {
            result=-mode;
        }
        return result;
	}
}

