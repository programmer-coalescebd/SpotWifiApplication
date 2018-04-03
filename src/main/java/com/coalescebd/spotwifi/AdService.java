package com.coalescebd.spotwifi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * Created by User on 11/26/2017.
 */

public class AdService extends Service {
    private static final String TAG = "AdService";
    private static final String LOG_TAG = "AdService";
    String address;
    String rAddress;
    int a;

    public AdService(){}

    @Override
    public void onCreate() {
        Log.i(TAG,"Service onCreate");

        //getting MAC address of the device
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        address=info.getMacAddress();
        boolean isRunning = true;
        //getting router's ip address
        final DhcpInfo dhcpInfo = manager.getDhcpInfo();
        a=dhcpInfo.gateway;
        rAddress= String.valueOf(intToInetAddress(a));
        Log.d(TAG,"Router IP: "+rAddress);


    }

    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Let it continue running until it is stopped
        if (intent.getExtras().getBoolean("run service")){
            Log.i(LOG_TAG,"Received Start Foreground Intent ");
            showNotification();
        }else {
            Log.i(LOG_TAG,"Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

}
