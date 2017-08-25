package com.ypc.handshakewear.sensor;

import android.util.Log;

import com.ypc.handshakewear.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yangpc on 2017/7/23.
 */

public class DetectShake {
    private static final float MAGNITUDE_THRESHOLD=1.0f;
    private static final float VARIANCE_THRESHOLD=0.2f;
    private static final int WINDOW_SIZE=5;
    private List<SensorData> mShakeData=new ArrayList<>();
    private List<SensorData> usefulData;
    private List<Float> mWindowData=new LinkedList<>();
    private int index=0;
    private boolean preState=true;
    private boolean currentState=true;
    private List<Integer> upIndex=new LinkedList<>();
    private List<Integer> downIndex=new LinkedList<>();
    private boolean isClose=false;
    private long lastTimestamp=0;
    public DetectShake(){
    }
    public void handleSensorData(SensorData sensorData){
        if(isClose)
            return;
        if(lastTimestamp==sensorData.getTimestamp()){
            return;
        }
        else {
            lastTimestamp=sensorData.getTimestamp();
        }
        float magnitude= Utils.getMagnitude(sensorData.getLinearAcceleration());
        mShakeData.add(sensorData.clone());
        mWindowData.add(magnitude);
        if(mWindowData.size()<WINDOW_SIZE)
            return;
        if(mWindowData.size()>WINDOW_SIZE){
            mWindowData.remove(0);
        }
        currentState=(calculateVariance(mWindowData)<VARIANCE_THRESHOLD&&magnitude<MAGNITUDE_THRESHOLD);
        if(preState==true&&currentState==false){
            downIndex.add(index);
            //Log.i("down",String.valueOf(index));
        }
        else if(preState==false&&currentState==true){
            upIndex.add(index);
            //Log.i("up",String.valueOf(index));
        }
        preState=currentState;
        index++;
    }

    private float calculateVariance(List<Float> data){
        float mean=0;
        for(float f:data)
            mean+=f;
        mean/=data.size();
        float var=0;
        for(float f:data)
            var+=(f-mean)*(f-mean);
        return (float) Math.sqrt(var/data.size());
    }

    private void judge(){
        for(int i=0;i<upIndex.size()&&(i+1)<downIndex.size();i++){
            if(downIndex.get(i+1)-upIndex.get(i)<5){
                downIndex.remove(i+1);
                upIndex.remove(i);
                i--;
            }
        }
        int start=downIndex.get(0);
        int end=upIndex.get(0);
        int len=end-start;
        for(int i=1;i<downIndex.size()&&i<upIndex.size();i++){
            if(upIndex.get(i)-downIndex.get(i)>len){
                len=upIndex.get(i)-downIndex.get(i);
                end=upIndex.get(i);
                start=downIndex.get(i);
            }
        }
        Log.i("start",String.valueOf(start));
        Log.i("end",String.valueOf(end));
        usefulData= mShakeData.subList(start,end);
    }

    public boolean isShake(){
        judge();
        if(usefulData.size()<100)
            return false;
        else
            return true;
    }

    public List<SensorData> getShakeData(){
        if(usefulData==null)
            judge();
        return usefulData;
    }

    public void close(){
        isClose=true;
    }
}
