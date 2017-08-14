/**
 * 
 */
package gov.noaa.pmel.dashboard.data.sanity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import gov.noaa.pmel.dashboard.datatype.CastSet;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * @author kamb
 *
 */
public class CastChecker {

	private static final double ABS_MAX_SPEED_knots = 20; // knots
	private static final double MAX_REASONABLE_SPEED_knots = 15; // knots
	private static final double MAX_REASONABLE_TIME_BETWEEN_CASTS_h = 7 * 24; // 1 week
	private static final double MAX_REASONABLE_TIME_BETWEEN_SAMPLES_h = 12; 
	private static final double PRESSURE_DEPTH_TOLERANCE = 0.002;
	private StdUserDataArray _dataset;
	private List<CastSet> _casts;
	
	public CastChecker(StdUserDataArray dataset) {
		_dataset = dataset;
		_casts = CastSet.extractCastSetsFrom(_dataset);
		System.out.println("casts:" + _casts);
		_casts = orderCasts(_casts);
		System.out.println("casts:" + _casts);
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
		Double lastLat = null;
		Double lastLon = null;
		Double lastTime = null;
		String lastId = null;
		for (CastSet cast : _casts) {
			System.out.println("Cast " + cast.id() + " time " + new Date((long)(cast.expectedTime()*1000)));
			if ( cast.indeces().size() == 1 ) {
				singleCastWarning(cast);
				continue;
			}
			checkBoatSpeed(_dataset, cast, lastId, lastLat, lastLon, lastTime);
			checkTimeBetweenCasts(_dataset, cast, lastId, lastTime);
			checkTimeBetweenSamples(_dataset, cast);
			checkCastLocations(_dataset, cast);
			checkCastDates(_dataset, cast);
			checkCastDepths(_dataset, cast);
			checkCastPressureDepth(_dataset, cast);
			lastLat = new Double(cast.expectedLat());
			lastLon = new Double(cast.expectedLon());
			lastTime = new Double(cast.expectedTime());
			lastId = cast.id();
		}
	}
	private static List<CastSet> orderCasts(List<CastSet>casts) {
		Comparator<CastSet> comp = new Comparator<CastSet>() {
			@Override
			public int compare(CastSet o1, CastSet o2) {
				if ( o1 == null ) { return o2 == null ? 0 : -1; }
				if ( o2 == null ) { return 1; }
				int result = Double.compare(o1.expectedTime(), o2.expectedTime());
				if ( result == 0 ) {
					result = String.valueOf(o1.id()).compareTo(String.valueOf(o2.id()));
				}
				if ( result == 0 ) {
					result = Integer.compare(o1.size(), o2.size());
				}
				return result;
			}
		};
		TreeSet<CastSet> orderedSet = new TreeSet<CastSet>(comp);
		orderedSet.addAll(casts);

		return new ArrayList<CastSet>(orderedSet);
	}
	
	private static void addTimeAndLocation(ADCMessage msg, CastSet cast) {
		Date castTime = new Date((long)(cast.expectedTime() * 1000));
		msg.setTimestamp(DashboardServerUtils.formatUTC(castTime));
		msg.setLatitude(cast.expectedLat());
		msg.setLongitude(cast.expectedLon());
	}
	private void addTimeAndLocation(ADCMessage msg, CastSet cast, int castRowIndex) {
		int datasetRow = cast.indeces().get(castRowIndex);
		Double rowTime = _dataset.getSampleTimes()[datasetRow];
		Date castTime = new Date((long)(rowTime * 1000));
		msg.setTimestamp(DashboardServerUtils.formatUTC(castTime));
		Double rowLat = _dataset.getSampleLatitudes()[datasetRow];
		Double rowLon = _dataset.getSampleLongitudes()[datasetRow];
		msg.setLatitude(rowLat);
		msg.setLongitude(rowLon);
	}
		
//	private static void checkCastOrdering(List<CastSet>casts) {
//		if ( casts.size() < 2 ) { return; }
//		double lastT = casts.get(0).expectedTime();
//		CastSet last = casts.get(1);
//		double deltaT = last.expectedTime() - lastT;
//		boolean asc = deltaT > 0;
//		for (int i = 2; i < casts.size(); i++) {
//			CastSet cur = casts.get(i);
//			deltaT = cur.expectedTime() - last.expectedTime();
//			if (( deltaT > 0 ) != asc ) {
//				System.err.println("Inconsistent time ordering between casts " + last.id() + " and " + cur.id());
//				asc = !asc;
//			}
//			last = cur;
//		}
//	}

	private void checkTimeBetweenCasts(StdUserDataArray stda, CastSet cast, String lastId, Double lastTime) {
		if ( lastTime == null || lastId == null ) {
			return;
		}
		Integer row = cast.indeces().get(0);
		double thisTime = cast.expectedTime();
		if ( thisTime == DashboardUtils.FP_MISSING_VALUE.doubleValue()) {
			// TODO: Warn
			ADCMessage msg = new ADCMessage();
			msg.setSeverity(Severity.ERROR);
			msg.setRowIndex(row);
			msg.setGeneralComment("Missing cast time");
			msg.setDetailedComment("Cast " + cast.id() + " missing expected time value.");
			addTimeAndLocation(msg, cast);
			stda.addStandardizationMessage(msg);
			return;
		}
		double deltaTs = (thisTime - lastTime.doubleValue());
		double deltaTh = (deltaTs / 3600);
		if ( Math.abs(deltaTh) > MAX_REASONABLE_TIME_BETWEEN_CASTS_h ) {
			// TODO: Warn
			ADCMessage msg = new ADCMessage();
			msg.setRowIndex(row);
			msg.setSeverity(Severity.ERROR);
			msg.setGeneralComment("Excessive time interval");
			msg.setDetailedComment("Excessive apparent time of " + deltaTh + " hours between casts " + cast.id() + " and " + lastId);
			addTimeAndLocation(msg, cast);
			stda.addStandardizationMessage(msg);
		}
		if ( deltaTh < 0 ) {
			System.err.println("Apparent negative time of " + deltaTh + " hours between casts " + cast.id() + " and " + lastId + " at row " + (row.intValue()+1));
		}
	}
	
	private void checkTimeBetweenSamples(StdUserDataArray stda, CastSet cast) {
		if ( cast.size() < 2 ) { return; }
		Double[] times = stda.getSampleTimes();
		for (int i = 1; i < cast.size(); i++) {
			Integer row = cast.indeces().get(i);
			double lastTime = times[cast.indeces().get(i-1).intValue()].doubleValue();
			double thisTime = times[row.intValue()].doubleValue();
			double deltaTs = thisTime - lastTime;
			double deltaTh = deltaTs / 3600;
			if ( Math.abs(deltaTh) > MAX_REASONABLE_TIME_BETWEEN_SAMPLES_h) {
				ADCMessage msg = new ADCMessage();
				msg.setRowIndex(row);
				msg.setSeverity(Severity.ERROR);
				msg.setGeneralComment("Excessive time interval");
				msg.setDetailedComment("Excessive apparent time of " + deltaTh + " hours between samples");
				addTimeAndLocation(msg, cast, i);
				stda.addStandardizationMessage(msg);
			}
			if ( deltaTh < 0 ) {
				System.err.println("Negative time of " + deltaTh + " hours between samples of " + cast + " at row " + (row.intValue()+1));
			}
		}
	}

	private static void checkBoatSpeed(StdUserDataArray stda, CastSet cast, 
	                                   String lastId, Double lastLat, Double lastLon, Double lastTime) {
		if ( lastId == null || lastLat == null || lastLon == null || lastTime == null ) {
			return;
		}
		double thisLat = cast.expectedLat();
		double thisLon = cast.expectedLon();
		double thisTime = cast.expectedTime();
		Integer row = cast.indeces().get(0);
		if ( thisLat == DashboardUtils.FP_MISSING_VALUE.doubleValue() || 
			 thisLon == DashboardUtils.FP_MISSING_VALUE.doubleValue() ||
			 thisTime == DashboardUtils.FP_MISSING_VALUE.doubleValue() ) {
			System.err.println("Missing expected value for " + cast);
			// TODO: log Error msg
			ADCMessage msg = new ADCMessage();
			msg.setSeverity(Severity.ERROR);
			msg.setRowIndex(row);
			msg.setGeneralComment("Missing cast location");
			msg.setDetailedComment("Cast " + cast.id() + " missing expected location or time value.");
			addTimeAndLocation(msg, cast);
			stda.addStandardizationMessage(msg);
			return;
		}
		double deltaDistance = DashboardServerUtils.greatCircleDistance_NM(thisLat, thisLon, 
				                                                     lastLat.doubleValue(), lastLon.doubleValue());
		double deltaT_h = (thisTime - lastTime.doubleValue()) / 3600;
		double knots = deltaDistance / Math.abs(deltaT_h);
		System.out.println(String.format("  [%s] distance: %.2f nm, boat speed: %.2f", cast.id(), deltaDistance, knots));
		if ( knots > MAX_REASONABLE_SPEED_knots ) {
			ADCMessage msg = new ADCMessage();
			msg.setRowIndex(row);
			msg.setSeverity(knots > ABS_MAX_SPEED_knots ? Severity.ERROR : Severity.WARNING);
			msg.setGeneralComment("Unreasonable boat speed");
			msg.setDetailedComment("Apparent boat speed of " + knots + " knots between casts " + cast.id() + " and " + lastId);
			addTimeAndLocation(msg, cast);
			stda.addStandardizationMessage(msg);
		}
	}

	private void singleCastWarning(CastSet cast) {
		ADCMessage msg = new ADCMessage();
		Integer row = cast.indeces().get(0);
		msg.setRowIndex(row);
		String castId = cast.id();
		String genlComment;
		String detailComment;
		Severity severity;
		if ( castId == null || castId.trim().length() == 0 || "null".equalsIgnoreCase(castId)) {
			genlComment = "Missing cast ID";
			detailComment = genlComment + " at row " + row;
			severity = Severity.ERROR;
		} else {
			genlComment = "Cast only contains one sample";
			detailComment = genlComment +  " for cast " + cast + " at row " + row;
			severity = Severity.WARNING;
		}
		msg.setGeneralComment(genlComment);
		msg.setDetailedComment(detailComment);
		msg.setSeverity(severity);
		addTimeAndLocation(msg, cast);
		_dataset.addStandardizationMessage(msg);
	}

	private void checkCastLocations(StdUserDataArray stda, CastSet cs) throws IllegalStateException {
		List<Integer> castRows = cs.indeces();
		Integer latCol = stda.lookForDataColumnIndex("latitude");
		Integer lonCol = stda.lookForDataColumnIndex("longitude");
		Double[] lats = stda.getSampleLatitudes();
		Double[] lons = stda.getSampleLongitudes();
		for (int idx = 1; idx < castRows.size(); idx++) {
			int prevRowIdx = castRows.get(idx-1).intValue();
			int nextRowIdx = castRows.get(idx).intValue();
			if ( ! ( lats[prevRowIdx].equals(lats[nextRowIdx]) )) {
				int prevRow = prevRowIdx+1;
				int nextRow = nextRowIdx+1;
				String genlComment = "Inconsistent cast locations.";
				String detailMsg = "Inconsistent latitudes for cast " + cs.toString() +
	                                " between rows " + prevRow + " and " + nextRow + ". " +
									" Found [" + lats[prevRowIdx] + ", " + lons[prevRowIdx] + "] " +
									" and [" + lats[nextRowIdx] + ", " + lons[nextRowIdx] + "] " ;
				ADCMessage amsg = new ADCMessage();
				amsg.setSeverity(Severity.ERROR); 
				amsg.setRowIndex(nextRowIdx);
				amsg.setColIndex(latCol);
				amsg.setColName("latitude");
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				addTimeAndLocation(amsg, cs, idx);
				stda.addStandardizationMessage(amsg);
			}
			if ( ! ( lons[prevRowIdx].equals(lons[nextRowIdx]))) {
				int prevRow = prevRowIdx+1;
				int nextRow = nextRowIdx+1;
				String genlComment = "Inconsistent cast locations.";
				String detailMsg = "Inconsistent longitudes for cast " + cs.toString() +
	                                " between rows " + prevRow + " and " + nextRow + ". " +
									" Found [" + lats[prevRowIdx] + ", " + lons[prevRowIdx] + "] " +
									" and [" + lats[nextRowIdx] + ", " + lons[nextRowIdx] + "] " ;
				ADCMessage amsg = new ADCMessage();
				amsg.setSeverity(Severity.ERROR);
				amsg.setRowIndex(nextRowIdx);
				amsg.setColIndex(lonCol);
				amsg.setColName("longitude");
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				addTimeAndLocation(amsg, cs, idx);
				stda.addStandardizationMessage(amsg);
			}
		}
	}
	// This is checking date based on the sampleTime double, interpreted in UTC, so we may roll over a date here
	// when in the field it was the same day.
	private void checkCastDates(StdUserDataArray stda, CastSet cs) throws IllegalStateException {
		List<Integer> castRows = cs.indeces();
		Integer dateCol = stda.lookForDataColumnIndex("date");
		if ( dateCol == null ) {
			dateCol = stda.lookForDataColumnIndex("date_time");
		}
		Double[] times = stda.getSampleTimes();
		Calendar prevDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Calendar nextDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		for (int idx = 1; idx < castRows.size(); idx++) {
			int prevRowIdx = castRows.get(idx-1).intValue();
			int nextRowIdx = castRows.get(idx).intValue();
			prevDate.setTimeInMillis(times[prevRowIdx].longValue());
			nextDate.setTimeInMillis(times[nextRowIdx].longValue());
			int prevYearDay = prevDate.get(Calendar.DAY_OF_YEAR);
			int nextYearDay = nextDate.get(Calendar.DAY_OF_YEAR);
			if ( prevYearDay != nextYearDay ) {
				int prevRow = prevRowIdx+1;
				int nextRow = nextRowIdx+1;
				String genlComment = "Inconsistent cast dates.";
				String detailMsg = "Inconsistent dates for cast " + cs.toString() +
	                                " between rows " + prevRow + " and " + nextRow + ". " +
									" Found " + prevDate.toString() +
									" and " + nextDate.toString();
				ADCMessage amsg = new ADCMessage();
				Severity severity = Math.abs(prevYearDay - nextYearDay) > 1 ? Severity.ERROR : Severity.WARNING;
				amsg.setSeverity(severity); 
				amsg.setRowIndex(nextRowIdx);
				amsg.setColIndex(dateCol);
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				addTimeAndLocation(amsg, cs, idx);
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
	private void checkCastDepths(StdUserDataArray stda, CastSet cs) {
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
				String genlComment = "Duplicate depths in cast.";
				String detailMsg =  "Duplicate depths in cast " + cs.toString() + " at row " + (checkRow + 1); 
				ADCMessage amsg = new ADCMessage();
				amsg.setSeverity(Severity.WARNING); 
				amsg.setRowIndex(checkRow);
				amsg.setColIndex(depthCol);
				amsg.setColName("sample_depth");
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				addTimeAndLocation(amsg, cs, check);
				stda.addStandardizationMessage(amsg);
			}
		}
	}
	private void checkCastPressureDepth(StdUserDataArray stda, CastSet cs) {
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
			if ( pressure == lastPressure && Math.abs(depth-lastDepth) > PRESSURE_DEPTH_TOLERANCE) {
				ADCMessage msg = new ADCMessage();
				msg.setSeverity(Severity.ERROR);
				msg.setRowIndex(checkRow);
				msg.setColName("ctd_pressure");
				msg.setColIndex(pressureCol);
				msg.setDepth(depth);
				msg.setGeneralComment(generalComment);
				addTimeAndLocation(msg, cs, check);
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
				addTimeAndLocation(msg, cs, check);
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
