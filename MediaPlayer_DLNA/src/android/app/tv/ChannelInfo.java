package android.app.tv;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public final class ChannelInfo implements Parcelable {
    private static final String TAG = "ChannelInfo";

    // RT_TV_SYSTEM
    public static final int RT_TV_SYSTEM_UNKNOWN = 0;
    public static final int RT_TV_SYSTEM_NTSC = 1;
    public static final int RT_TV_SYSTEM_PAL = 2;
    public static final int RT_TV_SYSTEM_PAL_N = 3;
    public static final int RT_TV_SYSTEM_SECAM = 4;
    public static final int RT_TV_SYSTEM_ATSC = 5;
    public static final int RT_TV_SYSTEM_DVB = 6;
    public static final int RT_TV_SYSTEM_MAX = RT_TV_SYSTEM_DVB + 1;

    // RT_SERVICE_TYPE
    public static final int RT_SERVICE_TYPE_UNKNOWN = 0;
    public static final int RT_SERVICE_TYPE_ATV = 1;
    public static final int RT_SERVICE_TYPE_DTV = 2;
    public static final int RT_SERVICE_TYPE_AUDIO = 3;
    public static final int RT_SERVICE_TYPE_DATA = 4;
    public static final int RT_SERVICE_TYPE_MAX = RT_SERVICE_TYPE_DATA + 1;


    public int iChNum;
    public int iChIndex;
    public int iFreq;
    public int TvSystem;
    public int ServiceType;
	public boolean m_bAfcEnable;

    public String toString() {
        return "ChannelInfo{ iChNum: " + iChNum
            + ",iChIndex: " + iChIndex
            + ",iFreq: " + iFreq
            + ",TvSystem: " + TvSystem
            + ",ServiceType: " + ServiceType 
			+ ",m_bAfcEnable: " + m_bAfcEnable + " }";
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(iChNum);
        out.writeInt(iChIndex);
        out.writeInt(iFreq);
        out.writeInt(TvSystem);
        out.writeInt(ServiceType);
		out.writeBooleanArray(new boolean[]{m_bAfcEnable});
    }

    public void readFromParcel(Parcel in) {
        iChNum = in.readInt();
        iChIndex = in.readInt();
        iFreq = in.readInt();
        TvSystem = in.readInt();
        ServiceType = in.readInt();
		
		boolean[] myBoolean = new boolean[1];
		in.readBooleanArray(myBoolean);
		m_bAfcEnable = myBoolean[0];
    }

    public static final Parcelable.Creator<ChannelInfo> CREATOR =
            new Parcelable.Creator<ChannelInfo>() {
        public ChannelInfo createFromParcel(Parcel in) {
            ChannelInfo chInfo = new ChannelInfo();
            chInfo.readFromParcel(in);
            return chInfo;
        }

        public ChannelInfo[] newArray(int size) {
            return new ChannelInfo[size];
        }
    };

    public ChannelInfo() {
        iChNum = 0;
        iChIndex = 0;
        iFreq= 0;
        TvSystem = RT_TV_SYSTEM_UNKNOWN;
        ServiceType = RT_SERVICE_TYPE_UNKNOWN;
		m_bAfcEnable = true;
    }

    public ChannelInfo(Parcel in) {
       readFromParcel(in);
    }
}
