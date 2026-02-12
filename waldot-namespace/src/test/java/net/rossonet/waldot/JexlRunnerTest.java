package net.rossonet.waldot;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.rossonet.waldot.jexl.BaseExecutor;
import net.rossonet.waldot.jexl.JexlExecutor;

public class JexlRunnerTest {

	public String prova = "init string";

	private BaseExecutor createExecutor() {

		final List<String> list = new ArrayList<String>();
		list.add("one");
		list.add("two");
		final BaseExecutor executor = new JexlExecutor("test");
		executor.addOrUpdateContext("list", list);
		executor.setFunctionObject("button", this);
		executor.setFunctionObject("system", System.out);
		return executor;
	}

	public String prova() {
		return "prova";
	}

	@Test
	public void simpleQueryTest() throws Exception {

		final BaseExecutor executor = createExecutor();
		System.out.println((boolean) executor.execute("true"));
		System.out.flush();
		final BaseExecutor executor3 = createExecutor();
		System.out.println((boolean) executor3.execute("true"));
		System.out.flush();
		final BaseExecutor executor4 = createExecutor();
		System.out.println(executor4.execute("button"));
		System.out.flush();
		final BaseExecutor executor1 = createExecutor();
		System.out.println(executor1.execute("button.prova()"));
		System.out.flush();
		final BaseExecutor executor2 = createExecutor();
		executor2.execute("LoggerFactory.getLogger('test').error('ciao');");
		System.out.flush();
		// executor.execute("for (item : list) { system:println(item) }");
		System.out.flush();
		executor.close();
	}

}
