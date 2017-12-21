package cxy.twitch;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private ImageView backgroundImageView, foregroundImageView, RB6theme, joystick_b;
    private Touchpad touchpad;
    private Joystick joystick_f;
    private ImageButton cfg_button, button_up, button_down, button_left, button_right, button_voice;
    private Button button_confirm;
    private RelativeLayout config_pad;
    private RelativeLayout[] methods = new RelativeLayout[5];
    private ImageView[] method_images = new ImageView[5];
    private TextView IP_info;
    private EditText port_cfg, bluetooth_cfg;
    private String IP_address;
    private Switch video_stream_switch, Bluetooth_switch;
    private final Handler View_toggle_Handler = new Handler();
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private BufferedOutputStream bluetoothSocketBufferedOutputStream;
    private int chosen_control_method = -1, buttonX = 0, buttonY = 0;
    private String[] commands = {"Z", "X", "C", "A", "S", "D", "Q", "W", "E"};
    private SensorManager sensorManager;
    private SensorEventListener mSensorEventListener;
    private Vector baseline_vec = null;
    private ServerSocket mServerSocket;
    private Bitmap bmp;
    private Handler updateVideoHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            backgroundImageView.setImageBitmap(bmp);
        }
    };
    private Thread drawThread;
    private Path mpath;
    private LinkedList<Pair<Float, Float>> pointList;
    private LinkedList<Pair<String, Double>> instructions;
    private Pair<Float, Float> prev;
    private Paint normalPaint, unfinishedPaint, finishedPaint;
    private Double advanceSpeed = 0.4, turningSpeed = 0.16;
    private RecognizerDialog recognizerDialog;


    // 这两个Runnable用来切换背景（静态壁纸/实时图像）
    private final Runnable video_on_Runnable = new Runnable() {
        @Override
        public void run() {
            RB6theme.setVisibility(View.GONE);
        }
    };

    private final Runnable video_off_Runnable = new Runnable() {
        @Override
        public void run() {
            RB6theme.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        // 重力传感器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorEventListener = new SensorEventListener(){
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] values = event.values;
                if (baseline_vec == null)
                    baseline_vec = new Vector(values[0], values[1], values[2]);
                else {
                    Vector vector = new Vector(values[0], values[1], values[2]);
                    if (Vector.getAngleCos(vector, baseline_vec) > Math.cos(Math.toRadians(15))) {
                        sendBluetoothMessage("S");
                    }
                    else {
                        Vector direction = Vector.minus(vector, baseline_vec);
                        sendBluetoothMessage(new Degreetoinstruction(direction.getangle()).getInstruction());
                    }
                }
            }
        };
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=5a3b7906");
        recognizerDialog =  new RecognizerDialog(this, new InitListener() {
            @Override
            public void onInit(int code) {
                System.out.println("SpeechRecognizer init() code = " + code);
                if (code != ErrorCode.SUCCESS) {
                    System.out.println("初始化失败，错误码：" + code);
                }
            }
        });

        normalPaint = generatePaint(Color.CYAN);
        finishedPaint = generatePaint(Color.GREEN);
        unfinishedPaint = generatePaint(Color.RED);
    }

    // 生成笔触
    private Paint generatePaint(int color){
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5);
        paint.setColor(color);
        return paint;
    }

    // 初始化各种View
    private void initView() {
        backgroundImageView = (ImageView) findViewById(R.id.background_view);
        foregroundImageView = (ImageView) findViewById(R.id.foreground_view);
        RB6theme = (ImageView) findViewById(R.id.RB6_theme);
        touchpad = (Touchpad) findViewById(R.id.touchPad);
        joystick_b = (ImageView) findViewById(R.id.j01);
        cfg_button = (ImageButton) findViewById(R.id.button_config);
        button_up = (ImageButton) findViewById(R.id.button_up);
        button_down = (ImageButton) findViewById(R.id.button_down);
        button_left = (ImageButton) findViewById(R.id.button_left);
        button_right = (ImageButton) findViewById(R.id.button_right);
        button_confirm = (Button) findViewById(R.id.button_confirm);
        button_voice = (ImageButton) findViewById(R.id.button_voice);
        config_pad = (RelativeLayout) findViewById(R.id.cfg_pad);
        IP_info = (TextView) findViewById(R.id.IP_info);
        port_cfg = (EditText) findViewById(R.id.port_number);
        bluetooth_cfg = (EditText) findViewById(R.id.bluetooth_name);
        for (int i = 0; i < 5; ++i) {
            int resId = getResources().getIdentifier("c0" + (i + 1), "id", getPackageName());
            methods[i] = (RelativeLayout) findViewById(resId);
            methods[i].setVisibility(View.GONE);
            resId = getResources().getIdentifier("m0" + (i + 1), "id", getPackageName());
            method_images[i] = (ImageView) findViewById(resId);
            method_images[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < 5; ++j) {
                        if (v.getId() == method_images[j].getId()) {
                            chosen_control_method = j;
                            methods[j].setVisibility(View.VISIBLE);
                        } else {
                            methods[j].setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
        method_images[1].setAlpha(160);
        button_up.setAlpha(160);
        button_down.setAlpha(160);
        button_left.setAlpha(160);
        button_right.setAlpha(160);
        joystick_b.setAlpha(160);

        joystick_f = (Joystick) findViewById(R.id.j02);
        joystick_b.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN)
                    moveJoystick(event.getX() - joystick_b.getWidth() / 2, event.getY() - joystick_b.getHeight() / 2);
                else
                    moveJoystick(0, 0);
                return true;
            }
        });

        foregroundImageView.setVisibility(View.GONE);
        config_pad.setVisibility(View.GONE);
        backgroundImageView.setKeepScreenOn(true);
        backgroundImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        RB6theme.setKeepScreenOn(true);
        RB6theme.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        checkIP();
        port_cfg.setText(getResources().getString(R.string.port_default));
        bluetooth_cfg.setText(getResources().getString(R.string.Bluetooth_name_default));
        cfg_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg_button.setVisibility(View.GONE);
                config_pad.setVisibility(View.VISIBLE);
                foregroundImageView.setVisibility(View.GONE);
                pack_layouts();
            }
        });
        button_up.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP://松开按钮
                        buttonY = 0;
                        control_by_buttons();
                        break;
                    case MotionEvent.ACTION_DOWN://按住事件
                        buttonY = 1;
                        control_by_buttons();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        button_down.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP://松开按钮
                        buttonY = 0;
                        control_by_buttons();
                        break;
                    case MotionEvent.ACTION_DOWN://按住事件
                        buttonY = -1;
                        control_by_buttons();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        button_left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP://松开按钮
                        buttonX = 0;
                        control_by_buttons();
                        break;
                    case MotionEvent.ACTION_DOWN://按住事件
                        buttonX = -1;
                        control_by_buttons();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        button_right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP://松开按钮
                        buttonX = 0;
                        control_by_buttons();
                        break;
                    case MotionEvent.ACTION_DOWN://按住事件
                        buttonX = 1;
                        control_by_buttons();
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        button_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cfg_button.setVisibility(View.VISIBLE);
                config_pad.setVisibility(View.GONE);
                foregroundImageView.setVisibility(View.VISIBLE);
                if (Bluetooth_switch.isChecked())
                    unpack_layout(chosen_control_method);
            }
        });
        video_stream_switch = (Switch) findViewById(R.id.video_stream_switch);
        Bluetooth_switch = (Switch) findViewById(R.id.Bluetooth_switch);
        video_stream_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                video_stream_switch.setOnCheckedChangeListener(null);
                if (isChecked) {
                    try {
                        if (mServerSocket == null)
                            mServerSocket = new ServerSocket(Integer.valueOf(port_cfg.getText().toString()));
                        receiveVideoStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    View_toggle_Handler.removeCallbacks(video_off_Runnable);
                    View_toggle_Handler.postDelayed(video_on_Runnable, 300);
                } else {
                    System.out.println("Video off");
                    View_toggle_Handler.removeCallbacks(video_on_Runnable);
                    View_toggle_Handler.postDelayed(video_off_Runnable, 300);
                }
                video_stream_switch.setOnCheckedChangeListener(this);
            }
        });
        Bluetooth_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Bluetooth_switch.setOnCheckedChangeListener(null);
                if (isChecked) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!startBluetooth()) {
                                Bluetooth_switch.setChecked(false);
                            }
                        }
                    }).start();
                } else {
                    closeBluetooth();
                }
                Bluetooth_switch.setOnCheckedChangeListener(this);
            }
        });
        touchpad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent){
                if (drawThread != null)
                    if (drawThread.isAlive())
                        return true;
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Canvas canvas = touchpad.getHolder().lockCanvas();
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    touchpad.getHolder().unlockCanvasAndPost(canvas);
                    mpath = new Path();
                    pointList = new LinkedList<>();
                    mpath.moveTo(motionEvent.getX(), motionEvent.getY());
                    pointList.add(new Pair<>(motionEvent.getX(), motionEvent.getY()));
                    prev = new Pair<>(motionEvent.getX(), motionEvent.getY());
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(motionEvent.getX() - prev.first) > 20 || Math.abs(motionEvent.getY() - prev.second) > 20) {
                        mpath.lineTo(motionEvent.getX(), motionEvent.getY());
                        Canvas canvas = touchpad.getHolder().lockCanvas();
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        canvas.drawPath(mpath, normalPaint);
                        touchpad.getHolder().unlockCanvasAndPost(canvas);
                        pointList.add(new Pair<>(motionEvent.getX(), motionEvent.getY()));
                        prev = new Pair<>(motionEvent.getX(), motionEvent.getY());
                    }
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    instructions = getInstructions(pointList);
                    conductInstructions();
                }
                return true;
            }
        });
        button_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SpeechUtility.getUtility().checkServiceInstalled()){
                    String url = SpeechUtility.getUtility().getComponentUrl();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return;
                }

                recognizerDialog.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
                recognizerDialog.setParameter(SpeechConstant.DOMAIN, "iat");
                recognizerDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
                recognizerDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
                recognizerDialog.setListener(new RecognizerDialogListener() {
                    @Override
                    public void onResult(RecognizerResult recognizerResult, boolean b) {
                        process(recognizerResult.getResultString());
                    }

                    @Override
                    public void onError(SpeechError speechError) {
                        System.out.println(speechError.toString());
                    }
                });
                recognizerDialog.show();
            }
        });
        pack_layouts();
    }

    // 执行一系列指令
    private void conductInstructions() {
        drawThread = new Thread() {
            @Override
            public void run() {
                Canvas canvas = touchpad.getHolder().lockCanvas();
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawPath(mpath, unfinishedPaint);
                touchpad.getHolder().unlockCanvasAndPost(canvas);

                Path trajectory = new Path();
                trajectory.moveTo(pointList.get(0).first, pointList.get(0).second);
                for (int i = 0;i < instructions.size();i++) {
                    sendBluetoothMessage(instructions.get(i).first);
                    double delay = instructions.get(i).second;
                    try {
                        Thread.sleep((long) delay);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    if (i % 2 == 1) {
                        trajectory.lineTo(pointList.get(i / 2 + 1).first, pointList.get(i / 2 + 1).second);
                        canvas = touchpad.getHolder().lockCanvas();
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        canvas.drawPath(mpath, unfinishedPaint);
                        canvas.drawPath(trajectory, finishedPaint);
                        touchpad.getHolder().unlockCanvasAndPost(canvas);
                    }
                }
                trajectory.lineTo(pointList.get(pointList.size() - 1).first, pointList.get(pointList.size() - 1).second);
                canvas = touchpad.getHolder().lockCanvas();
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                canvas.drawPath(mpath, unfinishedPaint);
                canvas.drawPath(trajectory, finishedPaint);
                touchpad.getHolder().unlockCanvasAndPost(canvas);
                sendBluetoothMessage("S");
                mpath.close();
                trajectory.close();

                // 清屏
                canvas = touchpad.getHolder().lockCanvas();
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                touchpad.getHolder().unlockCanvasAndPost(canvas);
            }
        };
        drawThread.start();
    }

    //获取语音
    private void process(String json) {
        StringBuffer result = new StringBuffer();
        try {
            JSONObject sentence = new JSONObject(json);
            JSONArray words = sentence.getJSONArray("ws");
            for (int i = 0;i < words.length();i++) {
                JSONArray chineseWords = words.getJSONObject(i).getJSONArray("cw");
                JSONObject chineseWord = chineseWords.getJSONObject(0);
                result.append(chineseWord.getString("w"));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println(result.toString());
        if (result.toString().contains("前")) {
            sendBluetoothMessage("W");
        }
        else if (result.toString().contains("后")) {
            sendBluetoothMessage("X");
        }
        else if (result.toString().contains("左")) {
            sendBluetoothMessage("A");
        }
        else if (result.toString().contains("右")) {
            sendBluetoothMessage("D");
        }
        else if (result.toString().contains("停")) {
            sendBluetoothMessage("S");
        }
    }

    // 获取本机IP地址，存入IP_address
    private void checkIP() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        // 判断wifi是否开启，wifi未开启时，返回的IP为0.0.0.0
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        IP_address = intToIp(wifiManager.getConnectionInfo().getIpAddress());
        IP_info.setText(getResources().getString(R.string.IP_prefix) + IP_address + ":");
    }

    // 分割二进制IP地址
    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }

    // 启动蓝牙连接
    private boolean startBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bluetoothDeviceSet;
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
        if (bluetoothDeviceSet.size() != 0) {
            boolean find = false;
            for (BluetoothDevice dd : bluetoothDeviceSet) {
                if (dd.getName().equals(bluetooth_cfg.getText().toString())) {
                    bluetoothDevice = dd;
                    find = true;
                }
            }
            if (!find)
                return false;
        }
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            bluetoothSocketBufferedOutputStream = new BufferedOutputStream(bluetoothSocket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // 通过蓝牙发送控制指令
    private void sendBluetoothMessage(String s) {
        try {
            bluetoothSocketBufferedOutputStream.write(s.getBytes());
            bluetoothSocketBufferedOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 关闭蓝牙
    private void closeBluetooth() {
        try {
            bluetoothSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 接收视频流
    private void receiveVideoStream() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket;
                try {
                    while (video_stream_switch.isChecked()) {
                        socket = mServerSocket.accept();
                        int imageSize = 0;
                        String packageHeader;
                        InputStream inputStream = socket.getInputStream();
                        byte buffer[] = new byte[13];
                        int temp = 0;
                        if ((temp = inputStream.read(buffer)) != -1) {
                            packageHeader = new String(buffer, 0, temp);
                            if (packageHeader.startsWith("size:"))
                                imageSize = Integer.valueOf(packageHeader.split(":")[1]);
                            byte[] imageData = new byte [imageSize];
                            byte[] tmpData = new byte[4*1024];
                            int pos = 0;
                            while((temp = inputStream.read(tmpData)) != -1) {
                                if (pos+temp > imageSize) {
                                    System.arraycopy(tmpData, 0, imageData, pos, imageSize-pos);
                                    break;
                                }
                                System.arraycopy(tmpData, 0, imageData, pos, temp);
                                pos = pos + temp;
                            }
                            if (pos>0) {
                                bmp = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                                updateVideoHandler.sendEmptyMessage(0);
                            }
                        }
                    }
                    mServerSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 展开控制布局
    private void unpack_layout(int control_method) {
        switch (control_method) {
            case 0:
                button_up.setVisibility(View.VISIBLE);
                button_down.setVisibility(View.VISIBLE);
                button_left.setVisibility(View.VISIBLE);
                button_right.setVisibility(View.VISIBLE);
                break;
            case 1:
                joystick_b.setVisibility(View.VISIBLE);
                joystick_f.setVisibility(View.VISIBLE);
                break;
            case 2:
                touchpad.setVisibility(View.VISIBLE);
                break;
            case 3:
                start_gravity_control();
                break;
            case 4:
                button_voice.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    // 收起控制布局
    private void pack_layout(int control_method) {
        if (Bluetooth_switch.isChecked())
            sendBluetoothMessage("S");

        switch (control_method) {
            case 0:
                button_up.setVisibility(View.GONE);
                button_down.setVisibility(View.GONE);
                button_left.setVisibility(View.GONE);
                button_right.setVisibility(View.GONE);
                break;
            case 1:
                joystick_b.setVisibility(View.GONE);
                joystick_f.setVisibility(View.GONE);
                break;
            case 2:
                touchpad.setVisibility(View.GONE);
                break;
            case 3:
                end_gravity_control();
                break;
            case 4:
                button_voice.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    // 收起所有控制布局
    private void pack_layouts() {
        for(int i = 0; i< 5; ++i)
            pack_layout(i);
    }

    // 按键遥控
    private void control_by_buttons(){
        int cal_res = buttonY * 3 + buttonX + 4;
        sendBluetoothMessage(commands[cal_res]);
    }

    // 开始重力感应遥控
    private void start_gravity_control(){
        sensorManager.registerListener(mSensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_GAME);
    }

    // 结束重力感应遥控
    private void end_gravity_control(){
        if (baseline_vec != null) {
            sensorManager.unregisterListener(mSensorEventListener);
            baseline_vec = null;
        }
    }

    // 重绘虚拟摇杆并更新指令
    private void moveJoystick(float x, float y){
        joystick_f.move(x, y);
        if (x * x + y * y < 105 * 105)
            sendBluetoothMessage("S");
        else {
            Vector v = new Vector(y, x, 0);
            sendBluetoothMessage(v.gravity_instruction());
        }
    }

    // 根据绘制路径生成真实指令
    private LinkedList<Pair<String, Double>> getInstructions(final LinkedList<Pair<Float, Float>> pointList) {
        LinkedList<Pair<String, Double>> instructions = new LinkedList<>();
        Double ang;
        int size = pointList.size();
        for (int i = 0;i < size - 2;i++) {
            instructions.add(new Pair<String, Double>("W", distance(pointList.get(i), pointList.get(i + 1)) / advanceSpeed));
            ang = angle(pointList.get(i), pointList.get(i + 1), pointList.get(i + 2));
            if (ang > 0)
                instructions.add(new Pair<String, Double>("A", ang / turningSpeed));
            else
                instructions.add(new Pair<String, Double>("D", -ang / turningSpeed));
        }
        if (size >= 2)
            instructions.add(new Pair<String, Double>("W", distance(pointList.get(size-2), pointList.get(size-1)) / advanceSpeed));
        return instructions;
    }

    // 计算转向角度
    private Double angle(Pair<Float, Float> p1, Pair<Float, Float> p2, Pair<Float, Float> p3){
        Double a1 = Math.atan2(p1.second - p2.second, p2.first - p1.first), a2 = Math.atan2(p2.second - p3.second, p3.first - p2.first);
        Double a = Math.toDegrees(a2 - a1);
        while (a >= 180)
            a = a - 360;
        while (a <= -180)
            a = a + 360;
        return a;
    }

    // 计算距离
    private Double distance(Pair<Float, Float> p1, Pair<Float, Float> p2){
        return Math.sqrt(Math.pow(p1.first - p2.first, 2) + Math.pow(p1.second - p2.second, 2));
    }
}
