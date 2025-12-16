package net.rossonet.zenoh.api.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;

import net.rossonet.zenoh.exception.ZenohSerializationException;

public final class RpcConfiguration implements Serializable {

	private static final String KEY_FIELD = "k";
	private static final String RPC_CONFIGURATION_ID_FIELD = "x";
	private static final String RPC_DELETE_FIELD = "d";
	private static final String RPC_EPOCH_FIELD = "r";
	private static final String RPC_PROPERTIES_FIELD = "p";
	private static final String RPC_UNIQUE_FIELD = "u";
	private static final long serialVersionUID = 481783288298046237L;
	private static final String VALUE_FIELD = "v";

	public static final RpcConfiguration fromJson(JSONObject jsonObject) throws ZenohSerializationException {
		final String c = jsonObject.getString(RPC_CONFIGURATION_ID_FIELD);
		final long uid = jsonObject.getLong(RPC_UNIQUE_FIELD);
		if (uid == 0) {
			throw new ZenohSerializationException("RPC Unique Id cannot be zero");
		}
		final long epoch = jsonObject.getLong(RPC_EPOCH_FIELD);
		final JSONArray jsonValues = jsonObject.getJSONArray(RPC_PROPERTIES_FIELD);
		final Map<String, Object> values = jsonToValues(jsonValues);
		final boolean d = jsonObject.getBoolean(RPC_DELETE_FIELD);
		return new RpcConfiguration(uid, epoch, c, values, d);
	}

	private static Map<String, Object> jsonToValues(JSONArray array) {
		final Map<String, Object> values = new HashMap<>();
		for (int i = 0; i < array.length(); i++) {
			final JSONObject jsonObject = array.getJSONObject(i);
			final String key = jsonObject.getString(KEY_FIELD);
			final JSONObject value = jsonObject.getJSONObject(VALUE_FIELD);
			final Object objValue = JsonIterator.deserialize(value.toString());
			values.put(key, objValue);
		}
		return values;
	}

	private final String configurationId;

	private final boolean deleteMessage;
	private final long epoch;
	private final long uniqueId;
	private final Map<String, Object> values;

	public RpcConfiguration(long uniqueId, long epoch, String configurationId, Map<String, Object> values) {
		this(uniqueId, epoch, configurationId, values, false);

	}

	public RpcConfiguration(long uniqueId, long epoch, String configurationId, Map<String, Object> values,
			boolean deleteMessage) {
		this.configurationId = configurationId;
		this.values = values;
		this.uniqueId = uniqueId;
		this.epoch = epoch;
		this.deleteMessage = deleteMessage;
	}

	public String getConfigurationId() {
		return configurationId;
	}

	public long getEpoch() {
		return epoch;
	}

	public long getUniqueId() {
		return uniqueId;
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public boolean isDeleteMessage() {
		return deleteMessage;
	}

	public JSONObject toJson() {
		final JSONObject jsonObject = new JSONObject();
		jsonObject.put(RPC_CONFIGURATION_ID_FIELD, configurationId);
		jsonObject.put(RPC_UNIQUE_FIELD, uniqueId);
		jsonObject.put(RPC_EPOCH_FIELD, epoch);
		if (values != null && !values.isEmpty()) {
			jsonObject.put(RPC_PROPERTIES_FIELD, valuesToJson(values));
		}
		jsonObject.put(RPC_DELETE_FIELD, deleteMessage);
		return jsonObject;
	}

	private JSONArray valuesToJson(Map<String, Object> values) {
		final JSONArray array = new JSONArray();
		for (final Map.Entry<String, Object> entry : values.entrySet()) {
			final JSONObject record = new JSONObject();
			record.put(KEY_FIELD, entry.getKey());
			final JSONObject json = new JSONObject(JsonStream.serialize(entry.getValue()));
			record.put(VALUE_FIELD, json);
			final JSONObject keyObject = new JSONObject();
			array.put(keyObject);
		}
		return array;
	}

}
