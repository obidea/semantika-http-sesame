/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.resultio.binary;

import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.BNODE_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.DATATYPE_LITERAL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.ERROR_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.FORMAT_VERSION;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.LANG_LITERAL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.MAGIC_NUMBER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.MALFORMED_QUERY_ERROR;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.NAMESPACE_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.NULL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.PLAIN_LITERAL_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.QNAME_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.QUERY_EVALUATION_ERROR;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.REPEAT_RECORD_MARKER;
import static org.openrdf.query.resultio.binary.BinaryQueryResultConstants.TABLE_END_RECORD_MARKER;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.ListBindingSet;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;

/**
 * Writer for the binary tuple result format. The format is explained in
 * {@link BinaryQueryResultConstants}.
 * 
 * @author Arjohn Kampman
 */
public class BinaryQueryResultWriter implements TupleQueryResultWriter {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The output stream to write the results table to.
	 */
	private DataOutputStream out;

	private CharsetEncoder charsetEncoder = Charset.forName("UTF-8").newEncoder();

	/**
	 * Map containing the namespace IDs (Integer objects) that have been defined
	 * in the document, stored using the concerning namespace (Strings).
	 */
	private Map<String, Integer> namespaceTable = new HashMap<String, Integer>(32);

	private int nextNamespaceID;

	private BindingSet previousBindings;

	private List<String> bindingNames;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public BinaryQueryResultWriter(OutputStream out) {
		this.out = new DataOutputStream(out);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public final TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.BINARY;
	}

	public void startQueryResult(List<String> bindingNames)
		throws TupleQueryResultHandlerException
	{
		// Copy supplied column headers list and make it unmodifiable
		bindingNames = new ArrayList<String>(bindingNames);
		this.bindingNames = Collections.unmodifiableList(bindingNames);

		try {
			out.write(MAGIC_NUMBER);
			out.writeInt(FORMAT_VERSION);

			out.writeInt(this.bindingNames.size());
			
			for (String bindingName : this.bindingNames) {
				writeString(bindingName);
			}

			List<Value> nullTuple = Collections.nCopies(this.bindingNames.size(), (Value)null);
			previousBindings = new ListBindingSet(this.bindingNames, nullTuple);
			nextNamespaceID = 0;
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	public void endQueryResult()
		throws TupleQueryResultHandlerException
	{
		try {
			out.writeByte(TABLE_END_RECORD_MARKER);
			out.flush();
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	public void handleSolution(BindingSet bindingSet)
		throws TupleQueryResultHandlerException
	{
		try {
			for (String bindingName : bindingNames) {
				Value value = bindingSet.getValue(bindingName);

				if (value == null) {
					writeNull();
				}
				else if (value.equals(previousBindings.getValue(bindingName))) {
					writeRepeat();
				}
				else if (value instanceof URI) {
					writeQName((URI)value);
				}
				else if (value instanceof BNode) {
					writeBNode((BNode)value);
				}
				else if (value instanceof Literal) {
					writeLiteral((Literal)value);
				}
				else {
					throw new TupleQueryResultHandlerException("Unknown Value object type: " + value.getClass());
				}
			}

			previousBindings = bindingSet;
		}
		catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	private void writeNull()
		throws IOException
	{
		out.writeByte(NULL_RECORD_MARKER);
	}

	private void writeRepeat()
		throws IOException
	{
		out.writeByte(REPEAT_RECORD_MARKER);
	}

	private void writeQName(URI uri)
		throws IOException
	{
		// Check if the URI has a new namespace
		String namespace = uri.getNamespace();

		Integer nsID = namespaceTable.get(namespace);

		if (nsID == null) {
			// Generate a ID for this new namespace
			nsID = writeNamespace(namespace);
		}

		out.writeByte(QNAME_RECORD_MARKER);
		out.writeInt(nsID.intValue());
		writeString(uri.getLocalName());
	}

	private void writeBNode(BNode bnode)
		throws IOException
	{
		out.writeByte(BNODE_RECORD_MARKER);
		writeString(bnode.getID());
	}

	private void writeLiteral(Literal literal)
		throws IOException
	{
		String label = literal.getLabel();
		String language = literal.getLanguage();
		URI datatype = literal.getDatatype();

		int marker = PLAIN_LITERAL_RECORD_MARKER;

		if (datatype != null) {
			String namespace = datatype.getNamespace();

			if (!namespaceTable.containsKey(namespace)) {
				// Assign an ID to this new namespace
				writeNamespace(namespace);
			}

			marker = DATATYPE_LITERAL_RECORD_MARKER;
		}
		else if (language != null) {
			marker = LANG_LITERAL_RECORD_MARKER;
		}

		out.writeByte(marker);
		writeString(label);

		if (datatype != null) {
			writeQName(datatype);
		}
		else if (language != null) {
			writeString(language);
		}
	}

	/**
	 * Writes an error msg to the stream.
	 * 
	 * @param errType
	 *        The error type.
	 * @param msg
	 *        The error message.
	 * @throws IOException
	 *         When the error could not be written to the stream.
	 */
	public void error(QueryErrorType errType, String msg)
		throws IOException
	{
		out.writeByte(ERROR_RECORD_MARKER);

		if (errType == QueryErrorType.MALFORMED_QUERY_ERROR) {
			out.writeByte(MALFORMED_QUERY_ERROR);
		}
		else {
			out.writeByte(QUERY_EVALUATION_ERROR);
		}

		writeString(msg);
	}

	private Integer writeNamespace(String namespace)
		throws IOException
	{
		out.writeByte(NAMESPACE_RECORD_MARKER);
		out.writeInt(nextNamespaceID);
		writeString(namespace);

		Integer result = new Integer(nextNamespaceID);
		namespaceTable.put(namespace, result);

		nextNamespaceID++;

		return result;
	}

	private void writeString(String s)
		throws IOException
	{
		ByteBuffer byteBuf = charsetEncoder.encode(CharBuffer.wrap(s));
		out.writeInt(byteBuf.remaining());
		out.write(byteBuf.array(), 0, byteBuf.remaining());
	}
}