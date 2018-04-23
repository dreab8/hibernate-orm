package org.hibernate.type.descriptor.java;

import org.hibernate.type.descriptor.java.internal.BooleanJavaDescriptor;

import org.junit.Test;

import static org.junit.Assert.*;

public class BooleanTypeDescriptorTest {
    private BooleanJavaDescriptor underTest = new BooleanJavaDescriptor();

    @Test
    public void testWrapShouldReturnTrueWhenYStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("Y", null);
        // then
        assertTrue(result);
    }

    @Test
    public void testWrapShouldReturnFalseWhenFStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("N", null);
        // then
        assertFalse(result);
    }

    @Test
    public void testWrapShouldReturnFalseWhenRandomStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("k", null);
        // then
        assertFalse(result);
    }

    @Test
    public void testWrapShouldReturnNullWhenNullStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap(null, null);
        // then
        assertNull(result);
    }

    @Test
    public void testWrapShouldReturnFalseWhenEmptyStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("", null);
        // then
        assertFalse(result);
    }
}