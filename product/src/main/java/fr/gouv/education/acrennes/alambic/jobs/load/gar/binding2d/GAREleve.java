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
//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2019.05.02 à 09:45:42 AM CEST 
//


package fr.gouv.education.acrennes.alambic.jobs.load.gar.binding2d;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Classe Java pour GAREleve complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="GAREleve">
 *   &lt;complexContent>
 *     &lt;extension base="{http://data.education.fr/ns/gar}GARIdentite">
 *       &lt;sequence>
 *         &lt;element name="GARPersonEtab" type="{http://data.education.fr/ns/gar}GARStructureUAIType" maxOccurs="unbounded"/>
 *         &lt;element name="GARPersonDateNaissance" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GAREleve", propOrder = {
    "garPersonEtab",
    "garPersonDateNaissance"
})
public class GAREleve
    extends GARIdentite
{

    @XmlElement(name = "GARPersonEtab", required = true)
    protected List<String> garPersonEtab;
    @XmlElement(name = "GARPersonDateNaissance")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar garPersonDateNaissance;

    /**
     * Gets the value of the garPersonEtab property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garPersonEtab property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARPersonEtab().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getGARPersonEtab() {
        if (garPersonEtab == null) {
            garPersonEtab = new ArrayList<String>();
        }
        return this.garPersonEtab;
    }

    /**
     * Obtient la valeur de la propriété garPersonDateNaissance.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getGARPersonDateNaissance() {
        return garPersonDateNaissance;
    }

    /**
     * Définit la valeur de la propriété garPersonDateNaissance.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setGARPersonDateNaissance(XMLGregorianCalendar value) {
        this.garPersonDateNaissance = value;
    }

}
