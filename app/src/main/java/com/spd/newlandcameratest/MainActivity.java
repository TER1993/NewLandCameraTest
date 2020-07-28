package com.spd.newlandcameratest;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.serialport.DeviceControl;
import android.serialport.SerialPort;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private SerialPort mSerialPort = null;
    private static SoundPool sp; //声音池
    private static Map<Integer, Integer> mapSRC;
    List<String> codeList = new ArrayList<>();
    boolean isHave = false;
    TextView showInfo;

    //上下电
    private DeviceControl deviceControlSpd;
    ReadThread readThread;
    private String readstr = "";
    private byte[] tmpbuf = new byte[1024];
    private int readed = 0;

    private ToggleButton button1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        showInfo = findViewById(R.id.showInfo);
        button1 = findViewById(R.id.button1);
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
        mSerialPort = new SerialPort();
        try {
            //修改实际名称和波特率
            mSerialPort.OpenSerial("/dev/ttyMT1", 9600);
            deviceControlSpd = new DeviceControl(DeviceControl.PowerType.NEW_MAIN, 54);
            deviceControlSpd.PowerOnDevice();

            handler.postDelayed(() -> {
//                try {
//                    deviceControlSpd.newSetGpioOn(19);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                Log.w("codecameraopen", "二维码扫描头初始化成功");
            }, 1000);
            if (mSerialPort != null) {
                readThread = new ReadThread();
                readThread.start();
            }

        } catch (IOException e) {
            Log.w("codecameraopen", "二维码扫描头初始化失败");
            e.printStackTrace();
        }
        //SystemProperties.set("12", "sd");


        button1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                if (isChecked) {
                    deviceControlSpd.newSetGpioOn(69);
                } else {
                    deviceControlSpd.newSetGpioOff(69);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mSerialPort != null) {
            mSerialPort.CloseSerial(mSerialPort.getFd());
        }
        try {
            //deviceControlSpd.newSetGpioOff(19);
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
    }

    private String thisdecodeData = "";
    private String lastdecodeData = "";

    class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            //等待接收数据
            while (!isInterrupted()) {
                try {
                    //byte[] info = serialPortSpd.ReadSerial(fd, 10000, true);
                    try {
                        tmpbuf = mSerialPort.ReadSerial(mSerialPort.getFd(), 1024, 120);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (tmpbuf != null) {
                        readed = tmpbuf.length;
                        byte[] readbuf = new byte[readed];
                        System.arraycopy(tmpbuf, 0, readbuf, 0, readed);
//                        //处理条码末尾0x0d判断数据完整性。
//                        if (readbuf[readed - 1] == 0x0d) {
//                            System.arraycopy(tmpbuf, 0, readbuf, 0, readed - 1);
//                            if (!"".equals(lastdecodeData)) {
//                                thisdecodeData = lastdecodeData + new String(readbuf, "utf8");
//                                lastdecodeData = "";
//                            }
//                        } else {
//                            System.arraycopy(tmpbuf, 0, readbuf, 0, readed);
//                            lastdecodeData = lastdecodeData + new String(readbuf, "utf8");
//                            return;
//                        }
//
//                        String decodeData = thisdecodeData;

                        String decodeData = new String(readbuf, "utf8");
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

                } catch (SecurityException | UnsupportedEncodingException e) {
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
