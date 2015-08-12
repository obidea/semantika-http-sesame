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
package org.openrdf.rio;

/**
 * An interface defining methods for receiving warning and error messages from
 * an RDF parser.
 */
public interface ParseErrorListener {

	/**
	 * Reports a warning from the parser. Warning messages are generated by the
	 * parser when it encounters data that is syntactically correct but which is
	 * likely to be a typo. Examples are the use of unknown or deprecated RDF
	 * URIs, e.g. <tt>rdfs:Property</tt> instead of <tt>rdf:Property</tt>.
	 *
	 * @param msg
	 *        A warning message.
	 * @param lineNo
	 *        A line number related to the warning, or -1 if not available or
	 *        applicable.
	 * @param colNo
	 *        A column number related to the warning, or -1 if not available or
	 *        applicable.
	 * @since 4.0
	 */
	public void warning(String msg, long lineNo, long colNo);

	/**
	 * Reports an error from the parser. Error messages are generated by the
	 * parser when it encounters an error in the RDF document. The parser will
	 * try its best to recover from the error and continue parsing when
	 * <tt>stopAtFirstError</tt> has been set to <tt>false</tt>.
	 *
	 * @param msg
	 *        A error message.
	 * @param lineNo
	 *        A line number related to the error, or -1 if not available or
	 *        applicable.
	 * @param colNo
	 *        A column number related to the error, or -1 if not available or
	 *        applicable.
	 * @see org.openrdf.rio.RDFParser#setStopAtFirstError
	 * @since 4.0
	 */
	public void error(String msg, long lineNo, long colNo);

	/**
	 * Reports a fatal error from the parser. A fatal error is an error of which
	 * the RDF parser cannot recover. The parser will stop parsing directly
	 * after it reported the fatal error. Example fatal errors are unbalanced
	 * start- and end-tags in an XML-encoded RDF document.
	 *
	 * @param msg
	 *        A error message.
	 * @param lineNo
	 *        A line number related to the error, or -1 if not available or
	 *        applicable.
	 * @param colNo
	 *        A column number related to the error, or -1 if not available or
	 *        applicable.
	 * @since 4.0
	 */
	public void fatalError(String msg, long lineNo, long colNo);
}
