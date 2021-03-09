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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour GARIdentite complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="GARIdentite">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARPersonIdentifiant" type="{http://data.education.fr/ns/gar}GARPersonIdentifiantType"/>
 *         &lt;element name="GARPersonProfils" type="{http://data.education.fr/ns/gar}GARPersonProfils" maxOccurs="unbounded"/>
 *         &lt;element name="GARPersonIdSecondaire" type="{http://data.education.fr/ns/gar}GARPersonIdSecondaireType" minOccurs="0"/>
 *         &lt;element name="GARPersonNomPatro" type="{http://data.education.fr/ns/gar}GARPersonNomPatroType" minOccurs="0"/>
 *         &lt;element name="GARPersonNom" type="{http://data.education.fr/ns/gar}GARPersonNomType"/>
 *         &lt;element name="GARPersonPrenom" type="{http://data.education.fr/ns/gar}GARPersonPrenomType"/>
 *         &lt;element name="GARPersonAutresPrenoms" type="{http://data.education.fr/ns/gar}GARPersonAutresPrenomsType" maxOccurs="unbounded"/>
 *         &lt;element name="GARPersonCivilite" type="{http://data.education.fr/ns/gar}GARPersonCiviliteType" minOccurs="0"/>
 *         &lt;element name="GARPersonStructRattach" type="{http://data.education.fr/ns/gar}GARStructureUAIType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GARIdentite", propOrder = {
    "garPersonIdentifiant",
    "garPersonProfils",
    "garPersonIdSecondaire",
    "garPersonNomPatro",
    "garPersonNom",
    "garPersonPrenom",
    "garPersonAutresPrenoms",
    "garPersonCivilite",
    "garPersonStructRattach"
})
@XmlSeeAlso({
    GAREnseignant.class,
    GAREleve.class
})
public class GARIdentite {

    @XmlElement(name = "GARPersonIdentifiant", required = true)
    protected String garPersonIdentifiant;
    @XmlElement(name = "GARPersonProfils", required = true)
    protected List<GARPersonProfils> garPersonProfils;
    @XmlElement(name = "GARPersonIdSecondaire")
    protected String garPersonIdSecondaire;
    @XmlElement(name = "GARPersonNomPatro")
    protected String garPersonNomPatro;
    @XmlElement(name = "GARPersonNom", required = true)
    protected String garPersonNom;
    @XmlElement(name = "GARPersonPrenom", required = true)
    protected String garPersonPrenom;
    @XmlElement(name = "GARPersonAutresPrenoms", required = true)
    protected List<String> garPersonAutresPrenoms;
    @XmlElement(name = "GARPersonCivilite")
    protected String garPersonCivilite;
    @XmlElement(name = "GARPersonStructRattach")
    protected String garPersonStructRattach;

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
     * Gets the value of the garPersonProfils property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garPersonProfils property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARPersonProfils().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GARPersonProfils }
     * 
     * 
     */
    public List<GARPersonProfils> getGARPersonProfils() {
        if (garPersonProfils == null) {
            garPersonProfils = new ArrayList<GARPersonProfils>();
        }
        return this.garPersonProfils;
    }

    /**
     * Obtient la valeur de la propriété garPersonIdSecondaire.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARPersonIdSecondaire() {
        return garPersonIdSecondaire;
    }

    /**
     * Définit la valeur de la propriété garPersonIdSecondaire.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARPersonIdSecondaire(String value) {
        this.garPersonIdSecondaire = value;
    }

    /**
     * Obtient la valeur de la propriété garPersonNomPatro.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARPersonNomPatro() {
        return garPersonNomPatro;
    }

    /**
     * Définit la valeur de la propriété garPersonNomPatro.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARPersonNomPatro(String value) {
        this.garPersonNomPatro = value;
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
     * Gets the value of the garPersonAutresPrenoms property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garPersonAutresPrenoms property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARPersonAutresPrenoms().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getGARPersonAutresPrenoms() {
        if (garPersonAutresPrenoms == null) {
            garPersonAutresPrenoms = new ArrayList<String>();
        }
        return this.garPersonAutresPrenoms;
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
     * Obtient la valeur de la propriété garPersonStructRattach.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGARPersonStructRattach() {
        return garPersonStructRattach;
    }

    /**
     * Définit la valeur de la propriété garPersonStructRattach.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGARPersonStructRattach(String value) {
        this.garPersonStructRattach = value;
    }

}
