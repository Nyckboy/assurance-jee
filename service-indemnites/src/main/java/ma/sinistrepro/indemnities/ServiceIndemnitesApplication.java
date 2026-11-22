package ma.sinistrepro.indemnities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ServiceIndemnitesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceIndemnitesApplication.class, args);
    }
}