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

package org.springframework.boot.test.mock.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * {@link TestExecutionListener} to trigger {@link MockitoAnnotations#initMocks(Object)}
 * when {@link MockBean @MockBean} annotations are used. Primarily to allow {@link Captor}
 * annotations.
 *
 * @author Phillip Webb
 */
class MockitoInitializeTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		MockDefinitionsParser parser = new MockDefinitionsParser();
		parser.parse(testContext.getTestClass());
		Set<MockDefinition> definitions = parser.getDefinitions();
		if (!definitions.isEmpty() || hasMockitoAnnotations(testContext)) {
			MockitoAnnotations.initMocks(testContext.getTestInstance());
		}
		if (!definitions.isEmpty()) {
			inject(testContext, definitions);
		}
	}

	private boolean hasMockitoAnnotations(TestContext testContext) {
		MockitoAnnotationCollection collector = new MockitoAnnotationCollection();
		ReflectionUtils.doWithFields(testContext.getClass(), collector);
		return collector.hasAnnotations();
	}

	private void inject(TestContext testContext, Set<MockDefinition> definitions) {
		ApplicationContext applicationContext = testContext.getApplicationContext();
		MockitoPostProcessor postProcessor = applicationContext
				.getBean(MockitoPostProcessor.class);
		for (MockDefinition definition : definitions) {
			AnnotatedElement element = definition.getElement();
			if (element instanceof Field) {
				postProcessor.reinjectMock(testContext.getApplicationContext(),
						definition, (Field) element);
			}
		}
	}

	/**
	 * {@link FieldCallback} to collect mockito annotations.
	 */
	private static class MockitoAnnotationCollection implements FieldCallback {

		private final Set<Annotation> annotations = new LinkedHashSet<Annotation>();

		@Override
		public void doWith(Field field)
				throws IllegalArgumentException, IllegalAccessException {
			for (Annotation annotation : field.getDeclaredAnnotations()) {
				if (annotation.annotationType().getName().startsWith("org.mockito")) {
					this.annotations.add(annotation);
				}
			}
		}

		public boolean hasAnnotations() {
			return !this.annotations.isEmpty();
		}

	}

}
