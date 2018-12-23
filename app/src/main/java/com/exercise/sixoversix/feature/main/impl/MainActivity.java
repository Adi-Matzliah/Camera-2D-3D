package com.exercise.sixoversix.feature.main.impl;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.exercise.sixoversix.R;
import com.exercise.sixoversix.application.App;
import com.exercise.sixoversix.feature.camera.AutoFitTextureView;
import com.exercise.sixoversix.feature.main.MainView;

import java.util.Arrays;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.DimensionState.THREE_D;
import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.DimensionState.TWO_D;
import static com.exercise.sixoversix.feature.main.impl.MainScreenPresenter.PositionDetection;


public class MainActivity extends AppCompatActivity implements
        MainView,
        SensorEventListener {

    @Inject
    SensorManager sensorManager;

    @Inject
    Sensor accelerometer;

/*    @Inject
    Sensor magnetometer;*/

    @Inject
    MainScreenPresenter presenter;

    @BindView(R.id.pictureStatus)
    TextView pictureTextView;

    @BindView(R.id.capturePicture)
    Button btnCapture;

    @BindView(R.id.dimensionsToggleButton)
    ToggleButton btnDimeToggle;

    /* Camera Variable */
    @Inject
    CameraManager cameraManager;

    @BindView(R.id.texture)
    AutoFitTextureView textureView;

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean flashSupported;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        App.getAppComponent(this).inject(this);
        ButterKnife.bind(this);
        presenter.attachView(this);

        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
    }

    protected void onPause() {
        stopBackgroundThread();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        super.onPause();
    }

    protected void onResume() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        startBackgroundThread();
        if(textureView.isAvailable()) {
            openCamera();
        }
        else {
            textureView.setSurfaceTextureListener(textureListener);
        }
        super.onResume();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        presenter.handleSensor(sensorEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    public void showAccelerometerInfo(String text) {
        //pictureTextView.setText(text);
    }

    @Override
    public void rotateScreen2Axis(double angle) {
        textureView.setRotation((float)angle);
        textureView.setRotationX(0);
        textureView.setRotationY(0);
    }

    /*
        Pitch (degrees of rotation about the x axis)
        Roll (degrees of rotation about the y axis)
        Azimuth (degrees of rotation about the z axis)
     */
    @Override
    public void rotateScreen3Axis(float pitch, float roll, int azimuth) {
        textureView.setRotation(0);//
        textureView.setRotationX(-pitch -90);//
        textureView.setRotationY(-roll);
    }

    @Override
    public void showDevicePositionMessage(@PositionDetection String status) {
        pictureTextView.setText(status);
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showCaptureButton() {
        btnCapture.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideCaptureButton() {
        btnCapture.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.dimensionsToggleButton)
    void onToggleDimeButtonPressed() {
        presenter.setDimensionState(!btnDimeToggle.isChecked() ? TWO_D : THREE_D);
    }

    @Override
    protected void onDestroy() {
        presenter.detachView();
        super.onDestroy();
    }

    // Camera Ui handling

    @OnClick(R.id.capturePicture)
    void onCapturePicturePressed() {
        if(cameraDevice != null && textureView != null) {
            try {
                final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                presenter.takePicture(cameraDevice,
                        getWindowManager().getDefaultDisplay().getRotation(),
                        textureView.getSurfaceTexture(),
                        captureBuilder,
                        backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }
    }

    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    public void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    showMessage(getString(R.string.config_failed_msg));
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(cameraDevice == null) {
            showMessage(getString(R.string.update_preview_error));
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openCamera() {
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{

            for (String cameraId : cameraManager.getCameraIdList()) {
                this.cameraId = cameraId;
                CameraCharacteristics characteristics
                        = cameraManager.getCameraCharacteristics(cameraId);

                // We skip on no-front facing camera.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                //cameraId = cameraManager.getCameraIdList()[0];
            }
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }

            cameraManager.openCamera(cameraId,stateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) { }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) { }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showMessage(getString(R.string.camera_require_permission_error));
                finish();
            }
        }
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try{
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread(getString(R.string.camera_bg));
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
}
