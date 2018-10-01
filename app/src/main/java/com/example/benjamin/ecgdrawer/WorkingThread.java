package com.example.benjamin.ecgdrawer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class WorkingThread extends Thread
{
    private final int ThreadNumber = 5;
    public Handler ToWorkingThread;
    public Handler ReturnHandler;
    private Context MainContext;
    private float[] Weights;
    private float[] TestData;
    private boolean Debouncing =false;
    private float[] Phase2Prev = new float[5];
    private float[] Phase4PrevFirst = new float[10];
    private float Sum = 0;
    private float MaxValue = 0;
    private int DebounceCounter = 0;
    private HelperThread[] HelperThread = new HelperThread[ThreadNumber];
    private Message[] ToHelperThreadMsg = new Message[ThreadNumber];
    private Handler ReturnFromHelperThread;
    private boolean[] HelperThreadReady = new boolean[ThreadNumber];
    private float[] ReturnedResult = new float[ThreadNumber];
    private MessageClass MessageHelper;
    private MessageClass MessageHelper2;
    private MessageClass MessageHelper3;
    private MessageClass MessageHelper4;
    private MessageClass MessageHelper5;
    private float buffer[];
    private float bufferForThread[];
    private float[] ResultArray;
    private float[] ResultArray3;
    private float[] ResultArray4;
    private int MaxDebounce = 0;
    private int RWaveCounter = 0;

    public WorkingThread(Context c, Handler handler)
    {
        MainContext = c;
        ReturnHandler = handler;
        MessageHelper = new MessageClass();
        MessageHelper2 = new MessageClass();
        MessageHelper3 = new MessageClass();
        MessageHelper4 = new MessageClass();
        MessageHelper5 = new MessageClass();

        ResultArray = new float[500];
        ResultArray3 = new float[500];
        ResultArray4 = new float[500];
        ReturnFromHelperThread = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.arg1)
                {
                    case 1:
                        HelperThreadReady[0] = true;
                        ReturnedResult[0] = (float) msg.obj;
                        break;
                    case 2:
                        HelperThreadReady[1] = true;
                        ReturnedResult[1] = (float) msg.obj;
                        break;
                    case 3:
                        HelperThreadReady[2] = true;
                        ReturnedResult[2] = (float) msg.obj;
                        break;
                    case 4:
                        HelperThreadReady[3] = true;
                        ReturnedResult[3] = (float) msg.obj;
                        break;
                    case 5:
                        HelperThreadReady[4] = true;
                        ReturnedResult[4] = (float) msg.obj;
                        break;
                }
                return false;
            }
        });
        /*ReturnFromHelperThread = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.arg1)
                {
                    case 1:
                        HelperThreadReady[0] = true;
                        ReturnedResult[0] = (float) msg.obj;
                        break;
                    case 2:
                        HelperThreadReady[1] = true;
                        ReturnedResult[1] = (float) msg.obj;
                        break;
                    case 3:
                        HelperThreadReady[2] = true;
                        ReturnedResult[2] = (float) msg.obj;
                        break;
                    case 4:
                        HelperThreadReady[3] = true;
                        ReturnedResult[3] = (float) msg.obj;
                        break;
                    case 5:
                        HelperThreadReady[4] = true;
                        ReturnedResult[4] = (float) msg.obj;
                        break;
                }
            }
        };*/
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void run()
    {
        Looper.prepare();
        ToWorkingThread = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.arg1)
                {
                    case 0:
                        try
                        {
                            InputStream inputStream = MainContext.getAssets().open("FilterWeight.txt");
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
                            Log.d("---WorkingThread---", "Weights of the filter have been read up successful.");
                        }
                        catch (IOException e)
                        {
                            Log.d("---WorkingThread---", "An error has been occurred while reading the weights.");
                        }

                        buffer = new float[Weights.length];
                        bufferForThread = new float[Weights.length];

                        /* Read in the test data */
                        for(int looper = 0; looper<ThreadNumber; looper++)
                        {
                            HelperThread[looper] = new HelperThread(MainContext, ReturnFromHelperThread);
                            HelperThread[looper].start();
                            while(HelperThread[looper].ToHelperThread == null);
                            ToHelperThreadMsg[looper] = HelperThread[looper].ToHelperThread.obtainMessage();
                            ToHelperThreadMsg[looper].arg1 = 0;
                            HelperThread[looper].ToHelperThread.sendMessage(ToHelperThreadMsg[looper]);
                        }

                        break;
                    case 1:
                        int looper2;
                        float LocalMax = 0;
                        int GivenData;
                        TestData = (float[]) msg.obj;
                        for(looper2 = 0; looper2 < msg.arg2; looper2++)
                        {
                            ToHelperThreadMsg[0] = HelperThread[0].ToHelperThread.obtainMessage();
                            HelperThreadReady[0] =false;

                            System.arraycopy(buffer,0,bufferForThread,0,buffer.length);

                            MessageHelper.buffer = bufferForThread;
                            MessageHelper.Value = TestData[looper2];
                            looper2++;
                            GivenData=1;
                            ToHelperThreadMsg[0].obj = MessageHelper;
                            ToHelperThreadMsg[0].arg1 = 1;
                            HelperThread[0].ToHelperThread.sendMessage(ToHelperThreadMsg[0]);

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[1] = HelperThread[1].ToHelperThread.obtainMessage();
                                HelperThreadReady[1]=false;//*/

                                MessageHelper2.buffer = bufferForThread;
                                MessageHelper2.Value = TestData[looper2 - 1];
                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[1].obj = MessageHelper2;
                                ToHelperThreadMsg[1].arg1 = 2;
                                HelperThread[1].ToHelperThread.sendMessage(ToHelperThreadMsg[1]);//*/
                            }

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[2] = HelperThread[2].ToHelperThread.obtainMessage();
                                HelperThreadReady[2]=false;//*/

                                MessageHelper3.buffer = bufferForThread;
                                MessageHelper3.Value = TestData[looper2 - 2];
                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[2].obj = MessageHelper3;
                                ToHelperThreadMsg[2].arg1 = 3;
                                HelperThread[2].ToHelperThread.sendMessage(ToHelperThreadMsg[2]);//*/
                            }

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[3] = HelperThread[3].ToHelperThread.obtainMessage();
                                HelperThreadReady[3]=false;//*/

                                MessageHelper4.buffer = bufferForThread;
                                MessageHelper4.Value = TestData[looper2 - 3];
                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[3].obj = MessageHelper4;
                                ToHelperThreadMsg[3].arg1 = 4;
                                HelperThread[3].ToHelperThread.sendMessage(ToHelperThreadMsg[3]);//*/
                            }

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[4] = HelperThread[4].ToHelperThread.obtainMessage();
                                HelperThreadReady[4]=false;//*/

                                MessageHelper5.buffer = bufferForThread;
                                MessageHelper5.Value = TestData[looper2 - 4];
                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[4].obj = MessageHelper5;
                                ToHelperThreadMsg[4].arg1 = 5;
                                HelperThread[4].ToHelperThread.sendMessage(ToHelperThreadMsg[4]);//*/
                            }
                            {
                                looper2--;
                                System.arraycopy(buffer, 0, buffer, GivenData, buffer.length - GivenData);
                                for(int looper = 0; looper<GivenData; looper++) buffer[looper]=TestData[looper2-looper];

                                /* Add the remaining part of the values of thread */
                                for(int looper = 0; looper<GivenData;looper++)
                                {
                                    switch (looper)
                                    {
                                        case 0:
                                            ResultArray[looper2-GivenData+1] = 0;
                                            break;
                                        case 1:
                                            ResultArray[looper2-GivenData+2] = buffer[3] * Weights[0];
                                            break;
                                        case 2:
                                            ResultArray[looper2-GivenData+3] = buffer[3] * Weights[1];
                                            ResultArray[looper2-GivenData+3] += buffer[2] * Weights[0];
                                            break;
                                        case 3:
                                            ResultArray[looper2-GivenData+4] = buffer[3] * Weights[2];
                                            ResultArray[looper2-GivenData+4] += buffer[2] * Weights[1];
                                            ResultArray[looper2-GivenData+4] += buffer[1] * Weights[0];
                                            break;
                                        case 4:
                                            ResultArray[looper2-GivenData+5] = buffer[3] * Weights[3];
                                            ResultArray[looper2-GivenData+5] += buffer[2] * Weights[2];
                                            ResultArray[looper2-GivenData+5] += buffer[1] * Weights[1];
                                            ResultArray[looper2-GivenData+5] += buffer[0] * Weights[0];
                                            break;
                                    }
                                }

                                /* Wait for the threads */
                                int Threads=0;
                                for(int looper = 0;Threads!=GivenData;looper++)
                                {
                                    if(looper==5)
                                    {
                                        looper = 0;
                                        Threads = 0;
                                    }
                                    if(HelperThreadReady[looper]) Threads++;
                                }
                                for(int looper = 0; looper<GivenData; looper++) ResultArray[looper2-GivenData+looper+1] += ReturnedResult[looper];

                                for(int looper3=0;looper3<GivenData;looper3++)
                                {
                                    RWaveCounter++;
                                    System.arraycopy(Phase2Prev,0,Phase2Prev,1,Phase2Prev.length-1);
                                    Phase2Prev[0] = ResultArray[looper2+1-GivenData+looper3];
                                    ResultArray3[looper2+1-GivenData+looper3] =(float) Math.pow(2*Phase2Prev[0]+Phase2Prev[1]-Phase2Prev[3]-2*Phase2Prev[4],2);

                                    ResultArray4[looper2+1-GivenData+looper3] = Sum+ResultArray3[looper2+1-GivenData+looper3]-Phase4PrevFirst[9];
                                    Sum = ResultArray4[looper2+1-GivenData+looper3];
                                    System.arraycopy(Phase4PrevFirst,0,Phase4PrevFirst,1,Phase4PrevFirst.length-1);
                                    Phase4PrevFirst[0] = ResultArray3[looper2+1-GivenData+looper3];
                                    ResultArray4[looper2+1-GivenData+looper3] /= 10;//*/

                                    if(ResultArray4[looper2+1-GivenData+looper3] > MaxValue) MaxValue=ResultArray4[looper2+1-GivenData+looper3];
                                    if(Debouncing)
                                    {
                                        if(ResultArray[looper2+1-GivenData+looper3] >= LocalMax)
                                        {
                                            LocalMax = ResultArray4[looper2+1-GivenData+looper3];
                                        }
                                        if(DebounceCounter < 600) DebounceCounter++;
                                        else
                                        {
                                            MaxDebounce = 0;
                                            Debouncing = false;
                                            Message m = ReturnHandler.obtainMessage();
                                            m.obj = ResultArray4;
                                            m.arg1 = 3;
                                            m.arg2 = RWaveCounter;
                                            ReturnHandler.sendMessage(m);
                                            RWaveCounter = 0;
                                        }
                                    }
                                    else if(ResultArray4[looper2+1-GivenData+looper3] >MaxValue*0.5)
                                    {
                                        Debouncing = true;
                                        DebounceCounter = 0;
                                        LocalMax = ResultArray[looper2+1-GivenData+looper3];
                                    }
                                    else {
                                        MaxDebounce++;
                                        if(MaxDebounce > 6000)
                                        {
                                            MaxValue = 0;
                                            MaxDebounce = 0;
                                        }
                                    }
                                }//*/
                            }
                        }
                        Message m = ReturnHandler.obtainMessage();
                        m.obj = ResultArray4;
                        m.arg1 = 0;
                        m.arg1 = 2;
                        ReturnHandler.sendMessage(m);
                        break;
                    case 3:
                        Log.d("---WorkingThread---","In case 3!!");
                        break;
                }
            }
        };
        Looper.loop();
    }
}
