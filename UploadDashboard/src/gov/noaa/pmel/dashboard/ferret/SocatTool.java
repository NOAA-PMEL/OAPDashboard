package gov.noaa.pmel.dashboard.ferret;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;

import gov.noaa.pmel.dashboard.dsg.DsgNcFile;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.tws.util.TemplateFileContentLoader;



public class SocatTool extends Thread {

    private static Logger logger = LogManager.getLogger(SocatTool.class);
    
    static boolean deleteScriptOnExit = false;
    
	FerretConfig ferret = new FerretConfig();
	ArrayList<String> scriptArgs;
    DashboardDataset dataset;
    StdUserDataArray stdUser;
	String expocode;
	FerretConfig.Action action;
    FeatureType dsgType;
	String message;
	boolean error;
	boolean done;
    List<String> plottableVarNames;

	public SocatTool(FerretConfig ferretConf) {
	    ferret = new FerretConfig();
	    ferret.setRootElement((Element)ferretConf.getRootElement().clone());
	    scriptArgs = new ArrayList<String>(3);
        dataset = null;
        stdUser = null;
	    expocode = null;
	    message = null;
	    error = false;
	    done = false;
	}

	public void init(List<String> scriptArgs, String expocode, FerretConfig.Action action) {
        this.init(scriptArgs, expocode, action, null, null, null);
	}
	public void init(List<String> scriptArgs, String expocode, FerretConfig.Action action, 
	                 DashboardDataset dataset, StdUserDataArray stdUser, List<String> plottableVarNames) {
		this.scriptArgs.clear();
		this.scriptArgs.addAll(scriptArgs);
        this.dataset = dataset;
        this.stdUser = stdUser;
		this.expocode = expocode;
		this.action = action;
        this.dsgType = dataset.getFeatureType();
        this.plottableVarNames = plottableVarNames;
	}

	@Override
	public void run() {
		done = false;
		error = false;
		try {

			String temp_dir = ferret.getTempDir();
			if ( !temp_dir.endsWith(File.separator) ) temp_dir = temp_dir + "/";
			
			File temp = new File(temp_dir);
			if ( !temp.exists() ) {
				temp.mkdirs();
			}

			String driver = ferret.getDriverScript(action);
            String driverTemplate = driver+".template";
            driver = createDriverScript(driverTemplate, temp);
            
			File script;
			if ( action.equals(FerretConfig.Action.COMPUTE) ) {
				script = new File(temp_dir, "ferret_compute_" + expocode + ".jnl");
			}
			else if ( action.equals(FerretConfig.Action.DECIMATE) ) {
				script = new File(temp_dir, "ferret_decimate_" + expocode + ".jnl");
			}
			else { // if ( action.equals(FerretConfig.Action.PLOTS)) {
				script = new File(temp_dir, "ferret_plots_" + expocode + ".jnl");
			}
//			else
//				throw new RuntimeException("Unknown action " + action.toString());
			
            logger.debug("Script file: "+ script);
			try ( PrintStream script_writer = new PrintStream(script); ) {
    			script_writer.print("go " + driver);
    			for (String scarg : scriptArgs)
    				script_writer.print(" \"" + scarg + "\"");
    			script_writer.println();
			}

			List<String> args = ferret.getArgs();
		    String interpreter = ferret.getInterpreter();
		    String executable = ferret.getExecutable();
		    String[] fullCmd;
		    int offset = 0;
		    if ( interpreter != null && !interpreter.equals("") ) {
		    	fullCmd = new String[args.size() + 3];
		    	fullCmd[0] = interpreter;
		    	fullCmd[1] = executable;
		    	offset = 2;
		    } else {
		    	fullCmd = new String[args.size() + 2];
		    	fullCmd[0] = executable;
		    	offset = 1;
		    }
		    for (int index = 0; index < args.size(); index++) {
	            String arg = (String) args.get(index);
	            fullCmd[offset+index] = arg;
            }

			fullCmd[args.size()+offset] = script.getAbsolutePath();
			
			long timelimit = ferret.getTimeLimit();

            logger.debug("Ferret tool to run command " + args + " using " + interpreter + " by " + executable);
			Task task = new Task(fullCmd, ferret.getRuntimeEnvironment().getEnv(), 
					new File(temp_dir), new File("cancel"), timelimit, ferret.getErrorKeys());
			task.run();
			error = task.getHasError();
			message = task.getErrorMessage();
			done = true;
			if ( ! error && deleteScriptOnExit )
				script.delete();
		} catch ( Exception e ) {
            logger.warn(e,e);
            e.printStackTrace();
			done = true;
			error = true;
			message = e.getMessage();
		} 
	}
    /**
     * @param driverTemplate
     * @param temp
     * @param dsgType2
     * @throws IOException 
     */
    private String createDriverScript(String driverTemplate, File temp) throws Exception {
        File templateFile = getTemplateFile(driverTemplate);
        String driverName = driverTemplate.substring(0, driverTemplate.indexOf(".template"))+"_"+expocode + ".jnl";
        switch (dsgType) {
            case TIMESERIES:
                createTimeseriesDriver(templateFile, driverName, temp);
                break;
            case TRAJECTORY:
                createTrajectoryDriver(templateFile, driverName, temp);
                break;
            case PROFILE:
                createProfileDriver(templateFile, driverName, temp);
                break;
            case OTHER:
            case TIMESERIES_PROFILE:
            case TRAJECTORY_PROFILE:
            case UNSPECIFIED:
            default:
                throw new IllegalArgumentException("DSG type " + dsgType + " not supported.");
        }
        return driverName;
    }

    /**
     * @param driverTemplate
     * @return
     */
    private File getTemplateFile(String driverTemplate) {
        File templatesDir = new File(ferret.getTemplatesDirectory());
        File template = new File(templatesDir, driverTemplate);
        return template;
    }

    /**
     * @param driver
     * @param temp
     * @throws IOException 
     */
    private void createTimeseriesDriver(File templateFile, String driverName, File temp) throws Exception {
        File driverFile = new File(temp, driverName);
        TemplateFileContentLoader template = TemplateFileContentLoader.NewLoader(templateFile);
        StringBuilder timeseriesPlots = new StringBuilder();
        Map<String, String>replacements = new HashMap<String, String>();
        if ( plottableVarNames != null ) {
            for ( String varname : plottableVarNames) {
                timeseriesPlots.append("go OA_timeseries_plot ").append(varname).append("\n");
            }
            replacements.put("TIMESERIES_PLOTS", timeseriesPlots.toString());
        } else {
            replacements.put("TIMESERIES_PLOTS", "");  // nothing to plot...
        }
        StringBuilder propertiesPlots = new StringBuilder("! Property-property plots \n");
        // Leaving this out for now
//        DoubleDashDataType pco2 = (DoubleDashDataType)stdUser.findDataColumn("pCO2_water_sst_100humidity_uatm");
//        logger.debug("Found pco2:"+pco2);
//        DoubleDashDataType ph = (DoubleDashDataType)stdUser.findDataColumn("ph_total");
//        logger.debug("Found ph:"+ph);
//        DoubleDashDataType sst = (DoubleDashDataType)stdUser.findDataColumn("sea_surface_temperature");
//        logger.debug("Found sst:"+sst);
//        DoubleDashDataType o2 = (DoubleDashDataType)stdUser.findDataColumn("ctd_oxygen");
//        logger.debug("Found o2:"+o2);
//        if ( pco2 != null && ph != null ) {
//            propertiesPlots.append("go OA2_thumbnail_pair " + pco2.getVarName() + " " + ph.getVarName() + "\n");
//        }
//        if ( ph != null && sst != null ) {
//            propertiesPlots.append("go OA2_thumbnail_pair " + ph.getVarName() + " " + sst.getVarName() + "\n");
//        }
//        if ( o2 != null && sst != null ) {
//            propertiesPlots.append("go OA2_thumbnail_pair " + o2.getVarName() + " " + sst.getVarName() + "\n");
//        }
        replacements.put("PROPERTIES_PLOTS", propertiesPlots.toString()); 
        String driverContent = template.getContent(replacements);
        try ( FileWriter fout = new FileWriter(driverFile)) {
            fout.write(driverContent);
        }
    }

    /**
     * @param driver
     * @param temp
     * @throws Exception 
     */
    private void createTrajectoryDriver(File templateFile, String driverName, File temp) throws Exception {
        File driverFile = new File(temp, driverName);
        TemplateFileContentLoader template = TemplateFileContentLoader.NewLoader(templateFile);
        StringBuilder timeseriesPlots = new StringBuilder();
        Map<String, String>replacements = new HashMap<String, String>();
        if ( plottableVarNames != null ) {
            for ( String varname : plottableVarNames) {
                timeseriesPlots.append("go OA_trajectory_plot ").append(varname).append("\n");
            }
            replacements.put("TRAJECTORY_PLOTS", timeseriesPlots.toString());
        } else {
            replacements.put("TRAJECTORY_PLOTS", "");  // nothing to plot...
        }
        String driverContent = template.getContent(replacements);
        try ( FileWriter fout = new FileWriter(driverFile)) {
            fout.write(driverContent);
        }
    }

    /**
     * @param driver
     * @param temp
     * @throws IOException 
     */
    private void createProfileDriver(File templateFile, String driverName, File temp) throws Exception {
        File driverFile = new File(temp, driverName);
        TemplateFileContentLoader template = TemplateFileContentLoader.NewLoader(templateFile);
        Map<String, String>replacements = new HashMap<String, String>();
        String depthVarName =  stdUser.hasSamplePressure() ?
                                DsgNcFile.SAMPLE_PRESSURE_VARNAME :
                                DsgNcFile.SAMPLE_DEPTH_VARNAME;
        replacements.put("DEPTH_VAR_NAME", depthVarName);
        
        String driverContent = template.getContent(replacements);
        try ( FileWriter fout = new FileWriter(driverFile)) {
            fout.write(driverContent);
        }
    }

    /**
     * @param dsgType2
     * @return
    private String getDsgPlotsDriverScript() {
        switch (dsgType) {
            case TIMESERIES:
                return "OAPPreview_timeseries";
            case TRAJECTORY:
                return "OAPPreview_trajectory";
            case PROFILE:
                return "OAPPreview_profile";
            default:
                throw new IllegalArgumentException("DSG type " + dsgType + " not supported.");
        }
    }
     */

    public boolean hasError() {
        return error;
    }
    public String getErrorMessage() {
        return message;
    }
	public boolean isDone() {
		return done;
	}
}
