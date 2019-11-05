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
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.crypto.SecretKey;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import fr.gouv.education.acrennes.alambic.security.CipherHelper.CIPHER_MODE;

public class CipherHelperTest {

	private String AESkeyString;
	private String RSAPrivatekeyString;
	private String RSAPublickeyString;

	@Before
	public void setUp() throws UnrecoverableKeyException, KeyStoreException, CertificateException, IOException {
		try {
			/**
			 * The AES secret key is built via the command-line:
			 * keytool -genseckey -alias aliasaes128 -keyalg AES -keysize 128 -keypass testpass -keystore alambic.keystore -storetype JCEKS -storepass testpass
			 * The command line to check the key generate & import in keystore:
			 * keytool -list -keystore alambic.keystore -storetype JCEKS
			 */

			// Get the AES secret key from the keystore
			File file = new File("./src/test/resources/data/security/alambic.keystore");
			FileInputStream is = new FileInputStream(file);
			KeyStore keystore = KeyStore.getInstance("JCEKS");

			/* getting the secret key */
			String password = "testpass";
			String alias = "aliasaes128";
			keystore.load(is, password.toCharArray());
			SecretKey secretKey = (SecretKey) keystore.getKey(alias, password.toCharArray());
			AESkeyString = Base64.encodeBase64String(secretKey.getEncoded());

			/**
			 * The RSA key pair (public & private) and certificate are built via the command-line:
			 * keytool -genkeypair -alias aliasrsa1024 -keyalg RSA -keysize 1024 -keypass testpass -keystore alambic.keystore -storetype JCEKS -storepass testpass
			 */

			// Get the RSA key pair from the keystore
			file = new File("./src/test/resources/data/security/alambic.keystore");
			is = new FileInputStream(file);
			keystore = KeyStore.getInstance("JCEKS"); // KeyStore.getDefaultType());

			/* Information for certificate to be generated */
			password = "testpass";
			alias = "aliasrsa1024";

			/* getting the private key */
			keystore.load(is, password.toCharArray());
			PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());

			/* Get certificate of public key */
			java.security.cert.Certificate cert = keystore.getCertificate(alias);
			PublicKey publicKey = cert.getPublicKey();

			RSAPrivatekeyString = Base64.encodeBase64String(privateKey.getEncoded());
			RSAPublickeyString = Base64.encodeBase64String(publicKey.getEncoded());
		} catch (NoSuchAlgorithmException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test1() {
		String PLAIN_STRING = "Voici une chaîne de caractère qui sera encodée";

		try {
			// Encrypt
			CipherHelper cipher = new CipherHelper("AES", AESkeyString, null);
			byte[] encoded = cipher.execute(CIPHER_MODE.ENCRYPT_MODE, PLAIN_STRING);

			// Decrypt
			cipher = new CipherHelper("AES", AESkeyString, null);
			Assert.assertEquals(PLAIN_STRING, new String(cipher.execute(CIPHER_MODE.DECRYPT_MODE, encoded)));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test2() {
		String PLAIN_STRING = "Voici une chaîne de caractère qui sera encodée avec RSA";

		try {
			// Encrypt
			CipherHelper cipher = new CipherHelper("RSA", RSAPublickeyString, "Public");
			byte[] encoded = cipher.execute(CIPHER_MODE.ENCRYPT_MODE, PLAIN_STRING);

			// Decrypt
			cipher = new CipherHelper("RSA", RSAPrivatekeyString, "Private");
			Assert.assertEquals(PLAIN_STRING, new String(cipher.execute(CIPHER_MODE.DECRYPT_MODE, encoded)));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

}
