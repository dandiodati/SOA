/*
 * Copyright(c) 2002 NightFire Software, Inc.
 * All rights reserved.
 */

package com.nightfire.webgui.core.meta;

// jdk imports
import java.util.HashMap;

// third party imports

// nightfire imports
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.message.util.xml.ParsedXPath;
import com.nightfire.framework.message.util.xml.NamespaceMap;

/**
 * Contains XPaths for use in loading persisted message definitions
 */
public class MessagePaths
{
    private NamespaceMap resolver = buildPrefixResolver();

    // basic paths
    public final ParsedXPath idPath;
    public final ParsedXPath altIdPath;
    public final ParsedXPath displayNamePath;
    public final ParsedXPath fullNamePath;
    public final ParsedXPath abbreviationPath;
    public final ParsedXPath helpPath;

    // message / build paths
    public final ParsedXPath NFTypePath;
    public final ParsedXPath elemChildPath;
    public final ParsedXPath seqChildPath;
    public final ParsedXPath complexTypeChildPath;
    public final ParsedXPath includesPath;
    public final ParsedXPath globalDataTypesPath;
    public final ParsedXPath globalComplexTypesPath;
    public final ParsedXPath anonElemsPath;
    public final ParsedXPath messageRootPath;

    // section paths
    public final ParsedXPath idFieldsPath;
    public final ParsedXPath repeatablePath;
    public final ParsedXPath optionalPath;

    // field paths
    public final ParsedXPath dataTypePath;
    public final ParsedXPath customPath;
    public final ParsedXPath customNamePath;
    public final ParsedXPath customValuePath;

    // data type paths
    public final ParsedXPath minLengthPath;
    public final ParsedXPath maxLengthPath;
    public final ParsedXPath usagePath;
    public final ParsedXPath formatPath;
    public final ParsedXPath examplesPath;
    public final ParsedXPath enumPath;
    public final ParsedXPath enumClassPath;
    public final ParsedXPath baseTypePath;
    public final ParsedXPath textAreaPath;

    // static option source paths
    public final ParsedXPath optionValuesPath;
    public final ParsedXPath displayValuesPath;
    public final ParsedXPath descriptionsPath;

    // extension paths
    public final ParsedXPath extensionPath;
    public final ParsedXPath extensionChildPath;
    public final ParsedXPath maxOccursPath;

    /**
     * Constructor
     */
    public MessagePaths() throws FrameworkException
    {
        // basic paths
        idPath = new ParsedXPath("@name", resolver);
        altIdPath = new ParsedXPath("@id", resolver);
        displayNamePath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:displayName/@value", resolver);
        fullNamePath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:fullName/@value", resolver);
        abbreviationPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:abbreviation/@value", resolver);
        optionalPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:optional/@value", resolver);
        helpPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:helpText", resolver);

        // message / build paths
        NFTypePath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:type/@value", resolver);
        elemChildPath = new ParsedXPath(
            "xsd:complexType/xsd:sequence/*", resolver);
        seqChildPath = new ParsedXPath(
            "*[local-name() != \"annotation\"]", resolver);
        complexTypeChildPath = new ParsedXPath(
            "xsd:sequence/*", resolver);
        includesPath = new ParsedXPath( // nfbase.xsd is for W3C validity,
                                        // it defines the builtin types
            "/xsd:schema/xsd:include[@schemaLocation != \"nfbase.xsd\"]/" +
            "@schemaLocation", resolver);
        globalDataTypesPath = new ParsedXPath(
            "/xsd:schema/xsd:simpleType[xsd:annotation/xsd:appinfo/nf:type/" +
            "@value = \"nf:DataType\"]", resolver);
        globalComplexTypesPath = new ParsedXPath(
            "/xsd:schema/xsd:complexType[xsd:annotation/xsd:appinfo/nf:type/"
            + "@value = \"nf:ComplexType\"]", resolver);
        anonElemsPath = new ParsedXPath(
            "/xsd:schema/xsd:element[xsd:annotation/xsd:appinfo/nf:type/" +
            "@value != \"nf:Message\"]", resolver);
        messageRootPath = new ParsedXPath(
            "/xsd:schema/xsd:element[xsd:annotation/xsd:appinfo/nf:type/" +
            "@value = \"nf:Message\"]", resolver);

        // section paths
        idFieldsPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:idFields/nf:idField/@value",
            resolver);
        repeatablePath = new ParsedXPath("@maxOccurs", resolver);

        // field paths
        dataTypePath = new ParsedXPath(
            "xsd:complexType/xsd:attribute/xsd:simpleType[xsd:annotation/" +
            "xsd:appinfo/nf:type/@value=\"nf:DataType\"]", resolver);
        customPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:customValues/nf:customValue",
            resolver);
        customNamePath = idPath;
        customValuePath = new ParsedXPath("@value", resolver);

        // data type paths
        minLengthPath =
            new ParsedXPath("xsd:restriction/xsd:minLength/@value", resolver);
        maxLengthPath =
            new ParsedXPath("xsd:restriction/xsd:maxLength/@value", resolver);
        usagePath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:usage/@value", resolver);
        formatPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:pattern/@value", resolver);
        examplesPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:examples/nf:example/@value",
            resolver);
        enumPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:enumClass", resolver);
        enumClassPath = customValuePath;
        baseTypePath = new ParsedXPath("xsd:restriction/@base", resolver);
        textAreaPath = new ParsedXPath(
            "xsd:annotation/xsd:appinfo/nf:textArea/@value", resolver);

        // static option source paths
        optionValuesPath = new ParsedXPath("nf:enumeration/@value", resolver);
        displayValuesPath = new ParsedXPath(
            "nf:enumeration/@display", resolver);
        descriptionsPath = new ParsedXPath("nf:enumeration/@help", resolver);

        // extension paths
        extensionPath = new ParsedXPath(
            "xsd:complexType/xsd:extension/@base", resolver);
        extensionChildPath = new ParsedXPath(
            "xsd:complexType/xsd:extension/xsd:sequence/*", resolver);
        maxOccursPath = repeatablePath;
    }

    /**
     * Creates a NamespaceMap to use for namespaces
     */
    private static NamespaceMap buildPrefixResolver()
    {
        HashMap map = new HashMap();
        // This assumes no namespace support in the XML parser.  If namespace
        // support is added, these must be modified to contain the correct
        // URI
        map.put("xsd", "");
        map.put("nf", "");

        return new NamespaceMap(map);
    }
}
