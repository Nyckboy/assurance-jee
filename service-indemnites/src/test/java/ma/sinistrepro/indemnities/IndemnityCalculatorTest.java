package ma.sinistrepro.indemnities;

import ma.sinistrepro.indemnities.service.IndemnityCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndemnityCalculatorTest {

    private IndemnityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new IndemnityCalculator();
    }

    @Test
    void shouldCalculateIndemnityWithPlafondAndFranchise() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("10000"),
                new BigDecimal("8000"),
                new BigDecimal("1000")
        );

        assertEquals(new BigDecimal("7000"), result);
    }

    @Test
    void shouldCalculateIndemnityWhenAmountIsBelowPlafond() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("5000"),
                new BigDecimal("8000"),
                new BigDecimal("1000")
        );

        assertEquals(new BigDecimal("4000"), result);
    }

    @Test
    void shouldReturnZeroWhenFranchiseExceedsEligibleAmount() {
        BigDecimal result = calculator.calculate(
                new BigDecimal("500"),
                new BigDecimal("8000"),
                new BigDecimal("1000")
        );

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void shouldRejectNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculate(
                        new BigDecimal("-100"),
                        new BigDecimal("8000"),
                        new BigDecimal("1000")
                )
        );
    }
}