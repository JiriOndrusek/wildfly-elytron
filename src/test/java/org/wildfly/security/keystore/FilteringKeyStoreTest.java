/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.security.keystore;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Testing of the filtering KeyStore implementation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class FilteringKeyStoreTest {

    private static KeyStore baseKeyStore;

    @BeforeClass
    public static void loadKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("jks");
        try (InputStream is = FilteringKeyStoreTest.class.getResourceAsStream("filtered.keystore")) {
            keyStore.load(is, "Elytron".toCharArray());
        }
        baseKeyStore = keyStore;
    }

    public void performTest(Predicate<String> aliasPredicate, String... expectedAlias) throws Exception {
        Set<String> expectedSet = new HashSet<>(Arrays.asList(expectedAlias));
        Enumeration<String> baseAliases = baseKeyStore.aliases();
        assertEquals("Base KeyStore Alias Count", 4, baseKeyStore.size());
        while (baseAliases.hasMoreElements()) {
            String currentAlias = baseAliases.nextElement();
            System.out.println("Testing Alias " + currentAlias);

            assertEquals(String.format("Alias '%s'", currentAlias), expectedSet.contains(currentAlias),
                    aliasPredicate.test(currentAlias));
        }

        KeyStore testStore = FilteringKeyStore.filteringKeyStore(baseKeyStore, aliasPredicate);

        assertEquals("Expected number of aliases", expectedSet.size(), testStore.size());
        baseAliases = baseKeyStore.aliases();
        while (baseAliases.hasMoreElements()) {
            String currentAlias = baseAliases.nextElement();

            assertEquals(String.format("Alias '%s'", currentAlias), expectedSet.contains(currentAlias),
                    testStore.containsAlias(currentAlias));
        }
    }

    @Test
    public void testAll() throws Exception {
        performTest(AliasFilter.ALL, "alias1", "alias2", "alias3", "alias4");
    }

    @Test
    public void testAllFilter() throws Exception {
        performTest(AliasFilter.fromString("ALL"), "alias1", "alias2", "alias3", "alias4");
    }

    @Test
    public void testCommaFilter() throws Exception {
        performTest(AliasFilter.fromString("alias1,alias2,alias3"), "alias1", "alias2", "alias3");
    }

    @Test
    public void testAllMinusOne() throws Exception {
        performTest(AliasFilter.ALL.remove("alias4"), "alias1", "alias2", "alias3");
    }

    @Test
    public void testAllMinusOneFilter() throws Exception {
        performTest(AliasFilter.fromString("ALL:-alias4"), "alias1", "alias2", "alias3");
    }

    @Test
    public void testNone() throws Exception {
        performTest(AliasFilter.NONE);
    }

    @Test
    public void testNoneFilter() throws Exception {
        performTest(AliasFilter.fromString("NONE"));
    }

    @Test
    public void testNonePlusOne() throws Exception {
        performTest(AliasFilter.NONE.add("alias2"), "alias2");
    }

    @Test
    public void testNonePlusOneFilter() throws Exception {
        performTest(AliasFilter.fromString("NONE:+alias2"), "alias2");
    }

    @Test
    public void testJustOneFilter() throws Exception {
        performTest(AliasFilter.fromString("alias2"), "alias2");
    }

    /**
     * This is not a filter we would expect to see used, however we document that the filter is interpreted right to left so the
     * end result should be that 'alias4' is included.
     */
    @Test
    public void testAmbiguousFilter() throws Exception {
        performTest(AliasFilter.fromString("ALL:-alias4:+alias4"), "alias1", "alias2", "alias3", "alias4");
    }

}
