import com.amrit.taxiserviceapi.messaging.Duty;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Emulator {

    //private final int numberOfCabs;
    private final Map<String, Cab> cabs;
    private final MessagingService messagingService;
    private BlockingQueue<byte[]> incomingQueue;
    private final ExecutorService executorService;
    private AtomicBoolean isConsuming;

    public Emulator(int numberOfCabs, MessagingService messagingService) {
        //this.numberOfCabs = numberOfCabs;
        cabs = new HashMap<>();
        this.incomingQueue = new LinkedBlockingQueue<>();
        this.isConsuming = new AtomicBoolean(false);
        executorService = Executors.newFixedThreadPool(2);
        for (int i = 0; i < numberOfCabs; i++) {
            String id = "reg-" + i;
            cabs.put(id, new Cab(id));
        }
        this.messagingService = messagingService;
    }


    private void startConsuming() {
        if (isConsuming.compareAndSet(false, true)) {
            messagingService.subscribe("taxiservice.assignduty", incomingQueue);
            executorService.submit(() -> {
               while (true) {
                   byte [] bytes = incomingQueue.take();
                   Duty duty = deserialize(bytes);
                   System.out.println("duty : " + duty);
               }
            });
        }
    }

    private static <V> V deserialize(final byte[] objectData) {
        return org.apache.commons.lang3.SerializationUtils.deserialize(objectData);
    }

    public static void main(String[] args) {

        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092");
        props.setProperty("group.id", "test");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        MessagingService messagingService = new MessagingService(props);

        Emulator emulator = new Emulator(5, messagingService);
        emulator.startConsuming();
    }

}
