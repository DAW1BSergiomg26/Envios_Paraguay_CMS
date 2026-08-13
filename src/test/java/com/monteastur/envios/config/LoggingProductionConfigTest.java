package com.monteastur.envios.config;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingProductionConfigTest {

    private Document loadLogbackXml() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/logback-spring.xml")) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            return factory.newDocumentBuilder().parse(in);
        }
    }

    @Test
    void definePerfilProdConAppenderJson() throws Exception {
        Document doc = loadLogbackXml();
        NodeList profiles = doc.getElementsByTagName("springProfile");
        assertThat(profiles.getLength()).isGreaterThan(0);

        boolean prodProfilePresente = false;
        boolean defineAppenderJson = false;
        boolean refAppenderJson = false;
        for (int i = 0; i < profiles.getLength(); i++) {
            Element p = (Element) profiles.item(i);
            if ("prod".equals(p.getAttribute("name"))) {
                prodProfilePresente = true;
                NodeList appenders = p.getElementsByTagName("appender");
                for (int j = 0; j < appenders.getLength(); j++) {
                    if ("CONSOLE_JSON".equals(((Element) appenders.item(j)).getAttribute("name"))) {
                        defineAppenderJson = true;
                    }
                }
                NodeList refs = p.getElementsByTagName("appender-ref");
                for (int j = 0; j < refs.getLength(); j++) {
                    if ("CONSOLE_JSON".equals(((Element) refs.item(j)).getAttribute("ref"))) {
                        refAppenderJson = true;
                    }
                }
            }
        }
        assertThat(prodProfilePresente).isTrue();
        assertThat(defineAppenderJson).isTrue();
        assertThat(refAppenderJson).isTrue();
    }

    @Test
    void consolaJsonUsaLogstashEncoder() throws Exception {
        Document doc = loadLogbackXml();
        NodeList appenders = doc.getElementsByTagName("appender");
        Element jsonAppender = null;
        for (int i = 0; i < appenders.getLength(); i++) {
            Element a = (Element) appenders.item(i);
            if ("CONSOLE_JSON".equals(a.getAttribute("name"))) {
                jsonAppender = a;
                break;
            }
        }
        assertThat(jsonAppender).isNotNull();
        assertThat(jsonAppender.getAttribute("class"))
                .isEqualTo("ch.qos.logback.core.ConsoleAppender");
        Element encoder = (Element) jsonAppender.getElementsByTagName("encoder").item(0);
        assertThat(encoder.getAttribute("class"))
                .isEqualTo("net.logstash.logback.encoder.LogstashEncoder");
    }

    @Test
    void rotacionArchivoRetiene30DiasYErroresSeparados() throws Exception {
        Document doc = loadLogbackXml();
        NodeList fileAppenders = doc.getElementsByTagName("appender");
        boolean rotacionOk = false;
        boolean errorFilterOk = false;
        for (int i = 0; i < fileAppenders.getLength(); i++) {
            Element a = (Element) fileAppenders.item(i);
            String cls = a.getAttribute("class");
            if (cls.contains("RollingFileAppender") && a.getAttribute("name").equals("FILE")) {
                NodeList maxHistory = a.getElementsByTagName("maxHistory");
                assertThat(maxHistory.getLength()).isGreaterThan(0);
                assertThat(Integer.parseInt(maxHistory.item(0).getTextContent().trim()))
                        .isGreaterThanOrEqualTo(30);
                rotacionOk = true;
            }
            if (a.getAttribute("name").equals("ERROR_FILE")) {
                NodeList filters = a.getElementsByTagName("filter");
                assertThat(filters.getLength()).isGreaterThan(0);
                Element filter = (Element) filters.item(0);
                assertThat(filter.getAttribute("class"))
                        .isEqualTo("ch.qos.logback.classic.filter.ThresholdFilter");
                Element level = (Element) filter.getElementsByTagName("level").item(0);
                assertThat(level.getTextContent().trim()).isEqualTo("WARN");
                errorFilterOk = true;
            }
        }
        assertThat(rotacionOk).isTrue();
        assertThat(errorFilterOk).isTrue();
    }
}
