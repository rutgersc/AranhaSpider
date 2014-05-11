package com.aranha.spider.app;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rutger on 10-05-2014.
 */
public class BluetoothService extends Service {

    public static final int MSG_RASPBERRYPI_FOUND = 1;
    public static final int MSG_CONNECTED_TO_RASPBERRYPI = 2;
    public static final int MSG_CONNECTING_FAILED = 3;

    public static final int MSG_READ = 4;

    private final IBinder mBinder = new BluetoothBinder();
    private Messenger messageReceiver;

    /**
     * The binder which clients use to communicate with this bluetooth service.
     */
    public class BluetoothBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.messageReceiver = intent.getParcelableExtra("messageReceiver");
        return mBinder;
    }

//----------------------------------------------------------------
//-------------Set up bluetooth-----------------------------------
//----------------------------------------------------------------

    private boolean isReadyToConnect = false;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice device;

    @Override
    public void onCreate() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBluetoothReceiver, filter); // Don't forget to unregister in onDestroy()
        System.out.println("Bluetooth receiver registered.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothReceiver);
    }


    /**
     * Every time a bluetooth device is found the onReceive() function gets executed.
     */
    final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        final List<String> ss = new ArrayList<String>();
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); // Get the BluetoothDevice object from the Intent
                ss.add(device.getName() + "\n" + device.getAddress()); // Add the name and address to an array adapter to show in a ListView
                System.out.println(device.getName() + "\n" + device.getAddress());

                if(device.getName()!= null && device.getName().equals("raspberrypi-0-aranha")) {
                    onRaspberryPiFound(device);
                }
            }
        }
    };

    /**
     * Gets called whenever the Raspberry Pi is found via bluetooth.
     * @param device Device with the Raspberry Pi MAC address
     */
    public void onRaspberryPiFound(BluetoothDevice device) {
        this.device = device;
        mBluetoothAdapter.cancelDiscovery();
        isReadyToConnect = true;
        sendMessageToActivity(MSG_RASPBERRYPI_FOUND);
    }

    /**
     * The message handler. This receives all the incoming messages from the Raspberry Pi.
     */
    final Messenger mBluetoothConnecterMessenger = new Messenger(new BluetoothConnectionMessenger());
    class BluetoothConnectionMessenger extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case BluetoothService.MSG_CONNECTING_FAILED:
                    //TODO: Stuff
                    sendMessageToActivity(MSG_CONNECTING_FAILED);
                    break;

                case BluetoothService.MSG_CONNECTED_TO_RASPBERRYPI:
                    sendMessageToActivity(MSG_CONNECTED_TO_RASPBERRYPI);
                    break;


                case BluetoothService.MSG_READ:
                    String in = new String(Base64.decode((byte[]) msg.obj, Base64.NO_PADDING));
                    System.out.println("Received: " + in);
                    break;
            }
        }
    }

    private void sendMessageToActivity(int message) {

        if(messageReceiver != null) {
            Message msg = Message.obtain(null, message, 0,0 );
            try {
                messageReceiver.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


//----------------------------------------------------------------
//-------------Spider functies------------------------------------
//----------------------------------------------------------------

    public void discoverBluetoothDevices() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
        System.out.println("Discovering bluetooth devices!");
    }

    /**
     * Connect to the Raspberry Pi with a known MAC address.
     * @param MACAddress string
     */
    public void manualConnect(String MACAddress) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MACAddress);
        onRaspberryPiFound(device);
    }

    public void connect() {
        BluetoothThread btThread = new BluetoothThread(device, mBluetoothConnecterMessenger);
        btThread.start();
    }



    /**
     * VOORBEELD FUNCTIE!!!!!!!!!!!
     */
    public void spin_moveLeft() {

    }

}
