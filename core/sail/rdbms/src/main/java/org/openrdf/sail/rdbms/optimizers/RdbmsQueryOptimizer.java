/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.sail.rdbms.optimizers;

import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.evaluation.EvaluationStrategy;
import org.openrdf.query.algebra.evaluation.impl.BindingAssigner;
import org.openrdf.query.algebra.evaluation.impl.CompareOptimizer;
import org.openrdf.query.algebra.evaluation.impl.ConjunctiveConstraintSplitter;
import org.openrdf.query.algebra.evaluation.impl.ConstantOptimizer;
import org.openrdf.query.algebra.evaluation.impl.QueryJoinOptimizer;
import org.openrdf.query.algebra.evaluation.impl.SameTermFilterOptimizer;
import org.openrdf.query.algebra.evaluation.util.QueryOptimizerList;
import org.openrdf.sail.rdbms.RdbmsValueFactory;
import org.openrdf.sail.rdbms.schema.LiteralTable;
import org.openrdf.sail.rdbms.schema.ResourceTable;

/**
 * Facade to the underlying RDBMS optimizations.
 * 
 * @author James Leigh
 * 
 */
public class RdbmsQueryOptimizer {
	private RdbmsValueFactory vf;
	private ResourceTable uris;
	private ResourceTable longUris;
	private ResourceTable bnodes;
	private LiteralTable literals;
	private SelectQueryOptimizerFactory factory;

	public void setSelectQueryOptimizerFactory(
			SelectQueryOptimizerFactory factory) {
		this.factory = factory;
	}

	public void setValueFactory(RdbmsValueFactory vf) {
		this.vf = vf;
	}

	public void setUriTable(ResourceTable uris) {
		this.uris = uris;
	}

	public void setLongUriTable(ResourceTable longUris) {
		this.longUris = longUris;
	}

	public void setBnodeTable(ResourceTable bnodes) {
		this.bnodes = bnodes;
	}

	public void setLiteralTable(LiteralTable literals) {
		this.literals = literals;
	}

	public TupleExpr optimize(TupleExpr expr, Dataset dataset,
			BindingSet bindings, EvaluationStrategy strategy) {
		// Clone the tuple expression to allow for more aggressive optimisations
		TupleExpr tupleExpr = expr.clone();

		if (!(tupleExpr instanceof QueryRoot)) {
			// Add a dummy root node to the tuple expressions to allow the
			// optimisers to modify the actual root node
			tupleExpr = new QueryRoot(tupleExpr);
		}

		QueryOptimizerList optimizerList = new QueryOptimizerList();
		addCoreOptimizations(strategy, optimizerList);
		addRdbmsOptimizations(optimizerList);
		optimizerList.add(new SqlConstantOptimizer());

		optimizerList.optimize(tupleExpr, dataset, bindings);
		return tupleExpr;
	}

	protected void addCoreOptimizations(EvaluationStrategy strategy,
			QueryOptimizerList optimizerList) {
		optimizerList.add(new BindingAssigner());
		optimizerList.add(new ConstantOptimizer(strategy));
		optimizerList.add(new CompareOptimizer());
		optimizerList.add(new ConjunctiveConstraintSplitter());
		optimizerList.add(new SameTermFilterOptimizer());
		optimizerList.add(new QueryJoinOptimizer());
	}

	protected void addRdbmsOptimizations(QueryOptimizerList optimizerList) {
		optimizerList.add(new ValueIdLookupOptimizer(vf));
		optimizerList.add(factory.createRdbmsFilterOptimizer());
		optimizerList.add(new VarColumnLookupOptimizer());
		ValueJoinOptimizer valueJoins = new ValueJoinOptimizer();
		valueJoins.setBnodeTable(bnodes);
		valueJoins.setUriTable(uris);
		valueJoins.setLongUriTable(longUris);
		valueJoins.setLiteralTable(literals);
		optimizerList.add(valueJoins);
	}

}