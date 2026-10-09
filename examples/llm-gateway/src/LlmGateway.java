import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Independent in-memory example: no SDK, HTTP call, or product configuration. */
public final class LlmGateway {
    public enum Purpose { DOCUMENT, JUDGMENT }

    public interface Provider {
        String id();
        boolean available();
        String generate(String prompt);
    }

    public record Response(String provider, String text) {}

    /** Only this explicit provider failure permits a fallback attempt. */
    public static final class ProviderFailure extends RuntimeException {
        public ProviderFailure(String message) { super(message); }
    }

    public static final class Unavailable extends RuntimeException {
        public Unavailable() { super("No permitted provider completed the request"); }
    }

    private final Map<Purpose, List<Provider>> routes;

    public LlmGateway(Map<Purpose, List<Provider>> routes) {
        var copy = new java.util.EnumMap<Purpose, List<Provider>>(Purpose.class);
        routes.forEach((purpose, providers) -> {
            Objects.requireNonNull(purpose);
            if (providers.isEmpty()) throw new IllegalArgumentException("Empty route");
            copy.put(purpose, List.copyOf(providers));
        });
        this.routes = Map.copyOf(copy);
    }

    private List<Provider> permittedChain(Purpose purpose) {
        var chain = routes.get(Objects.requireNonNull(purpose));
        if (chain == null) throw new IllegalArgumentException("Unconfigured purpose");
        return purpose == Purpose.JUDGMENT ? chain.subList(0, 1) : chain;
    }

    public boolean available(Purpose purpose) {
        return permittedChain(purpose).stream().anyMatch(Provider::available);
    }

    public Response generate(Purpose purpose, String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }
        for (var provider : permittedChain(purpose)) {
            if (!provider.available()) continue;
            try {
                return new Response(provider.id(), provider.generate(prompt));
            } catch (ProviderFailure failure) {
                // A judgment route has one element, so it never changes provider.
                // Programming errors and other exception types are not hidden.
            }
        }
        throw new Unavailable();
    }
}
