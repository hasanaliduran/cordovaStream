/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;


public class CordovaActivity extends Activity {
    public static String TAG = "CordovaActivity";

    
    protected CordovaWebView appView;

    private static int ACTIVITY_STARTING = 0;
    private static int ACTIVITY_RUNNING = 1;
    private static int ACTIVITY_EXITING = 2;

   
    protected boolean keepRunning = true;

    
    protected boolean immersiveMode;

    
    protected CordovaPreferences preferences;
    protected String launchUrl;
    protected ArrayList<PluginEntry> pluginEntries;
    protected CordovaInterfaceImpl cordovaInterface;

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
       
        loadConfig();

        String logLevel = preferences.getString("loglevel", "ERROR");
        LOG.setLogLevel(logLevel);

        LOG.i(TAG, "Apache Cordova native platform version " + CordovaWebView.CORDOVA_VERSION + " is starting");
        LOG.d(TAG, "CordovaActivity.onCreate()");

        if (!preferences.getBoolean("ShowTitle", false)) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        if (preferences.getBoolean("SetFullscreen", false)) {
            LOG.d(TAG, "The SetFullscreen configuration is deprecated in favor of Fullscreen, and will be removed in a future version.");
            preferences.set("Fullscreen", true);
        }
        if (preferences.getBoolean("Fullscreen", false)) {
           
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) && !preferences.getBoolean("FullscreenNotImmersive", false)) {
                immersiveMode = true;
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }

        super.onCreate(savedInstanceState);

        cordovaInterface = makeCordovaInterface();
        if (savedInstanceState != null) {
            cordovaInterface.restoreInstanceState(savedInstanceState);
        }
    }

    protected void init() {
        appView = makeWebView();
        createViews();
        if (!appView.isInitialized()) {
            appView.init(cordovaInterface, pluginEntries, preferences);
        }
        cordovaInterface.onCordovaInit(appView.getPluginManager());
		
        String volumePref = preferences.getString("DefaultVolumeStream", "");
        if ("media".equals(volumePref.toLowerCase(Locale.ENGLISH))) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }

    @SuppressWarnings("deprecation")
    protected void loadConfig() {
        ConfigXmlParser parser = new ConfigXmlParser();
        parser.parse(this);
        preferences = parser.getPreferences();
        preferences.setPreferencesBundle(getIntent().getExtras());
        launchUrl = parser.getLaunchUrl();
        pluginEntries = parser.getPluginEntries();
        Config.parser = parser;
    }

   
    @SuppressWarnings({"deprecation", "ResourceType"})
    protected void createViews() {
        
        appView.getView().setId(100);
        appView.getView().setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(appView.getView());

        if (preferences.contains("BackgroundColor")) {
            try {
                int backgroundColor = preferences.getInteger("BackgroundColor", Color.YELLOW);
               
                appView.getView().setBackgroundColor(backgroundColor);
            }
            catch (NumberFormatException e){
                e.printStackTrace();
            }
        }

        appView.getView().requestFocusFromTouch();
    }

   
    protected CordovaWebView makeWebView() {
        return new CordovaWebViewImpl(makeWebViewEngine());
    }

    protected CordovaWebViewEngine makeWebViewEngine() {
        return CordovaWebViewImpl.createEngine(this, preferences);
    }

    protected CordovaInterfaceImpl makeCordovaInterface() {
        return new CordovaInterfaceImpl(this) {
            @Override
            public Object onMessage(String id, Object data) {
                // Plumb this to CordovaActivity.onMessage for backwards compatibility
                return CordovaActivity.this.onMessage(id, data);
            }
        };
    }

    
    public void loadUrl(String url) {
        if (appView == null) {
            init();
        }

        
        this.keepRunning = preferences.getBoolean("KeepRunning", true);

        appView.loadUrlIntoView(url, true);
    }

    
    @Override
    protected void onPause() {
        super.onPause();
        LOG.d(TAG, "Paused the activity.");

        if (this.appView != null) {
            
            boolean keepRunning = this.keepRunning || this.cordovaInterface.activityResultCallback != null;
            this.appView.handlePause(keepRunning);
        }
    }

   
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        if (this.appView != null)
            this.appView.onNewIntent(intent);
    }

    
    @Override
    protected void onResume() {
        super.onResume();
        LOG.d(TAG, "Resumed the activity.");

        if (this.appView == null) {
            return;
        }
        
        this.getWindow().getDecorView().requestFocus();

        this.appView.handleResume(this.keepRunning);
    }

   
    @Override
    protected void onStop() {
        super.onStop();
        LOG.d(TAG, "Stopped the activity.");

        if (this.appView == null) {
            return;
        }
        this.appView.handleStop();
    }

   
    @Override
    protected void onStart() {
        super.onStart();
        LOG.d(TAG, "Started the activity.");

        if (this.appView == null) {
            return;
        }
        this.appView.handleStart();
    }

   
    @Override
    public void onDestroy() {
        LOG.d(TAG, "CordovaActivity.onDestroy()");
        super.onDestroy();

        if (this.appView != null) {
            appView.handleDestroy();
        }
    }

   
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && immersiveMode) {
            final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
       
        cordovaInterface.setActivityResultRequestCode(requestCode);
        super.startActivityForResult(intent, requestCode, options);
    }

    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOG.d(TAG, "Incoming Result. Request code = " + requestCode);
        super.onActivityResult(requestCode, resultCode, intent);
        cordovaInterface.onActivityResult(requestCode, resultCode, intent);
    }

    
    public void onReceivedError(final int errorCode, final String description, final String failingUrl) {
        final CordovaActivity me = this;

        
        final String errorUrl = preferences.getString("errorUrl", null);
        if ((errorUrl != null) && (!failingUrl.equals(errorUrl)) && (appView != null)) {
            
            me.runOnUiThread(new Runnable() {
                public void run() {
                    me.appView.showWebPage(errorUrl, false, true, null);
                }
            });
        }
        
        else {
            final boolean exit = !(errorCode == WebViewClient.ERROR_HOST_LOOKUP);
            me.runOnUiThread(new Runnable() {
                public void run() {
                    if (exit) {
                        me.appView.getView().setVisibility(View.GONE);
                        me.displayError("Application Error", description + " (" + failingUrl + ")", "OK", exit);
                    }
                }
            });
        }
    }

    
    public void displayError(final String title, final String message, final String button, final boolean exit) {
        final CordovaActivity me = this;
        me.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    AlertDialog.Builder dlg = new AlertDialog.Builder(me);
                    dlg.setMessage(message);
                    dlg.setTitle(title);
                    dlg.setCancelable(false);
                    dlg.setPositiveButton(button,
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    if (exit) {
                                        finish();
                                    }
                                }
                            });
                    dlg.create();
                    dlg.show();
                } catch (Exception e) {
                    finish();
                }
            }
        });
    }

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (appView != null) {
            appView.getPluginManager().postMessage("onCreateOptionsMenu", menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (appView != null) {
            appView.getPluginManager().postMessage("onPrepareOptionsMenu", menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (appView != null) {
            appView.getPluginManager().postMessage("onOptionsItemSelected", item);
        }
        return true;
    }

    
    public Object onMessage(String id, Object data) {
        if ("onReceivedError".equals(id)) {
            JSONObject d = (JSONObject) data;
            try {
                this.onReceivedError(d.getInt("errorCode"), d.getString("description"), d.getString("url"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if ("exit".equals(id)) {
            finish();
        }
        return null;
    }

    protected void onSaveInstanceState(Bundle outState) {
        cordovaInterface.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

   
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.appView == null) {
            return;
        }
        PluginManager pm = this.appView.getPluginManager();
        if (pm != null) {
            pm.onConfigurationChanged(newConfig);
        }
    }

  
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        try
        {
            cordovaInterface.onRequestPermissionResult(requestCode, permissions, grantResults);
        }
        catch (JSONException e)
        {
            LOG.d(TAG, "JSONException: Parameters fed into the method are not valid");
            e.printStackTrace();
        }

    }

}
