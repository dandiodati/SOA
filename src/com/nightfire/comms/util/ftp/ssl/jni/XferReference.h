#ifndef __XFERREFERENCE_HEADER__
#define __XFERREFERENCE_HEADER__

#include <string.h>

//running in SOLARIS
#ifdef UNIX
//enum bool{false, true};
#define EA_DLL
#define EA_62
#define EA_SOLARIS251
#define EA_SOLARIS26
#endif

// running on NT
#ifndef UNIX    
#define EA_DLL
#define EA_62
#define EA_WIN32
#endif

#include "xferdefs.h"
#include "xferapi.h"
#include "eaipc.h"

 


class XferReference
{
    

     public:
        // connection context
	xferContext_t* pXferContext;
       
        // connection params 
        xferParms_t*  xParams;

        // transfer info
	transfer_t*    transfer;

        // Constructor
        XferReference();

        //Destructor
        ~XferReference();

};
	

#endif

