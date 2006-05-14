/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openjpa.lib.xml;


import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.sax.*;

import org.apache.commons.lang.exception.*;
import org.w3c.dom.*;
import org.xml.sax.*;


/**
 *	<p>The XMLFactory produces validating and non-validating DOM level 2
 *	and SAX level 2 parsers and XSL transformers through JAXP.  It uses 
 *	caching to avoid repeatedly paying the relatively expensive runtime costs
 *	associated with resolving the correct XML implementation through the
 *	JAXP configuration mechanisms.</p>
 *	
 *	@author		Abe White
 *	@nojavadoc
 */
public class XMLFactory
{
	// cache parsers and transformers in all possible configurations
	private static SAXParserFactory[] 		_saxFactories	= null;
	private static DocumentBuilderFactory[]	_domFactories 	= null;
	private static TransformerFactory		_transFactory	= null;
	private static ErrorHandler				_validating;
	static
	{
		_saxFactories = new SAXParserFactory[4];
		_domFactories = new DocumentBuilderFactory[4];
		_validating = new ValidatingErrorHandler ();
	}


	/**
	 *	Return a DocumentBuilder with the specified configuration.
	 */
	public static DocumentBuilder getDOMParser (boolean validating, 
		boolean namespaceAware)
	{
		DocumentBuilder db = checkDOMCache (validating, namespaceAware);
		if (validating)
			db.setErrorHandler (_validating);

		return db;
	}


	/**
	 *	Return a new DOM Document.
	 */
	public static Document getDocument ()
	{
		DocumentBuilder db = checkDOMCache (false, false);
		return db.newDocument ();
	}


	/**
	 *	Return a SAXParser with the specified configuration.
	 */
	public static SAXParser getSAXParser (boolean validating, 
		boolean namespaceAware)
	{
		SAXParser sp = checkSAXCache (validating, namespaceAware);
		if (validating)
		{
			try
			{
				sp.getXMLReader ().setErrorHandler (_validating);
			}
			catch (SAXException se)
			{
				throw new NestableRuntimeException (se);
			}
		}

		return sp;
	}


	/**
	 *	Return a Transformer that will apply the XSL transformation
	 *	from the given source.  If the source is null,
	 *	no transformation will be applied.
	 */
	public static Transformer getTransformer (Source source)
	{
		TransformerFactory fact = checkTransCache ();
		try
		{
			if (source == null)
				return fact.newTransformer ();

			return fact.newTransformer (source);
		}
		catch (TransformerConfigurationException tfce)
		{
			throw new NestableRuntimeException (tfce);
		}
	}


	/**
	 *	Return a Templates for the given XSL source.
	 */
	public static Templates getTemplates (Source source)
	{
		TransformerFactory fact = checkTransCache ();
		try
		{
			return fact.newTemplates (source);
		}
		catch (TransformerConfigurationException tfce)
		{
			throw new NestableRuntimeException (tfce);
		}
	}


	/**
	 *	Return a TransformerHandler for transforming SAX events, applying the
	 *	XSL transform from the given source.  If the source is null, no
	 *	transform will be applied.  
	 */
	public static TransformerHandler getTransformerHandler (Source source)
	{
		SAXTransformerFactory fact = (SAXTransformerFactory) checkTransCache ();
		try
		{
			if (source == null)
				return fact.newTransformerHandler ();

			return fact.newTransformerHandler (source);
		}
		catch (TransformerConfigurationException tfce)
		{
			throw new NestableRuntimeException (tfce);
		}
	}


	/**
	 *	Returns the cached TransformerFactory, creating it if necessary.
	 */
	private static TransformerFactory checkTransCache ()
	{
		// no synchronization necessary; multiple assignments OK
		if (_transFactory == null)
			_transFactory = TransformerFactory.newInstance ();

		return _transFactory;	
	}

 
	/**
	 *	Returns the cached DocumentBuilder matching the given configuration, 
	 *	creating it if necessary.
	 */
	private static DocumentBuilder checkDOMCache (boolean validating, 
		boolean namespaceAware)
	{
		// calculate where the factory with the correct config should
		// be in our array cache
		int arrayIndex = 0;
		if (validating)
			arrayIndex += 2;
		if (namespaceAware)
			arrayIndex += 1;

		try
		{
			DocumentBuilderFactory factory = null;
			factory = _domFactories[arrayIndex];

			// no synchronization necessary; multiple assignments OK
			if (factory == null)
			{
				factory = DocumentBuilderFactory.newInstance ();
				factory.setValidating (validating);
				factory.setNamespaceAware (namespaceAware);
				_domFactories[arrayIndex] = factory;	
			}
			return factory.newDocumentBuilder ();
		}
		catch (ParserConfigurationException pce)
		{
			throw new NestableRuntimeException (pce);
		}
	}


	/**
	 *	Returns the cached SAXParser matching the given configuration, 
	 *	creating it if necessary.
	 */
	private static SAXParser checkSAXCache (boolean validating,
		boolean namespaceAware)
	{
		// calculate where the factory with the correct config should
		// be in our array cache
		int arrayIndex = 0;
		if (validating)
			arrayIndex += 2;
		if (namespaceAware)
			arrayIndex += 1;

		try
		{
			SAXParserFactory factory = null;
			factory = _saxFactories[arrayIndex];

			// no synchronization necessary; multiple assignments OK
			if (factory == null)
			{
				factory = SAXParserFactory.newInstance ();
				factory.setValidating (validating);
				factory.setNamespaceAware (namespaceAware);
				_saxFactories[arrayIndex] = factory;	
			}
			return factory.newSAXParser ();
		}
		catch (ParserConfigurationException pce)
		{
			throw new NestableRuntimeException (pce);
		}
		catch (SAXException se)
		{
			throw new NestableRuntimeException (se);
		}
	}
}
