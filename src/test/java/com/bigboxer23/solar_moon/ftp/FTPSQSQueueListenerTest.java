package com.bigboxer23.solar_moon.ftp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@ExtendWith(MockitoExtension.class)
class FTPSQSQueueListenerTest {

	private FTPSQSQueueListener ftpSQSQueueListener;

	@Mock
	private FTPConfigurationComponent mockFtpConfigurationComponent;

	@Mock
	private SqsClient mockSqsClient;

	private final String testQueueURL = "https://sqs.us-east-1.amazonaws.com/123456789/test-queue";

	@BeforeEach
	void setUp() {
		ftpSQSQueueListener = new FTPSQSQueueListener(mockFtpConfigurationComponent);
		ReflectionTestUtils.setField(ftpSQSQueueListener, "queueURL", testQueueURL);
	}

	@Test
	void constructor_setsConfigurationComponent() {
		FTPSQSQueueListener listener = new FTPSQSQueueListener(mockFtpConfigurationComponent);
		Object actualComponent = ReflectionTestUtils.getField(listener, "ftpConfigurationComponent");
		assertEquals(mockFtpConfigurationComponent, actualComponent);
	}

	@Test
	void handleMessage_updatesConfigurationAndDeletesMessage() throws IOException {
		Message testMessage = Message.builder()
				.body("test message body")
				.receiptHandle("test-receipt-handle")
				.build();

		try {
			java.lang.reflect.Method handleMessageMethod =
					FTPSQSQueueListener.class.getDeclaredMethod("handleMessage", SqsClient.class, Message.class);
			handleMessageMethod.setAccessible(true);
			handleMessageMethod.invoke(ftpSQSQueueListener, mockSqsClient, testMessage);
		} catch (Exception e) {
			fail("Failed to invoke handleMessage method: " + e.getMessage());
		}

		verify(mockFtpConfigurationComponent).updateConfiguration();
		verify(mockSqsClient)
				.deleteMessage(DeleteMessageRequest.builder()
						.queueUrl(testQueueURL)
						.receiptHandle("test-receipt-handle")
						.build());
	}

	@Test
	void handleMessage_propagatesIOException() throws IOException {
		Message testMessage = Message.builder()
				.body("test message body")
				.receiptHandle("test-receipt-handle")
				.build();

		doThrow(new IOException("Configuration update failed"))
				.when(mockFtpConfigurationComponent)
				.updateConfiguration();

		try {
			java.lang.reflect.Method handleMessageMethod =
					FTPSQSQueueListener.class.getDeclaredMethod("handleMessage", SqsClient.class, Message.class);
			handleMessageMethod.setAccessible(true);

			assertThrows(IOException.class, () -> {
				try {
					handleMessageMethod.invoke(ftpSQSQueueListener, mockSqsClient, testMessage);
				} catch (java.lang.reflect.InvocationTargetException e) {
					throw e.getCause();
				}
			});
		} catch (Exception e) {
			fail("Failed to test exception handling: " + e.getMessage());
		}

		verify(mockFtpConfigurationComponent).updateConfiguration();
		verify(mockSqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
	}

	@Test
	void longPoll_receivesAndProcessesMessages() throws IOException {
		Message message1 =
				Message.builder().body("message 1").receiptHandle("receipt-1").build();

		Message message2 =
				Message.builder().body("message 2").receiptHandle("receipt-2").build();

		ReceiveMessageResponse responseWithMessages = ReceiveMessageResponse.builder()
				.messages(List.of(message1, message2))
				.build();

		ReceiveMessageResponse emptyResponse =
				ReceiveMessageResponse.builder().messages(List.of()).build();

		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(responseWithMessages)
				.thenReturn(emptyResponse)
				.thenThrow(new RuntimeException("Stop infinite loop"));

		try {
			java.lang.reflect.Method longPollMethod =
					FTPSQSQueueListener.class.getDeclaredMethod("longPoll", SqsClient.class);
			longPollMethod.setAccessible(true);

			assertThrows(RuntimeException.class, () -> {
				try {
					longPollMethod.invoke(ftpSQSQueueListener, mockSqsClient);
				} catch (java.lang.reflect.InvocationTargetException e) {
					throw e.getCause();
				}
			});
		} catch (Exception e) {
			fail("Failed to test longPoll method: " + e.getMessage());
		}

		verify(mockFtpConfigurationComponent, times(2)).updateConfiguration();
		verify(mockSqsClient)
				.deleteMessage(DeleteMessageRequest.builder()
						.queueUrl(testQueueURL)
						.receiptHandle("receipt-1")
						.build());
		verify(mockSqsClient)
				.deleteMessage(DeleteMessageRequest.builder()
						.queueUrl(testQueueURL)
						.receiptHandle("receipt-2")
						.build());
	}

	@Test
	void longPoll_configuresCorrectReceiveMessageRequest() {
		ReceiveMessageResponse emptyResponse =
				ReceiveMessageResponse.builder().messages(List.of()).build();

		when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
				.thenReturn(emptyResponse)
				.thenThrow(new RuntimeException("Stop infinite loop"));

		try {
			java.lang.reflect.Method longPollMethod =
					FTPSQSQueueListener.class.getDeclaredMethod("longPoll", SqsClient.class);
			longPollMethod.setAccessible(true);

			assertThrows(RuntimeException.class, () -> {
				try {
					longPollMethod.invoke(ftpSQSQueueListener, mockSqsClient);
				} catch (java.lang.reflect.InvocationTargetException e) {
					throw e.getCause();
				}
			});
		} catch (Exception e) {
			fail("Failed to test longPoll method: " + e.getMessage());
		}

		verify(mockSqsClient, atLeastOnce())
				.receiveMessage(argThat(
						(ReceiveMessageRequest request) -> request.queueUrl().equals(testQueueURL)
								&& request.waitTimeSeconds().equals(20)));
	}
}
