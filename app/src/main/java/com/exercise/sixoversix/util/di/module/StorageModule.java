package com.exercise.sixoversix.util.di.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.exercise.sixoversix.util.di.scope.AppScope;

import java.io.File;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Anonym on 07/03/2018.
 */
@Module
public class StorageModule {
    private String mFileName;
    //private RoomDatabase mRoomDB;
    public StorageModule(Context context, String fileName) {
        this.mFileName = fileName;
        //this.mRoomDB = Room.databaseBuilder(context, ContactsDatabase.class, mFileName).build();
    }

    @Provides
    @AppScope
    File provideFileInternalStorage(Context context) {
        return context.getFileStreamPath(mFileName);
    }

    @Provides
    @AppScope
    SharedPreferences provideSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

/*    @Provides
    @AppScope
    ContactsDatabase provideDB(Context context) {
        return Room.databaseBuilder(context,
                ContactsDatabase.class, mFileName).build();
    }

    @Provides
    @AppScope
    ContactDao provideDao(ContactsDatabase db) {
        return db.contactDao();
    }

    @Provides
    @AppScope
    StorageRepository provideStorageRepository(ContactsDatabase roomDB){
        return new StorageRepository(roomDB);
    }*/
}
