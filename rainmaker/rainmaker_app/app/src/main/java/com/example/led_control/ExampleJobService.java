package com.example.led_control;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;


public class ExampleJobService extends JobService {

    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;


    public static BluetoothSocket btSocket = null;

    BluetoothAdapter myBluetooth = BluetoothAdapter.getDefaultAdapter();

    private boolean jobCancelled = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public boolean onStartJob(JobParameters params) {
        Toast.makeText(getApplicationContext(), "JobStarted!", Toast.LENGTH_SHORT).show();
        doBackgroundWork(params);

        return true;
    }

    private void doBackgroundWork(JobParameters params) {


        try {
            myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
            //BluetoothDevice dispositivo = myBluetooth.getRemoteDevice("24:6F:28:80:E4:82");//connects to the device's address and checks if it's available
            BluetoothDevice dispositivo = myBluetooth.getRemoteDevice("FC:F5:C4:07:66:5E"); // second prototype
            btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            btSocket.connect();


        } catch (IOException e) {

            return;

            //if the try failed, you can check the exception here
        }


        if (btSocket != null) {

            if (btSocket.isConnected() && (myBluetooth.isEnabled())) {
                try {

                    final Handler handler = new Handler();
                    stopWorker = false;
                    readBufferPosition = 0;
                    readBuffer = new byte[1024];

                    workerThread = new Thread(new Runnable() {
                        public void run() {
                            while (!Thread.currentThread().isInterrupted() && !stopWorker) {

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

                                                            msg("stuffrecieved!");

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

                                                            if (batLvl < 80 && batLvl > 20) {

                                                                msg("all good");
                                                            }


                                                            if (batLvl < 20 && batLvl > 0) {

                                                                msg("low_bat :(");
                                                            }


                                                            int finihsed_n = encodedBytes[1];
                                                            //msg(String.valueOf(finihsed_n));

                                                        }
                                                        if (jobCancelled) {

                                                            stopWorker = true;
                                                        } else {
                                                            stopWorker = false;
                                                        }


                                                    }
                                                });

                                            } else {

                                                readBuffer[readBufferPosition++] = b;
                                            }

                                        }
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

                }


                //recyclerAdapter.notifyDataSetChanged();
            } else {

                msg("Not connected. Please press CONNECT and try again");


            }
        } else {
            msg("Not connected. Please press CONNECT and try again");


        }

    }

    @Override
    public boolean onStopJob(JobParameters params) {
        jobCancelled = true;

        return false;
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }


}
