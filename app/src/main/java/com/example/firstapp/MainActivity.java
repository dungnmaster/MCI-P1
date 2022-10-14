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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final Map<Long,SensorData> dataMap = new TreeMap<>();
    private final BlockingQueue<SensorData> dataQueue = new LinkedBlockingQueue<>();
    private Long dumpThreshold = 500L;
    private Boolean heading = true;
    private static Boolean recording = false;
    private static LocalDateTime recordingStartTime;
    private static String selectedActivity = "IDLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerButtons();
        registerSensors();
        startTime = System.currentTimeMillis();
        scheduleWriterThread();

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SensorData sensorData = dataMap.getOrDefault(event.timestamp, new SensorData(event.timestamp));
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorData.setAcc_x(event.values[0]);
            sensorData.setAcc_y(event.values[1]);
            sensorData.setAcc_z(event.values[2]);
//            dataMap.put(event.timestamp, sensorData);
            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add acceleration data to queue");
                e.printStackTrace();
            }
//            System.out.println("ACC:"+getTimeStamp().toString()+" :: "+ Arrays.toString(event.values));
        } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorData.setGyro_x(event.values[0]);
            sensorData.setGyro_y(event.values[1]);
//            dataMap.put(event.timestamp, sensorData);
            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add gyro data to queue");
                e.printStackTrace();
            }
//            System.out.println("GYRO:"+getTimeStamp().toString()+" :: "+ Arrays.toString(event.values));
        } else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorData.setMag_x(event.values[0]);
            sensorData.setMag_y(event.values[1]);
            sensorData.setMag_z(event.values[2]);
//            dataMap.put(event.timestamp, sensorData);
            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add mag data to queue");
                e.printStackTrace();
            }
//            System.out.println("MAG:"+getTimeStamp().toString()+" :: "+ Arrays.toString(event.values));
        } else if(event.sensor.getType() == Sensor.TYPE_LIGHT) {
            sensorData.setLux(event.values[0]);
//            dataMap.put(event.timestamp, sensorData);
            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add light data to queue");
                e.printStackTrace();
            }
//            System.out.println("LIGHT:"+getTimeStamp().toString()+" :: "+event.values[2]);
        }
//        flushData(event.timestamp);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerButtons() {
        Button btnClick = (Button) findViewById(R.id.btnClick);
        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText("Hello");
                v.setBackgroundColor(Color.parseColor("#ff00ff"));
            }
        });

        Button btnAcc = (Button) findViewById(R.id.btnAcc);
        btnAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView txtStatus = (TextView) findViewById(R.id.txtStatus);
                txtStatus.setText("World!");
                showAcc = true;

            }
        });

        Button btnStart = (Button) findViewById(R.id.recordStart);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording = true;
                recordingStartTime = LocalDateTime.now();
                btnStart.setEnabled(false);
            }
        });

        Button btnEnd = (Button) findViewById(R.id.recordEnd);
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recording = false;
                btnStart.setEnabled(true);
            }
        });

        Spinner dropdown = findViewById(R.id.spinner1);
        String[] options = new String[]{"IDLE", "WALKING", "RUNNING", "STAIRS", "JUMPING"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        dropdown.setAdapter(adapter);

        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                selectedActivity = item;
                System.out.println("SELECTED: "+selectedActivity);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void registerSensors() {
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
    }

    private Long getTimeStamp() {
        return System.currentTimeMillis() - this.startTime;
    }

    private String getFileName() {
        return selectedActivity + " "
                + DateTimeFormatter.ofPattern("dd-MM-yy HH;mm;ss").format(recordingStartTime) + ".csv";
    }

    private void writeToFile(List<SensorData> data, boolean useHeader, String filename) {
        String[] header = {"Timestamp", "Accel X", "Accel Y", "Accel Z", "Gyro X", "Gyro Y", "Mag X", "Mag Y", "Mag Z", "Light Intensity"};
        List<String[]> csvData = new ArrayList<>();
        Long lastKey = null;
        Map<Long, SensorData> mergedData = new TreeMap<>();

        for (SensorData currentData : data) {
            SensorData sensorData = mergedData.getOrDefault(currentData.getTimestamp(), currentData);
            sensorData.forwardFill(currentData);
            if(Objects.nonNull(lastKey) && mergedData.containsKey(lastKey) && !lastKey.equals(currentData.getTimestamp())) {
                sensorData.forwardFill(mergedData.get(lastKey));
            }
            lastKey = currentData.getTimestamp();
            mergedData.put(currentData.getTimestamp(), sensorData);
        }

        if(useHeader) {
            csvData.add(header);
        }
        for (Map.Entry<Long, SensorData> entry : mergedData.entrySet()) {
            SensorData currentData = entry.getValue();
            Instant instant = Instant.ofEpochMilli (currentData.getTimestamp());
            ZonedDateTime zdt = ZonedDateTime.ofInstant ( instant , ZoneOffset.UTC );
            String time = DateTimeFormatter.ofPattern("HH:mm:ss:SSS").format(zdt);

            String[] row = {Objects.toString(currentData.getTimestamp()), Objects.toString(currentData.getAcc_x()), Objects.toString(currentData.getAcc_y()),
                    Objects.toString(currentData.getAcc_z()), Objects.toString(currentData.getGyro_x()), Objects.toString(currentData.getGyro_y()),
                    Objects.toString(currentData.getMag_x()), Objects.toString(currentData.getMag_y()), Objects.toString(currentData.getMag_z()),
                    Objects.toString(currentData.getLux())};
            csvData.add(row);
        }

        File file = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + filename);
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

    private void scheduleWriterThread() {
        final Boolean[] useHeader = {true};
        final Integer[] idleCount = {0};

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
//                    System.out.println("EXECUTING THREAD : "+dataQueue.size());
                    List<SensorData> writableData = new ArrayList<>();
                    dataQueue.drainTo(writableData, 2000);

                    if(recording && writableData.size() > 0) {
                        String filePath = getFileName();
                        writeToFile(writableData, useHeader[0], filePath);
                        useHeader[0] = false;
                    } else if(recording) {
                        idleCount[0]++;
                    }

                    if(idleCount[0] <= 2) {
                        try {
//                            System.out.println("SLEEPING THREAD");
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            System.out.println("Writer thread failed to sleep");
                            e.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }
            }
        }).start();
    }
}
