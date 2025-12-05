package net.rossonet.zenoh.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.rossonet.waldot.dtdl.DtdlHandler;
import net.rossonet.waldot.dtdl.TelemetryObject;

public class TelemetryData implements Serializable {

	private static final long serialVersionUID = -5410105221655456698L;

	public static Map<String, TelemetryData> fromDtml(DtdlHandler dtmlHandler) {

		// TODO Auto-generated method stub
		return new HashMap<String, TelemetryData>();
	}

	private final long uniqueId;

	public TelemetryData() {
		uniqueId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
	}

	public TelemetryObject generateDtmlTelemetryObject() {
		// TODO Auto-generated method stub
		return null;
	}

	public long getUniqueId() {
		return uniqueId;
	}

}
