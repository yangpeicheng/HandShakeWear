package com.ypc.handshakewear.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.ypc.handshakewear.Utils;

/**
 * Created by yangpc on 2017/7/7.
 */

public  class SensorReader implements SensorEventListener {
    private SensorData mSensorData=new SensorData();
    private MeanSmooth mAccelerationMeanSmooth=new MeanSmooth();
    private MeanSmooth mGyroscopeMeanSmooth=new MeanSmooth();
    private MeanSmooth mMagnetometerMeanSmooth=new MeanSmooth();
    private MeanSmooth mLinearAccelerationMeanSmooth=new MeanSmooth();
    private MeanSmooth mGravityMeanSmooth=new MeanSmooth();
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType()== Sensor.TYPE_ACCELEROMETER){
            //mSensorData.setAcceleration(event.values.clone());
            mSensorData.setAcceleration(mAccelerationMeanSmooth.addSamples(event.values));
        }
        else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
            //mSensorData.setGyroscope(event.values.clone());
            mSensorData.setGyroscope(mGyroscopeMeanSmooth.addSamples(event.values));
        }
        else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            //mSensorData.setMagnetometer(event.values.clone());
            mSensorData.setMagnetometer(mMagnetometerMeanSmooth.addSamples(event.values));
        }
        else if(event.sensor.getType()==Sensor.TYPE_GRAVITY){
            mSensorData.setGravity(mGravityMeanSmooth.addSamples(event.values));
        }
        else if(event.sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION){
            mSensorData.setLinearAcceleration(mLinearAccelerationMeanSmooth.addSamples(event.values));
            mSensorData.setTimestamp(event.timestamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public SensorData getSensorData(){
        return mSensorData;
    }
}
