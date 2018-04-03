package com.coalescebd.spotwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by User on 11/26/2017.
 */

public class ScheduledReciever extends BroadcastReceiver {
    private static final String TAG = "ScheduledReciever";
    String address;
    String raddress;
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent scheduledIntent = new Intent(context,LogicActivity.class);
        scheduledIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(scheduledIntent);
    }
    String httpGet(String url){
        final String url2 = url;
        final StringBuilder sb = new StringBuilder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"httpGet:"+"http://192.168.42.1:5000"+url2);
                OkHttpClient client=new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10,TimeUnit.SECONDS)
                        .readTimeout(10,TimeUnit.SECONDS)
                        .build();
                try {
                    Request request = new Request.Builder()
                            .url("http://192.168.42.1:5000"+url2)
                            .build();
                    Response response = client.newCall(request).execute();
                    sb.append(response.body().string());
                } catch (IOException e) {
                }
            }
        });
        return sb.toString();
    }
    public static String getMacAddress(){
        try{
            List<NetworkInterface>all= Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all){
                if (!nif.getName().equalsIgnoreCase("wlan0"))continue;
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes==null){
                    return "";
                }
                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes){
                    res1.append(Integer.toHexString(b & 0xFF)+":");
                }
                if (res1.length()>0){
                    res1.deleteCharAt(res1.length()-1);
                }
                return res1.toString();
            }
        }catch (Exception ex){
        }
        return "02:00:00:00:00:00";
    }
}
