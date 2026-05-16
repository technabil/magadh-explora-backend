package com.magadhexplora.api.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CountryMapsTest {

    @Test
    void currencyForKnownCodes() {
        assertEquals("USD", CountryMaps.currencyFor("US"));
        assertEquals("EUR", CountryMaps.currencyFor("DE"));
        assertEquals("JPY", CountryMaps.currencyFor("JP"));
        assertEquals("INR", CountryMaps.currencyFor("IN"));
    }

    @Test
    void currencyFallsBackToDefaultForUnknown() {
        assertEquals(CountryMaps.DEFAULT_CURRENCY, CountryMaps.currencyFor("XX"));
        assertEquals(CountryMaps.DEFAULT_CURRENCY, CountryMaps.currencyFor(null));
    }

    @Test
    void currencyIsCaseInsensitive() {
        assertEquals("USD", CountryMaps.currencyFor("us"));
        assertEquals("JPY", CountryMaps.currencyFor("jp"));
    }

    @Test
    void languageForKnownCodes() {
        assertEquals("ja", CountryMaps.languageFor("JP"));
        assertEquals("zh", CountryMaps.languageFor("CN"));
        assertEquals("hi", CountryMaps.languageFor("IN"));
        assertEquals("dz", CountryMaps.languageFor("BT"));
    }

    @Test
    void languageFallsBackToDefaultForUnknown() {
        assertEquals(CountryMaps.DEFAULT_LANG, CountryMaps.languageFor("ZZ"));
        assertEquals(CountryMaps.DEFAULT_LANG, CountryMaps.languageFor(null));
    }
}
