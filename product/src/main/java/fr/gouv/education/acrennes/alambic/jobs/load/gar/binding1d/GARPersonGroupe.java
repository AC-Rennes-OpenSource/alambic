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
 * <p>Classe Java pour GARPersonGroupe complex type.
 * 
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="GARPersonGroupe">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARStructureUAI" type="{http://data.education.fr/ns/gar/1d}GARStructureUAIType"/>
 *         &lt;element name="GARPersonIdentifiant" type="{http://data.education.fr/ns/gar/1d}GARPersonIdentifiantType"/>
 *         &lt;element name="GARGroupeCode" type="{http://data.education.fr/ns/gar/1d}GARGroupeCodeType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GARPersonGroupe", propOrder = {
    "garStructureUAI",
    "garPersonIdentifiant",
    "garGroupeCode"
})
public class GARPersonGroupe {

    @XmlElement(name = "GARStructureUAI", required = true)
    protected String garStructureUAI;
    @XmlElement(name = "GARPersonIdentifiant", required = true)
    protected String garPersonIdentifiant;
    @XmlElement(name = "GARGroupeCode", required = true)
    protected String garGroupeCode;

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

}
