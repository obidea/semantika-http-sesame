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
package org.openrdf.sail.memory;

import info.aduna.iteration.CloseableIteration;
import info.aduna.iteration.ExceptionConvertingIteration;
import info.aduna.iteration.Iteration;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.evaluation.TripleSource;
import org.openrdf.sail.SailException;
import org.openrdf.sail.derived.RdfDataset;

/**
 * Implementation of the TripleSource interface from the Sail Query Model
 */
class MemTripleSource implements TripleSource {

	protected final boolean includeInferred;

	private final ValueFactory vf;

	private final RdfDataset dataset;

	MemTripleSource(ValueFactory vf, RdfDataset dataset, boolean includeInferred) {
		this.vf = vf;
		this.includeInferred = includeInferred;
		this.dataset = dataset;
	}

	public CloseableIteration<? extends Statement, QueryEvaluationException> getStatements(Resource subj,
			URI pred, Value obj, Resource... contexts)
		throws QueryEvaluationException
	{
		try {
			if (includeInferred) {
				return new Eval(dataset.getStatements(subj, pred, obj, contexts));
			}
			else {
				return new Eval(dataset.getExplicit(subj, pred, obj, contexts));
			}
		}
		catch (SailException e) {
			throw new QueryEvaluationException(e);
		}
	}

	public ValueFactory getValueFactory() {
		return vf;
	}

	public static class Eval extends ExceptionConvertingIteration<Statement, QueryEvaluationException> {

		public Eval(Iteration<? extends Statement, ? extends Exception> iter) {
			super(iter);
		}

		protected QueryEvaluationException convert(Exception e) {
			return new QueryEvaluationException(e);
		}

	}
} // end inner class MemTripleSource