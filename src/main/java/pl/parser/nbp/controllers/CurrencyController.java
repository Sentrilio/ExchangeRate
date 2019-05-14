package pl.parser.nbp.controllers;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pl.parser.nbp.model.Exchange;

import pl.parser.nbp.services.CurrencyService;

@Controller
public class CurrencyController {

	private final CurrencyService currencyService;

	public CurrencyController(CurrencyService currencyService) {
		this.currencyService = currencyService;
	}

	@RequestMapping(value = "/exchange", method = RequestMethod.POST)
	public HttpEntity<String> exchangePost(@RequestBody Exchange exchange) throws Exception {
		return currencyService.getAvgAndStdDev(exchange);
	}

}
