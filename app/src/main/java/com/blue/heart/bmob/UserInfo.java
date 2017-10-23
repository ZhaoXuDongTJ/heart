package com.blue.heart.bmob;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by 92198 on 2017/10/23.
 */

public class UserInfo extends BmobObject {

    String userID;
    BmobFile headPortrait;

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public BmobFile getHeadPortrait() {
        return headPortrait;
    }

    public void setHeadPortrait(BmobFile headPortrait) {
        this.headPortrait = headPortrait;
    }


}
