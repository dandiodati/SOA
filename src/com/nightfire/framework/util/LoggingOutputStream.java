/**
 * Copyright 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.framework.util;

// jdk imports
import java.io.*;
import java.util.*;
import java.text.MessageFormat;

// thirdparty imports

// nightfire imports

/**
 * LoggingOutputStream is an OutputStream that converts lines sent to the
 * output stream to a format suitable for using as a log file.  It is
 * intended as a way to capture stdout with timestamp information.
 */
public class LoggingOutputStream extends OutputStream
{
    /** The default buffer size */
    public static final int DEFAULT_BUFFER_SIZE = 512;

    /** The output stream we wrap */
    protected OutputStream out;

    /** All of the buffers */
    protected HashSet allBuffers;

    /** The local buffer */
    protected LocalBuffer localBuff;

    /** The character encoding to use */
    protected String enc;

    /** Indicates whether logging is enabled or not */
    protected boolean enabled;

    /** The format for log entries */
    protected MessageFormat fmt;

    /** The line separator to use */
    protected byte[] nl;

    /**
     * Constructor for an OutputStream
     *
     * @param out  The OutputStream that logging entries go to
     */
    public LoggingOutputStream(OutputStream out)
    {
        this(out, DEFAULT_BUFFER_SIZE, null, true);
    }

    /**
     * Constructor for an output stream and a flag indicating whether the
     * stream is initially enabled or not
     *
     * @param out     The OutputStream that logging entries go to
     * @param enabled Indicates whether the stream is initially enabled
     */
    public LoggingOutputStream(OutputStream out, boolean enabled)
    {
        this(out, DEFAULT_BUFFER_SIZE, null, enabled);
    }

    /**
     * Constructor for an output stream, a buffer size, character encoding,
     * and an initial enabled state.
     *
     * @param out       The OutputStream that logging entries go to
     * @param buffSize  The size of the buffer to use
     * @param enc       The character encoding to use
     * @param enabled   Indicates whether logging is enabled or not
     */
    public LoggingOutputStream(OutputStream out, int buffSize, String enc,
                               boolean enabled)
    {
        if (buffSize < 1)
            throw new IllegalArgumentException("Buffer size must be > 1.");
        if (out == null)
            throw new NullPointerException(
        "Cannot construct a LoggingOutputStream to wrap a null OutputStream.");

        this.out     = out;
        allBuffers   = new HashSet();
        localBuff    = new LocalBuffer(allBuffers, buffSize);
        this.enc     = enc;
        this.enabled = enabled;
        fmt = new MessageFormat("[{0,date,HH:mm:ss.SSS}] [{1}] ");
        nl = getBytes(System.getProperty("line.separator", "\n"));
    }

    /**
     * Returns the character encoding used
     */
    public String getEncoding()
    {
        return enc;
    }

    /**
     * Sets the character encoding used
     */
    public void setEncoding(String enc)
    {
        this.enc = enc;
    }

    /**
     * Returns the enabled state of the stream
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Sets the enabled state of the stream
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Closes the output stream
     */
    public void close() throws IOException
    {
        if (out == null)
            return;

        enabled = false;
        flush();
        out.close();
        allBuffers.clear();
        out = null;
    }

    /**
     * Flushes the output stream
     */
    public void flush() throws IOException
    {
        synchronized(allBuffers)
        {
            Iterator iter = allBuffers.iterator();
            ArrayList dead = new ArrayList();

            while (iter.hasNext())
            {
                // flush each buffer in the collection
                LogBuffer buff = (LogBuffer)iter.next();
                buff.flushBuffer();

                // check to see if the buffer is still in use
                if (!buff.isAlive())
                    dead.add(buff);
            }

            // now remove any unused buffers
            iter = dead.iterator();
            while (iter.hasNext())
                allBuffers.remove(iter.next());
        }
    }

    /**
     * Writes a byte array to the output stream
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
        // ignore attempts to write to a closed stream
        if (out == null)
            return;

        // ignore it if logging is not enabled
        if (!enabled)
            return;

        // get our buffer
        LogBuffer buff = localBuff.getBuffer();
        
        // break the buffer into lines
        int pos;
        int lineLen = 0;
        for (pos = off; pos < len; pos++)
        {
            // \n and \r signal new lines, empty lines are discarded
            if ((b[pos] == '\n') || (b[pos] == '\r'))
            {
                if ((lineLen != 0))
                    buff.addBytes(b, pos - lineLen, lineLen, true);
                else
                    buff.flushBuffer();
                lineLen = 0;
            }
            else
                lineLen++;
        }

        // partial lines are stored in the buffer
        if (lineLen > 0)
            buff.addBytes(b, len - lineLen, lineLen, false);
    }

    /**
     * Writes a single byte to the output stream
     */
    public void write(int b) throws IOException
    {
        // ignore attempts to write to a closed stream
        if (out == null)
            return;

        // ignore it if logging is not enabled
        if (!enabled)
            return;

        // get our buffer
        LogBuffer buff = localBuff.getBuffer();

        // flush the buffer if this is a new line
        if ((b == '\n') || (b == '\r'))
            buff.flushBuffer();
        // otherwise, add the byte
        else
            buff.addByte((byte)b, false);
    }

    /**
     * Writes a byte array as a complete log entry
     */
    protected void writeEntry(byte[] b, int off, int len, String name)
        throws IOException
    {
        // ignore attempts to write to a closed stream
        if (out == null)
            return;

        // format the message header
        Object[] args = {new Date(), name};
        String hdr = fmt.format(args);
        
        // get the header bytes
        byte[] hdrBytes = getBytes(hdr);

        // write it out
        synchronized (out)
        {
            out.write(hdrBytes);
            out.write(b, off, len);
            out.write(nl);
        }
    }

    /**
     * Returns a string encoded as a byte array
     */
    protected byte[] getBytes(String str)
    {
        byte[] bytes = null;
        if (enc != null)
        {
            try
            {
                bytes = str.getBytes(enc);
            }
            catch (Exception ex)
            {
                // prevent future errors
                enc = null;
            }
        }
        
        if (bytes == null)
            bytes = str.getBytes();

        return bytes;
    }

    /**
     * Pads a string out (left-justified) to a minimum size
     */
    public String leftJustify(String str, int minLen)
    {
        if (str.length() >= minLen)
            return str;

        StringBuffer buff = new StringBuffer(minLen);
        buff.append(str);
        for (int i = str.length(); i < minLen; i++)
            buff.append(' ');

        return buff.toString();
    }

    /**
     * LocalBuffer is a ThreadLocal which maintains a LogBuffer on a
     * per-thread basis.  This prevents writes from separate threads from
     * getting mixed up.
     */
    protected class LocalBuffer extends ThreadLocal
    {
        /** The index of buffers */
        Set buffers;

        /** The size for each buffer */
        int buffSize;

        /**
         * Constructor
         */
        public LocalBuffer(Set buffers, int buffSize)
        {
            this.buffers = buffers;
            this.buffSize = buffSize;
        }

        /**
         * Returns a new LogBuffer for a thread
         */
        protected Object initialValue()
        {
            Thread t = Thread.currentThread();
            LogBuffer buff = new LogBuffer(buffSize, t);
            synchronized (buffers)
            {
                buffers.add(buff);
            }

            return buff;
        }

        /**
         * Convenience for obtaining this thread's LogBuffer
         */
        public LogBuffer getBuffer()
        {
            return (LogBuffer)get();
        }
    }

    /**
     * LogBuffer is a buffer for a thread
     */
    protected class LogBuffer
    {
        /** The buffer */
        protected byte[] buff;

        /** The number of bytes in the buffer */
        protected int count = 0;

        /** The thread we're associated with */
        protected Thread thread;

        /** The name to use for the thread */
        protected String threadName;

        /**
         * Constructor
         */
        public LogBuffer(int size, Thread t)
        {
            buff = new byte[size];
            thread = t;
            threadName = leftJustify(thread.getName(), 20);
        }

        /**
         * Adds bytes to this buffer
         */
        public synchronized void addBytes(byte[] b, int off, int len,
                                          boolean flush)
            throws IOException
        {
            // see if this will overflow the buffer
            if ( (count + len) > buff.length )
            {
                // write out what's in the buffer right now
                flushBuffer();

                // see if the whole thing is too big
                if (len > buff.length)
                {
                    writeEntry(b, off, len, threadName);
                    return;
                }
            }


            // store the bytes
            System.arraycopy(b, off, buff, count, len);
            count += len;

            // flush it, if instructed to do so
            if (flush)
                flushBuffer();
        }

        /**
         * Adds a single byte to this buffer
         */
        public synchronized void addByte(byte b, boolean flush)
            throws IOException
        {
            // see if this will overflow the buffer
            if ( (count + 1) > buff.length )
            {
                // write out what's in the buffer right now
                flushBuffer();
            }

            // store the bytes
            buff[count++] = b;

            // flush it, if instructed to do so
            if (flush)
                flushBuffer();
        }

        /**
         * Flushes this buffer
         */
        public synchronized void flushBuffer() throws IOException
        {
            if (count < 1)
                return;
            
            // log the buffer
            writeEntry(buff, 0, count, threadName);

            // clear the buffer
            count = 0;
        }

        /**
         * Indicates whether this buffer's thread is still alive or not
         */
        public boolean isAlive()
        {
            return thread.isAlive();
        }
    }
}
