/**
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */
package net.rossonet.waldot.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.utils.gremlin.UpdateTrigger;

public final class GremlinHelper {
	private final static Logger logger = LoggerFactory.getLogger(GremlinHelper.class);

	public static List<UpdateTrigger> elaborateInstance(final WaldotGraph g, final Object analizedObject,
			final String parentName) {
		final List<UpdateTrigger> updateTriggers = new ArrayList<>();
		if (analizedObject instanceof List) {
			logger.info("elaborateInstance - List: " + analizedObject.getClass().getName() + " - " + analizedObject);
			int count = 1;
			for (final Object item : (List<?>) analizedObject) {
				if (item != null) {
					logger.info("Item: " + item.getClass().getName() + " - " + item);
					updateTriggers.addAll(elaborateInstance(g, item, parentName + "/" + count));
					count++;
				}
			}
		} else {
			logger.info("elaborateInstance - Object: " + analizedObject.getClass().getName() + " - " + analizedObject);
			final List<Method> methods = Arrays.stream(analizedObject.getClass().getMethods())
					.filter(m -> Modifier.isPublic(m.getModifiers())).filter(m -> !Modifier.isStatic(m.getModifiers()))
					.collect(Collectors.toList());
			for (final Method method : methods) {
				final String name = method.getName();
				try {
					if (name.startsWith("get") && !name.equals("getClass") && !name.equals("get")
							&& !name.equals("getFirst") && !name.equals("getLast")
							&& (parentName == null || !parentName.endsWith(name)) && !name.equals("getThreadId")) {
						logger.info("Method: " + name + " - Return Type: " + method.getReturnType().getName()
								+ " - Parameters: " + Arrays.toString(method.getParameterTypes()));
						if (method.getParameterCount() > 0) {
							logger.info("Cannot invoke method with parameters");
							continue;
						}
						if (method.getReturnType().equals(void.class)) {
							logger.info("Method returns void, skipping invocation");
							continue;
						}
						final Object result = method.invoke(analizedObject);
						if (method.getReturnType().isPrimitive() || method.getReturnType().isEnum()
								|| method.getReturnType().equals(String.class)) {
							final List<Object> queryData = new ArrayList<>();
							queryData.add("label");
							queryData.add(name.substring(3));
							queryData.add("id");
							queryData.add(parentName + "/" + name.substring(3));
							if (parentName != null) {
								queryData.add("directory");
								queryData.add(parentName);
							}
							queryData.add("value");
							if (result != null) {
								queryData.add(result);
							} else {
								queryData.add("NaN");
							}
							logger.info("** QUERY: " + Arrays.toString(queryData.toArray()));
							final Vertex v = g.addVertex(queryData.toArray());
							final UpdateTrigger currentTrigger = new UpdateTrigger(v, analizedObject, method);
							updateTriggers.add(currentTrigger);
						} else {
							logger.debug("ENTER IN OBJECT " + result.getClass().getName());
							updateTriggers.addAll(elaborateInstance(g, result,
									(parentName != null ? (parentName + "/") : "") + name.substring(3)));
						}
					}
				} catch (final Throwable e) {
					logger.info("Error invoking method " + name + ": " + e.getMessage());
				}
			}
		}
		return updateTriggers;
	}

	private GremlinHelper() {
		throw new UnsupportedOperationException("Just for static usage");
	}
}
