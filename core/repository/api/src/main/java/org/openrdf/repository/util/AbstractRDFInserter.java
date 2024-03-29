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
package org.openrdf.repository.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openrdf.OpenRDFException;
import org.openrdf.OpenRDFUtil;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * An RDFHandler that adds RDF data to some RDF sink.
 */
public abstract class AbstractRDFInserter extends RDFHandlerBase {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The contexts to add the statements to. If this variable is a non-empty
	 * array, statements will be added to the corresponding contexts.
	 */
	protected Resource[] contexts = new Resource[0];

	/**
	 * Flag indicating whether blank node IDs should be preserved.
	 */
	private boolean preserveBNodeIDs;

	/**
	 * Map that stores namespaces that are reported during the evaluation of the
	 * query. Key is the namespace prefix, value is the namespace name.
	 */
	private final Map<String, String> namespaceMap;

	/**
	 * Map used to keep track of which blank node IDs have been mapped to which
	 * BNode object in case preserveBNodeIDs is false.
	 */
	private final Map<String, BNode> bNodesMap;

	/**
	 * ValueFactory used to create BNodes.
	 */
	private final ValueFactory valueFactory;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFInserter object that preserves bnode IDs and that does
	 * not enforce any context upon statements that are reported to it.
	 */
	protected AbstractRDFInserter(ValueFactory vf) {
		preserveBNodeIDs = true;
		namespaceMap = new HashMap<String, String>();
		bNodesMap = new HashMap<String, BNode>();
		valueFactory = vf;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Sets whether this RDFInserter should preserve blank node IDs.
	 * 
	 * @param preserveBNodeIDs
	 *        The new value for this flag.
	 */
	public void setPreserveBNodeIDs(boolean preserveBNodeIDs) {
		this.preserveBNodeIDs = preserveBNodeIDs;
	}

	/**
	 * Checks whether this RDFInserter preserves blank node IDs.
	 */
	public boolean preservesBNodeIDs() {
		return preserveBNodeIDs;
	}

	/**
	 * Enforces the supplied contexts upon all statements that are reported to
	 * this RDFInserter.
	 * 
	 * @param contexts
	 *        the contexts to use. Use an empty array (not null!) to indicate no
	 *        context(s) should be enforced.
	 */
	public void enforceContext(Resource... contexts) {
		OpenRDFUtil.verifyContextNotNull(contexts);
		this.contexts = Arrays.copyOf(contexts, contexts.length);
	}

	/**
	 * Checks whether this RDFInserter enforces its contexts upon all statements
	 * that are reported to it.
	 * 
	 * @return <tt>true</tt> if it enforces its contexts, <tt>false</tt>
	 *         otherwise.
	 */
	public boolean enforcesContext() {
		return contexts.length != 0;
	}

	/**
	 * Gets the contexts that this RDFInserter enforces upon all statements that
	 * are reported to it (in case <tt>enforcesContext()</tt> returns
	 * <tt>true</tt>).
	 * 
	 * @return A Resource[] identifying the contexts, or an empty array if no
	 *         contexts is enforced.
	 */
	public Resource[] getContexts() {
		return Arrays.copyOf(contexts, contexts.length);
	}

	protected abstract void addNamespace(String prefix, String name) throws OpenRDFException;
	protected abstract void addStatement(Resource subj, URI pred, Value obj, Resource ctxt) throws OpenRDFException;

	@Override
	public void endRDF()
		throws RDFHandlerException
	{
		for (Map.Entry<String, String> entry : namespaceMap.entrySet()) {
			String prefix = entry.getKey();
			String name = entry.getValue();

			try {
				addNamespace(prefix, name);
			}
			catch (OpenRDFException e) {
				throw new RDFHandlerException(e);
			}
		}

		namespaceMap.clear();
		bNodesMap.clear();
	}

	@Override
	public void handleNamespace(String prefix, String name) {
		// FIXME: set namespaces directly when they are properly handled wrt
		// rollback
		// don't replace earlier declarations
		if (prefix != null && !namespaceMap.containsKey(prefix)) {
			namespaceMap.put(prefix, name);
		}
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();
		Resource ctxt = st.getContext();

		if (!preserveBNodeIDs) {
			if (subj instanceof BNode) {
				subj = mapBNode((BNode)subj);
			}

			if (obj instanceof BNode) {
				obj = mapBNode((BNode)obj);
			}

			if (!enforcesContext() && ctxt instanceof BNode) {
				ctxt = mapBNode((BNode)ctxt);
			}
		}

		try {
			addStatement(subj, pred, obj, ctxt);
		}
		catch (OpenRDFException e) {
			throw new RDFHandlerException(e);
		}
	}

	/**
	 * Maps the supplied BNode, which comes from the data, to a new BNode object.
	 * Consecutive calls with equal BNode objects returns the same object
	 * everytime.
	 * 
	 * @throws RepositoryException
	 */
	private BNode mapBNode(BNode bNode) {
		BNode result = bNodesMap.get(bNode.getID());

		if (result == null) {
			result = valueFactory.createBNode();
			bNodesMap.put(bNode.getID(), result);
		}

		return result;
	}
}
