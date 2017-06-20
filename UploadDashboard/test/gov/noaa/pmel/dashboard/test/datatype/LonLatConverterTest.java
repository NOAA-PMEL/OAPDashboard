/**
 * 
 */
package gov.noaa.pmel.dashboard.test.datatype;

import static org.junit.Assert.*;

import org.junit.Test;

import gov.noaa.pmel.dashboard.datatype.LonLatConverter;
import gov.noaa.pmel.dashboard.datatype.ValueConverter;

/**
 * Unit tests for methods of {@link gov.noaa.pmel.dashboard.datatype.LonLatConverter}
 * 
 * @author Karl Smith
 */
public class LonLatConverterTest {

	/**
	 * Test method for {@link gov.noaa.pmel.dashboard.datatype.LonLatConverter#LonLatConverter(
	 * 		java.lang.String,java.lang.String,java.lang.String)}.
	 */
	@Test
	public void testLonLatConverter() {
		String[] fromLonUnits = new String[] {
				"deg E", 
				"deg W", 
				"deg min E", 
				"deg min W", 
				"deg min sec E", 
				"deg min sec W"
		};
		String[] fromLatUnits = new String[] {
				"deg N", 
				"deg S", 
				"deg min N", 
				"deg min S", 
				"deg min sec N", 
				"deg min sec S"
		};
		for ( String str : fromLonUnits ) {
			assertNotNull( new LonLatConverter(str, fromLonUnits[0], null) );
		}
		for ( String str : fromLatUnits ) {
			assertNotNull( new LonLatConverter(str, fromLatUnits[0], null) );
		}
	}

	/**
	 * Test method for {@link gov.noaa.pmel.dashboard.datatype.LonLatConverter#convertValueOf(java.lang.String, int)}.
	 */
	@Test
	public void testConvertValueOfString() {
		LonLatConverter converter = new LonLatConverter("deg E", "deg E", null);
		assertEquals(123.45, converter.convertValueOf("123.45", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(-45.67, converter.convertValueOf("-45.67", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// standardize if reasonable
		assertEquals(-45.0, converter.convertValueOf("315", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(45.0, converter.convertValueOf("-315", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// anti-meridian is +180.0
		assertEquals(180.0, converter.convertValueOf("-180.0", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// do not standardize if unreasonable
		assertEquals(9999.0, converter.convertValueOf("9999", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// missing values
		assertNull( converter.convertValueOf("-9999", ValueConverter.VALUE_NOT_APPLICABLE) );
		assertNull( converter.convertValueOf(" --- ", ValueConverter.VALUE_NOT_APPLICABLE) );

		converter = new LonLatConverter("deg W", "deg E", null);
		assertEquals(-123.45, converter.convertValueOf("123.45", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(45.67, converter.convertValueOf("-45.67", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// standardize if reasonable
		assertEquals(45.0, converter.convertValueOf("315", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(-45.0, converter.convertValueOf("-315", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// anti-meridian is +180.0
		assertEquals(180.0, converter.convertValueOf("-180.0", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// do not standardize if unreasonable
		assertEquals(-9999.0, converter.convertValueOf("9999", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		// missing values
		assertNull( converter.convertValueOf("-9999", ValueConverter.VALUE_NOT_APPLICABLE) );
		assertNull( converter.convertValueOf(" --- ", ValueConverter.VALUE_NOT_APPLICABLE) );

		converter = new LonLatConverter("deg min E", "deg E", null);
		assertEquals(-45.25, converter.convertValueOf("314" + LonLatConverter.DEGREE_SYMBOL + " 45.0'", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(45.855, converter.convertValueOf("45" + LonLatConverter.DEGREE_SYMBOL + " 51.3'", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(45.855, converter.convertValueOf("45 51.3", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);

		// degrees must be given if deg min- does not pay attention to ° and '
		boolean errCaught = false;
		try {
			converter.convertValueOf("11.4'", ValueConverter.VALUE_NOT_APPLICABLE);
		} catch ( IllegalArgumentException ex ) {
			errCaught = true;
		}
		assertTrue( errCaught );

		converter = new LonLatConverter("deg min W", "deg E", null);
		assertEquals(45.25, converter.convertValueOf("314" + LonLatConverter.DEGREE_SYMBOL + " 45.0'", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(-45.35, converter.convertValueOf("45" + LonLatConverter.DEGREE_SYMBOL + " 21'", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);

		converter = new LonLatConverter("deg min sec E", "deg E", null);
		assertEquals(-45.7675, converter.convertValueOf("314" + LonLatConverter.DEGREE_SYMBOL + " 13' 57.0\"", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(45.8365, converter.convertValueOf("45" + LonLatConverter.DEGREE_SYMBOL + " 50' 11.4\"", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(45.8365, converter.convertValueOf("45" + LonLatConverter.DEGREE_SYMBOL + " 50' 11.4''", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(45.8365, converter.convertValueOf("45  50  11.4", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);

		converter = new LonLatConverter("deg min sec W", "deg E", null);
		assertEquals(45.7675, converter.convertValueOf("314" + LonLatConverter.DEGREE_SYMBOL + " 13' 57.0\"", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
		assertEquals(-45.8365, converter.convertValueOf("45" + LonLatConverter.DEGREE_SYMBOL + " 50' 11.4\"", ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);

		// degrees and minutes must be given if deg min sec - does not pay attention to °, ', and "
		errCaught = false;
		try {
			converter.convertValueOf("45" + LonLatConverter.DEGREE_SYMBOL + " 11.4\"", ValueConverter.VALUE_NOT_APPLICABLE);
		} catch ( IllegalArgumentException ex ) {
			errCaught = true;
		}
		assertTrue( errCaught );

		errCaught = false;
		try {
			converter.convertValueOf("50' 11.4\"", ValueConverter.VALUE_NOT_APPLICABLE);
		} catch ( IllegalArgumentException ex ) {
			errCaught = true;
		}
		assertTrue( errCaught );

		// °, ', and " are ignored - always taken as deg-min-sec
		assertEquals(-45.2325, converter.convertValueOf("45\" 13' 57" + LonLatConverter.DEGREE_SYMBOL, ValueConverter.VALUE_NOT_APPLICABLE), 1.0E-6);
	}

}
