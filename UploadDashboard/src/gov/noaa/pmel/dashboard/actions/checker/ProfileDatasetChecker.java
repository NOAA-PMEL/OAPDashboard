/**
 * 
 */
package gov.noaa.pmel.dashboard.actions.checker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.data.sanity.CastChecker;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.CheckerMessageHandler;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.ADCMessage;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.QCFlag;
import gov.noaa.pmel.dashboard.shared.QCFlag.Severity;

/**
 * Class for interpreting, standardizing, and checking user-provided data. 
 * 
 * @author Karl Smith
 */
public class ProfileDatasetChecker extends BaseDatasetChecker implements DatasetChecker {

    private static Logger logger = LogManager.getLogger(ProfileDatasetChecker.class);
    
	/**
	 * @param userDataTypes
	 * 		all known user data types
	 * @param checkerMessageHandler
	 * 		handler for automated data checker messages
	 * @throws IllegalArgumentException
	 * 		if either argument is null, or 
	 * 		if there are no user data types given
	 */
	public ProfileDatasetChecker(KnownDataTypes userDataTypes, 
			CheckerMessageHandler checkerMessageHandler) throws IllegalArgumentException {
		if ( (userDataTypes == null) || userDataTypes.isEmpty() )
			throw new IllegalArgumentException("no known user data types");
		if ( checkerMessageHandler == null )
			throw new IllegalArgumentException("no message handler given to the dataset checker");
		knownUserDataTypes = userDataTypes;
		msgHandler = checkerMessageHandler;
	}

    @Override
	public StdUserDataArray standardizeDataset(DashboardDatasetData dataset) throws IllegalArgumentException {
        return standardizeDataset(dataset, null);
	}
    
	/**
	 * Interprets the data string representations and standardizes, if required, 
	 * these data values for given dataset.  Performs the automated data checks 
	 * on these data values.  Saves the messages generated from these steps and 
	 * assigns the automated data checker WOCE flags from these messages.  
	 * <br /><br />
	 * The given dateset object is updated with the set of checker QC flags, the 
	 * set of user-provided QC flags, the number of rows with errors (not marked 
	 * by the PI), the number of rows with warnings (not marked by the PI), and 
	 * the current data check status.
	 * <br /><br />
	 * The given metadata object, if not null, it is updated with values that can 
	 * be derived from the data: western-most longitude, eastern-most longitude, 
	 * southern-most latitude, northern-most latitude, start time, and end time.
	 *  
	 * @param dataset
	 * 		dataset data to check; various fields will be updated by this method
	 * @param metadata
	 * 		metadata to update; MUST BE null ...
	 * @return
	 * 		standardized user data array of checked values
	 * @throws IllegalArgumentException
	 * 		if there are no data values,
	 * 		if a data column description is not a known user data type,
	 * 		if a required unit conversion is not supported, 
	 * 		if a standardizer for a given data type is not known, 
     *      if the DsgMetadata is not null
	 * 		if ....
	 */
	public StdUserDataArray standardizeDataset(DashboardDatasetData dataset,
                                    		   DsgMetadata metadata) throws IllegalArgumentException {
		// Generate array of standardized data objects
		StdUserDataArray stdUserData = new StdUserDataArray(dataset, knownUserDataTypes);

        checkForUnknownColumns(stdUserData);
        
		if ( ! hasRequiredColumns(stdUserData)) {
			msgHandler.processCheckerMessages(dataset, stdUserData);
            dataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_UNACCEPTABLE);
            return stdUserData;
//			throw new IllegalStateException("Dataset is missing required columns. See messages for details.");
		}
		
		// Check for missing lon/lat/time 
		stdUserData.checkMissingLonLatTime();
        boolean timesOk = stdUserData.timesAreOk();
        boolean depthsOk = checkForMissingPressureOrDepth(stdUserData);

		// Bounds check the standardized data values
		stdUserData.checkBounds();

		// Perform any other data checks // TODO:
        if ( timesOk && depthsOk ) {
    		checkCastConsistency(stdUserData);
        } else {
            ADCMessage warn = new ADCMessage();
            warn.setSeverity(Severity.WARNING);
            warn.setGeneralComment("Failed to check cast data.");
            StringBuilder sb = new StringBuilder("Unable to check cast data: There was a problem with ");
            if ( !timesOk ) { 
                sb.append("time and location ");
                if ( !depthsOk ) { 
                    sb.append("and depth data.");
                }
            } else {
                sb.append("depth data.");
            }
            warn.setDetailedComment(sb.toString());
            stdUserData.addStandardizationMessage(warn);
        }

		// Save the messages accumulated in stdUserData for this dataset.
		// Assigns the sets of checker-generated QC flags and user-provided QC flags 
		msgHandler.processCheckerMessages(dataset, stdUserData);

		// IMPORTANT: DO THIS ONLY AFTER ALL DATA CHECKS HAVE BEEN COMPLETED!
		// INCLUDING processing the CheckerMessages (since that pulls in User QC flags.)
        // Question: might delaying mess up possible inter-cast checks? TODO: Check it out.
		// Reorder the data as best possible
		if ( timesOk ) {
			Double[] sampleTimes = stdUserData.getSampleTimes();
			stdUserData.reorderData(sampleTimes);
            if ( stdUserData.locationsAreOk() ) {
                stdUserData.crossesDateLine();
            }
		}
		
		// Get the indices values the PI marked as bad.
		boolean hasCriticalError = false;
		HashSet<RowColumn> userErrs = new HashSet<RowColumn>();
		for ( QCFlag wtype : dataset.getUserFlags() ) {
			if ( Severity.CRITICAL.equals(wtype.getSeverity()) ) {
				hasCriticalError = true;
				userErrs.add(new RowColumn(wtype.getRowIndex(), wtype.getColumnIndex()));
			}
			else if ( Severity.ERROR.equals(wtype.getSeverity()) ) {
				userErrs.add(new RowColumn(wtype.getRowIndex(), wtype.getColumnIndex()));
			}
		}
		// Get the indices of values the PI marked as questionable 
		HashSet<RowColumn> userWarns = new HashSet<RowColumn>();
		for ( QCFlag wtype : dataset.getUserFlags() ) {
			if ( Severity.WARNING.equals(wtype.getSeverity()) ) {
				userWarns.add(new RowColumn(wtype.getRowIndex(), wtype.getColumnIndex()));
			}
		}
        int numCritical = 0;
		// Get the indices of data rows the automated data checker 
		// found having errors not not detected by the PI.
		List<Integer> errRows = new ArrayList<>();
		for ( QCFlag wtype : dataset.getCheckerFlags() ) {
			if ( Severity.CRITICAL.equals(wtype.getSeverity()) ) {
				hasCriticalError = true;
                numCritical += 1;
				RowColumn rowCol = new RowColumn(wtype.getRowIndex(), wtype.getColumnIndex());
				if ( ! userErrs.contains(rowCol) )
					errRows.add(wtype.getRowIndex());
			}
			if ( Severity.ERROR.equals(wtype.getSeverity())  ) {
				RowColumn rowCol = new RowColumn(wtype.getRowIndex(), wtype.getColumnIndex());
				if ( ! userErrs.contains(rowCol) )
					errRows.add(wtype.getRowIndex());
			}
		}
		// Get the indices of data rows the automated data checker 
		// found having only warnings but not detected by the PI.
		List<Integer> warnRows = new ArrayList<Integer>();
		for ( QCFlag wtype : dataset.getCheckerFlags() ) {
			if ( Severity.WARNING.equals(wtype.getSeverity()) ) {
				RowColumn rowCol = new RowColumn(wtype.getRowIndex(), wtype.getColumnIndex());
				Integer rowIdx = wtype.getRowIndex();
				if ( ! ( userErrs.contains(rowCol) || 
						 userWarns.contains(rowCol) || 
                         ( ! DashboardUtils.INT_MISSING_VALUE.equals(rowIdx) && errRows.contains(rowIdx) )))
					warnRows.add(rowIdx);
			}
		}

		int numErrorRows = errRows.size();
		int numWarnRows = warnRows.size();

		dataset.setNumErrorRows(numErrorRows);
		dataset.setNumWarnRows(numWarnRows);

		// Assign the data-check status message using the results of the sanity check
		if ( hasCriticalError ) {
//			dataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_UNACCEPTABLE);
			dataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_CRITICAL_ERRORS_PREFIX +
					Integer.toString(numCritical) + " errors");
		}
		else if ( numErrorRows > 0 ) {
			dataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_ERRORS_PREFIX +
					Integer.toString(numErrorRows) + " errors");
		}
		else if ( numWarnRows > 0 ) {
			dataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_WARNINGS_PREFIX +
					Integer.toString(numWarnRows) + " warnings");
		}
		else {
			dataset.setDataCheckStatus(DashboardUtils.CHECK_STATUS_ACCEPTABLE);
		}

		if ( metadata != null ) {
			// TODO:
			throw new IllegalArgumentException("updating metdata from data values not yet implemented");
		}

		return stdUserData;
	}
    
   /**
     * @return
     */
    public static boolean checkForMissingPressureOrDepth(StdUserDataArray stdUserData) {
        boolean allGood = true;
        Integer depthIdx = stdUserData.lookForDataColumnIndex(DashboardUtils.SAMPLE_DEPTH_VARNAME);
        Integer pressureIdx = stdUserData.lookForDataColumnIndex(DashboardUtils.CTD_PRESSURE_VARNAME);
        for (int row = 0; row < stdUserData.getNumSamples(); row++) {
            Object depth = depthIdx != null ? stdUserData.getStdVal(row, depthIdx.intValue()) : null;
            Object press = pressureIdx != null ? stdUserData.getStdVal(row, pressureIdx.intValue()) : null;
            if (( depth == null || DashboardUtils.FP_MISSING_VALUE.equals(depth)) && 
                ( press == null || DashboardUtils.FP_MISSING_VALUE.equals(press))) {
                Severity severity;
                String message;
                if ( depth == null && press == null ) {
                    severity = Severity.ERROR;
                    message = "No pressure or depth value for row " + (row+1);
                } else {
                    severity = Severity.WARNING;
                    message = "Missing Value for pressure or depth for row " + (row+1);
                }
                ADCMessage msg = stdUserData.messageFor(severity, new Integer(row), null, "Missing value", 
                                                        message);
                stdUserData.addStandardizationMessage(msg);
                allGood = false;
            } else if ( depthIdx != null && depth == null || DashboardUtils.FP_MISSING_VALUE.equals(depth)) {
                // warn missing depth
                ADCMessage msg = stdUserData.messageFor(Severity.WARNING, new Integer(row), depthIdx, "Missing value", 
                                                        "No depth value for row " + (row+1));
                stdUserData.addStandardizationMessage(msg);
            } else if ( pressureIdx != null && press == null || DashboardUtils.FP_MISSING_VALUE.equals(press)) {
                // warn missing pressure
                ADCMessage msg = stdUserData.messageFor(Severity.WARNING, new Integer(row), pressureIdx, "Missing value", 
                                                        "No pressure value for row " + (row+1));
                stdUserData.addStandardizationMessage(msg);
            }
        }
        return allGood;
    }

    public static boolean hasRequiredColumns(StdUserDataArray stdUserData) {
        boolean gotem = true;
        if ( ! stdUserData.hasDate()) {
            logger.info("dataset " + stdUserData.getDatasetName() + " missing date.");
            gotem = false;
            ADCMessage msg = new ADCMessage();
            msg.setSeverity(Severity.CRITICAL);
            msg.setGeneralComment("missing column");
            msg.setDetailedComment("The dataset does not identify the sample Date.");
            stdUserData.addStandardizationMessage(msg);
        }
        if ( ! stdUserData.hasSampleTime()) {
            logger.info("dataset " + stdUserData.getDatasetName() + " missing sample time.");
            gotem = false;
            ADCMessage msg = new ADCMessage();
            msg.setSeverity(Severity.CRITICAL);
            msg.setGeneralComment("missing column");
            msg.setDetailedComment("The dataset does not identify the sample Time.");
            stdUserData.addStandardizationMessage(msg);
        }
        if ( ! stdUserData.hasDataColumn(DashboardServerUtils.LATITUDE.getStandardName())) {
            logger.info("dataset " + stdUserData.getDatasetName() + " missing sample latitude.");
            gotem = false;
            ADCMessage msg = new ADCMessage();
            msg.setSeverity(Severity.CRITICAL);
            msg.setGeneralComment("missing column");
            msg.setDetailedComment("The dataset does not identify the cast Latitude.");
            stdUserData.addStandardizationMessage(msg);
        }
        if ( ! stdUserData.hasDataColumn(DashboardServerUtils.LONGITUDE.getStandardName())) {
            logger.info("dataset " + stdUserData.getDatasetName() + " missing sample longitude.");
            gotem = false;
            ADCMessage msg = new ADCMessage();
            msg.setSeverity(Severity.CRITICAL);
            msg.setGeneralComment("missing column");
            msg.setDetailedComment("The dataset does not identify the cast Longitude.");
            stdUserData.addStandardizationMessage(msg);
        }
        if ( ! ( stdUserData.hasDataColumn(DashboardServerUtils.SAMPLE_DEPTH.getStandardName()) || 
                 stdUserData.hasDataColumn(DashboardUtils.CTD_PRESSURE_VARNAME))) {
            logger.info("dataset " + stdUserData.getDatasetName() + " missing sample depth and pressure.");
            gotem = false;
            ADCMessage msg = new ADCMessage();
            msg.setSeverity(Severity.CRITICAL);
            msg.setGeneralComment("missing column");
            msg.setDetailedComment("The dataset does not identify either the sample Depth or Pressure.");
            stdUserData.addStandardizationMessage(msg);
        }
//	      if ( ! ( hasDataColumn(DashboardServerUtils.EXPO_CODE.getStandardName()) || 
//	               hasDataColumn(DashboardServerUtils.PLATFORM_CODE.getStandardName()))) {
//	          gotem = false;
//	          ADCMessage msg = new ADCMessage();
//	          msg.setSeverity(Severity.CRITICAL);
//	          msg.setGeneralComment("missing column");
//	          msg.setDetailedComment("The dataset must identify either the Platform Code or the Cruise Expocode.");
//	          addStandardizationMessage(msg);
//	      }
        
        return gotem;
    }


	private static void checkCastConsistency(StdUserDataArray stdData) {
		if ( !stdData.hasCastIdColumn()) {
            logger.info("No CastID column found for dataset " + stdData.getDatasetName());
            if ( !stdData.hasStationIdColumn()) {
                logger.warn("No CastID OR StationID found for dataset: " + stdData.getDatasetName());
                ADCMessage msg = new ADCMessage();
                msg.setSeverity(Severity.ERROR);
                msg.setGeneralComment("missing column");
                msg.setDetailedComment("The dataset has no profile identifier specified. Profiles cannot be checked.");
                stdData.addStandardizationMessage(msg);
                return;
//                throw new IllegalStateException("No station or cast identifier found.");
            }
		}
//		try {
			CastChecker cc = new CastChecker(stdData);
			cc.checkCastConsistency();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
	}
	
	public static void main(String[] args) {
        try {
            DashboardConfigStore cfg = DashboardConfigStore.get(false);
            DataFileHandler dfh = cfg.getDataFileHandler();
            DashboardDatasetData dataset = dfh.getDatasetDataFromFiles("PRISM082008", 0, -1);
            DatasetChecker dc = cfg.getDashboardDatasetChecker(FeatureType.PROFILE);
            StdUserDataArray std = dc.standardizeDataset(dataset, null);
            System.out.println(std);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
}
