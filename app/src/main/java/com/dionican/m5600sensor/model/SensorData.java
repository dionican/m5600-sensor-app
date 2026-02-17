package com.dionican.m5600sensor.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents sensor data from M5600 device
 * Stores pressure, temperature, and battery information
 */
public class SensorData implements Serializable {
    private static final long serialVersionUID = 1L;

    // Sensor measurements
    private float pressure; // in PSI
    private float temperature; // in Celsius
    private int batteryLevel; // 0-100 percentage
    private long timestamp;

    // M5600 specific GATT characteristics UUIDs
    public static final String PRESSURE_CHARACTERISTIC_UUID = "00002A3D-0000-1000-8000-00805F9B34FB";
    public static final String TEMPERATURE_CHARACTERISTIC_UUID = "00002A1F-0000-1000-8000-00805F9B34FB";
    public static final String BATTERY_CHARACTERISTIC_UUID = "00002A19-0000-1000-8000-00805F9B34FB";

    public SensorData() {
        this.pressure = 0f;
        this.temperature = 0f;
        this.batteryLevel = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public SensorData(float pressure, float temperature, int batteryLevel) {
        this.pressure = pressure;
        this.temperature = temperature;
        this.batteryLevel = batteryLevel;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
        this.timestamp = System.currentTimeMillis();
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
        this.timestamp = System.currentTimeMillis();
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Convert pressure from PSI to BAR
     */
    public float getPressureInBar() {
        return pressure / 14.5038f;
    }

    /**
     * Convert temperature from Celsius to Fahrenheit
     */
    public float getTemperatureInFahrenheit() {
        return (temperature * 9 / 5) + 32;
    }

    /**
     * Get formatted timestamp string
     */
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date(timestamp));
    }

    /**
     * Parse pressure value from raw GATT bytes (M5600 format)
     * The M5600 sends pressure in a specific binary format
     */
    public static float parsePressureFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return 0f;
        }
        // M5600 sends pressure as little-endian 16-bit value in PSI
        int rawValue = ((bytes[1] & 0xFF) << 8) | (bytes[0] & 0xFF);
        // Scale factor for M5600 (typically 0.01 PSI per LSB)
        return rawValue * 0.01f;
    }

    /**
     * Parse temperature value from raw GATT bytes (M5600 format)
     * The M5600 sends temperature in a specific binary format
     */
    public static float parseTemperatureFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 1) {
            return 0f;
        }
        // M5600 sends temperature as signed byte in Celsius
        return (float) bytes[0];
    }

    /**
     * Parse battery level from raw GATT bytes
     */
    public static int parseBatteryFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 1) {
            return 0;
        }
        // Battery level is typically a percentage (0-100)
        return bytes[0] & 0xFF;
    }

    @Override
    public String toString() {
        return "SensorData{" +
                "pressure=" + pressure +
                ", temperature=" + temperature +
                ", batteryLevel=" + batteryLevel +
                ", timestamp=" + getFormattedTimestamp() +
                '}';
    }
}
