// ----------------------------------------------------------------------------
//
//	This document contains material which is the proprietary property of and
//	confidential to bTrade.com, Incorporated
//	Disclosure outside bTrade.com is prohibited except by license agreement
//	or other confidentiality agreement.
//
//	Copyright (c) 2000-2002 by bTrade.com Incorporated
//	All Rights Reserved
//
// ----------------------------------------------------------------------------
//	Description: 
//
// ----------------------------------------------------------------------------
//			MAINTENANCE	HISTORY
//
// DATE		BY	BUG NO.	DESCRIPTION
// -------- --- ------- -------------------------------------------------------
// 20020323	PAA			Initial version
// ----------------------------------------------------------------------------
#if !defined(__BTFILE_H__)
#define __BTFILE_H__

#include <stdio.h>
#include <sys/types.h>
#include "eapltfrm.h"

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
#if defined(ASCII_LF)
#undef ASCII_LF
#endif
#define	ASCII_LF	0x0A

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
class BTFILE
{
private:
	FILE*			fp;
	char			fileSpec[BUF_1024_SZ];
	char			mode[BUF_256_SZ];		// Open mode for file
	size_t			lastBytesRead;			// Bytes read last 'read' call
	size_t			lastBytesWritten;		// Bytes written last 'write' call
	off_t			totalBytesRead;			// Total bytes read from file
	off_t			totalBytesWritten;		// Total bytes written to file
	unsigned int	ioBufSz;
	bool			deleteTempFile;			// Delete fileSpec when destruct?

public:
	BTFILE(unsigned int inBufSz, void* pCf, char* tmpDir, char* baseName,
		bool initFile, void* pTempInfo);
	BTFILE(BTFILE* copyObj);
	BTFILE(char* inFileSpec, unsigned int inBufSz);
	BTFILE(unsigned int inBufSz);
	~BTFILE(void);

	int		Append(BTFILE* inputBtFile);
	void	CommonInit(void);
	void	CommonAlloc(void);
	int		Fclose(void);
	int		Feof(void);
	int		Ferror(void);
	int		Fflush(void);
	char*	Fgets(char* buf = (char*)0, size_t cnt = 0);
	int		Fopen(char* inFileSpec, char* inMode);
	int		Fopen(char* inMode);
	int		Fprintf(bool xlate, char* format, ...);
	int		Fputs(char* buf);
	size_t	Fread(char* buf = (char*)0, size_t cnt = 0);
	size_t	FreadToDelimiter(size_t cnt = 0, char delim = ASCII_LF);
	int		Fseek(long offset, int origin);
	long	Ftell(void);
	size_t	Fwrite(char* outBuf = (char*)0, size_t cnt = 0);
	int		Remove(void);
	void	Rewind(void);

	unsigned int	TellIoBufSz() { return(ioBufSz); };
	size_t	TellLastBytesRead() { return(lastBytesRead); };
	size_t	TellLastBytesWritten() { return(lastBytesWritten); };
	off_t	TellTotalBytesRead() { return(totalBytesRead); };
	off_t	TellTotalBytesWritten() { return(totalBytesWritten); };
	void	SetTotalBytesWritten(off_t bytes) { totalBytesWritten = bytes; };
	FILE*	TellFileHandle() { return(fp); };
	char*	TellFileSpec() { return(fileSpec); };
	off_t	TellFileSize();

	void	AsciiToEbcdic(int len = 0, char* buf = (char*)0);
	void	EbcdicToAscii(int len = 0, char* buf = (char*)0);
	void	DumpData(int len = 0, char* buf = (char*)0);

	// Public data
	SINT4			lastCode;				// Last error code
	char			lastMsg[BUF_4096_SZ];	// Last error message
	char*			ioBuf;					// Buffer read into/written from
};

#endif // if !defined(__BTFILE_H__)
