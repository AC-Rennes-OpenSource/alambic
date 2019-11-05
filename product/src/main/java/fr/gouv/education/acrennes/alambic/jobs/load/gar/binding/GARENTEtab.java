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
//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2019.05.02 à 09:45:42 AM CEST 
//


package fr.gouv.education.acrennes.alambic.jobs.load.gar.binding;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour GAR-ENT-Etab complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="GAR-ENT-Etab">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GAREtab" type="{http://data.education.fr/ns/gar}GAREtab" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GARMEF" type="{http://data.education.fr/ns/gar}GARMEF" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GARMatiere" type="{http://data.education.fr/ns/gar}GARMatiere" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Version" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GAR-ENT-Etab", propOrder = {
    "garEtab",
    "garmef",
    "garMatiere"
})
public class GARENTEtab {

    @XmlElement(name = "GAREtab")
    protected List<GAREtab> garEtab;
    @XmlElement(name = "GARMEF")
    protected List<GARMEF> garmef;
    @XmlElement(name = "GARMatiere")
    protected List<GARMatiere> garMatiere;
    @XmlAttribute(name = "Version", required = true)
    protected String version;

    /**
     * Gets the value of the garEtab property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garEtab property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGAREtab().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GAREtab }
     * 
     * 
     */
    public List<GAREtab> getGAREtab() {
        if (garEtab == null) {
            garEtab = new ArrayList<GAREtab>();
        }
        return this.garEtab;
    }

    /**
     * Gets the value of the garmef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garmef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARMEF().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GARMEF }
     * 
     * 
     */
    public List<GARMEF> getGARMEF() {
        if (garmef == null) {
            garmef = new ArrayList<GARMEF>();
        }
        return this.garmef;
    }

    /**
     * Gets the value of the garMatiere property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garMatiere property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARMatiere().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GARMatiere }
     * 
     * 
     */
    public List<GARMatiere> getGARMatiere() {
        if (garMatiere == null) {
            garMatiere = new ArrayList<GARMatiere>();
        }
        return this.garMatiere;
    }

    /**
     * Obtient la valeur de la propriété version.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Définit la valeur de la propriété version.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

}
