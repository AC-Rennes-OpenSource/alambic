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
package fr.gouv.education.acrennes.alambic.jobs.transform;

import au.com.bytecode.opencsv.CSVWriter;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.CsvToStateBase;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityTrafficLight;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StateBaseToFile extends AbstractDestination {

    private final FileWriter fw;
    private final Variables variables = new Variables();
    private int count = 0;
    private List<Map<String, List<String>>> extGeneric = null;
    private String version;
    private String format;

    public StateBaseToFile(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
        super(context, job, jobActivity);

        String path = job.getChildText("path");
        if (StringUtils.isBlank(path)) {
            throw new AlambicException("le path n'est pas precise");
        } else {
            path = context.resolvePath(path);
        }

        format = job.getChildText("format");
        if (StringUtils.isBlank(format)) {
            throw new AlambicException("le format n'est pas precise");
        } else {
            format = context.resolveString(format);
        }

        version = job.getChildText("version");
        if (StringUtils.isNotBlank(version)) {
            version = context.resolveString(version);
        }

        try {
            count = 0;
            fw = new FileWriter(path);
            // Chargement des variables de la liste de jobs
            reloadVariablesList();
        } catch (final IOException e) {
            throw new AlambicException(e.getMessage());
        }
    }

    private void reloadVariablesList() throws AlambicException {
        this.variables.clearTable();
        if (this.context.getVariables() != null) {
            this.variables.loadFromMap(this.context.getVariables().getHashMap());
        }
        // execution des éventuelles fonctions chargées dans la table de
        this.variables.executeFunctions();
    }

    public int executeCsvExportation() throws IOException {
        final String delimiter = ";";
        final String endOfLine = "\n";
        for (Map<String, List<String>> stringListMap : extGeneric) {
            // activity monitoring
            jobActivity.setProgress(((count + 1) * 100) / extGeneric.size());
            jobActivity.setProcessing("processing entry " + (count + 1) + "/" + extGeneric.size());

            StringBuilder s = new StringBuilder();
            final Iterator<List<String>> j = stringListMap.values().iterator();
            while (j.hasNext()) {
                final String subString = j.next().get(0);
                if (j.hasNext()) {
                    s.append(subString).append(delimiter);
                } else {
                    s.append(subString).append(endOfLine);
                }
            }
            fw.write(s.toString());
            count++;
        }
        fw.close();
        return count;
    }

    public int executeFormatedExportation(final String stringFormat) throws IOException, AlambicException {
        if (StringUtils.isNotBlank(stringFormat)) {
            // write header
            fw.write(stringFormat.replace("%", "") + "\n");

            // fill content
            for (Map<String, List<String>> stringListMap : extGeneric) {
                // activity monitoring
                jobActivity.setProgress(((count + 1) * 100) / extGeneric.size());
                jobActivity.setProcessing("processing entry " + (count + 1) + "/" + extGeneric.size());

                // remplacement des variables à partir d'une MAP
                // Chargement de la liste de variables
                reloadVariablesList();
                this.variables.loadFromExtraction(stringListMap);
                // variables.executeFunctions();
                String s = this.variables.resolvString(stringFormat);
                s = Functions.getInstance().executeAllFunctions(s);

                // Execution des fonctions
                if (s != null) {
                    fw.write(s + "\n");
                }
                count++;
            }
            fw.close();
        }

        return count;
    }

    public int executeFormatedExportationV2(final String stringFormat) throws IOException {
        if (StringUtils.isNotBlank(stringFormat)) {
            try (CSVWriter csvw = new CSVWriter(fw, CsvToStateBase.DEFAULT_SEPARATOR)) {
                // Header
                final String[] header = stringFormat.replaceAll("%", "").split(";");
                csvw.writeNext(header);

                // Data
                for (final Map<String, List<String>> item : extGeneric) {
                    // activity monitoring
                    jobActivity.setProgress(((count + 1) * 100) / extGeneric.size());
                    jobActivity.setProcessing("processing entry " + (count + 1) + "/" + extGeneric.size());

                    final String[] line = new String[header.length];
                    for (int i = 0; i < header.length; i++) {
                        line[i] = item.get(header[i]).get(0);
                    }
                    csvw.writeNext(line);
                    count++;
                }
            }
        }
        fw.close();
        return count;
    }

    public int executeXmlExportation(final String stringFormat) throws IOException, AlambicException {
        for (Map<String, List<String>> stringListMap : extGeneric) {
            // activity monitoring
            jobActivity.setProgress(((count + 1) * 100) / extGeneric.size());
            jobActivity.setProcessing("processing entry " + (count + 1) + "/" + extGeneric.size());

            // remplacement des variables � partir d'une MAP
            // Chargement de la liste de variables
            final Variables svp = new Variables();
            svp.loadFromExtraction(stringListMap);
            svp.executeFunctions();
            final String s = svp.resolvString(stringFormat);
            // Execution des fonctions
            if (s != null) {
                fw.write(s + "\n");
            }
            count++;
        }
        fw.close();
        return count;
    }

    @Override
    public void execute() throws AlambicException {
        try {
            // Get entries
            if (null != source) {
                extGeneric = source.getEntries();
            } else {
                extGeneric = Collections.emptyList();
            }

            // export them into CSV format according to the required version
            if (StringUtils.isBlank(version)) {
                executeFormatedExportation(format);
            } else {
                executeFormatedExportationV2(format);
            }
        } catch (final IOException e) {
            jobActivity.setTrafficLight(ActivityTrafficLight.RED);
            throw new AlambicException(e.getMessage());
        }
    }

}
