/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.config;

import javax.servlet.Servlet;

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContextTests;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.web.MockServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example {@code @Configuration} for use with
 * {@link AnnotationConfigEmbeddedWebApplicationContextTests}.
 *
 * @author Phillip Webb
 */
@Configuration
public class ExampleEmbeddedWebApplicationConfiguration {

	@Bean
	public EmbeddedServletContainerFactory containerFactory() {
		return new MockEmbeddedServletContainerFactory();
	}

	@Bean
	public Servlet servlet() {
		return new MockServlet();
	}

}
