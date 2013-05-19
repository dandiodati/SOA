/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.framework.util;


import java.util.*;


/**
 * The TokenReplacer utility provides template-driven token replacement
 * functionality using data obtained from a TokenDataSource.
 *
 * Usage syntax using default tokens:
 *
 * Required item:   {{item-name}}
 * Optional item:   {{?item-name}}
 * Commented text:  {{!text}}
 *
 * NOTE: Nested tokens are not allowed and all token-delimiters 
 * must be unique and distinct from the surrounding text.
 */
public final class TokenReplacer
{
    /**
     * String indicating the beginning of a token to be replaced in template.
     */
	public static final String DEFAULT_TOKEN_START_INDICATOR = "{{";

    /**
     * String indicating the end of a token to be replaced in template.
     */
	public static final String DEFAULT_TOKEN_END_INDICATOR = "}}";

    /**
     * String indicating an optional token in template.
     */
	public static final String DEFAULT_OPTIONAL_TOKEN_INDICATOR = "?";

    /**
     * String indicating a comment token in template.
     */
	public static final String DEFAULT_COMMENT_TOKEN_INDICATOR = "!";


    /**
     * Create a token replacer.
     */
    public TokenReplacer ( )
    {
        this( null );
    }


    /**
     * Create a token replacer, giving it the template to use in replacements.
     *
     * @param  template  Template text whose tokens should be replaced by 
     *                   values from a token data source object.
     */
    public TokenReplacer ( String template )
    {
        Debug.log( Debug.MSG_LIFECYCLE, "Creating template-driven token replacement object." );

        setTemplate( template );
    }


    /**
     * Set the template to the value passed-in.
     *
     * @param  template  Template value.
     */
    public void setTemplate ( String template )
    {
        Debug.log( Debug.MSG_LIFECYCLE, "Setting template on token replacement object." );

        this.template = template;
    }


    /**
     * Set the template using the contents of the named file.
     *
     * @param  templateFileName  Name of file containing template.
     *
     * @exception  FrameworkException  Thrown if template can't be 
     *                                 loaded from given file.
     */
    public void setTemplateFromFile ( String templateFileName ) throws FrameworkException
    {
        Debug.log( Debug.MSG_LIFECYCLE, "Initializing token replacement template from file [" 
                   + templateFileName + "]." );
        
        setTemplate( FileUtils.readFile( templateFileName ) );
    }


    /**
     * Perform the token replacement against the template using the 
     * given token data source and return the resulting text.
     *
     * @param  tds  Token data source object to obtain values from.
     *
     * @return  Copy of template text whose tokens have been replaced 
     *          by values obtained from token data source object.
     *
     * @exception  FrameworkException  Thrown if required token values are missing,
     *                                 or template is malformed.
     */
    public String generate ( TokenDataSource tds ) throws FrameworkException
    {
        if ( template == null )
        {
            throw new FrameworkException( "ERROR: No template available to perform token replacement against." );
        }

        Debug.log( Debug.MSG_LIFECYCLE, "Replacing tokens in template with data values ..." );

		int tokenStartIndicatorLen    = tokenStartIndicator.length( );
		int tokenEndIndicatorLen      = tokenEndIndicator.length( );
		int optionalTokenIndicatorLen = optionalTokenIndicator.length( );
        
        int templateLen = template.length( );

        int curTemplatePos = 0;

		StringBuffer sb = new StringBuffer( );
        
        do
		{
            // Locate next available token to be replaced.
			int startTokenPos = template.indexOf( tokenStartIndicator, curTemplatePos );
            
            // If no more tokens are found ...
			if ( startTokenPos == -1 )
			{
                // Append remaining template data to result and exit loop.
                if ( curTemplatePos < templateLen )
                    sb.append( template.substring( curTemplatePos ) );
                
				break;
			}
            
            // Append non-token template data before current token to result.
            if ( startTokenPos > curTemplatePos )
                sb.append( template.substring( curTemplatePos, startTokenPos ) );
            
            // Advance past start-token indicator.
            startTokenPos += tokenStartIndicatorLen;

            // Locate end of current token.
			int endTokenPos = template.indexOf( tokenEndIndicator, startTokenPos );
            
            // If the end-token indicator can't be found ...
			if ( endTokenPos == -1 )
			{
                throw new FrameworkException( 
                             "ERROR: Token-delimiter mismatch encountered near position [" 
                             + startTokenPos + "] in template during token replacement." );
			}
            
            // Advance past current token in template.
            curTemplatePos = endTokenPos + tokenEndIndicatorLen;
            
            // Get token text between start-token and end-token delimiters.
			String token = template.substring( startTokenPos, endTokenPos );
            
            // Remove any leading and trailing whitespace in name.
            token = token.trim( );

            // If name is empty, skip token replacement.
            if ( token.length() == 0 )
                continue;

            // If token is a comment, continue on to the next token.
			if ( token.startsWith( commentTokenIndicator ) )
			{
				Debug.log( Debug.MSG_DATA, "Skipping comment token: " + token );
                
                continue;
			}
            
            // If all data is being treated as optional, or token begins with optional marker ...
            if ( (allDataOptional) || token.startsWith( optionalTokenIndicator ) )
            {
                // Get token name following the optional indicator.
                if ( token.startsWith( optionalTokenIndicator ) )
                    token = token.substring( optionalTokenIndicatorLen );
                
                // If value doesn't exist, continue on to the next 
                // token, since current one is optional.
                if ( !tds.exists( token ) )
                {
                    Debug.log( Debug.MSG_DATA, "Skipping absent item [" 
                               + token + "] since it's optional." );

                    continue;
                }
            }
            
            try
            {
                // Get the value from the token data source and append it to the output result.
                String value = tds.getValue( token );
                
                if ( StringUtils.hasValue( value ) )
                {
                    /* If tokenexpression is being used in SQL query perform DB formatting.*/
                    if (performDBFormatting)
                    {
                        value = StringUtils.dbFormat(value);
                    }
                    Debug.log( Debug.MSG_DATA, "Replacing token [" +
                               token + "] with value [" + value + "]." );
                    
                    sb.append( value );
                }
            }
            catch ( Exception e )
            {
                throw new FrameworkException( "Missing token [" + token +
                                              "] encountered during template token replacement." );
            }
		}
        while ( true );
        
        // Return template with tokens replaced by values.
		return( sb.toString() );
    }


    /**
     * Set the all-data-optional flag.
     *
     * @param  flag  If 'true', all data will be treated
     *               as optional.  If 'false', only those
     *               data items marked with the optional
     *               indicator will be treated as optional.
     */
    public void setAllDataOptional ( boolean flag )
    {
        allDataOptional = flag;
    }

    /**
     * Set the perform-SQL-Formatting flag.
     *
     * @param  performDBFormatting  If 'true' then the
     *               formatting would be made and if 
     *               'false' then formatting would not 
     *               be made. Default value is 'false'.
     */
    public void setPerformDBFormatting ( boolean performDBFormatting )
    {
        this.performDBFormatting = performDBFormatting;
    }
    /**
     * Set the token-start indicator value.
     *
     * @param  value  The value to set the indicator to.
     */
    public void setTokenStartIndicator ( String value )
    {
        if ( StringUtils.hasValue( value ) )
            tokenStartIndicator = value;
    }
    
    
    /**
     * Set the token-end indicator value.
     *
     * @param  value  The value to set the indicator to.
     */
    public void setTokenEndIndicator ( String value )
    {
        if ( StringUtils.hasValue( value ) )
            tokenEndIndicator = value;
    }
    
    
    /**
     * Set the optional-token indicator value.
     *
     * @param  value  The value to set the indicator to.
     */
    public void setOptionalTokenIndicator ( String value )
    {
        if ( StringUtils.hasValue( value ) )
            optionalTokenIndicator = value;
    }
    
    
    /**
     * Set the comment-token indicator value.
     *
     * @param  value  The value to set the indicator to.
     */
    public void setCommentTokenIndicator ( String value )
    {
        if ( StringUtils.hasValue( value ) )
            commentTokenIndicator = value;
    }


    private String tokenStartIndicator    = DEFAULT_TOKEN_START_INDICATOR;
    private String tokenEndIndicator      = DEFAULT_TOKEN_END_INDICATOR;
    private String optionalTokenIndicator = DEFAULT_OPTIONAL_TOKEN_INDICATOR;
    private String commentTokenIndicator  = DEFAULT_COMMENT_TOKEN_INDICATOR;
    
    private String template;

    private boolean allDataOptional = false;

    private boolean performDBFormatting = false;
}
