#if !defined(__cpapi_h)
#define __cpapi_h 1
 
#if defined( __CPAPI )
#undef __CPAPI
#endif

#if defined( __EXPORT )
#undef __EXPORT
#endif

#if defined( __CPCONV )
#undef __CPCONV
#endif

#if defined( WIN32 )
#define __EXPORT    __declspec(dllexport)
#define __CPCONV    __stdcall
#if defined( DLL )
#define __CPAPI     __EXPORT __CPCONV
#else
#define __CPAPI     __CPCONV
#endif

#elif defined( OS2 )
#define __EXPORT    _export
#define __CPCONV    _Pascal
#if defined( DLL )
#define __CPAPI     __EXPORT __CPCONV
#else
#define __CPAPI     __CPCONV
#endif

#elif defined( DOS )
#define __EXPORT    _export
#define __CPCONV    _pascal
#if defined( DLL )
#define __CPAPI     __EXPORT __CPCONV
#else
#define __CPAPI     __CPCONV
#endif

#else
#define __CPAPI
#define __EXPORT
#define __CPCONV
#endif

#endif

