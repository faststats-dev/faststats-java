package dev.faststats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class StackTraceFingerprintTest {
    @Test
    public void normalizeIncludesExceptionClassAndFrameOwnersOnly() {
        final var error = new RuntimeException("message is ignored");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Plugin", "run", "Plugin.java", 42),
                new StackTraceElement("example.Worker", "call", "Worker.java", 7)
        });

        assertEquals("""
                ejava.lang.RuntimeException
                fexample.Plugin.run
                fexample.Worker.call""", StackTraceFingerprint.normalize(error));
    }

    @Test
    public void normalizeExcludesLibraryFrames() {
        final var error = new RuntimeException("message is ignored");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("java.util.ArrayList", "get", "ArrayList.java", 427),
                new StackTraceElement("javax.script.ScriptEngine", "eval", "ScriptEngine.java", 1),
                new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke0", "NativeMethodAccessorImpl.java", -2),
                new StackTraceElement("com.sun.proxy.Proxy", "invoke", "Proxy.java", 1),
                new StackTraceElement("jdk.internal.reflect.DirectMethodHandleAccessor", "invoke", "DirectMethodHandleAccessor.java", 104),
                new StackTraceElement("example.Plugin", "run", "Plugin.java", 42)
        });

        assertEquals("""
                ejava.lang.RuntimeException
                fexample.Plugin.run""", StackTraceFingerprint.normalize(error));
    }

    @Test
    public void normalizeIncludesOnlyFirstFiveNonLibraryFrames() {
        final var error = new RuntimeException("message is ignored");
        error.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("java.util.ArrayList", "get", "ArrayList.java", 427),
                new StackTraceElement("example.Plugin", "run", "Plugin.java", 42),
                new StackTraceElement("example.Worker", "call", "Worker.java", 7),
                new StackTraceElement("example.Service", "execute", "Service.java", 15),
                new StackTraceElement("example.Repository", "load", "Repository.java", 23),
                new StackTraceElement("example.Database", "query", "Database.java", 31),
                new StackTraceElement("example.Ignored", "extra", "Ignored.java", 39)
        });

        assertEquals("""
                ejava.lang.RuntimeException
                fexample.Plugin.run
                fexample.Worker.call
                fexample.Service.execute
                fexample.Repository.load
                fexample.Database.query""", StackTraceFingerprint.normalize(error));
    }

    @Test
    public void normalizeIgnoresMessageFileAndLineNumberDifferences() {
        final var first = new RuntimeException("This is error #23f4");
        first.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Plugin", "run", "Plugin.java", 42)
        });
        final var second = new RuntimeException("This is error #93dsff");
        second.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Plugin", "run", "Generated.java", 99)
        });

        assertEquals(StackTraceFingerprint.normalize(first), StackTraceFingerprint.normalize(second));
        assertEquals(StackTraceFingerprint.hash(first), StackTraceFingerprint.hash(second));
    }

    @Test
    public void differentExceptionClassChangesFingerprint() {
        final var first = new RuntimeException("same");
        first.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Plugin", "run", "Plugin.java", 42)
        });
        final var second = new IllegalStateException("same");
        second.setStackTrace(first.getStackTrace());

        assertNotEquals(StackTraceFingerprint.normalize(first), StackTraceFingerprint.normalize(second));
        assertNotEquals(StackTraceFingerprint.hash(first), StackTraceFingerprint.hash(second));
    }

    @Test
    public void differentFrameMethodChangesFingerprint() {
        final var first = new RuntimeException("same");
        first.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Plugin", "run", "Plugin.java", 42)
        });
        final var second = new RuntimeException("same");
        second.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Plugin", "stop", "Plugin.java", 42)
        });

        assertNotEquals(StackTraceFingerprint.normalize(first), StackTraceFingerprint.normalize(second));
        assertNotEquals(StackTraceFingerprint.hash(first), StackTraceFingerprint.hash(second));
    }

    @Test
    public void normalizeIncludesNestedCausesInOrder() {
        final var root = new IllegalArgumentException("root");
        root.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Root", "fail", "Root.java", 10)
        });
        final var top = new RuntimeException("top", root);
        top.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Top", "run", "Top.java", 30)
        });

        assertEquals("""
                ejava.lang.RuntimeException
                fexample.Top.run
                ejava.lang.IllegalArgumentException
                fexample.Root.fail""", StackTraceFingerprint.normalize(top));
    }

    @Test
    public void cyclicCauseChainStopsAfterFirstVisit() {
        final var first = new RuntimeException("first");
        first.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.First", "run", "First.java", 1)
        });
        final var second = new IllegalStateException("second", first);
        second.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("example.Second", "call", "Second.java", 2)
        });
        first.initCause(second);

        assertEquals("""
                ejava.lang.RuntimeException
                fexample.First.run
                ejava.lang.IllegalStateException
                fexample.Second.call""", StackTraceFingerprint.normalize(first));
    }
}
