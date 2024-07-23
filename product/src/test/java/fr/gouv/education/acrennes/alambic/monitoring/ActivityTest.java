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
package fr.gouv.education.acrennes.alambic.monitoring;

import fr.gouv.education.acrennes.alambic.exception.AlambicException;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean.ACTIVITY_TYPE;
import org.junit.Assert;
import org.junit.Test;

public class ActivityTest {

    @Test
    public void testSingleLevel() throws AlambicException {
        ActivityMBean amb = ActivityHelper.getMBean("root", ACTIVITY_TYPE.INNER, "1");
        Assert.assertEquals(ActivityTrafficLight.GREEN, amb.getTrafficLight());
        amb.setTrafficLight(ActivityTrafficLight.ORANGE);
        Assert.assertEquals(ActivityTrafficLight.ORANGE, amb.getTrafficLight());
    }

    @Test
    public void testMultipleLevel1() throws AlambicException {
        ActivityMBean amb_inner1 = ActivityHelper.getMBean("inner-1", ACTIVITY_TYPE.INNER, "2");
        ActivityMBean amb_inner2 = ActivityHelper.getMBean("inner-2", ACTIVITY_TYPE.INNER, "3");
        ActivityMBean amb_root = ActivityHelper.getMBean("root", ACTIVITY_TYPE.META, "1");
        amb_root.registerInnerActivity(amb_inner1);
        amb_root.registerInnerActivity(amb_inner2);

        Assert.assertEquals(ActivityTrafficLight.GREEN, amb_root.getTrafficLight());

        amb_inner1.setTrafficLight(ActivityTrafficLight.ORANGE);
        Assert.assertEquals(ActivityTrafficLight.ORANGE, amb_inner1.getTrafficLight());
        Assert.assertEquals(ActivityTrafficLight.GREEN, amb_inner2.getTrafficLight());
        Assert.assertEquals(ActivityTrafficLight.ORANGE, amb_root.getTrafficLight());
    }

    @Test
    public void testMultipleLevel2() throws AlambicException {
        ActivityMBean amb_inner1 = ActivityHelper.getMBean("inner-1", ACTIVITY_TYPE.INNER, "2");
        ActivityMBean amb_inner2 = ActivityHelper.getMBean("inner-2", ACTIVITY_TYPE.INNER, "3");
        ActivityMBean amb_root = ActivityHelper.getMBean("root", ACTIVITY_TYPE.META, "1");
        amb_root.registerInnerActivity(amb_inner1);
        amb_root.registerInnerActivity(amb_inner2);

        amb_inner1.setTrafficLight(ActivityTrafficLight.ORANGE);
        amb_inner2.setTrafficLight(ActivityTrafficLight.RED);
        Assert.assertEquals(ActivityTrafficLight.ORANGE, amb_inner1.getTrafficLight());
        Assert.assertEquals(ActivityTrafficLight.RED, amb_inner2.getTrafficLight());
        Assert.assertEquals(ActivityTrafficLight.RED, amb_root.getTrafficLight());
    }

    @Test
    public void testMultipleLevel3() throws AlambicException {
        ActivityMBean amb_inner1 = ActivityHelper.getMBean("inner-1", ACTIVITY_TYPE.INNER, "2");
        ActivityMBean amb_inner2 = ActivityHelper.getMBean("inner-2", ACTIVITY_TYPE.META, "3");
        ActivityMBean amb_root = ActivityHelper.getMBean("root", ACTIVITY_TYPE.META, "1");
        amb_root.registerInnerActivity(amb_inner1);
        amb_root.registerInnerActivity(amb_inner2);

        ActivityMBean amb_inner2_inner1 = ActivityHelper.getMBean("inner-inner-1", ACTIVITY_TYPE.INNER, "4");
        ActivityMBean amb_inner2_inner2 = ActivityHelper.getMBean("inner-inner-2", ACTIVITY_TYPE.INNER, "5");
        amb_inner2.registerInnerActivity(amb_inner2_inner1);
        amb_inner2.registerInnerActivity(amb_inner2_inner2);

        Assert.assertEquals(ActivityTrafficLight.GREEN, amb_root.getTrafficLight());

        amb_inner2_inner1.setTrafficLight(ActivityTrafficLight.RED);
        Assert.assertEquals(ActivityTrafficLight.GREEN, amb_inner1.getTrafficLight());
        Assert.assertEquals(ActivityTrafficLight.RED, amb_inner2.getTrafficLight());
        Assert.assertEquals(ActivityTrafficLight.RED, amb_root.getTrafficLight());
    }

}
