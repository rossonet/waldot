package net.rossonet.waldot.gremlin.opcgraph.strategies.agent;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.UserNameIdentityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.agent.auth.AgentRegisterAnonymousValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterUsernameIdentityValidator;
import net.rossonet.waldot.agent.auth.AgentRegisterX509IdentityValidator;
import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.WaldotAgentManagementStrategy;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;

public class BaseAgentManagementStrategy implements WaldotAgentManagementStrategy {
	private class GenerateAddProvisioningMethodInvocationHandler extends AbstractMethodInvocationHandler {

		private final List<Argument> inputArguments;

		public GenerateAddProvisioningMethodInvocationHandler(final UaMethodNode methodNode,
				final List<Argument> inputArguments) {
			super(methodNode);
			this.inputArguments = inputArguments;
		}

		@Override
		public Argument[] getInputArguments() {
			return inputArguments.toArray(new Argument[0]);
		}

		@Override
		public Argument[] getOutputArguments() {
			return new Argument[0];
		}

		@Override
		protected Variant[] invoke(final InvocationContext invocationContext, final Variant[] inputValues)
				throws UaException {
			try {
				generateNewProvisioningToken(inputValues[0].getValue().toString(),
						inputValues[1].getValue().toString());
				return new Variant[0];
			} catch (final Exception e) {
				logger.error("Error generating new provisioning token", e);
				return new Variant[0];
			}
		}

	}

	private class GenerateDeleteProvisioningMethodInvocationHandler extends AbstractMethodInvocationHandler {

		private final List<Argument> inputArguments;

		public GenerateDeleteProvisioningMethodInvocationHandler(final UaMethodNode methodNode,
				final List<Argument> inputArguments) {
			super(methodNode);
			this.inputArguments = inputArguments;
		}

		@Override
		public Argument[] getInputArguments() {
			return inputArguments.toArray(new Argument[0]);
		}

		@Override
		public Argument[] getOutputArguments() {
			return new Argument[0];
		}

		@Override
		protected Variant[] invoke(final InvocationContext invocationContext, final Variant[] inputValues)
				throws UaException {
			try {
				logger.info("Deleting provisioning token with ID: {}", inputValues[0].getValue().toString());
				deleteProvisioningToken(inputValues[0].getValue().toString());

				return new Variant[0];
			} catch (final Exception e) {
				logger.error("Error deleting provisioning token", e);
				return new Variant[0];
			}
		}

	}

	public enum ProvisioningStatus {
		PENDING, APPROVED, REJECTED
	}

	public final class ProvisioningToken {
		private final String id;
		private final String secret;
		private final NodeId nodeId;

		public ProvisioningToken(final String id, final String secret, final NodeId nodeId) {
			this.id = id;
			this.secret = secret;
			this.nodeId = nodeId;
		}

		public String getId() {
			return id;
		}

		public NodeId getNodeId() {
			return nodeId;
		}

		public String getSecret() {
			return secret;
		}
	}

	private final class SessionData {
		private final Session session;
		private final ProvisioningStatus status;

		public SessionData(final Session session, final ProvisioningStatus status) {
			this.session = session;
			this.status = status;
		}

		public Session getSession() {
			return session;
		}

		public ProvisioningStatus getStatus() {
			return status;
		}
	}

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private WaldotNamespace waldotNamespace;
	private UaFolderNode provisioningFolder;
	private UaFolderNode assetRootNode;
	private UaFolderNode agentFolder;

	private AgentRegisterAnonymousValidator agentAnonymousValidator;
	private AgentRegisterUsernameIdentityValidator agentIdentityValidator;

	private AgentRegisterX509IdentityValidator agentX509IdentityValidator;
	private UaMethodNode provisioningAddTokenMethod;

	private final Map<NodeId, SessionData> sessions = new HashMap<>();

	private final Map<String, ProvisioningToken> tokenSecrets = new HashMap<>();
	private UaObjectNode managementRecord;
	private UaMethodNode provisioningDeleteTokenMethod;

	@Override
	public void activate(final AgentRegisterAnonymousValidator agentAnonymousValidator,
			final AgentRegisterUsernameIdentityValidator agentIdentityValidator,
			final AgentRegisterX509IdentityValidator agentX509IdentityValidator) {
		this.agentAnonymousValidator = agentAnonymousValidator;
		this.agentIdentityValidator = agentIdentityValidator;
		this.agentX509IdentityValidator = agentX509IdentityValidator;

	}

	protected void deleteProvisioningToken(final String id) {
		waldotNamespace.getStorageManager().removeNode(tokenSecrets.get(id).getNodeId());
		tokenSecrets.remove(id);
	}

	private void generateAddProvisioningTokenMethod() {
		final Boolean userExecutable = Boolean.TRUE;
		final Boolean executable = Boolean.TRUE;
		final UInteger writeMask = UInteger.valueOf(WriteMask.Executable.getValue());
		final UInteger userWriteMask = UInteger.valueOf(WriteMask.Executable.getValue());
		provisioningAddTokenMethod = new UaMethodNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("add-pt"),
				waldotNamespace.generateQualifiedName("Add Provisioning Token"),
				LocalizedText.english("Add Provisioning Token"),
				LocalizedText.english("with this method you can add the provisioning token"), writeMask, userWriteMask,
				executable, userExecutable);

		final List<Argument> inputArguments = new ArrayList<>();
		final Argument id = new Argument("provisioningID", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar,
				null, LocalizedText.english("unique identifier of the provisioning token"));
		inputArguments.add(id);
		final Argument secret = new Argument("provisioningPassword", VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english("provvisioning secret token ( password)"));
		inputArguments.add(secret);
		provisioningAddTokenMethod.setInputArguments(inputArguments.toArray(new Argument[0]));
		provisioningAddTokenMethod.setOutputArguments(new Argument[0]);
		provisioningAddTokenMethod.setInvocationHandler(
				new GenerateAddProvisioningMethodInvocationHandler(provisioningAddTokenMethod, inputArguments));
		waldotNamespace.getStorageManager().addNode(provisioningAddTokenMethod);
		// provisioningAddTokenMethod.addReference(new
		// Reference(provisioningAddTokenMethod.getNodeId(),
		// Identifiers.Organizes, managementRecord.getNodeId().expanded(), false));
		provisioningAddTokenMethod.addReference(new Reference(provisioningAddTokenMethod.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		managementRecord.addComponent(provisioningAddTokenMethod);
	}

	private void generateAgentFolder() {
		agentFolder = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId("agents"),
				waldotNamespace.generateQualifiedName("Agent Management"),
				LocalizedText.english("Agents Management Instrumentation"));
		waldotNamespace.getStorageManager().addNode(agentFolder);
		// agentFolder.addReference(new Reference(agentFolder.getNodeId(),
		// Identifiers.Organizes,
		// assetRootNode.getNodeId().expanded(), false));
		assetRootNode.addOrganizes(agentFolder);
	}

	@Override
	public void generateAssetFolders(final UaFolderNode assetRootNode) {
		this.assetRootNode = assetRootNode;
		generateProvisioningFolder();
		generateAddProvisioningTokenMethod();
		generateDeleteProvisioningTokenMethod();
		generateAgentFolder();
	}

	private void generateDeleteProvisioningTokenMethod() {
		final Boolean userExecutable = Boolean.TRUE;
		final Boolean executable = Boolean.TRUE;
		final UInteger writeMask = UInteger.valueOf(WriteMask.Executable.getValue());
		final UInteger userWriteMask = UInteger.valueOf(WriteMask.Executable.getValue());
		provisioningDeleteTokenMethod = new UaMethodNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("del-pt"),
				waldotNamespace.generateQualifiedName("Delete Provisioning Token"),
				LocalizedText.english("Delete Provisioning Token"),
				LocalizedText.english("with this method you can delete the provisioning token"), writeMask,
				userWriteMask, executable, userExecutable);
		final List<Argument> inputArguments = new ArrayList<>();
		final Argument id = new Argument("provisioningID", VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar,
				null, LocalizedText.english("unique identifier of the provisioning token"));
		inputArguments.add(id);
		provisioningDeleteTokenMethod.setInputArguments(inputArguments.toArray(new Argument[0]));
		provisioningDeleteTokenMethod.setOutputArguments(new Argument[0]);
		provisioningDeleteTokenMethod.setInvocationHandler(
				new GenerateDeleteProvisioningMethodInvocationHandler(provisioningDeleteTokenMethod, inputArguments));
		waldotNamespace.getStorageManager().addNode(provisioningDeleteTokenMethod);
		// provisioningAddTokenMethod.addReference(new
		// Reference(provisioningAddTokenMethod.getNodeId(),
		// Identifiers.Organizes, managementRecord.getNodeId().expanded(), false));
		provisioningDeleteTokenMethod.addReference(new Reference(provisioningDeleteTokenMethod.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		managementRecord.addComponent(provisioningDeleteTokenMethod);
	}

	private void generateDeleteProvisioningTokenMethod(final UaObjectNode token, final String id) {
		final Boolean userExecutable = Boolean.TRUE;
		final Boolean executable = Boolean.TRUE;
		final UInteger writeMask = UInteger.valueOf(WriteMask.Executable.getValue());
		final UInteger userWriteMask = UInteger.valueOf(WriteMask.Executable.getValue());
		final UaMethodNode provisioningDeleteTokenMethod = new UaMethodNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("del-pt-" + id),
				waldotNamespace.generateQualifiedName("Delete Provisioning Token " + id),
				LocalizedText.english("Delete Provisioning Token " + id),
				LocalizedText.english("with this method you can delete the provisioning token"), writeMask,
				userWriteMask, executable, userExecutable);
		provisioningDeleteTokenMethod.setInvocationHandler(new MethodInvocationHandler() {

			@Override
			public CallMethodResult invoke(final AccessContext accessContext, final CallMethodRequest request) {
				try {
					deleteProvisioningToken(id);
					return CallMethodResult.builder().statusCode(StatusCode.GOOD).build();
				} catch (final Exception e) {
					logger.error("Error deleting provisioning token with ID: {}", id, e);
					return CallMethodResult.builder().statusCode(StatusCode.BAD).build();
				}
			}

		});
		waldotNamespace.getStorageManager().addNode(provisioningDeleteTokenMethod);
		// provisioningDeleteTokenMethod.addReference(new
		// Reference(provisioningDeleteTokenMethod.getNodeId(),
		// Identifiers.Organizes, token.getNodeId().expanded(), false));
		token.addComponent(provisioningDeleteTokenMethod);
	}

	protected void generateNewProvisioningToken(final String id, final String secret) {
		final NodeId nodeId = generateProvisioningTokenRecord(id, secret);
		tokenSecrets.put(id, new ProvisioningToken(id, secret, nodeId));
		logger.info("New provisioning token generated with ID: {}", id);
	}

	private void generateProvisioningFolder() {
		provisioningFolder = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("provisioning"),
				waldotNamespace.generateQualifiedName("Provisioning Management"),
				LocalizedText.english("Provisioning Management Instrumentation"));
		waldotNamespace.getStorageManager().addNode(provisioningFolder);
		// provisioningFolder.addReference(new Reference(provisioningFolder.getNodeId(),
		// Identifiers.Organizes,
		// assetRootNode.getNodeId().expanded(), false));
		assetRootNode.addOrganizes(provisioningFolder);
		final NodeId nodeId = waldotNamespace.generateNodeId("token_management");
		managementRecord = new UaObjectNode(waldotNamespace.getOpcUaNodeContext(), nodeId,
				waldotNamespace.generateQualifiedName("Token Management"), LocalizedText.english("Token Management"));

		waldotNamespace.getStorageManager().addNode(managementRecord);
		managementRecord.addReference(new Reference(managementRecord.getNodeId(), Identifiers.HasTypeDefinition,
				Identifiers.BaseObjectType.expanded(), true));
		// managementRecord.addReference(new Reference(managementRecord.getNodeId(),
		// Identifiers.Organizes,
		// provisioningFolder.getNodeId().expanded(), false));
		provisioningFolder.addComponent(managementRecord);
	}

	private NodeId generateProvisioningTokenRecord(final String id, final String secret) {
		final NodeId nodeId = waldotNamespace.generateNodeId("pt-" + id);
		final UaObjectNode provisioningRecord = new UaObjectNode(waldotNamespace.getOpcUaNodeContext(), nodeId,
				waldotNamespace.generateQualifiedName("Provisioning Token " + id), LocalizedText.english("ID " + id));
		waldotNamespace.getStorageManager().addNode(provisioningRecord);
		// provisioningRecord.addReference(new Reference(provisioningRecord.getNodeId(),
		// Identifiers.Organizes,
		// provisioningFolder.getNodeId().expanded(), false));
		final QualifiedProperty<String> ID = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				"provisioningID", Identifiers.BaseDataType.expanded(), ValueRanks.Scalar, String.class);
		provisioningRecord.setProperty(ID, id);
		final QualifiedProperty<String> SECRET = new QualifiedProperty<String>(waldotNamespace.getNamespaceUri(),
				"secret", Identifiers.BaseDataType.expanded(), ValueRanks.Scalar, String.class);
		provisioningRecord.setProperty(SECRET, secret);
		provisioningFolder.addOrganizes(provisioningRecord);
		generateDeleteProvisioningTokenMethod(provisioningRecord, id);
		waldotNamespace.opcuaUpdateEvent(provisioningFolder);
		return nodeId;
	}

	private String getSessionId(final Session session) {
		if (session == null) {
			logger.warn("Session is null, cannot get session ID");
			return null;
		}
		if (sessions.containsKey(session.getSessionId())) {
			return session.getSessionId().toString();
		} else {
			logger.warn("Session {} not found in registered sessions", session.getSessionId());
			throw new IllegalStateException("Session " + session.getSessionId() + " not found in registered sessions");
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		logger.info("Agent Management Strategy initialized for namespace: {}", waldotNamespace);
	}

	@Override
	public String registerNewAgentForApproval(final Session session) {
		registerSessionIfNeeded(session, ProvisioningStatus.PENDING);
		logger.info("New agent registered for approval: {}", session.getSessionId());
		return getSessionId(session);
	}

	@Override
	public String registerNewAgentWithProvisioningPassword(final Session session, final UserNameIdentityToken token) {
		// TODO verificare il token
		registerSessionIfNeeded(session, ProvisioningStatus.APPROVED);
		return getSessionId(session);
	}

	private void registerSessionIfNeeded(final Session session, final ProvisioningStatus pending) {
		if (sessions.containsKey(session.getSessionId())) {
			logger.debug("Session {} already registered", session.getSessionId());
			return;
		} else {
			logger.info("Registering new session: {}", session.getSessionId());
			sessions.put(session.getSessionId(), new SessionData(session, pending));
		}

	}

	@Override
	public Object updateAgentCertificate(final Session session, final X509Certificate identityCertificate) {
		// TODO controllare la validità del certificato
		registerSessionIfNeeded(session, ProvisioningStatus.APPROVED);
		return getSessionId(session);
	}

}
