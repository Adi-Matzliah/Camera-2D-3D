package com.exercise.sixoversix.util.di.module;

import android.app.Application;

import com.exercise.sixoversix.util.di.scope.AppScope;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule {
    Application application;

    public AppModule(Application application) {
        this.application = application;
    }

    @Provides
    @AppScope
    Application provideApplication() {
        return application;
    }
}
