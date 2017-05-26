/**
 * 
 */
package gov.noaa.pmel.dashboard.programs;

import java.util.ArrayList;
import java.util.Date;

import gov.noaa.pmel.dashboard.actions.OverlapChecker;
import gov.noaa.pmel.dashboard.handlers.DsgNcFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.server.Overlap;
import gov.noaa.pmel.dashboard.server.SocatTypes;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataLocation;
import gov.noaa.pmel.dashboard.shared.WoceEvent;

/**
 * Assigns WOCE-4 flags to the tail duplicate data points in an overlap between two datasets.
 * 
 * @author Karl Smith
 */
public class WoceOverlapTail {

	/**
	 * @param args
	 * 		firstExpo  secondExpo
	 * 
	 * Computes the overlaps between the two datasets indicated by the expocodes.
	 * If the overlap is one where the tail of one is a duplicate of the start of
	 * another, then the tail duplicates are all given a WOCE-4 flag on aqueous fCO2.
	 */
	public static void main(String[] args) {
		if ( args.length != 2 ) {
			System.err.println();
			System.err.println("Usage:  firstExpo  secondExpo");
			System.err.println();
			System.err.println("Computes the overlaps between the two datasets indicated by the expocodes. ");
			System.err.println("If the overlap is one where the tail of the first dataset is a duplicate ");
			System.err.println("of the start of the second dataset, then the tail duplicates in the first ");
			System.err.println("dataset are all given a WOCE-4 flag on aqueous fCO2. ");
			System.err.println();
			System.exit(1);
		}
		String firstExpo = DashboardServerUtils.checkExpocode(args[0]);
		String secondExpo = DashboardServerUtils.checkExpocode(args[1]);

		DashboardConfigStore configStore = null;
		try {
			configStore = DashboardConfigStore.get(false);
		} catch (Exception ex) {
			System.err.println("Problems obtaining the default dashboard configuration: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
		try {

			DsgNcFileHandler dsgHandler = configStore.getDsgNcFileHandler();
			OverlapChecker oerlapChecker = new OverlapChecker(dsgHandler);

			Overlap oerlap = null;
			try {
				ArrayList<String> others = new ArrayList<String>(1);
				others.add(secondExpo);
				ArrayList<Overlap> overlapList = oerlapChecker.getOverlaps(firstExpo, others, null, 0);
				if ( overlapList == null )
					throw new IllegalArgumentException("no overlap found");
				if ( overlapList.size() != 1 )
					throw new RuntimeException("unexpected overlap list size of " + 
							Integer.toString(overlapList.size()));
				oerlap = overlapList.get(0);
			} catch ( Exception ex ) {
				System.err.println("Problems getting the overlap between " + firstExpo + 
						" and " + secondExpo + ": " + ex.getMessage());
				System.exit(1);
			}

			ArrayList<Integer> firstRowNums = oerlap.getRowNums()[0];
			double[] firstFCo2 = null;
			try {
				ArrayList<Integer> secondRowNums = oerlap.getRowNums()[1];
				// Verify the overlap row numbers are appropriate for performing this automatic WOCE flagging
				if ( firstRowNums.size() != secondRowNums.size() )
					throw new RuntimeException("unexpected different number of data row numbers");
				int delta = firstRowNums.get(0) - secondRowNums.get(0) + 1;
				for (int k = 0; k < firstRowNums.size(); k++) {
					if ( secondRowNums.get(k) != k+1 )
						throw new IllegalArgumentException("overlap in second dataset is not contiguous start of the dataset");
					if ( firstRowNums.get(k) != k+delta )
						throw new IllegalArgumentException("overlap in first dataset is not contiguous");
				}
				// Verify the fCO2_rec values are the same in the overlap
				firstFCo2 = dsgHandler.readDoubleVarDataValues(firstExpo, SocatTypes.FCO2_REC.getVarName());
				if ( firstFCo2.length != firstRowNums.get(firstRowNums.size()-1) )
					throw new IllegalArgumentException("overlap in first dataset is not end of the dataset");
				double[] secondFCo2 = dsgHandler.readDoubleVarDataValues(secondExpo, SocatTypes.FCO2_REC.getVarName());
				for (int k = 0; k < firstRowNums.size(); k++) {
					if ( ! DashboardUtils.closeTo(firstFCo2[firstRowNums.get(k) - 1], secondFCo2[secondRowNums.get(k) - 1], 
							DashboardUtils.MAX_RELATIVE_ERROR, DashboardUtils.MAX_ABSOLUTE_ERROR) )
						throw new IllegalArgumentException("overlap is not a duplicate in fCO2_rec values");
				}
			} catch ( Exception ex ) {
				System.err.println("Invalid overlap between " + firstExpo + " and " + secondExpo + ": " + ex.getMessage());
				System.exit(1);
			}
			
			// Add the WOCE flags to the tail of the first dataset
			try {
				// Create the WOCE event
				double[][] datavals = dsgHandler.readLonLatTimeDataValues(firstExpo);
				double[] longitudes = datavals[0];
				double[] latitudes = datavals[1];
				double[] times = datavals[2];
				char[] regionIDs = dsgHandler.readCharVarDataValues(firstExpo, SocatTypes.REGION_ID.getVarName());
				ArrayList<DataLocation> locations = new ArrayList<DataLocation>(firstRowNums.size());
				for ( Integer num : firstRowNums ) {
					int k = num - 1;
					DataLocation loc = new DataLocation();
					loc.setDataDate(new Date(Math.round(times[k] * 1000.0)));
					loc.setDataValue(firstFCo2[k]);
					loc.setLatitude(latitudes[k]);
					loc.setLongitude(longitudes[k]);
					loc.setRegionID(regionIDs[k]);
					loc.setRowNumber(num);
					locations.add(loc);
				}
				WoceEvent woceEvent = new WoceEvent();
				woceEvent.setExpocode(firstExpo);
				woceEvent.setVersion(configStore.getQCVersion());
				woceEvent.setWoceName(SocatTypes.WOCE_CO2_WATER.getVarName());
				woceEvent.setFlag(DashboardUtils.WOCE_BAD);
				woceEvent.setFlagDate(new Date());
				woceEvent.setComment("duplicate lon/lat/time/fCO2_rec data points with " + 
						secondExpo + " detected by automation");
				woceEvent.setUsername(DashboardUtils.SANITY_CHECKER_USERNAME);
				woceEvent.setRealname(DashboardUtils.SANITY_CHECKER_REALNAME);
				woceEvent.setVarName(SocatTypes.FCO2_REC.getVarName());
				woceEvent.setLocations(locations);

				// Add the WOCE event to the database
				configStore.getDatabaseRequestHandler().addWoceEvent(woceEvent);
				// Assign the WOCE-4 flags in the full-data DSG file
				ArrayList<String> issues = dsgHandler.getDsgNcFile(firstExpo).assignWoceFlags(woceEvent);
				if ( ! issues.isEmpty() ) {
					for ( String msg : issues ) {
						System.err.println(msg);
					}
					throw new RuntimeException("unexpected error");
				}
				configStore.getDsgNcFileHandler().decimateCruise(firstExpo);
				// Report WOCE-4 of duplicates
				System.out.println("WOCE-4 assigned to duplicate datapoints in " + firstExpo + ": " + firstRowNums.toString());
			} catch ( Exception ex ) {
				System.err.println("Problems assigning WOCE flags to " + firstExpo + ": " + ex.getMessage());
			}
		} finally {
			DashboardConfigStore.shutdown();
		}

		System.exit(0);
	}

}
