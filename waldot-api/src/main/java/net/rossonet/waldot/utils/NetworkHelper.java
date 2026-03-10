package net.rossonet.waldot.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NetworkHelper provides utility methods for network-related operations such as
 * checking port availability, validating IP addresses and MAC addresses, and
 * retrieving network interface information.
 * 
 * @Author Andrea Ambrosini - Rossonet s.c.a.r.l.
 */
public final class NetworkHelper {

	private final static Long[] SUBNET_MASK = new Long[] { 4294934528L, 4294950912L, 4294959104L, 4294963200L,
			4294965248L, 4294966272L, 4294966784L, 4294967040L, 4294967168L, 4294967232L, 4294967264L, 4294967280L,
			4294967288L, 4294967292L, 4294967294L, 4294967295L };

	/**
	 * Checks if a local TCP port is available for binding.
	 * 
	 * @param port the port number to check (1-65535)
	 * @return true if the port is available (not in use), false if the port is already in use
	 * @throws IOException if an I/O error occurs while checking the port
	 */
	public static boolean checkLocalPortAvailable(final int port) throws IOException {
		boolean portTaken = false;
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(port);
			socket.setReuseAddress(true);
		} catch (final IOException e) {
			portTaken = true;
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
		return !portTaken;
	}

	/**
	 * Checks if a TCP port is reachable at the specified socket address.
	 * The check is performed with a timeout to avoid hanging on unreachable hosts.
	 * 
	 * @param inetSocketAddress     the socket address to check (host and port)
	 * @param socketTimeoutSeconds the timeout in seconds for the connection attempt
	 * @return true if the port is reachable, false otherwise (including timeout)
	 * @throws IOException if an I/O error occurs during the check
	 */
	public static boolean checkTcpPort(final SocketAddress inetSocketAddress, final int socketTimeoutSeconds)
			throws IOException {
		final AtomicBoolean ok = new AtomicBoolean(false);
		try {
			ThreadHelper.runWithTimeout(new Runnable() {
				@Override
				public void run() {
					Socket s = null;
					try {
						s = new Socket();
						s.setSoTimeout(socketTimeoutSeconds * 3);
						s.connect(inetSocketAddress);
						ok.set(true);
					} catch (final Exception e) {
						ok.set(false);
					} finally {
						if (s != null) {
							try {
								s.close();
							} catch (final IOException e) {
							}
						}
					}
				}
			}, socketTimeoutSeconds, TimeUnit.SECONDS);
			return ok.get();
		} catch (final Exception e) {
			return false;
		}
	}

	/**
	 * Finds an available port that can be bound. This method binds to port 0 (an ephemeral port),
	 * immediately releases it, and returns the port number that was assigned.
	 * 
	 * @param faultPort the port to return if no available port can be found
	 * @return an available port number, or faultPort if an error occurs
	 */
	public static int findAvailablePort(final int faultPort) {
		try {
			final ServerSocket socket = new ServerSocket(0);
			socket.setReuseAddress(true);
			final int port = socket.getLocalPort();
			socket.close();
			return port;
		} catch (final IOException ex) {
			return faultPort;
		}
	}

	/**
	 * Retrieves the MAC addresses of all network interfaces on the system.
	 * 
	 * @return a list of MAC addresses in hexadecimal format (e.g., "00-1A-2B-3C-4D-5E"),
	 *         or an empty list if no interfaces have MAC addresses
	 * @throws SocketException if an I/O error occurs while retrieving network interfaces
	 */
	public static List<String> getAllNetworkMacAddress() throws SocketException {
		final List<String> result = new ArrayList<>();
		final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			final NetworkInterface ni = networkInterfaces.nextElement();
			final byte[] hardwareAddress = ni.getHardwareAddress();
			if (hardwareAddress != null) {
				final String[] hexadecimalFormat = new String[hardwareAddress.length];
				for (int i = 0; i < hardwareAddress.length; i++) {
					hexadecimalFormat[i] = String.format("%02X", hardwareAddress[i]);
				}
				result.add(String.join("-", hexadecimalFormat));
			}
		}
		return result;
	}

	/**
	 * Gets the local hostname of the machine.
	 * 
	 * @return the hostname, or "localhost" if the hostname cannot be determined
	 */
	public static String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e) {
			return "localhost";
		}
	}

	/**
	 * Gets all hostnames associated with a given IP address or hostname.
	 * If the address is a wildcard address (0.0.0.0), returns hostnames for all network interfaces.
	 * 
	 * @param address the IP address or hostname to resolve
	 * @return a set of hostnames including the hostname, IP address, and canonical hostname
	 * @throws UnknownHostException if the address cannot be resolved
	 * @throws SocketException      if an I/O error occurs while retrieving network interfaces
	 */
	public static Set<String> getHostnames(final String address) throws UnknownHostException, SocketException {
		final Set<String> hostnames = new HashSet<>();

		final InetAddress inetAddress = InetAddress.getByName(address);

		if (inetAddress.isAnyLocalAddress()) {

			final Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

			for (final NetworkInterface ni : Collections.list(nis)) {
				Collections.list(ni.getInetAddresses()).forEach(ia -> {
					if (ia instanceof Inet4Address) {
						hostnames.add(ia.getHostName());
						hostnames.add(ia.getHostAddress());
						hostnames.add(ia.getCanonicalHostName());
					}
				});
			}
		} else {
			hostnames.add(inetAddress.getHostName());
			hostnames.add(inetAddress.getHostAddress());
			hostnames.add(inetAddress.getCanonicalHostName());
		}
		return hostnames;
	}

	/**
	 * Gets the MAC address of the network interface associated with a hostname or IP address.
	 * 
	 * @param hostname the hostname or IP address to look up
	 * @return the MAC address as a lowercase hexadecimal string, or null if not found
	 * @throws Exception if the hostname cannot be resolved or the MAC address cannot be retrieved
	 */
	public static String getMacAddressAsString(final String hostname) throws Exception {
		InetAddress ip = null;

		ip = InetAddress.getByName(hostname);
		final NetworkInterface network = NetworkInterface.getByInetAddress(ip);
		if (network != null) {
			final byte[] mac = network.getHardwareAddress();
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < mac.length; i++) {
				sb.append(String.format("%02X%s", mac[i], ""));
			}
			return sb.toString().toLowerCase();
		} else {
			return null;
		}

	}

	private static long ipAddressToLong(final String ipAddress) {
		if (ipAddress != null) {
			final String[] s = ipAddress.split("\\.");
			if (s != null && s.length == 4) {
				long result = 0;
				for (int i = 3; i >= 0; i--) {
					try {
						final long n = Long.parseLong(s[3 - i]);
						result |= n << (i * 8);
					} catch (final Exception ex) {
						return -1;
					}
				}
				return result;
			}
		}
		return -1;
	}

	/**
	 * Validates whether a string is a valid IPv4 address.
	 * 
	 * @param ip the string to validate
	 * @return true if the string is a valid IPv4 address, false otherwise
	 */
	public static boolean isValidIPAddress(final String ip) {
		final String zeroTo255 = "(\\d{1,2}|(0|1)\\" + "d{2}|2[0-4]\\d|25[0-5])";
		final String regex = zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255 + "\\." + zeroTo255;
		final Pattern p = Pattern.compile(regex);
		if (ip == null) {
			return false;
		}
		final Matcher m = p.matcher(ip);
		return m.matches();
	}

	/**
	 * Validates whether a string is a valid MAC address.
	 * Supports formats: AA:BB:CC:DD:EE:FF, AA-BB-CC-DD-EE-FF, AABB.CCDD.EEFF
	 * 
	 * @param macAddress the MAC address string to validate
	 * @return true if the string is a valid MAC address, false otherwise
	 */
	public static boolean isValidMacAddress(final String macAddress) {
		final String regex = "^([0-9A-Fa-f]{2}[:-])" + "{5}([0-9A-Fa-f]{2})|" + "([0-9a-fA-F]{4}\\."
				+ "[0-9a-fA-F]{4}\\." + "[0-9a-fA-F]{4})$";
		final Pattern p = Pattern.compile(regex);
		if (macAddress == null) {
			return false;
		}
		final Matcher m = p.matcher(macAddress);
		return m.matches();
	}

	/**
	 * Validates whether a string is a valid subnet mask (IPv4).
	 * A valid subnet mask must be a valid IP address and have contiguous 1s followed by 0s.
	 * 
	 * @param subnetMask the subnet mask string to validate
	 * @return true if the string is a valid subnet mask, false otherwise
	 */
	public static boolean isValidSubnetMask(final String subnetMask) {
		if (subnetMask != null && isValidIPAddress(subnetMask)) {
			final long lSubnetMask = ipAddressToLong(subnetMask);
			if (lSubnetMask > 0) {
				return Arrays.asList(SUBNET_MASK).contains(lSubnetMask);
			}
		}
		return false;
	}

	private NetworkHelper() {
		throw new UnsupportedOperationException("Just for static usage");
	}

}
