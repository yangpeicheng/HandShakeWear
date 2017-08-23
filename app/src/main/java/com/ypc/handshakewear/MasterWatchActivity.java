package com.ypc.handshakewear;

import com.ypc.handshakewear.bluetoothUtil.BluetoothService;
import com.ypc.handshakewear.sensor.SensorData;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yangpc on 2017/8/8.
 */

public class MasterWatchActivity extends BluetoothBaseActivity {
    //UI
    private TextView mStatusTextView;
    private Button mStartButton;
    private Button mStopButton;
    //远程传感器数据
    private List<SensorData> remoteSensorDataList=new ArrayList<>();
    //Cascade 协议参数
    private int current_iter_num=0;
    private ReconcilationData mReconcilationData;
    private ArrayList<ReconcilationData> mReconcilationDataQueue=new ArrayList<>();
    //协商前的bit
    private List<Byte> preBits;
    //本地段的奇偶性
    private boolean isEven=false;
    //协商修改的bit数
    private int error=0;
    //levelcrossing量化
    private LevelCrossing levelCrossing;
    //slave 长度
    private int slaveBitLength=9999;
    //双方协定的bit长度
    private int bitLength;
    @Override
    protected void initBluetoothService(){
        Handler handler=new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                    {
                        switch (msg.arg1) {
                            case BluetoothService.STATE_CONNECTED:
                                mStatusTextView.append("\r\nconnected");
                                readSensorHandler.postDelayed(readSensorRunnable,DELAY_TIME);
                                break;
                            case BluetoothService.STATE_CONNECTING:
                                mStatusTextView.append("\r\nconnecting");
                                break;
                            case BluetoothService.STATE_LISTEN:
                            case BluetoothService.STATE_NONE:
                                mStatusTextView.append("\r\nnone");
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
                            SensorData sensorData=(SensorData)msg.obj;
                            remoteSensorDataList.add(sensorData);
                            //mRemoteCsvOutput.writeData(sensorData.toStringArray());
                        }
                        else if(code==CODE_START){
                            Toast.makeText(MasterWatchActivity.this,"ok",Toast.LENGTH_SHORT).show();
                            slaveBitLength=(int)msg.obj;
                        }
                        else if(code==CODE_FINISH){
                            Toast.makeText(MasterWatchActivity.this,"finish",Toast.LENGTH_SHORT).show();
                            int mTimeDelay=calculateTimeDelay(remoteSensorDataList,localSensorDataList);
                            Log.i("len",String.valueOf(localSensorDataList.size()));
                            localSensorDataList=localSensorDataList.subList(mTimeDelay,localSensorDataList.size());
                            bitLength=Math.min(localSensorDataList.size(),slaveBitLength);
                            mStatusTextView.append("\r\n bitLen: "+bitLength);
                            MasterWatchActivity.this.sendMessage(CODE_START,bitLength);
                            mStatusTextView.append(String.format("\r\n timedelay:%d",mTimeDelay));
                            //Toast.makeText(MasterActivity.this,String.format("timedelay:%d",mTimeDelay),Toast.LENGTH_SHORT).show();
                            InitMatrix initMatrix=new InitMatrix(remoteSensorDataList,localSensorDataList.subList(0,mTimeDelay+remoteSensorDataList.size()));
                            float initTheta=initMatrix.train();
                            mStatusTextView.append(String.format("\r\n theta:%f",initTheta));
                            mStatusTextView.append(String.format("\r\n max correlation:%f",initMatrix.getMaxCorrelation()));
                            TrainTools trainTools=new TrainTools();
                            final List<float[]> slocalLinearAcc=trainTools.getGlobalAcceleration(localSensorDataList,initTheta);
                            Handler writeGaccHandler=new Handler();
                            Runnable runnable=new Runnable() {
                                @Override
                                public void run() {
                                    for(float[] g:slocalLinearAcc){
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
                            levelCrossing=new LevelCrossing(slocalLinearAcc,Utils.ALPHA,bitLength);
                            List<Integer> indexList=levelCrossing.getMasterIndex(Utils.EXCURSIONS);
                            List<Byte> tempBits=levelCrossing.subsetByIndex(indexList);
                            mStatusTextView.append(String.format("\r\n Init bits:"+tempBits.toString()+"\r\n len : %d",tempBits.size()));
                            MasterWatchActivity.this.sendMessage(CODE_MASTER_INDEX,indexList);
                            //sendMessage(CODE_START);
                            Toast.makeText(MasterWatchActivity.this,String.format("commit:%d",indexList.size()),Toast.LENGTH_SHORT).show();
                        }
                        else if(code==CODE_MASTER_INDEX){
                            List<Integer> indexFromSlave=(LinkedList<Integer>)msg.obj;
                            preBits=levelCrossing.subsetByIndex(indexFromSlave);
                            mStatusTextView.append("\r\n Update bits :"+preBits.toString());
                            initRounds(preBits.size());
                            initReconcilationQueue(0);
                            mReconcilationData=mReconcilationDataQueue.remove(0);
                            isEven=mReconcilationData.isEven(preBits,mRounds);
                            MasterWatchActivity.this.sendMessage(CODE_CASCADE,mReconcilationData);
                        }
                        else if(code==CODE_ISEVEN){
                            boolean remoteIsEven=(boolean)msg.obj;
                            if(remoteIsEven!=isEven){
                                int start=mReconcilationData.getStart();
                                int end=mReconcilationData.getEnd();
                                if(end-start<=1){
                                    int index=mRounds[mReconcilationData.getRound()].getBlockIndex()[mReconcilationData.getBlock()][start];
                                    Byte val=preBits.get(index);
                                    preBits.set(index,(byte)(1-val));
                                    error++;
                                    for(int i=current_iter_num-1;i>=0;i--){
                                        int blocknum=mRounds[i].findBlockNum(index);
                                        if(blocknum!=-1){
                                            ReconcilationData tmp=new ReconcilationData(i,blocknum,0,mRounds[i].getBlockIndex()[blocknum].length);
                                            mReconcilationDataQueue.add(tmp);
                                        }
                                    }
                                }
                                else {
                                    int mid = (start + end) / 2;
                                    int round = mReconcilationData.getRound();
                                    int block = mReconcilationData.getBlock();
                                    ReconcilationData r1 = new ReconcilationData(round, block, start, mid);
                                    ReconcilationData r2 = new ReconcilationData(round, block, mid, end);
                                    mReconcilationDataQueue.add(0, r2);
                                    mReconcilationDataQueue.add(0, r1);
                                }
                            }
                            if(mReconcilationDataQueue.isEmpty()==false){
                                mReconcilationData=mReconcilationDataQueue.remove(0);
                                isEven=mReconcilationData.isEven(preBits,mRounds);
                                MasterWatchActivity.this.sendMessage(CODE_CASCADE,mReconcilationData);
                            }
                            else if(current_iter_num<CASCADE_ITER-1){
                                current_iter_num++;
                                initReconcilationQueue(current_iter_num);
                                mReconcilationData=mReconcilationDataQueue.remove(0);
                                isEven=mReconcilationData.isEven(preBits,mRounds);
                                MasterWatchActivity.this.sendMessage(CODE_CASCADE,mReconcilationData);
                            }
                            else{
                                mStatusTextView.append("\r\n final key:"+preBits.toString());
                                mStatusTextView.append("\r\n error:"+error+"/"+preBits.size());
                                RandomnessExtract randomnessExtract=new RandomnessExtract(preBits,Utils.MALKOV_ORDER,Utils.BLOCK_LEN);
                                mStatusTextView.append("\r\n randomnessExtract:"+randomnessExtract.codeByTable().toString());
                                MasterWatchActivity.this.sendMessage(CODE_END);
                            }

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
        mBluetoothService.startServer();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);
        final WatchViewStub stub=(WatchViewStub)findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mStatusTextView = (TextView) findViewById(R.id.status);
                //statusTextView.append("\r/n"+android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
                mStartButton = (Button) findViewById(R.id.startMaster);
                mStopButton = (Button) findViewById(R.id.stopMaster);
                mStartButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        readSensorHandler.removeCallbacks(readSensorRunnable);
                        rawDataOutput.closeFile();
                        if (mDetectShake.isShake()) {
                            localSensorDataList = mDetectShake.getShakeData();
                            sendMessage(CODE_START,localSensorDataList.size());
                        } else {
                            mStatusTextView.append("\r\n no handshake !");
                        }
                    }
                });

                mStopButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        });
    }

    private int calculateTimeDelay(List<SensorData> remote,List<SensorData> local){
        List<Float> remoteAcc=new ArrayList<>();
        List<Float> localAcc=new ArrayList<>();
        for(SensorData f:remote){
            remoteAcc.add(Utils.getMagnitude(f.getLinearAcceleration()));
        }
        for(SensorData f:local){
            localAcc.add(Utils.getMagnitude(f.getLinearAcceleration()));
        }
        DataAlign dataAlign=new DataAlign(remoteAcc,localAcc);
        return dataAlign.findAlignIndex();
    }


    private void initReconcilationQueue(int n){
        int blocks=mRounds[n].getBlockNum();
        Log.i("len",String.valueOf(blocks));
        for(int i=0;i<blocks;i++){
            mReconcilationDataQueue.add(new ReconcilationData(n,i,0,mRounds[n].getBlockIndex()[i].length));
        }
    }
}
