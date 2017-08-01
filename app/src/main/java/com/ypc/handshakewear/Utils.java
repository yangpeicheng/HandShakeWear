package com.ypc.handshakewear;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by yangpc on 2017/7/13.
 */
public class Utils {
    public static final float[] I={
            1,0,0,
            0,1,0,
            0,0,1
    };
    //获取长度为n的随机序列
    public static int[] getRandomSeq(int n){
        int[] seq=new int[n];
        List<Integer> set=new ArrayList<>();
        for(int i=0;i<n;i++)
            set.add(i);
        Random random=new Random();
        for(int j=0;j<n;j++){
            int seed=set.size();
            seq[j]=set.remove(random.nextInt(seed));
        }
        return seq;
    }

    public static int[] getRandomSeq(int n,int s){
        System.out.println("n:"+n+"seed:"+s);
        int[] seq=new int[n];
        List<Integer> set=new ArrayList<>();
        for(int i=0;i<n;i++)
            set.add(i);
        Random random=new Random(s);
        for(int j=0;j<n;j++){
            int seed=set.size();
            seq[j]=set.remove(random.nextInt(seed));
        }
        for(int i:seq)
            System.out.print(i+",");
        System.out.println();
        return seq;
    }

    //判断是否是偶数
    public static boolean sumIsEven(List<Byte> list){
        int result=0;
        for(byte bit:list)
            result+=bit;
        return result%2==0;
    }

    //判断是否是偶数
    public static boolean sumIsEven(List<Byte> list,int[] index){
        int result=0;
        for(int i:index)
            result+=list.get(i);
        return result%2==0;
    }

    public static float getMagnitude(float[] vector){
        float temp=0;
        for(int i=0;i<vector.length;i++){
            temp+=vector[i]*vector[i];
        }
        return (float)Math.sqrt(temp);
    }
    //归一化
    public static float[] norm(float[] vec){
        float[] result=new float[vec.length];
        float len=Utils.getMagnitude(vec);
        for(int i=0;i<vec.length;i++){
            result[i]=vec[i]/len;
        }
        return result;
    }
    //计算Pearson相似度
    public static float cross_correlation(List<Float> ss1, List<Float> ss2){
        float mean1=0,mean2=0;
        for(int i=0;i<ss1.size()&&i<ss2.size();i++){
            mean1+=ss1.get(i);
            mean2+=ss2.get(i);
        }
        mean1/=ss1.size();
        mean2/=ss2.size();
        float lxy=0,lxx=0,lyy=0;
        for(int i=0;i<ss1.size()&&i<ss2.size();i++){
            lxy+=(ss1.get(i)-mean1)*(ss2.get(i)-mean2);
            lxx+=(ss1.get(i)-mean1)*(ss1.get(i)-mean1);
            lyy+=(ss2.get(i)-mean2)*(ss2.get(i)-mean2);
        }
        return lxy/((float)Math.sqrt(lxx*lyy));
    }
    //计算数组的Pearson相似度
    public static float array_cross_correlation(List<float[]> s1,List<float[]> s2){
        float[] means1=new float[s1.get(0).length];
        float[] means2=new float[s2.get(0).length];
        for(int i=0;i<s1.size()&&i<s2.size();i++){
            for(int j=0;j<means1.length;j++){
                means1[j]+=s1.get(i)[j];
                means2[j]+=s2.get(i)[j];
            }
        }
        for(int i=0;i<means1.length;i++){
            means1[i]/=s1.size();
            means2[i]/=s2.size();
        }
        float[] lxy=new float[means1.length];
        float[] lxx=new float[means1.length];
        float[] lyy=new float[means1.length];
        for(int i=0;i<s1.size()&&i<s2.size();i++){
            for(int j=0;j<lxx.length;j++){
                lxy[j]+=(s1.get(i)[j]-means1[j])*(s2.get(i)[j]-means2[j]);
                lxx[j]+=(s1.get(i)[j]-means1[j])*(s1.get(i)[j]-means1[j]);
                lyy[j]+=(s2.get(i)[j]-means2[j])*(s2.get(i)[j]-means2[j]);
            }
        }
        float result=0;
        float[] c=new float[means1.length];
        for(int i=0;i<means1.length;i++){
            c[i]=/*Math.abs*/(lxy[i]/((float)Math.sqrt(lxx[i]*lyy[i])));
            result+=c[i];
            //result+=/*Math.abs*/(lxy[i]/((float)Math.sqrt(lxx[i]*lyy[i])));
        }
        System.out.println(String.format("c:%f,%f,%f",c[0],c[1],c[2]));
        return result/means1.length;
    }

    public static float weight_array_cross_correlation(List<float[]> s1,List<float[]> s2){
        float[] means1=new float[s1.get(0).length];
        float[] means2=new float[s2.get(0).length];
        float[] weight=new float[means1.length];
        for(int i=0;i<s1.size()&&i<s2.size();i++){
            for(int j=0;j<means1.length;j++){
                means1[j]+=s1.get(i)[j];
                means2[j]+=s2.get(i)[j];
                weight[j]+=Math.abs(s1.get(i)[j])+Math.abs(s2.get(i)[j]);
            }
        }
        for(int i=0;i<means1.length;i++){
            means1[i]/=s1.size();
            means2[i]/=s2.size();
        }
        float[] lxy=new float[means1.length];
        float[] lxx=new float[means1.length];
        float[] lyy=new float[means1.length];
        for(int i=0;i<s1.size()&&i<s2.size();i++){
            for(int j=0;j<lxx.length;j++){
                lxy[j]+=(s1.get(i)[j]-means1[j])*(s2.get(i)[j]-means2[j]);
                lxx[j]+=(s1.get(i)[j]-means1[j])*(s1.get(i)[j]-means1[j]);
                lyy[j]+=(s2.get(i)[j]-means2[j])*(s2.get(i)[j]-means2[j]);
            }
        }
        float result=0;
        float[] c=new float[means1.length];
        float sumWeight=0;
        for(float r:weight)
            sumWeight+=r;
        for(int i=0;i<means1.length;i++){
            c[i]=/*Math.abs*/(lxy[i]/((float)Math.sqrt(lxx[i]*lyy[i])))*(weight[i]/sumWeight);
            result+=c[i];
            //result+=/*Math.abs*/(lxy[i]/((float)Math.sqrt(lxx[i]*lyy[i])));
        }
        return result;
    }
    //向量点乘
    public static float dot_product(float[] vec1,float[] vec2){
        float result=0;
        for(int i=0;i<vec1.length&&i<vec2.length;i++){
            result+=vec1[i]*vec2[i];
        }
        return result;
    }
    //向量叉乘
    public static float[] cross_product_3(float[] a,float[] b){
        float[] result=new float[a.length];
        result[0]=a[1]*b[2]-a[2]*b[1];
        result[1]=a[2]*b[0]-a[0]*b[2];
        result[2]=a[0]*b[1]-a[1]*b[0];
        return result;
    }
    //向量乘常数
    public static float[] vector_multiple_constant(float[] a,float constant){
        float[] result=new float[a.length];
        for(int i=0;i<a.length;i++){
            result[i]=a[i]*constant;
        }
        return result;
    }
    //向量加法
    public static float[] vector_add(float[] a,float[] b){
        if(a.length!=b.length)
            return null;
        float[] result=new float[a.length];
        for(int i=0;i<a.length;i++){
            result[i]=a[i]+b[i];
        }
        return result;
    }
    //向量减法
    public static float[] vector_minus(float[] a,float[] b){
        if(a.length!=b.length)
            return null;
        float[] result=new float[a.length];
        for(int i=0;i<a.length;i++){
            result[i]=a[i]-b[i];
        }
        return result;
    }

    public static float[] matrixMultiplication(float[] a, float[] b)
    {
        float[] result = new float[9];

        result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
        result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
        result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

        result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
        result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
        result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

        result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
        result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
        result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

        return result;
    }

    public static float[] matrixMultiVector(float[] a, float[] b)
    {
        float[] result = new float[3];
        result[0] = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        result[1] = a[3] * b[0] + a[4] * b[1] + a[5] * b[2];
        result[2] = a[6] * b[0] + a[7] * b[1] + a[8] * b[2];
        return result;
    }
}
