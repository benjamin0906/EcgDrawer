package com.example.benjamin.ecgdrawer;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String LoadString = "Load";
    private final String ClearString = "Clear";
    private final String StartString = "Start";
    private final String StopString = "Stop";
    private final String ConnectString = "Connect";
    private final String DisconnectString = "Disconnect";
    private final String FilesString = "Files";

    private Button ConnectButtonObj;
    private Button StartButtonObj;
    private Button LoadButtonObj;
    private TextView textView3;
    public static TextView textView4;
    private CheckBox SavingCheckbox;

    private static final String s = "com.example.bodnrbenjamin.ecg1";

    private UsbEcgHAL ecg;
    public CurveDrawer Ch1Drawer;
    public CurveDrawer Ch2Drawer;
    public CurveDrawer Ch3Drawer;
    public CurveDrawer Ch4Drawer;
    public CurveDrawer Ch5Drawer;

    private ChannelSignal DataFromFile;

    DrawView DataCanvas1;
    DrawView DataCanvas2;
    DrawView DataCanvas3;
    DrawView DataCanvas4;
    DrawView DataCanvas5;
    FileDriver FileHandler;

    private boolean isConnected =   false;
    private boolean Started     =   false;
    private boolean Loaded      =   true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Set up the UI */
        ConnectButtonObj    =   findViewById(R.id.ConncectButton);
        StartButtonObj      =   findViewById(R.id.StartButton);
        LoadButtonObj       =   findViewById(R.id.LoadButton);
        SavingCheckbox      =   findViewById(R.id.checkBox);

        textView3           =   findViewById(R.id.textView3);
        textView4           =   findViewById(R.id.textView4);

        DataCanvas1         =   findViewById(R.id.drawview);

        DataCanvas2         =   findViewById(R.id.drawview2);
        DataCanvas3         =   findViewById(R.id.drawview3);
        DataCanvas4         =   findViewById(R.id.drawview4);
        DataCanvas5         =   findViewById(R.id.drawview5);
        DataCanvas1.setBackgroundColor(Color.rgb(146, 146, 251));
        DataCanvas2.setBackgroundColor(Color.rgb(146, 146, 251));
        DataCanvas3.setBackgroundColor(Color.rgb(146, 146, 251));
        DataCanvas4.setBackgroundColor(Color.rgb(146, 146, 251));
        DataCanvas5.setBackgroundColor(Color.rgb(146, 146, 251));

        /* Create a FileDriver object with a 200000 sample buffer */
        FileHandler = new FileDriver(this,200000);

        /* Set the sizes for DrawViews */
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

        /* Create the Drawer objects */
        Ch1Drawer       =   new CurveDrawer(DataCanvas1);
        Ch1Drawer.t=textView4;
        Ch2Drawer       =   new CurveDrawer(DataCanvas2);
        Ch3Drawer       =   new CurveDrawer(DataCanvas3);
        Ch4Drawer       =   new CurveDrawer(DataCanvas4);
        Ch5Drawer       =   new CurveDrawer(DataCanvas5);

        /* Create the handler of USB object */
        ecg             =   new UsbEcgHAL(this,s,0x2405,0xB, FileHandler);
        ecg.t2          =   textView4;
        ecg.setTextView(textView3);
        ecg.t2          =   textView4;
        ecg.Ch1Drawer   =   Ch1Drawer;
        ecg.Ch2Drawer   =   Ch2Drawer;
        ecg.Ch3Drawer   =   Ch3Drawer;
        ecg.Ch4Drawer   =   Ch4Drawer;
        ecg.Ch5Drawer   =   Ch5Drawer;
    }

    public void StartButtonOnClick(View v)
    {
        if(Started) /* Measure has to be stoped */
        {
            ecg.StopDataReadThread();
            //FileHandler.Open();
            /*FileHandler.Write(sinus,sinus.length);
            FileHandler.RefreshFileList();
            FileHandler.Close();*/
            StartButtonObj.setText(StartString);
            Started=false;
        }
        else /* Measure has to be started */
        {
            if(SavingCheckbox.isChecked()) FileHandler.Open();
            ecg.StartDataReadThread(SavingCheckbox.isChecked());
            StartButtonObj.setText(StopString);
            Started = true;
        }
    }
    public void ConnectButtonOnClick(View v)
    {
        Log.d("EcgDrawer", "Connect button is clicked");
        if(isConnected)
        {
            ConnectButtonObj.setText(ConnectString);
            isConnected=false;
        }
        else
        {
            isConnected=ecg.Initialize();
            if(isConnected)
            {
                ConnectButtonObj.setText(DisconnectString);
                //StartDataRefreshTimer();
            }
        }
    }

    public void LoadButtonOnClick(View v)
    {
        Log.d("EcgDrawer", "Load button is clicked");
        if(Loaded) /* Loading a file from the storage */
        {
            LoadButtonObj.setText(ClearString);

            final Dialog FileListDialog = new Dialog(MainActivity.this);
            FileListDialog.setContentView(R.layout.dialog);
            ListView FileListView = FileListDialog.findViewById(R.id.FileListView);

            /* Get the files */
            if(-1 != FileHandler.RefreshFileList())
            {
                File[] TemporaryFileContainer = FileHandler.GetFiles();
                final String[] Files = new String[TemporaryFileContainer.length];
                for (int looper = 0; looper < TemporaryFileContainer.length; looper++) Files[looper] = TemporaryFileContainer[looper].getName();

                final List<String> FileList = new ArrayList<>(Arrays.asList(Files));
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, FileList);
                FileListView.setAdapter(arrayAdapter);

                /* This function will be called when an item from the list is called. */
                FileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (0 == FileHandler.Open(Files[position])) {
                            DataFromFile = FileHandler.Read();
                            if (DataFromFile != null) {
                                /*DataFromFile = new ChannelSignal(15000);
                                for(DataFromFile.Channel1Size=0;DataFromFile.Channel1Size<15000;DataFromFile.Channel1Size++)
                                {
                                    DataFromFile.Channel1Data[DataFromFile.Channel1Size] = DataFromFile.Channel1Size%500;
                                }//*/
                                ecg.StartSavedDataReadThread(DataFromFile);
                            }
                        }
                        FileListDialog.cancel();
                    }
                });

                FileListDialog.setCancelable(true);
                FileListDialog.setTitle(FilesString);
                FileListDialog.show();
            }
        }
        else /* Clear this file */
        {
            LoadButtonObj.setText(LoadString);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        ecg.close();
    }
}


