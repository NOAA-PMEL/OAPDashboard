/**
 * 
 */
package gov.noaa.pmel.dashboard.test.dsg;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * @author kamb
 *
 */
public class DsgModifier {

    private File _dsgFile;
    private NetcdfFileWriter _ncFile;
    
    public DsgModifier(File dsgFile) {
        _dsgFile = dsgFile;
    }
    
    private synchronized NetcdfFileWriter getNcFile() throws IOException {
        if ( _ncFile == null ) {
            _ncFile = NetcdfFileWriter.openExisting(_dsgFile.getPath());
        }
        return _ncFile;
    }
    
    private Variable getVar(String varname) throws IOException {
        Variable v = getNcFile().findVariable(varname);
        if ( v == null ) {
            v = findVar(varname);
        }
        return v;
    }
    
    private Variable findVar(String varname) throws IOException, IllegalArgumentException {
        List<Variable> allVars = getNcFile().getNetcdfFile().getVariables();
        for (Variable v : allVars) {
            String vName = v.getShortName();
            if (vName.equalsIgnoreCase(varname)) {
                return v;
            }
        }
        throw new IllegalArgumentException("No variable found that matches : "+ varname);
    }
    
    public void setString(String varname, int idx, String value) throws IOException, InvalidRangeException {
        Variable v = getVar(varname);
        ArrayChar.D2 ar = (ArrayChar.D2)v.read();
        ar.setString(idx, value);
        _ncFile.write(v, ar);
    }
    
    public void setStrings(String varname, List<Integer> indeces, List<String> values) throws IOException, InvalidRangeException {
        if ( indeces.size() != values.size()) {
            throw new IllegalArgumentException("Inconsistent number of indeces and values.");
        }
        Variable v = getVar(varname);
        ArrayChar.D2 ar = (ArrayChar.D2)v.read();
        Iterator<String> vals = values.iterator();
        for (Integer idx : indeces) {
	        ar.setString(idx.intValue(), vals.next());
	    }
        _ncFile.write(v, ar);
    }
    
    public void setChar(String varname, int idx, Character value) throws IOException, InvalidRangeException {
        Variable v = getVar(varname);
        ArrayChar.D2 ar = (ArrayChar.D2)v.read();
        ar.set(idx, 0, value.charValue());
        _ncFile.write(v, ar);
    }
    
    public void setChars(String varname, List<Integer> indeces, List<Character> values) throws IOException, InvalidRangeException {
        if ( indeces.size() != values.size()) {
            throw new IllegalArgumentException("Inconsistent number of indeces and values.");
        }
        Variable v = getVar(varname);
        ArrayChar.D2 ar = (ArrayChar.D2)v.read();
        Iterator<Character> vals = values.iterator();
        for (Integer idx : indeces) {
	        ar.set(idx.intValue(), 0, vals.next().charValue());
	    }
        _ncFile.write(v, ar);
    }
    
    public void setInt(String varname, int idx, Integer value) throws IOException, InvalidRangeException {
        Variable v = getVar(varname);
        ArrayInt.D1 ar = (ArrayInt.D1)v.read();
        ar.set(idx, value.intValue());
        _ncFile.write(v, ar);
    }
    
    public void setInts(String varname, List<Integer> indeces, List<Integer> values) throws IOException, InvalidRangeException {
        if ( indeces.size() != values.size()) {
            throw new IllegalArgumentException("Inconsistent number of indeces and values.");
        }
        Variable v = getVar(varname);
        ArrayInt.D1 ar = (ArrayInt.D1)v.read();
        Iterator<Integer> vals = values.iterator();
        for (Integer idx : indeces) {
	        ar.set(idx.intValue(), vals.next());
	    }
        _ncFile.write(v, ar);
    }
    
    public void setDouble(String varname, int idx, Double value) throws IOException, InvalidRangeException {
        Variable v = getVar(varname);
        ArrayDouble.D1 ar = (ArrayDouble.D1)v.read();
        ar.set(idx, value.doubleValue());
        _ncFile.write(v, ar);
    }
    
    public void setDoubles(String varname, List<Integer> indeces, List<Double> values) throws IOException, InvalidRangeException {
        if ( indeces.size() != values.size()) {
            throw new IllegalArgumentException("Inconsistent number of indeces and values.");
        }
        Variable v = getVar(varname);
        ArrayDouble.D1 ar = (ArrayDouble.D1)v.read();
        Iterator<Double> vals = values.iterator();
        for (Integer idx : indeces) {
	        ar.set(idx.intValue(), vals.next());
	    }
        _ncFile.write(v, ar);
    }
    
    public void close() throws IOException {
        _ncFile.close();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        String fileLocation = "/Users/kamb/Desktop/oap_dashboard_data_scratch.nc";
        List<Integer> indeces = Arrays.asList(new Integer[] {1, 2, 3, 4, 5 });
        List<String> strings = Arrays.asList(new String[] { "one", "two", "three", "four", "five" });
        List<Character> chars = Arrays.asList(new Character[] { 'a', 'b', 'c', 'd', 'e' });
        List<Double> doubles = Arrays.asList(new Double[] { 1.2, 2.3, 3.4, 4.5, 5.6 });
        try {
            File dsgFile = new File(fileLocation);
            DsgModifier mod = new DsgModifier(dsgFile);
            mod.setStrings("dataset_id", indeces, strings);
            mod.setInts("sample_number", indeces, indeces);
            mod.setChars("WOCE_autocheck", indeces, chars);
            mod.setDoubles("longitude", indeces, doubles);
            mod.setString("dataset_id", 6, "A new one");
            mod.setChar("WOCE_autocheck", 6, 'F');
            mod.setDouble("longitude", 6, 42.42);
            mod.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }

}
