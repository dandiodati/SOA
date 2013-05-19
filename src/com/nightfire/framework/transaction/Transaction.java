/**
 * Copyright (c) 1999 Nightfire Software, Inc. All rights reserved.
 * 
 */

package com.nightfire.framework.transaction;

import java.util.*;

import com.nightfire.framework.resource.*;
import com.nightfire.framework.util.Debug;


/**
 * A Transaction object represents.. well, a transaction.  Each
 * transaction roughly corresponds to a transaction.  The transaction
 * does not span multiple Java VMs.
 *
 * <h3> Usage </h3>  <p>
 * A typical use of transaction looks like this:
 *   <li> Create a Transaction object 
 *   <li> Add changed transaction participants by calling tx.add(participant)
 *   <li> Call tx.commit() to save the objects and commit 
 *     changes to DB or whatever. Alternatively, call tx.rollback() 
 *
 * <h3> Implementing a new Transaction class </h3> <p>
 * To implement a new transaction, you need to do the following:
 *   <li> <u> Implement Tranaction class.</u> Subclass the Transaction class
 *            and implement the abstract methods.
 *   <li> <u> Implement a SaveContext.</u> See the DBSaveContext class
 *            for DB operations.
 *   <li> <u> Implement TransactionParticipants.</u> For each type of 
 *            object that participates in the transaction, make it 
 *            implement the TransactionParticipant interface.  The commit()
 *            method should implement the save operation (like updating a 
 *            DB row). The rollback() should implement the equivalent 
 *            undo method (like reloading from the DB).
 *
 */
public abstract class Transaction
{
    /**
     * Constructor.  It generates an ID for the transaction.  The 
     * transaction ID is a unique number within the VM.
     */
    protected Transaction()
    {
	id = generateID();
    }

    /**
     * @return A unique ID for the transaction within this Java VM.
     */
    public final int getID() {
	return id;
    }

    /**
     * Add a participant to the transaction.
     */
    public void add(TransactionParticipant participant)
    {
        Debug.log(Debug.NORMAL_STATUS, "Adding " + participant +
		  " to the participant list in transaction ["+ 
		  getID() + "]");
        // Add the participant to participantList vector
        // if it hasn't been added already.
        // Add an object only once to prevent saving an object multiple 
        // times.
        if (! participantList.contains(participant) ) {
            participantList.addElement(participant);
        }
    }	
    
    /**
     * Get a SaveConext (like a database connection) before 
     * committing information.
     */
    protected abstract SaveContext acquireSaveContext()
	throws ResourceException;


    /**
     * Release a previously acquired SaveContext.
     */
    protected abstract void releaseSaveContext(SaveContext sc)
	throws ResourceException;

    /**
     * A hook to be executed before committing information to
     * SaveContext.
     */
    protected synchronized void beforeCommit() {
    }

    /**
     * A hook to be executed after committing information to
     * SaveContext.
     */
    protected synchronized void afterCommit() {
        participantList = new Vector(); // cleanup
    }

    /**
     * A hook to be executed before rolling back changes to
     * participants.
     */
    protected synchronized void beforeRollback() {
    }

    /**
     * A hook to be executed after rolling back changes to
     * participants.
     */
    protected synchronized void afterRollback(){
    }


    /**
     * Commit changes.  This method does the following:
     *  <li> Get a save context by calling acquireSaveContext().
     *  <li> Save each participant by calling save() on each
     *       participant. If any of the saves fail, call this.rollback()
     *  <li> Commit changes by calling SaveContext.commit().
     *  <li> Release the SaveContext by calling releaseSaveContext().
     */
    public synchronized void commit() 
    throws CommitFailedException
    {
        Debug.log(Debug.NORMAL_STATUS, "Commit called on transaction ["+ 
		  getID() + "]");
	SaveContext saveCon = null;
	try {
	    Debug.log(Debug.NORMAL_STATUS, " transaction ["+ 
		  getID() + "]: Acquiring savecontext (Db connection?)");
	    saveCon = acquireSaveContext();
	    Debug.log(Debug.NORMAL_STATUS, " transaction ["+ 
		  getID() + "]: savecontext is:[" + saveCon + "]");
	} catch (ResourceException e) {
	    throw new CommitFailedException(e);
	}
	try {
	    beforeCommit();
	    saveAll(saveCon);
	    saveCon.commit();
	    afterCommit();
	} catch (SaveFailedException e) {
	    throw new CommitFailedException(e);
	} finally {
	    try {
		releaseSaveContext(saveCon);
	    } catch (ResourceException e2) {
		Debug.log(this, Debug.ALL_ERRORS, 
			  "Unable to release resource: " + saveCon);
	    }
	}
    }

    /**
     * Rollback changes.
     */
    protected final void rollback(SaveContext saveCon) 
    throws RollbackFailedException
    {
	beforeRollback();
	try {
	    undoAll();
	} catch (UndoFailedException e) {
				// TODO What to do here??
	}
	saveCon.rollback();
	afterRollback();
    }


    /**
     * Rollback changes.
     */
    public final void rollback() 
    throws RollbackFailedException
    {
	beforeRollback();
	try {
	    undoAll();
	} catch (UndoFailedException e) {
				// TODO What to do here??
	}
	afterRollback();
    }

    /**
     * Save all participants in this transaction.
     */
    protected void saveAll(SaveContext saveCon) 
    throws SaveFailedException
    {
	Debug.log(Debug.NORMAL_STATUS, "Saving all participants in transaction ["+ 
		  getID() + "]");
	for(Enumeration enumerator= participantList.elements();
	    enumerator.hasMoreElements(); ) {
	    TransactionParticipant participant = 
		(TransactionParticipant)enumerator.nextElement();
	    Debug.log(Debug.NORMAL_STATUS, "Saving participant: [" +
		      participant + "] in transaction ["+ 
		      getID() + "]");
	    participant.save(saveCon);
	    Debug.log(Debug.NORMAL_STATUS, "Saved participant: [" +
		      participant + "] in transaction ["+ 
		      getID() + "]");
	}	
    }

    /**
     * Undo changes to each participant.
     */
    protected void undoAll() 
	throws UndoFailedException
    {
	for(Enumeration enumerator= participantList.elements();
	    enumerator.hasMoreElements(); ) {
	    TransactionParticipant participant = 
		(TransactionParticipant)enumerator.nextElement();
	    participant.undo(this);
	}	
    }


    /**
     * Generate a unique ID for the transaction that's global
     * within this VM.
     */
    private static synchronized int generateID() {
	return globalTxID++;
    }


    /**
     * The list of participants in this transaction. Used to 
     * later save or undo each object.
     */
    protected Vector participantList = new Vector();
    /**
     * The unique ID of this transaction. 
     */
    private int id = 0;
    /**
     * The global transaction id counter used to generate 
     * transaction id's.
     */
    private static int globalTxID = 1;
}
