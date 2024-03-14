/*******************************************************************************
 * Copyright (C) 2019-2024 Rennes - Brittany Education Authority (<http://www.ac-rennes.fr>) and others.
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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;

public class JwtUtils {
    public static String createTokenFrom(String kid, String secret, int expirationTime) {
        byte[] secretBytes = DatatypeConverter.parseBase64Binary(secret);
        Key signingKey = Keys.hmacShaKeyFor(secretBytes);
        return "Bearer " + Jwts.builder()
                .setHeaderParam("kid", kid)
                .setExpiration(getExpirationDateFrom(expirationTime))
                .signWith(signingKey)
                .compact();
    }

    private static Date getExpirationDateFrom(int expirationTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, expirationTime);
        return calendar.getTime();
    }
}
