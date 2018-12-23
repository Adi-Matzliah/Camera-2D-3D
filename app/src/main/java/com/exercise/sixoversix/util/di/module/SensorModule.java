package com.exercise.sixoversix.util.di.module;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;

import com.exercise.sixoversix.util.di.scope.AppScope;

import dagger.Module;
import dagger.Provides;

@Module
public class SensorModule {

    public final Context context;

    public SensorModule(Context context) {
        this.context = context;
    }

    @Provides
    @AppScope
    SensorManager ProvideSensorManager() {
        return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Provides
    @AppScope
    CameraManager ProvideCameraManager() {
        return (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Provides
    @AppScope
    Sensor ProvideAccelerometer(SensorManager sensorManager) {
        return sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

/*    @Provides
    @AppScope
    Sensor ProvideMagnetometer(SensorManager sensorManager) {
        return sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }*/

}
