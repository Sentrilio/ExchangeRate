package pl.parser.nbp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import pl.parser.nbp.controllers.CurrencyController;
import pl.parser.nbp.model.Exchange;

import java.util.Arrays;

@SpringBootApplication
public class MainClass implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(MainClass.class);
	public static void main(String[] args)  {
		SpringApplication.run(MainClass.class, args).close();
	}


	@Override
	public void run(String...args) throws Exception {
		logger.info("Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
		CurrencyController currencyController= new CurrencyController();
		if(args.length==3){
			Exchange exchange = new Exchange();
			exchange.setCurrencyCode(args[0]);
			exchange.setStartDate(args[1]);
			exchange.setEndDate(args[2]);
			currencyController.exchangePost(exchange);
		}else{
			Exchange exchange = new Exchange();
			exchange.setCurrencyCode("EUR");
			exchange.setStartDate("2013-01-28");
			exchange.setEndDate("2013-01-31");
			currencyController.exchangePost(exchange);
		}
		//		int exitValue = SpringApplication.exit(context);
//		System.exit(exitValue);
	}
}
