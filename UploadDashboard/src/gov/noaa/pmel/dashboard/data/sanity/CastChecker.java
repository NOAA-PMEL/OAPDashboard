/**
 * 
 */
package gov.noaa.pmel.dashboard.data.sanity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.client.UploadDashboard;
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

	private static Logger logger = LogManager.getLogger(CastChecker.class.getName());
	
	private static final double ABS_MAX_SPEED_knots = 50; // knots
	private static final double MAX_REASONABLE_SPEED_knots = 25; // knots
	private static final double MAX_REASONABLE_TIME_BETWEEN_CASTS_h = 7 * 24; // 1 week
	private static final double MAX_REASONABLE_TIME_BETWEEN_SAMPLES_h = 12; 
	private static final double PRESSURE_DEPTH_TOLERANCE = 0.002;
	private static final double PRESSURE_DELTA_TOLERANCE = 0.02;
	private static final double DEPTH_DELTA_TOLERANCE = 0.02;
	private StdUserDataArray _dataset;
	private List<CastSet> _casts;
	
	public CastChecker(StdUserDataArray dataset) {
	    this(dataset, true);
	}
	public CastChecker(StdUserDataArray dataset, boolean reorderCasts) {
		_dataset = dataset;
		_casts = CastSet.extractCastSetsFrom(_dataset);
		logger.debug("unordered casts:" + _casts);
        if ( reorderCasts ) {
    		_casts = orderCasts(_casts);
    		logger.debug("ordered casts:" + _casts);
        }
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
//		Double lastLat = null;
//		Double lastLon = null;
//		Double lastTime = null;
//		String lastId = null;
        CastSet lastCast = null;
		checkForDuplicateCastIds(_dataset, _casts);
		for (CastSet cast : _casts) {
			logger.debug("Cast " + cast.id() + " time " + new Date((long)(cast.expectedTime()*1000)));
			if ( cast.indeces().size() == 1 ) {
				singleCastWarning(cast);
				continue;
			}
			checkBoatSpeed(_dataset, cast, lastCast); // lastId, lastLat, lastLon, lastTime);
			checkTimeBetweenCasts(_dataset, cast, lastCast); // lastId, lastTime);
			checkTimeBetweenSamples(_dataset, cast);
			checkCastLocations(_dataset, cast);
			checkCastDates(_dataset, cast);
			checkCastDepths(_dataset, cast);
//			checkCastPressureDepth(_dataset, cast);
            // happens in checkDepths
//            checkCastBottles(_dataset, cast);
            lastCast = cast;
		}
	}
	private static void checkForDuplicateCastIds(StdUserDataArray dataset, List<CastSet> casts) {
	    Set<String> ids = new TreeSet<>();
	    for (CastSet cs : casts) {
	        String cid = cs.id();
	        if (ids.contains(cid)) {
	            addDupIdMsg(dataset, cid);
	        } else {
	            ids.add(cid);
	        }
	    }
    }

    private static void addDupIdMsg(StdUserDataArray dataset, String cid) {
		ADCMessage msg = new ADCMessage();
		msg.setSeverity(Severity.ERROR);
		msg.setGeneralComment("Duplicate cast ID");
		msg.setDetailedComment("Cast ID " + cid + " occurs more than once.");
		dataset.addStandardizationMessage(msg);
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
		
	private static void checkTimeBetweenCasts(StdUserDataArray stda, CastSet cast, CastSet lastCast) {
        if ( lastCast == null ) { return; }
        if ( ! lastCast.datasetId().equals(cast.datasetId())) {
            return;
        }
        Double lastTime = new Double(lastCast.expectedTime());
        String lastId = lastCast.id();

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
			msg.setSeverity(Severity.WARNING);
			msg.setGeneralComment("Excessive time interval");
			msg.setDetailedComment("Excessive apparent time of " + deltaTh + " hours between casts " + cast.id() + " and " + lastId);
			addTimeAndLocation(msg, cast);
			stda.addStandardizationMessage(msg);
		}
		if ( deltaTh < 0 ) {
			logger.info("Apparent negative time of " + deltaTh + " hours between casts " + cast.id() + " and " + lastId + " at row " + (row.intValue()+1));
		}
	}
	
	private void checkTimeBetweenSamples(StdUserDataArray stda, CastSet cast) {
		if ( cast.size() < 2 ) { return; }
		Double[] times = stda.getSampleTimes();
        Integer timeColIdx = stda.lookForDataColumnIndex(DashboardUtils.TIME_OF_DAY_VARNAME);
		for (int i = 1; i < cast.size(); i++) {
			Integer row = cast.indeces().get(i);
			double lastTime = times[cast.indeces().get(i-1).intValue()].doubleValue();
			double thisTime = times[row.intValue()].doubleValue();
			double deltaTs = thisTime - lastTime;
			double deltaTh = deltaTs / 3600;
			if ( deltaTh > MAX_REASONABLE_TIME_BETWEEN_SAMPLES_h) {
				ADCMessage msg = new ADCMessage();
				msg.setRowIndex(row);
                if ( timeColIdx != null ) {
                    msg.setColIndex(timeColIdx.intValue());
                }
				msg.setSeverity(Severity.WARNING);
				msg.setGeneralComment("Excessive time interval");
				msg.setDetailedComment("Excessive apparent time of " + deltaTh + " hours between samples");
				addTimeAndLocation(msg, cast, i);
				stda.addStandardizationMessage(msg);
			}
			if ( deltaTh < 0 ) {
				ADCMessage msg = new ADCMessage();
				msg.setRowIndex(row);
                if ( timeColIdx != null ) {
                    msg.setColIndex(timeColIdx.intValue());
                }
				msg.setSeverity(Severity.WARNING);
				msg.setGeneralComment("Negative time interval");
				msg.setDetailedComment("Negative apparent time of " + deltaTh + " hours between samples");
				addTimeAndLocation(msg, cast, i);
				stda.addStandardizationMessage(msg);
				logger.info("Negative time of " + deltaTh + " hours between samples of " + cast + " at row " + (row.intValue()+1));
			}
		}
	}

	private static void checkBoatSpeed(StdUserDataArray stda, CastSet cast, CastSet lastCast) {
//	                                   String lastId, Double lastLat, Double lastLon, Double lastTime) {
        if ( lastCast == null ) { return; }
		Double lastLat = new Double(lastCast.expectedLat());
        Double lastLon = new Double(lastCast.expectedLon());
        Double lastTime = new Double(lastCast.expectedTime());
        String lastId = lastCast.id();

		double thisLat = cast.expectedLat();
		double thisLon = cast.expectedLon();
		double thisTime = cast.expectedTime();
		Integer row = cast.indeces().get(0);
		if ( thisLat == DashboardUtils.FP_MISSING_VALUE.doubleValue() || 
			 thisLon == DashboardUtils.FP_MISSING_VALUE.doubleValue() ||
			 thisTime == DashboardUtils.FP_MISSING_VALUE.doubleValue() ) {
			logger.warn("Missing expected value for " + cast);
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
		double deltaT = (thisTime - lastTime.doubleValue());
		double deltaT_h = deltaT / 3600;
		double knots = deltaDistance / Math.abs(deltaT_h);
		logger.trace(String.format("  [%s] distance: %.2f nm, boat speed: %.2f", cast.id(), deltaDistance, knots));
		if ( knots > MAX_REASONABLE_SPEED_knots ) {
    		logger.info(String.format("  [%s] distance: %.2f nm, boat speed: %.2f", cast.id(), deltaDistance, knots));
			ADCMessage msg = new ADCMessage();
			msg.setRowIndex(row);
            Severity severus = Double.isInfinite(knots) ? 
                                  Severity.WARNING :
                                  knots > ABS_MAX_SPEED_knots ? 
                                        Severity.ERROR : 
                                        Severity.WARNING;
			msg.setSeverity(severus);
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
			double thisLat = lats[prevRowIdx];
			double thisLon = lons[prevRowIdx];
			if ( Double.isInfinite(thisLat) || Double.isNaN(thisLat) ||
			     thisLat <= -90 || thisLat >= 90 ) {
				int thisRow = nextRowIdx;
				String genlComment = "Bad cast latitude.";
				String detailMsg = "Invalid latitude for cast " + cs.toString() +
	                                " at row " + thisRow + ". " +
									" Found [" + thisLat + "] ";
				ADCMessage amsg = new ADCMessage();
				amsg.setSeverity(Severity.ERROR); 
				amsg.setRowIndex(prevRowIdx);
				amsg.setColIndex(latCol);
				amsg.setColName(stda.getUserColumnNames()[latCol.intValue()]);
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				addTimeAndLocation(amsg, cs, idx);
				stda.addStandardizationMessage(amsg);
			}
			if ( Double.isInfinite(thisLon) || Double.isNaN(thisLon) ||
			     thisLon <= -180 || thisLon >= 180 ) {
				int thisRow = nextRowIdx;
				String genlComment = "Bad cast longitude.";
				String detailMsg = "Invalid longitude for cast " + cs.toString() +
	                                " at row " + thisRow + ". " +
									" Found [" + thisLon + "] ";
				ADCMessage amsg = new ADCMessage();
				amsg.setSeverity(Severity.ERROR); 
				amsg.setRowIndex(prevRowIdx);
				amsg.setColIndex(lonCol);
				amsg.setColName(stda.getUserColumnNames()[lonCol.intValue()]);
				amsg.setDetailedComment(detailMsg);
				amsg.setGeneralComment(genlComment);
				addTimeAndLocation(amsg, cs, idx);
				stda.addStandardizationMessage(amsg);
			}
			if ( ! ( lats[prevRowIdx].equals(lats[nextRowIdx]) )) {
				int prevRow = prevRowIdx+1;
				int nextRow = nextRowIdx+1;
				String genlComment = "Inconsistent cast locations.";
				String detailMsg = "Inconsistent latitudes for cast " + cs.toString() +
	                                " between rows " + prevRow + " and " + nextRow + ". " +
									" Found [" + lats[prevRowIdx] + ", " + lons[prevRowIdx] + "] " +
									" and [" + lats[nextRowIdx] + ", " + lons[nextRowIdx] + "] " ;
				ADCMessage amsg = new ADCMessage();
				Severity severity = Math.abs(lats[prevRowIdx].doubleValue() - lats[nextRowIdx].doubleValue()) > 0.1 ? 
										Severity.ERROR : 
										Severity.WARNING;
				amsg.setSeverity(severity); 
				amsg.setRowIndex(nextRowIdx);
				amsg.setColIndex(latCol);
				amsg.setColName(stda.getUserColumnNames()[latCol.intValue()]);
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
				Severity severity = Math.abs(lons[prevRowIdx].doubleValue() - lons[nextRowIdx].doubleValue()) > 0.1 ? 
										Severity.ERROR : 
										Severity.WARNING;
				amsg.setSeverity(severity); 
				amsg.setRowIndex(nextRowIdx);
				amsg.setColIndex(lonCol);
				amsg.setColName(stda.getUserColumnNames()[lonCol.intValue()]);
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
			logger.info("Cast of one. Not doing cast depth check.");
			return;
		}
        String columnName = DashboardUtils.SAMPLE_DEPTH_VARNAME;
        Double[] depthOrPressures = null;
		Integer depthCol = stda.lookForDataColumnIndex(columnName);
		if ( depthCol == null ) {
			logger.info("No sample depth column.  Checking for water pressure column.");
            columnName = DashboardUtils.CTD_PRESSURE_VARNAME;
    		depthCol = stda.lookForDataColumnIndex(columnName);
            if ( depthCol == null ) {
                // The critical error of their being no pressure or depth column is added in ProfileDatasetChecker.checkRequiredColumns()
    			logger.warn("No sample depth or pressure column.  Aborting cast depth checking.");
    			return;
            } else {
                depthOrPressures = stda.getStdValuesAsDouble(depthCol.intValue());
            }
		} else {
            depthOrPressures = stda.getSampleDepths();
		}
        columnName = stda.getUserColumnNames()[depthCol.intValue()];
        Integer bottleIdx = stda.lookForDataColumnIndex("niskin");
        Object[] bottles = bottleIdx != null ? stda.getStdValues(bottleIdx.intValue()) : null ;
						 
		Set<Double> checkDepths = new HashSet<>();
		Set<Object> checkBottles = new HashSet<>();
		for (int check = 0; check < castRows.size(); check++) {
			int checkRow = castRows.get(check).intValue();
			Double depth = depthOrPressures[checkRow];
            Object bottle = bottles != null ? bottles[checkRow] : null;
			if ( depth != null &&    // missing depths are reported in StdUserDataArray.checkMissingLatLonDepthTime() // ! XXX
			     ! checkDepths.add(depth)) {
                if ( bottle == null ) {
    				String genlComment = "Duplicate depths in cast.";
    				String detailMsg =  "Duplicate depths in cast " + cs.toString() + " at row " + (checkRow + 1); 
    				ADCMessage amsg = new ADCMessage();
    				amsg.setSeverity(Severity.WARNING); 
    				amsg.setRowIndex(checkRow);
    				amsg.setColIndex(depthCol);
    				amsg.setColName(columnName);
    				amsg.setDetailedComment(detailMsg);
    				amsg.setGeneralComment(genlComment);
    				addTimeAndLocation(amsg, cs, check);
    				stda.addStandardizationMessage(amsg);
                } else if ( ! checkBottles.add(bottle)) {
    				String genlComment = "Duplicate bottle in cast.";
    				String detailMsg =  "Duplicate bottle " + bottle + " in cast " + cs.toString() + " at row " + (checkRow + 1); 
    				ADCMessage amsg = new ADCMessage();
    				amsg.setSeverity(Severity.ERROR); 
    				amsg.setRowIndex(checkRow);
    				amsg.setColIndex(bottleIdx);
    				amsg.setColName(stda.getUserColumnNames()[bottleIdx.intValue()]);
    				amsg.setDetailedComment(detailMsg);
    				amsg.setGeneralComment(genlComment);
    				addTimeAndLocation(amsg, cs, check);
    				stda.addStandardizationMessage(amsg);
                }
			} else if ( bottle != null ) {
			    checkBottles.add(bottle);
			}
		}
	}
	private void checkCastPressureDepth(StdUserDataArray stda, CastSet cs) {
		List<Integer> castRows = cs.indeces();
		if ( castRows.size() < 2 ) {
			logger.info("Cast of one. Not doing pressure-depth consistency check.");
			return;
		}
        int start = castRows.get(0).intValue();
        int last = castRows.get(castRows.size()-1).intValue() + 1;
		Integer pressureCol = stda.lookForDataColumnIndex(DashboardUtils.CTD_PRESSURE_VARNAME);
		if ( pressureCol == null ) {
			logger.warn("No pressure column found. Not doing pressure-depth consistency check.");
			return;
		}
		int pressureIdx = pressureCol.intValue();
		Integer depthCol = stda.lookForDataColumnIndex(DashboardUtils.SAMPLE_DEPTH_VARNAME);
		if ( depthCol == null ) {
			logger.warn("No depth column found. Not doing pressure-depth consistency check.");
			return;
		}
		Double[] depths = stda.getSampleDepths();
		if ( hasMissingValues((Double[])Arrays.asList(depths).subList(start, last).toArray(new Double[last-start]))) {
		    logger.warn("Cast has missing depth values.  No pressure-depth correlation done.");
		    return;
		}
        Double[] pressures = stda.getStdValuesAsDouble(pressureIdx);
		if ( hasMissingValues((Double[])Arrays.asList(pressures).subList(start, last).toArray(new Double[last-start]))) {
		    logger.warn("Cast has missing pressure values.  No pressure-depth correlation done.");
		    return;
		}
		double d0 = pressures[castRows.get(0).intValue()].doubleValue(); 
		double d1 = pressures[castRows.get(castRows.size()-1).intValue()].doubleValue(); 
		boolean isDescendingProile = d1 > d0;
        double lastPressure = pressures[castRows.get(0)].doubleValue();
		double lastDepth = depths[castRows.get(0)].doubleValue();
		String generalComment = "Pressure-Depth consitency error";
		for (int check = 1; check < castRows.size(); check++) {
			int checkRow = castRows.get(check).intValue();
			Double dPressure = pressures[checkRow];
			if ( dPressure == null ) {
				logger.info("No pressure recorded at row " + checkRow);
				continue;
			}
			double depth = depths[checkRow].doubleValue();
			double pressure = dPressure.doubleValue();
            double deltaD = Math.abs(lastDepth-depth) / depth;
            double deltaP = Math.abs(lastPressure-pressure) / pressure;
			if ( pressure == lastPressure && Math.abs(depth-lastDepth) > PRESSURE_DEPTH_TOLERANCE) {
				ADCMessage msg = new ADCMessage();
				msg.setSeverity(Severity.ERROR);
				msg.setRowIndex(checkRow);
				msg.setColName(stda.getUserColumnNames()[pressureIdx]);
				msg.setColIndex(pressureCol);
				msg.setDepth(depth);
				msg.setGeneralComment(generalComment);
				addTimeAndLocation(msg, cs, check);
				String detailedComment = "Pressure unchanged between samples at different depths.";
                logger.info("Cast " + cs.id() + "@"+checkRow+":"+ detailedComment);
				msg.setDetailedComment(detailedComment);
				stda.addStandardizationMessage(msg);
			} else if (( isDescendingProile && // Sometimes 2 samples at approx. same depth are out of order
					     (( pressure < lastPressure && deltaP > PRESSURE_DELTA_TOLERANCE ) || 
					      ( depth < lastDepth && deltaD > DEPTH_DELTA_TOLERANCE ))) ||	
					   ( !isDescendingProile &&
					     (( pressure > lastPressure && deltaP > PRESSURE_DELTA_TOLERANCE ) ||
						  ( depth > lastDepth && deltaD > DEPTH_DELTA_TOLERANCE )))) {
				ADCMessage msg = new ADCMessage();
				msg.setSeverity(Severity.ERROR);
				msg.setRowIndex(checkRow);
				msg.setColName(stda.getUserColumnNames()[pressureIdx]);
				msg.setColIndex(pressureCol);
				msg.setDepth(depth);
				msg.setGeneralComment(generalComment);
				String detailedComment = "Pressure did not " + (isDescendingProile ? "increase" : "decrease") + 
										 " between consecutive samples.";
                logger.info("Cast " + cs.id() + "@"+checkRow+":"+ detailedComment);
				msg.setDetailedComment(detailedComment);
				addTimeAndLocation(msg, cs, check);
				stda.addStandardizationMessage(msg);
			}
			lastDepth = depth;
			lastPressure = pressure;
		}
	}
//    // happens in checkCastDepths
//    private static void checkCastBottles(StdUserDataArray stda, CastSet cs) {
//        Map<String, Integer> bottles = new HashMap<String, Integer>();
//        Map<String, Integer> samples = new HashMap<String, Integer>();
//        Integer bottleIdx = stda.lookForDataColumnIndex(DashboardUtils.NISKIN_VARNAME);
//        Integer sampleIdx = stda.lookForDataColumnIndex(DashboardUtils.SAMPLE_ID_VARNAME);
//        for (Integer row : cs.indeces()) {
//            if ( bottleIdx != null ) {
//                String bottle = (String)stda.getStdVal(row.intValue(), bottleIdx.intValue());
//                Integer already = bottles.put(bottle, row);
//                if ( already != null ) {
//                    ADCMessage msg = stda.messageFor(Severity.ERROR, row, bottleIdx, 
//                                                     "Duplicate bottle in cast", 
//                                                     "Duplicate bottle " + bottle + " in cast " + 
//                                                     cs.id() + " at row " + (row.intValue()+1));
//                    stda.addStandardizationMessage(msg);
//                }
//            }
//            if ( sampleIdx != null ) {
//                String sample = (String)stda.getStdVal(row.intValue(), sampleIdx.intValue());
//                Integer already = samples.put(sample, row);
//                if ( already != null ) {
//                    ADCMessage msg = stda.messageFor(Severity.ERROR, row, sampleIdx, 
//                                                     "Duplicate sample in cast", 
//                                                     "Duplicate sample " + sample + " in cast " + 
//                                                     cs.id() + " at row " + (row.intValue()+1));
//                    stda.addStandardizationMessage(msg);
//                }
//            }
//        }
//    }
        
	private boolean hasMissingValues(Double[] depths) {
	    for (Double d : depths) {
	        if ( d == null ) {
	            return true;
	        }
	    }
        return false;
    }

    private static double getN_extDepth(double d0, List<Integer> castRows, Double[] depths) {
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
