/*******************************************************************************
 * Copyright (C) 2019-2020 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.gouv.education.acrennes.alambic.nuxeo;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.nuxeo.marshaller.EsMarshaller;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.model.IdRef;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationRegistry;
import org.xml.sax.InputSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomationChainRunner extends AbstractDestination {

    protected static final Log log = LogFactory.getLog(AutomationChainRunner.class);

    private final HttpAutomationClient client;
    private final Session session;
    private final Element pivot;
    private int count;
    private OperationRegistry registry = null;

    public AutomationChainRunner(final CallableContext context, final Element destinationNode, final ActivityMBean jobActivity)
            throws AlambicException {
        super(context, destinationNode, jobActivity);

        log.info("  Chargement dans [" + destinationNode.getAttributeValue("name") + "]");

        String uri = destinationNode.getChildText("uri");
        if (uri == null) {
            throw new AlambicException("l'uri de nuxeo n'est pas precisee");
        } else {
            uri = context.resolveString(uri);
        }

        String login = destinationNode.getChildText("login");
        if (login == null) {
            throw new AlambicException("le login de nuxeo n'est pas precise");
        } else {
            login = context.resolveString(login);
        }

        String pwd = destinationNode.getChildText("passwd");
        if (pwd == null) {
            throw new AlambicException("le mot de passe de nuxeo n'est pas precise");
        } else {
            pwd = context.resolveString(pwd);
        }

        String pivotFichier = destinationNode.getChildText("pivot");
        if (pivotFichier == null) {
            throw new AlambicException("le fichier de generation des contenus nuxeo n'est pas precise");
        } else {
            pivotFichier = context.resolvePath(pivotFichier);
        }

        try {
            client = new HttpAutomationClient(uri);
            session = client.getSession(login, pwd);
            final InputSource fPivot = new InputSource(pivotFichier);
            pivot = (new SAXBuilder()).build(fPivot).getRootElement();
            count = 0;

            registry =
                    JsonMarshalling.readRegistry("{\"operations\":[" + OperationPersistFile.getJSONDescription() + "," + OperationGetLocalFile.getJSONDescription() + "," + OperationSetVar.getJSONDescription() + "," + OperationSetInputVar.getJSONDescription() + "," + OperationRestoreDocumentInput.getJSONDescription() + "]}");

            // Add Elastic responses marshaller to support operations like 'Document.QueryES' which lead to request Elastic
            JsonMarshalling.addMarshaller(new EsMarshaller());
        } catch (final Exception e) {
            throw new AlambicException(e.getMessage());
        }
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    @Override
    public void execute() {
        try {
            for (final Map<String, List<String>> currentResult : source.getEntries()) {
                // Itération sur la liste des entrées du pivot
                int index = 1;
                List<Element> chains = pivot.getChild("chains").getChildren();
                for (final Element chain : chains) {
                    // activity monitoring
                    jobActivity.setProgress((index * 100) / chains.size());
                    jobActivity.setProcessing("processing entry " + index++ + "/" + chains.size());

                    final String documentId = ((null != currentResult.get("id")) && (!currentResult.get("id").isEmpty()))
                                              ? currentResult.get("id").get(0)
                                              : null;
                    runChain(documentId, chain);
                }

                count++;
            }
        } catch (final Exception e) {
            jobActivity.setTrafficLight(ActivityTrafficLight.RED);
            log.error(e.getMessage());
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.shutdown();
        }
    }

    private void runChain(String documentId, final Element chain) {
        final String chainId = chain.getAttributeValue("id");

        if (StringUtils.isBlank(documentId)) {
            documentId = chain.getAttributeValue("documentId");
            if (StringUtils.isBlank(documentId)) {
                jobActivity.setTrafficLight(ActivityTrafficLight.RED);
                log.error("No document identifier is provided to run the Nuxeo chain '" + chainId + "'");
                return;
            }
        }

        log.info("Execute the Nuxeo chain '" + chainId + "' on document with id '" + documentId + "'");
        Object output = documentId;
        final Map<String, Object> ctx = new HashMap<>();
        for (final Element operation : chain.getChildren("operation")) {
            output = runOperation(output, operation, ctx);
        }
    }

    private Object runOperation(final Object input, final Element operation, final Map<String, Object> ctx) {
        Object output = null;
        final String operationId = operation.getAttributeValue("id");
        final boolean doIgnoreError = Boolean.parseBoolean(operation.getAttributeValue("ignoreError"));

        try {
            // Create a new operation request
            final OperationRequest request = new AlambicOperationRequest(session, getOperation(operationId), ctx);

            // Set the operation's input
            if (input instanceof String) {
                request.setInput(new IdRef((String) input));
            } else {
                request.setInput(input);
            }

            // Set the operation's parameters
            for (final Element param : operation.getChildren("param")) {
                final String type = param.getAttributeValue("type");
                final String name = param.getAttributeValue("name");
                final String value = param.getValue();
                if (null != getParamValue(value, type)) {
                    request.set(name, getParamValue(value, type));
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.RED);
                    log.error("	Failed to set parameter '" + name + "' of type '" + type + "' and value '" + value + "' on operation '" + operationId + "'");
                }
            }

            // Set the operation request's headers
            for (final Element header : operation.getChildren("header")) {
                final String name = header.getAttributeValue("name");
                final String value = header.getValue();
                if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)) {
                    request.setHeader(name, value);
                } else {
                    jobActivity.setTrafficLight(ActivityTrafficLight.RED);
                    log.error("	Failed to set header '" + name + "' with value '" + value + "' on operation '" + operationId + "'");
                }
            }

            // Execute the request
            output = request.execute();
            log.info("	Executed the Nuxeo operation '" + operationId + "'");
        } catch (final Exception e) {
            if (doIgnoreError) {
                log.info("Ignore the error while executing the operation '" + operationId + "' (input document is : '" + input + "'), error : " + e.getMessage());
            } else {
                jobActivity.setTrafficLight(ActivityTrafficLight.RED);
                log.error(e.getCause());
            }
        }

        return output;
    }

    private Object getParamValue(final String stringValue, final String type) {
        Object value = null;

        if ("string".equals(type) || "serializable".equals(type) || "object".equals(type)) {
            value = stringValue;
        } else if ("boolean".equals(type)) {
            value = Boolean.parseBoolean(stringValue);
        } else if ("stringlist".equals(type)) {
            value = stringValue.split(",");
        } else if ("document".equals(type)) {
            value = new IdRef(stringValue);
        } else if ("integer".equals(type)) {
            value = Integer.valueOf(stringValue);
        }

        return value;
    }

    private OperationDocumentation getOperation(final String operationID) {
        OperationDocumentation operation = registry.getOperation(operationID);
        if (null == operation) {
            operation = session.getOperation(operationID);
        }

        return operation;
    }

}
