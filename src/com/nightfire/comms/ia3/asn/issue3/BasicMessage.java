package com.nightfire.comms.ia3.asn.issue3; // machine generated code. DO NOT EDIT

import cryptix.asn1.lang.*;

public class BasicMessage extends Choice {

   // Constructor(s)
   // -------------------------------------------------------------------------

   /**
    * Constructs a new instance of this type with a blank Name.
    */
   public BasicMessage() {
      super("", null);
   }

   /**
    * Constructs a new instance of this type with a designated Name.
    *
    * @param name the designated Name for this new instance.
    */
   public BasicMessage(String name) {
      super(name, null);
   }

   /**
    * Constructs a new instance of this type with a designated Name and Tag.
    *
    * @param name the designated Name for this new instance.
    * @param tag the designated tag for this new instance.
    */
   public BasicMessage(String name, Tag tag) {
      super(name, tag);
   }

   /**
    * Constructs a new instance of this type with a trivial Name and an
    * initial value.
    *
    * @param value the initial value of this instance.
    */
   public BasicMessage(Choice value) {
      this("", value);
   }

   /**
    * Constructs a new instance of this type with a designated Name and an
    * initial value.
    *
    * @param name the designated Name for this new instance.
    * @param value the initial value of this instance.
    */
   public BasicMessage(String name, Choice value) {
      this(name, null, value);
   }

   /**
    * Constructs a new instance of this type given its Name, Tag and initial
    * value.
    *
    * @param name the designated Name for this new instance.
    * @param tag the specific tag for this instance.
    * @param value the initial value for this instance.
    */
   public BasicMessage(String name, Tag tag, Choice value) {
      super(name, tag, value == null ? null : value.value());
   }

   // Constants and variables
   // -------------------------------------------------------------------------


   // Over-loaded implementation of methods defined in superclass
   // -------------------------------------------------------------------------

   protected void initInternal() {
      super.initInternal();

      IType basicMessage2 = new IA5String("basicMessage2" );
      components.add(basicMessage2);
      IType basicMessage3 = new IA5String("basicMessage3", new Tag(Tag.CONTEXT, 0, true));
      components.add(basicMessage3);
      IType basicMessage4 = new IA5String("basicMessage4", new Tag(Tag.CONTEXT, 1, true));
      components.add(basicMessage4);
   }

   // Accessor methods
   // -------------------------------------------------------------------------

   public IA5String getBasicMessage2() {
      return (IA5String) components.get(0);
   }

   public void setBasicMessage2(IA5String obj) {
      IA5String it = getBasicMessage2();
      it.value(obj.value());
      components.set(0, it);
   }
   
    public void setBasicMessage2(String obj) {
      IA5String it = getBasicMessage2();
      it.value(obj);
      components.set(0, it);
   }

   public IA5String getBasicMessage3() {
      return (IA5String) components.get(1);
   }

   public void setBasicMessage3(IA5String obj) {
      IA5String it = getBasicMessage3();
      it.value(obj.value());
      components.set(1, it);
   }

   public IA5String getBasicMessage4() {
      return (IA5String) components.get(2);
   }

   public void setBasicMessage4(IA5String obj) {
      IA5String it = getBasicMessage4();
      it.value(obj.value());
      components.set(2, it);
   }

   // CHOICE-specific convenience methods
   // -------------------------------------------------------------------------

   /**
    * Returns true iff this CHOICE instance has been decoded, and its (only)
    * concrete alternative is the designated one. False otherwise.
    *
    * @return true iff this CHOICE instance has been decoded, and its (only)
    * concrete alternative is the designated one. False otherwise.
    */
   public boolean isBasicMessage2() {
      return !getBasicMessage2().isBlank();
   }
   /**
    * Returns true iff this CHOICE instance has been decoded, and its (only)
    * concrete alternative is the designated one. False otherwise.
    *
    * @return true iff this CHOICE instance has been decoded, and its (only)
    * concrete alternative is the designated one. False otherwise.
    */
   public boolean isBasicMessage3() {
      return !getBasicMessage3().isBlank();
   }
   /**
    * Returns true iff this CHOICE instance has been decoded, and its (only)
    * concrete alternative is the designated one. False otherwise.
    *
    * @return true iff this CHOICE instance has been decoded, and its (only)
    * concrete alternative is the designated one. False otherwise.
    */
   public boolean isBasicMessage4() {
      return !getBasicMessage4().isBlank();
   }
}

// Generated by the cryptix ASN.1 kit on Tue Aug 24 11:57:40 PDT 2004
