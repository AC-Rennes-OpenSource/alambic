//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2021.02.23 à 04:46:47 PM CET 
//


package fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Classe Java pour GAR-ENT-Enseignant complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="GAR-ENT-Enseignant">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GAREnseignant" type="{http://data.education.fr/ns/gar/1d}GAREnseignant" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="GARPersonMEFSTAT4" type="{http://data.education.fr/ns/gar/1d}GARPersonMEFSTAT4" maxOccurs="unbounded" minOccurs="0"/>
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
@XmlType(name = "GAR-ENT-Enseignant", propOrder = {
        "garEnseignant",
        "garPersonMEFSTAT4"
})
public class GARENTEnseignant {

    @XmlElement(name = "GAREnseignant")
    protected List<GAREnseignant> garEnseignant;
    @XmlElement(name = "GARPersonMEFSTAT4")
    protected List<GARPersonMEFSTAT4> garPersonMEFSTAT4;
    @XmlAttribute(name = "Version", required = true)
    protected String version;

    /**
     * Gets the value of the garEnseignant property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garEnseignant property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGAREnseignant().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GAREnseignant }
     *
     *
     */
    public List<GAREnseignant> getGAREnseignant() {
        if (garEnseignant == null) {
            garEnseignant = new ArrayList<>();
        }
        return this.garEnseignant;
    }

    /**
     * Gets the value of the garPersonMEFSTAT4 property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the garPersonMEFSTAT4 property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGARPersonMEFSTAT4().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link GARPersonMEFSTAT4 }
     *
     *
     */
    public List<GARPersonMEFSTAT4> getGARPersonMEFSTAT4() {
        if (garPersonMEFSTAT4 == null) {
            garPersonMEFSTAT4 = new ArrayList<>();
        }
        return this.garPersonMEFSTAT4;
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
