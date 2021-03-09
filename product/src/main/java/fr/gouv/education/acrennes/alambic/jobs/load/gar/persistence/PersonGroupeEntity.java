/*******************************************************************************
 * Copyright (C) 2019-2021 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class PersonGroupeEntity implements Serializable {

    public enum PERSON_TYPE {
        STUDENT,
        TEACHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private PERSON_TYPE type;

    @Column(name = "uai")
    private String uai;

    @Column(name = "personIdentifiant")
    private String personIdentifiant;

    @Column(name = "groupeCode")
    private String groupeCode;


    public PersonGroupeEntity() {

    }

    public PersonGroupeEntity(final PERSON_TYPE type, final String uai, final String personIdentifiant, final String groupeCode) {
        setType(type);
        setUai(uai);
        setPersonIdentifiant(personIdentifiant);
        setGroupeCode(groupeCode);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PERSON_TYPE getType() {
        return type;
    }

    public void setType(PERSON_TYPE type) {
        this.type = type;
    }

    public String getUai() {
        return uai;
    }

    public void setUai(String uai) {
        this.uai = uai;
    }

    public String getPersonIdentifiant() {
        return personIdentifiant;
    }

    public void setPersonIdentifiant(String personIdentifiant) {
        this.personIdentifiant = personIdentifiant;
    }

    public String getGroupeCode() {
        return groupeCode;
    }

    public void setGroupeCode(String groupeCode) {
        this.groupeCode = groupeCode;
    }
}
