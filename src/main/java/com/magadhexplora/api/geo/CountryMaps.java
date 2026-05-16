package com.magadhexplora.api.geo;

import java.util.Map;

/** Static country code → currency / language mapping. Admin can extend later via app_settings. */
final class CountryMaps {

    static final String DEFAULT_CURRENCY = "INR";
    static final String DEFAULT_LANG = "en";

    /** ISO country code → ISO currency code */
    static final Map<String, String> CURRENCY = Map.<String, String>ofEntries(
            Map.entry("US", "USD"), Map.entry("CA", "CAD"), Map.entry("MX", "MXN"),
            Map.entry("GB", "GBP"), Map.entry("IE", "EUR"),
            Map.entry("DE", "EUR"), Map.entry("FR", "EUR"), Map.entry("IT", "EUR"),
            Map.entry("ES", "EUR"), Map.entry("PT", "EUR"), Map.entry("NL", "EUR"),
            Map.entry("BE", "EUR"), Map.entry("AT", "EUR"), Map.entry("FI", "EUR"),
            Map.entry("GR", "EUR"), Map.entry("CH", "CHF"),
            Map.entry("SE", "SEK"), Map.entry("NO", "NOK"), Map.entry("DK", "DKK"),
            Map.entry("JP", "JPY"), Map.entry("CN", "CNY"), Map.entry("KR", "KRW"),
            Map.entry("HK", "HKD"), Map.entry("TW", "TWD"),
            Map.entry("SG", "SGD"), Map.entry("MY", "MYR"), Map.entry("TH", "THB"),
            Map.entry("ID", "IDR"), Map.entry("PH", "PHP"), Map.entry("VN", "VND"),
            Map.entry("AU", "AUD"), Map.entry("NZ", "NZD"),
            Map.entry("AE", "AED"), Map.entry("SA", "SAR"), Map.entry("QA", "QAR"),
            Map.entry("KW", "KWD"), Map.entry("BH", "BHD"), Map.entry("OM", "OMR"),
            Map.entry("IL", "ILS"), Map.entry("TR", "TRY"),
            Map.entry("BR", "BRL"), Map.entry("AR", "ARS"), Map.entry("CL", "CLP"),
            Map.entry("RU", "RUB"), Map.entry("ZA", "ZAR"), Map.entry("NG", "NGN"),
            Map.entry("EG", "EGP"),
            Map.entry("IN", "INR"), Map.entry("PK", "PKR"), Map.entry("BD", "BDT"),
            Map.entry("NP", "NPR"), Map.entry("LK", "LKR")
    );

    /** ISO country code → BCP-47 short language code (limited to ones we plan to translate to) */
    static final Map<String, String> LANGUAGE = Map.<String, String>ofEntries(
            Map.entry("US", "en"), Map.entry("CA", "en"), Map.entry("GB", "en"),
            Map.entry("AU", "en"), Map.entry("NZ", "en"), Map.entry("IE", "en"),
            Map.entry("SG", "en"), Map.entry("HK", "en"), Map.entry("ZA", "en"),
            Map.entry("JP", "ja"),
            Map.entry("CN", "zh"), Map.entry("TW", "zh"),
            Map.entry("IN", "hi"), Map.entry("NP", "hi"),
            Map.entry("TH", "th"),
            Map.entry("LK", "si"),
            Map.entry("VN", "vi"),
            Map.entry("BT", "dz"),
            Map.entry("PK", "en"), Map.entry("BD", "en")
    );

    static String currencyFor(String code) {
        return code == null ? DEFAULT_CURRENCY : CURRENCY.getOrDefault(code.toUpperCase(), DEFAULT_CURRENCY);
    }

    static String languageFor(String code) {
        return code == null ? DEFAULT_LANG : LANGUAGE.getOrDefault(code.toUpperCase(), DEFAULT_LANG);
    }

    private CountryMaps() {}
}
