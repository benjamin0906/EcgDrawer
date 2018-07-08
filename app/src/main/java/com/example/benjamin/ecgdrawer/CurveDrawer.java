package com.example.benjamin.ecgdrawer;
/**
 * Created by BodnárBenjamin on 2018. 04. 15..
 * This object is a complete object that can draw the given raw values from the ECG.
 */

public class CurveDrawer {
    private int DrawViewHeight;
    private int DrawViewWidth;
    private int LineID;
    private int LineIterator;
    private int PrevSampleLooper=0;
    private int OrdinatePrev =0;

    private float MaxValue=(float)0;
    private float MinValue=(float)0;
    private float Mul1=0;
    private float CurrentValue=0;
    private float tVoltageMax;
    private float tVoltageMin;

    private DrawView Lines2;
    
    public CurveDrawer(DrawView d)
    {
        LineIterator = 0;
        LineID = 1;

        Lines2 = d;

        DrawViewHeight = Lines2.getLayoutParams().height;
        DrawViewWidth = Lines2.getLayoutParams().width;

        LineID = DrawViewWidth;
        Lines2.LineNumber = DrawViewWidth;
    }

/* This method implements the drawing of the received signals. */
    public void DrawDatas(float Data[],int Sizes)
    {
        Mul1 =(float) DrawViewHeight/(MaxValue-MinValue);
        int looper;
        int Ordinate;
        int SampleNumber = 1100;
        int looper2=0;

        float Rate = (float)DrawViewWidth/(float) SampleNumber;
        float VoltagePrev = Data[0];
        float PreMul;
        float PostMul;
        float Volt=VoltagePrev;

        /* This condition checks that the received signal has to be compressed or enlarged. */
        if(Rate < 1)
        {
            /* In this case the signals has to be compressed. */
            float tRate = 1/Rate;

            for(looper=0;looper < Sizes;looper++)
            {
                PrevSampleLooper++;
                if((tRate*(float)(LineIterator+1)) <= (float)PrevSampleLooper)
                {
                    PostMul = (float)PrevSampleLooper - (tRate*(float)(LineIterator+1));
                    PreMul = 1-PostMul;

                    CurrentValue += Data[looper]*PreMul;
                    CurrentValue /= tRate;
                    tVoltageMax = Math.max(tVoltageMax,CurrentValue);
                    tVoltageMin = Math.min(tVoltageMin,CurrentValue);

                    /* These lines calculates the ordinate from the averaged voltage value */
                    if(CurrentValue < MaxValue && CurrentValue > MinValue)
                    {
                        Ordinate= (int) (Mul1*((MaxValue-CurrentValue)));
                    }
                    else if(CurrentValue >= MaxValue) Ordinate = 0;
                    else Ordinate = DrawViewHeight-1;

                    Lines2.modifyLineWithoutRefresh(LineIterator, LineIterator, OrdinatePrev, LineIterator+1, Ordinate);

                    OrdinatePrev = Ordinate;
                    LineIterator++;

                    if (LineIterator == LineID)
                    {
                        LineIterator = 0;
                        MaxValue = tVoltageMax;
                        MinValue = tVoltageMin;
                        tVoltageMax = CurrentValue;
                        tVoltageMin = CurrentValue;
                        PrevSampleLooper = 0;
                        CurrentValue = 0;
                    }
                    CurrentValue = Data[looper]*PostMul;
                }
                else CurrentValue += Data[looper];
            }
        }
        else //szét kell húzni
        {
            /* In this case the signals has to be enlarged. */
            int offset;
            if(Sizes>SampleNumber)
            {
                offset=Sizes-SampleNumber;
            }
            else offset=0;

            for (looper = 1; looper < DrawViewWidth - 1 && looper2<Sizes; looper++)
            {
                if ((Rate * (float) (looper2 + 1)) < (float) (looper + 1))
                {
                    float PostM = ((float) (looper + 1)) - Rate * ((float) (looper2 + 1));//hiányzó az egészhez
                    float PreM = 1-PostM;//tizedes rész
                    CurrentValue = Volt * PreM;
                    looper2++;
                    Volt = Data[looper2+offset];//EcgDataToFloat(Data[looper2+offset]);
                    CurrentValue += Volt * PostM;
                }
                else
                {
                    CurrentValue = Volt;
                }
                tVoltageMax = Math.max(tVoltageMax,VoltagePrev);
                tVoltageMin = Math.min(tVoltageMin,VoltagePrev);
                if(CurrentValue < MaxValue && CurrentValue > MinValue)
                {
                    Ordinate= (int) (Mul1*((MaxValue-CurrentValue)));
                }
                else if(CurrentValue >= MaxValue) Ordinate = 0;
                else Ordinate = DrawViewHeight-1;

                Lines2.modifyLineWithoutRefresh(LineIterator, LineIterator - 1, OrdinatePrev, LineIterator, Ordinate);

                VoltagePrev = CurrentValue;
                OrdinatePrev = Ordinate;
                LineIterator++;
                if (LineIterator == LineID - 1) LineIterator = 0;
            }
            MaxValue = tVoltageMax;
            MinValue = tVoltageMin;
        }
        Lines2.refresh();
    }
}
