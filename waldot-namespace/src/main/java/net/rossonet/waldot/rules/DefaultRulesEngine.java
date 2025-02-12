package net.rossonet.waldot.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.rules.ExecutorHelper;
import net.rossonet.waldot.api.rules.Rule;
import net.rossonet.waldot.api.rules.WaldotRulesEngine;
import net.rossonet.waldot.api.strategies.ConsoleStrategy;
import net.rossonet.waldot.jexl.JexlExecutorHelper;

public class DefaultRulesEngine implements WaldotRulesEngine, AutoCloseable {
	private final ExecutorHelper jexlEngine = new JexlExecutorHelper();

	private final WaldotJexlRuleInterface jexlInterface;
	private final List<RuleListener> listeners = new ArrayList<>();
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Map<NodeId, Rule> rules = new ConcurrentHashMap<>();
	private final RulesThreadManager rulesThreadManager;
	protected final WaldotNamespace waldotNamespace;

	public DefaultRulesEngine(WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		jexlInterface = new WaldotJexlRuleInterface(this);
		jexlEngine.setFunctionObject(ConsoleStrategy.G_LABEL, waldotNamespace.getGremlinGraph());
		jexlEngine.setFunctionObject(ConsoleStrategy.LOG_LABEL, waldotNamespace.getRulesLogger());
		jexlEngine.setFunctionObject(ConsoleStrategy.COMMANDS_LABEL, waldotNamespace.getCommandsAsFunction());
		jexlEngine.setFunctionObject(ConsoleStrategy.CTX_LABEL, jexlInterface);
		for (final PluginListener p : waldotNamespace.getPlugins()) {
			for (final Entry<String, Rule> f : p.getRuleFunctions().entrySet()) {
				if (f.getKey() != null && f.getValue() != null) {
					if (ConsoleStrategy.G_LABEL.equals(f.getKey()) || ConsoleStrategy.LOG_LABEL.equals(f.getKey())
							|| ConsoleStrategy.COMMANDS_LABEL.equals(f.getKey())
							|| ConsoleStrategy.CTX_LABEL.equals(f.getKey())) {
						logger.error("The plugin " + p.toString() + " tried to override a reserved function name "
								+ f.getKey());
					} else {
						jexlEngine.setFunctionObject(f.getKey(), f.getValue());
					}
				}
			}
		}
		rulesThreadManager = new RulesThreadManager(rules, waldotNamespace.getRulesLogger());
		rulesThreadManager.start();
	}

	@Override
	public void addListener(RuleListener listener) {
		listeners.add(listener);
	}

	@Override
	public void close() throws Exception {
		rulesThreadManager.stop();

	}

	@Override
	public void deregisterRule(NodeId ruleNodeId) {
		rules.remove(ruleNodeId);
	}

	@Override
	public ExecutorHelper getJexlEngine() {
		return jexlEngine;
	}

	@Override
	public Collection<RuleListener> getListeners() {
		return listeners;
	}

	@Override
	public WaldotNamespace getNamespace() {
		return waldotNamespace;
	}

	@Override
	public void registerObserver(WaldotVertex eventVertex, NodeId ruleNodeId) {
		if (rules.containsKey(ruleNodeId)) {
			eventVertex.addAttributeObserver(rules.get(ruleNodeId));
			eventVertex.addPropertyObserver(rules.get(ruleNodeId));
			eventVertex.addEventObserver(rules.get(ruleNodeId));
		} else {
			throw new IllegalArgumentException("Rule not found");

		}
	}

	@Override
	public void registerOrUpdateRule(Rule rule) {
		rules.put(rule.getNodeId(), rule);
	}

	@Override
	public void removeListener(RuleListener listener) {
		listeners.remove(listener);
	}

}
