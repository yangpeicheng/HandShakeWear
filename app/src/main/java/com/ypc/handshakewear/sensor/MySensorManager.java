package com.ypc.handshakewear.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

/**
 * Created by yangpc on 2017/7/7.
 */

public class MySensorManager {
    private SensorManager mSensorManager;
    private static MySensorManager mMySensorManager;
    private SensorReader mSensorReader=new SensorReader();
    private boolean isStart=false;
    public static MySensorManager newInstance(Context context){
        if(mMySensorManager==null)
            mMySensorManager=new MySensorManager(context);
        return mMySensorManager;
    }
    private MySensorManager(Context context){
        mSensorManager=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    }

    //开启监听
    public void startSensor(){

        mSensorManager.registerListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),SensorManager.SENSOR_DELAY_GAME);
        isStart=true;
    }

    //关闭监听
    public void stopSensor(){
        isStart=false;
        mSensorManager.unregisterListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        mSensorManager.unregisterListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        mSensorManager.unregisterListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        mSensorManager.unregisterListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
        mSensorManager.unregisterListener(mSensorReader,mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
    }

    //读取当前传感器数据
    public SensorData getCurrentSensorData(){
        return mSensorReader.getSensorData();
    }

    public boolean isStart(){
        return isStart;
    }
}
