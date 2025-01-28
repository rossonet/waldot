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

public final class NetworkHelper {

	private final static Long[] SUBNET_MASK = new Long[] { 4294934528L, 4294950912L, 4294959104L, 4294963200L,
			4294965248L, 4294966272L, 4294966784L, 4294967040L, 4294967168L, 4294967232L, 4294967264L, 4294967280L,
			4294967288L, 4294967292L, 4294967294L, 4294967295L };

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

	public static boolean checkTcpPort(SocketAddress inetSocketAddress, int socketTimeoutSeconds) throws IOException {
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

	/*
	 * @return the local hostname, if possible. Failure results in "localhost".
	 */
	public static String getHostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e) {
			return "localhost";
		}
	}

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
