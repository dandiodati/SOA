/**
 * Copyright (c) 2002 NightFire Software, Inc.  All rights reserved.
 *
 * $Header$
 */

package com.nightfire.webgui.core.beans;



/**
 * A bean which stores customer specific information in the session.
 *
 */

public class SessionInfoBean
{

    private String customerId;
    private String userId;
    private String password;
    private String wsp;
    private long startTime;
    private long requestTime;
    private long responseTime;
    private long requestCount=1;
    private long totalResponseTime;

    public SessionInfoBean(String customerId, String userId, String password,long startTime)
    {
        this (customerId, userId, password,"", startTime);
    }


    public SessionInfoBean(String customerId, String userId, String password,String wsp,long startTime)
    {
        this.customerId = customerId;
        this.userId     = userId;
        this.password   = password;
        this.wsp        = wsp;
        this.startTime  = startTime;
    }


    public void setCustomerId(String id)
    {
        customerId = id;
    }


    public void setUserId(String id)
    {
        userId = id;
    }


    public String getCustomerId()
    {
        return customerId;
    }

    public String getUserId()
    {
        return userId;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getWsp()
    {
        return wsp;
    }

    public void setWsp(String wsp)
    {
        this.wsp = wsp;
    }

    public long getStartTime()
    {
      return startTime;
    }

    public void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public long getRequestTime()
    {
      return requestTime;
    }

    public void setRequestTime(long reqTime)
    {
        this.requestTime = reqTime;
        requestCount++;
    }

    public long getResponseTime()
    {
      return responseTime;
    }

    public void setResponseTime(long respTime)
    {
        this.responseTime = respTime;
        totalResponseTime += responseTime - requestTime;
    }

    public long getAvgResponseTime()
    {
      return totalResponseTime/requestCount;
    }

}
