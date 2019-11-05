/*******************************************************************************
 * Copyright (C) 2019 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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
package fr.gouv.education.acrennes.alambic.freemarker;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.client.model.PropertyList;
import org.nuxeo.ecm.automation.client.model.PropertyMap;

import fr.gouv.education.acrennes.alambic.Constants;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

public class AlambicObjectWrapper extends DefaultObjectWrapper {

	public AlambicObjectWrapper() {
		super(Constants.FREEMARKER_VERSION);
	}

	@Override
	protected TemplateModel handleUnknownType(final Object obj) throws TemplateModelException {

		if (obj instanceof PropertyList) {
			return new PropertyListAdapter((PropertyList) obj, this);
		} else if (obj instanceof PropertyMap) {
			return new PropertyMapAdapter((PropertyMap) obj, this);
		} else if (obj instanceof JSONObject) {
			return new JSONObjectAdapter((JSONObject) obj, this);
		} else if (obj instanceof JSONArray) {
			return new JSONArrayAdapter((JSONArray) obj, this);
		}
		
		return super.handleUnknownType(obj);
	}

}
