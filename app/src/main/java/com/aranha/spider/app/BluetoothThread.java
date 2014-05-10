package com.aranha.spider.app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.util.UUID;

/**
 * Tries to establish a connection via bluetooth.
 * If successful it will start another thread which handles all the messages.
 */
public class BluetoothThread extends Thread {
    private UUID MY_UUID = UUID.randomUUID();
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final ConnectActivity connectActivity;

    private final Handler mHandler;

    public BluetoothThread(ConnectActivity connectActivity, BluetoothDevice device, Handler handler) {
        BluetoothSocket tmp = null;
        mmDevice = device;
        mHandler = handler;
        this.connectActivity = connectActivity;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("9d7debbc-c85d-11d1-9eb4-006008c3a19a"));
        } catch (IOException e) { }

        mmSocket = tmp;
    }

    public void run() {

        try {
            mmSocket.connect();

            if(!mmSocket.isConnected()) {
                mmSocket.close();
            } else {
                // Do work to manage the connection (in a separate thread)
                System.out.println("Starting spin connection thread. Socket connected: " + mmSocket.isConnected());
                Thread bluetoothToSpiderThread = new BluetoothSpiderConnectionThread(connectActivity, mmSocket, mHandler);
                bluetoothToSpiderThread.start();
            }

        } catch (IOException closeException) {
            connectActivity.onConnectingFailed(); // Call fail method of the main activity
            System.out.println("!!!!!!!! Socket is NOT connected !!!!!!!! " + mmSocket.isConnected());
        }
    }

    /**
     * Will cancel an in-progress connection, and close the socket
     */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
