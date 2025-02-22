package net.rossonet.waldot;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.api.rules.ExecutorHelper;
import net.rossonet.waldot.jexl.JexlExecutorHelper;

public class JexlRunnerTest {

	public String prova = "init string";

	private ExecutorHelper createExecutor() {

		final List<String> list = new ArrayList<String>();
		list.add("one");
		list.add("two");
		final ExecutorHelper executor = new JexlExecutorHelper();
		executor.setContext("list", list);
		executor.setFunctionObject("button", this);
		executor.setFunctionObject("system", System.out);
		return executor;
	}

	public String prova() {
		return "prova";
	}

	@Test
	public void simpleQueryTest() {

		final ExecutorHelper executor = createExecutor();
		System.out.println(executor.execute("'ciao'"));
		System.out.flush();
		final ExecutorHelper executor3 = createExecutor();
		System.out.println(executor3.execute("list"));
		System.out.flush();
		final ExecutorHelper executor4 = createExecutor();
		System.out.println(executor4.execute("button"));
		System.out.flush();
		final ExecutorHelper executor1 = createExecutor();
		System.out.println(executor1.execute("button.prova()"));
		System.out.flush();
		final ExecutorHelper executor2 = createExecutor();
		executor2.execute("LoggerFactory.getLogger('test').error('ciao');");
		System.out.flush();
		// executor.execute("for (item : list) { system:println(item) }");
		System.out.flush();
	}

}
