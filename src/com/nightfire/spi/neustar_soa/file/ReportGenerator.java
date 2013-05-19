/**
 * The purpose of this program to generate the Report which will have
 * failed record(s) i.e. record not inserted or updated in database
 *  for Bulk Data Download and SPID Mass Update  
 * 
 * @author Ravi M Sharma
 * @version 1.0
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
	1			Ravi M Sharma	06/12/2004			Created
	2			Ravi M Sharma	07/05/2004			Review comments incorporated.
	3			Ravi M Sharma 	07/15/2004			FI review comment 
													incorporated
	4			Ashok Kumar		07/20/2004			System testing comments
													incorporated
	5			D.subbarao		08/30/2005			Modified the scope of data members and a function formatData.												
 */

package com.nightfire.spi.neustar_soa.file;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.io.BufferedWriter;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.nightfire.spi.neustar_soa.utils.SOAConstants;
import com.nightfire.spi.neustar_soa.utils.SOAUtility;


import com.nightfire.framework.util.FrameworkException;
import com.nightfire.framework.util.StringUtils;

public class ReportGenerator  {
	
	/**
	 * This variable contains header , body and summary
	 */
	protected StringBuffer report = new StringBuffer();	
	
	/**
	 * This variable contains fileName
	 */
	protected String fileName = null;
	
	/**
	 * This variable contains fileType
	 */
	protected String fileType = null;
	
	/**
	 * This variable contains region ID
	 */
	protected String region = null;
	
	/**
	 * This variable contains spid
	 */
	protected String spid = null; 
	
	/**
	 * This variable used to contain new line 
	 */
	protected String newLine = null;
	
	/**
	 * This variable used to contain File object 
	 */
	protected File file = null;
	
	/**
	 * Constructs a Report Generator that will use the given fileName and 
	 * fileType.
	 *
	 * @param fileName String  contains fileName
	 * @param fileType String  contains fileType
	 * @param region   String  contains Region
	 * @param spid	   String  contains spid
	 * 
	 */
	public ReportGenerator( String fileName , String fileType , 
							String region ,String spid )
	{
		
		this.fileName = fileName;
		
		this.fileType = fileType;
		
		this.region = region ;
		
		this.spid = spid ;
		
		file = new File( fileName );
		
		try
		{
			
			newLine = System.getProperty("line.separator");
			
			
		}catch(SecurityException se)
		{
			
			newLine = "\n";
			
		}
		
	}
	
	/**
	 * This function cteates the report with log extension  
	 *
	 * @throws FrameworkException
	 */	
	public  void generateReport( ) throws FrameworkException
	{
		
		FileWriter fileWriter = null;
		
		BufferedWriter bw = null;
		try 
		{
			fileWriter = new FileWriter( new File( fileName + ".log" ) );
			
			bw = new BufferedWriter(fileWriter);
			
			bw.write(report.toString());
			
			bw.flush();
			
			bw.close();
			
		}catch (IOException e) {
			
			try
			{
			
				bw.close();
				
			}catch (IOException ex) 
			{
				throw new FrameworkException(ex.getMessage());			
			}
			
			throw new FrameworkException(e.getMessage());
			
		}
		
	}	
	
	
	/**
	 * This function used to add first header part to the report
	 *	 
	 */
	public void titleHeader( ) 
	{
		StringBuffer header = new StringBuffer();
			
		if(fileType.equalsIgnoreCase(SOAConstants.LRN_FILE_TYPE))
		{
			
			header.append("                       SIC-SMURF LRN Log");
			
			header.append( newLine );
			
			header.append("                       -----------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("SIC-SMURF LRN file	 :   " + file.getName() );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_FILE_TYPE))
		{
			
			header.append("                      SIC-SMURF NPANXX Log");
			
			header.append( newLine );
			
			header.append("                      --------------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("SIC-SMURF NPANXX file	:   " + file.getName() );
			
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_X_FILE_TYPE))
		{
			
			header.append("                      SIC-SMURF NPANXXX Log");
			
			header.append( newLine );
			
			header.append("                      ---------------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("SIC-SMURF NPANXXX file	:   " + file.getName() );
			
							
		}else if(fileType.equalsIgnoreCase(SOAConstants.SV_BDD_FILE_TYPE))
		{
			header.append("                      SV BDD Import Log");
			
			header.append( newLine );
			
			header.append("                      -----------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("SV BDD Data File 	   :   " + file.getName() );
			
			header.append( newLine );
			
			header.append("SPID             	   :   " + spid );
			
			header.append( newLine );
			
			header.append("Region ID        	   :   " + region );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.LRN_BDD_FILE_TYPE))
		{
			header.append("                      LRN BDD Import Log");
			
			header.append( newLine );
			
			header.append("                      ------------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("LRN BDD Data File 	   :   " + file.getName() );
			
			header.append( newLine );
			
			header.append("Region ID        	   :   " + region );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_BDD_FILE_TYPE))
		{
			
			header.append("                      NPANXX BDD Import Log");
			
			header.append( newLine );
			
			header.append("                      ---------------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("NPANXX BDD Data File	   :   " + file.getName() );
			
			header.append( newLine );
			
			header.append("Region ID        	   :   " + region );
			
		}else if(fileType.equalsIgnoreCase(
									SOAConstants.NPA_NXX_X_BDD_FILE_TYPE))
		{
			
			header.append("                      NPANXXX BDD Import Log");
			
			header.append( newLine );
			
			header.append("                      ----------------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("NPANXXX BDD Data File		:   " + file.getName() );
			
			header.append( newLine );
			
			header.append("Region ID        	     	:   " + region );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.NPB_BDD_FILE_TYPE))
		{
			
			header.append("                     NBRPoolBlock BDD Import Log");
			
			header.append( newLine );
			
			header.append("                     ---------------------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("NBRPoolBlock BDD Data File       :   " 
																+ file.getName() );
			header.append( newLine );
			
			header.append("Region ID                        :   " + region );
			
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.SPID_BDD_FILE_TYPE))
		{
			
			header.append("                      SPID BDD Import Log");
			
			header.append( newLine );
			
			header.append("                      -------------------");
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("SPID BDD Data File	:   " + file.getName() );
			
			header.append( newLine );
			
			header.append("Region ID        	:   " + region );
		}
		
					
		secondHeader( header );			
	
	}
	
	/**
	 * This function used to add second header part to the report
	 *
	 * @param  header StringBuffer
	 *
	 */
	private void secondHeader( StringBuffer header ) 
	{
		
		if(fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_X_FILE_TYPE))
		{
		
			header.append( newLine );
			
			header.append( newLine );
					
			header.append("List of NPANXXXs failed to Update: ");
			
			header.append( newLine );
			
			header.append("----------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "OLDSPID", 11, false, ' ' ));
			
			header.append(StringUtils.padString( "NEWSPID", 11, false, ' ' ));
			
			header.append(StringUtils.padString( "NPANXXX", 14, false, ' ' ));
			
			header.append("Reason");
			
			header.append( newLine );
			
			header.append("-------    -------    -------       ------");
			
			header.append( newLine );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.LRN_FILE_TYPE))
		{
			
			header.append( newLine );
			
			header.append( newLine );
		
			header.append("List of LRNs failed to Update: ");
			
			header.append( newLine );
			
			header.append("---------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "OLDSPID", 11, false, ' ' ));
			
			header.append(StringUtils.padString( "NEWSPID", 11, false, ' ' ));

			header.append(StringUtils.padString( "LRN", 14, false, ' ' ));

			header.append("Reason");
			
			header.append( newLine );
			
			header.append("-------    -------    ---           ------");
			
			header.append( newLine );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_FILE_TYPE))
		{
			
			header.append( newLine );
			
			header.append( newLine );

			header.append("List of NPANXXs failed to Update: ");
			
			header.append( newLine );
			
			header.append("---------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "OLDSPID", 11, false, ' ' ));
			
			header.append(StringUtils.padString( "NEWSPID", 11, false, ' ' ));

			header.append(StringUtils.padString( "NPANXX", 14, false, ' ' ));

			header.append("Reason");
			
			header.append( newLine );
			
			header.append("-------    -------    ------        ------");
			
			header.append( newLine );
	
		}else if(fileType.equalsIgnoreCase(SOAConstants.LRN_BDD_FILE_TYPE))
		{
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("List of LRNs failed to import: ");
			
			header.append( newLine );
			
			header.append("------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "SPID", 8, false, ' ' ));
			
			header.append(StringUtils.padString( "LRNID", 24, false, ' ' ));
			
			header.append(StringUtils.padString( "LRN", 14, false, ' ' ));

			header.append(StringUtils.padString( "Reason", 70, false, ' ' ));
			
			header.append("LRN Record");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "----", 8, false, ' ' ));
			
			header.append(StringUtils.padString( "-----", 24, false, ' ' ));
			
			header.append(StringUtils.padString( "---", 14, false, ' ' ));			

			header.append(StringUtils.padString( "------", 70, false, ' ' ));

			header.append("----------");
			
			header.append( newLine );

			
		}else if(fileType.equalsIgnoreCase(SOAConstants.SPID_BDD_FILE_TYPE))
		{
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("List of SPIDs failed to import: ");
			
			header.append( newLine );
			
			header.append("-------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "SPID", 8, false, ' ' ));
			
			header.append(StringUtils.padString( "NAME", 40, false, ' ' ));
			
			header.append("Reason");
						                         	
			header.append( newLine );
			
			header.append("----    ");
			
			header.append(StringUtils.padString( "----", 40, false, ' ' ));
			
			header.append("------");
			
			header.append( newLine );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_BDD_FILE_TYPE))
		{
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("List of NPANXXs failed to import: ");
			
			header.append( newLine );
			
			header.append("---------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "SPID", 8, false, ' ' ));
			
			header.append(StringUtils.padString( "NPANXXID", 24, false, ' ' ));

			header.append(StringUtils.padString( "NPA", 7, false, ' ' ));

			header.append(StringUtils.padString( "NXX", 7, false, ' ' ));
			
			header.append(StringUtils.padString( "Reason", 70, false, ' ' ));

			header.append("NPANXX Record");			
                         	
			header.append( newLine );
			
			header.append("----    --------                ---    ---    " );
			
			header.append(StringUtils.padString( "------", 70, false, ' ' )); 
			
			header.append("-------------");
			
			header.append( newLine );
			
		}else if(fileType.equalsIgnoreCase(
										SOAConstants.NPA_NXX_X_BDD_FILE_TYPE))
		{
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("List of NPANXXXs failed to import: ");
			
			header.append( newLine );			
			header.append("---------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "SPID", 8, false, ' ' ));
			
			header.append(StringUtils.padString( "NPANXXXID", 24, false, ' ' ));

			header.append(StringUtils.padString( "NPA", 7, false, ' ' ));

			header.append(StringUtils.padString( "NXX", 7, false, ' ' ));
			
			header.append(StringUtils.padString( "DASHX", 9, false, ' ' ));

			header.append(StringUtils.padString( "Reason", 70, false, ' ' ));

			header.append("NPANXXX Record");
			
			header.append( newLine );
			
			header.append("----    ---------               ---    ---    -----    ");
			
			header.append(StringUtils.padString( "------", 70, false, ' ' )); 
			
			header.append("--------------");
			
			header.append( newLine );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.NPB_BDD_FILE_TYPE))
		{
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("List of NBRPoolBlocks failed to import: ");
			
			header.append( newLine );
			
			header.append("---------------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "SPID", 8, false, ' ' ));
			
			header.append(StringUtils.padString( "NPBID", 24, false, ' ' ));

			header.append(StringUtils.padString( "NPA", 7, false, ' ' ));

			header.append(StringUtils.padString( "NXX", 7, false, ' ' ));

			header.append(StringUtils.padString( "DASHX", 9, false, ' ' ));

			header.append(StringUtils.padString( "Reason", 70, false, ' ' ));

			header.append("NBRPoolBlock Record");
			
			header.append( newLine );
			
			header.append("----    -----                   ---    ---    -----    ");
			
			header.append(StringUtils.padString( "------", 70, false, ' ' )); 
			
			header.append("-------------------");
			
			header.append( newLine );
			
		}else if(fileType.equalsIgnoreCase(SOAConstants.SV_BDD_FILE_TYPE))
		{
			
			header.append( newLine );
			
			header.append( newLine );
			
			header.append("List of Subscription Versions failed to import: ");
			
			header.append( newLine );
			
			header.append("----------------------------------------------");
			
			header.append( newLine );
			
			header.append(StringUtils.padString( "SVID", 24, false, ' ' ));

			header.append(StringUtils.padString( "PortingTN", 14, false, ' ' ));

			header.append(StringUtils.padString( "NNSP", 8, false, ' ' ));

			header.append(StringUtils.padString( "ONSP", 8, false, ' ' ));

			header.append(StringUtils.padString( "Status", 30, false, ' ' ));
			
			header.append(StringUtils.padString( "Reason", 80, false, ' ' ));

			header.append("SV Record");
			
			header.append( newLine );
			
			header.append("----                    ---------     ----    ----    " +
				"------                        ");
			header.append(StringUtils.padString( "------", 80, false, ' ' ));
			
			header.append("---------");
			
			header.append( newLine );
		
		}
		
		
		report.append(header);
	}
	
	
	/**
	 * This function used to add body part to the report
	 *
	 * @param  list List this list contains all failed records
	 *	 
	 */
	public void addBody ( List list ) 
	{
		
		StringBuffer content = new StringBuffer();
		
		String[] rowCol;		
				
		if(list.size() > 0) {
			
			for (Iterator iter = list.iterator(); iter.hasNext();) 
			{
				 rowCol = (String[])iter.next();
				
				if(fileType.equalsIgnoreCase(
											SOAConstants.LRN_BDD_FILE_TYPE))    
				{
					content.append(StringUtils.padString( 
											rowCol[0], 8, false, ' ' ));
					
					content.append(StringUtils.padString( 
											rowCol[1], 24 , false, ' ' ));										
					
					content.append(StringUtils.padString( 
											rowCol[2], 14 , false, ' ' ));										
						
					content.append(
						StringUtils.padString(rowCol[5], 70, false, ' ' ));
				
					content.append(rowCol[6]);
					
				}else if(fileType.equalsIgnoreCase(
										SOAConstants.SPID_BDD_FILE_TYPE))
				{
					
					content.append(StringUtils.padString( 
											rowCol[0], 8, false, ' ' ));
											
					content.append(StringUtils.padString( 
											rowCol[1], 40 , false, ' ' ));										
					
					content.append( rowCol[2] );										

				}else if(fileType.equalsIgnoreCase(
										SOAConstants.NPA_NXX_BDD_FILE_TYPE))
				{
					
					content.append(StringUtils.padString( 
											rowCol[0], 8, false, ' ' ));
											
					content.append(StringUtils.padString( 
											rowCol[1], 24, false, ' ' ));
							
					StringTokenizer npaNxxTokens 
									= new StringTokenizer(rowCol[2],"-");
									
					content.append(StringUtils.padString( 
								npaNxxTokens.nextToken(), 7, false, ' ' ));
								
					content.append(StringUtils.padString( 
								npaNxxTokens.nextToken(), 7, false, ' ' ));
											
					content.append(
						StringUtils.padString(rowCol[6], 70, false, ' ' ));

					content.append( rowCol[7] );
					
				}else if(fileType.equalsIgnoreCase(
									SOAConstants.NPA_NXX_X_BDD_FILE_TYPE))
				{
					
					content.append(StringUtils.padString( 
											rowCol[0], 8, false, ' ' ));
						
					content.append(StringUtils.padString( 
											rowCol[1], 24, false, ' ' ));

					StringTokenizer npaNxxTokens 
									= new StringTokenizer(rowCol[2],"-");
									
					content.append(StringUtils.padString( 
								npaNxxTokens.nextToken(), 7, false, ' ' ));
								
					content.append(StringUtils.padString( 
								npaNxxTokens.nextToken(), 7, false, ' ' ));
						
					content.append(StringUtils.padString( 
								npaNxxTokens.nextToken(), 9, false, ' ' ));
																							
					content.append(
						StringUtils.padString(rowCol[7], 70, false, ' ' ));

					content.append(rowCol[8]);
				
				}else if(fileType.equalsIgnoreCase(
										SOAConstants.NPB_BDD_FILE_TYPE))
				{
					
					content.append(StringUtils.padString( 
											rowCol[3], 8, false, ' ' ));
					
					content.append(StringUtils.padString( 
											rowCol[0], 24, false, ' ' ));

					content.append(StringUtils.padString( 
								rowCol[1].substring(0,3), 7, false, ' ' ));
		
					content.append(StringUtils.padString( 
								rowCol[1].substring(3 ,6), 7, false, ' ' ));

					content.append(StringUtils.padString( 
									rowCol[1].substring(6), 9, false, ' ' ));
																	
					content.append(
						StringUtils.padString(rowCol[16], 70, false, ' ' ));

					content.append(rowCol[17]);						
											
				}else if(fileType.equalsIgnoreCase(
											SOAConstants.SV_BDD_FILE_TYPE))
				{
					
					content.append(StringUtils.padString( 
											rowCol[0], 24, false, ' ' ));
						
					content.append(StringUtils.padString( 
											rowCol[1], 14, false, ' ' ));

					content.append(StringUtils.padString( 
											rowCol[3], 8, false, ' ' ));
											
					content.append(StringUtils.padString( 
											rowCol[21], 8, false, ' ' ));

					content.append( StringUtils.padString( 
						SOAUtility.getStatus( rowCol[20] ) , 30, false, ' ' ));												

					content.append(
						StringUtils.padString(rowCol[41], 80, false, ' ' ));

					content.append( rowCol[42] );
					
				}else 
				if(fileType.equalsIgnoreCase(SOAConstants.LRN_FILE_TYPE)  || 
				fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_FILE_TYPE) || 
				fileType.equalsIgnoreCase(SOAConstants.NPA_NXX_X_FILE_TYPE))
				{
					
					content.append(StringUtils.padString( 
										rowCol[0], 11, false, ' ' ));
										
					content.append(StringUtils.padString( 
										rowCol[1], 11, false, ' ' ));
				
					content.append(StringUtils.padString( 
										rowCol[2], 14, false, ' ' ));
				
					content.append(rowCol[3]);					
					
				}					
																				
				content.append( newLine );
				
			}
				
		} else {
			
			content.append("............................................");
			
							
		}
		
		
		report.append(content);
		
						
	}	
	
	
	/**
	 * This function used to add summary part to the report
	 *
	 * @param  stratTime Date time on which process started
	 * @param  endTime   Date time on which process ended
	 * @param  recRead   int 
	 * @param  recUpdate int
	 * @param  recFail   int
	 * @param  recordArr int[] this is used in case of mass spid update
	 *
	 */	
	public void summary( Date startTime , Date endTime , 
						 int recRead , int recUpdate , 
						 int recFail ,int [] recordArr )
	{
		
		StringBuffer summary = new StringBuffer();
				
		summary.append( newLine );
		
		summary.append( newLine );
		
		summary.append( newLine );
		
		summary.append("Summary Report");
		
		summary.append( newLine );
		
		summary.append("--------------");
		
		summary.append( newLine );
		
		summary.append( newLine );
		
		summary.append(StringUtils.padString( "Total records read" , 
													60, false, ' ' )+": ");
		
		summary.append(recRead);
		
		summary.append( newLine );
		
		if( fileType.equalsIgnoreCase( SOAConstants.LRN_FILE_TYPE ) )
		{
			
			summary.append(StringUtils.padString( "Total records skipped" , 
													60, false, ' ' )+": ");
			summary.append( recordArr[0] );
			
			summary.append( newLine );
			
			summary.append(StringUtils.padString( "Total records updated in [ " 
										+ SOAConstants.LRN_TABLE + " ] table" , 
										60, false, ' ' )+": ");
			summary.append( recUpdate );
			
			summary.append( newLine );
			
			summary.append(StringUtils.padString( "Total records updated in [ " 
										+ SOAConstants.SV_TABLE + " ] table" , 
										60, false, ' ' )+": ");
						
			summary.append( recordArr[1] );
			
			summary.append( newLine );
			
			summary.append(StringUtils.padString( "Total records updated in [ " 
								+ SOAConstants.NBRPOOL_BLOCK_TABLE + " ] table" , 
								60, false, ' ' )+": ");
			
			summary.append( recordArr[2] );
		
		}else if( fileType.equalsIgnoreCase( SOAConstants.NPA_NXX_FILE_TYPE) )
		{
			summary.append(StringUtils.padString( "Total records skipped" , 
													60, false, ' ' )+": ");
			
			summary.append( recordArr[0] );

			summary.append( newLine );
			
			summary.append(StringUtils.padString( "Total records updated in [ " 
									+ SOAConstants.NPANXX_TABLE + " ] table" , 
									60, false, ' ' )+": ");
			
			summary.append( recUpdate );
			
			summary.append( newLine );
			
			summary.append(StringUtils.padString( "Total records updated in [ " 
										+ SOAConstants.SV_TABLE + " ] table" , 
										60, false, ' ' )+": ");
			
			summary.append( recordArr[1] );			
			
		
		}else if( fileType.equalsIgnoreCase( SOAConstants.NPA_NXX_X_FILE_TYPE ) )
		{
			summary.append(StringUtils.padString( "Total records skipped" , 
													60, false, ' ' )+": ");
			
			summary.append( recordArr[0] );

			summary.append( newLine );
			
			summary.append(StringUtils.padString( "Total records updated in [ " 
									+ SOAConstants.NPANXXX_TABLE + " ] table" , 
									60, false, ' ' )+": ");
			
			summary.append( recUpdate );
			
			
		
		}else if( fileType.equalsIgnoreCase( SOAConstants.SV_BDD_FILE_TYPE ) )
		{
			summary.append(StringUtils.padString( "Total records skipped" , 
													60, false, ' ' )+": ");
			
			summary.append( recordArr[0] );

			summary.append( newLine );
			
			summary.append(StringUtils.padString( "Total records imported" , 
													60, false, ' ' )+": ");
			
			summary.append( recUpdate );		
			
		
		}else
		{
			
			summary.append(StringUtils.padString( "Total records imported" , 
													60, false, ' ' )+": ");
			
			summary.append( recUpdate );
			
		}
		
				
		summary.append( newLine );
		
		summary.append(StringUtils.padString( "Total records failed" , 
													60, false, ' ' )+": ");		
		
		summary.append( recFail );
		
		summary.append( newLine );
		
		summary.append( newLine );
		
		summary.append( newLine );		

		summary.append("Run began on :\t");
		
		summary.append(formatDate(startTime));
		
		summary.append( newLine );
		
		summary.append("Run ended on :\t");
		
		summary.append(formatDate(endTime));
		
		report.append(summary);
			
	}
	
	/**
	 * This function used to format the date
	 * 
	 * @return String
	 *
	 * @param  date Date time on which process started
	 * 
	 */	
	protected String formatDate ( Date date )
	{
						
		SimpleDateFormat dateTimeFormatter 
					= new SimpleDateFormat( SOAConstants.DATE_FORMAT );

		if ( date == null )
		{
				return null;
				
		} 
		else
		{
			
			return( dateTimeFormatter.format( date ) );
			
		}
	}
			
}