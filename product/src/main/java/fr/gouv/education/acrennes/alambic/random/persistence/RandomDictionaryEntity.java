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
package fr.gouv.education.acrennes.alambic.random.persistence;

import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

@Entity
@Indexes({
        @Index(name = "randomdictionaryentity_pk_idx", unique = true, columnNames = { "id", "elementname" }),
        @Index(name = "randomdictionaryentity_elementname_idx", columnNames = { "elementname" }),
        @Index(name = "randomdictionaryentity_id_idx", columnNames = { "id" })
})
public class RandomDictionaryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private RandomDictionaryEntityPK primaryKey;

    @Column(name = "elementvalue")
    private String elementvalue;

    public RandomDictionaryEntity() {
    }

    public RandomDictionaryEntity(final RandomDictionaryEntityPK pk, final String value) {
        this.primaryKey = pk;
        this.elementvalue = value;
    }

    public RandomDictionaryEntityPK getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(final RandomDictionaryEntityPK primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getElementvalue() {
        return elementvalue;
    }

    public void setElementvalue(String value) {
        this.elementvalue = value;
    }

    @Override
    public String toString() {
        return "{\"PK\":" + ((null != this.primaryKey) ? this.primaryKey.toString() : "{}") + ",\"value\":\"" + this.elementvalue + "\"}";
    }

}