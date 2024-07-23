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
package fr.gouv.education.acrennes.alambic.jobs.load.gar;

import fr.gouv.education.acrennes.alambic.jobs.JobContext;
import fr.gouv.education.acrennes.alambic.jobs.load.AbstractDestination.IsAnythingToDoStatus;
import fr.gouv.education.acrennes.alambic.jobs.load.Destination;
import fr.gouv.education.acrennes.alambic.jobs.load.DestinationFactory;
import fr.gouv.education.acrennes.alambic.jobs.load.NxmlToNuxeo;
import fr.gouv.education.acrennes.alambic.jobs.load.StateBaseToLdap;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityHelper;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean;
import fr.gouv.education.acrennes.alambic.monitoring.ActivityMBean.ACTIVITY_TYPE;
import fr.gouv.education.acrennes.alambic.utils.Variables;
import org.jdom2.Element;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.directory.InitialDirContext;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ NxmlToNuxeo.class, StateBaseToLdap.class, Element.class })
@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" })
public class DestinationTest {

    private static final String RUN_ID = "1";
    private static final String LDAP_ENTRY_NAME_SPACE = "ou=personnes,dc=ent-bretagne,dc=fr";

    private HttpAutomationClient mockedNuxeoAutomationClient;
    private Session mockedNuxeoClientSession;
    private Element mockedDestinationXMLElement;
    private InitialDirContext mockedDirContext;

    private ActivityMBean jobActivity;
    private JobContext context;
    private Variables variables;
    private Properties configuration;

    @Before
    public void setUp() throws Exception {
        this.configuration = new Properties();
        this.variables = new Variables();
        this.context = new JobContext(".", null, variables, configuration);
        this.jobActivity = ActivityHelper.getMBean("testu", ACTIVITY_TYPE.INNER, RUN_ID);
        this.mockedDestinationXMLElement = PowerMockito.mock(Element.class);

        // Prepare NUXEO mock
        this.mockedNuxeoAutomationClient = PowerMockito.mock(HttpAutomationClient.class);
        this.mockedNuxeoClientSession = PowerMockito.mock(Session.class);
        PowerMockito.whenNew(HttpAutomationClient.class).withAnyArguments().thenReturn(this.mockedNuxeoAutomationClient);
        PowerMockito.when(this.mockedNuxeoAutomationClient.getSession(nullable(String.class), nullable(String.class))).thenReturn(this.mockedNuxeoClientSession);

        // Prepare LDAP mock
        mockedDirContext = PowerMockito.mock(InitialDirContext.class);
        PowerMockito.whenNew(InitialDirContext.class).withAnyArguments().thenReturn(mockedDirContext);
        PowerMockito.when(mockedDirContext.getNameInNamespace()).thenReturn(LDAP_ENTRY_NAME_SPACE);
        PowerMockito.when(mockedDirContext.lookup(nullable(String.class))).thenReturn(mockedDirContext);
    }

    @After
    public void tearDown() {
        ActivityHelper.releaseMBean(this.jobActivity);
    }

    /* test use case :
     * - Destination connector : nuxeo
     * - The input file defines NO document to be either created or modified.
     * - Expected result : the destination operations are voided
     **/
    @Test
    public void test1() {
        try {
            String pivotFileAbsolutePath = DestinationTest.class.getClassLoader()
                    .getResource("data/jobs/destination/XmlToNuxeo-test1-input.xml")
                    .getPath();

            PowerMockito.when(mockedDestinationXMLElement.getAttributeValue(nullable(String.class)))
                    .thenReturn("NuxeoTarget")
                    .thenReturn("nuxeo");

            PowerMockito.when(mockedDestinationXMLElement.getChildText(nullable(String.class)))
                    .thenReturn("uri")
                    .thenReturn("login")
                    .thenReturn("passwd")
                    .thenReturn("path")
                    .thenReturn(pivotFileAbsolutePath);

            Destination destination = DestinationFactory.getDestination(this.context, this.mockedDestinationXMLElement, this.jobActivity);
            assertEquals(destination.isAnythingToDo(), IsAnythingToDoStatus.NO);
            Mockito.verify(this.mockedNuxeoAutomationClient, never()).getSession(nullable(String.class), nullable(String.class));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /* test use case :
     * - Destination connector : nuxeo
     * - The input file DO define documents to be either created or modified.
     * - Expected result : the destination operations are done.
     **/
    @Test
    public void test2() {
        try {
            String pivotFileAbsolutePath = DestinationTest.class.getClassLoader()
                    .getResource("data/jobs/destination/XmlToNuxeo-test2-input.xml")
                    .getPath();

            PowerMockito.when(mockedDestinationXMLElement.getAttributeValue(nullable(String.class)))
                    .thenReturn("NuxeoTarget")
                    .thenReturn("nuxeo");

            PowerMockito.when(mockedDestinationXMLElement.getChildText(nullable(String.class)))
                    .thenReturn("uri")
                    .thenReturn("login")
                    .thenReturn("passwd")
                    .thenReturn("path")
                    .thenReturn(pivotFileAbsolutePath);

            Destination destination = DestinationFactory.getDestination(this.context, this.mockedDestinationXMLElement, this.jobActivity);
            assertEquals(destination.isAnythingToDo(), IsAnythingToDoStatus.YES);
            Mockito.verify(this.mockedNuxeoAutomationClient, times(1)).getSession(nullable(String.class), nullable(String.class));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /* test use case :
     * - Destination connector : nuxeo
     * - The input file DO define documents to be either created or modified.
     * - The input file root element is not 'nuxeomatic'
     * - Expected result : the destination operations are done.
     **/
    @Test
    public void test3() {
        try {
            String pivotFileAbsolutePath = DestinationTest.class.getClassLoader()
                    .getResource("data/jobs/destination/XmlToNuxeo-test3-input.xml")
                    .getPath();

            PowerMockito.when(mockedDestinationXMLElement.getAttributeValue(nullable(String.class)))
                    .thenReturn("NuxeoTarget")
                    .thenReturn("nuxeo");

            PowerMockito.when(mockedDestinationXMLElement.getChildText(nullable(String.class)))
                    .thenReturn("uri")
                    .thenReturn("login")
                    .thenReturn("passwd")
                    .thenReturn("path")
                    .thenReturn(pivotFileAbsolutePath);

            Destination destination = DestinationFactory.getDestination(this.context, this.mockedDestinationXMLElement, this.jobActivity);
            assertEquals(destination.isAnythingToDo(), IsAnythingToDoStatus.YES);
            Mockito.verify(this.mockedNuxeoAutomationClient, times(1)).getSession(nullable(String.class), nullable(String.class));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /* test use case :
     * - Destination connector : ldap
     * - The input file defines NO document to be either created or modified.
     * - Expected result : the destination operations are voided
     **/
    @Test
    public void test4() {
        try {
            String pivotFileAbsolutePath = DestinationTest.class.getClassLoader()
                    .getResource("data/jobs/destination/StateBaseToLdap-test1-input.xml")
                    .getPath();

            PowerMockito.when(mockedDestinationXMLElement.getAttributeValue(nullable(String.class)))
                    .thenReturn("LDAPTarget")
                    .thenReturn("ldap");

            PowerMockito.when(mockedDestinationXMLElement.getChildText(nullable(String.class)))
                    .thenReturn("com.sun.jndi.ldap.LdapCtxFactory")
                    .thenReturn("uri")
                    .thenReturn("login")
                    .thenReturn("passwd")
                    .thenReturn(pivotFileAbsolutePath);

            Destination destination = DestinationFactory.getDestination(this.context, this.mockedDestinationXMLElement, this.jobActivity);
            assertEquals(destination.isAnythingToDo(), IsAnythingToDoStatus.NO);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /* test use case :
     * - Destination connector : ldap
     * - The input file DO define documents to be either created or modified.
     * - Expected result : the destination operations are done.
     **/
    @Test
    public void test5() {
        try {
            String pivotFileAbsolutePath = DestinationTest.class.getClassLoader()
                    .getResource("data/jobs/destination/StateBaseToLdap-test2-input.xml")
                    .getPath();

            PowerMockito.when(mockedDestinationXMLElement.getAttributeValue(nullable(String.class)))
                    .thenReturn("LDAPTarget")
                    .thenReturn("ldap");

            PowerMockito.when(mockedDestinationXMLElement.getChildText(nullable(String.class)))
                    .thenReturn("com.sun.jndi.ldap.LdapCtxFactory")
                    .thenReturn("uri")
                    .thenReturn("login")
                    .thenReturn("passwd")
                    .thenReturn(pivotFileAbsolutePath);

            Destination destination = DestinationFactory.getDestination(this.context, this.mockedDestinationXMLElement, this.jobActivity);
            assertEquals(destination.isAnythingToDo(), IsAnythingToDoStatus.YES);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /* test use case :
     * - Destination connector : ldap
     * - The input file DO define documents to be either created or modified.
     * - The input file root element is not 'alambic'
     * - Expected result : the destination operations are done.
     **/
    @Test
    public void test6() {
        try {
            String pivotFileAbsolutePath = DestinationTest.class.getClassLoader()
                    .getResource("data/jobs/destination/StateBaseToLdap-test3-input.xml")
                    .getPath();

            PowerMockito.when(mockedDestinationXMLElement.getAttributeValue(nullable(String.class)))
                    .thenReturn("LDAPTarget")
                    .thenReturn("ldap");

            PowerMockito.when(mockedDestinationXMLElement.getChildText(nullable(String.class)))
                    .thenReturn("com.sun.jndi.ldap.LdapCtxFactory")
                    .thenReturn("uri")
                    .thenReturn("login")
                    .thenReturn("passwd")
                    .thenReturn(pivotFileAbsolutePath);

            Destination destination = DestinationFactory.getDestination(this.context, this.mockedDestinationXMLElement, this.jobActivity);
            assertEquals(destination.isAnythingToDo(), IsAnythingToDoStatus.YES);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

}
