package com.example.userstudy;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.List;

interface ISensorCallback
{
    void OnEnumerateSupportedSensor(List<Sensor> sensors);
    void OnSensorChanged(SensorEvent event);
}

/**
 * SensorManager class designed by WengYueTing 2020/10/20 - YTSensorManager
 * What a good name!
 */
public class YTSensorManager
{

    /**
     * Private Constants
     */
    private static final String TAG = "YTSensorManager";


    /**
     * Private Fields
     */
    private SensorManager manager;
    private SensorEventListener listener;
    private ISensorCallback callback;


    /**
     * Singleton Design Pattern
     */
    private YTSensorManager() {}
    private static YTSensorManager mInstance;
    public static YTSensorManager getInstance()
    {
        if(mInstance == null)
            mInstance = new YTSensorManager();
        return mInstance;
    }


    public List<Sensor> getSensorList() {
        return manager.getSensorList(Sensor.TYPE_ALL);
    }


    /**
     * Init the sensor setup.
     * @param activity
     */
    public void init(Activity activity) {
        manager = (SensorManager) activity.getSystemService(activity.SENSOR_SERVICE);
        listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                callback.OnSensorChanged(event);

            }

            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {

            }
        };

        List<Sensor> sensorList = getSensorList();
        callback.OnEnumerateSupportedSensor(sensorList);
        for(int i=0;i<sensorList.size();i++)
        {
            if(manager.getDefaultSensor(sensorList.get(i).getType()) != null)
                manager.registerListener(listener, manager.getDefaultSensor(sensorList.get(i).getType()), SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
            else
                Log.i(TAG, "This Device can not support " + sensorList.get(i).getName());
        }
    }


    /**
     * Set callback handler.
     * @param callback
     */
    public void setSensorCallback(ISensorCallback callback) {
        this.callback = callback;
    }

}
