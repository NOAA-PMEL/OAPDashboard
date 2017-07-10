/**
 * 
 */
package gov.noaa.pmel.dashboard.data.sanity;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import gov.noaa.pmel.dashboard.datatype.CastSet;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * @author kamb
 *
 */
public class CastChecker {

	private StdUserDataArray _dataset;
	private Collection<CastSet> _casts;
	
	public CastChecker(StdUserDataArray dataset) {
		_dataset = dataset;
		_casts = CastSet.extractCastSetsFrom(dataset);
	}
	
	/**
	 * Perform all cast consitency checks.  For each cast, this checks for:
	 * <ul>
	 * <li>Consistent lat/lons.
	 * <li>Consistent dates.
	 * <li>Duplicate depths.
	 * <li>Consistency of pressure vs depth.
	 * </ul>
	 */
	public void checkCastConsistency() {
		for (CastSet cast : _casts) {
			if ( cast.indeces().size() == 1 ) {
				ADCMessage msg = new ADCMessage();
				msg.setRowNumber(cast.indeces().get(0));
				msg.setDetailedComment("Cast only contains one sample: " + cast);
				msg.setSeverity(Severity.WARNING);
				_dataset.addStandardizationMessage(msg);
				continue;
			}
			checkCastLocations(_dataset, cast);
			checkCastDates(_dataset, cast);
			checkCastDepths(_dataset, cast);
			checkCastPressureDepth(_dataset, cast);
		}
	}
	private static void checkCastLocations(StdUserDataArray stda, CastSet cs) throws IllegalStateException {
		List<Integer> castRows = cs.indeces();
		Integer latCol = stda.lookForDataColumnIndex("latitude");
		Integer lonCol = stda.lookForDataColumnIndex("longitude");
		Double[] lats = stda.getSampleLatitudes();
		Double[] lons = stda.getSampleLongitudes();
		for (int idx = 1; idx < castRows.size(); idx++) {
			int prevRow = castRows.get(idx-1).intValue();
			int nextRow = castRows.get(idx).intValue();
			if ( ! ( lats[prevRow].equals(lats[nextRow]) &&
					 lons[prevRow].equals(lons[nextRow]))) {
				String genlComment = "Inconsistent locations for cast " + cs.toString() ;
				String detailMsg = genlComment + 
	                                " between samples " + prevRow + " and " + nextRow + ". " +
									" Found [" + lats[prevRow] + ", " + lons[prevRow] + "] " +
									" and [" + lats[nextRow] + ", " + lons[nextRow] + "] " ;
				ADCMessage amsg = new ADCMessage();
				amsg.setSeverity(Severity.ERROR); // XXX Really an error?
				amsg.setRowIndex(nextRow);
				amsg.setColIndex(latCol);
				amsg.setColName("latitude");
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				stda.addStandardizationMessage(amsg);
			}
//			if ( ! ( lons[prevRow].equals(lons[nextRow]))) {
//				String genlComment = "Inconsistent locations for cast " + cs.toString() ;
//				String detailMsg = genlComment + 
//	                                " between samples " + prevRow + " and " + nextRow + ". " +
//									" Found [" + lats[prevRow] + ", " + lons[prevRow] + "] " +
//									" and [" + lats[nextRow] + ", " + lons[nextRow] + "] " ;
//				ADCMessage amsg = new ADCMessage();
//				amsg.setSeverity(Severity.ERROR); // XXX Really an error?
//				amsg.setRowIndex(nextRow);
//				amsg.setColIndex(lonCol);
//				amsg.setColName("longitude");
//				amsg.setDetailedComment(detailMsg);
//				amsg.setGeneralComment(genlComment);
//				stda.addStandardizationMessage(amsg);
//			}
		}
	}
	// This is checking date based on the sampleTime double, interpreted in UTC, so we may roll over a date here
	// when in the field it was the same day.
	private static void checkCastDates(StdUserDataArray stda, CastSet cs) throws IllegalStateException {
		List<Integer> castRows = cs.indeces();
		Integer dateCol = stda.lookForDataColumnIndex("date");
		if ( dateCol == null ) {
			dateCol = stda.lookForDataColumnIndex("date_time");
		}
		Double[] times = stda.getSampleTimes();
		Calendar prevDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Calendar nextDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		for (int idx = 1; idx < castRows.size(); idx++) {
			int prevRow = castRows.get(idx-1).intValue();
			int nextRow = castRows.get(idx).intValue();
			prevDate.setTimeInMillis(times[prevRow].longValue());
			nextDate.setTimeInMillis(times[nextRow].longValue());
			int prevYearDay = prevDate.get(Calendar.DAY_OF_YEAR);
			int nextYearDay = nextDate.get(Calendar.DAY_OF_YEAR);
			if ( prevYearDay != nextYearDay ) {
				String genlComment = "Inconsistent dates for cast " + cs.toString();
				String detailMsg = genlComment +
	                                " between samples " + prevRow + " and " + nextRow + ". " +
									" Found " + prevDate.toString() +
									" and " + nextDate.toString();
				ADCMessage amsg = new ADCMessage();
				Severity severity = Math.abs(prevYearDay - nextYearDay) > 1 ? Severity.ERROR : Severity.WARNING;
				amsg.setSeverity(severity); 
				amsg.setRowIndex(nextRow);
				amsg.setColIndex(dateCol);
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				stda.addStandardizationMessage(amsg);
			}
		}
	}
	/**
	 * This assumes there is a sample_depth column.
	 * 
	 * @param stda
	 * @param cs
	 */
	private static void checkCastDepths(StdUserDataArray stda, CastSet cs) {
		List<Integer> castRows = cs.indeces();
		if ( castRows.size() < 2 ) {
			System.err.println("Cast of one. Not doing cast depth check.");
			return;
		}
		Double[] depths = stda.getSampleDepths();
		Integer depthCol = stda.lookForDataColumnIndex("sample_depth");
		if ( depthCol == null ) {
			System.err.println("No sample depth column.  Aborting cast depth checking.");
			return;
		}
						 
		Set<Double> checkDepths = new HashSet<>();
		for (int check = 0; check < castRows.size(); check++) {
			int checkRow = castRows.get(check).intValue();
			Double depth = depths[checkRow];
			if ( ! checkDepths.add(depth)) {
				String genlComment = "Duplicate depths for cast " + cs.toString();
				String detailMsg = genlComment + " at row " + checkRow; 
				ADCMessage amsg = new ADCMessage();
				amsg.setSeverity(Severity.WARNING); 
				amsg.setRowIndex(checkRow);
				amsg.setColIndex(depthCol);
				amsg.setColName("sample_depth");
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				stda.addStandardizationMessage(amsg);
			}
		}
	}
	private static void checkCastPressureDepth(StdUserDataArray stda, CastSet cs) {
		List<Integer> castRows = cs.indeces();
		if ( castRows.size() < 2 ) {
			System.err.println("Cast of one. Not doing pressure-depth consistency check.");
			return;
		}
		Integer pressureCol = stda.lookForDataColumnIndex("ctd_pressure");
		if ( pressureCol == null ) {
			System.err.println("No pressure column found. Not doing pressure-depth consistency check.");
			return;
		}
		int pressureIdx = pressureCol.intValue();
		Integer depthCol = stda.lookForDataColumnIndex("sample_depth");
		if ( depthCol == null ) {
			System.err.println("No depth column found. Not doing pressure-depth consistency check.");
			return;
		}
		Double[] depths = stda.getSampleDepths();
		double d0 = depths[castRows.get(0).intValue()].doubleValue(); 
		double d1 = getNextDepth(d0,castRows,depths);
		boolean isDesc = d1 > d0;
		double lastPressure = ((Double)stda.getStdVal(castRows.get(0), pressureIdx)).doubleValue();
		double lastDepth = depths[castRows.get(0)].doubleValue();
		String generalComment = "Pressure-Depth consitency error";
		for (int check = 1; check < castRows.size(); check++) {
			int checkRow = castRows.get(check).intValue();
			Double dPressure = (Double)stda.getStdVal(checkRow, pressureIdx);
			if ( dPressure == null ) {
				System.err.println("No pressure recorded at row " + checkRow);
				continue;
			}
			double depth = depths[checkRow].doubleValue();
			double pressure = dPressure.doubleValue();
			if ( pressure == lastPressure && depth != lastDepth ) {
				ADCMessage msg = new ADCMessage();
				msg.setSeverity(Severity.ERROR);
				msg.setRowIndex(checkRow);
				msg.setColName("ctd_pressure");
				msg.setColIndex(pressureCol);
				msg.setDepth(depth);
				msg.setGeneralComment(generalComment);
				String detailedComment = "Pressure unchanged between samples at different depths.";
				msg.setDetailedComment(detailedComment);
				stda.addStandardizationMessage(msg);
			} else if (( isDesc && 
						 pressure < lastPressure && 
						 depth > lastDepth ) ||			// Sometimes 2 samples at approx. same depth are out of order
					   ( pressure > lastPressure &&
						 depth < lastDepth )) {
				ADCMessage msg = new ADCMessage();
				msg.setSeverity(Severity.ERROR);
				msg.setRowIndex(checkRow);
				msg.setColName("ctd_pressure");
				msg.setColIndex(pressureCol);
				msg.setDepth(depth);
				msg.setGeneralComment(generalComment);
				String detailedComment = "Pressure did not " + (isDesc ? "increase" : "decrease") + 
										 " between consecutive samples.";
				msg.setDetailedComment(detailedComment);
				stda.addStandardizationMessage(msg);
			}
			lastDepth = depth;
			lastPressure = pressure;
		}
	}
	private static double getNextDepth(double d0, List<Integer> castRows, Double[] depths) {
		double next = d0;
		for (Integer row : castRows) {
			double dd = depths[row.intValue()].doubleValue();
			if ( dd != d0 ) {
				next = dd;
				break;
			}
		}
		return next;
	}
	
}
