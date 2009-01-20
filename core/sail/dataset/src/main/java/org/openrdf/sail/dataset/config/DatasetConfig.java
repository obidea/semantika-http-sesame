/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007-2009.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.dataset.config;

import static org.openrdf.sail.dataset.config.DatasetSchema.CLOSED;
import static org.openrdf.sail.dataset.config.DatasetSchema.DATASET;
import static org.openrdf.sail.dataset.config.DatasetSchema.NAME;
import static org.openrdf.sail.dataset.config.DatasetSchema.NAMEDGRAPH;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.config.DelegatingSailImplConfigBase;
import org.openrdf.store.StoreConfigException;

/**
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class DatasetConfig extends DelegatingSailImplConfigBase {

	private Map<URI, String> namedGraphs = new HashMap<URI, String>();

	private boolean closed;

	private ValueFactory vf = ValueFactoryImpl.getInstance();

	public DatasetConfig() {
		super(DatasetFactory.SAIL_TYPE);
	}

	/**
	 * Absent dataset indicates the graph is known, but will be loaded
	 * externally.
	 */
	public void addNamedGraph(URI name, String dataset) {
		namedGraphs.put(name, dataset);
	}

	public void setNamedGraphs(Map<URI, String> graphs) {
		namedGraphs = graphs;
	}

	public Map<URI, String> getNamedGraphs() {
		return namedGraphs;
	}

	/** These are the only datasets that this repository can load. */
	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	@Override
	public Resource export(Model model) {
		Resource resource = super.export(model);
		for (Map.Entry<URI, String> e : getNamedGraphs().entrySet()) {
			BNode node = vf.createBNode();
			model.add(resource, NAMEDGRAPH, node);
			model.add(node, NAME, e.getKey());
			if (e.getValue() != null) {
				model.add(node, DATASET, vf.createLiteral(e.getValue()));
			}
		}
		model.add(resource, CLOSED, vf.createLiteral(closed));
		return resource;
	}

	@Override
	public void parse(Model model, Resource implNode)
		throws StoreConfigException
	{
		super.parse(model, implNode);
		for (Value node : model.filter(implNode, NAMEDGRAPH, null).objects()) {
			URI name = model.filter((Resource)node, NAME, null).objectURI();
			String url = model.filter((Resource)node, DATASET, null).objectString();
			addNamedGraph(name, url);
		}
		if (!model.contains(implNode, CLOSED, null)) {
			closed = model.filter(implNode, CLOSED, null).objectLiteral().booleanValue();
		}
	}
}