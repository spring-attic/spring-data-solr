/*
 * Copyright 2012 - 2016 the original author or authors.
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

import java.net.ConnectException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.solr.UncategorizedSolrException;
import org.springframework.util.ClassUtils;

/**
 * Implementation of {@link org.springframework.dao.support.PersistenceExceptionTranslator} capable of translating
 * {@link org.apache.solr.client.solrj.SolrServerException} instances to Spring's
 * {@link org.springframework.dao.DataAccessException} hierarchy.
 * 
 * @author Christoph Strobl
 */
public class SolrExceptionTranslator implements PersistenceExceptionTranslator {

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

		if (ex instanceof DataAccessException) {
			return (DataAccessException) ex;
		}

		if (ex.getCause() instanceof SolrServerException) {
			SolrServerException solrServerException = (SolrServerException) ex.getCause();
			if (solrServerException.getCause() instanceof SolrException) {
				SolrException solrException = (SolrException) solrServerException.getCause();
				// solr 4.x moved ParseExecption from org.apache.lucene.queryParser to org.apache.lucene.queryparser.classic
				// therefore compare ShortClassName instead of using instanceof expression
				if (solrException.getCause() != null
						&& ClassUtils.getShortName(solrException.getCause().getClass()).equalsIgnoreCase("ParseException")) {
					return new InvalidDataAccessApiUsageException((solrException.getCause()).getMessage(),
							solrException.getCause());
				} else {
					ErrorCode errorCode = SolrException.ErrorCode.getErrorCode(solrException.code());
					switch (errorCode) {
						case NOT_FOUND:
						case SERVICE_UNAVAILABLE:
						case SERVER_ERROR:
							return new DataAccessResourceFailureException(solrException.getMessage(), solrException);
						case FORBIDDEN:
						case UNAUTHORIZED:
							return new PermissionDeniedDataAccessException(solrException.getMessage(), solrException);
						case BAD_REQUEST:
							return new InvalidDataAccessApiUsageException(solrException.getMessage(), solrException);
						case UNKNOWN:
							return new UncategorizedSolrException(solrException.getMessage(), solrException);
						default:
							break;
					}
				}
			} else if (solrServerException.getCause() instanceof ConnectException) {
				return new DataAccessResourceFailureException(solrServerException.getCause().getMessage(),
						solrServerException.getCause());
			}
		}
		return null;
	}
}
