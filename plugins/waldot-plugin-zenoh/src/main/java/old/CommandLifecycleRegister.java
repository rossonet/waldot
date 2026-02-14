package old;

import java.time.Instant;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.opc.AbstractOpcCommand;
import net.rossonet.waldot.opc.AbstractOpcCommand.VariableNodeTypes;
import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.zenoh.api.message.RpcCommand;

public class CommandLifecycleRegister {

	private static final @Nullable String AGENT_ID_DESCRIPTION = "identifier of the agent";
	private static final String AGENT_ID_LABEL = "agentId";
	private static final @Nullable String CALL_TIME_MS_DESCRIPTION = "timestamp in milliseconds when the command was called";
	private static final String CALL_TIME_MS_LABEL = "callTimeMs";
	private static final @Nullable String COMMAND_ID_DESCRIPTION = "identifier of the command";
	private static final String COMMAND_ID_LABEL = "commandId";
	private static final @Nullable String COMPLETED_DESCRIPTION = "indicates if the command execution is completed";
	private static final String COMPLETED_LABEL = "completed";
	private static final @Nullable String ERROR_MESSAGE_DESCRIPTION = "error message in case of command execution failure";
	private static final String ERROR_MESSAGE_LABEL = "errorMessage";
	private final static Logger logger = LoggerFactory.getLogger(CommandLifecycleRegister.class);
	private static final @Nullable String REPLY_MESSAGE_DESCRIPTION = "message returned by the agent as reply";
	private static final String REPLY_MESSAGE_LABEL = "replyMessage";
	private static final @Nullable String REPLY_TIME_MS_DESCRIPTION = "timestamp in milliseconds when the command reply was received";
	private static final String REPLY_TIME_MS_LABEL = "replyTimeMs";
	private static final @Nullable String RPC_UNIQUE_ID_DESCRIPTION = "unique identifier of the RPC command";
	private static final String RPC_UNIQUE_ID_LABEL = "rpcUniqueId";
	private static final @Nullable String STACK_TRACE_DESCRIPTION = "stack trace in case of command execution failure";
	private static final String STACK_TRACE_LABEL = "stackTrace";
	private static final @Nullable String SUCCESS_DESCRIPTION = "indicates if the command execution was successful";
	private static final String SUCCESS_LABEL = "success";

	public static void addStandardAgentCommandOutputArguments(AbstractOpcCommand command) {
		command.addOutputArgument(AGENT_ID_LABEL, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(AGENT_ID_DESCRIPTION));
		command.addOutputArgument(COMMAND_ID_LABEL, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(COMMAND_ID_DESCRIPTION));
		command.addOutputArgument(RPC_UNIQUE_ID_LABEL, VariableNodeTypes.Int64.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(RPC_UNIQUE_ID_DESCRIPTION));
		command.addOutputArgument(COMPLETED_LABEL, VariableNodeTypes.Boolean.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(COMPLETED_DESCRIPTION));
		command.addOutputArgument(SUCCESS_LABEL, VariableNodeTypes.Boolean.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(SUCCESS_DESCRIPTION));
		command.addOutputArgument(REPLY_MESSAGE_LABEL, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(REPLY_MESSAGE_DESCRIPTION));
		command.addOutputArgument(ERROR_MESSAGE_LABEL, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(ERROR_MESSAGE_DESCRIPTION));
		command.addOutputArgument(CALL_TIME_MS_LABEL, VariableNodeTypes.Int64.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(CALL_TIME_MS_DESCRIPTION));
		command.addOutputArgument(REPLY_TIME_MS_LABEL, VariableNodeTypes.Int64.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(REPLY_TIME_MS_DESCRIPTION));
		command.addOutputArgument(STACK_TRACE_LABEL, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(STACK_TRACE_DESCRIPTION));

	}

	private final String agentId;
	private final long callTimeMs;
	private final String commandId;
	private boolean completed = false;
	private final Condition condition;
	private String errorMessage = null;
	private final ReentrantLock reentrantLock;
	private JSONObject replyMessage = null;

	private long replyTimeMs = -1;

	private final long rpcUniqueId;
	private String stackTrace = null;
	private boolean success = false;

	public CommandLifecycleRegister(RpcCommand rpcCommand) {
		commandId = rpcCommand.getCommandId();
		rpcUniqueId = rpcCommand.getUniqueId();
		agentId = rpcCommand.getAgentId();
		callTimeMs = Instant.now().toEpochMilli();
		reentrantLock = new ReentrantLock();
		condition = reentrantLock.newCondition();
	}

	public CommandLifecycleRegister(RpcCommand rpcCommand, Exception e) {
		this(rpcCommand);
		this.errorMessage = e.getMessage();
		this.success = false;
		this.completed = true;
		this.replyTimeMs = Instant.now().toEpochMilli();
		this.replyMessage = new JSONObject();
		this.stackTrace = LogHelper.stackTraceToString(e);
	}

	public CommandLifecycleRegister(String agentId, String commandName, String[] inputValues, Exception e) {
		commandId = commandName;
		rpcUniqueId = 0;
		this.agentId = agentId;
		callTimeMs = Instant.now().toEpochMilli();
		reentrantLock = new ReentrantLock();
		condition = reentrantLock.newCondition();
	}

	public String getAgentId() {
		return agentId;
	}

	public long getCallTimeMs() {
		return callTimeMs;
	}

	public String getCommandId() {
		return commandId;
	}

	public Condition getCompleteCondition() {
		return condition;

	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public Object[] getOutputValues() {
		logger.debug("CommandLifecycleRegister getOutputValues called for commandId {} rpcUniqueId {}", commandId,
				rpcUniqueId);

		return new Object[] { agentId, commandId, rpcUniqueId, completed, success,
				replyMessage != null ? replyMessage.toString() : null, errorMessage, callTimeMs, replyTimeMs,
				stackTrace };

	}

	public ReentrantLock getReentrantLock() {
		return reentrantLock;
	}

	public JSONObject getReplyMessage() {
		return replyMessage;
	}

	public long getReplyTimeMs() {
		return replyTimeMs;
	}

	public long getRpcUniqueId() {
		return rpcUniqueId;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public boolean isCompleted() {
		return completed;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setReplyMessage(JSONObject replyMessage) {
		this.replyMessage = replyMessage;
	}

	public void setReplyTimeMs(long replyTimeMs) {
		this.replyTimeMs = replyTimeMs;
	}

	public void setStackTrace(String stackTrace) {
		this.stackTrace = stackTrace;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CommandLifecycleRegister [agentId=");
		builder.append(agentId);
		builder.append(", commandId=");
		builder.append(commandId);
		builder.append(", rpcUniqueId=");
		builder.append(rpcUniqueId);
		builder.append(", completed=");
		builder.append(completed);
		builder.append(", success=");
		builder.append(success);
		builder.append(", replyMessage=");
		builder.append(replyMessage);
		builder.append(", errorMessage=");
		builder.append(errorMessage);
		builder.append(", callTimeMs=");
		builder.append(callTimeMs);
		builder.append(", replyTimeMs=");
		builder.append(replyTimeMs);
		builder.append(", stackTrace=");
		builder.append(stackTrace);
		builder.append("]");
		return builder.toString();
	}

}
