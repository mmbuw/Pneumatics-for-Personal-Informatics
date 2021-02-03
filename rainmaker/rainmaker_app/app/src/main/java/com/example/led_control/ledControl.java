package com.example.led_control;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import android.widget.ToggleButton;

import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.IOException;
import java.lang.reflect.Type;
//import java.time.LocalTime;
import org.threeten.bp.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;


public class ledControl extends AppCompatActivity implements DialogTaskClass.DialogListener {

    FirebaseAnalytics firebaseAnalytics =FirebaseAnalytics.getInstance(this);



    private static String userID = null;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    public int task_n = 0;
    public static String SHARED_PREFS = "sharedPrefs";

    ToggleButton toggleButton;
    Button btnBat, btnReset;
    FloatingActionButton floatBtn;

    TextView textViewBat;

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;

    RecyclerView recyclerViewFinished;
    RecyclerFinishedAdapter recyclerFinihsedAdapter;

    SwipeRefreshLayout swipeRefreshLayout, swipeRefreshLayoutFinished;

    ImageView imageViewBat;

    TextView textViewTitle;
    MultiAutoCompleteTextView multiAutoCompleteTextView;

    List<String> tasksList;
    List<String> finishedTasksList;


    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    public static BluetoothSocket btSocket = null;
    public boolean isBtConnected = false;

    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    int finishedTasks = 0;


    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    String toggle_msg="";
    String toggle_alert="";
    String androidId;
    String session_start_time;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidThreeTen.init(this);

//        requestWindowFeature(getWindow().FEATURE_NO_TITLE);
//
//        getSupportActionBar().hide();


        setContentView(R.layout.activity_led_control);

        //tasksList = new ArrayList<>();
        //finishedTasksList = new ArrayList<>();

        //Intent newint = getIntent();
        //address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        //call the widgtes
//         androidId = Settings.Secure.getString(getContentResolver(),
//                Settings.Secure.ANDROID_ID);

        userID=getUserId();
        //msg(userID);
        firebaseAnalytics.setUserId(userID);


        toggleButton= findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("InvalidAnalyticsName")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {



                if (toggleButton.isChecked()){
                    toggle_msg="TO";
                    toggle_alert="On!";
                }
                else{
                    toggle_msg="TF";
                    toggle_alert="Off!";

                }



                if (btSocket != null) {
                    if ((btSocket.isConnected()) && (myBluetooth.isEnabled())) {

                        try {
                            btSocket.getOutputStream().write("".getBytes());


                            //btSocket.getOutputStream().write("D".toString().getBytes());
                            btSocket.getOutputStream().write((toggle_msg).getBytes());


                        } catch (IOException e) {



                            msg("Not connected. Please press CONNECT and try again");
                            recyclerAdapter.notifyDataSetChanged();
                            textViewTitle.setText("Not connected!");
                            btnBat.setText("CONNECT");
                        }

                    } else {

                        msg("Not connected. Please press CONNECT and try again");
                        recyclerAdapter.notifyDataSetChanged();
                        textViewTitle.setText("Not connected!");
                        btnBat.setText("CONNECT");
                    }
                }
                else{


                    msg("Not connected. Please press CONNECT and try again");
                    recyclerAdapter.notifyDataSetChanged();
                    textViewTitle.setText("Not connected!");
                    btnBat.setText("CONNECT");
                }
            }
        });


        imageViewBat = findViewById(R.id.imageViewBat);
        textViewTitle = findViewById(R.id.textViewTitle);
        multiAutoCompleteTextView =findViewById(R.id.multiAutoCompleteTextView);

        btnBat = findViewById(R.id.btnBat);
        btnReset = findViewById(R.id.btnReset);
        floatBtn = findViewById(R.id.floatBtn);

        floatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (btSocket != null) {

                    if (btSocket.isConnected() && (myBluetooth.isEnabled())) {


                        try {

                            btSocket.getOutputStream().write("".getBytes());

                            if (finishedTasksList.size()+tasksList.size() < 10) {

                                DialogTaskClass dialogTaskClass = new DialogTaskClass();
                                Bundle bundle = new Bundle();

                                //bundle.putString("taskName", textViewTask.getText().toString());
                                bundle.putBoolean("editMode", false);

                                //bundle.putInt("taskId",position);

                                dialogTaskClass.setArguments(bundle);


                                dialogTaskClass.show(getSupportFragmentManager(), "example dialog");
                            }
                            else{

                                msg("Sorry, you can't add more than 10 tasks");
                            }


                        } catch (IOException e) {

                            msg("Not connected. Please press CONNECT and try again");


                            textViewTitle.setText("Not connected!");
                            btnBat.setText("CONNECT");
                        }



                        //recyclerAdapter.notifyItemInserted(tasksList.size()-1);


                    } else {


                        msg("Not connected. Please press CONNECT and try again");
                        textViewTitle.setText("Not connected!");
                        btnBat.setText("CONNECT");
                    }
                } else {
                   msg("Not connected. Please press CONNECT and try again");
                   textViewTitle.setText("Not connected!");
                   btnBat.setText("CONNECT");
                }


            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btSocket!=null) {

                    if (btSocket.isConnected() && myBluetooth.isEnabled()) {
                        new AlertDialog.Builder(ledControl.this)
                                .setTitle("Reset?")
                                .setMessage("Your progress will be reset. Are you sure?")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        try {
                                            btSocket.getOutputStream().write("R".getBytes());


                                            tasksList.clear();
                                            finishedTasksList.clear();

                                            recyclerAdapter.notifyDataSetChanged();
                                            recyclerFinihsedAdapter.notifyDataSetChanged();

                                            saveData();
                                        } catch (IOException e) {
                                            msg("Not connected. Please press CONNECT and try again");
                                            textViewTitle.setText("Not connected!");
                                            btnBat.setText("CONNECT");
                                        }

                                    }})
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){


                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();




                    } else {

                        msg("Not connected. Please press CONNECT and try again");
                        textViewTitle.setText("Not connected!");
                        btnBat.setText("CONNECT");
                    }
                }
                else{

                    msg("Not connected. Please press CONNECT and try again");
                    textViewTitle.setText("Not connected!");
                    btnBat.setText("CONNECT");
                }
            }


        });

        textViewBat = findViewById(R.id.textViewBat);

        btnBat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWorker = true;

                new ConnectBT().execute();

            }
        });

        swipeRefreshLayout = findViewById(R.id.swipeTasks);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                stopWorker = true;
                Refresh(true);
                stopWorker = true;
                Refresh(false);

                //msg(String.valueOf(finishedTasksList.size()));

                swipeRefreshLayout.setRefreshing(false);

            }
        });


        getData();
        recyclerView = findViewById(R.id.recycler);
        recyclerAdapter = new RecyclerAdapter(tasksList);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recyclerAdapter);

        recyclerAdapter.notifyDataSetChanged();

        //recyclerView.setNestedScrollingEnabled(false);


        recyclerViewFinished = findViewById(R.id.recyclerFinished);
        recyclerFinihsedAdapter = new RecyclerFinishedAdapter(finishedTasksList);


        recyclerViewFinished.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewFinished.setAdapter(recyclerFinihsedAdapter);


        recyclerFinihsedAdapter.notifyDataSetChanged();

        //recyclerViewFinished.setNestedScrollingEnabled(false);


        new ConnectBT().execute(); //Call the class to connect


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);






    }

    String deletedTask = null;

    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        TextView textViewTask;
        EditText editTextTask;
        FrameLayout frameTask;
        Button buttonTask;
        ImageView imageViewTask;

        private final ColorDrawable background = new ColorDrawable(Color.parseColor("#00ff00"));


        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            Collections.swap(tasksList, fromPosition, toPosition);
            saveData();

            recyclerView.getAdapter().notifyItemMoved(fromPosition, toPosition);

            recyclerAdapter.notifyDataSetChanged();


            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.delete, null))
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24)
                    .addSwipeRightBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.edit, null))
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_edit_24)
                    .create()
                    .decorate();

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            //swipeRefreshLayout.setEnabled(false);


        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            final int position = viewHolder.getAdapterPosition();


            //swipeRefreshLayout.setEnabled(true);


            textViewTask = viewHolder.itemView.findViewById(R.id.textViewTask);
            frameTask = viewHolder.itemView.findViewById(R.id.frameTask);


            switch (direction) {

                case ItemTouchHelper.LEFT:

                    if (btSocket != null) {
                        if ((btSocket.isConnected()) && (myBluetooth.isEnabled())) {

                            try {
                                btSocket.getOutputStream().write("".getBytes());

                                deletedTask = tasksList.get(position);
                                tasksList.remove(position);
                                saveData();
                                recyclerAdapter.notifyItemRemoved(position);

                                Snackbar.make(recyclerView, deletedTask + " was removed! ", Snackbar.LENGTH_LONG).setAction("Undo", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        tasksList.add(position, deletedTask);
                                        recyclerAdapter.notifyItemInserted(position);

                                    }
                                }).show();


                                //btSocket.getOutputStream().write("D".toString().getBytes());
                                btSocket.getOutputStream().write(("D" + position + finishedTasksList.size()).getBytes());
                                //msg(String.valueOf(tasksList.size()));


                            } catch (IOException e) {



                                msg("Not connected. Please press CONNECT and try again");
                                recyclerAdapter.notifyDataSetChanged();
                                textViewTitle.setText("Not connected!");
                                btnBat.setText("CONNECT");
                            }

                    } else {

                        msg("Not connected. Please press CONNECT and try again");
                        recyclerAdapter.notifyDataSetChanged();
                            textViewTitle.setText("Not connected!");
                            btnBat.setText("CONNECT");
                    }
                    }
                    else{


                        msg("Not connected. Please press CONNECT and try again");
                        recyclerAdapter.notifyDataSetChanged();
                        textViewTitle.setText("Not connected!");
                        btnBat.setText("CONNECT");
                    }



                    break;


                case ItemTouchHelper.RIGHT:

                    DialogTaskClass dialogTaskClass = new DialogTaskClass();
                    Bundle bundle = new Bundle();
                    bundle.putString("taskName", textViewTask.getText().toString());
                    bundle.putBoolean("editMode", true);
                    bundle.putString("session_time", session_start_time);
                    bundle.putInt("taskId", position);

                    dialogTaskClass.setArguments(bundle);
                    dialogTaskClass.show(getSupportFragmentManager(), "example dialog");


                    break;

            }

        }
    };

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void Disconnect() {
        if (btSocket != null) //If the btSocket is busy
        {
            try {
                btSocket.close(); //close connection
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish(); //return to the first layout
    }


    public void Refresh(final boolean once) {
        if (btSocket!=null) {

            if (btSocket.isConnected() && (myBluetooth.isEnabled())) {
                try {
                    if (once) {

                        btSocket.getOutputStream().write("N".getBytes());
                    }


                    final Handler handler = new Handler();
                    stopWorker = false;
                    readBufferPosition = 0;
                    readBuffer = new byte[1024];

                    workerThread = new Thread(new Runnable() {
                        public void run() {
                            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                                if (!isBtConnected) {
                                    stopWorker=true;

                                    textViewTitle.setText("Not connected!");
                                }
                                try {
                                    int bytesAvailable = btSocket.getInputStream().available();
                                    if (bytesAvailable > 0) {

                                        byte[] packetBytes = new byte[bytesAvailable];
                                        btSocket.getInputStream().read(packetBytes);

                                        for (int i = 0; i < bytesAvailable; i++) {
                                            byte b = packetBytes[i];
                                            //msg(String.valueOf(b));


                                            if (b == '\n') {
                                                final byte[] encodedBytes = new byte[readBufferPosition];
                                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                                //final String data = new String(encodedBytes, "US-ASCII");
                                                readBufferPosition = 0;
                                                handler.post(new Runnable() {
                                                    public void run() {

                                                        if (encodedBytes.length != 1) {

                                                            // message contains in order: bat lvl, finished tasks,
                                                            //msg(data);

                                                            //int splitter= data.indexOf('T');
                                                            //String bat_lvl=data.substring(0,splitter);
                                                            //String finished_tasks= data.substring(splitter+1);
                                                            int batLvl = encodedBytes[0];

                                                            if (batLvl > 100) {

                                                                batLvl = 100;
                                                            }

                                                            if (encodedBytes[0] < 0) {

                                                                batLvl = 0;
                                                            }

                                                            if (batLvl < 100 && batLvl > 70) {

                                                                imageViewBat.setImageResource(R.mipmap.bat_full);

                                                                imageViewBat.setTag(R.mipmap.bat_full);
                                                            }

                                                            if (batLvl < 70 && batLvl > 50) {

                                                                imageViewBat.setImageResource(R.mipmap.bat_mid2);
                                                                imageViewBat.setTag(R.mipmap.bat_mid2);
                                                            }

                                                            if (batLvl < 50 && batLvl > 30) {

                                                                imageViewBat.setImageResource(R.mipmap.bat_mid1);
                                                                imageViewBat.setTag(R.mipmap.bat_mid1);
                                                            }

                                                            if (batLvl < 30 && batLvl > 0) {

                                                                imageViewBat.setImageResource(R.mipmap.bat_low);
                                                                imageViewBat.setTag(R.mipmap.bat_low);
                                                            }


                                                            textViewBat.setText(batLvl + "%");

                                                            int finihsed_n = encodedBytes[1];
                                                            //msg(String.valueOf(finihsed_n));
                                                            int finished_tasks = finihsed_n - finishedTasksList.size();


                                                            if (finihsed_n > finishedTasksList.size()) {
                                                                for (int f = 0; f < finished_tasks; f++) {

                                                                    finishedTasksList.add(tasksList.get(f));

                                                                }

                                                                ArrayList<String>
                                                                        arrlist2 = new ArrayList<String>();

                                                                for (int t = 0; t < finished_tasks; t++) {
                                                                    arrlist2.add(tasksList.get(t));

                                                                }

                                                                tasksList.removeAll(arrlist2);

                                                                recyclerFinihsedAdapter.notifyDataSetChanged();
                                                                recyclerAdapter.notifyDataSetChanged();

                                                                saveData();

                                                                if (tasksList.size()==0 && finishedTasksList.size()!=0){
                                                                    multiAutoCompleteTextView.setVisibility(View.VISIBLE);
                                                                }
                                                                else{
                                                                    multiAutoCompleteTextView.setVisibility(View.GONE);

                                                                }
                                                            }



                                                            if (contains(encodedBytes,'f')){

                                                                int list_index= find(encodedBytes, (byte) 'f');
                                                                int session_end_time = encodedBytes[list_index+1];
                                                                //msg("session duration: "+ String.valueOf(encodedBytes[list_index+1]));

                                                                Bundle bundle = new Bundle();
                                                                bundle.putInt("session_ended_after", session_end_time);
                                                                bundle.putString("session_time", session_start_time);
                                                                firebaseAnalytics.logEvent("session_ended", bundle);

                                                            }


                                                            if (contains(encodedBytes,'e')){

                                                                int list_index= find(encodedBytes, (byte) 'e');
                                                                int pom_end_time = encodedBytes[list_index+1];
                                                                //msg("Pom duration: "+ String.valueOf(encodedBytes[list_index+1]));

                                                                Bundle bundle = new Bundle();
                                                                bundle.putInt("pom_duration", pom_end_time);
                                                                bundle.putString("session_time", session_start_time);
                                                                firebaseAnalytics.logEvent("pom_ended", bundle);

                                                            }

                                                            if (contains(encodedBytes,'s')){
                                                                int list_index= find(encodedBytes, (byte) 's');
                                                                int pom_start_time = encodedBytes[list_index+1];
                                                                //msg("Pom started after: "+ String.valueOf(encodedBytes[list_index+1]));

                                                                Bundle bundle = new Bundle();
                                                                bundle.putInt("duration_from_start", pom_start_time);
                                                                bundle.putString("session_time", session_start_time);
                                                                firebaseAnalytics.logEvent("pom_started", bundle);

                                                            }



                                                        }



                                                        stopWorker = once;


                                                    }
                                                });

                                            } else {

                                                readBuffer[readBufferPosition++] = b;
                                            }

                                        }
                                    }
                                    else{
                                        //isBtConnected=false;
                                    }
                                } catch (IOException ex) {
                                    stopWorker = true;
                                }
                            }
                        }
                    });

                    workerThread.start();

                } catch (Exception e) {
                    // ADD THIS TO SEE ANY ERROR
                    msg("Not connected. Please press CONNECT and try again");
                    textViewTitle.setText("Not connected!");
                    btnBat.setText("CONNECT");
                }


                //recyclerAdapter.notifyDataSetChanged();
            } else {

                msg("Not connected. Please press CONNECT and try again");
                textViewTitle.setText("Not connected!");
                btnBat.setText("CONNECT");



            }
        }
        else{
            msg("Not connected. Please press CONNECT and try again");
            textViewTitle.setText("Not connected!");
            btnBat.setText("CONNECT");

        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopWorker=true;
        //startJob();
        try {
            btSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void applyText(String taskName, boolean editMode, int taskId) {


        if (editMode) {

            tasksList.set(taskId, taskName);
            saveData();
            recyclerAdapter.notifyItemChanged(taskId);
        } else {
            if (btSocket!=null) {

                if (btSocket.isConnected()) {
                    try {

                        btSocket.getOutputStream().write("".getBytes());
                        tasksList.add(taskName);
                        if (tasksList.size()==1){


                            LocalTime time = LocalTime.now();
                            //LocalDateTime time= Loca
                            session_start_time = time.toString();
                            //msg(time.toString());
                            Bundle bundle = new Bundle();
                            bundle.putString("session_time", session_start_time);
                            bundle.putString("user_id", androidId);
                            firebaseAnalytics.logEvent("start_session", bundle);

                        }

                        //recyclerAdapter.notifyItemInserted(tasksList.size()-1);
                        recyclerAdapter.notifyDataSetChanged();

                        saveData();

                        btSocket.getOutputStream().write(String.valueOf(tasksList.size() + finishedTasksList.size()).getBytes());
                        Bundle bundle = new Bundle();
                        bundle.putString("name", taskName);
                        bundle.putString("user_id", androidId);
                        bundle.putString("session_time", session_start_time);
                        firebaseAnalytics.logEvent("task_added", bundle);


                    } catch (IOException e) {

                        msg("Not connected. Please press CONNECT and try again");
                    }
                }
            }
            else{

                msg("Not connected. Please press CONNECT and try again");

            }


        }

    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(ledControl.this, "Loading...", "Please wait");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {

                myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                //BluetoothDevice dispositivo = myBluetooth.getRemoteDevice("24:6F:28:80:E4:82");//connects to the device's address and checks if it's available
                //BluetoothDevice dispositivo = myBluetooth.getRemoteDevice("FC:F5:C4:07:66:5E"); // second prototype
                BluetoothDevice dispositivo = myBluetooth.getRemoteDevice("B4:E6:2D:EA:3B:63"); // test prototype
                btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                btSocket.connect();//start connection

                //if (btSocket == null) {
                //get the mobile bluetooth device
                //BluetoothDevice dispositivo = myBluetooth.getRemoteDevice("24:6F:28:80:E4:82");//connects to the device's address and checks if it's available
                //BluetoothDevice dispositivo_2 = myBluetooth.getRemoteDevice("FC:F5:C4:07:66:5E");


            } catch (IOException e) {
                ConnectSuccess = false;
                progress.dismiss();//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                textViewTitle.setText("Not connected!");

                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    msg("Device not supported!");
                    //finish();
                }

                if (myBluetooth == null || !myBluetooth.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);

                } else {
                    //Toast.makeText(getApplicationContext(), "Connection Failed. Please try again ", Toast.LENGTH_LONG).show();
                    //finish();
                    btnBat.setText("Connect");
                    msg("Connection failed. Check that the Rainmaker is on");


                    //new ConnectBT().execute();
                    isBtConnected = false;
                   // btSocket=null;
                    try {
                        btSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }


            } else {
                
                btnBat.setText("Disconnect");
                textViewTitle.setText("The Rainmaker");

                msg("Connected.");
                isBtConnected = true;

                Refresh(true);
                stopWorker = true;
                Refresh(false);

//                Intent sericeIntent = new Intent(getApplicationContext(),Service.class);
//                sericeIntent.putExtra("blStatus", true);
//
//                startService(sericeIntent);


            }
            progress.dismiss();


        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            new ConnectBT().execute();
        }
    }

    public synchronized String getUserId() {
        if (userID == null) {
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);

            userID = sharedPreferences.getString(PREF_UNIQUE_ID, null);
            if (userID == null) {
                userID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREF_UNIQUE_ID, userID);
                editor.commit();
            }
        }
        return userID;
    }


    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);


        Gson gson = new Gson();
        String json = gson.toJson(tasksList);
        String jsonFinished = gson.toJson(finishedTasksList);
        String bat = textViewBat.getText().toString();



        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("Set1", json);
        editor.putString("Set2", jsonFinished);
        editor.putString("bat", bat);


        editor.commit();

        multiAutoCompleteTextView.setVisibility(View.GONE);

    }

    private void getData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("Set1", null);
        String jsonFinished = sharedPreferences.getString("Set2", null);
        String bat = sharedPreferences.getString("bat", "--");


        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        tasksList = gson.fromJson(json, type);
        finishedTasksList = gson.fromJson(jsonFinished, type);

        textViewBat.setText(bat + '%');


        if (tasksList == null) {
            //msg("nothing :(");
            tasksList = new ArrayList<>();

        }
        if (finishedTasksList == null) {
            //msg("nothing :(");
            finishedTasksList = new ArrayList<>();

        }



        //recyclerAdapter.notifyDataSetChanged();}


    }

    public void startJob()
    {
        ComponentName componentName = new ComponentName(this,ExampleJobService.class);
        JobInfo info= new JobInfo.Builder(123,componentName)

                .build();
        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = scheduler.schedule(info);
        if (resultCode== JobScheduler.RESULT_SUCCESS){
            msg("Job success :)");
        }
        else{

            msg("job failed :(");
        }

    }


    public static boolean contains(final byte[] array, final int v) {

        boolean result = false;

        for(int i : array){
            if(i == v){
                result = true;
                break;
            }
        }

        return result;
    }



    public static int find(byte[] a, byte target)
    {
        for (int i = 0; i < a.length; i++)
            if (a[i] == target)
                return i;

        return -1;
    }







}