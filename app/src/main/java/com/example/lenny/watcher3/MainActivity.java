package com.example.lenny.watcher3;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.apollographql.apollo.ApolloClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    TextView peripheralTextView;
    TextView statusTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    Boolean bt_started = false;
    Tracker nearestTracker = null;
    Map<String, Tracker> scannedTrackers = new HashMap<>();


    private static final String BASE_URL = "http://192.168.1.220:4000/graphql";
    private ApolloClient apolloClient;

    private Spinner spinner_trackers, spinner_buildings, spinner_rooms;
    private List<String> list_trackers = new ArrayList<>();
    private ArrayAdapter<String> dataAdapter_trackers = null;
    private List<String> list_buildings = new ArrayList<>();
    private List<String> list_rooms = new ArrayList<>();

    private String m_Text = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();

        setContentView(R.layout.activity_main);

        apolloClient = ApolloClient.builder()
                .serverUrl(BASE_URL)
                .okHttpClient(okHttpClient)
                .build();

        peripheralTextView = findViewById(R.id.ctv_peripherical);

        statusTextView = findViewById(R.id.tv_status);
        statusTextView.setText("Stopped");

        setupBluetooth();

        getTrackers();
    }

    private void getTrackers() {
        spinner_trackers = findViewById(R.id.spinner_trackers);
        list_trackers = new ArrayList<>();
        list_trackers.add("list 1");
        list_trackers.add("list 2");
        list_trackers.add("list 3");
        dataAdapter_trackers = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, list_trackers);
        dataAdapter_trackers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_trackers.setAdapter(dataAdapter_trackers);
    }

    private void setupBluetooth() {
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    public void onTrackerAdd(View view) {


    }

    public void onTrackerRemove(View view) {
    }

    public void onBuildingAdd(View view) {
    }

    public void onBuildingRemove(View view) {
    }

    public void onRoomAdd(View view) {
    }

    public void onRoomRemove(View view) {
    }

    public void onAddNewTracker(View view) {
        Dialog dialog = onCreateDialogSingleChoice(nearestTracker);
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void onStartScanning(View view) {
        if (bt_started) {
            statusTextView.setText("Stopped");
            stopScanning();
        } else {
            statusTextView.setText("Scanning...");
            startScanning();
        }

        bt_started = !bt_started;
    }

    public void startScanning() {
        peripheralTextView.setText("");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String name = result.getDevice().getName();
            String address = result.getDevice().getAddress();
            Integer value = Math.abs(result.getRssi());

            Tracker scanned = new Tracker(name, address, value);
            if(name != null && address != null && value != null) {
                scannedTrackers.put(address, scanned);
            }

            nearestTracker = new Tracker(scanned.getName(), scanned.getAddress(), scanned.getValue());
            for (Tracker scannedList : scannedTrackers.values()) {
                if (nearestTracker.getValue() > scannedList.getValue()) {
                    nearestTracker = new Tracker(scannedList.getName(), scannedList.getAddress(), scannedList.getValue());
                }
            }

            peripheralTextView.setText(nearestTracker.toString());

        }
    };

    public Dialog onCreateDialogSingleChoice(Tracker tracker) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Who will use " + tracker.getName() + " ?");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);


        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
                dataAdapter_trackers.add(m_Text);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        return builder.create();
    }

}
