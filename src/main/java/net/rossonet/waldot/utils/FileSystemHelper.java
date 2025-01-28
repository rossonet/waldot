package net.rossonet.waldot.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileSystemHelper {

	@Deprecated
	public static void deleteDirectory(final File file) {
		deleteDirectoryRecursive(file);
	}

	public static void deleteDirectoryRecursive(final File file) {
		if (Files.exists(Paths.get(file.getAbsolutePath()))) {
			for (final File subfile : file.listFiles()) {
				if (subfile.isDirectory()) {
					deleteDirectoryRecursive(subfile);
				}
				subfile.delete();
			}
			file.delete();
		}
	}

	private FileSystemHelper() {
		throw new UnsupportedOperationException("Just for static usage");

	}

}
