package ff.ss.javaFxAuditStudio.application.ports.in;

import ff.ss.javaFxAuditStudio.domain.workbench.AnalysisSession;

import java.util.List;

public interface SubmitAnalysisUseCase {

    AnalysisSession handle(List<String> sourceFilePaths, String sessionName);
}
