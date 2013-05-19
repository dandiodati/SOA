package com.nightfire.framework.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.PersistentProperty;
import com.nightfire.framework.db.PropertyException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.StringUtils;

/**
 * Utility Class that generates ID's using a database sequence. ID's are incremented by one each time
 * when #getNextId or #getStrNextId is invoked until next sequence is fetched from database.
 * By default the next sequence is fetched after 100 increments
 */
public class SeqIdGenerator {

    /**
     * Name of database sequence used to generate ids.
     */
    private String seqName;
    /**
     * String containing SQL query to select the next value of sequence
     */
    private String SQL_QRY;

    /**
     * Default number of times to increment the sequence before fetching the next value from database.
     */
    private static int MAX_SEQ_INCR_COUNT = 100;

    /**
     * Counter to track number of increments.
     */
    private int nextIdIdx;

    /**
     * Current value of sequence in memory i.e. current unique id
     */
    private long currSeq;

    /**
     * Properties to be read from database for intializing this class
     */
    private static final String PROP_SEQ_NM = "REQUEST_ID_SEQUENCE_NM";
    private static final String PROP_ID_INCR_COUNT = "REQUEST_ID_INCR_COUNT";
    private static final String PROP_COMMMON_TYPE = "COMMON_PROPERTIES";

    /**
     * Create a Sequence ID Generator.
     * @param seqName String name of the database sequence to use for generating ID's.
     * @param maxSeqIncrCount int number of times to increment, before fetching next value from database
     */
    public SeqIdGenerator(String seqName,int maxSeqIncrCount)
    {
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,"Initializing SeqIdGenerator with sequence["+seqName+"]");

        this.seqName = seqName;
        SQL_QRY = "SELECT "+seqName+".NEXTVAL FROM DUAL";
        nextIdIdx = maxSeqIncrCount;
    }

    /**
     * Create a Sequence ID Generator which uses sequence as configured in persistent property table.
     * Number of times the sequence is incremented (by one) before fetching its next value from database
     * is also configurable.
     *
     * <p>
     * Properties that needs to be configured while using below constructor are
     * <ul>
     * <li>  REQUEST_ID_SEQUENCE_NM  : database sequence to use for generating ID's.
     * <li>  REQUEST_ID_INCR_COUNT  : number of times to increment, before fetching next value from database
     *                                    if not specified then defaults to 100.
     * </ul>
     * @throws PropertyException
     */
    public SeqIdGenerator() throws PropertyException
    {
        //Get sequence name and number of ids to increment before syncing with database.
        this.seqName = PersistentProperty.get(PROP_COMMMON_TYPE,PROP_COMMMON_TYPE,PROP_SEQ_NM);
        String val = PersistentProperty.get(PROP_COMMMON_TYPE,PROP_COMMMON_TYPE,PROP_ID_INCR_COUNT);
        if(Debug.isLevelEnabled(Debug.OBJECT_LIFECYCLE))
            Debug.log(Debug.OBJECT_LIFECYCLE,"Initializing SeqIdGenerator with sequence["+seqName+"]");

        if(StringUtils.hasValue(val))
            MAX_SEQ_INCR_COUNT = Integer.parseInt(val);

        nextIdIdx = MAX_SEQ_INCR_COUNT;
        SQL_QRY = "SELECT "+seqName+".NEXTVAL FROM DUAL";
    }

    /**
     * Get the next generated ID.
     * ID's are always incremented by one
     * @return long
     */
    public long getNextId() throws FrameworkException
    {
        synchronized(SeqIdGenerator.class)
        {
            if(nextIdIdx==MAX_SEQ_INCR_COUNT)
            {
               fetchNextSeq();
               nextIdIdx=0;
            }
            ++nextIdIdx;
            return ++currSeq;
        }
    }

    /**
     * Get the next String ID.
     * ID is prepended with 0's and a String is returned whose length is 10.
     * @return String
     */
    public String getStrNextId() throws FrameworkException
    {
        synchronized(SeqIdGenerator.class)
        {
            if(nextIdIdx==MAX_SEQ_INCR_COUNT)
            {
               fetchNextSeq();
               nextIdIdx=0;
            }
            ++nextIdIdx;
            ++currSeq;
        }

        String id = String.valueOf(currSeq);
        if(id.length()<10)
        {
            int zerosToAppend = 10 - id.length();
            StringBuffer str = new StringBuffer();

            for(int i=0;i<zerosToAppend;i++)
                str.append('0');
            return str.append(id).toString();
        }
        return id;

    }

    private void fetchNextSeq() throws FrameworkException
    {
        Connection conn = null;
        PreparedStatement pstmt =null;
        ResultSet rs = null;
        
        try
        {
            conn = DBInterface.acquireConnection();
            pstmt = conn.prepareStatement(SQL_QRY);
            rs = pstmt.executeQuery();
            rs.next();
            currSeq = rs.getLong(1);
        }
        catch(Exception e)
        {
            Debug.log(Debug.ALL_ERRORS,"Exception occured while fetching sequence from database "+e.getMessage());
            throw new FrameworkException(e);
        }
        finally
        {
            try{
             if(rs!=null)
                rs.close();
            }catch(SQLException e){}

            try{
            if(pstmt!=null)
                pstmt.close();
            }catch(SQLException e){}

            try
            {
              DBInterface.releaseConnection(conn);
            }catch(Exception e) {}
                }
    }
    
    public static void main(String args[]) throws Exception
    {
        DBInterface.initialize("jdbc:oracle:thin:@192.168.96.192:1521:ORCL132", "hpirosha", "hpirosha");
        SeqIdGenerator gen = new SeqIdGenerator("test_seq",100);
        for(int i=0;i<200;i++)
        {
            if(i%2==0)
                System.out.println("Seq Id [long]:"+gen.getNextId());
            else
                System.out.println("Seq Id [str]:"+gen.getStrNextId());
        }

    }
}