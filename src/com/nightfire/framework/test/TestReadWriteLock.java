package com.nightfire.framework.test;


import java.util.*;
import junit.framework.*;
import com.nightfire.framework.util.*;


public class TestReadWriteLock extends TestCase
{
  private String readString;
  private LinkedList list;
  private LinkedList messages;
  private ReadWriteLock lock;

  private int threadStepCount = 0;

  public static final int NUMBER_THREADS = 4;
  public static final int THREAD_STEPS = 60;
  public static final String ORIG_STRING = "Hello World";

  public TestReadWriteLock(String name) {
		super(name);
    list = new LinkedList();
    messages = new LinkedList();
    lock = new ReadWriteLock();
    try {
      setUpDebugOptions();
     } catch (Exception e) {
        fail(e.getMessage() );
     }
  }

  public void setUp()
  {
     threadStepCount = 0;
     readString = ORIG_STRING;
  }

   public void tearDown()
  {
     list.clear();
     messages.clear();

  }


  public void testReaders()
  {

        System.out.println("***** Testing Readers ******");
        for(int i =0; i < NUMBER_THREADS; i++) {
           new ReaderThread().start();
        }



     // wait for the other threads to exit
     while (checkThreadStepCount() < (NUMBER_THREADS*THREAD_STEPS) ) {
       try {
        Thread.sleep(500);
       } catch (InterruptedException i) {
        fail("Sleeping main thread was interrupted");
       }
     }

     System.out.println("Ran " + checkThreadStepCount() + " threads");

     // all the list should have each of the threads running currently
     // To check we make sure that the first thread has other threads interrupting it
     // during the first THREAD_STEPS steps.
     String t = (String) list.get(0);

     boolean failed = true;

     for (int i =1; i < THREAD_STEPS; i++ ) {
       String temp = (String)list.get(i);
       //System.out.println("Matching thread " + t + " to thread " + temp);
       if (!t.equals(temp) ) {
          failed = false;
          break;
       }
     }
      assertTrue("Reader Threads should have concurrent access to the readString", !failed ) ;

  }

   public void testWriters()
  {


         System.out.println("***** Testing Writers ******");
        for(int i =0; i < NUMBER_THREADS; i++) {
           new WriterThread().start();
        }




     // wait for the other threads to exit
     while (checkThreadStepCount() < ( NUMBER_THREADS*THREAD_STEPS) ) {
       try {
        Thread.sleep(500);
       } catch (InterruptedException i) {
        fail("Sleeping main thread was interrupted");
       }
     }

     // all the list should have each of the threads running sequentially.
     // To check we make sure that the first thread finishes THREAD_STEPS
     // steps before switching to another thread.
     String t = (String) list.get(0);

     boolean failed = false;

     for (int i =1; i < THREAD_STEPS; i++ ) {
       String temp = (String)list.get(i);
       //System.out.println("Matching thread " + t + " to thread " + temp);
       if (!t.equals(temp) ) {
          failed = true;
          break;
       }
     }
      assertTrue("Writer Threads should have non-concurrent access to the readString", !failed ) ;

  }

  /**
   * Run some writers followed by some readers. The writers modify
   * a string that all the readers are trying to read.
   * Since the writers are started first, the readers should block
   * util one of the writers are finished writing.
   * So if any of the reader threads get a readString with the value
   * of ORIG_STRING then they did not block and the test fails.
   *
   */
  public void testWritersAndReaders()
  {
      System.out.println("***** Testing both Readers and Writers ******");


        for(int i =0; i < NUMBER_THREADS; i++) {
           new WriterThread().start();
        }


        for(int i =0; i < NUMBER_THREADS; i++) {
           new ReaderThread().start();
        }


     // wait for the other threads to exit
     while (checkThreadStepCount() < 2*(NUMBER_THREADS*THREAD_STEPS) ) {
       try {
        Thread.sleep(500);
       } catch (InterruptedException i) {
        fail("Sleeping main thread was interrupted");
       }
     }

     String t = (String) list.get(0);

     boolean failed = false;

     for (int i =1; i < THREAD_STEPS; i++ ) {
       String temp = (String)list.get(i);
       //System.out.println("Matching thread " + t + " to thread " + temp);
       if (t.equals(ORIG_STRING) ) {
          failed = true;
          break;
       }
     }
      assertTrue("Reader Threads should never get a hold of the string " + ORIG_STRING, !failed ) ;

  }

   /**
   * A deadlocking writer followed by a reader using the acquired(msecs) method.
   *
   */
  public void testReaderTimedAquiredLock()
  {
      System.out.println("***** Testing Reader Timed Acquire ******");



        new WriterThreadDeadlock().start();


        ReaderThreadTimed t = new ReaderThreadTimed();
        //run in this thread
        t.run();
        if (t.message != null)
          fail(t.message);

  }

  /**
   * A deadlocking reader followed by a writer using the acquired(msecs) method.
   *
   */
  public void testWriterTimedAquiredLock()
  {
      System.out.println("***** Testing Reader Timed Acquire ******");



        new ReaderThreadDeadlock().start();


        WriterThreadTimed t = new WriterThreadTimed();
        //run in this thread
        t.run();
        if (t.message != null)
          fail(t.message);

  }

   public synchronized int checkThreadStepCount() {
     return threadStepCount;
  }

  public synchronized void addThread(String s) {
     list.add(s);
     threadStepCount++;
  }

  public synchronized void addMessage(String s) {
     messages.add(s);
  }

  private class ReaderThread extends Thread
  {
     public void run()
     {
       try {
        System.out.println("ReaderThread: " + Thread.currentThread() + " about to obtain the lock");
        lock.getReadLock().acquire();

        System.out.println("ReaderThread: " + Thread.currentThread() + " performing " + THREAD_STEPS + " steps");
        for (int i = 0; i < THREAD_STEPS; i++ ) {

           System.out.println("ReaderThread: " + Thread.currentThread() + " at step [" + i + "] Read a string " + readString);
           addThread(this.toString());
           System.out.println("ReaderThread: " + Thread.currentThread() +"Adding self to thread list");
           addMessage(readString);
           System.out.println("ReaderThread: " + Thread.currentThread() + "Adding message to thread list");
        }
         System.out.println("ReaderThread: " + Thread.currentThread() + " finished");
       } catch (InterruptedException i) {
         fail("ReaderThread was interrupted");
       } finally {
         lock.getReadLock().release();
       }

     }

  }

  private class WriterThread extends Thread
  {
     public void run()
     {
       try {
        lock.getWriteLock().acquire();
        System.out.println("WriterThread: " + Thread.currentThread() + " performing " + THREAD_STEPS + " steps");
        for (int i = 0; i < THREAD_STEPS; i++ ) {

           System.out.println("WriterThread: " + Thread.currentThread() + " at step [" + i + "] Read a string " + readString);
           addThread(this.toString());
           readString = ORIG_STRING + "_" + Thread.currentThread() + "_THREAD_STEP_" + i;
        }
         System.out.println("WriterThread: " + Thread.currentThread() + " finished");
       } catch (InterruptedException i) {
        fail("WriterThread was interrupted");
       } finally {
           System.out.println("WriterThread: "  + Thread.currentThread() + " releasing the lock.");
        lock.getWriteLock().release();
       }
     }

  }


   private class WriterThreadDeadlock extends Thread
  {
     public void run()
     {
       try {
        lock.getWriteLock().acquire();
        System.out.println("WriterThreadDeadlock: " + Thread.currentThread() + " performing " + THREAD_STEPS + " steps");



           addThread(this.toString());

           synchronized (this) {
             while (true) {
               try {
                 wait();
               } catch (InterruptedException e) {};
             }
           }


       } catch (InterruptedException i) {
        fail("WriterThreaddeadlock was interrupted");
       } finally {
           System.out.println("WriterThread: "  + Thread.currentThread() + " releasing the lock.");
        lock.getWriteLock().release();
       }
     }

  }

    private class ReaderThreadDeadlock extends Thread
  {
     public void run()
     {
       try {
        lock.getReadLock().acquire();
        System.out.println("ReaderThreadDeadlock: " + Thread.currentThread() + " performing " + THREAD_STEPS + " steps");



           addThread(this.toString());

           synchronized (this) {
             while (true) {
               try {
                 wait();
               } catch (InterruptedException e) {};
             }
           }


       } catch (InterruptedException i) {
        fail("ReaderThreaddeadlock was interrupted");
       } finally {
           System.out.println("ReaderThread: "  + Thread.currentThread() + " releasing the lock.");
        lock.getReadLock().release();
       }
     }

  }


    private class ReaderThreadTimed extends Thread
  {
     public String message = null;

     public void run()
     {
       boolean exceptionThrown = false;

       try {
        System.out.println("ReaderThreadTimed: " + Thread.currentThread() + " about to obtain the lock");
        lock.getReadLock().acquire(10);

        System.out.println("ReaderThreadTimed: " + Thread.currentThread() + " performing " + THREAD_STEPS + " steps");
        for (int i = 0; i < THREAD_STEPS; i++ ) {

           System.out.println("ReaderThreadTimed: " + Thread.currentThread() + " at step [" + i + "] Read a string " + readString);
           addThread(this.toString());
           addMessage(readString);
        }
         System.out.println("ReaderThreadTimed: " + Thread.currentThread() + " finished");
       } catch (InterruptedException i) {
         exceptionThrown = true;
         System.out.println("Got message : " + i.getMessage());
         if ( i.getMessage().indexOf("Acquiring read lock took longer than") == -1)
            message = "A time out exception should have been thrown";
       } finally {
         if (!exceptionThrown)
            message = "An exeception should have been thrown";
         lock.getReadLock().release();
       }

     }

  }


  private class WriterThreadTimed extends Thread
  {
     public String message = null;

     public void run()
     {
       boolean exceptionThrown = false;

       try {
        System.out.println("WriterThreadTimed: " + Thread.currentThread() + " about to obtain the lock");
        lock.getWriteLock().acquire(10);

        System.out.println("WriterThreadTimed: " + Thread.currentThread() + " performing " + THREAD_STEPS + " steps");
        for (int i = 0; i < THREAD_STEPS; i++ ) {

           System.out.println("WriterThreadTimed: " + Thread.currentThread() + " at step [" + i + "] Read a string " + readString);
           addThread(this.toString());
           addMessage(readString);
        }
         System.out.println("WriterThreadTimed: " + Thread.currentThread() + " finished");
       } catch (InterruptedException i) {
         exceptionThrown = true;
         System.out.println("Got message : " + i.getMessage());
         if ( i.getMessage().indexOf("Acquiring write lock took longer than") == -1)
            message = "A time out exception should have been thrown";
       } finally {
         if (!exceptionThrown)
            message = "An exeception should have been thrown";
         lock.getWriteLock().release();
       }

     }

  }


    public static final void setUpDebugOptions() throws FrameworkException {

         // may want to change this to take configurations from a file instead
         Hashtable props = new Hashtable();
         props.put("DEBUG_LOG_LEVELS", "all");
         props.put("LOG_FILE", "console");
         Debug.configureFromProperties(props);
         //


   }



}