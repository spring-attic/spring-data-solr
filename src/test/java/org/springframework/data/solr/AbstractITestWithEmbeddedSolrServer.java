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
package org.springframework.data.solr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrCore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

/**
 * @author Christoph Strobl
 */
public abstract class AbstractITestWithEmbeddedSolrServer {

	protected static SolrClient solrClient;
	protected static String DEFAULT_BEAN_ID = "1";

	@BeforeClass
	public static void initSolrServer()
			throws IOException, ParserConfigurationException, SAXException, InterruptedException {

		String solrHome = ResourceUtils.getURL("classpath:org/springframework/data/solr").getPath();

		Method createAndLoadMethod = ClassUtils.getStaticMethod(CoreContainer.class, "createAndLoad", String.class,
				File.class);

		CoreContainer coreContainer = null;
		if (createAndLoadMethod != null) {
			coreContainer = (CoreContainer) ReflectionUtils.invokeMethod(createAndLoadMethod, null, solrHome,
					new File(solrHome + "/solr.xml"));
		} else {

			createAndLoadMethod = ClassUtils.getStaticMethod(CoreContainer.class, "createAndLoad", Path.class, Path.class);

			coreContainer = (CoreContainer) ReflectionUtils.invokeMethod(createAndLoadMethod, null,
					FileSystems.getDefault().getPath(solrHome),
					FileSystems.getDefault().getPath(new File(solrHome + "/solr.xml").getPath()));
		}

		for (SolrCore core : coreContainer.getCores()) {
			core.addCloseHook(new CloseHook() {
				@Override
				public void preClose(SolrCore core) {}

				@Override
				public void postClose(SolrCore core) {
					CoreDescriptor cd = core.getCoreDescriptor();
					if (cd != null) {
						File dataDir = new File(cd.getInstanceDir() + File.separator + "data");
						try {
							FileUtils.deleteDirectory(dataDir);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			});
		}

		solrClient = new EmbeddedSolrServer(coreContainer, "collection1");
	}

	public static void cleanDataInSolr() throws SolrServerException, IOException {

		solrClient.deleteByQuery("*:*");
		solrClient.commit();
	}

	@AfterClass
	public static void shutdown() throws SolrServerException, IOException {
		cleanDataInSolr();
		solrClient.shutdown();
	}

	public ExampleSolrBean createDefaultExampleBean() {
		return createExampleBeanWithId(DEFAULT_BEAN_ID);
	}

	public ExampleSolrBean createExampleBeanWithId(String id) {
		return new ExampleSolrBean(id, "bean_" + id, "category_" + id);
	}

	public List<ExampleSolrBean> createBeansWithId(int nrObjectsToCreate) {
		return createBeansWithIdAndPrefix(nrObjectsToCreate, null);
	}

	public List<ExampleSolrBean> createBeansWithIdAndPrefix(int nrObjectsToCreate, String idPrefix) {
		ArrayList<ExampleSolrBean> list = new ArrayList<ExampleSolrBean>(nrObjectsToCreate);
		for (int i = 1; i <= nrObjectsToCreate; i++) {
			list.add(createExampleBeanWithId(idPrefix != null ? (idPrefix + Integer.toString(i)) : Integer.toString(i)));
		}
		return list;
	}

}
