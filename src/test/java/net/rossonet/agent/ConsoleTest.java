package net.rossonet.agent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;
import org.rossonet.ext.picocli.CommandLine;

@TestMethodOrder(OrderAnnotation.class)
public class ConsoleTest {

	@BeforeAll
	public static void setDebug() {
		CommandLine.tracer().setLevel(CommandLine.TraceLevel.DEBUG);
	}

}
