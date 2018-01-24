/*
 * Copyright 2012 - 2018 the original author or authors.
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
package org.springframework.data.solr.server.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.solr.server.SolrClientFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.xml.sax.SAXException;

/**
 * The EmbeddedSolrServerFactory allows hosting of an SolrServer instance in embedded mode. Configuration files are
 * loaded via {@link ResourceUtils}, therefore it is possible to place them in classpath. Use this class for Testing. It
 * is not recommended for production.
 *
 * @author Christoph Strobl
 */
public class EmbeddedSolrServerFactory implements SolrClientFactory, DisposableBean {

	private static final String SOLR_HOME_SYSTEM_PROPERTY = "solr.solr.home";

	private @Nullable String solrHome;
	private AtomicReference<CoreContainer> coreContainer = new AtomicReference<>(null);
	private ConcurrentHashMap<String, EmbeddedSolrServer> servers = new ConcurrentHashMap<>();

	protected EmbeddedSolrServerFactory() {

	}

	/**
	 * @param solrHome Any Path expression valid for use with {@link ResourceUtils} that points to the
	 *          {@code solr.solr.home} directory
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public EmbeddedSolrServerFactory(String solrHome) throws ParserConfigurationException, IOException, SAXException {
		Assert.hasText(solrHome, "SolrHome must not be null nor empty!");
		this.solrHome = solrHome;
	}

	@Override
	public EmbeddedSolrServer getSolrClient() {
		return new EmbeddedSolrServer(getCoreContainer(), "collection1");
	}

	protected void initCoreContainer() {
		try {
			this.coreContainer.compareAndSet(null, createCoreContainer(this.solrHome));
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param path Any Path expression valid for use with {@link ResourceUtils}
	 * @return new instance of {@link EmbeddedSolrServer}
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public final EmbeddedSolrServer createPathConfiguredSolrServer(String path)
			throws ParserConfigurationException, IOException, SAXException {
		return new EmbeddedSolrServer(createCoreContainer(path), "collection1");
	}

	private CoreContainer createCoreContainer(String path) throws FileNotFoundException, UnsupportedEncodingException {

		String solrHomeDirectory = System.getProperty(SOLR_HOME_SYSTEM_PROPERTY);

		if (StringUtils.isBlank(solrHomeDirectory)) {
			solrHomeDirectory = ResourceUtils.getFile(path).getPath();
		}

		solrHomeDirectory = URLDecoder.decode(solrHomeDirectory, "utf-8");

		File solrXmlFile = new File(solrHomeDirectory + "/solr.xml");
		if (ClassUtils.hasConstructor(CoreContainer.class, String.class, File.class)) {
			return createCoreContainerViaConstructor(solrHomeDirectory, solrXmlFile);
		}
		return createCoreContainer(solrHomeDirectory, solrXmlFile);
	}

	/**
	 * Create {@link CoreContainer} via its constructor (Solr 3.6.0 - 4.3.1)
	 *
	 * @param solrHomeDirectory
	 * @param solrXmlFile
	 * @return
	 */
	private CoreContainer createCoreContainerViaConstructor(String solrHomeDirectory, File solrXmlFile) {
		Constructor<CoreContainer> constructor = ClassUtils.getConstructorIfAvailable(CoreContainer.class, String.class,
				File.class);
		return BeanUtils.instantiateClass(constructor, solrHomeDirectory, solrXmlFile);
	}

	/**
	 * Create {@link CoreContainer} for Solr version 4.4+ and handle changes in .
	 *
	 * @param solrHomeDirectory
	 * @param solrXmlFile
	 * @return
	 */
	private CoreContainer createCoreContainer(String solrHomeDirectory, File solrXmlFile) {

		Method createAndLoadMethod = ClassUtils.getStaticMethod(CoreContainer.class, "createAndLoad", String.class,
				File.class);

		if (createAndLoadMethod != null) {
			return (CoreContainer) ReflectionUtils.invokeMethod(createAndLoadMethod, null, solrHomeDirectory, solrXmlFile);
		}

		createAndLoadMethod = ClassUtils.getStaticMethod(CoreContainer.class, "createAndLoad", Path.class, Path.class);
		return (CoreContainer) ReflectionUtils.invokeMethod(createAndLoadMethod, null,
				FileSystems.getDefault().getPath(solrHomeDirectory), FileSystems.getDefault().getPath(solrXmlFile.getPath()));
	}

	public void shutdownSolrServer() {
		if (coreContainer.get() != null) {
			coreContainer.get().shutdown();
		}
	}

	public List<String> getCores() {
		return new ArrayList<>(getCoreContainer().getAllCoreNames());
	}

	public void setSolrHome(String solrHome) {
		Assert.hasText(solrHome, "SolrHome must not be null nor empty!");
		this.solrHome = solrHome;
	}

	@Override
	public void destroy() throws Exception {
		shutdownSolrServer();
	}

	private CoreContainer getCoreContainer() {

		if (coreContainer.get() == null) {
			initCoreContainer();
		}

		return coreContainer.get();
	}

}
