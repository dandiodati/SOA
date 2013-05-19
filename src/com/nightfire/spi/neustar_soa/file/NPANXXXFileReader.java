/**
 * This class uses an NPANXX_X Token Consumer to update the SOA_NPANXX_X
 * table based on the contents of an NPANXX_X BDD file.
 * 
 * @author Ashok Kumar
 * @version 1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is considered
 * to be confidential and proprietary to NeuStar.
 * @see com.nightfire.spi.neustar_soa.file.DelimitedFileReader 
 * @see com.nightfire.framework.db.DBInterface
 * @see com.nightfire.framework.util.Debug
 * @see com.nightfire.spi.neustar_soa.file.NPANXXXTokenConsumer
 */

/** 
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			04/12/2004			Created
	2			Ashok			07/05/2004			Review comments incoporated
  
 */

package com.nightfire.spi.neustar_soa.file;

import java.io.FileNotFoundException;

import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class NPANXXXFileReader extends DelimitedFileReader {

	/**
	* This reads the named file and updates the SOA_NPANXX_X table based on its
	* contents.
	*
	* @param file String an NPANXX_X BDD file
	* @param region String all NPANXX_Xs will be assigned to this region.
	* @throws FileNotFoundException the named file was not found or was
	*                               inaccessable.
	* @throws FrameworkException thrown if some other error occurs during
	*                            processing. e.g. a database error or
	*                            bad formatting in the input file.
	*/
	public void read(String file, String region)
		throws FileNotFoundException, FrameworkException {
		boolean success = true;

		NPANXXXTokenConsumer npaNxxxConsumer = null;

		try {

			npaNxxxConsumer = new NPANXXXTokenConsumer(region);

			npaNxxxConsumer.init(file);

			super.read(
				file,
				npaNxxxConsumer,
				SOAConstants.NPA_NXX_X_BDD_FILE_TYPE,
				region,
				null);

		} catch (NumberFormatException fex) {

			success = false;

			// we don't want to lose any valuable stack trace
			// info about where this exception came from
			Debug.logStackTrace(fex);

		} catch (FileNotFoundException fnfex) {

			success = false;

			Debug.log(
				Debug.ALL_ERRORS,
				"NPANXXXFileReader:   "
					+ "Could not read file ["
					+ file
					+ "]: "
					+ fnfex);

		} catch (FrameworkException fex) {

			success = false;

			Debug.log(
				Debug.ALL_ERRORS,
				"NPANXXXFileReader:   " + fex.getMessage());

		} finally {
			
			
			if( npaNxxxConsumer != null)
			{

				npaNxxxConsumer.cleanup(success);

			}
			

		}

	}

	/**
	* This is the command-line interface for processing an NPANXX_X BDD file and
	* updates the DB with its contents.
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
			}

		}

		StringBuffer sb = new StringBuffer();
		
		sb.append("java ");
		
		sb.append(NPANXXXFileReader.class.getName());
		
		sb.append( " [-verbose] <file name> <region> ");
		
		sb.append( "<db name> <db user> <db password>");

		String usage = sb.toString();
		
		// check to see if all command line parameters were given, and if not,
		// print usage string and exit
		if (file == null
			|| region == null
			|| dbname == null
			|| dbuser == null
			|| dbpass == null) {

			System.err.println(usage);

			System.exit(-1);

		}

		NPANXXXFileReader reader = new NPANXXXFileReader();

		try {

			// Intialize the database interface. This only needs to be done once
			// per java process.
			DBInterface.initialize(dbname, dbuser, dbpass);

			reader.read(file, region);

		} catch (NumberFormatException nfex) {

			System.err.println(
				"[" + region + "] is not a valid numeric region value.");

		} catch (FileNotFoundException fnfex) {

			System.err.println("Could not read file [" + file + "]: " + fnfex);

		} catch (FrameworkException fex) {

			System.err.println(fex.getMessage());

		} catch (Exception ex) {

			ex.printStackTrace();

		}

	}

}
