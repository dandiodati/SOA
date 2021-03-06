package com.nightfire.comms.ia3.asn.issue2; // machine generated code. DO NOT EDIT

import cryptix.asn1.lang.*;

public class IAGeneric extends Sequence {

   // Constructor(s)
   // -------------------------------------------------------------------------

   /**
    * Constructs a new instance of this type with a blank Name.
    */
   public IAGeneric() {
      super("", new Tag(Tag.SEQUENCE));
   }

   /**
    * Constructs a new instance of this type with a designated Name.
    *
    * @param name the designated Name for this new instance.
    */
   public IAGeneric(String name) {
      super(name, new Tag(Tag.SEQUENCE));
   }

   /**
    * Constructs a new instance of this type with a designated Name and Tag.
    *
    * @param name the designated Name for this new instance.
    * @param tag the designated tag for this new instance.
    */
   public IAGeneric(String name, Tag tag) {
      super(name, tag);
   }

   /**
    * Constructs a new instance of this type with a trivial Name and an
    * initial value.
    *
    * @param value the initial value of this instance.
    */
   public IAGeneric(Sequence value) {
      this("", value);
   }

   /**
    * Constructs a new instance of this type with a designated Name and an
    * initial value.
    *
    * @param name the designated Name for this new instance.
    * @param value the initial value of this instance.
    */
   public IAGeneric(String name, Sequence value) {
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
   public IAGeneric(String name, Tag tag, Sequence value) {
      super(name, tag, value == null ? null : value.value());
   }

   // Constants and variables
   // -------------------------------------------------------------------------


   // Over-loaded implementation of methods defined in superclass
   // -------------------------------------------------------------------------

   protected void initInternal() {
      super.initInternal();

      IType contentType = new ContentType("contentType");
      components.add(contentType);
      IType content = new Any("content", new Tag(Tag.CONTEXT, 0, true));
      content.optional(true);
      components.add(content);
   }

   // Accessor methods
   // -------------------------------------------------------------------------

   public ContentType getContentType() {
      return (ContentType) components.get(0);
   }

   public void setContentType(ContentType obj) {
      ContentType it = getContentType();
      it.value(obj.value());
      components.set(0, it);
   }

   public Any getContent() {
      return (Any) components.get(1);
   }

   public void setContent(Any obj) {
      Any it = getContent();
      it.value(obj.value());
      components.set(1, it);
   }

}

// Generated by the cryptix ASN.1 kit on Sun Mar 09 12:59:14 PST 2003
