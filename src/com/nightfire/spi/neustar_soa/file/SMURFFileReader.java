/**
 * This class uses diffrent Token Consumer based on file Type for
 * Mass SPID update i.e. if file type is LRN then this class will
 * use LRNSMURFTokenConsumer to update SV,LRN and NBR tables.
 *
 * @author 		Ashok Kumar
 * @version		1.0
 * @Copyright (c) 2003-04 NeuStar, Inc. All rights reserved. The source code
 * provided herein is the exclusive property of NeuStar, Inc. and is
 * considered to be confidential and proprietary to NeuStar.
 *
 * @see			com.nightfire.spi.neustar_soa.file.DelimitedFileReader
 * @see			com.nightfire.spi.neustar_soa.file.SMURFTokenConsumer
 * @see			com.nightfire.spi.neustar_soa.file.LRNSMURFTokenConsumer
 * @see			com.nightfire.spi.neustar_soa.file.NPANXXSMURFTokenConsumer
 * @see			com.nightfire.spi.neustar_soa.file.NPANXXXSMURFTokenConsumer
 * @see			com.nightfire.spi.neustar_soa.utils.SOAConstants
 */

/**
	History
	---------------------
	Rev#		Modified By 	Date				Reason
	-----       -----------     ----------			--------------------------
	1			Ashok			05/04/2004			Created
	2			Ashok 			05/11/2004			Empty constructor using
													for each token consumer

 */

package com.nightfire.spi.neustar_soa.file;

import java.io.FileNotFoundException;
import com.nightfire.framework.db.DBInterface;
import com.nightfire.framework.util.Debug;
import com.nightfire.framework.util.FrameworkException;
import com.nightfire.spi.neustar_soa.utils.SOAConstants;

public class SMURFFileReader extends DelimitedFileReader {

	/**
	* This reads the named file and call the token consumer based on file type.
	* Update the table(s) based on file's content
	*
	* @param file String
	* @param fileType String i.e. lrn,npa_nxx,npa_nxx_x
	* @throws FileNotFoundException the named file was not found or was
	*                               inaccessable.
	* @throws FrameworkException thrown if some other error occurs during
	*                            processing. e.g. a database error or
	*                            bad formatting in the input file.
	*/
	public void read(String file, String fileType)
		throws FileNotFoundException, FrameworkException {

		SMURFTokenConsumer consumer = null;

		boolean success = true;

		try {

			if (fileType.equalsIgnoreCase(SOAConstants.LRN_FILE_TYPE)) {

				//if file type SIC-SMURF-LRN
				consumer = new LRNSMURFTokenConsumer();

			} else if (
				fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_FILE_TYPE)) {

				//if file type SIC-SMURF-NPANXX
				consumer = new NPANXXSMURFTokenConsumer();

			} else if (
				fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_X_FILE_TYPE)) {

				//if file type SIC-SMURF-NPANXXX

				consumer = new NPANXXXSMURFTokenConsumer();

			} else {

				// if file type except lrn,npa_nxx,npa_nxx_x
				throw new FrameworkException(
					"[" + fileType + "]is not a valid File Type.");

			}

			//Initialize DB
			consumer.init(file, fileType);

			super.read(file, consumer, fileType, null, null);

		} catch (FileNotFoundException fe) {

			success = false;

			Debug.logStackTrace(fe);

		} catch (FrameworkException fex) {

			success = false;

			Debug.log(
				Debug.ALL_ERRORS,
				"SUMRFFileReader:   " + fex.getMessage());

		} finally {
			if(consumer != null)	
				consumer.cleanup(success);

		}

	}

	/**
	* This is the command-line interface for processing SMURF file and
	* updated the DB with its contents.
	*
	* @param args String[] the command line arguments. See the usage string for
	*                      description.
	*/
	public static void main(String[] args) {

		String file = null;

		String fileType = null;

		String dbname = null;

		String dbuser = null;

		String dbpass = null;

		for (int i = 0; i < args.length; i++) {

			if (args[i].equals("-verbose")) {

				Debug.enableAll();

			} else if (fileType == null) {

				fileType = args[i];

			} else if (file == null) {

				file = args[i];

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

		sb.append(SMURFFileReader.class.getName());

		sb.append(" [-verbose] < file type {SIC-SMURF-LRN | ");

		sb.append("SIC-SMURF-NPANXX | SIC-SMURF-NPANXXX}> <file name> ");

		sb.append("<db name> <db user> <db password>");

		String usage = sb.toString();


		// check to see if all command line parameters were given, and if not,
		// print usage string and exit
		if (fileType == null
			|| file == null
			|| dbname == null
			|| dbuser == null
			|| dbpass == null) {

			System.err.println(usage);

			System.exit(-1);

		}

		SMURFFileReader reader = new SMURFFileReader();

		try {

			// Intialize the database interface. This only needs to be done once
			// per java process.
			DBInterface.initialize(dbname, dbuser, dbpass);

			reader.read(file, fileType);

		} catch (FileNotFoundException fnfex) {

			System.err.println("Could not read file [" + file + "]: " + fnfex);

		} catch (FrameworkException fex) {

			System.err.println(fex.getMessage());

		} catch (Exception ex) {

			ex.printStackTrace();

		}

	}

} //end of class SMURFFileReader
