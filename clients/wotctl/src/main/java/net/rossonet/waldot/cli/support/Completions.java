package net.rossonet.waldot.cli.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;

import net.rossonet.waldot.api.exchange.GraphRenderOptions;

/**
 * Candidates of the shell completion ({@code wotctl completion}) for the
 * options with a fixed set of values that are parsed by the commands (strings
 * or enums written in lower case in the help). They are also shown in the help
 * with {@code ${COMPLETION-CANDIDATES}}.
 */
public final class Completions {

	/** Constants of an enum in lower case (they are parsed ignoring the case). */
	private abstract static class LowerCase<E extends Enum<E>>
			implements
				Iterable<String> {

		private final Class<E> type;

		LowerCase(final Class<E> type) {
			this.type = type;
		}

		@Override
		public Iterator<String> iterator() {
			final List<String> names = new ArrayList<>();
			for (final E constant : type.getEnumConstants()) {
				names.add(constant.name().toLowerCase(Locale.ROOT));
			}
			return names.iterator();
		}
	}

	/** {@code render --layout}. */
	public static final class RenderAlgorithms
			extends
				LowerCase<GraphRenderOptions.Algorithm> {
		public RenderAlgorithms() {
			super(GraphRenderOptions.Algorithm.class);
		}
	}

	/** {@code render --color-by}. */
	public static final class RenderColors
			extends
				LowerCase<GraphRenderOptions.ColorBy> {
		public RenderColors() {
			super(GraphRenderOptions.ColorBy.class);
		}
	}

	/** {@code render --direction}. */
	public static final class RenderDirections
			extends
				LowerCase<GraphRenderOptions.Direction> {
		public RenderDirections() {
			super(GraphRenderOptions.Direction.class);
		}
	}

	/** {@code render --format}. */
	public static final class RenderFormats
			extends
				LowerCase<GraphRenderOptions.Format> {
		public RenderFormats() {
			super(GraphRenderOptions.Format.class);
		}
	}

	/** {@code render --group-by}. */
	public static final class RenderGroups
			extends
				LowerCase<GraphRenderOptions.GroupBy> {
		public RenderGroups() {
			super(GraphRenderOptions.GroupBy.class);
		}
	}

	/** {@code render --layout-direction}. */
	public static final class RenderLayoutDirections
			extends
				LowerCase<GraphRenderOptions.LayoutDirection> {
		public RenderLayoutDirections() {
			super(GraphRenderOptions.LayoutDirection.class);
		}
	}

	/** {@code render --theme}. */
	public static final class RenderThemes
			extends
				LowerCase<GraphRenderOptions.Theme> {
		public RenderThemes() {
			super(GraphRenderOptions.Theme.class);
		}
	}

	/** {@code --security-mode} (without Invalid). */
	public static final class SecurityModes implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			final List<String> names = new ArrayList<>();
			for (final MessageSecurityMode mode : MessageSecurityMode
					.values()) {
				if (mode != MessageSecurityMode.Invalid) {
					names.add(mode.name());
				}
			}
			return names.iterator();
		}
	}

	/** {@code --security-policy}. */
	public static final class SecurityPolicies implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			final List<String> names = new ArrayList<>();
			for (final SecurityPolicy policy : SecurityPolicy.values()) {
				names.add(policy.name());
			}
			return names.iterator();
		}
	}

	/** Types of {@code write --type} and {@code property set --type}. */
	public static final class ValueTypes implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			return Values.TYPES.keySet().iterator();
		}
	}

	private Completions() {
	}
}
