package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.CartographyAnalysisPort;
import ff.ss.javaFxAuditStudio.domain.cartography.FxmlComponent;
import ff.ss.javaFxAuditStudio.domain.cartography.HandlerBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Adaptateur de cartographie FXML.
 * Parse le contenu FXML via l'API DOM standard du JDK (javax.xml).
 * Extrait les composants ayant un fx:id et les handlers Java via regex.
 * Instancié par CartographyConfiguration — pas d'annotation Spring.
 */
public final class FxmlCartographyAnalysisAdapter implements CartographyAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(FxmlCartographyAnalysisAdapter.class);

    private static final String FXML_ID_ATTR = "fx:id";
    private static final String ON_ACTION_ATTR = "onAction";
    private static final Pattern HANDLER_PATTERN =
            Pattern.compile("@FXML\\s+(?:public\\s+)?void\\s+(\\w+)\\s*\\(");

    @Override
    public List<FxmlComponent> extractComponents(final String fxmlContent) {
        if (fxmlContent == null || fxmlContent.isBlank()) {
            return List.of();
        }
        List<FxmlComponent> components;
        components = parseXmlComponents(fxmlContent);
        return components;
    }

    @Override
    public List<HandlerBinding> extractHandlers(final String javaContent) {
        if (javaContent == null || javaContent.isBlank()) {
            return List.of();
        }
        List<HandlerBinding> handlers;
        handlers = parseJavaHandlers(javaContent);
        return handlers;
    }

    private List<FxmlComponent> parseXmlComponents(final String fxmlContent) {
        List<FxmlComponent> result;
        result = new ArrayList<>();
        try {
            DocumentBuilderFactory factory;
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder;
            builder = factory.newDocumentBuilder();
            byte[] bytes;
            bytes = fxmlContent.getBytes(StandardCharsets.UTF_8);
            InputStream stream;
            stream = new ByteArrayInputStream(bytes);
            Document document;
            document = builder.parse(stream);
            document.getDocumentElement().normalize();
            NodeList allNodes;
            allNodes = document.getElementsByTagName("*");
            int length;
            length = allNodes.getLength();
            for (int i = 0; i < length; i++) {
                Node node;
                node = allNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element;
                element = (Element) node;
                NamedNodeMap attributes;
                attributes = element.getAttributes();
                Node fxIdNode;
                fxIdNode = attributes.getNamedItem(FXML_ID_ATTR);
                if (fxIdNode == null) {
                    continue;
                }
                String fxId;
                fxId = fxIdNode.getNodeValue();
                String componentType;
                componentType = element.getLocalName();
                if (componentType == null) {
                    componentType = element.getNodeName();
                }
                Node onActionNode;
                onActionNode = attributes.getNamedItem(ON_ACTION_ATTR);
                String eventHandler;
                eventHandler = "";
                if (onActionNode != null) {
                    String rawHandler;
                    rawHandler = onActionNode.getNodeValue();
                    eventHandler = rawHandler.startsWith("#") ? rawHandler.substring(1) : rawHandler;
                }
                FxmlComponent component;
                component = new FxmlComponent(fxId, componentType, eventHandler);
                result.add(component);
                log.debug("Composant extrait - fxId={}, type={}, handler={}", fxId, componentType, eventHandler);
            }
        } catch (Exception e) {
            log.error("Erreur de parsing FXML - contenu invalide", e);
            FxmlComponent errorComponent;
            errorComponent = new FxmlComponent("PARSE_ERROR", "UNKNOWN", "");
            result.add(errorComponent);
        }
        return result;
    }

    private List<HandlerBinding> parseJavaHandlers(final String javaContent) {
        List<HandlerBinding> result;
        result = new ArrayList<>();
        Matcher matcher;
        matcher = HANDLER_PATTERN.matcher(javaContent);
        while (matcher.find()) {
            String methodName;
            methodName = matcher.group(1);
            HandlerBinding binding;
            binding = new HandlerBinding(methodName, "unknown", "void");
            result.add(binding);
            log.debug("Handler extrait - methode={}", methodName);
        }
        return result;
    }
}
