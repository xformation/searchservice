package com.synectiks.search.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.synectiks.search.SynectiksSearchApplication;
import com.synectiks.search.security.AuthoritiesConstants;
import com.synectiks.search.web.rest.errors.ExceptionTranslator;

/**
 * Integration tests for the {@link AccountResource} REST controller.
 */
@SpringBootTest(classes = SynectiksSearchApplication.class)
public class AccountResourceIT {

	@Autowired
	private ExceptionTranslator exceptionTranslator;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		AccountResource accountUserMockResource = new AccountResource();
		this.mockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource)
				.setControllerAdvice(exceptionTranslator).build();
	}

	@Test
	@WithMockUser(username = "test", roles = "ADMIN")
	public void testGetExistingAccount() throws Exception {
		mockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andExpect(jsonPath("$.login").value("test"))
				.andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
	}

	@Test
	public void testGetUnknownAccount() throws Exception {
		mockMvc.perform(get("/api/account").accept(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(status().isInternalServerError());
	}
}
