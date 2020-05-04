/**
 * 
 */
package gov.noaa.pmel.dashboard.datatype;

import java.util.ArrayList;
import java.util.List;

import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

/**
 * @author kamb
 *
 */
public class CastSet implements Comparable<CastSet> {

	private final String _datasetId;
	private final String _castId;
	private List<Integer> _castRowIndeces;
	private double _expectedLat = DashboardUtils.FP_MISSING_VALUE.doubleValue();
	private double _expectedLon = DashboardUtils.FP_MISSING_VALUE.doubleValue();
	private double _expectedTime = DashboardUtils.FP_MISSING_VALUE.doubleValue();
	
	public static List<CastSet> extractCastSetsFrom(StdUserDataArray dataset) {
		List<CastSet> casts = new ArrayList<>();
		String lastId = "";
		CastSet cast = null;
		Double[] lats = dataset.getSampleLatitudes();
		Double[] lons = dataset.getSampleLongitudes();
		Double[] times = dataset.getSampleTimes();
//		Integer dsNameCol = dataset.lookForDataColumnIndex(DashboardUtils.DATASET_IDENTIFIER_VARNAME);
//		if ( dsNameCol == null ) {
//            System.out.println("No dataset column specified.  Using specified datasetId:"+ dataset.getDatasetId());
////			throw new IllegalStateException("No dataset name column found");
//		}
		for (int row = 0; row < dataset.getNumSamples(); row++) {
			String castId = dataset.getDatasetStationCastIdentifier(row);
    		String datasetId = dataset.getDatasetId(row);
			if ( ! lastId.equals(castId)) {
				cast = new CastSet(castId, datasetId);
				casts.add(cast);
				lastId = castId;
				if ( lats[row] != null ) {
					cast._expectedLat = lats[row].doubleValue();
				} else {
					// TODO: warn
				}
				if ( lons[row] != null ) {
					cast._expectedLon = lons[row].doubleValue();
				} else {
					// TODO: warn
				}
				if ( times[row] != null ) {
					cast._expectedTime = times[row].doubleValue();
				} else {
					// TODO: warn
				}
			}
			cast.addIndex(row);
		}
		return casts;
	}
	
	public CastSet(String castId, String datasetId) {
        this(castId, datasetId, new ArrayList<>());
	}
	
	public CastSet(String castId, String datasetId, List<Integer> castRowIndeces) {
		this._castId = castId;
		this._datasetId = datasetId;
		this._castRowIndeces = castRowIndeces;
	}
	
	public int size() {
		return _castRowIndeces.size();
	}
	
	public String id() {
		return _castId;
	}
	
	public String datasetId() {
		return _datasetId;
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
	
	public double expectedLat() {
		return _expectedLat;
	}
	public double expectedLon() {
		return _expectedLon;
	}
	public double expectedTime() {
		return _expectedTime;
	}
	
	@Override
	public String toString() {
		return _castId;
	}
	public String debugString() {
		return _castId + _castRowIndeces;
	}

	@Override
	public int compareTo(CastSet o) {
		int result = o == null ? 1 : Double.compare(this._expectedTime, o._expectedTime);
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_datasetId == null) ? 0 : _datasetId.hashCode());
		result = prime * result + ((_castId == null) ? 0 : _castId.hashCode());
		result = prime * result + ((_castRowIndeces == null) ? 0 : _castRowIndeces.hashCode());
		long temp;
		temp = Double.doubleToLongBits(_expectedLat);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_expectedLon);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_expectedTime);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((_datasetId == null) ? 0 : _datasetId.hashCode());
		return result;
	}

    private static boolean strEquals(String s1, String s2) {
        if ( s1 == s2 ) { return true; }
        if ( s1 == null ) { return s2 == null; }
        return s1.equals(s2);
    }
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CastSet other = (CastSet) obj;
		if (_datasetId == null) {
			if (other._datasetId != null)
				return false;
		} else if (!_datasetId.equals(other._datasetId))
			return false;
		if (_castId == null) {
			if (other._castId != null)
				return false;
		} else if (!_castId.equals(other._castId))
			return false;
		if (_castRowIndeces == null) {
			if (other._castRowIndeces != null)
				return false;
		} else if (!_castRowIndeces.equals(other._castRowIndeces))
			return false;
		if (Double.doubleToLongBits(_expectedLat) != Double.doubleToLongBits(other._expectedLat))
			return false;
		if (Double.doubleToLongBits(_expectedLon) != Double.doubleToLongBits(other._expectedLon))
			return false;
		if (Double.doubleToLongBits(_expectedTime) != Double.doubleToLongBits(other._expectedTime))
			return false;
		if (_datasetId == null) {
			if (other._datasetId != null)
				return false;
		} else if (!_datasetId.equals(other._datasetId))
			return false;
		return true;
	}

//	public static void main(String[] args) {		// XXX Move to test
//		System.out.println(CastSet.class + " running");
//		String datasetId = "PRISM022008"; // "HOGREEF64W32N";
//		try {
//			DashboardConfigStore dcfg = DashboardConfigStore.get(false);
//			DataFileHandler dataFileHandler = dcfg.getDataFileHandler();
//			KnownDataTypes knownDataTypes = dcfg.getKnownUserDataTypes();
//			DashboardDatasetData ddd = dataFileHandler.getDatasetDataFromFiles(datasetId, 0, -1);
//			StdUserDataArray stda = new StdUserDataArray(ddd, knownDataTypes);
//			stda.checkCastConsistency();
//			System.out.println(stda.getStandardizationMessages());
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
//	}
}
