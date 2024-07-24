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
package fr.gouv.education.acrennes.alambic.ldap;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.IToStateBase;
import fr.gouv.education.acrennes.alambic.jobs.extract.clients.SqlToStateBase;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import fr.gouv.education.acrennes.alambic.utils.LdapUtils;
import fr.gouv.education.acrennes.alambic.utils.UniqueName;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Entry {

    private static final Log LOG = LogFactory.getLog(Entry.class);
    private static final String RELATION = "relation";
    private static final String ATTRIBUTES = "attributes";
    private static final String MODIFY_MODE = "modifyMode";
    private static final String CASE_SENSITIVE = "caseSensitive";
    private static final String EXPLICIT_MEMBER = "ExplicitMember";
    private final static int UPDATE = 0;
    private final static int IGNORE = 3;
    private final static String EXPLICIT = "explicit";
    private static final String DN = "dn";
    private static final String MEMBER = "member";
    private final Variables parentVariables;
    private final Map<String, List<Map<String, List<String>>>> stateBaseList = new HashMap<>();
    private final SearchControls contraintes;
    private final Element entry;
    private final Datasources datasources;
    private final Map<String, List<String>> cr;
    private Variables variables = new Variables();
    private DirContext rootCtx;
    private DirContext workCtx;
    private String dn;
    private String rdn;
    private String context;
    private String filtre;
    private boolean createOnly = false;
    private boolean updateOnly = false;
    private boolean deleteOnly = false;
    private boolean caseSensitiveEntry = false;
    public Entry(final Element entry, final DirContext ctx, final SearchControls contraintes,
                 final Variables variables, final Map<String, List<String>> cr,
                 final Datasources datasources) throws NamingException, AlambicException {
        this.entry = entry;
        rootCtx = ctx;
        this.contraintes = contraintes;
        parentVariables = variables;
        this.datasources = datasources;
        this.cr = cr;
        init();
    }

    private void init() throws NamingException, AlambicException {
        // Chargement des variables depuis le pivot
        loadVariablesList();
        // Chargement des bases états déclarées
        loadStateBaseList();

        createOnly = "C".equals(entry.getAttributeValue("operation"));
        updateOnly = "U".equals(entry.getAttributeValue("operation"));
        deleteOnly = "D".equals(entry.getAttributeValue("operation"));

        // Récupération du test d'existence de l'entrée
        filtre = variables.resolvString(entry.getAttributeValue("verifyIfExist"));
        entry.getChild(ATTRIBUTES);
        // Remplacement des variables
        dn = Functions.getInstance().executeAllFunctions(variables.resolvString(entry.getChildText("dn"))).toLowerCase();
        // Déduction du RDN de l'entrée estimé
        rdn = dn.substring(0, dn.indexOf(","));
        // Déduction du contexte de l'entrée à traiter
        context = dn.substring(dn.indexOf(",") + 1);
        final String nameInNamespace = rootCtx.getNameInNamespace().toLowerCase();
        String subContext;
        if (context.indexOf(nameInNamespace) > 0) {
            subContext = context.substring(0, context.indexOf(nameInNamespace) - 1);
        } else {
            subContext = context;
        }

        // Sensibilité globale à la casse
        String caseSensitive = variables.resolvString(entry.getAttributeValue(CASE_SENSITIVE));
        if (StringUtils.isNotBlank(caseSensitive) && caseSensitive.equalsIgnoreCase("true")) {
            caseSensitiveEntry = true;
        }

        // ouverture du contexte de travail
        workCtx = (DirContext) rootCtx.lookup(subContext);
    }

    private void loadVariablesList() throws AlambicException {
        variables.clearTable();
        if (parentVariables != null) {
            variables.loadFromMap(parentVariables.getHashMap());
        }
        if (entry.getChild("variables") != null) {
            variables.loadFromXmlNode(entry.getChild("variables").getChildren());
        }
        // execution des éventuelles fonctions chargées dans la table de
        variables.executeFunctions();
    }

    /*
     * Chargement des déclarations de bases état
     */
    private void loadStateBaseList() throws AlambicException {
        // Execution des statements déclarés
        if (entry.getChild("statebases") != null) {
            final List<Element> children = entry.getChild("statebases").getChildren("statebase");
            for (final Element stateBase : children) {
                // Récupération du datasource définit ds le pivot
                final String stateBaseName = stateBase.getAttributeValue("name");
                final String dataSourceName = stateBase.getAttributeValue("source");
                String stateBaseQuery = stateBase.getValue();
                // Faire une interface sur les extracteurs
                final IToStateBase extSql = (SqlToStateBase) datasources.getDatasource(dataSourceName);
                stateBaseQuery = Functions.getInstance().executeAllFunctions(variables.resolvString(stateBaseQuery));
                extSql.executeQuery(stateBaseQuery);
                stateBaseList.put(stateBaseName, extSql.getStateBase());
            }
        }
    }

    private DirContext getLdapEntry() throws NamingException {
        // Récupération du rdn de l'ou "personnes"
        DirContext ldapEntry = null;
        // Vérification de l'existence de l'entrée à créer
        final NamingEnumeration<SearchResult> searchRes = workCtx.search("", filtre, contraintes);
        try {
            if ((searchRes != null) && searchRes.hasMore()) {
                // Dans ce cas, l'entrée existe
                // Récupération de l'entrée existante
                final SearchResult res = searchRes.next();
                dn = res.getNameInNamespace();
                final String rdnAttr = dn.substring(0, dn.indexOf("="));
                // Vérification du contenu du RDN, si multivalué => suppression de l'entrée
                if (res.getAttributes().get(rdnAttr).size() > 1) {
                    LOG.info("[" + rdn + "] valeur de rnd multiple pour l'entree '" + dn + "'");
                    workCtx.unbind(res.getName());
                    ldapEntry = getLdapEntry();

                } else {
                    updateRdn(res.getName());
                    ldapEntry = (DirContext) workCtx.lookup(rdn);
                }

                if (searchRes.hasMore()) {
                    LOG.info("[" + rdn + "] plusieurs entrées ont la même valeur de rdn pour le filte '" + filtre + "'");
                }
            }
        } finally {
            searchRes.close();
        }
        return ldapEntry;
    }

    private void updateRdn(final String rdn) {
        this.rdn = rdn;
        // MAJ de la variable LOGIN dans la table de variables
        variables.put("LOGIN", rdn.substring(rdn.indexOf("=") + 1));
    }

    /*
     * Méthode permettant de déduire un RDN unique depuis l'annuaire retourne le
     * rdn calculé
     */
    private String getNewRdnInContext() throws NamingException {
        final NamingEnumeration<SearchResult> searchExistingRdn = workCtx.search("", rdn + "*", contraintes);
        try {
            if (searchExistingRdn.hasMore()) {
                final UniqueName uniqueName = new UniqueName(rdn);
                uniqueName.loadListRdnFromNamingEnumeration(searchExistingRdn);
                updateRdn(uniqueName.getUniqueName());
            }
        } finally {
            searchExistingRdn.close();
        }
        return rdn;
    }

    private void removeEntry(final DirContext ldapEntry) throws NamingException {
        workCtx.unbind(rdn);
        LOG.info("Effacement de l'entree [" + rdn + "]");
    }

    /**
     * Parcours les attributs listés dans la description des opérations à effectuer et prépare les modifications à
     * effectuer avant de les appliquer.
     *
     * @param ldapEntry
     * @throws NamingException
     * @throws AlambicException
     */
    private void updateEntry(final DirContext ldapEntry) throws NamingException, AlambicException {
        // Mise à jour de la variable %LOGIN%
        dn = ldapEntry.getNameInNamespace();
        // Récupération de la liste des attributs
        final Attributes ldapAttrs = ldapEntry.getAttributes("");
        // Initialisation des attributs en création, modification et suppression (des valeurs uniquement)
        final Attributes attrsToMod = new BasicAttributes();
        // Initialisation des attributs en suppression
        final Attributes attrsToDelete = new BasicAttributes();
        // Analyse de la liste des attributs du pivot
        for (final Element pAttr : entry.getChild(ATTRIBUTES).getChildren()) {
            final String modifyMode = pAttr.getAttributeValue(MODIFY_MODE);

            // Contruction de l'attribut attendu à partir du pivot
            Attribute pivotAttr = getAttributeFromPivot(pAttr);
            // Récupération du nom de l'attribut à modifier
            final String attributeName = pAttr.getAttributeValue("name");
            // Récupération de l'attribut existant
            Attribute existingAttr = ldapAttrs.get(attributeName);

            // Teste si l'attribut est défini comme modifiable et/ou supprimable dans le pivot
            final boolean canDeleteAttribute = ((modifyMode != null) && modifyMode.equalsIgnoreCase(ModifyMode.DELETE));
            final boolean canModifyAttribute = (StringUtils.isNotBlank(modifyMode) &&
                                                (
                                                        modifyMode.equalsIgnoreCase(ModifyMode.REPLACE)
                                                        || modifyMode.equalsIgnoreCase(ModifyMode.APPEND)
                                                        || (modifyMode.equalsIgnoreCase(ModifyMode.IGNORE) && ((null == existingAttr) || (0 == existingAttr.size())))
                                                        || modifyMode.equalsIgnoreCase(ModifyMode.REMOVE)
                                                )
            );
            if (canModifyAttribute) {
                if (existingAttr == null) {
                    existingAttr = new BasicAttribute(attributeName);
                }

                Attribute workAttr;
                if (attrsToDelete.get(existingAttr.getID()) != null) {
                    workAttr = new BasicAttribute(attributeName);
                } else {
                    if (attrsToMod.get(existingAttr.getID()) != null) {
                        workAttr = attrsToMod.get(existingAttr.getID());
                    } else {
                        workAttr = (BasicAttribute) existingAttr.clone();
                    }
                }

                if (ModifyMode.APPEND.equalsIgnoreCase(modifyMode) || ModifyMode.REPLACE.equalsIgnoreCase(modifyMode)) {
                    String explicitMerge = null;

                    // Traitement spécifique de l'attribut member et ENTManager
                    if (attributeName.equalsIgnoreCase(MEMBER)) {
                        explicitMerge = EXPLICIT_MEMBER;
                    } else {
                        // Traitement de la clause attribut explicit
                        explicitMerge = pAttr.getAttributeValue(EXPLICIT);
                    }
                    if (explicitMerge != null) {
                        final Attribute explicitAttr = ldapAttrs.get(explicitMerge);
                        if (explicitAttr != null) {
                            explicitAttribute(attrsToMod, pivotAttr, explicitAttr);
                        }
                    }
                }

                if (modifyMode.equalsIgnoreCase(ModifyMode.APPEND)) {
                    workAttr = appendAttributesValues(pivotAttr, workAttr.getAll());
                }
                if (modifyMode.equalsIgnoreCase(ModifyMode.REMOVE)) {
                    workAttr = removeAttributesValues(pivotAttr, workAttr.getAll());
                }
                if (modifyMode.equalsIgnoreCase(ModifyMode.REPLACE)) {
                    workAttr = pivotAttr;
                }
                if (modifyMode.equalsIgnoreCase(ModifyMode.IGNORE) && 0 == existingAttr.size()) {
                    workAttr = pivotAttr;
                }

                // L'attribut existe dans l'entrée courante
                int majAction = IGNORE;
                try {
                    //
                    final String relation = pAttr.getAttributeValue(RELATION);
                    if (relation == null) {
                        // Si les attributs sont égaux => pas de modification . Retourne la valeur IGNORE
                        boolean caseSensitive = caseSensitiveEntry;
                        if (StringUtils.isNotBlank(pAttr.getAttributeValue(CASE_SENSITIVE))) {
                            caseSensitive = pAttr.getAttributeValue(CASE_SENSITIVE).equalsIgnoreCase("true");
                        }
                        majAction = LdapUtils.compareAttributes(workAttr, existingAttr, caseSensitive);
                    } else {
                        // Cas ou l'attribut contient une relation avec une autre entrée (member - memberOf)
                        final List<String> deltaToAdd = LdapUtils.positiveDelta(pivotAttr, existingAttr);
                        final List<String> deltaToDel = LdapUtils.positiveDelta(existingAttr, pivotAttr);
                        if (!deltaToAdd.isEmpty() || !deltaToDel.isEmpty()) {
                            majAction = UPDATE;
                            if (!deltaToAdd.isEmpty()) {
                                // MAJ des attributs des entrées en relation
                                final LdapRelation relationsToAdd = new LdapRelation(rootCtx, deltaToAdd, LdapRelation.ADD, relation, dn);
                                relationsToAdd.execute();
                            }
                            if (!deltaToDel.isEmpty()) {
                                // MAJ des attributs des entrées en relation
                                final LdapRelation relationsToDel = new LdapRelation(rootCtx, deltaToDel, LdapRelation.DELETE, relation, dn);
                                relationsToDel.execute();
                            }
                        }
                    }
                } catch (final NamingException e) {
                    LOG.error("   -> Comparaison d'attributs pour [" + rdn + "]", e);
                } catch (final UnsupportedEncodingException e) {
                    LOG.error("   -> Comparaison d'attributs pour [" + rdn + "] -> conversion caractéres", e);
                }

                if (majAction == IGNORE) {
                    attrsToMod.remove(workAttr.getID());
                    attrsToDelete.remove(workAttr.getID());
                } else {
                    if (workAttr.size() == 0) {
                        // Si l'attribut était marqué à modifier, puis qu'on le supprime, il devient à supprimer
                        if (attrsToMod.get(workAttr.getID()) != null) {
                            attrsToMod.remove(workAttr.getID());
                        }
                        attrsToDelete.put(workAttr);
                    } else {
                        // Si l'attribut était marqué à supprimer, puis qu'on lui rajoute des valeurs, il devient à modifier
                        if (attrsToDelete.get(workAttr.getID()) != null) {
                            attrsToDelete.remove(workAttr.getID());
                        }
                        attrsToMod.put(workAttr);
                    }
                }
            } else if (canDeleteAttribute) {
                // Verify the attribut exists indeed
                if (existingAttr != null) {
                    attrsToDelete.put(pivotAttr);
                } else {
                    LOG.info("Attribute deletion is ignored since the attribute doesn't exist ([" + rdn + "] | attribute=" + pivotAttr.getID() + ")");
                }
            }
        }

        // modification des attributs
        if (attrsToMod.size() > 0) {
            LOG.info("Modification entree [" + rdn + "] | " + attrsToMod);
            ldapEntry.modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, attrsToMod);
        }

        // removal of attributes
        if (attrsToDelete.size() > 0) {
            LOG.info("Modification entree [" + rdn + "] | suppression des attributs " + attrsToDelete);
            ldapEntry.modifyAttributes("", DirContext.REMOVE_ATTRIBUTE, attrsToDelete);
        }

    }

    private void explicitAttribute(final Attributes attrsToMod, final Attribute attendedAttr, final Attribute explicitAttr)
            throws NamingException {
        final NamingEnumeration<?> values = explicitAttr.getAll();
        final Attribute explicitAttrCleaned = new BasicAttribute(explicitAttr.getID());

        while (values.hasMore()) {
            final Object value = values.next();
            try {
                if (!LdapUtils.attributeContainsValue(attendedAttr, value)) {
                    attendedAttr.add(value);
                    explicitAttrCleaned.add(value);
                }
            } catch (final UnsupportedEncodingException e) {
                LOG.error("UnsupportedEncodingException", e);
            }
        }
        if (explicitAttrCleaned.size() != explicitAttr.size()) {
            LOG.info("Nettoyage de l'attribut explicite [" + explicitAttr.getID() + "]");
            attrsToMod.put(explicitAttrCleaned);
        }
    }

    private Attribute appendAttributesValues(
            final Attribute attributeChanges,
            final NamingEnumeration<?> oldAttributeValues
    ) throws NamingException {
        final Attribute newState = new BasicAttribute(attributeChanges.getID());
        while (oldAttributeValues.hasMore()) {
            final Object oldValue = oldAttributeValues.next();
            newState.add(oldValue);
        }

        final NamingEnumeration<?> newValues = attributeChanges.getAll();
        while (newValues.hasMore()) {
            final Object newValue = newValues.next();
            try {
                if (!LdapUtils.attributeContainsValue(newState, newValue)) {
                    newState.add(newValue);
                }
            } catch (final UnsupportedEncodingException e) {
                LOG.error("UnsupportedEncodingException", e);
            }
        }

        return newState;
    }

    private Attribute removeAttributesValues(
            final Attribute attributeChanges,
            final NamingEnumeration<?> oldAttributeValues
    ) throws NamingException {
        final Attribute newState = new BasicAttribute(attributeChanges.getID());
        while (oldAttributeValues.hasMore()) {
            final Object value = oldAttributeValues.next();
            try {
                if (!LdapUtils.attributeContainsValueAllowRegExp(attributeChanges, value)) {
                    newState.add(value);
                }
            } catch (final UnsupportedEncodingException e) {
                LOG.error("UnsupportedEncodingException", e);
            }
        }
        return newState;
    }

    public void update() throws NamingException, AlambicException {
        // Récupération de l'entrée si elle existe
        final DirContext ldapEntry = getLdapEntry();
        if (ldapEntry != null) {
            try {
                // Elle existe => MAJ de l'entrée
                if (createOnly) {
                    LOG.warn("Create only entry requested; ignore the found LDAP entry: '" + ldapEntry.getNameInNamespace() + "'");
                } else {
                    dn = ldapEntry.getNameInNamespace();
                    final String rdnAttr = dn.substring(0, dn.indexOf("="));
                    // Suppression des entrées avec deux RDN
                    if (ldapEntry.getAttributes("").get(rdnAttr).size() > 1) {
                        workCtx.unbind(rdn);
                        update();
                    } else {
                        if (deleteOnly) {
                            removeEntry(ldapEntry);
                        } else {
                            updateEntry(ldapEntry);
                        }
                    }
                }
            } finally {
                ldapEntry.close();
            }
        } else {
            // Elle n'existe pas => Création de l'entrée
            if (updateOnly | deleteOnly) {
                LOG.warn("Either 'update only' or 'delete only' entry requested but the LDAP entry doesn't exist (filter is '" + filtre + "'); " +
                         "ignore the request");
            } else {
                LOG.info("Création de l'entrée dans le LDAP");
                createEntry();
            }
        }
    }

    public void close() throws NamingException {
        // Fermeture du contexte de travail
        workCtx.close();
    }

    private void createEntry() throws NamingException, AlambicException {
        // Constitution d'un RDN unique pour l'entrée à créer
        getNewRdnInContext();
        // Préparation de l'entrée é créer
        final Attributes ldapAttrs = getAttributesFromPivot();
        LOG.info("Creation entree [" + rdn + "]");
        workCtx.createSubcontext(rdn, ldapAttrs);

        // Traitement des attributs en relation4
        for (final Element pAttr : entry.getChild(ATTRIBUTES).getChildren()) {
            final String relation = pAttr.getAttributeValue(RELATION);
            final boolean hasRelation = (relation != null) && !relation.isEmpty();
            // Teste si l'attribut est défini comme modifiable dans le pivot
            if (hasRelation) {
                // Cas ou l'attribut contient une relation avec une autre entrée (member - memberOf)
                List<String> deltaToAdd = null;
                // Contruction de l'attribut attendu à partir du pivot
                final Attribute attendedAttr = getAttributeFromPivot(pAttr);
                // Récupération de l'attribut LDAP existant
                final String attributeName = pAttr.getAttributeValue("name");
                final Attribute existingAttr = new BasicAttribute(attributeName);
                try {
                    // Calcul de la liste des valeurs à traiter pour mettre à jour les DN correspondants
                    deltaToAdd = LdapUtils.positiveDelta(attendedAttr, existingAttr);
                } catch (final UnsupportedEncodingException e) {
                    LOG.error("   -> Comparaison d'attributs pour [" + rdn + "] -> conversion caractéres");
                    LOG.error("       - Erreur JAVA  = " + e.getMessage(), e);
                }
                if (!deltaToAdd.isEmpty()) {
                    // MAJ des attributs des entrées en relation
                    final LdapRelation relationsToAdd = new LdapRelation(rootCtx, deltaToAdd, LdapRelation.ADD, relation, dn);
                    relationsToAdd.execute();
                }
            }
        }
    }

    /*
     * Charge la liste des attributs d'une entrée du pivot
     */
    private Attributes getAttributesFromPivot() throws AlambicException {
        final Attributes attrs = new BasicAttributes();
        // Analyse de la liste des attributs
        for (final Element attrPivot : entry.getChild(ATTRIBUTES).getChildren()) {
            final Attribute attrLdap = getAttributeFromPivot(attrPivot);
            // Ajout de l'attribut dans l'entrée é créer
            if (attrLdap.size() > 0) {
                attrs.put(attrLdap);
            }
        }
        return attrs;
    }

    /*
     * Méthode préparant le contenu d'un attribut en fonction des données du pivot
     */
    private Attribute getAttributeFromPivot(final Element attrPivot) throws AlambicException {
        // Création d'une entrée LDAP temporaire

        Attribute attrLdap = new BasicAttribute(attrPivot.getAttributeValue("name"));
        // Récupréaration de la liste de valeurs contenues dans le pivot
        final List<Element> values = new ArrayList<>(attrPivot.getChildren("value"));
        final List<Element> foreaches = attrPivot.getChildren("foreach");
        for (final Element foreach : foreaches) {
            final String source = foreach.getAttributeValue("source");
            if (source.contains("model")) {
                final String value = foreach.getChildText("value");
                final String item = source.substring(source.indexOf('.') + 1);
                final List<String> l = cr.get(item);
                if (l != null) {
                    for (final String s : l) {
                        values.add(new Element("value").addContent(value.replace("%.%", s)));
                    }
                }
            }
        }

        attrLdap = processListValues(attrLdap, values);

        return attrLdap;
    }

    /*
     * Méthode évaluant le contenu de l'attribut value et traitant les cas
     * -- foreachStateBase
     * -- ldap://
     * --
     */
    private Attribute processListValues(final Attribute attrLdap, final List<Element> values) throws AlambicException {
        if ((values == null) || values.isEmpty()) {
            LOG.debug("Les valeurs de l'attribut [" + attrLdap.getID() + "] sont vides.");
            return attrLdap;
        }
        for (final Element valueElement : values) {
            final Condition condition = new Condition(valueElement.getAttributeValue("condition"), stateBaseList, variables);
            if (!condition.eval()) {
                continue;
            }
            // Récupération du template de valeur courant
            String valueString = valueElement.getValue();
            valueString = variables.resolvString(valueString);
            final Element foreachStateBaseElement = valueElement.getChild("foreachStateBase");
            String ldapQuery = valueElement.getAttributeValue("ldap");
            if (ldapQuery != null) {
                ldapQuery = variables.resolvString(ldapQuery);
                final String attribute = valueElement.getAttributeValue("attribute") == null ? DN : valueElement.getAttributeValue("attribute");
                /*
                 * Ex : <value id="lq" ldap="%LDAP_ENT_URI%/%LDAP_ENT_ouPersonnesDn%?uid?one?(&amp;(ENTPersonFonctions=%RNE%$DOC))">
                 * %lq.dn%</value>
                 */
                executeLdapQuery(attrLdap, ldapQuery, attribute);

            } else if (foreachStateBaseElement != null) {
                /*
                 * Exemple de balise foreach
                 * <foreachStateBase name="postes">
                 * <valueOfAttribute name="rne"/>$<valueOfAttribute name="codenat"/>
                 * </foreachStateBase>
                 */
                final ForeachElement foreachElement = new ForeachElement(foreachStateBaseElement, stateBaseList);
                for (String s : foreachElement.getValues()) {
                    s = variables.resolvString(s);
                    if (s.startsWith("ldap://")) {
                        executeLdapQuery(attrLdap, s, DN);
                    } else {
                        attrLdap.add(variables.resolvString(s));
                    }
                }
            } else if (valueString.startsWith("ldap://")) {
                /*
                 * Itération sur l'execusion d'une requète LDAP
                 */
                final String stringValue = Functions.getInstance().executeAllFunctions(valueString);
                executeLdapQuery(attrLdap, stringValue, DN);
            } else {
                /*
                 * Affectation normale de la valeur de la valeur courante de l'attribut
                 */
                final String stringValue = Functions.getInstance().executeAllFunctions(valueString);
                attrLdap.add(stringValue);
            }
        }
        return attrLdap;
    }

    /*
     * Execution des requètes LDAP embarquées dans les éléments <value></value>
     */
    private void executeLdapQuery(final Attribute attrLdap, final String query, final String attributeName) {
        try {
            final NamingEnumeration<SearchResult> res = rootCtx.search(query, "", null);
            try {
                while (res.hasMore()) {
                    if (DN.equals(attributeName)) {
                        // Le DN est demandé
                        attrLdap.add(res.next().getNameInNamespace());
                    } else {
                        // Un attribut est demandé...
                        final NamingEnumeration<?> values = res.next().getAttributes().get(attributeName).getAll();
                        while (values.hasMore()) {
                            final Object value = values.next();
                            if (!attrLdap.contains(value)) {
                                attrLdap.add(value);
                            }
                        }
                    }
                }
            } finally {
                res.close();
            }
        } catch (final NamingException e) {
            LOG.error("   Execusion requete [" + query + "]", e);
        }
    }

    public Variables getVariables() {
        return variables;
    }

    public void setVariables(final Variables variables) {
        this.variables = variables;
    }

    public DirContext getCtx() {
        return rootCtx;
    }

    public void setCtx(final DirContext ctx) {
        rootCtx = ctx;
    }

    private static final class ModifyMode {
        /**
         * Ignore les valeurs existantes et les remplace par les valeurs fournies.
         */
        private static final String REPLACE = "replace";
        /**
         * Crée l'attribut s'il n'est pas initialisé, mais ne remplace pas l'existant dans le cas contraire.
         */
        private static final String IGNORE = "ignore";
        /**
         * Supprime l'attribut et les valeurs associées.
         */
        private static final String DELETE = "delete";
        /**
         * Ajoute des valeurs à un attribut, en conservant sans les modifier les valeurs existantes.
         */
        private static final String APPEND = "append";
        /**
         * Supprime des valeurs d'un attribut, en conservant sans les modifier les valeurs non listées.
         */
        private static final String REMOVE = "remove";
    }
}
