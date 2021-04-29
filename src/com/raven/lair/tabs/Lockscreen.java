
/*
 * Copyright (C) 2017-2019 The Dirty Unicorns Project
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

package com.raven.lair.tabs;

import static android.os.UserHandle.USER_SYSTEM;
import static android.os.UserHandle.USER_CURRENT;

import android.app.ActivityManagerNative;
import android.app.UiModeManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManagerGlobal;
import android.view.IWindowManager;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.Locale;
import android.text.TextUtils;
import android.view.View;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.Utils;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

//import com.android.internal.util.custom.Utils;

public class Lockscreen extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String CUSTOM_CLOCK_FACE = Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE;
    private static final String DEFAULT_CLOCK = "com.android.keyguard.clock.DefaultClockController";

    private static final String FOD_DISABLED_BY_PROP ="ro.fingerprint.inscreen_disabled";
    private static final String FOD_TWEAKS = "fod_tweaks";
    private static final String AOD_SCHEDULE_KEY = "always_on_display_schedule";

    static final int MODE_DISABLED = 0;
    static final int MODE_NIGHT = 1;
    static final int MODE_TIME = 2;
    static final int MODE_MIXED_SUNSET = 3;
    static final int MODE_MIXED_SUNRISE = 4;

    Preference mAODPref;

    private ContentResolver mResolver;
    private Context mContext;
    private ListPreference mLockClockStyles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen);
        final PreferenceScreen prefScreen = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
        mContext = getActivity();
        mAODPref = findPreference(AOD_SCHEDULE_KEY);
        updateAlwaysOnSummary();
        mLockClockStyles = (ListPreference) findPreference(CUSTOM_CLOCK_FACE);
        String mLockClockStylesValue = getLockScreenCustomClockFace();
        mLockClockStyles.setValue(mLockClockStylesValue);
        mLockClockStyles.setSummary(mLockClockStyles.getEntry());
        mLockClockStyles.setOnPreferenceChangeListener(this);
        if (!isFODdevice()) {
            prefScreen.removePreference(findPreference(FOD_TWEAKS));
           }
        }

	private boolean isFODdevice() {
        return (getResources().getBoolean(
                com.android.internal.R.bool.config_supportsInDisplayFingerprint));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAlwaysOnSummary();
    }

    private void updateAlwaysOnSummary() {
        if (mAODPref == null) return;
        int mode = Settings.Secure.getIntForUser(getActivity().getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON_AUTO_MODE, 0, UserHandle.USER_CURRENT);
        switch (mode) {
            default:
            case MODE_DISABLED:
                break;
            case MODE_NIGHT:
                mAODPref.setSummary(R.string.night_display_auto_mode_twilight);
                break;
            case MODE_TIME:
                mAODPref.setSummary(R.string.night_display_auto_mode_custom);
                break;
            case MODE_MIXED_SUNSET:
                mAODPref.setSummary(R.string.always_on_display_schedule_mixed_sunset);
                break;
            case MODE_MIXED_SUNRISE:
                mAODPref.setSummary(R.string.always_on_display_schedule_mixed_sunrise);
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mLockClockStyles) {
            setLockScreenCustomClockFace((String) objValue);
            int index = mLockClockStyles.findIndexOfValue((String) objValue);
            mLockClockStyles.setSummary(mLockClockStyles.getEntries()[index]);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }
    private String getLockScreenCustomClockFace() {
        String value = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                CUSTOM_CLOCK_FACE, USER_CURRENT);

        if (value == null || value.isEmpty()) value = DEFAULT_CLOCK;

        try {
            JSONObject json = new JSONObject(value);
            return json.getString("clock");
        } catch (JSONException ex) {
        }
        return value;
    }

    private void setLockScreenCustomClockFace(String value) {
        try {
            JSONObject json = new JSONObject();
            json.put("clock", value);
            Settings.Secure.putStringForUser(mContext.getContentResolver(), CUSTOM_CLOCK_FACE,
                    json.toString(), USER_CURRENT);
        } catch (JSONException ex) {
        }
    }
}
