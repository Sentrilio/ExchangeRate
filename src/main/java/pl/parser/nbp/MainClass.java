package pl.parser.nbp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import pl.parser.nbp.model.Exchange;

import java.util.Arrays;

@SpringBootApplication
public class MainClass implements CommandLineRunner {

	@Value("${server.port}")
	int port;
	private static final Logger logger = LoggerFactory.getLogger(MainClass.class);

	public static void main(String[] args) {
		SpringApplication.run(MainClass.class, args);
	}

	@Override
	public void run(String... args) {
		long start = System.nanoTime();
		logger.info("Application started with command-line arguments: {} . \n", Arrays.toString(args));
		if (args.length == 3) {
			Exchange exchange = new Exchange();
			exchange.setCurrencyCode(args[0]);
			exchange.setStartDate(args[1]);
			exchange.setEndDate(args[2]);
			RestTemplate restTemplate = new RestTemplate();
			try {
				ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/exchange", exchange, String.class);
				System.out.println(response.getBody());
			} catch (Exception e) {
			}
		}
		long end = System.nanoTime();
		logger.info("Time of executing method: {}", (end - start) / 1000000);
	}
}
