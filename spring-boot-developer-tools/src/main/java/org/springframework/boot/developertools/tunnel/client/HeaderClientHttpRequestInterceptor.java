/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.tunnel.client;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * Allows populating an arbitrary header with a value. For example, it might be used to
 * provide an X-AUTH-TOKEN and value for security purposes.
 *
 * @author Rob Winch
 * @since 1.3.0
 */
public class HeaderClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private final String name;

	private final String value;

	/**
	 * Creates a new {@link HeaderClientHttpRequestInterceptor} instance.
	 * @param headerName the header name to populate. Cannot be null or empty.
	 * @param headerValue the header value to populate. Cannot be null or empty.
	 */
	public HeaderClientHttpRequestInterceptor(String headerName, String headerValue) {
		Assert.hasLength(headerName, "HeaderName" + " must not be empty");
		Assert.hasLength(headerValue, "HeaderValue" + " must not be empty");
		this.name = headerName;
		this.value = headerValue;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		request.getHeaders().add(this.name, this.value);
		return execution.execute(request, body);
	}

}
