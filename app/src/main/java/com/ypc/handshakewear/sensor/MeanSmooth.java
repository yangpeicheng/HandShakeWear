package com.ypc.handshakewear.sensor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yangpc on 2017/7/12.
 */

public class MeanSmooth {
    private static final int WINDOWSIZE=10;
    private boolean dataInit;
    private float [] means;
    private ArrayList<LinkedList<Float>> dataLists;

    public MeanSmooth(){
        dataLists=new ArrayList<LinkedList<Float>>();
        dataInit=false;
    }

    public float[] addSamples(float [] data){
        for(int i=0;i<data.length;i++){
            if(!dataInit){
                dataLists.add(new LinkedList<Float>());
            }
            dataLists.get(i).addLast(data[i]);
            if(dataLists.get(i).size()>WINDOWSIZE){
                dataLists.get(i).removeFirst();
            }
        }
        if(!dataInit)
            means=new float[dataLists.size()];
        dataInit= true;
        for(int i=0;i<dataLists.size();i++){
            means[i]=getMean(dataLists.get(i));
        }
        return means;
    }
    private float getMean(List<Float> data){
        float m=0;
        float count=0;
        for(int i=0;i<data.size();i++){
            m+=data.get(i).floatValue();
            count++;
        }
        if(count!=0){
            m=m/count;
        }
        return m;
    }
}
