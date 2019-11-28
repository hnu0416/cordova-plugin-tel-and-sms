package org.apache.cordova.telandsms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class TELAndSMS extends CordovaPlugin {
    private static String PHONE_STATE_FILTER = "android.intent.action.PHONE_STATE";
    private static String SENT_SMS_ACTION = "SENT_SMS_ACTION";
    private static final int CALL_PHONE_REQUEST_CODE = 0;
    private static final int SEND_SMS_REQUEST_CODE = 1;
    private static final int READ_PHONE_STATE = 2;
    private CallbackContext callbackContext;
    private CordovaArgs args;
    private Long time;
    private BroadcastReceiver phoneStateReceiver;
    private BroadcastReceiver sendSMSReceiver;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(Manifest.permission.READ_PHONE_STATE, READ_PHONE_STATE, "请打开读取电话状态权限");
            checkPermission(Manifest.permission.READ_CALL_LOG, CALL_PHONE_REQUEST_CODE, "请打开读取通话记录权限");
        }
        this.sendSMSReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context _context, Intent _intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        triggerCallback("发送短信成功", true);
                        break;
                    default:
                        triggerCallback("发送短信失败", false);
                }
            }
        };
        this.phoneStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context _context, Intent _intent) {
                TelephonyManager tm = (TelephonyManager) _context.getSystemService(Service.TELEPHONY_SERVICE);
                PhoneStateListener listener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String incomingNumber) {
                        super.onCallStateChanged(state, incomingNumber);
                        switch (state) {
                            case TelephonyManager.CALL_STATE_IDLE:
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        String seconds = null;
                                        try {
                                            seconds = getCallHistoryList(cordova.getActivity(), args.getString(0));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        Log.i("aaa", "run: " + seconds);
                                        triggerCallback(seconds, true);
                                    }
                                }, 500);
                                break;
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                time = (new Date()).getTime();
                                break;
                        }
                    }
                };
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        };

        registerListenPhoneReceive();
        registerListenSMSReceive();
    }

    private void checkPermission(String permission, int requestCode, String permissionMissingAlertInLaterM) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cordova.getActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                cordova.requestPermissions(TELAndSMS.this, requestCode, new String[]{permission});
            }
        } else {
            showNoPermissionAlert(permissionMissingAlertInLaterM);
        }
    }

    private void registerListenSMSReceive() {
        cordova.getActivity().registerReceiver(sendSMSReceiver, new IntentFilter(SENT_SMS_ACTION));
    }

    private void registerListenPhoneReceive() {
        IntentFilter filterOutCall = new IntentFilter();
        filterOutCall.addAction(PHONE_STATE_FILTER);
        cordova.getActivity().registerReceiver(phoneStateReceiver, filterOutCall);
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        this.args = args;
        switch (action) {
            case "callTel":
                this.handleCallPhone(args.getString(0));
                break;
            case "callSMS":
                this.sendSMS(args.getJSONObject(0));
            default:
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                triggerCallback("PERMISSION_DENIED_ERROR", false);
                return;
            }
        }
        switch (requestCode) {
            case CALL_PHONE_REQUEST_CODE:
                makeCall(args.getString(0));
                break;
            case SEND_SMS_REQUEST_CODE:
                sendSMS(args.getJSONObject(0));
                break;
        }
    }

    public void handleCallPhone(String phoneNum) {
        new AlertDialog.Builder(cordova.getActivity())
                .setMessage(phoneNum)
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        triggerCallback("取消拨号", false);
                    }
                })
                .setPositiveButton("呼叫", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        makeCall(phoneNum);
                    }
                })
                .setCancelable(false)
                .create().show();
    }

    private void makeCall(String phoneNumber) {
        if (!readSIMCard()) {
            triggerCallback("没有移动网络", false);
            return;
        }
        if (!cordova.hasPermission(Manifest.permission.CALL_PHONE)) {
            checkPermission(Manifest.permission.CALL_PHONE, CALL_PHONE_REQUEST_CODE, "请打开拨打电话权限");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CALL);
        Uri data = Uri.parse("tel:" + phoneNumber);
        intent.setData(data);
        cordova.getActivity().startActivity(intent);
    }

    public void sendSMS(JSONObject smsObj) throws JSONException {
        if (!readSIMCard()) {
            triggerCallback("没有移动网络", false);
            return;
        }
        if (!cordova.hasPermission(Manifest.permission.SEND_SMS)) {
            checkPermission(Manifest.permission.SEND_SMS, SEND_SMS_REQUEST_CODE, "请打开发送短信权限");
            return;
        }
        SmsManager smsManager = SmsManager.getDefault();
        Intent sentIntent = new Intent(SENT_SMS_ACTION);
        PendingIntent sentPI = PendingIntent.getBroadcast(cordova.getActivity(), 0, sentIntent,
                0);
        smsManager.sendTextMessage(smsObj.getJSONArray("phones").toString(), null, smsObj.getString("content"), sentPI, null);
    }

    private void showNoPermissionAlert(String message) {
        new AlertDialog.Builder(cordova.getActivity())
                .setMessage(message)
                .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        triggerCallback(message, false);
                    }
                })
                .setCancelable(false)
                .create().show();
    }

    private void triggerCallback(String message, boolean isExecSuccessCallback) {
        JSONObject object = new JSONObject();
        try {
            object.put("message", message);
            if (isExecSuccessCallback) {
                callbackContext.success(object);
            } else {
                callbackContext.error(object);
            }
        } catch (JSONException e) {
            callbackContext.error(object);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cordova.getActivity().unregisterReceiver(sendSMSReceiver);
        cordova.getActivity().unregisterReceiver(phoneStateReceiver);
    }

    @SuppressLint("MissingPermission")
    private boolean readSIMCard() {
        TelephonyManager tm = (TelephonyManager) cordova.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        boolean canCall = false;
        switch (tm.getSimState()) {
            case TelephonyManager.SIM_STATE_READY:
                canCall = true;
                break;
        }
        if (!canCall) {
            return false;
        }
        if (tm.getSimSerialNumber() == null) {
            return false;
        }

        if (tm.getSimOperator().equals("")) {
            return false;
        }

        if (tm.getSimOperatorName().equals("")) {
            return false;
        }

        if (tm.getSimCountryIso().equals("")) {
            return false;
        }

        if (tm.getNetworkOperator().equals("")) {
            return false;
        }
        if (tm.getNetworkOperatorName().equals("")) {
            return false;
        }
        if (tm.getNetworkType() == 0) {
            return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public String getCallHistoryList(Activity activity, String phoneNum) {
        Cursor cs;
        cs = activity.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DURATION,
                }, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
        int i = 0;
        if (cs == null || cs.getCount() <= 0) {
            return "0";
        }
        for (cs.moveToFirst(); (!cs.isAfterLast()) && i < 2; cs.moveToNext(), i++) {
            String callNumber = cs.getString(0);
            int callType = Integer.parseInt(cs.getString(1));
            if (callNumber.equals(phoneNum) && callType == CallLog.Calls.OUTGOING_TYPE) {
                return cs.getString(2);
            }
        }
        return "0";
    }
}
