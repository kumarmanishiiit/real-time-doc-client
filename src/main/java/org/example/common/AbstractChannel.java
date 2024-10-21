package org.example.common;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class AbstractChannel {

    protected ManagedChannel channel;

    public void setupChannel(int port) {
        this.channel = ManagedChannelBuilder.forTarget("dns:///localhost")
                .usePlaintext()
                .build();
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void stopChannel() throws InterruptedException {
        this.channel.shutdownNow()
                .awaitTermination(5, TimeUnit.SECONDS);
    }

}
