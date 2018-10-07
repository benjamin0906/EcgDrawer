package com.example.benjamin.ecgdrawer;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by BodnárBenjamin on 2018. 04. 17..
 * This thread is responsible for the periodical refreshing of the datas.
 * In this thread runs a counter that refreshes the datas in every tick.
 */

public class PeriodicalDataRefresherThread extends Thread {
    public Handler handler;
    public Handler mainHandler;
    private ChannelSignal EcgChannelSignals = new ChannelSignal(500);
    public UsbEcgHAL ecg;
    public volatile int RefreshedData[][];
    public CurveDrawer Ch1Drawer;
    public CurveDrawer Ch2Drawer;
    public CurveDrawer Ch3Drawer;
    public CurveDrawer Ch4Drawer;
    public CurveDrawer Ch5Drawer;

    private CountDownTimer DataRefreshTimer;

    private void StartDataRefreshTimer()
    {
        DataRefreshTimer.start();
    }
    private void StopDataRefreshTimer()
    {
        DataRefreshTimer.cancel();
    }

    @Override
    public void run()
    {
        Looper.prepare();
        handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch (msg.arg1)
                {
                    case 1: //start timer
                        DataRefreshTimer= new CountDownTimer(Long.MAX_VALUE,80) {
                            @Override
                            public void onTick(long l) {
                                ecg.Read(EcgChannelSignals);
                                Message msg = mainHandler.obtainMessage();
                                msg.arg1=1;
                                msg.obj = EcgChannelSignals;
                                mainHandler.sendMessage(msg);
                            }

                            @Override
                            public void onFinish() {
                                this.start();
                            }
                        }.start();
                        break;
                    case 2: //stop timer
                        StopDataRefreshTimer();
                        break;
                    case 3:
                        break;
                    case -1:
                        /* Stoping the thread */
                        Looper.myLooper().quit();
                        break;
                }

            }


        };
        Looper.loop();
    }
}
