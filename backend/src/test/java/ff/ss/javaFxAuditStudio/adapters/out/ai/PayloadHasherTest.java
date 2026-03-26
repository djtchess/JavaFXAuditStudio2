package ff.ss.javaFxAuditStudio.adapters.out.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires du PayloadHasher (JAS-029).
 */
class PayloadHasherTest {

    private PayloadHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PayloadHasher();
    }

    @Test
    void should_produce_64_char_hex_hash() {
        String hash = hasher.hash("some sanitized content");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void should_produce_same_hash_for_same_input() {
        String input = "public class MyController { void onSave() {} }";
        String hash1 = hasher.hash(input);
        String hash2 = hasher.hash(input);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void should_handle_empty_input() {
        String hash = hasher.hash("");
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void should_handle_null_input() {
        String hash = hasher.hash(null);
        // null est traite comme chaine vide : meme hash que ""
        String hashEmpty = hasher.hash("");
        assertThat(hash).isEqualTo(hashEmpty);
    }

    @Test
    void should_produce_different_hashes_for_different_inputs() {
        String hash1 = hasher.hash("content-a");
        String hash2 = hasher.hash("content-b");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
