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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

@Entity
@Indexes({
		@Index(name = "randomuserfemaleentity_id_idx", columnNames = { "id" }),
		@Index(name = "randomuserfemaleentity_hash_idx", columnNames = { "hash" }),
		@Index(name = "randomuserfemaleentity_available_idx", columnNames = { "is_available" })
})
public class RandomUserFemaleEntity implements RandomEntity, RandomUserEntity {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(RandomUserFemaleEntity.class);

	@Id
//	@GeneratedValue(strategy = GenerationType.SEQUENCE) - defined by the provisioning script
	@Column(name = "id")
	private Long id;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "json_definition")
	private String json_definition;

	@Column(name = "hash")
	private String hash = "";

	@Column(name = "is_available")
	private Boolean is_available = true;
	
	public RandomUserFemaleEntity() {
	}

	public RandomUserFemaleEntity(final Long id, final String json) {
		this.id = id;
		this.json_definition = json;
		setHash(getHash());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getJson_definition() {
		return json_definition;
	}

	public void setJson_definition(final String json_definition) {
		this.json_definition = json_definition;
	}
	
	@Override
	public Boolean getIs_available() {
		return is_available;
	}

	@Override
	public void setIs_available(Boolean is_available) {
		this.is_available = is_available;
	}

	@Override
	public String toString() {
		return "{\"json_definition\":\"" + this.json_definition + "\"}";
	}

	@Override
	public String getJson() {
		return json_definition;
	}

	@Override
	public String getHash() {
		if (StringUtils.isBlank(this.hash)) {
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(toString().getBytes());
				byte byteData[] = md.digest();
				this.hash = Base64.encodeBase64String(byteData);
			} catch (NoSuchAlgorithmException e) {
				log.error("Failed to produce the entity hash string, error:" + e.getMessage());
			}
		}

		return this.hash;
	}

	public void setHash(final String hash) {
		this.hash = hash;
	}

}