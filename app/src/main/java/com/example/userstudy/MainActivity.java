package com.example.userstudy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Service;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Vibrator; // 記得匯入震動類別
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.IntStream;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements INetworkCallback {

    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static final int CAMERA_FRONT = 1;
    public static final int CAMERA_BACK = 0;
    private int currentCameraId = CAMERA_BACK;
    private int mPicturesToken = 0;

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private TCPManager mTCPManager;
    private MainActivity mActivity;
    private EditText mEditText;
    private HashMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture);
        mEditText = findViewById(R.id.editText2);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        mActivity = this;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTCPManager = new TCPManager(mEditText.getText().toString(), 8888);
                mTCPManager.start();
                mTCPManager.callback = mActivity;
                mEditText.setVisibility(View.INVISIBLE);
                takePictureButton.setVisibility(View.INVISIBLE);
            }
        });

//        YTSensorManager.getInstance().setSensorCallback(this);
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera(0);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.i("YueTing", "onOpened");
            cameraDevice = camera;
//            try {
//                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
//
//                int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
//                boolean isSupportedManualFocus = false;
//                for(int i=0;i<capabilities.length;i++) {
//                    isSupportedManualFocus |= (capabilities[i] == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);
//                }
//
//                Log.e("QinYue", String.valueOf(isSupportedManualFocus));
//                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
//                captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 2.0f);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            createCameraPreview();
            if(mPicturesToken % 2 != 0){
                takePicture();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        assert cameraDevice != null;
        Log.i("YueTing", "Take a picture");
        mPicturesToken++;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            int width = 1920; //default
            int height = 1080; // default
//            if (jpegSizes != null && 0 < jpegSizes.length) {
//                width = jpegSizes[0].getWidth();
//                height = jpegSizes[0].getHeight();
//                Log.i("YueTing", String.format("%d %d", width, height));
//            }
            imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(imageReader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
//            captureRequestBuilder.addTarget(imageReader.getSurface());
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
//            captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.5f);
//            Log.e("QinTue", "----------------");
//            Log.e("QinYue", String.valueOf(captureRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE)));
//            Log.e("QinYue", String.valueOf(captureRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE)));
//            Log.e("QinYue", String.valueOf(captureRequestBuilder.get(CaptureRequest.CONTROL_AF_REGIONS)));
//            Log.e("QinYue", String.valueOf(captureRequestBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE)));
//            updatePreview();

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
//            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        mTCPManager.send(bytes);
                        Log.i("YueTing", "onImageAvailable");

                        closeCamera();

                        if (textureView.isAvailable()) {
                            if(currentCameraId == CAMERA_BACK) {
                                openCamera(CAMERA_FRONT);
                                currentCameraId = CAMERA_FRONT;
                            } else {
                                openCamera(CAMERA_BACK);
                                currentCameraId = CAMERA_BACK;
                            }
                        } else {
                            textureView.setSurfaceTextureListener(textureListener);
                        }

                        if(mPicturesToken % 2 == 0) {
                            setVibrate(100);
                        }

                    } catch(Exception e) {
                        Log.e(TAG, e.getMessage());
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            imageReader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.i("YueTing", "CaptureComplete");


                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
//                        session.capture(captureRequestBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            Log.i("QinYue", "createCameraPreview");
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    Log.i("QinYue", "CameraSession OnConfigured");
                    if (null == cameraDevice)
                        return;
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();


                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int id) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[id];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                    try {
                        cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to start camera preview.", e);
                }
            }
        }, 500);


        Log.i("YueTing", "updatePreview");
    }

    private void closeCamera() {
        Log.i("YueTing", "closeCamera");
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != cameraCaptureSessions) {
            cameraCaptureSessions.close();
            cameraCaptureSessions = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }

        if(null != captureRequestBuilder) {
            captureRequestBuilder = null;
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera(currentCameraId);
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }

        //reopen camera
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    public void setVibrate(int time){
        Vibrator myVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        myVibrator.vibrate(time);
    }

    @Override
    public void onReceiveData(String data) {
        if(data.equals("tp")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    takePicture();
                }
            });
        }

    }

//    private JSONObject json;
//    @Override
//    public void OnEnumerateSupportedSensor(List<Sensor> sensors) {
//        json = new JSONObject();
//        for(int i=0;i<sensors.size();i++) {
//            try {
//                JSONObject obj = new JSONObject();
//                obj.put("x", 0.0f);
//                obj.put("y", 0.0f);
//                obj.put("z", 0.0f);
//                json.put(sensors.get(i).getStringType(), obj);
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    public void OnSensorChanged(SensorEvent event) {
//        float x = event.values[0];
//        float y = event.values[1];
//        float z = event.values[2];
//        try {
//            JSONObject obj = new JSONObject();
//            obj.put("x", x);
//            obj.put("y", y);
//            obj.put("z", z);
//            json.remove(event.sensor.getStringType());
//            json.put(event.sensor.getStringType(), obj);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//    }
}
