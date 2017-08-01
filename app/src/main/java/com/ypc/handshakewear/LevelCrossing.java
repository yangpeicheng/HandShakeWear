package com.ypc.handshakewear;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by yangpc on 2017/7/17.
 */
public class LevelCrossing {
    private List<Byte> bits;
    public LevelCrossing(List<float[]> data,float alpha){
        List<Float> convertData=convert(data);
        bits=generateTempBits(convertData,alpha);
    }

    public List<Integer> getMasterIndex(int m){
        List<Integer> masterIndex=getExcurtionIndexes(bits,m);
        return masterIndex;
    }

    public List<Integer> getSlaveIndex(List<Integer> index,int m){
        return getIndexesFromBob(bits,index,m);
    }

    public List<Byte> subsetByIndex(List<Integer> indexs){
        List<Byte> result=new LinkedList<>();
        for(int i:indexs){
            result.add(bits.get(i));
        }
        return result;
    }
    //alice部分下标
    private  List<Integer> getExcurtionIndexes(List<Byte> d,int m){
        List<Integer> indexs=new LinkedList<>();
        Byte preBit=d.get(0);
        int consecutive=1;
        for(int i=1;i<d.size();i++){
            Byte currentBit=d.get(i);
            if(currentBit!=(byte)2&&currentBit==preBit){
                consecutive++;
            }
            else{
                if(consecutive>=m){
                    indexs.add(i-consecutive/2-1);
                }
                consecutive=1;
                preBit=currentBit;
            }
            if(i==d.size()-1&&consecutive>=m)
                indexs.add(i-consecutive/2);
        }
        return indexs;
    }
    //bobo部分下标
    private List<Integer> getIndexesFromBob(List<Byte> bits,List<Integer> indexs,int m){
        int lowfloor=m/2;
        List<Integer> result=new LinkedList<>();
        for(int i=0;i<indexs.size();i++){
            int index=indexs.get(i);
            int start=index-lowfloor;
            int end=index+lowfloor;
            if(start<0||index>=bits.size()||end>=bits.size())
                continue;
            byte bit=bits.get(index);
            boolean flag=true;
            for(int j=start;j<=end;j++){
                if(bit!=bits.get(j)||bits.get(j)==2){
                    flag=false;
                    break;
                }
            }
            if(flag)
                result.add(index);
        }
        return result;
    }
    //将List<float[]>转换为List<Float>
    private List<Float> convert(List<float[]> input){
        List<Float> output=new LinkedList<>();
        for(int i=0;i<input.get(0).length;i++){
            for(int j=0;j<input.size();j++){
                output.add(input.get(j)[i]);
            }
        }
        return output;
    }

    private List<Byte> generateTempBits(List<Float> list,float alpha){
        float mean=calculateMean(list);
        float sd=calculateSd(list,mean);
        LinkedList<Byte> bits=new LinkedList<>();
        float q_plus=mean+alpha*sd;
        float q_minus=mean-alpha*sd;
        for(float f:list){
            bits.add(quantizer(f,q_plus,q_minus));
        }
        return bits;
    }

    private byte quantizer(float f,float q_plus,float q_minus){
        if(f>q_plus){
            return (byte)1;
        }else if(f<q_minus){
            return (byte)0;
        }else {
            return (byte)2;
        }
    }
    //计算均值
    private float calculateMean(List<Float> list){
        float mean=0;
        for(float f:list)
            mean+=f;
        return mean/list.size();
    }
    //计算方差
    private float calculateSd(List<Float> list,float mean){
        float sd=0;
        for(float f:list){
            sd+=(f-mean)*(f-mean);
        }
        return (float)Math.sqrt(sd/list.size());
    }

    public List<Byte> getInitalBits(){
        return bits;
    }
}
