/**
 * 
 */
package gov.noaa.pmel.dashboard.test.datatype;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Properties;

import gov.noaa.pmel.dashboard.datatype.DashDataType;
import static gov.noaa.pmel.dashboard.datatype.DashDataType.*;

/**
 * @author kamb
 *
 */
public class DataTypes {

    /*
	public static final String VAR_NAME_TAG = "var_name";
	public static final String DATA_CLASS_NAME_TAG = "data_class";
	public static final String SORT_ORDER_TAG = "sort_order";
	public static final String DISPLAY_NAME_TAG = "display_name";
	public static final String DESCRIPTION_TAG = "description";
	public static final String IS_CRITICAL_TAG = "is_critial";
	public static final String STANDARD_NAME_TAG = "standard_name";
	public static final String CATEGORY_NAME_TAG = "category_name";
	public static final String FILE_STD_UNIT_TAG = "file_std_unit";
	public static final String UNITS_TAG = "units";
	public static final String MIN_QUESTIONABLE_VALUE_TAG = "min_question_value";
	public static final String MIN_ACCEPTABLE_VALUE_TAG = "min_accept_value";
	public static final String MAX_ACCEPTABLE_VALUE_TAG = "max_accept_value";
	public static final String MAX_QUESTIONABLE_VALUE_TAG = "max_question_value";
    */
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        String userTypesFile = "/Users/kamb/workspace/OADashboard/ProductionConfigFiles/config/UserTypes.properties.hazy";
        String outputFile = "/Users/kamb/workspace/OADashboard/UserTypes.csv";
        try (InputStream inStream = new FileInputStream(userTypesFile);
             PrintWriter out = new PrintWriter(new FileWriter(outputFile)); ) {
            out.println("var_name,display_name,standard_name,description,category_name,"
                        + "file_std_unit,units,"
                        + "min_question_value,min_accept_value,max_accept_value,max_question_value,"
                        + "is_critical,data_class");
            Properties types = new Properties();
            types.load(inStream);
    		for ( String name : types.stringPropertyNames() ) {
                String typeDesc = types.getProperty(name);
                DashDataType<?> ddtype = DashDataType.fromPropertyValue(name, typeDesc);
                out.println(toCommaLine(ddtype));
    		}
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

    private static final String COMMA = ",";
    /**
     * @param ddtype
     * @return
     */
    private static String toCommaLine(DashDataType ddtype) {
        StringBuilder b = new StringBuilder()
            .append(ddtype.getVarName()).append(COMMA)
            .append(quote(ddtype.getDisplayName())).append(COMMA)
            .append(quote(ddtype.getStandardName())).append(COMMA)
            .append(quote(ddtype.getDescription())).append(COMMA)
            .append(ddtype.getCategoryName()).append(COMMA)
            .append(ddtype.getFileStdUnit()).append(COMMA)
            .append(quote(getUnits(ddtype))).append(COMMA)
            .append(COMMA) // min_questionable
            .append(COMMA) // min_acceptable
            .append(COMMA) // max_acceptable
            .append(COMMA) // max_questionable
            .append(ddtype.isCritical()).append(COMMA)
            .append(ddtype.getDataClassName()).append(COMMA);
        return b.toString();
    }
    /**
     * @param standardName
     * @return
     */
    private static String quote(String text) {
        return "\""+text+"\"";
    }
    
    /**
     * @param ddtype
     * @return
     */
    private static String getUnits(DashDataType<?> ddtype) {
        ArrayList<String> unitsList = ddtype.getUnits();
        if ( unitsList == null || unitsList.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder().append("[");
        String comma = "";
        for (String units : unitsList) {
            b.append(comma).append(units);
            comma = ",";
        }
        b.append("]");
        return b.toString();
    }

}
