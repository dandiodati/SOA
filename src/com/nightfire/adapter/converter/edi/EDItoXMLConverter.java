/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import java.lang.reflect.*;
import java.io.*;

import antlr.*;
import antlr.collections.AST;

import com.nightfire.framework.message.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.framework.util.*;
import com.nightfire.adapter.converter.*;
import com.nightfire.adapter.converter.util.*;

import org.w3c.dom.Document;

/**
 * This Abstract class is responsible for implementing the majority 
 * of the work in converting EDI to XML. 

 * This class Parses EDI in a somewhat more complicated manner than
 * the parent ParsingConverter provides.

 * This class implements EDI Parsing in two stages so that parsing
 * failures for individual transaction sets may be noted. 

 * In order to parse in two stages, this class uses two sets of 
 * grammar related classes. The first set is for parsing the 
 * envelope. The second if for parsing individual transaction 
 * sets. 

 * Because the grammars for groups of transactions sets (request
 * versus response) and for transaction sets defined between 
 * various EDI Versions differ, this class requires that the 
 * parser for use for in parsing transaction sets be provided
 * by any child classes. 
 * 
 * Parser classes are provided by child classes by implementing
 * the getTransactionSetParserClass() methods, which are abastract 
 * here.
 *
 */
public abstract class EDItoXMLConverter extends ParsingConverter{

    /**
     * The original delimiters which have been extracted from 
     * EDI message.
     *
     */
    private EDIDelimiters  oldDelimiters;

    /**
     * Class used to perform Lexical Analysis at the Envelope level.
     *
     */
    private Class envelopeLexerClass;

    /**
     * Class used to Parse the token stream at the envelope level.
     *
     */
    private Class envelopeParserClass;

    /**
     * Class used to process each node of the AST once parsed at 
     * the envelope level.
     *
     */
    private Class envelopeNodeVistorClass;

    /**
     * Class used to perform Lexical analysis at the transaction set level.
     *
     */
    private Class transactionSetLexerClass;

    /**
     * Class used to parse transaction sets.
     *
     */
    private Class transactionSetParserClass;

    /**
     * Class used to process the each node of the AST once it has been 
     * fully parsed to the transaction set level.
     *
     */
    private Class transactionSetNodeVisitorClass;

    /**
     * Creates a new <code>EDItoXMLConverter</code> instance.
     *
     */
    public EDItoXMLConverter()
    {

        super(null, null, SimpleEDINodeVisitor.class);

        try
	{ 
            // get the instances of the envelope parsing classes.
            envelopeLexerClass = getEnvelopeLexerClass();
            envelopeParserClass = getEnvelopeParserClass();
            envelopeNodeVistorClass = getEnvelopeNodeVisitorClass();
        }
        catch(Exception e)
        {
            String message = "Failed to find a gramar class necessary for parsing. \n" + 
		             e.getMessage();
            Debug.error(message);
            // the interface won't let me throw an exception here.
        }
        // reset the converter to be ready to parse an envelope, in case 
        // this instance is cached for reuse.
        setupForEnvelopeParsing();
    }


    /**
     * Return the Object holding the original EDI Delimiters extracted
     * from the EDI message during normalization 
     *
     * @return The Object holding the original EDI Delimiters extracted
     *         from the EDI message during normalization 
     * @exception ConverterException if the EDIMessage has not yet been 
     *                               normalized.
     */
    public EDIDelimiters getOriginalEDIDelimters()
    throws ConverterException
    {
        if(null != oldDelimiters)
        {
            return oldDelimiters;
        }
        else
        {
            String message = "EDIDelimiters accessed before EDI Normalization Complete.";
            Debug.error(message);
            throw new ConverterException(message);
        }
    }

    /**
     * Return the class used to perform lexical analyis at the 
     * envelope level.
     * 
     * @return The class used to perform lexical analysis at the envelope level.
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getEnvelopeLexerClass()
    throws ClassNotFoundException
    {
        // NOTE: The class which is being returned here exists in SPI Common, so 
        // can not be specified as a Class Object at compile time. Hence the 
        // call to Class.forName();
        // This results in a runtime dependency on SPICommon. 
        return Class.forName("com.nightfire.adapter.converter.edi.SimpleEDILexer");
    }

    /**
     * Return the class used for parsing to the envelope level
     *
     * @return The class used to parse to the envelope level
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getEnvelopeParserClass()
    throws ClassNotFoundException
    {
        // NOTE: The class which is being returned here exists in SPI Common, so 
        // can not be specified as a Class Object at compile time. Hence the 
        // call to Class.forName();
        // This results in a runtime dependency on SPICommon. 
        return Class.forName("com.nightfire.adapter.converter.edi.SimpleEDIParser");
    }

    /**
     * Return the class used to process each node of the AST when 
     * parsed at the envelope level.
     *
     * @return The class used to process each node of the AST when
     * parsed to the evnvelope level.
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getEnvelopeNodeVisitorClass()
    throws ClassNotFoundException
    {
        // NOTE: The class which is being returned here exists in SPI Common, so 
        // can not be specified as a Class Object at compile time. Hence the 
        // call to Class.forName();
        // This results in a runtime dependency on SPICommon. 
        return Class.forName("com.nightfire.adapter.converter.edi.SimpleEDINodeVisitor");
    }

    /**
     * Return the Default class used to perform lexical analysis at the transaction 
     * set level when the EDI version is not known, or not relevant.
     *
     * @return The class used to perform lexical analysis at the transaction set level.
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getTransactionSetLexerClass()
    throws ClassNotFoundException
    {
        // NOTE: The class which is being returned here exists in SPI Common, so 
        // can not be specified as a Class Object at compile time. Hence the 
        // call to Class.forName();
        // This results in a runtime dependency on SPICommon. 
        return Class.forName("com.nightfire.adapter.converter.edi.EDILexer");
    }

    /**
     * Return the class used to parse transaction sets for the particular 
     * EDI version given. This is not likely to change for children of this
     * class, but can be overridden. 
     *
     * @param ediVersion The version of the EDI standard the given transaction 
     *                   sets adhere to.
     * @return The class used to perform lexical analysis at the transaction set level.
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getTransactionSetLexerClass(String ediVersion)
    throws ClassNotFoundException
    {
        return getTransactionSetLexerClass();
    }

    /**
     * Return the default class used to parse at the transaction set level
     * when the EDI version is not known.
     *
     * @return The default class used to parse at the transaction set level.
     * @exception ClassNotFoundException if an error occurs
     */
    public abstract Class getTransactionSetParserClass()
	throws ClassNotFoundException;

    /**
     * Return the class used to parse at the transaction set level for 
     * transaction sets which adhere to the given EDI version.
     *
     * @param ediVersion The version of the EDI standard the given transaction 
     *                   sets adhere to.
     * @return The class used to parse at the transaction set level.
     * @exception ClassNotFoundException if an error occurs
     */
    public abstract Class getTransactionSetParserClass(String ediVersion)
	throws ClassNotFoundException;

    /**
     * Return the class used to process each node of the AST once it 
     * has been parsed to the transaction set level.
     *
     * @return The class used to process each node of the AST once it has 
     *         been parsed to the transaction set level.
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getTransactionSetNodeVisitorClass()
    throws ClassNotFoundException
    {
        return com.nightfire.adapter.converter.edi.EDINodeVisitor.class;
        //return Class.forName("com.nightfire.adapter.converter.edi.EDINodeVisitor");
    }

    /**
     * Return the class used to process each node of the AST after it has 
     * be parsed to the transaction set level.
     * This is not likely to change for children of this
     * class, but can be overridden. 
     *
     * @param ediVersion The version of the EDI standard the given transaction 
     *                   sets adhere to.
     * @return The class used to process each node of the AST once it has been
     *         parsed to the transaction set level. 
     * @exception ClassNotFoundException if an error occurs
     */
    public Class getTransactionSetNodeVisitorClass(String ediVersion)
    throws ClassNotFoundException
    {
        return getTransactionSetNodeVisitorClass();
    }

    /**
     * Convert the given EDI to XEDI (XML)
     *
     * @param input Valid EDI.
     * @return DOM containing the converted XEDI.
     * @exception ConverterException if an error occurs
     */
    public Document convert(String input) throws ConverterException
    {
        XMLMessageGenerator output = null;
        try
        {
            output = new XMLMessageGenerator(ParsingConverter.ROOT);
            // pre process the EDI to discover the EDI version 
            // and to normalize the separators. 
            String preProcessedInput = preProcess(input);
 
            AST root = parseEnvelope(preProcessedInput);

            root = reconstituteTransactionSets(root);


            // detailed parse to verify the proper structure for
            // each particular transaction set. 
            // create envelope visitor before changing to transaction set classes.
            SimpleEDINodeVisitor transactionSetVisitor = (SimpleEDINodeVisitor) createVisitor(output);

            root = parseTransactionSets(root, transactionSetVisitor);

            Debug.log(Debug.MSG_STATUS, "Walking parsed tree to generate XML.");
            walker.walk(createVisitor(output), root);
            Debug.log(Debug.MSG_STATUS, "Completed walking parsed tree to generate XML."); 
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, output.generate());
            }
        } 
        catch(FrameworkException e)
        {
            String message = "Failed to complete conversion. \n" + 
		             e.getMessage();
            Debug.error(message);
            // attempt to generate anyway
        }
        finally
        {
            // reset the intial parsing classes in case someone caches and reuses this instance.
            setupForEnvelopeParsing();
        }
	return output.getDocument();
    }

    /**
     * Normalize the EDI to NF standard delimiters,
     * extract the EDI version, and set the 
     * grammar related classes used for parsing the 
     * transaction sets based on the EDI version.
     *
     * @param input The EDI String to be preprocessed.
     * @return The normalized EDI String.
     * @exception ConverterException if an error occurs
     */
    private String preProcess(String input)
    throws ConverterException
    {
        String output = null;
        try
        {
            // retrieve the delimiters from the original EDI.
            Debug.log(Debug.MSG_STATUS, "preprocessing the input string");
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "non normalized edi \n" + input);
            }
            oldDelimiters = EDIDelimiterExtractor.extractDelimiters(input);

            // set the grammar based classes as appropriate for the EDI Version.
            String ediVersion = oldDelimiters.getEdiVersion();
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "EDI Version [" + oldDelimiters.getEdiVersion() + "]");
            }
            transactionSetLexerClass = getTransactionSetLexerClass(ediVersion);
            transactionSetParserClass = getTransactionSetParserClass(ediVersion);
            transactionSetNodeVisitorClass = getTransactionSetNodeVisitorClass(ediVersion);

            // normalize the EDI to NF standard delimiters. 

            output = EDIDelimiterReplacer.replaceDelimiters(input, oldDelimiters);
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "normalized edi \n" + output);
            }
        }
        catch(FrameworkException e)
        {
            String message = "Failed to replace delimiters. \n" + 
		             e.getMessage();
            Debug.error(message);
            throw new ConverterException(message);
        }
        catch(Exception e)
        {
            String message = "Failed to find a gramar class necessary for parsing. \n" + 
		             e.getMessage();
            Debug.error(message);
            throw new ConverterException(message);
        }
        return output;
    }

    /**
     * Parse the EDI into basic Data Elements. 
     *
     * @param input The EDI String to be parsed.
     * @return The AST of Data Elements. 
     * @exception ConverterException if an error occurs
     */
    private AST parseEnvelope(String input) throws ConverterException
    {
        AST root = null;
        try
        {
            // simple parse to get an AST representing the basic 
            // structure of EDI.
            Debug.log(Debug.MSG_STATUS, "Preparing for first pass parse.");
            Reader reader = new StringReader(input);
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, "Using parser class [" + parserClass + "]");
            }
            Parser antlrParser = parse(reader);
            Debug.log(Debug.MSG_STATUS, "Starting first pass parse.");
            root = getRoot(antlrParser);
            Debug.log(Debug.MSG_STATUS, "Completed first pass parser.");
        }
        catch(Exception e)
        {
            String message = "Failed to parse the envelope. \n" + 
		             e.getMessage();
            Debug.error(message);
            throw new ConverterException(message);
        }
        return root;
    }

    /**
     * Walk the AST and rebuild String EDI transaction sets from the 
     * Data Elements. 
     *
     * @param input The Parsed AST.
     * @return AST containing the reconstituted EDI transaction sets.
     * @exception ConverterException if an error occurs
     */
    private AST reconstituteTransactionSets(AST input) throws ConverterException
    {
        AST root = null;
        try
        {
            // create an instance for use with Method.invoke()
            // NOTE: The class which is being returned here exists in SPI Common, so 
            // can not be specified as a Class Object at compile time. Hence the 
            // call to Class.forName();
            // This results in a runtime dependency on SPICommon. 
            Object transSetWalker = ObjectFactory.create(
                                    "com.nightfire.adapter.converter.edi.EDITreeWalker");
            // obtain the root() Method Object.
            Class ediTreeWalkerClass = transSetWalker.getClass();
            Class [] rootMethodParamType = new Class[1];
            rootMethodParamType[0] = AST.class;
            Object [] params = new Object [1];
            params[0] = input;
            Method rootMethod = ediTreeWalkerClass.getMethod(ParsingConverter.ROOT, rootMethodParamType);
            // reconstitute the trans sets as EDI text
            Debug.log(Debug.MSG_STATUS, "Reconstituting transaction sets as text.");
            rootMethod.invoke(transSetWalker, params);
            Debug.log(Debug.MSG_STATUS, "Completed reconstituting transaction sets as text.");
            root = ((TreeParser)transSetWalker).getAST();
        }
        catch(FrameworkException fe)
        {
            String message = "Failed to construct the EDITreeWalker. \n" + 
		             fe.getMessage();
            Debug.error(message);
            throw new ConverterException(message);
        }
        catch(IllegalAccessException iae)
        {        
            String message = "Non public root method for the EDITreeWalker.";
            Debug.error(message);
            throw new ConverterException(message);
        }
        catch(NoSuchMethodException e)
        {        
            String message = "No root method found on the EDITreeWalker.";
            Debug.error(message);
            throw new ConverterException(message);
        }
        catch(InvocationTargetException ivte)
        {        
            String message = "Failed in executing the root method of the EDITreeWalker."
                             + ivte.getTargetException().getMessage();
            Debug.logStackTrace(ivte.getTargetException());
            Debug.error(message);
            throw new ConverterException(message);
        }
        return root;
    }

    /**
     * Walk the AST containing reconstituted transaction sets,
     * parsing each one in turn (defered to the NodeVisitor).
     * 
     * In the first stage of parsing, the EDI is parsed into semantically
     * meaningless Data Elements at the Transaction Set level. 
     * 
     * Following this, the Data Elements in Transaction Sets are reconstituted
     * into plain text EDI Transaction Sets. This is to allow each 
     * Transaction Set to be parsed into semantically meaningful Segments
     * Loops and Compostites, dependent on the type of the individual 
     * Transaction Set. 
     * 
     * The reconstitution step allows the individual Transaction Sets
     * to be parsed in a manner where failure to parse a Transaction Set
     * does not result in failure to parse the entire EDI message.
     *
     * @param input The AST containing reconstituted transaction sets.
     * @param transactionSetVisitor The Visitor used to walk the tree and 
     *                              perform the transaction set parsing.
     * @return The fully parsed EDI AST.
     * @exception ConverterException if an error occurs
     */
    private AST parseTransactionSets(AST input, SimpleEDINodeVisitor transactionSetVisitor) throws ConverterException
    {

        setupForTransactionSetParsing();

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, "Using parser class [" + parserClass + "]");
        }

        transactionSetVisitor.setEDIParserClass(parserClass);

        Debug.log(Debug.MSG_STATUS, "Walking tree for second stage parsing.");

        walker.walk( transactionSetVisitor, input );

        Debug.log(Debug.MSG_STATUS, "Completed walking tree for second stage parse.");

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Writer debugWriter = new StringWriter();
            try
            {
                ((BaseAST)input).xmlSerialize(debugWriter);
            }
            catch(IOException e)
            {
                // do nothing This is only for logging.
                Debug.warning("Failed to serialize the AST for debugging purposes.");
            }
            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
            {
                Debug.log(Debug.MSG_STATUS, debugWriter.toString());
            }
        }

        return input;

    }

    /**
     * Set the parent member variables to the appropriate
     * classes for parsing to the envelope level.
     *
     */
    private void  setupForEnvelopeParsing()
    {
        lexerClass = envelopeLexerClass;
        parserClass = envelopeParserClass;
        visitorClass = envelopeNodeVistorClass;
    }

    /**
     * Set the parent member variables to the appropriate
     * classes for parsing to the transaction set level.
     *
     */
    private void  setupForTransactionSetParsing()
    {
        lexerClass = transactionSetLexerClass;
        parserClass = transactionSetParserClass;
        visitorClass = transactionSetNodeVisitorClass;
    }
} 
