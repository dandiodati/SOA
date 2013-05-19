/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;
import java.lang.reflect.*;
import antlr.*;
import antlr.debug.*;

import antlr.collections.AST;

import java.io.Reader;
import java.io.StringReader;

import com.nightfire.adapter.converter.util.*;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.util.*;

import com.nightfire.adapter.converter.*;
import com.nightfire.adapter.converter.util.XMLVisitor;

/**
 * This class is responsible for processing Nodes of an AST
 * which represent EDI parsed to the beasic structures of
 * EDI, Data Elements, without having parsed them in a manner
 * which gives them meaning within a Transaction Set.
 * 
 * Each Node that is processed by this class has its Type 
 * attribute mapped fmom the value used in the simple EDI
 * grammar tokens, to the value used for the same structure
 * in the more detailed grammars used to parse individual 
 * Transaction Sets. 
 * 
 * Additionally, when processing a Node whose type is 
 * Container, this class will extract the Transaction Set
 * String from each of the container's children and 
 * parser the Transaction Set using its given Parser
 * class. 
 * 
 * This class will then replace the Transaction Node 
 * containing the String Transaction Set with the 
 * AST containing the parsed EDI AST.
 *
 */
public class SimpleEDINodeVisitor extends XMLVisitor{

    /**
     * Defines the AST Node Type attribute value for 
     * a bad Transaction Set.
     * Node Types are integers. Since the grammar doesn't
     * define a type for a bad transaction, a value
     * must be chosen which falls well outside the bounds
     * of what might be defined in the grammar, even given
     * the possibility of expanding the grammar.
     *
     */
    public int ERROR_TRANSACTION = -1;
 
    /**
     * The class which will be used to parse the individual
     * EDI Transaction Sets. 
     *
     */
    private Class parserClass = null;

    /**
     * The class which will be used to perform lexical 
     * analysis on the individual Transaction Sets 
     * prior to parsing.
     *
     */
    private Class lexerClass = null;    

    /**
     * Creates a new <code>SimpleEDINodeVisitor</code> instance.
     *
     * @param XMLMessageGenerator The XMLMessageGenerator into 
     *        which any information extracted from the Nodes 
     *        visted would be placed populated. 
     */
    public SimpleEDINodeVisitor(XMLMessageGenerator gen){

        super(gen);
    }

    /**
     * Set the Parser class to be instantiated for parsing individual 
     * Transaction Sets.
     *
     * @param pClass The Parser class to use for parsing 
     *               individual Transaction Sets. 
     */
    public void setEDIParserClass(Class pClass)
    {
        parserClass = pClass;
    }

    /**
     * Process the Node, mapping the Type attribute, and 
     * parsing any Transaction Sets found in Container Nodes.
     *
     * @param node The Node to visit.
     * @param currentPath Dotted notation String holding the heirarchical
     *                    context which describes where the given node 
     *                    exists in the AST.
     * @return The path given, with the path information for the current
     *         Node added.
     */
    public String visit(AST node, String currentPath){

        switch(node.getType())
        {
            case SimpleEDITokenTypes.CONTAINER : // walk the child transaction sets to parse them.
                                             Debug.log(Debug.MSG_STATUS, "Found CONTAINER node.");
                                             container(node);
                                             node.setType(EDITokenTypes.CONTAINER);
                                             break;
            case SimpleEDITokenTypes.ELEM : 
                                             Debug.log(Debug.MSG_STATUS, "Found ELEM node.");
                                             node.setType(EDITokenTypes.ELEM);
                                             break;
            case SimpleEDITokenTypes.TRANSACTION : 
                                             Debug.log(Debug.MSG_STATUS, "Found TRANSACTION node.");
                                             node.setType(EDITokenTypes.TRANSACTION);
                                             break;
            case SimpleEDITokenTypes.LITERAL_ISA : 
                                             Debug.log(Debug.MSG_STATUS, "Found ISA node.");
                                             node.setType(EDITokenTypes.LITERAL_ISA);
                                             break;
            case SimpleEDITokenTypes.LITERAL_IEA : 
                                             Debug.log(Debug.MSG_STATUS, "Found IEA node.");
                                             node.setType(EDITokenTypes.LITERAL_IEA);
                                             break;
            case SimpleEDITokenTypes.LITERAL_GS : 
                                             Debug.log(Debug.MSG_STATUS, "Found GS node.");
                                             node.setType(EDITokenTypes.LITERAL_GS);
                                             break;
            case SimpleEDITokenTypes.LITERAL_GE : 
                                             Debug.log(Debug.MSG_STATUS, "Found GE node.");
                                             node.setType(EDITokenTypes.LITERAL_GE);
                                             break;
            case SimpleEDITokenTypes.LITERAL_ST : 
                                             Debug.log(Debug.MSG_STATUS, "Found ST node.");
                                             node.setType(EDITokenTypes.LITERAL_ST);
                                             break;
            case SimpleEDITokenTypes.LITERAL_SE :
                                             Debug.log(Debug.MSG_STATUS, "Found SE node.");
                                             node.setType(EDITokenTypes.LITERAL_SE);
                                             break;
            case SimpleEDITokenTypes.SEGMENT : 
                                             Debug.log(Debug.MSG_STATUS, "Found SEGMENT node.");
                                             node.setType(EDITokenTypes.SEGMENT);
                                             break;
            case SimpleEDITokenTypes.ENVELOPE : 
                                             Debug.log(Debug.MSG_STATUS, "Found ENVELOPE node.");
                                             node.setType(EDITokenTypes.ENVELOPE);
                                             break;
            case SimpleEDITokenTypes.SEGTERM : 
                                             Debug.log(Debug.MSG_STATUS, "Found SEGTERM node.");
                                             node.setType(EDITokenTypes.SEGTERM);
                                             break;
            case SimpleEDITokenTypes.SEP : 
                                             Debug.log(Debug.MSG_STATUS, "Found SEP node.");
                                             node.setType(EDITokenTypes.SEP);
                                             break;
            case SimpleEDITokenTypes.FUNCGROUP : 
                                             Debug.log(Debug.MSG_STATUS, "Found FUNCGROUP node.");
                                             node.setType(EDITokenTypes.FUNCGROUP);
                                             break;
   	    default                       : 
                                             if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                                             {
                                                 Debug.log(Debug.MSG_STATUS, "Found general node [" + 
                                                 node.getType() + "]");
                                             }
                                             break;

        }
        return null;
    }

    /**
     * Process a Container Node. 
     * This consists of extracting all Transaction Set Strings from
     * the contained children and parsing them. 
     *
     * @param node The Container Node to proess. 
     */
    private void container(AST node)
   {
        // For each child -- 
        //     if it is a TRANSACTION node
        //         get the text, parse the text into an AST, replace the TRANSACTION node 
        //         with the new parsed TRANSACTION node.

        AST child = node.getFirstChild();
        AST lastChild = null; 
        AST transactionTree = null;
        AST lastTransactionTree = null;
        boolean firstChild = true;
        while(child != null)
        {
            Debug.log(Debug.MSG_STATUS, "Working on child of container node");
            if(SimpleEDITokenTypes.TRANSACTION == child.getType())
            {
                Debug.log(Debug.MSG_STATUS, "Child is a Transaction");
                String transactionSet = child.getText();
                try
                {
                    transactionTree = parseTransactionSet(transactionSet);
                }
                catch(ConverterException e)
                {
                    Debug.log(Debug.ALL_ERRORS, "Failed parsing this transaction set." + 
                                                e.getMessage());
                    transactionTree = new CommonAST();
                    transactionTree.setType(EDITokenTypes.BAD_TRANSACTION);
                    transactionTree.setText(EDIMessageConstants.BAD_TRANSACTION_NODE);
                }
                Debug.log(Debug.MSG_STATUS, "Adding the transaction set to the original AST.");
                if(firstChild)
                {
                    node.setFirstChild(transactionTree);
                }        
                else
                {
                    lastTransactionTree.setNextSibling(transactionTree);
                }
                Debug.log(Debug.MSG_STATUS, 
                          "Completed adding the transaction set to the original AST.");
                firstChild = false;
            }
            lastChild = child;
            child = lastChild.getNextSibling();
            lastTransactionTree = transactionTree;
        }
    }

    /**
     * Parse the Transaction set into an AST.
     *
     * @param transactionSet The Transaction Set to parse.
     * @return The AST representing the parsed Transaction Set.
     * @exception ConverterException if an error occurs
     */
    private AST parseTransactionSet(String transactionSet)
    throws ConverterException
    {
        Debug.log(Debug.MSG_STATUS, "Preparing to parse the transaction set.");
        if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        {
            Debug.log(Debug.MSG_STATUS, transactionSet);
        }
        TokenStream lexer = getLexer(transactionSet);           
        Parser result = parse(lexer);   
        AST transactionTree = result.getAST();
        return transactionTree; 
    }

    /**
     * Construct a TokenStream containing the Tokens
     * of the given Transaction Set based on the lexical
     * analysis built into the configured grammar.
     *
     * @param transactionSet The Transaction Set to lex.
     * @return The TokenStream of EDI tokens. 
     * @exception ConverterException if an error occurs
     */
    private TokenStream getLexer(String transactionSet)
    throws ConverterException
    {
        try
        {
            StringReader reader = new StringReader(transactionSet);
            Debug.log(Debug.MSG_STATUS, "creating Lexer from the transaction set.");
            Class lexerClass = Class.forName("com.nightfire.adapter.converter.edi.EDILexer");
            Class [] constructorArgs = new Class [1];
            constructorArgs[0] = Reader.class;
            Object [] lexerParams = new Object[1];
            lexerParams[0] = reader;
            TokenStream lexer = (TokenStream) ObjectFactory.create(
                                "com.nightfire.adapter.converter.edi.EDILexer", 
                                constructorArgs, lexerParams, TokenStream.class);
            return lexer;
        }                   
        catch(FrameworkException e) // two separate catches to avoid catching the converter exception.
        {
            String message = "Failed to parse the Transaction Set." + e.getMessage();
            Debug.log(Debug.ALL_ERRORS, message);
            Debug.logStackTrace(e);
            throw new ConverterException(message);
        }
        catch(ClassNotFoundException e)
        {
            String message = "Failed to parse the Transaction Set." + e.getMessage();
            Debug.log(Debug.ALL_ERRORS, message);
            Debug.logStackTrace(e);
            throw new ConverterException(message);
        }
    }

    /**
     * Apply the configured Parser to the TokenStream. 
     * Once applied, the Parser will contain the AST
     * representing the parsed Transaction Set.
     *
     * @param lexer The TokenStream of EDI tokens to be parsed.
     * @return The Parser containing the AST representing the 
     *         parsed Transaction Set.
     * @exception ConverterException if an error occurs
     */
    private Parser parse(TokenStream lexer)
    throws ConverterException
    {
        Parser result = null;
        try
        {

            TokenBuffer buffer = new TokenBuffer(lexer);
            Object[] params = new Object[1];
            params[0] = buffer;
            Class[] types = new Class[1];
            types[0] = TokenBuffer.class;

            Debug.log(Debug.MSG_STATUS, "Creating Parser from the Lexer");
            result = (Parser) ObjectFactory.create(parserClass.getName(),
                                                          types,
                                                          params,
                                                          LLkParser.class );
            Debug.log(Debug.MSG_STATUS, "Parsing the transaction set.");
            Method rootMethod = parserClass.getMethod("root", null);
            rootMethod.invoke(result, null);
            Debug.log(Debug.MSG_STATUS, "Completed parsing the transaction set.");

            return result;

        }                   
        catch(Exception e)
        {
            String message = "Failed to parse the Transaction Set." + e.getMessage();
            if(e instanceof InvocationTargetException)
            {
                message = "Failed to parse the Transaction Set." +
                          ((InvocationTargetException)e).getTargetException().getMessage();

            }
            Debug.log(Debug.ALL_ERRORS, message);
            Debug.logStackTrace(e);
            throw new ConverterException(message);
        }
    }
}




