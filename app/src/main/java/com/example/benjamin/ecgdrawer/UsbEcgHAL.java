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
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeoutException;
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
    private FileDriver FileHandler;
    private WorkingThread RWaveDetectorThread;
    byte Saved[][]= new byte[3200][63];
    int SavedIter =0;

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
        ReturnHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.arg1)
                {
                    case 1:
                        ChannelSignal Datas = (ChannelSignal) msg.obj;
                        float temp[] = new float[500];
                        //FileHandler.Write(Datas.Channel1Data,Datas.Channel1Size);
                        Ch1Drawer.DrawDatas(Datas.Channel1Data,Datas.Channel1Size);
                        Ch2Drawer.DrawDatas(Datas.Channel2Data,Datas.Channel2Size);
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
                        t.setText("HRT: "+Float.toString(60*2000/((float)msg.arg2)));
                        //asd = !asd;
                        break;
                }
                return false;
            }
        });
        /*ReturnHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                switch (msg.arg1)
                {
                    case 1:
                        ChannelSignal Datas = (ChannelSignal) msg.obj;
                        float temp[] = new float[500];
                        //FileHandler.Write(Datas.Channel1Data,Datas.Channel1Size);
                        Ch2Drawer.DrawDatas(Datas.Channel2Data,Datas.Channel2Size);
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
                        t.setText("HRT: "+Float.toString(60*2000/((float)msg.arg2)));
                        //asd = !asd;
                        break;
                }

                //FileHandler.Write(Datas.Channel1Data,Datas.Channel1Size);
                //FileHandler.TEST_Write(Datas.TEST_Date,Datas.TEST_DATA_Size);
                //Ch1Drawer.DrawDatas(Datas.Channel1Data,Datas.Channel1Size);
                //Ch2Drawer.DrawDatas(Datas.Channel2Data,Datas.Channel2Size);
                //Ch3Drawer.DrawDatas(Datas.Channel3Data,Datas.Channel3Size);
                //Ch4Drawer.DrawDatas(Datas.Channel4Data,Datas.Channel4Size);
                //Ch5Drawer.DrawDatas(Datas.Channel5Data,Datas.Channel5Size);
            }
        };*/

        RWaveDetectorThread = new WorkingThread(context,ReturnHandler);
        RWaveDetectorThread.start();
        boolean vari;
        do
        {
            vari = RWaveDetectorThread.ToWorkingThread == null;
        }while(vari);
        Message msg = RWaveDetectorThread.ToWorkingThread.obtainMessage();
        msg.arg1=0;
        RWaveDetectorThread.ToWorkingThread.sendMessage(msg);
    }
    public boolean Initialize()
    {
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
            device = deviceIterator.next();
            manager.requestPermission(device, mPermissionIntent);
            while (!manager.hasPermission(device));
            if(device.getVendorId() == VendorID)
            {
                if(device.getProductId() == ProductID)
                {
                    for(int looper=0; looper < device.getInterfaceCount(); looper++)
                    {
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
            if(WriteEndpoint != null && ReadEndpoint != null) CorrectDevice = true;
        }
        if(WriteEndpoint != null && ReadEndpoint != null)
        {
            connection = manager.openDevice(device);
            connection.claimInterface(ControlInterface,true);
            connection.claimInterface(DataInterface,true);
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

    public void Read2(ChannelSignal Data)
    {
        long StartTime = System.nanoTime();
        int BytesWithBulk = 0;
        byte temp2[] = new byte[63];
        int ReadBytes = 0;
        byte ReadData[] = new byte[90000];
        int error=0;
        int ReceivedSize=0;
        int ReservedBytes=0;
        int debounce = 0;
        long Stime1=0;
        long Stime2 = 0;

        UsbRequest WriteRequest = new UsbRequest();
        WriteRequest.initialize(connection,WriteEndpoint);
        ByteBuffer buff = ByteBuffer.wrap(AskArray);
        boolean WriteQueue = WriteRequest.queue(buff,4);//*/

        //BytesWithBulk=connection.bulkTransfer(WriteEndpoint,AskArray,AskArray.length,1);
        //UsbRequest ReadRequest =new UsbRequest();
        UsbRequest ReadRequest = connection.requestWait();
        boolean initialized = ReadRequest.initialize(connection,ReadEndpoint);
        if(!initialized) Log.d("---UsbEcgHAL---", "reading uninitiated");
        ByteBuffer ReadBuff = ByteBuffer.allocate(ReadEndpoint.getMaxPacketSize());
        byte ReceiverPacket[] = ReadBuff.array();
        int MessageSize=0;
        boolean FirstPacketArrived = false;
        do {
            boolean ReadQueue = ReadRequest.queue(ReadBuff,63);
            if(ReadQueue)
            {
                    if (ReadRequest == connection.requestWait()) {
                        ReceiverPacket = ReadBuff.array();
                        if (ReceiverPacket[0] == (byte) 0x53) {
                            if (ReceiverPacket[1] == (byte) 0x02) {
                                MessageSize = 0xff00 & ((int) ReceiverPacket[2] << 8) | 0xff & ((int) ReceiverPacket[3]);
                                FirstPacketArrived = true;
                            }
                            else
                            {
                                Log.d("---UsbEcgHAL---","ERROR 1");
                            }
                        }
                        else
                        {
                            Log.d("---UsbEcgHAL---","ERROR 1");
                        }
                    }
                    else
                    {
                        Log.d("---UsbEcgHAL---","ERROR 1");
                    }
            }
        }while (!FirstPacketArrived);

        int DataCounter=0;
        byte DataAddress=0;
        int looper=4;
        int RawData=0;
        Data.Channel1Size = 0;
        Data.Channel2Size = 0;
        Data.Channel3Size = 0;
        Data.Channel4Size = 0;
        Data.Channel5Size = 0;
        do {
            if (DataCounter % 4 == 0) {
                switch (DataAddress) {
                    case 0x11:
                        Data.Channel1Data[Data.Channel1Size++] = EcgDataToFloat(RawData);
                        break;
                }
                DataAddress = ReceiverPacket[looper];
                RawData = 0;
            } else {
                RawData |= (0xff & (int) ReceiverPacket[looper]) << (8 * (3 - (DataCounter % 4)));
            }
            looper++;
            DataCounter++;
        } while(looper <63);
        do
        {
            if(DataCounter<MessageSize*4 && looper==63)
            {
                boolean ReadQueue = ReadRequest.queue(ReadBuff,63);
                if(ReadQueue)
                {
                    if(ReadRequest == connection.requestWait())
                    {
                        ReceiverPacket = ReadBuff.array();
                        looper=0;
                    }
                }
            }
            if(looper != 63) {
                if (DataCounter % 4 == 0) {
                    switch (DataAddress) {
                        case 0x11:
                            Data.Channel1Data[Data.Channel1Size++] = EcgDataToFloat(RawData);
                            break;
                        case 0x12:
                            Data.Channel2Data[Data.Channel2Size++] = EcgDataToFloat(RawData);
                            break;
                        case 0x13:
                            Data.Channel3Data[Data.Channel3Size++] = EcgDataToFloat(RawData);
                            break;
                        case 0x14:
                            Data.Channel4Data[Data.Channel4Size++] = EcgDataToFloat(RawData);
                            break;
                        case 0x15:
                            Data.Channel5Data[Data.Channel5Size++] = EcgDataToFloat(RawData);
                            break;
                    }
                    DataAddress = ReceiverPacket[looper];
                    RawData = 0;
                } else {
                    RawData |= (0xff & (int) ReceiverPacket[looper]) << (8 * (3 - (DataCounter % 4)));
                }
                looper++;
                DataCounter++;
            }
        }while (DataCounter < MessageSize*4);
    }

    public void Read(ChannelSignal Data)
    {
        int BytesWithBulk=0;
        byte temp2[] = new byte[64];
        if(connection != null)
        {
            do {
                BytesWithBulk=connection.bulkTransfer(ReadEndpoint,temp2,63,1);
                BytesWithBulk = connection.bulkTransfer(WriteEndpoint, AskArray, AskArray.length, 5);
            }while(BytesWithBulk != AskArray.length);
        }

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
                BytesWithBulk=connection.bulkTransfer(ReadEndpoint,temp2,63,5);
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
                                    break;
                                case 0x12:
                                    Data.Channel2Data[GlobalCounter12]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter12++;
                                    break;
                                case 0x13:
                                    Data.Channel3Data[GlobalCounter13]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter13++;
                                    break;
                                case 0x14:
                                    Data.Channel4Data[GlobalCounter14]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter14++;
                                    break;
                                case 0x15:
                                    Data.Channel5Data[GlobalCounter15]=EcgDataToFloat(RawEcgData);
                                    GlobalCounter15++;
                                    break;
                                    default:
                                        if((0xff&Address) != 0x80)
                                        {
                                            if(!((0xff&Address) == 0x0 && GlobalLooper == 0))
                                            {
                                                if(looper2 < BytesWithBulk-1)
                                                {
                                                    switch (temp2[looper2+1])
                                                    {
                                                        case 0x11:
                                                            if(looper2 < BytesWithBulk -1-4) {
                                                                if (temp2[looper2 + 1 + 4] == 0x12) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            else {
                                                                //if (temp2[looper2 + 1 - 4] == 0x80) {
                                                                if (temp2[looper2 + 1 - 4] == 0x15) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            break;
                                                        case 0x12:
                                                            if(looper2 < BytesWithBulk -1-4) {
                                                                if (temp2[looper2 + 1 + 4] == 0x13) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            else {
                                                                if (temp2[looper2 + 1 - 4] == 0x11) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            break;
                                                        case 0x13:
                                                            if(looper2 < BytesWithBulk -1-4) {
                                                                if (temp2[looper2 + 1 + 4] == 0x14) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            else {
                                                                if (temp2[looper2 + 1 - 4] == 0x12) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            break;
                                                        case 0x14:
                                                            if(looper2 < BytesWithBulk -1-4) {
                                                                if (temp2[looper2 + 1 + 4] == 0x15) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            else {
                                                                if (temp2[looper2 + 1 - 4] == 0x13) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            break;
                                                        case 0x15:
                                                            if(looper2 < BytesWithBulk -1-4) {
                                                                //if (temp2[looper2 + 1 + 4] == 0x80) {
                                                                if (temp2[looper2 + 1 + 4] == 0x11) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            else {
                                                                if (temp2[looper2 + 1 - 4] == 0x14) {
                                                                    looper2++;
                                                                    looper -= 4;
                                                                } else looper--;
                                                            }
                                                            break;
                                                            default:
                                                                looper--;
                                                    }
                                                }
                                            }
                                        }
                            }
                            RawEcgData=0;
                            Address = 0xff&temp2[looper2];
                            GlobalLooper++;
                        }
                        else RawEcgData |= (0xFF&(int)temp2[looper2]) << (8 * (3 - (looper % 4)));
                        looper++;
                    }
                    looper2++;
                }
                if(SavedIter<3000)
                {
                    System.arraycopy(temp2,0,Saved[SavedIter],0,63);
                    SavedIter++;
                }

            } while ((GlobalLooper < DSize || !Header) && BytesWithBulk>=0);
            //Log.d("---UsbEcgHAL---","Time: "+Long.toString((System.currentTimeMillis()-time)));
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
            Log.d("---UsbEcgHAL---","Asking failed");
        }

    }
    public void close()
    {
        if(connection != null) connection.close();
        //manager.cl
    }
    public void StartDataReadThread(boolean SavingNeeded)
    {
        Thread = new PeriodicalDataRefresherThread();
        Thread.ecg = this;
        Thread.mainHandler=ReturnHandler;
        Thread.RefreshedData = RefreshedData;
        Thread.Ch1Drawer=this.Ch1Drawer;
        Thread.Ch2Drawer=this.Ch2Drawer;
        Thread.Ch3Drawer=this.Ch3Drawer;
        Thread.Ch4Drawer=this.Ch4Drawer;
        Thread.Ch5Drawer=this.Ch5Drawer;
        Thread.setName("PeriodicalDataRefresherThread");


        Thread.start();
        while (Thread.handler == null);
        Message msg = Thread.handler.obtainMessage();
        msg.arg1=1;
        Thread.handler.sendMessage(msg);
    }
    public void StopDataReadThread()
    {
        FileHandler.Close();
        Message msg = Thread.handler.obtainMessage();
        msg.arg1=-1;
        Thread.handler.sendMessage(msg);
        //Thread.stop(); //TODO: This is not working, if this is executed the app is crashed.
    }
}
