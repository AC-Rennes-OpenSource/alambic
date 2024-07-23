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
package fr.gouv.education.acrennes.alambic.nuxeo.marshaller;

import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ReportUnmarshallerTest {

    @Test
    public void test1() throws JAXBException {
        ReportUnmarshaller unmarshaller = new ReportUnmarshaller();
        Report report = unmarshaller.unmarshall("{ \"report\":{ \"status\":\"success\", \"activity\":{ \"job_name\":\"my_job\", " +
                                                "\"status\":\"COMPLETED\", \"traffic_light\":\"GREEN\", \"progress\":100, \"duration\":236, " +
                                                "\"inner_jobs\":[ ] } } }");
        assertEquals(Report.STATUS.success, Report.STATUS.valueOf(report.getStatus()));
        assertNull(report.getReasons());
        assertEquals("my_job", report.getActivity().getJob_name());
        assertEquals(236, report.getActivity().getDuration());
        assertEquals(100, report.getActivity().getProgress());
        assertEquals("GREEN", report.getActivity().getTraffic_light());
        assertEquals("COMPLETED", report.getActivity().getStatus());
        assertEquals(Collections.EMPTY_LIST, report.getActivity().getInner_jobs());
    }

    @Test
    public void test2() throws JAXBException {
        ReportUnmarshaller unmarshaller = new ReportUnmarshaller();
        Report report = unmarshaller.unmarshall("{ \"report\":{ \"status\":\"error\", \"reasons\":[\"Oups!\"], \"activity\":{ " +
                                                "\"job_name\":\"my_job\", \"status\":\"COMPLETED\", \"traffic_light\":\"RED\", \"progress\":100, " +
                                                "\"duration\":236, \"inner_jobs\":[ ] } } }");
        assertEquals(Report.STATUS.error, Report.STATUS.valueOf(report.getStatus()));
        assertEquals(1, report.getReasons().size());
        assertEquals("Oups!", report.getReasons().get(0));
        assertEquals("my_job", report.getActivity().getJob_name());
        assertEquals(236, report.getActivity().getDuration());
        assertEquals(100, report.getActivity().getProgress());
        assertEquals("RED", report.getActivity().getTraffic_light());
        assertEquals("COMPLETED", report.getActivity().getStatus());
        assertEquals(Collections.EMPTY_LIST, report.getActivity().getInner_jobs());
    }

    @Test
    public void test3() throws JAXBException {
        ReportUnmarshaller unmarshaller = new ReportUnmarshaller();
        Report report = unmarshaller.unmarshall("{ \"report\":{ \"status\":\"error\", \"reasons\":[\"Oups!\"], \"activity\":{ " +
                                                "\"job_name\":\"my_job\", \"status\":\"COMPLETED\", \"traffic_light\":\"RED\", \"progress\":100, " +
                                                "\"duration\":236, \"inner_jobs\":[ { \"job_name\":\"my_inner_job_1\", \"status\":\"COMPLETED\", " +
                                                "\"traffic_light\":\"GREEN\", \"progress\":100, \"duration\":74, \"inner_jobs\":[] }, { " +
                                                "\"job_name\":\"my_inner_job_2\", \"status\":\"COMPLETED\", \"traffic_light\":\"RED\", " +
                                                "\"progress\":100, \"duration\":162, \"inner_jobs\":[] } ] } } }");
        assertEquals(Report.STATUS.error, Report.STATUS.valueOf(report.getStatus()));
        assertEquals(1, report.getReasons().size());
        assertEquals("Oups!", report.getReasons().get(0));
        assertEquals("my_job", report.getActivity().getJob_name());
        assertEquals(236, report.getActivity().getDuration());
        assertEquals(100, report.getActivity().getProgress());
        assertEquals("RED", report.getActivity().getTraffic_light());
        assertEquals("COMPLETED", report.getActivity().getStatus());
        assertEquals(2, report.getActivity().getInner_jobs().size());

        assertEquals("my_inner_job_1", report.getActivity().getInner_jobs().get(0).getJob_name());
        assertEquals(74, report.getActivity().getInner_jobs().get(0).getDuration());
        assertEquals(100, report.getActivity().getInner_jobs().get(0).getProgress());
        assertEquals("GREEN", report.getActivity().getInner_jobs().get(0).getTraffic_light());
        assertEquals("COMPLETED", report.getActivity().getInner_jobs().get(0).getStatus());
        assertEquals(Collections.EMPTY_LIST, report.getActivity().getInner_jobs().get(0).getInner_jobs());

        assertEquals("my_inner_job_2", report.getActivity().getInner_jobs().get(1).getJob_name());
        assertEquals(162, report.getActivity().getInner_jobs().get(1).getDuration());
        assertEquals(100, report.getActivity().getInner_jobs().get(1).getProgress());
        assertEquals("RED", report.getActivity().getInner_jobs().get(1).getTraffic_light());
        assertEquals("COMPLETED", report.getActivity().getInner_jobs().get(1).getStatus());
        assertEquals(Collections.EMPTY_LIST, report.getActivity().getInner_jobs().get(1).getInner_jobs());
    }

}