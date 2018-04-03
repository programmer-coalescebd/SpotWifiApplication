package com.coalescebd.spotwifi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main Activity debug";
    private static final String MY_PREFS_NAME = "SharedPreference";
    String address, rAddress;
    String key = null;
    TextView txtConnecting;
    Activity mActivity;
    VideoView videoView;
    Context context;
    static Context sContext;
    private Handler mHandler = new Handler();
    int a;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (isFirstTime()) {
            final AlertDialog.Builder builder =  new AlertDialog.Builder(this);
            builder.setTitle("DISCLIAMER")
                    .setMessage(R.string.disclaimer_text)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    }).show();

        }
        GoogleAnalyticsApplication application = (GoogleAnalyticsApplication) getApplication();
        mTracker=application.getDefaultTracker();

        mActivity = this;
        context = getApplicationContext();
        MainActivity.sContext = getAppContext();

        //getting device's MAC address
        final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()==false){
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled",Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
        address = getMacAddress();
        Log.d(TAG, "Mac Address: " + address);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        Log.d(TAG,"call_state"+telephonyManager.getCallState());

        //getting router's ip
        final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        a=dhcpInfo.gateway;
        rAddress= String.valueOf(intToInetAddress(a));
        Log.d(TAG, "Router IP: " + rAddress);

        //Streaming ad
        videoView = findViewById(R.id.adVideo);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //incrementing view count of the portal ad
                RequestQueue requestQueue = Volley.newRequestQueue(mActivity);
                String url = "http://104.237.58.82/fileentry/view/"+1;
                String videoAddress = "http://"+"192.168.42.1"+":5000"+getString(R.string.ad_url)+1;
                StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.GET, url, new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "response from the url is: " + response);
                    }
                }, new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG,"unable to get a valid response");
                    }
                });
                requestQueue.add(stringRequest);
                Uri videoUri = Uri.parse(videoAddress);
                videoView.setVideoURI(videoUri);
                try {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            videoView.requestFocus();
                            videoView.start();
                        }
                    });
                }catch (Exception e){}
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.setVolume(0.2f, 0.2f);
                    }
                });
                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        finish();
                        launchAdIn(5);
                    }
                });
            }
        }).start();
        //enabling internet
        txtConnecting = findViewById(R.id.txt_connecting);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtConnecting.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();



        //set serial count
        setDefaults("currentSerial", 1, getApplicationContext());
        new Thread(new Runnable() {
            @Override
            public void run() {
                String response = httpGet(getString(R.string.is_connected_url));
                try {
                    if (response.equals("true")) {
                        //set the text of connect button to "Connected"
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtConnecting.setText(R.string.btn_connected);
                                startService(true);
                            }
                        });
                        try {
                            Log.d("DEBUG", response);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String response = httpGet(getString(R.string.mac_url)+address);
                                    try {
                                        Log.d("DEBUG", response);
                                    } catch (NullPointerException ex) {
                                        Log.d("DEBUG", "Null response");
                                    }
                                }
                            }).start();
                        } catch (NullPointerException ex) {
                            Log.d("DEBUG", "Null response");
                        }
                    }
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTracker.setScreenName("Main Screen");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        new Thread(new Runnable() {
            @Override
            public void run() {
                String response = httpGet(getString(R.string.is_internet_url)+address);
                try {
                    if (response.toString().equals("false")) {
                        //set the text of connect button to "Connect to Internet"
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtConnecting.setText(R.string.btn_connect_to_internet);
                                startService(false);
                            }
                        });
                    } else {
                        //set the text of connect button to "connected"
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtConnecting.setText(R.string.btn_connected);
                                startService(true);
                            }
                        });
                    }
                    Log.d("DEBUG", response);
                } catch (NullPointerException ex) {
                    //set the text of connect button to "Connect to the internet"
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtConnecting.setText(R.string.btn_connect_to_internet);
                        }
                    });
                }
            }
        }).start();
    }

    private void startService(Boolean runService) {
        Intent intent = new Intent(getBaseContext(),AdService.class);
        intent.putExtra("runService",runService);
        startService(intent);
    }

    String httpGet(String url) {
        Log.d(TAG, "httpGet: "+"http://" + rAddress+":5000"+url);
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        try {
            Request request = new Request.Builder()
                    .url("http://"+rAddress+":5000"+url)
                    .build();
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
        }
        return null;
    }

    public static void setDefaults(String key, int value, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key,value);
        editor.commit();
    }

    public static int getDefaults(String key, Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(key,4);
    }

    public static Context getAppContext() {
        return MainActivity.sContext;
    }

    //getting MAC Address
    public static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return " ";
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

    public void launchAdIn(int minute) {
        Log.d(TAG, "launchAdIn: 1st time");
        //launch ad with alarm manager
        Intent intent = new Intent(getBaseContext(), ScheduledReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, intent, 0);
        //Later we wil use "Job Scheduler" API instead of "Alarm Manager"
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.SECOND, 10);
        long interval = minute * 60 * 1000;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, pendingIntent);
        Log.d(TAG, "launchAdIn: alarm set");
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

    private boolean isFirstTime()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("RanBefore", false);
        if (!ranBefore) {
            // first time app has been run
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("RanBefore", true);
            editor.apply();
        }
        return !ranBefore;
    }
}

