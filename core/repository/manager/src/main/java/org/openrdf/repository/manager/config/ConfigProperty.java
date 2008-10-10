/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.openrdf.repository.manager.config;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDFS;

/**
 * @author james
 */
public class ConfigProperty {

	private URI pred;

	private Value defaultValue;

	private Locale locale;

	private Model schema;

	public ConfigProperty(URI predicate, Value defaultValue, Locale locale, Model schema) {
		this.defaultValue = defaultValue;
		this.locale = locale;
		this.pred = predicate;
		this.schema = schema;
	}

	public URI getPredicate() {
		return pred;
	}

	public String getLabel() {
		for (Value value : schema.objects(pred, RDFS.LABEL)) {
			// TODO check the language
			return value.stringValue();
		}
		return null;
	}

	public String getComment() {
		for (Value value : schema.objects(pred, RDFS.COMMENT)) {
			// TODO check the language
			return value.stringValue();
		}
		return null;
	}

	public URI getRange() {
		for (Value value : schema.objects(pred, RDFS.RANGE)) {
			if (value instanceof URI) {
				return (URI) value;
			}
		}
		return null;
	}

	public Literal getDefaultLiteral() {
		if (defaultValue instanceof Literal) {
			return (Literal) defaultValue;
		}
		return null;
	}

	public List<Literal> getPossibleLiterals() {
		return Collections.singletonList(getDefaultLiteral());
	}

}