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
package fr.gouv.education.acrennes.alambic.nuxeo;

import org.jdom2.Element;

/*
 * Structure XML à décoder dans le
 * <acp block-inheritance="true" override="true" ttc="true">
 * <acl name="local">
 * <ace granted="true" permission="Everything" principal="Administrator"/>
 * <ace granted="true" permission="Everything" principal="administrators"/>
 * <ace granted="true" permission="Read" principal="${rne}_Tous"/>
 * </acl>
 * </acp>
 */

public class ACP {
    private static final String LOCAL = "local";
    private static final String TRUE = "true";
    private static final String ACLNAME = "name";
    private static final String ACL = "acl";
    private static final String OVERWRITE = "overwrite";
    private static final String BLOCKINHERITANCE = "block-inheritance";
    private static final String TTC = "ttc"; // true par défault, indique s'il faut utiliser l'opération opentoutatice
    private final Element acpElement;

    public ACP(final Element acpElement) {
        this.acpElement = acpElement;

    }

    public String getName() {
        String name = LOCAL;
        if (acpElement.getChild(ACL) != null) {
            if (acpElement.getChild(ACL).getAttributeValue(ACLNAME) != null) {
                name = acpElement.getAttributeValue(ACLNAME);
            }
        }
        return name;
    }

    public boolean getOverwrite() {
        boolean overwrite = true;
        if (acpElement.getAttributeValue(OVERWRITE) != null) {
            overwrite = acpElement.getAttributeValue(OVERWRITE).equalsIgnoreCase(TRUE);
        }
        return overwrite;
    }

    public boolean getTtc() {
        boolean ttc = true;
        if (acpElement.getAttributeValue(TTC) != null) {
            ttc = acpElement.getAttributeValue(TTC).equalsIgnoreCase(TRUE);
        }
        return ttc;
    }

    public boolean getBlock() {
        boolean block = true;
        if (acpElement.getAttributeValue(BLOCKINHERITANCE) != null) {
            block = acpElement.getAttributeValue(BLOCKINHERITANCE).equalsIgnoreCase(TRUE);
        }
        return block;
    }

    public ACL getACL() {
        final Element aclElement = acpElement.getChild(ACL);
        return (null != aclElement) ? new ACL(aclElement) : null;
    }

}
