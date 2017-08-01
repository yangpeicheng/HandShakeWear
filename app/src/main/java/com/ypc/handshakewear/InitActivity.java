package com.ypc.handshakewear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class InitActivity extends Activity {

    private Button startMaster;
    private Button startSlave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                startMaster=(Button)stub.findViewById(R.id.startMaster);
                startSlave=(Button)stub.findViewById(R.id.startSlave);
                startMaster.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent(InitActivity.this,MasterActivity.class);
                        startActivity(intent);
                    }
                });
                startSlave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent=new Intent(InitActivity.this,SlaveActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });
    }
}
