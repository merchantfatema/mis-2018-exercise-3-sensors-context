package com.example.mis.sensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener,
                            SeekBar.OnSeekBarChangeListener {

    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private List<Entry> xValueList = new ArrayList<>();
    private List<Entry> yValueList = new ArrayList<>();
    private List<Entry> zValueList = new ArrayList<>();
    private List<Entry> magnitudeList = new ArrayList<>();
    private static int chartListSize = 10;
    private SeekBar sampleRateSeekBar, fftWindowSizeSeekBar;
    private TextView sampleRateValue, fftSeekBarValue;
    private int sampleRate, fftWindowSize;
    private int axisEntryIndex = 0, wSize = 64, magnitudeIndex = 0;
    private double[] magnitudeArray = new double[wSize];
    private MediaPlayer musicJogging, musicBiking;
    private LocationManager mLocationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inititalise();

        //initiate and fill example array with random values
        /*rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues);*/
    }

    /*
        @Purpose: To initialise the app
                    Check for permissions, if not present ask for permission
     */
    public void inititalise(){
        initialiseSeekBarAndLabel();
        getSensorData();
        inititaliseMediaPlayer();
        initialiseLocationService();
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
        @Purpose: To initialise the Seek Bar and its value labels
     */
    public void initialiseSeekBarAndLabel(){
        sampleRateSeekBar = (SeekBar)findViewById(R.id.sampleRate);
        fftWindowSizeSeekBar = (SeekBar)findViewById(R.id.fftWindowSize);
        sampleRateValue = (TextView) findViewById(R.id.sampleRateValue);
        fftSeekBarValue = (TextView) findViewById(R.id.fftSeekBarValue);
        sampleRate = sampleRateSeekBar.getProgress();
        sampleRateValue.setText(String.valueOf(sampleRate));
        fftSeekBarValue.setText(String.valueOf(fftWindowSizeSeekBar.getProgress()));
        sampleRateSeekBar.incrementProgressBy(10000);

        sampleRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if( progress == 0 ){
                    progress = 1;
                }
                sampleRateValue.setText(String.valueOf(progress));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {

                int progress = seekBar.getProgress();
                if( progress == 0 ){
                    progress = 1;
                }
                updateChartData();
                seekBar.setProgress(progress );
                sampleRate = progress;
                mSensorManager.unregisterListener(MainActivity.this);
                mSensorManager.registerListener(MainActivity.this, mSensor, sampleRate);
            }
        });

        fftWindowSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if( progress == 0 ){
                    progress = 1;
                }
                fftSeekBarValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                fftSeekBarValue.setText(String.valueOf(progress));
                wSize = (int) Math.pow(2, progress);;
                magnitudeArray = new double[wSize];
            }
        });
    }

    /*
        @Purpose: To initialise media for playing music while joggine/biking
    */
    public void inititaliseMediaPlayer(){
        musicJogging = MediaPlayer.create(this, R.raw.dilbaro);
        musicBiking = MediaPlayer.create(this, R.raw.total_breakdown);
    }

    public void initialiseLocationService(){
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION )
                != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            HelperClass.showToastMessage("No location permission" , this);
            ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            String locationProvider = LocationManager.GPS_PROVIDER;

        }
    }
    /*
        @Purpose: Initialise Sensor
     */
    public boolean initialiseSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null
            && mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if( magnitudeIndex <= wSize){
            updateChartData( event );
            FFTAsynctask fftAsynctask = new FFTAsynctask(wSize);
            fftAsynctask.execute(magnitudeArray);
        }
        else{
            magnitudeArray = new double[wSize];
            magnitudeIndex = 0;
        }
    }

    /*
        @Purpose: To plot data from accelerometer in grap-view
     */
    public void updateChartData( SensorEvent event ){

        float x,y,z,magnitude;
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        magnitude = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));

        if(xValueList.size() >= chartListSize)
            xValueList.remove(0);
        if(yValueList.size() >= chartListSize)
            yValueList.remove(0);
        if(zValueList.size() >= chartListSize)
            zValueList.remove(0);
        if( magnitudeList.size() >= chartListSize)
            magnitudeList.remove(0);

        magnitudeArray[magnitudeIndex] =  magnitude;
        axisEntryIndex += 1;
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
        @Purpose: To update the accelerometer chart with new data
     */
    public void updateChartData(){
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
        @Purpose: To update the FFT chart with new data
     */
    public void updateFFTChart(){
        LineChart fftGraph = (LineChart) findViewById(R.id.fftGraph);
        List<Entry> magnitudeEntryList = new ArrayList<>();
        for (int i = 0; i < freqCounts.length; i++){
            magnitudeEntryList.add( new Entry(i, (float) freqCounts[i]));
        }
        LineData lineData = new LineData();
        lineData.addDataSet(addLineData( magnitudeEntryList, "X" , Color.BLACK));
        fftGraph.setData(lineData);
        fftGraph.notifyDataSetChanged();
        fftGraph.invalidate();

        updateMusic();
    }

    /*
        @Purpose: To create a data set for each line to be shown on graph
        @Return: LineDataSet
    */
    public LineDataSet addLineData( List<Entry> valueList, String label, int lineColor  ){
        LineDataSet lineDataSet = new LineDataSet(valueList, label);
        lineDataSet.setColor(lineColor);
        lineDataSet.setValueTextColor(lineColor);
        lineDataSet.setValueTextSize(10);
        return lineDataSet;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

    @Override public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override public void onStopTrackingTouch(SeekBar seekBar) { }

    private void updateMusic(){
        double[] freqCountsTemp;
        double avgFreq = 0.0;
        freqCountsTemp = freqCounts;
        avgFreq = freqCountsTemp[freqCountsTemp.length -1];
        /*for (int i=0; i< freqCountsTemp.length; i++){
            avgFreq += freqCountsTemp[i];
            avgFreq = avgFreq/freqCountsTemp.length;
        }*/
        Log.d("####avgFreq:" , String.valueOf(avgFreq));
        if( avgFreq > 2){
            musicJogging.start();
            pauseBikeMusic();
        }
        else if( avgFreq <= 2 && avgFreq >= 0.9){
            musicBiking.start();
            pauseJogMusic();
        }
        else if( avgFreq <= 0){
            pauseJogMusic();
            pauseBikeMusic();
        }
    }

    private void pauseJogMusic(){
        if( musicJogging.isPlaying()){
            musicJogging.pause();
        }
    }

    private void pauseBikeMusic(){
        if(musicBiking.isPlaying()){
            musicBiking.pause();
        }
    }
    //When user moves out of the app, stop playing the music & unregister the sensor
    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

        // Release the Camera because we don't need it when paused
        // and other activities might need to use it.
        mSensorManager.unregisterListener(MainActivity.this);
        musicJogging.pause();
        musicBiking.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(MainActivity.this, mSensor, sampleRate);
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
            updateFFTChart();
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
