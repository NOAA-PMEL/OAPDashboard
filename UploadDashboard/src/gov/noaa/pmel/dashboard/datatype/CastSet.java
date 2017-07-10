/**
 * 
 */
package gov.noaa.pmel.dashboard.datatype;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import gov.noaa.pmel.dashboard.data.sanity.CastChecker;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;

/**
 * @author kamb
 *
 */
public class CastSet {

	private final String _expoCode;
	private final String _castId;
	private List<Integer> _castRowIndeces;
	
	public static Collection<CastSet> extractCastSetsFrom(StdUserDataArray dataset) {
		Collection<CastSet> casts = new ArrayList<>();
		String lastId = "";
		CastSet cast = null;
		Integer dsNameCol = dataset.lookForDataColumnIndex("dataset_name");
		if ( dsNameCol == null ) {
			throw new IllegalStateException("No dataset name column found");
		}
		String expoCode = String.valueOf(dataset.getStdVal(0, dsNameCol.intValue()));
		for (int row = 0; row < dataset.getNumSamples(); row++) {
			String castId = dataset.getCastId(row);
			if ( ! lastId.equals(castId)) {
				cast = new CastSet(castId, expoCode);
				casts.add(cast);
				lastId = castId;
			}
			cast.addIndex(row);
		}
		return casts;
	}
	
	public CastSet(String castId, String expoCode) {
		this._castId = castId;
		this._expoCode = expoCode;
		this._castRowIndeces = new ArrayList<>();
	}
	
	public CastSet(String castId, String expoCode, List<Integer> castRowIndeces) {
		this._castId = castId;
		this._expoCode = expoCode;
		this._castRowIndeces = castRowIndeces;
	}
	
	public String id() {
		return _castId;
	}
	
	public String expoCode() {
		return _expoCode;
	}
	
	public List<Integer> indeces() {
		return _castRowIndeces;
	}
	
	public void addIndex(int idx) {
		_castRowIndeces.add(new Integer(idx));
	}
	public void setIndeces(List<Integer> castRowIndeces) {
		_castRowIndeces = castRowIndeces;
	}
	
	@Override
	public String toString() {
		return _expoCode + ":" + _castId;
	}
	public String debugString() {
		return _expoCode + ":" + _castId + _castRowIndeces;
	}
	
	public static void main(String[] args) {
		System.out.println(CastSet.class + " running");
		String datasetId = "PRISM022008"; // "HOGREEF64W32N";
		try {
			DashboardConfigStore dcfg = DashboardConfigStore.get(false);
			DataFileHandler dataFileHandler = dcfg.getDataFileHandler();
			KnownDataTypes knownDataTypes = dcfg.getKnownUserDataTypes();
			DashboardDatasetData ddd = dataFileHandler.getDatasetDataFromFiles(datasetId, 0, -1);
			StdUserDataArray stda = new StdUserDataArray(ddd, knownDataTypes);
			stda.checkCastConsistency();
			System.out.println(stda.getStandardizationMessages());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/*
	private static void checkCastConsistency(StdUserDataArray stda, CastSet cs) throws IllegalStateException {
		checkCastLocations(stda, cs);
		checkCastDates(stda, cs);
		checkCastDepths(stda, cs);
	}
	
	private static void checkCastLocations(StdUserDataArray stda, CastSet cs) throws IllegalStateException {
		List<Integer> castRows = cs.indeces();
		Double[] lats = stda.getSampleLatitudes();
		Double[] lons = stda.getSampleLongitudes();
		for (int idx = 1; idx < castRows.size(); idx++) {
			int prevRow = castRows.get(idx-1).intValue();
			int nextRow = castRows.get(idx).intValue();
			if ( ! ( lats[prevRow].equals(lats[nextRow]) || 
					 lons[prevRow].equals(lons[nextRow]) )) {
				throw new IllegalStateException("Inconsistent locations for cast " + cs.toString() + 
				                                " between samples " + prevRow + " and " + nextRow + ". " +
												" Found [" + lats[prevRow] + ", " + lons[prevRow] + "] " +
												" and [" + lats[nextRow] + ", " + lons[nextRow] + "] " );
			}
		}
	}
	private static void checkCastDates(StdUserDataArray stda, CastSet cs) throws IllegalStateException {
		List<Integer> castRows = cs.indeces();
		Double[] times = stda.getSampleTimes();
		Calendar prevDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Calendar nextDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		for (int idx = 1; idx < castRows.size(); idx++) {
			int prevRow = castRows.get(idx-1).intValue();
			int nextRow = castRows.get(idx).intValue();
			prevDate.setTimeInMillis(times[prevRow].longValue());
			nextDate.setTimeInMillis(times[nextRow].longValue());
			if ( prevDate.get(Calendar.DAY_OF_YEAR) != nextDate.get(Calendar.DAY_OF_YEAR)) {
				throw new IllegalStateException("Inconsistent dates for cast " + cs.toString() + 
				                                " between samples " + prevRow + " and " + nextRow + ". " +
												" Found " + prevDate.toString() +
												" and " + nextDate.toString());
			}
		}
	}
	private static void checkCastDepths(StdUserDataArray stda, CastSet cs) {
		List<Integer> castRows = cs.indeces();
		Double[] depths = stda.getSampleDepths();
		Set<Double> checkDepths = new HashSet<>();
		for (int check = 0; check < castRows.size(); check++) {
			int checkRow = castRows.get(check).intValue();
			if ( ! checkDepths.add(depths[checkRow])) {
				throw new IllegalStateException("Duplicate depths for cast " + cs.toString() + 
				                                " at row " + checkRow); 
			}
		}
	}
	*/
}
