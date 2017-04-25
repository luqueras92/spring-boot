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

package org.springframework.boot.context.properties.bind;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName.Form;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * {@link AggregateBinder} for arrays.
 *
 * @author Phillip Webb
 */
class ArrayBinder extends IndexedElementsBinder<Object> {

	ArrayBinder(BindContext context) {
		super(context);
	}

	@Override
	protected Object bind(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder itemBinder, Class<?> type) {
		ResolvableType elementType = target.getType().getComponentType();
		return bindToArray(name, target, itemBinder, elementType);
	}

	private Object bindToArray(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder itemBinder, ResolvableType elementType) {
		for (ConfigurationPropertySource source : getContext().getSources()) {
			Object array = bindToArray(name, target, itemBinder, elementType, source);
			if (array != null) {
				return array;
			}
		}
		return null;
	}

	private Object bindToArray(ConfigurationPropertyName name, Bindable<?> target,
			AggregateElementBinder itemBinder, ResolvableType elementType,
			ConfigurationPropertySource source) {
		ConfigurationProperty property = source.getConfigurationProperty(name);
		if (property != null) {
			return convert(property, target.getType());
		}
		else {
			List<Object> list = bindIndexed(source, name, itemBinder, elementType);
			Object array = Array.newInstance(elementType.resolve(), list.size());
			for (int i = 0; i < list.size(); i++) {
				Array.set(array, i, list.get(i));
			}
			return (ObjectUtils.isEmpty(array) ? null : array);
		}
	}

	private Object convert(ConfigurationProperty property, ResolvableType type) {
		Object value = property.getValue();
		value = getContext().getPlaceholdersResolver().resolvePlaceholders(value);
		BinderConversionService conversionService = getContext().getConversionService();
		return conversionService.convert(value, type);
	}

	private List<Object> bindIndexed(ConfigurationPropertySource source,
			ConfigurationPropertyName root, AggregateElementBinder elementBinder,
			ResolvableType elementType) {
		MultiValueMap<String, ConfigurationProperty> knownIndexedChildren = getKnownIndexedChildren(
				source, root);
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			ConfigurationPropertyName name = root.append("[" + i + "]");
			Object value = elementBinder.bind(name, Bindable.of(elementType), source);
			if (value == null) {
				break;
			}
			knownIndexedChildren.remove(name.getElement().getValue(Form.UNIFORM));
			list.add(value);
		}
		assertNoUnboundChildren(knownIndexedChildren);
		return list;
	}

	@Override
	protected Object merge(Object existing, Object additional) {
		return additional;
	}

}
