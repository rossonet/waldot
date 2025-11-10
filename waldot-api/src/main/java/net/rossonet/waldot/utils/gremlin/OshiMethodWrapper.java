package net.rossonet.waldot.utils.gremlin;

import java.lang.reflect.Method;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OshiMethodWrapper {
	private final static Logger logger = LoggerFactory.getLogger(OshiMethodWrapper.class);
	private final Object analizedObject;

	private final Method method;

	private final Vertex vertex;

	public OshiMethodWrapper(Vertex vertex, Object analizedObject, Method method) {
		if (vertex == null) {
			throw new IllegalArgumentException("Vertex cannot be null");
		}
		if (analizedObject == null) {
			throw new IllegalArgumentException("AnalizedObject cannot be null");
		}
		if (method == null) {
			throw new IllegalArgumentException("Method cannot be null");
		}
		this.analizedObject = analizedObject;
		this.method = method;
		this.vertex = vertex;
	}

	public Object getAnalizedObject() {
		return analizedObject;
	}

	public Method getMethod() {
		return method;
	}

	public Vertex getVertex() {
		return vertex;
	}

	public void invoke() {
		try {
			final Object result = method.invoke(analizedObject);
			final Object from = vertex.property("value").value();
			if (!result.equals(from)) {
				vertex.property("value", result);
				logger.debug("vertex " + vertex + " updated from " + from + " to " + result);
			}
		} catch (final Exception e) {
			throw new RuntimeException("Error invoking method: " + method.getName(), e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("OshiMethodWrapper [vertex=");
		builder.append(vertex);
		builder.append(", analizedObject=");
		builder.append(analizedObject);
		builder.append("]");
		return builder.toString();
	}

}
