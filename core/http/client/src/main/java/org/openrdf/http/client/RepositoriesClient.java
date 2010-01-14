/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008-2010.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.http.client;

import org.openrdf.http.client.connections.HTTPConnectionPool;
import org.openrdf.http.client.helpers.StoreClient;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

/**
 * @author Herko ter Horst
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class RepositoriesClient {

	private final HTTPConnectionPool pool;

	private final StoreClient client;

	public RepositoriesClient(HTTPConnectionPool pool) {
		this.pool = pool;
		this.client = new StoreClient(pool);
	}

	public TupleResult list()
		throws StoreException
	{
		return client.list();
	}

	public RepositoryClient slash(String id) {
		return new RepositoryClient(pool.slash(id));
	}
}
