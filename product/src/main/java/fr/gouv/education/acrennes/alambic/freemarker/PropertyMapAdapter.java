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
import org.nuxeo.ecm.automation.client.model.PropertyMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PropertyMapAdapter extends WrappingTemplateModel implements
        TemplateHashModelEx, AdapterTemplateModel {

    private final PropertyMap propertyMap;

    public PropertyMapAdapter(final PropertyMap propertyMap, final ObjectWrapper ow) {
        super(ow);
        this.propertyMap = propertyMap;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdaptedObject(final Class hint) {
        return propertyMap;
    }

    @Override
    public TemplateModel get(final String key) throws TemplateModelException {
        return wrap(propertyMap.get(key));
    }

    @Override
    public boolean isEmpty() throws TemplateModelException {
        return propertyMap.isEmpty();
    }

    @Override
    public int size() throws TemplateModelException {
        return propertyMap.size();
    }

    @Override
    public TemplateCollectionModel keys() throws TemplateModelException {
        Set<String> keys = propertyMap.getKeys();
        return new CollectionModel(keys, (BeansWrapper) getObjectWrapper());
    }

    @Override
    public TemplateCollectionModel values() throws TemplateModelException {
        List<String> values = new ArrayList<>();
        Set<String> keys = propertyMap.getKeys();
        for (String key : keys) {
            values.add(propertyMap.get(key).toString());
        }
        return new CollectionModel(values, (BeansWrapper) getObjectWrapper());
    }
}
