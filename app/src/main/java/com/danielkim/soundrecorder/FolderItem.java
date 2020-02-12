package com.danielkim.soundrecorder;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by carter on 2018-03-13.
 */

public class FolderItem implements Parcelable {
    private String mName; // file name
    private int mId; //id in database

    public FolderItem()
    {

    }

    public FolderItem(Parcel in) {
        mName = in.readString();
        mId = in.readInt();
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }


    public static final Parcelable.Creator<FolderItem> CREATOR = new Parcelable.Creator<FolderItem>() {
        public FolderItem createFromParcel(Parcel in) {
            return new FolderItem(in);
        }

        public FolderItem[] newArray(int size) {
            return new FolderItem[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
