package net.rossonet.waldot.regression;

import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.gremlin.opcgraph.strategies.opcua.MiloSingleServerBaseStrategy;

/**
 * {@link NamespaceDelegationRegressionTest} with {@link MiloSingleServerBaseStrategy}.
 */
class BaseStrategyNamespaceDelegationTest extends NamespaceDelegationRegressionTest {

	@Override
	protected MiloStrategy createMiloStrategy() {
		return new MiloSingleServerBaseStrategy();
	}
}
