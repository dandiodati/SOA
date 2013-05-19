package com.nightfire.comms.ia3.asn.issue2; // machine generated code. DO NOT EDIT

import cryptix.asn1.lang.*;

public class IABasicEDI extends Sequence {

   // Constructor(s)
   // -------------------------------------------------------------------------

   /**
    * Constructs a new instance of this type with a blank Name.
    */
   public IABasicEDI() {
      super("", new Tag(Tag.SEQUENCE));
   }

   /**
    * Constructs a new instance of this type with a designated Name.
    *
    * @param name the designated Name for this new instance.
    */
   public IABasicEDI(String name) {
      super(name, new Tag(Tag.SEQUENCE));
   }

   /**
    * Constructs a new instance of this type with a designated Name and Tag.
    *
    * @param name the designated Name for this new instance.
    * @param tag the designated tag for this new instance.
    */
   public IABasicEDI(String name, Tag tag) {
      super(name, tag);
   }

   /**
    * Constructs a new instance of this type with a trivial Name and an
    * initial value.
    *
    * @param value the initial value of this instance.
    */
   public IABasicEDI(Sequence value) {
      this("", value);
   }

   /**
    * Constructs a new instance of this type with a designated Name and an
    * initial value.
    *
    * @param name the designated Name for this new instance.
    * @param value the initial value of this instance.
    */
   public IABasicEDI(String name, Sequence value) {
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
   public IABasicEDI(String name, Tag tag, Sequence value) {
      super(name, tag, value == null ? null : value.value());
   }

   // Constants and variables
   // -------------------------------------------------------------------------


   // Over-loaded implementation of methods defined in superclass
   // -------------------------------------------------------------------------

   protected void initInternal() {
      super.initInternal();

      IType plainEDImessage = new ObjectIdentifier("plainEDImessage");
      components.add(plainEDImessage);
      IType ediContent = new OctetString("ediContent", new Tag(Tag.CONTEXT, 0, true, true));
      components.add(ediContent);
   }

   // Accessor methods
   // -------------------------------------------------------------------------

   public ObjectIdentifier getPlainEDImessage() {
      return (ObjectIdentifier) components.get(0);
   }

   public void setPlainEDImessage(ObjectIdentifier obj) {
      ObjectIdentifier it = getPlainEDImessage();
      it.value(obj.value());
      components.set(0, it);
   }

   public OctetString getEdiContent() {
      return (OctetString) components.get(1);
   }

   public void setEdiContent(OctetString obj) {
      OctetString it = getEdiContent();
      it.value(obj.value());
      components.set(1, it);
   }

}

// Generated by the cryptix ASN.1 kit on Sun Mar 09 12:59:12 PST 2003