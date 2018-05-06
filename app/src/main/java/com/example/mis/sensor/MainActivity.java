package com.example.mis.sensor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.HorizontalScrollView;

import com.example.mis.sensor.FFT;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private List<Entry> xValueList = new ArrayList<>();
    private List<Entry> yValueList = new ArrayList<>();
    private List<Entry> zValueList = new ArrayList<>();
    private List<Entry> magnitudeList = new ArrayList<>();
    private Integer axisEntryIndex = 0;
    private static int chartListSize = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Log.d("@@@@@@", "In Create");
        inititalise();

        //initiate and fill example array with random values
        rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues);
    }

    /*
        @Purpose: To initialise the app
                    Check for permissions, if not present ask for permission
     */
    public void inititalise(){
        getSensorData();
    }
    /*
    @Purpose: To get data from Motion Sensor -
     */
    public void getSensorData(){
        boolean isSucess = initialiseSensors();
        Log.d("@@@Is Sucess: " , String.valueOf(isSucess));
        if( !isSucess ){
            HelperClass.showToastMessage("The device sensor could not be acessed" +
                    " or there is no accelerometer present on device " , this);
        }
    }
    /*
        @Purpose: Initialise Sensor
     */
    public boolean initialiseSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null
            && mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            Log.d("####","In Sensor Initialisation");
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        updateChartData( event );
    }

    /*
        @Purpose: To plot data from accelerometer in grap-view
     */
    public void updateChartData( SensorEvent event ){
        axisEntryIndex += 1;
        float x,y,z,magnitude;
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        magnitude = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        Log.d("###x" , String.valueOf(x));
        Log.d("###y" , String.valueOf(y));
        Log.d("###z" , String.valueOf(z));
        if(xValueList.size() >= chartListSize)
            xValueList.remove(0);
        if(yValueList.size() >= chartListSize)
            yValueList.remove(0);
        if(zValueList.size() >= chartListSize)
            zValueList.remove(0);
        if( magnitudeList.size() >= chartListSize)
            magnitudeList.remove(0);
        xValueList.add( new Entry( axisEntryIndex, x));
        yValueList.add( new Entry( axisEntryIndex, y));
        zValueList.add( new Entry( axisEntryIndex, z));
        magnitudeList.add( new Entry( axisEntryIndex, magnitude));

        LineChart graph = (LineChart) findViewById(R.id.dataGraph);
        LineData lineData = new LineData();
        lineData.addDataSet(addLineData( xValueList, "X" , Color.RED));
        lineData.addDataSet(addLineData( yValueList, "Y" , Color.GREEN ));
        lineData.addDataSet(addLineData( zValueList, "Z" , Color.BLUE));
        lineData.addDataSet(addLineData( magnitudeList, "Magnitude" , Color.BLACK));

        graph.setData(lineData);
        graph.notifyDataSetChanged();
        graph.invalidate();
    }

    /*
        @Purpose: To create a data set for each line to be shown on graph
    */
    public LineDataSet addLineData( List<Entry> valueList, String label, int lineColor  ){
        LineDataSet lineDataSet = new LineDataSet(valueList, label);
        lineDataSet.setColor(lineColor);
        lineDataSet.setValueTextColor(lineColor);
        lineDataSet.setValueTextSize(10);
        Log.v("@@@@@@@@@@@2lineDataSet" , String.valueOf(lineDataSet));
        return lineDataSet;
    }

    public static void calculateFFT(){

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    /**
     * Implements the fft functionality as an async task
     * FFT(int n): constructor with fft length
     * fft(double[] x, double[] y)
     */

    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }
            return magnitude;
        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;
        }
    }

    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }
}
