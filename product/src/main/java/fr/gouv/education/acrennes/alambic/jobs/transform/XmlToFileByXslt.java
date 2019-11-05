/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;

public class XmlToFileByXslt {

    public static void transform(String xml, String xslt, String pivot) throws AlambicException {
        DocumentBuilderFactory fabriqueD = DocumentBuilderFactory.newInstance();
        DocumentBuilder constructeur;
        try {
            constructeur = fabriqueD.newDocumentBuilder();

            File fileXml = new File(xml);
            Document document = constructeur.parse(fileXml);
            Source domSource = new DOMSource(document);

            // Création du fichier de sortie
            File fichierDest = new File(pivot);
            Result resultat = new StreamResult(fichierDest.toURI().getPath());

            // Configuration du transformer
            TransformerFactory fabriqueT = TransformerFactory.newInstance();
            StreamSource stylesource = new StreamSource(xslt);
            Transformer transformer = fabriqueT.newTransformer(stylesource);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            // Transformation
            transformer.transform(domSource, resultat);

        } catch (ParserConfigurationException|SAXException|IOException|TransformerException e) {
        	throw new AlambicException(e);
        }
    }

}
