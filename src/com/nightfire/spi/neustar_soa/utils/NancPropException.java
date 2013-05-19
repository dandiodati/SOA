package com.nightfire.spi.neustar_soa.utils;

import com.nightfire.framework.util.FrameworkException;

/**

 * A NancPropException is thrown when an error occurs in the

 * NANC Support Properties API.

 *

 * @author NeuStar

 */

public class NancPropException extends FrameworkException {



    /**

     * Create a NANC Support Properties exception object with the

     * given message.

     *

     * @param msg Error message associated with exception.

     */

    public NancPropException(String msg) {

        super(msg);

    }



    /**

     * Create a NANC Support Properties exception object with the

     * given exception's message.

     *

     * @param e Exception object used in creation.

     */

    public NancPropException(Exception e) {

        super(e);

    }



}


