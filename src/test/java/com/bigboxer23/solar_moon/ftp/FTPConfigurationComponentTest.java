package com.bigboxer23.solar_moon.ftp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FTPConfigurationComponentTest {

	private FTPConfigurationComponent ftpConfigurationComponent;

	@Mock
	private FTPCustomerComponent mockCustomerComponent;

	@TempDir
	Path tempDir;

	private String configurationBase;
	private String userDirectoriesBase;
	private String permissionCommand;
	private String updateDBCommand;

	@BeforeEach
	void setUp() throws Exception {
		ftpConfigurationComponent = new FTPConfigurationComponent(mockCustomerComponent);

		configurationBase = tempDir.resolve("config").toString();
		userDirectoriesBase = tempDir.resolve("users").toString();
		permissionCommand = "echo permission-command";
		updateDBCommand = "echo update-db-command";

		Files.createDirectories(Path.of(configurationBase));
		Files.createDirectories(Path.of(userDirectoriesBase));

		ReflectionTestUtils.setField(ftpConfigurationComponent, "configurationBase", configurationBase);
		ReflectionTestUtils.setField(ftpConfigurationComponent, "userDirectoriesBase", userDirectoriesBase);
		ReflectionTestUtils.setField(ftpConfigurationComponent, "permissionCommand", permissionCommand);
		ReflectionTestUtils.setField(ftpConfigurationComponent, "updateDBCommand", updateDBCommand);

		ftpConfigurationComponent.afterPropertiesSet();
	}

	@Test
	void afterPropertiesSet_initializesFilesCorrectly() throws Exception {
		File userDBFile = (File) ReflectionTestUtils.getField(ftpConfigurationComponent, "userDBFile");
		File userDirectoriesBaseFile =
				(File) ReflectionTestUtils.getField(ftpConfigurationComponent, "userDirectoriesBaseFile");

		assertNotNull(userDBFile);
		assertNotNull(userDirectoriesBaseFile);
		assertEquals(new File(configurationBase, "vusers.txt"), userDBFile);
		assertEquals(new File(userDirectoriesBase), userDirectoriesBaseFile);
	}

	@Test
	void updateConfiguration_createsUserDirectoriesAndFiles() throws IOException {
		List<String> accessKeys = List.of("user1", "user2", "user3");
		when(mockCustomerComponent.getAccessKeys()).thenReturn(accessKeys);

		ftpConfigurationComponent.updateConfiguration();

		File userDBFile = new File(configurationBase, "vusers.txt");
		assertFalse(userDBFile.exists()); // Should be deleted after processing

		for (String accessKey : accessKeys) {
			File userDir = new File(userDirectoriesBase, accessKey);
			assertTrue(userDir.exists() && userDir.isDirectory(), "User directory should exist: " + userDir.getPath());
		}
	}

	@Test
	void updateConfiguration_handlesIOException() {
		when(mockCustomerComponent.getAccessKeys()).thenThrow(new RuntimeException("Database error"));

		assertThrows(RuntimeException.class, () -> ftpConfigurationComponent.updateConfiguration());
	}

	@Test
	void updateConfiguration_createsUserFileContent() throws IOException {
		List<String> accessKeys = List.of("testuser");
		when(mockCustomerComponent.getAccessKeys()).thenReturn(accessKeys);

		try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class)) {
			ftpConfigurationComponent.updateConfiguration();

			File expectedUserDBFile = new File(configurationBase, "vusers.txt");
			mockedFileUtils.verify(() -> FileUtils.writeStringToFile(
					eq(expectedUserDBFile), eq("testuser\ntestuser\n"), eq(StandardCharsets.UTF_8), eq(true)));
		}
	}

	@Test
	void updateConfiguration_handlesEmptyAccessKeysList() throws IOException {
		when(mockCustomerComponent.getAccessKeys()).thenReturn(List.of());

		assertDoesNotThrow(() -> ftpConfigurationComponent.updateConfiguration());

		File userDBFile = new File(configurationBase, "vusers.txt");
		assertFalse(userDBFile.exists());
	}

	@Test
	void updateConfiguration_createsDirectoriesForNewUsers() throws IOException {
		List<String> accessKeys = List.of("newuser1", "newuser2");
		when(mockCustomerComponent.getAccessKeys()).thenReturn(accessKeys);

		ftpConfigurationComponent.updateConfiguration();

		for (String accessKey : accessKeys) {
			File userDir = new File(userDirectoriesBase, accessKey);
			assertTrue(userDir.exists() && userDir.isDirectory());
		}
	}

	@Test
	void updateConfiguration_doesNotRecreateExistingDirectories() throws IOException {
		String existingUser = "existinguser";
		File existingUserDir = new File(userDirectoriesBase, existingUser);
		assertTrue(existingUserDir.mkdirs());

		long originalModified = existingUserDir.lastModified();

		when(mockCustomerComponent.getAccessKeys()).thenReturn(List.of(existingUser));

		ftpConfigurationComponent.updateConfiguration();

		assertTrue(existingUserDir.exists());
		assertEquals(originalModified, existingUserDir.lastModified());
	}
}
