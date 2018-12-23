package com.exercise.sixoversix.feature.main;

import com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.PositionDetection;
import com.hannesdorfmann.mosby3.mvp.MvpView;

public interface MainView extends MvpView {

    void showAccelerometerInfo(String text);

    void openCamera();

    void createCameraPreview();

    void rotateScreen2Axis(double angle);

    void rotateScreen3Axis(float pitch, float roll, int azimuth);

    void showDevicePositionMessage(@PositionDetection String status);

    void showMessage(String message);

    void showCaptureButton();

    void hideCaptureButton();
}
