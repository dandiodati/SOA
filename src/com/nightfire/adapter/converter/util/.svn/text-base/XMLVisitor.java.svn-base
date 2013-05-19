/**
* Copyright (c) 2001-2002 NightFire Software, Inc. All rights reserved.
*/
package com.nightfire.adapter.converter.util;

import antlr.collections.AST;

import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

/**
* This is the base class for Visitor implemenations that visit
* AST nodes and generate a corresponding XML structure. 
*/
public abstract class XMLVisitor implements Visitor{

   /**
   * The generator used to create the output XML.
   */
   protected XMLMessageGenerator generator;

   /**
   * Creates an XMLVisitor whose output will be written to the given
   * XML generator.
   *
   * @param generator the XML generator where the XML output will be stored.
   */
   public XMLVisitor(XMLMessageGenerator generator){

      this.generator = generator;

   }

   /**
   * Gets the XML generator being used.  
   */
   public XMLMessageGenerator getGenerator(){

      return generator;

   }

} 