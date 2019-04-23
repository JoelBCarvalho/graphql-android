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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.CustomTypeValue;
import com.apollographql.apollo.sample.CreateTrackerMutation;
import com.apollographql.apollo.sample.RoutersQuery;
import com.apollographql.apollo.sample.TrackersQuery;
import com.apollographql.apollo.sample.type.CreateTrackerInput;
import com.apollographql.apollo.sample.type.CustomType;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

        peripheralTextView = findViewById(R.id.ctv_peripherical);

        statusTextView = findViewById(R.id.tv_status);
        statusTextView.setText("Stopped");

        setupBluetooth();


        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
        CustomTypeAdapter dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
            @Override public Date decode(CustomTypeValue value) {
                try {
                    return DATE_FORMAT.parse(value.value.toString());
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override public CustomTypeValue encode(Date value) {
                return new CustomTypeValue.GraphQLString(DATE_FORMAT.format(value));
            }
        };

        apolloClient = ApolloClient.builder()
                .serverUrl(BASE_URL)
                .okHttpClient(okHttpClient)
                .addCustomTypeAdapter(CustomType.DATE, dateCustomTypeAdapter)
                .build();

        setupTrackerSpinner();
    }

    private void setupTrackerSpinner() {
        spinner_trackers = findViewById(R.id.spinner_trackers);
        dataAdapter_trackers = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<String>());
        dataAdapter_trackers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_trackers.setAdapter(dataAdapter_trackers);

        populateTrackers();
    }

    private void populateTrackers() {
        RoutersQuery routersQuery = RoutersQuery.builder().build();
        apolloClient.query(routersQuery)
                .enqueue(new ApolloCall.Callback<RoutersQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<RoutersQuery.Data> response) {

                        for (RoutersQuery.Router router : response.data().routers()) {
                            Log.i("graphql", router._id());
                            Log.i("graphql", router.activation_link());
                            if(router.name() != null) {
                                Log.i("graphql", router.name());
                            }
                            Log.i("graphql", router.bt_active().toString());
                            Log.i("graphql", router.connected().toString());
                        }
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("fail", e.getStackTrace().toString());
                    }
                });

        TrackersQuery trackersQuery = TrackersQuery.builder().build();
        apolloClient.query(trackersQuery)
                .enqueue(new ApolloCall.Callback<TrackersQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull final Response<TrackersQuery.Data> response) {

                        for (final TrackersQuery.Tracker tracker : response.data().trackers()) {
                            Log.i("graphql", tracker._id());
                            Log.i("graphql", tracker.name());
                            Log.i("graphql", tracker.mac());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dataAdapter_trackers.add(tracker.name());
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        Log.e("fail", e.getStackTrace().toString());
                    }
                });
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

    public void onTrackerListRefresh(View view) {
        populateTrackers();
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
        stopScanning();
        Dialog dialog = onCreateDialogTrackerName(nearestTracker);
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
            String deviceName = result.getDevice().getName();
            String address = result.getDevice().getAddress();
            Integer value = Math.abs(result.getRssi());

            Tracker scanned = new Tracker(deviceName, address, value);
            if(deviceName != null && address != null && value != null) {
                scannedTrackers.put(address, scanned);
            }

            nearestTracker = new Tracker(scanned.getDeviceName(), scanned.getAddress(), scanned.getValue());
            for (Tracker scannedList : scannedTrackers.values()) {
                if (nearestTracker.getValue() > scannedList.getValue()) {
                    nearestTracker = new Tracker(scannedList.getDeviceName(), scannedList.getAddress(), scannedList.getValue());
                }
            }

            peripheralTextView.setText(nearestTracker.toString());

        }
    };

    public Dialog onCreateDialogTrackerName(final Tracker tracker) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Who will use " + tracker.getDeviceName() + " ?");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String trackerName = input.getText().toString();
                if(!trackerName.isEmpty()) {
                    apolloClient.mutate(createTrackerMutation(trackerName, tracker))
                            .enqueue(createPostMutationCallback);
                }
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

    @NonNull
    private CreateTrackerMutation createTrackerMutation(String trackerName, Tracker tracker) {
        CreateTrackerInput input = CreateTrackerInput.builder()
                .name(trackerName)
                .mac(tracker.getAddress())
                .build();
        return CreateTrackerMutation
                .builder()
                .input(input)
                .build();
    }

    final ApolloCall.Callback<CreateTrackerMutation.Data> createPostMutationCallback = new ApolloCall.Callback<CreateTrackerMutation.Data>() {
        @Override
        public void onResponse(@NotNull final Response<CreateTrackerMutation.Data> response) {
            if(response.data() == null) {
                Log.i("graphql", "Not saved probably duplicate mac");
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataAdapter_trackers.add(response.data().createTracker().name());
                    }
                });
            }
        }

        @Override
        public void onFailure(@NotNull ApolloException e) {
            Log.e("fail", e.getStackTrace().toString());
        }
    };

}
