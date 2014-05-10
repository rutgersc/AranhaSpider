package com.aranha.spider.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.spider.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * The activity which is used to connect to the Raspberry Pi.
 */
public class ConnectActivity extends ActionBarActivity implements View.OnClickListener {

    private final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothDevice device;
    private Button connectButton, refreshButton, manualConnectButton;
    private TextView connectedTextView;

    /**
     * The thread that handles the in/out socket to the spider.
     */
    public BluetoothSpiderConnectionThread spiderConnectionThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        // Get the UI Resources from the xml
        //
        connectButton = (Button)findViewById(R.id.connectButton);
        connectButton.setOnClickListener(this);
        connectButton.setEnabled(false);
        manualConnectButton = (Button)findViewById(R.id.manualConnectButton);
        manualConnectButton.setOnClickListener(this);
        connectedTextView = (TextView)findViewById(R.id.connectedTextView);

        refreshButton = (Button)findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);

        // Enable Bluetoef
        //
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        enableBluetooth();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    /**
     * Every time a bluetooth device is found the onReceive() function gets executed.
     */
    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        final List<String> ss = new ArrayList<String>();
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); // Get the BluetoothDevice object from the Intent
                ss.add(device.getName() + "\n" + device.getAddress()); // Add the name and address to an array adapter to show in a ListView
                System.out.println(device.getName() + "\n" + device.getAddress());

                if(device.getName()!= null && device.getName().equals("raspberrypi-0")) {
                    onRaspberryPiFound(device);
                }
            }
        }
    };

    /**
     * Connect to the Raspberry Pi with a known MAC address.
     * @param MACAddress string
     */
    private void manualConnect(String MACAddress) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MACAddress);
        onRaspberryPiFound(device);
    }

    /**
     * The message handler. This receives all the incoming messages from the Raspberry Pi.
     */
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case BluetoothSpiderConnectionThread.MESSAGE_READ:
                    String in = new String(Base64.decode((byte[])msg.obj, Base64.NO_PADDING));
                    System.out.println("Received: " + in);
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if(resultCode == 1) // User pressed 'Allow' when asked to activate bluetooth.
                    discoverBluetoothDevices();
                else
                    enableBluetooth(); // Keep asking to enable bluetooth :>:>:>
                break;
        }
    }

    /**
     * Enables bluetooth if it's not already on yet.
     * Also will start to look for bluetooth devices.
     */
    public void enableBluetooth(){

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                System.out.println("Trying to enable bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                discoverBluetoothDevices();
            }
        }
    }

    public void discoverBluetoothDevices() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * Gets called whenever the Raspberry Pi is found via bluetooth.
     * @param device Device with the Raspberry Pi MAC address
     */
    public void onRaspberryPiFound(BluetoothDevice device) {
        this.device = device;
        mBluetoothAdapter.cancelDiscovery();
        connectButton.setEnabled(true);
    }

    /**
     * Gets called when the app is connected and paired with the spider.
     * @param connectionThread The thread with the in/out sockets.
     */
    public void onConnectedWithRaspberryPi(BluetoothSpiderConnectionThread connectionThread) {
        connectedTextView.setText("Connected!");
        connectButton.setEnabled(false);
    }

    /**
     * BluetoothThread.java failed to connect to the Raspberry Pi
     */
    public void onConnectingFailed() {
        this.device = null;
        connectButton.setEnabled(false);
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * Opens the main activity
     */
    public void onBluetoothConnectionEstablished() {
        Intent intent =  new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.connectButton) {
            BluetoothThread btThread = new BluetoothThread(this, device, mHandler);
            btThread.start();
            connectButton.setEnabled(false);
        }
        else if(view.getId() == R.id.refresh) {
            discoverBluetoothDevices();
        }
        else if(view.getId() == R.id.manualConnectButton) {
            manualConnect("00:15:83:6A:31:B7");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
