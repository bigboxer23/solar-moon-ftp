package com.bigboxer23.solar_moon.ftp.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bigboxer23.solar_moon.ftp.FTPConfigurationComponent;
import com.bigboxer23.solar_moon.ftp.FTPCustomerComponent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

class FTPConfigurationIntegrationTest {

	private FTPConfigurationComponent ftpConfigurationComponent;
	private FTPCustomerComponent mockCustomerComponent;

	@TempDir
	Path tempDir;

	private String configurationBase;
	private String userDirectoriesBase;

	@BeforeEach
	void setUp() throws Exception {
		mockCustomerComponent = mock(FTPCustomerComponent.class);
		ftpConfigurationComponent = new FTPConfigurationComponent(mockCustomerComponent);

		configurationBase = tempDir.resolve("config").toString();
		userDirectoriesBase = tempDir.resolve("users").toString();

		Files.createDirectories(Path.of(configurationBase));
		Files.createDirectories(Path.of(userDirectoriesBase));

		ReflectionTestUtils.setField(ftpConfigurationComponent, "configurationBase", configurationBase);
		ReflectionTestUtils.setField(ftpConfigurationComponent, "userDirectoriesBase", userDirectoriesBase);
		ReflectionTestUtils.setField(ftpConfigurationComponent, "permissionCommand", "echo permission-test");
		ReflectionTestUtils.setField(ftpConfigurationComponent, "updateDBCommand", "echo update-db-test");

		ftpConfigurationComponent.afterPropertiesSet();
	}

	@Test
	void endToEndConfigurationUpdate_createsFilesAndDirectories() throws IOException {
		List<String> accessKeys = List.of("integration-user1", "integration-user2", "integration-user3");
		when(mockCustomerComponent.getAccessKeys()).thenReturn(accessKeys);

		ftpConfigurationComponent.updateConfiguration();

		for (String accessKey : accessKeys) {
			File userDir = new File(userDirectoriesBase, accessKey);
			assertTrue(userDir.exists() && userDir.isDirectory(), "User directory should exist: " + userDir.getPath());
		}

		File userDBFile = new File(configurationBase, "vusers.txt");
		assertFalse(userDBFile.exists(), "User DB file should be deleted after processing");
	}

	@Test
	void configurationUpdate_handlesLargeNumberOfUsers() throws IOException {
		List<String> accessKeys = java.util.stream.IntStream.range(1, 101)
				.mapToObj(i -> "user" + i)
				.toList();

		when(mockCustomerComponent.getAccessKeys()).thenReturn(accessKeys);

		long startTime = System.currentTimeMillis();
		ftpConfigurationComponent.updateConfiguration();
		long endTime = System.currentTimeMillis();

		assertTrue((endTime - startTime) < 5000, "Configuration update took too long");

		for (String accessKey : accessKeys) {
			File userDir = new File(userDirectoriesBase, accessKey);
			assertTrue(userDir.exists() && userDir.isDirectory());
		}
	}

	@Test
	void configurationUpdate_idempotentOperation() throws IOException {
		List<String> accessKeys = List.of("idempotent-user1", "idempotent-user2");
		when(mockCustomerComponent.getAccessKeys()).thenReturn(accessKeys);

		ftpConfigurationComponent.updateConfiguration();

		long[] firstModifiedTimes = accessKeys.stream()
				.mapToLong(accessKey -> new File(userDirectoriesBase, accessKey).lastModified())
				.toArray();

		ftpConfigurationComponent.updateConfiguration();

		for (int i = 0; i < accessKeys.size(); i++) {
			File userDir = new File(userDirectoriesBase, accessKeys.get(i));
			assertTrue(userDir.exists() && userDir.isDirectory());
			assertEquals(
					firstModifiedTimes[i],
					userDir.lastModified(),
					"Directory should not be recreated on second update");
		}
	}

	@Test
	void configurationUpdate_handlesSpecialCharactersInAccessKeys() throws IOException {
		List<String> accessKeys = List.of("user-with-dashes", "user_with_underscores", "user123with456numbers");
		when(mockCustomerComponent.getAccessKeys()).thenReturn(accessKeys);

		assertDoesNotThrow(() -> ftpConfigurationComponent.updateConfiguration());

		for (String accessKey : accessKeys) {
			File userDir = new File(userDirectoriesBase, accessKey);
			assertTrue(
					userDir.exists() && userDir.isDirectory(),
					"Directory should exist for access key with special characters: " + accessKey);
		}
	}
}
