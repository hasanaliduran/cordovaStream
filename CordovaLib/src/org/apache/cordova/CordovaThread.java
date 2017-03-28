

package org.apache.cordova;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CordovaThread implements CordovaInterface {
    private static final String TAG = "CordovaInterfaceImpl";
    protected Activity activity;
    protected ExecutorService threadPool;
    protected PluginManager pluginManager;

    protected ActivityResultHolder savedResult;
    protected CallbackMap permissionResultCallbacks;
    protected CordovaPlugin activityResultCallback;
    protected String initCallbackService;
    protected int activityResultRequestCode;
    protected boolean activityWasDestroyed = false;
    protected Bundle savedPluginState;

    public CordovaThread(Activity activity) {
        this(activity, Executors.newCachedThreadPool());
    }

    public CordovaThread(Activity activity, ExecutorService threadPool) {
        this.activity = activity;
        this.threadPool = threadPool;
        this.permissionResultCallbacks = new CallbackMap();
    }

    @Override
    public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
        setActivityResultCallback(command);
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (RuntimeException e) { // E.g.: ActivityNotFoundException
            activityResultCallback = null;
            throw e;
        }
    }

    @Override
    public void setActivityResultCallback(CordovaPlugin plugin) {
        
        if (activityResultCallback != null) {
            activityResultCallback.onActivityResult(activityResultRequestCode, Activity.RESULT_CANCELED, null);
        }
        activityResultCallback = plugin;
    }

    @Override
    public Activity getActivity() {
        return activity;
    }

    @Override
    public Object onMessage(String id, Object data) {
        if ("exit".equals(id)) {
            activity.finish();
        }
        return null;
    }

    @Override
    public ExecutorService getThreadPool() {
        return threadPool;
    }

    
    public void onCordovaInit(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        if (savedResult != null) {
            onActivityResult(savedResult.requestCode, savedResult.resultCode, savedResult.intent);
        } else if(activityWasDestroyed) {
           
            activityWasDestroyed = false;
            if(pluginManager != null)
            {
                CoreAndroid appPlugin = (CoreAndroid) pluginManager.getPlugin(CoreAndroid.PLUGIN_NAME);
                if(appPlugin != null) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("action", "resume");
                    } catch (JSONException e) {
                        LOG.e(TAG, "Failed to create event message", e);
                    }
                    appPlugin.sendResumeEvent(new PluginResult(PluginResult.Status.OK, obj));
                }
            }

        }
    }

   
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        CordovaPlugin callback = activityResultCallback;
        if(callback == null && initCallbackService != null) {
          
            savedResult = new ActivityResultHolder(requestCode, resultCode, intent);
            if (pluginManager != null) {
                callback = pluginManager.getPlugin(initCallbackService);
                if(callback != null) {
                    callback.onRestoreStateForActivityResult(savedPluginState.getBundle(callback.getServiceName()),
                            new ResumeCallback(callback.getServiceName(), pluginManager));
                }
            }
        }
        activityResultCallback = null;

        if (callback != null) {
            LOG.d(TAG, "Sending activity result to plugin");
            initCallbackService = null;
            savedResult = null;
            callback.onActivityResult(requestCode, resultCode, intent);
            return true;
        }
        LOG.w(TAG, "Got an activity result, but no plugin was registered to receive it" + (savedResult != null ? " yet!" : "."));
        return false;
    }

   
    public void setActivityResultRequestCode(int requestCode) {
        activityResultRequestCode = requestCode;
    }

    
    public void onSaveInstanceState(Bundle outState) {
        if (activityResultCallback != null) {
            String serviceName = activityResultCallback.getServiceName();
            outState.putString("callbackService", serviceName);
        }
        if(pluginManager != null){
            outState.putBundle("plugin", pluginManager.onSaveInstanceState());
        }

    }

   
    public void restoreInstanceState(Bundle savedInstanceState) {
        initCallbackService = savedInstanceState.getString("callbackService");
        savedPluginState = savedInstanceState.getBundle("plugin");
        activityWasDestroyed = true;
    }

    private static class ActivityResultHolder {
        private int requestCode;
        private int resultCode;
        private Intent intent;

        public ActivityResultHolder(int requestCode, int resultCode, Intent intent) {
            this.requestCode = requestCode;
            this.resultCode = resultCode;
            this.intent = intent;
        }
    }

   
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        Pair<CordovaPlugin, Integer> callback = permissionResultCallbacks.getAndRemoveCallback(requestCode);
        if(callback != null) {
            callback.first.onRequestPermissionResult(callback.second, permissions, grantResults);
        }
    }

    public void requestPermission(CordovaPlugin plugin, int requestCode, String permission) {
        String[] permissions = new String [1];
        permissions[0] = permission;
        requestPermissions(plugin, requestCode, permissions);
    }

    public void requestPermissions(CordovaPlugin plugin, int requestCode, String [] permissions) {
        int mappedRequestCode = permissionResultCallbacks.registerCallback(plugin, requestCode);
        getActivity().requestPermissions(permissions, mappedRequestCode);
    }

    public boolean hasPermission(String permission)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int result = activity.checkSelfPermission(permission);
            return PackageManager.PERMISSION_GRANTED == result;
        }
        else
        {
            return true;
        }
    }
}
