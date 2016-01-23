/*
 * Copyright 2012 Kexanoids Lab. 
 * 
 * This file has been written for Kexanoids Lab blog http://kexanoids.blogspot.com/.
 * Feel free to modify, reuse or play with it without any limitation.
 * 
 * Author      : Victor Zaitsev (Melnosta)
 */
package com.fedorkzsoft.speedometer;

import java.util.Timer;
import java.util.TimerTask;

import com.fedorkzsoft.speedometer.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

/**
 * Activity that performs canvas painting in the same thread.
 */
public class DemoActivity extends Activity {

    private SpeedometerView surfaceView;
    private Timer timer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean enableAccelerationFlag = true;//getIntent().getExtras().getBoolean(MainActivity.HW_ENABLED_KEY);
        if (enableAccelerationFlag) {
            // Build target in AndroidManifest.xml and/or Eclipse must be 11 or higher to compile this code.
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        setContentView(R.layout.speedometer_demo);

        surfaceView = (SpeedometerView) findViewById(R.id.single_thread_view);
        surfaceView.setWillNotDraw(false); // Enable calling onDraw() method that is switched off by default. 
    }

    @Override
    protected void onResume() {
        super.onResume();
        surfaceView.onResume();

        startTimer();
        startGasTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surfaceView.onPause();

        stopTimer();
    }

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                surfaceView.postInvalidate();
            }
        }, 1, 1);
    }
    Handler mHandler = new Handler();
    
    private void startGasTimer() {
    	mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
	              long t = System.currentTimeMillis();
	              double ns = NoiseGen.getValue(t);
	              surfaceView.setValue((float) (ns * 181));
	              
	              startGasTimer();
			}
		}, (int)(100f * Math.random()));
    }
    
    private void stopTimer() {
        timer.cancel();
    }
}