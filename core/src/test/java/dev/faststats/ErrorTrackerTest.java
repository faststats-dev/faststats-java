package dev.faststats;

import dev.faststats.core.ErrorTracker;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ErrorTrackerTest {
    // todo: add redaction tests
    // todo: add nesting tests
    // todo: add duplicate tests

    @Test
    public void sameClassLoader() {
        final var loader = getClass().getClassLoader();
        final var error = new RuntimeException("test");
        assertTrue(ErrorTracker.isSameLoader(loader, error));
    }

    @Test
    public void childLoaderMatchesParentLoader() {
        final var parentLoader = getClass().getClassLoader();
        final var childLoader = new URLClassLoader(new URL[0], parentLoader);

        final var errorFromParent = new RuntimeException("test from parent");
        assertTrue(ErrorTracker.isSameLoader(parentLoader, errorFromParent));
        assertFalse(ErrorTracker.isSameLoader(childLoader, errorFromParent));
    }

    @Test
    public void differentClassLoader() {
        final var isolatedLoader = new URLClassLoader(new URL[0], null);
        final var error = new RuntimeException("test");

        assertFalse(ErrorTracker.isSameLoader(isolatedLoader, error));
    }

    @Test
    public void classLoaderHierarchyMatching() {
        final var mainLoader = getClass().getClassLoader();
        final var submissionsLoader = new URLClassLoader(new URL[0], mainLoader);
        final var virtualLoader = new URLClassLoader(new URL[0], mainLoader);
        final var netLoader = new URLClassLoader(new URL[0], submissionsLoader);

        final var errorFromMain = new RuntimeException("from main");

        assertTrue(ErrorTracker.isSameLoader(mainLoader, errorFromMain));
        assertFalse(ErrorTracker.isSameLoader(submissionsLoader, errorFromMain));
        assertFalse(ErrorTracker.isSameLoader(virtualLoader, errorFromMain));
        assertFalse(ErrorTracker.isSameLoader(netLoader, errorFromMain));

        final var isolatedLoader = new URLClassLoader(new URL[0], null);
        assertFalse(ErrorTracker.isSameLoader(isolatedLoader, errorFromMain));
    }

    @Test
    public void siblingLoadersDoNotMatch() {
        final var mainLoader = getClass().getClassLoader();
        final var submissionsLoader = new URLClassLoader(new URL[0], mainLoader);
        final var virtualLoader = new URLClassLoader(new URL[0], mainLoader);
        final var netLoader = new URLClassLoader(new URL[0], submissionsLoader);

        final var errorFromSubmissions = createErrorWithStackFrom("submissions.Plugin");
        final var errorFromVirtual = createErrorWithStackFrom("virtual.Handler");
        final var errorFromNet = createErrorWithStackFrom("net.Connection");

        assertFalse(ErrorTracker.isSameLoader(virtualLoader, errorFromSubmissions));
        assertFalse(ErrorTracker.isSameLoader(submissionsLoader, errorFromVirtual));
        assertFalse(ErrorTracker.isSameLoader(virtualLoader, errorFromNet));
    }

    private RuntimeException createErrorWithStackFrom(final String className) {
        final var error = new RuntimeException("test");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement(className, "test", "Test.java", 1)
        });
        return error;
    }

    @Test
    public void nestedCauseSameLoader() {
        final var loader = getClass().getClassLoader();
        final var cause = new IllegalArgumentException("cause");
        final var error = new RuntimeException("wrapper", cause);

        assertTrue(ErrorTracker.isSameLoader(loader, error));
    }

    @Test
    public void emptyStackTrace() {
        final var loader = getClass().getClassLoader();
        final var error = new RuntimeException("no stack");
        error.setStackTrace(new StackTraceElement[0]);

        assertFalse(ErrorTracker.isSameLoader(loader, error));
    }

    @Test
    public void emptyStackTraceChecksCause() {
        final var loader = getClass().getClassLoader();
        final var cause = createExceptionWithStack();
        final var error = new RuntimeException("no stack", cause);
        error.setStackTrace(new StackTraceElement[0]);

        assertTrue(ErrorTracker.isSameLoader(loader, error));
    }

    @Test
    public void libraryOnlyStackFallsThroughToCause() {
        final var loader = getClass().getClassLoader();
        final var cause = createExceptionWithStack();
        final var error = new RuntimeException("library only", cause);
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("java.lang.String", "valueOf", "String.java", 100)
        });

        assertTrue(ErrorTracker.isSameLoader(loader, error));
    }

    private IllegalArgumentException createExceptionWithStack() {
        return new IllegalArgumentException("cause with stack");
    }

    @Test
    // todo: fix this mess
    public void testCompile() throws InterruptedException {
        final var tracker = ErrorTracker.contextUnaware();
        tracker.attachErrorContext(null);

        try {
            roundAndRound(10);
        } catch (final Throwable t) {
            tracker.trackError(t);
        }
        try {
            recursiveError();
        } catch (final Throwable t) {
            tracker.trackError("↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ↓→ħſðđſ→ðđ””ſ→ʒðđ↓ʒ”ſðđʒ");
            tracker.trackError(t);
        }
        try {
            aroundAndAround();
        } catch (final Throwable t) {
            tracker.trackError(t);
            return;
        }

        tracker.trackError("Test error");
        final var nestedError = new RuntimeException("Nested error");
        final var error = new RuntimeException(null, nestedError);
        tracker.trackError(error);

        tracker.trackError("hello my name is david");
        tracker.trackError("/home/MyName/Documents/MyFile.txt");
        tracker.trackError("C:\\Users\\MyName\\AppData\\Local\\Temp");
        tracker.trackError("/Users/MyName/AppData/Local/Temp");
        tracker.trackError("my ipv4 address is 215.223.110.131");
        tracker.trackError("my ipv6 address is f833:be65:65da:975b:4896:88f7:6964:44c0");

        final var deepAsyncError = new RuntimeException("deep async error");

        final var thisIsANiceError = new Thread(() -> {
            final var nestedAsyncError = new RuntimeException("nested async error", deepAsyncError);
            throw new CompletionException("async error", nestedAsyncError);
        });
        thisIsANiceError.start();
        thisIsANiceError.join(1000);

        Thread.sleep(1000);

        tracker.trackError("Test error");
    }

    public void recursiveError() throws StackOverflowError {
        goRoundAndRound();
    }

    public void goRoundAndRound() throws StackOverflowError {
        andRoundAndRound();
    }

    public void andRoundAndRound() throws StackOverflowError {
        goRoundAndRound();
    }

    public void aroundAndAround() throws StackOverflowError {
        aroundAndAround();
    }

    public void roundAndRound(final int i) throws RuntimeException {
        if (i <= 0) throw new RuntimeException("out of stack");
        roundAndRound(i - 1);
    }
}
