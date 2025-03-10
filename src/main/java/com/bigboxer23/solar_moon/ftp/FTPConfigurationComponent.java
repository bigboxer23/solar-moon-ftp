package com.bigboxer23.solar_moon.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** */
@Slf4j
@Component
public class FTPConfigurationComponent implements InitializingBean {
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
		for (String accessKey : customerComponent.getAccessKeys()) {
			handleUser(accessKey);
		}
		updateUserDB();
		deleteUserFile();
		updateUserDirectoryPermissions();
	}

	private void updateUserDB() throws IOException {
		log.info("running updateUserDB");
		runCommand(updateDBCommand);
	}

	private void deleteUserFile() {
		log.info("deleting user file");
		userDBFile.delete();
	}

	private void updateUserDirectoryPermissions() {
		log.info("running updateUserDirectoryPermissions");
		runCommand(permissionCommand);
	}

	private void handleUser(String accessKey) throws IOException {
		FileUtils.writeStringToFile(userDBFile, accessKey + "\n" + accessKey + "\n", StandardCharsets.UTF_8, true);
		File userDirectory = new File(userDirectoriesBaseFile, accessKey);
		if (!userDirectory.exists()) {
			userDirectory.mkdirs();
		}
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
				log.debug("command output: " + out);
			}
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IOException("error exit status from command: " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			log.error("error running command '" + command + "'", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		userDBFile = new File(new File(configurationBase), "vusers.txt");
		userDirectoriesBaseFile = new File(userDirectoriesBase);
		MDC.put("service.name", "FTPConfiguration");
	}
}
