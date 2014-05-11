package com.aranha.spider.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.spider.app.R;

/**
 * The activity which is used to connect to the Raspberry Pi.
 */
public class ConnectActivity extends ActionBarActivity implements View.OnClickListener {
    private final int REQUEST_ENABLE_BLUETOOTH = 1;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothDevice device;
    private Button connectButton, refreshButton, manualConnectButton;
    private TextView connectedTextView;

    BluetoothService mBluetoothService;
    boolean mBound = false;


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
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                System.out.println("Trying to enable bluetooth");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
            } else {
                startBluetoothService();
            }
        }
    }
    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) { // Unbind from the service
            unbindService(mConnection);
            mBound = false;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                if(resultCode == 1) // User pressed 'Allow' when asked to activate bluetooth.
                    startBluetoothService();
                //else close application
                break;
        }
    }

    public void startBluetoothService() {
        Intent intent = new Intent(this, BluetoothService.class);
        intent.putExtra("messageReceiver", mBluetoothServiceMessenger);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Used to connect to the BluetoothService
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Get the bluetoothService class via BluetoothBinder.
            mBluetoothService = ((BluetoothService.BluetoothBinder) service).getService();
            mBluetoothService.discoverBluetoothDevices();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            System.out.println("Bluetooth service disconnected? :/");
        }
    };

    final Messenger mBluetoothServiceMessenger = new Messenger(new BluetoothServiceMessageHandler());
    class BluetoothServiceMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MSG_RASPBERRYPI_FOUND:
                    connectButton.setEnabled(true);
                    break;

                case BluetoothService.MSG_CONNECTING_FAILED:
                    connectButton.setEnabled(false);
                    break;

                case BluetoothService.MSG_CONNECTED_TO_RASPBERRYPI:
                    connectedTextView.setText("Connected!");
                    connectButton.setEnabled(false);

                    Intent intent =  new Intent(ConnectActivity.this, MainActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    }

    @Override
    public void onClick(View view) {

        if(view.getId() == R.id.connectButton) {
            mBluetoothService.connect();
            connectButton.setEnabled(false);
        }
        else if(view.getId() == R.id.refresh) {
            mBluetoothService.discoverBluetoothDevices();
        }
        else if(view.getId() == R.id.manualConnectButton) {
            mBluetoothService.manualConnect("00:15:83:6A:31:B7");
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
