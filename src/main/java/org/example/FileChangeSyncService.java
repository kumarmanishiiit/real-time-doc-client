package org.example;

import com.iiith.assignment.model.sync.ACTION;
import com.iiith.assignment.model.sync.ContentChange;
import com.iiith.assignment.model.sync.ContentChanges;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileChangeSyncService {

    private final List<String> currentContent;

    private final UUID clientID;

    public FileChangeSyncService() {
        clientID = AppMetadata.getClientID();
        this.currentContent = ContentData.getCurrentContent();
    }

    // Method to read the entire content of the file
    private static String getFileContent(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return String.join("\n", lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    // Method to compare and print differences between two versions of content
    public List<ContentChange> getChanges(String oldContent, String newContent) {

        List<ContentChange> contentChangeList = new ArrayList<>();

//        ContentChange contentChange = ContentChange.newBuilder().setContent("").setLine(1).setAction(ACTION.ADD).build();

        System.out.println("----- Changes Detected -----");
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");

        int minLength = Math.min(oldLines.length, newLines.length);

        for (int i = 0; i < minLength; i++) {
            if (!oldLines[i].equals(newLines[i])) {
                System.out.println("Line " + (i + 1) + " changed:");
                System.out.println("Old: " + oldLines[i]);
                System.out.println("New: " + newLines[i]);
                if (currentContent.get(i) == null) {
                    contentChangeList.add(ContentChange.newBuilder().setContent(newLines[i]).setLine(i).setAction(ACTION.ADD).build());
                    currentContent.add(newLines[i]);
                } else {
                    contentChangeList.add(ContentChange.newBuilder().setContent(newLines[i]).setLine(i).setAction(ACTION.MODIFY).build());
                    currentContent.set(i, newLines[i]);
                }
            }
        }

        // If new content has extra lines
        if (newLines.length > oldLines.length) {
            System.out.println("New lines added:");
            for (int i = oldLines.length; i < newLines.length; i++) {
                System.out.println("Line " + (i + 1) + ": " + newLines[i]);
                contentChangeList.add(ContentChange.newBuilder().setContent(newLines[i]).setLine(i).setAction(ACTION.ADD).build());
                currentContent.add(newLines[i]);
            }
        } else if (oldLines.length > newLines.length) {
            System.out.println("Lines removed:");
            for (int i = newLines.length; i < oldLines.length; i++) {
                System.out.println("Line " + (i + 1) + ": " + oldLines[i]);
                contentChangeList.add(ContentChange.newBuilder().setContent("").setLine(i).setAction(ACTION.DELETE).build());
                currentContent.set(i, "");
            }
        }
        return contentChangeList;
    }

    public <T> void updateContent(T t) {
        ContentChanges contentChanges = (ContentChanges) t;

        if (clientID.toString().equals(contentChanges.getClientId())) {
           return;
        }
        processChange(contentChanges.getContentChangeList());
        loadToFSFromCurrentContent();
    }

    private void processChange(List<ContentChange> contentChangeList) {

        for (ContentChange contentChange : contentChangeList) {
            if (contentChange.getAction().equals(ACTION.ADD)) {
                System.out.println(contentChange.getAction());
                System.out.println(contentChange.getContent());
                System.out.println(contentChange.getLine());
                currentContent.add(contentChange.getContent());
            } else if (contentChange.getAction().equals(ACTION.MODIFY)) {
                System.out.println(contentChange.getAction());
                System.out.println(contentChange.getContent());
                System.out.println(contentChange.getLine());
                currentContent.set(contentChange.getLine(), contentChange.getContent());
            } else if (contentChange.getAction().equals(ACTION.DELETE)) {
                System.out.println(contentChange.getAction());
                System.out.println(contentChange.getContent());
                System.out.println(contentChange.getLine());
                currentContent.set(contentChange.getLine(), "");
            }
        }
    }

    public void loadToFSFromCurrentContent() {
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter("cache/test.txt"));
            for (String line : currentContent) {
                bufferedWriter.write(line);
                bufferedWriter.write("\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

