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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Classe Java pour GARRespAff complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="GARRespAff">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARPersonIdentifiant" type="{http://data.education.fr/ns/gar}GARPersonIdentifiantType"/>
 *         &lt;element name="GARPersonNom" type="{http://data.education.fr/ns/gar}GARPersonNomType"/>
 *         &lt;element name="GARPersonPrenom" type="{http://data.education.fr/ns/gar}GARPersonPrenomType"/>
 *         &lt;element name="GARPersonCivilite" type="{http://data.education.fr/ns/gar}GARPersonCiviliteType" minOccurs="0"/>
 *         &lt;element name="GARPersonMail" type="{http://data.education.fr/ns/gar}GARPersonMailType" maxOccurs="unbounded"/>
 *         &lt;element name="GARRespAffEtab" type="{http://data.education.fr/ns/gar}GARStructureUAIType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GARRespAff", propOrder = {
        "garPersonIdentifiant",
        "garPersonNom",
        "garPersonPrenom",
        "garPersonCivilite",
        "garPersonMail",
        "garRespAffEtab"
})
public class GARRespAff {

    @XmlElement(name = "GARPersonIdentifiant", required = true)
    protected String garPersonIdentifiant;
    @XmlElement(name = "GARPersonNom", required = true)
    protected String garPersonNom;
    @XmlElement(name = "GARPersonPrenom", required = true)
    protected String garPersonPrenom;
    @XmlElement(name = "GARPersonCivilite")
    protected String garPersonCivilite;
    @XmlElement(name = "GARPersonMail", required = true)
    protected List<String> garPersonMail;
    @XmlElement(name = "GARRespAffEtab", required = true)
    protected List<String> garRespAffEtab;

    /**
     * Obtient la valeur de la propriété garPersonIdentifiant.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGARPersonIdentifiant() {
        return garPersonIdentifiant;
    }

    /**
     * Définit la valeur de la propriété garPersonIdentifiant.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGARPersonIdentifiant(String value) {
        this.garPersonIdentifiant = value;
    }

    /**
     * Obtient la valeur de la propriété garPersonNom.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGARPersonNom() {
        return garPersonNom;
    }

    /**
     * Définit la valeur de la propriété garPersonNom.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGARPersonNom(String value) {
        this.garPersonNom = value;
    }

    /**
     * Obtient la valeur de la propriété garPersonPrenom.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGARPersonPrenom() {
        return garPersonPrenom;
    }

    /**
     * Définit la valeur de la propriété garPersonPrenom.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGARPersonPrenom(String value) {
        this.garPersonPrenom = value;
    }

    /**
     * Obtient la valeur de la propriété garPersonCivilite.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGARPersonCivilite() {
        return garPersonCivilite;
    }

    /**
     * Définit la valeur de la propriété garPersonCivilite.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGARPersonCivilite(String value) {
        this.garPersonCivilite = value;
    }

    /**
     * Gets the value of the garPersonMail property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garPersonMail property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARPersonMail().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getGARPersonMail() {
        if (garPersonMail == null) {
            garPersonMail = new ArrayList<>();
        }
        return this.garPersonMail;
    }

    /**
     * Gets the value of the garRespAffEtab property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garRespAffEtab property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARRespAffEtab().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getGARRespAffEtab() {
        if (garRespAffEtab == null) {
            garRespAffEtab = new ArrayList<>();
        }
        return this.garRespAffEtab;
    }

}
