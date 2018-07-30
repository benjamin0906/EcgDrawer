package com.example.benjamin.ecgdrawer;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.TimingLogger;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import com.example.benjamin.ecgdrawer.FileDriver;

import java.io.File;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button ConnectButtonObj;
    private Button StartButtonObj;
    private Button LoadButtonObj;
    private static TextView textView;
    private TextView textView2;
    private TextView textView3;
    public static TextView textView4;
    private ListView listView;

    ChannelSignal Datas;

    private static final String s = "com.example.bodnrbenjamin.ecg1";
    private final float[] sinus = new float[10000];

    private UsbEcgHAL ecg;
    public int[][] RefreshedData;
    public CurveDrawer Ch1Drawer;
    public CurveDrawer Ch2Drawer;
    public CurveDrawer Ch3Drawer;
    public CurveDrawer Ch4Drawer;
    public CurveDrawer Ch5Drawer;

    private float[] DatasFromFile;
    private String[] Files;

    DrawView DataCanvas1;
    DrawView DataCanvas2;
    DrawView DataCanvas3;
    DrawView DataCanvas4;
    DrawView DataCanvas5;
    FileDriver FileHandler;

    private boolean isConnected =   false;
    private boolean Started     =   false;
    private boolean Loaded      =   true;

private TimingLogger timings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectButtonObj    =   findViewById(R.id.ConncectButton);
        StartButtonObj      =   findViewById(R.id.StartButton);
        LoadButtonObj       =   findViewById(R.id.LoadButton);

        textView            =   findViewById(R.id.textView);
        textView2           =   findViewById(R.id.textView2);
        textView3           =   findViewById(R.id.textView3);
        textView4           =   findViewById(R.id.textView4);

        DataCanvas1         =   findViewById(R.id.drawview);

        DataCanvas2         =   findViewById(R.id.drawview2);
        DataCanvas3         =   findViewById(R.id.drawview3);
        DataCanvas4         =   findViewById(R.id.drawview4);
        DataCanvas5         =   findViewById(R.id.drawview5);
        DataCanvas1.setBackgroundColor(Color.GREEN);
        DataCanvas2.setBackgroundColor(Color.GREEN);
        DataCanvas3.setBackgroundColor(Color.GREEN);
        DataCanvas4.setBackgroundColor(Color.GREEN);
        DataCanvas5.setBackgroundColor(Color.GREEN);

        RefreshedData = new int[5][2200];

        FileHandler = new FileDriver(this,textView4);


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

        for(int looper=0;looper<sinus.length;looper++) sinus[looper] = (float) Math.sin(2*Math.PI /(sinus.length)*looper);

        Ch1Drawer = new CurveDrawer(DataCanvas1);
        Ch1Drawer.t=textView4;
        Ch2Drawer = new CurveDrawer(DataCanvas2);
        Ch3Drawer = new CurveDrawer(DataCanvas3);
        Ch4Drawer = new CurveDrawer(DataCanvas4);
        Ch5Drawer = new CurveDrawer(DataCanvas5);
        ecg=new UsbEcgHAL(this,s,0x2405,0xB); //TODO:original
        ecg.t2=textView4;
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


    public void StartButtonOnClick(View v)
    {
        if(Started) /* Measure has to be stoped */
        {
            ecg.StopDataReadThread();
            FileHandler.Write(sinus,sinus.length);
            FileHandler.Close();
            StartButtonObj.setText("Start");
        }
        else /* Measure has to be started */
        {
            Ch1Drawer.DrawDatas(sinus,sinus.length);
            StartButtonObj.setText("Stop");
            Started = true;
        }
    }
    public void ConnectButtonOnClick(View v)
    {
        isConnected=true;
        if(!isConnected)
        {
            isConnected=ecg.Initialize();//TODO
            if(isConnected)
            {
                ConnectButtonObj.setText("DISCONNECT");
                FileHandler.Open();
                //StartDataRefreshTimer();//TODO
            }
        }
        else
        {
            //ecg.StartDataReadThread(SaveCheckBox.isChecked());
            //ChannelSignal datas = new ChannelSignal(5000);
            //ecg.Read(datas);
            textView4.setText(Integer.toHexString(Float.floatToIntBits(sinus[50])));
            //FileHandler.Write(sinus,sinus.length);
            //FileHandler.Close();
            ConnectButtonObj.setText("Connect");
        }
    }

    public void LoadButtonOnClick(View v)
    {
        if(Loaded) /* Loading a file from the storage */
        {
            LoadButtonObj.setText("Clear");

            final Dialog FileListDialog = new Dialog(MainActivity.this);
            FileListDialog.setContentView(R.layout.dialog);
            ListView FileListView = (ListView ) FileListDialog.findViewById(R.id.FileListView);

            /* Get the files */
            File[] TemporaryFileContainer = FileHandler.GetFiles();
            final String[] Files = new String[TemporaryFileContainer.length];
            for(int looper=0; looper<TemporaryFileContainer.length; looper++) Files[looper] = TemporaryFileContainer[looper].getName();

            final List<String> fruits_list = new ArrayList<String>(Arrays.asList(Files));
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fruits_list);

            FileListView.setAdapter(arrayAdapter);

            /* This function will be called when an item from the list is called. */
            FileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //textView4.append(" "+Integer.toString(position));
                    textView3.setText(Files[position]);
                    if(0==FileHandler.Open(Files[position]))
                    {
                        Float[] a= new Float[3];
                        if(0!=FileHandler.Read(a)) textView2.setText("SZAR A FILLE");
                    }
                    FileListDialog.cancel();
                }
            });

            FileListDialog.setCancelable(true);
            FileListDialog.setTitle("Files");
            FileListDialog.show();
        }
        else /* Clear this file */
        {
            LoadButtonObj.setText("Load");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ecg.close();
    }
}


