package org.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass


interface ProvideLogs {
    val LOG: Logger
    fun String.linfo() = linfo { this }
}

open class LogProvider(forClass: KClass<*>? = null) : ProvideLogs {
    override val LOG: Logger
    init {
        var cls: String? = null
        if (forClass == null) {
            cls = this::class.java.name
            if (cls.endsWith("\$Companion")) {
                cls = cls.dropLast("\$Companion".length)
            }
        } else {
            cls = forClass.qualifiedName
        }

        LOG = LoggerFactory.getLogger(cls)
    }
}


inline fun ProvideLogs.ltrace(msg: () -> String) {
    if (LOG.isTraceEnabled) LOG.trace(msg())
}

inline fun ProvideLogs.ltrace(err: Throwable, msg: () -> String) {
    if (LOG.isTraceEnabled) LOG.trace(msg(), err)
}

inline fun ProvideLogs.ldebug(msg: () -> String) {
    if (LOG.isDebugEnabled) LOG.debug(msg())
}

inline fun ProvideLogs.linfo(msg: () -> String) {
    if (LOG.isInfoEnabled) LOG.info(msg())
}

inline fun ProvideLogs.lwarn(msg: () -> String) {
    if (LOG.isWarnEnabled) LOG.warn(msg())
}

inline fun ProvideLogs.lerror(msg: () -> String) {
    if (LOG.isErrorEnabled) LOG.error(msg())
}

inline fun ProvideLogs.lwarn(ex: Throwable, msg: () -> String) {
    if (LOG.isWarnEnabled) LOG.warn(msg(), ex)
}

inline fun ProvideLogs.lerror(ex: Throwable, msg: () -> String) {
    if (LOG.isErrorEnabled) LOG.error(msg(), ex)
}
