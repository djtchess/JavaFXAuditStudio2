package ff.ss.javaFxAuditStudio.adapters.out.analysis;

import ff.ss.javaFxAuditStudio.application.ports.out.CartographyAnalysisPort;
import ff.ss.javaFxAuditStudio.domain.cartography.CartographyUnknown;
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
 * Extrait les composants ayant un fx:id, les handlers Java via regex,
 * et les elements non standard (imports, fx:include, bindings, stylesheets).
 * Instancie par CartographyConfiguration — pas d'annotation Spring.
 */
public final class FxmlCartographyAnalysisAdapter implements CartographyAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(FxmlCartographyAnalysisAdapter.class);

    private static final String FXML_ID_ATTR = "fx:id";
    private static final String ON_ACTION_ATTR = "onAction";
    private static final Pattern HANDLER_PATTERN =
            Pattern.compile("@FXML\\s+(?:public\\s+)?void\\s+(\\w+)\\s*\\(");
    private static final Pattern IMPORT_PATTERN =
            Pattern.compile("<\\?import\\s+([^?]+)\\s*\\?>");
    private static final Pattern BINDING_PATTERN =
            Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public List<FxmlComponent> extractComponents(final String fxmlContent) {
        if (fxmlContent == null || fxmlContent.isBlank()) {
            return List.of();
        }
        return parseXmlComponents(fxmlContent);
    }

    @Override
    public List<HandlerBinding> extractHandlers(final String javaContent) {
        if (javaContent == null || javaContent.isBlank()) {
            return List.of();
        }
        return parseJavaHandlers(javaContent);
    }

    @Override
    public List<CartographyUnknown> extractUnknowns(final String fxmlContent) {
        if (fxmlContent == null || fxmlContent.isBlank()) {
            return List.of();
        }
        List<CartographyUnknown> unknowns = new ArrayList<>();
        extractImports(fxmlContent, unknowns);
        extractBindings(fxmlContent, unknowns);
        extractFxIncludesAndStylesheets(fxmlContent, unknowns);
        return List.copyOf(unknowns);
    }

    private void extractImports(final String fxmlContent, final List<CartographyUnknown> unknowns) {
        Matcher matcher = IMPORT_PATTERN.matcher(fxmlContent);
        while (matcher.find()) {
            String importedClass = matcher.group(1).trim();
            unknowns.add(new CartographyUnknown("import", importedClass));
            log.debug("Import detecte - classe={}", importedClass);
        }
    }

    private void extractBindings(final String fxmlContent, final List<CartographyUnknown> unknowns) {
        Matcher matcher = BINDING_PATTERN.matcher(fxmlContent);
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            unknowns.add(new CartographyUnknown("binding", expression));
            log.debug("Binding detecte - expression={}", expression);
        }
    }

    private void extractFxIncludesAndStylesheets(
            final String fxmlContent, final List<CartographyUnknown> unknowns) {
        try {
            Document document = parseDocument(fxmlContent);
            NodeList allNodes = document.getElementsByTagName("*");
            int length = allNodes.getLength();
            for (int i = 0; i < length; i++) {
                Node node = allNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                collectFxInclude(element, unknowns);
                collectStylesheets(element, unknowns);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction des unknowns FXML", e);
        }
    }

    private void collectFxInclude(final Element element, final List<CartographyUnknown> unknowns) {
        String localName = element.getLocalName();
        String nodeName = (localName != null) ? localName : element.getNodeName();
        if (!"include".equals(nodeName) && !"fx:include".equals(nodeName)) {
            return;
        }
        String source = element.getAttribute("source");
        if (source != null && !source.isBlank()) {
            unknowns.add(new CartographyUnknown("fx:include", source));
            log.debug("fx:include detecte - source={}", source);
        }
    }

    private void collectStylesheets(final Element element, final List<CartographyUnknown> unknowns) {
        String stylesheets = element.getAttribute("stylesheets");
        if (stylesheets == null || stylesheets.isBlank()) {
            return;
        }
        unknowns.add(new CartographyUnknown("stylesheet", stylesheets));
        log.debug("Stylesheet detecte - path={}", stylesheets);
    }

    private List<FxmlComponent> parseXmlComponents(final String fxmlContent) {
        List<FxmlComponent> result = new ArrayList<>();
        try {
            Document document = parseDocument(fxmlContent);
            NodeList allNodes = document.getElementsByTagName("*");
            int length = allNodes.getLength();
            for (int i = 0; i < length; i++) {
                Node node = allNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                FxmlComponent component = extractComponentFromElement((Element) node);
                if (component != null) {
                    result.add(component);
                }
            }
        } catch (Exception e) {
            log.error("Erreur de parsing FXML - contenu invalide", e);
            result.add(new FxmlComponent("PARSE_ERROR", "UNKNOWN", ""));
        }
        return result;
    }

    private FxmlComponent extractComponentFromElement(final Element element) {
        NamedNodeMap attributes = element.getAttributes();
        Node fxIdNode = attributes.getNamedItem(FXML_ID_ATTR);
        if (fxIdNode == null) {
            return null;
        }
        String fxId = fxIdNode.getNodeValue();
        String componentType = element.getLocalName();
        if (componentType == null) {
            componentType = element.getNodeName();
        }
        Node onActionNode = attributes.getNamedItem(ON_ACTION_ATTR);
        String eventHandler = "";
        if (onActionNode != null) {
            String rawHandler = onActionNode.getNodeValue();
            eventHandler = rawHandler.startsWith("#") ? rawHandler.substring(1) : rawHandler;
        }
        log.debug("Composant extrait - fxId={}, type={}, handler={}", fxId, componentType, eventHandler);
        return new FxmlComponent(fxId, componentType, eventHandler);
    }

    private Document parseDocument(final String fxmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        byte[] bytes = fxmlContent.getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(bytes);
        Document document = builder.parse(stream);
        document.getDocumentElement().normalize();
        return document;
    }

    private List<HandlerBinding> parseJavaHandlers(final String javaContent) {
        List<HandlerBinding> result = new ArrayList<>();
        Matcher matcher = HANDLER_PATTERN.matcher(javaContent);
        while (matcher.find()) {
            String methodName = matcher.group(1);
            result.add(new HandlerBinding(methodName, "unknown", "void"));
            log.debug("Handler extrait - methode={}", methodName);
        }
        return result;
    }
}
