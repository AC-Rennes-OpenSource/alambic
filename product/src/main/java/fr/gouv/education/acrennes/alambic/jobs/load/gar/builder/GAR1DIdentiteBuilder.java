/*******************************************************************************
 * Copyright (C) 2019-2021 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.builder;

import fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d.GARIdentite;
import fr.gouv.education.acrennes.alambic.jobs.load.gar.builder.exception.MissingAttributeException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class GAR1DIdentiteBuilder extends GAR1DBuilder {

    private static final Log log = LogFactory.getLog(GAR1DIdentiteBuilder.class);

    // Attributes applying to the identities list
    protected final List<String> memberStructuresList;

    // Attributes applying to a single identity
    protected String structRattach;
    protected String personIdentifiant;

    protected GAR1DIdentiteBuilder(GARBuilderParameters parameters) {
        super(parameters);
        memberStructuresList = new ArrayList<>();
        parameters.getResources().get("Structures").getEntries().stream().map(structure -> structure.get("ENTStructureUAI")).forEach(entStructureUAI -> {
            if (null != entStructureUAI && 1 == entStructureUAI.size()) {
                memberStructuresList.add(entStructureUAI.get(0).toUpperCase());
            }
        });
    }

    protected void buildIdentite(GARIdentite garIdentite, Map<String, List<String>> entity) throws MissingAttributeException {
        personIdentifiant = getMandatoryAttribute(entity, "ENTPersonUid");
        garIdentite.setGARPersonIdentifiant(personIdentifiant);

        setGARCivilite(garIdentite, entity);

        handleOptionalAttribute(entity, "ENTPersonStructRattach", value -> {
            String uai = value.toUpperCase();
            structRattach = memberStructuresList.contains(uai) ? uai : "";
            garIdentite.setGARPersonStructRattach(structRattach);
        });

        handleOptionalAttribute(entity, "ENTPersonDateNaissance", value -> {
            try {
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                Date date = dateFormat.parse(value);
                GregorianCalendar gc = new GregorianCalendar();
                gc.setTime(date);
                XMLGregorianCalendar xmlgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
                setGARDateNaissance(garIdentite, xmlgc);
            } catch (ParseException | DatatypeConfigurationException e) {
                jobActivity.setTrafficLight(ActivityTrafficLight.ORANGE);
                log.warn("Failed to parse the attribute 'ENTPersonDateNaissance', might not match the following expected date format 'dd/MM/yyyy', " +
                         "error: " + e.getMessage());
            }
        });

        garIdentite.setGARPersonNom(getMandatoryAttribute(entity, "sn"));

        String givenName = getMandatoryAttribute(entity, "givenName");
        garIdentite.setGARPersonPrenom(givenName);
        garIdentite.getGARPersonAutresPrenoms().add(givenName);

        handleOptionalList(entity, "ENTPersonAutresPrenoms", value -> {
            if (StringUtils.isNotBlank(value) && !garIdentite.getGARPersonAutresPrenoms().contains(value)) {
                garIdentite.getGARPersonAutresPrenoms().add(value);
            }
        });

        handleOptionalAttribute(entity, "ENTPersonNomPatro", garIdentite::setGARPersonNomPatro);
    }

    protected abstract void setGARCivilite(GARIdentite garIdentite, Map<String, List<String>> entity);

    protected abstract void setGARDateNaissance(GARIdentite garIdentite, XMLGregorianCalendar xmlgc);
}
