package com.example.benjamin.ecgdrawer;

/**
 * Created by BodnárBenjamin on 2018. 05. 02..
 */

public class ChannelSignal {
    public float Channel1Data[];
    public float Channel2Data[];
    public float Channel3Data[];
    public float Channel4Data[];
    public float Channel5Data[];

    public int Channel1Size;
    public int Channel2Size;
    public int Channel3Size;
    public int Channel4Size;
    public int Channel5Size;

    public int TEST_Date[];
    public int TEST_DATA_Size = 0;

    public ChannelSignal(int size)
    {
        Channel1Data = new float[size];
        Channel2Data = new float[size];
        Channel3Data = new float[size];
        Channel4Data = new float[size];
        Channel5Data = new float[size];

        Channel1Size = 0;
        Channel2Size = 0;
        Channel3Size = 0;
        Channel4Size = 0;
        Channel5Size = 0;

        TEST_Date = new int[6*size];
        TEST_DATA_Size = 0;
    }
}
