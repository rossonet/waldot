package net.rossonet.oshi;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcFactory;
import net.rossonet.waldot.utils.GremlinHelper;
import oshi.SystemInfo;
import oshi.hardware.Display;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

@TestMethodOrder(OrderAnnotation.class)
public class ObjectToCodeWithReflexionTest {

	@Test
	public void translateObjectToCode() throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, InterruptedException, ExecutionException {
		final WaldotGraph g = OpcFactory.getOpcGraph();
		Thread.sleep(1000);
		final SystemInfo systemInfo = new SystemInfo();
		final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
		GremlinHelper.elaborateInstance(g, operatingSystem, null);
		final HardwareAbstractionLayer hw = systemInfo.getHardware();
		GremlinHelper.elaborateInstance(g, hw, null);
		// Thread.sleep(20 * 60_000);
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