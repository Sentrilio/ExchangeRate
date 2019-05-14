package pl.parser.nbp.utils;

import com.jidesoft.utils.BigDecimalMathUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public class Calculation {

	public BigDecimal avg(List<BigDecimal> list) {
		BigDecimal sum = list.stream()
				.map(Objects::requireNonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(new BigDecimal(list.size()), RoundingMode.HALF_UP);
	}

	public BigDecimal stddev(List<BigDecimal> list) {
		return BigDecimalMathUtils.stddev(list,false,MathContext.DECIMAL128)
				.setScale(4,RoundingMode.HALF_UP);
	}
}
