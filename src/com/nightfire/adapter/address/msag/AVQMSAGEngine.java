package com.nightfire.adapter.address.msag ;

import org.w3c.dom.*;
import java.util.*;
import java.sql.*;
import com.nightfire.common.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;
import com.nightfire.framework.message.*;
import com.nightfire.framework.message.parser.xml.*;
import com.nightfire.framework.message.parser.*;
import com.nightfire.framework.message.generator.xml.*;
import com.nightfire.spi.common.driver.MessageProcessorContext;

import oracle.jdbc.driver.*;

/**
 * MSAG message protocol adapter for Address Validation Queries
 */
public class AVQMSAGEngine extends AVQEngine
{
	//------------------------------------------------------------------------------
	// Constants

	private static final int          SP_NO_RESULT = 0 ;
	private static final int SP_EXACT_MATCH_RESULT = 1 ;
	private static final int         SP_ALT_RESULT = 2 ;

	//------------------------------------------------------------------------------
	// Configurable properties

	private static int maxOutputAlternatives = 50 ;
	private static int indexScanInterval     = 50 ;

	//------------------------------------------------------------------------------
	// Static properties to read (name / req field)

   private static final String [][] propStaticNames =
    {
    	{"ILEC_HAS_NPA_NXX",            "true"},  // 0
    	{"ILEC_HAS_WIRE_CENTER_CODE",   "true"},  // 1
    	{"ILEC_VALIDATES_BY_WTN",       "true"},  // 2
    	{"INDEX_SCAN_INTERVAL",         "true"},  // 3
    	{"MAX_OUTPUT_ALTERNATIVES",     "true"},  // 4
    	{"PATH_SEPARATOR",              "true"},  // 5
      {"RESP_ALT_COPY_NODE",          "true"},  // 6
      {"RESP_DOC_NAME",               "true"},  // 7
      {"RESP_DTD_NAME",               "true"},  // 8
      {"SP_BY_ADDR_EXACT_MATCH",      "true"},  // 9
      {"SP_LERG6_LOOKUP",             "true"},  // 10
      {"SP_OUT_ALT_WIRECENTER",       "false"}, // 11
      {"SP_OUT_MATCH_WIRECENTER",     "false"}, // 12
      {"SP_OUT_PARAMS_NUMBER",        "true"}   // 13
    } ;

	//------------------------------------------------------------------------------
	// Dymanic properties (name_ + '#' to read (name / req field)
  // example: SP_BY_ADDR_PASS_0

  private static final String [][] propDynamicNames =
    {
    	{"SP_BY_ADDR_ALT_",            "false"}, // 0
    	{"SP_IN_",                     "true"},  // 1
    	{"SP_OUT_MATCH_",              "true"},  // 2
    	{"SP_OUT_ALT_",                "true"},  // 3
      {"COPY_IN_",                   "false"}, // 4
      {"COPY_OUT_ALT_",              "false"}, // 5
      {"COPY_OUT_MATCH_",            "false"}, // 6
      {"COPY_OUT_ERROR_",            "false"}, // 7
      {"SP_LERG6_LOOKUP_IN_",        "false"}, // 8
      {"SP_LERG6_LOOKUP_OUT_ALT_",   "false"}, // 9
      {"SP_LERG6_LOOKUP_OUT_MATCH_", "false"}  // 10
    } ;

/*
  private static final String
    SP_LERG6_LOOKUP_OUT_MATCH_PATTERN =
      propDynamicNames [10][0] ;
  private static final String
    SP_LERG6_LOOKUP_OUT_ALT_PATTERN =
      propDynamicNames [9][0] ;
  private static final String
    SP_LERG6_LOOKUP_IN_PATTERN =
      propDynamicNames [8][0] ;
*/
  private static final String
    SP_BY_ADDR_ALT_PATTERN =
      propDynamicNames [0][0] ;
  private static final String
    SP_IN_PATTERN =
      propDynamicNames [1][0] ;
/*
  private static final String
    SP_OUT_MATCH_PATTERN =
      propDynamicNames [2][0] ;
  private static final String
    SP_OUT_ALT_PATTERN =
      propDynamicNames [3][0] ;
  private static final String
    COPY_IN_PATTERN =
      propDynamicNames [4][0] ;
  private static final String
    COPY_OUT_ALT_PATTERN =
      propDynamicNames [5][0] ;
  private static final String
    COPY_OUT_MATCH_PATTERN =
      propDynamicNames [6][0] ;
  private static final String
    COPY_OUT_ERROR_PATTERN =
      propDynamicNames [7][0] ;
*/

	//---------------------------------------------------------------------
  // Get the NAME of stored proc for EXACT MATCH from properties

  private String spName             = null ;
  private String spLerg6Name        = null ;
  private Vector spInNames          = null ;
//  private Vector spOutNames         = null ;
  //private Vector COPY_IN_PARAM_NAME        = null ;
  //private Vector COPY_OUT_MATCH_PARAM_NAME = null ;
  //private Vector COPY_OUT_ALT_PARAM_NAME   = null ;
  //private Vector COPY_OUT_ERROR_PARAM_NAME = null ;
  private Vector spLerg6LookupInNames  = null ;
  private Vector spLerg6LookupOutNames = null ;


  /**
  * This method passes street validation input in XML format to appropriate
  * validation method and returns the result to calling processor.
  *
  * @param  input  Input message to process.
  *
  * @param  mpcontext The context
  *
  * @return  Optional NVPair containing a Destination name and a Document,
  *          or null if none.
  *
  * @exception  ProcessingException  Thrown if processing fails.
  *
  * @exception  MessageException  Thrown if message error occurs.
  */
  public NVPair[] execute ( MessageProcessorContext mpcontext, java.lang.Object input )
      throws MessageException, ProcessingException
  {

      if (Debug.isLevelEnabled(Debug.MSG_LIFECYCLE))
        Debug.log(Debug.MSG_LIFECYCLE, "AVQMSAGEngine.process: Starting processing ... " );

    if (input == null)
    {
      return null ;
    }

    //------------------------------------------------------------------------------
    // First, we need to fix input
    // If it's a String, assume it's an XML stream and attempt to parse
    // it into DOM.

    String inputXML  = null ;

    if (input instanceof String)
    {
      inputXML = (String) input ;
    }
    if (input instanceof Document)
    {
      inputXML = DocumentToString ((Document) input) ;
    }
    //else
    //{
    //  Debug.log(this, Debug.ALL_ERRORS, "ERROR: AVQMSAGEngine.process: input to process is neither a string nor a document");
    // throw new ProcessingException( "ERROR: AVQMSAGEngine.process: input to process is neither a string nor a document");
    //}

		//------------------------------------------------------------------------------
    // Second, we want to parse it and get DOM
    // At this point we have parsed XML in parser

    XMLMessageParser inputParser = getParser (inputXML) ;

		//------------------------------------------------------------------------------
    // Next, we want to know if input contains WTN
    // according to email (10/4/99) from Delane
    //
    //xmk: What is this comment about? WTN will be used but not be required.

		//------------------------------------------------------------------------------
    // Next, we want to know if ILEC can validate by WTN
    // read the properties to discover that fact
    //
    //xmk: background?

    //xmk: move to init?
    String vlidatesByWTN = (String) adapterProperties.get ("ILEC_VALIDATES_BY_WTN") ;
    maxOutputAlternatives = String2int ((String) adapterProperties.get ("MAX_OUTPUT_ALTERNATIVES"), 50) ;

    if (vlidatesByWTN.equalsIgnoreCase ("false"))
    {
      // ILEC cannot validate by WTN or input doesn't contain WTN
      // so, validate by Address information

      //xmk: this code does not support validation by WTN, but WTN will be used
      // to get wire center either from SAG or from LERG6.

      return (formatNVPair (validateByAddress (inputParser))) ;
    }
    else
    {
      // ILEC can validate by WTN and input contain WTN
      // so, validate by WTN
      // Not implemented.
    }
    throw new MessageException( "ERROR: AVQMSAGEngine: Operation not supported, input [" +
                                    input.getClass().getName() + "]" );
  } // process


  /**
  * This method passes street validation input in XML format to stored
  * procedures, then, if gets ruturns, decides how to report and passes
  * results back to process method.
  *
  * @param  inputParser  XMLMessageParser object containing address validation query.
  *
  * @return  String containing query result.
  *
  * @exception  ProcessingException  Thrown if processing fails.
  *
  * @exception  MessageException  Thrown if message error occurs.
  */
  private String validateByAddress (XMLMessageParser inputParser)
    throws MessageException, ProcessingException
  {

    int spResultType = SP_NO_RESULT;  // default
    String outputXML = "";

    //------------------------------------------------------------------------------
    // First, try to get exact match
    try {

      boolean considerSingleAltAsExactMatch = true ; // default

      //xmk: move to init?
      String strTemp = getPropertyByName("CONSIDER_SINGLE_ALT_AS_EXACT_MATCH") ;

      if (strTemp != null && strTemp.length () > 0 && strTemp.equalsIgnoreCase ("false"))
      {
        considerSingleAltAsExactMatch = false ;
      }

      spName = getPropertyByName ("SP_BY_ADDR_EXACT_MATCH") ;
      spInNames = new Vector () ;
      getDynamicParams ("SP_IN_", spInNames) ;

      //xmk: move to init?
      int spOutParamsNumber = String2int ((String) adapterProperties.get ("SP_OUT_PARAMS_NUMBER"), 0) ;

      Vector spOutValues = executeStoredProcedure (spName, spInNames, inputParser, spOutParamsNumber) ;

      if (spOutValues.size () != 1) {  // EXACT MATCH return check: should be only one
        // at this point we didn't get EXACT MATCH results
        // RUN stored proc for ALTRERNATIVES
        // discover # of alt stored procedures to run

        Vector spByAddrAltNames = new Vector () ;
        getDynamicParams ("SP_BY_ADDR_ALT_", spByAddrAltNames) ;

        // Execute alternative match stored procedures until return is not empty
        // Discover output params for alt stored proc
        for (int i = 0, size = spByAddrAltNames.size () ; i < size ; ++i) {

          spOutValues = executeStoredProcedure ((String) adapterProperties.get (SP_BY_ADDR_ALT_PATTERN + i), spInNames, inputParser, spOutParamsNumber) ;

          if (spOutValues.size () > 0) {
            break ;
          }
        }

        // Decide response type if return is not empty
        
        if (spOutValues.size () > 0) {  //ALTERNATIVE MATCH return check

          // nik (3/17/99)
          // this peace of code made by NF rqst
          // consider alt case with one alt as an exact match

          if (considerSingleAltAsExactMatch && spOutValues.size () == 1) {
            spResultType = SP_EXACT_MATCH_RESULT ;
          }
          else {
            spResultType = SP_ALT_RESULT ;
          }

        }
        else {
          spResultType = SP_NO_RESULT ;
        } //ALTERNATIVE MATCH return check

      }
      else {
        spResultType = SP_EXACT_MATCH_RESULT ;
      }  // EXACT MATCH return check


      // at this point we have (or do not have) results from stored proc
			// create response response
      String respPattern = null ;

			switch (spResultType) {

				case SP_EXACT_MATCH_RESULT: {
          respPattern = "RESP_MATCH_" ;
          break ;
        }

				case SP_ALT_RESULT: {
          respPattern = "RESP_ALT_" ;
          break ;
        }

        default:

				case SP_NO_RESULT: {
          respPattern = "RESP_ERROR_" ;
          break ;
        }
			}  // swtich

      String docName = (String) adapterProperties.get ("RESP_DOC_NAME") ;
      //Do not validate the response
      //String dtdName = (String) adapterProperties.get ("RESP_DTD_NAME") ;
      //XMLMessageGenerator generator = new XMLMessageGenerator (docName, dtdName) ;
      XMLMessageGenerator generator = new XMLMessageGenerator (docName) ;
			int cnt = 0 ;

      switch (spResultType) {

					case SP_EXACT_MATCH_RESULT: {
            // check if we need to correct WIRECENTER (LERG6 DB Lookup)
            // xmk: for alt too?
						correctWIRECENTER (spOutValues, inputParser) ;
            cnt = 0 ;
						break ;
					}

					case SP_ALT_RESULT: {
            //Alternative also checks wire center
            correctWIRECENTER (spOutValues, inputParser) ;
            cnt = spOutValues.size () < maxOutputAlternatives ? spOutValues.size () - 1 : maxOutputAlternatives - 1 ;
						break ;
					}

          case SP_NO_RESULT: {
            cnt = 0 ;
            break ;
          }

      }  // switch

			// NEW technology -- nik (10/26/99) -- MSAG main loop ----------------------------------
			Vector respMap = new Vector () ;
			getDynamicParams (respPattern, respMap) ;

			for ( ; cnt >= 0 ; --cnt) {
        String [] spOutParam = null ;

        if (spResultType != SP_NO_RESULT) {
          spOutParam = (String []) spOutValues.elementAt (cnt) ;
        }

        for (int i = 0, size = respMap.size () ; i < size ; ++i) {
          String propertyValue = (String) respMap.elementAt (i) ;
          String [] token = tokenize (propertyValue, ",") ;
          // token [0]    - response map
          // token [1]    - ? Zero or one allowed, * Zero or more allowed, + One or more allowed
          // token [2..n] - where to take source (# means from output fron SP, word means from props)
					String value = "" ; // by default empty string

					token [1] = token [1].trim () ;

          if (token [1].length () > 0) {   // token[1] length check
            switch (token [1].charAt (0)) {
              case '?':
              case '*': {
                value = null ;
                break ;
              }

              default:

              case '+': {
                value = "" ;
                break ;
              }
            } // switch
          }
          else {
            value = "" ;
          }  // token[1] length check

          for (int j = 2 ; j < token.length ; ++j) {
            token [j] = token [j].trim () ;
						int spIdx = String2int (token [j], -1) ;
            if (spIdx < 0) {
              // reference to another source property or value itself
							if (token [j].charAt (0) == '\"') {
                // value itself
                value = XMLPathParser.removeQuotes (token [j]) ;
              }
							else {
                // reference
                String tempValue = null ;
                try {
                  tempValue = inputParser.getValue ((String) adapterProperties.get (token [j])) ;
                }
                catch (MessageException ex) {
                  // do nothing, such node doesn't exist in input XML
                }

                if (tempValue != null) {
                  tempValue = tempValue.trim () ;
                  if (tempValue.length () > 0) {
                    value = tempValue ;
                  }
                }
              }
            }
            else {
              // output index from stored procedure
              if (spOutParam != null && spIdx < spOutParam.length && spOutParam [spIdx] != null )
              {
                spOutParam [spIdx] = spOutParam [spIdx].trim () ;

                if (spOutParam [spIdx].length () > 0) {
                  value = spOutParam [spIdx] ;
                }
              }
            }

          }

					if (value != null) {
						XMLPathParser.setValue (generator, token [0], value) ;
          }

        }

        if (cnt > 0) {
          Node copyNode = generator.getNode ((String) adapterProperties.get ("RESP_ALT_COPY_NODE")) ;
          Node parentNode = copyNode.getParentNode () ;
//          Document doc = generator.getDocument () ;
          //parentNode.appendChild (generator.copyNode (doc, copyNode)) ;
					parentNode.appendChild (copyNode.cloneNode (false)) ;
        }

      }
			// NEW technology ----------------------------------------------------------------------


      outputXML = generator.generate () ;
      
    } // try block
    catch (AVQEngineException ex) {
      Debug.log (Debug.IO_ERROR, this.getClass().getName() + " :" +
          "Address Validation Engine Failure: " + ex.getMessage()) ;
      throw new ProcessingException ( this.getClass().getName() + " :" +
          "Address Validation Engine Failure: " + ex.getMessage()) ;
    }

		return outputXML ;

  } // validateByAddress


	// ===================================================================================
  // xmk: need work here.
  // xmk: so far only exact match checks wire center. should do the same for alt
  /* xmk: BAN_SAGA and SWB (need to modify sp) return napnxx in wire center field.
          USWEST_SAGA may returns npa,nxx in wire center field. It may also return
          wire center code either 8 characters long or 7 characters long.
  */
	private void correctWIRECENTER (Vector spOutValues, XMLMessageParser inputParser)
      throws ProcessingException
  {

    if (spOutValues == null ||
        inputParser == null ||
        spOutValues.size () <= 0)
    {
      return ;
    }

    if(Debug.isLevelEnabled(Debug.MSG_DATA))
        Debug.log(Debug.MSG_DATA, "correctWIRECENTER: record number = " + spOutValues.size() );

    spLerg6Name  = (String) adapterProperties.get ("SP_LERG6_LOOKUP") ;

    if(Debug.isLevelEnabled(Debug.MSG_DATA))
        Debug.log(Debug.MSG_DATA, "LERG6 lookup stored procedure name: " + spLerg6Name);

    // Loop for all records so that this works for alternative as well as exact match
    for (int i = 0; i < spOutValues.size(); i++) {

      int ind = 0;

      if ( i == 0) {
        // Exact match
        ind = String2int ((String) adapterProperties.get ("SP_OUT_MATCH_WIRECENTER"), -1) ;
        
        if(Debug.isLevelEnabled(Debug.MSG_DATA))
		    Debug.log(Debug.MSG_DATA, "Index of wire center in exact match ouput: " + ind);
      } else {
        // Alternative match
        ind = String2int ((String) adapterProperties.get ("SP_OUT_ALT_WIRECENTER"), -1) ;
        
        if(Debug.isLevelEnabled(Debug.MSG_DATA))
		    Debug.log(Debug.MSG_DATA, "Index of wire center in alternative ouput: " + ind);
      }

      // Get returned record
		  String [] strArr = (String []) spOutValues.elementAt (i) ;
      
      if (ind >= 0 && ind < strArr.length) { // ind is valid

        String WIRECENTER = strArr [ind] ;
        
        if(Debug.isLevelEnabled(Debug.MSG_DATA))
		    Debug.log(Debug.MSG_DATA, "Wire center in ouput [" + i + "]: " + WIRECENTER);

        //Lerg6 lookup argument
        spLerg6LookupInNames = new Vector () ;

        if (WIRECENTER == null) {
          WIRECENTER = "" ;
        }

        if (WIRECENTER != null)	{

			    WIRECENTER = WIRECENTER.trim ();

          if ( WIRECENTER.length() > 0 && !Character.isDigit(WIRECENTER.charAt(0)) )
          {
              if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "Wire center in record [" + i + "] is not empty and does not start with digit, no change");
          }
          else
          {
            // Indices of nodes in exact match output that would be replaced with result of LERG6 lookup
            spLerg6LookupOutNames = new Vector () ;

            getDynamicParams ("SP_LERG6_LOOKUP_OUT_MATCH_", spLerg6LookupOutNames) ;

            Vector SP_LERG6_LOOKUP_OUT_PARAM_VALUE = new Vector();

            if ( WIRECENTER.length () <= 0 ) {
              // Old logic, use WTN to look up LERG6
              getDynamicParams ("SP_LERG6_LOOKUP_IN_", spLerg6LookupInNames) ;
              
              if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "Wire center in record [" + i + "] is empty, start LERG6 lookup with WTN");
              
              SP_LERG6_LOOKUP_OUT_PARAM_VALUE = executeStoredProcedure (spLerg6Name, spLerg6LookupInNames, inputParser, spLerg6LookupOutNames.size ()) ;
            }
            else if ( WIRECENTER.length() == 6 && StringUtils.getDigits(WIRECENTER).length() == 6 ) {
              //e.g. BAN and SWB
                
              if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "NPA and NXX are in record [" + i + "]: " + WIRECENTER);
              
              spLerg6LookupInNames.addElement(WIRECENTER.substring(0, 3));
              spLerg6LookupInNames.addElement(WIRECENTER.substring(3, 6));
              SP_LERG6_LOOKUP_OUT_PARAM_VALUE = executeStoredProcedureBase (spLerg6Name, spLerg6LookupInNames, spLerg6LookupOutNames.size ()) ;
            }
            else if ( WIRECENTER.length() == 7 && StringUtils.getDigits(WIRECENTER).length() == 6 ) {
              //e.g. USW, omit "," in "npa,nxx"
                
              if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "NPA and NXX are in record [" + i + "]: " + WIRECENTER);
              
              spLerg6LookupInNames.addElement(StringUtils.getDigits(WIRECENTER).substring(0, 3));
              spLerg6LookupInNames.addElement(StringUtils.getDigits(WIRECENTER).substring(3, 6));
              SP_LERG6_LOOKUP_OUT_PARAM_VALUE = executeStoredProcedureBase (spLerg6Name, spLerg6LookupInNames, spLerg6LookupOutNames.size ()) ;
            }

            if (SP_LERG6_LOOKUP_OUT_PARAM_VALUE != null && SP_LERG6_LOOKUP_OUT_PARAM_VALUE.size () > 0) {

              if(Debug.isLevelEnabled(Debug.MSG_DATA))
                Debug.log(Debug.MSG_DATA, "LERG6 lookup with WTN returns result");

              //String [] strArrOut    = (String []) spOutValues.elementAt (i) ;    //???
              String [] strArrLookup = (String []) SP_LERG6_LOOKUP_OUT_PARAM_VALUE.elementAt (0) ;

              for (int j = 0, size = spLerg6LookupOutNames.size () ; j < size ; j++) {
                int idx = String2int ((String) spLerg6LookupOutNames.elementAt (j), 0) ;
                strArr [idx] = strArrLookup [j] ;
                
                if(Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "Node [" + idx + "] in exact match response 1  is replaced with: " + strArr [idx]);
              } // all indecies need to be replaced

            } else {

                if(Debug.isLevelEnabled(Debug.MSG_DATA))
                    Debug.log(Debug.MSG_DATA, "LERG6 lookup returns nothing");
            
            } // Check LERG6 lookup results

          } // Check if WIRECENTER starts with digit

        } // WIRECANTER != null

      } // ind is valid

    } // for loop

  } // correctWIRECENTER



  public static Vector executeStoredProcedure (String spName, Vector spInNames, XMLMessageParser inputParser, int SP_OUT_PARAM_CNT)
      throws ProcessingException {

//    Vector spOutValues  = new Vector () ;
    Vector spInNamesVec = new Vector();

    for (int i = 0, size = spInNames.size ()  ; i < size ; i++) {

            String element = (String) spInNames.elementAt (i) ;
            String param = null ;

            try {
              param = inputParser.getValue (element) ;
            }
            catch (MessageParserException ex) {
							param = "" ;
            }
            spInNamesVec.addElement(param);
    }

//    return spOutValues =  executeStoredProcedureBase (spName, spInNamesVec, SP_OUT_PARAM_CNT);
    return executeStoredProcedureBase (spName, spInNamesVec, SP_OUT_PARAM_CNT);

  }



	// ===================================================================================
	public static Vector executeStoredProcedureBase (String spName, Vector spInNames,  int SP_OUT_PARAM_CNT)
      throws ProcessingException {

    Vector        spOutValues  = new Vector () ;
		Connection            conn = null ;
		CallableStatement    stmnt = null ;
		ResultSet           cursor = null ;
		String          ex_message = null ;

    try {

			conn = DBInterface.acquireConnection () ;

      if (conn != null) { // Check conn
      
				Debug.log ( Debug.MSG_STATUS, "Got database connection ");

        // construct call to Stored Proc
        String sqlStmnt = "BEGIN " + spName + "(" ;

        for (int i = 0, size = spInNames.size () ; i <= size ; i++) {

          if (i == 0)	{
						sqlStmnt += "?" ;
            continue ;
          }

					sqlStmnt += ",?" ;
        }

				sqlStmnt += "); END;" ;

        if(Debug.isLevelEnabled(Debug.MSG_STATUS))                
            Debug.log (Debug.MSG_STATUS, "sqlStmnt=" + sqlStmnt) ;

        stmnt = conn.prepareCall (sqlStmnt) ;

        if (stmnt != null) {  // Check stmnt

          for (int i = 0, size = spInNames.size ()  ; i < size ; i++) {

            String param = (String) spInNames.elementAt (i) ;

						stmnt.setString (i + 1, param.toUpperCase ()) ;

           if(Debug.isLevelEnabled(Debug.MSG_STATUS))
             Debug.log (Debug.MSG_STATUS, "[" + SP_IN_PATTERN  + i +"]=[" + (String) spInNames.elementAt (i) + "]=[" + param + "]") ;

          } // for loop

					stmnt.registerOutParameter (spInNames.size () + 1, OracleTypes.CURSOR) ;
          long timeStamp = timeStamp () ;

					stmnt.execute () ;

          // by nik
                    if(Debug.isLevelEnabled(Debug.MSG_STATUS)) 
					   Debug.log (Debug.MSG_STATUS, "*** Exec SP=[" + spName +"] *** " + timeStamp (timeStamp)) ;

					cursor = ((OracleCallableStatement) stmnt).getCursor (spInNames.size () + 1) ;

          if (cursor != null) {   // Check cursor

              if(Debug.isLevelEnabled(Debug.MSG_STATUS))
              {
                  Debug.log (Debug.MSG_STATUS, "Stored Procedure [" + spName + "] output:") ;
                  Debug.log (Debug.MSG_STATUS, "--- --- --- --- --- --- --- --- --- --- ---") ;
              }
						int recCounter = 0 ;

						while (cursor.next()) {

                            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                                Debug.log (Debug.MSG_STATUS, "*** RECORD [" + recCounter + "] ***") ;
              String [] outParam = new String [SP_OUT_PARAM_CNT] ;

							for (int i = 0 ; i < SP_OUT_PARAM_CNT ; i++) {

								outParam [i] = cursor.getString (i + 1) ;

                                if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                                    Debug.log (Debug.MSG_STATUS, "[" + i + "]=[" + outParam [i] + "]") ;

              } // for loop

              spOutValues.addElement (outParam) ;
              outParam = null ;
              ++recCounter ;

            } // while loop

            if(Debug.isLevelEnabled(Debug.MSG_STATUS))
                Debug.log ( Debug.MSG_STATUS, "--- --- --- --- --- --- --- --- --- --- ---") ;

            cursor.close () ; // cursor

            cursor = null ;

          } // Check cursor

					stmnt.close () ; // statement
          stmnt = null ;

				} // Check stmnt

				DBInterface.releaseConnection (conn) ; // return connection
				conn = null;

      } // Check conn

    }
		catch (SQLException ex) {
      Debug.log (Debug.ALL_ERRORS, "executeStoredProcedure :" + "SQL Execution failure: " + ex.getMessage());
      ex_message = ex.getMessage() ;
      //throw new ProcessingException ( this.getClass().getName() + " :" +
      //          "SQL Execution failure: " + ex.getMessage());
    }
    catch (DatabaseException ex) {
      Debug.log(Debug.ALL_ERRORS, "executeStoredProcedure :" +"Database failure: " + ex.getMessage());
      ex_message = ex.getMessage() ;
      //throw new ProcessingException ( this.getClass().getName() + " :" +
      //          "Database failure: " + ex.getMessage());
    }
    finally {
			try {
				if (cursor != null) {
					cursor.close () ;
				}
				if (stmnt != null) {
					stmnt.close () ;
        }
				if (conn != null) {
					DBInterface.releaseConnection (conn) ;
				}
      }
			catch (SQLException ex) {
        Debug.log(Debug.ALL_ERRORS, "executeStoredProcedure :" +
            "SQL Execution failure: " + ex.getMessage());
        ex_message = new String ( ex_message + ", " + ex.getMessage() );
			}
			catch (DatabaseException ex)
			{
        Debug.log(Debug.ALL_ERRORS, "executeStoredProcedure :" +
            "Database failure: " + ex.getMessage());
        ex_message = new String ( ex_message + ", " + ex.getMessage() );
      }
			if ( ex_message != null ) {
				throw new ProcessingException ( "executeStoredProcedure :" +
            "Database failure: " + ex_message);
			}//there is an exception
    }//finally

    return spOutValues ;

  } // executeStoredProcedure




	public static long timeStamp () {
		return System.currentTimeMillis () ;
	}



	public static String timeStamp (long startTime) {
		long currTime = System.currentTimeMillis () ;
		long diffTime = currTime - startTime ;

		long minutes  = diffTime / 60000 ;
		long seconds  = (diffTime % 60000) / 1000 ;
		long milisec  = diffTime % 1000 ;

		return " RT=" + minutes + ":" + seconds + "." + milisec ;
	}


  private int getDynamicParams (String pattern, Vector v)
	{
    int cnt = 0 ;

		for (int counter = 0, i = 0 ; counter < indexScanInterval; ++i)
    {
			String value = (String) adapterProperties.get (pattern + i) ;

			if (value == null || value.length () <= 0)
			{
           		++counter ;
			}
      else
      {
				v.addElement (value) ;
        ++cnt ;
      }
		}

    return cnt ;
  }


	
  /**
  * Initializes this object.
  *
  * @param  key   Property-key to use for locating initialization properties.
  * @param  type  Property-type to use for locating initialization properties.
  * @exception ProcessingException when initialization fails
  */
  public void initialize (String key, String type) throws ProcessingException
  {
    super.initialize (key, type) ;

    if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
        Debug.log (Debug.OBJECT_LIFECYCLE,    "Initializing the AVQ MSAG Engine.");

    // populate internal tables
    String indexScanIntervalString = getPropertyByName("INDEX_SCAN_INTERVAL");
    indexScanInterval = String2int (indexScanIntervalString, 50);

    Properties props = new Properties () ;

		// reading static properties first
		for (int i = 0 ; i < propStaticNames.length ; i++)
    {
      String name  = propStaticNames [i][0] ;
			String value = (String) adapterProperties.get (name) ;

      if ((value == null || value.length () <= 0) &&
        propStaticNames [i][1].equalsIgnoreCase ("true"))
      {
        throw new ProcessingException ("ERROR: AVQMSAGEngine: Required property [" + name + "] cannot be found or value is not set...") ;
      }

      props.put (name, value) ;
    }

		// reading dynamic properties

		for (int i = 0 ; i < propDynamicNames.length ; i++)
    {
			for (int j = 0, counter = 0 ; counter < indexScanInterval ; j++)
      {
        String name  = propDynamicNames [i][0] + j ;
				String value = (String) adapterProperties.get (name) ;

        if (value == null || value.length () <= 0)
        {
          ++counter ;
        }
        else
        {
          props.put (name, value) ;
          counter = 0 ;
        }

      }
      
    }

    if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log (Debug.MSG_STATUS, this.getClass().getName() + ".initialize: read follow properties from DB:") ;

    Enumeration en = props.keys () ;

		while (en.hasMoreElements ())
    {
      String name  = (String) en.nextElement () ;
      String value = (String) props.get (name) ;

      if(Debug.isLevelEnabled(Debug.MSG_STATUS))
        Debug.log (Debug.MSG_STATUS, "*** NAME=[" + name + "] *** VALUE=[" + value + "]") ;
    }
    
  } // initialize

  /**
   * Get property value
   *
   * @param   PropName     Name of the property.
   *
   * @return  String containing the value of the property.
   *
   * @exception  ProcessingException    Thrown when property not found.
   */
  private String getPropertyByName(String PropName) throws ProcessingException
  {
    String s = (String) adapterProperties.get(PropName);

    if( s==null)
      throw new ProcessingException( "ERROR: AVQMSAGEngine.getPropertyByName: " +
            "Null value found for property " + PropName );
    else {
        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "AVQMSAGEngine.getPropertyByName: Property "
            + PropName + " is set to " + s );
      return s;
    }
  }

    //------------------------------------------------------------------
	public static void main (String[] args)
    {

    	String str = "address_validation_response.preorder_response_header.CC,,RQST_0" ;

        try
        {
	        String [] strArr = tokenize (str, ",") ;

	       	System.out.println ("str=" + str) ;

    	    for (int i = 0 ; i < strArr.length ; i++)
        	{
        		System.out.println ("#" + i + "=[" + strArr [i] + "]") ;
	        }

        }
        catch (FrameworkException ex)
        {
	       	System.out.println (ex) ;
        }


        if( args.length < 3)
        {
          usage () ;
          return ;
        }

        Debug.enableAll () ;
        Debug.showLevels () ;

        String             key = args [0] ;
        String            type = args [1] ;
        String messageFileName = args [2] ;

        FileCache messageFile = new FileCache () ;

        String inputMessage = null ;

        try
        {
			inputMessage = messageFile.get (messageFileName) ;
        }
        catch (FrameworkException ex)
        {
			Debug.log ( Debug.MAPPING_ERROR,  "main: " + ex.getMessage ()) ;
			return;
        }

        // Read in message file and set up database:

        String dbProps = "db.properties" ;

        try
        {
			SetEnvironment.setSystemProperties (dbProps, true) ;
        }
        catch (FrameworkException ex)
        {
            Debug.log(Debug.MAPPING_ERROR, "main: " +
                      "SetEnvironment.setSystemProperties failed: " + ex.getMessage());
        }

        try
        {
            DBInterface.initialize () ;
        }
        catch (DatabaseException ex)
        {
            Debug.log(Debug.MAPPING_ERROR, "main: " +
                      "Database initialization failure: " + ex.getMessage());
        }

        AVQMSAGEngine engine = new AVQMSAGEngine () ;

        try
        {
            engine.initialize (key, type) ;
        }
        catch (ProcessingException ex)
        {
            Debug.log(Debug.MAPPING_ERROR, "AVQOutboundLogger.main: " +
                      "call to initialize failed with message: " + ex.getMessage());
        }

        try
        {
        	NVPair[] nvPair = engine.execute (null, inputMessage) ;

            System.out.println ("*** VALIDATING OUTPUT - BEGING ***") ;
//		    XMLMessageParser inputParser = getParser ((String) nvPair [0].value) ;
            System.out.println ("*** VALIDATING OUTPUT - END    ***") ;

            System.out.println ((String) nvPair [0].value) ;
        }
        catch (MessageException ex)
        {
            Debug.log(Debug.MAPPING_ERROR, "main: " +
                      "call to process failed with message: " + ex.getMessage()) ;
        }
        catch (ProcessingException ex)
        {
            Debug.log( Debug.MAPPING_ERROR, "main: " +
                      "call to process failed with message: " + ex.getMessage()) ;
        }

        Debug.log( Debug.DB_STATUS, "main: clearing prepared statement") ;

        try
        {
            engine.execute (null, null) ;
        }
        catch (MessageException ex)
        {
            Debug.log( Debug.MAPPING_ERROR, "main: " +
                      "call to process failed with message: " + ex.getMessage ()) ;
        }
        catch (ProcessingException ex)
        {
            Debug.log( Debug.MAPPING_ERROR, "main: " +
                      "call to process failed with message: " + ex.getMessage ()) ;
        }
    }

	private static void usage ()
	{
            System.err.println ("\nXinteX Information Services AVQ MSAG Engine\n\nUsage: \n\n" +
			 "  java " +  StartClass.getStartClassName() + " <property key> <property type> <request xml>]"
			);
	}
	/**
    * Converts String to int
    *
    * @param input               String to convert
    * @param defaultValue        dafault value in case of error
    *
    * @return                    converted int value or defaulValue if error occured
    */
	public static int String2int (String input, int defaultValue)
	{
		int value = defaultValue ;

		if (input != null && input.length () > 0)
        {
			try
			{
				value = Integer.parseInt  (input) ;
			}
			catch (NumberFormatException e)
			{
				value = defaultValue ;
			}
        }
		return value ;
	}

    public static String [] tokenize (String input, String separator)
    	throws AVQEngineException
	{
		if (input   == null || input.length ()   <= 0)
        {
        	throw new AVQEngineException ("ERROR: AVQMSAGEngine: CSVReader::tokenize: input file name cannot be null or have zero length...") ;
        }

        StringTokenizer st = new  StringTokenizer (input, separator, true) ;

        Vector output = new Vector () ;

        //System.out.println ("--- --- --- --- --- ---") ;

        String prev = separator ;

        for ( ; st.hasMoreTokens () ; )
        {
			String token = st.nextToken () ;

            //System.out.println ("token=[" + token + "]") ;

            if (token.equals (separator) && prev.equals (separator))
            {
				output.addElement ("") ;

	            //System.out.println ("#[" + i + "]=[]") ;
            }
            else
            {
            	if (!token.equals (separator))
                {
					output.addElement (token) ;

		            //System.out.println ("*[" + i + "]=[" + token + "]") ;
                }
            }

            prev = token ;

        }

        if (prev.equals (separator))
        {
			output.addElement ("") ;

            //System.out.println ("%[" + i + "]=[]") ;
        }

        return Vector2StringArray (output) ;
    }

	public static String [] Vector2StringArray (Vector v)
    {
    	if (v == null)
        {
        	return null ;
        }

        int size = v.size () ;

    	String [] arr = new String [size] ;

        for (int i = 0 ; i < size ; ++i)
        {
        	arr [i] = (String) v.elementAt (i) ;
			arr [i] = arr [i].trim () ;
        }

        return arr ;
    }
}
