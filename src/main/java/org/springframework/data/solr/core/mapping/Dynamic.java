/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.solr.core.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Declare a field as dynamic.
 * <p>
 * Used mainly to annotate {@link Map} fields, indicates the field should have its key parsed whenever reading the
 * document from Solr and formatted whenever being written to Solr based on wildcard field name defined for it.
 * <p>
 * Example:
 * <p>
 * Definition:
 * 
 * <pre class="code">
 * class MyBeanClass {
 * 
 * 	&#064;Id private String id;
 * 	&#064;Dynamic @Field(&quot;*_s&quot;) private Map&lt;String, String&gt; values;
 * 
 * 	// setters and getters
 * 
 * }
 * </pre>
 * <p>
 * Use:
 * 
 * <pre class="code">
 * Map&lt;String, String&gt; values = new HashMap&lt;&gt;();
 * values.put(&quot;v1&quot;, &quot;value for key v1&quot;);
 * values.put(&quot;v2&quot;, &quot;value for key v2&quot;);
 * 
 * MyBeanClass bean = new MyBeanClass();
 * bean.setId("id-bean-1");
 * bean.setValues(values);
 * 
 * solrTemplate.saveBean(bean);
 * solrTemplate.commit();
 * 
 * ...
 * 
 * MyBeanClass bean = solrTemplate.getById(&quot;id-bean-1&quot;, MyBeanClass.class);
 * bean.getValues().get(&quot;v1&quot;);
 * </pre>
 * 
 * The result on Solr side will be a document with the foolowing structure:
 * 
 * <pre class="code">
 * {
 *   &quot;id&quot;: &quot;id-bean-1&quot;,
 *   &quot;v1_s&quot;: &quot;value for key v1&quot;,
 *   &quot;v2_s&quot;: &quot;value for key v2&quot;,
 * }
 * </pre>
 * 
 * @author Francisco Spaeth
 * @since 1.5
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Dynamic {
}
