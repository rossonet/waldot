package net.rossonet.waldot.gremlin.opcgraph.structure;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;

class OpcFactoryStrategySelectionTest {

	@AfterEach
	void clearProperty() {
		System.clearProperty(OpcFactory.MILO_STRATEGY_PROPERTY);
	}

	@Test
	void defaultsToOptimizedStrategy() {
		System.clearProperty(OpcFactory.MILO_STRATEGY_PROPERTY);
		assertInstanceOf(MiloSingleServerOptimizedStrategy.class,
				OpcFactory.createMiloStrategy());
	}

	@Test
	void selectsBaseWhenExplicitlyRequested() {
		System.setProperty(OpcFactory.MILO_STRATEGY_PROPERTY, "base");
		assertInstanceOf(MiloSingleServerBaseStrategy.class,
				OpcFactory.createMiloStrategy());
	}
}
