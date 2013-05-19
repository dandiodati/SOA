package com.nightfire.adapter.util;

//////////////////////////////// NightFire packages //////////////////////////
import com.nightfire.common.*;
import com.nightfire.framework.util.*;

/**
 * This class replace all occurrences of &lt;, &gt;, &amp;, &apos;, &quot; by
 * <,>, &, ', " respectively or vice versa depending on the method - encode or
 * decode being called. Decode implies replacing "&gt;" by ">"...and encode implies
 * replacing ">" by "&gt;". This is also ilec dependent as some ilecs use some of
 * the above mentioned characters as special characters and this class skips the
 * replacement for such ilecs.
 */
public class ILECEntityResolver
{
    private static final String ENCODE = "ENCODE";
    private static final String DECODE = "DECODE";
    private static final String AMT = "AMT_ORDER";
    private static final String SWB = "SWB_ORDER";
    private static final String AMP_CHAR = "&" ;
    private static final String AMP_CODED = "&amp;" ;
    private static final String LT_CHAR = "<" ;
    private static final String LT_CODED = "&lt;" ;
    private static final String GT_CHAR = ">" ;
    private static final String GT_CODED = "&gt;" ;
    private static final String APOS_CHAR = "'" ;
    private static final String APOS_CODED = "&apos;" ;
    private static final String QUOT_CHAR = "\"" ;
    private static final String QUOT_CODED = "&quot;" ;
    private static final int SIZE = 5;

    //AMT uses "<" as a DESeparator, so "<" or "&lt" is skipped
    String amtCodedArray [] = { AMP_CODED, GT_CODED, APOS_CODED, QUOT_CODED };
    String amtCharArray [] = { AMP_CHAR, GT_CHAR, APOS_CHAR, QUOT_CHAR };

    //SWB uses ">" as a CompositeDE, so ">" or "&gt" is skipped
    String swbCodedArray [] = { AMP_CODED, LT_CODED, APOS_CODED, QUOT_CODED };
    String swbCharArray [] = { AMP_CHAR, LT_CHAR, APOS_CHAR, QUOT_CHAR };

    //General arrays for all other ilecs
    String commonCodedArray [] = { AMP_CODED, LT_CODED, GT_CODED, APOS_CODED, QUOT_CODED };
    String commonCharArray [] = { AMP_CHAR, LT_CHAR, GT_CHAR, APOS_CHAR, QUOT_CHAR };

  //encodedValue = { "&amp;", "&lt;", "&gt;", "&apos;", "&quot;" };
  //decodedValue = { "&", "<", ">", "'", "\"" };

   /**
    * Replace all occurrences of &lt;, &gt;, &amp;, &apos;, &quot; by
    * <,>, &, ', " respectively
    * @param input String containing the occurrences &lt;, &gt;, &amp;, &apos;, &quot;
    * @param ilecAndOSS The ILEC under consideration
    * @return String String with all occurrences replaced by the actual characters
    */
    public String decode ( String input, String ilecAndOSS )
      throws ProcessingException
    {
      return replaceStrings ( input, ilecAndOSS, DECODE );
    }//decode

   /**
    * Replace all occurrences of <,>, &, ', " by
    * &lt;, &gt;, &amp;, &apos;, &quot; respectively
    * @param input String containing the occurrences <,>, &, ', "
    * @param ilecAndOSS The ILEC under consideration
    * @return String String with all occurrences replaced by the encoded values
    */
    public String encode ( String input, String ilecAndOSS )
      throws ProcessingException
    {
      return replaceStrings ( input, ilecAndOSS, ENCODE );
    }//encode

   /**
    * Set up the array for encoding/decoding purposes depending on the ILEC
    * @param ilecAndOSS The ILEC under consideration
    * @param codedArray The array that is to be filled with the coded values depending on the ilec
    * @param charArray The array that is to be filled with the coded values depending on the ilec
    * @exception ProcessingException If ilecAndOSS value is null
    */
    private void setEncodeDecodeValues ( String ilecAndOSS, String codedArray[], String charArray [] )
      throws ProcessingException
    {
      if (ilecAndOSS == null )
      {
        Debug.log (Debug.ALL_ERRORS, "ERROR: ILECEntityResolver: ilecAndOSS value is null");
        throw new ProcessingException ("ERROR: ILECEntityResolver: ilecAndOSS value is null");
      }

      if ( ilecAndOSS.equalsIgnoreCase ( AMT ) )
      {
        for ( int i=0; i<amtCodedArray.length; i++)
        {
          codedArray[i] = new String(amtCodedArray[i]);
          charArray[i] = new String (amtCharArray[i]);
        }
      }
      else
      if ( ilecAndOSS.equalsIgnoreCase ( SWB ) )
      {
        for ( int i=0; i<swbCodedArray.length; i++)
        {
          codedArray[i] = new String(swbCodedArray[i]);
          charArray[i] = new String (swbCharArray[i]);
        }
      }
      else
      {
        for ( int i=0; i<commonCodedArray.length; i++)
        {
          codedArray[i] = new String(commonCodedArray[i]);
          charArray[i] = new String (commonCharArray[i]);
        }
      }

    }//setEncodeDecodeValues

   /**
    * Replace all occurrences of &lt;, &gt;, &amp;, &apos;, &quot; by
    * <,>, &, ', " respectively or vice versa depending on the "type" value
    * @param input String whose occurrences are to be replaced
    * @param ilecAndOSS The ILEC under consideration
    * @param type Whether to encode or decode values in input
    * @return String Modified input string
    * @exception ProcessingException Thrown if replacement fails
    */
    private String replaceStrings ( String input, String ilecAndOSS, String type )
      throws ProcessingException
    {
       if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log (Debug.MSG_STATUS, "ILECEntityResolver: Starting replacement for special entities");
      String origValue [] = new String [SIZE];
      String replaceValue [] = new String [SIZE];

      if ( type.equalsIgnoreCase (ENCODE) )
      {
        setEncodeDecodeValues ( ilecAndOSS, replaceValue, origValue );
      }
      else
      if ( type.equalsIgnoreCase (DECODE) )
      {
        setEncodeDecodeValues ( ilecAndOSS, origValue, replaceValue );
      }

       if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log (Debug.MSG_STATUS, "ILECEntityResolver: Got values to be modified");

      if ( input != null )
      {
          for (int i=0; i<SIZE; i++)
          {
            if ( origValue [i] != null )
              input = StringUtils.replaceSubstrings ( input, origValue[i], replaceValue[i]);
          }//for
      }//if
      else
      {
        Debug.log (Debug.ALL_ERRORS, "ERROR: ILECEntityResolver: Input to method replaceStrings is null");
        throw new ProcessingException ("ERROR: ILECEntityResolver: Input to method replaceStrings is null");
      }

       if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log (Debug.MSG_STATUS, "ILECEntityResolver: Finished replacement for special entities");

      return input;
    }//replaceStrings


    /**
     * Used only on strings containing NightFire standard XML.
     * Replaces special characters (', ", <, >, &) within a value attribute's actual value
     * with their corresponding entities (&apos;, &quot;, &lt;, &gt;, &amp;). 
     *
     * @param template   XML data in String format.
     * @return   A String containing XML data with all special characters replaced by
     * their corresponding entities.
     * @exception FrameworkException   Thrown if the starting and ending delimiters of the
     * value attribute's actual value are not legal.
     */
    
    public String addEntities (String template ) throws FrameworkException
    {

	String tokenStartIndicator = "value=\"";
        String tokenEndIndicator = null; // this is set below
	String tokenEndIndicator_1 = "\">";
	String tokenEndIndicator_2 = "\"/>";

	int tokenStartIndicatorLen    = tokenStartIndicator.length( );
	int tokenEndIndicatorLen     = 0; // this is set below

	int templateLen = template.length();
	int curTemplatePos = 0;

	StringBuffer sb = new StringBuffer( );

        if ( Debug.isLevelEnabled ( Debug.MSG_STATUS ) )
          Debug.log (Debug.MSG_STATUS, "ILECEntityResolver: Replacing special entities");
        
        do {
		// Locate next available token to be replaced.
		int startTokenPos = template.indexOf( tokenStartIndicator, curTemplatePos );
            
		// If no more tokens are found ...
		if ( startTokenPos == -1 ) {
		    
		    // Append remaining template data to result and exit loop.
		    if ( curTemplatePos < templateLen )
			sb.append( template.substring( curTemplatePos ) );
                
		    break;
		}

		// Append non-token template data before current token to result.
		if ( startTokenPos > curTemplatePos ) {
		    sb.append( template.substring( curTemplatePos, startTokenPos ) );
                    sb.append( tokenStartIndicator );
                }

		// Advance past start-token indicator.
		startTokenPos += tokenStartIndicatorLen;

		// Locate end of current token. Since the ending delimiter can be either
                // of "> or "/> and since the target contains both types in abundance,
                // we must take the next such delimiter from the current position
                
		int endTokenPos_1 = template.indexOf( tokenEndIndicator_1, startTokenPos );
		int endTokenPos_2 = template.indexOf( tokenEndIndicator_2, startTokenPos );
                
                int endTokenPos = -1;
                if (endTokenPos_1 != -1 && ( endTokenPos_1 < endTokenPos_2 || endTokenPos_2 == -1 ) ) {
                    
                    tokenEndIndicator = tokenEndIndicator_1;
                    endTokenPos = endTokenPos_1;
                        
                } else if (endTokenPos_2 != -1 && ( endTokenPos_2 < endTokenPos_1 || endTokenPos_1 == -1 ) ) {
                    
                    tokenEndIndicator = tokenEndIndicator_2;
                    endTokenPos = endTokenPos_2;

                }

                if (endTokenPos == -1){

                   throw new FrameworkException( "ERROR: Token-delimiter mismatch encountered near position [" 
						  + startTokenPos + "] in template during token replacement." );
		}
                
                tokenEndIndicatorLen = tokenEndIndicator.length( );
		
		// Advance past current token in template.
		curTemplatePos = endTokenPos + tokenEndIndicatorLen;
            
		// Get token text between start-token and end-token delimiters.
		String token = template.substring( startTokenPos, endTokenPos );
            
		// Remove any leading and trailing whitespace in token.
		token = token.trim( );

		// If the token is empty, skip token replacement.
		if ( token.length() == 0 ) {
                    sb.append( tokenEndIndicator );
		    continue;
                }
            
		
		// Replace any special characters in the token
		// with their corresponding entities.
		token = characterToEntity ( token );

		// Append the token to the output
		sb.append( token );
                sb.append( tokenEndIndicator );

	
	} while (true);

        
	     
	return sb.toString();

    }


    /*
     * A helper method for the addEntities method.
     * Replaces XML special characters with their corresponding entities.
     *
     * @param template   The string to be corrected.
     * @return   The template string with all special characters have been replaced by their corresponding entities.
     */
    
    private String characterToEntity ( String token ) {

	String[] specialCharacters = { "'", "\"", "<", ">", "&" };
	String[] entities = { "&apos;", "&quot;", "&lt;", "&gt;", "&amp;" };

	for (int i = 0; i < specialCharacters.length; i++) {

	    // Some maps already replace special characters with their corresponding
	    // entities, so ampersands must be handled specially.

	    if ( specialCharacters[i].equals( "&" ) ) {

		token = ampersandReplace( token, entities );
                
	    } else {
                 
                token = StringUtils.replaceSubstrings ( token, specialCharacters[i], entities[i] );

            }
	}

	return token;
    }


    /**
     * A helper method for the characterToEntity method.
     * Checks to see if an ampersand is just an ampersand,
     * in which case it is replaced by "&amp;", or if it starts
     * an XML entity, in which case it is allowed to pass through.
     *
     * @param target   A String containing one or more ampersands.
     * @param entities  A String array containg the XML entities.
     * @return   A String in which all non-entity ampersands have been
     * replaced by "&amp;".
     */
    
    private String ampersandReplace( String target, String[] entities) {

        String amp = "&";
        String ampEntity = "&amp;";
        
        StringBuffer sb = new StringBuffer( );

        do {
            // Locate the ampersand to be considered.
            int startIndex = target.indexOf( amp );
            
            // If we can't find substring, we're done, so append remaining part of 
            // target to transformed string.
            if ( startIndex == -1 ) {
                
                sb.append( target );

                break;
            }

            // Append text from start of 'target' to ampersand to new string.
            sb.append( target.substring( 0, startIndex ) );

            // if the ampersand starts an entity ignore it, otherwise replace it with &amp;
            int positionIncrement = 1;
            for (int i = 0; i < entities.length; i++) {
                
                if (target.substring( startIndex ).startsWith( entities[i] )) {
                    sb.append( entities[i] );
                    positionIncrement = entities[i].length();
                    break;
                }
            }

            if (positionIncrement == 1) {
                // the ampersand does not start an entity.
                sb.append(ampEntity);
            }
            
            // Make new target equal to remaining part of target just past the substring we just replaced.
            target = target.substring( startIndex + positionIncrement );
            
        } while ( true );

        return( sb.toString() );
    }
    

   /**
    * Main method for testing
    */
    public static void main ( String args [] )
    {
      Debug.enableAll();
      Debug.showLevels();

      String filetoModify = null;
      String retValue = null;
      ILECEntityResolver ILECEntityResolver = new ILECEntityResolver ();
      if (args.length != 3)
      {
        Debug.log (Debug.ALL_ERRORS, "ERROR: ILECEntityResolver.main: Input arguments to main method are incorrect");
      }
      else
      {
        try
        {
          if (args[1].equalsIgnoreCase ("ENCODE"))
          {
            filetoModify = new String (args[0]);
            filetoModify = FileUtils.readFile (filetoModify);
            retValue = ILECEntityResolver.encode ( filetoModify, args[2] );
          }
          else
          if (args[1].equalsIgnoreCase ("DECODE"))
          {
            filetoModify = new String (args[0]);
            filetoModify = FileUtils.readFile (filetoModify);
            retValue = ILECEntityResolver.decode ( filetoModify, args[2] );
          }
        }
        catch ( ProcessingException pe )
        {
          Debug.log (Debug.ALL_ERRORS, "ERROR: ILECEntityResolver.main: "+
            " Failed to modify string, " + pe.toString () );
        }
        catch ( FrameworkException fe )
        {
          Debug.log (Debug.ALL_ERRORS, "ERROR: ILECEntityResolver.main: "+
            " File reading failed, " + fe.toString () );
        }
      }//else

      Debug.log (Debug.NORMAL_STATUS, "ILECEntityResolver: Modified file is : ");
      Debug.log (Debug.NORMAL_STATUS, retValue );
    }//main
}//ILECEntityResolver
