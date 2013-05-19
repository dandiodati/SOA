/**
 * Copyright (c) 1997 Nightfire Software, Inc. All rights reserved.
 */

package com.nightfire.framework.util;

import java.util.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.db.*;


/**
 * Handles read/write syncronhization bewtween threads.
 * It allows concurrent reader threads to access a locked area at the same time when
 * there are no writer threads updating that area. When a writer thread is updating
 * the section the reader threads wait.
 * NOTE: This class does not allow reentry, so threads can not have
 * nested locks. Deadlock could occur if a reader thread is reading
 * , followed by a writer thread waiting, followed by the same reader thread waiting.
 *
 * the following format should be used for either a read or write lock:
 * try {
 *    readWriteLock.getReadLock().aquire();
 *    // do stuff
 * } catch (InterruptedException e) {}
 * finally {
 *    readWriteLock.getReadLock().release();
 * }
 *
 * @author Dan Diodati
 */
public class ReadWriteLock
{

  private long waitingReaders;
  private long runningReaders;

  private Thread runningWriter;
  private long waitingWriters;

  protected final ReadLock readLock = new ReadLock();
  protected final WriteLock writeLock = new WriteLock();

   public ReadWriteLock()
  {
     runningWriter = null;
  }


  /**
   * returns an instance of an object which handles read only locking.
   * @return Lock an instance of a read Lock
   */
  public final Lock getReadLock() { return readLock; };

  /**
   * returns an instance of an object which handles write only locking.
   * @return Lock an instance of write Lock.
   */
  public final Lock getWriteLock() { return writeLock; };

   /**
   * returns an instance of an object which handles read only locking.
   */
  protected final void signalReaders() {
     readLock.signal();
  }
  /**
   * returns an instance of an object which handles write only locking.
   */
  protected final void signalWriter() {
     writeLock.signal();
  }

  /**
   * signals other threads when a read lock's release method is called
   */
  protected final synchronized void readLockRelease() {
     removeRunningReader();

     if (runningReaders == 0 && waitingWriters > 0) {
        signalWriter(); // we only want to wake up one of the writers
     } else if (runningWriter == null) {
       signalReaders();
     }

  }

  /**
   * signals other threads when a write lock's release method is called
   */
  protected final synchronized void writeLockRelease() {

      // only the running write can release the writer
      // thread. This check prevents a waiting writer thread from 
      // releasing the writer lock if the thread is interrupted.
      // In other words,  if thread A obtains a write lock 
      // and then thread B becomes a waiting writer thread. And 
      // then thread B gets interrupted and tries to release the write lock which it does not
      // own. Then the number of waiting writers will decrement by one.
      if (runningWriter == Thread.currentThread()) {
          
          runningWriter = null;

          if (waitingWriters > 0) {
              signalWriter(); // we only want to wake up one of the writers
          } else if (waitingReaders > 0) {
              signalReaders(); // we want to wake up all readers
          }
      } else {
          removeWaitingWriter();
      }
      
      

  }

  /**
  * condition to test if a reader is able to read or should wait
  *
  * in this case, a reader can only read if there are no writers.
  *
  * @returns true if the reader is able to read
  *          false if the reader should wait.
  */
  protected boolean readable()
  {
     return (runningWriter == null && waitingWriters == 0);
  }


  /**
  * condition to test if a writer is able to write or should wait
  *
  * in this case, a writer can only write if there are no readers currently reading and if there are
  * no other writer doing an update.
  *
  * @returns true if the writer is able to read
  *          false if the writer should wait.
  */
  protected boolean writeable()
  {
     return(runningReaders == 0 && runningWriter == null);
  }

  /**
   * updates the reader status during the initial read.
   * @return true if the reader is able to read.
   */
  protected final synchronized boolean updateNewReader() {

      boolean readable =  readable();

     if (readable)
        ++runningReaders;
      else
        ++waitingReaders;

      return readable;
  }


  /**
   * updates the reader that has been idle
   * @return true if the reader is able to read
   */
  protected final synchronized boolean updateWaitingReader () {

      boolean readable =  readable();

     if (readable) {
        removeWaitingReader();
        ++runningReaders;
     }

     return readable;
  }

  /**
   * updates the status of a new writer
   * @return boolean true if the writer can write.
   */
   protected final synchronized boolean updateNewWriter () {

     boolean writeable = writeable();

     if (writeable)
        runningWriter = Thread.currentThread();
     else
        ++waitingWriters;

     return writeable;
  }

  /**
   * updates the status of an idle writer
   * @return boolean true if the idle writer can write
   */
  protected final synchronized boolean updateWaitingWriter() {

     boolean writeable = writeable();

     if (writeable)  {
        removeWaitingWriter();
        runningWriter = Thread.currentThread();
     }

     return writeable;
  }

  /**
   * removes an idle writer
   */
  protected final synchronized void removeWaitingWriter() {

    if (waitingWriters > 0)
      --waitingWriters;
  }

  /**
   * removes a idle reader
   */
  protected final synchronized void removeWaitingReader() {
    if (waitingReaders > 0 )
      --waitingReaders;
  }

  protected final synchronized void removeRunningReader() {
    if (runningReaders > 0 )
      --runningReaders;
  }









  //inner class that handles Reader threads
  private class ReadLock extends Signaller implements Lock
  {

      // obtains the reader lock
      public final void acquire() throws InterruptedException
      {

         if (Thread.interrupted()) throw new InterruptedException();

         if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
            Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Readlock aquired");
         synchronized(this) {
            if (!updateNewReader()) {
               do {
                  try {
                    if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
                       Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Readlock waiting");
                    wait();
                  } catch(InterruptedException ex) {
                     removeWaitingReader();
                     break;
                  }
               } while (!updateWaitingReader() );
            }
        }

     }

     public final void acquire(long msecs) throws InterruptedException
      {

         long startTime  = DateUtils.getCurrentTimeValue();
         if (Thread.interrupted()) throw new InterruptedException();

         if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
            Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Readlock aquired");
         synchronized(this) {
            if (!updateNewReader()) {
               do {
                  if ((DateUtils.getCurrentTimeValue() - startTime ) > msecs )
                     throw new InterruptedException("Acquiring read lock took longer than " + msecs + " milliseconds : " + (DateUtils.getCurrentTimeValue() - startTime ));
                  try {
                    if (Debug.isLevelEnabled(Debug.THREAD_STATUS))
                       Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Readlock waiting");
                    wait(msecs);
                  } catch(InterruptedException ex) {
                     removeWaitingReader();
                     break;
                  }
               } while ( (DateUtils.getCurrentTimeValue() - startTime ) > msecs ||
                                   !updateWaitingReader() );
            }
        }

     }

     //releases the reader lock
     public final void release() {
        synchronized(this) {
           if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
              Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Readlock releasing");
           readLockRelease();
        }
     }
     //notifys reader threads to wake up
     synchronized final void signal() {
       notifyAll();
     }


  }

  // inner class that handles writer threads
  private class WriteLock extends Signaller implements Lock
  {
      // obtains a write lock
      public final void acquire() throws InterruptedException
      {
         if (Thread.interrupted()) throw new InterruptedException();

         if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
            Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Writelock aquired");

         synchronized(this) {
            if (!updateNewWriter()) {
               do {
                  try {
                    if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
                       Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Writelock waiting");
                    wait();
                  } catch(InterruptedException ex) {
                     removeWaitingWriter();

                     break;
                  }
               } while (!updateWaitingWriter());
            }

        }

     }

     // obtains a write lock
      public final void acquire(long msecs) throws InterruptedException
      {
         long startTime  = DateUtils.getCurrentTimeValue();
         if (Thread.interrupted()) throw new InterruptedException();

         if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
            Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Writelock aquired");

         synchronized(this) {
            if (!updateNewWriter()) {
               do {
                   if ((DateUtils.getCurrentTimeValue() - startTime ) > msecs )
                     throw new InterruptedException("Acquiring write lock took longer than " + msecs + " milliseconds : " + (DateUtils.getCurrentTimeValue() - startTime ));
                  try {
                    if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
                       Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Writelock waiting");
                    wait(msecs);
                  } catch(InterruptedException ex) {
                     removeWaitingWriter();

                     break;
                  }
               } while ( ( DateUtils.getCurrentTimeValue() - startTime ) > msecs ||
                             !updateWaitingWriter());
            }

        }

     }

     // releases the write lock
     public final void release() {
        synchronized(this) {
           if (Debug.isLevelEnabled(Debug.THREAD_STATUS) )
              Debug.log(this,Debug.THREAD_STATUS, Thread.currentThread() + " : Writelock releasing");
           writeLockRelease();
        }
     }

      // signals one of the writers to wake up
      synchronized final void signal() {
       notify();
     }




  }

  //abstract class to handle signal handling
  protected abstract class Signaller {
     abstract void signal();
  }

}
