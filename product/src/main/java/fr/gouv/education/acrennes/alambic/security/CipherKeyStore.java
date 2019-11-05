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
package fr.gouv.education.acrennes.alambic.security;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fr.gouv.education.acrennes.alambic.exception.AlambicException;

public class CipherKeyStore {

	protected static final Log log = LogFactory.getLog(CipherKeyStore.class);

	public static enum KEYSTORE_TYPE {
		DEFAULT("DEFAULT"),
		JCEKS("JCEKS");

		private final String value;

		private KEYSTORE_TYPE(final String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

	};

	private FileInputStream is;
	private String ksPwd;
	private KeyStore keystore;

	public CipherKeyStore(final String path, final KEYSTORE_TYPE type, final String ksPwd) throws AlambicException {
		try {
			is = new FileInputStream(new File(path));
			this.ksPwd = ksPwd;
			if (KEYSTORE_TYPE.JCEKS.equals(type)) {
				keystore = KeyStore.getInstance("JCEKS");
			} else {
				keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			}
			keystore.load(is, ksPwd.toCharArray());
		} catch (Exception e) {
			log.error("Failed to load the keystore '" + path + "', error: " + e.getMessage());
		}
	}

	public Key getSecretKey(final String alias, final String keyPwd) {
		Key key = null;
		try {
			key = keystore.getKey(alias, (StringUtils.isNotBlank(keyPwd)) ? keyPwd.toCharArray() : ksPwd.toCharArray());
			log.debug("Secret key is: " + Base64.encodeBase64String(key.getEncoded()));
		} catch (Exception e) {
			log.error("Failed to get the key (alias='" + alias + "') from the keystore, error: " + e.getMessage());
		}
		return key;
	}

	public Key getPublicKey(final String alias, final String keyPwd) {
		Key key = null;
		try {
			Certificate cert = keystore.getCertificate(alias);
			key = cert.getPublicKey();
			log.debug("Public key is: " + Base64.encodeBase64String(key.getEncoded()));
		} catch (Exception e) {
			log.error("Failed to get the key (alias='" + alias + "') from the keystore, error: " + e.getMessage());
		}
		return key;
	}

	public Key getPrivateKey(final String alias, final String keyPwd) {
		Key key = null;
		try {
			key = keystore.getKey(alias, (StringUtils.isNotBlank(keyPwd)) ? keyPwd.toCharArray() : ksPwd.toCharArray());
			log.debug("Private key is: " + Base64.encodeBase64String(key.getEncoded()));
		} catch (Exception e) {
			log.error("Failed to get the key (alias='" + alias + "') from the keystore, error: " + e.getMessage());
		}
		return key;
	}

}
