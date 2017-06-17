package com.example.pc.silentmusicparty.Service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class StepCountService extends Service implements SensorEventListener {
    private boolean isRunning = false;
    private long steps;
    private Sensor stepSensor;
    private SensorManager sManager;


    @Override
    public void onCreate() {
        super.onCreate();
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        if (isRunning)
            return;
        registerListeners();
        isRunning = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        float[] values = event.values;
        if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            steps++;
        }

        Intent intent = new Intent();
        intent.setAction("com.example.pc.silentmusicparty.STEP_COUNT");
        intent.putExtra("STEP_COUNT",String.valueOf(steps));
        sendBroadcast(intent);
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerListeners() {
        Log.i("Main","registered");
        sManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unregisterListeners() {
        Log.i("Main","unregistered");
        sManager.unregisterListener(this, stepSensor);
        isRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterListeners();
        isRunning = false;
    }

}
