/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2024 The LineageOS Project
 * SPDX-FileCopyrightText: 2026 The BlazeAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard;

import static com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.lineageos.setupwizard.util.OobeSpringHelper;
import org.lineageos.setupwizard.util.SetupWizardUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NetworkSetupActivity extends BaseSetupWizardActivity {

    private WifiManager mWifiManager;
    private LinearLayout mNetworkListContainer;
    private TextView mStatusLabel;
    private Button mContinueButton;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mReceiverRegistered = false;

    private final BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshNetworkList();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getWindow().setStatusBarColor(0xFFF6FCFF);
            getWindow().setNavigationBarColor(0xFFEDF9FF);
            WindowInsetsController wic = getWindow().getInsetsController();
            if (wic != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                wic.setSystemBarsAppearance(mask, mask);
            }
        } catch (Throwable ignored) {}

        mWifiManager = getSystemService(WifiManager.class);
        if (SetupWizardUtils.isOwner()) {
            tryEnablingWifi();
        }

        mNetworkListContainer = findViewById(R.id.wifi_network_list_container);
        mStatusLabel = findViewById(R.id.wifi_status_label);
        mContinueButton = findViewById(R.id.wifi_continue_button);
        ImageButton backBtn = findViewById(R.id.wifi_back_button);
        View openSystemPicker = findViewById(R.id.wifi_open_system_picker);

        if (backBtn != null) {
            OobeSpringHelper.attachPressSpring(backBtn);
            backBtn.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                finishAction(RESULT_CANCELED, new Intent().putExtra("onBackPressed", true));
            });
        }

        if (mContinueButton != null) {
            OobeSpringHelper.attachPressSpring(mContinueButton);
            mContinueButton.setOnClickListener(v -> {
                OobeSpringHelper.vibrateSuccess(this);
                if (isWifiConnected()) {
                    onNextPressed();
                } else {
                    onSkipPressed();
                }
            });
        }

        if (openSystemPicker != null) {
            OobeSpringHelper.attachPressSpring(openSystemPicker);
            openSystemPicker.setOnClickListener(v -> {
                OobeSpringHelper.vibrateTick(this);
                openSystemWifiSettings();
            });
        }

        refreshNetworkList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mReceiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            registerReceiver(mWifiReceiver, filter);
            mReceiverRegistered = true;
        }
        if (mWifiManager != null) {
            try {
                mWifiManager.startScan();
            } catch (Throwable ignored) {}
        }
        refreshNetworkList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mReceiverRegistered) {
            try {
                unregisterReceiver(mWifiReceiver);
            } catch (Throwable ignored) {}
            mReceiverRegistered = false;
        }
    }

    private boolean isWifiConnected() {
        try {
            ConnectivityManager cm = getSystemService(ConnectivityManager.class);
            if (cm != null) {
                Network active = cm.getActiveNetwork();
                if (active != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                    if (caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private String getConnectedSsid() {
        try {
            if (mWifiManager != null) {
                WifiInfo info = mWifiManager.getConnectionInfo();
                if (info != null && info.getNetworkId() != -1) {
                    String ssid = info.getSSID();
                    if (ssid != null) {
                        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() > 2) {
                            ssid = ssid.substring(1, ssid.length() - 1);
                        }
                        if (!"<unknown ssid>".equalsIgnoreCase(ssid)) {
                            return ssid;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void refreshNetworkList() {
        if (mNetworkListContainer == null) return;
        mNetworkListContainer.removeAllViews();

        boolean connected = isWifiConnected();
        String connectedSsid = getConnectedSsid();

        if (mContinueButton != null) {
            if (connected) {
                mContinueButton.setText("Continue");
                mContinueButton.setTextColor(Color.WHITE);
                mContinueButton.setBackgroundResource(R.drawable.ameos_btn_pill_filled);
            } else {
                mContinueButton.setText("Continue without Wi-Fi");
                mContinueButton.setTextColor(0xFF203A51);
                mContinueButton.setBackgroundResource(R.drawable.ameos_btn_pill_outline);
            }
        }

        List<ScanResult> results = Collections.emptyList();
        if (mWifiManager != null) {
            try {
                results = mWifiManager.getScanResults();
            } catch (Throwable ignored) {}
        }

        Map<String, ScanResult> uniqueBySsid = new LinkedHashMap<>();
        if (results != null) {
            List<ScanResult> sorted = new ArrayList<>(results);
            sorted.sort((a, b) -> Integer.compare(b.level, a.level));
            for (ScanResult sr : sorted) {
                if (sr != null && !TextUtils.isEmpty(sr.SSID)) {
                    if (!uniqueBySsid.containsKey(sr.SSID)) {
                        uniqueBySsid.put(sr.SSID, sr);
                    }
                }
            }
        }

        if (connected && !TextUtils.isEmpty(connectedSsid) && !uniqueBySsid.containsKey(connectedSsid)) {
            addNetworkRow(connectedSsid, true, false, 4, null);
        }

        for (ScanResult sr : uniqueBySsid.values()) {
            boolean isCurrent = connected && sr.SSID.equals(connectedSsid);
            boolean isSecure = sr.capabilities != null && (sr.capabilities.contains("WPA")
                    || sr.capabilities.contains("WEP") || sr.capabilities.contains("SAE"));
            int bars = WifiManager.calculateSignalLevel(sr.level, 5);
            addNetworkRow(sr.SSID, isCurrent, isSecure, bars, sr);
        }

        if (mNetworkListContainer.getChildCount() == 0) {
            // Provide a clean interactive row to launch system Wi-Fi picker or rescan
            addFallbackScanRow();
        }
    }

    private void addNetworkRow(String ssid, boolean isConnected, boolean isSecure, int bars,
            @Nullable ScanResult scanResult) {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = Math.round(14f * d);
        int padV = Math.round(13f * d);
        row.setPadding(padH, padV, padH, padV);

        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setCornerRadius(18f * d);
        if (isConnected) {
            rowBg.setColor(0xFFE0F2FE);
            rowBg.setStroke(Math.round(1.5f * d), 0xFF02C9FF);
        } else {
            rowBg.setColor(0xFFFFFFFF);
            rowBg.setStroke(Math.round(1f * d), 0xFFE2F0FF);
        }
        row.setBackground(rowBg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Math.round(8f * d);
        row.setLayoutParams(lp);

        ImageView signalIcon = new ImageView(this);
        signalIcon.setImageResource(R.drawable.ic_oobe_wifi_signal);
        int sigSize = Math.round(22f * d);
        LinearLayout.LayoutParams sigLp = new LinearLayout.LayoutParams(sigSize, sigSize);
        sigLp.rightMargin = Math.round(12f * d);
        signalIcon.setLayoutParams(sigLp);
        signalIcon.setAlpha(bars >= 3 ? 1.0f : (bars == 2 ? 0.82f : 0.65f));
        row.addView(signalIcon);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView titleView = new TextView(this);
        titleView.setText(ssid);
        titleView.setTextSize(15.5f);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleView.setTextColor(0xFF203A51);
        textCol.addView(titleView);

        TextView subView = new TextView(this);
        subView.setTextSize(12f);
        if (isConnected) {
            subView.setText("Connected");
            subView.setTextColor(0xFF017CFE);
        } else {
            subView.setText(isSecure ? "Secured • Tap to connect" : "Open network");
            subView.setTextColor(0xFF6B86A0);
        }
        textCol.addView(subView);
        row.addView(textCol);

        if (isConnected) {
            ImageView checkIcon = new ImageView(this);
            checkIcon.setImageResource(R.drawable.ic_oobe_wifi_check);
            int iconSz = Math.round(22f * d);
            checkIcon.setLayoutParams(new LinearLayout.LayoutParams(iconSz, iconSz));
            row.addView(checkIcon);
        } else {
            if (isSecure) {
                ImageView lockIcon = new ImageView(this);
                lockIcon.setImageResource(R.drawable.ic_oobe_wifi_lock);
                int lockSz = Math.round(18f * d);
                LinearLayout.LayoutParams lockLp = new LinearLayout.LayoutParams(lockSz, lockSz);
                lockLp.rightMargin = Math.round(6f * d);
                lockIcon.setLayoutParams(lockLp);
                row.addView(lockIcon);
            }
            ImageView chevron = new ImageView(this);
            chevron.setImageResource(R.drawable.ic_oobe_chevron_right);
            int chevSz = Math.round(18f * d);
            chevron.setLayoutParams(new LinearLayout.LayoutParams(chevSz, chevSz));
            row.addView(chevron);
        }

        OobeSpringHelper.attachPressSpring(row);
        row.setOnClickListener(v -> {
            OobeSpringHelper.vibrateTick(this);
            if (isConnected) {
                onNextPressed();
            } else if (isSecure) {
                showPasswordDialog(ssid);
            } else {
                connectToWifi(ssid, null);
            }
        });

        mNetworkListContainer.addView(row);
    }

    private void addFallbackScanRow() {
        float d = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER);
        int pad = Math.round(22f * d);
        row.setPadding(pad, pad, pad, pad);

        TextView msg = new TextView(this);
        msg.setText("Scanning for Wi-Fi networks...");
        msg.setTextSize(14.5f);
        msg.setTextColor(0xFF203A51);
        msg.setGravity(Gravity.CENTER);
        row.addView(msg);

        TextView sub = new TextView(this);
        sub.setText("Tap here to open full Wi-Fi network picker");
        sub.setTextSize(12.5f);
        sub.setTextColor(0xFF017CFE);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Math.round(6f * d), 0, 0);
        row.addView(sub);

        row.setOnClickListener(v -> openSystemWifiSettings());
        mNetworkListContainer.addView(row);
    }

    private void showPasswordDialog(String ssid) {
        float d = getResources().getDisplayMetrics().density;
        EditText input = new EditText(this);
        input.setHint("Password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int pad = Math.round(18f * d);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle("Connect to " + ssid)
                .setView(container)
                .setPositiveButton("Connect", (dialog, which) -> {
                    String password = input.getText().toString();
                    connectToWifi(ssid, password);
                })
                .setNeutralButton("System Picker", (dialog, which) -> openSystemWifiSettings())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void connectToWifi(String ssid, @Nullable String password) {
        if (mWifiManager == null) return;
        try {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + ssid + "\"";
            if (!TextUtils.isEmpty(password)) {
                config.preSharedKey = "\"" + password + "\"";
            } else {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
            int netId = mWifiManager.addNetwork(config);
            if (netId != -1) {
                mWifiManager.disconnect();
                mWifiManager.enableNetwork(netId, true);
                mWifiManager.reconnect();
            } else {
                openSystemWifiSettings();
            }
            mHandler.postDelayed(this::refreshNetworkList, 1200);
        } catch (Throwable t) {
            openSystemWifiSettings();
        }
    }

    private void openSystemWifiSettings() {
        try {
            Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
            startActivity(intent);
        } catch (Throwable e) {
            try {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } catch (Throwable ignored) {}
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.setup_network;
    }

    @Override
    protected int getTitleResId() {
        return -1;
    }

    @Override
    protected int getIconResId() {
        return -1;
    }

    @Override
    protected com.google.android.setupdesign.GlifLayout getGlifLayout() {
        return null;
    }
}
