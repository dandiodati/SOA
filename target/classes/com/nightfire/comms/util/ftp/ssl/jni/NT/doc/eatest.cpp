// ----------------------------------------------------------------------------
//
//	This document contains material which is the proprietary property of and
//	confidential to Comm-Press Incorporated.
//	Disclosure outside Comm-Press is prohibited except by license agreement
//	or other confidentiality agreement.
//
//	Copyright (c) 1997 by Comm-Press Incorporated
//	All Rights Reserved
//
// ----------------------------------------------------------------------------
//	Description: Provides entry points for xfer dll.
//
// ----------------------------------------------------------------------------
//					MAINTENANCE	HISTORY
//
// DATE			BY	BUG NO.	DESCRIPTION
// -------- --- ------- -------------------------------------------------------
// 19990108	PA					Initial version
// ----------------------------------------------------------------------------
#include <zaf.hpp>
#include <assert.h>
#include <fcntl.h>
#include "sys/types.h"
#include "sys/stat.h"
#include "io.h"
#include "stdlib.h"
#include "stdio.h"
#include "string.h"
#include "eatest.hpp"

#ifdef  __cplusplus
extern "C" {
extern eaVersionData_t eaVersionData[];
}
#endif

// ----------------------------------------------------------------------------
//  Function called by DLL to send processing messages to application
// ----------------------------------------------------------------------------
void
dllCallBack(xferContext_t* pXferContext, int msgType, char msgLevel,
    char* msg, UINT4 bytesTransferred, UINT4 totalBytes)
{
    PopupWindow*  	popupWindow = (PopupWindow*)0;
	DISPLAY_WINDOW* displayWindow = (DISPLAY_WINDOW*)0;
    cf_t*           pCf = (cf_t*)0;
	char			buf[512];
    assert(pXferContext);
    popupWindow = (PopupWindow*)pXferContext->userObject;
	
    pCf = popupWindow->pCf;

    if (msgType == TEXT_MSG) {
		popupWindow->Show(msg);
        cf_log(pCf, LogInfo, "Thread dllCallBack: msg=[%s]\n", msg);
    }
    else if (msgType == PROGRESS_MSG) {
        sprintf(buf, "bytesTransfered=[%ld]\n", bytesTransferred);
		popupWindow->Show(buf);
        cf_log(pCf, LogInfo, "Thread dllCallBack: %s", buf);
    }

    return;
}   // End dllCallBack

// ---------------------------------------------------------------------------- 
// Driver to test the xfer dll.
// ---------------------------------------------------------------------------- 
int
ZafApplication::Main(void)
{
	PopupWindow*	popupWindow = (PopupWindow*)0;

	LinkMain();
	popupWindow = new PopupWindow();
	zafWindowManager->Add(popupWindow);
	// Cause all windows to close if the main window is closed.
	zafWindowManager->screenID = popupWindow->screenID;
	Control();
	return (0);
}	// End ZafApplication::Main

// ---------------------------------------------------------------------------- 
//	Constructor
// ---------------------------------------------------------------------------- 
PopupWindow::PopupWindow(void) :
//	ZafWindow(0, 0, 350, 100), popup(ZAF_NULLP(ZafPopUpMenu))
	ZafWindow(0, 0, 350, 100)
{

	ZafPositionStruct pos;
	pos.Assign(0, 0, ZAF_PIXEL);
	popup = PopupMenu(pos);
	AddGenericObjects(new ZafStringData(
		"Press the RIGHT mouse button for a MENU"));

	unlink(EAFTP_LOG);
	unlink(EAXFER_LOG);

	if ((pCf = new cf_t) != (cf_t*)0)
		cf_openLog(pCf,
			"",						// Use current dir
			"eatest.log",			// Main log file
			CF_ASCII,				// open mode
			-6,						// Max level of logging
			false,					// Don't use Mutex for logging
			true);					// operate in batch mode

}	// End PopupWindow::PopupWindow

// ---------------------------------------------------------------------------- 
//	Destructor
// ---------------------------------------------------------------------------- 
PopupWindow::~PopupWindow(void)
{
	delete popup; popup = (ZafPopUpMenu*)0;
	delete pXferContext; pXferContext = (xferContext_t*)0;
	delete pT; pT = (transfer_t*)0;

	if (displayWindow) {
		*windowManager - displayWindow;
		delete displayWindow;
		displayWindow = (DISPLAY_WINDOW*)0;
	}

	if (eaFtpLogWindow) {
		*windowManager - eaFtpLogWindow;
		delete eaFtpLogWindow;
		eaFtpLogWindow = (FILE_WINDOW*)0;
	}

	if (eaListFileWindow) {
		*windowManager - eaListFileWindow;
		delete eaListFileWindow;
		eaListFileWindow = (FILE_WINDOW*)0;
	}

	if (eaXferLogWindow) {
		*windowManager - eaXferLogWindow;
		delete eaXferLogWindow;
		eaXferLogWindow = (FILE_WINDOW*)0;
	}
}	// End PopupWindow::~PopupWindow

// ---------------------------------------------------------------------------- 
//	Event loop
// ---------------------------------------------------------------------------- 
ZafEventType
PopupWindow::Event(const ZafEventStruct &event)
{
	static char*		func = "PopupWindow::Event";
	char*				ptr = (char*)0;
	char*				pFileSpec = (char*)0;
	unsigned char*		randomArray = (unsigned char*)"abcdefghijklmnop";
//	FILE_WINDOW*		eaListFileWindow = (FILE_WINDOW*)0;
//	FILE_WINDOW*		eaFtpLogWindow = (FILE_WINDOW*)0;
//	FILE_WINDOW*		eaXferLogWindow = (FILE_WINDOW*)0;
	char				serverFileSpec[256];
	long				lastCode;
	ZafEventType		ccode = LogicalEvent(event);
	ZafPositionStruct	pos;
	xferParms_t			xferParms, *pX = &xferParms;
#if defined(WIN32)
	char*				clientSlash = "\\";
#elif defined(UNIX)
	char*				clientSlash = "/";
#endif

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	switch (ccode) {

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case S_INITIALIZE:							// Do once on object creation
		if ((pXferContext = new xferContext_t) == (xferContext_t*)0) {
			msgWindow = new ZafMessageWindow(
				ZAF_ITEXT("Error allocating xfer context"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"Could not allocate xfer context");
			eventManager->Put(ZafEventStruct(S_CLOSE));
			ZafErrorStub::Beep();
			msgWindow->Control();
			exit(1);
		}
		pXferContext->userCallback = dllCallBack;
		pXferContext->userObject = this;
		if ((pT = new transfer_t) == (transfer_t*)0) {
			msgWindow = new ZafMessageWindow(
				ZAF_ITEXT("Error allocating transfer"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"Could not allocate transfer");
			eventManager->Put(ZafEventStruct(S_CLOSE));
			ZafErrorStub::Beep();
			msgWindow->Control();
			exit(1);
		}
		else if ((pT->name = new char[strlen("Test transfer")+1]) == (char*)0) {
			msgWindow = new ZafMessageWindow(
				ZAF_ITEXT("Error allocating transfer name"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"Could not allocate transfer name");
			eventManager->Put(ZafEventStruct(S_CLOSE));
			ZafErrorStub::Beep();
			msgWindow->Control();
			exit(1);
		}
		sprintf(pT->name, "%s", "Test transfer");
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case L_BEGIN_ESCAPE:				// Convert mouse coords to screen pos'n
		pos = ConvertToScreenPosition(event.position);
		popup->zafRegion.top = pos.line;
		popup->zafRegion.left = pos.column;
		zafWindowManager->Add(popup);
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case START_SSL_EVENT:				// Use SSL ftp connection
	case START_EVENT:					// Use non-SSL connection
		if (!ReadData())
			break;

		// --------------------------------------------------------------------
		//	Instantiate XFER object, as needed
		// --------------------------------------------------------------------
		if (pXferContext->xfer) {
			Show("Already connected...");
			return(XFER_SUCCESS);
		}

		Show("Creating XFER object instance");

		//	Entries required by all network styles
		pX->pXferContext = pXferContext;		// Address of context
		pX->eaVersionData = eaVersionData;		// Address of eaVersionData
		pX->networkName = " ";					// Server Name - N/A here
		pX->networkStyle = style;				// From data file
		pX->clientSlash = clientSlash;			// Platform-dependent
		pX->tmpDir = tmpDir;					// From data file

		//	Entries used only for FTP-based network styles
		pX->storeUnique = 0;					// N/A here

		//	Entries used only by the CPFTP network style
		pX->siteDelay = 0;						// N/A here

		//	Entries used only by EDI_INT (SMTP/POP3) network styles
		pX->emailAddress = " ";					// N/A for this FTP example
		pX->smtpIp = " ";						// N/A for this FTP example
		pX->popIp = " ";						// N/A for this FTP example
		pX->popUserId = " ";					// N/A for this FTP example
		pX->popPasswd = " ";					// N/A for this FTP example
		pX->randomArray = randomArray;			// N/A for this FTP example

		//	Entries used only for certificate-based security/encryption
		pX->ediName = ediName;					// From data file
		pX->runtimeDir = runtimeDir,			// From data file
		pX->sslRuntimeDir = ignRuntimeDir,		// From data file
		pX->securityDir = " ";					// N/A here
		pX->aliasFileName = "alias.tbl";		// N/A here really...
		pX->exportFileName = "export.fil";		// ditto
		pX->privateFileName = "private.fil";	// ditto
		pX->localPrivateFileName = "priv.fil";	// ditto
		pX->secFileName = "header.def";			// ditto
		pX->approvalCode = " ";					// N/A here

		// Entries pertaining to log-file generation
		pX->compressLogDir = ".";				// pwd
		pX->ftpLogFileSpec = EAFTP_LOG;			// Log for FTP session
		pX->ftpLogLevel = -6;					// Maximum logging
		pX->xferLogFileSpec = EAXFER_LOG;		// Log for DLL logging
		pX->xferLogLevel = -6;					// Maximum logging
		pX->multiThreaded = false;				// Not for this exercise

		// --------------------------------------------------------------------
		//	Call DLL to create FTP object
		// --------------------------------------------------------------------
		Show("Calling xferCreate in DLL API");
		if (!xferCreate(pX)) {					// Create new FTP object
			sprintf(msgBuf, "%s\n%s\n", "Error creating FTP object for network",
				pXferContext->lastMsg);
			Show(msgBuf);
			msgWindow = new ZafMessageWindow(
				ZAF_ITEXT("CREATE ERROR MESSAGE"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				ZAF_ITEXT(msgBuf));
			ZafErrorStub::Beep();
			msgWindow->Control();
			break;
		}

		// --------------------------------------------------------------------
		//	Establish connection to FTP server
		// --------------------------------------------------------------------
		Show("Calling xferConnect in DLL API");
		if (!xferConnect(pX->pXferContext,		// Current context
				host,							// Domain name or IP address
				"",								// Secondary name/IP
				controlPort,					// FTP port on server
				(ccode == START_SSL_EVENT ? SSL : false),	// SSL or not
				passive)) {					// Passive mode or not

			sprintf(msgBuf, "%s\n%s\n", "Error connecting to network",
				pXferContext->lastMsg);			// Last message from FTP object
			Show(msgBuf);
			msgWindow = new ZafMessageWindow(
				ZAF_ITEXT("CONNECT ERROR MESSAGE"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				ZAF_ITEXT(msgBuf));
		}
		else
			msgWindow = new ZafMessageWindow(
				ZAF_ITEXT("CONNECT SUCCESSFUL"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));

		ShowFile(EAFTP_LOG);
		ShowFile(EAXFER_LOG);

		ZafErrorStub::Beep();
		msgWindow->Control();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case LOGIN_EVENT:							// Login to FTP server
		if (!pXferContext->xfer || !pXferContext->lastMsg)
			msgWindow = new ZafMessageWindow(ZAF_ITEXT("NO FTP OBJECT"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"Please Create an FTP object before attempting to Login!");
		else {
			if (!ReadData())
				break;

			Show("Calling xferLogin in DLL API");
			if (!xferLogin(	pXferContext,			// Current context
							userId,					// Login using this userId
							passwd,					// Login using this password
							(char*)0,				// Not changing password
							false)) {				// Not logging into Proxy
				Show(pXferContext->lastMsg);
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("LOGIN ERROR MESSAGE"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
			}
			else
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("LOGIN SUCCESSFUL"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
		}
		ShowFile(EAFTP_LOG);
		ShowFile(EAXFER_LOG);

		ZafErrorStub::Beep();
		msgWindow->Control();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case GET_EVENT:								// Get a file
		if (!pXferContext->xfer || !pXferContext->lastMsg)
			msgWindow = new ZafMessageWindow(ZAF_ITEXT("NO FTP OBJECT"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"Please Create an FTP object before attempting to Get a file!");
		else {
			if (!ReadData())
				break;

			pT->send.transmit = false;
			pT->receive.transmit = true;
			Show("Calling xferGet in DLL API");
			if (!xferGet(	pXferContext,
							pT->receive.clientFileSpec,
							pT->receive.serverFileSpec,
							pT->receiveParms.ascii,
							false)) {			// Not restarting a previous Get
				Show(pXferContext->lastMsg);
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("GET ERROR MESSAGE"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
			}
			else
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("GET SUCCESSFUL"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					xferGetLastMsg(pXferContext, &lastCode));
		}
		ShowFile(EAFTP_LOG);
		ShowFile(EAXFER_LOG);

		ZafErrorStub::Beep();
		msgWindow->Control();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case LIST_EVENT:							// List files on server
		if (!pXferContext->xfer || !pXferContext->lastMsg)
			msgWindow = new ZafMessageWindow(ZAF_ITEXT("NO FTP OBJECT"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"Please Create an FTP object before attempting to List files!");
		else {
			if (!ReadData())
				break;

			unlink(lstFileSpec);
			Show("Calling xferList in DLL API");
			if (!xferList(	pXferContext,
							lstFileSpec,
							mode,
							cmdString,
							SHORT_LS,
							"",					// No filter on UserId
							XFER_SEND,			// No filter on sender/receiver
							"")) {				// No filter on userClass
				Show(pXferContext->lastMsg);
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("LIST ERROR MESSAGE"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
			}
			else
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("LIST SUCCESSFUL"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
		}
		ShowFile(lstFileSpec);
		ShowFile(EAFTP_LOG);
		ShowFile(EAXFER_LOG);

		ZafErrorStub::Beep();
		msgWindow->Control();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case PUT_EVENT:								// Send a file
		if (!pXferContext->xfer || !pXferContext->lastMsg)
			msgWindow = new ZafMessageWindow(ZAF_ITEXT("NO FTP OBJECT"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"Please Create an FTP object before attempting to Put a file!");
		else {
			if (!ReadData())
				break;

			// ----------------------------------------------------------------
			//	Construct destination filename from full filespec.
			//	If no destination filespec, use clientFileSpec as default
			//	Destination filename is basename of destination fileSpec
			// ----------------------------------------------------------------
			ptr = pT->send.serverFileSpec;
			pFileSpec = (ptr && strlen(ptr) ? ptr : pT->send.clientFileSpec);
			if ((ptr = strrchr(pFileSpec, clientSlash[0])) != (char*)0)
				pFileSpec = ptr + 1;				// Skip slash

			// ----------------------------------------------------------------
			//	Build destination file-spec; this is overridden for some
			//	networks to allow for network-specific junk in the Put command!
			// ----------------------------------------------------------------
			Show("Calling xferBuildDestFileSpec in DLL API");
			xferBuildDestFileSpec(pXferContext, pT, pT->send.clientFileSpec,
				"A text description can go here", pFileSpec, serverFileSpec);

			pT->send.transmit = true;
			pT->receive.transmit = false;
			Show("Calling xferPut in DLL API");
			if (!xferPut(	pXferContext,
							pT->send.clientFileSpec,
							serverFileSpec,
							pT->sendParms.ascii,
							false,				// No restarting a previous Put
							(char*)0)) {		// Not specifying server file
				Show(pXferContext->lastMsg);
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("PUT ERROR MESSAGE"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
			}
			else
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("PUT SUCCESSFUL"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
		}
		ShowFile(EAFTP_LOG);
		ShowFile(EAXFER_LOG);

		ZafErrorStub::Beep();
		msgWindow->Control();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case END_EVENT:						// End ftp session
		if (!pXferContext->xfer || !pXferContext->lastMsg)
			msgWindow = new ZafMessageWindow(ZAF_ITEXT("NO FTP OBJECT"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"No FTP session exists to End!");
		else {
			if (!ReadData())
				break;

			Show("Calling xferEnd in DLL API");
			if (!xferEnd(pXferContext)) {
				Show(pXferContext->lastMsg);
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("END SESSION ERROR MESSAGE"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(pXferContext->lastMsg ?
						xferGetLastMsg(pXferContext, &lastCode) :
						"Error ending session"));
			}
			else
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("DISCONNECT SUCCESSFUL"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					"Session ended!");
		}
		ShowFile(EAFTP_LOG);
		ShowFile(EAXFER_LOG);

		ZafErrorStub::Beep();
		msgWindow->Control();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case GET_LAST_MSG_EVENT:			// Get last error or status message
		if (!pXferContext->xfer || !pXferContext->lastMsg)
			msgWindow = new ZafMessageWindow(ZAF_ITEXT("NO FTP OBJECT"),
				ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
				"No FTP session exists from which to get a last message!");
		else {
			if (!ReadData())
				break;

			Show("Calling xferGetlastMsg in DLL API");
			if (pXferContext->lastMsg) {
				Show(pXferContext->lastMsg);
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("Last message received"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					ZAF_ITEXT(xferGetLastMsg(pXferContext, &lastCode)));
			}
			else {
				Show("No current context or last Message found");
				msgWindow = new ZafMessageWindow(
					ZAF_ITEXT("No message exists"),
					ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK,
					"No message exists - must start session first!");
			}
		}
		ZafErrorStub::Beep();
		msgWindow->Control();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case BEEP_EVENT:					// Leftover from example program...
		ZafErrorStub::Beep();
		break;

	// ------------------------------------------------------------------------ 
	// ------------------------------------------------------------------------ 
	case EXIT_EVENT:
		exit(0);

	default:
		ccode = ZafWindow::Event(event);
	}
	return (ccode);
}	// End PopupWindow::Event

// ---------------------------------------------------------------------------- 
//	Display text in child DISPLAY_WINDOW
// ---------------------------------------------------------------------------- 
void
PopupWindow::Show(char* msg)
{
	if (!msg)
		return;
	else if (!displayWindow) {
		displayWindow = new DISPLAY_WINDOW(this, pCf, 5, 5, 50, 10);
		*windowManager + displayWindow;
	}
	displayWindow->Show(msg);
}	// End PopupWindow::Show

// ---------------------------------------------------------------------------- 
//	Display contents of a file in a window
// ---------------------------------------------------------------------------- 
void
PopupWindow::ShowFile(char* fileSpec)
{
	if (!stricmp(fileSpec, EAFTP_LOG)) {					// FTP log
		if (!eaFtpLogWindow) {								// No window yet
			eaFtpLogWindow = new FILE_WINDOW(this, fileSpec, 10, 10, 50, 10);
			*windowManager + eaFtpLogWindow;
		}
		eaFtpLogWindow->DisplayFile();
	}
	else if (!stricmp(fileSpec, EAXFER_LOG)) {				// XFER log
		if (!eaXferLogWindow) {								// No window yet
			eaXferLogWindow = new FILE_WINDOW(this, fileSpec, 15, 15, 50, 10);
			*windowManager + eaXferLogWindow;
		}
		eaXferLogWindow->DisplayFile();
	}
	else {
		if (!eaListFileWindow) {								// No window yet
			eaListFileWindow = new FILE_WINDOW(this, fileSpec, 20, 20, 50, 10);
			*windowManager + eaListFileWindow;
		}
		eaListFileWindow->DisplayFile();
	}
}	// End PopupWindow::ShowFile

// ---------------------------------------------------------------------------- 
//	Popup menu
// ---------------------------------------------------------------------------- 
ZafPopUpMenu*
PopupWindow::PopupMenu(ZafPositionStruct position)
{
	ZafPopUpMenu* menu = (ZafPopUpMenu*)0;
	ZafPopUpItem* item = (ZafPopUpItem*)0;

	menu = new ZafPopUpMenu(position.column, position.line);
	menu->SetCoordinateType(ZAF_PIXEL);

	item = new ZafPopUpItem(ZAF_ITEXT("Create FTP context and connect"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(START_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("Create FTP context and connect(SSL)"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(START_SSL_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("Login"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(LOGIN_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("List files on server"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(LIST_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("Get file"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(GET_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("Put file"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(PUT_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("EndSession"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(END_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("GetLastMsg"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(GET_LAST_MSG_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("Beep"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(BEEP_EVENT);
	menu->Add(item);

	item = new ZafPopUpItem(ZAF_ITEXT("Exit"));
	item->SetSendMessageWhenSelected(true);
	item->SetValue(EXIT_EVENT);
	menu->Add(item);

	return (menu);
};	// End PopupWindow::PopupMenu

// ---------------------------------------------------------------------------- 
//	Read data file for parms
// ---------------------------------------------------------------------------- 
int
PopupWindow::ReadData(void)
{
	int		nTmp = 0;

	fp = fopen("eatest.dat", "r");
	sprintf(msgBuf, "Could not open file eatest.dat\n");
	if (fp == (FILE*)0) {
		msgWindow = new ZafMessageWindow(
			ZAF_ITEXT("ERROR OPENING EATEST.DAT"),
			ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK, msgBuf);
		ZafErrorStub::Beep();
		msgWindow->Control();
		return(0);
	}

	GetString(host,				"host");
	GetString(userId,			"userId");
	GetString(passwd,			"passwd");
	GetString(ediName,			"ediName");
	GetBool(&passive,			"passive");
	GetBool(&SSL,				"SSL");

	GetString(sendFileSpec,		"sendFileSpec");
	GetString(recvFileSpec,		"recvFileSpec");
	GetString(cmdString,		"cmdString");
	GetString(userClass,		"userClass");

	GetString(runtimeDir,		"runtimeDir");
	GetString(ignRuntimeDir,	"ignRuntimeDir");
	GetString(tmpDir,			"tmpDir");
	GetString(lstFileSpec,		"lstFileSpec");
	GetString(mode,				"mode");

	GetBool(&compression,		"compression");
	GetBool(&filter,			"filter");
	GetBool(&ascii,				"ascii");
	GetBool(&edi,				"edi");
	GetBool(&secure,			"secure");
	GetBool(&crlf,				"crlf");
	GetBool(&append,			"append");
	GetBool(&autoext,			"autoext");

	GetInt(&nTmp,				"controlPort");
	controlPort = (unsigned short)nTmp;

	GetInt(&nTmp,				"dataPort");
	dataPort = (unsigned short)nTmp;

	GetInt(&style,				"style");

	(void)fclose(fp);

	fp = fopen("eatest.bak", "w");
	fprintf(fp, "host =             %s\n", host);
	fprintf(fp, "userId =           %s\n", userId);
	fprintf(fp, "passwd =           %s\n", passwd);
	fprintf(fp, "userClass =        %s\n", userClass);
	fprintf(fp, "recvFileSpec =     %s\n", recvFileSpec);
	fprintf(fp, "sendFileSpec =     %s\n", sendFileSpec);
	fprintf(fp, "ediName =          %s\n", ediName);
	fprintf(fp, "passive =          %s\n", IS_Y_OR_N(passive));
	fprintf(fp, "SSL =              %s\n", IS_Y_OR_N(SSL));
	fprintf(fp, "cmdString =        %s\n", cmdString);
	fprintf(fp, "runtimeDir =       %s\n", runtimeDir);
	fprintf(fp, "tmpDir =           %s\n", tmpDir);
	fprintf(fp, "lstFileSpec =      %s\n", lstFileSpec);
	fprintf(fp, "mode =             %s\n", mode);

	fprintf(fp, "compression =      %s\n", IS_Y_OR_N(compression));
	fprintf(fp, "filter =           %s\n", IS_Y_OR_N(filter));
	fprintf(fp, "ascii =            %s\n", IS_Y_OR_N(ascii));
	fprintf(fp, "edi =              %s\n", IS_Y_OR_N(edi));
	fprintf(fp, "secure =           %s\n", IS_Y_OR_N(secure));
	fprintf(fp, "crlf =             %s\n", IS_Y_OR_N(crlf));
	fprintf(fp, "append =           %s\n", IS_Y_OR_N(append));
	fprintf(fp, "autoext =          %s\n", IS_Y_OR_N(autoext));

	fprintf(fp, "controlPort =      %d\n", (int)controlPort);
	fprintf(fp, "dataPort =         %d\n", (int)dataPort);
	fprintf(fp, "style =            %d\n", (int)style);
	(void)fclose(fp);

	pT->send.transmit = false;
	pT->send.clientFileSpec = sendFileSpec;
	pT->send.serverFileSpec = cmdString;
	pT->send.userClass = userClass;
	pT->send.userId = userId;
	pT->sendParms.compress = compression;
	pT->sendParms.secure = secure;
	pT->sendParms.filter = filter;
	pT->sendParms.ascii = ascii;
	pT->sendParms.crlf = crlf;
	pT->sendParms.edi = edi;

	pT->receive.transmit = false;
	pT->receive.clientFileSpec = recvFileSpec;
	pT->receive.serverFileSpec = cmdString;
	pT->receive.userClass = userClass;
	pT->receive.userId = userId;
	pT->receiveParms.append = append;
	pT->receiveParms.autoext = autoext;
	pT->receiveParms.ascii = ascii;
	pT->receiveParms.edi = edi;

	return(1);
}	// End PopupWindow::ReadData

// ---------------------------------------------------------------------------- 
//	Read one line from file and store
// ---------------------------------------------------------------------------- 
void
PopupWindow::GetString(char* pString, char* msg)
{
	char*	ptr = (char*)0;

	memset(msgBuf, 0, 128);
	if (fgets(msgBuf, 128, fp) == (char*)0) {
		sprintf(msgBuf, "%s %s\n", "Error reading string data for", msg);
		msgWindow = new ZafMessageWindow(
			ZAF_ITEXT("ERROR OPENING EATEST.DAT"),
			ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK, msgBuf);
		ZafErrorStub::Beep();
		msgWindow->Control();
		exit(1);
	}

	if ((ptr = strchr(msgBuf, ';')) != (char*)0)	// Search for terminator
		*ptr = (char)0;								// NULL out if found

	sprintf(pString, "%s", msgBuf);
	
}	// End PopupWindow::GetString

// ---------------------------------------------------------------------------- 
//	Read one line from file and store
// ---------------------------------------------------------------------------- 
void
PopupWindow::GetBool(bool* pBool, char* msg)
{
	memset(msgBuf, 0, 128);
	if (fgets(msgBuf, 128, fp) == (char*)0) {
		sprintf(msgBuf, "%s %s\n", "Error reading bool data for", msg);
		msgWindow = new ZafMessageWindow(
			ZAF_ITEXT("ERROR READING EATEST.DAT"),
			ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK, msgBuf);
		ZafErrorStub::Beep();
		msgWindow->Control();
		exit(1);
	}

	*pBool = (msgBuf[0] == 'Y' || msgBuf[0] == 'y');
}	// End PopupWindow::GetBool

// ---------------------------------------------------------------------------- 
//	Read one line from file and store
// ---------------------------------------------------------------------------- 
void
PopupWindow::GetInt(int* pInt, char* msg)
{
	char*	ptr = (char*)0;
	memset(msgBuf, 0, 128);
	if (fgets(msgBuf, 128, fp) == (char*)0) {
		sprintf(msgBuf, "%s %s\n", "Error reading int data for", msg);
		msgWindow = new ZafMessageWindow(
			ZAF_ITEXT("ERROR READING EATEST.DAT"),
			ZAF_HAND_ICON, ZAF_DIALOG_OK, ZAF_DIALOG_OK, msgBuf);
		ZafErrorStub::Beep();
		msgWindow->Control();
		exit(1);
	}

	if ((ptr = strchr(msgBuf, ';')) != (char*)0)	// Search for terminator
		*ptr = (char)0;								// NULL out if found
	*pInt = (int)atoi(msgBuf);
}	// End PopupWindow::GetInt

// ---------------------------------------------------------------------------- 
// ---------------------------------------------------------------------------- 
DISPLAY_WINDOW::DISPLAY_WINDOW(ZafWindowObject* parentPtr, cf_t* pCfPtr,
	int left, int top, int width, int height) :
		ZafWindow(left, top, width, height)
{
	parent = parentPtr;								// Ptr to main window

	text = new ZafText(left, top, width, height, "", -1); // Create text object
	text->SetRegionType(ZAF_AVAILABLE_REGION);		// to fill window
	Add(text);

	stringData = new ZafStringData("", -1);			// Create string object
	text->SetStringData(stringData);				// to put in text object
	pCf = pCfPtr;

	AddGenericObjects(new ZafStringData(
		"Press the RIGHT mouse button for a MENU"));
	SetText("Real-time message display");			// Give title to window
	text->Add(new ZafScrollBar(ZAF_VERTICAL_SCROLL));
	text->Add(new ZafScrollBar(ZAF_HORIZONTAL_SCROLL));
}	// End DISPLAY_WINDOW constructor

// ---------------------------------------------------------------------------- 
//	Handle user input (we do nothing but pass it on).
// ---------------------------------------------------------------------------- 
DISPLAY_WINDOW::~DISPLAY_WINDOW(void)
{
	((PopupWindow*)parent)->displayWindow = (DISPLAY_WINDOW*)0;
}

// ---------------------------------------------------------------------------- 
//	Handle user input (we do nothing but pass it on).
// ---------------------------------------------------------------------------- 
ZafEventType
DISPLAY_WINDOW::Event(const ZafEventStruct &event)
{
	if (!parent)
		parent = Parent();
	return(ZafWindow::Event(event));
}

// ---------------------------------------------------------------------------- 
//	Display the contents of the specified file.
// ---------------------------------------------------------------------------- 
void
DISPLAY_WINDOW::Show(char* msg)
{
	int i = 0;
	int j = 0;

	if (!msg)
		return;
	memset(buf, 0, sizeof(buf));
	for (i = 0, j = 0; i < (int)strlen(msg); i++) {
		if (msg[i] == '\n') {
			if (i == 0) {
				buf[j] = '\r'; 
				j++;
				buf[j] = '\n'; 
				j++;
			}
			else if (msg[i-1] == '\r') {
				buf[j] = msg[i];
				j++;
			}
			else {
				buf[j] = '\r'; 
				j++;
				buf[j] = '\n'; 
				j++;
			}
		}
		else {
			buf[j] = msg[i];
			j++;
		}
	}
	stringData->Append(buf);
	if (buf[strlen(buf)-1] != '\n')
		stringData->Append("\r\n");
	text->SetStringData(stringData);					// Add text to window
	text->SetCursorOffset(strlen(stringData->Text()));	// Scroll text into view
}	// End DISPLAY_WINDOW::Show

// ---------------------------------------------------------------------------- 
// ---------------------------------------------------------------------------- 
FILE_WINDOW::FILE_WINDOW(ZafWindowObject* parentPtr, const char* pFileSpec,
	int left, int top, int width, int height) :
		ZafWindow(left, top, width, height)
{
	ZafWindowObject* object = (ZafWindowObject*)0;

	parent = parentPtr;								// Ptr to main window
	sprintf(fileSpec, "%s", pFileSpec);

	text = new ZafText(left, top, width, height);	// Create text object
	text->SetRegionType(ZAF_AVAILABLE_REGION);		// to fill window
	stringData = new ZafStringData("", -1);			// Create string object
	text->SetStringData(stringData);				// to put in text object

	AddGenericObjects(new ZafStringData( "FILE DISPLAY WINDOW"));
	sprintf(buf, "DISPLAY OF CONTENTS OF FILE %s", fileSpec);
	SetText(buf);									// Give title to window

	text->Add(new ZafScrollBar(ZAF_VERTICAL_SCROLL));	// Add scroll bars
	text->Add(new ZafScrollBar(ZAF_HORIZONTAL_SCROLL));
	Add(text);										// Add Text object to window
}	// End FILE_WINDOW constructor

// ---------------------------------------------------------------------------- 
// ---------------------------------------------------------------------------- 
FILE_WINDOW::~FILE_WINDOW(void)
{
	if (!stricmp(fileSpec, EAFTP_LOG))
		((PopupWindow*)parent)->eaFtpLogWindow = (FILE_WINDOW*)0;
	else if (!stricmp(fileSpec, EAXFER_LOG))
		((PopupWindow*)parent)->eaXferLogWindow = (FILE_WINDOW*)0;
	else
		((PopupWindow*)parent)->eaListFileWindow = (FILE_WINDOW*)0;
}

// ---------------------------------------------------------------------------- 
//	Handle user input (we do nothing but pass it on).
// ---------------------------------------------------------------------------- 
ZafEventType
FILE_WINDOW::Event(const ZafEventStruct &event)
{
	return(ZafWindow::Event(event));
}

// ---------------------------------------------------------------------------- 
//	Display the contents of the specified file.
// ---------------------------------------------------------------------------- 
void
FILE_WINDOW::DisplayFile(void)
{
	int len = 0;

	stringData->Clear();								// Clear previous text

	sprintf(buf, "Display of file %s\r\n", fileSpec);	// Text heading
	stringData->Append(buf);
	stringData->Append("\r\n");

	if ((fp = fopen(fileSpec, "r")) == (FILE*)0) {
		sprintf(buf, "Cannot open file %s\r\n%s\r\n", fileSpec,strerror(errno));
		stringData->Append(buf);
		return;
	}

	while (fgets(buf, 512 - 3, fp)) {
		if ((len = strlen(buf)) > 0) {
			buf[len-1] = (char)0;
			strcat(buf, "\r\n");
			stringData->Append(buf);
		}
	}
	fclose(fp);
	text->SetStringData(stringData);					// Add text to window
	text->SetCursorOffset(strlen(stringData->Text()));	// Scroll text into view
}	// End FILE_WINDOW::DisplayFile
