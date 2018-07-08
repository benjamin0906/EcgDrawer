package com.example.benjamin.ecgdrawer;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static Button button;
    private Button button2;
    private static TextView textView;
    private TextView textView2;
    private TextView textView3;
    public static TextView textView4;

    ChannelDatas Datas;

    private static final String s = "com.example.bodnrbenjamin.ecg1";
    private final float[] sinus = new float[100];
    private boolean isConnected=false;
    private UsbEcgHAL ecg;
    public int[][] RefreshedData;
    public CurveDrawer Ch1Drawer;
    public CurveDrawer Ch2Drawer;
    public CurveDrawer Ch3Drawer;
    public CurveDrawer Ch4Drawer;
    public CurveDrawer Ch5Drawer;

    DrawView DataCanvas1;
    DrawView DataCanvas2;
    DrawView DataCanvas3;
    DrawView DataCanvas4;
    DrawView DataCanvas5;


    private final CircularBuffer CBuffer=new CircularBuffer(8);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        button2 = findViewById(R.id.button2);

        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        textView4 = findViewById(R.id.textView4);

        DataCanvas1 = (DrawView) findViewById(R.id.drawview);
        DataCanvas1.setBackgroundColor(Color.GREEN);
        DataCanvas2 = (DrawView) findViewById(R.id.drawview2);
        DataCanvas2.setBackgroundColor(Color.GREEN);
        DataCanvas3 = (DrawView) findViewById(R.id.drawview3);
        DataCanvas3.setBackgroundColor(Color.GREEN);
        DataCanvas4 = (DrawView) findViewById(R.id.drawview4);
        DataCanvas4.setBackgroundColor(Color.GREEN);
        DataCanvas5 = (DrawView) findViewById(R.id.drawview5);
        DataCanvas5.setBackgroundColor(Color.GREEN);

        RefreshedData = new int[5][2200];


        DisplayMetrics dm=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        DataCanvas1.getLayoutParams().width = dm.widthPixels*5/6;
        DataCanvas1.requestLayout();
        DataCanvas2.getLayoutParams().width = dm.widthPixels*5/6;
        DataCanvas2.requestLayout();
        DataCanvas3.getLayoutParams().width = dm.widthPixels*5/6;
        DataCanvas3.requestLayout();
        DataCanvas4.getLayoutParams().width = dm.widthPixels*5/6;
        DataCanvas4.requestLayout();
        DataCanvas5.getLayoutParams().width = dm.widthPixels*5/6;
        DataCanvas5.requestLayout();


        int mul=30;
        for(int looper=0;looper<sinus.length;looper++)
        {
            sinus[looper] = (float) Math.sin(Math.PI /(sinus.length/2)*looper)*mul+mul+10;
        }

        Ch1Drawer = new CurveDrawer(textView4,DataCanvas1);
        Ch2Drawer = new CurveDrawer(textView4,DataCanvas2);
        Ch3Drawer = new CurveDrawer(textView4,DataCanvas3);
        Ch4Drawer = new CurveDrawer(textView4,DataCanvas4);
        Ch5Drawer = new CurveDrawer(textView4,DataCanvas5);
        ecg=new UsbEcgHAL(this,s,0x2405,0xB); //TODO:original
        //ecg=new UsbEcgHAL(this,s,0x0461,0x0033);
        ecg.setTextView(textView2);
        ecg.t2 = textView4;
        ecg.Ch1Drawer=Ch1Drawer;
        ecg.Ch2Drawer=Ch2Drawer;
        ecg.Ch3Drawer=Ch3Drawer;
        ecg.Ch4Drawer=Ch4Drawer;
        ecg.Ch5Drawer=Ch5Drawer;
    }
    /*
    private int HexStringByteArrayToInt(byte array[])
    {
        int ret=0;
        int temp=0;
        for(int looper = 0; looper < array.length; looper++)
        {
            if((array[looper] >= 0x30) && (array[looper] <=0x39))
            {
                temp=array[looper]-0x30;
            }
            else if((array[looper] >= 0x41) && (array[looper] <=0x46))
            {
                temp=array[looper]-55;
            }
            else if((array[looper] >= 0x61) && (array[looper] <=0x66))
            {
                temp=array[looper]-87;
            }
            ret+=temp<<(looper*4);
            //ret+=temp;
        }
        return ret;
    }
    private int HexCharToInt(byte data)
    {
        int ret=0;
        if(data >= 0x30 && data <=0x39)
        {
            ret=data-0x30;
        }
        else if(data >= 0x41 && data <=0x46)
        {
            ret=data-55;
        }
        else if(data >= 0x61 && data <=0x66)
        {
            ret=data-87;
        }
        return ret;
    }*/


    public void button2OnClick(View v)
    {
        //mt = new MyThread();
    }
    public void buttonOnClick(View v)
    {
        if(!isConnected)
        {
            isConnected=ecg.Initialize();//TODO
            if(isConnected)
            {
                button.setText("DISCONNECT");
                //StartDataRefreshTimer();//TODO
            }
        }
        else
        {
            ecg.StartDataReadThread();
            //ChannelDatas datas = new ChannelDatas(5000);
            //ecg.Read(datas);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int ret;
        switch (resultCode)
        {
            case Activity.RESULT_OK:
                ret=data.getIntExtra("DriverOpenedStatus",-1);
                if(ret == 0) isConnected=true;
                else isConnected=false;
                break;
            case Activity.RESULT_CANCELED:
                isConnected=false;
                break;
            case Activity.RESULT_FIRST_USER:
                ret=data.getIntExtra("DriverOpenedStatus",-1);
                if(ret == 0) isConnected=true;
                else isConnected=false;
                break;
        }
        if(isConnected)
        {
            String button1DisconnectText = "Disconnect";
            button.setText(button1DisconnectText);
        }
        else
        {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainActivity.this,"2")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentTitle("Not connected to port MAIN")
                    .setContentText("You are not connected to any port");
            notificationBuilder.setDefaults(
                    Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
            notificationManager.notify(1, notificationBuilder.build());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ecg.close();
    }
}


