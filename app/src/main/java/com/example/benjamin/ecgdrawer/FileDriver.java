package com.example.benjamin.ecgdrawer;

import android.content.Context;
import android.os.Environment;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

public class FileDriver
{
    public TextView t;
    private File[] SavedFiles;
    private File WorkingFile;
    FileWriter WorkingFileWriter;
    FileReader WorkingFileReader;
    FileOutputStream WorkingOutputStream;
    FileInputStream WorkingInputStream;
    private Context MainContext;
    private File[] FilesInFolder;
    public FileDriver(Context c,TextView textView)
    {
        MainContext=c;
        t=textView;
        WorkingFile = new File(c.getExternalFilesDir(null).getAbsolutePath());
        FilesInFolder = WorkingFile.listFiles();
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
        try
        {
            for(int looper = 0; looper < Size; looper++)
            {
                WorkingFileWriter.write(Integer.toHexString(Float.floatToIntBits(Data[looper]))+"\n");
            }
        }
        catch (IOException e)
        {
            ret =-1;
        }
        return ret;
    }
    public File[] GetFiles()
    {
        return FilesInFolder;
    }
    public void RefreshFileList()
    {
        WorkingFile = new File(MainContext.getExternalFilesDir(null).getAbsolutePath());
        FilesInFolder = WorkingFile.listFiles();
    }
    public int Read(Float[] Data)
    {
        int ret=0;
        byte[] a= new byte[(int)WorkingFile.length()];
        float[] b= new float[(int)WorkingFile.length()];
        int Temporay;

        try
        {
            WorkingInputStream.read(a);
            for(int looper2=0;looper2<a.length/4;looper2++)
            {
                Temporay=0;
                for (int looper = 0; looper < 4; looper++)
                {
                    Temporay += a[looper + looper2*4]<<(8*(3-looper));
                }
                b[looper2]=Temporay;
            }
            //a[0]='5';
            //t.setText(Integer.toString(Float.floatToIntBits(b[1])));
            //t.setText(Long.toString(WorkingFile.length()));
        }
        catch (IOException e)
        {
            ret = -1;
        }
        return ret;
    }
}
