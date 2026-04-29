package dev.faststats;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ErrorTrackerTest {
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
    public void redactsBuiltInSensitiveValuesFromMessageAndStackHeader() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();

        tracker.trackError("connect jdbc:postgresql://localhost:5432/secret@db from 192.168.1.20");

        final var report = tracker.getData().get(0).getAsJsonObject();
        final var message = report.get("message").getAsString();
        final var header = report.getAsJsonArray("stack").get(0).getAsString();

        assertEquals("connect jdbc:postgresql://localhost:[password hidden]@db from [IP hidden]", message);
        assertEquals("java.lang.RuntimeException: " + message, header);
    }

    @Test
    public void appliesCustomRedactionAfterBuiltInRedaction() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        tracker.anonymize("session=[^ ]+", "session=[hidden]");

        tracker.trackError("failed with session=abc123 from 10.0.0.1");

        final var message = tracker.getData()
                .get(0)
                .getAsJsonObject()
                .get("message")
                .getAsString();

        assertEquals("failed with session=[hidden] from [IP hidden]", message);
    }

    @Test
    public void nullMessagesAreNotSerializedAsMessageProperty() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();

        tracker.trackError(new RuntimeException((String) null));

        final var report = tracker.getData().get(0).getAsJsonObject();
        assertFalse(report.has("message"));
        assertEquals("java.lang.RuntimeException", report.getAsJsonArray("stack").get(0).getAsString());
    }

    @Test
    public void nestedCausesAreSerializedInOrder() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        final var root = new IllegalArgumentException("root secret 172.16.0.9");
        root.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Root", "fail", "Root.java", 10)
        });
        final var middle = new IllegalStateException("middle", root);
        middle.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Middle", "call", "Middle.java", 20),
                new StackTraceElement("example.Root", "fail", "Root.java", 10)
        });
        final var top = new RuntimeException("top", middle);
        top.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Top", "run", "Top.java", 30),
                new StackTraceElement("example.Middle", "call", "Middle.java", 20),
                new StackTraceElement("example.Root", "fail", "Root.java", 10)
        });

        tracker.trackError(top, false);

        final var report = tracker.getData().get(0).getAsJsonObject();
        final var stack = report.getAsJsonArray("stack");

        assertEquals(RuntimeException.class.getName(), report.get("error").getAsString());
        assertFalse(report.get("handled").getAsBoolean());
        assertEquals("java.lang.RuntimeException: top", stack.get(0).getAsString());
        assertEquals("  at example.Top.run(Top.java:30)", stack.get(1).getAsString());
        assertEquals("  at example.Middle.call(Middle.java:20)", stack.get(2).getAsString());
        assertEquals("  at example.Root.fail(Root.java:10)", stack.get(3).getAsString());
        assertEquals("Caused by: java.lang.IllegalStateException: middle", stack.get(4).getAsString());
        assertEquals("  ... 2 more", stack.get(5).getAsString());
        assertEquals("Caused by: java.lang.IllegalArgumentException: root secret [IP hidden]", stack.get(6).getAsString());
    }

    @Test
    public void cyclicCauseChainStopsAfterFirstVisit() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        final var first = new RuntimeException("first");
        final var second = new IllegalStateException("second", first);
        first.initCause(second);

        tracker.trackError(first);

        final var stack = tracker.getData().get(0).getAsJsonObject().getAsJsonArray("stack");
        var firstCauseCount = 0;
        var secondCauseCount = 0;
        for (final var element : stack) {
            final var line = element.getAsString();
            if (line.equals("Caused by: java.lang.RuntimeException: first")) firstCauseCount++;
            if (line.equals("Caused by: java.lang.IllegalStateException: second")) secondCauseCount++;
        }

        assertEquals(1, firstCauseCount);
        assertEquals(1, secondCauseCount);
    }

    @Test
    public void duplicateErrorsAreAggregatedWithCount() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        final var first = createStableError();
        final var second = createStableError();

        tracker.trackError(first);
        tracker.trackError(second);

        final var reports = tracker.getData();
        final var report = reports.get(0).getAsJsonObject();

        assertEquals(1, reports.size());
        assertEquals(2, report.get("count").getAsInt());
        assertEquals("build", report.get("buildId").getAsString());
        assertEquals("duplicate", report.get("message").getAsString());
    }

    @Test
    public void clearKeepsDuplicateCountButRemovesPayloadUntilRepeated() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        tracker.trackError(createStableError());
        tracker.trackError(createStableError());

        tracker.clear();

        assertFalse(tracker.needsFlushing());
        assertEquals(0, tracker.getData().size());

        tracker.trackError(createStableError());

        final var report = tracker.getData().get(0).getAsJsonObject();
        assertEquals("duplicate", report.get("message").getAsString());
        assertNull(report.get("count"));
    }

    @Test
    public void ignoredNestedCauseSuppressesWholeReport() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        tracker.ignoreError(IllegalArgumentException.class, "ignore me");

        tracker.trackError(new RuntimeException("wrapper", new IllegalArgumentException("ignore me")));

        assertEquals(0, tracker.getData().size());
        assertFalse(tracker.needsFlushing());
    }

    @Test
    public void repeatingStackFramesAreCollapsed() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        final var error = new StackOverflowError("recursive");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Recursive", "a", "Recursive.java", 1),
                new StackTraceElement("example.Recursive", "b", "Recursive.java", 2),
                new StackTraceElement("example.Recursive", "a", "Recursive.java", 1),
                new StackTraceElement("example.Recursive", "b", "Recursive.java", 2),
                new StackTraceElement("example.Recursive", "a", "Recursive.java", 1),
                new StackTraceElement("example.Recursive", "b", "Recursive.java", 2)
        });

        tracker.trackError(error);

        final var stack = tracker.getData().get(0).getAsJsonObject().getAsJsonArray("stack");
        assertEquals("java.lang.StackOverflowError: recursive", stack.get(0).getAsString());
        assertEquals("  at example.Recursive.a(Recursive.java:1)", stack.get(1).getAsString());
        assertEquals("  at example.Recursive.b(Recursive.java:2)", stack.get(2).getAsString());
        assertEquals("  ... 4 more", stack.get(3).getAsString());
        assertEquals(4, stack.size());
    }

    @Test
    public void longMessagesAreTruncatedBeforeSerialization() {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        final var message = "a".repeat(600);

        tracker.trackError(message);

        final var report = tracker.getData().get(0).getAsJsonObject();
        final var serialized = report.get("message").getAsString();
        assertEquals(503, serialized.length());
        assertTrue(serialized.endsWith("..."));
        assertEquals("java.lang.RuntimeException: " + serialized, report.getAsJsonArray("stack").get(0).getAsString());
    }

    @Test
    public void attachedContextTracksUnhandledThreadError() throws InterruptedException {
        final var tracker = (SimpleErrorTracker) ErrorTracker.contextUnaware();
        final var handled = new CountDownLatch(1);
        final var thrown = new RuntimeException("async failure");
        thrown.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Async", "run", "Async.java", 7)
        });

        tracker.setContextErrorHandler((loader, error) -> handled.countDown());
        tracker.attachErrorContext(null);
        try {
            final var thread = new Thread(() -> {
                throw thrown;
            });
            thread.start();
            thread.join(1000);

            assertTrue(handled.await(1, TimeUnit.SECONDS));
            final var report = tracker.getData().get(0).getAsJsonObject();
            assertEquals("async failure", report.get("message").getAsString());
            assertFalse(report.get("handled").getAsBoolean());
        } finally {
            tracker.detachErrorContext();
        }
    }

    private RuntimeException createStableError() {
        final var error = new RuntimeException("duplicate");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Plugin", "run", "Plugin.java", 42)
        });
        return error;
    }
}
