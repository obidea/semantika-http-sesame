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
package org.openrdf.query.resultio.sparqlxml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.TupleQueryResultImpl;
import org.openrdf.query.resultio.AbstractQueryResultIOBooleanTest;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParser;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.QueryResultParseException;
import org.openrdf.query.resultio.TupleQueryResultFormat;

/**
 * @author Peter Ansell
 */
public class SPARQLXMLBooleanTest extends AbstractQueryResultIOBooleanTest {

	@Override
	protected String getFileName() {
		return "test.srx";
	}

	@Override
	protected BooleanQueryResultFormat getBooleanFormat() {
		return BooleanQueryResultFormat.SPARQL;
	}

	@Test
	public void testParseTupleAsBoolean() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		TupleQueryResult tqr = new TupleQueryResultImpl(Arrays.asList("x"), Collections.<BindingSet>emptyList());
		QueryResultIO.write(tqr, TupleQueryResultFormat.SPARQL, out);
		out.flush();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		BooleanQueryResultParser parser = QueryResultIO.createParser(getBooleanFormat());
		try {
			parser.parse(in);
			Assert.fail();
		} catch (QueryResultParseException e) {
			
		}
	}

}
