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

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

/*
 * Structure XML à décoder dans le
 * <acl name="local">
 * <ace granted="true" permission="Everything" principal="Administrator"/>
 * <ace granted="true" permission="Everything" principal="administrators"/>
 * <ace granted="true" permission="Read" principal="${rne}_Tous"/>
 * </acl>
 */

public class ACL {

	private final Element aclElement;
	private static final String LOCAL = "local";
	private static final String NAME = "name";

	public ACL(final Element aclElement) {
		this.aclElement = aclElement;
	}

	public String getName() {
		String name = LOCAL;
		if (aclElement != null) {
			if (aclElement.getAttributeValue(NAME) != null) {
				name = aclElement.getAttributeValue(NAME);
			}
		}
		return name;
	}

	public List<ACE> getAceList() {
		List<ACE> lace = new ArrayList<ACE>();
		for (Element aceElement : (List<Element>) aclElement.getChildren()) {
			lace.add(new ACE(aceElement));
		}
		return lace;

	}
}
