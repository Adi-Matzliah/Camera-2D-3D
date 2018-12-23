package com.exercise.sixoversix.feature.main.impl;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.media.ExifInterface;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import com.exercise.sixoversix.data.StorageRepository;
import com.exercise.sixoversix.feature.main.MainPresenter;
import com.exercise.sixoversix.feature.main.MainView;
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.DimensionState.THREE_D;
import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.DimensionState.TWO_D;
import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.PositionDetection.AWESOME;
import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.PositionDetection.BAD;
import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.PositionDetection.ALMOST_GOOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public class MainScreenPresenter extends MvpBasePresenter<MainView> implements MainPresenter {

    private static final int MAX_ROTATION_IDENTATION = 10;

    private static final int MIN_FLAT_DEVICE_ANGLE = 25;
    private static final int MAX_FLAT_DEVICE_ANGLE = 155;

    private static final int MIN_EYE_LEVEL_INCLINATION = 80;
    private static final int MAX_EYE_LEVEL_INCLINATION = 100;

    private static final int CUSTOM_IMAGE_WIDTH = 640;
    private static final int CUSTOM_IMAGE_HEIGHT = 480;

    @NonNull
    final CameraManager cameraManager;

    @NonNull
    final StorageRepository storageRepo;

    @Retention(SOURCE) // means that the annotation is only available in the source code.
    @IntDef({TWO_D, THREE_D})
    public @interface DimensionState {
        int TWO_D = 2;
        int THREE_D = 3;
    }

    @NonNull
    @DimensionState
    private int dimensionsState = TWO_D; // indicate whether the topbar is enabled or disabled in the system

    @Retention(SOURCE) // means that the annotation is only available in the source code.
    @StringDef({BAD, ALMOST_GOOD, AWESOME})
    public @interface PositionDetection {
        String BAD = "BAD";
        String ALMOST_GOOD = "ALMOST GOOD...";
        String AWESOME = "AWESOME";
    }

    private final int ORIENTATION_PORTRAIT = ExifInterface.ORIENTATION_ROTATE_90; // 6
    private final int ORIENTATION_LANDSCAPE_REVERSE = ExifInterface.ORIENTATION_ROTATE_180; // 3
    private final int ORIENTATION_LANDSCAPE = ExifInterface.ORIENTATION_NORMAL; // 1
    private final int ORIENTATION_PORTRAIT_REVERSE = ExifInterface.ORIENTATION_ROTATE_270; // 8

    private int smoothness = 1;
    private float averagePitch = 0;
    private float averageRoll = 0;
    private int rotationAngel;
    private int inclination;

    private float[] pitches;
    private float[] rolls;
    private float[] gravity;
    private float[] geomagnetic;
    private boolean isFlatDevice;

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    @Inject
    public MainScreenPresenter(@NonNull CameraManager cameraManager, @NonNull StorageRepository storageRepo) {
        this.cameraManager = cameraManager;
        this.storageRepo = storageRepo;
        pitches = new float[smoothness];
        rolls = new float[smoothness];
    }

    @Override
    public void handleSensor(SensorEvent sensorEvent) {
        switch (this.dimensionsState) {
            case TWO_D:
                handle2D(sensorEvent);
                break;
            case THREE_D:
                handle3D(sensorEvent);
                break;
        }

    }

    private void handle2D(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                calculateRotationAndInclination(sensorEvent);
                if (inclination < MIN_FLAT_DEVICE_ANGLE || inclination > MAX_FLAT_DEVICE_ANGLE) {
                    isFlatDevice = true;
                    getView().rotateScreen2Axis(rotationAngel);
                }
                else {
                    isFlatDevice = false;
                    getView().rotateScreen2Axis(rotationAngel);
                }
                handleDevicePosition(inclination, rotationAngel);
                getView().showAccelerometerInfo(String.valueOf(rotationAngel));
                break;
        }
    }

    private void handle3D(SensorEvent sensorEvent) {
        pitches = new float[smoothness];
        rolls = new float[smoothness];
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = sensorEvent.values;
            calculateRotationAndInclination(sensorEvent);
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = sensorEvent.values;
        }
        if (gravity != null && geomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float orientationData[] = new float[3];
                SensorManager.getOrientation(R, orientationData);
                averagePitch = addValue(orientationData[1], pitches);
                averageRoll = addValue(orientationData[2], rolls);
                Log.d("pitch:", String.valueOf(averagePitch));
                Log.d("roll:", String.valueOf(averageRoll));
            }
            getView().rotateScreen3Axis(averagePitch, averageRoll, rotationAngel);
        }

        getView().showAccelerometerInfo(String.valueOf(rotationAngel));
    }

    private float addValue(float value, float[] values) {
        value = (float) Math.round((Math.toDegrees(value)));
        float average = 0;
        for (int i = 1; i < smoothness; i++) {
            values[i - 1] = values[i];
            average += values[i];
        }
        values[smoothness - 1] = value;
        average = (average + value) / smoothness;
        return average;
    }

    private void calculateRotationAndInclination(SensorEvent sensorEvent) {
        final float[] g;
        final boolean isFlatDevice;
        g = sensorEvent.values.clone();

        double normalGravity = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);

        // Normalize the accelerometer vector
        g[0] = g[0] / (float)normalGravity;
        g[1] = g[1] / (float)normalGravity;
        g[2] = g[2] / (float)normalGravity;

        inclination = (int) Math.round(Math.toDegrees(Math.acos(g[2])));

        rotationAngel = (int) Math.round(Math.toDegrees(Math.atan2(g[0], g[1])));
    }


    private int calculateOrientation() {
        // finding local orientation dip
        if (((rotationAngel == ORIENTATION_PORTRAIT || rotationAngel == ORIENTATION_PORTRAIT_REVERSE)
                && (averageRoll > -30 && averageRoll < 30))) {
            if (averagePitch > 0)
                return ORIENTATION_PORTRAIT_REVERSE;
            else
                return ORIENTATION_PORTRAIT;
        } else {
            // divides between all orientations
            if (Math.abs(averagePitch) >= 30) {
                if (averagePitch > 0)
                    return ORIENTATION_PORTRAIT_REVERSE;
                else
                    return ORIENTATION_PORTRAIT;
            } else {
                if (averageRoll > 0) {
                    return ORIENTATION_LANDSCAPE_REVERSE;
                } else {
                    return ORIENTATION_LANDSCAPE;
                }
            }
        }
    }

    @Override
        public void setDimensionState(@DimensionState int dimensionState) {
            this.dimensionsState = dimensionState;
        }

        private void handleDevicePosition(double inclination, double rotationAngle) {
            if (inclination > MIN_EYE_LEVEL_INCLINATION  && inclination < MAX_EYE_LEVEL_INCLINATION && Math.abs((float)rotationAngle) < MAX_ROTATION_IDENTATION) {
                getView().showCaptureButton();
                getView().showDevicePositionMessage(AWESOME);
            }
            else if (inclination > MIN_EYE_LEVEL_INCLINATION  && inclination < MAX_EYE_LEVEL_INCLINATION || Math.abs((float)rotationAngle) < MAX_ROTATION_IDENTATION) {
                getView().hideCaptureButton();
                getView().showDevicePositionMessage(ALMOST_GOOD);
            }
            else {
                getView().hideCaptureButton();
                getView().showDevicePositionMessage(BAD);
            }
    }

    public void takePicture(CameraDevice cameraDevice, int rotation, SurfaceTexture surfaceTexture, CaptureRequest.Builder captureBuilder, Handler backgroundHandler) {
        CameraCharacteristics characteristics = null;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
        } catch (CameraAccessException e1) {
            getView().showMessage(e1.getMessage());
        }
        Size[] jpegSizes = null;
        if(characteristics != null)
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);

        //Capture image with custom size
        int width = CUSTOM_IMAGE_WIDTH;
        int height = CUSTOM_IMAGE_HEIGHT;
        if(jpegSizes != null && jpegSizes.length > 0)
        {
            width = jpegSizes[0].getWidth();
            height = jpegSizes[0].getHeight();
        }
        final ImageReader reader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
        List<Surface> outputSurface = new ArrayList<>(2);
        outputSurface.add(reader.getSurface());
        outputSurface.add(new Surface(surfaceTexture));

        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        //Check orientation base on device
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

        storageRepo.saveFile(reader, backgroundHandler);

        final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                //unlockFocus();
                getView().showMessage("file is saved!");//"Saved: " + mFile
                getView().createCameraPreview();
                //getView().openCamera();
            }
        };


        try {
            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }
}