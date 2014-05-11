package com.aranha.spider.app;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

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

    private final Messenger mMessenger;

    public BluetoothThread(BluetoothDevice device, Messenger messenger) {
        BluetoothSocket tmp = null;
        mmDevice = device;
        mMessenger = messenger;

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
                Thread bluetoothToSpiderThread = new BluetoothSpiderConnectionThread(mmSocket, mMessenger);
                bluetoothToSpiderThread.start();
            }

        } catch (IOException closeException) {

            try {
                mMessenger.send(Message.obtain(null, BluetoothService.MSG_CONNECTING_FAILED));
            } catch (RemoteException e) {
                e.printStackTrace();
            }

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
