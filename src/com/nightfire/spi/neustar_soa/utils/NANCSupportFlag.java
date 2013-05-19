/**
 * Copyright (c) 2005 NeuStar, Inc.  All rights reserved.
 *           
 * $Header: //spi/neustar-soa/main/com/nightfire/spi/neustar_soa/utils/NANCSupportFlag.java$
 */
package com.nightfire.spi.neustar_soa.utils;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import com.nightfire.framework.cache.CacheManager;
import com.nightfire.framework.cache.CachingObject;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.db.SQLUtil;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

/**
 * This class provides singleton instance of the class SPID wise, providing
 * the access to the information of NPAC Support Flag being populated by using the spid
 * 
 * Added BPELSPIDFLAG & PUSHDUEDATE in SOA 5.6.4.1
 * 
 * Added following SOA 5.6.5
 * 	SIMPLEPORTINDICATORFLAG, 
 * 	LASTALTERNATIVESPIDFLAG
 * 	SPT2EXPIRATIONFLAG
 */
public class NANCSupportFlag implements CachingObject
{
    /**
     * member that would be used to populate the information of NANCSupportFlag
     */
    private static HashMap hmNancProp = new HashMap();
    
    private static String className = "NANCSupportFlag";

    //  Values representing the DB values for the specific spid
    private String spid;
    private int nanc399flag;
    private int apiFlag;
    private String xsdValidationFlag;
    private String altBidNanc436Flag;
    private String altEulvNanc436Flag;
    private String altEultNanc436Flag;
    private String requestIdListFlag;
    private String logInitMessageFlag;
	private String tnCoalescingFlag;
	
	private String voiceURI;
	private String mmsURI;
	private String pocURI;
	private String presURI;
	private String smsURI;
	
    //Added for SOA 5.6.3 Requirement (Ticket #1-61887801)
    private String effectiveReleaseDateFlag;
    private String bpelSpidFlag;
    private Integer pushduedate;
    private String simplePortIndicatorFlag;
    private String spT2ExpirationFlag;
    private String lastAltSpidFlag;

    //Added in SOA 5.6.8 Release
    private String spCustom1Flag;
    private String spCustom2Flag;
    private String spCustom3Flag;
    private String pseudoLrnFlag;
    
  //Added in SOA 5.8 Release
    private String onspFlag;
    
  //Added in SOA 5.9 Release
    private Integer adjustDueDateFlag;
        

	// Creating constants to be used
    public static final String NANC399FLAG = "NANC399FLAG";
    public static final String APIFLAG = "APIFLAG";
    public static final String XSDVALIDATIONFLAG = "XSDVALIDATIONFLAG";
    public static final String ALTBIDNANC436FLAG = "ALTBIDNANC436FLAG";
    public static final String ALTEULVNANC436FLAG = "ALTEULVNANC436FLAG";
    public static final String ALTEULTNANC436FLAG = "ALTEULTNANC436FLAG";
    public static final String REQUESTIDLISTFLAG = "REQUESTIDLISTFLAG";
    
    public static final String VOICEURIFLAG = "VOICEURIFLAG";
    public static final String MMSURIFLAG = "MMSURIFLAG";
    public static final String POCURIFLAG = "POCURIFLAG";
    public static final String PRESURIFLAG = "PRESURIFLAG";
    public static final String SMSURIFLAG = "SMSURIFLAG";
    
    //Added for SOA 5.6.3 Requirement (Ticket #1-61887801)
    public static final String EFFECTIVERELEASEDATEFLAG = "EFFECTIVERELEASEDATEFLAG";
    
    // column name of SOA_NANC_SUPPORT_FLAG table
    ////LogInitMessage flag added at spid level to log request message in SOA_REQUEST_MESSAGE table.
	public static final String LOGINITMESSAGEFLAG = "LOGINITMESSAGEFLAG";
	public static final String TNCOALESCINGFLAG = "TNCOALESCINGFLAG";
	public static final String BPELSPIDFLAG = "BPELSPIDFLAG";
	public static final String PUSHDUEDATE = "PUSHDUEDATE";      
	public static final String SIMPLEPORTINDICATORFLAG = "SIMPLEPORTINDICATORFLAG";
	public static final String LASTALTERNATIVESPIDFLAG = "LASTALTERNATIVESPIDFLAG";
	public static final String SPT2EXPIRATIONFLAG = "SPT2EXPIRATIONFLAG";
	public static final String SPCUSTOM1FLAG = "SPCUSTOM1FLAG";
	public static final String SPCUSTOM2FLAG = "SPCUSTOM2FLAG";
	public static final String SPCUSTOM3FLAG = "SPCUSTOM3FLAG";
	public static final String PSEUDOLRNFLAG = "PSEUDOLRNFLAG";
	public static final String ONSPAUTOPOPULATIONFLAG = "ONSPAUTOPOPULATIONFLAG";
	public static final String ADJUSTDUEDATEFLAG = "ADJUSTDUEDATEFLAG";
	
	/**
     * This method would make the instances
     * of NANCSupportFlag class on per spid basis present in the Database
     * and store them in the HashMap as a key of spid field value.
     *
     * This would create one object at a time spid wise
     *
     * @param spid String
     * @return NANCSupportFlag obj 
     */
    private static NANCSupportFlag createInstance (String spid)
    {
        Connection dbConn = null;
        NANCSupportFlag nancFlagProp = null;
        Vector<String> vColumns = new Vector<String> ();
        Vector vAllRows = null;
        Vector vRow = null;
        Hashtable htWhere = new Hashtable();

        try
        {
            vColumns.add(NANC399FLAG);
            vColumns.add(APIFLAG);
            vColumns.add(XSDVALIDATIONFLAG);
            vColumns.add(ALTBIDNANC436FLAG);
            vColumns.add(ALTEULVNANC436FLAG);
            vColumns.add(ALTEULTNANC436FLAG);
            vColumns.add(REQUESTIDLISTFLAG);
            // LogInitMessage flag added at spid level to log request message in SOA_REQUEST_MESSAGE table.
            vColumns.add(LOGINITMESSAGEFLAG);
			vColumns.add(TNCOALESCINGFLAG);
			
			//Add 5 new flags for SOA 5.6.2 release for nanc 400
			vColumns.add(VOICEURIFLAG);
			vColumns.add(MMSURIFLAG);
			vColumns.add(POCURIFLAG);
			vColumns.add(PRESURIFLAG);
			vColumns.add(SMSURIFLAG);
			vColumns.add(EFFECTIVERELEASEDATEFLAG);
			vColumns.add(BPELSPIDFLAG);
			vColumns.add(PUSHDUEDATE);
			vColumns.add(SIMPLEPORTINDICATORFLAG);
			vColumns.add(LASTALTERNATIVESPIDFLAG);
			vColumns.add(SPT2EXPIRATIONFLAG);
			vColumns.add(SPCUSTOM1FLAG);
			vColumns.add(SPCUSTOM2FLAG);
			vColumns.add(SPCUSTOM3FLAG);
			vColumns.add(PSEUDOLRNFLAG);
			vColumns.add(ONSPAUTOPOPULATIONFLAG);
			vColumns.add(ADJUSTDUEDATEFLAG);
			
            htWhere.put("SPID", spid);
            dbConn = DBInterface.acquireConnection();
            /**
             * The fetchRows function of SQLUtil return the vector containing
             * vectors as a row containing the column data in it
             */
            vAllRows = SQLUtil.fetchRows(dbConn, "SOA_NANC_SUPPORT_FLAGS", vColumns, htWhere);

            if (vAllRows != null && vAllRows.size() > 0)
            {
                // Obtaining first row of the resultset
                vRow = (Vector) vAllRows.get(0);

                nancFlagProp = new NANCSupportFlag();
                nancFlagProp.setSPID(spid);
                
                if (vRow.get(0) != null) {
					nancFlagProp.setNanc399flag(StringUtils.getInteger(vRow
							.get(0).toString()));
				} else {
					nancFlagProp.setNanc399flag(StringUtils.getInteger("0"));
				}
				if (vRow.get(1) != null) {
					nancFlagProp.setApiFlag(StringUtils.getInteger(vRow.get(1)
							.toString()));
				} else {
					nancFlagProp.setApiFlag(StringUtils.getInteger("0"));
				}
				if (vRow.get(2) != null) {
					nancFlagProp.setXsdValidationFlag(vRow.get(2).toString());
				} else {
					nancFlagProp.setXsdValidationFlag("0");
				}
				if (vRow.get(3) != null) {
					nancFlagProp.setAltBidNanc436Flag(vRow.get(3).toString());
				} else {
					nancFlagProp.setAltBidNanc436Flag("0");
				}
				if (vRow.get(4) != null) {
					nancFlagProp.setAltEulvNanc436Flag(vRow.get(4).toString());
				} else {
					nancFlagProp.setAltEulvNanc436Flag("0");
				}
				if (vRow.get(5) != null) {
					nancFlagProp.setAltEultNanc436Flag(vRow.get(5).toString());
				} else {
					nancFlagProp.setAltEultNanc436Flag("0");
				}
				if (vRow.get(6) != null) {
					nancFlagProp.setRequestIdListFlag(vRow.get(6).toString());
				} else {
					nancFlagProp.setRequestIdListFlag("0");
				}
				//LogInitMessage flag added at spid level to log request message in SOA_REQUEST_MESSAGE table.
				if (vRow.get(7)!=null){
					nancFlagProp.setLogInitMessageFlag(vRow.get(7).toString());
				}else
				{
					nancFlagProp.setLogInitMessageFlag("0");
				}
				if( vRow.get(8) != null ){
					nancFlagProp.setTnCoalescingFlag( vRow.get(8).toString());
				} else {
					nancFlagProp.setTnCoalescingFlag("0");
				}
				
				//Added for SOA 5.6.2
				if( vRow.get(9) != null ){
					nancFlagProp.setVoiceURI(vRow.get(9).toString());
				} else {
					nancFlagProp.setVoiceURI("0");
				}
				if( vRow.get(10) != null ){
					nancFlagProp.setMmsURI(vRow.get(10).toString());
				} else {
					nancFlagProp.setMmsURI("0");
				}
				if( vRow.get(11) != null ){
					nancFlagProp.setPocURI(vRow.get(11).toString());
				} else {
					nancFlagProp.setPocURI("0");
				}
				if( vRow.get(12) != null ){
					nancFlagProp.setPresURI(vRow.get(12).toString());
				} else {
					nancFlagProp.setPresURI("0");
				}
				if( vRow.get(13) != null ){
					nancFlagProp.setSmsURI(vRow.get(13).toString());
				} else {
					nancFlagProp.setSmsURI("0");
				}
				if( vRow.get(14) != null ){
					nancFlagProp.setEffectiveReleaseDateFlag(vRow.get(14).toString());
				} else {
					nancFlagProp.setEffectiveReleaseDateFlag("0");
				}
				if( vRow.get(15) != null ){
					nancFlagProp.setBpelSpidFlag(vRow.get(15).toString());
				} else {
					nancFlagProp.setBpelSpidFlag("0");
				}
				if( vRow.get(16) != null ){
					nancFlagProp.setPushduedate(StringUtils.getInteger( vRow.get(16).toString()));
				}else{
					nancFlagProp.setPushduedate(null);
				}
				if( vRow.get(17) != null ){
					nancFlagProp.setSimplePortIndicatorFlag(vRow.get(17).toString());
				} else {
					nancFlagProp.setSimplePortIndicatorFlag("0");
				}
				if( vRow.get(18) != null ){
					nancFlagProp.setLastAltSpidFlag(vRow.get(18).toString());
				} else {
					nancFlagProp.setLastAltSpidFlag("0");
				}
				if( vRow.get(19) != null ){
					nancFlagProp.setSpT2ExpirationFlag(vRow.get(19).toString());
				} else {
					nancFlagProp.setSpT2ExpirationFlag("0");
				}
				if( vRow.get(20) != null ){
					nancFlagProp.setSpCustom1Flag(vRow.get(20).toString());
				} else {
					nancFlagProp.setSpCustom1Flag("0");
				}
				if( vRow.get(21) != null ){
					nancFlagProp.setSpCustom2Flag(vRow.get(21).toString());
				} else {
					nancFlagProp.setSpCustom2Flag("0");
				}
				if( vRow.get(22) != null ){
					nancFlagProp.setSpCustom3Flag(vRow.get(22).toString());
				} else {
					nancFlagProp.setSpCustom3Flag("0");
				}
				if( vRow.get(23) != null ){
					nancFlagProp.setPseudoLrnFlag(vRow.get(23).toString());
				} else {
					nancFlagProp.setPseudoLrnFlag("0");
				}
				if( vRow.get(24) != null ){
					nancFlagProp.setOnspFlag(vRow.get(24).toString());
				} else {
					nancFlagProp.setOnspFlag("0");
				}
				if( vRow.get(25) != null ){
					nancFlagProp.setAdjustDueDateFlag(StringUtils.getInteger( vRow.get(25).toString()));
				}else{
					nancFlagProp.setAdjustDueDateFlag(StringUtils.getInteger("0"));					
				}
             }
            else
            {
            	nancFlagProp = new NANCSupportFlag();
            	nancFlagProp.setNanc399flag(StringUtils.getInteger("0"));
            	nancFlagProp.setApiFlag(StringUtils.getInteger("0"));
            	nancFlagProp.setXsdValidationFlag("0");
            	nancFlagProp.setAltBidNanc436Flag("0");
            	nancFlagProp.setAltEulvNanc436Flag("0");
            	nancFlagProp.setAltEultNanc436Flag("0");
            	nancFlagProp.setRequestIdListFlag("0");
            	//LogInitMessage flag added at spid level to log request message in SOA_REQUEST_MESSAGE table.
            	nancFlagProp.setLogInitMessageFlag("0");
				nancFlagProp.setTnCoalescingFlag("0");
				//Added for SOA 5.6.2
				nancFlagProp.setVoiceURI("0");
				nancFlagProp.setMmsURI("0");
				nancFlagProp.setPocURI("0");
				nancFlagProp.setPresURI("0");
				nancFlagProp.setSmsURI("0");
				//Added for SOA 5.6.3
				nancFlagProp.setEffectiveReleaseDateFlag("0");
				nancFlagProp.setBpelSpidFlag("0");
				nancFlagProp.setSimplePortIndicatorFlag("0");
				nancFlagProp.setLastAltSpidFlag("0");
				nancFlagProp.setSpT2ExpirationFlag("0");
				nancFlagProp.setSpCustom1Flag("0");
				nancFlagProp.setSpCustom2Flag("0");
				nancFlagProp.setSpCustom3Flag("0");
				nancFlagProp.setPseudoLrnFlag("0");
				nancFlagProp.setOnspFlag("0");
				nancFlagProp.setAdjustDueDateFlag(StringUtils.getInteger("0"));
			}
            if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "createInstance(): Instance Created for [" + spid
                            + "] with values Nanc399flag as [" + nancFlagProp.getNanc399flag()
                            + "], ApiFlag as [" + nancFlagProp.getApiFlag()
                            + "], XsdValidationFlag as [" + nancFlagProp.getXsdValidationFlag()
                            + "], BidNanc436Flag as [" + nancFlagProp.getAltBidNanc436Flag()
                            + "], EulvNanc436Flag as [" + nancFlagProp.getAltEulvNanc436Flag()
                            + "], EultNanc436Flag as [" + nancFlagProp.getAltEultNanc436Flag()
                            + "], RequestIdFlag as [" + nancFlagProp.getRequestIdListFlag()
                            + "], LogInitMessageFlag as [" + nancFlagProp.getLogInitMessageFlag()
							+ "], TnCoalescingFlag as [" + nancFlagProp.getTnCoalescingFlag()
							//Added for SOA 5.6.2
							+ "], VoiceURIFlag as [" + nancFlagProp.getVoiceURI()
							+ "], MMSURIFlag as [" + nancFlagProp.getMmsURI()
							+ "], PocURIFlag as [" + nancFlagProp.getPocURI()
							+ "], PresRUIFlag as [" + nancFlagProp.getPresURI()
							+ "], SMSURIFlag as [" + nancFlagProp.getSmsURI()
							//Added for SOA 5.6.2
							+ "], EFFECTIVERELEASEDATEFLAG as [" + nancFlagProp.getEffectiveReleaseDateFlag()
							+ "], BPELSPIDFLAG as [" + nancFlagProp.getBpelSpidFlag()
							+ "], PUSHDUEDATE as [" + nancFlagProp.getPushduedate()
                            + "], SIMPLEPORTINDICATORFLAG as [" + nancFlagProp.getSimplePortIndicatorFlag()
                            + "], LASTALTERNATIVESPIDFLAG as [" + nancFlagProp.getLastAltSpidFlag()
                            + "], SPT2EXPIRATIONFLAG as [" + nancFlagProp.getSpT2ExpirationFlag()
                            + "], SPCUSTOM1FLAG as [" + nancFlagProp.getSpCustom1Flag()
                            + "], SPCUSTOM2FLAG as [" + nancFlagProp.getSpCustom2Flag()
                            + "], SPCUSTOM3FLAG as [" + nancFlagProp.getSpCustom3Flag()
                            + "], PSEUDOLRNFLAG as [" + nancFlagProp.getPseudoLrnFlag()
                            + "], ONSPAUTOPOPULATIONFLAG as [" + nancFlagProp.getOnspFlag()
                             + "], ADJUSTDUEDATEFLAG as [" + nancFlagProp.getAdjustDueDateFlag()
                            + "].");                
            }
        } catch (DatabaseException e ) {
            Debug.error("createInstance(): Unable to acquire Database connection. Error: " + e.toString());
        } catch (Exception e) {
            Debug.error("createInstance(): Error: " + e.toString());
        } finally {
            /** Here try is required to release the DB Connection Pool Instance */
            try {
                DBInterface.releaseConnection(dbConn);
            }
            catch (Exception e) {
                Debug.error("createInstance(): Error: " + e.toString());
            }
        }
        return nancFlagProp;        
    }
    
    /**
     * Returns the voiceURIFlag associated with this NANCSupportFlag.
     *
     * @return int voiceURIFlag associated with this NANCSupportFlag.
     */
    public String getVoiceURI() {
		return voiceURI;
	}
    
    /**
     * Sets the voiceURIFlag for this NANCSupportFlag.
     * @param voiceURIFlag int to be set. 
     */
    public void setVoiceURI(String voiceURI) {
		this.voiceURI = voiceURI;
	}
    
    /**
     * Returns the MMSURIFlag associated with this NANCSupportFlag.
     *
     * @return int MMSURIFlag associated with this NANCSupportFlag.
     */
	public String getMmsURI() {
		return mmsURI;
	}
	
	/**
     * Sets the MMSURIFlag for this NANCSupportFlag.
     * @param MMSURIFlag int to be set. 
     */
	public void setMmsURI(String mmsURI) {
		this.mmsURI = mmsURI;
	}
	
	/**
     * Returns the POCURIFlag associated with this NANCSupportFlag.
     *
     * @return int POCURIFlag associated with this NANCSupportFlag.
     */
	public String getPocURI() {
		return pocURI;
	}
	
	/**
     * Sets the POCURIFlag for this NANCSupportFlag.
     * @param POCURIFlag int to be set. 
     */
	public void setPocURI(String pocURI) {
		this.pocURI = pocURI;
	}
	
	/**
     * Returns the PRESURIFlag associated with this NANCSupportFlag.
     *
     * @return int PRESURIFlag associated with this NANCSupportFlag.
     */
	public String getPresURI() {
		return presURI;
	}
	
	/**
     * Sets the PRESURIFlag for this NANCSupportFlag.
     * @param PRESURIFlag int to be set. 
     */
	public void setPresURI(String presURI) {
		this.presURI = presURI;
	}
	
	/**
     * Returns the SMSURIFlag associated with this NANCSupportFlag.
     *
     * @return int SMSURIFlag associated with this NANCSupportFlag.
     */
	public String getSmsURI() {
		return smsURI;
	}
	
	/**
     * Sets the SMSURIFlag for this NANCSupportFlag.
     * @param SMSURIFlag int to be set. 
     */
	public void setSmsURI(String smsURI) {
		this.smsURI = smsURI;
	}
	/**
     * This method would check if the instance of the NANCSupportFlag already exists,
     * if find, the instance from cache will return else new instance will return.
     *
     * @param spid String
     * @return NANCSupportFlag instance
     * @throws NancPropException on error
     */
    public static NANCSupportFlag getInstance (String spid) throws NancPropException
    {

        if (!StringUtils.hasValue(spid)) {
            throw new NancPropException("getInstance(spid): spid is null or empty String.");
        }

        NANCSupportFlag nancFlagProp = null;
        
        nancFlagProp = (NANCSupportFlag) hmNancProp.get(spid);

        if (nancFlagProp == null)
        {
            synchronized (hmNancProp) {
            	nancFlagProp = createInstance(spid);
                if (nancFlagProp!=null) {
                	hmNancProp.put(nancFlagProp.getSPID(), nancFlagProp);
                }
                if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                    Debug.log(Debug.SECURITY_CONFIG,
                            "getInstance(spid): Total instance in pool for NANCSupportFlag are [" + hmNancProp.size() + "]");
                }
            }
        }
        else
        {
            if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "getInstance(spid): Instance found in pool for spid [" + spid + "]");
            }
        }

        if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
            Debug.log(Debug.SECURITY_CONFIG,
                    "getInstance(spid): Returning instance for spid [" + spid + "]");
        }

        return nancFlagProp;
    }

    /**
     * This method makes the hash map
     * containing the NPAC Flag Properties
     * per spid wise null (EMPTY).
     */
    public void flushCache() throws FrameworkException 
    {
        if (hmNancProp!=null)
        {
            synchronized(hmNancProp)
            {
            	hmNancProp.clear();
                if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                    Debug.log(Debug.SECURITY_CONFIG,
                            "flushNANCSupportFlag(): Flushing the NPAC Flag Properties...");
                }
            }
        } else {
            if (Debug.isLevelEnabled(Debug.SECURITY_CONFIG)) {
                Debug.log(Debug.SECURITY_CONFIG,
                        "flushNANCSupportFlag(): Being null no need to flush NPAC Flag Properties...");
            }
        }
    }

     
    /**
     * Sets the spid for this NANCSupportFlag.
     *
     * @param spid String to be set.
     */
    private void setSPID( String spid )
    {
        this.spid = spid;
    }

    /**
     * Returns the spid associated with this NANCSupportFlag.
     *
     * @return String spid associated with this NANCSupportFlag.
     */
    public String getSPID()
    {
        return spid;
    }

    /**
     * Sets the Nanc399flag for this NANCSupportFlag.
     *
     * @param Nanc399flag int to be set.
     */
    private void setNanc399flag( int nanc399flag )
    {
        this.nanc399flag = nanc399flag;
    }

    /**
     * Returns the Nanc399flag associated with this NANCSupportFlag.
     *
     * @return int Nanc399flag associated with this NANCSupportFlag.
     */
    public int getNanc399flag()
    {
        return nanc399flag;
    }

   /**
     * Sets the apiFlag for this NANCSupportFlag.
     *
     * @param apiFlag int to be set.
     */
    private void setApiFlag(int apiFlag) {
        this.apiFlag = apiFlag;
    }

    /**
     * Returns the apiFlag associated with this NANCSupportFlag.
     *
     * @return int apiFlag associated with this NANCSupportFlag.
     */
    public int getApiFlag() {
        return apiFlag;
    }    

   /**
     * Sets the xsdValidationFlag for this NANCSupportFlag.
     *
     * @param xsdValidationFlag to be set.
     */
    private void setXsdValidationFlag(String xsdValidationFlag) {
        this.xsdValidationFlag = xsdValidationFlag;
    }
    /**
     * Returns the xsdValidationFlag associated with this NANCSupportFlag.
     *
     * @return String xsdValidationFlag associated with this NANCSupportFlag.
     */
    public String getXsdValidationFlag() {
        return xsdValidationFlag;
    }
    /**
     * Returns the AltBidNanc436Flag associated with this NANCSupportFlag.
     *
     * @return String AltBidNanc436Flag associated with this NANCSupportFlag.
     */
    public String getAltBidNanc436Flag() {
		return altBidNanc436Flag;
	}
    /**
     * Sets the AltBidNanc436Flag for this NANCSupportFlag.
     *
     * @param AltBidNanc436Flag String to be set.
     */
	private void setAltBidNanc436Flag(String altBidNanc436Flag) {
		this.altBidNanc436Flag = altBidNanc436Flag;
	}
	/**
     * Returns the AltEulvNanc436Flag associated with this NANCSupportFlag.
     *
     * @return String AltEulvNanc436Flag associated with this NANCSupportFlag.
     */

	public String getAltEulvNanc436Flag() {
		return altEulvNanc436Flag;
	}
	/**
     * Sets the altEulvNanc436Flag for this NANCSupportFlag.
     *
     * @param altEulvNanc436Flag String to be set.
     */

	private void setAltEulvNanc436Flag(String altEulvNanc436Flag) {
		this.altEulvNanc436Flag = altEulvNanc436Flag;
	}
	/**
     * Returns the altEultNanc436Flag associated with this NANCSupportFlag.
     *
     * @return String altEultNanc436Flag associated with this NANCSupportFlag.
     */

	public String getAltEultNanc436Flag() {
		return altEultNanc436Flag;
	}
	
	/**
     * Sets the altEultNanc436Flag for this NANCSupportFlag.
     *
     * @param altEultNanc436Flag String to be set.
     */

	private void setAltEultNanc436Flag(String altEultNanc436Flag) {
		this.altEultNanc436Flag = altEultNanc436Flag;
	}
	
	/*
	 * A single private instance of this class will be created to assist in cache flushing.
 	 */
	private NANCSupportFlag(){
		
		if ( Debug.isLevelEnabled( Debug.SYSTEM_CONFIG )){
			
			Debug.log(Debug.SYSTEM_CONFIG, className + " : Initializing...");
		}
			
		try
        {
            CacheManager.getRegistrar().register( this );
                       
        }
        catch ( Exception e )
        {
            Debug.warning( e.toString() );
        }
	}
	
    //Used exclusively for cache flushing.
    private static NANCSupportFlag flush = new NANCSupportFlag( );

    /**
     * Returns the requestIdListFlag associated with this NANCSupportFlag.
     *
     * @return String requestIdListFlag associated with this NANCSupportFlag.
     */
	public String getRequestIdListFlag() {
		return requestIdListFlag;
	}
	/**
     * Sets the requestIdListFlag for this NANCSupportFlag.
     *
     * @param requestIdListFlag String to be set.
     */
	private void setRequestIdListFlag(String requestIdListFlag) {
		this.requestIdListFlag = requestIdListFlag;
	}
	public String getLogInitMessageFlag() {
		return logInitMessageFlag;
	}
	public void setLogInitMessageFlag(String logInitMessageFlag) {
		this.logInitMessageFlag = logInitMessageFlag;
	}

   public String getTnCoalescingFlag() {
		return tnCoalescingFlag;
	}
	public void setTnCoalescingFlag(String tnCoalescingFlag) {
		this.tnCoalescingFlag = tnCoalescingFlag;
	}
	
	//Added for SOA 5.6.3 Requirement (Ticket #1-61887801)
	public String getEffectiveReleaseDateFlag() {
		return effectiveReleaseDateFlag;
	}

	private void setEffectiveReleaseDateFlag(String effectiveReleaseDateFlag) {
		this.effectiveReleaseDateFlag = effectiveReleaseDateFlag;
	}
	
	private void setBpelSpidFlag(String bpelSpidFlag){
		this.bpelSpidFlag = bpelSpidFlag;
	}
	public String getBpelSpidFlag(){
		return bpelSpidFlag; 
	}
	private void setPushduedate(Integer pushduedate) {
		this.pushduedate = pushduedate;
	}
	public Integer getPushduedate() {
		return pushduedate;
	}
	private void setSimplePortIndicatorFlag(String simplePortIndicatorFlag) {
		this.simplePortIndicatorFlag = simplePortIndicatorFlag;
	}
	public String getSimplePortIndicatorFlag() {
		return simplePortIndicatorFlag;
	}
	private void setLastAltSpidFlag(String lastAltSpidFlag){
		this.lastAltSpidFlag=lastAltSpidFlag;
	}
	public String getLastAltSpidFlag(){
		return lastAltSpidFlag;
	}
	private void setSpT2ExpirationFlag(String spT2ExpirationFlag) {
		this.spT2ExpirationFlag = spT2ExpirationFlag;
	}
    public String getSpT2ExpirationFlag() {
		return spT2ExpirationFlag;
	}
    
    /**
	 * @return the spCustom1Flag
	 */
	public String getSpCustom1Flag() {
		return spCustom1Flag;
	}

	/**
	 * @param spCustom1Flag the spCustom1Flag to set
	 */
	public void setSpCustom1Flag(String spCustom1Flag) {
		this.spCustom1Flag = spCustom1Flag;
	}

	/**
	 * @return the spCustom2Flag
	 */
	public String getSpCustom2Flag() {
		return spCustom2Flag;
	}

	/**
	 * @param spCustom2Flag the spCustom2Flag to set
	 */
	public void setSpCustom2Flag(String spCustom2Flag) {
		this.spCustom2Flag = spCustom2Flag;
	}

	/**
	 * @return the spCustom3Flag
	 */
	public String getSpCustom3Flag() {
		return spCustom3Flag;
	}

	/**
	 * @param spCustom3Flag the spCustom3Flag to set
	 */
	public void setSpCustom3Flag(String spCustom3Flag) {
		this.spCustom3Flag = spCustom3Flag;
	}
	
	/**
	 * @return the pSeudoLrnFlag
	 */
	public String getPseudoLrnFlag(){
		return pseudoLrnFlag;
	}
	
	/**
	 * @param pseudoLrnFlag to set 
	 */
	public void setPseudoLrnFlag(String pseudoLrnFlag){
		this.pseudoLrnFlag = pseudoLrnFlag;
	}
	
	/**
	 * @return the onspFlag
	 */
	
	public String getOnspFlag() {
		return onspFlag;
	}
	/**
	 * @param onspFlag to set 
	 */
	public void setOnspFlag(String onspFlag) {
		this.onspFlag = onspFlag;
	}

	public Integer getAdjustDueDateFlag() {
		return adjustDueDateFlag;
	}

	public void setAdjustDueDateFlag(Integer adjustDueDateFlag) {
		this.adjustDueDateFlag = adjustDueDateFlag;
	}

}
