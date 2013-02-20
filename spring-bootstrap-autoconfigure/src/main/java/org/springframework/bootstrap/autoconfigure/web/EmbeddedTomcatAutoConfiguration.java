/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure.web;

import javax.servlet.Servlet;

import org.apache.catalina.startup.Tomcat;
import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnClass;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.bootstrap.web.embedded.EmbeddedServletContainerFactory;
import org.springframework.bootstrap.web.embedded.EmbeddedTomcatFactory;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({Servlet.class, Tomcat.class})
@ConditionalOnMissingBean(EmbeddedServletContainerFactory.class)
public class EmbeddedTomcatAutoConfiguration {

	@Bean
	public EmbeddedTomcatFactory embeddedTomcatFactory() {
		return new EmbeddedTomcatFactory();
	}

}
