package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.domain.analysis.DependencyKind;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class ClassificationDependencyEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 40)
    private DependencyKind kind;

    @Column(name = "target", nullable = false, length = 512)
    private String target;

    @Column(name = "via", nullable = false, length = 512)
    private String via;

    protected ClassificationDependencyEmbeddable() {
    }

    public ClassificationDependencyEmbeddable(
            final DependencyKind kind,
            final String target,
            final String via) {
        this.kind = kind;
        this.target = target;
        this.via = via;
    }

    public DependencyKind getKind() {
        return kind;
    }

    public String getTarget() {
        return target;
    }

    public String getVia() {
        return via;
    }
}
