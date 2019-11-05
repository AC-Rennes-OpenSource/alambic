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

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

public class CipherKeyFactory {

	public static Key getKey(final String algorithm, final CipherKeyStore keystore, final String alias, final String keyPwd, final String type)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		Key key = null;

		if ("RSA".equals(algorithm) || "DSA".equals(algorithm)) {
			if ("private".equalsIgnoreCase(type)) {
				key = keystore.getPrivateKey(alias, keyPwd);
			} else {
				key = keystore.getPublicKey(alias, keyPwd);
			}
		} else {
			key = keystore.getSecretKey(alias, keyPwd);
		}

		return key;
	}

	public static Key getKey(final String algorithm, final String keyStr, final String type) throws NoSuchAlgorithmException, InvalidKeySpecException {
		Key key = null;

		if ("RSA".equals(algorithm) || "DSA".equals(algorithm)) {
			if ("private".equalsIgnoreCase(type)) {
				key = getPrivateKey(algorithm, keyStr);
			} else {
				key = getPublicKey(algorithm, keyStr);
			}
		} else {
			key = getSecretKey(algorithm, keyStr);
		}

		return key;
	}

	private static Key getSecretKey(final String algorithm, final String key) {
		byte[] encodedKey = Base64.decodeBase64(key);
		SecretKey originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, algorithm);
		return originalKey;
	}

	private static Key getPublicKey(final String algorithm, final String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] encodedKey = Base64.decodeBase64(key);
		X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(encodedKey);
		KeyFactory kf = KeyFactory.getInstance(algorithm);
		PublicKey publicKey = kf.generatePublic(X509publicKey);
		return publicKey;
	}

	private static Key getPrivateKey(final String algorithm, final String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] encodedKey = Base64.decodeBase64(key);
		PKCS8EncodedKeySpec PKCS8privateKey = new PKCS8EncodedKeySpec(encodedKey);
		KeyFactory kf = KeyFactory.getInstance(algorithm);
		PrivateKey privateKey = kf.generatePrivate(PKCS8privateKey);
		return privateKey;
	}

}
