package com.example.firstapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.SearchEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static boolean buttonClicked = false;
    public static boolean showAcc = false;
    private SensorManager sensorManager;
    private Sensor sensor;
    private Sensor accSensor;
    private Sensor gyroSensor;
    private Sensor magSensor;
    private Sensor luxSensor;
    private Long startTime;
    private Map<Long,SensorData> dataMap = new TreeMap<>();
    Long dumpThreshold = 500L;
    Boolean heading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button btnClick = (Button) findViewById(R.id.btnClick);
        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText("Hello");
                v.setBackgroundColor(Color.parseColor("#ff00ff"));
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        luxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, luxSensor, SensorManager.SENSOR_DELAY_FASTEST);
        startTime = System.currentTimeMillis();

        Button btnAcc = (Button) findViewById(R.id.btnAcc);
        btnAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                //txtStatus.setText("World!");
                showAcc = true;

            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorData sensorData = dataMap.getOrDefault(event.timestamp, new SensorData());
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorData.setAcc_x(event.values[0]);
            sensorData.setAcc_y(event.values[1]);
            sensorData.setAcc_z(event.values[2]);
            dataMap.put(event.timestamp, sensorData);
//            System.out.println("ACC:"+getTimeStamp().toString()+" :: "+ Arrays.toString(event.values));
        } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorData.setGyro_x(event.values[0]);
            sensorData.setGyro_y(event.values[1]);
            dataMap.put(event.timestamp, sensorData);
//            System.out.println("GYRO:"+getTimeStamp().toString()+" :: "+ Arrays.toString(event.values));
        } else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorData.setMag_x(event.values[0]);
            sensorData.setMag_y(event.values[1]);
            sensorData.setMag_z(event.values[2]);
            dataMap.put(event.timestamp, sensorData);
//            System.out.println("MAG:"+getTimeStamp().toString()+" :: "+ Arrays.toString(event.values));
        } else if(event.sensor.getType() == Sensor.TYPE_LIGHT) {
            sensorData.setLux(event.values[0]);
            dataMap.put(event.timestamp, sensorData);
//            System.out.println("LIGHT:"+getTimeStamp().toString()+" :: "+event.values[2]);
        }
        flushData(event.timestamp);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private Long getTimeStamp() {
        return System.currentTimeMillis() - this.startTime;
    }

    private void flushData(Long timeStamp) {
        String[] header = {"Timestamp", "Accel X", "Accel Y", "Accel Z", "Gyro X", "Gyro Y", "Mag X", "Mag Y", "Mag Z", "Light Intensity"};
        if(dataMap.size() >= dumpThreshold) {
            List<String[]> csvData = new ArrayList<>();
            if(heading) {
                csvData.add(header);
            }
            for (Map.Entry<Long, SensorData> entry: dataMap.entrySet()) {
                SensorData currentData = entry.getValue();
                Instant instant = Instant.ofEpochMilli (entry.getKey());
                ZonedDateTime zdt = ZonedDateTime.ofInstant ( instant , ZoneOffset.UTC );
                String time = DateTimeFormatter.ofPattern("HH:mm:ss:SSS").format(zdt);

                String[] row = {entry.getKey().toString(), Objects.toString(currentData.getAcc_x()), Objects.toString(currentData.getAcc_y()), Objects.toString(currentData.getAcc_z()),
                        Objects.toString(currentData.getGyro_x()), Objects.toString(currentData.getGyro_y()), Objects.toString(currentData.getMag_x()), Objects.toString(currentData.getMag_y()),
                        Objects.toString(currentData.getMag_z()), Objects.toString(currentData.getLux())};
                csvData.add(row);
            }

            String filePath = "datadump.csv";
//            File file = new File(filePath);
//            if(!file.exists()) {
//                try {
//                    file.createNewFile();
//                } catch (IOException e) {
//                    System.out.println("Failed to create new file to write the sensor values");
//                    e.printStackTrace();
//                }
//            }


//            final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/sensorData/");
//            if(!path.exists()) {
//                path.mkdirs();
//            }
//            final File file = new File(path, "data.csv");
            File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/dummy.csv");
            if(!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    System.out.println("Failed to create new file to write the sensor values");
                    e.printStackTrace();
                }
            }

            try (CSVWriter writer = new CSVWriter(new FileWriter(file, true))) {
                System.out.println("WRITING data to csv");;
                writer.writeAll(csvData);
                dataMap.clear();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("FAILED TO WRITE THE DUMP TO CSV FILE");
            }
        }
    }
}
