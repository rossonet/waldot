package net.rossonet.waldot.rules;

import net.rossonet.waldot.api.rules.WaldotRulesEngine;

/**
 * WaldotJexlRuleInterface is a class that provides an interface for the Waldot
 * rules
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public class WaldotJexlRuleInterface {
// TODO completare con i metodi chiamabili durante l'esecuzione delle regole
	protected WaldotRulesEngine rulesEngine;

	public WaldotJexlRuleInterface(final WaldotRulesEngine rulesEngine) {
		this.rulesEngine = rulesEngine;
	}

}
