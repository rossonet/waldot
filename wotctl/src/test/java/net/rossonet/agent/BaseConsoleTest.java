package net.rossonet.agent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class BaseConsoleTest {

	@Test
	public void runServerClientOneMinutes() throws InterruptedException, ExecutionException {
		// TODO: implementare test
		Thread.sleep(60_000);
	}

	@Test
	public void TryConsoleImplementation() throws IOException {

	}

}