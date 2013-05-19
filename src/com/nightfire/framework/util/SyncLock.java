package com.nightfire.framework.util;



/**
 * This class is a lock class that can be used to maintain a lock across methods,
 * different objects, or different threads.
 * It provides time out functionality and can handle nested calls by the same thread. 
 * Clients must always release the lock after acquiring it (usually via a finally clause).
 *
 */
public class SyncLock implements Lock
{
  private Thread curThread = null;
  private int nestedCount = 0;

  private String lockName;

  /**
   * create a sync lock with the specified name.
   * @param name - The name for this SyncLock. Used during logging to help identify different
   * SyncLocks.
   */
  public SyncLock(String name)
  {
     lockName = name;
  }

  /**
   * Create a sync lock with the default name of "Lock".
   */
  public SyncLock()
  {
     lockName = "Lock";
  }

  /**
    * Try to obtain the lock. Waits indefinitely.
    * @exception InterruptedException occurs if the thread is Interrupted.
    */
    public void acquire() throws InterruptedException
    {
       acquire(0);
    }


     /**
     * obtains or starts the lock with a wait time of msecs milliseconds.
     * If it takes longer than msecs to obtain the lock an InterruptedException is thrown.
     * @param msecs the number of milliseconds to wait to obtain the lock. (0 indicates to wait forever).
     * @exception InterruptedException occurs if the acquiring the lock takes longer than msecs or
     * if the thread is interrupted.
     */
    public void acquire(long msecs) throws InterruptedException
    {
       long startTime  = DateUtils.getCurrentTimeValue();

         synchronized(this) {


            String incomingThread = null;
            if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
              incomingThread = Thread.currentThread().getName();

            if (Thread.interrupted()) throw new InterruptedException();

            if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
               Debug.log(this,Debug.THREAD_STATUS, lockName + " [" + incomingThread +"] : SyncLock trying to acquire lock, waitTime["+ msecs+"] , nested count [" + nestedCount + "]");

            if (curThread == null) {
               curThread = Thread.currentThread();
            }
            else if (curThread != null && curThread == Thread.currentThread() )  {
               nestedCount++;
            } else  {

               do {
                  if (msecs != 0 && (DateUtils.getCurrentTimeValue() - startTime ) > msecs )
                     throw new InterruptedException(lockName + " [" + incomingThread +"] : Acquiring SyncLock took longer than " + msecs + " milliseconds : " + (DateUtils.getCurrentTimeValue() - startTime ));
                  try {
                    if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
                       Debug.log(this,Debug.THREAD_STATUS,lockName + " [" + incomingThread +"] : SyncLock waiting");
                       wait(msecs);
                  } catch(InterruptedException ex) {
                     Debug.log(Debug.THREAD_WARNING, lockName + " [" + incomingThread +"] : Thread was interrupted.");
                     throw ex;
                  }
               } while ( curThread != null || (msecs !=0 && (DateUtils.getCurrentTimeValue() - startTime ) > msecs)  );
               curThread = Thread.currentThread();
            }

            if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
               Debug.log(this,Debug.THREAD_STATUS, lockName + " [" + incomingThread +"]  : SyncLock acquired lock, nested count [" + nestedCount + "]");

        }

     }

    /**
     * releases the lock.
     * NOTE: This method checks if the current thread owns the lock before releasing.
     *
     */
    public void release()
    {
       synchronized(this) {
           String incomingThread = null;
           if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
              incomingThread = Thread.currentThread().getName();

          Debug.log(Debug.THREAD_STATUS, lockName + " [" + incomingThread +"]  : SyncLock Trying to release lock, nested count [" + nestedCount + "]");
          // if we own the lock release it, and notify others
          if ( curThread == Thread.currentThread() ) {

             if ( nestedCount > 0) {
                nestedCount--;
                if ( Debug.isLevelEnabled(Debug.THREAD_STATUS) )
                   Debug.log(Debug.THREAD_STATUS, lockName + " [" + incomingThread +"]  : SyncLock releasing nested lock, nested count [" + nestedCount + "]");
             } else if ( nestedCount < 0 ) {
                Debug.log(Debug.THREAD_WARNING,lockName + " [" + incomingThread +"]  : SyncLock nested thread count has fallen below 0, [" + nestedCount + "] reseting");
                nestedCount = 0;
                curThread = null;
                notify();
             } else {
                if ( Debug.isLevelEnabled(Debug.THREAD_STATUS) )
                   Debug.log(Debug.THREAD_STATUS, lockName + " [" + incomingThread +"]  : SyncLock releasing thread lock, nested count [" + nestedCount + "]");

                curThread = null;
                notify();
             }
          } else
             Debug.log(Debug.THREAD_STATUS, lockName + " [" + incomingThread +"]  : SyncLock current Thread [" + Thread.currentThread() + "] has not acquired this lock. Not releasing.");
       }
    }

}