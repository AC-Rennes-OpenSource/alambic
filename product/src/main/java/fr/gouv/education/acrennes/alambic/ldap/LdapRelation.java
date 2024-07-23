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

import fr.gouv.education.acrennes.alambic.Constants;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.utils.Functions;
import fr.gouv.education.acrennes.alambic.utils.LdapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class LdapRelation {
    private static final Log log = LogFactory.getLog(LdapRelation.class);

    public final static int DELETE = 2;
    public final static int ADD = 1;
    public final static int IGNORE = 0;

    private DirContext ctx = null;
    private List<String> listDn = new ArrayList<String>();
    private int updateMode = 0;
    private String attribute = "memberOf";
    private String value = "";

    public LdapRelation(final DirContext ctx, final List<String> listDn, final int updateMode,
                        final String attribute, final String value) throws AlambicException {
        super();
        if (Constants.MAX_LDAP_RELATION_SIZE >= listDn.size()) {
            this.ctx = ctx;
            this.listDn = listDn;
            this.updateMode = updateMode;
            this.attribute = attribute;
            this.value = value;
        } else {
            throw new AlambicException("Dépassement de capacité sur une mise en relation d'attribut '" + attribute + "' (taille limite " + Constants.MAX_LDAP_RELATION_SIZE + ")");
        }
    }

    public LdapRelation(final DirContext ctx) {
        this.ctx = ctx;
    }

    public void setDnList(final List<String> listDn) {
        this.listDn = listDn;
    }

    public void setAttribute(final String attribute) {
        this.attribute = attribute;
    }

    public void setCtx(final DirContext ctx) {
        this.ctx = ctx;
    }

    public void setUpdateMode(final int updateMode) {
        this.updateMode = updateMode;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    private Attribute deleteValue(final Attribute attr) throws NamingException, UnsupportedEncodingException {
        NamingEnumeration<?> values = attr.getAll();
        try {
            Attribute newAttr = new BasicAttribute(attr.getID());
            while (values.hasMore()) {
                String existingValue =
                        Functions.getInstance().valueToString(values.next());
                if (!existingValue.equalsIgnoreCase(value)) {
                    // Ajoute les attributs existants différents de la valeur à supprimer
                    newAttr.add(existingValue);
                }
            }
            return newAttr;
        } finally {
            values.close();
        }
    }

    private Attribute addValue(final Attribute attr) throws NamingException, UnsupportedEncodingException {

        if (!LdapUtils.attributeContainsValue(attr, value)) {
            attr.add(value);
        }

        return attr;
    }

    public boolean execute() {
        boolean test = false;
        SearchControls contraintes = new SearchControls();
        contraintes.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        contraintes.setReturningAttributes(new String[] { attribute });
        for (String dn : listDn) {
            try {
                String subContext = dn.substring(0, dn.indexOf(ctx.getNameInNamespace()) - 1);
                DirContext entry = (DirContext) ctx.lookup(subContext);
                Attribute existingAttr = entry.getAttributes("").get(attribute);
                if (existingAttr == null) {
                    existingAttr = new BasicAttribute(attribute);
                }
                Attribute newAttr = null;
                switch (updateMode) {
                    case ADD:
                        log.debug("LdapRelation ADD: " + dn + " -> " + attribute + " -> " + value);
                        newAttr = addValue(existingAttr);
                        break;
                    case DELETE:
                        log.debug("LdapRelation DEL: " + dn + " -> " + attribute + " -> " + value);
                        newAttr = deleteValue(existingAttr);
                        break;
                    default:
                        break;
                }
                Attributes attrsToMod = new BasicAttributes();
                attrsToMod.put(newAttr);
                entry.modifyAttributes("", DirContext.REPLACE_ATTRIBUTE, attrsToMod);
            } catch (NamingException e) {
                log.error("MAJ de la valeur (" + value + ") de l'attribut(" + attribute + ")de l'entrée(" + dn + ") : ");
                log.error("   -> JAVA ERROR = " + e.getMessage());
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                log.error("Effacement de la valeur (" + value + ") de l'attribut(" + attribute + ")de l'entrée(" + dn + ") : ");
                log.error("   -> JAVA ERROR = " + e.getMessage());
            }
        }
        return test;
    }

}
