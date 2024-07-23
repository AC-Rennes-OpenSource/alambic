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
package fr.gouv.education.acrennes.alambic.random.persistence;

import fr.gouv.education.acrennes.alambic.generator.service.RandomGeneratorService;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Indexes({
        @Index(name = "randomauditentity_pk_idx", unique = true, columnNames = { "id" }),
        @Index(name = "randomauditentity_capacityfilter_idx", columnNames = { "capacityfilter" }),
        @Index(name = "randomauditentity_type_idx", columnNames = { "type" }),
        @Index(name = "randomauditentity_hash_idx", columnNames = { "hash" }),
        @Index(name = "randomauditentity_processid_idx", columnNames = { "processid" }),
        @Index(name = "randomauditentity_exist_idx", columnNames = { "type", "blurid", "processid", "capacityfilter" }), // useful for finding
        // former produced entities
        @Index(name = "randomauditentity_capacity_idx", columnNames = { "type", "processid", "capacityfilter" }) // useful for querying the actual
        // capacity of a generator
})
public class RandomAuditEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // private static final Log log = LogFactory.getLog(RandomAuditEntity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    private String type;

    @Column(name = "processid")
    private String processId;

    @Column(name = "hash")
    private String hash;

    @Column(name = "capacityfilter")
    private String capacityFilter;

    @Column(name = "blurId")
    private String blurId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date")
    private Date date;

    public RandomAuditEntity() {
        date = new Date();
    }

    public RandomAuditEntity(final String processId, final RandomGeneratorService.GENERATOR_TYPE type, final String hash,
                             final String capacityFilter, final String blurId) {
        this();
        this.processId = processId;
        this.type = type.toString();
        this.hash = hash;
        this.capacityFilter = capacityFilter;
        this.blurId = blurId;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(final String processId) {
        this.processId = processId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(final String hash) {
        this.hash = hash;
    }

    public String getCapacityFilter() {
        return capacityFilter;
    }

    public void setCapacityFilter(final String capacityFilter) {
        this.capacityFilter = capacityFilter;
    }

    public String getBlurId() {
        return blurId;
    }

    public void setBlurId(final String blurId) {
        this.blurId = blurId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "{\"id\":\"" + id + "\",\"date\":\"" + date.toString() + "\",\"processId\":\"" + processId + "\",\"type\":\"" + type + "\"," +
               "\"capacityFilter\":\"" + capacityFilter + "\",\"hash\":\"" + hash + "\"}";
    }

}