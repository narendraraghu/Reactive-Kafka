package com.raective.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class KafkaReactiveApplication {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC = "demo-topic";

    private final KafkaSender<Integer, String> sender;
    private final SimpleDateFormat dateFormat;

    public KafkaReactiveApplication(String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        SenderOptions<Integer, String> senderOptions = SenderOptions.create(props);

        sender = KafkaSender.create(senderOptions);
        dateFormat = new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy");
    }



    public void sendMessages(String topic, int count, CountDownLatch latch) throws InterruptedException {
        sender.<Integer>send(Flux.range(1, count)
                .map(i -> SenderRecord.create(new ProducerRecord<>(topic, i, "Message_" + i), i)))
                .subscribe(r -> {
                    RecordMetadata metadata = r.recordMetadata();
                    System.out.printf("Message %d sent successfully, topic-partition=%s-%d offset=%d timestamp=%s\n",
                            r.correlationMetadata(),
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            dateFormat.format(new Date(metadata.timestamp())));
                    latch.countDown();
                });
    }

    public void close() {
        sender.close();
    }


    public static void main(String[] args) throws Exception {
        int count = 30;
        CountDownLatch latch = new CountDownLatch(count);
        KafkaReactiveApplication producer = new KafkaReactiveApplication(BOOTSTRAP_SERVERS);
        producer.sendMessages(TOPIC, count, latch);
        latch.await(1000, TimeUnit.SECONDS);
        producer.close();
    }
}
