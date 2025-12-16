package net.rossonet.zenoh.controller.command;

import java.time.Instant;

import org.json.JSONObject;

import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.zenoh.api.message.RpcCommand;

public class SuspendedCommand {

	private final String agentId;
	private final long callTimeMs;
	private final String commandId;
	private boolean completed = false;
	private String errorMessage = null;
	private JSONObject replyMessage = null;
	private long replyTimeMs = -1;
	private final long rpcUniqueId;
	private String stackTrace = null;
	private boolean success = false;

	public SuspendedCommand(RpcCommand rpcCommand) {
		commandId = rpcCommand.getCommandId();
		rpcUniqueId = rpcCommand.getUniqueId();
		agentId = rpcCommand.getAgentId();
		callTimeMs = Instant.now().toEpochMilli();
	}

	public SuspendedCommand(RpcCommand rpcCommand, Exception e) {
		this(rpcCommand);
		this.errorMessage = e.getMessage();
		this.success = false;
		this.completed = true;
		this.replyTimeMs = Instant.now().toEpochMilli();
		this.replyMessage = new JSONObject();
		this.stackTrace = LogHelper.stackTraceToString(e);
	}

	public SuspendedCommand(String agentId, String commandName, String[] inputValues, Exception e) {
		commandId = commandName;
		rpcUniqueId = 0;
		this.agentId = agentId;
		callTimeMs = Instant.now().toEpochMilli();
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

	public String getErrorMessage() {
		return errorMessage;
	}

	public Object[] getOutputValues() {
		return new Object[] { agentId, commandId, rpcUniqueId, completed, success, replyMessage, errorMessage,
				callTimeMs, replyTimeMs, stackTrace };
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
		builder.append("SuspendedCommand [agentId=");
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
