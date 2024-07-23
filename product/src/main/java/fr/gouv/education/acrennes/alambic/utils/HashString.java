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
package fr.gouv.education.acrennes.alambic.utils;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HashString {

    // private static final Log log = LogFactory.getLog(HashString.class);

    private static final String HASH_PATTERN = "plaintext=(.+)\\s*,\\s*algorithm=(.+)";

    public static String base64Sha(final String params) throws AlambicException {
        byte[] b;

        Pattern pattern = Pattern.compile(HASH_PATTERN);
        Matcher matcher = pattern.matcher(params);
        if (matcher.matches()) {
            String plaintext = matcher.group(1).trim();
            String algorithm = matcher.group(2).trim();
            try {
                MessageDigest md = MessageDigest.getInstance(algorithm);
                b = md.digest(plaintext.getBytes());
            } catch (NoSuchAlgorithmException e) {
                throw new AlambicException(e.getMessage());
            }
        } else {
            throw new AlambicException("The parameter doesn't fit the hash function parameters pattern '" + HASH_PATTERN + "'");
        }

        return new String(Base64.encodeBase64(b));
    }

    public static String base64(final String s) throws UnsupportedEncodingException {
        return new String(Base64.encodeBase64(s.getBytes()));
    }

    public byte[] encodePassword(final String password) throws UnsupportedEncodingException {
        String newQuotedPassword = "\"" + password + "\"";
        return newQuotedPassword.getBytes(StandardCharsets.UTF_16LE);
    }

}