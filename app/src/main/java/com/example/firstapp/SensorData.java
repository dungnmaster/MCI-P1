package com.example.firstapp;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
public class SensorData {
    private Float acc_x;
    private Float acc_y;
    private Float acc_z;
    private Float lux;
    private Float gyro_x;
    private Float gyro_y;
    private Float mag_x;
    private Float mag_y;
    private Float mag_z;
    private Long timestamp;


    public SensorData() {
    }

    public Float getAcc_x() {
        return acc_x;
    }

    public void setAcc_x(Float acc_x) {
        this.acc_x = acc_x;
    }

    public Float getAcc_y() {
        return acc_y;
    }

    public void setAcc_y(Float acc_y) {
        this.acc_y = acc_y;
    }

    public Float getAcc_z() {
        return acc_z;
    }

    public void setAcc_z(Float acc_z) {
        this.acc_z = acc_z;
    }

    public Float getLux() {
        return lux;
    }

    public void setLux(Float lux) {
        this.lux = lux;
    }

    public Float getGyro_x() {
        return gyro_x;
    }

    public void setGyro_x(Float gyro_x) {
        this.gyro_x = gyro_x;
    }

    public Float getGyro_y() {
        return gyro_y;
    }

    public void setGyro_y(Float gyro_y) {
        this.gyro_y = gyro_y;
    }

    public Float getMag_x() {
        return mag_x;
    }

    public void setMag_x(Float mag_x) {
        this.mag_x = mag_x;
    }

    public Float getMag_y() {
        return mag_y;
    }

    public void setMag_y(Float mag_y) {
        this.mag_y = mag_y;
    }

    public Float getMag_z() {
        return mag_z;
    }

    public void setMag_z(Float mag_z) {
        this.mag_z = mag_z;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

//    String[] getDataForCSV() {
//        String[] data = {timestamp.toString(), Objects.toString()}
//    }
}
