package com.example.benjamin.ecgdrawer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;

public class HelperThread extends Thread {
    public Handler ToHelperThread;
    private float[] Weights;
    public Handler ReturnHandler;
    private float[] Ret;

    public HelperThread(Context c, Handler handler)
    {
        ReturnHandler = handler;
        Ret = new float[2];
        try
        {
            InputStream inputStream = c.getAssets().open("FilterWeight.txt");
            int size = inputStream.available();
            byte[] FileContainer = new byte[size];
            inputStream.read(FileContainer);
            inputStream.close();

            int looper = 0;
            int looper2 = 0;
            int Start = 0;
            for(looper=0;looper<size;looper++) if(FileContainer[looper] == '\n') looper2++;
            Weights = new float[looper2];
            String FileString = new String(FileContainer);
            for(looper=0,looper2=0;looper2<size;looper++)
            {
                while(FileContainer[looper2] != '\n') looper2++;
                Weights[looper] = Float.parseFloat(FileString.substring(Start,looper2));
                looper2++;
                Start = looper2;
            }
            Log.d("HelperThread LOG", "Weights of the filter have been read up successful.");
        }
        catch (IOException e)
        {
            Log.d("HelperThread LOG", "An error has been occurred while reading the weights.");
        }
    }

    @Override
    public void run()
    {
        Looper.prepare();
        ToHelperThread = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                float[] buffer;
                switch (msg.arg1)
                {
                    case 0:
                        break;

                    default:
                        buffer = (float[]) msg.obj;
                        for(int CalcLooper=msg.arg2;CalcLooper>0;CalcLooper--)//TODO: CalcLooper is not handled in bufferindexing!!!!!!!!!!!!!!
                        {
                            Ret[CalcLooper%2] = 0;
                            int looper;
                            for(looper = 0;looper<(Weights.length-msg.arg1*2-CalcLooper-1);looper++)
                            {
                                Ret[CalcLooper%2] += buffer[looper]*Weights[looper+msg.arg1*2-CalcLooper+1];
                            }
                        }
                        Message msgBack3 = ReturnHandler.obtainMessage();
                        msgBack3.arg1=msg.arg1;
                        msgBack3.arg2=msg.arg2;
                        msgBack3.obj = Ret;
                        ReturnHandler.sendMessage(msgBack3);
                }
                return false;
            }
        });

        Looper.loop();
    }
}
