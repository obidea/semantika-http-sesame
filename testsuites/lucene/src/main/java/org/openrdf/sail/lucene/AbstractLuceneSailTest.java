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
package org.openrdf.sail.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openrdf.sail.lucene.LuceneSailSchema.MATCHES;
import static org.openrdf.sail.lucene.LuceneSailSchema.PROPERTY;
import static org.openrdf.sail.lucene.LuceneSailSchema.QUERY;
import static org.openrdf.sail.lucene.LuceneSailSchema.SCORE;
import static org.openrdf.sail.lucene.LuceneSailSchema.SNIPPET;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

public abstract class AbstractLuceneSailTest {

	public static final String QUERY_STRING;

	public static final URI SUBJECT_1 = new URIImpl("urn:subject1");

	public static final URI SUBJECT_2 = new URIImpl("urn:subject2");

	public static final URI SUBJECT_3 = new URIImpl("urn:subject3");

	public static final URI SUBJECT_4 = new URIImpl("urn:subject4");

	public static final URI SUBJECT_5 = new URIImpl("urn:subject5");

	public static final URI CONTEXT_1 = new URIImpl("urn:context1");

	public static final URI CONTEXT_2 = new URIImpl("urn:context2");

	public static final URI CONTEXT_3 = new URIImpl("urn:context3");

	public static final URI PREDICATE_1 = new URIImpl("urn:predicate1");

	public static final URI PREDICATE_2 = new URIImpl("urn:predicate2");

	public static final URI PREDICATE_3 = new URIImpl("urn:predicate3");

	protected LuceneSail sail;

	protected Repository repository;

	protected RepositoryConnection connection;

	static {
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT Subject, Score ");
		buffer.append("FROM {Subject} <" + MATCHES + "> {} ");
		buffer.append(" <" + QUERY + "> {Query}; ");
		buffer.append(" <" + SCORE + "> {Score} ");
		QUERY_STRING = buffer.toString();
	}

	protected abstract void configure(LuceneSail sail) throws IOException;

	@Before
	public void setUp()
		throws IOException, RepositoryException
	{
		// set logging, uncomment this to get better logging for debugging
		// org.apache.log4j.BasicConfigurator.configure();
		// TODO: disable logging for org.openrdf.query.parser.serql.SeRQLParser,
		// which is not possible
		// to configure using just the Logger

		// setup a LuceneSail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		info.aduna.concurrent.locks.Properties.setLockTrackingEnabled(true);
		sail = new LuceneSail();
		configure(sail);
		sail.setBaseSail(memoryStore);

		// create a Repository wrapping the LuceneSail
		repository = new SailRepository(sail);
		repository.initialize();

		// add some statements to it
		connection = repository.getConnection();
		connection.begin();
		connection.add(SUBJECT_1, PREDICATE_1, new LiteralImpl("one"));
		connection.add(SUBJECT_1, PREDICATE_1, new LiteralImpl("five"));
		connection.add(SUBJECT_1, PREDICATE_2, new LiteralImpl("two"));
		connection.add(SUBJECT_2, PREDICATE_1, new LiteralImpl("one"));
		connection.add(SUBJECT_2, PREDICATE_2, new LiteralImpl("three"));
		connection.add(SUBJECT_3, PREDICATE_1, new LiteralImpl("four"));
		connection.add(SUBJECT_3, PREDICATE_2, new LiteralImpl("one"));
		connection.add(SUBJECT_3, PREDICATE_3, SUBJECT_1);
		connection.add(SUBJECT_3, PREDICATE_3, SUBJECT_2);
		connection.commit();
	}

	@After
	public void tearDown()
		throws IOException, RepositoryException
	{
		if(connection != null) {
			connection.close();
		}
		if(repository != null) {
			repository.shutDown();
		}
	}

	@Test
	public void testTriplesStored()
		throws Exception
	{
		// are the triples stored in the underlying sail?
		assertTrue(connection.hasStatement(SUBJECT_1, PREDICATE_1, new LiteralImpl("one"), false));
		assertTrue(connection.hasStatement(SUBJECT_1, PREDICATE_1, new LiteralImpl("five"), false));
		assertTrue(connection.hasStatement(SUBJECT_1, PREDICATE_2, new LiteralImpl("two"), false));
		assertTrue(connection.hasStatement(SUBJECT_2, PREDICATE_1, new LiteralImpl("one"), false));
		assertTrue(connection.hasStatement(SUBJECT_2, PREDICATE_2, new LiteralImpl("three"), false));
		assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_1, new LiteralImpl("four"), false));
		assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_2, new LiteralImpl("one"), false));
		assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_3, SUBJECT_1, false));
		assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_3, SUBJECT_2, false));
	}

	@Test
	public void testRegularQuery()
		throws RepositoryException, MalformedQueryException, QueryEvaluationException
	{
		// fire a query for all subjects with a given term
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, QUERY_STRING);
		query.setBinding("Query", new LiteralImpl("one"));
		TupleQueryResult result = query.evaluate();

		// check the results
		ArrayList<URI> uris = new ArrayList<URI>();

		BindingSet bindings = null;

		assertTrue(result.hasNext());
		bindings = result.next();
		uris.add((URI)bindings.getValue("Subject"));
		assertNotNull(bindings.getValue("Score"));

		assertTrue(result.hasNext());
		bindings = result.next();
		uris.add((URI)bindings.getValue("Subject"));
		assertNotNull(bindings.getValue("Score"));

		assertTrue(result.hasNext());
		bindings = result.next();
		uris.add((URI)bindings.getValue("Subject"));
		assertNotNull(bindings.getValue("Score"));

		assertFalse(result.hasNext());

		result.close();

		assertTrue(uris.contains(SUBJECT_1));
		assertTrue(uris.contains(SUBJECT_2));
		assertTrue(uris.contains(SUBJECT_3));
	}

	@Test
	public void testComplexQueryOne()
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		// prepare the query
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT Resource, Matching, Score ");
		buffer.append("FROM {Resource} <" + PREDICATE_3 + "> {Matching} ");
		buffer.append(" <" + MATCHES + "> {} ");
		buffer.append(" <" + QUERY + "> {\"one\"}; ");
		buffer.append(" <" + SCORE + "> {Score} ");
		String q = buffer.toString();

		// fire a query for all subjects with a given term
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, q);
		TupleQueryResult result = query.evaluate();

		// check the results
		List<String> results = new ArrayList<String>();
		BindingSet bindings = null;

		assertTrue(result.hasNext());
		bindings = result.next();
		results.add("<" + bindings.getValue("Resource") + ">, " + "<" + bindings.getValue("Matching")
				+ ">");
		assertNotNull(bindings.getValue("Score"));

		assertTrue(result.hasNext());
		bindings = result.next();
		results.add("<" + bindings.getValue("Resource") + ">, " + "<" + bindings.getValue("Matching")
				+ ">");
		assertNotNull(bindings.getValue("Score"));

		assertFalse(result.hasNext());

		result.close();

		assertTrue(results.contains("<" + SUBJECT_3 + ">, <" + SUBJECT_1 + ">"));
		assertTrue(results.contains("<" + SUBJECT_3 + ">, <" + SUBJECT_2 + ">"));
	}

	@Test
	public void testComplexQueryTwo()
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		// prepare the query
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT Resource, Matching, Score ");
		buffer.append("FROM {Resource} <" + PREDICATE_3 + "> {Matching} ");
		buffer.append(" <" + MATCHES + "> {} ");
		buffer.append(" <" + QUERY + "> {\"two\"}; ");
		buffer.append(" <" + SCORE + "> {Score} ");
		String q = buffer.toString();

		// fire a query for all subjects with a given term
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, q);
		TupleQueryResult result = query.evaluate();

		// check the results
		assertTrue(result.hasNext());
		BindingSet bindings = result.next();
		assertEquals(SUBJECT_3, bindings.getValue("Resource"));
		assertEquals(SUBJECT_1, bindings.getValue("Matching"));
		assertNotNull(bindings.getValue("Score"));

		assertFalse(result.hasNext());

		result.close();
	}

	@Test
	public void testMultipleLuceneQueries()
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		// prepare the query
		String[] queries = new String[] {
				"SELECT \n" + "  Resource1, Resource2, R1Score, R2Score \n" + "FROM \n" + "  {Resource1} <"
						+ PREDICATE_3 + "> {Resource2}, \n" + "  {Resource1} <" + MATCHES + "> {} \n" + "    <"
						+ QUERY + "> {\"one\"}; \n" + "    <" + SCORE + "> {R1Score}, \n" + "  {Resource2} <"
						+ MATCHES + "> {} \n" + "    <" + QUERY + "> {\"one\"}; \n" + "    <" + SCORE
						+ "> {R2Score} ",
				"SELECT \n" + "  Resource1, Resource3, R1Score, R3Score \n" + "FROM \n"
						+ "  {Resource2} p21 {Resource1}, \n" + "  {Resource2} p23 {Resource3}, \n"
						+ "  {Resource1} <" + MATCHES + "> {} \n" + "    <" + QUERY + "> {\"one\"}; \n" + "    <"
						+ SCORE + "> {R1Score}, \n" + "  {Resource3} <" + MATCHES + "> {} \n" + "    <" + QUERY
						+ "> {\"one\"}; \n" + "    <" + SCORE + "> {R3Score}" + "WHERE \n"
						+ "  Resource1 != Resource3",
				"SELECT \n" + "  Resource1, Resource3, R1Score, R3Score \n" + "FROM \n"
						+ "  {Resource2} p21 {Resource1}, \n" + "  {Resource2} p23 {Resource3}, \n"
						+ "  {Resource1} <"
						+ MATCHES
						+ "> {} \n"
						+ "    <"
						+ QUERY
						+ "> {\"one\"}; \n"
						+ "    <"
						+ PROPERTY
						+ "> {<"
						+ PREDICATE_1
						+ ">}; \n"
						+ "    <"
						+ SCORE
						+ "> {R1Score}, \n"
						+ "  {Resource3} <"
						+ MATCHES
						+ "> {} \n"
						+ "    <"
						+ QUERY
						+ "> {\"two\"}; \n"
						+ "    <"
						+ PROPERTY
						+ "> {<"
						+ PREDICATE_2 + ">}; \n" + "    <" + SCORE + "> {R3Score}",
				"SELECT \n" + "  Resource1, Resource2, R1Score, R2Score \n" + "FROM \n" + "  {Resource1} <"
						+ MATCHES + "> {} \n" + "    <" + QUERY + "> {\"one\"}; \n" + "    <" + PROPERTY + "> {<"
						+ PREDICATE_1 + ">}; \n" + "    <" + SCORE + "> {R1Score}, \n" + "  {Resource2} <"
						+ MATCHES + "> {} \n" + "    <" + QUERY + "> {\"one\"}; \n" + "    <" + PROPERTY + "> {<"
						+ PREDICATE_2 + ">}; \n" + "    <" + SCORE + "> {R2Score}" };

		ArrayList<List<Map<String, String>>> results = new ArrayList<List<Map<String, String>>>();
		ArrayList<Map<String, String>> resultSet = null;
		Map<String, String> result = null;

		// create a new result set
		resultSet = new ArrayList<Map<String, String>>();

		// one possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_3.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_1.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_3.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_2.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<Map<String, String>>();

		// one possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_1.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_2.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_2.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_1.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<Map<String, String>>();

		// one possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_2.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_1.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_1.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_1.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<Map<String, String>>();

		// one possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_1.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_3.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<String, String>();
		result.put("Resource1", SUBJECT_2.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_3.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		evaluate(queries, results);
	}

	private void evaluate(String[] queries, ArrayList<List<Map<String, String>>> expectedResults)
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		for (int queryID = 0; queryID < queries.length; queryID++) {
			String serql = queries[queryID];
			List<Map<String, String>> expectedResultSet = expectedResults.get(queryID);

			// fire the query
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, serql);
			TupleQueryResult tqr = query.evaluate();

			// check the results
			int actualResults = 0;
			Set<Integer> matched = new HashSet<Integer>();
			while (tqr.hasNext()) {
				BindingSet bs = tqr.next();
				actualResults++;

				boolean matches;
				for (int resultSetID = 0; resultSetID < expectedResultSet.size(); resultSetID++) {
					// ignore results that matched before
					if (matched.contains(resultSetID))
						continue;

					// assume it matches
					matches = true;

					// get the result we compare with now
					Map<String, String> expectedResult = new HashMap<String, String>(
							expectedResultSet.get(resultSetID));

					// get all var names
					Collection<String> vars = new ArrayList<String>(expectedResult.keySet());

					// check if all actual results are expected
					for (String var : vars) {
						String expectedVal = expectedResult.get(var);
						Value actualVal = bs.getValue(var);

						if (expectedVal == null) {
							// don't care about the actual value, as long as there is
							// one
							if (actualVal == null) {
								matches = false;
								break;
							}
						}
						else {
							// compare the values
							if ((actualVal == null) || (expectedVal.compareTo(actualVal.stringValue()) != 0)) {
								matches = false;
								break;
							}
						}

						// remove the matched result so that we do not match it twice
						expectedResult.remove(var);
					}

					// check if expected results were existing
					if (expectedResult.size() != 0) {
						matches = false;
					}

					if (matches) {
						matched.add(resultSetID);
						break;
					}
				}
			}
			tqr.close();

			// the number of matched expected results must be equal to the number
			// of actual results
			assertEquals("How many expected results were retrieved for query #" + queryID + "?",
					expectedResultSet.size(), matched.size());
			assertEquals("How many actual results were retrieved for query #" + queryID + "?",
					expectedResultSet.size(), actualResults);
		}
	}

	@Test
	public void testPredicateLuceneQueries()
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		// prepare the query
		String[] queries = new String[] {
				"SELECT \n" + "  Resource, Score, Snippet \n" + "FROM \n" + "  {Resource} <" + MATCHES
						+ "> {} \n" + "    <" + QUERY + "> {\"one\"}; \n" + "    <" + SCORE + "> {Score}; \n"
						+ "    <" + SNIPPET + "> {Snippet}",
				"SELECT \n" + "  Resource, Score, Snippet \n" + "FROM \n" + "  {Resource} <" + MATCHES
						+ "> {} \n" + "    <" + QUERY + "> {\"five\"}; \n" + "    <" + SCORE + "> {Score}; \n"
						+ "    <" + SNIPPET + "> {Snippet}" };

		ArrayList<List<Map<String, String>>> results = new ArrayList<List<Map<String, String>>>();
		ArrayList<Map<String, String>> resultSet = null;
		Map<String, String> result = null;

		// create a new result set
		resultSet = new ArrayList<Map<String, String>>();

		// one possible result
		result = new HashMap<String, String>();
		result.put("Resource", SUBJECT_1.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>one</B>");
		resultSet.add(result);

		// another possible result
		result = new HashMap<String, String>();
		result.put("Resource", SUBJECT_2.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>one</B>");
		resultSet.add(result);

		// another possible result
		result = new HashMap<String, String>();
		result.put("Resource", SUBJECT_3.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>one</B>");
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<Map<String, String>>();

		// one possible result
		result = new HashMap<String, String>();
		result.put("Resource", SUBJECT_1.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>five</B>");
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		evaluate(queries, results);
	}

	@Test
	public void testSnippetQueries()
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		// prepare the query
		// search for the term "one", but only in predicate 1
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT \n");
		buffer.append("  Resource, Score \n");
		buffer.append("FROM \n");
		buffer.append("  {Resource} <" + MATCHES + "> {} ");
		buffer.append("    <" + QUERY + "> {\"one\"}; ");
		buffer.append("    <" + PROPERTY + "> {<" + PREDICATE_1 + ">}; ");
		buffer.append("    <" + SCORE + "> {Score} ");
		String q = buffer.toString();

		// fire the query
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, q);
		TupleQueryResult result = query.evaluate();

		// check the results
		BindingSet bindings = null;

		// the first result is subject 1 and has a score
		int results = 0;
		Set<URI> expectedSubject = new HashSet<URI>();
		expectedSubject.add(SUBJECT_1);
		expectedSubject.add(SUBJECT_2);
		while (result.hasNext()) {
			results++;
			bindings = result.next();

			// the resource should be among the set of expected subjects, if so,
			// remove it from the set
			assertTrue(expectedSubject.remove(bindings.getValue("Resource")));

			// there should be a score
			assertNotNull(bindings.getValue("Score"));
		}

		// there should have been only 2 results
		assertEquals(2, results);

		result.close();
	}

	/**
	 * Test if the snippets do not accidentially come from the "text" field while
	 * we actually expect them to come from the predicate field.
	 */
	@Test
	public void testSnippetLimitedToPredicate()
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		// more test-data
		RepositoryConnection myconnection = repository.getConnection();
		myconnection.begin();
		// we use the string 'charly' as test-case. the snippets should contain
		// "come" and "unicorn"
		// and 'poor' should not be returned if we limit on predicate1
		// and watch http://www.youtube.com/watch?v=Q5im0Ssyyus like 25mio others
		myconnection.add(SUBJECT_1, PREDICATE_1, new LiteralImpl("come charly lets go to candy mountain"));
		myconnection.add(SUBJECT_1, PREDICATE_1, new LiteralImpl("but the unicorn charly said to goaway"));
		myconnection.add(SUBJECT_1, PREDICATE_2, new LiteralImpl("there was poor charly without a kidney"));
		myconnection.commit();
		myconnection.close();

		{
			// prepare the query
			// search for the term "charly", but only in predicate 1
			StringBuilder buffer = new StringBuilder();
			buffer.append("SELECT \n");
			buffer.append("  Resource, Score, Snippet \n");
			buffer.append("FROM \n");
			buffer.append("  {Resource} <" + MATCHES + "> {} ");
			buffer.append("    <" + QUERY + "> {\"charly\"}; ");
			buffer.append("    <" + PROPERTY + "> {<" + PREDICATE_1 + ">}; ");
			buffer.append("    <" + SNIPPET + "> {Snippet}; ");
			buffer.append("    <" + SCORE + "> {Score} ");
			String q = buffer.toString();

			// fire the query
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, q);
			TupleQueryResult result = query.evaluate();

			// check the results
			BindingSet bindings = null;

			// the first result is subject 1 and has a score
			int results = 0;
			Set<String> expectedSnippetPart = new HashSet<String>();
			expectedSnippetPart.add("come");
			expectedSnippetPart.add("unicorn");
			String notexpected = "poor";
			while (result.hasNext()) {
				results++;
				bindings = result.next();

				// the resource should be among the set of expected subjects, if so,
				// remove it from the set
				String snippet = ((Literal)bindings.getValue("Snippet")).stringValue();
				boolean foundexpected = false;
				for (Iterator<String> i = expectedSnippetPart.iterator(); i.hasNext();) {
					String expected = i.next();
					if (snippet.contains(expected)) {
						foundexpected = true;
						i.remove();
					}
				}
				if (snippet.contains(notexpected))
					fail("snippet '" + snippet + "' contained value '" + notexpected + "' from predicate "
							+ PREDICATE_2);
				if (!foundexpected)
					fail("did not find any of the expected strings " + expectedSnippetPart + " in the snippet "
							+ snippet);

				// there should be a score
				assertNotNull(bindings.getValue("Score"));
			}

			// we found all
			assertTrue("These were expected but not found: " + expectedSnippetPart,
					expectedSnippetPart.isEmpty());

			assertEquals("there should have been 2 results", 2, results);

			result.close();
		}
		/**
		 * DO THE SAME, BUT WIHTOUT PROPERTY RESTRICTION, JUST TO CHECK
		 */
		{
			// prepare the query
			// search for the term "charly" in all predicates
			StringBuilder buffer = new StringBuilder();
			buffer.append("SELECT \n");
			buffer.append("  Resource, Score, Snippet \n");
			buffer.append("FROM \n");
			buffer.append("  {Resource} <" + MATCHES + "> {} ");
			buffer.append("    <" + QUERY + "> {\"charly\"}; ");
			buffer.append("    <" + SNIPPET + "> {Snippet}; ");
			buffer.append("    <" + SCORE + "> {Score} ");
			String q = buffer.toString();

			// fire the query
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, q);
			TupleQueryResult result = query.evaluate();

			// check the results
			BindingSet bindings = null;

			// the first result is subject 1 and has a score
			int results = 0;
			Set<String> expectedSnippetPart = new HashSet<String>();
			expectedSnippetPart.add("come");
			expectedSnippetPart.add("unicorn");
			expectedSnippetPart.add("poor");

			while (result.hasNext()) {
				results++;
				bindings = result.next();

				// the resource should be among the set of expected subjects, if so,
				// remove it from the set
				String snippet = ((Literal)bindings.getValue("Snippet")).stringValue();
				boolean foundexpected = false;
				for (Iterator<String> i = expectedSnippetPart.iterator(); i.hasNext();) {
					String expected = i.next();
					if (snippet.contains(expected)) {
						foundexpected = true;
						i.remove();
					}
				}
				if (!foundexpected)
					fail("did not find any of the expected strings " + expectedSnippetPart + " in the snippet "
							+ snippet);

				// there should be a score
				assertNotNull(bindings.getValue("Score"));
			}

			// we found all
			assertTrue("These were expected but not found: " + expectedSnippetPart,
					expectedSnippetPart.isEmpty());

			assertEquals("there should have been 3 results", 3, results);

			result.close();
		}
	}

	@Test
	public void testGraphQuery()
		throws QueryEvaluationException, MalformedQueryException, RepositoryException
	{
		 URI score = new URIImpl(LuceneSailSchema.NAMESPACE + "score");
		 StringBuilder query = new StringBuilder();
		
		
		 // here we would expect two links from SUBJECT3 to SUBJECT1 and SUBJECT2
		 // and one link from SUBJECT3 to its score
		 query.append("CONSTRUCT DISTINCT \n");
		 query.append("    {r} <" + PREDICATE_3 + "> {r2} , \n");
		 query.append("    {r} <" + score + "> {s} \n");
		 query.append("FROM \n");
		 query.append("    {r} lucenesail:matches {match} lucenesail:query {\"four\"}; \n");
		 query.append("                                   lucenesail:score {s}, \n");
		 query.append("    {r} <" + PREDICATE_3.toString() + "> {r2} \n");
		 query.append("USING NAMESPACE\n");
		 query.append("    lucenesail = <" + LuceneSailSchema.NAMESPACE +
		 "> \n");
		
		 int r = 0;
		 int n = 0;
		 GraphQuery gq = connection.prepareGraphQuery(QueryLanguage.SERQL,
		 query.toString());
		 GraphQueryResult result = gq.evaluate();
		 while(result.hasNext()) {
			 Statement statement = result.next();
			 n++;
			
			 if(statement.getSubject().equals(SUBJECT_3) &&
			 statement.getPredicate().equals(PREDICATE_3) &&
			 statement.getObject().equals(SUBJECT_1)) {
				 r |= 1;
				 continue;
			 }
			 if(statement.getSubject().equals(SUBJECT_3) &&
			 statement.getPredicate().equals(PREDICATE_3) &&
			 statement.getObject().equals(SUBJECT_2)) {
				 r |= 2;
				 continue;
			 }
			 if(statement.getSubject().equals(SUBJECT_3) &&
			 statement.getPredicate().equals(score)) {
				 r |= 4;
				 continue;
			 }
		 }
		
		 assertEquals(3, n);
		 assertEquals(7, r);
	}

	@Test
	public void testQueryWithSpecifiedSubject()
		throws RepositoryException, MalformedQueryException, QueryEvaluationException
	{
		// fire a query with the subject pre-specified
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, QUERY_STRING);
		query.setBinding("Subject", SUBJECT_1);
		query.setBinding("Query", new LiteralImpl("one"));
		TupleQueryResult result = query.evaluate();

		// check that this subject and only this subject is returned
		assertTrue(result.hasNext());
		BindingSet bindings = result.next();
		assertEquals(SUBJECT_1, bindings.getValue("Subject"));
		assertNotNull(bindings.getValue("Score"));
		assertFalse(result.hasNext());

		result.close();
	}

	@Test
	public void testUnionQuery()
		throws RepositoryException, MalformedQueryException, QueryEvaluationException
	{
		String queryStr = "";
		queryStr += "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> ";
		queryStr += "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
		queryStr += "SELECT DISTINCT ?result { ";
		queryStr += "{ ?result search:matches ?match1 . ";
		queryStr += "  ?match1 search:query 'one' ; ";
		queryStr += "          search:property <urn:predicate1> . }";
		queryStr += " UNION ";
		queryStr += "{ ?result search:matches ?match2 . ";
		queryStr += "  ?match2 search:query 'one' ; ";
		queryStr += "          search:property <urn:predicate2> . } ";
		queryStr += "} ";
		
		// fire a query with the subject pre-specified
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
		TupleQueryResult result = query.evaluate();
		while (result.hasNext()) {
			System.out.println(result.next());
		}

		// check that this subject and only this subject is returned
		result.close();
	}

	@Test
	public void testContextHandling()
		throws Exception
	{
		connection.add(SUBJECT_4, PREDICATE_1, new LiteralImpl("sfourponecone"), CONTEXT_1);
		connection.add(SUBJECT_4, PREDICATE_2, new LiteralImpl("sfourptwocone"), CONTEXT_1);
		connection.add(SUBJECT_5, PREDICATE_1, new LiteralImpl("sfiveponecone"), CONTEXT_1);
		connection.add(SUBJECT_5, PREDICATE_1, new LiteralImpl("sfiveponectwo"), CONTEXT_2);
		connection.add(SUBJECT_5, PREDICATE_2, new LiteralImpl("sfiveptwoctwo"), CONTEXT_2);
		connection.commit();
		// connection.close();
		// connection = repository.getConnection();
		// connection.setAutoCommit(false);
		// test querying
		assertQueryResult("sfourponecone", PREDICATE_1, SUBJECT_4);
		assertQueryResult("sfourptwocone", PREDICATE_2, SUBJECT_4);
		assertQueryResult("sfiveponecone", PREDICATE_1, SUBJECT_5);
		assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
		assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
		// blind test to see if this method works:
		assertNoQueryResult("johannesgrenzfurthner");
		// remove a context
		connection.clear(CONTEXT_1);
		connection.commit();
		assertNoQueryResult("sfourponecone");
		assertNoQueryResult("sfourptwocone");
		assertNoQueryResult("sfiveponecone");
		assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
		assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
	}

	@Test
	public void testConcurrentReadingAndWriting()
		throws Exception
	{

		connection.add(SUBJECT_1, PREDICATE_1, new LiteralImpl("sfourponecone"), CONTEXT_1);
		connection.add(SUBJECT_2, PREDICATE_1, new LiteralImpl("sfourponecone"), CONTEXT_1);

		connection.commit();
		// prepare the query

		{
			String queryString = "SELECT Resource " + "FROM {Resource} <" + MATCHES + "> {} " + " <" + QUERY
					+ "> {\"sfourponecone\"} ";
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
			TupleQueryResult result = query.evaluate();

			// check the results
			assertTrue(result.hasNext());
			@SuppressWarnings("unused")
			BindingSet bindings = result.next();

			connection.add(SUBJECT_3, PREDICATE_1, new LiteralImpl("sfourponecone"), CONTEXT_1);

			assertTrue(result.hasNext());
			bindings = result.next();
			result.close();
			connection.commit();
		}
		{
			String queryString = "SELECT Resource " + "FROM {Resource} <" + MATCHES + "> {} " + " <" + QUERY
					+ "> {\"sfourponecone\"} ";
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
			TupleQueryResult result = query.evaluate();

			// check the results
			assertTrue(result.hasNext());
			@SuppressWarnings("unused")
			BindingSet bindings = result.next();

			connection.add(SUBJECT_3, PREDICATE_1, new LiteralImpl("blubbb"), CONTEXT_1);
			connection.commit();

			assertTrue(result.hasNext());
			bindings = result.next();
			result.close();
		}
	}

	/**
	 * we experienced problems with the NULL context and lucenesail in August
	 * 2008
	 *
	 * @throws Exception
	 */
	@Test
	public void testNullContextHandling()
		throws Exception
	{
		connection.add(SUBJECT_4, PREDICATE_1, new LiteralImpl("sfourponecone"));
		connection.add(SUBJECT_4, PREDICATE_2, new LiteralImpl("sfourptwocone"));
		connection.add(SUBJECT_5, PREDICATE_1, new LiteralImpl("sfiveponecone"));
		connection.add(SUBJECT_5, PREDICATE_1, new LiteralImpl("sfiveponectwo"), CONTEXT_2);
		connection.add(SUBJECT_5, PREDICATE_2, new LiteralImpl("sfiveptwoctwo"), CONTEXT_2);
		connection.commit();
		// connection.close();
		// connection = repository.getConnection();
		// connection.setAutoCommit(false);
		// test querying
		assertQueryResult("sfourponecone", PREDICATE_1, SUBJECT_4);
		assertQueryResult("sfourptwocone", PREDICATE_2, SUBJECT_4);
		assertQueryResult("sfiveponecone", PREDICATE_1, SUBJECT_5);
		assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
		assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
		// blind test to see if this method works:
		assertNoQueryResult("johannesgrenzfurthner");
		// remove a context
		connection.clear((Resource)null);
		connection.commit();
		assertNoQueryResult("sfourponecone");
		assertNoQueryResult("sfourptwocone");
		assertNoQueryResult("sfiveponecone");
		assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
		assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
	}

	@Test
	public void testFuzzyQuery()
		throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		// prepare the query
		// search for the term "one" with 80% fuzzyness
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT \n");
		buffer.append("  Resource, Score \n");
		buffer.append("FROM \n");
		buffer.append("  {Resource} <" + MATCHES + "> {} ");
		buffer.append("    <" + QUERY + "> {\"one~0.8\"}; ");
		buffer.append("    <" + SCORE + "> {Score} ");
		String q = buffer.toString();

		// fire the query
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, q);
		TupleQueryResult result = query.evaluate();

		// check the results
		BindingSet bindings = null;

		// the first result is subject 1 and has a score
		int results = 0;
		Set<URI> expectedSubject = new HashSet<URI>();
		expectedSubject.add(SUBJECT_1);
		expectedSubject.add(SUBJECT_2);
		expectedSubject.add(SUBJECT_3);
		while (result.hasNext()) {
			results++;
			bindings = result.next();

			// the resource should be among the set of expected subjects, if so,
			// remove it from the set
			assertTrue(expectedSubject.remove(bindings.getValue("Resource")));

			// there should be a score
			assertNotNull(bindings.getValue("Score"));
		}

		// there should have been 3 results
		assertEquals(3, results);

		result.close();
	}

	@Test
	public void testReindexing()
		throws Exception
	{
		sail.reindex();
		testComplexQueryTwo();
	}

	@Test
	public void testPropertyVar()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT \n");
		buffer.append("  Resource, Property \n");
		buffer.append("FROM \n");
		buffer.append("  {Resource} <" + MATCHES + "> {} ");
		buffer.append("    <" + QUERY + "> {\"one\"}; ");
		buffer.append("    <" + PROPERTY + "> {Property} ");
		String q = buffer.toString();

		// fire the query
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, q);
		TupleQueryResult result = query.evaluate();

		int results = 0;
		Map<URI,URI> expectedSubject = new HashMap<URI,URI>();
		expectedSubject.put(SUBJECT_1, PREDICATE_1);
		expectedSubject.put(SUBJECT_2, PREDICATE_1);
		expectedSubject.put(SUBJECT_3, PREDICATE_2);
		while (result.hasNext()) {
			results++;
			BindingSet bindings = result.next();

			// the resource should be among the set of expected subjects, if so,
			// remove it from the set
			Value subject = bindings.getValue("Resource");
			URI expectedProperty = expectedSubject.remove(subject);
			assertEquals("For subject "+subject, expectedProperty, bindings.getValue("Property"));
		}

		// there should have been 3 results
		assertEquals(3, results);

		result.close();
	}

	protected void assertQueryResult(String literal, URI predicate, Resource resultUri)
		throws Exception
	{
		// fire a query for all subjects with a given term
		String queryString = "SELECT Resource " + "FROM {Resource} <" + MATCHES + "> {} " + " <" + QUERY
				+ "> {\"" + literal + "\"} ";
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
		TupleQueryResult result = query.evaluate();
		try {
			// check the result
			assertTrue("query for literal '" + literal + " did not return any results, expected was "
					+ resultUri, result.hasNext());
			BindingSet bindings = result.next();
			assertEquals("query for literal '" + literal + " did not return the expected resource", resultUri,
					bindings.getValue("Resource"));
			assertFalse(result.hasNext());
		}
		finally {
			result.close();
		}
	}

	protected void assertNoQueryResult(String literal)
		throws Exception
	{
		// fire a query for all subjects with a given term
		String queryString = "SELECT Resource " + "FROM {Resource} <" + MATCHES + "> {} " + " <" + QUERY
				+ "> {\"" + literal + "\"} ";
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
		TupleQueryResult result = query.evaluate();
		try {

			// check the result
			assertFalse("query for literal '" + literal + " did return results, which was not expected.",
					result.hasNext());
		}
		finally {
			result.close();
		}
	}

}
