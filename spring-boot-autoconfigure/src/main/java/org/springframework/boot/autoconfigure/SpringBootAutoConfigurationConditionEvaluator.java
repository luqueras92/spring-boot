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

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.SpringBootAutoConfigurationCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * Internal class used to evaluate {@link SpringBootAutoConfigurationCondition}
 * implements.
 *
 * @author Phillip Webb
 */
class SpringBootAutoConfigurationConditionEvaluator {

	private final ConditionContextImpl context;

	SpringBootAutoConfigurationConditionEvaluator(
			ConfigurableListableBeanFactory beanFactory, Environment environment,
			ResourceLoader resourceLoader) {
		this.context = new ConditionContextImpl(beanFactory, environment, resourceLoader);
	}

	public List<String> apply(List<String> configurations,
			List<SpringBootAutoConfigurationCondition> conditions,
			ConditionEvaluationReport report) {
		AnnotationAwareOrderComparator.sort(conditions);
		List<String> result = new ArrayList<String>(configurations);
		Iterator<String> iterator = result.iterator();
		while (iterator.hasNext()) {
			String configurationClass = iterator.next();
			if (shouldSkip(configurationClass, conditions, report)) {
				iterator.remove();
			}
		}
		return result;
	}

	private boolean shouldSkip(String configuration,
			List<SpringBootAutoConfigurationCondition> conditions,
			ConditionEvaluationReport report) {
		for (SpringBootAutoConfigurationCondition condition : conditions) {
			if (!condition.matches(this.context, configuration, report)) {
				return true;
			}
		}
		return false;
	}

	private static class ConditionContextImpl implements ConditionContext {

		private final ConfigurableListableBeanFactory beanFactory;

		private final Environment environment;

		private final ResourceLoader resourceLoader;

		public ConditionContextImpl(ConfigurableListableBeanFactory beanFactory,
				Environment environment, ResourceLoader resourceLoader) {
			this.beanFactory = beanFactory;
			this.environment = environment;
			this.resourceLoader = resourceLoader;
		}

		@Override
		public BeanDefinitionRegistry getRegistry() {
			if (this.beanFactory instanceof BeanDefinitionRegistry) {
				return (BeanDefinitionRegistry) this.beanFactory;
			}
			return null;
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			return this.beanFactory;
		}

		@Override
		public Environment getEnvironment() {
			return this.environment;
		}

		@Override
		public ResourceLoader getResourceLoader() {
			return this.resourceLoader;
		}

		@Override
		public ClassLoader getClassLoader() {
			if (this.resourceLoader != null) {
				return this.resourceLoader.getClassLoader();
			}
			if (this.beanFactory != null) {
				return this.beanFactory.getBeanClassLoader();
			}
			return null;
		}

	}

}
