/**
 * This class uses an SV Token Consumer to update the SOA_SUBSCRIPTION_VERSION
 *  table based on the contents of an SV BDD file.
 * 
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.file.DelimitedFileReader 
 * @see com.nightfire.framework.db.DBInterface
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.spi.neustar_soa.file.SVTokenConsumer
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			04/12/2004			Created
	2			Ashok 			07/15/2004			FI review comment 
													incorporated
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.io.FileNotFoundException;
import java.sql.SQLException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class SVFileReader extends DelimitedFileReader {

	/**
	* This reads the named file and updated the SV table based on its
	* contents.
	*
	* @param file String an SV BDD file
	* @param spid String only SVs with this SPID value will be processed.
	* @param region String all SVs will be assigned to this region.
	* @throws FileNotFoundException the named file was not found or was
	*                               inaccessable.
	* @throws FrameworkException thrown if some other error occurs during
	*                            processing. e.g. a database error or
	*                            bad formatting in the input file.
	*/
	public void read(String file, String spid, String region)
		throws FileNotFoundException, FrameworkException, SQLException {
		boolean success = true;

		SVTokenConsumer svConsumer = null;		

		try {

			svConsumer = new SVTokenConsumer(spid, region, SOAConstants.SV_BDD);

			// initialize the DB connection
			svConsumer.init(file);

			// Set customer ID
			svConsumer.setCustometID();

			super.read(
				file,
				svConsumer,
				SOAConstants.SV_BDD_FILE_TYPE,
				region,
				spid);

			// Insert records in LAST_NPAC_RESPONSE_TIME table     
			svConsumer.setLastNotificationTime();

		} catch (NumberFormatException nex) {

			success = false;
			
			// we don't want to lose any valuable stack trace
			// info about where this exception came from
			Debug.logStackTrace(nex);

		} catch (FileNotFoundException fex) {

			success = false;

			// we don't want to lose any valuable stack trace
			// info about where this exception came from
			Debug.log(
				Debug.ALL_ERRORS,
				"SVFileReader:   "
					+ "Could not read file ["
					+ file
					+ "]: "
					+ fex);

		} finally {
			
			if( svConsumer != null)
			{
						
				svConsumer.cleanup(success);
				
			}		

		}

	}

	/**
	* This is the command-line interface for processing an SV BDD file and
	* updated the DB with its contents.
	*
	* @param args String[] the command line arguments. See the usage string for
	*                      description.
	*/
	public static void main(String[] args) {

		String file = null;

		String spid = null;

		String region = null;

		String dbname = null;

		String dbuser = null;

		String dbpass = null;

		for (int i = 0; i < args.length; i++) {

			if (args[i].equals("-verbose")) {

				Debug.enableAll();

			} else if (file == null) {

				file = args[i];

			} else if (spid == null) {

				spid = args[i];

			} else if (region == null) {

				region = args[i];

			} else if (dbname == null) {

				dbname = args[i];

			} else if (dbuser == null) {

				dbuser = args[i];

			} else if (dbpass == null) {

				dbpass = args[i];
			}

		}

		StringBuffer sb = new StringBuffer();

		sb.append("java ");

		sb.append(SVFileReader.class.getName());
		
		sb.append( " [-verbose] <file name> <spid> <region> ");
		
		sb.append( "<db name> <db user> <db password>");		

		String usage = sb.toString();

		// check to see if all command line parameters were given, and if not,
		// print usage string and exit
		if (file == null
			|| spid == null
			|| region == null
			|| dbname == null
			|| dbuser == null
			|| dbpass == null) {

			System.err.println(usage);

			System.exit(-1);

		}

		SVFileReader reader = new SVFileReader();

		try {

			// Intialize the database interface. This only needs to be done once
			// per java process.
			DBInterface.initialize(dbname, dbuser, dbpass);

			reader.read(file, spid, region);

		} catch (NumberFormatException nfex) {

			System.err.println(
				"[" + region + "] is not a valid numeric region value.");

		} catch (FileNotFoundException fnfex) {

			System.err.println("Could not read file [" + file + "]: " + fnfex);

		} catch (FrameworkException fex) {

			System.err.println(fex.getMessage());

		} catch (SQLException ex) {

			Debug.logStackTrace(ex);

		}

	}

}
