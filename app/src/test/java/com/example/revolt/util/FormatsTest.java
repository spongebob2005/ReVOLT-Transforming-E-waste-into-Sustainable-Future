package com.example.revolt.util;

import static org.junit.Assert.assertEquals;

import com.example.revolt.data.SessionManager;

import org.junit.Test;

public class FormatsTest {
    @Test
    public void kgDropsTrailingZeroAndRoundsToOneDecimal() {
        assertEquals("12", Formats.kg(12.0));
        assertEquals("0.5", Formats.kg(0.45).replace(',', '.'));
        assertEquals("0", Formats.kg(0));
    }

    @Test
    public void initialsUseFirstTwoWords() {
        assertEquals("AP", Formats.initials("asha  patil kumar"));
        assertEquals("R", Formats.initials("Ravi"));
        assertEquals("?", Formats.initials("  "));
        assertEquals("?", Formats.initials(null));
    }

    @Test
    public void firstName() {
        assertEquals("Asha", Formats.firstName(" Asha Patil"));
        assertEquals("", Formats.firstName(null));
    }

    @Test
    public void unparseableDatesFormatAsEmpty() {
        assertEquals("", Formats.date("not a date"));
        assertEquals("", Formats.dateTime(null));
    }

    @Test
    public void serverUrlIsNormalized() {
        assertEquals("http://192.168.1.5:3000/", SessionManager.normalizeUrl(" 192.168.1.5:3000 "));
        assertEquals("https://revolt.example.com/", SessionManager.normalizeUrl("https://revolt.example.com"));
        assertEquals("http://10.0.2.2:3000/", SessionManager.normalizeUrl("http://10.0.2.2:3000/"));
    }
}
