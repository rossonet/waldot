package net.rossonet.waldot.opc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ManagedValueParsingTest {

	@ParameterizedTest
	@CsvSource({ "5000,5000", "5000.0,5000", " 42 ,42", "-3,-3", "1e3,1000", "0,0" })
	void wholeNumbers(final String text, final long expected) {
		assertEquals(expected, AbstractOpcVertex.parseWhole(text));
	}

	@ParameterizedTest
	@ValueSource(strings = { "20.6", "abc", "", "1.5e0", "NaN", "Infinity", "1e400" })
	void notWholeNumbers(final String text) {
		assertThrows(NumberFormatException.class, () -> AbstractOpcVertex.parseWhole(text));
	}

	@ParameterizedTest
	@CsvSource({ "true,true", "TRUE,true", "1,true", "yes,true", "on,true", "si,true", "false,false", "False,false",
			"0,false", "no,false", "off,false", " true ,true" })
	void booleans(final String text, final boolean expected) {
		assertEquals(expected, AbstractOpcVertex.parseBoolean(text));
	}

	@ParameterizedTest
	@ValueSource(strings = { "abc", "", "2", "vero", "t" })
	void notBooleans(final String text) {
		assertThrows(IllegalArgumentException.class, () -> AbstractOpcVertex.parseBoolean(text));
	}
}
