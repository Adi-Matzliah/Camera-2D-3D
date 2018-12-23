package com.exercise.sixoversix.util.di.component;

import com.exercise.sixoversix.feature.main.impl.MainActivity;
import com.exercise.sixoversix.application.App;
import com.exercise.sixoversix.util.di.module.AppModule;
import com.exercise.sixoversix.util.di.module.ContextModule;
import com.exercise.sixoversix.util.di.module.SensorModule;
import com.exercise.sixoversix.util.di.module.StorageModule;
import com.exercise.sixoversix.util.di.scope.AppScope;

import dagger.Component;

@AppScope
@Component(modules = {AppModule.class, ContextModule.class, SensorModule.class, StorageModule.class})
public interface AppComponent {
    void inject(App app);

    void inject(MainActivity activity);
}