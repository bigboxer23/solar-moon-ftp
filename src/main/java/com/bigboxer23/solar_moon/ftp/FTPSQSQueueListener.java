package com.bigboxer23.solar_moon.ftp;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

/** */
@Component
public class FTPSQSQueueListener implements InitializingBean {
	private static final Logger logger = LoggerFactory.getLogger(FTPSQSQueueListener.class);

	@Value("${ftp.update.sqs.url}")
	private String queueURL;

	private final FTPConfigurationComponent ftpConfigurationComponent;

	public FTPSQSQueueListener(FTPConfigurationComponent ftpConfigurationComponent) {
		this.ftpConfigurationComponent = ftpConfigurationComponent;
	}

	@Override
	public void afterPropertiesSet() {
		MDC.put("service.name", "FTPConfiguration");
		logger.info("Starting listening to FTP SQS");
		while (true) {
			logger.info("Creating new SQS client");
			try (SqsClient sqs = SqsClient.create()) {
				longPoll(sqs);
			} catch (SqsException e) {
				logger.error(e.awsErrorDetails().errorMessage());
			} catch (Exception e) {
				logger.error("FTPSQSQueueListener", e);
			}
		}
	}

	private void longPoll(SqsClient sqs) throws IOException {
		while (true) {
			ReceiveMessageResponse response = sqs.receiveMessage(ReceiveMessageRequest.builder()
					.queueUrl(queueURL)
					.waitTimeSeconds(20)
					.build());
			if (response.hasMessages()) {
				for (Message message : response.messages()) {
					handleMessage(sqs, message);
				}
			}
			logger.debug("FTP SQS iterating");
		}
	}

	private void handleMessage(SqsClient sqs, Message message) throws IOException {
		logger.info("FTP SQS message received " + message.body());
		ftpConfigurationComponent.updateConfiguration();
		sqs.deleteMessage(DeleteMessageRequest.builder()
				.queueUrl(queueURL)
				.receiptHandle(message.receiptHandle())
				.build());
		logger.info("FTP SQS message deleted " + message.body());
	}
}
