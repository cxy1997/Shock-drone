package cxy.shockdrone;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextureView mTextureView;
    private ImageView mImageView;
    private RelativeLayout mRelativeLayout;
    private EditText mEditText[] = new EditText[4];
    private  EditText port_EditText;
    private Button mButton;
    private Handler mHandler;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private String target_IP;
    private String port_number;
    private Socket mSocket;
    private Bitmap bmp;
    private MediaPlayer mediaPlayer;
    private boolean target_configured;
    private Matrix matrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        target_configured = false;
        target_IP = getResources().getString(R.string.IP_default);
        port_number = getResources().getString(R.string.port_default);
        initView();
    }

    // 初始化各种View
    private void initView() {
        mTextureView = (TextureView) findViewById(R.id.texture_view);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.IP_as_whole);
        mButton = (Button) findViewById(R.id.button);
        String[] IP_address = target_IP.split("\\.");
        for (int i = 0; i < 4; ++i)
        {
            int resId = getResources().getIdentifier("target_IP_" + i, "id", getPackageName());
            System.out.println(resId + ", " + R.id.target_IP_0);
            mEditText[i] = (EditText) findViewById(resId);
            mEditText[i].setText(IP_address[i]);
            mEditText[i].addTextChangedListener(new MyTextWatcher(mEditText[i]));
        }
        port_EditText = (EditText) findViewById(R.id.target_port);
        port_EditText.setText(port_number);
        mImageView.setVisibility(View.GONE);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                target_IP = mEditText[0].getText().toString() + "." + mEditText[1].getText().toString() + "." + mEditText[2].getText().toString() + "." + mEditText[3].getText().toString();
                port_number = port_EditText.getText().toString();
                target_configured = true;
                mImageView.setVisibility(View.VISIBLE);
                mRelativeLayout.setVisibility(View.GONE);
                mButton.setVisibility(View.GONE);
                playMusic();
            }
        });
        configureTransform();
        mTextureView.setTransform(matrix);
        // 全屏、无框、常亮
        mTextureView.setKeepScreenOn(true);
        mTextureView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener()
        {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                // 初始化Camera
                initCamera2();
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                // 摸鱼
            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                if (null != mCameraDevice) {
                    // 释放Camera资源
                    mCameraDevice.close();
                    MainActivity.this.mCameraDevice = null;
                }
                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                bmp = Bitmap.createBitmap(mTextureView.getBitmap(), 0, 0, 1920, 1080, matrix, true);
                if (target_configured)
                    sendMessage();
            }
        });
    }

    //利用 Camera2 初始化相机
    private void initCamera2() {
        HandlerThread mThreadHandler= new HandlerThread("Camera2");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());

        String mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT; // 后摄像头, id为0
        //获取摄像头管理

        CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE); // 摄像头管理器
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "需要相机权限", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PackageManager.PERMISSION_GRANTED);
            }
            mCameraManager.openCamera(mCameraID, stateCallback, mHandler); // 打开摄像头
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 创建摄像头监听
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {//打开摄像头
            mCameraDevice = camera;
            //开启预览
            try {
                takePreview();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {//关闭摄像头
            if (null != mCameraDevice) {
                mCameraDevice.close();
                MainActivity.this.mCameraDevice = null;
            }
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {//发生错误
            Toast.makeText(MainActivity.this, "相机发生错误！", Toast.LENGTH_SHORT).show();
        }
    };

    // 开始预览
    private void takePreview() {
        try {
            SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
            mSurfaceTexture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());
            // 创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            Surface surface = new Surface(mSurfaceTexture);
            previewRequestBuilder.addTarget(surface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback()
            {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        // 自动对焦
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // 打开闪光灯
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        // 连续显示
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 将相机图像旋转90°
    private void configureTransform() {
        matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, 1920, 1080);
        RectF bufferRect = new RectF(0, 0, 1080, 1920);
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        matrix.postRotate(-90, 540, 540);
    }

    private class MyTextWatcher implements TextWatcher
    {
        private EditText myEditText;

        private MyTextWatcher(EditText someEditText) {
            super();
            myEditText = someEditText;
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() == 3)
            {
                if(myEditText == mEditText[0])
                {
                    mEditText[1].requestFocus();
                }
                else if(myEditText == mEditText[1])
                {
                    mEditText[2].requestFocus();
                }
                else if(myEditText == mEditText[2])
                {
                    mEditText[3].requestFocus();
                }
            }
            else if(s.length() == 0)
            {
                if(myEditText == mEditText[3])
                {
                    mEditText[2].requestFocus();
                }
                else if (myEditText == mEditText[2])
                {
                    mEditText[1].requestFocus();
                }
                else if(myEditText == mEditText[1])
                {
                    mEditText[0].requestFocus();
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    // 向接收端发送实时图片
    private void sendMessage() {
        Thread mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    mSocket = new Socket();
                    mSocket.connect(new InetSocketAddress(target_IP, Integer.valueOf(port_number)), 5000);
                    if (!mSocket.isBound() || !mSocket.isConnected()) {
                        return;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bmp.compress(Bitmap.CompressFormat.JPEG, Integer.valueOf(getResources().getString(R.string.bitmap_compress_level)), baos);
                    baos.flush();
                    baos.close();
                    int size = baos.size();
                    String header = String.format(Locale.US, "size:%08d", size);
                    System.out.println(header);

                    OutputStream out = mSocket.getOutputStream();
                    out.write(header.getBytes("US-ASCII"));
                    out.write(baos.toByteArray());
                    out.flush();
                    out.close();

                    mSocket.close();
                } catch (Exception e) {
                    e.printStackTrace ();
                }
            }
        });
        mThread.start();
    }

    private void playMusic(){
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.kancolle);
            System.out.println(mediaPlayer.getDuration());
            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }
            mediaPlayer.prepare();
            mediaPlayer.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
