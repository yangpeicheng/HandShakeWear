package com.ypc.handshakewear;


import com.ypc.handshakewear.sensor.SensorData;

import java.util.List;

/**
 * Created by yangpc on 2017/7/13.
 */
public class InitMatrix {
    private List<SensorData> remoteList;
    private List<SensorData> localList;
    private float maxCorrelation=0;
    public InitMatrix(List<SensorData> remote,List<SensorData> local){
        remoteList=remote;
        localList=local;
    }
    public float train(){
        TrainTools trainTools=new TrainTools();
        int iterNum=60;
        float resultInitTheta=0;
        for(int i=0;i<iterNum;i++){
            float initTheta2=2*(float)Math.PI*i/iterNum;
            float correlation=trainTools.calculate_correlation(remoteList,localList,0,initTheta2);
            if(correlation>maxCorrelation){
                maxCorrelation=correlation;
                resultInitTheta=initTheta2;
            }
        }
        return resultInitTheta;
    }

    public float getMaxCorrelation(){
        return maxCorrelation;
    }
}
