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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class MasterActivity extends Activity {
    private static final int FREQUENCY=50;
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final int CASCADE_ITER=3;
    private int current_iter_num=0;

    private Round[] mRounds=new Round[CASCADE_ITER];
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String IS_SERVER = "is_server";
    public static final String TOAST = "toast";
    // Protocol codes
    public static final int CODE_START = 2;
    public static final int CODE_POSTS = 3;
    public static final int CODE_FINISH=4;
    public static final int CODE_MASTER_INDEX=5;
    public static final int CODE_CASCADE=6 ;
    public static final int CODE_ISEVEN=7;
    public static final int CODE_END=8;

    private static final int REQUEST_ENABLE_BT = 3;


    private TextView statusTextView;
    private Button startButton;
    private Button stopButton;

    private BluetoothService mBluetoothService;
    private BluetoothAdapter mBluetoothAdapter = null;
    private MySensorManager mMySensorManager=null;
    private DetectShake mDetectShake=new DetectShake();
    private Calendar mCalendar=Calendar.getInstance();
    private int minute=mCalendar.get(Calendar.MINUTE);
    private CsvOutput mRemoteCsvOutput=new CsvOutput("remote"+String.valueOf(minute)+".csv");
    private int mTimeDelay=0;
    private List<SensorData> localSensorDataList;
    private List<SensorData> remoteSensorDataList=new ArrayList<>();
    private LevelCrossing levelCrossing;

    private ReconcilationData mReconcilationData;
    private ArrayList<ReconcilationData> mReconcilationDataQueue=new ArrayList<>();

    private List<Byte> preBits;
    private boolean isEven=false;
    private int error=0;
    private final Handler masterHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                {
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            statusTextView.append("\r\nconnected");
                            writeLocalDataHandle.postDelayed(writeLoaclDataRunnable,1000/FREQUENCY);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            statusTextView.append("\r\nconnecting");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            statusTextView.append("\r\nnone");
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
                        //statusTextView.append(sensorData.toString());
                        remoteSensorDataList.add(sensorData);
                        mRemoteCsvOutput.writeData(sensorData.toStringArray());
                    }
                    else if(code==CODE_START){
                        Toast.makeText(MasterActivity.this,"ok",Toast.LENGTH_SHORT).show();
                    }
                    else if(code==CODE_FINISH){
                        Toast.makeText(MasterActivity.this,"finish",Toast.LENGTH_SHORT).show();
                        mRemoteCsvOutput.closeFile();
                        commitIndexHandler.post(commitIndexRunnable);
                    }
                    else if(code==CODE_MASTER_INDEX){
                        List<Integer> indexFromSlave=(LinkedList<Integer>)msg.obj;
                        preBits=levelCrossing.subsetByIndex(indexFromSlave);
                        statusTextView.append("\r\n Update bits :"+preBits.toString());
                        initRounds(preBits.size());
                        mReconcilationData=mReconcilationDataQueue.remove(0);
                        isEven=mReconcilationData.isEven(preBits,mRounds);
                        MasterActivity.this.sendMessage(CODE_CASCADE,mReconcilationData);
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
                            MasterActivity.this.sendMessage(CODE_CASCADE,mReconcilationData);
                        }
                        else if(current_iter_num<CASCADE_ITER-1){
                            current_iter_num++;
                            initReconcilationQueue(current_iter_num);
                            mReconcilationData=mReconcilationDataQueue.remove(0);
                            isEven=mReconcilationData.isEven(preBits,mRounds);
                            MasterActivity.this.sendMessage(CODE_CASCADE,mReconcilationData);
                        }
                        else{
                            statusTextView.append("\r\n final key:"+preBits.toString());
                            statusTextView.append("\r\n error:"+error+"/"+preBits.size());
                            RandomnessExtract randomnessExtract=new RandomnessExtract(preBits,2,4);
                            statusTextView.append("\r\n randomnessExtract:"+randomnessExtract.codeByTable().toString());
                            MasterActivity.this.sendMessage(CODE_END);
                        }

                    }
                    break;
                }
            }
        }
    };
    private Handler commitIndexHandler=new Handler();
    private Runnable commitIndexRunnable=new Runnable() {
        @Override
        public void run() {

            mTimeDelay=calculateTimeDelay(remoteSensorDataList,localSensorDataList);
            statusTextView.append(String.format("\r\n timedelay:%d",mTimeDelay));
            //Toast.makeText(MasterActivity.this,String.format("timedelay:%d",mTimeDelay),Toast.LENGTH_SHORT).show();
            InitMatrix initMatrix=new InitMatrix(remoteSensorDataList,localSensorDataList.subList(mTimeDelay,mTimeDelay+remoteSensorDataList.size()));
            float initTheta=initMatrix.train();
            statusTextView.append(String.format("\r\n theta:%f",initTheta));
            statusTextView.append(String.format("\r\n max correlation:%f",initMatrix.getMaxCorrelation()));
            List<float[]> globalAccLocal=new ArrayList<>();
            List<float[]> globalAccRemote=new ArrayList<>();
            for(int i=0;i<remoteSensorDataList.size();i++){
                globalAccLocal.add(localSensorDataList.get(mTimeDelay+i).getAcceleration());
                globalAccRemote.add(remoteSensorDataList.get(i).getAcceleration());
            }
            statusTextView.append(String.format("\r\n global acc correlation:"+Utils.array_cross_correlation(globalAccLocal,globalAccRemote)));
            // Toast.makeText(MasterActivity.this,String.format("theta:%f",initTheta),Toast.LENGTH_SHORT).show();
            TrainTools trainTools=new TrainTools();
            final List<float[]> slocalLinearAcc=trainTools.getGlobalAcceleration(localSensorDataList.subList(mTimeDelay,localSensorDataList.size()),initTheta);
            Handler writeGaccHandler=new Handler();
            Runnable runnable=new Runnable() {
                @Override
                public void run() {
                    CsvOutput tempCsv=new CsvOutput("globalLocal"+String.valueOf(minute)+".csv");
                    for(float[] g:slocalLinearAcc){
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
            levelCrossing=new LevelCrossing(slocalLinearAcc,0.3f);
            List<Integer> indexList=levelCrossing.getMasterIndex(3);
            List<Byte> tempBits=levelCrossing.subsetByIndex(indexList);
            /*for(int i:indexList)
                statusTextView.append(String.valueOf(i));*/
            statusTextView.append(String.format("\r\n Init bits:"+tempBits.toString()+"\r\n len : %d",tempBits.size()));
            sendMessage(CODE_MASTER_INDEX,indexList);
            //sendMessage(CODE_START);
            Toast.makeText(MasterActivity.this,String.format("commit:%d",indexList.size()),Toast.LENGTH_SHORT).show();
        }
    };
    //本地传感器数据写入csv
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);
        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        mMySensorManager=MySensorManager.newInstance(this);
        mMySensorManager.startSensor();
        mLocalCsvOutput=new CsvOutput("local"+String.valueOf(minute)+".csv");
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                statusTextView=(TextView)findViewById(R.id.status);
                //statusTextView.append("\r/n"+android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
                startButton=(Button)findViewById(R.id.startMaster);
                stopButton=(Button)findViewById(R.id.stopMaster);
                startButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage(CODE_START);
                    }
                });

                stopButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeLocalDataHandle.removeCallbacks(writeLoaclDataRunnable);
                        if(mDetectShake.isShake()) {
                            localSensorDataList = mDetectShake.getShakeData();
                            sendMessage(CODE_START);
                        }
                    }
                });
            }
        });

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
                mBluetoothService=new BluetoothService(this,masterHandler);
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
        mBluetoothService.startServer();
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
        mLocalCsvOutput.closeFile();
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

    private void sendMessage(int code){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.writeStart(code);
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

    private void sendMessage(int code,List<Integer> list){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code, list);
    }

    private void sendMessage(int code,ReconcilationData data){
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "no connected", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothService.write(code, data);
    }

    private void initRounds(int n){
        for(int i=0;i<CASCADE_ITER;i++){
            if(i==0){
                mRounds[i]=new Round(n,5-i,true);
            }
            else{
                mRounds[i]=new Round(n,5-i,false);
            }
        }
        initReconcilationQueue(0);
    }

    private void initReconcilationQueue(int n){
        int blocks=mRounds[n].getBlockNum();
        for(int i=0;i<blocks;i++){
            mReconcilationDataQueue.add(new ReconcilationData(n,i,0,mRounds[n].getBlockIndex()[i].length));
        }
    }
}
