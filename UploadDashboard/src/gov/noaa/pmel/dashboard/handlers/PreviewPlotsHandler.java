package gov.noaa.pmel.dashboard.handlers;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gov.noaa.pmel.dashboard.actions.DatasetChecker;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.DsgNcFile;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.ferret.FerretConfig;
import gov.noaa.pmel.dashboard.ferret.FerretConfig.Action;
import gov.noaa.pmel.dashboard.ferret.SocatTool;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.dashboard.shared.PreviewPlotImage;
import gov.noaa.pmel.dashboard.shared.PreviewTab;


public class PreviewPlotsHandler {

	File dsgFilesDir;
	File plotsFilesDir;
	DataFileHandler dataHandler;
//	DatasetChecker dataChecker;
	KnownDataTypes knownMetadataTypes;
	KnownDataTypes knownDataFileTypes;
	FerretConfig ferretConfig;
    private DashboardConfigStore _configStore;

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
        return datasetPlotsDir;
	}
	
	public File createDatasetPreviewPlotsDir(File datasetPlotsDir) throws IllegalArgumentException {
		if ( datasetPlotsDir.exists() ) {
			if ( ! datasetPlotsDir.isDirectory() ) {
				logger.warn("Plots directory exists but is not a directory: " + datasetPlotsDir.getPath());
                if ( ! datasetPlotsDir.delete()) {
        			throw new IllegalArgumentException("Failed to delete preview plots directory (file):" + 
    					datasetPlotsDir.getPath());
                }
			} else {
                clearDir(datasetPlotsDir);
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
        createPreviewPlots(_configStore.getDataFileHandler().getDatasetDataFromFiles(datasetId, 0, -1),
                                  timetag);
	}
	
	public void createPreviewPlots(DashboardDatasetData dataset, String timetag) 
										throws IllegalArgumentException {
        FeatureType featureType = dataset.getFeatureType();
		DatasetChecker dataChecker = _configStore.getDashboardDatasetChecker(featureType);

        String datasetId = dataset.getRecordId();
		logger.debug("standardizing data for " + datasetId);

		// Just create a minimal DsgMetadata to create the preview DSG file
		DsgMetadata dsgMData = new DsgMetadata(knownMetadataTypes);
		dsgMData.setDatasetId(datasetId);
		dsgMData.setVersion(dataset.getVersion());

		// TODO: update DsgMetadata with metadata derived from data
		// Although probably not important for the preview plots.
		StdUserDataArray stdUserData = dataChecker.standardizeDataset(dataset, null);
		if ( DashboardUtils.CHECK_STATUS_UNACCEPTABLE.equals(dataset.getDataCheckStatus()) )
			throw new IllegalArgumentException(datasetId + ": unacceptable; check data check error messages " +
										"(missing lon/lat/depth/time or uninterpretable values)");

		// Get the preview DSG filename, creating the parent directory if it does not exist
		File dsgDir = getDatasetPreviewDsgDir(datasetId);
        logger.debug("Preview DSG dir:"+ dsgDir);
        String dsgFilename = datasetId + "_" + timetag + ".nc";
        DsgNcFile dsgFile = DsgNcFile.newDsgFile(dataset.getFeatureType(), dsgDir, dsgFilename);
		logger.debug("creating preview DSG file " + dsgFile.getPath());

		// Create the preview NetCDF DSG file
		try {
			dsgFile.create(dsgMData, stdUserData, knownDataFileTypes);
			createSymlink(dsgFile, dsgDir, datasetId);
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
		File cruisePlotsDir = getDatasetPreviewPlotsDir(datasetId);
		String cruisePlotsDirname = cruisePlotsDir.getPath();
		File cruiseDsgDir = getDatasetPreviewDsgDir(datasetId);
		String dsgFileName = cruiseDsgDir.getAbsolutePath() + "/" + datasetId+".nc";

        // get the variables to plot
		List<String>plottableVarNames = getVariablesToPlot(dataset, stdUserData);
		
		boolean GENERATE_PLOTS = true;
		if ( GENERATE_PLOTS ) {
            cruisePlotsDir = createDatasetPreviewPlotsDir(cruisePlotsDir);
			// Call Ferret to generate the plots from the preview DSG file
//			if ( ! cruisePlotsDir.exists() ) {
//				cruisePlotsDir.mkdirs();
//			} else if ( cruisePlotsDir.list().length != 0 ) {
//				clearDir(cruisePlotsDir);
//			}
			SocatTool tool = new SocatTool(ferretConfig);
			ArrayList<String> scriptArgs = new ArrayList<String>(1);
			scriptArgs.add(dsgFileName);
			scriptArgs.add(cruisePlotsDirname);
			scriptArgs.add(timetag);
            FeatureType dataFeatureType = dataset.getFeatureType();
            FerretConfig.Action action = getFerretAction(dataFeatureType);
            if ( action == null ) {
                throw new IllegalArgumentException("No plotting action available for feature type: " + dataFeatureType);
            }
			tool.init(scriptArgs, datasetId, action, dataset, stdUserData, plottableVarNames);
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

	/**
     * @param dataset
     * @return
     */
    private List<String> getVariablesToPlot(DashboardDatasetData dataset, StdUserDataArray stdData) {
        List<String> dataVariables = new ArrayList<>();
        List<DataColumnType> columns = dataset.getDataColTypes();
        for (int col = 0; col < columns.size(); col++ ) {
            DataColumnType dcColumnType = columns.get(col);
            DashDataType<?> dashColumnType = knownDataFileTypes.getDataType(dcColumnType);
            if ( dashColumnType == null ) {
                dashColumnType = _configStore.getKnownUserDataTypes().getDataType(dcColumnType);
            }
            if ( dashColumnType != null 
                 && dashColumnType instanceof DoubleDashDataType
                 && stdData.isStandardized(col)
                 && ! excluded(dashColumnType)) {
                dataVariables.add(dcColumnType.getVarName());
            }
        }
        return dataVariables;
    }

//    private List<String> getVariablesToPlot(DashboardDatasetData dataset, StdUserDataArray stdData) {
//        List<String> dataVariables = new ArrayList<>();
//        for (DataColumnType dcColumnType : dataset.getDataColTypes()) {
//            DashDataType<?> dashColumnType = knownDataFileTypes.getDataType(dcColumnType);
//            if ( dashColumnType == null ) {
//                dashColumnType = _configStore.getKnownUserDataTypes().getDataType(dcColumnType);
//            }
//            if ( dashColumnType != null ) {
//                DashDataType<?> stdUserColumnType = stdData.findDataColumn(dashColumnType.getVarName());
//                if ( stdUserColumnType.isS)
//                 && dashColumnType instanceof DoubleDashDataType
//                 && ! excluded(dashColumnType)) {
//                dataVariables.add(dcColumnType.getVarName());
//            }
//        }
//        return dataVariables;
//    }

    static List<DoubleDashDataType> excludedVars = Arrays.asList(new DoubleDashDataType[] {
        DashboardServerUtils.TIME,
        DashboardServerUtils.LATITUDE,
        DashboardServerUtils.LONGITUDE,
        DashboardServerUtils.SAMPLE_DEPTH,
        DashboardServerUtils.CTD_PRESSURE,
        DashboardServerUtils.WESTERNMOST_LONGITUDE,
        DashboardServerUtils.EASTERNMOST_LONGITUDE,
        DashboardServerUtils.SOUTHERNMOST_LATITUDE,
        DashboardServerUtils.NORTHERNMOST_LATITUDE,
        DashboardServerUtils.TIME_COVERAGE_START,
        DashboardServerUtils.TIME_COVERAGE_END,
        DashboardServerUtils.SECOND_OF_MINUTE,
        DashboardServerUtils.DAY_OF_YEAR,
        DashboardServerUtils.SECOND_OF_DAY
    });
    /**
     * @param dashColumnType
     * @return
     */
    private boolean excluded(DashDataType<?> dashColumnType) {
        return excludedVars.contains(dashColumnType);
    }

    /**
     * @param dataFeatureType
     * @return
     */
    private static Action getFerretAction(FeatureType dataFeatureType) {
        switch (dataFeatureType) {
            case PROFILE:
                return FerretConfig.Action.profile_PLOTS;
            case TIMESERIES:
                return FerretConfig.Action.timeseries_PLOTS;
            case TRAJECTORY:
                return FerretConfig.Action.trajectory_PLOTS;
            case TIMESERIES_PROFILE:
                return FerretConfig.Action.timeseriesProfile_PLOTS;
            case TRAJECTORY_PROFILE:
                return FerretConfig.Action.trajectoryProfile_PLOTS;
            case UNSPECIFIED:
            case OTHER:
            default:
                return null;
        }
    }

    private void clearDir(File cruisePlotsDir) {
		for (File f : cruisePlotsDir.listFiles()) {
			if ( f.isDirectory()) {
				clearDir(f);
                f.delete();
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

	public List<PreviewTab> getPreviewPlots(final String datasetId, FeatureType featureType) {
        switch(featureType) {
            case TIMESERIES:
//                return getSimplePreviewPlots(datasetId);
            case TRAJECTORY:
//                return getSimplePreviewPlots(datasetId);
            case PROFILE:
                return getGeneralizedPreviewPlots(datasetId);
//                return getProfilePreviewPlots(datasetId);
            case TIMESERIES_PROFILE:
            case TRAJECTORY_PROFILE:
            case OTHER:
            case UNSPECIFIED:
            default:
                throw new IllegalArgumentException("DSG type " + featureType + " not supported.");
        }
	}
	/**
     * @param datasetId
     * @return
     */
    private List<PreviewTab> getGeneralizedPreviewPlots(String datasetId) {
		List<PreviewTab> plotTabs = new ArrayList<>();
		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
		String cruisePlotsDirname = getDatasetPreviewPlotsDir(stdId).getPath();
		File cruisePlotsDir = new File(cruisePlotsDirname);
        SortedSet<File> sortedPlotDirs = new TreeSet<>(new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortedPlotDirs.addAll(Arrays.asList(cruisePlotsDir.listFiles()));
        for (File dir : sortedPlotDirs) {
            if ( dir.isDirectory() && dir.list().length > 0 ) {
                PreviewTab tab = getTabPlots(dir);
                plotTabs.add(tab);
            }
        }
		return plotTabs;
    }
//    private List<PreviewTab> getSimplePreviewPlots(String datasetId) {
//		List<PreviewTab> plotTabs = new ArrayList<>();
//		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
//		String cruisePlotsDirname = getDatasetPreviewPlotsDir(stdId).getPath();
//		File cruisePlotsDir = new File(cruisePlotsDirname);
//		ArrayList<String> plotFiles = new ArrayList<>(Arrays.asList(cruisePlotsDir.list(new FilenameFilter() {
//			@Override
//			public boolean accept(File dir, String name) {
//				return name.startsWith(datasetId) && name.endsWith(".gif") || name.endsWith(".png");
//			}
//		})));
//		HashMap<String, PreviewPlotImage> plotNameMap = buildNameMap(plotFiles);
//		logger.debug("Overview---");
//		PreviewTab tabList = getOverviewPlots(plotNameMap, datasetId); //  :
//		plotTabs.add(tabList);
//		logger.debug("General---");
//		tabList = buildPlotList(plotNameMap);
//		plotTabs.add(tabList);
//        return plotTabs;
//    }

//    public List<PreviewTab> getProfilePreviewPlots(final String datasetId) {
//		List<PreviewTab> plotTabs = new ArrayList<>();
//		String stdId = DashboardServerUtils.checkDatasetID(datasetId);
//		String cruisePlotsDirname = getDatasetPreviewPlotsDir(stdId).getPath();
//		File cruisePlotsDir = new File(cruisePlotsDirname);
//		ArrayList<String> plotFiles = new ArrayList<>(Arrays.asList(cruisePlotsDir.list(new FilenameFilter() {
//			@Override
//			public boolean accept(File dir, String name) {
//				return name.startsWith(datasetId) && name.endsWith(".gif") || name.endsWith(".png");
//			}
//		})));
//		HashMap<String, PreviewPlotImage> plotNameMap = buildNameMap(plotFiles);
//		
//		logger.debug("Overview---");
//		PreviewTab tab = getOverviewPlots(plotNameMap, datasetId); //  :
//		plotTabs.add(tab);
//		logger.debug("General---");
//		tab = getProfileGeneralPlots(plotNameMap, datasetId);
//		plotTabs.add(tab);
//		logger.debug("BioGeo---");
//		tab = getBioGeoChemPlots(plotNameMap, datasetId);
//		plotTabs.add(tab);
////		logger.debug("Remain---");
////		tab = getRemainingPlots(plotNameMap, datasetId);
////		plotTabs.add(tab);
//        
////		int count = 0;
////		int plotsPerPage = 6;
////		List<String> tabList = new ArrayList<>();
////		for (File plotFile : plots) {
////			String plotFileName = plotFile.getName();
////			tabList.add(plotFileName);
////			if ( ++count % plotsPerPage == 0 ) {
////				plotTabs.add(tabList);
////				tabList = new ArrayList<>();
////			}
////		}
////		plotTabs.add(tabList);
//		return plotTabs;
//	}

	/**
     * @param plotNameMap
     * @return
     */
    private static PreviewTab buildPlotList(HashMap<String, PreviewPlotImage> plotNameMap) {
		PreviewTab plotList = new PreviewTab("PlotList");
        for (String plot : plotNameMap.keySet()) {
            plotList.addPlot(plotNameMap.get(plot));
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

	private static boolean checkAddAndRemove(String plotName, Map<String, PreviewPlotImage>plotsMap, PreviewTab plotList) {
		if ( plotsMap.containsKey(plotName)) {
			plotList.addPlot(plotsMap.get(plotName));
			plotsMap.remove(plotName);
			logger.debug(plotName);
			return true;
		} else {
			logger.info(plotName + " NOT FOUND.");
		}
		return false;
	}
	private static String plotTitle(String plotFilePath) {
	    String plotFileName = plotFilePath.indexOf("/") > -1 ?
	                            plotFilePath.substring(plotFilePath.lastIndexOf("/")+1) :
	                            plotFilePath;
        int firstIdx = plotFileName.indexOf('_');
        String varString = plotFileName.substring(firstIdx+1);
        if ( varString.indexOf('.') > 0 ) {
            varString = varString.substring(0, varString.lastIndexOf('.'));
        }
        String[] vars = varString.split("__");
        if ( vars.length == 2 ) {
            return vars[0].replaceAll("_", " ") + " vs " + vars[1].replaceAll("_", " ");
        }
        return vars[0].replaceAll("_", " ");
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
    private static PreviewTab getTabPlots(File directory) {
        String tabTitle = directory.getName();
        String dirName = tabTitle+"/";
        if ( tabTitle.matches("\\d+_[a-zA-Z].*")) {
            tabTitle = tabTitle.substring(tabTitle.indexOf('_')+1);
        }
		PreviewTab plotList = new PreviewTab(tabTitle);
        for (String plotFileName : directory.list()) {
            String plotPath = dirName + plotFileName;
            plotList.addPlot(new PreviewPlotImage(plotPath, plotTitle(plotPath)));
        }
		return plotList;
    }
	private static PreviewTab getOverviewPlots(HashMap<String, PreviewPlotImage> plotNameMap, String datasetId) {
		PreviewTab plotList = new PreviewTab("Overview");
		String plotName = plotName(datasetId,"map");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "profile_time", "show_time");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "sample_pressure", "sample_depth");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		return plotList;
	}

    private static final String pressureVarName = DsgNcFile.SAMPLE_PRESSURE_VARNAME;
    private static final String depthVarName = DsgNcFile.SAMPLE_DEPTH_VARNAME;
    
	private static PreviewTab getProfileGeneralPlots(HashMap<String, PreviewPlotImage> plotNameMap, String datasetId) {
		PreviewTab plotList = new PreviewTab("Profilactic");
		String pressureDepth = pressureVarName;
		String plotName = plotName(datasetId, "ctd_temperature", pressureVarName);
		if ( ! checkAddAndRemove(plotName, plotNameMap, plotList)) {
			plotName = plotName(datasetId, "ctd_temperature", depthVarName);
			if ( checkAddAndRemove(plotName, plotNameMap, plotList)) {
				pressureDepth = depthVarName;
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
		plotName = plotName(datasetId, "ctd_salinity", "ctd_temperature");
		checkAddAndRemove(plotName, plotNameMap, plotList);
		plotName = plotName(datasetId, "ctd_density", pressureDepth);
		checkAddAndRemove(plotName, plotNameMap, plotList);
		return plotList;
	}
	
	private PreviewTab getBioGeoChemPlots(HashMap<String,PreviewPlotImage> plotNameMap, String datasetId) {
		PreviewTab plotList = new PreviewTab("BGC");
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

	private static PreviewTab getRemainingPlots(HashMap<String, PreviewPlotImage> plotNameMap, String datasetId) {
		PreviewTab plotList = new PreviewTab("Nutrients", new ArrayList<>(plotNameMap.values()));
		logger.debug(plotList);
		return plotList;
	}
}
