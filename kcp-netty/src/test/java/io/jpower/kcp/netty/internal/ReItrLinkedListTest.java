package io.jpower.kcp.netty.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class ReItrLinkedListTest {

    @Test
    public void iterator() throws Exception {
        ReItrLinkedList<String> l = new ReItrLinkedList<>();
        String[] strs = {"abc", "123", "hehe"};
        Collections.addAll(l, strs);

        ReusableIterator<String> itr = l.iterator();
        assertEquals(strs[0], itr.next());
        assertEquals(strs[1], itr.next());

        ReusableIterator<String> retItr = itr.rewind();
        assertTrue(retItr == itr);

        assertEquals(strs[0], itr.next());
        assertEquals(strs[1], itr.next());
        assertEquals(strs[2], itr.next());

        // exception
        try {
            itr.next();
            fail("should throw exception");
        } catch (NoSuchElementException ignored) {

        }
    }

    @Test
    public void listIterator() throws Exception {
        ReItrLinkedList<String> l = new ReItrLinkedList<>();
        String[] strs = {"abc", "123", "hehe"};
        Collections.addAll(l, strs);

        ReusableListIterator<String> itr = l.listIterator();
        assertEquals(strs[0], itr.next());
        assertEquals(strs[1], itr.next());
        assertEquals(strs[1], itr.previous());

        ReusableIterator<String> retItr = itr.rewind();
        assertTrue(retItr == itr);

        assertFalse(itr.hasPrevious());
        assertEquals(strs[0], itr.next());

        // rewind with index
        retItr = itr.rewind(1);
        assertTrue(retItr == itr);
        assertTrue(itr.hasPrevious());
        assertEquals(0, itr.previousIndex());
        assertEquals(1, itr.nextIndex());
        assertEquals(strs[1], itr.next());
    }

    @Test
    public void listIteratorInt() throws Exception {
        ReItrLinkedList<String> l = new ReItrLinkedList<>();
        String[] strs = {"abc", "123", "hehe"};
        Collections.addAll(l, strs);

        ReusableListIterator<String> itr = l.listIterator(1);
        assertEquals(strs[1], itr.next());
        assertEquals(strs[1], itr.previous());

        ReusableIterator<String> retItr = itr.rewind();
        assertTrue(retItr == itr);

        assertFalse(itr.hasPrevious());
        assertEquals(strs[0], itr.next());

        // rewind with index
        retItr = itr.rewind(1);
        assertTrue(retItr == itr);
        assertTrue(itr.hasPrevious());
        assertEquals(0, itr.previousIndex());
        assertEquals(1, itr.nextIndex());
        assertEquals(strs[1], itr.next());
    }

}