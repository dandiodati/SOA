package com.nightfire.comms.ia3.asn.issue3; // machine generated code. DO NOT EDIT

import cryptix.asn1.lang.*;

public class AlgorithmIdentifier extends Sequence {

   // Constructor(s)
   // -------------------------------------------------------------------------

   /**
    * Constructs a new instance of this type with a blank Name.
    */
   public AlgorithmIdentifier() {
      super("", new Tag(Tag.SEQUENCE));
   }

   /**
    * Constructs a new instance of this type with a designated Name.
    *
    * @param name the designated Name for this new instance.
    */
   public AlgorithmIdentifier(String name) {
      super(name, new Tag(Tag.SEQUENCE));
   }

   /**
    * Constructs a new instance of this type with a designated Name and Tag.
    *
    * @param name the designated Name for this new instance.
    * @param tag the designated tag for this new instance.
    */
   public AlgorithmIdentifier(String name, Tag tag) {
      super(name, tag);
   }

   /**
    * Constructs a new instance of this type with a trivial Name and an
    * initial value.
    *
    * @param value the initial value of this instance.
    */
   public AlgorithmIdentifier(Sequence value) {
      this("", value);
   }

   /**
    * Constructs a new instance of this type with a designated Name and an
    * initial value.
    *
    * @param name the designated Name for this new instance.
    * @param value the initial value of this instance.
    */
   public AlgorithmIdentifier(String name, Sequence value) {
      this(name, new Tag(Tag.SEQUENCE), value);
   }

   /**
    * Constructs a new instance of this type given its Name, Tag and initial
    * value.
    *
    * @param name the designated Name for this new instance.
    * @param tag the specific tag for this instance.
    * @param value the initial value for this instance.
    */
   public AlgorithmIdentifier(String name, Tag tag, Sequence value) {
      super(name, tag, value == null ? null : value.value());
   }

   // Constants and variables
   // -------------------------------------------------------------------------


   // Over-loaded implementation of methods defined in superclass
   // -------------------------------------------------------------------------

   protected void initInternal() {
      super.initInternal();

      IType algorithm = new ObjectIdentifier("algorithm");
      components.add(algorithm);
      IType parameters = new Null("parameters");
      components.add(parameters);
   }

   // Accessor methods
   // -------------------------------------------------------------------------

   public ObjectIdentifier getAlgorithm() {
      return (ObjectIdentifier) components.get(0);
   }

   public void setAlgorithm(ObjectIdentifier obj) {
      ObjectIdentifier it = getAlgorithm();
      it.value(obj.value());
      components.set(0, it);
   }

   public Null getParameters() {
      return (Null) components.get(1);
   }

   public void setParameters(Null obj) {
      Null it = getParameters();
      it.value(obj.value());
      components.set(1, it);
   }

}

// Generated by the cryptix ASN.1 kit on Tue Aug 24 11:57:40 PDT 2004