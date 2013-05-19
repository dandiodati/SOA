/**
 * The purpose of this program is to get  records from NPA_SPLIT_TABLE and
 * NPA_SPLIT_NXX_TABLE , update the NBRPOOL_BLOCK_TABLE and SV_TABLE  accordingly . 
 * 
 * @author Phani Kumar
 * @version 1.1
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * 
 */

/** 
	Revision History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Phani			06/28/2004			Created
	2			Phani  			07/05/2004			PDP dates will be taken from 
													database.
	3			Phani			07/06/2004			Review comments incorporated
	
 */

package com.nightfire.spi.neustar_soa.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.util.Debug;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NPASplitScheduler {

	/**
	 * connection handle 
	 */
	private Connection conn = null;

	/**
	 * stores sql Query
	 */
	private StringBuffer sqlQuery = null;

	/**
	 * statement handle
	 */
	private PreparedStatement stmt = null;

	/**
	 * ResultSet handle
	 */
	private ResultSet rs = null;

	/**
	 * Timer handle
	 */

	Timer timer = null;

	/**
	 * list of new 'NPA's SPLIT
	 */
	ArrayList newNpaList = null;

	/**
	 * list of new 'NPA's SPLIT
	 */
	HashMap dateMap = null;

	/**
	 * Constructor.
	 * 
	 *  select new npa records from the NPA_SPLIT_TABLE   *
	 *
	 * @param oldNpa String the old NPA
	 * @param newNPA String the new NPA
	 *                      
	 * 
	 */
	public NPASplitScheduler(String oldNpa, String newNpa) {

		try {

			conn = DBInterface.acquireConnection();

		} catch (DatabaseException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());
		}
		
		if(conn != null){
			newNpaList = getNewNpaList(conn, oldNpa, newNpa);

			dateMap = getPDPDates(conn, oldNpa, newNpa);
		}
	}

	/**
	* schedule the task at given time
	* 
	* @param seconds long
	* @return void 
	*  
	*/
	public void schedule(long seconds) {

		timer = new Timer();

		timer.schedule(new RemindTask(), seconds);

	}

	/**
	 * This class extends java.util.TimerTask
	 * there by it can schedule a task at particular time
	 */
	class RemindTask extends TimerTask {

		/**
		 * start point for scheduled task
		 * @return void
		 */
		public void run() {

			process();

			System.out.println("Task's up");

			timer.cancel(); //Terminate the timer thread
		}
	}

	/**     
	 *update the SV_TABLE and NBRPOOL_TABLE accordingly     
	 * return void
	 */
	public void process() {

		ArrayList tnList = null;

		HashMap tnMap = null;

		try {

			tnList = getTnList(conn, newNpaList);

			if (tnList == null || tnList.size() <= 0) {

				System.out.println("No matching telephone numbers found ");

				return;

			}

			// update the NBRPOOL_BLOCK_TABLE with new 'Npa's

			sqlQuery = new StringBuffer("");

			sqlQuery.append("UPDATE  ");

			sqlQuery.append(SOAConstants.NBRPOOL_BLOCK_TABLE);

			sqlQuery.append("  SET ");

			sqlQuery.append(SOAConstants.NPA_COL);

			sqlQuery.append(" = ? WHERE ");

			sqlQuery.append(SOAConstants.NPA_COL);

			sqlQuery.append(" = ? AND ");

			sqlQuery.append(SOAConstants.NXX_COL);

			sqlQuery.append("= ? ");

			stmt = conn.prepareStatement(sqlQuery.toString());

			for (int i = 0;
				(newNpaList != null && i < newNpaList.size());
				i++) {

				tnMap = (HashMap) newNpaList.get(i);

				stmt.setObject(1, (String) tnMap.get(SOAConstants.NEWNPA_COL));

				stmt.setObject(2, (String) tnMap.get(SOAConstants.OLDNPA_COL));

				stmt.setObject(3, (String) tnMap.get(SOAConstants.NXX_COL));

				Debug.log(Debug.DB_DATA, "Executing  SQL:\n" + sqlQuery);

				stmt.execute();

			}

			if (stmt != null) {

				stmt.close();

			}

			//update SV_Table with 'newTn's

			sqlQuery = new StringBuffer("");

			sqlQuery.append("UPDATE /*+ INDEX(" + SOAConstants.SV_TABLE + " SOA_SV_INDEX_2) */ ");

			sqlQuery.append(SOAConstants.SV_TABLE);

			sqlQuery.append(" SET ");

			sqlQuery.append(SOAConstants.PORTINGTN_COL);

			sqlQuery.append(" = ? WHERE ");

			sqlQuery.append(SOAConstants.PORTINGTN_COL);

			sqlQuery.append(" = ?  ");

			stmt = conn.prepareStatement(sqlQuery.toString());

			for (int i = 0; i < tnList.size(); i++) {

				tnMap = (HashMap) tnList.get(i);

				stmt.setObject(1, (String) tnMap.get(SOAConstants.NEWTN));

				stmt.setObject(2, (String) tnMap.get(SOAConstants.OLDTN));

				Debug.log(Debug.DB_DATA, "Executing  SQL:\n" + sqlQuery);

				stmt.execute();

			}
			if (stmt != null) {

				stmt.close();
			}

			conn.commit();

		} catch (SQLException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());

		} finally {

			closeConnection(conn);
		}
	}

	/**
	 * release DB connection  	
	 * @param  Connection conn		
	 * @return  void
	 */

	private void closeConnection(Connection conn) {

		try {
			if (conn != null) {

				DBInterface.releaseConnection(conn);
			}

		} catch (DatabaseException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());
		}

	}

	/**
	 * 
	 * select 'PORTINGTN's records to update with new NPA 
	 * 
	 * @param conn the DB Connection handle 
	 * @param npaList the 'NPA's 'NXX's to be operated
	 * 
	 * @return ArrayList of the matching telephone numbers and
	 *                   the corresponding new numbers
	 */

	private ArrayList getTnList(Connection conn, ArrayList npaList) {

		String oldNpa = "";

		String newNpa = "";

		String newNxx = "";

		String oldTn = "";

		ArrayList tnList = new ArrayList();

		HashMap tnMap = null;

		sqlQuery = new StringBuffer("");

		sqlQuery.append("SELECT /*+ INDEX(" + SOAConstants.SV_TABLE + " SOA_SV_INDEX_2) */ ");

		sqlQuery.append(SOAConstants.PORTINGTN_COL);

		sqlQuery.append(" FROM ");

		sqlQuery.append(SOAConstants.SV_TABLE);

		sqlQuery.append(" WHERE ");

		sqlQuery.append(SOAConstants.PORTINGTN_COL);

		sqlQuery.append("  LIKE ? ");
		try {

			for (int i = 0;(npaList != null && i < npaList.size()); i++) {

				stmt = conn.prepareStatement(sqlQuery.toString());

				tnMap = (HashMap) npaList.get(i);
				if(tnMap != null){
					oldNpa = (String) tnMap.get(SOAConstants.OLDNPA_COL);

					newNpa = (String) tnMap.get(SOAConstants.NEWNPA_COL);

					newNxx = (String) tnMap.get(SOAConstants.NXX_COL);
	
					stmt.setString(1, oldNpa + "-" + newNxx + "-%");
	
					Debug.log(Debug.DB_DATA, "Executing  SQL:\n" + sqlQuery);
	
					stmt.execute();
	
					rs = stmt.getResultSet();
				}

				tnMap = null;

				while (rs.next()) {

					tnMap = new HashMap();

					oldTn = rs.getString(SOAConstants.PORTINGTN_COL);

					tnMap.put(SOAConstants.OLDTN, oldTn);

					tnMap.put(
						SOAConstants.NEWTN,
						newNpa + "-" + newNxx + oldTn.substring(7));

					tnList.add(tnMap);

				}
			}
			
		} catch (SQLException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());

			closeConnection(conn);
		}

		finally{
			try{
				if (stmt != null) {

					stmt.close();
				}
				if (rs != null) {

					rs.close();
				}
			}catch(Exception e){
				
			}
		}
		return tnList;
	}
	/**
	 * 
	 * select new npa records list 
	 * 
	 * @param conn the Connection object
	 * @param oldNPA the OLD NPA
	 * @param newNPA  the NEW NPA
	 * 
	 * @return ArrayList the PDP dates collection
	 * 
	 */

	private ArrayList getNewNpaList(
		Connection conn,
		String oldNpa,
		String newNpa) {

		ArrayList npaList = null;

		HashMap colList = null;

		sqlQuery = new StringBuffer("");

		try {

			sqlQuery.append("SELECT A.");

			sqlQuery.append(SOAConstants.OLDNPA_COL);

			sqlQuery.append(",A.");

			sqlQuery.append(SOAConstants.NEWNPA_COL);

			sqlQuery.append(", B.");

			sqlQuery.append(SOAConstants.NXX_COL);

			sqlQuery.append(" FROM ");

			sqlQuery.append(SOAConstants.NPA_SPLIT_TABLE);

			sqlQuery.append(" A ,");

			sqlQuery.append(SOAConstants.NPA_SPLIT_NXX_TABLE);

			sqlQuery.append(" B WHERE  A.");

			sqlQuery.append(SOAConstants.OLDNPA_COL);

			sqlQuery.append(" = B.");

			sqlQuery.append(SOAConstants.OLDNPA_COL);

			sqlQuery.append(" AND A.");

			sqlQuery.append(SOAConstants.NEWNPA_COL);

			sqlQuery.append(" = B.");

			sqlQuery.append(SOAConstants.NEWNPA_COL);

			sqlQuery.append(" AND A.");

			sqlQuery.append(SOAConstants.OLDNPA_COL);

			sqlQuery.append(" = ?");

			sqlQuery.append(" AND A.");

			sqlQuery.append(SOAConstants.NEWNPA_COL);

			sqlQuery.append(" = ?");

			stmt = conn.prepareStatement(sqlQuery.toString());

			stmt.setObject(1, oldNpa);

			stmt.setObject(2, newNpa);

			Debug.log(Debug.DB_DATA, "Executing  SQL:\n" + sqlQuery);

			stmt.execute();

			rs = stmt.getResultSet();

			npaList = new ArrayList();

			// build list of new 'npa's
			while (rs.next()) {

				colList = new HashMap();

				colList.put(
					SOAConstants.OLDNPA_COL,
					rs.getString(SOAConstants.OLDNPA_COL));

				colList.put(
					SOAConstants.NEWNPA_COL,
					rs.getString(SOAConstants.NEWNPA_COL));

				colList.put(
					SOAConstants.NXX_COL,
					rs.getString(SOAConstants.NXX_COL));

				npaList.add(colList);

			}

			if (npaList == null || npaList.size() <= 0) {

				Debug.log(
					Debug.ALL_ERRORS,
					"No records found in " + SOAConstants.NPA_SPLIT_TABLE);

			}
		} catch (SQLException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());

			closeConnection(conn);
		}
		finally {
			if(stmt != null){
				try {
					stmt.close();
				} catch (SQLException e) {
					Debug.log(Debug.ALL_ERRORS, "Error while closing the PrepareStatement: " + e.getMessage());
				}
			}
		}

		return npaList;

	}

	/**
	 * 
	 * Fetch the PDP start and end dates
	 * 
	 * @param conn the Connection object
	 * @param oldNPA the OLD NPA
	 * @param newNPA  the NEW NPA
	 * @return  HashMap the PDP dates collection
	 */
	private HashMap getPDPDates(
		Connection conn,
		String oldNPA,
		String newNPA) {

		HashMap pdpMap = null;

		sqlQuery = new StringBuffer("");

		try {

			sqlQuery.append(
				"SELECT "
					+ SOAConstants.PDPSTARTDATE_COL
					+ ","
					+ SOAConstants.PDPENDDATE_COL);
					

			sqlQuery.append(" FROM ");

			sqlQuery.append(SOAConstants.NPA_SPLIT_TABLE);

			sqlQuery.append(" WHERE  ");

			sqlQuery.append(SOAConstants.OLDNPA_COL);

			sqlQuery.append(" = ? ");

			sqlQuery.append(" AND ");

			sqlQuery.append(SOAConstants.NEWNPA_COL);

			sqlQuery.append(" = ? ");

			stmt = conn.prepareStatement(sqlQuery.toString());

			stmt.setObject(1, oldNPA);

			stmt.setObject(2, newNPA);

			Debug.log(Debug.DB_DATA, "Executing  SQL:\n" + sqlQuery);

			stmt.execute();

			rs = stmt.getResultSet();

			pdpMap = new HashMap();

			// build list of new 'npa's
			while (rs.next()) {

				pdpMap.put(SOAConstants.PDPSTARTDATE_COL, rs.getTimestamp(SOAConstants.PDPSTARTDATE_COL));

				pdpMap.put(SOAConstants.PDPENDDATE_COL, rs.getTimestamp( SOAConstants.PDPENDDATE_COL));

			}

		} catch (SQLException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());

			closeConnection(conn);
		}
		finally {
			if(stmt != null){
				try {
					stmt.close();
				} catch (SQLException e) {
					Debug.log(Debug.ALL_ERRORS, "Error while closing the PrepareStatement: " + e.getMessage());
				}
			}
		}

		return pdpMap;

	}

	public static void main(String args[]) {

		Properties props = new Properties();

		props.put("DEBUG_LOG_LEVELS", "all");

		props.put("LOG_FILE", "logmap.log");

		Debug.showLevels();

		Debug.configureFromProperties(props);

		if (args.length <= 0) {

			Debug.log(
				Debug.ALL_ERRORS,
				"NPASplitScheduler: USAGE:  "
					+ " jdbc:oracle:thin:@<IPADDRESS>:<PORT>:"
					+ "<DBNAME> <userid> <pswd>");
			System.out.println(
				"NPASplitScheduler: USAGE:  "
					+ " jdbc:oracle:thin:@<IPADDRESS>:<PORT>:"
					+ "<DBNAME> <userid> <pswd>");
			return;
		}

		try {
			DBInterface.initialize(args[0], args[1], args[2]);

		} catch (DatabaseException e) {
			Debug.log(
				null,
				Debug.MAPPING_ERROR,
				": " + "Database initialization failure: " + e.getMessage());
			System.out.print(" Database initialization failure: Invalid UserName/Password Or No suitable driver ");
				
			System.exit(-1);
					
				
		}

		

		//  open up standard input 
		BufferedReader br = null;
			
		try {

			String oldNpa = "";

			String newdNpa = "";

		
			
			
			while (true) {
				System.out.print(" Enter OLDNPA : ");
				// open up standard input 
				br = new BufferedReader(new InputStreamReader(System.in));

				oldNpa = br.readLine();

				if (oldNpa != null && oldNpa != "" && oldNpa.length() != 0 ) {

					break;

				} else {
					System.out.println(" OLD NPA can't be null. \n ");

				}
			}
			while (true) {
				System.out.print(" Enter NEWNPA : ");
				
				br = new BufferedReader(new InputStreamReader(System.in));

				newdNpa = br.readLine();

				if (newdNpa != null && newdNpa != ""
					&& newdNpa.length() != 0
					) {

					break;
				} else {
					System.out.print(" NEW NPA can't be null. \n ");

				}

			}
			
			if(oldNpa.equals(newdNpa) ) 
			{
				System.out.print(" NEW NPA and OLD NPA cannot be same. \n ");
				System.exit(-1);
				
			}
		
				
			
			NPASplitScheduler nsu = new NPASplitScheduler(oldNpa, newdNpa);

			if (nsu.newNpaList.isEmpty()) {

				Debug.log(
					Debug.NORMAL_STATUS,
					"No records found in "
						+ SOAConstants.NPA_SPLIT_TABLE
						+ ","
						+ SOAConstants.NPA_SPLIT_NXX_TABLE
						+ " for "
						+ oldNpa
						+ " & "
						+ newdNpa);

				System.out.println(
					"No records found in "
						+ SOAConstants.NPA_SPLIT_TABLE
						+ ","
						+ SOAConstants.NPA_SPLIT_NXX_TABLE
						+ " for "
						+ oldNpa
						+ " & "
						+ newdNpa);

				return;

			}
			
			
			
						

			Timestamp startDate =
				(Timestamp) nsu.dateMap.get(SOAConstants.PDPSTARTDATE_COL);

			Timestamp endDate =
				(Timestamp) nsu.dateMap.get(SOAConstants.PDPENDDATE_COL);

			Debug.log(
				Debug.NORMAL_STATUS,
				SOAConstants.PDPSTARTDATE_COL + " : " + startDate);

			Debug.log(
				Debug.NORMAL_STATUS,
				SOAConstants.PDPENDDATE_COL + " : " + endDate);

	
			if (startDate.getTime() > System.currentTimeMillis()) 
			{

				nsu.schedule(
				startDate.getTime() - System.currentTimeMillis());

				System.out.println("Task scheduled....");

			} else {

				nsu.process();

				System.out.println("Task's up.");
			}

		} catch (NumberFormatException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());

		} catch (IOException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());

		} catch (IllegalArgumentException e) {

			Debug.log(Debug.ALL_ERRORS, e.getMessage());

		} 

	}

}