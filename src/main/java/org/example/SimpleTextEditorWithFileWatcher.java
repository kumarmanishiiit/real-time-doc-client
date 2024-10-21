package org.example;

import com.iiith.assignment.model.sync.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.example.common.ResponseObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimpleTextEditorWithFileWatcher extends JFrame implements ActionListener {

    private final UUID clientID;

    static List<String> currentContent;

    private String previousContent = "";

    private FileChangeSyncService fileChangeSyncService;

    private ManagedChannel channel;

    // Text component
    JTextArea textArea;

    // Frame
    JFrame frame;

    // Current file being edited
    File currentFile;

    // WatchService for file changes
    WatchService watchService;
    ExecutorService executorService = Executors.newFixedThreadPool(5);

    // Constructor
    public SimpleTextEditorWithFileWatcher() {
        clientID = AppMetadata.getClientID();
        // Create a frame
        frame = new JFrame("Simple Text Editor with File Watcher");

        // Create a text area
        textArea = new JTextArea();

        currentContent = ContentData.getCurrentContent();
        // Create a menu bar
        JMenuBar menuBar = new JMenuBar();
        // Create file menu
        JMenu fileMenu = new JMenu("File");

        // Create menu items
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem downloadItem = new JMenuItem("Download From Server");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Add action listeners
        openItem.addActionListener(this);
        saveItem.addActionListener(this);
        exitItem.addActionListener(this);
        downloadItem.addActionListener(this);

        // Set keyboard shortcuts (accelerators)
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        downloadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));

        // Add menu items to file menu
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(downloadItem);
        fileMenu.add(exitItem);

        // Add file menu to menu bar
        menuBar.add(fileMenu);

        // Add menu bar to frame
        frame.setJMenuBar(menuBar);

        // Add text area to frame
        frame.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // Set the frame size and make it visible
        frame.setSize(500, 500);
        frame.setVisible(true);

        // Close the frame when the user closes it
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Handle menu actions
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        // Open file action
        if (command.equals("Open")) {
            JFileChooser fileChooser = new JFileChooser("cache");
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                loadFile(currentFile);

                // Start watching for file changes
                startFileWatcher(currentFile.toPath());
                try {
                    establishServerConnection(currentFile.toPath());
                } catch (InterruptedException ex) {
//                    throw new RuntimeException(ex);
                }
            }
        }
        // Save file action
        else if (command.equals("Save")) {
            if (currentFile != null) {
                saveFile(currentFile);
            } else {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showSaveDialog(this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    currentFile = fileChooser.getSelectedFile();
                    saveFile(currentFile);
                }
            }
        }
        // Exit action
        else if (command.equals("Exit")) {
            stopFileWatcher();  // Stop the file watcher before exiting
            System.exit(0);
        } else if (command.equals("Download From Server")) {
            try {
                downloadFileFromServer();
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void downloadFileFromServer() throws FileNotFoundException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("dns:///localhost:9090")
                .usePlaintext()
                .build();

        SyncServiceGrpc.SyncServiceStub stub = SyncServiceGrpc.newStub(channel);

        FileRequest request = FileRequest.newBuilder()
                .setFileName("example.txt")  // Replace with your file name
                .build();

        stub.downloadFile(request, new StreamObserver<FileChunk>() {
            FileOutputStream fos = new FileOutputStream("cache/test.txt"); // Destination path

            @Override
            public void onNext(FileChunk fileChunk) {
                try {
                    fos.write(fileChunk.getContent().toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCompleted() {
                System.out.println("File downloaded successfully.");
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Wait for the server to finish sending the file
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        channel.shutdown();
    }

    private void establishServerConnection(Path path) throws InterruptedException {
            channel = ManagedChannelBuilder.forTarget("dns:///localhost:9090")
                    .usePlaintext()
                    .build();
        // Run the watcher in a separate thread
        executorService.submit(() -> {
            // StreamObserver for sending messages to the server

            SyncServiceGrpc.SyncServiceStub asyncStub = SyncServiceGrpc.newStub(channel);

            var responseObserver = ResponseObserver.<ContentChanges>create(fileChangeSyncService);

            Client client = Client.newBuilder().setClientId(clientID.toString()).build();

            // StreamObserver for sending messages to the server
            asyncStub.registerClient(client, responseObserver);

//            channel.shutdown();
        });

    }

    // Load the file into the text area
    private void loadFile(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            textArea.read(br, null);
            frame.setTitle("Simple Text Editor - " + file.getName());
//            currentContent = getFileContent(file.toPath());
            currentContent.clear();
            currentContent.addAll(Files.readAllLines(file.toPath()));
            previousContent = String.join("\n", currentContent);
            fileChangeSyncService = new FileChangeSyncService();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    // Save the file from the text area
    private void saveFile(File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            textArea.write(bw);
            // work with the in-memory data....
            // Get the new content and compare it with the previous content
            String newContent = getFileContent(file.toPath());
            List<ContentChange> contentChangeList = fileChangeSyncService.getChanges(previousContent, newContent);

            syncFileToServer(contentChangeList);
            // Update the previous content
            previousContent = newContent;
//            }
        } catch (IOException | InterruptedException ioException) {
            ioException.printStackTrace();
        }
    }

    private void syncFileToServer(List<ContentChange> contentChangeList) throws InterruptedException {

        // Run the watcher in a separate thread
        executorService.submit(() -> {
            try {
                    SyncServiceGrpc.SyncServiceStub asyncStub = SyncServiceGrpc.newStub(channel);

                    // StreamObserver for sending messages to the server
                    asyncStub.syncFile(ContentChanges.newBuilder().addAllContentChange(contentChangeList).setClientId(clientID.toString()).build(), new StreamObserver<ContentChanges>() {

                        @Override
                        public void onNext(ContentChanges contentChanges) {
                            processChange(contentChanges.getContentChangeList());
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            // show message in Pane if server is down
                            JOptionPane.showMessageDialog(frame,
                                    "There are some issue with the file sync. Please try after sometime!!!",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);

                        }
                        @Override
                        public void onCompleted() {
                            System.out.println("Sync Completed");
                        }
                    });
                // Simulate a delay
                Thread.sleep(1000);
//                channel.shutdown();
            } catch (InterruptedException ex) {
//                ex.printStackTrace();
                // Display the error message in a modal dialog
                JOptionPane.showMessageDialog(frame,
                        ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

    }

    private void processChange(List<ContentChange> contentChangeList) {
        for (ContentChange contentChange : contentChangeList) {
            if (contentChange.getAction().equals(ACTION.ADD)) {
                System.out.println(contentChange.getAction());
                System.out.println(contentChange.getContent());
                System.out.println(contentChange.getLine());
            } else if (contentChange.getAction().equals(ACTION.MODIFY)) {
                System.out.println(contentChange.getAction());
                System.out.println(contentChange.getContent());
                System.out.println(contentChange.getLine());
            } else if (contentChange.getAction().equals(ACTION.DELETE)) {
                System.out.println(contentChange.getAction());
                System.out.println(contentChange.getContent());
                System.out.println(contentChange.getLine());
            }
        }
    }

    // Start the file watcher to monitor file changes
    private void startFileWatcher(Path filePath) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            filePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            // Run the watcher in a separate thread
            executorService.submit(() -> {
                try {
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();

                            // Check if the modified file is the one we're editing
                            if (kind == StandardWatchEventKinds.ENTRY_MODIFY && event.context().toString().equals(filePath.getFileName().toString())) {
                                // Reload the file in the text area
                                SwingUtilities.invokeLater(() -> loadFile(currentFile));
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Stop the file watcher
    private void stopFileWatcher() {
        try {
            if (watchService != null) {
                watchService.close();
            }
            executorService.shutdownNow();  // Stop the watcher thread
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method
    public static void main(String[] args) {
        new SimpleTextEditorWithFileWatcher();
    }

    private static String getFileContent(Path filePath) {
        try {
            List<String> lines = Files.readAllLines(filePath);
            return String.join("\n", lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

}

