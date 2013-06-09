/*
 * Copyright 2012-2013 the original author or authors.
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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.springframework.bootstrap.autoconfigure.web.EmbeddedServletContainerAutoConfiguration.EmbeddedJetty;
import org.springframework.bootstrap.autoconfigure.web.EmbeddedServletContainerAutoConfiguration.EmbeddedTomcat;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.ServletContextInitializer;
import org.springframework.bootstrap.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for an embedded servlet containers.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Import({ EmbeddedTomcat.class, EmbeddedJetty.class })
public class EmbeddedServletContainerAutoConfiguration {

	@Bean
	public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
		return new EmbeddedServletContainerCustomizerBeanPostProcessor();
	}

	@Bean
	@ConditionalOnMissingBean(value = ServletContextInitializer.class, considerHierarchy = false)
	public DispatcherServlet dispatcherServlet() {
		return new DispatcherServlet();
	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, Server.class, Loader.class })
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, considerHierarchy = false)
	public static class EmbeddedJetty {

		@Bean
		public JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

	}

	@Configuration
	@ConditionalOnClass({ Servlet.class, Tomcat.class })
	@ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, considerHierarchy = false)
	public static class EmbeddedTomcat {

		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

	}

}
