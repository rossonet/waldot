package net.rossonet.waldot.api.configuration;

import java.io.Serializable;
import java.util.Map;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;

/**
 * OpcConfiguration is an interface that defines the configuration settings for
 * an OPC server. It extends Serializable and Map<String, String> to allow for
 * easy serialization and access to configuration properties.
 * 
 * <p>OpcConfiguration provides configuration settings specific to the OPC UA
 * server, including application identity, network binding, security, and
 * product information.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Get OPC configuration
 * OpcConfiguration opcConfig = namespace.getOpcConfiguration();
 * 
 * // Get server identity
 * String appName = opcConfig.getApplicationName();
 * String productName = opcConfig.getProductName();
 * String manufacturer = opcConfig.getManufacturerName();
 * 
 * // Get network settings
 * int tcpPort = opcConfig.getTcpBindPort();
 * int httpsPort = opcConfig.getHttpsBindPort();
 * String bindAddress = opcConfig.getBindAddresses();
 * 
 * // Get security settings
 * String securityDir = opcConfig.getSecurityTempDir();
 * }</pre>
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public interface OpcConfiguration extends Serializable, Map<String, String> {

	/**
	 * Gets the application name.
	 * 
	 * @return the application name string
	 */
	String getApplicationName();

	/**
	 * Gets the bind addresses for the server.
	 * 
	 * @return comma-separated list of bind addresses
	 */
	String getBindAddresses();

	/**
	 * Gets the bind hostname.
	 * 
	 * @return the hostname string
	 */
	String getBindHostname();

	/**
	 * Gets the build date of the server.
	 * 
	 * @return the build DateTime
	 * @see DateTime
	 */
	DateTime getBuildDate();

	/**
	 * Gets the build number.
	 * 
	 * @return the build number string
	 */
	String getBuildNumber();

	/**
	 * Gets the DNS address certificate generator.
	 * 
	 * @return the generator class name
	 */
	String getDnsAddressCertificateGenerator();

	/**
	 * Gets the HTTPS bind port.
	 * 
	 * @return the port number
	 */
	int getHttpsBindPort();

	/**
	 * Gets the manufacturer name.
	 * 
	 * @return the manufacturer name string
	 */
	String getManufacturerName();

	/**
	 * Gets the path component of the endpoint URL.
	 * 
	 * @return the path string
	 */
	String getPath();

	/**
	 * Gets the product name.
	 * 
	 * @return the product name string
	 */
	String getProductName();

	/**
	 * Gets the product URI.
	 * 
	 * @return the product URI string
	 */
	String getProductUri();

	/**
	 * Gets the security temporary directory.
	 * 
	 * @return the directory path string
	 */
	String getSecurityTempDir();

	/**
	 * Gets the TCP bind port.
	 * 
	 * @return the port number
	 */
	int getTcpBindPort();

}
