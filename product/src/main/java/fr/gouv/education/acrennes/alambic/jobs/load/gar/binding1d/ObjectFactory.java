//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2021.02.23 à 04:46:47 PM CET 
//


package fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fr.gouv.education.acrennes.alambic.jobs.load.gar.binding1d package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GARENTEtab_QNAME = new QName("http://data.education.fr/ns/gar/1d", "GAR-ENT-Etab");
    private final static QName _GARENTEleve_QNAME = new QName("http://data.education.fr/ns/gar/1d", "GAR-ENT-Eleve");
    private final static QName _GARENTGroupe_QNAME = new QName("http://data.education.fr/ns/gar/1d", "GAR-ENT-Groupe");
    private final static QName _GARENTEnseignant_QNAME = new QName("http://data.education.fr/ns/gar/1d", "GAR-ENT-Enseignant");
    private final static QName _GARENTRespAff_QNAME = new QName("http://data.education.fr/ns/gar/1d", "GAR-ENT-RespAff");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fr.gouv.education.acrennes
     * .alambic.jobs.load.gar.binding1d
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GARENTRespAff }
     *
     */
    public GARENTRespAff createGARENTRespAff() {
        return new GARENTRespAff();
    }

    /**
     * Create an instance of {@link GARENTEnseignant }
     *
     */
    public GARENTEnseignant createGARENTEnseignant() {
        return new GARENTEnseignant();
    }

    /**
     * Create an instance of {@link GARENTEleve }
     *
     */
    public GARENTEleve createGARENTEleve() {
        return new GARENTEleve();
    }

    /**
     * Create an instance of {@link GARENTGroupe }
     *
     */
    public GARENTGroupe createGARENTGroupe() {
        return new GARENTGroupe();
    }

    /**
     * Create an instance of {@link GARENTEtab }
     *
     */
    public GARENTEtab createGARENTEtab() {
        return new GARENTEtab();
    }

    /**
     * Create an instance of {@link GAREnsSpecialitesPostes }
     *
     */
    public GAREnsSpecialitesPostes createGAREnsSpecialitesPostes() {
        return new GAREnsSpecialitesPostes();
    }

    /**
     * Create an instance of {@link GAREtab }
     *
     */
    public GAREtab createGAREtab() {
        return new GAREtab();
    }

    /**
     * Create an instance of {@link GARIdentite }
     *
     */
    public GARIdentite createGARIdentite() {
        return new GARIdentite();
    }

    /**
     * Create an instance of {@link GAREnseignant }
     *
     */
    public GAREnseignant createGAREnseignant() {
        return new GAREnseignant();
    }

    /**
     * Create an instance of {@link GARPersonProfils }
     *
     */
    public GARPersonProfils createGARPersonProfils() {
        return new GARPersonProfils();
    }

    /**
     * Create an instance of {@link GARGroupe }
     *
     */
    public GARGroupe createGARGroupe() {
        return new GARGroupe();
    }

    /**
     * Create an instance of {@link GARPersonGroupe }
     *
     */
    public GARPersonGroupe createGARPersonGroupe() {
        return new GARPersonGroupe();
    }

    /**
     * Create an instance of {@link GAREleve }
     *
     */
    public GAREleve createGAREleve() {
        return new GAREleve();
    }

    /**
     * Create an instance of {@link GARPersonMEFSTAT4 }
     *
     */
    public GARPersonMEFSTAT4 createGARPersonMEFSTAT4() {
        return new GARPersonMEFSTAT4();
    }

    /**
     * Create an instance of {@link GARRespAff }
     *
     */
    public GARRespAff createGARRespAff() {
        return new GARRespAff();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GARENTEtab }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://data.education.fr/ns/gar/1d", name = "GAR-ENT-Etab")
    public JAXBElement<GARENTEtab> createGARENTEtab(GARENTEtab value) {
        return new JAXBElement<>(_GARENTEtab_QNAME, GARENTEtab.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GARENTEleve }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://data.education.fr/ns/gar/1d", name = "GAR-ENT-Eleve")
    public JAXBElement<GARENTEleve> createGARENTEleve(GARENTEleve value) {
        return new JAXBElement<>(_GARENTEleve_QNAME, GARENTEleve.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GARENTGroupe }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://data.education.fr/ns/gar/1d", name = "GAR-ENT-Groupe")
    public JAXBElement<GARENTGroupe> createGARENTGroupe(GARENTGroupe value) {
        return new JAXBElement<>(_GARENTGroupe_QNAME, GARENTGroupe.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GARENTEnseignant }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://data.education.fr/ns/gar/1d", name = "GAR-ENT-Enseignant")
    public JAXBElement<GARENTEnseignant> createGARENTEnseignant(GARENTEnseignant value) {
        return new JAXBElement<>(_GARENTEnseignant_QNAME, GARENTEnseignant.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GARENTRespAff }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://data.education.fr/ns/gar/1d", name = "GAR-ENT-RespAff")
    public JAXBElement<GARENTRespAff> createGARENTRespAff(GARENTRespAff value) {
        return new JAXBElement<>(_GARENTRespAff_QNAME, GARENTRespAff.class, null, value);
    }

}
