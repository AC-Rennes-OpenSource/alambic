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
 * <p>Classe Java pour GAREtab complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="GAREtab">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARStructureUAI" type="{http://data.education.fr/ns/gar}GARStructureUAIType"/>
 *         &lt;element name="GARStructureNomCourant" type="{http://data.education.fr/ns/gar}GARStructureNomCourantType"/>
 *         &lt;element name="GAREtablissementStructRattachFctl" type="{http://data.education.fr/ns/gar}GAREtablissementStructRattachFctlType" minOccurs="0"/>
 *         &lt;element name="GARStructureContrat" type="{http://data.education.fr/ns/gar}GARStructureContratType" minOccurs="0"/>
 *         &lt;element name="GARStructureTelephone" type="{http://data.education.fr/ns/gar}GARStructureTelephoneType" minOccurs="0"/>
 *         &lt;element name="GARStructureEmail" type="{http://data.education.fr/ns/gar}GARStructureEmailType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GAREtab", propOrder = {
    "garStructureUAI",
    "garStructureNomCourant",
    "garEtablissementStructRattachFctl",
    "garStructureContrat",
    "garStructureTelephone",
    "garStructureEmail"
})
public class GAREtab {

    @XmlElement(name = "GARStructureUAI", required = true)
    protected String garStructureUAI;
    @XmlElement(name = "GARStructureNomCourant", required = true)
    protected String garStructureNomCourant;
    @XmlElement(name = "GAREtablissementStructRattachFctl")
    protected String garEtablissementStructRattachFctl;
    @XmlElement(name = "GARStructureContrat")
    protected String garStructureContrat;
    @XmlElement(name = "GARStructureTelephone")
    protected String garStructureTelephone;
    @XmlElement(name = "GARStructureEmail")
    protected String garStructureEmail;

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
     * Obtient la valeur de la propriété garStructureNomCourant.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARStructureNomCourant() {
        return garStructureNomCourant;
    }

    /**
     * Définit la valeur de la propriété garStructureNomCourant.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARStructureNomCourant(String value) {
        this.garStructureNomCourant = value;
    }

    /**
     * Obtient la valeur de la propriété garEtablissementStructRattachFctl.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGAREtablissementStructRattachFctl() {
        return garEtablissementStructRattachFctl;
    }

    /**
     * Définit la valeur de la propriété garEtablissementStructRattachFctl.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGAREtablissementStructRattachFctl(String value) {
        this.garEtablissementStructRattachFctl = value;
    }

    /**
     * Obtient la valeur de la propriété garStructureContrat.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARStructureContrat() {
        return garStructureContrat;
    }

    /**
     * Définit la valeur de la propriété garStructureContrat.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARStructureContrat(String value) {
        this.garStructureContrat = value;
    }

    /**
     * Obtient la valeur de la propriété garStructureTelephone.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARStructureTelephone() {
        return garStructureTelephone;
    }

    /**
     * Définit la valeur de la propriété garStructureTelephone.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARStructureTelephone(String value) {
        this.garStructureTelephone = value;
    }

    /**
     * Obtient la valeur de la propriété garStructureEmail.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARStructureEmail() {
        return garStructureEmail;
    }

    /**
     * Définit la valeur de la propriété garStructureEmail.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARStructureEmail(String value) {
        this.garStructureEmail = value;
    }

}
