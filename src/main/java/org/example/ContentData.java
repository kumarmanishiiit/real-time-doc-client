package org.example;

import java.util.ArrayList;
import java.util.List;

// Single source of truth for the content data
public class ContentData {
    private static volatile List<String> currentContent;

    private ContentData() {
    }

    public static List<String> getCurrentContent() {
        if (currentContent == null) {
            synchronized (ContentData.class) {
                if (currentContent == null) {
                    currentContent = new ArrayList<>();
                }
            }
        }
        return currentContent;
    }
}
