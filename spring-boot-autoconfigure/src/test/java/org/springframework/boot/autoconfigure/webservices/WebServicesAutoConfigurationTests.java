/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.webservices;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.WebApplicationContextTester;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServicesAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
public class WebServicesAutoConfigurationTests {

	private final WebApplicationContextTester contextLoader = new WebApplicationContextTester()
			.withConfiguration(AutoConfigurations.of(WebServicesAutoConfiguration.class));

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void defaultConfiguration() {
		this.contextLoader.run(context -> {
			assertThat(context.getBeansOfType(ServletRegistrationBean.class)).hasSize(1);
		});
	}

	@Test
	public void customPathMustBeginWithASlash() {
		this.contextLoader.withPropertyValues("spring.webservices.path=invalid")
				.loadAndFail(BeanCreationException.class, (ex) -> {
					System.out.println(ex.getMessage());
					assertThat(ex.getMessage()).contains(
							"Failed to bind properties under 'spring.webservices'");
				});
	}

	@Test
	public void customPath() {
		this.contextLoader.withPropertyValues("spring.webservices.path=/valid").run(context -> {
			ServletRegistrationBean<?> servletRegistrationBean = context
					.getBean(ServletRegistrationBean.class);
			assertThat(servletRegistrationBean.getUrlMappings()).contains("/valid/*");
		});
	}

	@Test
	public void customPathWithTrailingSlash() {
		this.contextLoader.withPropertyValues("spring.webservices.path=/valid/").run(context -> {
			ServletRegistrationBean<?> servletRegistrationBean = context
					.getBean(ServletRegistrationBean.class);
			assertThat(servletRegistrationBean.getUrlMappings()).contains("/valid/*");
		});
	}

	@Test
	public void customLoadOnStartup() {
		this.contextLoader.withPropertyValues("spring.webservices.servlet.load-on-startup=1")
				.run(context -> {
					ServletRegistrationBean<?> registrationBean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(ReflectionTestUtils.getField(registrationBean,
							"loadOnStartup")).isEqualTo(1);
				});
	}

	@Test
	public void customInitParameters() {
		this.contextLoader.withPropertyValues("spring.webservices.servlet.init.key1=value1",
				"spring.webservices.servlet.init.key2=value2").run(context -> {
					ServletRegistrationBean<?> registrationBean = context
							.getBean(ServletRegistrationBean.class);
					assertThat(registrationBean.getInitParameters()).containsEntry("key1",
							"value1");
					assertThat(registrationBean.getInitParameters()).containsEntry("key2",
							"value2");
				});
	}

}
