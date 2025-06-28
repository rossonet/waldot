package net.rossonet.waldot.dtdl;

/**
 * The {@code SemanticType} enumerator represents the semantic meaning of
 * various physical quantities and their associated {@link UnitType} in the
 * Digital Twin Definition Language (DTDL) version 2.
 *
 * <p>
 * Each {@code SemanticType} maps to a specific {@link UnitType}, which groups
 * related units that describe physical quantities such as acceleration,
 * temperature, or pressure. This mapping enables digital twin models to provide
 * meaningful context for telemetry, properties, and commands.
 * </p>
 *
 * <p>
 * Semantic types in DTDL are used to standardize the representation of physical
 * quantities, ensuring interoperability and consistency across digital twin
 * models.
 * </p>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 * @see UnitType
 */
public enum SemanticType {
	Acceleration(UnitType.AccelerationUnit), Angle(UnitType.AngleUnit),
	AngularAcceleration(UnitType.AngularAccelerationUnit), AngularVelocity(UnitType.AngularVelocityUnit),
	Area(UnitType.AreaUnit), Capacitance(UnitType.CapacitanceUnit), Current(UnitType.CurrentUnit),
	DataRate(UnitType.DataRateUnit), DataSize(UnitType.DataSizeUnit), Density(UnitType.DensityUnit),
	Distance(UnitType.LengthUnit), ElectricCharge(UnitType.ChargeUnit), Energy(UnitType.EnergyUnit),
	Force(UnitType.ForceUnit), Frequency(UnitType.FrequencyUnit), Humidity(UnitType.DensityUnit),
	Illuminance(UnitType.IlluminanceUnit), Inductance(UnitType.InductanceUnit), Latitude(UnitType.AngleUnit),
	Longitude(UnitType.AngleUnit), Length(UnitType.LengthUnit), Luminance(UnitType.LuminanceUnit),
	Luminosity(UnitType.PowerUnit), LuminousFlux(UnitType.LuminousFluxUnit),
	LuminousIntensity(UnitType.LuminousIntensityUnit), MagneticFlux(UnitType.MagneticFluxUnit),
	MagneticInduction(UnitType.MagneticInductionUnit), Mass(UnitType.MassUnit), MassFlowRate(UnitType.MassFlowRateUnit),
	Power(UnitType.PowerUnit), Pressure(UnitType.PressureUnit), RelativeHumidity(UnitType.unitless),
	Resistance(UnitType.ResistanceUnit), SoundPressure(UnitType.SoundPressureUnit),
	Temperature(UnitType.TemperatureUnit), Thrust(UnitType.ForceUnit), TimeSpan(UnitType.TimeUnit),
	Torque(UnitType.TorqueUnit), Velocity(UnitType.VelocityUnit), Voltage(UnitType.VoltageUnit),
	Volume(UnitType.VolumeUnit), VolumeFlowRate(UnitType.VolumeFlowRateUnit);

	private final UnitType unitType;

	/**
	 * Constructs a {@code SemanticType} with the specified {@link UnitType}.
	 *
	 * @param unitType the {@link UnitType} associated with this
	 *                 {@code SemanticType}.
	 */
	private SemanticType(final UnitType unitType) {
		this.unitType = unitType;
	}

	/**
	 * Retrieves the {@link UnitType} associated with this {@code SemanticType}.
	 *
	 * <p>
	 * The {@link UnitType} defines the group of units that are semantically related
	 * to this {@code SemanticType}. For example, the {@code Temperature} semantic
	 * type is associated with the {@code TemperatureUnit} group, which includes
	 * units such as Celsius, Fahrenheit, and Kelvin.
	 * </p>
	 *
	 * @return the {@link UnitType} associated with this {@code SemanticType}.
	 */
	public UnitType getUnitType() {
		return unitType;
	}
}