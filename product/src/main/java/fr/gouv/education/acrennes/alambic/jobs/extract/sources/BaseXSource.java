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
package fr.gouv.education.acrennes.alambic.jobs.extract.sources;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.BaseXToStateBase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BaseXSource extends AbstractSource {

    private static final Log log = LogFactory.getLog(BaseXSource.class);

    private int pageSize;

    public BaseXSource(final CallableContext context, final Element sourceNode) throws AlambicException {
        super(context, sourceNode);
    }

    @Override
    public void initialize(Element sourceNode) throws AlambicException {

        String host = sourceNode.getChildText("host");
        if (StringUtils.isBlank(host)) {
            log.error("le host du connecteur n'est pas precisé");
        } else {
            host = context.resolveString(host);
        }

        String port = sourceNode.getChildText("port");
        if (StringUtils.isBlank(port)) {
            log.error("le port du connecteur n'est pas precisé.");
        } else {
            port = context.resolveString(port);
        }

        String database = sourceNode.getChildText("database");
        if (StringUtils.isNotBlank(database)) {
            database = context.resolveString(database);
        } else {
            log.debug("la base de données cible n'est pas precisée.");
        }

        // Get proxy configuration
        String proxy_host = null;
        String proxy_port = null;
        Element proxyElt = sourceNode.getChild("proxy");
        if (null != proxyElt) {
            proxy_host = context.resolveString(proxyElt.getChildText("host"));
            proxy_port = context.resolveString(proxyElt.getChildText("port"));
        } else {
            log.debug("Pas de configuration de proxy");
        }

        // Get the connection timeout
        String timeout = sourceNode.getAttributeValue("connectionTimeout");

        // Get authentication configuration
        String auth_login = null;
        String auth_password = null;
        XPathFactory xpf = XPathFactory.instance();
        XPathExpression<Element> xpath = xpf.compile(".//authentication/credentials", Filters.element());
        List<Element> credentialsElts = xpath.evaluate(sourceNode);
        if (null != credentialsElts && !credentialsElts.isEmpty()) {
            auth_login = context.resolveString(credentialsElts.get(0).getChildText("login"));
            auth_password = context.resolveString(credentialsElts.get(0).getChildText("password"));
        } // else, no authentication

        query = sourceNode.getChildText("query"); // might be empty if either databases list or database inner content listing is required

        String page = sourceNode.getAttributeValue("page");
        if (StringUtils.isNotBlank(page)) {
            this.pageSize = Integer.parseInt(context.resolveString(page));
        } else {
            this.pageSize = 0;
        }

        setClient(new BaseXToStateBase(host, Integer.parseInt(port), database, proxy_host, proxy_port, timeout, auth_login, auth_password));
    }

    @Override
    public Iterator<List<Map<String, List<String>>>> getPageIterator() throws AlambicException {
        return getClient().getPageIterator(this.query, null, this.pageSize, null, null);
    }

}
