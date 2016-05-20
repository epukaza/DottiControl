package io.github.epukaza.dotticontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning = false;
    private Handler mHandler;
    private ListView deviceList;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothDevice mTargetDevice;
    private BluetoothGatt mGattServer;
    private boolean connected = false;
    private String logTag = "MainActivity";
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    Log.d(logTag, "scan callback fired");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };


    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(logTag,"Connection state callback called");
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED){
                mGattServer = gatt;
                connected = true;
                mGattServer.discoverServices();
            }
            if(newState == BluetoothProfile.STATE_DISCONNECTED){
                mGattServer = null;
                connected = false;
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final BluetoothManager bluetoothManager =
            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mHandler = new Handler();

        deviceList = (ListView) findViewById(R.id.listDevices);

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        deviceList.setAdapter(mLeDeviceListAdapter);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mTargetDevice = (BluetoothDevice) adapterView.getAdapter().getItem(i);
                mTargetDevice.connectGatt(getApplicationContext(), true, mBluetoothGattCallback);
            }
        });
        Button scanButton = (Button) findViewById(R.id.buttonScan);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanLeDevice(!mScanning);
            }
        });

        Button colorButton = (Button) findViewById(R.id.buttonRandomColor);
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(logTag,"colorbutton clicked with gattserver:" + mGattServer);
                if(mGattServer!=null){
                    sendRandomColor(mGattServer);
                }
            }
        });
    }

    public void sendRandomColor(BluetoothGatt gatt){
        BluetoothGattCharacteristic dottiCharacteristic = gatt.getService(UUID.fromString(getString(R.string.simpleserviceUUID)))
                .getCharacteristic(UUID.fromString(getString(R.string.simpleCellUUID)));
        Log.d(logTag, "sending random Color");
                //gatt.writeCharacteristic(new BluetoothGattCharacteristic(UUID.fromString(getString(R.string.simpleCellUUID)),5, 5));
        Random r = new Random();
        int pixelId=r.nextInt(64), red=r.nextInt(256), green=r.nextInt(256), blue=r.nextInt(256);

        dottiCharacteristic.setValue(new byte[]{(byte) 7, (byte) 2, (byte) (pixelId + 1), (byte) red, (byte) green, (byte) blue});
        gatt.writeCharacteristic(dottiCharacteristic);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void scanLeDevice(final boolean enable) {
        Log.d(logTag, "Scan called with: " + enable);
        if(enable){
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();

                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }


    static class ViewHolder{
        TextView deviceName;
        TextView deviceAddress;
    }

    private class LeDeviceListAdapter extends BaseAdapter implements ListAdapter{
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_row_layout, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.deviceAddress);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.deviceName);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
}
