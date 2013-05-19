/**
 * source file : FixedFormat.java
 *
 * This class is used to store the formatting information of a field
 * from a fixed format string.
 *
 * @author  Kalyani
 *
 * @Version  1.00
 */

package com.nightfire.adapter.util;

public class FixedFormat
{
    /**
     * This is the constructor , which is used to set the values of private variables .
     *
     * @param position - Starting position of the fixed format field
     * @param size - size(in bytes) of the fixed format field
     * @param type - type of the fixed format field ( i.e."AN"/"A"/"N" )
     * @param required - boolean value indicating whether the fixed format field
     *                   is required or not.
     */
    public FixedFormat ( int position , int size , String type , boolean required )
    {
        
        this.position = position;
        this.size = size;
        this.type = type;
        this.required = required;

    } // end constructor

    /**
     * This method is used to get the position of the fixed format field
     *
     * @return - value of 'position' attribute of this object
     */
	  public int getPosition( )
    {

        return this.position;

    } // end  getPosition()

    /**
     * This method is used to get the size of the fixed format field
     *
     * @return - value of 'size' attribute of this object
     */
  	public int getSize( )
    {

        return this.size;

    } // end getSize( )

    /**
     * This method is used to get the type of the fixed format field
     *
     * @return - value of 'type' attribute of this object
     */
  	public String getType( )
    {

        return this.type;

    } // end getType( )

    /**
     * This method is used to check if the fixed format field is required or not.
     *
     * @return - value of 'required' attribute of this object
     */
    public boolean isRequired( )
    {

        return this.required;

    } //end isRequired( )

    private int position;
    private int size;
    private String type;
    private boolean required;

    /**
        The main method to test this class independently
     */
    public static void main(String[] args)
    {
       try
       {
          FixedFormat format = new FixedFormat(2,7,"A",true);
          System.out.println (format.getPosition()+" "+format.getSize()+" "+format.getType()+" "+format.isRequired());
       }
       catch(Exception e)
       {
         System.out.println("Error : " + e.getMessage());
       }
   }
}