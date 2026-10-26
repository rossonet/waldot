package net.rossonet.waldot.dtdl;

import net.rossonet.waldot.utils.TextHelper;

/**
 * The {@code DigitalTwinModelIdentifier} class represents a unique identifier
 * for a digital twin model as defined in the Digital Twin Definition Language
 * (DTDL) version 2.
 *
 * <p>
 * This identifier is composed of three main components:
 * </p>
 * <ul>
 * <li>{@code scheme}: The scheme of the identifier, typically "dtmi" for DTDL
 * models.</li>
 * <li>{@code path}: The hierarchical path that uniquely identifies the model
 * within the scheme.</li>
 * <li>{@code version}: The version of the model, represented as an
 * integer.</li>
 * </ul>
 *
 * <p>
 * The identifier follows the format: {@code scheme:path;version}, where:
 * </p>
 * <ul>
 * <li>{@code scheme} specifies the protocol or namespace (e.g., "dtmi").</li>
 * <li>{@code path} provides a unique hierarchical path for the model (e.g.,
 * "example:Thermostat").</li>
 * <li>{@code version} indicates the version of the model (e.g., "1").</li>
 * </ul>
 *
 * @see <a href=
 *      "https://github.com/Azure/opendigitaltwins-dtdl/blob/master/DTDL/v2/DTDL.v2.md">DTDL
 *      v2 Specification</a>
 */
public class DigitalTwinModelIdentifier {
	/**
	 * Creates a {@code DigitalTwinModelIdentifier} instance from a string
	 * representation.
	 *
	 * @param value the string representation of the identifier, following the
	 *              format {@code scheme:path;version}.
	 * @return a new {@code DigitalTwinModelIdentifier} instance.
	 */
	public static DigitalTwinModelIdentifier fromString(final String value) {
		return new DigitalTwinModelIdentifier(value);
	}

	private final String path;
	private final String scheme;

	private final int version;

	private DigitalTwinModelIdentifier(final String value) {
		if (value == null) {
			throw new IllegalArgumentException("DigitalTwinModelIdentifier cannot be null or empty");
		}
		String cleanValue = value.toLowerCase().trim();
		if (cleanValue.isEmpty()) {
			throw new IllegalArgumentException("DigitalTwinModelIdentifier cannot be null or empty");
		}
		if (TextHelper.isDirtyValue(cleanValue)) {
			cleanValue = TextHelper.cleanText(cleanValue);
		}
		if (!cleanValue.contains(":") || !cleanValue.contains(";")) {
			throw new IllegalArgumentException("DigitalTwinModelIdentifier must be in format scheme:path;version");
		}
		scheme = cleanValue.split(":", 2)[0];
		final String others = cleanValue.substring(scheme.length() + 1);
		if (!others.contains(";")) {
			throw new IllegalArgumentException("DigitalTwinModelIdentifier must include a version section");
		}
		final String[] pathAndVersion = others.split(";", 2);
		path = pathAndVersion[0];
		version = Integer.parseInt(pathAndVersion[1]);
	}

	/**
	 * Retrieves the hierarchical path of the model identifier.
	 *
	 * @return the path component of the identifier.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Splits the hierarchical path into its individual segments.
	 *
	 * @return an array of strings representing the path segments.
	 */
	public String[] getPathSegments() {
		if (path.contains(":")) {
			return path.split(":");
		} else {
			return new String[] { path };
		}
	}

	/**
	 * Retrieves the scheme of the model identifier.
	 *
	 * @return the scheme component of the identifier.
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Retrieves the version of the model identifier.
	 *
	 * @return the version component of the identifier as an integer.
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Validates the model identifier.
	 *
	 * <p>
	 * A valid identifier must satisfy the following conditions:
	 * </p>
	 * <ul>
	 * <li>The {@code path} is not null or empty.</li>
	 * <li>The {@code scheme} is not null or empty.</li>
	 * <li>The {@code version} is greater than 0.</li>
	 * </ul>
	 *
	 * @return {@code true} if the identifier is valid, {@code false} otherwise.
	 */
	public boolean isValid() {
		return (path != null && !path.isEmpty()) && (scheme != null && !scheme.isEmpty() && version > 0);

	}

	/**
	 * Returns a string representation of the model identifier.
	 *
	 * <p>
	 * The string includes the scheme, path, and version components, formatted as:
	 * </p>
	 * 
	 * <pre>
	 * Model Identifier [scheme=..., path=..., version=...]
	 * </pre>
	 *
	 * @return a string representation of the identifier.
	 */
	@Override
	public String toString() {
		return getScheme() + ":" + getPath() + ";" + getVersion();
	}

}