package net.rossonet.waldot.api.rules;

import java.util.Collection;
import java.util.concurrent.Callable;

import net.rossonet.waldot.api.EventObserver;
import net.rossonet.waldot.api.RuleListener;
import net.rossonet.waldot.api.models.WaldotVertex;

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
