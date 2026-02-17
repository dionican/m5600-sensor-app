package com.dionican.m5600sensor.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.dionican.m5600sensor.model.SensorData;

import java.util.UUID;

/**
 * Service for managing Bluetooth Low Energy communication with M5600 sensors
 */
public class BluetoothLeService extends Service {
    private static final String TAG = "BluetoothLeService";

    public static final String ACTION_GATT_CONNECTED = "com.dionican.m5600sensor.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.dionican.m5600sensor.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.dionican.m5600sensor.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.dionican.m5600sensor.ACTION_DATA_AVAILABLE";
    public static final String EXTRA_DATA = "com.dionican.m5600sensor.EXTRA_DATA";

    // M5600 GATT UUIDs
    private static final UUID M5600_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice connectedDevice;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initialize Bluetooth manager and adapter
     */
    public boolean initialize() {
        bluetoothManager = getSystemService(BluetoothManager.class);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connect to a Bluetooth LE device
     */
    public boolean connect(String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // If we're already connected or connecting, disconnect first
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }

        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // Connect to the GATT server hosted on the BluetoothLE device
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            connectedDevice = device;
            Log.d(TAG, "Trying to create a new connection.");
            return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address.");
            return false;
        }
    }

    /**
     * Disconnect from the current device
     */
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * Close the GATT connection
     */
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * Enable notification/indication for a characteristic
     */
    public void enableNotification(String characteristicUUID) {
        if (bluetoothGatt == null) {
            return;
        }

        BluetoothGattCharacteristic characteristic = getCharacteristic(characteristicUUID);
        if (characteristic != null) {
            bluetoothGatt.setCharacteristicNotification(characteristic, true);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
            if (descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
            }
        }
    }

    /**
     * Get a specific characteristic by UUID
     */
    private BluetoothGattCharacteristic getCharacteristic(String characteristicUUID) {
        if (bluetoothGatt == null) {
            return null;
        }

        UUID uuid = UUID.fromString(characteristicUUID);
        return bluetoothGatt.getService(M5600_SERVICE_UUID).getCharacteristic(uuid);
    }

    /**
     * Read a characteristic value
     */
    public void readCharacteristic(String characteristicUUID) {
        if (bluetoothGatt == null) {
            return;
        }

        BluetoothGattCharacteristic characteristic = getCharacteristic(characteristicUUID);
        if (characteristic != null) {
            bluetoothGatt.readCharacteristic(characteristic);
        }
    }

    /**
     * GATT connection state callback
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothAdapter.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
            } else {
                return;
            }
            broadcastUpdate(intentAction);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.i(TAG, "GATT services discovered successfully.");
                // Enable notifications for characteristics
                enableNotification(SensorData.PRESSURE_CHARACTERISTIC_UUID);
                enableNotification(SensorData.TEMPERATURE_CHARACTERISTIC_UUID);
                enableNotification(SensorData.BATTERY_CHARACTERISTIC_UUID);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    /**
     * Broadcast updates to connected activities
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        byte[] data = characteristic.getValue();

        String uuid = characteristic.getUuid().toString();
        if (SensorData.PRESSURE_CHARACTERISTIC_UUID.equals(uuid)) {
            float pressure = SensorData.parsePressureFromBytes(data);
            intent.putExtra(EXTRA_DATA, "PRESSURE:" + pressure);
        } else if (SensorData.TEMPERATURE_CHARACTERISTIC_UUID.equals(uuid)) {
            float temperature = SensorData.parseTemperatureFromBytes(data);
            intent.putExtra(EXTRA_DATA, "TEMPERATURE:" + temperature);
        } else if (SensorData.BATTERY_CHARACTERISTIC_UUID.equals(uuid)) {
            int battery = SensorData.parseBatteryFromBytes(data);
            intent.putExtra(EXTRA_DATA, "BATTERY:" + battery);
        }

        sendBroadcast(intent);
    }

    public String getConnectedDeviceName() {
        return connectedDevice != null ? connectedDevice.getName() : null;
    }

    public String getConnectedDeviceAddress() {
        return connectedDevice != null ? connectedDevice.getAddress() : null;
    }
}
