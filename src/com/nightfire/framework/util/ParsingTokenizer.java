/*
 * Copyright (c) 2000 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.framework.util;

import java.util.TreeSet;
import java.util.SortedSet;
import java.io.Reader;
import java.io.PushbackReader;
import java.io.IOException;

/**
 * ParsingTokenizer is a utility class for the IDL/XML mapping solution,
 * which is used to read the configuration file.  It is a basic tokenizer
 * capable of handling delimiters made up of multi-character strings.
 */
public class ParsingTokenizer
{
    /**
     * Value used to indicate a token was retrieved
     */
    public static final int RETRIEVED_TOKEN = 1;

    /**
     * Value used to indicate a delimiter was retrieved
     */
    public static final int RETRIEVED_DELIMITER = 2;

    /**
     * Value used to indicate no token or delimiter was retrieved
     */
    public static final int EOF = 0;

    /**
     * Maximum supported delimiter size (a limit is required to use a
     * PushbackInputStream for look ahead)
     */
    public static final int MAXIMUM_DELIMITER_SIZE = 256;

    /**
     * Collection of delimiters
     */
    private TreeSet delimiters = new TreeSet();

    /**
     * Stream being parsed
     */
    private PushbackReader is;

    /**
     * Last token retrieved
     */
    private String lastToken = null;

    /**
     * Last delimiter retrieved
     */
    private String lastDelimiter = null;

    /**
     * Next delimiter retrieved
     */
    private String nextDelimiter = null;

    /**
     * Indicates EOF has been reached
     */
    private boolean eof = false;

    /**
     * Indicates whether or not to trim tokens that are retrieved
     */
    private boolean trimTokens = true;

    /**
     * Empty Constructor.  {@link #setStream(Reader) setStream} must be called
     * before this tokenizer is used.
     */
    public ParsingTokenizer()
    {
        is = null;
    }

    /**
     * Constructor
     *
     * @param is  The reader to tokenize
     */
    public ParsingTokenizer(Reader is)
    {
        // create a pushback reader for look-ahead
        this.is = new PushbackReader(is, MAXIMUM_DELIMITER_SIZE);
    }

    /**
     * Add a delimiter
     *
     * @param delim  The delimiter to add
     *
     * @exception FrameworkException  Thrown if the delimiter exceeds the
     *                                maximum allowed length
     */
    public void addDelimiter(String delim) throws FrameworkException
    {
        // make sure this doesn't exceed our allowed length
        if (delim.length() > MAXIMUM_DELIMITER_SIZE)
        {
            throw new FrameworkException("ParsingTokenizer: The delimiter ["
                + delim
                + "] exceeds the maximum allowable delimiter length of "
                + MAXIMUM_DELIMITER_SIZE + "characters.");
        }

        // add the delimiter
        delimiters.add(delim);
    }

    /**
     * Removes the specified delimiter from this collection, if it is
     * present.
     *
     * @param delim  The delimiter to remove
     */
    public void removeDelimiter(String delim)
    {
        // remove the delimiter
        delimiters.remove(delim);
    }

    /**
     * Returns the last retrieved token.  When {@link nextToken nextToken} is
     * called, it returns the type of value encountered, not the actual token.
     * This method is used to obtain the token.
     *
     * @return The last token retrieved
     */
    public String getToken()
    {
        return lastToken;
    }

    /**
     * Returns the last retrieved delimiter.  When {@link nextToken nextToken}
     * is called, it returns the type of value encountered, not the actual
     * token or delimiter.  This method is used to obtain the delimiter.
     *
     * @return The last delimiter retrieved
     */
    public String getDelimiter()
    {
        return lastDelimiter;
    }

    /**
     * Returns the reader being used by this tokenizer (including any pushed
     * back data).
     *
     * @return The reader used by this tokenizer
     */
    public Reader getStream()
    {
        return is;
    }

    /**
     * Sets the reader to be used by this tokenizer.  This clears out any
     * tokens or delimiters retrieved.
     *
     * @param is  The reader to use
     */
    public void setStream(Reader is)
    {
        // create a pushback reader for look-ahead
        this.is = new PushbackReader(is, MAXIMUM_DELIMITER_SIZE);

        // reset state
        lastToken = null;
        lastDelimiter = null;
        nextDelimiter = null;
        eof = false;
    }

    /**
     * Sets the stream to be used by this tokenizer from another tokenizer.
     * This clears out any tokens or delimiters retrieved.  This can also be
     * accomplished by calling setStream(tok.getStream()), but look ahead by
     * this tokenizer may leave the other tokenizer in a different state than
     * expected if it is used after such a call.  Using this method instead
     * ensures that the two tokenizers remain in sync and usable.
     *
     * @param tok  The tokenizer whose input stream should be used.
     */
    public void setStream(ParsingTokenizer tok)
    {
        // create a pushback reader for look-ahead
        this.is = tok.is;

        // reset state
        lastToken = null;
        lastDelimiter = null;
        nextDelimiter = null;
        eof = false;
    }

    /**
     * Indicates whether or not EOF has been reached on the stream
     *
     * @return true if EOF has been reached, false if it has not
     */
    public boolean isEOF()
    {
        return eof;
    }

    /**
     * Indicates whether or not this tokenizer trims tokens of whitespace
     *
     * @return true if this tokenizer trims tokens
     */
    public boolean isTrimming()
    {
        return trimTokens;
    }

    /**
     * Turn token trimming on or off
     *
     * @param flag  true turns trimming on, false turns it off
     */
    public void setTrimming(boolean flag)
    {
        trimTokens = flag;
    }

    /**
     * Retrieves the next token
     *
     * @return RETRIEVED_TOKEN if a token was encountered, RETRIEVED_DELIMITER
     *         if a delimiter was encountered, or EOF if neither were
     *
     * @exception FrameworkException  Thrown if an IO error occurs while
     *                                accessing the input stream
     */
    public int nextToken() throws FrameworkException
    {
        // first see if we have a delimiter waiting to be returned
        if (nextDelimiter != null)
        {
            // mark it as the last delimiter encountered
            lastDelimiter = nextDelimiter;

            // reset the nextDelimiter indicator
            nextDelimiter = null;

            // indicate we received a delimiter
            return RETRIEVED_DELIMITER;
        }

        // we need a buffer for the token and delimiter
        StringBuffer tokenBuff = new StringBuffer();
        StringBuffer delimBuff = new StringBuffer();

        // try to retrieve a token/delimiter pair
        try
        {
            if (!findNextDelimiter(tokenBuff, delimBuff))
            {
                // get the token
                lastToken = tokenBuff.toString();

                // trim it down if need be
                if (trimTokens)
                    lastToken = lastToken.trim();

                // see if we got a token after all that
                if (lastToken.length() == 0)
                {
                    // no token
                    nextDelimiter = null;
                    lastDelimiter = null;
                    lastToken = null;
                    eof = true;
                    return EOF;
                }

                // save the token (but no delimiter)
                nextDelimiter = null;
                lastDelimiter = null;
                return RETRIEVED_TOKEN;
            }

            // save the token and delimiter
            nextDelimiter = delimBuff.toString();
            lastToken = tokenBuff.toString();
            if (trimTokens)
                lastToken = lastToken.trim();
        }
        catch(IOException ex)
        {
            throw new FrameworkException(ex.toString());
        }

        // see if we got a token or a delimiter
        if ( (lastToken.length() == 0) && (nextDelimiter.length() != 0) )
        {
            // we got a delimiter with no token
            lastDelimiter = nextDelimiter;
            nextDelimiter = null;
            return RETRIEVED_DELIMITER;
        }

        return RETRIEVED_TOKEN;
    }

    /**
     * Moves through the stream until a delimiter is encountered
     *
     * @param tokenBuff  The buffer for the token
     * @param delimBuff  The buffer for the delimiter
     *
     * @return true if a delimiter was encountered, false if one was not
     *         (although tokenBuff may still contain a token, even if a
     *         delimiter was not found)
     */
    private boolean findNextDelimiter(StringBuffer tokenBuff,
                                      StringBuffer delimBuff)
        throws IOException
    {
        int b = is.read();

        // loop until EOF or a matching delimiter is encountered
        while (b != -1)
        {
            // add the byte to the buffer
            delimBuff.append((char)b);

            // get all delimiters starting with this sequence
            SortedSet matching = delimiters.subSet(delimBuff.toString(),
                                              endRange(delimBuff.toString()));

            // see if we have any matches
            if (matching.isEmpty())
            {
                // no matches
            
                // see if this is the first character we've checked for
                if (delimBuff.length() == 1)
                {
                    // add the character to the token buffer and start fresh
                    tokenBuff.append(delimBuff.charAt(0));
                    delimBuff.deleteCharAt(0);

                    // now loop around
                }
                // rewind one character
                else
                {
                    // push back the last byte read
                    is.unread(b);

                    // strip the last byte from the delimiter buffer
                    delimBuff.deleteCharAt(delimBuff.length() - 1);

                    // indicate no match for this next character
                    return false;
                }
            }
            else // the bit of the string we've examined so far matches
                 // at least part of one of the delimiters we're looking for
            {
                // there is a possible match, so try to match the next
                // character (we always look for the longest matching delim)
                if (findNextDelimiter(tokenBuff, delimBuff))
                {
                    // there is a match further on in the string,
                    // so return it
                    return true;
                }
                else // this is the longest string that matches anything
                {
                    // the next character does not match, so see if this
                    // delimiter exactly matches
                    if (delimiters.contains(delimBuff.toString()))
                        return true;
                    else // we only matched the start of a delimiter, not
                         // the full thing
                    {
                        // if there is only one character, add it to the
                        // token buffer
                        if (delimBuff.length() == 1)
                        {
                            tokenBuff.append(delimBuff.charAt(0));
                            delimBuff.deleteCharAt(0);

                            // now loop
                        }
                        else // rollback to the first character that matched
                        {
                            // push back the last byte read
                            is.unread(b);

                            // strip the last byte from the delimiter buffer
                            delimBuff.deleteCharAt(delimBuff.length() - 1);

                            // indicate no match for this next character
                            return false;
                        }
                    }
                }
            }

            // read one more byte from the stream
            b = is.read();
        }

        // we've reached the end of the stream

        // see if our delimiter matches anything
        if ( (delimBuff.length() > 0) &&
             (delimiters.contains(delimBuff.toString())) )
        {
            // we have a match, so stop here
            return true;
        }
        else
        {
            // no match, so return false
            return false;
        }
    }

    /**
     * Returns a value used to mark the end of the range to return when
     * searching for matching delimiters
     *
     * @param prefix  The delimiter prefix to search for
     *
     * @return The corresponding high value to use for matching that prefix
     */
    private String endRange(String prefix)
    {
        // get the length
        int len = prefix.length();

        // get the last character
        char c = prefix.charAt(len - 1);

        // increment the character
        c++;

        // substitute the new character for the last
        return prefix.substring(0, len - 1) + c;
    }
}
