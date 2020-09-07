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


package fr.gouv.education.acrennes.alambic.jobs.load.gar.binding;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour GARGroupe complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="GARGroupe">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARGroupeCode" type="{http://data.education.fr/ns/gar}GARGroupeCodeType"/>
 *         &lt;element name="GARStructureUAI" type="{http://data.education.fr/ns/gar}GARStructureUAIType"/>
 *         &lt;element name="GARGroupeLibelle" type="{http://data.education.fr/ns/gar}GARGroupeLibelleType"/>
 *         &lt;element name="GARGroupeStatut" type="{http://data.education.fr/ns/gar}GARGroupeStatutType"/>
 *         &lt;element name="GARGroupeDivAppartenance" type="{http://data.education.fr/ns/gar}GARGroupeCodeType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GARGroupe", propOrder = {
    "garGroupeCode",
    "garStructureUAI",
    "garGroupeLibelle",
    "garGroupeStatut",
    "garGroupeDivAppartenance"
})
public class GARGroupe {

    @XmlElement(name = "GARGroupeCode", required = true)
    protected String garGroupeCode;
    @XmlElement(name = "GARStructureUAI", required = true)
    protected String garStructureUAI;
    @XmlElement(name = "GARGroupeLibelle", required = true)
    protected String garGroupeLibelle;
    @XmlElement(name = "GARGroupeStatut", required = true)
    protected String garGroupeStatut;
    @XmlElement(name = "GARGroupeDivAppartenance")
    protected List<String> garGroupeDivAppartenance;

    /**
     * Obtient la valeur de la propriété garGroupeCode.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARGroupeCode() {
        return garGroupeCode;
    }

    /**
     * Définit la valeur de la propriété garGroupeCode.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARGroupeCode(String value) {
        this.garGroupeCode = value;
    }

    /**
     * Obtient la valeur de la propriété garStructureUAI.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARStructureUAI() {
        return garStructureUAI;
    }

    /**
     * Définit la valeur de la propriété garStructureUAI.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARStructureUAI(String value) {
        this.garStructureUAI = value;
    }

    /**
     * Obtient la valeur de la propriété garGroupeLibelle.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARGroupeLibelle() {
        return garGroupeLibelle;
    }

    /**
     * Définit la valeur de la propriété garGroupeLibelle.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARGroupeLibelle(String value) {
        this.garGroupeLibelle = value;
    }

    /**
     * Obtient la valeur de la propriété garGroupeStatut.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARGroupeStatut() {
        return garGroupeStatut;
    }

    /**
     * Définit la valeur de la propriété garGroupeStatut.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARGroupeStatut(String value) {
        this.garGroupeStatut = value;
    }

    /**
     * Gets the value of the garGroupeDivAppartenance property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garGroupeDivAppartenance property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARGroupeDivAppartenance().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getGARGroupeDivAppartenance() {
        if (garGroupeDivAppartenance == null) {
            garGroupeDivAppartenance = new ArrayList<String>();
        }
        return this.garGroupeDivAppartenance;
    }

}
