package net.rossonet.waldot.configuration;

import java.util.HashMap;

import net.rossonet.waldot.api.configuration.OpcConfiguration;

public class OpcUaConfiguration extends HashMap<String, String> implements OpcConfiguration {

	private static final long serialVersionUID = 1487462093576743910L;

	public static int DEFAULT_TCP_BIND_PORT = 12686;
	public static int DEFAULT_HTTPS_BIND_PORT = 8443;

	public static OpcUaConfiguration getDefault() {
		return new OpcUaConfiguration();
	}

	private OpcUaConfiguration() {
	}

	@Override
	public String getApplicationName() {
		return "WaldOT opcua server";
	}

	@Override
	public String getBindAddresses() {
		return "127.0.0.1";
	}

	@Override
	public String getBindHostname() {
		return "127.0.0.1";
	}

	@Override
	public String getDnsAddressCertificateGenerator() {
		// return "0.0.0.0";
		return "127.0.0.1";
	}

	@Override
	public int getHttpsBindPort() {
		return DEFAULT_HTTPS_BIND_PORT;
	}

	@Override
	public String getManufacturerName() {
		return "Rossonet s.c.a r.l.";
	}

	@Override
	public String getPath() {
		return "/waldot";
	}

	@Override
	public String getProductName() {
		return "WaldOT";
	}

	@Override
	public String getProductUri() {
		return "urn:rossonet:waldot:uaserver";
	}

	@Override
	public int getTcpBindPort() {
		return DEFAULT_TCP_BIND_PORT;
	}

}
