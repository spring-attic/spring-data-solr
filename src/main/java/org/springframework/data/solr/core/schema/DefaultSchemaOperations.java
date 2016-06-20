/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.solr.core.schema;

import java.io.IOException;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse.UpdateResponse;
import org.springframework.data.solr.core.CollectionCallback;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.schema.SchemaDefinition.CopyFieldDefinition;
import org.springframework.data.solr.core.schema.SchemaDefinition.FieldDefinition;
import org.springframework.data.solr.core.schema.SchemaDefinition.SchemaField;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link SchemaOperations} implementation based on {@link SolrTemplate}.
 *
 * @author Christoph Strobl
 * @since 2.1
 */
public class DefaultSchemaOperations implements SchemaOperations {

	private final SolrTemplate template;
	private final String collection;

	public DefaultSchemaOperations(String collection, SolrTemplate template) {

		Assert.hasText(collection, "Collection must not be null or empty!");
		Assert.notNull(template, "Template must not be null.");

		this.template = template;
		this.collection = collection;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.schema.SchemaOperations#getSchemaName()
	 */
	@Override
	public String getSchemaName() {

		return template.execute(collection, new CollectionCallback<String>() {

			@Override
			public String doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return new SchemaRequest.SchemaName().process(solrClient, collection).getSchemaName();
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.schema.SchemaOperations#getSchemaVersion()
	 */
	@Override
	public Double getSchemaVersion() {
		return template.execute(collection, new CollectionCallback<Double>() {

			@Override
			public Double doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {
				return new Double(new SchemaRequest.SchemaVersion().process(solrClient, collection).getSchemaVersion());
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.schema.SchemaOperations#readSchema()
	 */
	@Override
	public SchemaDefinition readSchema() {

		SchemaRepresentation representation = template.execute(collection, new CollectionCallback<SchemaRepresentation>() {

			@Override
			public SchemaRepresentation doInSolr(SolrClient solrClient, String collection)
					throws SolrServerException, IOException {
				return new SchemaRequest().process(solrClient, collection).getSchemaRepresentation();
			}
		});

		SchemaDefinition sd = new SchemaDefinition(collection);

		for (Map<String, Object> fieldValueMap : representation.getFields()) {
			sd.addFieldDefinition(FieldDefinition.fromMap(fieldValueMap));
		}
		for (Map<String, Object> fieldValueMap : representation.getCopyFields()) {

			CopyFieldDefinition cf = CopyFieldDefinition.fromMap(fieldValueMap);
			sd.addCopyField(cf);

			if (sd.getFieldDefinition(cf.getSource()) != null) {
				sd.getFieldDefinition(cf.getSource()).setCopyFields(cf.getDestination());
			}
		}

		return sd;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.schema.SchemaOperations#addField(org.springframework.data.solr.core.schema.SchemaDefinition.SchemaField)
	 */
	@Override
	public void addField(final SchemaField field) {

		if (field instanceof FieldDefinition) {
			addField((FieldDefinition) field);
		} else if (field instanceof CopyFieldDefinition) {
			addCopyField((CopyFieldDefinition) field);
		}
	}

	private void addField(final FieldDefinition field) {

		template.execute(collection, new CollectionCallback<Integer>() {

			@Override
			public Integer doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {

				UpdateResponse response = new SchemaRequest.AddField(field.asMap()).process(solrClient, collection);
				if (hasErrors(response)) {
					throw new SchemaModificationException(
							String.format("Adding field %s with args %s to collection %s failed with status %s. Server returned %s.",
									field.getName(), field.asMap(), collection, response.getStatus(), response));
				}
				return Integer.valueOf(response.getStatus());
			}
		});

		if (!CollectionUtils.isEmpty(field.getCopyFields())) {

			CopyFieldDefinition cf = new CopyFieldDefinition();
			cf.setSource(field.getName());
			cf.setDestination(field.getCopyFields());

			addCopyField(cf);
		}
	}

	private void addCopyField(final CopyFieldDefinition field) {

		template.execute(collection, new CollectionCallback<Integer>() {

			@Override
			public Integer doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {

				UpdateResponse response = new SchemaRequest.AddCopyField(field.getSource(), field.getDestination())
						.process(solrClient, collection);

				if (hasErrors(response)) {
					throw new SchemaModificationException(String.format(
							"Adding copy field %s with destinations %s to collection %s failed with status %s. Server returned %s.",
							field.getSource(), field.getDestination(), collection, response.getStatus(), response));
				}

				return Integer.valueOf(response.getStatus());
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.solr.core.schema.SchemaOperations#removeField(java.lang.String)
	 */
	@Override
	public void removeField(final String name) {

		template.execute(collection, new CollectionCallback<Integer>() {

			@Override
			public Integer doInSolr(SolrClient solrClient, String collection) throws SolrServerException, IOException {

				UpdateResponse response = new SchemaRequest.DeleteField(name).process(solrClient, collection);
				if (hasErrors(response)) {
					throw new SchemaModificationException(
							String.format("Removing field with name %s from collection %s failed with status %s. Server returned %s.",
									name, collection, response.getStatus(), response));
				}

				return Integer.valueOf(response.getStatus());
			}
		});
	}

	private boolean hasErrors(UpdateResponse response) {

		if (response.getStatus() != 0
				|| response.getResponse() != null && !CollectionUtils.isEmpty(response.getResponse().getAll("errors"))) {
			return true;
		}

		return false;
	}

}
