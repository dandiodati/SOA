/**
 * This class uses an NBRPOOL_BLOCK Token Consumer to update the
 * SOA_NBRPOOL_BLOCK table based on the contents of an NBRPOOL_BLOCK BDD file.
 *
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.file.DelimitedFileReader
 * @see com.nightfire.framework.db.DBInterface
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.spi.neustar_soa.file.NBRPoolBlockTokenConsumer
 */

/**
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			04/12/2004			Created
	2			Ashok			07/05/2004			Review comments incoporated
	3			Ashok			07/21/2004			SPID removed

 */

package com.nightfire.spi.neustar_soa.file;

import java.io.FileNotFoundException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.db.DatabaseException;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NBRPoolBlockFileReader extends DelimitedFileReader {

	/**
	* This reads the named file and updated the SOA_NBRPOOL_BLOCK table
	* based on its contents.
	*
	* @param file String an NBRPOOL_BLOCK BDD file
	* value will be processed.
	* @param region String all NBRPOOL_BLOCK will be assigned to this region.
	* @param customerID String this is the clearinghouse customer ID (CID)
   *                          that will be allowed to view these number pool
   *                          block records. Each customer will have their
   *                          own copy, or view, of the records.
	* @throws FileNotFoundException the named file was not found or was
	*                               inaccessable.
	* @throws FrameworkException thrown if some other error occurs during
	*                            processing. e.g. a database error or
	*                            bad formatting in the input file.
	*/
	public void read(String file, String region, String customerID)
		throws FileNotFoundException, FrameworkException {

		NBRPoolBlockTokenConsumer consumer = null;

		boolean success = true;

		try {

			consumer = new NBRPoolBlockTokenConsumer(region, customerID);

			consumer.init(file);

			super.read(
				file,
				consumer,
				SOAConstants.NPB_BDD_FILE_TYPE,
				region,
				null);

		} catch (FileNotFoundException fex) {

			success = false;

			// we don't want to lose any valuable stack trace
			// info about where this exception came from
			Debug.log(
				Debug.ALL_ERRORS,
				"NBRPoolBlockFileReader:   "
					+ "Could not read file ["
					+ file
					+ "]: "
					+ fex);

		} catch (NumberFormatException nfe) {

			success = false;

			// we don't want to lose any valuable stack trace
			// info about where this exception came from
			Debug.logStackTrace( nfe );

		} finally {

			if( consumer != null)
			{

				consumer.cleanup(success);

			}


		}

	}

	/**
	* This is the command-line interface for processing an NBRPOOL_BLOCK BDD
	* file and updated the DB with its contents.
	*
	* @param args String[] the command line arguments. See the usage string for
	*                      description.
	*/
	public static void main(String[] args) {

		String file = null;

		String region = null;

		String dbname = null;

		String dbuser = null;

		String dbpass = null;

      String customerID = null;

		for (int i = 0; i < args.length; i++) {

			if (args[i].equals("-verbose")) {
				Debug.enableAll();
			} else if (file == null) {
				file = args[i];
			} else if (region == null) {
				region = args[i];
			} else if (dbname == null) {
				dbname = args[i];
			} else if (dbuser == null) {
				dbuser = args[i];
			} else if (dbpass == null) {
				dbpass = args[i];
			} else if (customerID == null) {
				customerID = args[i];
			}

		}

		StringBuffer sb = new StringBuffer();

		sb.append("java ");

		sb.append(NBRPoolBlockFileReader.class.getName());

		sb.append( " [-verbose] <file name> <region> ");

		sb.append( "<db name> <db user> <db password> <customer ID>");

		String usage = sb.toString();

		// check to see if all command line parameters were given, and if not,
		// print usage string and exit
		if (file == null
			|| region == null
			|| dbname == null
			|| dbuser == null
			|| dbpass == null
         || customerID == null) {

			System.err.println(usage);
			Debug.log(
				Debug.ALL_ERRORS,
				"NBRPoolBlockFileReader: USAGE:  " + usage);

			System.exit(-1);

		}

		NBRPoolBlockFileReader reader = new NBRPoolBlockFileReader();

		try {

			// Intialize the database interface. This only needs to be done once
			// per java process.
			DBInterface.initialize(dbname, dbuser, dbpass);

			reader.read(file, region, customerID);

		} catch(DatabaseException de){

			Debug.error("Could not initialize database : "+de);

		} catch (NumberFormatException nfex) {

			System.err.println(
				"[" + region + "] is not a valid numeric region value.");
			Debug.log(
				Debug.ALL_ERRORS,
				"NBRPoolBlockFileReader:   "
					+ "["
					+ region
					+ "] is not a valid numeric region value.");

		} catch (FileNotFoundException fnfex) {

			System.err.println("Could not read file [" + file + "]: " + fnfex);
			Debug.log(
				Debug.ALL_ERRORS,
				"NBRPoolBlockFileReader:   "
					+ "Could not read file ["
					+ file
					+ "]: "
					+ fnfex);

		} catch (FrameworkException fex) {

			System.err.println(fex.getMessage());

			Debug.log(
				Debug.ALL_ERRORS,
				"NBRPoolBlockFileReader:   " + fex.getMessage());

		} catch (Exception ex) {

			ex.printStackTrace();

		}

	}

}
