package com.ypc.handshakewear;

import android.util.Log;

import java.util.List;

/**
 * Created by yangpc on 2017/7/12.
 */
public class DataAlign {
    private List<Float> s1;
    private List<Float> s2;
    private static final int RANGE=50;
    private float maxCor=0;
    public DataAlign(List<Float> alice,List<Float> bobo){
        s1=alice;
        s2=bobo;
    }
    //时域对齐
    public int findAlignIndex(){
        float maxCorrelation=0;
        int maxStart=0,start,end;
        int E=s1.size();
        for(int i=0;i<RANGE;i++){
            start=i;
            end=E+i;
            if(end>s2.size())
                break;
            float cvdata=Utils.cross_correlation(s1,s2.subList(start,end));
            if(cvdata>maxCorrelation){
                maxCorrelation=cvdata;
                maxStart=start;
            }
        }
        maxCor=maxCorrelation;
        //Log.i("max correlation:",String.valueOf(maxCor));
        return maxStart;
    }

    public float getMaxCor(){
        return maxCor;
    }
}
