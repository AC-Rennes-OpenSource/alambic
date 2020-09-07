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
package fr.gouv.education.acrennes.alambic.monitoring;

public enum ActivityTrafficLight {

	GREEN(1),
	ORANGE(2),
	RED(3);
	
	private int value;
	
	ActivityTrafficLight(int value) {
		this.value = value;
	}

	public int getScalarValue() {
		return this.value;
	}

	public boolean isLowerThan(final ActivityTrafficLight activity) {
		return this.value < activity.getScalarValue();
	}

	public boolean isEqualOrLowerThan(final ActivityTrafficLight activity) {
		return this.value <= activity.getScalarValue();
	}

	public boolean isGreaterThan(final ActivityTrafficLight activity) {
		return this.value > activity.getScalarValue();
	}

	public boolean isEqualOrGreaterThan(final ActivityTrafficLight activity) {
		return this.value >= activity.getScalarValue();
	}
	
}
