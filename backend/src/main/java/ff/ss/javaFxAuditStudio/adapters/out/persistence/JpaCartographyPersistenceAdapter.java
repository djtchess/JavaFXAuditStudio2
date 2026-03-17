package ff.ss.javaFxAuditStudio.adapters.out.persistence;

import ff.ss.javaFxAuditStudio.application.ports.out.CartographyPersistencePort;
import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
import ff.ss.javaFxAuditStudio.domain.cartography.ControllerCartography;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaCartographyPersistenceAdapter implements CartographyPersistencePort {

    private final CartographyResultRepository repository;

    public JpaCartographyPersistenceAdapter(final CartographyResultRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ControllerCartography save(final String sessionId, final ControllerCartography cartography) {
        repository.deleteBySessionId(sessionId);
        CartographyResultEntity entity = toEntity(sessionId, cartography);
        CartographyResultEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ControllerCartography> findBySessionId(final String sessionId) {
        return repository.findBySessionId(sessionId).map(this::toDomain);
    }

    private CartographyResultEntity toEntity(final String sessionId, final ControllerCartography c) {
        boolean hasUnknowns = !c.unknowns().isEmpty();
        CartographyResultEntity entity = new CartographyResultEntity(
                sessionId, c.controllerRef(), c.fxmlRef(), hasUnknowns, Instant.now());

        c.components().stream()
                .map(comp -> new FxmlComponentEntity(entity, comp.fxId(), comp.componentType(), comp.eventHandler()))
                .forEach(entity.getComponents()::add);

        c.handlers().stream()
                .map(h -> new HandlerBindingEntity(entity, h.methodName(), h.fxmlRef(), h.injectedType()))
                .forEach(entity.getHandlers()::add);

        c.unknowns().stream()
                .map(u -> new CartographyUnknownEntity(entity, u.location(), u.reason()))
                .forEach(entity.getUnknowns()::add);

        return entity;
    }

    private ControllerCartography toDomain(final CartographyResultEntity entity) {
        List<FxmlComponent> components = entity.getComponents().stream()
                .map(e -> new FxmlComponent(e.getFxId(), e.getComponentType(), e.getEventHandler()))
                .toList();

        List<HandlerBinding> handlers = entity.getHandlers().stream()
                .map(e -> new HandlerBinding(e.getMethodName(), e.getFxmlRef(), e.getInjectedType()))
                .toList();

        List<CartographyUnknown> unknowns = entity.getUnknowns().stream()
                .map(e -> new CartographyUnknown(e.getLocation(), e.getReason()))
                .toList();

        return new ControllerCartography(
                entity.getControllerRef(),
                entity.getFxmlRef(),
                components,
                handlers,
                unknowns);
    }
}
