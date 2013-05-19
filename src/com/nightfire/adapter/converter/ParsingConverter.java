/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter;

import java.lang.reflect.*;

import antlr.Parser;
import antlr.LLkParser;
import antlr.TokenBuffer;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.RecognitionException;

import antlr.collections.AST;

import java.io.Reader;
import java.io.StringReader;

import com.nightfire.adapter.converter.util.*;

import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.ObjectFactory;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

import org.w3c.dom.Document;

/**
* This is a string to XML converter that uses an ANLTR generated parser
* to parse the input string. A visitor class is specified to walk to tree
* structure and generate the output XML.
*/
public abstract class ParsingConverter extends StringToXMLConverter{

   /**
   * The parsing converter needs to know the name of the initial method
   * to call on the parser class. By default, the name of this entry method
   * is "root". 
   */
   public static final String ROOT = "root";

   /**
   * The class that will be constructed in order to create a TokenStream
   * from the input string. This class must extend antlr.TokenStream.
   */
   protected Class lexerClass;

   /**
   * The parser class that will be contructed to parse the tokens.
   * This class must extend antlr.LLkParser.
   */
   protected Class parserClass;

   /**
   * The visitor class is used by the tree walker to visit each node.
   * This class should generate the appropriate XML for each node.
   * This class must implement the
   * com.nightfire.adapter.converter.util.XMLVisitor interface.
   */
   protected Class visitorClass;

   /**
   * This is the tree walker that will be used to traverse the AST tree.
   * By default, this is a DepthFirstTreeWalker.
   */
   protected TreeWalker walker = new DepthFirstTreeWalker();

   /**
   * The name that will be used as a root if it is necessary to create an
   * XML message generator.
   */
   private String rootName = ROOT;

   /**
   *
   * @param lexerClass a class that extends antlr.TokenStream and is
   *                   used to tokenize the input message. This class should
   *                   be generated from a grammar (.g) file.
   * @param parserClass a class that extends antlr.Parser and is used to
   *                    create an AST from the input. This class must define a
   *                    method called <code>root()</code>.
   *                    This class should be generated from a grammar (.g) file.
   * @param visitorClass a class that implements
   *                     com.nightfire.adapter.converter.util.XMLVisitor and is
   *                     used to visit each node of the AST generating the
   *                     appropriate XML as it goes.
   */
   public ParsingConverter(Class lexerClass,
                           Class parserClass,
                           Class visitorClass){

      // Add check here to see if classes are instances of the right classes 

      this.lexerClass   = lexerClass;
      this.parserClass  = parserClass;
      this.visitorClass = visitorClass;

   }

   /**
   * This takes a string input and converts it to an XML Document.
   * This defines this method of the StringToXMLConverter. 
   */
   public Document convert(String input) throws ConverterException{

      XMLMessageGenerator generator;

      try{
         generator = new XMLMessageGenerator( getXMLRootName() );
      }
      catch(MessageException mex){

         throw new ConverterException("Could not create output XML generator with root ["+
                                      getXMLRootName()+"]: "+mex.getMessage());

      }

      convert(input, generator);

      return generator.getDocument();

   }

   public void convert(String input, XMLMessageGenerator output)
                       throws ConverterException{

      convert(new StringReader(input), output);

   }

   /**
   * This version of the convert method takes an input reader, and
   * generates the output XML to the given generator.
   */
   public void convert(Reader input, XMLMessageGenerator output)
                       throws ConverterException{

      try{
         Parser antlrParser = parse(input);
         AST root = getRoot(antlrParser);
         walker.walk( createVisitor(output), root );
      }
      catch(Exception ex){

         throw new ConverterException(ex);

      }

   }

   /**
   * Creates a parsed instance of the given input.
   */
   protected Parser parse(Reader input) throws FrameworkException{

      TokenStream tokens = createLexer(input);
      TokenBuffer buffer = new TokenBuffer(tokens);
      return createParser(buffer);

   }

   /**
   * Constructs a new instance of the lexer class.
   */
   protected TokenStream createLexer(Reader input) throws FrameworkException{

      // the parameters for constructing the lexer
      Object[] params = new Object[1];
      params[0] = (Reader) input;

      // the param type
      Class[] types = new Class[1];
      types[0] = Reader.class;

      return (TokenStream) ObjectFactory.create(lexerClass.getName(),
                                                types,
                                                params,
                                                TokenStream.class);

   }


   /**
   * Constructs a new instance of the parser class.
   */
   protected Parser createParser(TokenBuffer tokens) throws FrameworkException{

      Object[] params = new Object[1];
      params[0] = tokens;

      Class[] types = new Class[1];
      types[0] = TokenBuffer.class;

      return  (Parser)  ObjectFactory.create(parserClass.getName(),
                                             types,
                                             params,
                                             LLkParser.class );

   }


   /**
   * Gets an instance of the Visitor class to use while walking the tree.
   */
   protected Visitor createVisitor(XMLMessageGenerator output) throws FrameworkException{

      Object[] params = new Object[1];
      params[0] = output;

      Class[] types = new Class[1];
      types[0] = XMLMessageGenerator.class;

      return (Visitor) ObjectFactory.create(visitorClass.getName(),
                                            types,
                                            params,
                                            XMLVisitor.class);

   }

   /**
   * This assumes that the given parser is of the <code>parserClass</code>, and
   * that this class has a method called <code>root()</code>.
   * The root (start rule) method is located and invoked, and the root
   * AST is returned from the parser.
   */
   public AST getRoot(Parser parser) throws TokenStreamException,
                                            RecognitionException,
                                            ConverterException{

      try{
         Method rootMethod = parserClass.getMethod(ROOT, null);
         rootMethod.invoke(parser, null);
      }
      catch(NoSuchMethodException noRootMethod){

         throw new ConverterException("Could not find root method for parser: ["+
                                      parserClass.getName()+"]: "+
                                      noRootMethod.getMessage());

      }
      catch(IllegalAccessException illex){

         throw new ConverterException("Could access root method for parser: ["+
                                      parserClass.getName()+"]: "+
                                      illex.toString() );

      }
      catch(InvocationTargetException itex){

         Throwable target = itex.getTargetException();
         
         if(target instanceof TokenStreamException){

            throw (TokenStreamException) target;

         }

         if(target instanceof RecognitionException ){

            throw (RecognitionException) target;

         }

         throw new ConverterException("Could not invoke root method for parser: ["+
                                      parserClass.getName()+"]: "+
                                      target.toString() );
                                      
      }

      return parser.getAST();

   }

   /**
   * Sets the tree walker to use when visiting the parsed tree.
   * By default, if this is not set, a DepthFirstTreeWalker will be used.
   */
   public void setTreeWalker(TreeWalker walker){

      this.walker = walker;

   }

   /**
   * Gets a value to use as a root node name when creating
   * an XML generator. The default value for this is "root".
   */
   protected String getXMLRootName(){

      return rootName;

   }

   /**
   * Sets the value to use as a root node name when creating
   * an XML generator. The default value, if this is not set, is "root".
   */
   protected void setXMLRootName(String name){

      rootName = name;

   }

}
