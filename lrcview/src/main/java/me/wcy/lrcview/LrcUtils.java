/*
 * Copyright (C) 2017 wangchenyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package me.wcy.lrcview;

import android.animation.ValueAnimator;
import android.text.TextUtils;
import android.text.format.DateUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类
 */
class LrcUtils {
    private static final Pattern PATTERN_LINE = Pattern.compile("((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\])+)(.+)");
    private static final Pattern PATTERN_TIME = Pattern.compile("\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]");

    /**
     * 从文本解析双语歌词
     */
    static List<LrcEntry> parseLrc(String[] lrcTexts) {
        if (lrcTexts == null || lrcTexts.length != 2 || TextUtils.isEmpty(lrcTexts[0])) {
            return null;
        }

        String mainLrcText = lrcTexts[0];
        String secondLrcText = lrcTexts[1];
        List<LrcEntry> mainEntryList = parseLrc(mainLrcText);
        List<LrcEntry> secondEntryList = parseLrc(secondLrcText);

        if (mainEntryList != null && secondEntryList != null) {
            for (LrcEntry mainEntry : mainEntryList) {
                for (LrcEntry secondEntry : secondEntryList) {
                    if (mainEntry.getTime() == secondEntry.getTime()) {
                        mainEntry.setSecondText(secondEntry.getText());
                    }
                }
            }
        }
        return mainEntryList;
    }

    /**
     * 从文本解析歌词
     */
    private static List<LrcEntry> parseLrc(String lrcText) {
        if (TextUtils.isEmpty(lrcText)) {
            return null;
        }

        if (lrcText.startsWith("\uFEFF")) {
            lrcText = lrcText.replace("\uFEFF", "");
        }

        List<LrcEntry> entryList = new ArrayList<>();
        String[] array = lrcText.split("\\n");
        for (String line : array) {
            List<LrcEntry> list = parseLine(line);
            if (list != null && !list.isEmpty()) {
                entryList.addAll(list);
            }
        }

        Collections.sort(entryList);
        return entryList;
    }

    /**
     * 解析一行歌词
     */
    private static List<LrcEntry> parseLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return null;
        }

        line = line.trim();
        // [00:17.65]让我掉下眼泪的
        Matcher lineMatcher = PATTERN_LINE.matcher(line);
        if (!lineMatcher.matches()) {
            return null;
        }

        String times = lineMatcher.group(1);
        String text = lineMatcher.group(3);
        List<LrcEntry> entryList = new ArrayList<>();

        // [00:17.65]
        Matcher timeMatcher = PATTERN_TIME.matcher(times);
        while (timeMatcher.find()) {
            long min = Long.parseLong(timeMatcher.group(1));
            long sec = Long.parseLong(timeMatcher.group(2));
            String milString = timeMatcher.group(3);
            long mil = Long.parseLong(milString);
            // 如果毫秒是两位数，需要乘以10
            if (milString.length() == 2) {
                mil = mil * 10;
            }
            long time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil;
            entryList.add(new LrcEntry(time, text));
        }
        return entryList;
    }

    static void resetDurationScale() {
        try {
            Field mField = ValueAnimator.class.getDeclaredField("sDurationScale");
            mField.setAccessible(true);
            mField.setFloat(null, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
