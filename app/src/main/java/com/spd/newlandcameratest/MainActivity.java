package com.spd.newlandcameratest;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.serialport.DeviceControlSpd;
import android.serialport.SerialPortSpd;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    //新大陆解码摄像头是否开启并联通标志
    private static boolean isCodeCamerOpen = false;
    private static int fd = -1;
    SerialPortSpd serialPortSpd = null;
    private static SoundPool sp; //声音池
    private static Map<Integer, Integer> mapSRC;
    List<String> codeList = new ArrayList<>();
    boolean isHave = false;
    TextView showInfo;

    //上下电
    private DeviceControlSpd deviceControlSpd;
    MyThread readThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showInfo = findViewById(R.id.showInfo);
        //初始化声音池
        if (Build.VERSION.SDK_INT >= 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            // 传入音频数量
            builder.setMaxStreams(1);
            // AudioAttributes是一个封装音频各种属性的方法
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            // 设置音频流的合适的属性
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
            // 加载一个AudioAttributes
            builder.setAudioAttributes(attrBuilder.build());
            sp = builder.build();
            mapSRC = new HashMap<>();
            mapSRC.put(0, sp.load(getApplicationContext(), R.raw.di, 0));
//            mapSRC.put(xiaofeiSuccse, sp.load(context, R.raw.xiaofeichenggong, 0));

        }
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        Objects.requireNonNull(am).setStreamVolume(2, 35, 0);

        //初始化新大陆解码摄像头
        serialPortSpd = new SerialPortSpd();
        try {
            //修改实际名称和波特率
            serialPortSpd.OpenSerial("/dev/ttyMT0", 9600);
            fd = serialPortSpd.getFd();
            deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.NEW_MAIN, 46, 47);
            deviceControlSpd.PowerOnDevice();

            handler.postDelayed(() -> {
                try {
                    deviceControlSpd.newSetGpioOn(19);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                isCodeCamerOpen = true;
                Log.w("codecameraopen", "二维码扫描头初始化成功");
            }, 1000);


        } catch (IOException e) {
            Log.w("codecameraopen", "二维码扫描头初始化失败");
            e.printStackTrace();
        }
        SystemProperties.set("12", "sd");
    }

    @Override
    protected void onDestroy() {
        if (serialPortSpd != null) {
            serialPortSpd.CloseSerial(fd);
        }
        try {
            deviceControlSpd.newSetGpioOff(19);
            deviceControlSpd.PowerOffDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (readThread != null) {
            readThread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        readThread = new MyThread();
        readThread.start();

    }

    class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
            //等待接收数据
            while (isCodeCamerOpen) {
                try {
                    //byte[] info = serialPortSpd.ReadSerial(fd, 10000, true);
                    byte[] info = serialPortSpd.ReadSerial(fd, 1024);
                    if (info != null) {
                        String decodeData = new String(info, "utf8");
                        /*for (String s : codeList) {
                            if (decodeData.equals(s)) {
                                isHave = true;
                                break;
                            }
                        }
                        if(isHave)
                        {
                            isHave = false;
                            continue;
                        }
                        if (codeList.size() > 30) {
                            codeList.remove(codeList.get(0));
                            codeList.add(decodeData);
                        } else {
                            codeList.add(decodeData);
                        }*/
                        /*String result = DataUtils
                                .byteArrayToString(info);*/
                        Message message = new Message();
                        message.what = -1;
                        message.obj = decodeData;
                        handler.sendMessage(message);
                        sp.play(mapSRC.get(0),//播放的声音资源
                                1f,//左声道，范围为0--1.0
                                1f,//右声道，范围为0--1.0
                                1, //优先级，0为最低优先级
                                0,//循环次数,0为不循环
                                1);//播放速率，1为正常速率
                        /*switch(decodeData.substring(0,2))
                        {
                            case "TX":
                                Log.i("serialInfo", decodeData);
                                sp.play(mapSRC.get(0),//播放的声音资源
                                        1f,//左声道，范围为0--1.0
                                        1f,//右声道，范围为0--1.0
                                        1, //优先级，0为最低优先级
                                        0,//循环次数,0为不循环
                                        1);//播放速率，1为正常速率
                                break;
                            default:
                                Log.i("serialInfo", DataUtils
                                        .byteArrayToString(info));
                                showInfo.setText(DataUtils
                                        .byteArrayToString(info));
                                sp.play(mapSRC.get(0),//播放的声音资源
                                        1f,//左声道，范围为0--1.0
                                        1f,//右声道，范围为0--1.0
                                        1, //优先级，0为最低优先级
                                        0,//循环次数,0为不循环
                                        1);//播放速率，1为正常速率
                                break;

                        }*/

                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            showInfo.setText("");
            showInfo.setText(msg.obj.toString() + "\n");

        }
    };
}
