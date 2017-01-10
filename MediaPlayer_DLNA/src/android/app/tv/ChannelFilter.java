package android.app.tv;

import android.os.Parcel;
import android.os.Parcelable;

public final class ChannelFilter implements Parcelable {
    private static final String TAG = "ChannelFilter";

    public boolean keepSelected;
    public boolean scrambled;
    public boolean isFake;
    public boolean nonTv;
    public boolean atv;
    public boolean dtv;
    public boolean avDtv;
    public boolean audioDtv;
    public boolean hidden;
    public boolean hiddenGuide;
    public boolean oneSegment;
    public boolean nonPrimaryChannel;
    public boolean userDeleted;
    public boolean userFavorite;
    public boolean userSkipped;
    public boolean userLocked;
    public boolean userAdded;

    public String toString() {
        return "ChannelFilter{ keepSelected: " + keepSelected
            + ",scrambled: " + scrambled
            + ",isFake: " + isFake
            + ",nonTv: " + nonTv
            + ",atv: " + atv
            + ",dtv: " + dtv
            + ",avDtv: " + avDtv
            + ",audioDtv: " + audioDtv
            + ",hidden: " + hidden
            + ",hiddenGuide: " + hiddenGuide
            + ",oneSegment: " + oneSegment
            + ",nonPrimaryChannel: " + nonPrimaryChannel
            + ",userDeleted: " + userDeleted
            + ",userFavorite: " + userFavorite
            + ",userSkipped: " + userSkipped
            + ",userLocked: " + userLocked
            + ",userAdded: " + userAdded + " }";
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeValue(keepSelected);
        out.writeValue(scrambled);
        out.writeValue(isFake);
        out.writeValue(nonTv);
        out.writeValue(atv);
        out.writeValue(dtv);
        out.writeValue(avDtv);
        out.writeValue(audioDtv);
        out.writeValue(hidden);
        out.writeValue(hiddenGuide);
        out.writeValue(oneSegment);
        out.writeValue(nonPrimaryChannel);
        out.writeValue(userDeleted);
        out.writeValue(userFavorite);
        out.writeValue(userSkipped);
        out.writeValue(userLocked);
        out.writeValue(userAdded);
    }

    public void readFromParcel(Parcel in) {
        keepSelected = (Boolean)in.readValue(null);
        scrambled = (Boolean)in.readValue(null);
        isFake = (Boolean)in.readValue(null);
        nonTv = (Boolean)in.readValue(null);
        atv = (Boolean)in.readValue(null);
        dtv = (Boolean)in.readValue(null);
        avDtv = (Boolean)in.readValue(null);
        audioDtv = (Boolean)in.readValue(null);
        hidden = (Boolean)in.readValue(null);
        hiddenGuide = (Boolean)in.readValue(null);
        oneSegment = (Boolean)in.readValue(null);
        nonPrimaryChannel = (Boolean)in.readValue(null);
        userDeleted = (Boolean)in.readValue(null);
        userFavorite = (Boolean)in.readValue(null);
        userSkipped = (Boolean)in.readValue(null);
        userLocked = (Boolean)in.readValue(null);
        userAdded = (Boolean)in.readValue(null);
    }

    public static final Parcelable.Creator<ChannelFilter> CREATOR =
            new Parcelable.Creator<ChannelFilter>() {
        public ChannelFilter createFromParcel(Parcel in) {
            ChannelFilter filter = new ChannelFilter();
            filter.readFromParcel(in);
            return filter;
        }

        public ChannelFilter[] newArray(int size) {
            return new ChannelFilter[size];
        }
    };

    public ChannelFilter() {
        keepSelected = false;
        scrambled = false;
        isFake = false;
        nonTv = false;
        atv = false;
        dtv = false;
        avDtv = false;
        audioDtv = false;
        hidden = false;
        hiddenGuide = false;
        oneSegment = false;
        nonPrimaryChannel = false;
        userDeleted = false;
        userFavorite = false;
        userSkipped = false;
        userLocked = false;
        userAdded = false;
    }

    public ChannelFilter(Parcel in) {
       readFromParcel(in);
    }
}
