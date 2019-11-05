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
package fr.gouv.education.acrennes.alambic.random.persistence;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@Embeddable
public class RandomUserEntityPK implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum USER_GENDER {
		FEMALE,
		MALE
	}

	@Column(name = "id")
	private long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "gender")
	private USER_GENDER gender;

	public RandomUserEntityPK() {
	}

	public RandomUserEntityPK(final USER_GENDER gender, final long id) {
		this.gender = gender;
		this.id = id;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public USER_GENDER getGender() {
		return gender;
	}

	public void setGender(final USER_GENDER gender) {
		this.gender = gender;
	}

	@Override
	public int hashCode() {
		return "RandomUserEntityPK::".concat(toString()).hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		boolean isEqual = false;

		if (obj == this) {
			isEqual = true;
		} else if (null != obj) {
			if (obj instanceof RandomUserEntityPK) {
				RandomUserEntityPK other = (RandomUserEntityPK) obj;
				if (id == other.getId() && gender == other.getGender()) {
					isEqual = true;
				}
			}
		}

		return isEqual;
	}

	@Override
	public String toString() {
		return "{\"gender\":\"" + gender + "\", \"id\":" + id + "}";
	}

}
