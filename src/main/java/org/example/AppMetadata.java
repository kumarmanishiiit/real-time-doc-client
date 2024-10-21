package org.example;

import java.util.UUID;

public class AppMetadata {

    private static UUID clientID;

    private AppMetadata() {

    }

    public static UUID getClientID() {
            if (clientID == null) {
                synchronized (ContentData.class) {
                    if (clientID == null) {
                        clientID = UUID.randomUUID();
                    }
                }
            }
            return clientID;
    }
}
