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
package fr.gouv.education.acrennes.alambic.utils.lambert;


import static fr.gouv.education.acrennes.alambic.utils.lambert.LambertZone.*;
import static java.lang.Math.*;

public class Lambert {

    /*
     *   ALGO0002
     */
    private static double latitudeFromLatitudeISO(double latISo, double e, double eps) {

        double phi0 = 2 * atan(exp(latISo)) - M_PI_2;
        double phiI = 2 * atan(pow((1 + e * sin(phi0)) / (1 - e * sin(phi0)), e / 2.0) * exp(latISo)) - M_PI_2;
        double delta = abs(phiI - phi0);

        while (delta > eps) {
            phi0 = phiI;
            phiI = 2 * atan(pow((1 + e * sin(phi0)) / (1 - e * sin(phi0)), e / 2.0) * exp(latISo)) - M_PI_2;
            delta = abs(phiI - phi0);
        }

        return phiI;

    }

    /*
     *	ALGO0004 - Lambert vers geographiques
     */

    private static LambertPoint lambertToGeographic(LambertPoint org, LambertZone zone, double lonMeridian, double e, double eps) {
        double n = zone.n();
        double C = zone.c();
        double xs = zone.xs();
        double ys = zone.ys();

        double x = org.getX();
        double y = org.getY();


        double lon, gamma, R, latIso;

        R = sqrt((x - xs) * (x - xs) + (y - ys) * (y - ys));

        gamma = atan((x - xs) / (ys - y));

        lon = lonMeridian + gamma / n;

        latIso = -1 / n * log(abs(R / C));

        double lat = latitudeFromLatitudeISO(latIso, e, eps);

        LambertPoint dest = new LambertPoint(lon, lat, 0);
        return dest;
    }

    /*
     * ALGO0021 - Calcul de la grande Normale
     *
     */

    private static double lambertNormal(double lat, double a, double e) {

        return a / sqrt(1 - e * e * sin(lat) * sin(lat));
    }

    /*
     * ALGO0009 - Transformations geographiques -> cartésiennes
     *
     */

    private static LambertPoint geographicToCartesian(double lon, double lat, double he, double a, double e) {
        double N = lambertNormal(lat, a, e);

        LambertPoint pt = new LambertPoint(0, 0, 0);

        pt.setX((N + he) * cos(lat) * cos(lon));
        pt.setY((N + he) * cos(lat) * sin(lon));
        pt.setZ((N * (1 - e * e) + he) * sin(lat));

        return pt;

    }

    /*
     * ALGO0012 - Passage des coordonnées cartésiennes aux coordonnées géographiques
     */

    private static LambertPoint cartesianToGeographic(LambertPoint org, double meridien, double a, double e, double eps) {
        double x = org.getX(), y = org.getY(), z = org.getZ();

        double lon = meridien + atan(y / x);

        double module = sqrt(x * x + y * y);

        double phi0 = atan(z / (module * (1 - (a * e * e) / sqrt(x * x + y * y + z * z))));
        double phiI = atan(z / module / (1 - a * e * e * cos(phi0) / (module * sqrt(1 - e * e * sin(phi0) * sin(phi0)))));
        double delta = abs(phiI - phi0);
        while (delta > eps) {
            phi0 = phiI;
            phiI = atan(z / module / (1 - a * e * e * cos(phi0) / (module * sqrt(1 - e * e * sin(phi0) * sin(phi0)))));
            delta = abs(phiI - phi0);

        }

        double he = module / cos(phiI) - a / sqrt(1 - e * e * sin(phiI) * sin(phiI));

        LambertPoint pt = new LambertPoint(lon, phiI, he);

        return pt;
    }
    /*
     * Convert Lambert -> WGS84
     * http://geodesie.ign.fr/contenu/fichiers/documentation/pedagogiques/transfo.pdf
     *
     */

    public static LambertPoint convertToWGS84(LambertPoint org, LambertZone zone) {

        if (zone == Lambert93) {
            return lambertToGeographic(org, Lambert93, LON_MERID_IERS, E_WGS84, DEFAULT_EPS);
        } else {
            LambertPoint pt1 = lambertToGeographic(org, zone, LON_MERID_PARIS, E_CLARK_IGN, DEFAULT_EPS);

            LambertPoint pt2 = geographicToCartesian(pt1.getX(), pt1.getY(), pt1.getZ(), A_CLARK_IGN, E_CLARK_IGN);

            pt2.translate(-168, -60, 320);

            //WGS84 refers to greenwich
            return cartesianToGeographic(pt2, LON_MERID_GREENWICH, A_WGS84, E_WGS84, DEFAULT_EPS);
        }
    }

    public static LambertPoint convertToWGS84(double x, double y, LambertZone zone) {

        LambertPoint pt = new LambertPoint(x, y, 0);
        return convertToWGS84(pt, zone);
    }

    public static LambertPoint convertToWGS84Deg(double x, double y, LambertZone zone) {

        LambertPoint pt = new LambertPoint(x, y, 0);
        return convertToWGS84(pt, zone).toDegree();
    }

}


