package com.ypc.handshakewear;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ypc.handshakewear.bluetoothUtil.BluetoothService;
import com.ypc.handshakewear.csvUtils.CsvOutput;
import com.ypc.handshakewear.sensor.DetectShake;
import com.ypc.handshakewear.sensor.MySensorManager;
import com.ypc.handshakewear.sensor.SensorData;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class SlaveActivity extends Activity {
    private static final int FREQUENCY=50;
    private static final String MAC_ADDRESS="84:10:0D:67:D6:2A";

    private TextView statusTextView;
    private Button mStopButton;
    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter;

    private MySensorManager mMySensorManager=null;
    private DetectShake mDetectShake=new DetectShake();
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    // Protocol codes
    private static final int REQUEST_ENABLE_BT = 3;

    private Round[] mRounds=new Round[MasterActivity.CASCADE_ITER];

    private List<Byte> preBits;
    //本地传感器数据写入csv
    List<SensorData> sensorDataList;
    private Calendar mCalendar=Calendar.getInstance();
    private int minute=mCalendar.get(Calendar.MINUTE);
    private CsvOutput mLocalCsvOutput;
    private Handler writeLocalDataHandle=new Handler();
    private Runnable writeLoaclDataRunnable=new Runnable() {
        @Override
        public void run() {
            writeLocalDataHandle.postDelayed(this,1000/FREQUENCY);
            SensorData sensorData=mMySensorManager.getCurrentSensorData();
            mLocalCsvOutput.writeData(sensorData.toStringArray());
            mDetectShake.handleSensorData(sensorData);
        }
    };
    private final Handler slaveHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                {
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            statusTextView.setText("connected");
                            writeLocalDataHandle.postDelayed(writeLoaclDataRunnable,1000/FREQUENCY);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            statusTextView.setText("connecting");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            statusTextView.setText("none");
                            break;
                    }
                    break;
                }
                case MESSAGE_WRITE:
                {
                    // no-op
                    break;
                }
                case MESSAGE_READ:
                {

                    Log.d("message",String.valueOf(msg.arg1));
                    int code = msg.arg1;
                    if (code == MasterActivity.CODE_POSTS) {
                        SensorData sensorData=(SensorData)msg.obj;
                    }
                    else if(code==MasterActivity.CODE_START){
                        Toast.makeText(SlaveActivity.this,"ok",Toast.LENGTH_SHORT).show();
                        writeLocalDataHandle.removeCallbacks(writeLoaclDataRunnable);
                        mLocalCsvOutput.closeFile();
                        if(mDetectShake.isShake()){
                            sensorDataList=mDetectShake.getShakeData();
                            commitHandler.post(commitDataRunnable);
                        }
                    }
                    else if(code==MasterActivity.CODE_MASTER_INDEX){
                        List<Integer> indexFromMaster=(LinkedList<Integer>)msg.obj;
                        TrainTools trainTools=new TrainTools();
                        final List<float[]> gacc=trainTools.getGlobalAcceleration(sensorDataList,0);
                        Handler writeGaccHandler=new Handler();
                        Runnable runnable=new Runnable() {
                            @Override
                            public void run() {
                                CsvOutput tempCsv=new CsvOutput("globalRemote"+String.valueOf(minute)+".csv");
                                for(float[] g:gacc){
                                    tempCsv.writeData(convert(g));
                                }
                                tempCsv.closeFile();
                            }
                            public String[] convert(float[] d){
                                String[] result=new String[d.length];
                                for(int i=0;i<result.length;i++){
                                    result[i]=String.valueOf(d[i]);
                                }
                                return result;
                            }
                        };
                        writeGaccHandler.post(runnable);
                        LevelCrossing levelCrossing=new LevelCrossing(gacc,0.3f);
                        List<Integer> tempIndex=levelCrossing.getMasterIndex(3);
                        List<Byte> tempBits=levelCrossing.subsetByIndex(tempIndex);
                        statusTextView.append(String.format("\r\n Init bits:"+tempBits.toString()+"\r\n len : %d",tempBits.size()));
                        List<Integer> indexFromSlave=levelCrossing.getSlaveIndex(indexFromMaster,3);
                        preBits=levelCrossing.subsetByIndex(indexFromSlave);
                        SlaveActivity.this.sendMessage(MasterActivity.CODE_MASTER_INDEX,indexFromSlave);
                        statusTextView.append("\r\n Update bits :"+levelCrossing.subsetByIndex(indexFromSlave).toString());
                        initRounds(preBits.size());
                    }
                    else if(code==MasterActivity.CODE_CASCADE){
                        ReconcilationData reconcilationData=(ReconcilationData)msg.obj;
                        boolean isEven=reconcilationData.isEven(preBits,mRounds);
                        SlaveActivity.this.sendMessage(MasterActivity.CODE_ISEVEN,isEven);
                    }
                    else if(code==MasterActivity.CODE_END){
                        RandomnessExtract randomnessExtract=new RandomnessExtract(preBits,2,4);
                        statusTextView.append("\r\n final key:"+randomnessExtract.codeByTable().toString());
                    }
                    break;
                }
            }
        }
    };
    private Runnable commitDataRunnable=new Runnable() {
        @Override
        public void run() {
            for(SensorData sensorData:sensorDataList.subList(0,50)){
                sendMessage(MasterActivity.CODE_POSTS,sensorData);
            }
            sendMessage(MasterActivity.CODE_FINISH);
            Toast.makeText(SlaveActivity.this,String.valueOf(sensorDataList.size()),Toast.LENGTH_SHORT).show();
        }
    };
    private Handler commitHandler=new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slave);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mStopButton=(Button)findViewById(R.id.stopSlave);
                statusTextView=(TextView)findViewById(R.id.slaveStatus);
                mStopButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeLocalDataHandle.removeCallbacks(writeLoaclDataRunnable);
                    }
                });
            }
        });

        mLocalCsvOutput=new CsvOutput("remote"+String.valueOf(minute)+".csv");
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        mMySensorManager=MySensorManager.newInstance(this);
        mMySensorManager.startSensor();
    }

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
                mBluetoothService=new BluetoothService(this,slaveHandler);
            }
        }
    }
    @Override
    protected synchronized void onResume(){
        super.onResume();
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
        ensureDiscoverable();
        mBluetoothService.connect(MAC_ADDRESS,true);
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
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void sendMessage(int code, SensorData sensorData) {
        // Check that we're actually connected before trying anything
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code, sensorData);
    }

    private void sendMessage(int code,List<Integer> list){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code, list);
    }

    private void sendMessage(int code){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }

        mBluetoothService.writeStart(code);
    }

    private void sendMessage(int code,boolean isEven){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code,isEven);
    }

    private void initRounds(int n){
        for(int i=0;i<MasterActivity.CASCADE_ITER;i++){
            if(i==0){
                mRounds[i]=new Round(n,5-i,true);
            }
            else{
                mRounds[i]=new Round(n,5-i,false);
            }
        }
    }
}
