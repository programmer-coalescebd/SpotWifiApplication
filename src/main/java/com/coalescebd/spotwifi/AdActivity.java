package com.coalescebd.spotwifi;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AdActivity extends AppCompatActivity {

    private static final String TAG = "AdActivity";
    private static final String MY_PREFS_NAME = "SharedPreference";

    VideoView vidView;
    String rAddress;
    TextView skipTextArea;
    Boolean isSkipable;
    int currentAdSerial;
    String address;
    int a;
    Activity aActivity;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad);
        aActivity=this;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (telephonyManager != null&&powerManager!=null) {
            if (telephonyManager.getCallState()==0){
                if (Build.VERSION.SDK_INT>20){
                    if (powerManager.isScreenOn()){
                        init();
                    }
                }
                else{
                    init();
                }
            }else
                finish();
        }

    }

    void init(){
        address = getMacAddr();

        //get router ip
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final DhcpInfo dhcp = manager.getDhcpInfo();
        a = dhcp.gateway;
        rAddress = String.valueOf(intToInetAddress(a));
        Log.d(TAG, "Router IP: "+ "192.168.42.1");

        //stream ad
        vidView = findViewById(R.id.adServiceVideo);

        currentAdSerial = MainActivity.getDefaults("currentSerial",getApplicationContext());
        Log.d(TAG, "retried serial  "+ currentAdSerial);

        new Thread(new Runnable() {
            int randomSerial = getRandomNumberFrom(2,8);
            @Override
            public void run() {
                //updating view count of the ad in the remote server
                try {
                    RequestQueue queue = Volley.newRequestQueue(aActivity);
                    String url = "http://104.237.58.82/fileentry/view/"+randomSerial;
                    StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.GET, url,new com.android.volley.Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    Log.d(TAG,"response from the url is: "+response);
                                }
                            }, new com.android.volley.Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "That didn't work!");
                        }
                    });
                    queue.add(stringRequest);
                }catch (Exception e){}

                try {
                    aActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            vidView.setVideoURI(Uri.parse("http://"+"192.168.42.1"+":5000"+getString(R.string.ad_url)+ String.valueOf(randomSerial)));
                            vidView.requestFocus();
                            vidView.start();
                        }
                    });
                }catch (Exception e){}

                //kill activity after video play finished
                vidView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        finish();
                        Intent intent = new Intent(AdActivity.this,Offers.class);
                        startActivity(intent);
                    }
                });

                vidView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        //start timer
                        startTimerThread();
                        mediaPlayer.setVolume(0.1f,0.1f);
                    }
                });
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                try{
                    Log.d(TAG, "run: currentSerial " + currentAdSerial);
                    int adCount = Integer.parseInt(httpGet("/getAdCount"));
                    Log.d(TAG, "run: AdCount "+adCount);
                    String meta = httpGet("/getAdMeta?serial="+currentAdSerial);
                    Log.d(TAG, "run: Meta"+ meta);
                    if(currentAdSerial>=adCount){
                        currentAdSerial = 0;
                    }

                    MainActivity.setDefaults("currentSerial", currentAdSerial+1 ,getApplicationContext());
                }catch (Exception ex){
                    Log.d(TAG, "run: Error "+ex.toString());
                }
            }
        }).start();

        //start video play
        vidView.start();

        //get skip Text Area
        skipTextArea = findViewById(R.id.skipTextArea);
        skipTextArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isSkipable){
                    finish();
                    Intent intent = new Intent(AdActivity.this,Offers.class);
                }
            }
        });

        new Thread(new Runnable() {
            public void run() {
                String response = httpGet(getString(R.string.mac_url)+address);
                try{
                    Log.d(TAG ,response+" for 20 mins");
                }catch (NullPointerException ex){
                    Log.d("DEBUG","Null response");
                }
            }
        }).start();

    }

    private void startTimerThread() {
        isSkipable = false;
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            private long startTime = System.currentTimeMillis();
            public void run() {

                for (int i = 10; i >= 0; i--)  {
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable(){
                        public void run() {
                            int skipTime = 10;

                            skipTextArea.setText("Skip in " + (skipTime - ((System.currentTimeMillis() - startTime) / 1000)));
                            if((skipTime - ((System.currentTimeMillis() - startTime) / 1000))<=0){
                                skipTextArea.setText("Skip");
                                isSkipable = true;
                            }
                        }
                    });
                }

            }
        };
        new Thread(runnable).start();
    }

    String httpGet(String url){
        Log.d(TAG, "httpGet: "+"http://"+"192.168.42.1"+":5000"+url);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        try{
            Request request = new Request.Builder()
                    .url("http://"+"192.168.42.1"+":5000"+url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }catch (IOException ex){
        }
        return null;
    }

    String httpGetGlobal(String url){
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        try{
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }catch (IOException ex){
        }
        return null;
    }

    public static String getMacAddr() {
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

    public static int getRandomNumberFrom(int min, int max) {
        Random foo = new Random();
        int randomNumber = foo.nextInt((max + 1) - min) + min;
        Log.d(TAG,"randomly generated number is: "+randomNumber);
        return randomNumber;
    }
    private boolean lastPlayedAdSerial(int serial){
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        int previousAdSerial = preferences.getInt("Previous Ad Serial",0);
        if (previousAdSerial!=serial){
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("Previous Ad Serial",serial);
            editor.apply();
            return true;
        }
        else
            return false;
    }
}
