package net.rossonet.waldot.api.rules;

import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.commons.jexl3.JexlContext;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotVertex;

/**
 * Rule interface represents a rule in the Waldot system. It extends
 * WaldotVertex and provides methods to manage rule execution, conditions,
 * actions, and listeners.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface Rule extends WaldotVertex, AutoCloseable, EventObserver {

	void clear();

	String getAction();

	String getCondition();

	long getDefaultValidDelayMs();

	long getDefaultValidUntilMs();

	int getDelayBeforeEvaluation();

	int getDelayBeforeExecute();

	int getExecutionTimeout();

	Collection<CachedRuleRecord> getFacts();

	JexlContext getJexlContext(ClonableMapContext baseJexlContext);

	Collection<RuleListener> getListeners();

	Callable<WaldotStepLogger> getNewRunner();

	int getPriority();

	int getRefractoryPeriodMs();

	public int getRunners();

	String getThreadName();

	public boolean isClearFactsAfterExecution();

	public boolean isDirty();

	public boolean isParallelExecution();

	void setClearFactsAfterExecution(boolean clearFactsAfterExecution);

	void setDelayBeforeEvaluation(int delayBeforeEvaluation);

	void setDelayBeforeExecute(int delayBeforeExecute);

	void setParallelExecution(boolean parallelExecution);

	void setRefractoryPeriodMs(int refractoryPeriodMs);

}
