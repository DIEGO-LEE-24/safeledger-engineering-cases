import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LlmGatewayTest {
    private static int passed;
    private static int failed;

    private static final class Fake implements LlmGateway.Provider {
        final String id;
        boolean enabled;
        RuntimeException error;
        int calls;
        Fake(String id, boolean enabled) { this.id = id; this.enabled = enabled; }
        public String id() { return id; }
        public boolean available() { return enabled; }
        public String generate(String prompt) {
            calls++;
            if (error != null) throw error;
            return "reply:" + prompt;
        }
    }

    private static LlmGateway gateway(Fake first, Fake second) {
        return new LlmGateway(Map.of(
            LlmGateway.Purpose.DOCUMENT, List.of(first, second),
            LlmGateway.Purpose.JUDGMENT, List.of(first, second)
        ));
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Condition not met");
    }

    private static void raises(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Unexpected exception", error);
        }
        throw new AssertionError("Expected exception: " + type.getSimpleName());
    }

    private static void test(String name, Runnable action) {
        try { action.run(); passed++; System.out.println("PASS " + name); }
        catch (Throwable error) {
            failed++;
            System.out.println("FAIL " + name + ": " + error);
        }
    }

    public static void main(String[] args) {
        test("document uses first healthy provider", () -> {
            var first = new Fake("first", true); var next = new Fake("next", true);
            var response = gateway(first, next).generate(LlmGateway.Purpose.DOCUMENT, "x");
            check(response.provider().equals("first") && response.text().equals("reply:x"));
            check(first.calls == 1 && next.calls == 0);
        });
        test("judgment uses first provider", () -> {
            var first = new Fake("first", true); var next = new Fake("next", true);
            check(gateway(first, next).generate(LlmGateway.Purpose.JUDGMENT, "x").provider().equals("first"));
            check(next.calls == 0);
        });
        test("availability regression: first unavailable but document chain usable", () -> {
            var first = new Fake("first", false); var next = new Fake("next", true);
            check(!first.available()); // The incorrect first-only gate would stop here.
            check(gateway(first, next).available(LlmGateway.Purpose.DOCUMENT));
        });
        test("document skips unavailable first provider", () -> {
            var first = new Fake("first", false); var next = new Fake("next", true);
            check(gateway(first, next).generate(LlmGateway.Purpose.DOCUMENT, "x").provider().equals("next"));
            check(first.calls == 0 && next.calls == 1);
        });
        test("document falls back on declared provider failure", () -> {
            var first = new Fake("first", true); var next = new Fake("next", true);
            first.error = new LlmGateway.ProviderFailure("synthetic outage");
            check(gateway(first, next).generate(LlmGateway.Purpose.DOCUMENT, "x").provider().equals("next"));
            check(first.calls == 1 && next.calls == 1);
        });
        test("judgment availability does not consult alternate", () -> {
            check(!gateway(new Fake("first", false), new Fake("next", true)).available(LlmGateway.Purpose.JUDGMENT));
        });
        test("judgment unavailable first never invokes alternate", () -> {
            var first = new Fake("first", false); var next = new Fake("next", true);
            raises(LlmGateway.Unavailable.class, () -> gateway(first, next).generate(LlmGateway.Purpose.JUDGMENT, "x"));
            check(first.calls == 0 && next.calls == 0);
        });
        test("judgment failure never invokes alternate", () -> {
            var first = new Fake("first", true); var next = new Fake("next", true);
            first.error = new LlmGateway.ProviderFailure("synthetic outage");
            raises(LlmGateway.Unavailable.class, () -> gateway(first, next).generate(LlmGateway.Purpose.JUDGMENT, "x"));
            check(first.calls == 1 && next.calls == 0);
        });
        test("all unavailable returns false and rejects execution", () -> {
            var route = gateway(new Fake("first", false), new Fake("next", false));
            check(!route.available(LlmGateway.Purpose.DOCUMENT));
            raises(LlmGateway.Unavailable.class, () -> route.generate(LlmGateway.Purpose.DOCUMENT, "x"));
        });
        test("all provider failures reject execution", () -> {
            var first = new Fake("first", true); var next = new Fake("next", true);
            first.error = new LlmGateway.ProviderFailure("one");
            next.error = new LlmGateway.ProviderFailure("two");
            raises(LlmGateway.Unavailable.class, () -> gateway(first, next).generate(LlmGateway.Purpose.DOCUMENT, "x"));
            check(first.calls == 1 && next.calls == 1);
        });
        test("programming error is not hidden by fallback", () -> {
            var first = new Fake("first", true); var next = new Fake("next", true);
            first.error = new IllegalStateException("bug");
            raises(IllegalStateException.class, () -> gateway(first, next).generate(LlmGateway.Purpose.DOCUMENT, "x"));
            check(next.calls == 0);
        });
        test("missing route fails closed", () -> {
            var route = new LlmGateway(Map.of(LlmGateway.Purpose.DOCUMENT, List.of(new Fake("doc", true))));
            raises(IllegalArgumentException.class, () -> route.available(LlmGateway.Purpose.JUDGMENT));
        });
        test("empty route rejected", () -> {
            raises(IllegalArgumentException.class, () -> new LlmGateway(Map.of(LlmGateway.Purpose.DOCUMENT, List.of())));
        });
        test("blank prompt rejected before provider call", () -> {
            var first = new Fake("first", true); var next = new Fake("next", true);
            raises(IllegalArgumentException.class, () -> gateway(first, next).generate(LlmGateway.Purpose.DOCUMENT, " "));
            check(first.calls == 0 && next.calls == 0);
        });
        test("route list copied instead of sharing mutable configuration", () -> {
            var list = new ArrayList<LlmGateway.Provider>(); list.add(new Fake("first", true));
            var route = new LlmGateway(Map.of(LlmGateway.Purpose.DOCUMENT, list));
            list.clear();
            check(route.available(LlmGateway.Purpose.DOCUMENT));
        });
        System.out.printf("RESULT passed=%d failed=%d%n", passed, failed);
        if (failed != 0) System.exit(1);
    }
}
