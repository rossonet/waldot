package net.rossonet.waldot.jexl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Declares which variables a JEXL script finds in each place where WaldOT runs
 * one: the console ({@code query}), the rules, the actions of the IEC 104
 * commands, the Modbus points, ... The namespace and the plugins register their
 * contexts at start-up; {@code jexl-check} and the guide read them from here.
 *
 * <p>
 * The registry describes the contexts, it does not build them: every place keeps
 * creating its own {@code JexlContext}, and must register a descriptor that
 * matches it.
 * </p>
 */
public final class JexlContextRegistry {

	/**
	 * Description of a context.
	 *
	 * @param name            identifier, for example {@code console} or
	 *                        {@code rule}
	 * @param description     where the context is used
	 * @param variables       variables always present
	 * @param consoleCommands true if every console command is also a variable
	 *                        (by its console name)
	 */
	public record Context(String name, String description, List<Variable> variables, boolean consoleCommands) {

		public Context {
			Objects.requireNonNull(name, "name");
			description = description == null ? "" : description;
			variables = List.copyOf(variables);
		}

		/** Names of the variables always present. */
		public List<String> variableNames() {
			final List<String> names = new ArrayList<>();
			for (final Variable v : variables) {
				names.add(v.name());
			}
			return names;
		}
	}

	/**
	 * A variable of a context.
	 *
	 * @param name        name in the script
	 * @param type        Java type or short description of the value
	 * @param description meaning
	 */
	public record Variable(String name, String type, String description) {
		public Variable {
			Objects.requireNonNull(name, "name");
			type = type == null ? "" : type;
			description = description == null ? "" : description;
		}
	}

	/** Console of the server: {@code query}, {@code script}, bootstrap file. */
	public static final String CONSOLE = "console";

	/** Condition and action of a {@code rules:rule} vertex. */
	public static final String RULE = "rule";

	private static final ConcurrentSkipListMap<String, Context> CONTEXTS = new ConcurrentSkipListMap<>();

	/** Common variables, with the names of {@code ConsoleStrategy}. */
	public static Variable cmd() {
		return new Variable("cmd", "JexlCmdFunction",
				"funzioni di utilità e comandi globali: cmd.toNumber(x), cmd.<comando>(...)");
	}

	public static Variable g() {
		return new Variable("g", "GraphTraversalSource", "traversal Gremlin del grafo");
	}

	public static Variable graph() {
		return new Variable("graph", "Graph", "il grafo WaldOT (addVertex, vertices, ...)");
	}

	public static Variable log(final String description) {
		return new Variable("log", "org.slf4j.Logger", description);
	}

	public static Variable math() {
		return new Variable("math", "java.lang.Math", "funzioni matematiche: math.abs(x), math.max(a, b), ...");
	}

	public static Variable rand() {
		return new Variable("rand", "java.util.Random", "numeri casuali: rand.nextDouble(), rand.nextInt(n)");
	}

	public static Variable self(final String description) {
		return new Variable("self", "WaldotVertex", description);
	}

	/** All the registered contexts, sorted by name. */
	public static Collection<Context> all() {
		return List.copyOf(CONTEXTS.values());
	}

	/** The context with the given name, if registered. */
	public static Optional<Context> get(final String name) {
		return name == null ? Optional.empty() : Optional.ofNullable(CONTEXTS.get(name));
	}

	/**
	 * Registers a context, replacing the one with the same name (registration is
	 * idempotent, plugins can be initialised more than once).
	 */
	public static void register(final Context context) {
		Objects.requireNonNull(context, "context");
		CONTEXTS.put(context.name(), context);
	}

	/** Removes a context. */
	public static void unregister(final String name) {
		if (name != null) {
			CONTEXTS.remove(name);
		}
	}

	private JexlContextRegistry() {
	}
}
