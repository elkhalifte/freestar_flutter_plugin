package com.freestar.ads.flutter.freestar_flutter_plugin;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.freestar.ads.flutter.freestar_flutter_plugin.FreestarFlutterPlugin;
import com.freestar.android.ads.AdRequest;
import com.freestar.android.ads.ChocolateLogger;
import com.freestar.android.ads.ErrorCodes;
import com.freestar.android.ads.NativeAd;
import com.freestar.android.ads.NativeAdListener;

import androidx.annotation.Nullable;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;

import static io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import static io.flutter.plugin.common.MethodChannel.Result;

public class FlutterNativeAd implements PlatformView, MethodCallHandler, NativeAdListener {

    private static final String TAG = "fsfp_tag: FlutterNativeAd";

    private final NativeAd nativeAd;
    private final AdRequest adRequest;
    private final MethodChannel methodChannel;

    FlutterNativeAd(Context context, BinaryMessenger messenger, int id) {
        ChocolateLogger.i(TAG, "FlutterNativeAd created");
        nativeAd = new NativeAd(context);
        nativeAd.setNativeAdListener(this);
        adRequest = new AdRequest(context);
        methodChannel = new MethodChannel(messenger, "plugins.freestar.ads/NativeAd_" + id);
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public View getView() {
        return nativeAd;
    }

    @Override
    public void onMethodCall(MethodCall methodCall, Result result) {

        ChocolateLogger.i(TAG, "onMethodCall. method: " + methodCall.method + " args: " + methodCall.arguments);

        if (methodCall.method.equals("loadNativeAd")) {
            String[] args = ((String) methodCall.arguments).split("\\|");
            String placement = args[0].trim().isEmpty() ? null : args[0];
            nativeAd.setTemplate(Integer.parseInt(args[1]));
            loadNativeAd(adRequest, placement);
            result.success("loadNativeAd invoked.");
        } else {
            result.notImplemented();
        }
    }

    private void loadNativeAd(final AdRequest adRequest, final String placement) {

        if (FreestarFlutterPlugin.isPartnerChooserEnabled) {
            MediationPartners.choosePartners(FreestarFlutterPlugin.activity.get(), adRequest, MediationPartners.ADTYPE_BANNER, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ChocolateLogger.i(TAG, "loading...");
                    nativeAd.loadAd(adRequest, placement);
                }
            });
        } else {
            ChocolateLogger.i(TAG, "loading...");
            nativeAd.loadAd(adRequest, placement);
        }
    }

    private String placement(String s) {
        if (s == null || (s.trim().isEmpty())) {
            return " "; //single white-space
        }
        return s;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void onNativeAdLoaded(View view, String s) {
        methodChannel.invokeMethod("onNativeAdLoaded", placement(s), result);
    }

    @Override
    public void onNativeAdFailed(String s, int i) {
        methodChannel.invokeMethod("onNativeAdFailed", placement(s) + "|" + ErrorCodes.getErrorDescription(i), result);
    }

    @Override
    public void onNativeAdClicked(String s) {
        methodChannel.invokeMethod("onNativeAdClicked", placement(s), result);
    }

    private Result result = new Result() {
        @Override
        public void success(@Nullable Object result) {
            ChocolateLogger.e(TAG, "plugins.freestar.ads/NativeAd success: " + result);
        }

        @Override
        public void error(String errorCode, @Nullable String errorMessage, @Nullable Object errorDetails) {
            ChocolateLogger.e(TAG, "plugins.freestar.ads/NativeAd error: " + errorMessage + " " + errorDetails);
        }

        @Override
        public void notImplemented() {
            ChocolateLogger.e(TAG, "plugins.freestar.ads/NativeAd notImplemented");
        }
    };
}