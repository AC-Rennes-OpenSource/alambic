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

import javax.persistence.*;
import java.io.Serializable;

/**
 * This entity may be used to store the blur identifier used to anonymise an entry.
 *
 * @author mberhaut1
 */

@Entity
@Indexes({
        @Index(name = "randombluridentity_pk_idx", unique = true, columnNames = { "id" }),
        @Index(name = "randombluridentity_signature_idx", columnNames = { "signature" })
})
public class RandomBlurIDEntity implements Serializable {

    private static final long serialVersionUID = 1L;

//	private static final Log log = LogFactory.getLog(RandomBlurIDEntity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "signature")
    private String signature;

    @Column(name = "blurid")
    private String blurid;

    public RandomBlurIDEntity() {
    }

    public RandomBlurIDEntity(final String signature, final String blur_id) {
        this.signature = signature;
        this.blurid = blur_id;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getBlurid() {
        return blurid;
    }

    public void setBlurid(String blurid) {
        this.blurid = blurid;
    }

    @Override
    public String toString() {
        return "{\"id\":" + this.id + ", \"signature\":\"" + this.signature + "\", \"blurid\":\"" + this.blurid + "\"}";
    }

}