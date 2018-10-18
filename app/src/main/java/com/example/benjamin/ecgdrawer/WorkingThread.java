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
    private float[] Phase2Prev = new float[5];
    private float[] Phase4PrevFirst = new float[10];
    private float Sum = 0;
    private float MaxValue = 0;
    private HelperThread[] HelperThread = new HelperThread[ThreadNumber];
    private Message[] ToHelperThreadMsg = new Message[ThreadNumber];
    private Handler ReturnFromHelperThread;
    private boolean[] HelperThreadReady = new boolean[ThreadNumber*2];
    private float[] ReturnedResult = new float[ThreadNumber*2];
    private float buffer[];
    private float bufferForThread[];
    private float[] ResultArray;
    private float[] ResultArray3;
    private float[] ResultArray4;
    private int RWaveCounter = 0;
    private static final int State_MaxSearch = 1;
    private static final int State_Deadtime = 2;
    private static final int State_Search = 0;
    private int State=0;
    private int MaxCheckerDebounce=0;
    private int MaxSearchDebounce = 0;
    private int DeadtimeDebounce=0;

    public WorkingThread(Context c, Handler handler)
    {
        MainContext = c;
        ReturnHandler = handler;

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
                        ReturnedResult[0] = ((float[]) msg.obj)[0];
                        if(msg.arg2 == 2)
                        {
                            ReturnedResult[1] = ((float[]) msg.obj)[1];
                            HelperThreadReady[1] = true;
                        }
                        break;
                    case 2:
                        HelperThreadReady[2] = true;
                        ReturnedResult[2] = ((float[]) msg.obj)[0];
                        if(msg.arg2 == 2)
                        {
                            ReturnedResult[3] = ((float[]) msg.obj)[1];
                            HelperThreadReady[3] = true;
                        }
                        break;
                    case 3:
                        HelperThreadReady[4] = true;
                        ReturnedResult[4] = ((float[]) msg.obj)[0];
                        if(msg.arg2 == 2)
                        {
                            ReturnedResult[5] = ((float[]) msg.obj)[1];
                            HelperThreadReady[5] = true;
                        }
                        break;
                    case 4:
                        HelperThreadReady[6] = true;
                        ReturnedResult[6] = ((float[]) msg.obj)[0];
                        if(msg.arg2 == 2)
                        {
                            ReturnedResult[7] = ((float[]) msg.obj)[1];
                            HelperThreadReady[7] = true;
                        }
                        break;
                    case 5:
                        HelperThreadReady[8] = true;
                        ReturnedResult[8] = ((float[]) msg.obj)[0];
                        if(msg.arg2 == 2)
                        {
                            ReturnedResult[9] = ((float[]) msg.obj)[1];
                            HelperThreadReady[9] = true;
                        }
                        break;
                }
                return false;
            }
        });
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

                            int looper;
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
                            HelperThread[looper].setName("HelperThread"+Integer.toString(looper));
                            HelperThread[looper].start();
                            while(HelperThread[looper].ToHelperThread == null);
                            ToHelperThreadMsg[looper] = HelperThread[looper].ToHelperThread.obtainMessage();
                            ToHelperThreadMsg[looper].arg1 = 0;
                            //ToHelperThreadMsg[looper].setTarget(HelperThread[looper].ToHelperThread);
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

                            looper2++;
                            GivenData=1;
                            ToHelperThreadMsg[0].arg2 = 1;
                            if(looper2<TestData.length)
                            {
                                HelperThreadReady[1] =false;
                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[0].arg2++;
                            }
                            ToHelperThreadMsg[0].obj = bufferForThread;
                            ToHelperThreadMsg[0].arg1 = 1;
                            HelperThread[0].ToHelperThread.sendMessage(ToHelperThreadMsg[0]);

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[1] = HelperThread[1].ToHelperThread.obtainMessage();
                                HelperThreadReady[2]=false;//*/

                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[1].arg2=1;
                                if(looper2<TestData.length)
                                {
                                    HelperThreadReady[3] =false;
                                    looper2++;
                                    GivenData++;
                                    ToHelperThreadMsg[1].arg2++;
                                }
                                ToHelperThreadMsg[1].obj = bufferForThread;
                                ToHelperThreadMsg[1].arg1 = 2;
                                HelperThread[1].ToHelperThread.sendMessage(ToHelperThreadMsg[1]);//*/
                            }

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[2] = HelperThread[2].ToHelperThread.obtainMessage();
                                HelperThreadReady[4]=false;//*/

                                GivenData++;
                                looper2++;
                                ToHelperThreadMsg[2].arg2=1;
                                if(looper2<TestData.length)
                                {
                                    HelperThreadReady[5] =false;
                                    GivenData++;
                                    looper2++;
                                    ToHelperThreadMsg[2].arg2++;
                                }
                                ToHelperThreadMsg[2].obj = bufferForThread;
                                ToHelperThreadMsg[2].arg1 = 3;
                                HelperThread[2].ToHelperThread.sendMessage(ToHelperThreadMsg[2]);//*/
                            }

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[3] = HelperThread[3].ToHelperThread.obtainMessage();
                                HelperThreadReady[6]=false;//*/

                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[3].arg2=1;
                                if(looper2<TestData.length)
                                {
                                    HelperThreadReady[7] =false;
                                    looper2++;
                                    GivenData++;
                                    ToHelperThreadMsg[3].arg2++;
                                }
                                ToHelperThreadMsg[3].obj = bufferForThread;
                                ToHelperThreadMsg[3].arg1 = 4;
                                HelperThread[3].ToHelperThread.sendMessage(ToHelperThreadMsg[3]);//*/
                            }

                            if(looper2 < TestData.length)
                            {
                                ToHelperThreadMsg[4] = HelperThread[4].ToHelperThread.obtainMessage();
                                HelperThreadReady[8]=false;//*/

                                looper2++;
                                GivenData++;
                                ToHelperThreadMsg[4].arg2=1;
                                if(looper2<TestData.length)
                                {
                                    HelperThreadReady[9] =false;
                                    looper2++;
                                    GivenData++;
                                    ToHelperThreadMsg[4].arg2++;
                                }
                                ToHelperThreadMsg[4].obj = bufferForThread;
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
                                            ResultArray[looper2-GivenData+1] = buffer[9]*Weights[0];
                                            break;
                                        case 1:
                                            ResultArray[looper2-GivenData+2] =  buffer[9] * Weights[1];
                                            ResultArray[looper2-GivenData+2] += buffer[8] * Weights[0];
                                            break;
                                        case 2:
                                            ResultArray[looper2-GivenData+3] =  buffer[9] * Weights[2];
                                            ResultArray[looper2-GivenData+3] += buffer[8] * Weights[1];
                                            ResultArray[looper2-GivenData+3] += buffer[7] * Weights[0];
                                            break;
                                        case 3:
                                            ResultArray[looper2-GivenData+4] =  buffer[9] * Weights[3];
                                            ResultArray[looper2-GivenData+4] += buffer[8] * Weights[2];
                                            ResultArray[looper2-GivenData+4] += buffer[7] * Weights[1];
                                            ResultArray[looper2-GivenData+4] += buffer[6] * Weights[0];
                                            break;
                                        case 4:
                                            ResultArray[looper2-GivenData+5] =  buffer[9] * Weights[4];
                                            ResultArray[looper2-GivenData+5] += buffer[8] * Weights[3];
                                            ResultArray[looper2-GivenData+5] += buffer[7] * Weights[2];
                                            ResultArray[looper2-GivenData+5] += buffer[6] * Weights[1];
                                            ResultArray[looper2-GivenData+5] += buffer[5] * Weights[0];
                                            break;
                                        case 5:
                                            ResultArray[looper2-GivenData+6] =  buffer[9]*Weights[5];
                                            ResultArray[looper2-GivenData+6] += buffer[8]*Weights[4];
                                            ResultArray[looper2-GivenData+6] += buffer[7]*Weights[3];
                                            ResultArray[looper2-GivenData+6] += buffer[6]*Weights[2];
                                            ResultArray[looper2-GivenData+6] += buffer[5]*Weights[1];
                                            ResultArray[looper2-GivenData+6] += buffer[4]*Weights[0];
                                            break;
                                        case 6:
                                            ResultArray[looper2-GivenData+7] =  buffer[9]*Weights[6];
                                            ResultArray[looper2-GivenData+7] += buffer[8]*Weights[5];
                                            ResultArray[looper2-GivenData+7] += buffer[7]*Weights[4];
                                            ResultArray[looper2-GivenData+7] += buffer[6]*Weights[3];
                                            ResultArray[looper2-GivenData+7] += buffer[5]*Weights[2];
                                            ResultArray[looper2-GivenData+7] += buffer[4]*Weights[1];
                                            ResultArray[looper2-GivenData+7] += buffer[3]*Weights[0];
                                            break;
                                        case 7:
                                            ResultArray[looper2-GivenData+8] =  buffer[9]*Weights[7];
                                            ResultArray[looper2-GivenData+8] += buffer[8]*Weights[6];
                                            ResultArray[looper2-GivenData+8] += buffer[7]*Weights[5];
                                            ResultArray[looper2-GivenData+8] += buffer[6]*Weights[4];
                                            ResultArray[looper2-GivenData+8] += buffer[5]*Weights[3];
                                            ResultArray[looper2-GivenData+8] += buffer[4]*Weights[2];
                                            ResultArray[looper2-GivenData+8] += buffer[3]*Weights[1];
                                            ResultArray[looper2-GivenData+8] += buffer[2]*Weights[0];
                                            break;
                                        case 8:
                                            ResultArray[looper2-GivenData+9] = buffer[9]*Weights[8];
                                            ResultArray[looper2-GivenData+9] += buffer[8]*Weights[7];
                                            ResultArray[looper2-GivenData+9] += buffer[7]*Weights[6];
                                            ResultArray[looper2-GivenData+9] += buffer[6]*Weights[5];
                                            ResultArray[looper2-GivenData+9] += buffer[5]*Weights[4];
                                            ResultArray[looper2-GivenData+9] += buffer[4]*Weights[3];
                                            ResultArray[looper2-GivenData+9] += buffer[3]*Weights[2];
                                            ResultArray[looper2-GivenData+9] += buffer[2]*Weights[1];
                                            ResultArray[looper2-GivenData+9] += buffer[1]*Weights[0];
                                            break;
                                        case 9:
                                            ResultArray[looper2-GivenData+10] = buffer[9]*Weights[9];
                                            ResultArray[looper2-GivenData+10] += buffer[8]*Weights[8];
                                            ResultArray[looper2-GivenData+10] += buffer[7]*Weights[7];
                                            ResultArray[looper2-GivenData+10] += buffer[6]*Weights[6];
                                            ResultArray[looper2-GivenData+10] += buffer[5]*Weights[5];
                                            ResultArray[looper2-GivenData+10] += buffer[4]*Weights[4];
                                            ResultArray[looper2-GivenData+10] += buffer[3]*Weights[3];
                                            ResultArray[looper2-GivenData+10] += buffer[2]*Weights[2];
                                            ResultArray[looper2-GivenData+10] += buffer[1]*Weights[1];
                                            ResultArray[looper2-GivenData+10] += buffer[0]*Weights[0];
                                            break;
                                    }
                                }

                                /* Wait for the threads */
                                int Threads=0;
                                for(int looper = 0;Threads!=GivenData;looper++)
                                {
                                    if(looper==10)
                                    {
                                        looper = 0;
                                        Threads = 0;
                                    }
                                    if(HelperThreadReady[looper]) Threads++;
                                }
                                for(int looper = 0; looper<GivenData; looper++)
                                {
                                    ResultArray[looper2-GivenData+looper+1] += ReturnedResult[looper];
                                }

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
                                    switch (State)
                                    {
                                        case State_Search:
                                            if(ResultArray4[looper2+1-GivenData+looper3] >= MaxValue*0.5)
                                            {
                                                LocalMax = ResultArray[looper2+1-GivenData+looper3];
                                                State = State_MaxSearch;
                                                MaxCheckerDebounce=0;
                                            }
                                            else
                                            {
                                                MaxCheckerDebounce++;
                                                if(MaxCheckerDebounce >= 6000)
                                                {
                                                    MaxValue=0;
                                                    MaxCheckerDebounce=0;
                                                }
                                            }
                                            break;
                                        case State_MaxSearch:
                                            MaxSearchDebounce++;
                                            if(MaxSearchDebounce<240)
                                            {
                                                if(ResultArray[looper2+1-GivenData+looper3] > LocalMax)
                                                {
                                                    LocalMax = ResultArray4[looper2+1-GivenData+looper3];
                                                }
                                            }
                                            else
                                            {
                                                MaxSearchDebounce=0;
                                                Message m = ReturnHandler.obtainMessage();
                                                m.obj = ResultArray4;
                                                m.arg1 = 3;
                                                m.arg2 = RWaveCounter;
                                                ReturnHandler.sendMessage(m);
                                                RWaveCounter = 0;
                                                State = State_Deadtime;
                                                //State = State_Search;
                                            }
                                            break;
                                        case State_Deadtime:
                                            DeadtimeDebounce++;
                                            if(DeadtimeDebounce>360)
                                            {
                                                State = State_Search;
                                                DeadtimeDebounce=0;
                                            }
                                            break;
                                    }
                                }
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
