package net.rossonet.waldot.configuration;

import java.util.HashMap;

import net.rossonet.waldot.api.Configuration;

public class OpcUaConfiguration extends HashMap<String, String> implements Configuration {

	private static final long serialVersionUID = 1487462093576743910L;

	public static int DEFAULT_TCP_BIND_PORT = 12686;
	public static int DEFAULT_HTTPS_BIND_PORT = 8443;

	public static OpcUaConfiguration getDefault() {
		return new OpcUaConfiguration();
	}

	private OpcUaConfiguration() {
	}

	public boolean getAnonymousAccessAllowed() {
		return true;
	}

	public String getApplicationName() {
		return "WaldOT opcua server";
	}

	public String getBindAddresses() {
		return "127.0.0.1";
	}

	public String getBindHostname() {
		return "127.0.0.1";
	}

	public String getDnsAddressCertificateGenerator() {
		// return "0.0.0.0";
		return "127.0.0.1";
	}

	public String getFactoryPassword() {
		return "password123";
	}

	public String getFactoryUsername() {
		return "admin";
	}

	public int getHttpsBindPort() {
		return DEFAULT_HTTPS_BIND_PORT;
	}

	public String getManufacturerName() {
		return "Rossonet s.c.a r.l.";
	}

	public String getPath() {
		return "/waldot";
	}

	public String getProductName() {
		return "WaldOT";
	}

	public String getProductUri() {
		return "urn:rossonet:waldot:uaserver";
	}

	public int getTcpBindPort() {
		return DEFAULT_TCP_BIND_PORT;
	}

}
