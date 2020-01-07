/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.junit.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.data.solr.UncategorizedSolrException;

/**
 * @author Christoph Strobl
 */
public class SolrExceptionTranslatorTests {

	private SolrExceptionTranslator exceptionTranslator = new SolrExceptionTranslator();

	@Test
	public void testNotFoundError() {
		assertThat(exceptionTranslator
				.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(ErrorCode.NOT_FOUND, "message")))
						.isInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	public void testServiceUnavailableError() {
		assertThat(exceptionTranslator
				.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(ErrorCode.SERVICE_UNAVAILABLE, "message")))
						.isInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	public void testServerErrorError() {
		assertThat(exceptionTranslator
				.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(ErrorCode.SERVER_ERROR, "message")))
						.isInstanceOf(DataAccessResourceFailureException.class);
	}

	@Test
	public void testForbiddenError() {
		assertThat(exceptionTranslator
				.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(ErrorCode.FORBIDDEN, "message")))
						.isInstanceOf(PermissionDeniedDataAccessException.class);
	}

	@Test
	public void testUnauthorizedError() {
		assertThat(exceptionTranslator
				.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(ErrorCode.UNAUTHORIZED, "message")))
						.isInstanceOf(PermissionDeniedDataAccessException.class);
	}

	@Test
	public void testBadRequestError() {
		assertThat(exceptionTranslator
				.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(ErrorCode.BAD_REQUEST, "message")))
						.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test
	public void testUnknownError() {
		assertThat(exceptionTranslator
				.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(ErrorCode.UNKNOWN, "message")))
						.isInstanceOf(UncategorizedSolrException.class);
	}

	@Test
	public void testWithNonSolrServerException() {
		assertThat(exceptionTranslator.translateExceptionIfPossible(new RuntimeException("message", new IOException())))
				.isNull();
	}

	@Test
	public void testWithParseException() {
		SolrServerException solrServerException = new SolrServerException("meessage",
				new SolrException(ErrorCode.BAD_REQUEST, new org.apache.solr.parser.ParseException("parse execption message")));

		assertThat(exceptionTranslator.translateExceptionIfPossible(new RuntimeException(solrServerException)))
				.isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // DATASOLR-158
	public void shouldConvertConnectExceptionCorrectly() {

		SolrServerException ex = new SolrServerException("message",
				new java.net.ConnectException("Cannot connect to server"));
		assertThat(exceptionTranslator.translateExceptionIfPossible(new RuntimeException(ex)))
				.isInstanceOf(DataAccessResourceFailureException.class);
	}

	private RuntimeException createWrappedSolrServerExceptionFor(ErrorCode errorCode, String message) {
		SolrServerException rootException = createSolrServerExceptionFor(errorCode, message);
		return new RuntimeException(rootException.getMessage(), rootException);
	}

	private SolrServerException createSolrServerExceptionFor(ErrorCode errorCode, String message) {
		return new SolrServerException("wrapper exception", new SolrException(errorCode, message));
	}

}
