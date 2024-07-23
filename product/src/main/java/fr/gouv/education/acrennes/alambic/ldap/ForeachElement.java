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

import org.jdom2.Content;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForeachElement {

    private final Map<String, List<Map<String, List<String>>>> stateBaseList;
    private final Element foreachStateBaseElement;

    public ForeachElement(Element foreachStateBaseElement, Map<String, List<Map<String, List<String>>>> stateBaseList) {
        this.stateBaseList = stateBaseList;
        this.foreachStateBaseElement = foreachStateBaseElement;
    }

    public List<String> getValues() {
        List<String> res = new ArrayList<String>();
        String statebaseName = foreachStateBaseElement.getAttributeValue("name");
        List<Map<String, List<String>>> liste = stateBaseList.get(statebaseName);
        for (Map<String, List<String>> map : liste) {
            List<Content> maListe = foreachStateBaseElement.getContent();
            StringBuilder s = new StringBuilder();
            for (Object objet : maListe) {
                if (objet instanceof org.jdom2.Element) {
                    String attr = ((Element) objet).getAttributeValue("name");
                    s.append(map.get(attr).get(0));
                }
                if (objet instanceof org.jdom2.Text) {
                    s.append(((org.jdom2.Text) objet).getText());
                }
            }
            res.add(s.toString());
        }
        return res;

    }

}
