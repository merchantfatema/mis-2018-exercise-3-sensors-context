/*
 * Name: Exercise 3
 * Created Date: 10 May, 2018
 * Purpose: Accelerometer & FFT
 * Student Name: Fatema Merchant
 * Student Id: 119431
 * */
package com.example.mis.sensor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener,
                            SeekBar.OnSeekBarChangeListener, LocationListener {

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
    private TextView sampleRateValue, fftSeekBarValue,locationSpeed;
    private int sampleRate,axisEntryIndex = 0, wSize, magnitudeIndex = 0;
    private double[] magnitudeArray = new double[wSize];
    private MediaPlayer musicJogging, musicBiking;
    private LocationManager mLocationManager;
    Boolean isProviderEnabled;
    boolean verifyLoc;


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

        sampleRate = 100000; //in microseconds = 0.1 seconds
        wSize = 8;
        initialiseSeekBarAndLabel();
        getSensorData();
        inititaliseMediaPlayer();
        initialiseLocationService();
        locationSpeed = (TextView) findViewById(R.id.locationSpeed);
        locationSpeed.setText("0");
    }

    /*
        @Purpose: To update variable to check or not to check location verifiaction
     */
    public void onCheckboxClicked( View v ){
        verifyLoc = ((CheckBox) v).isChecked();
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
        sampleRateValue.setText("0.1");
        fftSeekBarValue.setText(String.valueOf(wSize));
        sampleRateSeekBar.incrementProgressBy(1);

        sampleRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if( progress == 0 ){
                    progress = 1;
                }
                //progress = progress * 1000;
                float temp = progress;
                temp /= 10;
                sampleRateValue.setText(String.valueOf(temp));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {

                int progress = seekBar.getProgress();
                if( progress == 0 ){
                    progress = 1;
                }
                //progress = progress*1000;
                float temp = progress;
                temp /= 10;
                seekBar.setProgress( progress );
                sampleRate = progress * 100000;
                Log.d("###sampleRate: " , String.valueOf(temp));
                mSensorManager.unregisterListener(MainActivity.this);
                mSensorManager.registerListener(MainActivity.this, mSensor, sampleRate);
                updateChartData();
            }
        });

        fftWindowSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if( progress == 0 ){
                    progress = 1;
                }
                int tempSize = (int) Math.pow(2, progress);;
                fftSeekBarValue.setText(String.valueOf(tempSize));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();

                wSize = (int) Math.pow(2, progress);
                fftSeekBarValue.setText(String.valueOf(wSize));

                if( magnitudeIndex > wSize){
                    magnitudeArray = new double[wSize];
                    magnitudeIndex = 0;
                }
                else if( magnitudeIndex < wSize){

                    double[] tempArray = new double[wSize];
                    tempArray = magnitudeArray;

                    magnitudeArray = new double[wSize];
                    for( int i = 0; i <= magnitudeIndex; i++ ){
                        magnitudeArray[i] = tempArray[i];
                    }
                }
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
        }
        isProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if( isProviderEnabled ){
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", true);
            //Log.d("########","Inside");
        }
        //Log.d("### GPS_PROVIDER:" , String.valueOf(isProviderEnabled));
    }
    /*
        @Purpose: Initialise Sensor
     */
    public boolean initialiseSensors(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null
            && mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null){
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mSensorManager.registerListener(this, mSensor, sampleRate);
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if( magnitudeIndex == wSize){
            FFTAsynctask fftAsynctask = new FFTAsynctask(wSize);
            fftAsynctask.execute(magnitudeArray);
        }
        updateChartData( event );
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

        if( magnitudeIndex < magnitudeArray.length){
            if(magnitudeArray.length <= wSize){
                magnitudeArray[magnitudeIndex] =  magnitude;
                magnitudeIndex++;
            }
        }
        else{
            magnitudeIndex = 0;
            magnitudeArray = new double[wSize];
        }
        axisEntryIndex += 1;
        xValueList.add( new Entry( axisEntryIndex, x));
        yValueList.add( new Entry( axisEntryIndex, y));
        zValueList.add( new Entry( axisEntryIndex, z));
        magnitudeList.add( new Entry( axisEntryIndex, magnitude));

        updateChartData();
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
        lineData.addDataSet(addLineData( magnitudeList, "Magnitude" , Color.WHITE));
        graph.setBackgroundColor(Color.DKGRAY);
        graph.setDescription(new Description());
        graph.setGridBackgroundColor(Color.BLACK);
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
        fftGraph.setDescription(new Description());

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

    private float getLocationSpeed(){
        @SuppressWarnings({"ResourceType"})
        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //Log.d("@@@@location:" , String.valueOf(location));
//        Log.d("@@@@location HAS SPEED:" , String.valueOf(location.hasSpeed()));
        float speed1 = 9999;
        if (location != null && location.hasSpeed()) {
            speed1 = (int) (location.getSpeed() * 3.6);
            //Log.d("@@@@@speed1: " , String.valueOf(speed1));
            locationSpeed.setText(String.valueOf(speed1));
        }
        return speed1;
    }

    private void updateMusic(){
        double[] freqCountsTemp;
        double avgFreq = 0.0;
        freqCountsTemp = freqCounts;
        //avgFreq = freqCountsTemp[freqCountsTemp.length -1];
        for (int i=0; i< freqCountsTemp.length; i++){
            avgFreq += freqCountsTemp[i];
            avgFreq = avgFreq/freqCountsTemp.length;
        }
        Log.d("####avgFreq:" , String.valueOf(avgFreq));

        float locSpeed = getLocationSpeed();
        //Log.d("####verifyLoc:" , String.valueOf(verifyLoc));
        if(verifyLoc && isProviderEnabled && locSpeed != 9999){
            if((locSpeed >= 2.7 && locSpeed < 5.6 && avgFreq >= 15 && avgFreq <= 25)){ //If biking
                //https://en.wikipedia.org/wiki/Bicycle_performance
                if(!musicBiking.isPlaying()){ musicBiking.start(); }
                pauseJogMusic();
            }
            else if( locSpeed < 2.7 && locSpeed >= 1.3 && avgFreq < 15 && avgFreq >= 5 ){//If jogging
                //https://www.curejoy.com/content/average-jogging-speed/
                if(!musicJogging.isPlaying()){ musicJogging.start();}
                pauseBikeMusic();
            }
            else if( locSpeed == 0 && avgFreq < 5 && avgFreq > 25){ //If not jogging or biking
                //Have kept minimum avgFreq as 0.9 as person can become slower while doing an activity.
                pauseJogMusic();
                pauseBikeMusic();
            }
        }
        else{
            if(  avgFreq <= 25 && avgFreq >= 15 ){ //Person is biking
                if(!musicBiking.isPlaying()){ musicBiking.start(); }
                pauseJogMusic();
            }
            else if( avgFreq < 15 && avgFreq >= 2 ){
                if(!musicJogging.isPlaying()){ musicJogging.start();}
                pauseBikeMusic();
            }
            else if( avgFreq < 2 || avgFreq > 25){ //If not jogging or biking
                //Have ket minimum avgFreq as 0.9 as person can become slower while doing an activity.
                new java.util.Timer().schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                // your code here
                                pauseJogMusic();
                                pauseBikeMusic();
                            }
                        },
                        2000
                );
            }
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

        mSensorManager.unregisterListener(MainActivity.this);
        musicJogging.pause();
        musicBiking.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(MainActivity.this, mSensor, sampleRate);
    }

    @Override
    public void onLocationChanged(Location location) {
        /*if( location != null ){
            speed = location.getSpeed();
            Log.d("$$$$$speed: " , String.valueOf(speed));
            Double tempSpeed = speed;
            if( tempSpeed == null ){

                tempSpeed = 0.0;
            }
            locationSpeed.setText("onLocation Changed:" + String.valueOf(tempSpeed));
        }
        else{
            speed = 0.0;
        }*/
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

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
