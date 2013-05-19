package com.nightfire.framework.xrq;


/**
 * Defines constants used by the XRQ infrastructure.
 */
public abstract class XrqConstants
{



  // Name of locale resource catalog used by xrq engine.
	public static final String XRQ_RESOURCE_CATALOG = "XRQ_RESOURCE_CATALOG";

  /**
   * xml page node constants
   */
  public static final String PAGE_KEY_NODE = "pageKey";
  public static final String CURRENT_PAGE_NODE = "currentPage";
  public static final String IN_MEM_PAGES_NODE = "inMemoryPages";
  public static final String TOTAL_PAGES_NODE = "totalPages";

  public static final String NEXTPAGE_NODE = "nextPage";

  public static final String HEADER_NODE = "Header";

  public static final String RECORD_CONTAINER_NODE = "RecordContainer";
  public static final String RECORD_NODE = "Record";




  /**
   * Date related constants
   */
  public static final String DATE_FORMAT_NODE = "dateFormat";

  public static final String DATE_FLAG = "DATE";
  public static final String TIME_FLAG = "TIME";
  public static final String DATE_TIME_FLAG ="DATE_TIME";

  public static final String DB_CONVERT_DATE = "MM-dd-yyyy";
  public static final String DB_CONVERT_TIME = "hh:mm:ssa";
  public static final String DB_CONVERT_DATETIME = DB_CONVERT_DATE + "-" + DB_CONVERT_TIME;

  public static final String SQL_DATE_FORMAT = "MM-DD-YYYY";
  public static final String SQL_TIME_FORMAT = "HH:MI:SSAM";
  public static final String SQL_DATETIME_FORMAT = SQL_DATE_FORMAT + "-" + SQL_TIME_FORMAT;


  /*
   * data separators
   */
  public static final String FIELD_VAL_SEP = ",";

  /**
   * conversion constants
   */
  public static final int MSEC_PER_SECOND = 1000;
}