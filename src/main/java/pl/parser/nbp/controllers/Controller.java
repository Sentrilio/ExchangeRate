package pl.parser.nbp.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import pl.parser.nbp.model.Exchange;
import pl.parser.nbp.model.Rate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Controller {


	public void exchangePost(Model model, @ModelAttribute Exchange exchange) throws Exception {
		List<Rate> list = new ArrayList<>();

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
				BufferedReader in = new BufferedReader(new InputStreamReader(nbp.openStream()));

//				int formatedDateFromTxt;
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					if (inputLine.startsWith("c")) {
//						formatedDateFromTxt = Integer.parseInt(inputLine.substring(inputLine.length() - 6));
//						if (formatedStartDate <= formatedDateFromTxt && formatedDateFromTxt <= formatedEndDate) {
//							System.out.println(formatedDateFromTxt);
//							System.out.println(inputLine);

						RestTemplate restTemplate = new RestTemplate();
						String resourceUrl = "http://www.nbp.pl/kursy/xml/" + inputLine + ".xml";
						ResponseEntity<String> response
								= restTemplate.getForEntity(resourceUrl, String.class);

						DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						InputSource src = new InputSource();
						src.setCharacterStream(new StringReader(Objects.requireNonNull(response.getBody())));
						Document doc = builder.parse(src);
						NodeList nodelist = doc.getElementsByTagName("pozycja");

						String data_publikacji = doc.getElementsByTagName("data_publikacji").item(0).getTextContent();
						LocalDate tempDate = LocalDate.parse(data_publikacji, formatter);
						if ((start.isEqual(tempDate) || start.isBefore(tempDate)) && (end.isEqual(tempDate) || end.isAfter(tempDate))) {
							System.out.println("date of publish: " + data_publikacji);
//							System.out.println(inputLine);
//							System.out.println("nodelist length: " + nodelist.getLength());
							Node node;
							boolean foundCurrencyCode = false;
							Rate rate = new Rate();
							outer:
							for (int i = 0; i < nodelist.getLength(); i++) {
								for (int j = 0; j < nodelist.item(i).getChildNodes().getLength(); j++) {
									node = nodelist.item(i).getChildNodes().item(j);
									if (node.getNodeType() == Node.ELEMENT_NODE) {
										if (node.getTextContent().equals(exchange.getCurrencyCode())) {
//											System.out.println(node.getNodeName() + ": " + node.getTextContent());
											foundCurrencyCode = true;
										}
										if (foundCurrencyCode) {
											if (node.getNodeName().equals("kurs_kupna")) {
												rate.setBuyingRate(new BigDecimal(node.getTextContent().replace(",", ".")));
//												System.out.println(node.getNodeName() + ": " + node.getTextContent());
											}
											if (node.getNodeName().equals("kurs_sprzedazy")) {
												rate.setSellingRate(new BigDecimal(node.getTextContent().replace(",", ".")));
												list.add(rate);
//												System.out.println(node.getNodeName() + ": " + node.getTextContent());
												break outer;
											}
										}
									}
								}
							}
						} else if (tempDate.isAfter(end)) {
							break;
						}
					}
				}
				in.close();
			}
			BigDecimal sumofBuyingRates = new BigDecimal(0);
			for (Rate rate : list) {
				sumofBuyingRates = sumofBuyingRates.add(rate.getBuyingRate());
			}
//			System.out.println("sum of Selling Rates: " + sumofBuyingRates);
			System.out.println("average of selling rates: "
					+ sumofBuyingRates.divide(new BigDecimal(list.size()), 4, RoundingMode.HALF_UP));
		} else {
			String info = "Start date has to be after end date";
			System.out.println(info);
		}

	}


}

