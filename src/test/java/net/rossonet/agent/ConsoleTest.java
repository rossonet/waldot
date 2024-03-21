package net.rossonet.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.cmd.BasePicocliRunner;
import picocli.CommandLine;

@TestMethodOrder(OrderAnnotation.class)
public class ConsoleTest {

	@BeforeAll
	public static void setDebug() {
		CommandLine.tracer().setLevel(CommandLine.TraceLevel.DEBUG);
	}

	private StringWriter appenderOutput = new StringWriter();

	private StringWriter appenderError = new StringWriter();

	private PrintWriter output = new PrintWriter(appenderOutput);
	private PrintWriter error = new PrintWriter(appenderError);

	private BasePicocliRunner instance;

	@Test
	@Order(6)
	public void execCommandErrorParameterTest() {
		final String[] commandHumanShort = new String[] { "-t", "error" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanShort);
		printAndResetInputErrorStreams();
		assertEquals(2, instance.waitCompletionAndGetReturnCode());
		final String[] commandHumanLong = new String[] { "--output-format", "another" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanLong);
		printAndResetInputErrorStreams();
		assertEquals(2, instance.waitCompletionAndGetReturnCode());
	}

	@Test
	@Order(3)
	public void execCommandHumanTest() {
		// final String[] command = new String[] { "sysinfo" };
		final String[] command = new String[0];
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error).execute(command);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
		final String[] commandHumanShort = new String[] { "-t", "human" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanShort);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
		final String[] commandHumanLong = new String[] { "--output-format", "human" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanLong);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
	}

	@Test
	@Order(4)
	public void execCommandJsonTest() {
		final String[] commandHumanShort = new String[] { "-t", "json" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanShort);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
		final String[] commandHumanLong = new String[] { "--output-format", "json" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanLong);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
	}

	@Test
	@Order(5)
	public void execCommandYamlTest() {
		final String[] commandHumanShort = new String[] { "-t", "yaml" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanShort);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
		final String[] commandHumanLong = new String[] { "--output-format", "yaml" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandHumanLong);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());

	}

	@Test
	@Order(1)
	public void helpCommandTest() {
		final String[] commandLong = new String[] { "--help" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandLong);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
		final String[] commandShort = new String[] { "-h" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandShort);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
	}

	private void printAndResetInputErrorStreams() {
		final String stringOut = appenderOutput.toString();
		if (stringOut != null && !stringOut.isBlank()) {
			System.out.println("-- OUTPUT --\n" + stringOut);
		}
		final String stringErr = appenderError.toString();
		if (stringErr != null && !stringErr.isBlank()) {
			System.out.println("-- ERROR --\n" + stringErr);
		}
		System.out.println("RETURN CODE: " + instance.waitCompletionAndGetReturnCode());
		appenderOutput = new StringWriter();
		appenderError = new StringWriter();
		output = new PrintWriter(appenderOutput);
		error = new PrintWriter(appenderError);
	}

	@Test
	@Order(2)
	public void versionCommandTest() {
		final String[] commandLong = new String[] { "--version" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandLong);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
		final String[] commandShort = new String[] { "-V" };
		instance = BasePicocliRunner.getNewInstance().setOutputWriter(output).setErrorWriter(error)
				.execute(commandShort);
		printAndResetInputErrorStreams();
		assertEquals(0, instance.waitCompletionAndGetReturnCode());
	}

}
