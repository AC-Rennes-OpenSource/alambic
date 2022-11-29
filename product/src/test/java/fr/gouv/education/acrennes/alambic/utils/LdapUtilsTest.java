package fr.gouv.education.acrennes.alambic.utils;

import org.junit.Assert;
import org.junit.Test;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.io.UnsupportedEncodingException;

public class LdapUtilsTest {
    private final static int UPDATE = 0;
    private final static int IGNORE = 3;

    @Test
    public void testCompareCaseInsensitiveAttributes() throws UnsupportedEncodingException, NamingException {
        Attribute attr1 = new BasicAttribute("foo");
        attr1.add("foo");
        Attribute attr2 = new BasicAttribute("foo");
        attr2.add("Foo");

        Assert.assertEquals(IGNORE, LdapUtils.compareAttributes(attr1, attr2, false));
    }

    @Test
    public void testCompareCaseSensitiveAttributes() throws UnsupportedEncodingException, NamingException {
        Attribute attr1 = new BasicAttribute("foo");
        attr1.add("foo");
        Attribute attr2 = new BasicAttribute("foo");
        attr2.add("Foo");

        Assert.assertEquals(UPDATE, LdapUtils.compareAttributes(attr1, attr2, true));
    }
}
