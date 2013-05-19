package com.nightfire.adapter.util;

import com.nightfire.framework.message.*;
import java.util.*;


/**
 * checks each name value pair if they exist in the src
 */
public interface ConditionInterpreter {
   /**
    * all pairs have to exist and have correct values to return true.
    * @param pairs - a vector of NVPairs where each pair is a name and value.
    * @param src - the message source to check for the pairs in.
    */
   public boolean getAndAnswer(Vector pairs, MessageDataSource src) throws MessageException;

    /**
    * only one pair has to exist and have a correct value to return true.
    * @param pairs - a vector of NVPairs where each pair is a name and value.
    * @param src - the message source to check for the pairs in.
    */
   public boolean getOrAnswer(Vector pairs, MessageDataSource src) throws MessageException;
}

