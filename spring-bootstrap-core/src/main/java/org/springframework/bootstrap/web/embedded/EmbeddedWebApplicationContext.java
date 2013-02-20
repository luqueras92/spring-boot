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

package org.springframework.bootstrap.web.embedded;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

// FIXME DC A context that can either run inside a web container or can bootstrap its own
// by expecting a single EmbeddedServletContainerFactort bean

/**
 * A {@link WebApplicationContext} that may be used in a classic servlet container or
 * run from an {@link EmbeddedServletContainer}.  When not running in a classic container
 * a {@link EmbeddedServletContainerFactory} bean should be defined within the context.
 *
 * @author Phillip Webb
 */
public abstract class EmbeddedWebApplicationContext extends
		AbstractRefreshableWebApplicationContext {

	private EmbeddedServletContainer embeddedServletContainer;

	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Add a late binding variant of ServletContextAwareProcessor since
		// the servlet context may not yet be available
		beanFactory.addBeanPostProcessor(new LateBindingServletContextAwareProcessor());
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
	}

	@Override
	protected void onRefresh() {
		super.onRefresh();
		initializeServlet();
	}

	private void initializeServlet() {
		if (getServletContext() != null) {
			registerServletContext();
		}
		else {
			startEmbeddedServlet();
		}
	}

	private void startEmbeddedServlet() {
		try {
			EmbeddedServletContainerFactory provider = getBeanFactory().getBean(
					EmbeddedServletContainerFactory.class);
			this.embeddedServletContainer = provider.getContainer(this,
					new EmbeddedContextLoaderListener(this));
			this.embeddedServletContainer.start();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	protected void doClose() {
		super.doClose();
		if (embeddedServletContainer != null) {
			EmbeddedServletContainer stopping = embeddedServletContainer;
			embeddedServletContainer = null;
			try {
				stopping.stop();
			} catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	protected void contextInitialized(ServletContext servletContext) {
		setServletContext(servletContext);
		registerServletContext();
		initPropertySources();
	}

	private void registerServletContext() {
		WebApplicationContextUtils.registerWebApplicationScopes(
				getBeanFactory(), getServletContext());
		WebApplicationContextUtils.registerEnvironmentBeans(
				getBeanFactory(), getServletContext(), getServletConfig());
	}

	private class EmbeddedContextLoaderListener extends ContextLoaderListener {

		public EmbeddedContextLoaderListener(WebApplicationContext context) {
			super(context);
		}

		@Override
		public void contextInitialized(ServletContextEvent event) {
			EmbeddedWebApplicationContext.this.contextInitialized(event.getServletContext());
			super.contextInitialized(event);
		}

		@Override
		protected void configureAndRefreshWebApplicationContext(
				ConfigurableWebApplicationContext wac, ServletContext sc) {
			// Configuration is already complete
		}
	}

	/**
	 * Alternative {@link ServletContextAwareProcessor} implementation that resolves the
	 * servlet context as late as possible. This is required because the servlet context
	 * may be initially {@code null} when running with an {@link EmbeddedServletContainer}.
	 */
	private class LateBindingServletContextAwareProcessor implements BeanPostProcessor {

		private ServletContextAwareProcessor delegate;

		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (createDelegateIfPossible()) {
				return this.delegate.postProcessBeforeInitialization(bean, beanName);
			}
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (createDelegateIfPossible()) {
				return this.delegate.postProcessAfterInitialization(bean, beanName);
			}
			return bean;
		}

		private boolean createDelegateIfPossible() {
			if (this.delegate == null && getServletContext() != null) {
				this.delegate = new ServletContextAwareProcessor(getServletContext(),
						getServletConfig());
			}
			return this.delegate != null;
		}
	}
}
