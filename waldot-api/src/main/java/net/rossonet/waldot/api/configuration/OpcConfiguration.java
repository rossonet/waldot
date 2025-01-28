package net.rossonet.waldot.api.configuration;

import java.io.Serializable;
import java.util.Map;

public interface OpcConfiguration extends Serializable, Map<String, String> {

	String getApplicationName();

	String getBindAddresses();

	String getBindHostname();

	String getDnsAddressCertificateGenerator();

	int getHttpsBindPort();

	String getManufacturerName();

	String getPath();

	String getProductName();

	String getProductUri();

	int getTcpBindPort();

}
