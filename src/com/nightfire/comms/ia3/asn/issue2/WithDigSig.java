package com.nightfire.comms.ia3.asn.issue2; // machine generated code. DO NOT EDIT

import cryptix.asn1.lang.*;

public class WithDigSig extends Sequence {

   // Constructor(s)
   // -------------------------------------------------------------------------

   /**
    * Constructs a new instance of this type with a blank Name.
    */
   public WithDigSig() {
      super("", new Tag(Tag.CONTEXT, 2, true));
   }

   /**
    * Constructs a new instance of this type with a designated Name.
    *
    * @param name the designated Name for this new instance.
    */
   public WithDigSig(String name) {
      super(name, new Tag(Tag.CONTEXT, 2, true));
   }

   /**
    * Constructs a new instance of this type with a designated Name and Tag.
    *
    * @param name the designated Name for this new instance.
    * @param tag the designated tag for this new instance.
    */
   public WithDigSig(String name, Tag tag) {
      super(name, tag);
   }

   /**
    * Constructs a new instance of this type with a trivial Name and an
    * initial value.
    *
    * @param value the initial value of this instance.
    */
   public WithDigSig(Sequence value) {
      this("", value);
   }

   /**
    * Constructs a new instance of this type with a designated Name and an
    * initial value.
    *
    * @param name the designated Name for this new instance.
    * @param value the initial value of this instance.
    */
   public WithDigSig(String name, Sequence value) {
      this(name, new Tag(Tag.CONTEXT, 2, true), value);
   }

   /**
    * Constructs a new instance of this type given its Name, Tag and initial
    * value.
    *
    * @param name the designated Name for this new instance.
    * @param tag the specific tag for this instance.
    * @param value the initial value for this instance.
    */
   public WithDigSig(String name, Tag tag, Sequence value) {
      super(name, tag, value == null ? null : value.value());
   }

   // Constants and variables
   // -------------------------------------------------------------------------


   // Over-loaded implementation of methods defined in superclass
   // -------------------------------------------------------------------------

   protected void initInternal() {
      super.initInternal();

      IType receiptSignatureAlgorithm = new ObjectIdentifier("receiptSignatureAlgorithm");
      components.add(receiptSignatureAlgorithm);
      IType receiptDigitalSignature = new OctetString("receiptDigitalSignature");
      components.add(receiptDigitalSignature);
   }

   // Accessor methods
   // -------------------------------------------------------------------------

   public ObjectIdentifier getReceiptSignatureAlgorithm() {
      return (ObjectIdentifier) components.get(0);
   }

   public void setReceiptSignatureAlgorithm(ObjectIdentifier obj) {
      ObjectIdentifier it = getReceiptSignatureAlgorithm();
      it.value(obj.value());
      components.set(0, it);
   }

   public OctetString getReceiptDigitalSignature() {
      return (OctetString) components.get(1);
   }

   public void setReceiptDigitalSignature(OctetString obj) {
      OctetString it = getReceiptDigitalSignature();
      it.value(obj.value());
      components.set(1, it);
   }

}

// Generated by the cryptix ASN.1 kit on Sun Mar 09 12:59:13 PST 2003