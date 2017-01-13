package android.app.tv;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public final class SpectrumDataInfo implements Parcelable {
    private static final String TAG = "SpectrumDataInfo";

	public int specDataLen;
	public long[] specData;

    public String toString() {
        return "SpectrumDataInfo{ specDataLen: " + specDataLen + " }";
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(specDataLen);
		out.writeLongArray(specData);
    }

    public void readFromParcel(Parcel in) {
        specDataLen = in.readInt();
		in.readLongArray(specData);
    }

    public static final Parcelable.Creator<SpectrumDataInfo> CREATOR =
            new Parcelable.Creator<SpectrumDataInfo>() {
        public SpectrumDataInfo createFromParcel(Parcel in) {
            SpectrumDataInfo spInfo = new SpectrumDataInfo();
            spInfo.readFromParcel(in);
            return spInfo;
        }

        public SpectrumDataInfo[] newArray(int size) {
            return new SpectrumDataInfo[size];
        }
    };

    public SpectrumDataInfo() {
        specDataLen = 0;
		specData = new long[64];
    }

    public SpectrumDataInfo(Parcel in) {
       readFromParcel(in);
    }
}

