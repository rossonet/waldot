package net.rossonet.waldot.client.api;

import java.util.List;

import org.eclipse.milo.opcua.stack.core.UaException;

/**
 * Basic operations available in every client version (the text console
 * commands of the server).
 */
public interface WaldotBaseOperations {

	/** Calls {@code add edge} on the edges folder. */
	String[] createEdge(String label, String sourceNodeId, String destinationNodeId, String[] keyValues)
			throws UaException;

	/** Calls {@code add vertex} on the vertices folder. */
	String[] createVertex(String id, String label, String type, String[] keyValues) throws UaException;

	/** Calls the {@code delete} command of a vertex with a numeric id. */
	String[] deleteVertex(int id) throws UaException;

	/** Calls the {@code delete} command of a vertex. */
	String[] deleteVertex(String id) throws UaException;

	/** Calls the {@code about} command: outputs of the command. */
	List<String> getServerInfo() throws UaException;

	/**
	 * Runs a JEXL expression with the {@code query} command.
	 *
	 * @return the outputs of the command: result as text and error
	 */
	List<String> runExpression(String expression) throws UaException;
}
