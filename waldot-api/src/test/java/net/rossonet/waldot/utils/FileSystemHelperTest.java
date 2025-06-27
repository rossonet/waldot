package net.rossonet.waldot.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class FileSystemHelperTest {

	@Test
	void testDeleteDirectoryRecursive() throws IOException {
		// Create a temporary directory structure
		final File rootDir = new File("test-root-dir");
		final File subDir1 = new File(rootDir, "subdir1");
		final File subDir2 = new File(rootDir, "subdir2");
		final File file1 = new File(subDir1, "file1.txt");
		final File file2 = new File(subDir2, "file2.txt");

		assertTrue(subDir1.mkdirs(), "Failed to create subdir1");
		assertTrue(subDir2.mkdirs(), "Failed to create subdir2");
		assertTrue(file1.createNewFile(), "Failed to create file1.txt");
		assertTrue(file2.createNewFile(), "Failed to create file2.txt");

		// Verify the directory structure exists
		assertTrue(rootDir.exists(), "Root directory does not exist");
		assertTrue(subDir1.exists(), "Subdir1 does not exist");
		assertTrue(subDir2.exists(), "Subdir2 does not exist");
		assertTrue(file1.exists(), "File1 does not exist");
		assertTrue(file2.exists(), "File2 does not exist");

		// Delete the directory structure
		FileSystemHelper.deleteDirectoryRecursive(rootDir);

		// Verify the directory structure is deleted
		assertFalse(rootDir.exists(), "Root directory was not deleted");
	}
}