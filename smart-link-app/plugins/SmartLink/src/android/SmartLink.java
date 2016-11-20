package SmartLink;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

// smart link lib
import com.hiflying.smartlink.ISmartLinker;
import com.hiflying.smartlink.OnSmartLinkListener;
import com.hiflying.smartlink.SmartLinkedModule;
import com.hiflying.smartlink.v3.SnifferSmartLinker;
import com.hiflying.smartlink.v7.MulticastSmartLinker;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.ProgressDialog;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.app.Activity;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.Toast;

/**
 * This class echoes a string called from JavaScript.
 */
public class SmartLink extends CordovaPlugin implements OnSmartLinkListener {

    public static final String EXTRA_SMARTLINK_VERSION = "EXTRA_SMARTLINK_VERSION";
    protected ISmartLinker mSnifferSmartLinker;
    private boolean mIsConncting = false;
    protected Handler mViewHandler = new Handler();
    protected ProgressDialog mWaitingDialog;
    private BroadcastReceiver mWifiChangedReceiver;
    private  Activity activity;
    private static final String TAG = "SmartLink";

    // 连接状态消息
    private  CharSequence TIME_OUT = "Connect timeout...";
    private  CharSequence MODUE_TOUND = "Module Found: \r\n";
    private CharSequence COMPLETE = "Completed!";


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webview) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();

        int smartLinkVersion = cordova.getActivity().getIntent().getIntExtra(EXTRA_SMARTLINK_VERSION, 3);

        if(smartLinkVersion == 7) {
            mSnifferSmartLinker = MulticastSmartLinker.getInstance();
        }else {
            mSnifferSmartLinker = SnifferSmartLinker.getInstance();
        }

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        return false;
    }

    private void stopConnect() {
        mSnifferSmartLinker.setOnSmartLinkListener(null);
        mSnifferSmartLinker.stop();
        mIsConncting = false;
    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mSnifferSmartLinker.setOnSmartLinkListener(null);
        try {
            activity.unregisterReceiver(mWifiChangedReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onLinked(final SmartLinkedModule module) {
        // TODO Auto-generated method stub

        Log.w(TAG, "onLinked");
        mViewHandler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(
                        activity,
                        MODUE_TOUND +  module.getMac() + module.getModuleIP(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onCompleted() {

        Log.w(TAG, "onCompleted");
        mViewHandler.post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                Toast.makeText(
                        activity,
                        COMPLETE,
                        Toast.LENGTH_SHORT).show();
                mWaitingDialog.dismiss();
                mIsConncting = false;
            }
        });
    }

    @Override
    public void onTimeOut() {

        mViewHandler.post(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub


                Toast.makeText(
                        activity,
                        TIME_OUT,
                        Toast.LENGTH_SHORT).show();
                mWaitingDialog.dismiss();
                mIsConncting = false;
            }
        });
    }

    private String getSSid(){

        WifiManager wm = (WifiManager) activity.getSystemService(activity.WIFI_SERVICE);
        if(wm != null){
            WifiInfo wi = wm.getConnectionInfo();
            if(wi != null){
                String ssid = wi.getSSID();
                if(ssid.length()>2 && ssid.startsWith("\"") && ssid.endsWith("\"")){
                    return ssid.substring(1,ssid.length()-1);
                }else{
                    return ssid;
                }
            }
        }

        return "";
    }

    private void connect(JSONArray args, CallbackContext callbackContext) {
        if(!mIsConncting){

            //设置要配置的ssid 和pswd
            try {
                Log.w(TAG,  "SSID is: " + args.getString(0));
                Log.w(TAG,  "PWD is: " + args.getString(1));

                mSnifferSmartLinker.setOnSmartLinkListener(SmartLink.this);
                //开始 smartLink
                mSnifferSmartLinker.start(
                        activity,
                        args.getString(0),
                        args.getString(1)
                );
                mIsConncting = true;
                mWaitingDialog.show();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }


}
