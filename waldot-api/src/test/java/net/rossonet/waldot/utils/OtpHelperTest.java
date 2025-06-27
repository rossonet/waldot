package net.rossonet.waldot.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OtpHelperTest {

	@Test
	void testCheckTOTPInvalid() {
		final String seed = OtpHelper.getRandomHexString(32);
		final String invalidOtp = "123456";
		final String returnDigits = "6";

		for (final OtpHelper.Algorithm algorithm : OtpHelper.Algorithm.values()) {
			final boolean isValid = OtpHelper.checkTOTP(algorithm, seed, invalidOtp, 3, returnDigits);
			assertFalse(isValid, "Invalid OTP should not be valid for algorithm " + algorithm);
		}
	}

	@Test
	void testGenerateAndCheckTOTPWithAllAlgorithms() {
		final String seed = OtpHelper.getRandomHexString(32); // Generate a random seed
		final long currentTimeMillis = System.currentTimeMillis();

		for (final OtpHelper.Algorithm algorithm : OtpHelper.Algorithm.values()) {
			for (final String returnDigits : new String[] { "6", "8" }) {
				// Generate OTP
				final String otp = OtpHelper.generateTOTP(algorithm, seed, currentTimeMillis, returnDigits);
				assertNotNull(otp, "Generated OTP should not be null");

				// Verify OTP
				final boolean isValid = OtpHelper.checkTOTP(algorithm, seed, otp, 3, returnDigits);
				assertTrue(isValid, "Generated OTP should be valid for algorithm " + algorithm);
			}
		}
	}

	@Test
	void testGetRandomHexString() {
		final int length = 16;
		final String randomHex = OtpHelper.getRandomHexString(length);
		assertNotNull(randomHex, "Random hex string should not be null");
		assertEquals(length, randomHex.length(), "Random hex string should have the correct length");
	}
}