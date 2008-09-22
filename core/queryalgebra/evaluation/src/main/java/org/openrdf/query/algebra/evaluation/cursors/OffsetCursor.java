/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra.evaluation.cursors;

import org.openrdf.StoreException;
import org.openrdf.query.Cursor;
import org.openrdf.query.base.FilteringCursor;


/**
 * An Iteration that skips the first <tt>offset</tt> elements from an
 * underlying Iteration.
 *
 * @author James Leigh
 */
public class OffsetCursor<E> extends FilteringCursor<E> {

	private int offset;

	public OffsetCursor(Cursor<? extends E> cursor, int offset) {
		super(cursor);
		this.offset = offset;
	}

	@Override
	protected boolean accept(E next)
		throws StoreException
	{
		return --offset < 0;
	}

}