/*
 * Licensed to Aduna under one or more contributor license agreements. See the NOTICE.txt file
 * distributed with this work for additional information regarding copyright ownership.
 * 
 * Aduna licenses this file to you under the terms of the Aduna BSD License (the "License"); you may
 * not use this file except in compliance with the License. See the LICENSE.txt file distributed
 * with this work for the full License.
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.openrdf.query.resultio.text.csv;

import org.junit.Ignore;
import org.openrdf.query.resultio.AbstractQueryResultIOTupleTest;
import org.openrdf.query.resultio.TupleQueryResultFormat;

/**
 * TODO disabled for now since CSV format is lossy and therefore does not
 * provide round-trip
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
@Ignore
public class SPARQLCSVTupleTest extends AbstractQueryResultIOTupleTest {

	@Override
	protected String getFileName() {
		return "test.csv";
	}

	@Override
	protected TupleQueryResultFormat getTupleFormat() {
		return TupleQueryResultFormat.CSV;
	}

}
