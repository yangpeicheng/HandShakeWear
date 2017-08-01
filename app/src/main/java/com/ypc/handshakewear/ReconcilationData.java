package com.ypc.handshakewear;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by yangpc on 2017/7/25.
 */

public class ReconcilationData {
    private int round=0;
    private int block=0;
    private int start=0;
    private int end=0;
    public ReconcilationData(int round,int block,int start,int end){
        this.round=round;
        this.block=block;
        this.start=start;
        this.end=end;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(round);
        out.writeInt(block);
        out.writeInt(start);
        out.writeInt(end);
    }

    public void read(DataInputStream in) throws IOException{
        this.round=in.readInt();
        this.block=in.readInt();
        this.start=in.readInt();
        this.end=in.readInt();
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getBlock() {
        return block;
    }

    public void setBlock(int block) {
        this.block = block;
    }

    public int getStart() {
        return start;

    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public boolean isEven(List<Byte> bits,Round[] rounds){
        int[] index=new int[end-start];
        for(int i=0;i<index.length;i++){
            index[i]=rounds[round].getBlockIndex()[block][start+i];
            System.out.println("index:"+index[i]);
        }
        return Utils.sumIsEven(bits,index);
    }
}
