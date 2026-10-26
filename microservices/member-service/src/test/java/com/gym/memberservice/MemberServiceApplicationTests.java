package com.gym.memberservice;


import com.gym.memberservice.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MemberServiceApplicationTests {

	@Test
	void databaseConstraintViolationReturns409Conflict() {

		// Arrange: Create the exception handler
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		DataIntegrityViolationException exception =
				new DataIntegrityViolationException(
						"Duplicate email detected"
				);

		// Act: Call the database exception handler
		ResponseEntity<Map<String, String>> response =
				handler.handleDataIntegrityViolation(exception);

		// Assert: Verify the HTTP status and response body
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

		assertNotNull(response.getBody());

		assertEquals(
				"The request conflicts with existing database data",
				response.getBody().get("error")
		);
	}

}
