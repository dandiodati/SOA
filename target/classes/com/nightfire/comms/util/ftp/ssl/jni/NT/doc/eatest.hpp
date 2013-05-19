// ------------------------------------------------------------------------
//
//	This document contains material which is the proprietary property of and
//	confidential to Comm-Press Incorporated.
//	Disclosure outside Comm-Press is prohibited except by license agreement
//	or other confidentiality agreement.
//
//	Copyright (c) 1997 by Comm-Press Incorporated
//	All Rights Reserved
//
// ------------------------------------------------------------------------
//	Description: Provides entry points for xfer dll.
//
// ------------------------------------------------------------------------
//					MAINTENANCE	HISTORY
//
// DATE			BY	BUG NO.	DESCRIPTION
// -------- --- ------- ---------------------------------------------------
// 19990108	PA					Initial version
// ------------------------------------------------------------------------
#include "zaf.hpp"				// For Zinc Application Framework GUI
#include "xferapi.h"
#include "xferdefs.h"
#include "eaipc.h"				// TEXT_MSG, PROGRESS_MSG, etc.
#include "cf.hpp"				// cf_t object not included with DLL...

#define		EAFTP_LOG			"eaftp.log"
#define		EAXFER_LOG			"eaxfer.log"

const ZafEventType START_EVENT				= 28001;
const ZafEventType START_SSL_EVENT			= 28002;
const ZafEventType LOGIN_EVENT				= 28003;
const ZafEventType LIST_EVENT				= 28004;
const ZafEventType GET_EVENT				= 28005;
const ZafEventType PUT_EVENT				= 28006;
const ZafEventType END_EVENT				= 28007;
const ZafEventType GET_LAST_MSG_EVENT		= 28008;
const ZafEventType BEEP_EVENT				= 28009;
const ZafEventType EXIT_EVENT				= 28010;

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
class ZAF_EXPORT DISPLAY_WINDOW : public ZafWindow
{
public:
	DISPLAY_WINDOW(ZafWindowObject* parentPtr, cf_t* pCf,
		int left, int top, int width, int height);
	~DISPLAY_WINDOW(void);
	ZafEventType Event(const ZafEventStruct &event);
	void Show(char* msg);

private:
	ZafText*			text;
	ZafStringData*		stringData;
	char				buf[2048];
	cf_t*				pCf;
	ZafWindowObject*	parent;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
class ZAF_EXPORT FILE_WINDOW : public ZafWindow
{
public:
	FILE_WINDOW(ZafWindowObject* parentPtr, const char* fileSpec,
		int left, int top, int width, int height) ;
	~FILE_WINDOW(void);
	ZafEventType Event(const ZafEventStruct &event);
	void DisplayFile(void);

private:
	ZafText*			text;
	ZafStringData*		stringData;
	char				buf[2048];
	char				fileSpec[256];
	FILE*				fp;
	ZafWindowObject*	parent;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
class PopupWindow : public ZafWindow
{
public:
	PopupWindow(void);
	~PopupWindow(void);

	ZafEventType		Event(const ZafEventStruct &event);
	ZafPopUpMenu*		PopupMenu(ZafPositionStruct position);
	DISPLAY_WINDOW*		displayWindow;
	FILE_WINDOW*		eaFtpLogWindow;
	FILE_WINDOW*		eaXferLogWindow;
	FILE_WINDOW*		eaListFileWindow;
	void				Show(char* msg);
	void				ShowFile(char* msg);
	int					ReadData(void);
	void				GetString(char*, char*);
	void				GetBool(bool*, char*);
	void				GetInt(int*, char*);

	ZafMessageWindow*	msgWindow;
	xferContext_t*		pXferContext;
	cf_t*				pCf;				// "Logging" object
	transfer_t*			pT;
	char				msgBuf[2048];
	FILE*				fp;

	char				host[256];
	char				userId[256];
	char				passwd[256];
	char				userClass[256];
	char				recvFileSpec[256];
	char				sendFileSpec[256];
	char				ediName[256];
	bool				passive;
	bool				SSL;
	char				cmdString[256];
	char				runtimeDir[256];
	char				ignRuntimeDir[256];
	char				tmpDir[256];
	char				lstFileSpec[256];
	char				mode[256];
	char				dllLogFileSpec[256];
	char				ftpLogFileSpec[256];

	bool				compression;
	bool				filter;
	bool				ascii;
	bool				edi;
	bool				secure;
	bool				crlf;
	bool				append;
	bool				autoext;

	unsigned short		controlPort;
	unsigned short		dataPort;

	int					style;

protected:
	ZafPopUpMenu*		popup;
};
