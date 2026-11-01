package ma.sinistrepro.claims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ServiceSinistresApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceSinistresApplication.class, args);
	}

}
