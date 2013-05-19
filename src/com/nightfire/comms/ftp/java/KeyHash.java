/**
 * Copyright (c) 2003 Nightfire Software, Inc. All rights reserved.
 *
 */

package com.nightfire.comms.ftp.java;

import java.io.*;
import java.util.*;
import com.nightfire.framework.util.*;



/*
 * This is a utility class for the FTPPoller and FTPClient.
 * It provides unique locks to be returned based on a id.
*/
public class KeyHash
{

    private static HashMap map = new HashMap();
    
    public static String getLock(String id) 
    {
        // make sure you always return the same lock
        synchronized (map) {
            String lock = (String)map.get(id);

          if (lock == null) {
              lock = new String(id);
              map.put(id, lock);
          }
            return lock;
        }
    }
}

            
             
