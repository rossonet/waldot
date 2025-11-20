package net.rossonet.waldot.dtdl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.milo.shaded.com.google.common.collect.Lists;

/**
 * The {@code Unit} enumerator represents the various units of measurement
 * defined in the Digital Twin Definition Language (DTDL) version 2. These units
 * are used to provide semantic meaning to telemetry, properties, and commands
 * in digital twin models.
 *
 * <p>
 * Each unit corresponds to a standard measurement, such as length, mass, or
 * temperature, and is associated with one or more {@link UnitType} categories.
 * This enumerator facilitates the mapping of DTDL unit definitions to their
 * respective categories and provides utility methods for unit retrieval and
 * categorization.
 * </p>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 * @see UnitType
 */
public enum Unit {
	_byte, acre, ampere, astronomicalUnit, bar, bel, bit, bitPerSecond, bytePerSecond, candela, candelaPerSquareMetre,
	centimetre, centimetrePerSecond, centimetrePerSecondSquared, coulomb, cubicCentimetre, cubicFoot, cubicInch,
	cubicMetre, day, decibel, degreeCelsius, degreeFahrenheit, degreeOfArc, degreePerSecond, electronvolt, exbibit,
	exbibitPerSecond, exbibyte, exbibytePerSecond, farad, fluidOunce, foot, footcandle, gallon, general, gForce,
	gibibit, gibibitPerSecond, gibibyte, gibibytePerSecond, gigahertz, gigajoule, gigawatt, gram, gramPerCubicMetre,
	gramPerHour, gramPerSecond, hectare, henry, hertz, horsepower, hour, inch, inchesOfMercury, inchesOfWater, joule,
	kelvin, kibibit, kibibitPerSecond, kibibyte, kibibytePerSecond, kilogram, kilogramPerCubicMetre, kilogramPerHour,
	kilogramPerSecond, kilohertz, kilojoule, kilometre, kilometrePerHour, kilometrePerSecond, kiloohm, kilopascal,
	kilovolt, kilowatt, kilowattHour, kilowattHourPerYear, knot, litre, litrePerHour, litrePerSecond, lumen, lux,
	maxwell, mebibit, mebibitPerSecond, mebibyte, mebibytePerSecond, megaelectronvolt, megahertz, megajoule, megaohm,
	megavolt, megawatt, metre, metrePerHour, metrePerSecond, metrePerSecondSquared, microampere, microfarad, microgram,
	microhenry, micrometre, microsecond, microvolt, microwatt, mile, milePerHour, milePerSecond, milliampere, millibar,
	millifarad, milligram, millihenry, millilitre, millilitrePerHour, millilitrePerSecond, millimetre,
	millimetresOfMercury, milliohm, millisecond, millivolt, milliwatt, minute, minuteOfArc, nanofarad, nanometre,
	nanosecond, nauticalMile, newton, newtonMetre, ohm, ounce, pascal, percent, picofarad, pound, poundPerSquareInch,
	radian, radianPerSecond, radianPerSecondSquared, revolutionPerMinute, revolutionPerSecond, second, secondOfArc,
	slug, squareCentimetre, squareFoot, squareInch, squareKilometre, squareMetre, squareMillimetre, tebibit,
	tebibitPerSecond, tebibyte, tebibytePerSecond, tesla, ton, tonne, turn, unity, volt, watt, weber, year, yobibit,
	yobibitPerSecond, yobibyte, yobibytePerSecond, zebibit, zebibitPerSecond, zebibyte, zebibytePerSecond;

	/**
	 * Retrieves the {@code Unit} corresponding to the specified string value.
	 *
	 * <p>
	 * This method maps the input string to a {@code Unit} instance, handling
	 * special cases such as the {@code "byte"} unit, which is internally
	 * represented as {@code "_byte"}.
	 * </p>
	 *
	 * @param value the string representation of the unit.
	 * @return the corresponding {@code Unit} instance.
	 * @throws IllegalArgumentException if the specified value does not match any
	 *                                  {@code Unit}.
	 */
	public static Unit getUnit(final String value) {
		final String checkValue = (value.equals("byte") ? "_byte" : value);
		return Unit.valueOf(checkValue);
	}

	/**
	 * Retrieves the {@link UnitType} categories associated with this {@code Unit}.
	 *
	 * <p>
	 * Each {@code Unit} can belong to one or more {@link UnitType} categories,
	 * which group related units based on their physical quantities (e.g., length,
	 * mass, temperature).
	 * </p>
	 *
	 * @return a collection of {@link UnitType} categories associated with this
	 *         {@code Unit}.
	 */
	public Collection<UnitType> getUnitTypes() {
		final Set<UnitType> result = new HashSet<>();
		for (final UnitType u : UnitType.values()) {
			if (Lists.newArrayList(u.getUnits()).contains(this)) {
				result.add(u);
			}
		}
		return result;
	}
}