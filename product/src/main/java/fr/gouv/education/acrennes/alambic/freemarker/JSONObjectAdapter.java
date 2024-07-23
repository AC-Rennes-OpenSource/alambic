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

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.template.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JSONObjectAdapter extends WrappingTemplateModel implements TemplateHashModelEx, AdapterTemplateModel {

    private final JSONObject jsonObj;

    public JSONObjectAdapter(JSONObject jsonObj, ObjectWrapper ow) {
        super(ow);
        this.jsonObj = jsonObj;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdaptedObject(Class hint) {
        return this.jsonObj;
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        return (this.jsonObj.has(key)) ? wrap(this.jsonObj.get(key)) : null;
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return this.jsonObj.isEmpty();
    }

    @Override
    public int size() throws TemplateModelException {
        return this.jsonObj.length();
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        Set<String> keys = this.jsonObj.keySet();
        return new CollectionModel(keys, (BeansWrapper) getObjectWrapper());
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        List<String> values = new ArrayList<>();
        Set<String> keys = this.jsonObj.keySet();
        for (String key : keys) {
            values.add(this.jsonObj.get(key).toString());
        }
        return new CollectionModel(values, (BeansWrapper) getObjectWrapper());
    }

}
