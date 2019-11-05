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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

@Entity
@Indexes({
		@Index(name = "randomuserentity_pk_idx", unique = true, columnNames = { "id", "gender" }),
		@Index(name = "randomuserentity_gender_idx", columnNames = { "gender" }),
		@Index(name = "randomuserentity_id_idx", columnNames = { "id" })
})
public class RandomUserEntity implements Serializable, RandomEntity {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(RandomUserEntity.class);

	@EmbeddedId
	private RandomUserEntityPK primaryKey;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "json_definition")
	private String json_definition;

	@Index(name = "randomuserentity_hash_idx")
	@Column(name = "hash")
	private String hash;

	public RandomUserEntity() {
	}

	public RandomUserEntity(final RandomUserEntityPK pk, final String json) {
		primaryKey = pk;
		json_definition = json;
		setHash(getHash());
	}

	public RandomUserEntityPK getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(final RandomUserEntityPK primaryKey) {
		this.primaryKey = primaryKey;
	}

	public String getJson_definition() {
		return json_definition;
	}

	public void setJson_definition(final String json_definition) {
		this.json_definition = json_definition;
	}

	@Override
	public String toString() {
		return "{\"PK\":" + ((null != primaryKey) ? primaryKey.toString() : "{}") + ",\"json_definition\":\"" + json_definition + "\"}";
	}

	@Override
	public String getJson() {
		return json_definition;
	}

	@Override
	public String getHash() {
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

	public void setHash(final String hash) {
		this.hash = hash;
	}

}
