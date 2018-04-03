package com.coalescebd.spotwifi;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by User on 11/26/2017.
 */

public class LogicActivity extends AppCompatActivity {
    private static final String TAG = "Logic Activity debug";
    String rAddress;
    Context context;
    int a;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getApplicationContext();
        try {
            //getting router ip
            WifiManager manager=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final DhcpInfo dhcpInfo=manager.getDhcpInfo();
            a=dhcpInfo.gateway;
            rAddress= String.valueOf(intToInetAddress(a));
            Log.d(TAG,"Router IP: "+ rAddress);
        }catch (Exception ex){
            Log.d(TAG,"onCreate: "+ex.toString());
        }
        TelephonyManager telephonyManager =(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        PhoneStateListener callStateListener = new PhoneStateListener(){
            public void onCallStateChanged(int state, String incomingNumber){
                KeyguardManager keyguardManager=(KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
                boolean locked = keyguardManager.inKeyguardRestrictedInputMode();
                System.out.println(locked);
                if ((state==TelephonyManager.CALL_STATE_IDLE)&&!locked){
                    startAdd();
                }
            }
        };
        telephonyManager.listen(callStateListener,PhoneStateListener.LISTEN_CALL_STATE);
        finish();
    }

    private void startAdd(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = getString(R.string.is_internet_url)+getMacAddress();
                    String response = httpGet(url);
                    if (response.equals("false")) {
                        finish();
                    } else {
                        Intent scheduledIntent = new Intent(context, AdActivity.class);
                        scheduledIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(scheduledIntent);
                        finish();
                    }
                    Log.d("DEBUG", response);
                }catch (NullPointerException ex){
                }
            }
        }).start();
    }
    String httpGet(String url){
        Log.d(TAG,"httpGet: "+"http://"+rAddress+":5000"+url);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .readTimeout(10,TimeUnit.SECONDS)
                .build();
        try {
            Request request = new Request.Builder()
                    .url("http://"+rAddress+":5000"+url)
                    .build();
            Response response = client.newCall(request).execute();
            Log.d(TAG, "httpGet: "+response.toString());
            return response.body().string();
        }catch (IOException ex){
            ex.printStackTrace();
        }
        return null;
    }


    public static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(Integer.toHexString(b & 0xFF) + ":");
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
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
