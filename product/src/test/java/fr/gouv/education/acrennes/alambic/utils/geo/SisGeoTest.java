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
package fr.gouv.education.acrennes.alambic.utils.geo;

import org.apache.sis.geometry.DirectPosition2D;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

public class SisGeoTest {

    private final GeoConvert geoConvert;

    public SisGeoTest() throws FactoryException {
        geoConvert = new GeoConvert();
    }

    @Test
    public void testStPierreEtMiquelon() throws FactoryException, MismatchedDimensionException, TransformException {
        // Lycée général Emile Letournel de Saint-Pierre - 9750001C
        // Rue Marcel Bonin - 97500 Saint pierre

        final DirectPosition ptSrc = new DirectPosition2D(562959.6, 5181121.2);
        final DirectPosition ptDst = geoConvert.getStPierreEtMiquelonToWgs84().getMathTransform().transform(ptSrc, null);

        System.out.println("Target: " + ptDst);

        Assert.assertEquals(46.78066756, ptDst.getCoordinate()[0], 0.0001);
        Assert.assertEquals(-56.1752302, ptDst.getCoordinate()[1], 0.0001);
    }

    @Test
    public void testMayotte() throws FactoryException, MismatchedDimensionException, TransformException {
        // Collège de Boueni M Titi - 9760008E
        // Boulevard des amoureux - Dzaoudzi (Mayotte)

        final DirectPosition ptSrc = new DirectPosition2D(530360.4, 8586870.0);
        final DirectPosition ptDst = geoConvert.getMayotteToWgs84().getMathTransform().transform(ptSrc, null);

        System.out.println("Target: " + ptDst);

        // Il y a un décalage de 280 mètres plus au Nord que la position du collège sur Google Maps,
        // mais cela semble être conforme aux données du site http://www.education.gouv.fr/acce_public
        // Les coordonnées de départ sont sûrement légèrement erronées.
        Assert.assertEquals(-12.782773, ptDst.getCoordinate()[0], 0.0001);
        Assert.assertEquals(45.279728, ptDst.getCoordinate()[1], 0.0001);
    }

    @Test
    public void testGuyane() throws FactoryException, MismatchedDimensionException, TransformException {
        // Lycée général et technologique Felix Eboué - 9730001N
        // Rocade Sud - 97306 CAYENNE

        final DirectPosition ptSrc = new DirectPosition2D(354048.4, 544850.3);
        final DirectPosition ptDst = geoConvert.getGuyaneToWgs84().getMathTransform().transform(ptSrc, null);

        System.out.println("Target: " + ptDst);

        Assert.assertEquals(4.9280065, ptDst.getCoordinate()[0], 0.0001);
        Assert.assertEquals(-52.3163493, ptDst.getCoordinate()[1], 0.0001);
    }

    @Test
    public void testRgr92ToWgs84() throws MismatchedDimensionException, TransformException {

        // Coordonnées de l'établissement COLLEGE PRIVE ST CHARLES (de la Réunion) - 9741630D en RGR 92
        final DirectPosition ptSrc = new DirectPosition2D(342107.7, 7639694.1);
        final DirectPosition ptDst = geoConvert.getRgr92ToWgs84().getMathTransform().transform(ptSrc, null);

        System.out.println("Source: " + ptSrc);
        System.out.println("Target: " + ptDst);

        // Latitude :
        Assert.assertEquals(-21.3378794, ptDst.getOrdinate(0), 0.001);
        // Longitude :
        Assert.assertEquals(55.4774444, ptDst.getOrdinate(1), 0.001);

    }

    @Test
    public void testLambertToWgs84() throws FactoryException, MismatchedDimensionException, TransformException {

        // Collège des Gayeulles à Rennes
        final DirectPosition ptSrc = new DirectPosition2D(354121.65, 6790754.38);
        final DirectPosition ptDst = geoConvert.getLambert93ToWgs84().getMathTransform().transform(ptSrc, null);

        System.out.println("Source: " + ptSrc);
        System.out.println("Target: " + ptDst);

        Assert.assertEquals(48.125447, ptDst.getCoordinate()[0], 0.0001);
        Assert.assertEquals(-1.651402, ptDst.getCoordinate()[1], 0.0001);

    }

    @Test
    public void testGuadeloupeToWgs84() throws FactoryException, MismatchedDimensionException, TransformException {
        // Collège Sadi Carnot - 9710073C, Pointe-à-Pitre en Guadeloupe
        final DirectPosition ptSrc = new DirectPosition2D(656287.3, 1795918.7);
        final DirectPosition ptDst = geoConvert.getGuadeloupeToWgs84().getMathTransform().transform(ptSrc, null);

        Assert.assertEquals(16.2388, ptDst.getCoordinate()[0], 0.0001);
        Assert.assertEquals(-61.5377, ptDst.getCoordinate()[1], 0.0001);

    }

    @Test
    public void testmartiniqueToWgs84() throws FactoryException, MismatchedDimensionException, TransformException {
        // La Martinique est Dans le même référentiel que la Guadeloupe
        // Lycée général et technologique Bellevue - 9720003W
        // Rue MARIE THERESE GERTRUDE - 97262 Fort-de-France

        final DirectPosition ptSrcMartinique = new DirectPosition2D(706254.9, 1615099.2);
        final DirectPosition ptDst = geoConvert.getGuadeloupeToWgs84().getMathTransform().transform(ptSrcMartinique, null);

        Assert.assertEquals(14.601204, ptDst.getCoordinate()[0], 0.001);
        Assert.assertEquals(-61.085922, ptDst.getCoordinate()[1], 0.001);
    }


}