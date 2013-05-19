/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */
package com.nightfire.framework.util;


import java.io.*;
import java.util.*;


/**
 * Class for various file utilities.
 */
public class FileUtils implements Comparator
{
    /**
     * The default character encoding used to read and write text files.
     */
    public static final String DEFAULT_TEXT_ENCODING = "UTF8";

    /**
     * Class encapsulating file statistics.
     */
    public static class FileStatistics
    {
        /**
         * 'true' if file exists, otherwise 'false'.
         */
        public final boolean exists;

        /**
         * 'true' if file can be read, otherwise 'false'.
         */
        public final boolean readable;

        /**
         * 'true' if file can be written to, otherwise 'false'.
         */
        public final boolean writeable;

        /**
         * 'true' if named item is a file, otherwise 'false'.
         */
        public final boolean isFile;

        /**
         * 'true' if named item is a directory, otherwise 'false'.
         */
        public final boolean isDirectory;

        /**
         * The number of bytes in the file.
         */
        public final long    length;

        /**
         * A long value representing the time the file was last modified, measured in milliseconds since 
         * the epoch (00:00:00 GMT, January 1, 1970), or 0L if the file does not exist or if an I/O error occurs
         */
        public final long    lastModified;


        /**
         * Constructor - Arguments are typically populated via calls against File object.
         */
        public FileStatistics ( boolean exists, boolean readable, boolean writeable, boolean isFile,
                                boolean isDirectory, long length, long lastModified )
        {
            this.exists       = exists;
            this.readable     = readable;
            this.writeable    = writeable;
            this.isFile       = isFile;
            this.isDirectory  = isDirectory;
            this.length       = length;
            this.lastModified = lastModified;
        }
    }

    /**
     * This method returns the file-name/Directory, given the properties and 
     * key corresponding to base-path property.
     * If the base-path is not set in the properties then the original Filename is returned.
     * 
     * @param props
     * @param basePathKey
     * @param oldLogFileName
     * @return location at the 
     */
    public static String prependBaseLogPath(Properties props,String basePathKey, String oldLogFileName){
  	  String logFile = oldLogFileName;
  	  String logLocation = props.getProperty(basePathKey);
  	  //if the base-path property is present in the properties then 
  	  if(StringUtils.hasValue(logLocation)){
        	// First check if the logFile value is just the File-Name/Directory or not.
    		// If it contains a location, then extract the File-name/Directory only.
  		  	if(logFile.indexOf("\\")>=0){
  	    		int fileSepratorIndex = logFile.lastIndexOf("\\");
  	    		logFile = logFile.substring(fileSepratorIndex + 1, logFile.length());
  	    	}
    		else if(logFile.indexOf("/")>=0){
  	    		int fileSepratorIndex = logFile.lastIndexOf("/");
  	    		logFile = logFile.substring(fileSepratorIndex + 1, logFile.length());
  	    	}
    		//prepend the base-path to the filename.
        	logFile = logLocation+ System.getProperty("file.separator")+ logFile;	
  	  }
  	  
  	  return logFile;
    }

    /**
     * Configure file utilities dealing with reading and writing text-based
     * files to use UTF-8, or the encoding provided by java.io Readers and Writers.
     *
     * @param  useUTF8  Flag indicating whether UTF8 should be used or not.
     *
     * @return  Previous value of flag.
     */
    public static boolean useUTF8Encoding ( boolean useUTF8 )
    {
        boolean old = useUTF8Flag;

        useUTF8Flag = useUTF8;

        Debug.log( Debug.IO_STATUS, "Using UTF8 for text-based file reads/writes? [" + useUTF8Flag 
                   + "].  Previous value [" + old + "]." );

        return old;
    }


    /**
     * Utility to get statistics on named file.
     *
     * @param  fileName  Name of file to get information about.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static FileStatistics getFileStatistics ( String fileName ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Getting statistics on file [" + fileName + "] ..." );
        
        try
        {
            File f = new File( fileName );

            FileStatistics fs = new FileStatistics( f.exists(), f.canRead(), f.canWrite(), f.isFile(), 
                                                    f.isDirectory(), f.length(), f.lastModified() );
            return fs;
        }
        catch ( SecurityException se )
        {
            throw new FrameworkException( "ERROR: Could not get statistics on file [" + 
                                          fileName + "]:\n\t" + se.toString() );
        }
    }


    /**
     * Utility to get file signature uniquely indentifying it.
     *
     * @param  fileName  Name of file to get signature on.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur or it is a directory.
     */
    public static String getFileSignature ( String fileName ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Getting signature on file [" + fileName + "] ..." );
        
        try
        {
            File f = new File( fileName );
			
			if (!f.exists() || !f.isFile())
				throw new FrameworkException( "ERROR: File [" + fileName + "] does not exist.");
				
			if (!f.canRead())
				throw new FrameworkException( "ERROR: Cannot read file [" + fileName + "].");
				
			if (f.isDirectory())
				throw new FrameworkException( "ERROR: File [" + fileName + "] is invalid. It is directory.");
				
			return f.getName() + "-" + String.valueOf(f.length()) + "-" + String.valueOf(f.lastModified());
        }
        catch ( SecurityException se )
        {
            throw new FrameworkException( "ERROR: Could not get signature of the file [" + 
                                          fileName + "]:\n\t" + se.toString() );
        }
    }

    
    /**
     * Utility to check wether file exist or not
     *
     * @param  file Path for existence checking
     * 
     * @return  true for exists and false for any other case
     * 
     */
    // new function isFileExists(...) introduced from NFI 4.3 release. 
    public static boolean isFileExists( String filePath )
    {
        Debug.log( null, Debug.IO_STATUS, "Checking existence for file [" + filePath + "] ..." );
        
        boolean isFileExists = false;
        try
        {
            File f = new File( filePath );
			
			if (f.exists())
				isFileExists = true;
	
        }
        catch ( Exception se )
        {
        	Debug.log( null, Debug.IO_STATUS,"WARN: Could not check existence of file [" +filePath + "]:\n\t" + se.toString() );
        }
        return isFileExists;
    }
    

    /**
     * Utility to check for files in a given directory.
     *
     * @param  sourceDir  Name of source directory to check for files.
     *
     * @return  Array of strings containing names of available files.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static String[] getAvailableFileNames ( String sourceDir ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Checking source directory [" + sourceDir + "] for files ..." );
        
        try
        {
            // Check that source directory is a proper directory.
            File srcDir = new File( sourceDir );
            
            if ( srcDir.exists() && srcDir.isDirectory() )
            {
                String[] fileList = srcDir.list( );

                if ( fileList == null )
                {
                    throw new FrameworkException( "ERROR: Could not get directory file list due to system I/O error." );
                }
                
                // Sort filenames alphabetically (according to String.compartTo() semantics).
                // NOTE:  File names that are longer will sort later in the order.
                Arrays.sort( fileList, new FileUtils() );

                if ( fileList.length == 0 )
                {
                    Debug.log( null, Debug.IO_STATUS, "Source directory is empty." );
                }
                else
                {
                    StringBuffer sb = new StringBuffer( );
                    
                    for ( int Ix = 0;  Ix < fileList.length;  Ix ++ )
                    {
                        sb.append( fileList[Ix] );
                        sb.append( ' ' );
                    }
                    
                    Debug.log( null, Debug.IO_STATUS, "Available files: [" + sb.toString() + "]" );
                }
                
                return fileList;
            }
            else
            {
                throw new FrameworkException( "ERROR: Source directory [" + sourceDir + "] doesn't exist." );
            }
        }
        catch ( SecurityException se )
        {
            throw new FrameworkException( "ERROR: Could not read source directory [" + 
                                          sourceDir + "]:\n\t" + se.toString() );
        }
    }

    /**
     * Utility to check for files in a given directory.
     *
     * @param  sourceDir  Name of source directory to check for files.
     *
     * @return  Array of strings containing names of available files.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static File[] getAvailableFileList ( String sourceDir ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Checking source directory [" + sourceDir + "] for files ..." );

        try
        {
            // Check that source directory is a proper directory.
            File srcDir = new File( sourceDir );

            if ( srcDir.exists() && srcDir.isDirectory() )
            {
                File[] files = srcDir.listFiles();

                if ( files == null )
                {
                    throw new FrameworkException( "ERROR: Could not get directory file list due to system I/O error." );
                }

                if ( files.length == 0 )
                {
                    Debug.log( null, Debug.IO_STATUS, "Source directory is empty." );
                }
                else
                {
                    Debug.log( null, Debug.IO_STATUS, "Available files: [" + files.length + "]" );
                }

                return files;
            }
            else
            {
                throw new FrameworkException( "ERROR: Source directory [" + sourceDir + "] doesn't exist." );
            }
        }
        catch ( SecurityException se )
        {
            throw new FrameworkException( "ERROR: Could not read source directory [" +
                                          sourceDir + "]:\n\t" + se.toString() );
        }
    }


    /**
     * Utility to read and return a file's contents, given its name.
     *
     * @param  fileName  Name of file.
     *
     * @return  String containing named-file's contents.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static String readFile ( String fileName ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Reading data from file [" + fileName + "] ..." );

        BufferedReader br = null;

        try
        {
            StringBuffer sb = new StringBuffer( );

            char[] inArray = new char [ 1024 ];

            if ( useUTF8Flag )
                br = new BufferedReader( new InputStreamReader( new FileInputStream( fileName ), 
                                                               DEFAULT_TEXT_ENCODING ) );
            else
                br = new BufferedReader( new FileReader( fileName ) );

            // While file contents can be read, place what's read in buffer.
            do
            {
                int numRead = br.read( inArray, 0, inArray.length );

                // If EOF, we're done.
                if ( numRead == -1 )
                    break;

                // Append what we just got to end of buffer.
                sb.append( inArray, 0, numRead );
            }
            while ( true );

            // Return buffer as string.
            return( sb.toString() );
        }
        catch ( IOException ioe )
        {
            throw new FrameworkException( "ERROR: Could not read file [" + 
                                          fileName + "]:\n\t" + ioe.toString() );
        }
        finally   // Make sure we always close the stream.
        {
            if ( br != null )
            {
                try
                {
                    Debug.log( null, Debug.IO_STATUS, "Closing file." );

                    br.close( );
                }
                catch ( IOException ioe )
                {
                    Debug.log( null, Debug.ALL_ERRORS, "ERROR: Could not close file [" + 
                               fileName + "]:\n\t" + ioe.toString() );
                }
            }
        }
    }


    /**
     * Utility to read and return a binary file's contents, given its name.
     *
     * @param  fileName  Name of file.
     *
     * @return  Byte array containing named-file's contents.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static byte[] readBinaryFile ( String fileName ) throws FrameworkException
    {
        Debug.log( Debug.IO_STATUS, "Reading binary data from file [" + fileName + "] ..." );

        FileInputStream fis = null;

        try
        {
            // Get file statistics (giving length) so that we can create a proper
            // sized array.
            File f = new File( fileName );

            long length = f.length( );

            if ( length <= 0 )
            {
                Debug.log( Debug.ALL_WARNINGS, "WARNING: File [" + fileName + "] is empty." );

                return( new byte[ 0 ] );
            }

            byte[] inArray = new byte[ (int)(length) ];

            fis = new FileInputStream( f );

            int numRead = fis.read( inArray );

            // check that lengths match.
            if ( numRead != length )
            {
                Debug.log( Debug.ALL_WARNINGS, "WARNING: File [" + fileName + "], expecting to read [" 
                           + length + "] bytes, got [" + numRead + "]." );
            }

            // Return byte array.
            return( inArray );
        }
        catch ( IOException ioe )
        {
            throw new FrameworkException( "ERROR: Could not read file [" + 
                                          fileName + "]:\n\t" + ioe.toString() );
        }
        finally   // Make sure we always close the stream.
        {
            if ( fis != null )
            {
                try
                {
                    Debug.log( Debug.IO_STATUS, "Closing file." );

                    fis.close( );
                }
                catch ( IOException ioe )
                {
                    Debug.log( Debug.ALL_ERRORS, "ERROR: Could not close file [" + 
                               fileName + "]:\n\t" + ioe.toString() );
                }
            }
        }
    }


    /**
     * Utility to write data to the end of an open file.
     *
     * @param  fileName  Name of file.
     * @param  data      Data to write to file.
     * @param   fw       File stream
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void appendToFile ( String fileName, String data, FileWriter fw ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Appending data to file [" + fileName + "] ..." );

        try
        {
            fw.write( data );

            fw.flush();
        }
        catch ( IOException ioe )
        {
            throw new FrameworkException( "ERROR: Could not append to file [" + 
                                          fileName + "]\n\t" + ioe.toString() );
        }
    }


    /**
     * Utility to write data to a named file.
     *
     * @param  fileName  Name of file.
     * @param  data      Data to write to file.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void writeFile ( String fileName, String data ) throws FrameworkException
    {
        writeFile( fileName, data, false );
    }


    /**
     * Utility to append data to a named file.
     *
     * @param  fileName  Name of file.
     * @param  data      Data to append to end of file.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void appendFile ( String fileName, String data ) throws FrameworkException
    {
        writeFile( fileName, data, true );
    }


    /**
     * Utility to write data to a named file.
     *
     * @param  fileName  Name of file.
     * @param  data      Data to write to file.
     * @param  append    If 'true' data will be appended to file rather than the beginning.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void writeFile ( String fileName, String data, boolean append ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Writing data to file [" + fileName + "] ..." );

        BufferedWriter bw = null;

        try
        {
            if ( useUTF8Flag )
                bw = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( fileName, append ), 
                                                                 DEFAULT_TEXT_ENCODING ) );
            else
                bw = new BufferedWriter( new FileWriter( fileName, append ) );

            bw.write( data );
        }
        catch ( IOException ioe )
        {
            throw new FrameworkException( "ERROR: Could not write to file [" + 
                                          fileName + "]\n\t" + ioe.toString() );
        }
        finally   // Make sure we always close the stream.
        {
            if ( bw != null )
            {
                try
                {
                    Debug.log( null, Debug.IO_STATUS, "Closing file." );

                    bw.close( );
                }
                catch ( IOException ioe )
                {
                    Debug.log( null, Debug.ALL_ERRORS, "ERROR: Could not close file [" + 
                               fileName + "]:\n\t" + ioe.toString() );
                }
            }
        }
    }


    /**
     * Utility to write binary data to a named file.
     *
     * @param  fileName  Name of file.
     * @param  data      Binary data to write to file.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void writeBinaryFile ( String fileName, byte[] data ) throws FrameworkException
    {
        writeBinaryFile( fileName, data, false );
    }


    /**
     * Utility to append binary data to a named file.
     *
     * @param  fileName  Name of file.
     * @param  data      Binary data to append to end of file.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void appendBinaryFile ( String fileName, byte[] data ) throws FrameworkException
    {
        writeBinaryFile( fileName, data, true );
    }


    /**
     * Utility to write binary data to a named file.
     *
     * @param  fileName  Name of file.
     * @param  data      Binary data to write to file.
     * @param  append    If 'true' data will be appended to file rather than the beginning.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void writeBinaryFile ( String fileName, byte[] data, boolean append ) throws FrameworkException
    {
        Debug.log( Debug.IO_STATUS, "Writing binary data to file [" + fileName + "] ..." );

        FileOutputStream fos = null;

        try
        {
            fos = new FileOutputStream( fileName, append );

            fos.write( data );
        }
        catch ( IOException ioe )
        {
            throw new FrameworkException( "ERROR: Could not write to file [" + 
                                          fileName + "]\n\t" + ioe.toString() );
        }
        finally   // Make sure we always close the stream.
        {
            if ( fos != null )
            {
                try
                {
                    Debug.log( Debug.IO_STATUS, "Closing file." );

                    fos.close( );
                }
                catch ( IOException ioe )
                {
                    Debug.log( Debug.ALL_ERRORS, "ERROR: Could not close file [" + 
                               fileName + "]:\n\t" + ioe.toString() );
                }
            }
        }
    }


    /**
     * Utility to remove named file.
     *
     * @param  fileName  Name of file to delete.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void removeFile ( String fileName ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Removing file [" + fileName + "] ..." );
        
        try
        {
            File f = new File( fileName );
            
            // Only delete files, not directories.
            if ( f.exists() && f.isFile() )
            {
                boolean retcode = f.delete( );
                
                if ( retcode == false )
                {
                    throw new FrameworkException( "ERROR: Could not delete file [" + 
                                                  fileName + "]." );
                }
            }
            else
            {
                throw new FrameworkException( "ERROR: Could not find file [" + 
                                              fileName + "] to delete." );
            }

            Debug.log( null, Debug.IO_STATUS, "File successfully removed." );
        }
        catch ( SecurityException se )
        {
            throw new FrameworkException( "ERROR: Could not delete file [" + 
                                          fileName + "]:\n\t" + se.toString() );
        }
    }


    /**
     * Utility to move a named file from one directory to another.
     *
     * @param  targetDir   Name of target directory to move file to.
     * @param  sourceFile  Name of source file to move.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static void moveFile ( String targetDir, String sourceFile ) throws FrameworkException
    {
        Debug.log( null, Debug.IO_STATUS, "Moving source file [" + 
                   sourceFile + "] to directory [" + targetDir + "] ..." );

        try
        {
            // Check that source file exists
            File srcFile = new File( sourceFile );
            
            if ( !srcFile.exists() || !srcFile.isFile() )
            {
                throw new FrameworkException( "ERROR: Can't find source file [" + 
                                              sourceFile + "] to move." );
            }
            
            // Check that target directory is a proper directory.
            File tgtDir = new File( targetDir );
            
            if ( !tgtDir.exists() || !tgtDir.isDirectory() )
            {
                throw new FrameworkException( "ERROR: Target directory [" + 
                                              targetDir + "] doesn't exist." );
            }

            String fileName = srcFile.getName( );
            
            // Remove any directory prefix from file name.
            int index = fileName.lastIndexOf( "/" );

            if ( index != -1 )
            {
                fileName = fileName.substring( index + 1 );
            }
            else
            {
                index = fileName.lastIndexOf( "\\" );
                
                if ( index != -1 )
                {
                    fileName = fileName.substring( index + 1 );
                }
            }

            Debug.log( null, Debug.IO_STATUS, "Source file name [" + fileName + "]" );

            boolean retcode = srcFile.renameTo( new File( targetDir, fileName ) );

            if ( retcode == false )
            {
                throw new FrameworkException( "ERROR: Could not move file [" + sourceFile + 
                                              "] to [" + targetDir + "]." );
            }
        }
        catch ( SecurityException se )
        {
            throw new FrameworkException( "ERROR: Could not move file [" + sourceFile + 
                                          "] to [" + targetDir + "]:\n\t" + se.toString() );
        }
    }
    

    /**
     * Utility to load properties into a Properties object from a named file.
     *
     * @param  props  Properties object to add the file's properties to, or
     *                null if a new Properties object should be loaded.
     * @param  fileName  Name of file containing the properties.
     *
     * @exception  FrameworkException  Thrown if any file I/O errors occur.
     */
    public static Properties loadProperties ( Properties props, String fileName ) 
        throws FrameworkException
    {
        // Add the properties from the file to the properties object passed-in, if available
        Properties fileProps = props;
        
        // No properties object was given, so create a new one.
        if ( fileProps == null )
            fileProps = new Properties( );
        
        FileInputStream fis = null;
        
        try
        {
            fis = new FileInputStream( fileName );
            
            fileProps.load( fis );
            
            return fileProps;
        }
        catch ( Exception e )
        {
            throw new FrameworkException( "ERROR: Could not load properties from file [" 
                                          + fileName + "]:\n" + e.toString() );
        }
        finally
        {
            try
            {
                if ( fis != null )
                    fis.close( );
            }
            catch ( Exception e )
            {
                Debug.warning( e.toString() );
            }
        }
    }
    
    
    /**
    * Changes the file separators ("/" or "\") to the
    * platform-specific file separator.
    * 
    * @param    fileName -- Name of the file
    */
    public static String fixFileSeparator(String fileName) 
    {
        char fileSeparator = System.getProperty("file.separator").charAt(0);
        
        fileName = fileName.replace('\\', fileSeparator);
        fileName = fileName.replace('/', fileSeparator);

        return fileName;
    }


    public int compare ( Object o1, Object o2 )
    {
        String s1 = (String)o1;
        String s2 = (String)o2;

        // Longer strings should always sort later in the ordering.
        if ( s1.length() != s2.length() )
            return( s1.length() - s2.length() );

        // If the strings are the same length, just use normal 
        // lexicographical sort ordering (alphabetical).
        return( s1.compareTo( s2 ) );
    }


    // Class only has static methods, so don't allow instances to be created - 
    // except for use internally to provide Comparator for file name sorting.
    private FileUtils ( )
    {
        // NOT TO BE USED !!!
    }


    // The default is to use the default encoding provided by java.io Readers and Writers.
    private static boolean useUTF8Flag = false;
}
