/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;

import com.nightfire.framework.util.Debug;

/**
 * This class holds critical information for parsing EDI structures.
 * The information it holds consists of the EDI Version of the 
 * transaction sets and the values of the three types of delimiters.
 */
public class EDIDelimiters {

    private final String elementDelimiter;
    private final String subElementDelimiter;
    private final String segmentDelimiter;
    private final String ediVersion;

    /**
     * Creates a new <code>EDIDelimiters</code> instance.
     *
     * @param elementDelimiter The value of the Element Delimiter.
     * @param subElementDelimiter The value of the SubElement Delimiter.
     * @param segmentDelimiter The value of the Segment Delimiter.
     * @param ediVersion The Version of the EDI transaction sets.
     */
    public EDIDelimiters(String elementDelimiter, String subElementDelimiter,
            String segmentDelimiter, String ediVersion) {

        this.elementDelimiter = elementDelimiter;
        this.subElementDelimiter = subElementDelimiter;
        this.segmentDelimiter = segmentDelimiter;
        this.ediVersion = ediVersion;
    }

    /**
     * Creates a new <code>EDIDelimiters</code> instance.
     *
     * @param elementDelimiterChar The value of the Element Delimiter.
     * @param subElementDelimiterChar The value of the SubElement Delimiter.
     * @param segmentDelimiterChar The value of the Segment Delimiter.
     * @param ediVersion The Version of the EDI transaction sets.
     */
    public EDIDelimiters(char elementDelimiterChar, char subElementDelimiterChar, 
            char segmentDelimiterChar, String ediVersion) {

        this( String.valueOf(elementDelimiterChar),
              String.valueOf(subElementDelimiterChar),
              String.valueOf(segmentDelimiterChar),
              ediVersion);
    }

    /**
     * Accessor for the Element Delimiter
     *
     * @return The Value of the Element Delimiter.
     */
    public String getElementDelimiter(){

        return elementDelimiter;
    }

    /**
     * Accessor for the SubElement Delimiter
     *
     * @return The Value of the SubElement Delimiter.
     */
    public String getSubElementDelimiter(){

        return subElementDelimiter;
    }

    /**
     * Accessor for the Segment Delimiter
     *
     * @return The Value of the Segment Delimiter.
     */
    public String getSegmentDelimiter(){

        return segmentDelimiter;
    }

    /**
     * Accessor for the EDI Version. 
     *
     * @return  The Version of the EDI transaction sets.
     */
    public String getEdiVersion(){

        return ediVersion;
    }

    /**
     * Stringify the EDI parsing information encapsulated in this instance. 
     *
     */
    public void describe(){

        if(Debug.isLevelEnabled(Debug.EDI_PARSE)){

            StringBuffer description = new StringBuffer();
            description.append("Describing Delimiters:\n");
            description.append("ElementDelimiter = [");
            description.append(elementDelimiter);
            description.append("]\nSubElementDelimiter = [");
            description.append(subElementDelimiter);
            description.append("]\nSegmentDelimiter = [");
            description.append(segmentDelimiter);
            description.append("]\nEDI Version = [");
            description.append(ediVersion);
            description.append("]\n");

            Debug.log(Debug.EDI_PARSE, description.toString() );
        }
    }
}
