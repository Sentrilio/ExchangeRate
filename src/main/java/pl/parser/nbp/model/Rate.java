package pl.parser.nbp.model;

import java.math.BigDecimal;

public class Rate {

	private BigDecimal buyingRate;
	private BigDecimal sellingRate;

	public BigDecimal getBuyingRate() {
		return buyingRate;
	}

	public void setBuyingRate(BigDecimal buyingRate) {
		this.buyingRate = buyingRate;
	}

	public BigDecimal getSellingRate() {
		return sellingRate;
	}

	public void setSellingRate(BigDecimal sellingRate) {
		this.sellingRate = sellingRate;
	}

	@Override
	public String toString() {
		return "Rate{" +
				"buyingRate=" + buyingRate +
				", sellingRate=" + sellingRate +
				'}';
	}
}
