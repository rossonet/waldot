package net.rossonet.waldot.gremlin.opcgraph.strategies.client;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.milo.opcua.sdk.core.QualifiedProperty;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.AccessContext;
import org.eclipse.milo.opcua.sdk.server.Session;
import org.eclipse.milo.opcua.sdk.server.identity.Identity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.AnonymousIdentity;
import org.eclipse.milo.opcua.sdk.server.identity.Identity.UsernameIdentity;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.methods.MethodInvocationHandler;
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

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.api.strategies.ClientManagementStrategy;
import net.rossonet.waldot.client.auth.ClientRegisterAnonymousValidator;
import net.rossonet.waldot.client.auth.ClientRegisterUsernameIdentityValidator;
import net.rossonet.waldot.client.auth.ClientRegisterX509IdentityValidator;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;

public class BaseClientManagementStrategy implements ClientManagementStrategy {

	private class GenerateAddProvisioningMethodInvocationHandler extends TemplateMethodInvocationHandler {

		public GenerateAddProvisioningMethodInvocationHandler(final UaMethodNode methodNode,
				final List<Argument> inputArguments) {
			super(methodNode, inputArguments, new ArrayList<>());
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

	private class GenerateDeleteProvisioningMethodInvocationHandler extends TemplateMethodInvocationHandler {

		public GenerateDeleteProvisioningMethodInvocationHandler(final UaMethodNode methodNode,
				final List<Argument> inputArguments) {
			super(methodNode, inputArguments, new ArrayList<>());
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

	private class GenerateRpcProvisioningManualRequestMethodInvocationHandler extends TemplateMethodInvocationHandler {

		public GenerateRpcProvisioningManualRequestMethodInvocationHandler(final UaMethodNode methodNode,
				final List<Argument> inputArguments, List<Argument> outputArguments) {
			super(methodNode, inputArguments, outputArguments);
		}

		@Override
		protected Variant[] invoke(final InvocationContext invocationContext, final Variant[] inputValues)
				throws UaException {
			try {
				return rpcProvisioningManualRequestMethodAction(inputValues);
			} catch (final Exception e) {
				logger.error("Error requesting provisioning manual approval", e);
				return new Variant[0];
			}
		}
	}

	private class GenerateRpcProvisioningTokenRequestMethodInvocationHandler extends TemplateMethodInvocationHandler {

		public GenerateRpcProvisioningTokenRequestMethodInvocationHandler(final UaMethodNode methodNode,
				final List<Argument> inputArguments, List<Argument> outputArguments) {
			super(methodNode, inputArguments, outputArguments);
		}

		@Override
		protected Variant[] invoke(final InvocationContext invocationContext, final Variant[] inputValues)
				throws UaException {
			try {
				return rpcProvisioningTokenRequestMethodAction(inputValues);
			} catch (final Exception e) {
				logger.error("Error requesting provisioning token", e);
				return new Variant[0];
			}
		}
	}

	public enum ProvisioningStatus {
		APPROVED, PENDING, REJECTED
	}

	public final class ProvisioningToken {
		private final String id;
		private final NodeId nodeId;
		private final String secret;

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

	}

	private abstract class TemplateMethodInvocationHandler extends AbstractMethodInvocationHandler {

		private final List<Argument> inputArguments;

		private final List<Argument> outputArguments;

		public TemplateMethodInvocationHandler(UaMethodNode methodNode, final List<Argument> inputArguments,
				List<Argument> outputArguments) {
			super(methodNode);
			this.inputArguments = inputArguments;
			this.outputArguments = outputArguments;
		}

		@Override
		public Argument[] getInputArguments() {
			return inputArguments.toArray(new Argument[0]);
		}

		@Override
		public Argument[] getOutputArguments() {
			return outputArguments.toArray(new Argument[0]);
		}

	}

	private static final String PROVISIONING_CRT_CHAIN = "provisioning-chain";
	private static final String PROVISIONING_CRT_CHAIN_DESCRIPTION = "certificate issued for the agent's certificate with all the chain";
	private static final String PROVISIONING_CSR = "provisioning-csr";
	private static final String PROVISIONING_CSR_DESCRIPTION = "certificate signing request (CSR) for the agent's certificate";
	private static final String PROVISIONING_ID = "provisioning-id";
	private static final String PROVISIONING_ID_DESCRIPTION = "unique identifier of the provisioning token";
	private static final String PROVISIONING_NOTE = "provisioning-note";
	private static final String PROVISIONING_NOTE_DESCRIPTION = "note about the provisioning request";
	private static final String PROVISIONING_SECRET = "provisioning-secret";
	private static final String PROVISIONING_SECRET_DESCRIPTION = "provisioning secret token ( password)";
	private static final String PROVISIONING_STATUS = "provisioning-status";
	private static final String PROVISIONING_STATUS_DESCRIPTION = "status of the provisioning request";
	private static final String PROVISIONING_UNIQUE_ID = "provisioning-unique-id";
	private static final String PROVISIONING_UNIQUE_ID_DESCRIPTION = "unique identifier of the provisioning session visualized in the client";
	private ClientRegisterAnonymousValidator agentAnonymousValidator;
	private UaFolderNode agentFolder;
	private ClientRegisterUsernameIdentityValidator agentIdentityValidator;
	private UaObjectNode agentLifeCycleRecord;
	private ClientRegisterX509IdentityValidator agentX509IdentityValidator;
	private UaFolderNode assetRootNode;
	private UaFolderNode clientFolder;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private UaObjectNode managementRecord;
	private UaMethodNode provisioningAddTokenMethod;
	private UaMethodNode provisioningDeleteTokenMethod;
	private UaFolderNode provisioningFolder;
	private UaMethodNode provisioningRpcManualRequest;
	private UaFolderNode rpcFolder;

	private final Map<NodeId, SessionData> sessions = new HashMap<>();

	private final Map<String, ProvisioningToken> tokenSecrets = new HashMap<>();
	private WaldotNamespace waldotNamespace;

	@Override
	public void activate(final ClientRegisterAnonymousValidator agentAnonymousValidator,
			final ClientRegisterUsernameIdentityValidator agentIdentityValidator,
			final ClientRegisterX509IdentityValidator agentX509IdentityValidator) {
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
		final Argument id = new Argument(PROVISIONING_ID, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(PROVISIONING_ID_DESCRIPTION));
		inputArguments.add(id);
		final Argument secret = new Argument(PROVISIONING_SECRET, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_SECRET_DESCRIPTION));
		inputArguments.add(secret);
		provisioningAddTokenMethod.setInputArguments(inputArguments.toArray(new Argument[0]));
		provisioningAddTokenMethod.setOutputArguments(new Argument[0]);
		provisioningAddTokenMethod.setInvocationHandler(
				new GenerateAddProvisioningMethodInvocationHandler(provisioningAddTokenMethod, inputArguments));
		waldotNamespace.getStorageManager().addNode(provisioningAddTokenMethod);
		provisioningAddTokenMethod.addReference(new Reference(provisioningAddTokenMethod.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		managementRecord.addComponent(provisioningAddTokenMethod);
	}

	private void generateAgentFolder() {
		agentFolder = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("asset.agents"),
				waldotNamespace.generateQualifiedName("Agent Management"),
				LocalizedText.english("Agents Management folder"));
		waldotNamespace.getStorageManager().addNode(agentFolder);
		assetRootNode.addOrganizes(agentFolder);
	}

	@Override
	public void generateAssetFolders(final UaFolderNode assetRootNode) {
		this.assetRootNode = assetRootNode;
		generateAgentFolder();
		generateClientFolder();
		generateProvisioningFolder();
		generateRpcFolder();

		generateRpcProvisioningManualRequest();
		generateRpcProvisioningTokenRequest();
		generateAddProvisioningTokenMethod();
		generateDeleteProvisioningTokenMethod();

	}

	private void generateClientFolder() {
		clientFolder = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("asset.clients"),
				waldotNamespace.generateQualifiedName("Client Management"),
				LocalizedText.english("Clients Management folder"));
		waldotNamespace.getStorageManager().addNode(clientFolder);
		assetRootNode.addOrganizes(clientFolder);
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
		final Argument id = new Argument(PROVISIONING_ID, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(PROVISIONING_ID_DESCRIPTION));
		inputArguments.add(id);
		provisioningDeleteTokenMethod.setInputArguments(inputArguments.toArray(new Argument[0]));
		provisioningDeleteTokenMethod.setOutputArguments(new Argument[0]);
		provisioningDeleteTokenMethod.setInvocationHandler(
				new GenerateDeleteProvisioningMethodInvocationHandler(provisioningDeleteTokenMethod, inputArguments));
		waldotNamespace.getStorageManager().addNode(provisioningDeleteTokenMethod);
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
					return new CallMethodResult(StatusCode.GOOD, null, null, null);
				} catch (final Exception e) {
					logger.error("Error deleting provisioning token with ID: {}", id, e);
					return new CallMethodResult(StatusCode.BAD, null, null, null);

				}
			}

		});
		waldotNamespace.getStorageManager().addNode(provisioningDeleteTokenMethod);
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
		clientFolder.addOrganizes(provisioningFolder);
		final NodeId nodeId = waldotNamespace.generateNodeId("token-management");
		managementRecord = new UaObjectNode(waldotNamespace.getOpcUaNodeContext(), nodeId,
				waldotNamespace.generateQualifiedName("Token Management"), LocalizedText.english("Token Management"),
				LocalizedText.english("Token Management"), UInteger.MIN, UInteger.MIN);
		waldotNamespace.getStorageManager().addNode(managementRecord);
		managementRecord.addReference(new Reference(managementRecord.getNodeId(), Identifiers.HasTypeDefinition,
				Identifiers.BaseObjectType.expanded(), true));
		provisioningFolder.addComponent(managementRecord);
	}

	private NodeId generateProvisioningTokenRecord(final String id, final String secret) {
		final NodeId nodeId = waldotNamespace.generateNodeId("pt-" + id);
		final UaObjectNode provisioningRecord = new UaObjectNode(waldotNamespace.getOpcUaNodeContext(), nodeId,
				waldotNamespace.generateQualifiedName(id), LocalizedText.english("ID " + id),
				LocalizedText.english("Provisioning Token " + id), UInteger.MIN, UInteger.MIN);
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

	private void generateRpcFolder() {
		rpcFolder = new UaFolderNode(waldotNamespace.getOpcUaNodeContext(), waldotNamespace.generateNodeId("agent-rpc"),
				waldotNamespace.generateQualifiedName("Agent RPC folder"),
				LocalizedText.english("Folder for Agent RPC methods"));
		waldotNamespace.getStorageManager().addNode(rpcFolder);
		clientFolder.addOrganizes(rpcFolder);
		final NodeId nodeId = waldotNamespace.generateNodeId("lifecycle");
		agentLifeCycleRecord = new UaObjectNode(waldotNamespace.getOpcUaNodeContext(), nodeId,
				waldotNamespace.generateQualifiedName("Life Cycle Manager"),
				LocalizedText.english("Life Cycle Manager"), LocalizedText.english("Life Cycle Manager"), UInteger.MIN,
				UInteger.MIN);

		waldotNamespace.getStorageManager().addNode(agentLifeCycleRecord);
		agentLifeCycleRecord.addReference(new Reference(agentLifeCycleRecord.getNodeId(), Identifiers.HasTypeDefinition,
				Identifiers.BaseObjectType.expanded(), true));
		provisioningFolder.addComponent(agentLifeCycleRecord);
	}

	private void generateRpcProvisioningManualRequest() {
		final Boolean userExecutable = Boolean.TRUE;
		final Boolean executable = Boolean.TRUE;
		final UInteger writeMask = UInteger.valueOf(WriteMask.Executable.getValue());
		final UInteger userWriteMask = UInteger.valueOf(WriteMask.Executable.getValue());
		provisioningRpcManualRequest = new UaMethodNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("provisioning-manual-request"),
				waldotNamespace.generateQualifiedName("Create Provisioning Manual Request"),
				LocalizedText.english("Create Provisioning Manual Request"),
				LocalizedText.english("RPC for Manual Request Provisioning"), writeMask, userWriteMask, executable,
				userExecutable);
		final List<Argument> inputArguments = new ArrayList<>();
		final Argument unique = new Argument(PROVISIONING_UNIQUE_ID, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_UNIQUE_ID_DESCRIPTION));
		inputArguments.add(unique);
		final Argument id = new Argument(PROVISIONING_ID, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(PROVISIONING_ID_DESCRIPTION));
		inputArguments.add(id);
		final List<Argument> outputArguments = new ArrayList<>();
		final Argument generatedToken = new Argument(PROVISIONING_SECRET, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_SECRET_DESCRIPTION));
		outputArguments.add(generatedToken);
		final Argument provisioningStatus = new Argument(PROVISIONING_STATUS, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_STATUS_DESCRIPTION));
		outputArguments.add(provisioningStatus);
		final Argument provisioningNote = new Argument(PROVISIONING_NOTE, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_NOTE_DESCRIPTION));
		outputArguments.add(provisioningNote);
		provisioningRpcManualRequest.setOutputArguments(outputArguments.toArray(new Argument[0]));
		provisioningRpcManualRequest.setInvocationHandler(
				new GenerateRpcProvisioningManualRequestMethodInvocationHandler(provisioningRpcManualRequest,
						inputArguments, outputArguments));
		waldotNamespace.getStorageManager().addNode(provisioningRpcManualRequest);
		provisioningRpcManualRequest.addReference(new Reference(provisioningRpcManualRequest.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		rpcFolder.addComponent(provisioningRpcManualRequest);
	}

	private void generateRpcProvisioningTokenRequest() {
		final Boolean userExecutable = Boolean.TRUE;
		final Boolean executable = Boolean.TRUE;
		final UInteger writeMask = UInteger.valueOf(WriteMask.Executable.getValue());
		final UInteger userWriteMask = UInteger.valueOf(WriteMask.Executable.getValue());
		provisioningRpcManualRequest = new UaMethodNode(waldotNamespace.getOpcUaNodeContext(),
				waldotNamespace.generateNodeId("provisioning-manual-request"),
				waldotNamespace.generateQualifiedName("Create Provisioning Manual Request"),
				LocalizedText.english("Create Provisioning Manual Request"),
				LocalizedText.english("RPC for Manual Request Provisioning"), writeMask, userWriteMask, executable,
				userExecutable);
		final List<Argument> inputArguments = new ArrayList<>();
		final Argument id = new Argument(PROVISIONING_ID, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(PROVISIONING_ID_DESCRIPTION));
		inputArguments.add(id);
		final Argument secret = new Argument(PROVISIONING_SECRET, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_SECRET_DESCRIPTION));
		inputArguments.add(secret);
		final Argument csr = new Argument(PROVISIONING_CSR, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar,
				null, LocalizedText.english(PROVISIONING_CSR_DESCRIPTION));
		inputArguments.add(csr);
		final List<Argument> outputArguments = new ArrayList<>();

		final Argument provisioningStatus = new Argument(PROVISIONING_STATUS, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_STATUS_DESCRIPTION));
		outputArguments.add(provisioningStatus);
		final Argument provisioningNote = new Argument(PROVISIONING_NOTE, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_NOTE_DESCRIPTION));
		outputArguments.add(provisioningNote);
		final Argument provisioningCrt = new Argument(PROVISIONING_CRT_CHAIN, VariableNodeTypes.String.getNodeId(),
				ValueRanks.Scalar, null, LocalizedText.english(PROVISIONING_CRT_CHAIN_DESCRIPTION));
		outputArguments.add(provisioningCrt);
		provisioningRpcManualRequest.setOutputArguments(outputArguments.toArray(new Argument[0]));
		provisioningRpcManualRequest.setInvocationHandler(
				new GenerateRpcProvisioningTokenRequestMethodInvocationHandler(provisioningRpcManualRequest,
						inputArguments, outputArguments));
		waldotNamespace.getStorageManager().addNode(provisioningRpcManualRequest);
		provisioningRpcManualRequest.addReference(new Reference(provisioningRpcManualRequest.getNodeId(),
				Identifiers.HasModellingRule, Identifiers.ModellingRule_Mandatory.expanded(), true));
		rpcFolder.addComponent(provisioningRpcManualRequest);
	}

	@Override
	public UaFolderNode getAssetClientsFolderNode() {
		return agentFolder;
	}

	private Identity getSessionIdentity(final Session session) {
		if (session == null) {
			logger.warn("Session is null, cannot get session ID");
			return null;
		}
		if (sessions.containsKey(session.getSessionId())) {
			return session.getIdentity();
		} else {
			logger.warn("Session {} not found in registered sessions", session.getSessionId());
			throw new IllegalStateException("Session " + session.getSessionId() + " not found in registered sessions");
		}
	}

	@Override
	public void initialize(final WaldotNamespace waldotNamespace) {
		this.waldotNamespace = waldotNamespace;
		logger.info("Agent Management Strategy initialized");
	}

	@Override
	public AnonymousIdentity registerNewClientForApproval(final Session session) {
		registerSessionIfNeeded(session, ProvisioningStatus.PENDING);
		logger.info("New agent registered for approval: {}", session.getSessionId());
		return (AnonymousIdentity) getSessionIdentity(session);
	}

	@Override
	public UsernameIdentity registerNewClientWithProvisioningPassword(final Session session,
			final UserNameIdentityToken token) {
		// TODO verificare il token
		registerSessionIfNeeded(session, ProvisioningStatus.APPROVED);
		return (UsernameIdentity) getSessionIdentity(session);
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

	public Variant[] rpcProvisioningManualRequestMethodAction(Variant[] inputValues) {
		// TODO Auto-generated method stub
		return null;
	}

	public Variant[] rpcProvisioningTokenRequestMethodAction(Variant[] inputValues) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object updateClientCertificate(final Session session, final X509Certificate identityCertificate) {
		// TODO controllare la validità del certificato
		registerSessionIfNeeded(session, ProvisioningStatus.APPROVED);
		return session.getSessionId();
	}

}
