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
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Classe Java pour GAREnsSpecialitesPostes complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="GAREnsSpecialitesPostes">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GARStructureUAI" type="{http://data.education.fr/ns/gar/1d}GARStructureUAIType"/>
 *         &lt;element name="GAREnsSpecialitePosteCode" type="{http://data.education.fr/ns/gar/1d}GAREnsSpecialitePosteCodeType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GAREnsSpecialitesPostes", propOrder = {
        "garStructureUAI",
        "garEnsSpecialitePosteCode"
})
public class GAREnsSpecialitesPostes {

    @XmlElement(name = "GARStructureUAI", required = true)
    protected String garStructureUAI;
    @XmlElement(name = "GAREnsSpecialitePosteCode", required = true)
    protected List<String> garEnsSpecialitePosteCode;

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
     * Gets the value of the garEnsSpecialitePosteCode property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garEnsSpecialitePosteCode property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGAREnsSpecialitePosteCode().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getGAREnsSpecialitePosteCode() {
        if (garEnsSpecialitePosteCode == null) {
            garEnsSpecialitePosteCode = new ArrayList<String>();
        }
        return this.garEnsSpecialitePosteCode;
    }

}
