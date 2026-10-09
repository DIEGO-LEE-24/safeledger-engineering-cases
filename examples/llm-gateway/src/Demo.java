import java.util.List;
import java.util.Map;

public final class Demo {
    private record Stub(String id, boolean available) implements LlmGateway.Provider {
        public String generate(String prompt) { return "Synthetic reply: " + prompt; }
    }

    public static void main(String[] args) {
        var first = new Stub("primary", false);
        var second = new Stub("secondary", true);
        var gateway = new LlmGateway(Map.of(
            LlmGateway.Purpose.DOCUMENT, List.of(first, second),
            LlmGateway.Purpose.JUDGMENT, List.of(first, second)
        ));
        System.out.println("document=" + gateway.generate(LlmGateway.Purpose.DOCUMENT, "draft"));
        System.out.println("judgment_available=" + gateway.available(LlmGateway.Purpose.JUDGMENT));
    }
}
