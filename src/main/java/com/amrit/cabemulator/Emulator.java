package com.amrit.cabemulator;

import com.amrit.taxiserviceapi.messaging.Constants;
import com.amrit.taxiserviceapi.messaging.Duty;
import com.amrit.taxiserviceapi.messaging.MessageRecord;
import com.amrit.taxiserviceapi.messaging.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;

public class Emulator implements Cab.UpdateSubscriber {

    private static final Logger LOGGER = LoggerFactory.getLogger(Emulator.class.getName());

    //private final int numberOfCabs;
    private final Map<String, Cab> cabs;
    private final MessagingService messagingService;
    private BlockingQueue<MessageRecord> incomingQueue;
    private final ExecutorService executorService;
    private AtomicBoolean isConsuming;
    private final ThreadFactory threadFactory;

    public Emulator(int numberOfCabs, MessagingService messagingService) {
        //this.numberOfCabs = numberOfCabs;
        cabs = new HashMap<>();
        this.incomingQueue = new LinkedBlockingQueue<>();
        this.isConsuming = new AtomicBoolean(false);
        this.threadFactory = new TaxiServiceThreadFactory();
        executorService = Executors.newFixedThreadPool(2, threadFactory);
        for (int i = 0; i < numberOfCabs; i++) {
            String id = "Cab-" + i;
            cabs.put(id, new Cab(id));
        }
        this.messagingService = messagingService;
    }


    private void startConsuming() {
        if (isConsuming.compareAndSet(false, true)) {
            messagingService.subscribe(Constants.TOPIC_ASSIGN_DUTY, incomingQueue);
            executorService.submit(() -> {
               while (true) {
                   MessageRecord record = incomingQueue.take();
                   Duty duty = deserialize(record.getValue());
                   Cab cab = cabs.get(record.getKey());
                   if (cab != null) {
                       LOGGER.info("Assigned duty : {} to cab {}", duty.toString(), cab.getRegNo());
                       cab.addPositionUpdateSubscriber(this);
                       cab.setTrip(duty.getPositions());
                   } else {
                       LOGGER.error("No cab by registration number " + record.getKey());
                   }
               }
            });
        }
    }

    @Override
    public void notifyUpdate(Cab cab) {
        messagingService.sendMessage(Constants.TOPIC_CAB_POSITION_UPDATE, cab.getRegNo(), serialize(cab.getPosition()));
        LOGGER.debug("Sent position update {} for cab {}", cab.getPosition().toString() , cab.getRegNo());
        if (!cab.isOnTrip()) {
            cab.removePositionSubscriber(this);
        }
    }

    public static void main(String[] args) {

        LOGGER.info("Application started");

        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.setProperty("group.id", Emulator.class.getName());
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        MessagingService messagingService = new MessagingService(props);

        Emulator emulator = new Emulator(5, messagingService);
        emulator.startConsuming();
    }

}
