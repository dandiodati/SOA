package com.nightfire.comms.ia3.asn.issue3; // machine generated code. DO NOT EDIT

import cryptix.asn1.lang.*;

public final class Module {

   // Constants and variables
   // -------------------------------------------------------------------------

   /** If module has explicit tagging (true) or not (false). */
   public static final boolean EXPLICIT_TAGGING = false;

   /** The class Singleton. */
   private static Module singleton = null;

   /**
    * The Map containing the name and string representations of OIDs defined.
    * in this ASN.1 module.
    */
   private static java.util.Map map;

   // Constructor(s)
   // -------------------------------------------------------------------------

   /** Trivial private constructor to enforce Singleton pattern. */
   private Module() {
      super();

      map = new java.util.HashMap();
      map.put("securityModule", "1.3.6.1.4.1.3576.6");
      map.put("private", "4");
      map.put("interactiveAgent", "1.3.6.1.4.1.3576.5");
      map.put("mciwcom", "3576");
      map.put("enterprises", "1");
      map.put("internet", "1");
      map.put("dod", "6");
      map.put("org", "3");
      map.put("iso", "1");
   }

   // Class methods
   // -------------------------------------------------------------------------

   /** @return the Singleton instance. */
   public static synchronized Module instance() {
      if (singleton == null)
         singleton = new Module();

      return singleton;
   }

   // Instance methods
   // -------------------------------------------------------------------------

   /**
    * @return the OID value for the designated user-defined OBJECT IDENTIFIER
    * name.
    */
   public ObjectIdentifier getOID(String name) {
      String oid = (String) map.get(name);
      return ((oid == null) ? null : ObjectIdentifier.getInstance(oid));
   }
}

// Generated by the cryptix ASN.1 kit on Tue Aug 24 11:57:40 PDT 2004
