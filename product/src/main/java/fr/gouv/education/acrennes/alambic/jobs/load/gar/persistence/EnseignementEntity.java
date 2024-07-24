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

import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Indexes({
        @Index(name = "enseignemententity_pk_idx", unique = true, columnNames = { "id" })
})
public class EnseignementEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;
    @Column(name = "sourceSI")
    private String sourceSI;
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ENSEIGNEMENT_TYPE type;
    @Column(name = "code")
    private String code;
    @Column(name = "divOrGrpCode")
    private String divOrGrpCode;

    public EnseignementEntity() {
    }

    public EnseignementEntity(final String sourceSI, final String code, final String divOrGrpCode, final ENSEIGNEMENT_TYPE type) {
        setSourceSI(sourceSI);
        setCode(code);
        setDivOrGrpCode(divOrGrpCode);
        setType(type);
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getSourceSI() {
        return sourceSI;
    }

    public void setSourceSI(String sourceSI) {
        this.sourceSI = sourceSI;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getDivOrGrpCode() {
        return divOrGrpCode;
    }

    public void setDivOrGrpCode(final String libelle) {
        this.divOrGrpCode = libelle;
    }

    public ENSEIGNEMENT_TYPE getType() {
        return type;
    }

    public void setType(final ENSEIGNEMENT_TYPE type) {
        this.type = type;
    }

    @Override
    public boolean equals(final Object obj) {
        boolean isEqual = false;

        if (obj == this) {
            isEqual = true;
        } else if (null != obj) {
            if (obj instanceof final EnseignementEntity other) {
                isEqual = type.equals(other.getType())
                          && ((null != code) ? code.equals(other.getCode()) : null == other.getCode())
                          && divOrGrpCode.equals(other.getDivOrGrpCode());
            }
        }

        return isEqual;
    }

    @Override
    public String toString() {
        return "{\"type\":\"" + type + "\",\"code\":\"" + code + "\",\"divOrGrpCode\":\"" + divOrGrpCode + "\"}";
    }

    @Override
    public int hashCode() {
        return "EnseignementEntity::".concat(toString()).hashCode();
    }

    public enum ENSEIGNEMENT_TYPE {
        DISCIPLINE,
        GROUPE_MATIERE,
        CLASSE_MATIERE,
        MEF
    }

}
