package net.rossonet.waldot.gremlin.opcgraph.strategies.opcua;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.tinkerpop.gremlin.process.computer.GraphFilter;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Graph.Variables;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.model.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Longs;

import net.rossonet.waldot.api.PluginListener;
import net.rossonet.waldot.api.annotation.WaldotMiloStrategy;
import net.rossonet.waldot.api.models.WaldotCommand;
import net.rossonet.waldot.api.models.WaldotEdge;
import net.rossonet.waldot.api.models.WaldotGraph;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.models.WaldotProperty;
import net.rossonet.waldot.api.models.WaldotVertex;
import net.rossonet.waldot.api.models.WaldotVertexProperty;
import net.rossonet.waldot.api.models.base.GremlinElement;
import net.rossonet.waldot.api.strategies.MiloStrategy;
import net.rossonet.waldot.exception.PluginBlockingException;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputerView;
import net.rossonet.waldot.gremlin.opcgraph.structure.OpcGraphVariables;
import net.rossonet.waldot.gremlin.opcgraph.structure.edge.FireMonitoredEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.edge.LinkMonitoredEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.edge.LinkMonitoredEdge.LinkDirection;
import net.rossonet.waldot.gremlin.opcgraph.structure.edge.OpcEdge;
import net.rossonet.waldot.gremlin.opcgraph.structure.vertex.OpcProperty;
import net.rossonet.waldot.gremlin.opcgraph.structure.vertex.OpcVertex;
import net.rossonet.waldot.gremlin.opcgraph.structure.vertex.OpcVertexProperty;
import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;
import net.rossonet.waldot.opc.AbstractOpcProperty;
import net.rossonet.waldot.opc.AbstractOpcVertex;
import net.rossonet.waldot.opc.MiloSingleServerBaseReferenceNodeBuilder;

/**
 * Optimized and hardened variant of {@link MiloSingleServerBaseStrategy}.
 * <p>
 * It keeps the same OPC UA address space layout of the base strategy (same
 * folders, same property nodes, same references, same commands) but it adds
 * a set of in-memory indexes so that the hot paths of the Gremlin API do not
 * need to walk the OPC UA address space, and it makes the creation / removal
 * of vertices, edges and properties atomic and complete.
 *
 * <h2>Maintenance notes</h2>
 *
 * <h3>In-memory indexes</h3>
 * <ul>
 * <li>{@link #cachedVertices} / {@link #cachedEdges}: published elements, the
 * only ones visible to {@code getVertices()} / {@code getEdges()}. An element
 * is published as the very last step of its creation and unpublished as the
 * first step of its removal.</li>
 * <li>{@link #edgeIndex}: nodeId of an edge &rarr; {@link EdgeEntry}
 * (immutable snapshot with source, target, label and type). It is filled
 * <b>before</b> the monitors and the plugins are notified because they call
 * {@code inVertex()} / {@code outVertex()} on the half built edge. Labels
 * cannot change (an attempt is reverted by {@code OpcEdge}) so the snapshot
 * never goes stale.</li>
 * <li>{@link #outEdges} / {@link #inEdges}: adjacency per vertex, ordered by
 * the creation sequence of the edge. They contain only published edges.</li>
 * <li>{@link #vertexProperties} / {@link #edgeProperties}: one
 * {@link PropertyBag} per element, key &rarr; property node. The bag is the
 * serialization point that makes "create or update" atomic.</li>
 * </ul>
 *
 * <h3>Directories</h3> Elements (vertices, edges) and commands can be placed
 * in a tree of folders. Folder creation, element linking and the pruning of
 * empty folders are serialized by {@link #directoryLock}; every folder keeps
 * a counter of its children in {@link DirectoryNode}. Empty path components
 * ({@code a//b}, {@code /x}) are ignored.
 *
 * <h3>Atomicity</h3> {@code addVertex} and {@code addEdge} roll back every
 * node, reference and index entry they created if anything fails (not only
 * {@link PluginBlockingException}). Removal of a vertex cascades to the
 * incident edges, the property nodes, the {@code .delete} / {@code .property}
 * commands and the folder links.
 *
 * <h3>Locking order</h3> {@link #directoryLock} and the monitor of a
 * {@link PropertyBag} are never held while calling plugins or observers, so
 * they cannot be part of a lock cycle with user code.
 */
@WaldotMiloStrategy
public class MiloSingleServerOptimizedStrategy implements MiloStrategy {

	/**
	 * Folder in which an element is linked. {@code directory} is {@code null} when
	 * the element is linked directly under a root folder.
	 */
	private record DirectoryLink(UaFolderNode folder, DirectoryNode directory,
			boolean component) {
	}

	/** A dynamic folder created for the {@code directory} parameter. */
	private static final class DirectoryNode {
		private int children;
		private final UaFolderNode folder;
		private final String key;
		private final DirectoryNode parentDirectory;
		private final UaFolderNode parentFolder;
		private final Map<String, UaFolderNode> registry;

		private DirectoryNode(final String key, final UaFolderNode folder,
				final UaFolderNode parentFolder,
				final DirectoryNode parentDirectory,
				final Map<String, UaFolderNode> registry) {
			this.key = key;
			this.folder = folder;
			this.parentFolder = parentFolder;
			this.parentDirectory = parentDirectory;
			this.registry = registry;
		}
	}

	/**
	 * Immutable snapshot of an edge: everything needed to navigate the graph
	 * without reading OPC UA properties.
	 */
	private record EdgeEntry(long sequence, WaldotEdge edge, NodeId nodeId,
			NodeId source, NodeId target, String label, String type) {
	}

	/**
	 * Insertion ordered, thread safe map key &rarr; property node. The factory
	 * of {@link #getOrCreate} runs inside the monitor, so two threads can never
	 * create the same property node.
	 */
	private static final class PropertyBag<P> {
		private final Map<String, P> items = new LinkedHashMap<>();

		synchronized List<P> drain() {
			final List<P> result = new ArrayList<>(items.values());
			items.clear();
			return result;
		}

		synchronized P get(final String key) {
			return items.get(key);
		}

		synchronized P getOrCreate(final String key,
				final Supplier<P> factory) {
			P value = items.get(key);
			if (value == null) {
				value = factory.get();
				items.put(key, value);
			}
			return value;
		}

		synchronized void removeIf(
				final java.util.function.Predicate<P> predicate) {
			items.values().removeIf(predicate);
		}

		synchronized Map<String, P> snapshot() {
			return new LinkedHashMap<>(items);
		}
	}

	/** Qualified properties used on every call, built once. */
	private record SharedProperties(QualifiedProperty<String> labelEdge,
			QualifiedProperty<String> typeEdge,
			QualifiedProperty<NodeId> source, QualifiedProperty<NodeId> target,
			QualifiedProperty<NodeId> referenceType,
			QualifiedProperty<Boolean> forward,
			QualifiedProperty<String> labelVertex,
			QualifiedProperty<String> typeVertex) {
	}

	private static final String COMMAND_DIRECTORY_NODEID_PREFIX = "c";
	private static final String DELETE_COMMAND_SUFFIX = ".delete";
	private static final String EDGE_DIRECTORY_NODEID_PREFIX = "e";
	private static final String PROPERTY_COMMAND_SUFFIX = ".property";
	private static final String VERTEX_DIRECTORY_NODEID_PREFIX = "v";

	private UaFolderNode assetRootNode;
	private transient final ConcurrentHashMap<NodeId, WaldotEdge> cachedEdges = new ConcurrentHashMap<>();
	private volatile MiloSingleServerBaseReferenceNodeBuilder.ReferenceNodes cachedReferenceNodes;
	private transient final ConcurrentHashMap<NodeId, WaldotVertex> cachedVertices = new ConcurrentHashMap<>();
	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final Map<NodeId, DirectoryLink> directoryLinks = new ConcurrentHashMap<>();
	private final Object directoryLock = new Object();
	private final Map<NodeId, DirectoryNode> directoryNodes = new ConcurrentHashMap<>();
	private transient final ConcurrentHashMap<String, UaFolderNode> edgeDirectories = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<NodeId, EdgeEntry> edgeIndex = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<NodeId, PropertyBag<OpcProperty<?>>> edgeProperties = new ConcurrentHashMap<>();
	private final AtomicLong edgeSequence = new AtomicLong(0);
	private final AtomicLong edgeSuffix = new AtomicLong(0);
	private final MiloSingleServerBaseFolderManager folderManager = new MiloSingleServerBaseFolderManager(
			this);
	private final ConcurrentHashMap<NodeId, ConcurrentSkipListMap<Long, EdgeEntry>> inEdges = new ConcurrentHashMap<>();
	private UaFolderNode interfaceRootNode;
	private final AtomicLong lastEventId = new AtomicLong(0);
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private Graph.Variables opcGraphVariables;
	private final ConcurrentHashMap<NodeId, ConcurrentSkipListMap<Long, EdgeEntry>> outEdges = new ConcurrentHashMap<>();
	private final Object parametersLock = new Object();
	private final Set<NodeId> pendingEdgeIds = ConcurrentHashMap.newKeySet();
	private final Set<NodeId> pendingVertexIds = ConcurrentHashMap.newKeySet();
	private transient final ConcurrentHashMap<String, NodeId> referenceCache = new ConcurrentHashMap<>();
	private UaFolderNode rootNode;
	private volatile SharedProperties sharedProperties;
	private final ConcurrentHashMap<NodeId, PropertyBag<OpcVertexProperty<?>>> vertexProperties = new ConcurrentHashMap<>();
	private WaldotNamespace waldotNamespace;

	/**
	 * Traversal starting from the element of a per-element command. The NodeId
	 * is used as it is: its identifier alone does not resolve generated
	 * numeric ids, and an edge must start from E().
	 */
	private GraphTraversal<?, ? extends Element> elementTraversal(
			final GremlinElement element) {
		final GraphTraversalSource g = waldotNamespace.getGremlinGraph()
				.traversal();
		return element instanceof Edge
				? g.E(element.getNodeId())
				: g.V(element.getNodeId());
	}

	private void addDeletedCommand(GremlinElement vertex) {
		final AbstractOpcCommand deleteCommand = new AbstractOpcCommand(
				waldotNamespace.getGremlinGraph(), waldotNamespace,
				vertex.id().getIdentifier().toString() + DELETE_COMMAND_SUFFIX,
				"delete", "Delete " + vertex.getBrowseName().getName(), null,
				waldotNamespace.getConfiguration().getWaldotCommandWriteMask(),
				waldotNamespace.getConfiguration()
						.getWaldotCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandExecutable(),
				waldotNamespace.getConfiguration()
						.getWaldotCommandUserExecutable()) {

			@Override
			public Object[] runCommand(InvocationContext invocationContext,
					String[] inputValues) {
				String output = "";
				String error = "";
				try {
					output = elementTraversal(vertex).drop()
							.iterate() + " element deleted";
				} catch (final Exception e) {
					logger.error("Error deleting element {}: {}",
							vertex.getNodeId().getIdentifier(), e.getMessage());
					error = e.getMessage();
				}
				return new String[]{output, error};
			}
		};
		deleteCommand.addOutputArgument("output",
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		deleteCommand.addOutputArgument("error",
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		waldotNamespace.getStorageManager().addNode(deleteCommand);
		vertex.addComponent((UaNode) deleteCommand);
	}

	@Override
	public Edge addEdge(final WaldotVertex sourceVertex,
			final WaldotVertex targetVertex, final String label,
			final Object[] keyValues) {
		Objects.requireNonNull(sourceVertex, "sourceVertex");
		Objects.requireNonNull(targetVertex, "targetVertex");
		final Object[] propertyKeyValues = keyValues == null
				? new Object[0]
				: keyValues;
		ElementHelper.legalPropertyKeyValueArray(propertyKeyValues);
		String type = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				TYPE_FIELD.toLowerCase());
		String elaboratedLabel = label;
		if (type == null || type.isEmpty()) {
			if (elaboratedLabel != null && !elaboratedLabel.isEmpty()) {
				type = elaboratedLabel;
			} else {
				type = DEFAULT_EDGE_TYPE;
			}
		}
		if (elaboratedLabel == null || elaboratedLabel.isEmpty()) {
			elaboratedLabel = type;
			logger.debug(
					"Edge label not found in propertyKeyValues, using default label '{}'",
					elaboratedLabel);
		}
		String name = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				NAME_FIELD.toLowerCase());
		if (name == null) {
			name = elaboratedLabel;
			logger.debug(NAME_FIELD.toLowerCase()
					+ " not found in propertyKeyValues, using default name '{}'",
					name);
		}
		String description = MiloStrategy.getKeyValuesProperty(
				propertyKeyValues, DESCRIPTION_PARAMETER.toLowerCase());
		if (description == null || description.isEmpty()) {
			description = sourceVertex.getDisplayName().getText() + " -- "
					+ elaboratedLabel + " -> "
					+ targetVertex.getDisplayName().getText();
			logger.debug(DESCRIPTION_PARAMETER
					+ " not found in propertyKeyValues, using default '{}'",
					description);
		}
		final NodeId nodeId = claimEdgeNodeId(
				MiloStrategy.getIdValue(propertyKeyValues).orElse(null),
				sourceVertex, targetVertex, elaboratedLabel);
		try {
			final OpcEdge edge = new OpcEdge(waldotNamespace.getGremlinGraph(),
					nodeId, sourceVertex, targetVertex, elaboratedLabel, name,
					description,
					MiloSingleServerBaseReferenceNodeBuilder
							.getWriteMask(propertyKeyValues),
					MiloSingleServerBaseReferenceNodeBuilder
							.getUserWriteMask(propertyKeyValues),
					MiloSingleServerBaseReferenceNodeBuilder
							.getEventNotifier(propertyKeyValues),
					MiloSingleServerBaseReferenceNodeBuilder
							.getVersion(propertyKeyValues));
			// the index must exist before the monitors and the plugins run:
			// they call inVertex() / outVertex() on the half built edge
			final EdgeEntry entry = new EdgeEntry(
					edgeSequence.incrementAndGet(), edge, nodeId,
					sourceVertex.getNodeId(), targetVertex.getNodeId(),
					elaboratedLabel, type);
			edgeIndex.put(nodeId, entry);
			edgeProperties.put(nodeId, new PropertyBag<>());
			waldotNamespace.getStorageManager().addNode(edge);
			linkToDirectory(
					MiloStrategy.getKeyValuesProperty(propertyKeyValues,
							DIRECTORY_PARAMETER.toLowerCase()),
					edge, folderManager.getEdgesFolderNode(), edgeDirectories,
					EDGE_DIRECTORY_NODEID_PREFIX, false);
			edge.addReference(new Reference(edge.getNodeId(),
					NodeIds.HasTypeDefinition,
					referenceNodes().edgeTypeNode.getNodeId().expanded(),
					true));
			final SharedProperties qp = shared();
			edge.setProperty(qp.labelEdge(), elaboratedLabel);
			edge.setProperty(qp.typeEdge(), type);
			edge.setProperty(qp.source(), sourceVertex.getNodeId());
			edge.setProperty(qp.target(), targetVertex.getNodeId());
			edge.addReference(new Reference(edge.getNodeId(),
					referenceNodes().hasSourceNodeReferenceType,
					sourceVertex.getNodeId().expanded(), true));
			edge.addReference(new Reference(edge.getNodeId(),
					referenceNodes().hasTargetNodeReferenceType,
					targetVertex.getNodeId().expanded(), true));
			final Reference sourceReference = new Reference(
					sourceVertex.getNodeId(),
					referenceNodes().hasReferenceDescriptionReferenceType,
					edge.getNodeId().expanded(), true);
			edge.addRelatedReference(sourceReference);
			sourceVertex.addReference(sourceReference);
			final Reference targetReference = new Reference(
					targetVertex.getNodeId(),
					referenceNodes().hasReferenceDescriptionReferenceType,
					edge.getNodeId().expanded(), true);
			edge.addRelatedReference(targetReference);
			targetVertex.addReference(targetReference);
			// create the main reference type if it does not exist
			final NodeId mainReferenceTypeNodeId = getOrCreateReferenceType(
					type);
			edge.setProperty(qp.referenceType(), mainReferenceTypeNodeId);
			edge.setProperty(qp.forward(), true);
			final Reference reference = new Reference(sourceVertex.getNodeId(),
					mainReferenceTypeNodeId,
					targetVertex.getNodeId().expanded(), true);
			edge.addRelatedReference(reference);
			sourceVertex.addReference(reference);
			popolateEdgePropertiesFromPropertyKeyValues(propertyKeyValues,
					edge);
			if (type.equals(LINK_TO_EDGE_TYPE)) {
				edge.setMonitor(new LinkMonitoredEdge(waldotNamespace,
						LinkDirection.TO, edge, sourceVertex, targetVertex));
			}
			if (type.equals(LINK_FROM_EDGE_TYPE)) {
				edge.setMonitor(new LinkMonitoredEdge(waldotNamespace,
						LinkDirection.FROM, edge, sourceVertex, targetVertex));
			}
			if (type.equals(FIRE_EDGE_TYPE)) {
				edge.setMonitor(new FireMonitoredEdge(waldotNamespace, edge,
						sourceVertex, targetVertex));
			}
			for (final PluginListener p : waldotNamespace.getPlugins()) {
				if (p.containsEdgeType(type)) {
					try {
						p.notifyAddEdge(edge, sourceVertex, targetVertex,
								elaboratedLabel, type, propertyKeyValues);
					} catch (final PluginBlockingException e) {
						logger.error("Plugin {} blocked the edge addition: {}",
								p.getClass().getName(), e.getMessage());
						throw new RuntimeException(
								"Plugin " + p.getClass().getName()
										+ " blocked the edge addition: "
										+ e.getMessage(),
								e);
					}
				}
			}
			addDeletedCommand(edge);
			addPropertyCommand(edge);
			publishEdge(entry);
			return edge;
		} catch (final RuntimeException | Error e) {
			// complete rollback: nodes, references, properties, monitor, folder
			// link and indexes created so far
			try {
				removeEdgeInternal(nodeId, false);
			} catch (final Throwable rollbackError) {
				logger.error("Rollback of edge {} failed", nodeId,
						rollbackError);
			}
			throw e;
		}
	}

	private void addPropertyCommand(GremlinElement vertex) {
		final AbstractOpcCommand propertyCommand = new AbstractOpcCommand(
				waldotNamespace.getGremlinGraph(), waldotNamespace,
				// TODO: usare delle costanti per tutte le variabili stringa che
				// non sono messaggi di log
				vertex.id().getIdentifier().toString()
						+ PROPERTY_COMMAND_SUFFIX,
				"property",
				"Add or update vertex property"
						+ vertex.getBrowseName().getName(),
				null,
				waldotNamespace.getConfiguration().getWaldotCommandWriteMask(),
				waldotNamespace.getConfiguration()
						.getWaldotCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getWaldotCommandExecutable(),
				waldotNamespace.getConfiguration()
						.getWaldotCommandUserExecutable()) {

			@Override
			public Object[] runCommand(InvocationContext invocationContext,
					String[] inputValues) {
				String output = "";
				String error = "";
				final String label = inputValues[0];
				final String value = inputValues[1];
				try {
					output = elementTraversal(vertex)
							.property(label, value).iterate() + " property '"
							+ label + "' updated with value '" + value + "'";
				} catch (final Exception e) {
					logger.error(
							"Error updating property '{}' of element {}: {}",
							label, vertex.getNodeId().getIdentifier(),
							e.getMessage());
					error = e.getMessage();
				}
				return new String[]{output, error};
			}
		};
		propertyCommand.addOutputArgument("output",
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command output"));
		propertyCommand.addOutputArgument("error",
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("command error"));
		propertyCommand.addInputArgument("propertyLabel",
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("property to add or update"));
		propertyCommand.addInputArgument("propertyValue",
				VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english("value of the property"));
		waldotNamespace.getStorageManager().addNode(propertyCommand);
		vertex.addComponent((UaNode) propertyCommand);
	}

	@Override
	public AbstractOpcVertex addVertex(final NodeId requestedNodeId,
			final Object[] keyValues) {
		final Object[] propertyKeyValues = keyValues == null
				? new Object[0]
				: keyValues;
		ElementHelper.legalPropertyKeyValueArray(propertyKeyValues);
		final NodeId nodeId = requestedNodeId != null
				? requestedNodeId
				: waldotNamespace.getNodeIdManager()
						.getNextId(waldotNamespace.getGremlinGraph());
		// atomic reservation: two concurrent threads cannot pass the check
		// with the same nodeId (TOCTOU fix)
		if (!pendingVertexIds.add(nodeId)) {
			throw Graph.Exceptions.vertexWithIdAlreadyExists(nodeId);
		}
		try {
			if (cachedVertices.containsKey(nodeId)
					|| waldotNamespace.hasNodeId(nodeId)) {
				// checked before touching anything: the rollback below must never
				// delete the nodes of an element that already exists
				throw Graph.Exceptions.vertexWithIdAlreadyExists(nodeId);
			}
			String label = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
					LABEL_FIELD.toLowerCase());
			if (label == null) {
				label = DEFAULT_VERTEX_LABEL;
				logger.debug(LABEL_FIELD.toLowerCase()
						+ " not found in propertyKeyValues, using default label '{}'",
						label);
			}
			String name = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
					NAME_FIELD.toLowerCase());
			if (name == null) {
				name = label;
				logger.debug(NAME_FIELD.toLowerCase()
						+ " not found in propertyKeyValues, using default name '{}'",
						name);
			}
			final QualifiedName browseName = waldotNamespace
					.generateQualifiedName(name);
			final LocalizedText displayName = new LocalizedText(name);
			String description = MiloStrategy.getKeyValuesProperty(
					propertyKeyValues, DESCRIPTION_PARAMETER.toLowerCase());
			if (description == null) {
				description = label;
				logger.debug(DESCRIPTION_PARAMETER
						+ " not found in propertyKeyValues, using default '{}'",
						description);
			}
			try {
				NodeId typeDefinition = getVertexTypeNode(propertyKeyValues);
				if (typeDefinition == null) {
					typeDefinition = referenceNodes().vertexTypeNode.getNodeId();
					logger.debug(TYPE_FIELD.toLowerCase()
							+ " not found in propertyKeyValues, using default type '{}'",
							typeDefinition);
				}
				final AbstractOpcVertex vertex = createVertex(nodeId,
						typeDefinition, label, description, browseName, displayName,
						propertyKeyValues,
						MiloSingleServerBaseReferenceNodeBuilder
								.getWriteMask(propertyKeyValues),
						MiloSingleServerBaseReferenceNodeBuilder
								.getUserWriteMask(propertyKeyValues),
						MiloSingleServerBaseReferenceNodeBuilder
								.getEventNotifier(propertyKeyValues),
						MiloSingleServerBaseReferenceNodeBuilder
								.getVersion(propertyKeyValues));
				addDeletedCommand(vertex);
				addPropertyCommand(vertex);
				cachedVertices.put(vertex.getNodeId(), vertex);
				return vertex;
			} catch (final RuntimeException | Error e) {
				// an invalid property must not leave a ghost node in the address
				// space
				try {
					discardVertexNodes(nodeId);
				} catch (final Throwable rollbackError) {
					logger.error("Rollback of vertex {} failed", nodeId,
							rollbackError);
				}
				throw e;
			}
		} finally {
			pendingVertexIds.remove(nodeId);
		}
	}

	/**
	 * Releases an edge id claimed with {@link #claimEdgeNodeId} when it is
	 * not available.
	 */
	private boolean claimEdgeId(final NodeId id) {
		if (!pendingEdgeIds.add(id)) {
			return false;
		}
		if (edgeIndex.containsKey(id) || waldotNamespace.hasNodeId(id)) {
			pendingEdgeIds.remove(id);
			return false;
		}
		return true;
	}

	/**
	 * Picks (and reserves) the nodeId of a new edge. An explicit id that is
	 * already in use is an error. Without an explicit id the nodeId is
	 * {@code source:label:target}; a second edge with the same label between
	 * the same vertices gets a unique numeric suffix instead of silently
	 * overwriting the first one.
	 */
	private NodeId claimEdgeNodeId(final Object readId,
			final WaldotVertex sourceVertex, final WaldotVertex targetVertex,
			final String elaboratedLabel) {
		if (readId != null) {
			final NodeId explicit = MiloStrategy.getNodeIdManager()
					.convert(waldotNamespace.getGremlinGraph(), readId);
			if (explicit != null) {
				if (!claimEdgeId(explicit)) {
					throw Graph.Exceptions.edgeWithIdAlreadyExists(explicit);
				}
				return explicit;
			}
		}
		final String base = sourceVertex.getNodeId().getIdentifier() + ":"
				+ elaboratedLabel + ":"
				+ targetVertex.getNodeId().getIdentifier();
		NodeId candidate = waldotNamespace.generateNodeId(base);
		while (!claimEdgeId(candidate)) {
			candidate = waldotNamespace
					.generateNodeId(base + "#" + edgeSuffix.incrementAndGet());
		}
		return candidate;
	}

	@Override
	public void close() throws Exception {
		if (!closed.compareAndSet(false, true)) {
			return;
		}
		// the graph is not emptied on close (resetNameSpace() removes every
		// element and is meant for cmd.reset()): only the indexes are released
		// stop the monitors of the edges
		for (final WaldotEdge edge : new ArrayList<>(cachedEdges.values())) {
			try {
				edge.removeRelatedOpcUaNodes();
			} catch (final Throwable e) {
				logger.debug("Error stopping the monitor of edge {}",
						edge.getNodeId(), e);
			}
		}
		referenceCache.clear();
		cachedVertices.clear();
		cachedEdges.clear();
		edgeIndex.clear();
		outEdges.clear();
		inEdges.clear();
		vertexProperties.clear();
		edgeProperties.clear();
		pendingEdgeIds.clear();
		pendingVertexIds.clear();
		synchronized (directoryLock) {
			directoryLinks.clear();
			directoryNodes.clear();
			edgeDirectories.clear();
			folderManager.getVertexDirectories().clear();
			folderManager.getCommandDirectories().clear();
		}
	}

	private void collectEdges(
			final ConcurrentSkipListMap<Long, EdgeEntry> entries,
			final String[] edgeLabels, final List<WaldotEdge> result) {
		if (entries == null) {
			return;
		}
		for (final EdgeEntry entry : entries.values()) {
			if (labelMatches(entry, edgeLabels)) {
				result.add(entry.edge());
			}
		}
	}

	private void collectVertices(
			final ConcurrentSkipListMap<Long, EdgeEntry> entries,
			final String[] edgeLabels, final List<WaldotVertex> result,
			final boolean outgoing) {
		if (entries == null) {
			return;
		}
		for (final EdgeEntry entry : entries.values()) {
			if (labelMatches(entry, edgeLabels)) {
				// tolerant lookup: a vertex that disappeared is skipped
				final WaldotVertex vertex = vertexById(
						outgoing ? entry.target() : entry.source());
				if (vertex != null) {
					result.add(vertex);
				}
			}
		}
	}

	private NodeId commandNodeId(final NodeId elementId, final String suffix) {
		return waldotNamespace
				.generateNodeId(elementId.getIdentifier().toString() + suffix);
	}

	@Override
	public OpcGraphComputerView createGraphComputerView(final WaldotGraph graph,
			final GraphFilter graphFilter,
			final Set<VertexComputeKey> vertexComputeKeys) {
		logger.info(
				"createGraphComputerView: graph={}, graphFilter={}, VertexComputeKey={}",
				graph, graphFilter, vertexComputeKeys);
		// GraphComputer (OLAP) is not supported: see
		// WaldotNamespace#createGraphComputerView
		throw new UnsupportedOperationException(
				"GraphComputer (OLAP) is not supported by WaldOT");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <DATA_TYPE> OpcProperty<DATA_TYPE> createOrUpdateWaldotEdgeProperty(
			final WaldotEdge opcEdge, final String key, final DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace
				.generateNodeId(opcEdge.getNodeId().getIdentifier().toString()
						+ PROPERTY_SPLIT_SIMBOL_IN_NODEID + key);
		final PropertyBag<OpcProperty<?>> bag = edgeProperties
				.computeIfAbsent(opcEdge.getNodeId(), k -> new PropertyBag<>());
		final AtomicBoolean created = new AtomicBoolean(false);
		final OpcProperty<DATA_TYPE> property = (OpcProperty<DATA_TYPE>) bag
				.getOrCreate(key, () -> {
					final Optional<UaNode> existing = waldotNamespace
							.getStorageManager().getNode(nodeId);
					if (existing.isPresent()
							&& existing.get() instanceof OpcProperty) {
						return (OpcProperty<?>) existing.get();
					}
					final OpcProperty<DATA_TYPE> newProperty = new OpcProperty(
							waldotNamespace.getGremlinGraph(), opcEdge, key,
							value, opcEdge.getNodeContext(), nodeId,
							LocalizedText.english(key + " of edge property "
									+ opcEdge.getBrowseName().getName()),
							MiloSingleServerBaseReferenceNodeBuilder.edgeVariableWriteMask,
							MiloSingleServerBaseReferenceNodeBuilder.edgeVariableUserWriteMask,
							NodeIds.BaseDataType, ValueRanks.Scalar, null,
							MiloSingleServerBaseReferenceNodeBuilder.edgeVariableAccessLevel,
							MiloSingleServerBaseReferenceNodeBuilder.edgeVariableUserAccessLevel,
							-1.0, false);
					waldotNamespace.getStorageManager().addNode(newProperty);
					try {
						opcEdge.addRelatedProperty(newProperty);
						opcEdge.addReference(new Reference(opcEdge.getNodeId(),
								referenceNodes().hasGremlinPropertyReferenceType,
								newProperty.getNodeId().expanded(), true));
					} catch (final RuntimeException | Error e) {
						waldotNamespace.getStorageManager().removeNode(nodeId);
						throw e;
					}
					created.set(true);
					return newProperty;
				});
		if (created.get()) {
			opcEdge.notifyPropertyValueChanging(key, property.getValue());
		} else {
			property.setValue(newDataValue(value));
		}
		return property;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public <DATA_TYPE> OpcVertexProperty<DATA_TYPE> createOrUpdateWaldotVertexProperty(
			final WaldotVertex opcVertex, final String key,
			final DATA_TYPE value) {
		final NodeId nodeId = waldotNamespace
				.generateNodeId(opcVertex.getNodeId().getIdentifier().toString()
						+ PROPERTY_SPLIT_SIMBOL_IN_NODEID + key);
		final PropertyBag<OpcVertexProperty<?>> bag = vertexProperties
				.computeIfAbsent(opcVertex.getNodeId(),
						k -> new PropertyBag<>());
		final AtomicBoolean created = new AtomicBoolean(false);
		final OpcVertexProperty<DATA_TYPE> property = (OpcVertexProperty<DATA_TYPE>) bag
				.getOrCreate(key, () -> {
					final Optional<UaNode> existing = waldotNamespace
							.getStorageManager().getNode(nodeId);
					if (existing.isPresent()
							&& existing.get() instanceof OpcVertexProperty) {
						return (OpcVertexProperty<?>) existing.get();
					}
					final OpcVertexProperty<DATA_TYPE> newProperty = new OpcVertexProperty(
							waldotNamespace.getGremlinGraph(), opcVertex, key,
							value, opcVertex.getNodeContext(), nodeId,
							LocalizedText.english(key + " of vertex property "
									+ opcVertex.getBrowseName().getName()),
							MiloSingleServerBaseReferenceNodeBuilder.variableWriteMask,
							MiloSingleServerBaseReferenceNodeBuilder.variableUserWriteMask,
							NodeIds.BaseDataType, ValueRanks.Scalar, null,
							MiloSingleServerBaseReferenceNodeBuilder.variableAccessLevel,
							MiloSingleServerBaseReferenceNodeBuilder.variableUserAccessLevel,
							-1.0, false, false);
					waldotNamespace.getStorageManager().addNode(newProperty);
					try {
						opcVertex.addReference(new Reference(
								opcVertex.getNodeId(),
								referenceNodes().hasGremlinPropertyReferenceType,
								newProperty.getNodeId().expanded(), true));
						opcVertex.addRelatedProperty(newProperty);
					} catch (final RuntimeException | Error e) {
						waldotNamespace.getStorageManager().removeNode(nodeId);
						throw e;
					}
					created.set(true);
					return newProperty;
				});
		if (created.get()) {
			opcVertex.notifyPropertyValueChanging(key, property.getValue());
		} else {
			property.setValue(newDataValue(value));
		}
		return property;
	}

	private AbstractOpcVertex createVertex(final NodeId nodeId,
			final NodeId typeDefinition, final String label,
			final String description, final QualifiedName browseName,
			final LocalizedText displayName, final Object[] propertyKeyValues,
			final UInteger writeMask, final UInteger userWriteMask,
			final UByte eventNotifierActive, final long version) {
		vertexProperties.putIfAbsent(nodeId, new PropertyBag<>());
		final AbstractOpcVertex vertex = createVertexObject(propertyKeyValues,
				typeDefinition, waldotNamespace.getGremlinGraph(),
				waldotNamespace.getOpcUaNodeContext(), nodeId, browseName,
				displayName, new LocalizedText(description), writeMask,
				userWriteMask, eventNotifierActive, version);
		waldotNamespace.getStorageManager().addNode(vertex);
		vertex.addReference(new Reference(vertex.getNodeId(),
				NodeIds.HasTypeDefinition, typeDefinition.expanded(), true));
		linkToDirectory(
				MiloStrategy.getKeyValuesProperty(propertyKeyValues,
						DIRECTORY_PARAMETER.toLowerCase()),
				vertex, folderManager.getVerticesFolderNode(),
				folderManager.getVertexDirectories(),
				VERTEX_DIRECTORY_NODEID_PREFIX, false);
		final SharedProperties qp = shared();
		vertex.setProperty(qp.labelVertex(), label);
		String type = MiloStrategy.getKeyValuesProperty(propertyKeyValues,
				TYPE_FIELD.toLowerCase());
		if (type == null || type.isEmpty()) {
			type = "vertex";
			logger.debug(TYPE_FIELD.toLowerCase()
					+ " not found in propertyKeyValues, using default type '{}'",
					type);
		}
		vertex.setProperty(qp.typeVertex(), type);
		popolateVertexPropertiesFromPropertyKeyValues(propertyKeyValues,
				vertex);
		return vertex;
	}

	private AbstractOpcVertex createVertexObject(Object[] propertyKeyValues,
			final NodeId typeDefinition, final WaldotGraph graph,
			final UaNodeContext context, final NodeId nodeId,
			final QualifiedName browseName, final LocalizedText displayName,
			final LocalizedText description, final UInteger writeMask,
			final UInteger userWriteMask, final UByte eventNotifier,
			final long version) {
		if (typeDefinition != null
				&& waldotNamespace.hasNodeId(typeDefinition)) {
			for (final PluginListener p : waldotNamespace.getPlugins()) {
				if (p.containsVertexTypeNode(typeDefinition)) {
					return (AbstractOpcVertex) p.createVertex(typeDefinition,
							graph, context, nodeId, browseName, displayName,
							description, writeMask, userWriteMask,
							eventNotifier, version, propertyKeyValues);
				}
			}
		}
		return new OpcVertex(graph, context, nodeId, browseName, displayName,
				description, writeMask, userWriteMask, eventNotifier, version);
	}

	@Override
	public String deleteOpcNodeId(String nodeId) {
		final NodeId nodeTargetId = MiloStrategy.getNodeIdManager()
				.convert(waldotNamespace.getGremlinGraph(), nodeId);
		if (nodeTargetId == null) {
			throw new IllegalArgumentException("invalid nodeId " + nodeId);
		}
		final Optional<UaNode> node = waldotNamespace.getStorageManager()
				.getNode(nodeTargetId);
		if (node.isEmpty()) {
			cachedVertices.remove(nodeTargetId);
			cachedEdges.remove(nodeTargetId);
			throw new IllegalArgumentException("node " + nodeId + " not found");
		}
		final UaNode target = node.get();
		if (target instanceof WaldotEdge
				|| edgeIndex.containsKey(nodeTargetId)) {
			removeEdge(nodeTargetId);
		} else if (target instanceof WaldotVertex) {
			removeVertex(nodeTargetId);
		} else if (target instanceof WaldotVertexProperty) {
			removeVertexProperty(nodeTargetId);
		} else {
			deleteDirectoryContent(nodeTargetId);
			// the folder may already have been pruned with its last child
			forgetDirectory(nodeTargetId);
			waldotNamespace.getStorageManager().getNode(nodeTargetId)
					.ifPresent(this::deleteQuietly);
		}
		return nodeTargetId.toParseableString();
	}

	/**
	 * Removes every edge and vertex linked in a directory folder or in one of
	 * its sub folders (edges first, then vertices with their remaining edges).
	 * Elements are removed with the normal removal path, so monitors, plugins
	 * and OPC UA nodes are cleaned up as for a single deletion.
	 *
	 * @return the number of removed elements
	 */
	private int deleteDirectoryContent(final NodeId folderId) {
		final List<NodeId> elements = new ArrayList<>();
		synchronized (directoryLock) {
			final DirectoryNode root = directoryNodes.get(folderId);
			if (root == null) {
				return 0;
			}
			for (final Map.Entry<NodeId, DirectoryLink> link : directoryLinks
					.entrySet()) {
				DirectoryNode directory = link.getValue().directory();
				while (directory != null && directory != root) {
					directory = directory.parentDirectory;
				}
				if (directory == root) {
					elements.add(link.getKey());
				}
			}
		}
		int removed = 0;
		for (final NodeId id : elements) {
			if (edgeIndex.containsKey(id) || cachedEdges.containsKey(id)) {
				try {
					removeEdgeInternal(id, true);
					removed++;
				} catch (final RuntimeException e) {
					logger.error("Error removing edge {} of directory {}", id,
							folderId, e);
				}
			}
		}
		for (final NodeId id : elements) {
			// only published graph vertices: commands are never removed here
			if (cachedVertices.containsKey(id)) {
				try {
					removeVertex(id);
					removed++;
				} catch (final RuntimeException e) {
					logger.error("Error removing vertex {} of directory {}", id,
							folderId, e);
				}
			}
		}
		logger.info("Directory {} deleted with {} elements", folderId,
				removed);
		return removed;
	}

	/** Removes a node from the storage and deletes it, ignoring any error. */
	private void deleteQuietly(final UaNode node) {
		if (node == null) {
			return;
		}
		try {
			waldotNamespace.getStorageManager().removeNode(node.getNodeId());
			node.delete();
		} catch (final Throwable e) {
			logger.debug("Error deleting node {}", node.getNodeId(), e);
		}
	}

	/**
	 * Removes every node, reference and index entry of a (possibly half built)
	 * vertex. Tolerant of missing pieces: it is used by the rollback of
	 * {@link #addVertex} and by {@link #removeVertex}.
	 */
	private void discardVertexNodes(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId)
				.orElse(null);
		removePropertyNodes(vertexProperties.remove(nodeId), node);
		removeNodeQuietly(commandNodeId(nodeId, DELETE_COMMAND_SUFFIX));
		removeNodeQuietly(commandNodeId(nodeId, PROPERTY_COMMAND_SUFFIX));
		unlinkFromDirectory(nodeId, node);
		cachedVertices.remove(nodeId);
		deleteQuietly(node);
	}

	@Override
	public void dropGraphComputerView() {
		logger.info("dropGraphComputerView");
	}

	private WaldotVertex endpoint(final WaldotEdge edge, final NodeId nodeId,
			final String role) {
		final WaldotVertex vertex = nodeId == null ? null : vertexById(nodeId);
		if (vertex == null) {
			throw new IllegalStateException("The " + role + " vertex " + nodeId
					+ " of edge " + edge.getNodeId() + " does not exist");
		}
		return vertex;
	}

	/** Forgets the folder of a directory (and its sub folders) when deleted. */
	private void forgetDirectory(final NodeId folderId) {
		synchronized (directoryLock) {
			final DirectoryNode directory = directoryNodes.remove(folderId);
			if (directory == null) {
				return;
			}
			directory.registry.remove(directory.key);
			final String prefix = directory.key + DIRECTORY_SPLIT_SIMBOL;
			directory.registry.keySet().removeIf(k -> k.startsWith(prefix));
			directoryNodes.values()
					.removeIf(d -> d.registry == directory.registry
							&& !d.registry.containsKey(d.key));
		}
	}

	@Override
	public UaFolderNode getAssetRootFolderNode() {
		return assetRootNode;
	}

	@Override
	public WaldotVertex getEdgeInVertex(final WaldotEdge edge) {
		final EdgeEntry entry = edgeIndex.get(edge.getNodeId());
		final NodeId nodeId = entry != null
				? entry.target()
				: edge.getProperty(shared().target()).orElse(null);
		return endpoint(edge, nodeId, "target");
	}

	@Override
	public WaldotVertex getEdgeOutVertex(final WaldotEdge edge) {
		final EdgeEntry entry = edgeIndex.get(edge.getNodeId());
		final NodeId nodeId = entry != null
				? entry.source()
				: edge.getProperty(shared().source()).orElse(null);
		return endpoint(edge, nodeId, "source");
	}

	@Override
	public Map<NodeId, WaldotEdge> getEdges() {
		return Collections.unmodifiableMap(cachedEdges);
	}

	@Override
	public Collection<WaldotEdge> getEdges(final WaldotVertex opcVertex,
			final Direction direction, final String[] edgeLabels) {
		final List<WaldotEdge> result = new ArrayList<>();
		final NodeId id = opcVertex.getNodeId();
		if (direction == Direction.OUT || direction == Direction.BOTH) {
			collectEdges(outEdges.get(id), edgeLabels, result);
		}
		if (direction == Direction.IN || direction == Direction.BOTH) {
			collectEdges(inEdges.get(id), edgeLabels, result);
		}
		return result;
	}

	public int getLastEventId() {
		return lastEventId.intValue();
	}

	private NodeId getOrCreateReferenceType(final String type) {
		// atomic: two threads asking for the same new type create one node
		return referenceCache.computeIfAbsent(type,
				t -> MiloSingleServerBaseReferenceNodeBuilder
						.generateReferenceTypeNode(t, "is a " + t + " of ",
								t + " reference type",
								NodeIds.NonHierarchicalReferences, false, false,
								waldotNamespace));
	}

	private NodeId getParameterNodeId(final String key) {
		return waldotNamespace.generateNodeId(folderManager
				.getVariablesFolderNode().getNodeId().getIdentifier()
				+ PROPERTY_SPLIT_SIMBOL_IN_NODEID + key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> List<WaldotProperty<DATA_TYPE>> getProperties(
			final WaldotEdge opcEdge) {
		final List<WaldotProperty<DATA_TYPE>> result = new ArrayList<>();
		final PropertyBag<OpcProperty<?>> bag = edgeProperties
				.get(opcEdge.getNodeId());
		if (bag != null) {
			for (final OpcProperty<?> p : bag.snapshot().values()) {
				result.add((WaldotProperty<DATA_TYPE>) p);
			}
			return result;
		}
		// edge not created by this strategy: tolerant scan of the references
		for (final UaNode node : linkedPropertyNodes(opcEdge.getReferences())) {
			if (node instanceof WaldotProperty) {
				result.add((WaldotProperty<DATA_TYPE>) node);
			}
		}
		return result;
	}

	@Override
	public <DATA_TYPE> WaldotEdge getPropertyReference(
			final WaldotProperty<DATA_TYPE> property) {
		// the property node knows its edge: no need to scan the references
		// (property.element() is not used: it may delegate back here)
		if (property instanceof AbstractOpcProperty) {
			return ((AbstractOpcProperty<DATA_TYPE>) property)
					.getReferenceEdge();
		}
		for (final Reference r : property.getReferences()) {
			if (r.getReferenceTypeId().equals(NodeIds.HasComponent)
					&& r.isInverse()) {
				final UaNode node = r.getTargetNodeId()
						.toNodeId(waldotNamespace.getNamespaceTable())
						.flatMap(id -> waldotNamespace.getStorageManager()
								.getNode(id))
						.orElse(null);
				return node instanceof WaldotEdge ? (WaldotEdge) node : null;
			}
		}
		return null;
	}

	@Override
	public UaFolderNode getRootFolderNode() {
		return rootNode;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <DATA_TYPE> Map<String, WaldotVertexProperty<DATA_TYPE>> getVertexProperties(
			final WaldotVertex opcVertex) {
		final Map<String, WaldotVertexProperty<DATA_TYPE>> result = new LinkedHashMap<>();
		final PropertyBag<OpcVertexProperty<?>> bag = vertexProperties
				.get(opcVertex.getNodeId());
		if (bag != null) {
			for (final Map.Entry<String, OpcVertexProperty<?>> e : bag
					.snapshot().entrySet()) {
				result.put(e.getKey(),
						(WaldotVertexProperty<DATA_TYPE>) e.getValue());
			}
			return result;
		}
		// vertex not created by this strategy: tolerant scan of the references
		for (final UaNode node : linkedPropertyNodes(
				opcVertex.getReferences())) {
			if (node instanceof WaldotVertexProperty) {
				result.put(node.getBrowseName().getName(),
						(WaldotVertexProperty<DATA_TYPE>) node);
			}
		}
		return result;
	}

	@Override
	public <DATA_TYPE> WaldotVertex getVertexPropertyReference(
			final WaldotVertexProperty<DATA_TYPE> opcVertexProperty) {
		// NOTE: do NOT call opcVertexProperty.element() here: it delegates back
		// to this method via the namespace, causing infinite recursion
		// (StackOverflowError) when the property has no resolvable owner.
		for (final Reference r : opcVertexProperty.getReferences()) {
			if (r.getReferenceTypeId().equals(NodeIds.HasComponent)
					&& r.isInverse()) {
				final UaNode node = waldotNamespace.getStorageManager()
						.get(r.getSourceNodeId());
				return node instanceof WaldotVertex
						? (WaldotVertex) node
						: null;
			}
		}
		return null;
	}

	private NodeId getVertexTypeNode(final Object[] propertyKeyValues) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String
					&& TYPE_FIELD.toLowerCase().equals(propertyKeyValues[i])) {
				if (DIRECTORY_PARAMETER
						.equals(propertyKeyValues[i + 1].toString())) {
					return NodeIds.FolderType;
				} else {
					for (final PluginListener p : waldotNamespace
							.getPlugins()) {
						if (p.containsVertexType(
								propertyKeyValues[i + 1].toString())) {
							return p.getVertexTypeNode(
									propertyKeyValues[i + 1].toString());
						}
					}
					final String requestType = propertyKeyValues[i + 1]
							.toString();
					return NodeId.parse(requestType);
				}
			}
		}
		return null;
	}

	@Override
	public Map<NodeId, WaldotVertex> getVertices() {
		return Collections.unmodifiableMap(cachedVertices);
	}

	@Override
	public Collection<WaldotVertex> getVertices(final WaldotVertex opcVertex,
			final Direction direction, final String[] edgeLabels) {
		final List<WaldotVertex> result = new ArrayList<>();
		final NodeId id = opcVertex.getNodeId();
		if (direction == Direction.OUT || direction == Direction.BOTH) {
			collectVertices(outEdges.get(id), edgeLabels, result, true);
		}
		if (direction == Direction.IN || direction == Direction.BOTH) {
			collectVertices(inEdges.get(id), edgeLabels, result, false);
		}
		return result;
	}

	@Override
	public WaldotNamespace getWaldotNamespace() {
		return waldotNamespace;
	}

	/**
	 * Edges with the same source, target and type share the main reference
	 * {@code source -type-> target}: it must stay while one of them is left.
	 */
	private boolean hasOtherEdgeSharingReference(final EdgeEntry entry) {
		final ConcurrentSkipListMap<Long, EdgeEntry> siblings = outEdges
				.get(entry.source());
		if (siblings == null) {
			return false;
		}
		for (final EdgeEntry other : siblings.values()) {
			if (other.sequence() != entry.sequence()
					&& other.target().equals(entry.target())
					&& other.type().equals(entry.type())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public WaldotNamespace initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		this.sharedProperties = null;
		this.cachedReferenceNodes = null;
		closed.set(false);
		MiloSingleServerBaseReferenceNodeBuilder.generateRefereceNodes(this);
		waldotNamespace.getOpcuaServer().updateReferenceTypeTree();
		rootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(
						waldotNamespace.getConfiguration().getRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace
						.getConfiguration().getRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration()
						.getRootNodeDisplayName()));
		interfaceRootNode = new UaFolderNode(
				waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace
						.getConfiguration().getInterfaceRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace
						.getConfiguration().getInterfaceRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration()
						.getInterfaceRootNodeDisplayName()));
		interfaceRootNode.addReference(new Reference(
				interfaceRootNode.getNodeId(), NodeIds.HasTypeDefinition,
				referenceNodes().interfaceTypeNode.getNodeId().expanded(),
				true));
		assetRootNode = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId(waldotNamespace
						.getConfiguration().getAssetRootNodeId()),
				waldotNamespace.generateQualifiedName(waldotNamespace
						.getConfiguration().getAssetRootNodeBrowseName()),
				LocalizedText.english(waldotNamespace.getConfiguration()
						.getAssetRootNodeDisplayName()));
		waldotNamespace.getClientManagementStrategy()
				.generateAssetFolders(assetRootNode);
		waldotNamespace.getStorageManager().addNode(getRootFolderNode());
		waldotNamespace.getStorageManager().addNode(assetRootNode);
		waldotNamespace.getStorageManager().addNode(interfaceRootNode);
		getRootFolderNode().addReference(new Reference(
				getRootFolderNode().getNodeId(), NodeIds.Organizes,
				NodeIds.ObjectsFolder.expanded(), false));
		assetRootNode.addReference(new Reference(assetRootNode.getNodeId(),
				NodeIds.Organizes, NodeIds.ObjectsFolder.expanded(), false));
		interfaceRootNode.addReference(
				new Reference(interfaceRootNode.getNodeId(), NodeIds.Organizes,
						NodeIds.ObjectsFolder.expanded(), false));
		folderManager.initialize();
		return waldotNamespace;
	}

	private boolean labelMatches(final EdgeEntry entry,
			final String[] edgeLabels) {
		if (edgeLabels == null || edgeLabels.length == 0) {
			return true;
		}
		final String label = entry.label();
		if (label == null || label.isEmpty()) {
			return false;
		}
		for (final String wanted : edgeLabels) {
			if (label.equals(wanted)) {
				return true;
			}
		}
		return false;
	}

	private void linkCommandDirectoryStructure(AbstractOpcCommand command) {
		linkToDirectory(command.getDirectory(), command, interfaceRootNode,
				folderManager.getCommandDirectories(),
				COMMAND_DIRECTORY_NODEID_PREFIX, true);
	}

	/**
	 * Gremlin property nodes referenced by an element, found scanning its
	 * references. Missing targets are skipped. Used only for elements that are
	 * not in the property indexes.
	 */
	private List<UaNode> linkedPropertyNodes(final List<Reference> references) {
		final List<UaNode> result = new ArrayList<>();
		for (final Reference p : new ArrayList<>(references)) {
			if (p.isForward() && p.getReferenceTypeId()
					.equals(referenceNodes().hasGremlinPropertyReferenceType)) {
				p.getTargetNodeId()
						.toNodeId(waldotNamespace.getNamespaceTable())
						.flatMap(id -> waldotNamespace.getStorageManager()
								.getNode(id))
						.ifPresent(result::add);
			}
		}
		return result;
	}

	/**
	 * Links an element under the folder tree described by {@code directory}
	 * (components separated by {@code /}), creating the missing folders. Empty
	 * components are ignored; without directory the element is linked directly
	 * under {@code rootFolder}. The folder tree and the children counters are
	 * guarded by {@link #directoryLock}.
	 */
	private void linkToDirectory(final String directory, final UaNode element,
			final UaFolderNode rootFolder,
			final Map<String, UaFolderNode> registry, final String prefix,
			final boolean component) {
		final List<String> components = new ArrayList<>();
		if (directory != null) {
			for (final String c : directory.split(DIRECTORY_SPLIT_SIMBOL)) {
				final String trimmed = c.trim();
				if (!trimmed.isEmpty()) {
					components.add(trimmed);
				}
			}
		}
		synchronized (directoryLock) {
			UaFolderNode parentFolder = rootFolder;
			DirectoryNode parentDirectory = null;
			String key = null;
			for (final String name : components) {
				key = key == null ? name : key + DIRECTORY_SPLIT_SIMBOL + name;
				UaFolderNode folder = registry.get(key);
				DirectoryNode current = folder == null
						? null
						: directoryNodes.get(folder.getNodeId());
				if (folder == null) {
					// NodeId without prefix for command directories: the base
					// strategy and the client library address them as
					// ns=2;s=<directory> (e.g. ns=2;s=information)
					final NodeId folderNodeId = component
							? waldotNamespace.generateNodeId(key)
							: waldotNamespace.generateNodeId(prefix + ":" + key);
					folder = new UaFolderNode(
							waldotNamespace.getOpcUaNodeContext(),
							folderNodeId,
							waldotNamespace.generateQualifiedName(name),
							LocalizedText.english(name));
					waldotNamespace.getStorageManager().addNode(folder);
					parentFolder.addOrganizes(folder);
					current = new DirectoryNode(key, folder, parentFolder,
							parentDirectory, registry);
					registry.put(key, folder);
					directoryNodes.put(folder.getNodeId(), current);
					if (parentDirectory != null) {
						parentDirectory.children++;
					}
				}
				parentFolder = folder;
				parentDirectory = current;
			}
			if (component) {
				parentFolder.addComponent(element);
			} else {
				parentFolder.addOrganizes(element);
			}
			if (parentDirectory != null) {
				parentDirectory.children++;
			}
			directoryLinks.put(element.getNodeId(), new DirectoryLink(
					parentFolder, parentDirectory, component));
		}
	}

	@Override
	public Object namespaceParametersGet(final String key) {
		if (waldotNamespace.hasNodeId(getParameterNodeId(key))) {
			final UaVariableNode property = (UaVariableNode) waldotNamespace
					.getStorageManager().getNode(getParameterNodeId(key)).get();
			return property.getValue().getValue().getValue();
		} else {
			return null;
		}
	}

	@Override
	public Set<String> namespaceParametersKeySet() {
		final Set<String> keySet = new HashSet<>();
		for (final Node n : folderManager.getVariablesFolderNode()
				.getOrganizesNodes()) {
			keySet.add(n.getBrowseName().getName());
		}
		return keySet;
	}

	@Override
	public void namespaceParametersPut(final String key, final Object value) {
		synchronized (parametersLock) {
			if (waldotNamespace.hasNodeId(getParameterNodeId(key))) {
				final UaVariableNode property = (UaVariableNode) waldotNamespace
						.getStorageManager().getNode(getParameterNodeId(key))
						.get();
				property.setValue(new DataValue(new Variant(value)));
			} else {
				final UaVariableNode property = new UaVariableNode(
						waldotNamespace.getOpcUaNodeContext(),
						getParameterNodeId(key),
						waldotNamespace.generateQualifiedName(key),
						LocalizedText.english(key),
						LocalizedText.english("variable " + key), UInteger.MIN,
						UInteger.MIN);
				waldotNamespace.getStorageManager().addNode(property);
				folderManager.getVariablesFolderNode().addOrganizes(property);
				property.setValue(new DataValue(new Variant(value)));
			}
		}
	}

	@Override
	public void namespaceParametersRemove(final String key) {
		synchronized (parametersLock) {
			if (waldotNamespace.hasNodeId(getParameterNodeId(key))) {
				final UaVariableNode property = (UaVariableNode) waldotNamespace
						.getStorageManager().getNode(getParameterNodeId(key))
						.get();
				folderManager.getVariablesFolderNode()
						.removeOrganizes(property);
				waldotNamespace.getStorageManager()
						.removeNode(property.getNodeId());
				property.delete();
			}
		}
	}

	@Override
	public Variables namespaceParametersToVariables() {
		if (opcGraphVariables == null) {
			opcGraphVariables = new OpcGraphVariables(waldotNamespace);
		}
		return opcGraphVariables;
	}

	private DataValue newDataValue(final Object value) {
		return DataValue.newValue().setStatus(StatusCode.GOOD)
				.setSourceTime(DateTime.now()).setValue(new Variant(value))
				.build();
	}

	private void popolateEdgePropertiesFromPropertyKeyValues(
			final Object[] propertyKeyValues, final OpcEdge edge) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String) {
				createOrUpdateWaldotEdgeProperty(edge,
						(String) propertyKeyValues[i],
						propertyKeyValues[i + 1]);
			}
		}
	}

	public void popolateVertexPropertiesFromPropertyKeyValues(
			final Object[] propertyKeyValues, final AbstractOpcVertex vertex) {
		for (int i = 0; i < propertyKeyValues.length; i = i + 2) {
			if (propertyKeyValues[i] instanceof String) {
				createOrUpdateWaldotVertexProperty(vertex,
						(String) propertyKeyValues[i],
						propertyKeyValues[i + 1]);
			}
		}
	}

	/**
	 * Makes a fully built edge visible: adjacency indexes first, then the
	 * public cache.
	 */
	private void publishEdge(final EdgeEntry entry) {
		outEdges.compute(entry.source(), (k, m) -> withEntry(m, entry));
		inEdges.compute(entry.target(), (k, m) -> withEntry(m, entry));
		cachedEdges.put(entry.nodeId(), entry.edge());
	}

	private String readType(final WaldotEdge edge) {
		try {
			return edge.getProperty(shared().typeEdge()).orElse(null);
		} catch (final Throwable e) {
			return null;
		}
	}

	private MiloSingleServerBaseReferenceNodeBuilder.ReferenceNodes referenceNodes() {
		// hot path: the registry is synchronized, so the snapshot of this
		// namespace is cached once it is available
		MiloSingleServerBaseReferenceNodeBuilder.ReferenceNodes nodes = cachedReferenceNodes;
		if (nodes == null) {
			nodes = MiloSingleServerBaseReferenceNodeBuilder
					.registeredFor(waldotNamespace);
			if (nodes == null) {
				return MiloSingleServerBaseReferenceNodeBuilder
						.forNamespace(waldotNamespace);
			}
			cachedReferenceNodes = nodes;
		}
		return nodes;
	}

	@Override
	public void registerCommand(final WaldotCommand command) {
		linkCommandDirectoryStructure((AbstractOpcCommand) command);
	}

	@Override
	public void removeCommand(final WaldotCommand command) {
		if (!unlinkFromDirectory(command.getNodeId(),
				(AbstractOpcCommand) command)) {
			interfaceRootNode.removeComponent((AbstractOpcCommand) command);
		}
	}

	@Override
	public void removeEdge(final NodeId nodeId) {
		removeEdgeInternal(nodeId, true);
	}

	/**
	 * Removes an edge completely. Idempotent and tolerant of half built edges
	 * (it is also the rollback of {@link #addEdge}).
	 */
	private void removeEdgeInternal(final NodeId nodeId,
			final boolean notifyPlugins) {
		final EdgeEntry entry = edgeIndex.get(nodeId);
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId)
				.orElse(null);
		if (entry == null && node == null) {
			logger.debug("Edge {} already removed", nodeId);
			return;
		}
		if (entry == null && !(node instanceof WaldotEdge)) {
			logger.warn("NodeId {} is not an edge", nodeId);
			return;
		}
		final WaldotEdge edge = entry != null
				? entry.edge()
				: (WaldotEdge) node;
		// 1. unpublish: traversals no longer see the edge. The edgeIndex stays
		// until the end, the monitors still need inVertex() / outVertex()
		cachedEdges.remove(nodeId);
		if (entry != null) {
			outEdges.computeIfPresent(entry.source(),
					(k, m) -> withoutEntry(m, entry));
			inEdges.computeIfPresent(entry.target(),
					(k, m) -> withoutEntry(m, entry));
		}
		// 2. monitor, properties and references registered on the edge
		try {
			edge.removeRelatedOpcUaNodes();
		} catch (final Throwable e) {
			logger.warn("Error removing the related nodes of edge {}", nodeId,
					e);
		}
		if (entry != null) {
			removeEdgeReferences(entry);
		}
		if (notifyPlugins) {
			final String type = entry != null ? entry.type() : readType(edge);
			for (final PluginListener p : waldotNamespace.getPlugins()) {
				if (type != null && p.containsEdgeType(type)) {
					p.notifyRemoveEdge(edge);
				}
			}
		}
		// 3. nodes owned by the edge
		removePropertyNodes(edgeProperties.remove(nodeId), node);
		removeNodeQuietly(commandNodeId(nodeId, DELETE_COMMAND_SUFFIX));
		removeNodeQuietly(commandNodeId(nodeId, PROPERTY_COMMAND_SUFFIX));
		unlinkFromDirectory(nodeId, node);
		edgeIndex.remove(nodeId);
		deleteQuietly(node);
	}

	/** Removes from the end vertices the references created for the edge. */
	private void removeEdgeReferences(final EdgeEntry entry) {
		final WaldotVertex source = vertexById(entry.source());
		final WaldotVertex target = vertexById(entry.target());
		final NodeId descriptionType = referenceNodes().hasReferenceDescriptionReferenceType;
		if (source != null) {
			removeReferenceQuietly(source, new Reference(entry.source(),
					descriptionType, entry.nodeId().expanded(), true));
			final NodeId mainType = referenceCache.get(entry.type());
			if (mainType != null && !hasOtherEdgeSharingReference(entry)) {
				removeReferenceQuietly(source, new Reference(entry.source(),
						mainType, entry.target().expanded(), true));
			}
		}
		if (target != null) {
			removeReferenceQuietly(target, new Reference(entry.target(),
					descriptionType, entry.nodeId().expanded(), true));
		}
	}

	/** Removes (storage and delete) a node by id, ignoring any error. */
	private void removeNodeQuietly(final NodeId nodeId) {
		try {
			waldotNamespace.getStorageManager().removeNode(nodeId)
					.ifPresent(n -> {
						try {
							n.delete();
						} catch (final Throwable e) {
							logger.debug("Error deleting node {}", nodeId, e);
						}
					});
		} catch (final Throwable e) {
			logger.debug("Error removing node {}", nodeId, e);
		}
	}

	/**
	 * Deletes the property nodes of an element: the ones of the index and any
	 * other referenced by the owner node.
	 */
	private void removePropertyNodes(final PropertyBag<? extends UaNode> bag,
			final UaNode owner) {
		final Set<NodeId> done = new HashSet<>();
		if (bag != null) {
			for (final UaNode property : bag.drain()) {
				done.add(property.getNodeId());
				deleteQuietly(property);
			}
		}
		if (owner != null) {
			try {
				for (final UaNode residual : linkedPropertyNodes(
						owner.getReferences())) {
					if (done.add(residual.getNodeId())) {
						deleteQuietly(residual);
					}
				}
			} catch (final Throwable e) {
				logger.debug("Error removing the residual properties of {}",
						owner.getNodeId(), e);
			}
		}
	}

	@Override
	public void removeReference(Reference reference) {
		waldotNamespace.getStorageManager().removeReference(reference);
	}

	private void removeReferenceQuietly(final WaldotVertex vertex,
			final Reference reference) {
		try {
			vertex.removeReference(reference);
		} catch (final Throwable e) {
			logger.debug("Error removing reference {}", reference, e);
		}
	}

	@Override
	public void removeVertex(final NodeId nodeId) {
		final UaNode node = waldotNamespace.getStorageManager().getNode(nodeId)
				.orElse(null);
		if (node == null) {
			if (cachedVertices.remove(nodeId) != null) {
				logger.debug("Vertex {} removed from cache only", nodeId);
			}
			return;
		}
		if (!(node instanceof WaldotVertex)) {
			logger.warn("NodeId {} is not a vertex", nodeId);
			return;
		}
		// cascade: the incident edges go first, while the vertex still exists
		final Set<NodeId> incident = new HashSet<>();
		for (final Map<NodeId, ConcurrentSkipListMap<Long, EdgeEntry>> adjacency : List
				.of(outEdges, inEdges)) {
			final ConcurrentSkipListMap<Long, EdgeEntry> entries = adjacency
					.get(nodeId);
			if (entries != null) {
				for (final EdgeEntry entry : entries.values()) {
					incident.add(entry.nodeId());
				}
			}
		}
		for (final NodeId edgeId : incident) {
			try {
				removeEdgeInternal(edgeId, true);
			} catch (final RuntimeException e) {
				logger.error("Error removing edge {} of vertex {}", edgeId,
						nodeId, e);
			}
		}
		((WaldotVertex) node).notifyRemoveVertex();
		((WaldotVertex) node).removeRelatedOpcUaNodes();
		discardVertexNodes(nodeId);
		outEdges.remove(nodeId);
		inEdges.remove(nodeId);
		logger.info("Vertex with NodeId {} removed", nodeId);
	}

	@Override
	public void removeVertexProperty(final NodeId nodeId) {
		final Optional<UaNode> node = waldotNamespace.getStorageManager()
				.getNode(nodeId);
		// drop it from the index of its vertex
		final java.util.function.Predicate<OpcVertexProperty<?>> sameNode = p -> p
				.getNodeId().equals(nodeId);
		if (node.isPresent() && node.get() instanceof WaldotVertexProperty) {
			final Object owner = ((WaldotVertexProperty<?>) node.get())
					.element();
			if (owner instanceof WaldotVertex) {
				final PropertyBag<OpcVertexProperty<?>> bag = vertexProperties
						.get(((WaldotVertex) owner).getNodeId());
				if (bag != null) {
					bag.removeIf(sameNode);
				}
			} else {
				vertexProperties.values().forEach(b -> b.removeIf(sameNode));
			}
		} else {
			vertexProperties.values().forEach(b -> b.removeIf(sameNode));
		}
		if (node.isEmpty()) {
			logger.debug("Vertex property with NodeId {} already removed",
					nodeId);
			return;
		}
		waldotNamespace.getStorageManager().removeNode(nodeId);
		node.get().delete();
	}

	@Override
	public void resetNameSpace() {
		logger.info("resetNameSpace");
		// edges first: their monitors observe the vertices
		for (final NodeId edgeId : new ArrayList<>(cachedEdges.keySet())) {
			try {
				removeEdgeInternal(edgeId, true);
			} catch (final RuntimeException e) {
				logger.error("Error removing edge {} during reset", edgeId, e);
			}
		}
		for (final NodeId vertexId : new ArrayList<>(
				cachedVertices.keySet())) {
			try {
				removeVertex(vertexId);
			} catch (final RuntimeException e) {
				logger.error("Error removing vertex {} during reset", vertexId,
						e);
			}
		}
		logger.info("resetNameSpace done: {} vertices, {} edges left",
				cachedVertices.size(), cachedEdges.size());
	}

	private SharedProperties shared() {
		SharedProperties props = sharedProperties;
		if (props == null) {
			final String uri = waldotNamespace.getNamespaceUri();
			final MiloSingleServerBaseReferenceNodeBuilder.ReferenceNodes nodes = referenceNodes();
			props = new SharedProperties(
					new QualifiedProperty<String>(uri, LABEL_FIELD,
							nodes.labelEdgeTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, String.class),
					new QualifiedProperty<String>(uri, TYPE_FIELD,
							nodes.typeEdgeTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, String.class),
					new QualifiedProperty<NodeId>(uri, SOURCE_NODE,
							nodes.sourceNodeTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, NodeId.class),
					new QualifiedProperty<NodeId>(uri, TARGET_NODE,
							nodes.targetNodeTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, NodeId.class),
					new QualifiedProperty<NodeId>(uri, REFERENCE_TYPE,
							nodes.referenceTypeTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, NodeId.class),
					new QualifiedProperty<Boolean>(uri, IS_FORWARD,
							nodes.isForwardTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, Boolean.class),
					new QualifiedProperty<String>(uri, LABEL_FIELD,
							nodes.labelVertexTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, String.class),
					new QualifiedProperty<String>(uri, TYPE_FIELD,
							nodes.vertexTypeNode.getNodeId().expanded(),
							ValueRanks.Scalar, String.class));
			sharedProperties = props;
		}
		return props;
	}

	/**
	 * Unlinks an element from its folder and prunes the folders left empty.
	 * 
	 * @return true if the element was linked
	 */
	private boolean unlinkFromDirectory(final NodeId elementId,
			final UaNode element) {
		synchronized (directoryLock) {
			final DirectoryLink link = directoryLinks.remove(elementId);
			if (link == null) {
				return false;
			}
			try {
				if (element != null) {
					if (link.component()) {
						link.folder().removeComponent(element);
					} else {
						link.folder().removeOrganizes(element);
					}
				}
			} catch (final Throwable e) {
				logger.debug("Error unlinking {}", elementId, e);
			}
			DirectoryNode directory = link.directory();
			while (directory != null) {
				directory.children--;
				if (directory.children > 0) {
					break;
				}
				directory.registry.remove(directory.key);
				directoryNodes.remove(directory.folder.getNodeId());
				try {
					directory.parentFolder.removeOrganizes(directory.folder);
				} catch (final Throwable e) {
					logger.debug("Error unlinking folder {}", directory.key, e);
				}
				deleteQuietly(directory.folder);
				directory = directory.parentDirectory;
			}
			return true;
		}
	}

	@Override
	public void updateEventGenerator(final Node sourceNode, String eventName,
			String eventDisplayName, String message, int severity) {
		try {
			final BaseEventTypeNode eventNode = waldotNamespace.getOpcuaServer()
					.getServer().getEventFactory().createEvent(
							waldotNamespace.generateNodeId(UUID.randomUUID()),
							NodeIds.BaseModelChangeEventType);
			eventNode.setBrowseName(new QualifiedName(1, eventName));
			eventNode.setDisplayName(LocalizedText.english(eventDisplayName));
			eventNode.setEventId(ByteString
					.of(Longs.toByteArray(lastEventId.incrementAndGet())));
			eventNode.setEventType(NodeIds.BaseEventType);
			eventNode.setSourceNode(sourceNode.getNodeId());
			eventNode.setSourceName(sourceNode.getDisplayName().getText());
			eventNode.setTime(DateTime.now());
			eventNode.setReceiveTime(DateTime.NULL_VALUE);
			eventNode.setMessage(LocalizedText.english(message));
			eventNode.setSeverity(ushort(severity));
			waldotNamespace.getEventBus().fire(eventNode);
			eventNode.delete();
		} catch (final Throwable e) {
			logger.error("Error creating EventNode: {}", e.getMessage(), e);
		}
	}

	/** Vertex by id: index first, then the storage (plugin vertices). */
	private WaldotVertex vertexById(final NodeId nodeId) {
		final WaldotVertex cached = cachedVertices.get(nodeId);
		if (cached != null) {
			return cached;
		}
		final UaNode node = waldotNamespace.getStorageManager().get(nodeId);
		return node instanceof WaldotVertex ? (WaldotVertex) node : null;
	}

	private ConcurrentSkipListMap<Long, EdgeEntry> withEntry(
			final ConcurrentSkipListMap<Long, EdgeEntry> map,
			final EdgeEntry entry) {
		final ConcurrentSkipListMap<Long, EdgeEntry> result = map == null
				? new ConcurrentSkipListMap<>()
				: map;
		result.put(entry.sequence(), entry);
		return result;
	}

	private ConcurrentSkipListMap<Long, EdgeEntry> withoutEntry(
			final ConcurrentSkipListMap<Long, EdgeEntry> map,
			final EdgeEntry entry) {
		map.remove(entry.sequence());
		return map.isEmpty() ? null : map;
	}

}
