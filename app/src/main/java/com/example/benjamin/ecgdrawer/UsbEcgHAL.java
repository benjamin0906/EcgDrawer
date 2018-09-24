package com.example.benjamin.ecgdrawer;

/*
 * Created by BodnárBenjamin on 2018. 04. 05..
 * This object does the whole communicaton between the ECG and the android device.
 * There is also implemented a thread on that a counter timer works, that read for new data periodically.
 */

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.FileHandler;

public class UsbEcgHAL extends AppCompatActivity {
    private Context context;
    private Handler ReturnHandler;

    private int VendorID;
    private int ProductID;
    byte StartByte = 0x53;
    byte AskArray[] = new byte[4];
    byte AskCommand = 0x01;
    private PeriodicalDataRefresherThread Thread;


    private PendingIntent       mPermissionIntent;
    private UsbManager          manager;
    private UsbDevice           device                =   null;
    private UsbInterface        ControlInterface   =   null;
    private UsbInterface        DataInterface      =   null;
    private UsbEndpoint         ControlEndpoint     =   null;
    private UsbEndpoint         WriteEndpoint       =   null;
    private UsbEndpoint         ReadEndpoint        =   null;
    private UsbDeviceConnection connection  =   null;

    private TextView t;

    public volatile  int[][] RefreshedData;
    public TextView t2;
    public CurveDrawer Ch1Drawer;
    public CurveDrawer Ch2Drawer;
    public CurveDrawer Ch3Drawer;
    public CurveDrawer Ch4Drawer;
    public CurveDrawer Ch5Drawer;
    private boolean Saving;
    private FileDriver FileHandler;

    private int PrevAddress=0;
    private byte Prevs[] = new byte[12];

    private WorkingThread RWaveDetectorThread;
    private int PrevRawData = 0;
private boolean asd = false;
    public UsbEcgHAL(Context c, String s, int VID, int PID, FileDriver FH)
    {
        context=c;
        manager  = (UsbManager) context.getSystemService(USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(s), 0);
        VendorID = VID;
        ProductID = PID;
        AskArray[0]=StartByte;
        AskArray[1]=AskCommand;
        RefreshedData = new int[5][2200];
        FileHandler = FH;
        ;


        ReturnHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.arg1)
                {
                    case 1:
                        ChannelSignal Datas = (ChannelSignal) msg.obj;
                        float temp[] = new float[500];
                        FileHandler.Write(Datas.Channel1Data,Datas.Channel1Size);
                        System.arraycopy(Datas.Channel2Data,0, temp,0,Datas.Channel2Size);
                        msg = RWaveDetectorThread.ToWorkingThread.obtainMessage();
                        msg.arg2=Datas.Channel2Size;
                        msg.arg1=1;
                        msg.obj=temp;
                        RWaveDetectorThread.ToWorkingThread.sendMessage(msg);
                        break;
                    case 2:
                        break;
                    case 3:
                        t.setText(Boolean.toString(asd));
                        asd = !asd;
                        break;
                }

                //FileHandler.Write(Datas.Channel1Data,Datas.Channel1Size);
                //FileHandler.TEST_Write(Datas.TEST_Date,Datas.TEST_DATA_Size);
                //Ch1Drawer.DrawDatas(Datas.Channel1Data,Datas.Channel1Size);
                //Ch2Drawer.DrawDatas(Datas.Channel2Data,Datas.Channel2Size);
                /*Ch3Drawer.DrawDatas(Datas.Channel3Data,Datas.Channel3Size);
                Ch4Drawer.DrawDatas(Datas.Channel4Data,Datas.Channel4Size);
                Ch5Drawer.DrawDatas(Datas.Channel5Data,Datas.Channel5Size);*/
            }
        };

        RWaveDetectorThread = new WorkingThread(context,ReturnHandler);
        RWaveDetectorThread.start();
        while(RWaveDetectorThread.ToWorkingThread == null);
        Message msg = RWaveDetectorThread.ToWorkingThread.obtainMessage();
        msg.arg1=0;
        RWaveDetectorThread.ToWorkingThread.sendMessage(msg);
    }
    public boolean Initialize()
    {
        //t.setText("1 ");
        boolean ret=false;
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if(1 < deviceList.values().size())
        {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,"0")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    //.setLargeIcon(BitmapFactory.decodeResource(getResources(), com.example.usbecgcom.R.drawable.my_progras_bar_babyblue))
                    .setContentTitle("More than 1 device")
                    .setContentText("There is more than 1 device is connected. This might be dangerous for the patient");
            notificationBuilder.setDefaults(
                    Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(1, notificationBuilder.build());
        }

        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        boolean CorrectDevice=false;
        while(deviceIterator.hasNext() && !CorrectDevice)
        {
            //t.append("1.1 ");
            device = deviceIterator.next();
            manager.requestPermission(device, mPermissionIntent);
            while (!manager.hasPermission(device));
            if(device.getVendorId() == VendorID)
            {
                //t.append("1.2 ");
                if(device.getProductId() == ProductID)
                {
                    //t.append("1.3 ");
                    //t.setText(Integer.toString(device.getInterfaceCount()));
                    for(int looper=0; looper < device.getInterfaceCount(); looper++)
                    {
                        //t.append("2 ");
                        UsbInterface TemporaryInterface = device.getInterface(looper);
                        switch (TemporaryInterface.getEndpointCount())
                        {
                            case 1:

                                ControlInterface = TemporaryInterface;
                                ControlEndpoint = ControlInterface.getEndpoint(0);
                                break;
                            case 2:
                                DataInterface = TemporaryInterface;
                                UsbEndpoint TemporaryEndpoint = DataInterface.getEndpoint(0);
                                if(TemporaryEndpoint.getDirection() == UsbConstants.USB_DIR_IN)
                                {
                                    ReadEndpoint = TemporaryEndpoint;
                                    WriteEndpoint = DataInterface.getEndpoint(1);
                                }
                                else
                                {
                                    WriteEndpoint = TemporaryEndpoint;
                                    ReadEndpoint = DataInterface.getEndpoint(1);
                                }
                                break;
                        }
                    }
                }
            }
            if(WriteEndpoint != null && ReadEndpoint != null)
            {
                CorrectDevice = true;
                //t.setText("3 ");
            }
        }
        if(WriteEndpoint != null && ReadEndpoint != null)
        {
            //t.append("jó");
            connection = manager.openDevice(device);
            connection.claimInterface(ControlInterface,true);
            connection.claimInterface(DataInterface,true);
            //t.setText("4 ");
            //thread =new MyThread2();
            /*thread =new MyThread(InputBuffer);
            thread.mainHandler = handler;
            thread.start();
            while (thread.handler == null);*/

            ret=true;
        }
        return ret;
    }
    public void setTextView(TextView t)
    {
        this.t=t;
    }

    private static final float ScaleFactor = (float)4*(float)1.8/(float)1.4/(float)16777216;
    private float EcgDataToFloat(int data)
    {
        int sData;
        if(0 != (data&0x800000)) sData=data|0xff000000;
        else sData=data;
        return (float) sData*ScaleFactor;
    }

    public void TEST_Read(ChannelSignal Data)
    {
        int BytesWithBulk=0;
        byte temp2[] = new byte[64];
        byte TEST_address=0x11;
        int TEST_value = 0;
        int TEST_Packet_Looper = 0;
        {
            boolean Header=false;
            boolean StartSign=false;
            boolean DSizeH=false;
            int CommandSign=0;
            int DSize=0;
            int GlobalCounter11=0;
            int GlobalCounter12=0;
            int GlobalCounter13=0;
            int GlobalCounter14=0;
            int GlobalCounter15=0;
            int Address=0;
            int looper=0;
            int GlobalLooper=0;
            int looper2;
            int RawEcgData=0;
            do
            {
                //BytesWithBulk=connection.bulkTransfer(ReadEndpoint,temp2,64,1);
                if(TEST_Packet_Looper == 0)
                {
                    BytesWithBulk=64;
                    temp2[0] = 0x53;
                    temp2[1] = 0x01;
                    temp2[2] = 0x08;
                    temp2[3] = 0x00;
                    for(int looper3=4;looper3<64;looper3++)
                    {
                        if(looper3%4 == 0)
                        {
                            temp2[looper3] =TEST_address;
                            TEST_address++;
                            if(TEST_address == 0x16)
                            {
                                TEST_address = 0x11;
                                TEST_value++;
                            }
                        }
                        else
                        {
                            temp2[looper3] = (byte)(TEST_value>>(8*(3-looper3%4)));
                        }
                    }
                }
                else
                {
                    if(TEST_Packet_Looper < 128)
                    {
                        for(int looper3=0;looper3<64;looper3++)
                        {
                            if(looper3%4 == 0)
                            {
                                temp2[looper3] =TEST_address;
                                TEST_address++;
                                if(TEST_address == 0x16)
                                {
                                    TEST_address = 0x11;
                                    TEST_value++;
                                }
                            }
                            else
                            {
                                temp2[looper3] = (byte)(TEST_value>>(8*(3-looper3%4)));
                            }
                        }
                    }
                    else
                    {
                        if(TEST_Packet_Looper == 128) {
                            temp2[0] = TEST_address;
                            TEST_address++;
                            temp2[1] = (byte) (TEST_value >> (8 * (3 - 1 % 4)));
                            temp2[2] = (byte) (TEST_value >> (8 * (3 - 2 % 4)));
                            temp2[3] = (byte) (TEST_value >> (8 * (3 - 3 % 4)));
                            BytesWithBulk = 4;
                        }
                        else BytesWithBulk = 0;
                    }
                }
                TEST_Packet_Looper++;
                looper2=0;
                while (BytesWithBulk > looper2)
                {
                    if (!Header)
                    {
                        if (!StartSign)
                        {
                            if (temp2[looper2] == 0x53) StartSign = true;
                        }
                        else
                        {
                            if (CommandSign == 0) CommandSign = temp2[looper2];
                            else
                            {
                                if (!DSizeH)
                                {
                                    DSize = (temp2[looper2]) << 8;
                                    DSizeH = true;
                                }
                                else
                                {
                                    DSize += temp2[looper2];
                                    Header = true;
                                }
                            }
                        }
                    }
                    else
                    {
                        if ((looper % 4) == 0)
                        {
                            switch (Address)
                            {
                                case 0x11:
                                    Data.Channel1Data[GlobalCounter11]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter11++;
                                    RawEcgData=0;
                                    break;
                                case 0x12:
                                    Data.Channel2Data[GlobalCounter12]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter12++;
                                    RawEcgData=0;
                                    break;
                                case 0x13:
                                    Data.Channel3Data[GlobalCounter13]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter13++;
                                    RawEcgData=0;
                                    break;
                                case 0x14:
                                    Data.Channel4Data[GlobalCounter14]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter14++;
                                    RawEcgData=0;
                                    break;
                                case 0x15:
                                    Data.Channel5Data[GlobalCounter15]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter15++;
                                    RawEcgData=0;
                                    break;
                            }
                            Address = temp2[looper2];
                            GlobalLooper++;
                        }
                        else RawEcgData |= (0xFF&(int)temp2[looper2]) << (8 * (3 - (looper % 4)));
                        looper++;
                    }
                    looper2++;
                }

            } while ((GlobalLooper < DSize || !Header) && BytesWithBulk>=0);
            Data.Channel1Size=GlobalCounter11;
            Data.Channel2Size=GlobalCounter12;
            Data.Channel3Size=GlobalCounter13;
            Data.Channel4Size=GlobalCounter14;
            Data.Channel5Size=GlobalCounter15;
            BytesWithBulk=0;
            while (BytesWithBulk>0) BytesWithBulk=connection.bulkTransfer(ReadEndpoint,temp2,64,2);
        }
    }

    public void Read(ChannelSignal Data)
    {
        int BytesWithBulk=0;
        byte temp2[] = new byte[64];
        if(connection != null) BytesWithBulk=connection.bulkTransfer(WriteEndpoint,AskArray,AskArray.length,1);

        if(BytesWithBulk == AskArray.length)
        {
            boolean Header=false;
            boolean StartSign=false;
            boolean DSizeH=false;
            int CommandSign=0;
            int DSize=0;
            int GlobalCounter11=0;
            int GlobalCounter12=0;
            int GlobalCounter13=0;
            int GlobalCounter14=0;
            int GlobalCounter15=0;
            Data.TEST_DATA_Size=0;

            int Address=0;
            int looper=0;
            int GlobalLooper=0;
            int looper2;
            int RawEcgData=0;
            do
            {
                BytesWithBulk=connection.bulkTransfer(ReadEndpoint,temp2,63,1);
                looper2=0;
                while (BytesWithBulk > looper2)
                {
                    if (!Header)
                    {
                        if (!StartSign)
                        {
                            if (temp2[looper2] == 0x53) StartSign = true;
                        }
                        else
                        {
                            if (CommandSign == 0) CommandSign = temp2[looper2];
                            else
                            {
                                if (!DSizeH)
                                {
                                    DSize = (temp2[looper2]) << 8;
                                    DSizeH = true;
                                }
                                else
                                {
                                    DSize += temp2[looper2];
                                    Header = true;
                                }
                            }
                        }
                    }
                    else
                    {
                        if ((looper % 4) == 0)
                        {
                            switch (Address)
                            {
                                case 0x11:
                                    Data.Channel1Data[GlobalCounter11]=EcgDataToFloat(RawEcgData);
                                    /*if(Data.Channel1Data[GlobalCounter11] >= 0.01)
                                    {
                                        Log.d("EcgDrawer", Integer.toHexString(0xff&(int)Prevs[11])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[10])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[9])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[8])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[7])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[6])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[5])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[4])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[3])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[2])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[1])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[0])+" "+Integer.toHexString(Address)+Integer.toHexString(RawEcgData));
                                    }
                                    else if(Data.Channel1Data[GlobalCounter11] <= -0.01)
                                    {
                                        Log.d("EcgDrawer", Integer.toHexString(0xff&(int)Prevs[11])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[10])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[9])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[8])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[7])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[6])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[5])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[4])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[3])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[2])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[1])+" "+
                                                Integer.toHexString(0xff&(int)Prevs[0])+" "+Integer.toHexString(Address)+Integer.toHexString(RawEcgData));
                                    }*/
                                    GlobalCounter11++;
                                    RawEcgData=0;
                                    break;
                                case 0x12:
                                    Data.Channel2Data[GlobalCounter12]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter12++;
                                    RawEcgData=0;
                                    break;
                                case 0x13:
                                    Data.Channel3Data[GlobalCounter13]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter13++;
                                    RawEcgData=0;
                                    break;
                                case 0x14:
                                    Data.Channel4Data[GlobalCounter14]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter14++;
                                    RawEcgData=0;
                                    break;
                                case 0x15:
                                    Data.Channel5Data[GlobalCounter15]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter15++;
                                    RawEcgData=0;
                                    break;
                                    default:
                                        if((0xff&Address) != 0x80)
                                        {
                                            if(!((0xff&Address) == 0x0 && GlobalLooper == 0))
                                            {
                                                looper--;
                                            }
                                        }
                                        /*if((0xff&Address) != 0x80 && (0xff&Address) != 0x0)
                                        {
                                            Log.d("EcgDrawer", "Add:"+Integer.toHexString(0xff&Address) + " Raw:"+Integer.toHexString(RawEcgData)+" "
                                                    + Integer.toHexString(0xff&Prevs[11]) + " "
                                                    + Integer.toHexString(0xff&Prevs[10]) + " "
                                                    + Integer.toHexString(0xff&Prevs[9]) + " "
                                                    + Integer.toHexString(0xff&Prevs[8]) + " "
                                                    + Integer.toHexString(0xff&Prevs[7]) + " "
                                                    + Integer.toHexString(0xff&Prevs[6]) + " "
                                                    + Integer.toHexString(0xff&Prevs[5]) + " "
                                                    + Integer.toHexString(0xff&Prevs[4]) + " "
                                                    + Integer.toHexString(0xff&Prevs[3]) + " "
                                                    + Integer.toHexString(0xff&Prevs[2]) + " "
                                                    + Integer.toHexString(0xff&Prevs[1]) + " "
                                                    + Integer.toHexString(0xff&Prevs[0]));
                                        }*/
                                        RawEcgData=0;
                                        //TODO: Ki kell találni, hogy mi legyen a RawEcgData, ha elcsúszott az adat és ide belépünk

                            }
                            PrevAddress=Address;
                            Address = temp2[looper2];
                            GlobalLooper++;
                        }
                        else
                        {
                            RawEcgData |= (0xFF&(int)temp2[looper2]) << (8 * (3 - (looper % 4)));
                            if(RawEcgData == 0x97ffff)
                            {
                                RawEcgData = PrevRawData;
                                //Log.d("EcgDrawer", "KAKSI");
                            }
                            else PrevRawData = RawEcgData;
                        }
                        System.arraycopy(Prevs,0,Prevs,1,Prevs.length-1);
                        Prevs[0]=temp2[looper2];
                        looper++;
                    }
                    looper2++;
                }

            } while ((GlobalLooper < DSize || !Header) && BytesWithBulk>=0);
            Data.Channel1Size=GlobalCounter11;
            Data.Channel2Size=GlobalCounter12;
            Data.Channel3Size=GlobalCounter13;
            Data.Channel4Size=GlobalCounter14;
            Data.Channel5Size=GlobalCounter15;
            while (BytesWithBulk>0) BytesWithBulk=connection.bulkTransfer(ReadEndpoint,temp2,63,2);
        }
        else
        {
            do
            {
                BytesWithBulk=connection.bulkTransfer(ReadEndpoint,temp2,1,10);
            }while(BytesWithBulk>0);
        }

    }
    public void close()
    {
        if(connection != null) connection.close();
        //manager.cl
    }
    public void StartDataReadThread(boolean SavingNeeded)
    {
        if(SavingNeeded)
        {
            Saving = true;
        }
        Thread = new PeriodicalDataRefresherThread();
        Thread.ecg = this;
        Thread.mainHandler=ReturnHandler;
        Thread.RefreshedData = RefreshedData;
        Thread.Ch1Drawer=this.Ch1Drawer;
        Thread.Ch2Drawer=this.Ch2Drawer;
        Thread.Ch3Drawer=this.Ch3Drawer;
        Thread.Ch4Drawer=this.Ch4Drawer;
        Thread.Ch5Drawer=this.Ch5Drawer;


        Thread.start();
        while (Thread.handler == null);
        Message msg = Thread.handler.obtainMessage();
        msg.arg1=1;
        Thread.handler.sendMessage(msg);
    }
    public void StopDataReadThread()
    {
        FileHandler.Close();
        Thread.stop(); //TODO: This is not working, if this is executed the app is crashed.
    }
}
