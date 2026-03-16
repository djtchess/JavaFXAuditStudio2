package ff.ss.javaFxAuditStudio.configuration;

import ff.ss.javaFxAuditStudio.adapters.out.analysis.FxmlCartographyAnalysisAdapter;
import ff.ss.javaFxAuditStudio.application.ports.in.CartographyUseCase;
import ff.ss.javaFxAuditStudio.application.ports.out.CartographyAnalysisPort;
import ff.ss.javaFxAuditStudio.application.service.CartographyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CartographyConfiguration {

    @Bean
    public CartographyAnalysisPort cartographyAnalysisPort() {
        return new FxmlCartographyAnalysisAdapter();
    }

    @Bean
    public CartographyUseCase cartographyUseCase(final CartographyAnalysisPort cartographyAnalysisPort) {
        CartographyUseCase useCase;
        useCase = new CartographyService(cartographyAnalysisPort);
        return useCase;
    }
}
