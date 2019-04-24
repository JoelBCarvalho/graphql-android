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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.CustomTypeValue;
import com.apollographql.apollo.sample.CreateRoomMutation;
import com.apollographql.apollo.sample.CreateRouterMutation;
import com.apollographql.apollo.sample.CreateTrackerMutation;
import com.apollographql.apollo.sample.RoomsQuery;
import com.apollographql.apollo.sample.RoutersQuery;
import com.apollographql.apollo.sample.TrackersQuery;
import com.apollographql.apollo.sample.type.CreateRoomInput;
import com.apollographql.apollo.sample.type.CreateRouterInput;
import com.apollographql.apollo.sample.type.CreateTrackerInput;
import com.apollographql.apollo.sample.type.CustomType;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
    private ArrayAdapter<String> dataAdapter_buildings = null;
    private ArrayAdapter<String> dataAdapter_rooms = null;


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

        setupTrackersSpinner();
        setupBuildingsSpinner();
        setupRoomsSpinner();
    }

    private void setupTrackersSpinner() {
        spinner_trackers = findViewById(R.id.spinner_trackers);
        dataAdapter_trackers = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<String>());
        dataAdapter_trackers.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_trackers.setAdapter(dataAdapter_trackers);

        populateTrackers();
    }

    private void setupBuildingsSpinner() {
        spinner_buildings = findViewById(R.id.spinner_buildings);
        dataAdapter_buildings = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<String>());
        dataAdapter_buildings.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_buildings.setAdapter(dataAdapter_buildings);

        populateBuildings();
    }

    private void setupRoomsSpinner() {
        spinner_rooms = findViewById(R.id.spinner_rooms);
        dataAdapter_rooms = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<String>());
        dataAdapter_rooms.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_rooms.setAdapter(dataAdapter_rooms);

        populateRooms();
    }

    private void populateRooms() {
        dataAdapter_rooms.clear();
        RoomsQuery roomsQuery = RoomsQuery.builder().build();
        apolloClient.query(roomsQuery)
                .enqueue(new ApolloCall.Callback<RoomsQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<RoomsQuery.Data> response) {

                        for (final RoomsQuery.Room room : response.data().rooms()) {
                            Log.i("graphql", room._id());
                            Log.i("graphql", room.name());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dataAdapter_rooms.add(room.name());
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

    private void populateBuildings() {
        dataAdapter_buildings.clear();
        RoutersQuery routersQuery = RoutersQuery.builder().build();
        apolloClient.query(routersQuery)
                .enqueue(new ApolloCall.Callback<RoutersQuery.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<RoutersQuery.Data> response) {

                        for (final RoutersQuery.Router router : response.data().routers()) {
                            Log.i("graphql", router._id());
                            Log.i("graphql", router.activation_link());
                            if(router.name() != null) {
                                Log.i("graphql", router.name());
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dataAdapter_buildings.add(router.name());
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

    private void populateTrackers() {
        dataAdapter_trackers.clear();
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
        Dialog dialog = onCreateDialogBuildingName();
        dialog.show();
    }

    public void onBuildingRemove(View view) {
    }

    public void onRoomAdd(View view) {
        Dialog dialog = onCreateDialogRoomName();
        dialog.show();
    }

    public void onRoomRemove(View view) {
    }

    public void onAddNewTracker(View view) {
        stopScanning();
        Dialog dialog = onCreateDialogTrackerName(nearestTracker);
        dialog.show();
    }

    public void onBuildingRefresh(View view) {
        populateBuildings();
    }

    public void onRoomRefresh(View view) {
        populateRooms();
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

    ///////////START TRACKER MUTATAION
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
    ///////////END TRACKER MUTATAION

    ///////////START BUIlDING MUTATAION
    public Dialog onCreateDialogBuildingName() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("PLease enter your new building name and activation link!");

        final EditText inputBuildingName = new EditText(this);
        inputBuildingName.setInputType(InputType.TYPE_CLASS_TEXT);
        final EditText inputActivationLink = new EditText(this);
        inputActivationLink.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout addBuildingLayout = new LinearLayout(this);
        addBuildingLayout.setOrientation(LinearLayout.VERTICAL);
        addBuildingLayout.addView(inputBuildingName);
        addBuildingLayout.addView(inputActivationLink);
        builder.setView(addBuildingLayout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String buildingName = inputBuildingName.getText().toString();
                String activationLink = inputActivationLink.getText().toString();
                if(!buildingName.isEmpty() && !activationLink.isEmpty()) {
                    apolloClient.mutate(createBuildingMutation(activationLink, buildingName))
                            .enqueue(createPostBuildingMutationCallback);
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
    private CreateRouterMutation createBuildingMutation(String activationLink, String buildingName) {
        CreateRouterInput input = CreateRouterInput.builder()
                .activation_link(activationLink)
                .name(buildingName)
                .build();
        return CreateRouterMutation
                .builder()
                .input(input)
                .build();
    }

    final ApolloCall.Callback<CreateRouterMutation.Data> createPostBuildingMutationCallback = new ApolloCall.Callback<CreateRouterMutation.Data>() {
        @Override
        public void onResponse(@NotNull final Response<CreateRouterMutation.Data> response) {
            if(response.data() == null) {
                Log.i("graphql", "Not saved probably duplicate ");
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataAdapter_buildings.add(response.data().createRouter().name());
                    }
                });
            }
        }

        @Override
        public void onFailure(@NotNull ApolloException e) {
            Log.e("fail", e.getStackTrace().toString());
        }
    };
    ///////////END BUILDING MUTATAION

    ///////////START ROOM MUTATAION
    public Dialog onCreateDialogRoomName() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Give a name to the room of the building you're going to map!");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String roomName = input.getText().toString();
                if(!roomName.isEmpty()) {
                    apolloClient.mutate(createRoomMutation(roomName))
                            .enqueue(createPostRoomMutationCallback);
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
    private CreateRoomMutation createRoomMutation(String roomName) {
        CreateRoomInput input = CreateRoomInput.builder()
                .name(roomName)
                .build();
        return CreateRoomMutation
                .builder()
                .input(input)
                .build();
    }

    final ApolloCall.Callback<CreateRoomMutation.Data> createPostRoomMutationCallback = new ApolloCall.Callback<CreateRoomMutation.Data>() {
        @Override
        public void onResponse(@NotNull final Response<CreateRoomMutation.Data> response) {
            if(response.data() == null) {
                Log.i("graphql", "Not saved probably duplicate ");
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dataAdapter_rooms.add(response.data().createRoom().name());
                    }
                });
            }
        }

        @Override
        public void onFailure(@NotNull ApolloException e) {
            Log.e("fail", e.getStackTrace().toString());
        }
    };
///////////END ROOM MUTATAION


}
