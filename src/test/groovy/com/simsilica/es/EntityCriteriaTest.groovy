/*
 * $Id$
 *
 * Copyright (c) 2024, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.es;


/**
 *  Tests for the EntityCriteria class.
 *
 *  @author    Paul Speed
 */
public class EntityCriteriaTest {

    static class SetTest extends GroovyTestCase {
        void testInOrder() {
            def filter1 = Name.filter("test");
            def criteria = new EntityCriteria().set(filter1, Name.class, CreatedBy.class);

            def filters = criteria.toFilterArray();
            def types = criteria.toTypeArray();

            assert [filter1, null] == filters;
            assert [Name.class, CreatedBy.class] == types;
        }

        void testOutOfOrder() {
            def filter1 = Name.filter("test");
            def criteria = new EntityCriteria().set(filter1, CreatedBy.class, Name.class);

            def filters = criteria.toFilterArray();
            def types = criteria.toTypeArray();

            assert [null, filter1] == filters;
            assert [CreatedBy.class, Name.class] == types;
        }
    }

    static class AddTest extends GroovyTestCase {
        void testAddFilters() {
            def filter1 = Name.filter("test");
            def filter2 = CreatedBy.filter(new EntityId(42));
            def criteria = new EntityCriteria().add(filter1, filter2);

            def filters = criteria.toFilterArray();
            def types = criteria.toTypeArray();

            assert [filter1, filter2] == filters;
            assert [Name.class, CreatedBy.class] == types;
        }

        void testAddTypes() {
            def criteria = new EntityCriteria().add(Name.class, CreatedBy.class);

            def filters = criteria.toFilterArray();
            def types = criteria.toTypeArray();

            assert [null, null] == filters;
            assert [Name.class, CreatedBy.class] == types;
        }
    }

    static class IsMatchingComponentTest extends GroovyTestCase {
        void testTypeOnly() {
            def criteria = new EntityCriteria().add(Name.class);
            assert criteria.hasType(Name.class);
            assert !criteria.hasType(CreatedBy.class);
        }

        void testOneFilter() {
            def filter1 = Name.filter("test");
            def criteria = new EntityCriteria().add(filter1);
            assert criteria.isMatchingComponent(new Name("test"));
            assert !criteria.isMatchingComponent(new Name("not-test"));
            assert !criteria.isMatchingComponent(new CreatedBy(new EntityId(42)));
        }

        void testTwoFilters() {
            def filter1 = Name.filter("test");
            def filter2 = CreatedBy.filter(new EntityId(42));
            def criteria = new EntityCriteria().add(filter1, filter2);
            assert criteria.isMatchingComponent(new Name("test"));
            assert !criteria.isMatchingComponent(new Name("not-test"));
            assert criteria.isMatchingComponent(new CreatedBy(new EntityId(42)));
            assert !criteria.isMatchingComponent(new CreatedBy(new EntityId(43)));
        }

        void testMixed() {
            def filter1 = Name.filter("test");
            def criteria = new EntityCriteria().add(filter1).add(CreatedBy.class);
            assert criteria.isMatchingComponent(new Name("test"));
            assert !criteria.isMatchingComponent(new Name("not-test"));
            assert criteria.isMatchingComponent(new CreatedBy(new EntityId(42)));
        }
    }
}


