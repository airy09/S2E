/*
 * Copyright (C) 2012 OlegKrikun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.krikun.s2e;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

public class Target {

    public static final int TARGET_ON_DATA = 0;
    public static final int TARGET_ON_CACHE = 1;
    public static final int TARGET_ON_EXT = 2;
    public static final int TARGET_MOVE_TO_DATA = 3;
    public static final int TARGET_MOVE_TO_CACHE = 4;
    public static final int TARGET_MOVE_TO_EXT = 5;

    private SharedPreferences prefs;

    private String path;
    private String targetName;

    private int size;
    private int status;

    private boolean displaced = false;

    private String external;
    private String internal;

    public String getExternal() {
        return external;
    }

    public String getInternal() {
        return internal;
    }

    public String getPath() {
        return path;
    }

    public int getSize() {
        return size;
    }

    public int getStatus() {
        return status;
    }

    public String getTargetName() {
        return targetName;
    }

    public Target(Context context, String target) {
        this.targetName = target;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        path = getPath(target);
        external = "/sd-ext";
        internal = loadInternalPartition();

        displaced = isDisplaced();

        status = loadTargetStatus();
        size = loadSize();
    }

    public void updateStatus() {
        status = loadTargetStatus();
    }

    private String getPath(String target) {
        if (target.equals("download")) return "/cache/" + target;
        else return "/data/" + target;
    }

    private String loadInternalPartition() {
        if (targetName.equals("download")) return "/cache";
        else return "/data";
    }

    private boolean isDisplaced() {
        return Helper.checkStatusFileExists(targetName);
    }

    private int loadSize() {
        List<String> output = Helper.sendShell("busybox du -s " + path);
        if (output != null) {
            String[] array = output.get(0).split("\\s");
            if (!array[0].equals("")) return Integer.parseInt(array[0]);
        }
        return 0;
    }

    private int loadTargetStatus() {
        boolean move = prefs.getBoolean(targetName, false);

        // Если на INT и надо переместить на EXT
        if (move && !displaced) {
            return TARGET_MOVE_TO_EXT;
        }
        // Если на EXT и надо переместить на INT
        else if (!move && displaced) {
            if (targetName.equals("download")) return TARGET_MOVE_TO_CACHE;
            else return TARGET_MOVE_TO_DATA;
        }
        // Если на EXT и перемещать не надо
        else if (move && displaced) {
            return TARGET_ON_EXT;
        }
        // Если на INT и перемещать не надо [move && displaced]
        else {
            if (targetName.equals("download")) return TARGET_ON_CACHE;
            else return TARGET_ON_DATA;
        }
    }
}
