package com.ypc.handshakewear;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by yangpc on 2017/7/18.
 */
public class RandomnessExtract {
    private int malkovOrder;
    private List<Byte> bits;
    private List<Byte>[] subStrings;
    private List<Byte>[] codeTable;
    private int blockLen;
    public RandomnessExtract(List<Byte> bits,int malkovOrder,int blockLen){
        this.bits=bits;
        this.malkovOrder=malkovOrder;
        this.blockLen=blockLen;
        subStrings=new List[1<<malkovOrder];
        for(int i=0;i<subStrings.length;i++){
            subStrings[i]=new LinkedList<>();
        }
    }

    private void loadTable(){
        codeTable=new List[1<<blockLen];
        ArrayList<Integer>[] tempTable=new ArrayList[blockLen+1];
        for(int i=0;i<tempTable.length;i++){
            tempTable[i]=new ArrayList<>();
            for(int j=0;j<Combination(blockLen,i);j++){
                tempTable[i].add(j);
            }
        }
        for(int i=0;i<codeTable.length;i++){
            List<Byte> list=int2Bit(i);
            int oneNum=countOneNum(list);
            int codeLen=(int)Math.ceil( Math.log(Combination(blockLen,oneNum))/Math.log(2));
            if(codeLen==0)
                codeLen=1;
            int range=tempTable[oneNum].size();
            Random random=new Random(i);
            int index=random.nextInt(range);
            int num=tempTable[oneNum].remove(index);
            codeTable[i]=int2Bit(num,codeLen);
            /*System.out.print(i+":");
            for(byte b:codeTable[i]){
                System.out.print(b);
            }
            System.out.println();*/
        }

    }

    private int countOneNum(List<Byte> list){
        int count=0;
        for(byte b:list){
            if(b==(byte)1)
                count++;
        }
        return count;
    }
    //a>b
    private int Combination(int a,int b){
        int up=1,low=1;
        for(int i=0;i<b;i++){
            up*=(a-i);
        }
        for(int i=2;i<=b;i++){
            low*=i;
        }
        return up/low;
    }

    public List<Byte> codeByTable(){
        loadTable();
        subStringByMalkov();
        List<Byte> finalkey=new LinkedList<>();
        for(List<Byte> list:subStrings){
            int strLen=list.size();
            if(strLen==0)
                continue;
            else {
                for(int j=0;j<strLen;j=j+blockLen){
                    int end=Math.min(j+blockLen-1,strLen-1)+1;
                    int key=bit2Int(list.subList(j,end));
                    /*System.out.print(key+":");
                    for(byte b:list.subList(j,end))
                        System.out.print(b);
                    System.out.println();*/
                    finalkey.addAll(codeTable[key]);
                }
            }
        }
        /*for(Byte b:finalkey)
            System.out.print(b);
        System.out.println();
        System.out.println(finalkey.size());*/
        return finalkey;

    }

    private List<Byte> int2Bit(int input){
        List<Byte> list=new LinkedList<>();
        if(input==0){
            list.add((byte)0);
            return list;
        }
        while (input!=0){
            list.add(0,(byte)(input%2));
            input=input/2;
        }
        return list;
    }

    private List<Byte> int2Bit(int input,int codelen){
        List<Byte> result=int2Bit(input);
        while (result.size()<codelen){
            result.add(0,(byte)0);
        }
        return result;
    }

    private int bit2Int(List<Byte> list){
        int result=0;
        for(int i=0;i<list.size();i++){
            result+=list.get(i)<<(list.size()-1-i);
        }
        return result;
    }

    private void subStringByMalkov(){
        Byte2Num byte2Num=new Byte2Num(malkovOrder);
        for(int i=0;i<bits.size()-1;i++){
            if(!byte2Num.readBit(bits.get(i)))
                continue;
            int index=byte2Num.getNum();
            subStrings[index].add(bits.get(i+1));
        }
        /*for(int i=0;i<subStrings.length;i++){
            System.out.print(i+":");
            for (Byte b:subStrings[i])
                System.out.print(b);
            System.out.println();
        }*/
    }

    private class Byte2Num{
        private int malkovOrder;
        private List<Byte> bits=new LinkedList<>();
        public Byte2Num(int malkovOrder){
            this.malkovOrder=malkovOrder;
        }
        public boolean readBit(byte bit){
            bits.add(bit);
            if(bits.size()>malkovOrder)
                bits.remove(0);
            if(bits.size()==malkovOrder)
                return true;
            else
                return false;
        }
        public int getNum(){
            int result=0;
            for(int i=0;i<bits.size();i++){
                result+=bits.get(i)*(1<<(malkovOrder-1-i));
            }
            return result;
        }
    }
}
