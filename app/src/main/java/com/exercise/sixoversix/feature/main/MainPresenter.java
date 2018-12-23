package com.exercise.sixoversix.feature.main;

import android.hardware.SensorEvent;

import com.exercise.sixoversix.feature.main.impl.MainScreenPresenter;
import com.hannesdorfmann.mosby3.mvp.MvpPresenter;

public interface MainPresenter extends MvpPresenter<MainView> {

    void handleSensor(SensorEvent sensorEvent);

    void setDimensionState(@MainScreenPresenter.DimensionState int dimensionState);
}