/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.adapter.converter.edi;


/**
 * Constants for use in parsing and generating EDI.
 */
public interface EDIMessageConstants {

    /**
      *   Location of data-element delimiter in fixed-length
      *   ISA segment using zero-based indexing.
      */
    public static final int DE_DELIM_LOC  = 3;

    /**
      *   Max Size of data-element delimiter.
      */
    public static final int DE_DELIM_MAX_SIZE  = 3;

    /**
      *   Location of Component Element Separator in ISA segment using
      *   zero-based indexing. (Zero is the ISA tag itself)
      */
    public static final int SUB_DE_DELIM_POSITION = 16;

    /**
     * Size of the Data Element which holds the Component Element Separator. 
     *
     */
    public static final int SUB_DE_DELIM_LENGTH = 1;

    /**
      *   Max Size of Segment Terminator.
      */
    public static final int SEGMENT_DELIM_MAX_SIZE  = 3;

    /**
      *   Line Number of the Interchange Control Version Number in
      *   the message using zero-based indexing. (Zero is the ISA line)
      */
    public static final int EDI_VERSION_LINE_NUMBER = 1;

    /**
      *   Element Number of the Interchange Control Version Number inside
      *   the line using zero-based indexing. (Zero is the TAG itself)
      */
    public static final int EDI_VERSION_ELEMENT_NUMBER = 8;

    /**
     * By default, replace Data Element Delimiter by Control-A
     *
     */
    public static final char DEFAULT_NF_DE_DELIMITER = '\u0001';

    /**
     * By default, replace Segment Delimiter by Control-B 
     *
     */
    public static final char DEFAULT_NF_SEGMENT_DELIMITER = '\u0002';

    /**
     * By default, replace SubElement Delimiter by DEL 
     *
     * NOTE: This is not Control-C (\u0003 because this character 
     * will show up in the ISA16 data element in the XEDI (XML EDI),
     * and characters 0 - 9 are not legal characters in XML.
     */
    public static final char DEFAULT_NF_SUB_DE_DELIMITER = '\u007f';

    /**
     * The Perl escape character. When used in a replace call, this character
     * causes the Regex libraries to fail, so it must be detected and escaped. 
     */
    public static final String SLASH = "\\";

    /**
     * An escaped Perl escaped character. The Regex liraries recognize this 
     * as a single slash in th replace call.
     */
    public static final String ESCAPED_SLASH = "\\\\";

    /**
     * A Carriage Return String.
     */
    public static final String CR = "\r";

    /**
     * A Line Feed String.
     */
    public static final String LF = "\n";

    /**
     * A Carraige Return / Line Feed String.
     */
    public static final String CRLF = "\r\n";

    /**
     * The index of the Transaction Set container within 
     * the Segment. 
     */
    public static final int TRANSACTION_SET_CONTAINER_NODE_INDEX  = 1;

    /**
     * The Data Element which holds the Composite Element 
     * delimiter characer. 
     */
    public static final int COMPOSITE_ELEMENT_DELIMITER_DATA_ELEMENT_INDEX  = 15;

    /**
     * The index inside an ISE Segment where the count of 
     * Functional Groups in the Envelope belongs.  
     */
    public static final int FUNC_GROUP_COUNT_DATA_ELEMENT_INDEX  = 0;

    /**
     * The index inside an GSE Segment where the count of 
     * Transaction Sets in the Fucntional Group belongs.  
     * 
     */
    public static final int TRANS_SET_COUNT_DATA_ELEMENT_INDEX  = 0;

    /**
     * The index inside an SE Segment where the count of 
     * Segments in the Transaction Set belongs.  
     * 
     */
    public static final int SEGMENT_COUNT_DATA_ELEMENT_INDEX  = 0;

    /**
     * The XML Node for an Element.
     */
    public static final String ENVELOPE_NODE = "envelope";

    /**
     * The XML Node for a Functional Group Container.
     * 
     */
    public static final String FUNC_GROUP_CONTAINER_NODE = "funcgroupContainer";

    /**
     * The prefix to add to the name of a Node which is 
     * a XEDI Loop node. 
     */
    public static final String LOOP_NODE_PREFIX  = "loop";

    /**
     * The suffix to add to the Name of an XML Node which is a 
     * Data Element. 
     */
    public static final String FIRST_ELEMENT_NODE_SUFFIX = "01";

    /**
     * The XML Node for an Element Pair Container.
     * 
     */
    public static final String ELEMENT_PAIR_CONTAINER_NODE = "elemPairContainer";

    /**
     * The XML Node for an Element Triple Container.
     */
    public static final String ELEMENT_TRIPLE_CONTAINER_NODE = "elemTripleContainer";

    /**
     * The XML Node for an Element Group.
     */
    public static final String ELEMENT_PAIR_NODE = "elemPair";

    /**
     * The XML Node for a Bad Transaction.
     */
    public static final String BAD_TRANSACTION_NODE = "badTransaction";

    /**
     * The name of the ISA Segment.
     * 
     */
    public static final String ISA = "ISA";

    /**
     * The name of the IEA Segment.
     */
    public static final String IEA = "IEA";

    /**
     * The name of the GS Segment.
     */
    public static final String GS = "GS";

    /**
     * The name of the GE Segment.
     * 
     */
    public static final String GE = "GE";

    /**
     * The name of the ST Segment.
     * 
     */
    public static final String ST = "ST";

    /**
     * The name of the SE Segment.
     * 
     */
    public static final String SE = "SE";

}
