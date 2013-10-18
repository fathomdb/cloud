package io.fathom.cloud.log;

import java.io.IOException;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

import com.google.common.collect.Lists;

public class LogbackHook<E> extends AppenderBase<E> {

    public static void attachToRootLogger() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        LogbackHook appender = new LogbackHook();
        appender.start();

        Logger logbackLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logbackLogger.addAppender(appender);
    }

    @Override
    protected void append(E e) {
        List<LogHook> hooks = LogHook.getHooks();

        if (!hooks.isEmpty()) {
            ILoggingEvent event = (ILoggingEvent) e;

            // Note that we can get the unformatted message in getMessage(),
            // presumably along with the parameters...
            String loggerName = event.getLoggerName();
            String message = event.getFormattedMessage();
            Level level = event.getLevel();
            int levelInt = level.toInt();

            List<String[]> exceptionStacks = null;

            IThrowableProxy throwableInformation = event.getThrowableProxy();
            while (throwableInformation != null) {
                String[] exceptionStackTrace = null;
                StackTraceElementProxy[] trace = throwableInformation.getStackTraceElementProxyArray();

                String exceptionMessage = throwableInformation.getMessage();
                String exceptionClass = throwableInformation.getClassName();

                if (trace != null) {
                    exceptionStackTrace = new String[1 + trace.length];
                    exceptionStackTrace[0] = exceptionClass + ": " + exceptionMessage;

                    for (int i = 0; i < trace.length; i++) {
                        exceptionStackTrace[1 + i] = trace[i].getSTEAsString();
                    }
                } else {
                    exceptionStackTrace = new String[1];
                    exceptionStackTrace[0] = exceptionClass + ": " + exceptionMessage;
                }

                if (exceptionStacks == null) {
                    exceptionStacks = Lists.newArrayList();
                }
                exceptionStacks.add(exceptionStackTrace);

                throwableInformation = throwableInformation.getCause();
            }

            if (message != null || exceptionStacks != null) {
                for (LogHook hook : hooks) {
                    try {
                        hook.log(loggerName, message, exceptionStacks, levelInt);
                    } catch (IOException e2) {
                        // Ignore
                    }
                }
            }
        }
    }
}
