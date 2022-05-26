package com.example.prj_singer;

import android.provider.BaseColumns;

public final class DataBases {
    public static final class CreateDB implements BaseColumns {
        public static final String USERID = "userid";
        public static final String HIGHPITCH = "highpitch";
        public static final String _TABLENAME0 = "usertable";
        public static final String _CREATE0 = "create table if not exists "+_TABLENAME0+"("
                +_ID+" integer primary key autoincrement, "
                +USERID+" text not null , "
                +HIGHPITCH+" text not null );";
    }
}
