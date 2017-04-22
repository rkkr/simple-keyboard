/*
 * Copyright (C) 2014 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin.settings;

/**
 * Collection of device specific preference constants.
 */
public class LocalSettingsConstants {
    // Preference file for storing preferences that are tied to a device
    // and are not backed up.
    public static final String PREFS_FILE = "local_prefs";

    // Preference key for the current account.
    // Do not restore.
    public static final String PREF_ACCOUNT_NAME = "pref_account_name";
    // Preference key for enabling cloud sync feature.
    // Do not restore.
    public static final String PREF_ENABLE_CLOUD_SYNC = "pref_enable_cloud_sync";
}
