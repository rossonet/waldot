package net.rossonet.waldot.configuration;

import java.util.HashMap;

import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;

import net.rossonet.waldot.api.configuration.OpcConfiguration;

public class DefaultOpcUaConfiguration extends HashMap<String, String> implements OpcConfiguration {

	public static String DEFAULT_ADDRESS_CERTIFICATE_GENERATOR = "127.0.0.1";
	public static String DEFAULT_APPLICATION_NAME = "WaldOT OPCUA server";
	public static String DEFAULT_BIND_ADDRESSES = "0.0.0.0";
	public static String DEFAULT_BIND_HOSTNAME = "127.0.0.1";
	public static DateTime DEFAULT_BUILD_DATE = DateTime.now();
	public static String DEFAULT_BUILD_NUMBER = "w001";
	public static int DEFAULT_HTTPS_BIND_PORT = 8443;
	public static String DEFAULT_MANUFACTURER_NAME = "Rossonet s.c.a r.l.";
	public static String DEFAULT_PATH = "/waldot";
	public static String DEFAULT_PRODUCT_NAME = "WaldOT";
	public static String DEFAULT_PRODUCT_URI = "urn:rossonet:waldot:uaserver";
	public static String DEFAULT_SECURITY_DIR = ".security";
	public static int DEFAULT_TCP_BIND_PORT = 12686;
	public static final long serialVersionUID = 1487462093576743910L;

	public static OpcConfiguration getDefault() {
		return new DefaultOpcUaConfiguration();
	}

	private String applicationName = DEFAULT_APPLICATION_NAME;
	private String bindAddresses = DEFAULT_BIND_ADDRESSES;
	private String bindHostname = DEFAULT_BIND_HOSTNAME;
	private DateTime buildDate = DEFAULT_BUILD_DATE;
	private String buildNumber = DEFAULT_BUILD_NUMBER;
	private String dnsAddressCertificateGenerator = DEFAULT_ADDRESS_CERTIFICATE_GENERATOR;
	private int httpsBindPort = DEFAULT_HTTPS_BIND_PORT;
	private String manufacturerName = DEFAULT_MANUFACTURER_NAME;
	private String path = DEFAULT_PATH;
	private String productName = DEFAULT_PRODUCT_NAME;
	private String productUri = DEFAULT_PRODUCT_URI;
	private String securityTempDir = DEFAULT_SECURITY_DIR;
	private int tcpBindPort = DEFAULT_TCP_BIND_PORT;

	private DefaultOpcUaConfiguration() {
	}

	@Override
	public String getApplicationName() {
		return applicationName;
	}

	@Override
	public String getBindAddresses() {
		return bindAddresses;
	}

	@Override
	public String getBindHostname() {
		return bindHostname;
	}

	@Override
	public DateTime getBuildDate() {
		return buildDate;
	}

	@Override
	public String getBuildNumber() {
		return buildNumber;
	}

	@Override
	public String getDnsAddressCertificateGenerator() {
		return dnsAddressCertificateGenerator;
	}

	@Override
	public int getHttpsBindPort() {
		return httpsBindPort;
	}

	@Override
	public String getManufacturerName() {
		return manufacturerName;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getProductName() {
		return productName;
	}

	@Override
	public String getProductUri() {
		return productUri;
	}

	@Override
	public String getSecurityTempDir() {
		return securityTempDir;
	}

	@Override
	public int getTcpBindPort() {
		return tcpBindPort;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public void setBindAddresses(String bindAddresses) {
		this.bindAddresses = bindAddresses;
	}

	public void setBindHostname(String bindHostname) {
		this.bindHostname = bindHostname;
	}

	public void setBuildDate(DateTime buildDate) {
		this.buildDate = buildDate;
	}

	public void setBuildNumber(String buildNumber) {
		this.buildNumber = buildNumber;
	}

	public void setDnsAddressCertificateGenerator(String dnsAddressCertificateGenerator) {
		this.dnsAddressCertificateGenerator = dnsAddressCertificateGenerator;
	}

	public void setHttpsBindPort(int httpsBindPort) {
		this.httpsBindPort = httpsBindPort;
	}

	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public void setProductUri(String productUri) {
		this.productUri = productUri;
	}

	public void setSecurityTempDir(String securityTempDir) {
		this.securityTempDir = securityTempDir;
	}

	public void setTcpBindPort(int tcpBindPort) {
		this.tcpBindPort = tcpBindPort;
	}

}
