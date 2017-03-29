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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;

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
    protected ProgressDialog mWaitingDialog;
    private BroadcastReceiver mWifiChangedReceiver;
    private  Activity activity;
    private static final String TAG = "SmartLink";
    private CallbackContext callbackCtx;

    // 连接状态消息
    private  CharSequence TIME_OUT = "连接超时...";
    private  CharSequence MODUE_TOUND = "模块发现: \r\n";
    private CharSequence COMPLETE = "连接成功!";
    private CharSequence WATING = "请等待...";
    private String CANCEL = "已取消.";


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

        mWaitingDialog = new ProgressDialog(activity);
        mWaitingDialog.setMessage(WATING);

        mWaitingDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, activity.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        mWaitingDialog.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {

                mSnifferSmartLinker.setOnSmartLinkListener(null);
                mSnifferSmartLinker.stop();
                mIsConncting = false;
                callbackCtx.error(CANCEL);
            }
        });


        mWifiChangedReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Log.w(TAG, "WIFI changed.");
                ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(activity.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (networkInfo != null && networkInfo.isConnected()) {
                    getSSID(null, callbackCtx);
                }else {
                    if (mWaitingDialog.isShowing()) {
                        mWaitingDialog.dismiss();
                    }
                }
            }
        };

        activity.registerReceiver(mWifiChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        callbackCtx = callbackContext;

        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        } else if(action.equals("getSSID")){

            this.getSSID(args, callbackContext);
            return true;
        } else if(action.equals("connect")){

            this.connect(args, callbackContext);
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
            callbackCtx = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onLinked(final SmartLinkedModule module) {
        // TODO Auto-generated method stub

        Log.w(TAG, "onLinked");
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "before get MAC info");

                Log.w(TAG, MODUE_TOUND +  module.getMac() + " " + module.getIp());

                callbackCtx.success(
                    String.format("%s$%s", module.getMac(), module.getIp())
                );

                if (mWaitingDialog.isShowing()) {
                    mWaitingDialog.dismiss();
                }
            }
        });
    }


    @Override
    public void onCompleted() {

        Log.w(TAG, "onCompleted");
        cordova.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                Toast.makeText(
                        activity,
                        COMPLETE,
                        Toast.LENGTH_SHORT).show();
                if (mWaitingDialog.isShowing()) {
                    mWaitingDialog.dismiss();
                }
                mIsConncting = false;
            }
        });
    }

    @Override
    public void onTimeOut() {

        cordova.getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                Toast.makeText(
                        activity,
                        TIME_OUT,
                        Toast.LENGTH_SHORT).show();
                if (mWaitingDialog.isShowing()) {
                    mWaitingDialog.dismiss();
                }
                mIsConncting = false;

                callbackCtx.error("Connect Timeout.");
            }
        });
    }

    private String getSSID(JSONArray args, CallbackContext callbackContext){
        Log.w(TAG, "Get SSID...");

        WifiManager wm = (WifiManager) activity.getSystemService(activity.WIFI_SERVICE);
        if(wm != null){
            WifiInfo wi = wm.getConnectionInfo();
            if(wi != null){
                String ssid = wi.getSSID();
                if(ssid.length()>2 && ssid.startsWith("\"") && ssid.endsWith("\"")){
                    ssid = ssid.substring(1,ssid.length()-1);
                }

                Log.w(TAG, "SSID is: " + ssid);
                callbackContext.success(ssid);

                return ssid;
            }
        }

        return "";
    }

    private void connect(JSONArray args, CallbackContext callbackContext) {
        if(!mIsConncting){

            //设置要配置的ssid 和pswd
            try {
                JSONObject obj = new JSONObject(args.getString(0));

                Log.w(TAG,  "SSID is: " + obj.getString("wifiName"));
                Log.w(TAG,  "PWD is: " + obj.getString("wifiPass"));

                mSnifferSmartLinker.setOnSmartLinkListener(SmartLink.this);
                //开始 smartLink
                mSnifferSmartLinker.start(
                        activity,
                        obj.getString("wifiPass"),
                        obj.getString("wifiName")
                );
                mIsConncting = true;
                // mWaitingDialog.show();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                mIsConncting = false;
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
