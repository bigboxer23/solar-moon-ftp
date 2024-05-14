package com.bigboxer23.solar_moon.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** */
@Component
public class FTPConfigurationComponent implements InitializingBean {
	private static final Logger logger = LoggerFactory.getLogger(FTPConfigurationComponent.class);

	private FTPCustomerComponent customerComponent;

	@Value("${user_directories_base}")
	private String userDirectoriesBase;

	@Value("${ftp_configuration_base}")
	private String configurationBase;

	@Value("${permission_command}")
	private String permissionCommand;

	@Value("${update_db_command}")
	private String updateDBCommand;

	private File userDBFile;

	private File userDirectoriesBaseFile;

	public FTPConfigurationComponent(FTPCustomerComponent component) {
		customerComponent = component;
	}

	public synchronized void updateConfiguration() throws IOException {
		deleteUserFile();
		Map<String, String> customers = customerComponent.getCustomerAccessKeyMap();
		for (Map.Entry<String, String> entry : customers.entrySet()) {
			handleUser(entry.getKey(), entry.getValue());
		}
		updateUserDB();
		deleteUserFile();
		updateUserDirectoryPermissions();
	}

	private void updateUserDB() throws IOException {
		runCommand(updateDBCommand);
	}

	private void deleteUserFile() {
		userDBFile.delete();
	}

	private void updateUserDirectoryPermissions() {
		runCommand(permissionCommand);
	}

	private void handleUser(String userId, String accessKey) throws IOException {
		FileUtils.writeStringToFile(userDBFile, userId + "\n" + accessKey + "\n", StandardCharsets.UTF_8, true);
		File userDirectory = new File(userDirectoriesBaseFile, userId);
		if (!userDirectory.exists()) {
			userDirectory.mkdirs();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		userDBFile = new File(new File(configurationBase), "userDB");
		userDirectoriesBaseFile = new File(userDirectoriesBase);
	}

	private void runCommand(String command) {
		ProcessBuilder builder = new ProcessBuilder(command.split(" "));
		try {
			Process process = builder.start();
			StringBuilder out = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					out.append(line).append("\n");
				}
				logger.info("command output: " + out);
			}
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IOException("error exit status from command: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			logger.warn("error running command", e);
		}
	}
}
