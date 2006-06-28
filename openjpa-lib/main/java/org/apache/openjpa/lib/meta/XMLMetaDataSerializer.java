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
package org.apache.openjpa.lib.meta;

import org.apache.openjpa.lib.log.*;
import org.apache.openjpa.lib.util.*;
import org.apache.openjpa.lib.xml.*;

import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.xml.sax.helpers.*;

import java.io.*;

import java.util.*;

import javax.xml.transform.*;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.*;


/**
 *  <p>Abstract base type for serlializers that transfer groups of objects
 *  to XML.  Includes a way of serializing objects back to the XML files
 *  they were parsed from.</p>
 *
 *  <p>Serializers are not thread safe.</p>
 *
 *  @author Abe White
 *  @nojavadoc */
public abstract class XMLMetaDataSerializer implements MetaDataSerializer {
    private static final Localizer _loc = Localizer.forPackage(XMLMetaDataSerializer.class);
    private static final SAXTransformerFactory _factory = (SAXTransformerFactory) TransformerFactory.newInstance();
    private Log _log = null;

    // current serialization state
    private final AttributesImpl _attrs = new AttributesImpl();
    private ContentHandler _handler = null;
    private int _flags = 0;
    private File _backup = null;

    /**
     *  The log to write to.
      */
    public Log getLog() {
        return _log;
    }

    /**
     *  The log to write to.
      */
    public void setLog(Log log) {
        _log = log;
    }

    public void serialize(int flags) throws IOException {
        serialize((Map) null, flags);
    }

    public void serialize(Map output, int flags) throws IOException {
        Map files = getFileMap();

        if (files == null) {
            return;
        }

        // for each file, serialize objects
        Map.Entry entry;

        for (Iterator itr = files.entrySet().iterator(); itr.hasNext();) {
            entry = (Map.Entry) itr.next();

            File file = (File) entry.getKey();
            Collection fileObjs = (Collection) entry.getValue();

            if ((_log != null) && _log.isInfoEnabled()) {
                _log.info(_loc.get("ser-file", file));
            }

            try {
                TransformerHandler trans = _factory.newTransformerHandler();
                Writer writer;

                if (output == null) {
                    _backup = prepareWrite(file);
                    writer = new FileWriter(file);
                } else {
                    writer = new StringWriter();
                }

                Writer xml = writer;

                if ((flags & PRETTY) > 0) {
                    xml = new XMLWriter(writer);
                }

                trans.setResult(new StreamResult(xml));
                serialize(fileObjs, trans, flags);

                if (output != null) {
                    output.put(file, ((StringWriter) writer).toString());
                }
            } catch (SAXException se) {
                throw new IOException(se.toString());
            } catch (TransformerConfigurationException tce) {
                throw new IOException(tce.toString());
            }
        }
    }

    /**
     *  Prepare to write to the given file.  Back up the file and make sure the
     *  path to it is created.
     */
    protected File prepareWrite(File file) throws IOException {
        File backup = Files.backup(file, false);

        if (backup == null) {
            File parent = file.getParentFile();

            if ((parent != null) && !parent.exists()) {
                parent.mkdirs();
            }
        }

        return backup;
    }

    /**
     *  Returns a {@link Map} with keys of the {@link File} to be
     *  written to, and values of a {@link Collection} of
     *  {@link SourceTracker} instances.
     */
    protected Map getFileMap() {
        Collection objs = getObjects();

        if ((objs == null) || objs.isEmpty()) {
            return null;
        }

        // create a map of files to lists of objects
        Map files = new HashMap();
        File file;
        Collection fileObjs;
        Object obj;

        for (Iterator itr = objs.iterator(); itr.hasNext();) {
            obj = itr.next();
            file = getSourceFile(obj);

            if (file == null) {
                if ((_log != null) && _log.isTraceEnabled()) {
                    _log.trace(_loc.get("no-file", obj));
                }

                continue;
            }

            fileObjs = (Collection) files.get(file);

            if (fileObjs == null) {
                fileObjs = new LinkedList();
                files.put(file, fileObjs);
            }

            fileObjs.add(obj);
        }

        return files;
    }

    /**
     *  Return the source file for the given instance.  By default, checks
     *  to see if the instance implements {@link SourceTracker}.
     */
    protected File getSourceFile(Object obj) {
        if (obj instanceof SourceTracker) {
            return ((SourceTracker) obj).getSourceFile();
        }

        return null;
    }

    public void serialize(File file, int flags) throws IOException {
        if (_log != null) {
            _log.info(_loc.get("ser-file", file));
        }

        _backup = prepareWrite(file);

        FileWriter out = new FileWriter(file.getCanonicalPath(),
                (flags & APPEND) > 0);
        serialize(out, flags);
        out.close();
    }

    public void serialize(Writer out, int flags) throws IOException {
        try {
            if ((flags & PRETTY) > 0) {
                serialize(new StreamResult(new XMLWriter(out)), flags);
            } else {
                serialize(new StreamResult(out), flags);
            }
        } catch (SAXException se) {
            throw new IOException(se.toString());
        }
    }

    /**
     *  Serialize the current set of objects to the given result.
     */
    public void serialize(Result result, int flags) throws SAXException {
        try {
            TransformerHandler trans = _factory.newTransformerHandler();
            trans.setResult(result);
            serialize(trans, flags);
        } catch (TransformerConfigurationException tce) {
            throw new SAXException(tce);
        }
    }

    /**
     *  Serilize the current set of objects to a series of SAX events on the
     *  given handler.
     */
    public void serialize(ContentHandler handler, int flags)
        throws SAXException {
        serialize(getObjects(), handler, flags);
    }

    /**
     *  Serialize the given collection of objects to the given handler.
     */
    private void serialize(Collection objs, ContentHandler handler, int flags)
        throws SAXException {
        if ((_log != null) && _log.isTraceEnabled()) {
            _log.trace(_loc.get("ser-objs", objs));
        }

        _handler = handler;
        _flags = flags;

        try {
            if (!objs.isEmpty()) {
                handler.startDocument();
                serialize(objs);
                handler.endDocument();
            }
        } finally {
            reset();
        }
    }

    /**
     *  Whether this serialization is in verbose mode.
     */
    protected boolean isVerbose() {
        return (_flags & VERBOSE) > 0;
    }

    /**
     *  The backup file made for the current file being parsed.
     */
    protected File currentBackupFile() {
        return _backup;
    }

    /**
     *  Start an element with the current attribute settings.  Clears the
     *  attributes as well.
     */
    protected void startElement(String name) throws SAXException {
        _handler.startElement("", name, name, _attrs);
        _attrs.clear();
    }

    /**
     *  End the current element.
     */
    protected void endElement(String name) throws SAXException {
        _handler.endElement("", name, name);
    }

    /**
     *  Add text to the current element.
     */
    protected void addText(String text) throws SAXException {
        _handler.characters(text.toCharArray(), 0, text.length());
    }

    /**
     *  Add an attribute to the current group.
     */
    protected void addAttribute(String name, String value) {
        _attrs.addAttribute("", name, name, "CDATA", value);
    }

    /**
     *  The current attributes.
     */
    protected Attributes getAttributes() {
        return _attrs;
    }

    /**
     *  Add a comment to the stream.
     */
    protected void addComments(String[] comments) throws SAXException {
        if ((comments == null) || (comments.length == 0) ||
                !(_handler instanceof LexicalHandler)) {
            return;
        }

        LexicalHandler lh = (LexicalHandler) _handler;
        char[] chars;

        for (int i = 0; i < comments.length; i++) {
            chars = comments[i].toCharArray();
            lh.comment(chars, 0, chars.length);
        }
    }

    /**
     *  Write the given entity's comments.  By default, tests if entity is
     *  {@link Commentable}.
     */
    protected void addComments(Object obj) throws SAXException {
        if (obj instanceof Commentable) {
            addComments(((Commentable) obj).getComments());
        }
    }

    /**
     *  Reset serialization state for the next document.
     */
    protected void reset() {
        _attrs.clear();
        _handler = null;
        _flags = 0;
        _backup = null;
    }

    /**
     *  Serialize the given set of objects.
     */
    protected abstract void serialize(Collection objs)
        throws SAXException;

    /**
     *  Return the current set of objects for serialization.
     */
    protected abstract Collection getObjects();
}
