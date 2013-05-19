package com.nightfire.spi.neustar_soa.rules;

import org.w3c.dom.Document;

import com.nightfire.framework.message.MessageException;
import com.nightfire.framework.message.parser.xml.XMLMessageParser;
import com.nightfire.framework.message.generator.xml.XMLMessageGenerator;

public class Context {

	public static final String DEFAULT_ROOT_NODE = "Body";

	/**
	* This is the name of the node in the GUI context that contains that
	* name of the GUI service type, e.g. "SOAPortIn".
	*/
	public static final String SERVICE_TYPE_NODE = "ServiceType";

	/**
	 * This is the name of the SVID node in the GUI context XML.
	 */
	public static final String SVID_NODE = "SvId";

	/**
	 * This is the name of the region node in the GUI context XML.
	 */
	public static final String REGION_NODE = "RegionId";

	/**
	 * This is the name of the telephone number node in the GUI context XML.
	 */
	public static final String TN_NODE = "Tn";

	/**
	 * This is the name of the old service provider node in the GUI context XML.
	 */
	public static final String OLD_SP_NODE = "OldSP";

	/**
	 * This is the name of the new service provider node in the GUI context XML.
	 */
	public static final String NEW_SP_NODE = "NewSP";

	/**
	 * This is the name of the Lrn value node in the GUI context XML.
	 */
	public static final String LRN_VALUE_NODE = "LrnValue";

	/**
	 * This is the name of the Lrn ID node in the GUI context XML.
	 */
	public static final String LRN_ID_NODE = "LrnId";

	/**
	 * This is the name of the NPA value node in the GUI context XML.
	 */
	public static final String NPA_NODE = "Npa";

	/**
	 * This is the name of the NXX value node in the GUI context XML.
	 */
	public static final String NXX_NODE = "Nxx";

	/**
	* This is the name of the DASHX value node in the GUI context XML.
	*/
	public static final String DASHX_NODE = "DashX";

	/**
	 * This is the name of the NpaNxxId  node in the GUI context XML.
	 */
	public static final String NPA_NXX_ID_NODE = "NpaNxxId";

	/**
	 * This is the name of the NpbId  node in the GUI context XML.
	 */
	public static final String BLOCKID_NODE = "NpbId";

	/**
	 * This is the name of the SPID  node in the GUI context XML.
	 */
	public static final String SPID_NODE = "SPID";

	/**
	 * This is the name of the GTTID  node in the GUI context XML.
	 */
	public static final String GTTID_NODE = "GTTID";

	/**
	 * This is the name of the Lrn  node in the GUI context XML.
	 */
	public static final String LRN_NODE = "Lrn";

	/**
	 * This is the name of the AuditName  node in the GUI context XML.
	 */
	public static String AUDITNAME_NODE = "AuditName";

	/**
	 * This is the name of the AuditId  node in the GUI context XML.
	 */
	public static String AUDITID_NODE = "AuditId";

	/**
	 * This is the name of the Status  node in the GUI context XML.
	 */
	public static String STATUS_NODE = "SvStatus";

	private XMLMessageParser parsedContext;

	private XMLMessageGenerator contextGenerator;

	public Context() throws MessageException {

		this(DEFAULT_ROOT_NODE);

	}

	public Context(String rootNode) throws MessageException {

		contextGenerator = new XMLMessageGenerator(rootNode);
		parsedContext = (XMLMessageParser) contextGenerator.getParser();

	}

	public Context(Document doc) throws MessageException {

		parsedContext = new XMLMessageParser(doc);
		contextGenerator = (XMLMessageGenerator) parsedContext.getGenerator();

	}

	public String getXML() throws MessageException {

		return contextGenerator.generate();

	}

	public Document getDocument() throws MessageException {

		return contextGenerator.getDocument();

	}

	public void setValue(String path, String value) throws MessageException {

		contextGenerator.setValue(path, value);

	}

	public String getValue(String path) throws MessageException {

		return parsedContext.getValue(path);

	}

	public boolean valueExists(String path) {

		return parsedContext.valueExists(path);

	}

	public String getServiceType() throws MessageException {

		return getValue(SERVICE_TYPE_NODE);

	}

	public void setServiceType(String serviceType) throws MessageException {

		setValue(SERVICE_TYPE_NODE, serviceType);

	}

	public String getSvId() throws MessageException {

		return getValue(SVID_NODE);

	}

	public void setSvId(String svid) throws MessageException {

		setValue(SVID_NODE, svid);

	}

	public String getRegionId() throws MessageException {

		return getValue(REGION_NODE);

	}

	public void setRegionId(String region) throws MessageException {

		setValue(REGION_NODE, region);

	}

	public String getTn() throws MessageException {

		return getValue(TN_NODE);

	}

	public void setTn(String tn) throws MessageException {

		setValue(TN_NODE, tn);

	}

	public String getOldSP() throws MessageException {

		return getValue(OLD_SP_NODE);

	}

	public void setOldSP(String sp) throws MessageException {

		setValue(OLD_SP_NODE, sp);

	}

	public String getNewSP() throws MessageException {

		return getValue(NEW_SP_NODE);

	}

	public void setNewSP(String sp) throws MessageException {

		setValue(NEW_SP_NODE, sp);

	}

	public String getLrnValue() throws MessageException {

		return getValue(LRN_VALUE_NODE);

	}

	public void setLrnValue(String lrnValue) throws MessageException {

		setValue(LRN_VALUE_NODE, lrnValue);

	}

	public String getAuditId() throws MessageException {

		return getValue(AUDITID_NODE);

	}

	public void setAuditId(String auditId) throws MessageException {

		setValue(AUDITID_NODE, auditId);

	}

	public String getAuditName() throws MessageException {

		return getValue(AUDITNAME_NODE);

	}

	public void setAuditName(String auditName) throws MessageException {

		setValue(AUDITNAME_NODE, auditName);

	}

	public String getLrnId() throws MessageException {

		return getValue(LRN_ID_NODE);

	}

	public void setLrnId(String lrnId) throws MessageException {

		setValue(LRN_ID_NODE, lrnId);

	}

	public String getBlockId() throws MessageException {

		return getValue(BLOCKID_NODE);

	}

	public void setBlockId(String blockId) throws MessageException {

		setValue(BLOCKID_NODE, blockId);
	}

	public String getSpid() throws MessageException {

		return getValue(SPID_NODE);

	}

	public void setSpid(String spid) throws MessageException {

		setValue(SPID_NODE, spid);
	}

	public String getGttId() throws MessageException {

		return getValue(GTTID_NODE);

	}

	public void setGttId(String gttId) throws MessageException {

		setValue(GTTID_NODE, gttId);
	}

	public String getLrn() throws MessageException {

		return getValue(LRN_NODE);

	}

	public void setLrn(String lrn) throws MessageException {

		setValue(LRN_NODE, lrn);
	}

	public String getNpaNxxId() throws MessageException {

		return getValue(NPA_NXX_ID_NODE);

	}

	public void setNpaNxxId(String npaNxxId) throws MessageException {

		setValue(NPA_NXX_ID_NODE, npaNxxId);
	}

	public String getNpa() throws MessageException {

		return getValue(NPA_NODE);

	}

	public void setNpa(String npa) throws MessageException {

		setValue(NPA_NODE, npa);
	}

	public String getNxx() throws MessageException {

		return getValue(NXX_NODE);

	}

	public void setNxx(String nxx) throws MessageException {

		setValue(NXX_NODE, nxx);
	}

	public String getDashX() throws MessageException {

		return getValue(DASHX_NODE);

	}

	public void setDashX(String dashx) throws MessageException {

		setValue(DASHX_NODE, dashx);
	}

	public String getStatus() throws MessageException {

		return getValue(STATUS_NODE);

	}

	public void setStatus(String status) throws MessageException {

		setValue(STATUS_NODE, status);
	}

}
