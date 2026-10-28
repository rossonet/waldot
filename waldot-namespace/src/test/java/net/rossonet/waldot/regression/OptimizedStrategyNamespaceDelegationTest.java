package net.rossonet.waldot.regression;

import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerOptimizedStrategy;

/**
 * {@link NamespaceDelegationRegressionTest} with {@link MiloSingleServerOptimizedStrategy}.
 */
class OptimizedStrategyNamespaceDelegationTest extends NamespaceDelegationRegressionTest {

	@Override
	protected MiloStrategy createMiloStrategy() {
		return new MiloSingleServerOptimizedStrategy();
	}
}
