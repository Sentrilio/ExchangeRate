package pl.parser.nbp.controllers;

import com.jidesoft.utils.BigDecimalMathUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import pl.parser.nbp.model.Exchange;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import pl.parser.nbp.model.Rate;

import static java.lang.StrictMath.sqrt;
import static java.math.MathContext.*;

@Controller
public class CurrencyController {

	@RequestMapping(value = "/exchange", method = RequestMethod.POST)
//	@ResponseBody
	public HttpEntity<String> exchangePost(@RequestBody Exchange exchange) throws Exception {
		List<Rate> list = new ArrayList<>();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
		LocalDate start = LocalDate.parse(exchange.getStartDate(), formatter);
		LocalDate end = LocalDate.parse(exchange.getEndDate(), formatter);
//		System.out.println(exchange.getCurrencyCode());
//		System.out.println(start);
//		System.out.println(end);
		if (start.isBefore(end)) {
			int formatedStartDate = (start.getYear() % 100) * 10000 + start.getMonthValue() * 100 + start.getDayOfMonth();
			int formatedEndDate = (end.getYear() % 100) * 10000 + end.getMonthValue() * 100 + end.getDayOfMonth();
//			System.out.println("Start: " + formatedStartDate);
//			System.out.println("End: " + formatedEndDate);

			for (LocalDate date = start; date.getYear() <= end.getYear(); date = date.plusYears(1)) {
				String path;
				if (date.getYear() == 2019) {
					path = "http://www.nbp.pl/kursy/xml/dir.txt";
				} else {
					path = "http://www.nbp.pl/kursy/xml/dir" + date.getYear() + ".txt";
				}
				URL nbp = new URL(path);
				BufferedReader in = new BufferedReader(new InputStreamReader(nbp.openStream()));

				int formatedDateFromTxt;
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					if (inputLine.startsWith("c")) {
						formatedDateFromTxt = Integer.parseInt(inputLine.substring(inputLine.length() - 6));
						if (formatedStartDate <= formatedDateFromTxt && formatedDateFromTxt <= formatedEndDate) {
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

//							String data_publikacji = doc.getElementsByTagName("data_publikacji").item(0).getTextContent();
//							LocalDate tempDate = LocalDate.parse(data_publikacji, formatter);
//							if ((start.isEqual(tempDate) || start.isBefore(tempDate)) && (end.isEqual(tempDate) || end.isAfter(tempDate))) {
//							System.out.println("date of publish: " + data_publikacji);
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
												foundCurrencyCode = true;
											}
											if (foundCurrencyCode) {
												if (node.getNodeName().equals("kurs_kupna")) {
													rate.setBuyingRate(new BigDecimal(node.getTextContent().replace(",", ".")));
												}
												if (node.getNodeName().equals("kurs_sprzedazy")) {
													rate.setSellingRate(new BigDecimal(node.getTextContent().replace(",", ".")));
													list.add(rate);
													break outer;
												}
											}
										}
									}
								}
							} else if (formatedDateFromTxt>formatedEndDate) {
								break;
							}
//						}
					}
				}
				in.close();
			}
			BigDecimal sumofBuyingRates = new BigDecimal(0).setScale(4, RoundingMode.HALF_UP);
			for (Rate rate : list) {
				sumofBuyingRates = sumofBuyingRates.add(rate.getBuyingRate());
			}
			BigDecimal avgOfBuyingRates = sumofBuyingRates.divide(new BigDecimal(list.size()), 4, RoundingMode.HALF_UP);
//			System.out.println(avgOfBuyingRates);

			//standard deviation
			BigDecimal sumOfSellingRates = new BigDecimal(0).setScale(32, RoundingMode.HALF_UP);
//			List<BigDecimal> listOfSellingRates = new ArrayList<>();
			for (Rate rate : list) {
//				listOfSellingRates.add(rate.getSellingRate());
				sumOfSellingRates = sumOfSellingRates.add(rate.getSellingRate());
			}
//			System.out.println(BigDecimalMathUtils.stddev(listOfSellingRates,false,MathContext.DECIMAL128).toString());

			BigDecimal avgOfSellingRates = sumOfSellingRates.divide(new BigDecimal(list.size()), 32, RoundingMode.HALF_UP);
			BigDecimal sumOfZ = new BigDecimal(0).setScale(32, RoundingMode.HALF_UP);
			for (Rate rate : list) {
				sumOfZ = sumOfZ.add(((rate.getSellingRate().subtract(avgOfSellingRates)).pow(2)));
			}
			BigDecimal sigma = new BigDecimal(0).setScale(32, RoundingMode.HALF_UP);
			sigma = sigma.add(sumOfZ.divide(new BigDecimal(list.size()), 32, RoundingMode.HALF_UP));

//			String standardDeviation = ("%.4f", Math.sqrt(sigma.doubleValue()));
//			System.out.println(Math.sqrt(sigma.doubleValue()));
			BigDecimal standardDeviation = new BigDecimal(String.valueOf(Math.sqrt(sigma.doubleValue()))).setScale(4,RoundingMode.HALF_UP);
			return new HttpEntity<>(avgOfBuyingRates + "\n" + standardDeviation);
//			Math.pow(sigma.doubleValue(),1/2);
//			System.out.format("%.4f", Math.sqrt(sigma.doubleValue()));
		} else {
			String info = "Start date has to be after end date";
//			System.out.println(info);
			return new HttpEntity<>(info);
		}

	}


}
