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

import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Entity
@Indexes({
        @Index(name = "staffentity_pk_idx", unique = true, columnNames = { "uuid", "uai" }),
        @Index(name = "staffentity_uuid_idx", columnNames = { "uuid" }),
        @Index(name = "staffentity_uai_idx", columnNames = { "uai" }),
        @Index(name = "staffentity_type_idx", columnNames = { "type" })
})
public class StaffEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum STAFF_TYPE {
        TEACHER,
        STUDENT
    }

    @EmbeddedId
    private StaffEntityPK primaryKey;

    @OneToMany(targetEntity = EnseignementEntity.class, orphanRemoval = true, cascade = CascadeType.ALL /*, fetch = FetchType.LAZY */)
    @CascadeOnDelete
    private List<EnseignementEntity> enseignements;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private STAFF_TYPE type;

    public StaffEntity() {
    }

    public StaffEntity(final StaffEntityPK pk, final STAFF_TYPE type) {
        primaryKey = pk;
        this.type = type;
        enseignements = Collections.emptyList();
    }

    public StaffEntity(final StaffEntityPK pk, final List<EnseignementEntity> enseignements, final STAFF_TYPE type) {
        this(pk, type);
        this.enseignements = enseignements;
    }

    public StaffEntityPK getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(final StaffEntityPK primaryKey) {
        this.primaryKey = primaryKey;
    }

    public STAFF_TYPE getType() {
        return type;
    }

    public void setType(final STAFF_TYPE type) {
        this.type = type;
    }

    public List<EnseignementEntity> getEnseignements() {
        return enseignements;
    }

    public void setEnseignements(final List<EnseignementEntity> enseignements) {
        this.enseignements = enseignements;
    }

    @Override
    public boolean equals(final Object obj) {
        boolean isEqual = false;

        if (obj == this) {
            isEqual = true;
        } else if (null != obj) {
            if (obj instanceof final StaffEntity other) {
                if (other.getPrimaryKey().equals(getPrimaryKey()) && type == other.getType()) {
                    isEqual = true;
                }
            }
        }

        return isEqual;
    }

    @Override
    public String toString() {
        return "{\"primaryKey\":" + getPrimaryKey() + ",\"type\":\"" + getType() + "\",\"enseignements\":[" + Arrays.toString(getEnseignements().toArray()) + "]}";
    }

    @Override
    public int hashCode() {
        return "StaffEntity::".concat(toString()).hashCode();
    }

}
