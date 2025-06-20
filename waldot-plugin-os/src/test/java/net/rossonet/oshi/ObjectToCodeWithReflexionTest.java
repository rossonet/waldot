package net.rossonet.oshi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import oshi.SystemInfo;
import oshi.hardware.Display;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixDisplay;
import oshi.software.os.OperatingSystem;

@TestMethodOrder(OrderAnnotation.class)
public class ObjectToCodeWithReflexionTest {

	private void elaborateInstance(final WaldotGraph g, final Object analizedObject, final String parentName)
			throws SecurityException, IllegalAccessException, InvocationTargetException {
		final List<Method> methods = Arrays.stream(analizedObject.getClass().getDeclaredMethods())
				.filter(m -> Modifier.isPublic(m.getModifiers())).filter(m -> !Modifier.isStatic(m.getModifiers()))
				.collect(Collectors.toList());
		for (final Method method : methods) {

			final String name = method.getName();
			try {
				if (name.startsWith("get") && !name.equals("getClass") && !name.equals("get")
						&& (parentName == null || !parentName.endsWith(name)) && !name.equals("getThreadId")) {
					System.out.println("Method: " + name + " - Return Type: " + method.getReturnType().getName()
							+ " - Parameters: " + Arrays.toString(method.getParameterTypes()));
					if (method.getParameterCount() > 0) {
						// System.out.println("Cannot invoke method with parameters");
						continue;
					}
					if (method.getReturnType().equals(void.class)) {
						// System.out.println("Method returns void, skipping invocation");
						continue;
					}
					final Object result = method.invoke(analizedObject);
					if (method.getReturnType().isPrimitive() || method.getReturnType().isEnum()
							|| method.getReturnType().equals(String.class)) {

						final List<Object> queryData = new ArrayList<>();
						queryData.add("label");
						queryData.add(name.substring(3));
						if (parentName != null) {
							queryData.add("directory");
							queryData.add(parentName.substring(3));
						}
						queryData.add("value");
						if (result != null) {
							queryData.add(result);
							System.out.println("** QUERY: " + Arrays.toString(queryData.toArray()));
							g.addVertex(queryData.toArray());

						} else {
							queryData.add("NaN");
							System.out.println("** QUERY: " + Arrays.toString(queryData.toArray()));
							g.addVertex(queryData.toArray());

						}
						System.out.println();
					} else if (method.getReturnType().equals(List.class)) {
						System.out.println("ENTER IN LIST");

						int count = 1;
						for (final Object item : (List<?>) result) {
							if (item.getClass().equals(UnixDisplay.class)) {
								System.out.println("break");
							}
							if (item != null) {
								System.out.println("Item: " + item.getClass().getName() + " - " + item);
								elaborateInstance(g, item,
										(parentName != null ? (parentName + "/") : "") + name + "/" + count);
								count++;
							}
						}
					} else {
						System.out.println("ENTER IN OBJECT " + result.getClass().getName());
						elaborateInstance(g, result, (parentName != null ? (parentName + "/") : "") + name);
					}
				}
			} catch (final Exception e) {
				System.out.println("Error invoking method " + name + ": " + e.getMessage());
			}
		}
	}

	@Test
	public void translateObjectToCode() throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, InterruptedException, ExecutionException {
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(1000);
		final SystemInfo systemInfo = new SystemInfo();
		final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
		elaborateInstance(g, operatingSystem, null);
		final HardwareAbstractionLayer hw = systemInfo.getHardware();
		elaborateInstance(g, hw, null);
		Thread.sleep(120_000);
	}

	@Test
	public void translateObjectToCodeWithReflexion() throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, InterruptedException, ExecutionException {
		final SystemInfo systemInfo = new SystemInfo();
		final List<Display> hw = systemInfo.getHardware().getDisplays();
		for (final Display display : hw) {
			System.out.println("Display: " + display);
		}
	}

}