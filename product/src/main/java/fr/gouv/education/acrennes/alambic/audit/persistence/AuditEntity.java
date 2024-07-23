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
package fr.gouv.education.acrennes.alambic.audit.persistence;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.SerializationUtils;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.Indexes;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Indexes({
        @Index(name = "auditentity_pk_idx", unique = true, columnNames = { "id" }),
        @Index(name = "auditentity_pk_source", columnNames = { "source" })
})
public class AuditEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // private static final Log log = LogFactory.getLog(AuditEntity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date")
    private Date date;

    @Column(name = "fulltext")
    private String fulltext;

    @Column(name = "source")
    private String source;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "object")
    private Serializable object;

    public AuditEntity() {
        setDate(new Date());
    }

    public AuditEntity(final String source, final String fulltext) {
        this();
        setSource(source);
        setFulltext(fulltext);
    }

    public AuditEntity(final String source, final String fulltext, final Serializable object) {
        this(source, fulltext);
        setObject(object);
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getFulltext() {
        return fulltext;
    }

    public void setFulltext(final String fulltext) {
        this.fulltext = fulltext;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public Serializable getObject() {
        return object;
    }

    public void setObject(final Serializable object) {
        byte[] bytes = SerializationUtils.serialize(object);
        byte[] b64rep = Base64.encodeBase64(bytes);
        this.object = new String(b64rep, Charsets.UTF_8);
    }

    public String getSource() {
        return source;
    }

    public void setSource(final String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "{\"id\":\"" + id + "\",\"source\":\"" + source + "\",\"date\":\"" + date.toString() + "\",\"fulltext\":\"" + fulltext + "\"}";
    }

}