package net.rossonet.waldot.api.configuration;

import java.io.Serializable;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;

/**
 * OpcConfiguration is an interface that defines the configuration settings for
 * an OPC server. It extends Serializable and Map<String, String> to allow for
 * easy serialization and access to configuration properties.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface OpcConfiguration extends Serializable, Map<String, String> {

	String getApplicationName();

	String getBindAddresses();

	String getBindHostname();

	DateTime getBuildDate();

	String getBuildNumber();

	String getDnsAddressCertificateGenerator();

	int getHttpsBindPort();

	String getManufacturerName();

	String getPath();

	String getProductName();

	String getProductUri();

	String getSecurityTempDir();

	int getTcpBindPort();

}
