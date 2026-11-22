package ma.sinistrepro.indemnities;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Starts the whole application (Feign clients, Kafka listener, outbox publisher, JPA).
// The listener stays stopped: the tests have no Kafka broker.
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class ServiceIndemnitesApplicationTests {

    @Test
    void contextLoads() {
    }
}
