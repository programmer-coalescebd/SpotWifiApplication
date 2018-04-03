package com.coalescebd.spotwifi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;

public class Offers extends AppCompatActivity {
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offers);
        webView = findViewById(R.id.web_view_offer);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://revostack.com/projects/scratch-win/");
    }
}
