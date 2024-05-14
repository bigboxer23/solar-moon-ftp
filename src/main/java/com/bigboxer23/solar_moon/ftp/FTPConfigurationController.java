package com.bigboxer23.solar_moon.ftp;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** */
@RestController
public class FTPConfigurationController {
	private static final Logger logger = LoggerFactory.getLogger(FTPConfigurationController.class);

	private FTPConfigurationComponent ftpConfigurationComponent;

	public FTPConfigurationController(FTPConfigurationComponent component) {
		ftpConfigurationComponent = component;
	}

	@PostMapping("/ftp/update")
	public ResponseEntity<Void> updateFTPConfig(HttpServletRequest servletRequest) {
		try {
			logger.info("updateFTPConfig requested");
			ftpConfigurationComponent.updateConfiguration();
		} catch (IOException e) {
			logger.error("updateFTPConfig", e);
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>(null, HttpStatus.OK);
	}
}
