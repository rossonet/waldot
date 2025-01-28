/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package net.rossonet.waldot.rules;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class representing a named fact. Facts have unique names within a
 * {@link Facts} instance.
 * 
 * @param <T> type of the fact
 * @author Mahmoud Ben Hassine
 * @author Andrea Ambrosini
 */
public class Fact<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(Fact.class);
	private static int maxCharsInValueToString = 80;
	private static boolean humanDateInToString = true;
	private static final String DIGEST_ALGHORITM = "SHA-256";

	private static MessageDigest digest;

	static {
		try {
			digest = MessageDigest.getInstance(DIGEST_ALGHORITM);
		} catch (final NoSuchAlgorithmException e) {
			LOGGER.error("");
		}
	}

	public static int getMaxCharsInValueToString() {
		return maxCharsInValueToString;
	}

	public static boolean isHumanDateInToString() {
		return humanDateInToString;
	}

	public static void setHumanDateInToString(final boolean humanDateInToString) {
		Fact.humanDateInToString = humanDateInToString;
	}

	public static void setMaxCharsInValueToString(final int maxCharsInValueToString) {
		Fact.maxCharsInValueToString = maxCharsInValueToString;
	}

	private final String name;
	private final T value;
	private final long createdAt;

	/**
	 * Create a new fact.
	 * 
	 * @param name  of the fact
	 * @param value of the fact
	 */
	public Fact(final String name, final T value) {
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(value, "value must not be null");
		this.name = name;
		this.value = value;
		this.createdAt = Instant.now().toEpochMilli();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final Fact<?> fact = (Fact<?>) o;
		return name.equals(fact.name);
	}

	public long getCreatedAt() {
		return createdAt;
	}

	/*
	 * The Facts API represents a namespace for facts where each fact has a unique
	 * name. Hence, equals/hashcode are deliberately calculated only on the fact
	 * name.
	 */

	/**
	 * Get the fact name.
	 * 
	 * @return fact name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the fact value.
	 * 
	 * @return fact value
	 */
	public T getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		String valueString = "";
		if (value != null) {
			final String valueCache = value.toString();
			if (valueCache.length() <= maxCharsInValueToString) {
				valueString = value.toString();
			} else {
				valueString = valueCache.substring(0, maxCharsInValueToString) + "[" + valueCache.length() + "::"
						+ Base64.getEncoder()
								.encodeToString(digest.digest(value.toString().getBytes(StandardCharsets.UTF_8)))
						+ "]";
			}
		}
		return "Fact{time="
				+ (humanDateInToString ? Instant.ofEpochMilli(createdAt).toString() : String.valueOf(createdAt))
				+ ", name='" + name + '\'' + ", value=" + valueString + '}';
	}
}
