//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2021.02.23 à 04:46:47 PM CET 
//


package fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d;

import javax.xml.bind.annotation.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Classe Java pour GAREnseignant complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="GAREnseignant">
 *   &lt;complexContent>
 *     &lt;extension base="{http://data.education.fr/ns/gar/1d}GARIdentite">
 *       &lt;sequence>
 *         &lt;element name="GARPersonEtab" type="{http://data.education.fr/ns/gar/1d}GARStructureUAIType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GARPersonDateNaissance" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *         &lt;element name="GAREnsSpecialitesPostes" type="{http://data.education.fr/ns/gar/1d}GAREnsSpecialitesPostes" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GARPersonMail" type="{http://data.education.fr/ns/gar/1d}GARPersonMailType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GAREnseignant", propOrder = {
        "garPersonEtab",
        "garPersonDateNaissance",
        "garEnsSpecialitesPostes",
        "garPersonMail"
})
public class GAREnseignant
        extends GARIdentite {

    @XmlElement(name = "GARPersonEtab")
    protected List<String> garPersonEtab;
    @XmlElement(name = "GARPersonDateNaissance")
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar garPersonDateNaissance;
    @XmlElement(name = "GAREnsSpecialitesPostes")
    protected List<GAREnsSpecialitesPostes> garEnsSpecialitesPostes;
    @XmlElement(name = "GARPersonMail")
    protected List<String> garPersonMail;

    /**
     * Gets the value of the garPersonEtab property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garPersonEtab property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARPersonEtab().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getGARPersonEtab() {
        if (garPersonEtab == null) {
            garPersonEtab = new ArrayList<>();
        }
        return this.garPersonEtab;
    }

    /**
     * Obtient la valeur de la propriété garPersonDateNaissance.
     *
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public XMLGregorianCalendar getGARPersonDateNaissance() {
        return garPersonDateNaissance;
    }

    /**
     * Définit la valeur de la propriété garPersonDateNaissance.
     *
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *
     */
    public void setGARPersonDateNaissance(XMLGregorianCalendar value) {
        this.garPersonDateNaissance = value;
    }

    /**
     * Gets the value of the garEnsSpecialitesPostes property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garEnsSpecialitesPostes property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGAREnsSpecialitesPostes().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GAREnsSpecialitesPostes }
     *
     *
     */
    public List<GAREnsSpecialitesPostes> getGAREnsSpecialitesPostes() {
        if (garEnsSpecialitesPostes == null) {
            garEnsSpecialitesPostes = new ArrayList<>();
        }
        return this.garEnsSpecialitesPostes;
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

}
