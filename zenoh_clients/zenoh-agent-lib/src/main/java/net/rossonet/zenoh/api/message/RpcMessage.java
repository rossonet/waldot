package net.rossonet.zenoh.api.message;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONObject;

public final class RpcMessage implements Serializable {

	private static final String RPC_ID_FIELD = "u";
	private static final String RPC_INPUT_FIELD = "i";
	private static final String RPC_OUTPUT_FIELD = "o";
	private static final long serialVersionUID = -1966997465117341867L;

	public static final RpcMessage fromJson(JSONObject jsonObject) {
		final long id = jsonObject.getLong(RPC_ID_FIELD);
		final String type = jsonObject.getString(RPC_INPUT_FIELD);
		final Object v = jsonObject.get(RPC_OUTPUT_FIELD);
//TODO: completare
		return null;

	}

	private final String commandId;

	private final String[] inputValues;
	private String[] outputValues;

	public RpcMessage(RpcMessage input, String[] outputValues) {
		this(input.commandId, input.inputValues);
		this.outputValues = outputValues;
	}

	public RpcMessage(String commandId, String[] inputValues) {
		this.commandId = commandId;
		this.inputValues = inputValues;

	}

	public RpcMessage(String commandId, String[] inputValues, String[] outputValues) {
		this(commandId, inputValues);
		this.outputValues = outputValues;
	}

	public String[] getOutputValues() {
		return outputValues;
	}

	public void setOutputValues(String[] outputValues) {
		this.outputValues = outputValues;
	}

	public JSONObject toJson() {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put(RPC_ID_FIELD, commandId);
		if (inputValues != null && inputValues.length > 0) {
			jsonObject.put(RPC_INPUT_FIELD, valuesToJson(inputValues));
		}
		if (outputValues != null && outputValues.length > 0) {
			jsonObject.put(RPC_OUTPUT_FIELD, valuesToJson(outputValues));
		}
		return jsonObject;
	}

	private JSONArray valuesToJson(String[] array) {
		// TODO Auto-generated method stub
		return null;
	}

}
