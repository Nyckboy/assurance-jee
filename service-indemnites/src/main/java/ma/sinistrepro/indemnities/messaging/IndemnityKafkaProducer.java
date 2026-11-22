package ma.sinistrepro.indemnities.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class IndemnityKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public IndemnityKafkaProducer(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Publication Kafka interrompue sur le topic " + topic,
                    exception
            );
        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                    "Échec de publication Kafka sur le topic " + topic,
                    exception
            );
        }
    }
}