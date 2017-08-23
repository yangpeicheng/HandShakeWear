package com.ypc.handshakewear;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.ypc.handshakewear.bluetoothUtil.BluetoothService;
import com.ypc.handshakewear.csvUtils.CsvOutput;
import com.ypc.handshakewear.sensor.DetectShake;
import com.ypc.handshakewear.sensor.MySensorManager;
import com.ypc.handshakewear.sensor.SensorData;

import java.util.Calendar;
import java.util.List;

/**
 * Created by yangpc on 2017/8/6.
 */

public abstract class BluetoothBaseActivity extends Activity{
    //子类具体实现
    protected abstract void initBluetoothService();
    //读Sensor频率
    protected static final int FREQUENCY=50;
    protected static final int DELAY_TIME=1000/FREQUENCY;
    //Message类型
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    // Protocol codes
    public static final int CODE_START = 2;
    public static final int CODE_POSTS = 3;
    public static final int CODE_FINISH=4;
    public static final int CODE_MASTER_INDEX=5;
    public static final int CODE_CASCADE=6 ;
    public static final int CODE_ISEVEN=7;
    public static final int CODE_END=8;
    //bluetooth
    protected BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;
    //Sensor
    protected MySensorManager mMySensorManager;
    protected DetectShake mDetectShake=new DetectShake();
    protected Handler readSensorHandler=new Handler();
    protected Runnable readSensorRunnable=new Runnable() {
        @Override
        public void run() {
            readSensorHandler.postDelayed(this,DELAY_TIME);
            SensorData sensorData=mMySensorManager.getCurrentSensorData();
            mDetectShake.handleSensorData(sensorData);
            rawDataOutput.writeData(sensorData.toStringArray());
        }
    };
    //本地传感器握手数据
    protected List<SensorData> localSensorDataList;
    //bluetooth request
    private static final int REQUEST_ENABLE_BT = 3;
    //Cascade协议循环次数
    public static final int CASCADE_ITER=3;
    protected Round[] mRounds=new Round[CASCADE_ITER];
    //记录本地数据,用时间信息组合生成文件名
    private Calendar mCalendar=Calendar.getInstance();
    private int minute=mCalendar.get(Calendar.MINUTE);
    private int second=mCalendar.get(Calendar.SECOND);
    protected CsvOutput rawDataOutput=new CsvOutput(String.format("local-%d%d.csv",minute,second));
    protected CsvOutput globalDataOutput=new CsvOutput(String.format("globalLocal-%d%d.csv",minute,second));
    //启动service
    @Override
    protected void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mBluetoothService == null) {
                initBluetoothService();
                if(mBluetoothService==null)
                    Toast.makeText(BluetoothBaseActivity.this,"null",Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        mMySensorManager=MySensorManager.newInstance(this);
        mMySensorManager.startSensor();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mBluetoothService!=null){
            mBluetoothService.stop();
        }
    }

    //ensure server discoverable
    protected void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    protected void sendMessage(int code){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code);
    }

    protected void sendMessage(int code,List<Integer> list){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code, list);
    }

    protected void sendMessage(int code,ReconcilationData data){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code, data);
    }

    protected void sendMessage(int code,boolean isEven){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code,isEven);
    }

    protected void sendMessage(int code,int length){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code,length);
    }

    protected void sendMessage(int code, SensorData sensorData) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code, sensorData);
    }

    protected void initRounds(int n){
        for(int i=0;i<CASCADE_ITER;i++){
            if(i==0){
                mRounds[i]=new Round(n,5-i,true);
            }
            else{
                mRounds[i]=new Round(n,5-i,false);
            }
        }
    }
}
