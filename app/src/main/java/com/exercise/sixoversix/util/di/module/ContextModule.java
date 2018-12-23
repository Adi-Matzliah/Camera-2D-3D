package com.exercise.sixoversix.util.di.module;

import android.content.Context;

import com.exercise.sixoversix.util.di.scope.AppScope;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Adi Matzliah.
 */
@Module
public class ContextModule {
    public final Context context;

    public ContextModule(Context context) {
        this.context = context;
    }

    @Provides
    @AppScope
    Context provideContext() {
        return context;
    }
}
