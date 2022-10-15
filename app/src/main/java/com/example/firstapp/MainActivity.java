package com.example.firstapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.SearchEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Locale;
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
    private static int distanceTraversedInCm = 0;
    private Long lastAccelSensorDisplayRefresh, lastGyroSensorDisplayRefresh;
    private Long lastMagSensoryDisplayRefresh, lastLightIntensitySensorDisplayRefresh;

    private TextView accelXTextView, accelYTextView, accelZTextView, gyroXTextView, gyroYTextView;
    private TextView magXTextView, magYTextView, magZTextView;
    private TextView lightIntensityTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerButtons();
        registerSensors();
        registerTextViews();

        startTime = System.currentTimeMillis();
        scheduleWriterThread();

    }

    private boolean shouldDisplaySensorValuesInUI(long currentTimeInMillis, Long lastSensorDisplayRefresh) {
        return lastSensorDisplayRefresh == null || currentTimeInMillis - lastSensorDisplayRefresh > 500;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!recording) {
            return;
        }
        long currentTimeInMillis = System.currentTimeMillis();

        SensorData sensorData = dataMap.getOrDefault(event.timestamp, new SensorData(event.timestamp));

        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorData.setAcc_x(event.values[0]);
            sensorData.setAcc_y(event.values[1]);
            sensorData.setAcc_z(event.values[2]);

            if (shouldDisplaySensorValuesInUI(currentTimeInMillis, lastAccelSensorDisplayRefresh)) {
                accelXTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[0]));
                accelYTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[1]));
                accelZTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[2]));
                lastAccelSensorDisplayRefresh = currentTimeInMillis;
            }


            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add acceleration data to queue");
                e.printStackTrace();
            }

        } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorData.setGyro_x(event.values[0]);
            sensorData.setGyro_y(event.values[1]);

            if (shouldDisplaySensorValuesInUI(currentTimeInMillis, lastGyroSensorDisplayRefresh)) {
                gyroXTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[0]));
                gyroYTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[1]));
                lastGyroSensorDisplayRefresh = currentTimeInMillis;
            }

            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add gyro data to queue");
                e.printStackTrace();
            }

        } else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            sensorData.setMag_x(event.values[0]);
            sensorData.setMag_y(event.values[1]);
            sensorData.setMag_z(event.values[2]);

            if (shouldDisplaySensorValuesInUI(currentTimeInMillis, lastMagSensoryDisplayRefresh)) {
                magXTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[0]));
                magYTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[1]));
                magZTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[2]));
                lastMagSensoryDisplayRefresh = currentTimeInMillis;
            }


            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add mag data to queue");
                e.printStackTrace();
            }

        } else if(event.sensor.getType() == Sensor.TYPE_LIGHT) {
            sensorData.setLux(event.values[0]);
            if (shouldDisplaySensorValuesInUI(currentTimeInMillis, lastLightIntensitySensorDisplayRefresh)) {
                lightIntensityTextView.setText(String.format(Locale.ENGLISH, "%.2f", event.values[0]));
                lastLightIntensitySensorDisplayRefresh = currentTimeInMillis;
            }

            try {
                dataQueue.put(sensorData);
            } catch (InterruptedException e) {
                System.out.println("Failed to add light data to queue");
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void registerTextViews() {
        accelXTextView = (TextView) findViewById(R.id.accelXValue);
        accelYTextView = (TextView) findViewById(R.id.accelYValue);
        accelZTextView = (TextView) findViewById(R.id.accelZValue);

        gyroXTextView = (TextView) findViewById(R.id.gyroXValue);
        gyroYTextView = (TextView) findViewById(R.id.gyroYValue);

        magXTextView = (TextView) findViewById(R.id.magXValue);
        magYTextView = (TextView) findViewById(R.id.magYValue);
        magZTextView = (TextView) findViewById(R.id.magZValue);
        lightIntensityTextView = (TextView) findViewById(R.id.LightIntensityValue);
    }

    private void registerButtons() {
        SwitchCompat toggleSwitch = (SwitchCompat) findViewById(R.id.button1);
        toggleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if (isChecked) {
                    showCustomDialog();

                }
                else {
                    recording = false;
                }
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
        return selectedActivity + " " + DateTimeFormatter.ofPattern(
                "dd-MM-yy HH;mm;ss").format(recordingStartTime
        ) + " " + distanceTraversedInCm + "cm.csv";
    }

    private void showCustomDialog() {
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.activity_dialog);

        Spinner dropdown = dialog.findViewById(R.id.spinner1);
        String[] options = new String[]{"IDLE", "WALKING", "RUNNING", "STAIRS", "JUMPING"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, options
        );
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

        EditText distanceTraversedField = dialog.findViewById(R.id.distanceTraversedField);
        Button submitButton = (Button) dialog.findViewById(R.id.actvitySubmitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String distanceTraversedFieldText = distanceTraversedField.getText().toString();
                if (distanceTraversedFieldText.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Please enter distance in cm", Toast.LENGTH_LONG).show();
                    return;
                }
                distanceTraversedInCm = Integer.parseInt(distanceTraversedFieldText);
                recording = true;
                recordingStartTime = LocalDateTime.now();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
    private void writeToFile(List<SensorData> data, boolean useHeader, String filename) {
        String[] header = {"Timestamp", "Accel X", "Accel Y", "Accel Z", "Gyro X", "Gyro Y", "Mag X", "Mag Y", "Mag Z", "Light Intensity"};
        List<String[]> csvData = new ArrayList<>();
        Long lastKey = null;
        Map<Long, SensorData> mergedData = new TreeMap<>();

        for (SensorData currentData : data) {
            SensorData sensorData = mergedData.getOrDefault(currentData.getTimestamp(), currentData);
            sensorData.merge(currentData);
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

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
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
