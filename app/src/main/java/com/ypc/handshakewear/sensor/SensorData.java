package com.ypc.handshakewear.sensor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by yangpc on 2017/7/7.
 */

public class SensorData implements Serializable,Cloneable{

    private static final long serialVersionUID = 7683127339085597779L;
    private static final String TAG="SensorData";

    private float[] mAcceleration=new float[3];
    private float[] mGyroscope=new float[3];
    private float[] mMagnetometer=new float[3];
    private float[] mLinearAcceleration=new float[3];
    private float[] mGravity=new float[3];

    private long timestamp;
    private static final int DATALENGTH=16;

    public SensorData(){

    }
    //csv文件按行读入为string[]
    public SensorData(String[] csvInput){
        if(csvInput.length==DATALENGTH){
            int index=0;
            for(int i=0;i<mAcceleration.length;i++){
                mAcceleration[i]=Float.parseFloat(csvInput[index]);
                index++;
            }
            for(int i=0;i<mGyroscope.length;i++){
                mGyroscope[i]=Float.parseFloat(csvInput[index]);
                index++;
            }
            for(int i=0;i<mMagnetometer.length;i++){
                mMagnetometer[i]=Float.parseFloat(csvInput[index]);
                index++;
            }
            timestamp=Long.parseLong(csvInput[index]);
        }
    }

    public String[] toStringArray(){
        String[] to=new String[DATALENGTH];
        int index=0;
        for(int i=0;i<mAcceleration.length;i++){
            to[index]=String.valueOf(mAcceleration[i]);
            index++;
        }
        for(int i=0;i<mLinearAcceleration.length;i++){
            to[index]=String.valueOf(mLinearAcceleration[i]);
            index++;
        }
        for(int i=0;i<mGravity.length;i++){
            to[index]=String.valueOf(mGravity[i]);
            index++;
        }
        for(int i=0;i<mGyroscope.length;i++){
            to[index]=String.valueOf(mGyroscope[i]);
            index++;
        }
        for(int i=0;i<mMagnetometer.length;i++){
            to[index]=String.valueOf(mMagnetometer[i]);
            index++;
        }
        to[index]=String.valueOf(timestamp);
        return to;
    }

    public void read(DataInputStream in) throws IOException{
        for(int i=0;i<mAcceleration.length;i++){
            mAcceleration[i]=in.readFloat();
        }
        for(int i=0;i<mLinearAcceleration.length;i++){
            mLinearAcceleration[i]=in.readFloat();
        }
        for(int i=0;i<mGravity.length;i++){
            mGravity[i]=in.readFloat();
        }
        for(int i=0;i<mGyroscope.length;i++){
            mGyroscope[i]=in.readFloat();
        }
        for(int i=0;i<mMagnetometer.length;i++){
            mMagnetometer[i]=in.readFloat();
        }
        timestamp=in.readLong();
    }

    public void write(DataOutputStream out) throws IOException{
        for(int i=0;i<mAcceleration.length;i++){
            out.writeFloat(mAcceleration[i]);
        }
        for(int i=0;i<mLinearAcceleration.length;i++){
            out.writeFloat(mLinearAcceleration[i]);
        }
        for(int i=0;i<mGravity.length;i++){
            out.writeFloat(mGravity[i]);
        }
        for(int i=0;i<mGyroscope.length;i++){
            out.writeFloat(mGyroscope[i]);
        }
        for(int i=0;i<mMagnetometer.length;i++){
            out.writeFloat(mMagnetometer[i]);
        }
        out.writeLong(timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float[] getAcceleration() {
        return mAcceleration;
    }

    public void setAcceleration(float[] acceleration) {
        mAcceleration = acceleration;
    }

    public float[] getGyroscope() {
        return mGyroscope;
    }

    public void setGyroscope(float[] gyroscope) {
        mGyroscope = gyroscope;
    }

    public float[] getMagnetometer() {
        return mMagnetometer;
    }

    public void setMagnetometer(float[] magnetometer) {
        mMagnetometer = magnetometer;
    }

    public float[] getLinearAcceleration() {
        return mLinearAcceleration;
    }

    public void setLinearAcceleration(float[] linearAcceleration) {
        mLinearAcceleration = linearAcceleration;
    }

    public float[] getGravity() {
        return mGravity;
    }

    public void setGravity(float[] gravity) {
        mGravity = gravity;
    }

    public String toString(){
        StringBuffer tmp=new StringBuffer();
        tmp.append("\r\nacc:");
        for(int i=0;i<mAcceleration.length;i++){
            tmp.append(String.valueOf(mAcceleration[i])+',');
        }
        tmp.append("\r\nlinearacc:");
        for(int i=0;i<mLinearAcceleration.length;i++){
            tmp.append(String.valueOf(mLinearAcceleration[i])+',');
        }
        tmp.append("\r\ngravity:");
        for(int i=0;i<mGravity.length;i++){
            tmp.append(String.valueOf(mGravity[i])+',');
        }

        tmp.append("\r\ngyro:");
        for(int i=0;i<mGyroscope.length;i++){
            tmp.append(String.valueOf(mGyroscope[i])+',');
        }
        tmp.append("\r\nmagn:");
        for(int i=0;i<mMagnetometer.length;i++){
            tmp.append(String.valueOf(mMagnetometer[i])+',');
        }
        tmp.append("\r\ntimestamp:");
        tmp.append(String.valueOf(timestamp));
        return tmp.toString();
    }

    @Override
    protected SensorData clone() {
        SensorData newSensorData=new SensorData();
        newSensorData.setAcceleration(this.getAcceleration().clone());;
        newSensorData.setGravity(this.getGravity().clone());
        newSensorData.setLinearAcceleration(this.getLinearAcceleration().clone());
        newSensorData.setGyroscope(this.getGyroscope().clone());
        newSensorData.setMagnetometer(this.getMagnetometer().clone());
        newSensorData.setTimestamp(this.getTimestamp());
        return newSensorData;
    }
}
