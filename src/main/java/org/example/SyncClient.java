package org.example;

import com.iiith.assignment.model.sync.Client;
import com.iiith.assignment.model.sync.ContentChange;
import com.iiith.assignment.model.sync.ContentChanges;
import com.iiith.assignment.model.sync.SyncServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.checkerframework.checker.units.qual.A;
import org.example.common.AbstractChannel;
import org.example.common.ResponseObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SyncClient {

    static List<String> currentContent;

    public SyncClient() {
        currentContent = ContentData.getCurrentContent();
        AbstractChannel abstractChannel = new AbstractChannel();
        abstractChannel.setupChannel(9090);
//        channel = abstractChannel.getChannel();
    }
    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        SyncServiceGrpc.SyncServiceStub asyncStub = SyncServiceGrpc.newStub(channel);

        FileChangeSyncService fileChangeSyncService = new FileChangeSyncService();

        var responseObserver = ResponseObserver.<ContentChanges>create(fileChangeSyncService);

        Client client = Client.newBuilder().setClientId("clientID").build();

        // StreamObserver for sending messages to the server
        asyncStub.registerClient(client, responseObserver);
        // Simulate a delay
        while (true) {
            Thread.sleep(1000);
        }
    }
}
