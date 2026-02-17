package com.dionican.m5600sensor.model;

/**
 * Represents a TE M5600 Bluetooth pressure sensor device
 */
public class SensorDevice {
    private String name;
    private String address;
    private int rssi;
    private boolean connected;
    private SensorData sensorData;

    public SensorDevice(String name, String address, int rssi) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
        this.connected = false;
        this.sensorData = new SensorData();
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public SensorData getSensorData() {
        return sensorData;
    }

    public void setSensorData(SensorData sensorData) {
        this.sensorData = sensorData;
    }

    @Override
    public String toString() {
        return "SensorDevice{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", rssi=" + rssi +
                ", connected=" + connected +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensorDevice that = (SensorDevice) o;

        return address != null ? address.equals(that.address) : that.address == null;
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }
}
