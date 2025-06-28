package net.rossonet.waldot.dtdl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Lists;

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
	metrePerSecondSquared, centimetrePerSecondSquared, gForce, radianPerSecondSquared, radianPerSecond, degreePerSecond,
	revolutionPerSecond, revolutionPerMinute, squareMetre, squareCentimetre, squareMillimetre, squareKilometre, hectare,
	squareFoot, squareInch, acre, farad, millifarad, microfarad, nanofarad, picofarad, ampere, microampere, milliampere,
	bitPerSecond, kibibitPerSecond, mebibitPerSecond, gibibitPerSecond, tebibitPerSecond, exbibitPerSecond,
	zebibitPerSecond, yobibitPerSecond, bytePerSecond, kibibytePerSecond, mebibytePerSecond, gibibytePerSecond,
	tebibytePerSecond, exbibytePerSecond, zebibytePerSecond, yobibytePerSecond, bit, kibibit, mebibit, gibibit, tebibit,
	exbibit, zebibit, yobibit, _byte, kibibyte, mebibyte, gibibyte, tebibyte, exbibyte, zebibyte, yobibyte, coulomb,
	joule, kilojoule, megajoule, gigajoule, electronvolt, megaelectronvolt, kilowattHour, hertz, kilohertz, megahertz,
	gigahertz, kilogramPerCubicMetre, gramPerCubicMetre, lux, footcandle, henry, millihenry, microhenry, secondOfArc,
	turn, metre, centimetre, millimetre, micrometre, nanometre, kilometre, foot, inch, mile, nauticalMile,
	astronomicalUnit, candelaPerSquareMetre, gigawatt, horsepower, lumen, candela, weber, maxwell, tesla, kilogram,
	gram, milligram, microgram, tonne, slug, gramPerSecond, kilogramPerSecond, gramPerHour, kilogramPerHour, watt,
	microwatt, milliwatt, kilowatt, megawatt, kilowattHourPerYear, pascal, kilopascal, bar, millibar,
	millimetresOfMercury, poundPerSquareInch, inchesOfMercury, inchesOfWater, unity, percent, ohm, milliohm, kiloohm,
	megaohm, decibel, bel, kelvin, degreeCelsius, degreeFahrenheit, newton, pound, ounce, ton, second, millisecond,
	microsecond, nanosecond, minute, hour, day, year, newtonMetre, metrePerSecond, centimetrePerSecond,
	kilometrePerSecond, metrePerHour, kilometrePerHour, milePerHour, milePerSecond, knot, volt, millivolt, microvolt,
	kilovolt, megavolt, cubicMetre, cubicCentimetre, litre, millilitre, cubicFoot, cubicInch, fluidOunce, gallon,
	litrePerSecond, millilitrePerSecond, litrePerHour, millilitrePerHour, radian, degreeOfArc, minuteOfArc;

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