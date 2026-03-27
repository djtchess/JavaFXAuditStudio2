package ff.ss.javaFxAuditStudio.domain.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEstimatorTest {

    @Test
    void should_return_zero_for_null_and_blank_sources() {
        assertThat(TokenEstimator.estimate(null)).isZero();
        assertThat(TokenEstimator.estimate("   ")).isZero();
    }

    @Test
    void should_count_lexical_tokens_in_java_like_source() {
        assertThat(TokenEstimator.estimate("public class MyController {}")).isEqualTo(6);
    }

    @Test
    void should_split_camel_case_and_underscore_words() {
        assertThat(TokenEstimator.estimate("savePatientData()")).isEqualTo(5);
        assertThat(TokenEstimator.estimate("save_patient_data()")).isEqualTo(5);
    }

    @Test
    void should_split_acronyms_before_mixed_case_suffixes() {
        assertThat(TokenEstimator.estimate("HTTPServerFactory")).isEqualTo(3);
    }
}
