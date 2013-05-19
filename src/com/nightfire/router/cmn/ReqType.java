package com.nightfire.router.cmn;

/**
 * Enum class representing various request types in clearing house. 
 * @author hpirosha
 */
public class ReqType {

    public static enum REQTYPE {ASYNC,SYNC,SYNC_W_HEADER_REQUEST};
    
    private REQTYPE reqType ;
    
    private ReqType(REQTYPE reqType)
    {
       this.reqType = reqType;
    }
    
   /**
    * Method to obtain an instance of request type based upon the 
    * passed in member of enum.  
    * @param type REQTYPE
    * @return ReqType
    */
    public static ReqType getReqType(REQTYPE type)
    {
        return new ReqType(type);    
    }

    public boolean equals(Object aReq)
    {
        if((aReq!=null) && (aReq instanceof ReqType) && 
                this.intValue() == ((ReqType)aReq).intValue())
            return true;
        
        return false;
    }
    
    public int intValue()
    {
        return reqType.ordinal();
    }
    
    public int hashCode() 
    {
        return reqType.ordinal();
    }
    
}
