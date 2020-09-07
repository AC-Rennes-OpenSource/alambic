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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import org.junit.Assert;
import org.junit.Test;

public class FunctionsTest {

	@Test
	public void test1() throws AlambicException {
		final String gps = Functions.getInstance().executeAllFunctions("(lambert93.to.gps)327592.7,6829576.2(/lambert93.to.gps)");
		Assert.assertEquals("48.45944326636733,-2.040519398538259", gps);
	}

	@Test
	public void test2() throws AlambicException {
		final String MDP = "Le mot de passe à chiffrer avec RSA";
		final String encryptedText = Functions.getInstance().executeAllFunctions("(CIPHER)mode=ENCRYPT_MODE,algorithm=RSA,path=./src/test/resources/data/security/alambic.keystore,ksPwd=testpass,ksType=JCEKS,alias=aliasrsa1024,keyPwd=testpass,keyType=public,plaintext=" + MDP + "(/CIPHER)");
		Assert.assertEquals(MDP, Functions.getInstance().executeAllFunctions("(CIPHER)mode=DECRYPT_MODE,algorithm=RSA,path=./src/test/resources/data/security/alambic.keystore,ksPwd=testpass,ksType=JCEKS,alias=aliasrsa1024,keyPwd=testpass,keyType=private,plaintext=" + encryptedText + "(/CIPHER)"));
	}

	@Test
	public void test3() throws AlambicException {
		final String MDP = "Le mot de passe à chiffrer avec AES";
		final String encryptedText = Functions.getInstance().executeAllFunctions("(CIPHER)mode=ENCRYPT_MODE,algorithm=AES,path=./src/test/resources/data/security/alambic.keystore,ksPwd=testpass,ksType=JCEKS,alias=aliasaes128,keyPwd=testpass,keyType=secret,plaintext=" + MDP + "(/CIPHER)");
		Assert.assertEquals(MDP, Functions.getInstance().executeAllFunctions("(CIPHER)mode=DECRYPT_MODE,algorithm=AES,path=./src/test/resources/data/security/alambic.keystore,ksPwd=testpass,ksType=JCEKS,alias=aliasaes128,keyPwd=testpass,keyType=secret,plaintext=" + encryptedText + "(/CIPHER)"));
	}

	@Test
	public void test4() throws AlambicException {
		final String params = "plaintext=(CIPHER)mode=DECRYPT_MODE,algorithm=AES,path=./src/test/resources/data/security/alambic.keystore,ksPwd=testpass,ksType=JCEKS,alias=aliasaes128,keyPwd=testpass,keyType=secret,plaintext=zDU5J8Tw7Pd7ftMthM4a4r5pzaRWTHc/20ekKlNCsVPG4zOpCxpPb9ZSfp4jTgwf(/CIPHER),algorithm= SHA-256";
		final String hashedMDP = Functions.getInstance().executeAllFunctions("{SHA-256}(B64SHAx)" + params + "(/B64SHAx)");
		Assert.assertEquals("{SHA-256}/s176IZXSDsl++PXjYDuQpTkxx07Ftc4sFF2FOXGO5w=", hashedMDP);
	}

	@Test
	public void test5() throws AlambicException {
		final String MDP = "Le mot de passe à chiffrer avec RSA en 2048 bits";
		final String encryptedText = Functions.getInstance().executeAllFunctions("(CIPHER)mode=ENCRYPT_MODE,algorithm=RSA,path=./src/test/resources/data/security/alambic.keystore,ksPwd=testpass,ksType=JCEKS,alias=aliasrsa2048,keyPwd=testpass,keyType=public,plaintext=" + MDP + "(/CIPHER)");
		Assert.assertEquals(MDP, Functions.getInstance().executeAllFunctions("(CIPHER)mode=DECRYPT_MODE,algorithm=RSA,path=./src/test/resources/data/security/alambic.keystore,ksPwd=testpass,ksType=JCEKS,alias=aliasrsa2048,keyPwd=testpass,keyType=private,plaintext=" + encryptedText + "(/CIPHER)"));
	}

	@Test
	public void test6() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("%var% - (STRINGFORMAT mem='var')pattern=Hello %s, agent %03d!;values=you,7;types=String,Integer(/STRINGFORMAT)");
		Assert.assertEquals("Hello you, agent 007! - Hello you, agent 007!", actual);
	}

	@Test
	public void test7() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(STRINGFORMAT)pattern=%010d;values=(INCREMENT)1(/INCREMENT);types=Integer(/STRINGFORMAT)");
		Assert.assertEquals("0000000001", actual);
	}

	@Test
	public void test8() throws AlambicException {
		final LocalDate expectedDate = LocalDate.now().plusDays(5);
		final String actual = Functions.getInstance().executeAllFunctions("(COMPUTEDATE){\"format\":\"dd/MM/yyyy\",\"value\":\"(NOW)dd/MM/yyyy(/NOW)\",\"operator\":\"PLUS\",\"unit\":\"DAY\",\"operand\":\"5\"}(/COMPUTEDATE)");
		Assert.assertEquals(expectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), actual);
	}

	@Test
	public void test9() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(COMPUTEDATE){\"format\":\"dd/MM/yyyy\",\"value\":\"08/03/2018\",\"operator\":\"MINUS\",\"unit\":\"DAY\",\"operand\":\"5\"}(/COMPUTEDATE)");
		Assert.assertEquals("03/03/2018", actual);
	}

	@Test
	public void test10() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(COMPUTEDATE){\"format\":\"dd/MM/yyyy\",\"value\":\"08/03/2018\",\"operator\":\"MINUS\",\"unit\":\"MONTH\",\"operand\":\"5\"}(/COMPUTEDATE)");
		Assert.assertEquals("08/10/2017", actual);
	}

	@Test
	public void test11() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(COMPUTEDATE){\"format\":\"dd/MM/yyyy\",\"value\":\"08/03/2018\",\"operator\":\"PLUS\",\"unit\":\"YEAR\",\"operand\":\"4\"}(/COMPUTEDATE)");
		Assert.assertEquals("08/03/2022", actual);
	}

	@Test
	public void test12() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(COMPUTEDATE){\"format\":\"dd/MM/yyyy\",\"value\":\"08/03/2018\",\"operator\":\"PLUS\",\"operand\":\"15\"}(/COMPUTEDATE)");
		Assert.assertEquals("23/03/2018", actual);
	}
	
	@Test
	public void test16() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(SALT)16(/SALT)");
		Assert.assertNotNull(actual);
		Assert.assertEquals(16, actual.length());
	}

	@Test
	public void test17() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(SALT) (/SALT)");
		Assert.assertNotNull(actual);
		Assert.assertEquals(16, actual.length());
	}

	@Test
	public void test18() throws AlambicException {
		final String actual = Functions.getInstance().executeAllFunctions("(SALT)20(/SALT)");
		Assert.assertNotNull(actual);
		Assert.assertEquals(20, actual.length());

		final String actual2 = Functions.getInstance().executeAllFunctions("(SALT)20(/SALT)");
		Assert.assertNotNull(actual2);
		Assert.assertEquals(20, actual2.length());
		Assert.assertNotSame(actual, actual2);
	}
	
}