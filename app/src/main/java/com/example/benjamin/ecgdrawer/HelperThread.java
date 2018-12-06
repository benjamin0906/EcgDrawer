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
    private ReturnMsg ret = new ReturnMsg();

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
                        int Amount1 = msg.arg1;
                        int Amount2 = msg.arg2;
                        //msg.recycle();
                        for(int CalcLooper=Amount2;CalcLooper>0;CalcLooper--)//TODO: CalcLooper is not handled in bufferindexing!!!!!!!!!!!!!!
                        {
                            Ret[CalcLooper%2] = 0;
                            int looper;
                            for(looper = 0;looper<(Weights.length-Amount1*2-CalcLooper-1);looper++)
                            {
                                Ret[CalcLooper%2] += buffer[looper]*Weights[looper+Amount1*2-CalcLooper+1];
                            }
                        }
                        ret.msg=ToHelperThread.obtainMessage();
                        ret.data=Ret;
                        Message msgBack3 = ReturnHandler.obtainMessage();
                        msgBack3.arg1=Amount1;
                        msgBack3.arg2=Amount2;
                        //msgBack3.obj = Ret;
                        msgBack3.obj = ret;
                        ReturnHandler.sendMessage(msgBack3);
                }
                return false;
            }
        });

        Looper.loop();
    }
}
