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
package fr.gouv.education.acrennes.alambic.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.jobs.CallableContext;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Element;

public class CipherHelper extends AbstractDestination {
	protected static final Log log = LogFactory.getLog(CipherHelper.class);

	private Cipher cipher;
	private Key key;
	private File inputFile;
	private File outputFile;
	private CIPHER_MODE mode;
	private String algorithm;

	public static enum CIPHER_MODE {
		ENCRYPT_MODE("ENCRYPT_MODE"),
		DECRYPT_MODE("DECRYPT_MODE");

		private final String value;

		private CIPHER_MODE(final String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

		public int getInt() throws AlambicException {
			int cipherMode;

			if (CIPHER_MODE.ENCRYPT_MODE.toString().equals(value)) {
				cipherMode = Cipher.ENCRYPT_MODE;
			} else if (CIPHER_MODE.DECRYPT_MODE.toString().equals(value)) {
				cipherMode = Cipher.DECRYPT_MODE;
			} else {
				throw new AlambicException("Unknown mode '" + value + "', should be either 'ENCRYPT_MODE' or 'DECRYPT_MODE'");
			}

			return cipherMode;
		}

	};

	public CipherHelper(final CallableContext context, final Element job, final ActivityMBean jobActivity) throws AlambicException {
		super(context, job, jobActivity);

		CipherKeyStore keystore = null;
		String keyStr = null;
		String keyType = null;
		String keyAlias = null;
		String keyPwd = null;

		try {
			algorithm = job.getChildText("algorithm");
			if (StringUtils.isBlank(algorithm)) {
				throw new AlambicException("the ciphering algorithm (AES,DES...) MUST be specified");
			} else {
				algorithm = context.resolveString(algorithm);
			}

			String modeStg = job.getChildText("mode");
			if (StringUtils.isBlank(modeStg)) {
				throw new AlambicException("the ciphering mode (ENCRYPT_MODE/DECRYPT_MODE) MUST be specified");
			} else {
				mode = CIPHER_MODE.valueOf(context.resolveString(modeStg));
			}

			Element keyNode = job.getChild("key");
			if (null != keyNode) {
				keyStr = keyNode.getText();
				keyType = keyNode.getAttributeValue("type");
				if (StringUtils.isBlank(keyStr)) {
					throw new AlambicException("the ciphering key MUST be specified");
				} else {
					keyStr = context.resolveString(keyStr);
				}
			} else {
				Element keystoreNode = job.getChild("keystore");
				if (null != keystoreNode) {
					String keystorePath = keystoreNode.getChildText("path");
					String keystorePwd = keystoreNode.getChildText("password");
					String keystoreType = keystoreNode.getAttributeValue("type");

					keyNode = keystoreNode.getChild("key");
					keyType = keyNode.getAttributeValue("type");
					keyAlias = keyNode.getChildText("alias");
					keyPwd = keyNode.getChildText("password");

					if (StringUtils.isBlank(keystorePath) ||
							StringUtils.isBlank(keystorePwd) ||
							StringUtils.isBlank(keystoreType) ||
							StringUtils.isBlank(keyType) ||
							StringUtils.isBlank(keyAlias) ||
							StringUtils.isBlank(keyPwd)) {
						throw new AlambicException("the ciphering keystore definition is missing data (either path, passsword, type, key alias, key type, key password)");
					} else {
						keystorePath = context.resolvePath(keystorePath);
						keystorePwd = context.resolveString(keystorePwd);
						keystoreType = context.resolveString(keystoreType);
						keyType = context.resolveString(keyType);
						keyAlias = context.resolveString(keyAlias);
						keyPwd = context.resolveString(keyPwd);
						keystore = new CipherKeyStore(keystorePath, CipherKeyStore.KEYSTORE_TYPE.valueOf(keystoreType), keystorePwd);
					}
				} else {
					throw new AlambicException("Either a ciphering key or keystore MUST be specified");
				}
			}

			String input = job.getChildText("input");
			if (StringUtils.isBlank(input)) {
				throw new AlambicException("the input file MUST be specified");
			} else {
				input = context.resolvePath(input);
				inputFile = new File(input);
			}

			String output = job.getChildText("output");
			if (StringUtils.isBlank(output)) {
				throw new AlambicException("the output file MUST be specified");
			} else {
				output = context.resolvePath(output);
				outputFile = new File(output);
			}

			if (null != keystore) {
				key = CipherKeyFactory.getKey(algorithm, keystore, keyAlias, keyPwd, keyType);
			} else {
				key = CipherKeyFactory.getKey(algorithm, keyStr, keyType);
			}
			cipher = Cipher.getInstance(algorithm);
		} catch (Exception e) {
			throw new AlambicException("Failed to instantiate Ciphering operation, error: " + e.getMessage());
		}
	}

	public CipherHelper(final String algorithm, final CipherKeyStore keystore, final String alias, final String keyPwd, final String keyType)
			throws AlambicException {
		try {
			key = CipherKeyFactory.getKey(algorithm, keystore, alias, keyPwd, keyType);
			cipher = Cipher.getInstance(algorithm);
			// initialize(algorithm, mode, key);
		} catch (Exception e) {
			throw new AlambicException("Failed to instantiate Ciphering operation, error: " + e.getMessage());
		}
	}

	public CipherHelper(final String algorithm, final String keyStr, final String keyType) throws AlambicException {
		try {
			key = CipherKeyFactory.getKey(algorithm, keyStr, keyType);
			cipher = Cipher.getInstance(algorithm);
		} catch (Exception e) {
			throw new AlambicException("Failed to instantiate Ciphering operation, error: " + e.getMessage());
		}
	}

	public byte[] execute(final CIPHER_MODE mode, final byte[] input) throws AlambicException {
		byte[] result = null;

		try {
			cipher.init(mode.getInt(), key);
			result = cipher.doFinal(input);
		} catch (Exception e) {
			log.error("Failed to execute cipher operation, error: " + e.getMessage());
			throw new AlambicException(e.getMessage());
		}

		return result;
	}

	public byte[] execute(final CIPHER_MODE mode, final String input) throws AlambicException {
		return execute(mode, input.getBytes());
	}

	public void execute(final CIPHER_MODE mode, final File input, final File output) {
		InputStream in = null;
		OutputStream out = null;

		try {
			input.createNewFile();
			output.createNewFile();

			cipher.init(mode.getInt(), key);
			if (CIPHER_MODE.ENCRYPT_MODE.equals(mode)) {
				in = new FileInputStream(input);
				out = new CipherOutputStream(new FileOutputStream(output), cipher);
			} else {
				in = new CipherInputStream(new FileInputStream(input), cipher);
				out = new FileOutputStream(output);
			}

			byte[] data = new byte[1024];
			int numread = in.read(data);
			while (0 < numread) {
				out.write(data, 0, numread);
				numread = in.read(data);
			}
			out.flush();
		} catch (Exception e) {
			log.error("Failed to process the ciphering operation, error: " + e.getMessage());
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				log.error("Failed to close input/output streams, error: " + e.getMessage());
			}
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				log.error("Failed to close input/output streams, error: " + e.getMessage());
			}
		}
	}

	@Override
	public void execute() throws AlambicException {
		execute(mode, inputFile, outputFile);
	}

}
