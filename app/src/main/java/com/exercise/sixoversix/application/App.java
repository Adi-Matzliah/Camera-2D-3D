package com.exercise.sixoversix.application;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.exercise.sixoversix.util.di.component.AppComponent;
import com.exercise.sixoversix.util.di.component.DaggerAppComponent;
import com.exercise.sixoversix.util.di.module.AppModule;
import com.exercise.sixoversix.util.di.module.ContextModule;
import com.exercise.sixoversix.util.di.module.SensorModule;
import com.exercise.sixoversix.util.di.module.StorageModule;

public class App extends Application {

    @NonNull
    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        createAppComponent();
    }

    private void createAppComponent() {
        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .contextModule(new ContextModule(getApplicationContext()))
                .sensorModule(new SensorModule(getApplicationContext()))
                .storageModule(new StorageModule(getApplicationContext(), "db_contacts"))
                .build();
        appComponent.inject(this);
    }

    public static AppComponent getAppComponent(Context context) {
        return ((App) context.getApplicationContext()).appComponent;
    }

}
