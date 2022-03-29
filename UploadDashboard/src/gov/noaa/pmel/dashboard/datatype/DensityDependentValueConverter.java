/**
 * 
 */
package gov.noaa.pmel.dashboard.datatype;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;

/**
 * @author kamb
 *
 */
public class DensityDependentValueConverter extends ValueConverter<Double> {

    private static final Logger logger =  LogManager.getLogger(DensityDependentValueConverter.class);
    
	public static final String uMOLES_PER_KG_UNIT_STR = "umol/kg";
	public static final String uMOLES_PER_L_UNIT_STR = "umol/L";
	public static final String uMOLES_PER_mL_UNIT_STR = "umol/mL";
	public static final String mgPerLiter_UNIT_STR = "mg/L";
	public static final String mlPerLiter_UNIT_STR = "mL/L";
	private static final double OXY_CONST = 44659.6;
	private static final double MgL_CONST = 1.42903;
	
	private String _varName;
	private boolean _isNutrient = false;
	private boolean _isMgL = false;
	private boolean _isUmolmL = false;
	private String _conversionKey;
	private SigmaThetaCalculator _sigTheta;
    private String _toString;
	
	private static String _mgPerL2uMolesPerKgKey = conversionKey(mgPerLiter_UNIT_STR, uMOLES_PER_KG_UNIT_STR);
	private static String _mlPerL2uMolesPerKgKey = conversionKey(mlPerLiter_UNIT_STR, uMOLES_PER_KG_UNIT_STR);
	private static String _uMolesPerL2uMolesPerKgKey = conversionKey(uMOLES_PER_L_UNIT_STR, uMOLES_PER_KG_UNIT_STR);
//	private static String _uMolesPermL2uMolesPerKgKey = conversionKey(uMOLES_PER_mL_UNIT_STR, uMOLES_PER_KG_UNIT_STR);

	private static TreeSet<String> _supportedConversions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	static {
		
		_supportedConversions.add(_mgPerL2uMolesPerKgKey);
		_supportedConversions.add(_mlPerL2uMolesPerKgKey);
		_supportedConversions.add(_uMolesPerL2uMolesPerKgKey);
//		_supportedConversions.add(_uMolesPermL2uMolesPerKgKey);
	}
	
	public static DensityDependentValueConverter getConverterFor(String varName, String inputUnit, String outputUnit, 
	                                                             String missingValue, StdUserDataArray dataset) {
		return new DensityDependentValueConverter(inputUnit, outputUnit, missingValue, dataset, varName);
	}
	/**
	 * @param inputUnit
	 * @param outputUnit
	 * @param missingValue
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 */
	public DensityDependentValueConverter(String inputUnit, String outputUnit, String missingValue, StdUserDataArray dataset, 
			                               String varName)
			throws IllegalArgumentException, IllegalStateException {
		super(inputUnit, outputUnit, missingValue);
		if ( varName == null || varName.trim().length() == 0 ) {
			throw new IllegalArgumentException("No variable name specified");
		}
		_varName = varName.trim();
		_conversionKey = conversionKey(inputUnit, outputUnit);
		if ( ! _supportedConversions.contains(_conversionKey)) {
			throw new IllegalArgumentException("Unsupported conversion: "+ _conversionKey);
		}
        boolean isOxygen = _varName.contains("oxygen");
		_isNutrient = isNutrient(_varName); 
		if ( _isNutrient && ! _uMolesPerL2uMolesPerKgKey.equals(_conversionKey)) {
			throw new IllegalArgumentException( _varName + ": Unsupported conversion for nutrient type: " + _conversionKey);
		}
        if ( ! (isOxygen || _isNutrient)) {
			throw new IllegalArgumentException( "Conversion not supported for type: " + _varName );
		}
		_isMgL = inputUnit.equalsIgnoreCase(mgPerLiter_UNIT_STR);
		_isUmolmL = inputUnit.equalsIgnoreCase(uMOLES_PER_mL_UNIT_STR);
		_sigTheta = SigmaThetaCalculator.getCalculatorFor(varName, dataset);
        _toString = toString();
	}

    private static final Set<String> nutrients = new HashSet<String>() {{
        add("nitrate");
        add("nitrite");
        add("ammonia");
        add("ammonium");
        add("phosphate");
        add("silicate");
    }};
    /**
     * @param varName
     * @return
     */
    private static boolean isNutrient(String varName) {
        for (String nut : nutrients) {
            if ( varName.contains(nut)) { 
                return true; 
            }
        }
        return false;
    }
    @Override
    public String toString() {
        if ( _toString == null ) {
            StringBuilder sb = new StringBuilder();
            sb.append(_conversionKey)
              .append(" for ")
              .append(_varName);
            _toString = sb.toString();
        }
        return _toString;
    }
    
	/**
	 * @see gov.noaa.pmel.dashboard.datatype.ValueConverter#convertValueOf(java.lang.String)
	 */
	@Override
	public Double convertValueOf(String valueString, int recordNumber) throws IllegalArgumentException, IllegalStateException {
		if ( isMissingValue(valueString, true)) {
			return null;
		}
		double columnValue = Double.parseDouble(valueString);
        try {
    		double dependentValue = _sigTheta.getSigmaThetaForRow(recordNumber);
    		double convertedValue = _isNutrient ?
    									convertNutrient(columnValue, dependentValue) :
    									convertOxy(columnValue, dependentValue);
    		return new Double(convertedValue);
        } catch (IllegalStateException isex) {
            logger.info(isex.getMessage() + " at row " + recordNumber);
            return null;
        }
	}
	
	private double convertNutrient(double value, double sigTheta) throws IllegalArgumentException, IllegalStateException {
        double vPrime = _isUmolmL ? umolPerml2umolPerL(value) : value;
		double convertedValue = vPrime / ( 1 + (sigTheta/1000));
		return convertedValue;
	}

    private static double umolPerml2umolPerL(double value) {
        return value / 1000;
    }
    private double convertOxy(double value, double sigTheta) throws IllegalArgumentException, IllegalStateException {
		double mlL = _isMgL ? mgL2mlL(value) : value;
		double convertedValue = mlL * ( OXY_CONST / (sigTheta+1000.));
		return convertedValue;
	}

	private static double mgL2mlL(double mgL) {
		return mgL / MgL_CONST;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.setProperty("CATALINA_BASE","/local/tomcat/7");
			System.setProperty("UPLOAD_DASHBOARD_SERVER_NAME","OAPUploadDashboard");
			String datasetId = "PRISM102011"; // "HOGREEF64W32N";
			DashboardConfigStore dcfg = DashboardConfigStore.get(false);
			DataFileHandler dataFileHandler = dcfg.getDataFileHandler();
			KnownDataTypes knownDataTypes = dcfg.getKnownUserDataTypes();
			DashboardDatasetData ddd = dataFileHandler.getDatasetDataFromFiles(datasetId, 0, -1);
			StdUserDataArray stda = new StdUserDataArray(ddd, knownDataTypes);
			Double[] oxyValues = (Double[])stda.getStdValues(17); // "ctd_oxygen");
			System.out.println(oxyValues);
			oxyValues = (Double[])stda.getStdValues("ctd_oxygen");
			System.out.println(oxyValues);
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

}
