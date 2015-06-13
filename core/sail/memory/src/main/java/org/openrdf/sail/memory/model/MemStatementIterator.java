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
package org.openrdf.sail.memory.model;

import info.aduna.iteration.LookAheadIteration;
import info.aduna.lang.ObjectUtil;

/**
 * A StatementIterator that can iterate over a list of Statement objects. This
 * iterator compares Resource and Literal objects using the '==' operator, which
 * is possible thanks to the extensive sharing of these objects in the
 * MemoryStore.
 */
public class MemStatementIterator<X extends Exception> extends LookAheadIteration<MemStatement, X> {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The lists of statements over which to iterate.
	 */
	private final MemStatementList statementList;

	/**
	 * The subject of statements to return, or null if any subject is OK.
	 */
	private final MemResource subject;

	/**
	 * The predicate of statements to return, or null if any predicate is OK.
	 */
	private final MemURI predicate;

	/**
	 * The object of statements to return, or null if any object is OK.
	 */
	private final MemValue object;

	/**
	 * The context of statements to return, or null if any context is OK.
	 */
	private final MemResource[] contexts;

	/**
	 * Flag indicating whether this iterator should only return explicitly added
	 * statements or only return inferred statements.
	 */
	private final Boolean explicit;

	/**
	 * Indicates which snapshot should be iterated over.
	 */
	private final int snapshot;

	/**
	 * The index of the last statement that has been returned.
	 */
	private volatile int statementIdx;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemStatementIterator that will iterate over the statements
	 * contained in the supplied MemStatementList searching for statements that
	 * match the specified pattern of subject, predicate, object and context(s).
	 * 
	 * @param statementList
	 *        the statements over which to iterate.
	 * @param subject
	 *        subject of pattern.
	 * @param predicate
	 *        predicate of pattern.
	 * @param object
	 *        object of pattern.
	 * @param contexts
	 *        context(s) of pattern.
	 */
	public MemStatementIterator(MemStatementList statementList, MemResource subject, MemURI predicate,
			MemValue object, Boolean explicit, int snapshot, MemResource... contexts)
	{
		this.statementList = statementList;
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.contexts = contexts;
		this.explicit = explicit;
		this.snapshot = snapshot;

		this.statementIdx = -1;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Searches through statementList, starting from index
	 * <tt>_nextStatementIdx + 1</tt>, for statements that match the constraints
	 * that have been set for this iterator. If a matching statement has been
	 * found it will be stored in <tt>_nextStatement</tt> and
	 * <tt>_nextStatementIdx</tt> points to the index of this statement in
	 * <tt>_statementList</tt>. Otherwise, <tt>_nextStatement</tt> will set to
	 * <tt>null</tt>.
	 */
	protected MemStatement getNextElement() {
		statementIdx++;

		for (; statementIdx < statementList.size(); statementIdx++) {
			MemStatement st = statementList.get(statementIdx);

			if (isInSnapshot(st) && (subject == null || subject == st.getSubject())
					&& (predicate == null || predicate == st.getPredicate())
					&& (object == null || object == st.getObject()))
			{
				// A matching statement has been found, check if it should be
				// skipped due to explicitOnly, contexts and readMode requirements

				if (contexts != null && contexts.length > 0) {
					boolean matchingContext = false;
					for (int i = 0; i < contexts.length && !matchingContext; i++) {
						matchingContext = ObjectUtil.nullEquals(st.getContext(), contexts[i]);
					}
					if (!matchingContext) {
						// statement does not appear in one of the specified contexts,
						// skip it.
						continue;
					}
				}

				if (explicit != null && explicit.booleanValue() != st.isExplicit()) {
					// Explicit flag does not match
					continue;
				}

				return st;
			}
		}

		// No more matching statements.
		return null;
	}

	private boolean isInSnapshot(MemStatement st) {
		return snapshot < 0 || st.isInSnapshot(snapshot);
	}
}
