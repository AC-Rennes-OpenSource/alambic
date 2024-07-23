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
package fr.gouv.education.acrennes.alambic.jobs.load.gar.persistence;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class StaffEntityPK implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "uai")
    private String uai;

    @Column(name = "uuid")
    private String uuid;

    public StaffEntityPK() {
    }

    public StaffEntityPK(final String uai, final String uuid) {
        this.uai = uai;
        this.uuid = uuid;
    }

    public String getUai() {
        return uai;
    }

    public void setUai(final String uai) {
        this.uai = uai;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(final Object obj) {
        boolean isEqual = false;

        if (obj == this) {
            isEqual = true;
        } else if (null != obj) {
            if (obj instanceof final StaffEntityPK other) {
                if (uai == other.getUai() && uuid == other.getUuid()) {
                    isEqual = true;
                }
            }
        }

        return isEqual;
    }

    @Override
    public String toString() {
        return "{\"uuid\":\"" + uuid + "\",\"uai\":\"" + uai + "\"}";
    }

    @Override
    public int hashCode() {
        return "StaffEntityPK::".concat(toString()).hashCode();
    }

}
