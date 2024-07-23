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

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Classe Java pour GAR-ENT-Groupe complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="GAR-ENT-Groupe">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARGroupe" type="{http://data.education.fr/ns/gar}GARGroupe" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GARPersonGroupe" type="{http://data.education.fr/ns/gar}GARPersonGroupe" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GAREnsGroupeMatiere" type="{http://data.education.fr/ns/gar}GAREnsGroupeMatiere" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GAREnsClasseMatiere" type="{http://data.education.fr/ns/gar}GAREnsClasseMatiere" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "GAR-ENT-Groupe", propOrder = {
        "garGroupe",
        "garPersonGroupe",
        "garEnsGroupeMatiere",
        "garEnsClasseMatiere"
})
public class GARENTGroupe {

    @XmlElement(name = "GARGroupe")
    protected List<GARGroupe> garGroupe;
    @XmlElement(name = "GARPersonGroupe")
    protected List<GARPersonGroupe> garPersonGroupe;
    @XmlElement(name = "GAREnsGroupeMatiere")
    protected List<GAREnsGroupeMatiere> garEnsGroupeMatiere;
    @XmlElement(name = "GAREnsClasseMatiere")
    protected List<GAREnsClasseMatiere> garEnsClasseMatiere;
    @XmlAttribute(name = "Version", required = true)
    protected String version;

    /**
     * Gets the value of the garGroupe property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garGroupe property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARGroupe().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GARGroupe }
     *
     *
     */
    public List<GARGroupe> getGARGroupe() {
        if (garGroupe == null) {
            garGroupe = new ArrayList<>();
        }
        return this.garGroupe;
    }

    /**
     * Gets the value of the garPersonGroupe property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garPersonGroupe property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARPersonGroupe().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GARPersonGroupe }
     *
     *
     */
    public List<GARPersonGroupe> getGARPersonGroupe() {
        if (garPersonGroupe == null) {
            garPersonGroupe = new ArrayList<>();
        }
        return this.garPersonGroupe;
    }

    /**
     * Gets the value of the garEnsGroupeMatiere property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garEnsGroupeMatiere property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGAREnsGroupeMatiere().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GAREnsGroupeMatiere }
     *
     *
     */
    public List<GAREnsGroupeMatiere> getGAREnsGroupeMatiere() {
        if (garEnsGroupeMatiere == null) {
            garEnsGroupeMatiere = new ArrayList<>();
        }
        return this.garEnsGroupeMatiere;
    }

    /**
     * Gets the value of the garEnsClasseMatiere property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garEnsClasseMatiere property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGAREnsClasseMatiere().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GAREnsClasseMatiere }
     *
     *
     */
    public List<GAREnsClasseMatiere> getGAREnsClasseMatiere() {
        if (garEnsClasseMatiere == null) {
            garEnsClasseMatiere = new ArrayList<>();
        }
        return this.garEnsClasseMatiere;
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
