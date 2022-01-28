/*
 * Copyright (c) 1996, 2005, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.th3ee9ine.java.util.io;


import java.io.IOException;
import java.io.Writer;

/**
 * Abstract class for writing filtered character streams.
 * The abstract class <code>FilterWriter</code> itself
 * provides default methods that pass all requests to the
 * contained stream. Subclasses of <code>FilterWriter</code>
 * should override some of these methods and may also
 * provide additional methods and fields.
 *
 * @author      Mark Reinhold
 * @since       JDK1.1
 */

public abstract class FilterWriter extends java.io.Writer {

    /**
     * The underlying character-output stream.
     */
    protected java.io.Writer out;

    /**
     * Create a new filtered writer.
     *
     * @param out  a Writer object to provide the underlying stream.
     * @throws NullPointerException if <code>out</code> is <code>null</code>
     */
    protected FilterWriter(Writer out) {
        super(out);
        this.out = out;
    }

    /**
     * Writes a single character.
     *
     * @exception java.io.IOException  If an I/O error occurs
     */
    public void write(int c) throws java.io.IOException {
        out.write(c);
    }

    /**
     * Writes a portion of an array of characters.
     *
     * @param  cbuf  Buffer of characters to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     * @exception java.io.IOException  If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws java.io.IOException {
        out.write(cbuf, off, len);
    }

    /**
     * Writes a portion of a string.
     *
     * @param  str  String to be written
     * @param  off  Offset from which to start reading characters
     * @param  len  Number of characters to be written
     *
     * @exception java.io.IOException  If an I/O error occurs
     */
    public void write(String str, int off, int len) throws java.io.IOException {
        out.write(str, off, len);
    }

    /**
     * Flushes the stream.
     *
     * @exception java.io.IOException  If an I/O error occurs
     */
    public void flush() throws java.io.IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }

}
