
#include "XferReference.h"

eaVersionData_t eaVer[] =

           {
              {"FtpSslLayerInterconnect","Copyright (c) 2001 by Nightfire Software, Inc."},
              {"Build date", __DATE__ },
			  {"System types", "WinNt or Solaris" },
			  {"Comm-press version", "1.49" },
			  {"All Rights Reserved", "By Nightfire Software, and Comm-press" },
			  {0,0}
           };


   XferReference::XferReference() {
      

       xParams = new xferParms_t;
       memset(xParams,0,sizeof(*xParams) );

       pXferContext = new xferContext_t;
       // need to memset for Easy access api to work correctly
       memset(pXferContext,0, sizeof(*pXferContext) );

      xParams->pXferContext = pXferContext;
      
	  
      // used for version info
      xParams->eaVersionData = eaVer;

      // transfer info struct
      transfer = new transfer_t;
      memset(transfer,0, sizeof(*transfer) );

   }		
   // destructor to destroy this obj
   XferReference::~XferReference() {

      delete xParams->pXferContext;

      delete xParams;

      delete transfer;
   }


   	
