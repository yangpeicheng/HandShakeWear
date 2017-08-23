package com.ypc.handshakewear;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;

import android.widget.TextView;
import android.widget.Toast;

import com.ypc.handshakewear.bluetoothUtil.BluetoothService;
import com.ypc.handshakewear.sensor.SensorData;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by yangpc on 2017/8/8.
 */

public class SlaveWatchActivity extends BluetoothBaseActivity {
    //master mac地址
    private static final String MAC_ADDRESS="84:10:0D:67:D6:2A";
    //ui
    private TextView mStatusTextView;
    //cascade协商前的比特
    private List<Byte> preBits;
    //是否传输了长度
    private boolean commitLengthFlag=false;
    //双方协定的bit长度
    private int bitLength;
    //传输前50个数据用于训练参数
    private Runnable commitDataRunnable=new Runnable() {
        @Override
        public void run() {
            for(SensorData sensorData:localSensorDataList.subList(0,50)){
                sendMessage(CODE_POSTS,sensorData);
            }
            sendMessage(CODE_FINISH);
            //Toast.makeText(SlaveActivity.this,String.valueOf(sensorDataList.size()),Toast.LENGTH_SHORT).show();
        }
    };
    private Handler commitHandler=new Handler();
    @Override
    protected void initBluetoothService(){
        Handler handler=new Handler(){
            @Override
            public void handleMessage(Message msg){
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                    {
                        switch (msg.arg1) {
                            case BluetoothService.STATE_CONNECTED:
                                mStatusTextView.setText("connected");
                                readSensorHandler.postDelayed(readSensorRunnable,1000/FREQUENCY);
                                break;
                            case BluetoothService.STATE_CONNECTING:
                                mStatusTextView.setText("connecting");
                                break;
                            case BluetoothService.STATE_LISTEN:
                            case BluetoothService.STATE_NONE:
                                mStatusTextView.setText("none");
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
                        if (code == CODE_POSTS) {
                            //SensorData sensorData=(SensorData)msg.obj;
                        }
                        else if(code==CODE_START){
                            if(!commitLengthFlag) {
                                commitLengthFlag=true;
                                readSensorHandler.removeCallbacks(readSensorRunnable);
                                rawDataOutput.closeFile();
                                if (mDetectShake.isShake()) {
                                    localSensorDataList = mDetectShake.getShakeData();
                                    SlaveWatchActivity.this.sendMessage(CODE_START, localSensorDataList.size());
                                    commitHandler.post(commitDataRunnable);
                                    Toast.makeText(SlaveWatchActivity.this, "ok", Toast.LENGTH_SHORT).show();
                                }
                            }else {
                                bitLength=(int)msg.obj;
                                mStatusTextView.append("\r\n bitLen: "+bitLength);
                            }
                        }
                        else if(code==CODE_MASTER_INDEX){
                            List<Integer> indexFromMaster=(LinkedList<Integer>)msg.obj;
                            TrainTools trainTools=new TrainTools();
                            final List<float[]> gacc=trainTools.getGlobalAcceleration(localSensorDataList,0);
                            Handler writeGaccHandler=new Handler();
                            Runnable runnable=new Runnable() {
                                @Override
                                public void run() {
                                    for(float[] g:gacc){
                                        globalDataOutput.writeData(convert(g));
                                    }
                                    globalDataOutput.closeFile();
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
                            LevelCrossing levelCrossing=new LevelCrossing(gacc,Utils.ALPHA,bitLength);
                            List<Integer> tempIndex=levelCrossing.getMasterIndex(Utils.EXCURSIONS);
                            List<Byte> tempBits=levelCrossing.subsetByIndex(tempIndex);
                            mStatusTextView.append(String.format("\r\n Init bits:"+tempBits.toString()+"\r\n len : %d",tempBits.size()));
                            List<Integer> indexFromSlave=levelCrossing.getSlaveIndex(indexFromMaster,Utils.EXCURSIONS);
                            preBits=levelCrossing.subsetByIndex(indexFromSlave);
                            SlaveWatchActivity.this.sendMessage(CODE_MASTER_INDEX,indexFromSlave);
                            mStatusTextView.append("\r\n Update bits :"+levelCrossing.subsetByIndex(indexFromSlave).toString());
                            initRounds(preBits.size());
                        }
                        else if(code==CODE_CASCADE){
                            ReconcilationData reconcilationData=(ReconcilationData)msg.obj;
                            boolean isEven=reconcilationData.isEven(preBits,mRounds);
                            SlaveWatchActivity.this.sendMessage(CODE_ISEVEN,isEven);
                        }
                        else if(code==CODE_END){
                            RandomnessExtract randomnessExtract=new RandomnessExtract(preBits,Utils.MALKOV_ORDER,Utils.BLOCK_LEN);
                            mStatusTextView.append("\r\n final key:"+randomnessExtract.codeByTable().toString());
                        }
                        break;
                    }
                }
            }
        };
        mBluetoothService=new BluetoothService(handler);
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
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slave);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mStatusTextView=(TextView)findViewById(R.id.slaveStatus);
            }
        });
    }
}
