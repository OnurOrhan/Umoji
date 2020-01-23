package com.umoji.umoji.Utils;

import android.os.Environment;

/**
 * Created by User on 7/24/2017.
 */

public class FilePaths {

    //"storage/emulated/0"
    public String ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath();

    public String PICTURES = ROOT_DIR + "/Pictures";
    public String CAMERA = ROOT_DIR + "/DCIM/Camera";

    //public String STORIES = ROOT_DIR + "/Stories";
    //public String FIREBASE_STORY_STORAGE = "stories/users";
    public String FIREBASE_PROFILE_PHOTO_STORAGE = "profile_photos/";

}
