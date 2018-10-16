package com.example.benjamin.ecgdrawer;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class FileDriver
{
    private File WorkingFile;
    private FileWriter WorkingFileWriter;
    private FileReader WorkingFileReader;
    private FileOutputStream WorkingOutputStream;
    private FileInputStream WorkingInputStream;
    private Context MainContext;
    private ArrayList<File> FilesInFolder2;
    private final String ConstFileID="Ecg Data\n";
    private ChannelSignal DataBuffer;
    public FileDriver(Context c, int BufferSize)
    {
        MainContext=c;
        DataBuffer = new ChannelSignal(BufferSize);
        FilesInFolder2 = new ArrayList<File>();
    }
    public int Open()
    {
        int ret=0;
        WorkingFile = new File(MainContext.getExternalFilesDir(null), Calendar.getInstance().getTime().toString()+".txt");
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
    private String FloatTo8DigitHex(float data)
    {
        String ret = Integer.toHexString(Float.floatToIntBits(data));
        if(ret.length() < 8)
        {
            while (8>ret.length())
            {
                ret = "0"+ret;
            }
        }
        return ret;
    }
    public int Close()
    {
        int ret=0;
        int looper1=0;
        int looper2=0;
        int looper3=0;
        int looper4=0;
        int looper5=0;
        boolean ready = false;
        try
        {
            while(!ready)
            {
                if(looper1<DataBuffer.Channel1Size)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel1Data[looper1])+"\t");
                    looper1++;
                }
                else if(!ready)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel1Data[(looper1-1<0)?0:(looper1-1)])+"\t");
                }
                if(looper2<DataBuffer.Channel2Size)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel2Data[looper2])+"\t");
                    looper2++;
                }
                else if(!ready)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel2Data[(looper2-1<0)?0:(looper2-1)])+"\t");
                }
                if(looper3<DataBuffer.Channel3Size)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel3Data[looper3])+"\t");
                    looper3++;
                }
                else if(!ready)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel3Data[(looper3-1<0)?0:(looper3-1)])+"\t");
                }
                if(looper4<DataBuffer.Channel4Size)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel4Data[looper4])+"\t");
                    looper4++;
                }
                else if(!ready)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel4Data[(looper4-1<0)?0:(looper4-1)])+"\t");
                }
                if(looper5<DataBuffer.Channel5Size)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel5Data[looper5])+"\n");
                    looper5++;
                }
                else if(!ready)
                {
                    WorkingFileWriter.write(FloatTo8DigitHex(DataBuffer.Channel5Data[(looper5-1<0)?0:(looper5-1)])+"\n");
                }
                if((looper1>=DataBuffer.Channel1Size)||
                        (looper2>=DataBuffer.Channel2Size)||
                        (looper3>=DataBuffer.Channel3Size)||
                        (looper4>=DataBuffer.Channel4Size)||
                        (looper5>=DataBuffer.Channel5Size))
                {
                    ready=true;
                }
            }
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
    public int Write(ChannelSignal data)
    {
        int ret=0;
        System.arraycopy(data.Channel1Data,0,DataBuffer.Channel1Data,DataBuffer.Channel1Size,data.Channel1Size);
        System.arraycopy(data.Channel2Data,0,DataBuffer.Channel2Data,DataBuffer.Channel2Size,data.Channel2Size);
        System.arraycopy(data.Channel3Data,0,DataBuffer.Channel3Data,DataBuffer.Channel3Size,data.Channel3Size);
        System.arraycopy(data.Channel4Data,0,DataBuffer.Channel4Data,DataBuffer.Channel4Size,data.Channel4Size);
        System.arraycopy(data.Channel5Data,0,DataBuffer.Channel5Data,DataBuffer.Channel5Size,data.Channel5Size);
        DataBuffer.Channel1Size+=data.Channel1Size;
        DataBuffer.Channel2Size+=data.Channel2Size;
        DataBuffer.Channel3Size+=data.Channel3Size;
        DataBuffer.Channel4Size+=data.Channel4Size;
        DataBuffer.Channel5Size+=data.Channel5Size;
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
                switch (looper2%5)
                {
                    case 0:
                        b.Channel1Data[b.Channel1Size++] = (Float.intBitsToFloat(asd2.intValue()));
                        break;
                    case 1:
                        b.Channel2Data[b.Channel2Size++] = (Float.intBitsToFloat(asd2.intValue()));
                        break;
                    case 2:
                        b.Channel3Data[b.Channel3Size++] = (Float.intBitsToFloat(asd2.intValue()));
                        break;
                    case 3:
                        b.Channel4Data[b.Channel4Size++] = (Float.intBitsToFloat(asd2.intValue()));
                        break;
                    case 4:
                        b.Channel5Data[b.Channel5Size++] = (Float.intBitsToFloat(asd2.intValue()));
                        break;
                }

            }
        }
        catch (IOException e)
        {
            b= null;
        }
        return b;
    }
}
