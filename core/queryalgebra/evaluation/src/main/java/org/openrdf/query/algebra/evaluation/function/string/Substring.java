/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2011.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.query.algebra.evaluation.function.string;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.FN;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.Function;

/**
 * The SPARQL built-in {@link Function} SUBSTR, as defined in <a
 * href="http://www.w3.org/TR/sparql11-query/#func-substr">SPARQL Query Language
 * for RDF</a>.
 * 
 * @author Jeen Broekstra
 */
public class Substring implements Function {

	public String getURI() {
		return FN.SUBSTRING.toString();
	}

	public Literal evaluate(ValueFactory valueFactory, Value... args)
		throws ValueExprEvaluationException
	{
		if (args.length < 2 || args.length > 3) {
			throw new ValueExprEvaluationException("Incorrect number of arguments for SUBSTR: " + args.length);
		}

		Value argValue = args[0];
		Value startIndexValue = args[1];
		Value lengthValue = null;
		if (args.length > 2) {
			lengthValue = args[2];
		}

		if (argValue instanceof Literal) {
			Literal literal = (Literal)argValue;

			String language = literal.getLanguage();

			// substr function accepts only plain literals (optionally
			// language-tagged) or string-typed literals.
			if (language != null
					|| (literal.getDatatype() == null || XMLSchema.STRING.equals(literal.getDatatype())))
			{
				String lexicalValue = literal.getLabel();

				// determine start index.
				int startIndex = 0;
				if (startIndexValue instanceof Literal) {
					try {
						// xpath:substring startIndex is 1-based.
						startIndex = ((Literal)startIndexValue).intValue() - 1;

						if (startIndex < 0) {
							throw new ValueExprEvaluationException(
									"illegal start index value (expected 1 or larger): " + startIndexValue);
						}
					}
					catch (NumberFormatException e) {
						throw new ValueExprEvaluationException("illegal start index value (expected int value): "
								+ startIndexValue);
					}
				}
				else if (startIndexValue != null) {
					throw new ValueExprEvaluationException("illegal start index value (expected literal value): "
							+ startIndexValue);
				}

				// optionally convert supplied length expression to an end index for
				// the substring.
				int endIndex = lexicalValue.length();
				if (lengthValue instanceof Literal) {
					try {
						int length = ((Literal)lengthValue).intValue();
						endIndex = startIndex + length;
					}
					catch (NumberFormatException e) {
						throw new ValueExprEvaluationException("illegal length value (expected int value): "
								+ lengthValue);
					}
				}
				else if (lengthValue != null) {
					throw new ValueExprEvaluationException("illegal length value (expected literal value): "
							+ lengthValue);
				}

				try {
					lexicalValue = lexicalValue.substring(startIndex, endIndex);

					if (language != null) {
						return valueFactory.createLiteral(lexicalValue, language);
					}
					else if (XMLSchema.STRING.equals(literal.getDatatype())) {
						return valueFactory.createLiteral(lexicalValue, XMLSchema.STRING);
					}
					else {
						return valueFactory.createLiteral(lexicalValue);
					}
				}
				catch (IndexOutOfBoundsException e) {
					throw new ValueExprEvaluationException("could not determine substring", e);
				}
			}
			else {
				throw new ValueExprEvaluationException("unexpected input value for function substring: "
						+ argValue);
			}
		}
		else {
			throw new ValueExprEvaluationException("unexpected input value for function substring: " + argValue);
		}
	}

}
