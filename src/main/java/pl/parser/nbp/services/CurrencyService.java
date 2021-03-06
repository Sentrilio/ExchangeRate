package pl.parser.nbp.services;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import pl.parser.nbp.model.Exchange;
import pl.parser.nbp.utils.Calculation;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CurrencyService {

	public HttpEntity<String> getAvgAndStdDev(Exchange exchange) throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyy-MM-dd");
		LocalDate startDate = LocalDate.parse(exchange.getStartDate(), formatter);
		LocalDate endDate = LocalDate.parse(exchange.getEndDate(), formatter);
		if (startDate.isBefore(endDate)) {
			List<BigDecimal> listOfBuyingRates = new ArrayList<>();
			List<BigDecimal> listOfSellingRates = new ArrayList<>();
			int formatedStartDate = getFormatedStartDate(startDate);
			int formatedEndDate = getFormatedStartDate(endDate);
			for (LocalDate date = startDate; date.getYear() <= endDate.getYear(); date = date.plusYears(1)) {
				String path = createNBPPath(date);
				if (path == null) {
					return new HttpEntity<>("Year must be between 2002-2019");
				}
				URL nbp = new URL(path);
				BufferedReader in = new BufferedReader(new InputStreamReader(nbp.openStream()));

				int formatedDateFromTxt;
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					if (inputLine.startsWith("c")) {
						formatedDateFromTxt = Integer.parseInt(inputLine.substring(inputLine.length() - 6));
						if (formatedStartDate <= formatedDateFromTxt && formatedDateFromTxt <= formatedEndDate) {
							RestTemplate restTemplate = new RestTemplate();
							String resourceUrl = "http://www.nbp.pl/kursy/xml/" + inputLine + ".xml";
							ResponseEntity<String> response = restTemplate.getForEntity(resourceUrl, String.class);
							collectRates(response, listOfBuyingRates, listOfSellingRates, exchange);
						} else if (formatedDateFromTxt > formatedEndDate) {
							break;
						}
					}
				}
				in.close();
			}
			Calculation calculation = new Calculation();
			if (listOfBuyingRates.isEmpty() && listOfSellingRates.isEmpty()) {
				return new HttpEntity<>("There were no publications in this period");
			} else {
				BigDecimal averageOfBuyingRates = calculation.avg(listOfBuyingRates);
				BigDecimal stdDevOfSellingRates = calculation.stdDev(listOfSellingRates);
				return new HttpEntity<>(averageOfBuyingRates + "\n" + stdDevOfSellingRates);
//				return new HttpEntity<>(averageOfBuyingRates+"\n"+stdDevOfSellingRates);
			}
		} else {
			return new HttpEntity<>("Start date has to be after end date");
		}
	}

	private void collectRates(ResponseEntity<String> response, List<BigDecimal> listOfBuyingRates, List<BigDecimal> listOfSellingRates, @RequestBody Exchange exchange) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		InputSource src = new InputSource();
		src.setCharacterStream(new StringReader(Objects.requireNonNull(response.getBody())));
		System.out.println(response.getBody());
		Document doc = builder.parse(src);
		NodeList nodelist = doc.getElementsByTagName("pozycja");
		Node node;
		boolean foundCurrencyCode = false;
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
							listOfBuyingRates.add(new BigDecimal(node.getTextContent().replace(",", ".")));
						}
						if (node.getNodeName().equals("kurs_sprzedazy")) {
							listOfSellingRates.add(new BigDecimal(node.getTextContent().replace(",", ".")));
//												rate.setSellingRate(new BigDecimal(node.getTextContent().replace(",", ".")));
//												listOfRates.add(rate);
							break outer;
						}
					}
				}
			}
		}
	}

	private int getFormatedStartDate(LocalDate date) {
		return (date.getYear() % 100) * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();
	}

	private String createNBPPath(LocalDate date) {
		String path;
		if (date.getYear() == 2019) {
			path = "http://www.nbp.pl/kursy/xml/dir.txt";
		} else if (date.getYear() >= 2002 && date.getYear() <= 2018) {
			path = "http://www.nbp.pl/kursy/xml/dir" + date.getYear() + ".txt";
		} else {
			return null;
		}
		return path;
	}

}
