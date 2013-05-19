/**
 * Copyright(c) 2000-2006 Neustar, Inc.
 * All rights reserved.
 *
 * $Header: $
 */

package  com.nightfire.framework.util;


//(java.util.concurrent) package is added in Java 1.5
import java.util.concurrent.*;

/**
  *  This class is a wrapper class containing object edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor.
  *  Two parameters can be configured from outside for this thread pool, CORE_POOL_SIZE and MAX_POOL_SIZE.
  *  Another parameter keepAliveTime is with value 0 seconds so thread will be terminates as soon as they will be free.
  *  Another parameter linked bounded queue is used with max pool size to prevent resource exhaustion.
  *  This class relies on defaultThreadFactory of ThreadPoolExecutor, that creates threads to all
  *  be in the same ThreadGroup  and with the same NORM_PRIORITY priority and non-daemon status. 
  *
  */

public class ThreadPoolExecutorService {

	protected ThreadPoolExecutor pool;
    protected int keepAliveTime = 0; // in seconds

	public ThreadPoolExecutorService(int corePoolSize, int maxPoolSize) throws FrameworkException
    {
	    poolInitialize(corePoolSize, maxPoolSize);
	}

    /**
     * This method initializes ThreadPoolExecutor.
     * @param corePoolSize the number of threads to keep in the
     * pool, even if they are idle.
     * @param maximumPoolSize the maximum number of threads to allow in the
     * pool.
     * @param keepAliveTime when the number of threads is greater than
     * the core, this is the maximum time that excess idle threads
     * will wait for new tasks before terminating.
     * @param unit the time unit for the keepAliveTime
     * argument.
     * @param workQueue the queue to use for holding tasks before they
     * are executed. This queue will hold only the Runnable
     * tasks submitted by the  execute method.
     * @throws IllegalArgumentException if corePoolSize, or
     * keepAliveTime less than zero, or if maximumPoolSize less than or
     * equal to zero, or if corePoolSize greater than maximumPoolSize.
     */
	protected void poolInitialize(int corePoolSize, int maxPoolSize) throws FrameworkException
	{
		try
		{	
            pool = new ThreadPoolExecutor(  corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue(maxPoolSize) );

			Debug.log(Debug.SYSTEM_CONFIG, StringUtils.getClassName(this) + ": Loaded ThreadPool with corePoolSize " + corePoolSize +" and maxPoolSize "+ maxPoolSize);
		}
        catch(RuntimeException re)
		{
            if(re instanceof IllegalArgumentException)
            {
                Debug.log(Debug.ALL_ERRORS, "Failed to initialize ThreadPoolExecutor. Check if corePoolSize, or keepAliveTime less than zero, or if maximumPoolSize less than or equal to zero, or if corePoolSize greater than maximumPoolSize.");
            }

            throw new FrameworkException("Failed initialization of Pool in class "+ StringUtils.getClassName(this));
		}
	}

    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    public int getPoolSize()
	{
		return pool.getPoolSize();
	}

    /**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     * RejectedExecutionHandler, if task cannot be accepted
     * for execution
     * @throws NullPointerException if command is null
     */
	public void execute (Runnable command)
	{
		boolean flag = true;
	   while(flag)
	   {
		   try
		   {
			   pool.execute(command);
			   flag = false;
		   }
		   catch(RejectedExecutionException ree)
		   {
			   // if executor rejects this message wait for a
			   // free thread in the pool.
			   try
			   {
				   Debug.log(Debug.ALL_WARNINGS, "ThreadPoolExecutor is not accepting more tasks. Pausing further task submission for 10 millisecs so that thread could free up...");
				   Debug.log(Debug.ALL_WARNINGS, "Thread pool statistics: [CorePoolSize: " +pool.getCorePoolSize()+ ", MaxPoolSize: " + pool.getMaximumPoolSize()
						   + ", TotalTaskScheduled: " + pool.getTaskCount() +", TotalTasksCompleted: " +pool.getCompletedTaskCount()+"]");
				   Thread.sleep(10);
			   }
			   catch (InterruptedException e)
			   {
				   Debug.log(Debug.ALL_ERRORS, "Intterrupted sleeping thread..");
			   }
		   }
		   catch(Exception re)
		   {
			   flag = false;
			   Debug.log(Debug.ALL_ERRORS, "Unexpected Exception occured while executing the request.");
			   Debug.log(Debug.ALL_ERRORS, Debug.getStackTrace(re));
			   Debug.error("Skipping this request .. "+command.toString());
		   }
	   }
	}

    /**
     * Returns the size of task queue used by pool. 
     * This queue may be in active use.  Retrieving the task queue from pool
     * does not prevent queued tasks from executing.
     *
     * @return the size of task queue
     */
    public int getQueueSize()
    {
        return (pool.getQueue()).size() ;
    }
    
     /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be
     * accepted. Invocation has no additional effect if already shut
     * down.
     * @throws SecurityException if a security manager exists and
     * shutting down this ExecutorService may manipulate threads that
     * the caller is not permitted to modify because it does not hold
     * java.lang.RuntimePermission("modifyThread"),
     * or the security manager's checkAccess method denies access.
     */
    public void shutdown()
    {
        try
        {
            pool.shutdown();
        }
        catch(RuntimeException re)
		{
            Debug.log(Debug.ALL_ERRORS, "Failed to execute shutdown() on Thread Pool.");
 		}
    }

}