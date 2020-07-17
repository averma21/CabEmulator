import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessagingService {

    private final KafkaConsumer<String, byte[]> consumer;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final Map<String, Queue<byte[]>> queues;

    public MessagingService(Properties kafkaProps) {
        this.consumer = new KafkaConsumer<String, byte[]>(kafkaProps);
        this.queues = new HashMap<>();
    }

    private void startConsuming() {
        executorService.submit(() -> {
            while (true) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, byte[]> record : records) {
                    queues.get(record.topic()).add(record.value());
                }
            }
        });
    }

    public void subscribe(String topic, Queue<byte[]> queue) {
        if (!queues.containsKey(topic)) {
            consumer.subscribe(Collections.singleton(topic));
            queues.put(topic, queue);
            startConsuming();
        }
    }
}
