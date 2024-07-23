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
package fr.gouv.education.acrennes.alambic.freemarker;

import freemarker.template.*;
import org.json.JSONArray;

public class JSONArrayAdapter extends WrappingTemplateModel implements TemplateSequenceModel, AdapterTemplateModel {

    private final JSONArray jsonObj;

    public JSONArrayAdapter(JSONArray jsonObj, ObjectWrapper ow) {
        super(ow);
        this.jsonObj = jsonObj;
    }

    @Override
    public Object getAdaptedObject(Class<?> hint) {
        return this.jsonObj;
    }

    @Override
    public TemplateModel get(int index) throws TemplateModelException {
        return wrap(this.jsonObj.get(index));
    }

    @Override
    public int size() throws TemplateModelException {
        return this.jsonObj.length();
    }

}
