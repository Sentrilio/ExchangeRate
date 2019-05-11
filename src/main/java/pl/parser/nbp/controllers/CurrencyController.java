package pl.parser.nbp.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import pl.parser.nbp.model.Exchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;

@Controller
public class CurrencyController {


	@GetMapping("/exchange")
	public String exchangeGet(Model model) {
		model.addAttribute("exchange", new Exchange());
		model.addAttribute("info", "");

		return "exchange";
	}

	@PostMapping("/exchange")
	public String exchangePost(Model model, @ModelAttribute Exchange exchange) throws ParseException, IOException {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
		LocalDate start = LocalDate.parse(exchange.getStartDate(), formatter);
		LocalDate end = LocalDate.parse(exchange.getEndDate(), formatter);

		System.out.println(start);
		System.out.println(end);
		if (start.isBefore(end)) {
			int formatedStartDate = (start.getYear() % 100) * 10000 + start.getMonthValue() * 100 + start.getDayOfMonth();
			int formatedEndDate = (end.getYear() % 100) * 10000 + end.getMonthValue() * 100 + end.getDayOfMonth();
			System.out.println("Start: " + formatedStartDate);
			System.out.println("End: " + formatedEndDate);

			for (LocalDate date = start; date.getYear() <= end.getYear(); date = date.plusYears(1)) {
				String path;
				if (date.getYear() == 2019) {
					path = "http://www.nbp.pl/kursy/xml/dir.txt";
				} else {
					path = "http://www.nbp.pl/kursy/xml/dir" + date.getYear() + ".txt";
				}
				URL nbp = new URL(path);
				BufferedReader in = new BufferedReader(
						new InputStreamReader(nbp.openStream()));

				int formatedDateFromTxt;
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					if (inputLine.startsWith("c")) {
						formatedDateFromTxt = Integer.parseInt(inputLine.substring(inputLine.length() - 6));
						if (formatedStartDate <= formatedDateFromTxt && formatedDateFromTxt <= formatedEndDate) {
							System.out.println(formatedDateFromTxt);
//							formatedStartDate += date.getMonthValue() * 100 + date.getDayOfMonth();
						}
//						System.out.println(formatedStartDate);
//						System.out.println(formatedDateFromTxt);
					}
				}
				in.close();
			}
			return "result";
		} else {
			String info = "Start date has to be after end date";
			model.addAttribute("info", info);
			return "exchange";
		}


	}
}
