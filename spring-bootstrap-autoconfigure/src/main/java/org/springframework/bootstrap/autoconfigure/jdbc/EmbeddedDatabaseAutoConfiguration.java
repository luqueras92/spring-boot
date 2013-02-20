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

package org.springframework.bootstrap.autoconfigure.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.ClassUtils;

/**
 * {@link AutoConfiguration} for embedded databases.
 *
 * @author Phillip Webb
 */
@AutoConfiguration
@Conditional(EmbeddedDatabaseAutoConfiguration.EmbeddedDatabaseCondition.class)
@ConditionalOnMissingBean(DataSource.class)
public class EmbeddedDatabaseAutoConfiguration {

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_TYPE_CLASSES;
	static {
		EMBEDDED_DATABASE_TYPE_CLASSES = new LinkedHashMap<EmbeddedDatabaseType, String>();
		EMBEDDED_DATABASE_TYPE_CLASSES.put(EmbeddedDatabaseType.HSQL, "org.hsqldb.Database");
		// FIXME additional types
	}

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder().setType(getEmbeddedDatabaseType()).build();
	}

	public static EmbeddedDatabaseType getEmbeddedDatabaseType() {
		for (Map.Entry<EmbeddedDatabaseType, String> entry : EMBEDDED_DATABASE_TYPE_CLASSES.entrySet()) {
			if (ClassUtils.isPresent(entry.getValue(), EmbeddedDatabaseAutoConfiguration.class.getClassLoader())) {
				return entry.getKey();
			}
		}
		return null;
	}

	static class EmbeddedDatabaseCondition implements Condition {

		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (!ClassUtils.isPresent(
					"org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType",
					context.getClassLoader())) {
				return false;
			}
			return getEmbeddedDatabaseType() != null;
		}
	}

}
