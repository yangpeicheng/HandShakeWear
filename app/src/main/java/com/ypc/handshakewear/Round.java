package com.ypc.handshakewear;

/**
 * Created by yangpc on 2017/7/25.
 */

public class Round {
    private int totalNum;
    private int blockSize;
    private int[] positionMap;
    private int blockNum;
    private int[][] blockIndex;
    public Round(int n,int k,boolean isFirstRound){
        totalNum=n;
        blockSize=k;
        positionMap=new int[totalNum];
        blockNum=(int)Math.ceil(n/(k+0.0d));
        blockIndex=new int[blockNum][];
        for(int i=0;i<blockNum-1;i++){
            blockIndex[i]=new int[blockSize];
        }
        blockIndex[blockNum-1]=new int[totalNum-(blockNum-1)*blockSize];
        if(!isFirstRound)
            positionMap=Utils.getRandomSeq(totalNum,blockSize);
        else {
            positionMap=new int[totalNum];
            for(int i=0;i<positionMap.length;i++)
                positionMap[i]=i;
        }
        int index=0;
        for(int i=0;i<blockNum;i++){
            for(int j=0;j<blockSize;j++){
                if(index==positionMap.length)
                    break;
                blockIndex[i][j]=positionMap[index++];
            }
        }
            /*for(int i=0;i<blockIndex.length;i++){
                for(int j=0;j<blockIndex[i].length;j++){
                    System.out.print(blockIndex[i][j]);
                }
                System.out.println();
            }*/
    }

    public int findBlockNum(int index){
        for(int i=0;i<blockNum;i++){
            for(int j=0;j<blockIndex[i].length;j++){
                if(blockIndex[i][j]==index)
                    return i;
            }
        }
        return -1;
    }

    public int[] findBlock(int index){
        int i=findBlockNum(index);
        return blockIndex[i];
    }

    public int[][] getBlockIndex(){
        return blockIndex;
    }

    public int getBlockNum() {
        return blockNum;
    }
}
