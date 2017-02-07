package com.fedorkzsoft.speedometer;

import android.content.PeriodicSync;

public class NoiseGen {

	private static final int N = 5;
	static float[] periods;
	private static int mLastTime;
	private static float mLastVal;
	private static float mLastSpeed;
	private static float mLastAccel;
	
	public static void init(){
		periods = new float[N];
		
		periods[0] = 1000;
		periods[1] = 5000;
		periods[2] = 6000;
		periods[3] = 8000;
		periods[4] = 9000;
	}
	
	public static double getValue(long t){
		if (periods == null)
			init();
		
		int it = (int)(t & 0xFFFFFFFF);

		float v = 0;
		
		for (int i=0; i<N; i++){
			v += (1 + Math.sin((float)it / periods[i]));
			periods[i] += Math.random() * 100 - 50;
		}
		
		if (mLastTime == 0)
			mLastTime = it;
		
		float dt = ((float)it - mLastTime) / 1000f;
		
		mLastAccel = (float) (Math.random() - 0.5);
		mLastSpeed += mLastAccel * dt;
		
		mLastVal += mLastSpeed * dt;
		
		if (mLastVal <=0){
			mLastVal = 0;
			mLastAccel = 0;
			mLastSpeed = 0;
		}
		
		if (mLastVal >=1){
			mLastVal = 1;
			mLastAccel = 0;
			mLastSpeed = 0;
		}
		mLastTime = it;
		return mLastVal;
	}
}
