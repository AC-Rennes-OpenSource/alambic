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


/**
 * <p>Classe Java pour GARMEF complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="GARMEF">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARStructureUAI" type="{http://data.education.fr/ns/gar}GARStructureUAIType"/>
 *         &lt;element name="GARMEFCode" type="{http://data.education.fr/ns/gar}GARMEFCodeType"/>
 *         &lt;element name="GARMEFLibelle" type="{http://data.education.fr/ns/gar}GARMEFLibelleType"/>
 *         &lt;element name="GARMEFRattach" type="{http://data.education.fr/ns/gar}GARMEFCodeType" minOccurs="0"/>
 *         &lt;element name="GARMEFSTAT11" type="{http://data.education.fr/ns/gar}GARMEFSTAT11Type" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GARMEF", propOrder = {
    "garStructureUAI",
    "garmefCode",
    "garmefLibelle",
    "garmefRattach",
    "garmefstat11"
})
public class GARMEF {

    @XmlElement(name = "GARStructureUAI", required = true)
    protected String garStructureUAI;
    @XmlElement(name = "GARMEFCode", required = true)
    protected String garmefCode;
    @XmlElement(name = "GARMEFLibelle", required = true)
    protected String garmefLibelle;
    @XmlElement(name = "GARMEFRattach")
    protected String garmefRattach;
    @XmlElement(name = "GARMEFSTAT11")
    protected String garmefstat11;

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
     * Obtient la valeur de la propriété garmefCode.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARMEFCode() {
        return garmefCode;
    }

    /**
     * Définit la valeur de la propriété garmefCode.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARMEFCode(String value) {
        this.garmefCode = value;
    }

    /**
     * Obtient la valeur de la propriété garmefLibelle.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARMEFLibelle() {
        return garmefLibelle;
    }

    /**
     * Définit la valeur de la propriété garmefLibelle.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARMEFLibelle(String value) {
        this.garmefLibelle = value;
    }

    /**
     * Obtient la valeur de la propriété garmefRattach.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARMEFRattach() {
        return garmefRattach;
    }

    /**
     * Définit la valeur de la propriété garmefRattach.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARMEFRattach(String value) {
        this.garmefRattach = value;
    }

    /**
     * Obtient la valeur de la propriété garmefstat11.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARMEFSTAT11() {
        return garmefstat11;
    }

    /**
     * Définit la valeur de la propriété garmefstat11.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARMEFSTAT11(String value) {
        this.garmefstat11 = value;
    }

}
