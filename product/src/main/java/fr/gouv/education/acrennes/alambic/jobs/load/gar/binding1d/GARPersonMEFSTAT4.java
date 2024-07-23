//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2021.02.23 à 04:46:47 PM CET 
//


package fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour GARPersonMEFSTAT4 complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="GARPersonMEFSTAT4">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARStructureUAI" type="{http://data.education.fr/ns/gar/1d}GARStructureUAIType"/>
 *         &lt;element name="GARPersonIdentifiant" type="{http://data.education.fr/ns/gar/1d}GARPersonIdentifiantType"/>
 *         &lt;element name="GARMEFSTAT4Code" type="{http://data.education.fr/ns/gar/1d}GARMEFSTAT4CodeType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GARPersonMEFSTAT4", propOrder = {
        "garStructureUAI",
        "garPersonIdentifiant",
        "garmefstat4Code"
})
public class GARPersonMEFSTAT4 {

    @XmlElement(name = "GARStructureUAI", required = true)
    protected String garStructureUAI;
    @XmlElement(name = "GARPersonIdentifiant", required = true)
    protected String garPersonIdentifiant;
    @XmlElement(name = "GARMEFSTAT4Code", required = true)
    protected String garmefstat4Code;

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
     * Obtient la valeur de la propriété garmefstat4Code.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getGARMEFSTAT4Code() {
        return garmefstat4Code;
    }

    /**
     * Définit la valeur de la propriété garmefstat4Code.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setGARMEFSTAT4Code(String value) {
        this.garmefstat4Code = value;
    }

}
