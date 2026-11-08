package com.example.safeher;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

public class XmlParserHelper {

    public static List<SafetyTip> parseSafetyTips(
            XmlPullParser parser) throws Exception {

        List<SafetyTip> tips = new ArrayList<>();

        String title = "";
        String description = "";

        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {

                String tagName = parser.getName();

                if ("tip".equals(tagName)) {
                    title = "";
                    description = "";
                } else if ("title".equals(tagName)) {
                    title = parser.nextText();
                } else if ("description".equals(tagName)) {
                    description = parser.nextText();
                }

            } else if (eventType == XmlPullParser.END_TAG) {

                if ("tip".equals(parser.getName())) {
                    tips.add(new SafetyTip(
                            title.trim(),
                            description.trim()
                    ));
                }
            }

            eventType = parser.next();
        }

        return tips;
    }
}