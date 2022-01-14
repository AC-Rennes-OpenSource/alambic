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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

/**
 * This entity may be used for entities built programmatically instead of storage in DB.
 * 
 * @author mberhaut1
 */

@Entity
@Indexes({
		@Index(name = "randomlambdaentity_pk_idx", unique = true, columnNames = { "id" }),
		@Index(name = "randomlambdaentity_hash_idx", columnNames = { "hash" })
})
public class RandomLambdaEntity implements RandomEntity {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(RandomLambdaEntity.class);

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id")
	private Long id;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "json_definition")
	private String json_definition;

	@Column(name = "hash")
	private String hash;

	public RandomLambdaEntity() {
	}

	public RandomLambdaEntity(final String json) {
		this.json_definition = json;
		setHash(computeHash());
	}

	public Long getId() {
		return id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getJson_definition() {
		return json_definition;
	}

	public void setJson_definition(final String json_definition) {
		this.json_definition = json_definition;
	}

	@Override
	public String getHash() {
		return this.hash;
	}
	
	@Override
	public void setHash(final String hash) {
		this.hash = hash;
	}

	@Override
	public String toString() {
		return "{\"json_definition\":" + getJson() + "}";
	}

	@Override
	public String getJson() {
		return this.json_definition;
	}

	private String computeHash() {
		String hash = null;

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(toString().getBytes());
			byte byteData[] = md.digest();
			hash = Base64.encodeBase64String(byteData);
		} catch (NoSuchAlgorithmException e) {
			log.error("Failed to produce the entity hash string, error:" + e.getMessage());
		}

		return hash;
	}

}