/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MyExpectedException;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.example.ExampleConfig;
import org.springframework.boot.test.context.example.scan.Example;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotatedClassFinder}.
 *
 * @author Phillip Webb
 */
public class AnnotatedClassFinderTests {

	@Rule
	public MyExpectedException thrown = MyExpectedException.none();

	private AnnotatedClassFinder finder = new AnnotatedClassFinder(
			SpringBootConfiguration.class);

	@Test
	public void findFromClassWhenSourceIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Source must not be null");
		this.finder.findFromClass((Class<?>) null);
	}

	@Test
	public void findFromPackageWhenSourceIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Source must not be null");
		this.finder.findFromPackage((String) null);
	}

	@Test
	public void findFromPackageWhenNoConfigurationFoundShouldReturnNull() {
		Class<?> config = this.finder.findFromPackage("org.springframework.boot");
		assertThat(config).isNull();
	}

	@Test
	public void findFromClassWhenConfigurationIsFoundShouldReturnConfiguration() {
		Class<?> config = this.finder.findFromClass(Example.class);
		assertThat(config).isEqualTo(ExampleConfig.class);
	}

	@Test
	public void findFromPackageWhenConfigurationIsFoundShouldReturnConfiguration() {
		Class<?> config = this.finder
				.findFromPackage("org.springframework.boot.test.context.example.scan");
		assertThat(config).isEqualTo(ExampleConfig.class);
	}

}
