/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package net.rossonet.waldot.rules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class encapsulates a set of facts and represents a facts namespace.
 * Facts have unique names within a <code>Facts</code> object.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class Facts implements Iterable<Fact<?>> {

	private final Set<Fact<?>> facts = new HashSet<>();

	private boolean traceEnable;

	/**
	 * Add a fact, replacing any fact with the same name.
	 * 
	 * @param fact to add, must not be null
	 */
	public <T> void add(final Fact<T> fact) {
		Objects.requireNonNull(fact, "fact must not be null");
		final Fact<?> retrievedFact = getFact(fact.getName());
		if (retrievedFact != null) {
			remove(retrievedFact);
		}
		facts.add(fact);
	}

	/**
	 * Return a copy of the facts as a map. It is not intended to manipulate facts
	 * outside of the rules engine (aka other than manipulating them through rules).
	 *
	 * @return a copy of the current facts as a {@link HashMap}
	 */
	public Map<String, Object> asMap() {
		final Map<String, Object> map = new HashMap<>();
		for (final Fact<?> fact : facts) {
			map.put(fact.getName(), fact.getValue());
		}
		return map;
	}

	/**
	 * Clear facts.
	 */
	public void clear() {
		facts.clear();
	}

	/**
	 * Get the value of a fact by its name. This is a convenience method provided as
	 * a short version of {@code getFact(factName).getValue()}.
	 *
	 * @param factName name of the fact, must not be null
	 * @param <T>      type of the fact's value
	 * @return the value of the fact having the given name, or null if there is no
	 *         fact with the given name
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(final String factName) {
		Objects.requireNonNull(factName, "fact name must not be null");
		final Fact<?> fact = getFact(factName);
		if (fact != null) {
			return (T) fact.getValue();
		}
		return null;
	}

	/**
	 * Get a fact by name.
	 *
	 * @param factName name of the fact, must not be null
	 * @return the fact having the given name, or null if there is no fact with the
	 *         given name
	 */
	public Fact<?> getFact(final String factName) {
		Objects.requireNonNull(factName, "fact name must not be null");
		return facts.stream().filter(fact -> fact.getName().equals(factName)).findFirst().orElse(null);
	}

	public boolean isTraceEnable() {
		return traceEnable;
	}

	/**
	 * Return an iterator on the set of facts. It is not intended to remove facts
	 * using this iterator outside of the rules engine (aka other than doing it
	 * through rules)
	 * 
	 * @return an iterator on the set of facts
	 */
	@Override
	public Iterator<Fact<?>> iterator() {
		return facts.iterator();
	}

	/**
	 * Add a fact, replacing any fact with the same name.
	 *
	 * @param name  of the fact to add, must not be null
	 * @param value of the fact to add, must not be null
	 */
	public <T> void put(final String name, final T value) {
		Objects.requireNonNull(name, "fact name must not be null");
		Objects.requireNonNull(value, "fact value must not be null");
		final Fact<?> retrievedFact = getFact(name);
		if (retrievedFact != null) {
			remove(retrievedFact);
		}
		add(new Fact<>(name, value));
	}

	/**
	 * Remove a fact.
	 *
	 * @param fact to remove, must not be null
	 */
	public <T> void remove(final Fact<T> fact) {
		Objects.requireNonNull(fact, "fact must not be null");
		facts.remove(fact);
	}

	/**
	 * Remove a fact by name.
	 *
	 * @param factName name of the fact to remove, must not be null
	 */
	public void remove(final String factName) {
		Objects.requireNonNull(factName, "fact name must not be null");
		final Fact<?> fact = getFact(factName);
		if (fact != null) {
			remove(fact);
		}
	}

	public void setTraceEnable(final boolean traceEnable) {
		this.traceEnable = traceEnable;
	}

	@Override
	public String toString() {
		final Iterator<Fact<?>> iterator = facts.iterator();
		final StringBuilder stringBuilder = new StringBuilder("[");
		while (iterator.hasNext()) {
			stringBuilder.append(iterator.next().toString());
			if (iterator.hasNext()) {
				stringBuilder.append(",");
			}
		}
		stringBuilder.append("]");
		return stringBuilder.toString();
	}
}
