package net.rossonet.zenoh.api.message;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;

import net.rossonet.waldot.utils.LogHelper;
import net.rossonet.zenoh.exception.ExecutionCommandException;
import net.rossonet.zenoh.exception.ZenohSerializationException;

public final class RpcCommand implements Serializable {

	private static final String KEY_FIELD = "k";
	private static final String RPC_AGENT_FIELD = "a";
	private static final String RPC_COMMAND_ID_FIELD = "x";
	private static final String RPC_EXCEPTION_FIELD = "z";
	private static final String RPC_INPUT_FIELD = "i";
	private static final String RPC_OUTPUT_FIELD = "o";
	private static final String RPC_RELATED_FIELD = "r";
	private static final String RPC_UNIQUE_FIELD = "u";
	private static final long serialVersionUID = -2725272535926420176L;
	private static final String VALUE_FIELD = "v";

	public static final RpcCommand fromJson(JSONObject jsonObject) throws ZenohSerializationException {
		final String c = jsonObject.getString(RPC_COMMAND_ID_FIELD);
		final String a = jsonObject.getString(RPC_AGENT_FIELD);
		final long uid = jsonObject.getLong(RPC_UNIQUE_FIELD);
		if (uid == 0) {
			throw new ZenohSerializationException("RPC Unique Id cannot be zero");
		}
		final long rel = jsonObject.optLong(RPC_RELATED_FIELD);
		final JSONArray in = jsonObject.getJSONArray(RPC_INPUT_FIELD);
		final JSONObject outjson = jsonObject.optJSONObject(RPC_OUTPUT_FIELD);
		final Map<String, Object> invalues = jsonToValues(in);
		final JSONObject ex = jsonObject.optJSONObject(RPC_EXCEPTION_FIELD);
		ExecutionCommandException executionCommandException = null;
		if (ex != null) {
			executionCommandException = ExecutionCommandException.fromJson(ex);
		}
		return new RpcCommand(uid, a, c, invalues, outjson, rel, executionCommandException);

	}

	private static long generateRandomLong() {
		return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
	}

	private static Map<String, Object> jsonToValues(JSONArray array) {
		final Map<String, Object> values = new HashMap<>();
		if (array == null) {
			return values;
		}
		for (int i = 0; i < array.length(); i++) {
			final JSONObject jsonObject = array.getJSONObject(i);
			final String key = jsonObject.getString(KEY_FIELD);
			final JSONObject value = jsonObject.getJSONObject(VALUE_FIELD);
			final Object objValue = JsonIterator.deserialize(value.toString());
			values.put(key, objValue);
		}
		return values;
	}

	private final String agentId;

	private final String commandId;

	private final ExecutionCommandException executionCommandException;

	private final Map<String, Object> inputValues;
	private JSONObject outputValue;

	private long relatedId;

	private final long uniqueId;

	private RpcCommand(long uniqueId, String targetAgentId, String commandId, Map<String, Object> inputValues,
			JSONObject outputValue, long rpcRelatedId, ExecutionCommandException executionCommandException) {
		this.agentId = targetAgentId;
		this.commandId = commandId;
		this.inputValues = inputValues;
		this.uniqueId = uniqueId;
		this.relatedId = rpcRelatedId;
		this.outputValue = outputValue;
		this.executionCommandException = executionCommandException;
	}

	public RpcCommand(RpcCommand input, ExecutionCommandException executionCommandException) {
		this(generateRandomLong(), input.getAgentId(), input.commandId, input.inputValues, null, input.uniqueId,
				executionCommandException);
	}

	public RpcCommand(RpcCommand input, JSONObject outputValue) {
		this(generateRandomLong(), input.getAgentId(), input.commandId, input.inputValues, outputValue, input.uniqueId,
				null);
	}

	public RpcCommand(String targetAgentId, String commandId, Map<String, Object> inputValues) {
		this(generateRandomLong(), targetAgentId, commandId, inputValues, null, 0, null);
	}

	public RpcCommand(String targetAgentId, String commandId, Map<String, Object> inputValues, JSONObject outputValues,
			long rpcRelatedId, ExecutionCommandException executionCommandException) {
		this(generateRandomLong(), targetAgentId, commandId, inputValues, outputValues, rpcRelatedId,
				executionCommandException);
	}

	public String getAgentId() {
		return agentId;
	}

	public String getCommandId() {
		return commandId;
	}

	public ExecutionCommandException getExecutionCommandException() {
		return executionCommandException;
	}

	public Map<String, Object> getInputValues() {
		return inputValues;
	}

	public Object[] getParameterInputValues() {
		return inputValues.values().toArray();
	}

	public long getRelatedId() {
		return relatedId;
	}

	public JSONObject getReplyMessage() {
		return outputValue;
	}

	public long getUniqueId() {
		return uniqueId;
	}

	public void setRelatedId(long relatedId) {
		this.relatedId = relatedId;
	}

	public void setReplyMessage(JSONObject message) {
		this.outputValue = message;
	}

	public void setRpcRelatedId(long rpcInputId) {
		this.relatedId = rpcInputId;
	}

	public JSONObject toJson() {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put(RPC_COMMAND_ID_FIELD, commandId);
		jsonObject.put(RPC_UNIQUE_FIELD, uniqueId);
		jsonObject.put(RPC_AGENT_FIELD, agentId);
		if (relatedId != 0) {
			jsonObject.put(RPC_RELATED_FIELD, relatedId);
		}
		if (executionCommandException != null) {
			jsonObject.put(RPC_EXCEPTION_FIELD, executionCommandException.toJson());
		}
		jsonObject.put(RPC_INPUT_FIELD, valuesToJson(inputValues));
		if (outputValue != null && !outputValue.isEmpty()) {
			jsonObject.put(RPC_OUTPUT_FIELD, outputValue);
		}
		return jsonObject;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RpcCommand [uniqueId=");
		builder.append(uniqueId);
		builder.append(", agentId=");
		builder.append(agentId);
		builder.append(", commandId=");
		builder.append(commandId);
		builder.append(", relatedId=");
		builder.append(relatedId);
		builder.append(", inputValues=");
		builder.append(inputValues != null ? toString(inputValues.entrySet(), 20) : null);
		builder.append(", outputValue=");
		builder.append(outputValue);
		builder.append(", executionCommandException=");
		builder.append(
				executionCommandException != null ? LogHelper.stackTraceToString(executionCommandException) : null);
		builder.append("]");
		return builder.toString();
	}

	private String toString(Collection<?> collection, int maxLen) {
		final StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (final Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}

	private JSONArray valuesToJson(Map<String, Object> values) {
		final JSONArray array = new JSONArray();
		for (final Map.Entry<String, Object> entry : values.entrySet()) {
			final JSONObject record = new JSONObject();
			record.put(KEY_FIELD, entry.getKey());
			final JSONObject json = new JSONObject(JsonStream.serialize(entry.getValue()));
			record.put(VALUE_FIELD, json);
			array.put(record);
		}
		return array;
	}

}
