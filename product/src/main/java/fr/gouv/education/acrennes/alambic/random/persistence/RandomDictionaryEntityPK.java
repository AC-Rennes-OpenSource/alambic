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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class RandomDictionaryEntityPK implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum IDENTITY_ELEMENT {
		FIRSTNAME_FEMALE,
		FIRSTNAME_MALE,
		LASTNAME,
		ADDRESS_TYPE,
		ADDRESS_LABEL,
		ADDRESS_CITY
	}

	@Column(name = "id")
	private long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "elementname")
	private IDENTITY_ELEMENT elementname;

	public RandomDictionaryEntityPK() {
	}

	public RandomDictionaryEntityPK(final IDENTITY_ELEMENT elementname, final long id) {
		this.elementname = elementname;
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public IDENTITY_ELEMENT getElementname() {
		return this.elementname;
	}

	public void setElementname(final IDENTITY_ELEMENT elementname) {
		this.elementname = elementname;
	}

	@Override
	public int hashCode() {
		return "RandomDictionaryEntityPK::".concat(toString()).hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		boolean isEqual = false;

		if (obj == this) {
			isEqual = true;
		} else if (null != obj) {
			if (obj instanceof RandomDictionaryEntityPK) {
				RandomDictionaryEntityPK other = (RandomDictionaryEntityPK) obj;
				if (this.id == other.getId() && this.elementname == other.getElementname()) {
					isEqual = true;
				}
			}
		}

		return isEqual;
	}

	@Override
	public String toString() {
		return "{\"element\":\"" + this.elementname + "\", \"id\":" + id + "}";
	}

}