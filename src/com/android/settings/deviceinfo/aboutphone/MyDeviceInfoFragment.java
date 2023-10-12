/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.os.UserManager;
import android.util.Log;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.DeviceNamePreferenceController;
import com.android.settings.deviceinfo.firmwareversion.BuildStatusPreferenceController;
import com.android.settings.deviceinfo.firmwareversion.SigmaInfoPreferenceController;
import com.android.settings.deviceinfo.firmwareversion.SELinuxStatusPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.google.android.material.appbar.CollapsingToolbarLayout;

@SearchIndexable
public class MyDeviceInfoFragment extends DashboardFragment
        implements DeviceNamePreferenceController.DeviceNamePreferenceHost {

    private static final String LOG_TAG = "MyDeviceInfoFragment";
    protected CollapsingToolbarLayout mCollapsingToolbarLayout;
    private final BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                String state = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
                Log.d(LOG_TAG, "Received ACTION_SIM_STATE_CHANGED: " + state);
                updatePreferenceStates();
            }
        }
    };

    private BuildNumberPreferenceController mBuildNumberPreferenceController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        hideToolbar();
        setDashboardStyle();
    }

    private void hideToolbar() {
        if (mCollapsingToolbarLayout == null) {
            mCollapsingToolbarLayout = getActivity().findViewById(R.id.collapsing_toolbar);
        }
        if (mCollapsingToolbarLayout != null) {
            mCollapsingToolbarLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(DeviceNamePreferenceController.class).setHost(this /* parent */);
        mBuildNumberPreferenceController = use(BuildNumberPreferenceController.class);
        mBuildNumberPreferenceController.setHost(this /* parent */);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideToolbar();
        Context context = getContext();
        if (context != null) {
            context.unregisterReceiver(mSimStateReceiver);
        } else {
            Log.i(LOG_TAG, "context already null, not unregistering SimStateReceiver");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context != null) {
            context.registerReceiver(mSimStateReceiver,
                    new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED));
        } else {
            Log.i(LOG_TAG, "context is null, not registering SimStateReceiver");
        }
        setDashboardStyle();
        hideToolbar();
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.my_device_info;
    }

    private void setDashboardStyle() {
        int mDashBoardStyle = geSettingstDashboardStyle();
        final PreferenceScreen mScreen = getPreferenceScreen();
        final int mCount = mScreen.getPreferenceCount();

        for (int i = 0; i < mCount; i++) {
            final Preference mPreference = mScreen.getPreference(i);
            if (mPreference == null) continue;

            String mKey = mPreference.getKey();
            if (mKey == null) continue;

            if (mDashBoardStyle == 1 || mDashBoardStyle == 3) { // 0=stock aosp, 1=dot, 2=nad, 3=sigma
                if (mKey.equals("sigma_logo")) {
                mPreference.setLayoutResource(R.layout.dot_about_logo);
                } else if (mKey.equals("rom_build_status")) {
                mPreference.setLayoutResource(R.layout.dot_card_build_status);
                } else if (
                        mKey.equals("sigma_logo")
                        || mKey.equals("branded_account")
                ) {
                    mPreference.setLayoutResource(R.layout.dot_top_no_chevron);
                } else if (
                        mKey.equals("device_name")
                ) {
                    mPreference.setLayoutResource(R.layout.dot_bottom_no_chevron);
                } else if (mKey.equals("sigma_info")) {
                    mPreference.setLayoutResource(R.layout.dot_blank); 
                } else if (mKey.equals("kernel_version")) {
                    mPreference.setLayoutResource(R.layout.dot_dashboard_preference_bottom);
                } else if (mKey.equals("device_model")) {
                    mPreference.setLayoutResource(R.layout.dot_dashboard_preference_middle);
                } else {
                    mPreference.setLayoutResource(R.layout.dot_middle_no_chevron); 
                } 

            } else if (mDashBoardStyle == 2) {
                if (mKey.equals("sigma_version") || mKey.equals("security_key") || mKey.equals("kernel_version")) {
                    mPreference.setLayoutResource(R.layout.nad_dashboard_preference_full);
                } else if (mKey.equals("sigma_info")) {
                    mPreference.setLayoutResource(R.layout.dot_blank); 
                } else if (mKey.equals("sigma_logo")) {
                mPreference.setLayoutResource(R.layout.nad_about_logo);
                 } else if (mKey.equals("rom_build_status")) {
                mPreference.setLayoutResource(R.layout.nad_card_build_status);
                } 
                else {
                    mPreference.setLayoutResource(R.layout.nad_full_no_chevron);
                }
            }
        }
    }

    private int geSettingstDashboardStyle() {
        return Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SETTINGS_DASHBOARD_STYLE, 2, UserHandle.USER_CURRENT);
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this /* fragment */, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context, MyDeviceInfoFragment fragment, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new SigmaInfoPreferenceController(context));
        controllers.add(new SELinuxStatusPreferenceController(context));
        return controllers;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mBuildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void showDeviceNameWarningDialog(String deviceName) {
        DeviceNameWarningDialog.show(this);
    }

    public void onSetDeviceNameConfirm(boolean confirm) {
        final DeviceNamePreferenceController controller = use(DeviceNamePreferenceController.class);
        controller.updateDeviceName(confirm);
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.my_device_info) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context, null /* fragment */,
                            null /* lifecycle */);
                }
            };
}