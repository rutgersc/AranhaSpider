package com.aranha.spider.app;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Handles all the communication between the Raspberry Pi and the App.
 */
public class BluetoothSpiderConnectionThread extends Thread {

    public static final int MESSAGE_READ = 9998;

    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private final ConnectActivity connectActivity;
    private final Handler mHandler;

    /**
     * Gets an already connected socket and sets up the communication.
     * @param connectActivity The main activity.
     * @param socket A connected socket.
     * @param handler Message handler.
     */
    public BluetoothSpiderConnectionThread(ConnectActivity connectActivity, BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        mHandler = handler;
        this.connectActivity = connectActivity;

        try {
            // Get the input and output streams, using temp objects because member streams are final
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) { }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        connectActivity.onConnectedWithRaspberryPi(this);


        //
        //  !!!!!!!!!!!!!!!!!!!!!!!!!!! TEST TEST TEST
        //
        String test = "De app is connected. altijd connected. je moet gewoon connected zijn. ja toch? hallo hallo";
        writeBase64(test);
        //
        //  !!!!!!!!!!!!!!!!!!!!!!!!!!! TEST TEST TEST
        //
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
            } catch (IOException e) {
                System.out.println("In stream Exception!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                break;
            }
        }
    }

    /**
     * Called from the main activity to send a string encoded with Base64.
     * @param input
     */
    public void writeBase64(String input) {
        write(Base64.encode(input.getBytes(), Base64.NO_PADDING));
    }

    /**
     * Write to the output stream.
     * @param bytes
     */
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /**
     * Called from the main activity to close the connection.
     */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }
}
