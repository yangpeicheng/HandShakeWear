package com.ypc.handshakewear;

import com.ypc.handshakewear.sensor.SensorData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangpc on 2017/7/19.
 */
public class TrainTools {
    protected static final float NS2S = 1.0f / 1000000000.0f;
    public float calculate_correlation(List<SensorData> list1, List<SensorData> list2, float initTheta1, float initTheta2){
        List<float[]> s1=getGlobalAcceleration(list1,initTheta1);
        List<float[]> s2=getGlobalAcceleration(list2,initTheta2);
        return Utils.array_cross_correlation(s1,s2);
    }
    public  List<float[]> getGlobalAcceleration(List<SensorData> list,float theta){
        long lastTimetamp=0;
        float[] lastGacc=Utils.norm(list.get(0).getGravity());
        float[] initMatrix=getInitMatrix(lastGacc,theta);
        float[] currentMatrix=initMatrix;
        System.out.println("magnitude:"+Utils.getMagnitude(initMatrix));
        float[] preMatrix=initMatrix;
        List<float[]> globalAcc=new ArrayList<>();
        for (int i=0;i<list.size();i++){
            SensorData sensorData=list.get(i);
            if(i==0){
                lastTimetamp=sensorData.getTimestamp();
            }
            else {
                if(sensorData.getTimestamp()==lastTimetamp)
                    continue;
                float dt=(sensorData.getTimestamp()-lastTimetamp)*NS2S;
                //System.out.println(dt);
                lastTimetamp=sensorData.getTimestamp();
                currentMatrix=updateMatrix(currentMatrix,sensorData.getGyroscope(),dt);
                if(i%50==0){
                    float[] currentGacc=sensorData.getGravity();
                    float[] accMatrix=getCarlibratedMatrix(lastGacc,currentGacc);
                    currentMatrix=carlibrate(Utils.matrixMultiplication(preMatrix,accMatrix),currentGacc,currentMatrix);
                    lastGacc=currentGacc;
                    preMatrix=currentMatrix;
                }
            }

            //System.out.println(sensorData.getAcceleration()[0]);
            globalAcc.add(Utils.matrixMultiVector(currentMatrix,sensorData.getLinearAcceleration()));
        }
        return globalAcc;
    }
    private float[] getInitMatrix(float[] gacc,float initTheta){
        float fi=(float) (Math.PI/2-Math.atan2(Math.sqrt(gacc[0]*gacc[0]+gacc[1]*gacc[1]),gacc[2]));
        float[] x=convertFromSphericalToCardinal(1,fi,initTheta);
        float[] y=Utils.cross_product_3(gacc,x);
        y=Utils.norm(y);
        return new float[]{
                x[0],x[1],x[2],
                y[0],y[1],y[2],
                gacc[0],gacc[1],gacc[2]
        };
    }
    //极坐标到笛卡尔坐标转化
    private float[] convertFromSphericalToCardinal(float r,float fi,float theta){
        float[] result=new float[3];
        result[0]=r*(float)(Math.sin(fi)*Math.cos(theta));
        result[1]=r*(float)(Math.sin(fi)*Math.sin(theta));
        result[2]=r*(float)Math.cos(fi);
        return result;
    }

    private float[] updateMatrix(float[] cuMatrix,float[] gyroscope,float dt){
        float delta=(float)Math.sqrt((gyroscope[0]*dt)*(gyroscope[0]*dt)+(gyroscope[1]*dt)*(gyroscope[1]*dt)+(gyroscope[2]*dt)*(gyroscope[2]*dt));
        float[] B=new float[]{
                0,-gyroscope[2]*dt,gyroscope[1]*dt,
                gyroscope[2]*dt,0,-gyroscope[0]*dt,
                -gyroscope[1]*dt,gyroscope[0]*dt,0
        };
        float[] B1=Utils.vector_multiple_constant(B,(float)Math.sin(delta)/delta);
        float[] B2=Utils.vector_multiple_constant(Utils.matrixMultiplication(B,B),(1-(float)Math.cos(delta))/(delta*delta));
        float[] matrix=Utils.vector_add(Utils.vector_add(Utils.I,B1),B2);
        return Utils.matrixMultiplication(cuMatrix,matrix);
    }

    private float[] carlibrate(float[] preMatrix,float[] rotationAxis,float[] currentMatrix){
        float minDistance=100000;
        float[] bestRm=null;
        int gap=1;
        for(int i=0;i<360;i+=gap){
            float[] rm=getRotationMatrixFromAngle(rotationAxis,i*180/(float)Math.PI);
            float distance=Utils.getMagnitude(Utils.vector_minus(Utils.matrixMultiplication(preMatrix,rm),currentMatrix));
            if(distance<minDistance){
                minDistance=distance;
                bestRm=Utils.matrixMultiplication(preMatrix,rm);
            }
        }

        return bestRm;
    }

    private float[] getCarlibratedMatrix(float[] preGacc,float[] currentGacc){
        float[] rotationAxis=Utils.cross_product_3(preGacc,currentGacc);
        float angle=(float)Math.acos(Utils.dot_product(preGacc,currentGacc)/(Utils.getMagnitude(preGacc)*Utils.getMagnitude(currentGacc)));
        return getRotationMatrixFromAngle(rotationAxis,angle);
    }

    /**
     * \    cos+x^2(1-cos)     xy(1-cos)-zsin      xz(1-cos)+ysin   \
     * \    yx(1-cos)+zsin      cos+y^2(1-cos)      yz(1-cos)-xsin  \
     * \    zx(1-cos)-ysin      zy(1-cos)+xsin      cos+z^2(1-cos)  \
     * @param axis
     * @param angle
     * @return
     */
    private float[] getRotationMatrixFromAngle(float[] axis,float angle){
        axis=Utils.norm(axis);
        float[] rotationMaxtrix=new float[9];
        float cos=(float)Math.cos(angle),sin=(float)Math.sin(angle);
        rotationMaxtrix[0]=cos+axis[0]*axis[0]*(1-cos);
        rotationMaxtrix[1]=axis[0]*axis[1]*(1-cos)-axis[2]*sin;
        rotationMaxtrix[2]=axis[1]*sin+axis[0]*axis[2]*(1-cos);

        rotationMaxtrix[3]=axis[2]*sin+axis[0]*axis[1]*(1-cos);
        rotationMaxtrix[4]=cos+axis[1]*axis[1]*(1-cos);
        rotationMaxtrix[5]=-axis[0]*sin+axis[1]*axis[2]*(1-cos);

        rotationMaxtrix[6]=-axis[1]*sin+axis[0]*axis[2]*(1-cos);
        rotationMaxtrix[7]=axis[0]*sin+axis[1]*axis[2]*(1-cos);
        rotationMaxtrix[8]=cos+axis[2]*axis[2]*(1-cos);
        return rotationMaxtrix;
    }
}
