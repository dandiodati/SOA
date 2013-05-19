#if !defined(__mimeparse_h)
#define __mimeparse_h

#if defined(__cplusplus)
extern "C" {
#endif  /* if defined(__cplusplus) */

#include "cpapi.h"
#include "mimeids.h"

/* define special characters */

#if defined(EBCDIC)
#define CR  0x0d
#define LF  0x15
#define HT  0x05
#define NL  0x15
#else
#define CR  0x0d
#define LF  0x0a
#define HT  0x09
#define NL  0x0a
#endif  /* defined(EBCDIC) */

/* define max number of header/parm tokens */
#define MAX_TOKEN_COUNT 100

/* define error codes */
#define MIME_ERR_INVALID_CHARACTER              -1
#define MIME_ERR_TOKEN_TOO_LONG                 -2
#define MIME_ERR_LINE_TOO_LONG                  -3
#define MIME_ERR_INVALID_SYNTAX                 -4
#define MIME_ERR_TOO_MANY_TOKENS                -5
#define MIME_ERR_INVALID_HTTP_RESPONSE_SYNTAX   -6
#define MIME_ERR_MEMORY_ALLOCATION              -9

/* define structure used to represent headers/parms with their values */
typedef struct mime_header MIMEHEADER;
struct mime_header {
    MIMEHEADER* next;           // pointer to sibling header/parm structure.
                                //   NULL terminates the chain of siblings.
    int         headerOrParmID; // enumerated header/parm type.
                                //   If header or parm are unknown, then the
                                //   'valueID' is undefined and the unknown
                                //   header/parm and its value are given 
                                //   as a single string pointed to by 'value'
                                //   with a length of 'valueLen'.
    int         valueID;        // enumerated value for a header or parm.
                                //   Most parms do not give an enumerated
                                //   valueID; rather the valueID is 'v_string'
                                //   and the actual string is pointed to by
                                //   'value' with a length of 'valueLen'.
	int			status;         // enumerated status of the parsed header/parm.
	                            //   If a parsing error occurred,
	                            //   it is reflected here.
    char*       keyword;        // string representation of header/parm keyword.
                                //   This is a NULL-terminated string.
    int         keywordLen;     // length of keyword not including NULL
    char*       value;          // string representation of value.
                                //   This is a NULL-terminated string.
    int         valueLen;       // length of value not including NULL
    MIMEHEADER* parms;          // pointer to start of parameter structures
                                //   'owned' by this header.  NULL if no parms.
};

/* define 'context' structure used to parse a group of MIME headers */
#define	MAX_TOKEN_LEN	1024
typedef struct mime_header_context MIMEHEADERCTX;
struct mime_header_context {
    MIMEHEADER* headers;        // head of linked list of MIMEHEADERs.
                                //   this is the program-usable representation
                                //   of MIME headers, values and parms.  It is
                                //   the only element of this structure that
                                //   should be referenced by the caller.
    MIMEHEADER* curHeader;      // reserved for parsing routine
    MIMEHEADER* curParm;        // reserved for parsing routine
    int         checkForEnd;    // reserved for parsing routine
	int			skipWhitespace;	// reserved for parsing routine
    int         parseHeader;    // reserved for parsing routine
    int         parenCount;     // reserved for parsing routine
    int         quoted;         // reserved for parsing routine
    int         escaped;        // reserved for parsing routine
    int         hangingCR;      // reserved for parsing routine
    int         tokenCount;     // reserved for parsing routine
    int         tokenLen;       // reserved for parsing routine
    int         lineLen;        // reserved for parsing routine
    char        token[MAX_TOKEN_LEN];  // reserved for parsing routine
    char        line[1024];     // reserved for parsing routine
};

/*******************************************************************************
 * The MIME header parser accepts a pointer to a MIMEHEADERCTX structure.
 * It parses a group of continguous headers into a linked list of MIMEHEADER
 * structures.  The head of the linked list is pointed to by 'headers.'
 *
 * MIME headers, values and parms are parsed into a linked list of MIMEHEADER
 * structures.  An individual MIMEHEADER structure represents either a header
 * type/value (e.g. 'Content-Type: text/plain'), or a parameter type/value
 * supplied with a header (e.g. the 'charset=US_ASCII' parm that is optional
 * with the 'Content-Type: text/plain' header).
 *
 * The first MIMEHEADER in the linked list represents the first header
 * type/value in the group of headers.  The 'next' pointer points to the next
 * header's MIMEHEADER structure.  Headers are thus independent siblings of
 * each other.
 *
 * Headers can have a secondary chain of MIMEHEADERS that represent any
 * parameters supplied with the header.  Each parameter is represented with
 * its own MIMEHEADER, and the 'next' pointer points to the next parameter's
 * MIMEHEADER structure.  Parameters are thus independent siblings of each
 * other and are chained off of their header.
 *
 * Here is an example showing some headers and a logical representation of
 * their MIMEHEADER structure chain:
 *
 *  MIME-Version: 1.0
 *  Content-Type: application/edi-x12; charset=US-ASCII
 *  Content-Transfer-Encoding: base64
 *
 *  (MIMEHEADER-1)          (MIMEHEADER-2)          (MIMEHEADER-3)
 *  --------------          --------------          --------------
 *  ptr to MIMEHEADER-2     ptr to MIMEHEADER-3     NULL
 *  h_mimeVersion           h_contentType           h_ContentTransferEncoding
 *  v_string                v_application/edi-x12   v_base64
 *  "MIME-Version"          "Content-Type"          "Content-Transfer-Encoding"
 *  12                      12                      25
 *  "1.0"                   "application/edi-x12"   "base64"
 *  3                       19                      6
 *  NULL                    ptr to MIMEHEADER-2a    NULL
 *
 *                          (MIMEHEADER-2a)
 *                          ---------------
 *                          NULL
 *                          p_charset
 *                          v_string
 *                          "charset"
 *                          7
 *                          "US-ASCII"
 *                          8
 *                          NULL
 ******************************************************************************/

/*******************************************************************************
 * Function prototypes
 ******************************************************************************/

/*******************************************************************************
 * Allocate a MIMEHEADERCTX.  Return NULL if allocation fails.
 ******************************************************************************/
__EXPORT MIMEHEADERCTX* __CPCONV MIMEInit(  void );

/*******************************************************************************
 * Begin or continue parsing a group of contiguous MIME headers.  The linked
 * list of MIMEHEADER structures are allocated and filled by this routine.
 * The headers are provided via the 'buffer' and 'bufferLen' arguments.
 *
 * Each MIME header is terminated by a CR/LF pair.  An empty line (i.e.,
 * two consecutive CR/LF pairs) indicate the end of a group of headers.  This
 * routine allows headers to have embedded CR/LF pairs so they can be split
 * across multiple lines.  If the last parameter on a header is terminated
 * with a semicolon, this is not treated as an error.
 *
 * This routine interprets single LF characters to be the same as a CR/LF pair.
 * This is in anticipation of the caller reading the headers using C text I/O.
 *
 * The entire group of headers do not need to be provided on a single call to
 * this function.  The function returns '0' when it exhausts the input
 * buffer before finding the end of the header group.  The caller should call
 * the function again providing more header input.
 *
 * The function returns the offset of the ending location in the input buffer
 * when it has parsed a complete group of headers (i.e., the return value is a
 * positive integer).  The caller must NOT call this function again with the
 * same MIMEHEADERCTX.  A single MIMEHEADERCTX can represent only a single
 * group of MIME headers.
 *
 * The function returns '-1' when it encounters an invalid character in the
 * input (MIME headers can only contain 7-bit US-ASCII characters, CR and LF).
 *
 * The function returns '-2' when a single header line, after being unfolded,
 * exceeds 1024 characters in length.
 *
 * The function returns '-3' when a single header/parm keyword or value exceeds
 * 1024 characters (MAX_TOKEN_LEN) in length.  This is a protection against
 * parsing data that are not really MIME headers.
 *
 * The function returns '-4' when the header has an invalid syntax.
 *
 * The function returns '-5' if the maximum number of headers/parms is
 * exceeded.  The MAX_TOKEN_COUNT in mimeparse.h defines the maximum.
 *
 * The function returns '-6' if the HTTP response has an invalid syntax.
 *
 * The function returns '-9' when a memory allocation function fails.
 *
 * The caller must NOT call this function using the same MIMEHEADERCTX after
 * an error has been returned.  Also, since errors are negative numbers,
 * the maximum length of the buffer provided to this routine is the largest
 * value of a signed integer.
 ******************************************************************************/
__EXPORT int  __CPCONV MIMEParse( MIMEHEADERCTX* ctx, char* buffer,
                                int bufferLen );

/*******************************************************************************
 * Free the memory allocated by the linked list of MIMEHEADERS and the
 * MIMEHEADERCTX.
 ******************************************************************************/
__EXPORT void __CPCONV MIMEFree(  MIMEHEADERCTX* ctx );

#if defined(__cplusplus)
}
#endif  /* if defined(__cplusplus) */
#endif /* defined(__mimeparse_h) */
