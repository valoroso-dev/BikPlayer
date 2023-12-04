package tv.danmaku.ijk.media.common;

public class ErrorNo {
    public static final int EPERM   = 1;    /* Operation not permitted */
    public static final int ENOENT  = 2;    /* No such file or directory */
    public static final int ESRCH   = 3;    /* No such process */
    public static final int EINTR   = 4;    /* Interrupted system call */
    public static final int EIO     = 5;    /* I/O error */
    public static final int ENXIO   = 6;    /* No such device or address */
    public static final int E2BIG   = 7;    /* Argument list too long */
    public static final int ENOEXEC = 8;    /* Exec format error */
    public static final int EBADF   = 9;    /* Bad file number */
    public static final int ECHILD  = 10;    /* No child processes */
    public static final int EAGAIN  = 11;    /* Try again */
    public static final int ENOMEM  = 12;    /* Out of memory */
    public static final int EACCES  = 13;    /* Permission denied */
    public static final int EFAULT  = 14;    /* Bad address */
    public static final int ENOTBLK = 15;    /* Block device required */
    public static final int EBUSY   = 16;    /* Device or resource busy */
    public static final int EEXIST  = 17;    /* File exists */
    public static final int EXDEV   = 18;    /* Cross-device link */
    public static final int ENODEV  = 19;    /* No such device */
    public static final int ENOTDIR = 20;    /* Not a directory */
    public static final int EISDIR  = 21;    /* Is a directory */
    public static final int EINVAL  = 22;    /* Invalid argument */
    public static final int ENFILE  = 23;    /* File table overflow */
    public static final int EMFILE  = 24;    /* Too many open files */
    public static final int ENOTTY  = 25;    /* Not a typewriter */
    public static final int ETXTBSY = 26;    /* Text file busy */
    public static final int EFBIG   = 27;    /* File too large */
    public static final int ENOSPC  = 28;    /* No space left on device */
    public static final int ESPIPE  = 29;    /* Illegal seek */
    public static final int EROFS   = 30;    /* Read-only file system */
    public static final int EMLINK  = 31;    /* Too many links */
    public static final int EPIPE   = 32;    /* Broken pipe */
    public static final int EDOM    = 33;    /* Math argument out of domain of func */
    public static final int ERANGE  = 34;    /* Math result not representable */

    public static final int	EDEADLK	= 35;	 /* Resource deadlock would occur */
    public static final int	ENAMETOOLONG = 36;	/* File name too long */
    public static final int	ENOLCK	= 37;	 /* No record locks available */

    /*
     * This error code is special: arch syscall entry code will return
     * -ENOSYS if users try to call a syscall that doesn't exist.  To keep
     * failures of syscalls that really do exist distinguishable from
     * failures due to attempts to use a nonexistent syscall, syscall
     * implementations should refrain from returning -ENOSYS.
     */
    public static final int	ENOSYS	=	38;	/* Invalid system call number */

    public static final int	ENOTEMPTY=	39;	/* Directory not empty */
    public static final int	ELOOP	=	40;	/* Too many symbolic links encountered */
    // #define	EWOULDBLOCK	EAGAIN	/* Operation would block */
    public static final int	ENOMSG	=	42;	/* No message of desired type */
    public static final int	EIDRM	=	43;	/* Identifier removed */
    public static final int	ECHRNG	=	44;	/* Channel number out of range */
    public static final int	EL2NSYNC=	45;	/* Level 2 not synchronized */
    public static final int	EL3HLT	=	46;	/* Level 3 halted */
    public static final int	EL3RST	=	47;	/* Level 3 reset */
    public static final int	ELNRNG	=	48;	/* Link number out of range */
    public static final int	EUNATCH	=	49;	/* Protocol driver not attached */
    public static final int	ENOCSI	=	50;	/* No CSI structure available */
    public static final int	EL2HLT	=	51;	/* Level 2 halted */
    public static final int	EBADE	=	52;	/* Invalid exchange */
    public static final int	EBADR	=	53;	/* Invalid request descriptor */
    public static final int	EXFULL	=	54;	/* Exchange full */
    public static final int	ENOANO	=	55;	/* No anode */
    public static final int	EBADRQC	=	56;	/* Invalid request code */
    public static final int	EBADSLT	=	57;	/* Invalid slot */

    // #define	EDEADLOCK	EDEADLK

    public static final int	EBFONT	=	59;	/* Bad font file format */
    public static final int	ENOSTR	=	60;	/* Device not a stream */
    public static final int	ENODATA	=	61;	/* No data available */
    public static final int	ETIME	=	62;	/* Timer expired */
    public static final int	ENOSR	=	63;	/* Out of streams resources */
    public static final int	ENONET	=	64;	/* Machine is not on the network */
    public static final int	ENOPKG	=	65;	/* Package not installed */
    public static final int	EREMOTE	=	66;	/* Object is remote */
    public static final int	ENOLINK	=	67;	/* Link has been severed */
    public static final int	EADV	=	68;	/* Advertise error */
    public static final int	ESRMNT	=	69;	/* Srmount error */
    public static final int	ECOMM	=	70;	/* Communication error on send */
    public static final int	EPROTO	=	71;	/* Protocol error */
    public static final int	EMULTIHOP=	72;	/* Multihop attempted */
    public static final int	EDOTDOT	=	73;	/* RFS specific error */
    public static final int	EBADMSG	=	74;	/* Not a data message */
    public static final int	EOVERFLOW=	75;	/* Value too large for defined data type */
    public static final int	ENOTUNIQ=	76;	/* Name not unique on network */
    public static final int	EBADFD	=	77;	/* File descriptor in bad state */
    public static final int	EREMCHG	=	78;	/* Remote address changed */
    public static final int	ELIBACC	=	79;	/* Can not access a needed shared library */
    public static final int	ELIBBAD	=	80;	/* Accessing a corrupted shared library */
    public static final int	ELIBSCN	=	81;	/* .lib section in a.out corrupted */
    public static final int	ELIBMAX	=	82;	/* Attempting to link in too many shared libraries */
    public static final int	ELIBEXEC=	83;	/* Cannot exec a shared library directly */
    public static final int	EILSEQ	=	84;	/* Illegal byte sequence */
    public static final int	ERESTART=	85;	/* Interrupted system call should be restarted */
    public static final int	ESTRPIPE=	86;	/* Streams pipe error */
    public static final int	EUSERS	=	87;	/* Too many users */
    public static final int	ENOTSOCK=	88;	/* Socket operation on non-socket */
    public static final int	EDESTADDRREQ=89;/* Destination address required */
    public static final int	EMSGSIZE=	90;	/* Message too long */
    public static final int	EPROTOTYPE=	91;	/* Protocol wrong type for socket */
    public static final int	ENOPROTOOPT=92;	/* Protocol not available */
    public static final int	EPROTONOSUPPORT=93;	/* Protocol not supported */
    public static final int	ESOCKTNOSUPPORT=94;	/* Socket type not supported */
    public static final int	EOPNOTSUPP=	95;   	/* Operation not supported on transport endpoint */
    public static final int	EPFNOSUPPORT=96;    /* Protocol family not supported */
    public static final int	EAFNOSUPPORT=97;    /* Address family not supported by protocol */
    public static final int	EADDRINUSE=	98;	    /* Address already in use */
    public static final int	EADDRNOTAVAIL=99;	/* Cannot assign requested address */
    public static final int	ENETDOWN=	100;	/* Network is down */
    public static final int	ENETUNREACH=101;	/* Network is unreachable */
    public static final int	ENETRESET=	102;	/* Network dropped connection because of reset */
    public static final int	ECONNABORTED=103;	/* Software caused connection abort */
    public static final int	ECONNRESET=	104;	/* Connection reset by peer */
    public static final int	ENOBUFS	=	105;	/* No buffer space available */
    public static final int	EISCONN	=	106;	/* Transport endpoint is already connected */
    public static final int	ENOTCONN=	107;	/* Transport endpoint is not connected */
    public static final int	ESHUTDOWN=	108;	/* Cannot send after transport endpoint shutdown */
    public static final int	ETOOMANYREFS=109;	/* Too many references: cannot splice */
    public static final int	ETIMEDOUT=	110;	/* Connection timed out */
    public static final int	ECONNREFUSED=111;	/* Connection refused */
    public static final int	EHOSTDOWN=	112;	/* Host is down */
    public static final int	EHOSTUNREACH=113;	/* No route to host */
    public static final int	EALREADY=	114;	/* Operation already in progress */
    public static final int	EINPROGRESS=115;	/* Operation now in progress */
    public static final int	ESTALE	=	116;	/* Stale file handle */
    public static final int	EUCLEAN	=	117;	/* Structure needs cleaning */
    public static final int	ENOTNAM	=	118;	/* Not a XENIX named type file */
    public static final int	ENAVAIL	=	119;	/* No XENIX semaphores available */
    public static final int	EISNAM	=	120;	/* Is a named type file */
    public static final int	EREMOTEIO=	121;	/* Remote I/O error */
    public static final int	EDQUOT	=	122;	/* Quota exceeded */

    public static final int	ENOMEDIUM=	123;	/* No medium found */
    public static final int	EMEDIUMTYPE=124;	/* Wrong medium type */
    public static final int	ECANCELED=	125;	/* Operation Canceled */
    public static final int	ENOKEY	=	126;	/* Required key not available */
    public static final int	EKEYEXPIRED=127;	/* Key has expired */
    public static final int	EKEYREVOKED=128;	/* Key has been revoked */
    public static final int	EKEYREJECTED=129;	/* Key was rejected by service */

    /* for robust mutexes */
    public static final int	EOWNERDEAD=	130;	/* Owner died */
    public static final int	ENOTRECOVERABLE=131;/* State not recoverable */

    public static final int ERFKILL	=	132;	/* Operation not possible due to RF-kill */

    public static final int EHWPOISON=	133;	/* Memory page has hardware error */

    public static int MKTAG(int a, int b, int c, int d) {
        return ((a) | ((b) << 8) | ((c) << 16) | ((d) << 24));
    }

    public static int FFERRTAG(int a, int b, int c, int d) {
        return -MKTAG(a, b, c, d);
    }

    public static final int AVERROR_BSF_NOT_FOUND      = FFERRTAG(0xF8, 'B', 'S', 'F'); ///< Bitstream filter not found
    public static final int AVERROR_BUG                = FFERRTAG('B', 'U', 'G', '!'); ///< Internal bug, also see AVERROR_BUG2
    public static final int AVERROR_BUFFER_TOO_SMALL   = FFERRTAG('B', 'U', 'F', 'S'); ///< Buffer too small
    public static final int AVERROR_DECODER_NOT_FOUND  = FFERRTAG(0xF8, 'D', 'E', 'C'); ///< Decoder not found
    public static final int AVERROR_DEMUXER_NOT_FOUND  = FFERRTAG(0xF8, 'D', 'E', 'M'); ///< Demuxer not found
    public static final int AVERROR_ENCODER_NOT_FOUND  = FFERRTAG(0xF8, 'E', 'N', 'C');///< Encoder not found
    public static final int AVERROR_EOF                = FFERRTAG('E', 'O', 'F', ' '); ///< End of file
    public static final int AVERROR_EXIT               = FFERRTAG('E', 'X', 'I', 'T'); ///< Immediate exit was requested; the called function should not be restarted
    public static final int AVERROR_EXTERNAL           = FFERRTAG('E', 'X', 'T', ' ');///< Generic error in an external library
    public static final int AVERROR_FILTER_NOT_FOUND   = FFERRTAG(0xF8, 'F', 'I', 'L'); ///< Filter not found
    public static final int AVERROR_INVALIDDATA        = FFERRTAG('I', 'N', 'D', 'A');///< Invalid data found when processing input
    public static final int AVERROR_MUXER_NOT_FOUND    = FFERRTAG(0xF8, 'M', 'U', 'X');///< Muxer not found
    public static final int AVERROR_OPTION_NOT_FOUND   = FFERRTAG(0xF8, 'O', 'P', 'T'); ///< Option not found
    public static final int AVERROR_PATCHWELCOME       = FFERRTAG('P', 'A', 'W', 'E');///< Not yet implemented in FFmpeg, patches welcome
    public static final int AVERROR_PROTOCOL_NOT_FOUND = FFERRTAG(0xF8, 'P', 'R', 'O');///< Protocol not found

    public static final int AVERROR_STREAM_NOT_FOUND   = FFERRTAG(0xF8, 'S', 'T', 'R');///< Stream not found
    /**
     * This is semantically identical to AVERROR_BUG
     * it has been introduced in Libav after our AVERROR_BUG and with a modified value.
     */
    public static final int AVERROR_BUG2              = FFERRTAG('B', 'U', 'G', ' ');
    public static final int AVERROR_UNKNOWN           = FFERRTAG('U', 'N', 'K', 'N'); ///< Unknown error, typically from an external library
    public static final int AVERROR_EXPERIMENTAL      = (-0x2bb2afa8);///< Requested feature is flagged experimental. Set strict_std_compliance if you really want to use it.
    public static final int AVERROR_INPUT_CHANGED     = (-0x636e6701); ///< Input changed between calls. Reconfiguration is required. (can be OR-ed with AVERROR_OUTPUT_CHANGED)
    public static final int AVERROR_OUTPUT_CHANGED    = (-0x636e6702); ///< Output changed between calls. Reconfiguration is required. (can be OR-ed with AVERROR_INPUT_CHANGED)
    /* HTTP & RTSP errors */
    public static final int AVERROR_HTTP_BAD_REQUEST  = FFERRTAG(0xF8, '4', '0', '0');
    public static final int AVERROR_HTTP_UNAUTHORIZED = FFERRTAG(0xF8, '4', '0', '1');
    public static final int AVERROR_HTTP_FORBIDDEN    = FFERRTAG(0xF8, '4', '0', '3');
    public static final int AVERROR_HTTP_NOT_FOUND    = FFERRTAG(0xF8, '4', '0', '4');
    public static final int AVERROR_HTTP_OTHER_4XX    = FFERRTAG(0xF8, '4', 'X', 'X');
    public static final int AVERROR_HTTP_SERVER_ERROR = FFERRTAG(0xF8, '5', 'X', 'X');

    public static final int AVERROR_TCP_CONNECT_TIMEOUT = -1001;
    public static final int AVERROR_TCP_READ_TIMEOUT    = -1002;
    public static final int AVERROR_TCP_WRITE_TIMEOUT   = -1003;

    public static final int FFP_ERROR_AUDIO_NOT_CONSUME                    = 998;    // audio data not consume too long
    public static final int FFP_ERROR_VIDEO_DECODER_OPEN_ERROR             = 999;    // open video decoder fail
    public static final int FFP_ERROR_NO_AUDIO_RECV                        = 10002;  // haven't receive audio for a while
    public static final int FFP_ERROR_NO_VIDEO_RECV                        = 10003;  // haven't receive video for a while
    public static final int FFP_ERROR_INVALID_STREAM                       = 10006;  // there is not a valid stream
    public static final int FFP_ERROR_DRM_DEVICE_UNSUPPORT                 = 10010;  // device unsupport drm
    public static final int FFP_ERROR_DRM_ACQUIRE_FAIL                     = 10011;  // drm key aquire fail
    public static final int FFP_ERROR_ABR_FAIL                             = 10012;  // abr fail

    public static class ErrorEntry {
        public int num;
        public String name;
        public String desc;

        public ErrorEntry(int num, String name, String desc) {
            this.num = num;
            this.name = name;
            this.desc = desc;
        }
    }

    public static final ErrorEntry[] ERROR_ENTRIES = new ErrorEntry[] {
            new ErrorEntry(FFP_ERROR_AUDIO_NOT_CONSUME,"FFP_ERROR_AUDIO_NOT_CONSUME","audio data not consume too long"),
            new ErrorEntry(FFP_ERROR_VIDEO_DECODER_OPEN_ERROR,"FFP_ERROR_VIDEO_DECODER_OPEN_ERROR","open video decoder fail"),
            new ErrorEntry(FFP_ERROR_NO_AUDIO_RECV,    "FFP_ERROR_NO_AUDIO_RECV",    "haven't receive audio for a while"),
            new ErrorEntry(FFP_ERROR_NO_VIDEO_RECV,    "FFP_ERROR_NO_VIDEO_RECV",    "haven't receive video for a while"),
            new ErrorEntry(FFP_ERROR_INVALID_STREAM,   "FFP_ERROR_INVALID_STREAM",   "there is not a valid stream"),
            new ErrorEntry(FFP_ERROR_DRM_DEVICE_UNSUPPORT,"FFP_ERROR_DRM_DEVICE_UNSUPPORT","device unsupport drm"),
            new ErrorEntry(FFP_ERROR_DRM_ACQUIRE_FAIL, "FFP_ERROR_DRM_ACQUIRE_FAIL", "drm key aquire fail"),
            new ErrorEntry(FFP_ERROR_ABR_FAIL,         "FFP_ERROR_ABR_FAIL",         "abr fail"),

            new ErrorEntry(AVERROR_BSF_NOT_FOUND,      "AVERROR_BSF_NOT_FOUND",      "Bitstream filter not found"),
            new ErrorEntry(AVERROR_BUG,                "AVERROR_BUG",                "Internal bug, should not have happened"),
            new ErrorEntry(AVERROR_BUG2,               "AVERROR_BUG2",               "Internal bug, should not have happened"),
            new ErrorEntry(AVERROR_BUFFER_TOO_SMALL,   "AVERROR_BUFFER_TOO_SMALL",   "Buffer too small"),
            new ErrorEntry(AVERROR_DECODER_NOT_FOUND,  "AVERROR_DECODER_NOT_FOUND",  "Decoder not found"),
            new ErrorEntry(AVERROR_DEMUXER_NOT_FOUND,  "AVERROR_DEMUXER_NOT_FOUND",  "Demuxer not found"),
            new ErrorEntry(AVERROR_ENCODER_NOT_FOUND,  "ENCODER_NOT_FOUND",          "Encoder not found"),
            new ErrorEntry(AVERROR_EOF,                "AVERROR_EOF",                "End of file"),
            new ErrorEntry(AVERROR_EXIT,               "AVERROR_EXIT",               "Immediate exit requested"),
            new ErrorEntry(AVERROR_EXTERNAL,           "AVERROR_EXTERNAL",           "Generic error in an external library"),
            new ErrorEntry(AVERROR_FILTER_NOT_FOUND,   "AVERROR_FILTER_NOT_FOUND",   "Filter not found"),
            new ErrorEntry(AVERROR_INPUT_CHANGED,      "AVERROR_INPUT_CHANGED",      "Input changed"),
            new ErrorEntry(AVERROR_INVALIDDATA,        "AVERROR_INVALIDDATA",        "Invalid data found when processing input"),
            new ErrorEntry(AVERROR_MUXER_NOT_FOUND,    "AVERROR_MUXER_NOT_FOUND",    "Muxer not found"),
            new ErrorEntry(AVERROR_OPTION_NOT_FOUND,   "AVERROR_OPTION_NOT_FOUND",   "Option not found"),
            new ErrorEntry(AVERROR_OUTPUT_CHANGED,     "AVERROR_OUTPUT_CHANGED",     "Output changed"),
            new ErrorEntry(AVERROR_PATCHWELCOME,       "AVERROR_PATCHWELCOME",       "Not yet implemented in FFmpeg, patches welcome"),
            new ErrorEntry(AVERROR_PROTOCOL_NOT_FOUND, "AVERROR_PROTOCOL_NOT_FOUND", "Protocol not found"),
            new ErrorEntry(AVERROR_STREAM_NOT_FOUND,   "AVERROR_STREAM_NOT_FOUND",   "Stream not found"),
            new ErrorEntry(AVERROR_UNKNOWN,            "AVERROR_UNKNOWN",            "Unknown error occurred"),
            new ErrorEntry(AVERROR_EXPERIMENTAL,       "AVERROR_EXPERIMENTAL",       "Experimental feature"),
            new ErrorEntry(AVERROR_HTTP_BAD_REQUEST,   "AVERROR_HTTP_BAD_REQUEST",   "Server returned 400 Bad Request"),
            new ErrorEntry(AVERROR_HTTP_UNAUTHORIZED,  "AVERROR_HTTP_UNAUTHORIZED",  "Server returned 401 Unauthorized (authorization failed)"),
            new ErrorEntry(AVERROR_HTTP_FORBIDDEN,     "AVERROR_HTTP_FORBIDDEN",     "Server returned 403 Forbidden (access denied)"),
            new ErrorEntry(AVERROR_HTTP_NOT_FOUND,     "AVERROR_HTTP_NOT_FOUND",     "Server returned 404 Not Found"),
            new ErrorEntry(AVERROR_HTTP_OTHER_4XX,     "AVERROR_HTTP_OTHER_4XX",     "Server returned 4XX Client Error, but not one of 40{0,1,3,4}"),
            new ErrorEntry(AVERROR_HTTP_SERVER_ERROR,  "AVERROR_HTTP_SERVER_ERROR",  "Server returned 5XX Server Error reply"),

            new ErrorEntry(E2BIG,                      "E2BIG",                      "Argument list too long"),
            new ErrorEntry(EACCES,                     "EACCES",                     "Permission denied"),
            new ErrorEntry(EAGAIN,                     "EAGAIN",                     "Resource temporarily unavailable"),
            new ErrorEntry(EBADF,                      "EBADF",                      "Bad file descriptor"),
            new ErrorEntry(EBUSY,                      "EBUSY",                      "Device or resource busy"),
            new ErrorEntry(ECHILD,                     "ECHILD",                     "No child processes"),
            new ErrorEntry(EDEADLK,                    "EDEADLK"               ,     "Resource deadlock avoided"),
            new ErrorEntry(EDOM,                       "EDOM",                       "Numerical argument out of domain"),
            new ErrorEntry(EEXIST,                     "EEXIST",                     "File exists"),
            new ErrorEntry(EFAULT,                     "EFAULT",                     "Bad address"),
            new ErrorEntry(EFBIG,                      "EFBIG",                      "File too large"),
            new ErrorEntry(EILSEQ,                     "EILSEQ",                     "Illegal byte sequence"),
            new ErrorEntry(EINTR,                      "EINTR",                      "Interrupted system call"),
            new ErrorEntry(EINVAL,                     "EINVAL",                     "Invalid argument"),
            new ErrorEntry(EIO,                        "EIO",                        "I/O error"),
            new ErrorEntry(EISDIR,                     "EISDIR",                     "Is a directory"),
            new ErrorEntry(EMFILE,                     "EMFILE",                     "Too many open files"),
            new ErrorEntry(EMLINK,                     "EMLINK",                     "Too many links"),
            new ErrorEntry(ENAMETOOLONG,               "ENAMETOOLONG",               "File name too long"),
            new ErrorEntry(ENFILE,                     "ENFILE",                     "Too many open files in system"),
            new ErrorEntry(ENODEV,                     "ENODEV",                     "No such device"),
            new ErrorEntry(ENOENT,                     "ENOENT",                     "No such file or directory"),
            new ErrorEntry(ENOEXEC,                    "ENOEXEC",                    "Exec format error"),
            new ErrorEntry(ENOLCK,                     "ENOLCK",                     "No locks available"),
            new ErrorEntry(ENOMEM,                     "ENOMEM",                     "Cannot allocate memory"),
            new ErrorEntry(ENOSPC,                     "ENOSPC",                     "No space left on device"),
            new ErrorEntry(ENOSYS,                     "ENOSYS",                     "Function not implemented"),
            new ErrorEntry(ENOTDIR,                    "ENOTDIR",                    "Not a directory"),
            new ErrorEntry(ENOTEMPTY,                  "ENOTEMPTY",                  "Directory not empty"),
            new ErrorEntry(ENOTTY,                     "ENOTTY",                     "Inappropriate I/O control operation"),
            new ErrorEntry(ENXIO,                      "ENXIO",                      "No such device or address"),
            new ErrorEntry(EPERM,                      "EPERM",                      "Operation not permitted"),
            new ErrorEntry(EPIPE,                      "EPIPE",                      "Broken pipe"),
            new ErrorEntry(ERANGE,                     "ERANGE",                     "Result too large"),
            new ErrorEntry(EROFS,                      "EROFS",                      "Read-only file system"),
            new ErrorEntry(ESPIPE,                     "ESPIPE",                     "Illegal seek"),
            new ErrorEntry(ESRCH,                      "ESRCH",                      "No such process"),
            new ErrorEntry(EXDEV,                      "EXDEV",                      "Cross-device link"),
            new ErrorEntry(ENETUNREACH,                "ENETUNREACH",                "Network is unreachable"),
    };

    public static String error2string(int err) {
        for (ErrorEntry item : ERROR_ENTRIES) {
            if (item.num == err) {
                return item.name + ":" + item.desc;
            }
        }
        return "unknown " + err;
    }
}
