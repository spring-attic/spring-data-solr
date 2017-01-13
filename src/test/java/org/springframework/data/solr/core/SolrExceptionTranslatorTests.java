/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
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
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(
				ErrorCode.NOT_FOUND, "message")), IsInstanceOf.instanceOf(DataAccessResourceFailureException.class));
	}

	@Test
	public void testServiceUnavailableError() {
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(
				ErrorCode.SERVICE_UNAVAILABLE, "message")), IsInstanceOf.instanceOf(DataAccessResourceFailureException.class));
	}

	@Test
	public void testServerErrorError() {
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(
				ErrorCode.SERVER_ERROR, "message")), IsInstanceOf.instanceOf(DataAccessResourceFailureException.class));
	}

	@Test
	public void testForbiddenError() {
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(
				ErrorCode.FORBIDDEN, "message")), IsInstanceOf.instanceOf(PermissionDeniedDataAccessException.class));
	}

	@Test
	public void testUnauthorizedError() {
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(
				ErrorCode.UNAUTHORIZED, "message")), IsInstanceOf.instanceOf(PermissionDeniedDataAccessException.class));
	}

	@Test
	public void testBadRequestError() {
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(
				ErrorCode.BAD_REQUEST, "message")), IsInstanceOf.instanceOf(InvalidDataAccessApiUsageException.class));
	}

	@Test
	public void testUnknownError() {
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(createWrappedSolrServerExceptionFor(
				ErrorCode.UNKNOWN, "message")), IsInstanceOf.instanceOf(UncategorizedSolrException.class));
	}

	@Test
	public void testWithNonSolrServerException() {
		Assert.assertNull(exceptionTranslator.translateExceptionIfPossible(new RuntimeException("message",
				new IOException())));
	}

	@Test
	public void testWithParseException() {
		SolrServerException solrServerException = new SolrServerException("meessage", new SolrException(
				ErrorCode.BAD_REQUEST, new org.apache.solr.parser.ParseException("parse execption message")));

		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(new RuntimeException(solrServerException)),
				IsInstanceOf.instanceOf(InvalidDataAccessApiUsageException.class));
	}

	@Test // DATASOLR-158
	public void shouldConvertConnectExceptionCorrectly() {

		SolrServerException ex = new SolrServerException("message", new java.net.ConnectException(
				"Cannot connect to server"));
		Assert.assertThat(exceptionTranslator.translateExceptionIfPossible(new RuntimeException(ex)),
				IsInstanceOf.instanceOf(DataAccessResourceFailureException.class));
	}

	private RuntimeException createWrappedSolrServerExceptionFor(ErrorCode errorCode, String message) {
		SolrServerException rootException = createSolrServerExceptionFor(errorCode, message);
		return new RuntimeException(rootException.getMessage(), rootException);
	}

	private SolrServerException createSolrServerExceptionFor(ErrorCode errorCode, String message) {
		return new SolrServerException("wrapper exception", new SolrException(errorCode, message));
	}

}
