package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.actions.checker.ProfileDatasetChecker;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.DsgNcFile;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.ferret.FerretConfig;
import gov.noaa.pmel.dashboard.ferret.SocatTool;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.PreviewPlotImage;


public class PreviewPlotsHandler {

	File dsgFilesDir;
	File plotsFilesDir;
	DataFileHandler dataHandler;
//	DatasetChecker dataChecker;
	KnownDataTypes knownMetadataTypes;
	KnownDataTypes knownDataFileTypes;
	FerretConfig ferretConfig;
    private DashboardConfigStore _configStore;
    private FeatureType featureType;

	private static Logger logger = LogManager.getLogger(PreviewPlotsHandler.class);
	
	/**
	 * Create with the given directories for the preview DSG files and plots.
	 * 
	 * @param previewDsgsDirName
	 * 		directory to contain the preview DSG files
	 * @param previewPlotsDirName
	 * 		directory to contain the preview plots
	 * @param configStore
	 * 		get the DataFileHandler, DatasetChecker, 
	 * 		MetadataFileHandler, and FerretConfig from here
	 */
	public PreviewPlotsHandler(String previewDsgsDirName, 
			String previewPlotsDirName, DashboardConfigStore configStore) {
		dsgFilesDir = new File(previewDsgsDirName);
		if ( ! dsgFilesDir.exists()) {
			dsgFilesDir.mkdirs();
		}
		if ( ! dsgFilesDir.exists() || ! dsgFilesDir.isDirectory() )
			throw new IllegalArgumentException(previewDsgsDirName + " doesn't exist or is not a directory");
		plotsFilesDir = new File(previewPlotsDirName);
		if ( ! plotsFilesDir.exists()) {
			plotsFilesDir.mkdirs();
		}
		if ( ! plotsFilesDir.exists() || ! plotsFilesDir.isDirectory() )
			throw new IllegalArgumentException(previewPlotsDirName + " doesn't exist or is not a directory");
		dataHandler = configStore.getDataFileHandler();
//		dataChecker = configStore.getDashboardDatasetChecker();
		knownMetadataTypes = configStore.getKnownMetadataTypes();
		knownDataFileTypes = configStore.getKnownDataFileTypes();
		ferretConfig = configStore.getFerretConfig();
        _configStore = configStore;
	}

	/**
	 * Returns the virtual file naming the directory that will contain 
	 * the preview DSG file of a dataset.  Creates this directory if it 
	 * does not exist.
	 * 
	 * @param datasetId
	 * 		ID of the dataset to use 
	 * @return
	 * 		preview DSG directory of the dataset
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid, 
	 * 		if there are problems creating the directory
	 */
	public File getDatasetPreviewDsgDir(String datasetId) throws IllegalArgumentException {
		// Check and standardize the dataset ID
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
		// Create the preview DSG subdirectory if it does not exist
		File datasetDsgDir = new File(dsgFilesDir, stdId.substring(0,4));
		if ( datasetDsgDir.exists() ) {
			if ( ! datasetDsgDir.isDirectory() ) {
				throw new IllegalArgumentException("Preview DSG file subdirectory exists but is not a directory: " + 
						datasetDsgDir.getPath()); 
			}
		}
		else if ( ! datasetDsgDir.mkdirs() ) {
			throw new IllegalArgumentException("Cannot create the preview DSG file subdirectory " + 
					datasetDsgDir.getPath());
		}
		return datasetDsgDir;
	}

	/**
	 * Returns the virtual file naming the directory that will contain 
	 * the preview plots directory for a dataset.  Creates this directory 
	 * if it does not exist.
	 * 
	 * @param datasetId
	 * 		ID of the dataset to use
	 * @return
	 * 		preview plots directory for the dataset
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid, or
	 * 		if there are problems creating the directory
	 */
	public File getDatasetPreviewPlotsDir(String datasetId) throws IllegalArgumentException {
		// Check and standardize the dataset ID
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
		// Create the preview plots subdirectory if it does not exist
		File datasetPlotsParent = new File(plotsFilesDir, stdId.substring(0,4));
		File datasetPlotsDir = new File(datasetPlotsParent, stdId);
        logger.debug("Plots dir:"+datasetPlotsDir);
		if ( datasetPlotsDir.exists() ) {
			if ( ! datasetPlotsDir.isDirectory() ) {
				throw new IllegalArgumentException("Plots directory exists but is not a directory: " + 
						datasetPlotsDir.getPath());
			}
		}
		else if ( ! datasetPlotsDir.mkdirs() ) {
			throw new IllegalArgumentException("Cannot create the preview plots file subdirectory " + 
					datasetPlotsDir.getPath());
		}
		return datasetPlotsDir;
	}

	/**
	 * Generates the data preview plots for the given dataset.  The data 
	 * is checked and standardized, the preview DSG file is created, Ferret 
	 * is called to add the computed variables to the DSG file, and 
	 * finally Ferret is called to generate the data preview plots from the 
	 * data in the DSG file.
	 * 
	 * @param datasetId
	 * 		ID of the dataset to preview
	 * @param timetag
	 * 		time tag to add to the end of the names of the plots 
	 * 		(before the filename extension)
	 * @throws IllegalArgumentException
	 * 		if the dataset ID is invalid,
	 * 		if there is an error reading the data or OME metadata,
	 * 		if there is an error checking and standardizing the data,
	 * 		if there is an error generating the preview DSG file, or
	 * 		if there is an error generating the preview data plots 
	 */
	public void createPreviewPlots(String datasetId, String timetag) 
										throws IllegalArgumentException {
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
		logger.debug("reading data for " + stdId);

		// Get the complete original cruise data
		DashboardDatasetData dataset = dataHandler.getDatasetDataFromFiles(stdId, 0, -1);
        featureType = dataset.getFeatureType();
		DatasetChecker dataChecker = _configStore.getDashboardDatasetChecker(featureType);

		logger.debug("standardizing data for " + stdId);

		// Just create a minimal DsgMetadata to create the preview DSG file
		DsgMetadata dsgMData = new DsgMetadata(knownMetadataTypes);
		dsgMData.setDatasetId(stdId);
		dsgMData.setVersion(dataset.getVersion());

		// TODO: update DsgMetadata with metadata derived from data
		// Although probably not important for the preview plots.
		StdUserDataArray stdUserData = dataChecker.standardizeDataset(dataset, null);
		if ( DashboardUtils.CHECK_STATUS_UNACCEPTABLE.equals(dataset.getDataCheckStatus()) )
			throw new IllegalArgumentException(stdId + ": unacceptable; check data check error messages " +
										"(missing lon/lat/depth/time or uninterpretable values)");

		// Get the preview DSG filename, creating the parent directory if it does not exist
		File dsgDir = getDatasetPreviewDsgDir(stdId);
        logger.debug("Preview DSG dir:"+ dsgDir);
        DsgNcFile dsgFile;
        if ( FeatureType.PROFILE.equals(dataset.getFeatureType())) {
    		dsgFile = DsgNcFile.createProfileFile(dsgDir, stdId + "_" + timetag + ".nc");
        } else if ( FeatureType.TRAJECTORY.equals(dataset.getFeatureType())) {
    		dsgFile = DsgNcFile.createTrajectoryFile(dsgDir, stdId + "_" + timetag + ".nc");
        } else {
            throw new IllegalArgumentException("Cannot create preview plots for feature type: " + dataset.getFeatureTypeName());
        }

		logger.debug("creating preview DSG file " + dsgFile.getPath());

		// Create the preview NetCDF DSG file
		try {
			dsgFile.create(dsgMData, stdUserData, knownDataFileTypes);
			createSymlink(dsgFile, dsgDir, stdId);
		} catch (Exception ex) {
            logger.warn(ex, ex);
            ex.printStackTrace();
//			dsgFile.delete();
			throw new IllegalArgumentException("Problems creating the preview DSG file for " + 
					datasetId + ": " + ex.getMessage(), ex);
		}

//		boolean DO_COMPUTED_VARS = false;
//		if ( DO_COMPUTED_VARS ) {
//			logger.debug("adding computed variables to preview DSG file " + dsgFile.getPath());
//			// Call Ferret to add the computed variables to the preview DSG file
//			SocatTool tool = new SocatTool(ferretConfig);
//			ArrayList<String> scriptArgs = new ArrayList<String>(1);
//			scriptArgs.add(dsgFile.getPath());
//			tool.init(scriptArgs, stdId, FerretConfig.Action.COMPUTE);
//			tool.run();
//			if ( tool.hasError() )
//				throw new IllegalArgumentException("Failure adding computed variables to the preview DSG file for " + 
//						datasetId + ": " + tool.getErrorMessage());
//		}

		logger.debug("generating preview plots for " + dsgFile.getPath());

		// Get the location for the preview plots, creating the directory if it does not exist
		File cruisePlotsDir = getDatasetPreviewPlotsDir(stdId);
		String cruisePlotsDirname = cruisePlotsDir.getPath();
		File cruiseDsgDir = getDatasetPreviewDsgDir(datasetId);
		String dsgFileName = cruiseDsgDir.getAbsolutePath() + "/" + stdId+".nc";

		boolean GENERATE_PLOTS = true;
		if ( GENERATE_PLOTS ) {
			// Call Ferret to generate the plots from the preview DSG file
			if ( ! cruisePlotsDir.exists() ) {
				cruisePlotsDir.mkdirs();
			} else if ( cruisePlotsDir.list().length != 0 ) {
				clearDir(cruisePlotsDir);
			}
			SocatTool tool = new SocatTool(ferretConfig);
			ArrayList<String> scriptArgs = new ArrayList<String>(1);
			scriptArgs.add(dsgFileName);
			scriptArgs.add(cruisePlotsDirname);
			scriptArgs.add(timetag);
            FeatureType dataFeatureType = dataset.getFeatureType();
            FerretConfig.Action action = dataFeatureType.equals(FeatureType.PROFILE) ?
                                            FerretConfig.Action.PLOTS :
                                            FerretConfig.Action.trajectory_PLOTS;
			tool.init(scriptArgs, stdId, action);
            try {
    			tool.run();
            } catch (Exception ex) {
                logger.warn(ex, ex);
            }
			if ( tool.hasError() )
				throw new IllegalArgumentException("Failure generating data preview plots for " + 
						datasetId + ": " + tool.getErrorMessage());
		}

		logger.debug("preview plots generated in " + cruisePlotsDirname);
	}

	private void clearDir(File cruisePlotsDir) {
		for (File f : cruisePlotsDir.listFiles()) {
			if ( f.isDirectory()) {
				clearDir(f);
			} else {
				f.delete();
			}
		}
	}

	private void createSymlink(DsgNcFile dsgFile, File dsgDir, String stdId) {
		String linkName = stdId + ".nc";
		File linkFile = new File(dsgDir, linkName);
		if ( linkFile.exists()) {
			linkFile.delete();
		}
		Path dsgTarget = dsgFile.toPath();
		Path dsgLink = linkFile.toPath();
		try {
			Files.createSymbolicLink(dsgLink, dsgTarget);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to create symlink for dsg preview file.", e);
		}
	}

	public List<List<PreviewPlotImage>> getPreviewPlots(final String datasetId) {
		List<List<PreviewPlotImage>> plotTabs = new ArrayList<>();
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
		String cruisePlotsDirname = getDatasetPreviewPlotsDir(stdId).getPath();
		File cruisePlotsDir = new File(cruisePlotsDirname);
		ArrayList<String> plotFiles = new ArrayList<>(Arrays.asList(cruisePlotsDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(datasetId) && name.endsWith(".gif") || name.endsWith(".png");
			}
		})));
		HashMap<String, PreviewPlotImage> plotNameMap = buildNameMap(plotFiles);
		
		logger.debug("Overview---");
		List<PreviewPlotImage> tabList = featureType.equals(FeatureType.PROFILE) ? 
		                                    getOverviewPlots(plotNameMap, datasetId) :
		                                    buildPlotList(plotNameMap);
		plotTabs.add(tabList);
		logger.debug("General---");
		tabList = getGeneralPlots(plotNameMap, datasetId);
		plotTabs.add(tabList);
		logger.debug("BioGeo---");
		tabList = getBioGeoChemPlots(plotNameMap, datasetId);
		plotTabs.add(tabList);
		logger.debug("Remain---");
		tabList = getRemainingPlots(plotNameMap, datasetId);
		plotTabs.add(tabList);
//		int count = 0;
//		int plotsPerPage = 6;
//		List<String> tabList = new ArrayList<>();
//		for (File plotFile : plots) {
//			String plotFileName = plotFile.getName();
//			tabList.add(plotFileName);
//			if ( ++count % plotsPerPage == 0 ) {
//				plotTabs.add(tabList);
//				tabList = new ArrayList<>();
//			}
//		}
//		plotTabs.add(tabList);
		return plotTabs;
	}

	/**
     * @param plotNameMap
     * @return
     */
    private static List<PreviewPlotImage> buildPlotList(HashMap<String, PreviewPlotImage> plotNameMap) {
		List<PreviewPlotImage> plotList = new ArrayList<>();
        for (String plot : plotNameMap.keySet()) {
            plotList.add(plotNameMap.get(plot));
        }
        return plotList;
    }

    private static HashMap<String, PreviewPlotImage> buildNameMap(ArrayList<String> plotFiles) {
		HashMap<String, PreviewPlotImage> nameMap = new HashMap<>();
		for (String file : plotFiles) {
			String name = file.substring(0, file.lastIndexOf('.'));
			PreviewPlotImage ppi = new PreviewPlotImage(file);
			nameMap.put(name, ppi);
		}
		return nameMap;
	}

	private static boolean checkAddAndRemove(String plotName, Map<String, PreviewPlotImage>plotsMap, List<PreviewPlotImage> plotList) {
		if ( plotsMap.containsKey(plotName)) {
			plotList.add(plotsMap.get(plotName));
			plotsMap.remove(plotName);
			logger.debug(plotName);
			return true;
		} else {
			logger.info(plotName + " NOT FOUND.");
		}
		return false;
	}
	private static String plotName(String datasetId, String... vars) {
		StringBuilder bldr = new StringBuilder(datasetId);
		String concat = "_";
		for (String var : vars) {
			bldr.append(concat).append(var);
			concat+="_";
		}
		return bldr.toString();
	}
	private static List<PreviewPlotImage> getOverviewPlots(HashMap<String, PreviewPlotImage> plotNameMap, String datasetId) {
		List<PreviewPlotImage> plotList = new ArrayList<>();
		String plotName = plotName(datasetId,"map");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "profile_time", "show_time");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ctd_pressure", "sample_depth");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		return plotList;
	}

	private static List<PreviewPlotImage> getGeneralPlots(HashMap<String, PreviewPlotImage> plotNameMap, String datasetId) {
		List<PreviewPlotImage> plotList = new ArrayList<>();
		String pressureDepth = "ctd_pressure";
		String plotName = plotName(datasetId, "ctd_temperature", "ctd_pressure");
		if ( ! checkAddAndRemove(plotName, plotNameMap, plotList)) {
			plotName = plotName(datasetId, "ctd_temperature", "sample_depth");
			if ( checkAddAndRemove(plotName, plotNameMap, plotList)) {
				pressureDepth = "sample_depth";
			}
		}
		plotName = plotName(datasetId, "ctd_salinity", pressureDepth);
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ctd_oxygen", pressureDepth);
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "inorganic_carbon", pressureDepth);
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "alkalinity", pressureDepth);
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ctd_temperature", "ctd_salinity");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ctd_density", pressureDepth);
		checkAddAndRemove(plotName, plotNameMap, plotList);
		return plotList;
	}
	
	private List<PreviewPlotImage> getBioGeoChemPlots(HashMap<String,PreviewPlotImage> plotNameMap, String datasetId) {
		List<PreviewPlotImage> plotList = new ArrayList<>();
		String plotName = plotName(datasetId, "ctd_salinity", "alkalinity");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ctd_oxygen", "inorganic_carbon");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "nitrate", "inorganic_carbon");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "nitrate", "alkalinity");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "nitrate", "ph_total");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ph_total", "inorganic_carbon");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ph_total", "alkalinity");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "alkalinity", "inorganic_carbon");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "oxygen", "ctd_oxygen");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		
		return plotList;
	}

	private static List<PreviewPlotImage> getRemainingPlots(HashMap<String, PreviewPlotImage> plotNameMap, String datasetId) {
		List<PreviewPlotImage> plotList = new ArrayList<>(plotNameMap.values());
		logger.debug(plotList);
		return plotList;
	}
}
