package pl.parser.nbp.controllers;

import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.parser.nbp.model.Exchange;

import pl.parser.nbp.services.CurrencyService;

import java.util.Objects;

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

	@GetMapping("/api")
	public String exchangeGet(Model model) {
		model.addAttribute("exchange", new Exchange());
		model.addAttribute("info", "");
		return "api";
	}

	@PostMapping("/api")
	public String calculateAvgAndStdDev(Model model, @ModelAttribute Exchange exchange) throws Exception {
		HttpEntity<String> response = currencyService.getAvgAndStdDev(exchange);
		String lines[] = response.getBody().split("\\r?\\n");
		if (lines.length == 2) {
			model.addAttribute("avgOfBuyingRates", lines[0]);
			model.addAttribute("stdDevOfSellingRates", lines[1]);
			return "result";
		} else if (lines.length == 1) {
			model.addAttribute("info", response.getBody());
			return "api";
		} else {
			return "api";
		}
	}

}
