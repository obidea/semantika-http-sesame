/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.http.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Uses {@link HttpClient} to manage HTTP connections.
 * 
 * @author James Leigh
 */
public class SesameClientImpl implements SesameClient {

	private HttpClient httpClient;

	private CloseableHttpClient closeableClient;

	private ExecutorService executor = null;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SesameClientImpl() {
		initialize();
	}

	/**
	 * @return Returns the httpClient.
	 */
	public synchronized HttpClient getHttpClient() {
		if (httpClient == null) {
			return httpClient = closeableClient = createHttpClient();
		}
		return httpClient;
	}

	/**
	 * @param httpClient The httpClient to use for remote/service calls.
	 */
	public synchronized void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	private CloseableHttpClient createHttpClient() {
		return HttpClients.createSystem();
	}

	public synchronized SparqlSession createSparqlSession(String queryEndpointUrl, String updateEndpointUrl) {
		SparqlSession session = new SparqlSession(getHttpClient(), executor);
		session.setQueryURL(queryEndpointUrl);
		session.setUpdateURL(updateEndpointUrl);
		return session;
	}

	public synchronized SesameSession createSesameSession(String serverURL) {
		SesameSession session = new SesameSession(getHttpClient(), executor);
		session.setServerURL(serverURL);
		return session;
	}

	/*-----------------*
	 * Get/set methods *
	 *-----------------*/

	public synchronized void shutDown() {
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
		if (closeableClient != null) {
			HttpClientUtils.closeQuietly(closeableClient);
			closeableClient = null;
		}
	}

	/**
	 * (re)initializes the connection manager and HttpClient (if not already
	 * done), for example after a shutdown has been invoked earlier. Invoking
	 * this method multiple times will have no effect.
	 */
	public synchronized void initialize() {
		if (executor == null) {
			executor = Executors.newCachedThreadPool();
		}
	}

}
