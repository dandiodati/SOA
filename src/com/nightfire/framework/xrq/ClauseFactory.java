package com.nightfire.framework.xrq;

import java.util.*;

import com.nightfire.framework.db.*;
import com.nightfire.framework.util.*;
import com.nightfire.framework.resource.*;

import java.util.*;


/**
 * Handles the creation and initialization of all Clause objects.
 *
 */
public class ClauseFactory
{

  private HashMap cachedClauses;

  /**
   * property type for each clause object.
   */
  public static final String CLAUSE_TYPE = "CLAUSE_TYPE";

  /**
   * initializes the factory and all of the known clause objects.
   * (Interates over configured properties and creates each Clause object, at this time.).
   * @param key The property key for this factory.
   * @param type The property type for this factory.
   */
  public ClauseFactory (String key, String type) throws FrameworkException
  {

     if(Debug.isLevelEnabled(Debug.MSG_DATA)) 
        Debug.log(Debug.MSG_DATA,"ClauseFactory: Initializing.");
     
     cachedClauses = new HashMap();
     Map props;
     try {
      props = PersistentProperty.getProperties(key, type);
      if (props == null || props.size() == 0)
         throw new PropertyException("No properties found with key [" + key +"] and type [" + type + "]" );
     } catch (PropertyException pe) {
        String error = "ClauseFactory : Failed to load props : " + pe.getMessage();
        Debug.error(error);
        throw new FrameworkException(error);
     }



     int i = 0;
     String clauseType = PropUtils.getPropertyValue(props, PersistentProperty.getPropNameIteration(CLAUSE_TYPE,i));

     while (clauseType != null ) {

        if(Debug.isLevelEnabled(Debug.MSG_DATA))
            Debug.log(Debug.MSG_DATA, "ClauseFactory : adding Clause [" + clauseType + "]");
        
        addClause(key, clauseType);
        i = i + 1;
        
        clauseType = PropUtils.getPropertyValue(props, PersistentProperty.getPropNameIteration(CLAUSE_TYPE, i));
     }


  }

  /**
   * returns a the clause object associated with the name clauseName.
   * NOTE: two Clause objects with the same name equals the same Clause Object.
   * This safe due to the fact that Clause objects are small and maintain no state.
   * @param clauseName The name of the clause object to obtain.
   * @exception FrameworkException is thrown when the associated clause object can not be found.
   *
   */
  public final Clause getClause(String clauseName) throws FrameworkException
  {
     Clause obj = (Clause) cachedClauses.get(clauseName);

     if (obj == null ) {
        String err =  "ClauseFactory: Unknown Clause Type requested: [" + clauseName + "]";
        Debug.error(err);
        throw new FrameworkException(err);
     }

     if (Debug.isLevelEnabled(Debug.MSG_STATUS) )
        Debug.log(Debug.MSG_STATUS,"ClauseFactory: Returning Clause [" + clauseName +"], Object [" + obj.toString() + "]");

     return (obj);
  }


  /**
   * obtains a clause's properties and creates the clause.
   * used by constructor
   */
  private String addClause(String key, String type) throws FrameworkException
  {
     String className = null;
     
     try {
       Map localProps = PersistentProperty.getProperties(key,type);

       className = PropUtils.getRequiredPropertyValue(localProps, Clause.CLAUSE_CLASS_PROP);
       Clause obj = (Clause) ObjectFactory.create(className);
       obj.initialize(type, localProps);

       if (obj instanceof ClauseObject)
        ((ClauseObject)obj).setClauseFactory(this);
       else
        Debug.warning("Clausefactory: Could not set factory on Clause object [" + className + "], assuming it has it own factory to create sub clauses.");

       cachedClauses.put(type, obj);

     } catch (Exception fe) {
        String error = "ClauseFactory: Failed creating Clause [" + className + "], "  + fe.getMessage() ;
        Debug.error(error);
        throw new FrameworkException(error );
     }

     return className;
  }


  /**
   * prints a status of all clause within this ClauseFactory, which
   * can be used for debugging.
   * NOTE: This is an expensive operation, so only call it when neccessary.
   */
  public String printLoadedClauses()
  {
      Iterator iter = cachedClauses.entrySet().iterator();
      StringBuffer status = new StringBuffer("Clause Factory Known Clauses: \n");

      String name;
      Clause item;

      while ( iter.hasNext() ) {
        Map.Entry entry = (Map.Entry) iter.next();
        name = (String) entry.getKey();
        item = (Clause) entry.getValue();

        status.append("NAME: [" + name + "], OBJECT: [" + item.getClass().getName() + "]");
      }

      return (status.toString());

  }



  
}

