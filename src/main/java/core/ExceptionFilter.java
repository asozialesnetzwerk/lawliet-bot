package core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.Arrays;

public class ExceptionFilter extends Filter<ILoggingEvent> {

    private final String[] FILTERS = {
            "java.net.SocketTimeoutException",
            "org.javacord.api.exception.CannotMessageUserException",
            "java.util.concurrent.RejectedExecutionException",
            "java.lang.InterruptedException",
            "500: Internal Server Error",
            "Read timed out",
            "Unknown Member",
            "disconnect was called already",
            "java.util.concurrent.RejectedExecutionException",
            "Received a 502 response from Discord",
            "org.javacord.api.exception.UnknownMessageException",
            "Cannot send messages to this user",
            "timeout",
            "connect timed out",
            "java.util.concurrent.TimeoutException",
            "Unknown Message",
            "return value of \"java.util.Queue.poll()\" is null"
    };

    public ExceptionFilter() {}

    @Override
    public FilterReply decide(final ILoggingEvent event) {
        final IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return FilterReply.NEUTRAL;
        }

        if (!(throwableProxy instanceof ThrowableProxy)) {
            return FilterReply.NEUTRAL;
        }

        final ThrowableProxy throwableProxyImpl = (ThrowableProxy) throwableProxy;
        if (!checkThrowable(throwableProxyImpl.getThrowable()))
            return FilterReply.DENY;

        return FilterReply.NEUTRAL;
    }

    public boolean checkThrowable(final Throwable throwable) {
        return Arrays.stream(FILTERS)
                .noneMatch(filter -> throwable.toString().contains(filter));
    }

}
