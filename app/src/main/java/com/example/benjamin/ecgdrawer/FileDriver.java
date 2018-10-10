package com.example.benjamin.ecgdrawer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class FileDriver
{
    public TextView t;
    private File[] SavedFiles;
    private File WorkingFile;
    private FileWriter WorkingFileWriter;
    private FileReader WorkingFileReader;
    private FileOutputStream WorkingOutputStream;
    private FileInputStream WorkingInputStream;
    private Context MainContext;
    ArrayList<File> FilesInFolder2;
    private final String ConstFileID="Ecg Data\n";
    private float[] DataBuffer;
    private int DataBufferCounter;
    public FileDriver(Context c,int BufferSize,TextView textView)
    {
        MainContext=c;
        t=textView;
        DataBuffer = new float[BufferSize];
        DataBufferCounter=0;
        FilesInFolder2 = new ArrayList<File>();
    }
    public int Open()
    {
        int ret=0;
        WorkingFile = new File(MainContext.getExternalFilesDir(null), Calendar.getInstance().getTime().toString()+".txt");
        t.setText(WorkingFile.getAbsolutePath());
        try
        {
            WorkingFile.createNewFile();
            WorkingFileWriter = new FileWriter(WorkingFile,true);
            WorkingFileReader = new FileReader(WorkingFile);
            WorkingOutputStream = new FileOutputStream(WorkingFile,true);
            WorkingInputStream = new FileInputStream(WorkingFile);
            WorkingFileWriter.write(ConstFileID.toCharArray());
        }
        catch (IOException e) {
            ret = -1;
        }
        return ret;
    }
    public int Open(String Path)
    {
        int ret=0;
        WorkingFile = new File(MainContext.getExternalFilesDir(null),Path);
        try
        {
            WorkingFileWriter = new FileWriter(WorkingFile,true);
            WorkingFileReader = new FileReader(WorkingFile);
            WorkingOutputStream = new FileOutputStream(WorkingFile,true);
            WorkingInputStream = new FileInputStream(WorkingFile);
        }
        catch (IOException e)
        {
            ret = -1;
        }
        return ret;
    }
    public int Close()
    {
        int ret=0;
        try
        {
            for(int looper=0;looper<DataBufferCounter;looper++)
            {
                String temp = Integer.toHexString(Float.floatToIntBits(DataBuffer[looper]))+"\n";
                if(temp.length() < 9)
                {
                    while (9>temp.length())
                    {
                        temp = "0"+temp;
                    }
                }
                WorkingFileWriter.write(temp);
                DataBuffer[looper]=0;
            }
            DataBufferCounter=0;
            WorkingFileWriter.flush();
            WorkingFileWriter.close();
            WorkingFileReader.close();
            WorkingInputStream.close();
            WorkingOutputStream.close();
        }
        catch (IOException e)
        {
            ret = -1;
        }
        return ret;
    }
    public int Write(float[] Data, int Size)
    {
        int ret=0;
        //try
        {
            for(int looper = 0; looper < Size; looper++)
            {
                //ret = (Float.floatToIntBits(Data[looper]));
                //WorkingFileWriter.write(Integer.toHexString(Float.floatToIntBits(Data[looper]))+"\n");
                DataBuffer[DataBufferCounter]=Data[looper];
                DataBufferCounter++;
                //t.setText(Integer.toString(DataBufferCounter));
            }
        }
        /*catch (IOException e)
        {
            ret =-1;
        }*/
        return ret;
    }
    public File[] GetFiles()
    {
        File[] ret = new File[FilesInFolder2.size()];
        ret = FilesInFolder2.toArray(ret);
        return ret;
    }
    public int RefreshFileList()
    {
        int looper;
        int ret=0;
        WorkingFile = new File(MainContext.getExternalFilesDir(null).getAbsolutePath());
        File[] TemporaryFileList = WorkingFile.listFiles();
        char[] temp = new char[ConstFileID.length()];
        FilesInFolder2.clear();
        File FileTester;
        for(looper=0;looper<TemporaryFileList.length;looper++)
        {
            FileTester = new File(MainContext.getExternalFilesDir(null),TemporaryFileList[looper].getName());
            try
            {
                WorkingFileReader = new FileReader(FileTester);
                WorkingFileReader.read(temp);
                WorkingFileReader.close();
                if(Arrays.equals(temp,ConstFileID.toCharArray()))
                {
                    FilesInFolder2.add(FileTester);
                }
            }
            catch (IOException e)
            {
                ret--;
            }
        }
        if(FilesInFolder2.size() > 0) ret = FilesInFolder2.size();
        return ret;
    }
    public ChannelSignal Read()
    {
        byte[] a= new byte[(int)WorkingFile.length()];
        ChannelSignal b;
        try
        {
            WorkingInputStream.read(a);
            b= new ChannelSignal((int)WorkingFile.length());
            char[] temp = new char[8];
            for(int looper2=0;looper2<(a.length-ConstFileID.length())/9;looper2++)
            {
                for (int looper = 0; looper <8; looper++)
                {
                    //Temporay += a[looper + looper2*4]<<(8*(3-looper));
                    temp[looper] = (char)a[looper+ConstFileID.length() + looper2*9];
                }
                String asd = String.copyValueOf(temp);
                Long asd2 = Long.parseLong(asd,16);
                b.Channel1Data[looper2] = (Float.intBitsToFloat(asd2.intValue()));
                b.Channel1Size++;
            }
        }
        catch (IOException e)
        {
            b= null;
        }
        return b;
    }
}
