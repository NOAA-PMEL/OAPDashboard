/**
 * 
 */
package gov.noaa.pmel.dashboard.datatype;


import java.util.List;

import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;

/**
 * @author kamb
 *
 */
public abstract class SigmaThetaCalculator {
	
	/**
	 * @param ctdT90Temp The water temperature, in ITS90 scale.
	 * @param salinity The water salinity as measured.
	 * 
	 * @return The calculated value of sigma-theta per formula
	 * from Dana Greeley
	 */
	public static double fromTempAndSalinity(double ctdT90Temp, double salinity) {
		double tempT68 = 1.00024 * ctdT90Temp;
		double d = (0.00048314 * salinity 
						+ ((-0.0000016546 * tempT68 + 0.00010227) * tempT68 - 0.00572466) 
						* Math.sqrt(salinity) 
						+ (((0.0000000053875 * tempT68 - 0.00000082467) * tempT68 + 0.000076438) * tempT68 - 0.0040899) 
						* tempT68 + 0.824493) 
					* salinity 
					+ ((((0.000000006536332 * tempT68 - 0.000001120083) * tempT68 + 0.0001001685) 
						* tempT68 - 0.00909529) 
						* tempT68 + 0.06793952) 
					* tempT68 + 999.842594 - 1000;
		return d;
	}
	
	public static SigmaThetaCalculator getCalculatorFor(String varName, StdUserDataArray dataset) throws IllegalArgumentException {
		boolean isCtd = varName.startsWith("ctd_");
		SigmaThetaCalculator stc = isCtd ?
									new CTDSigmaThetaCalculator(dataset) :
									new LabSigmaThetaCalculator(dataset);
		return stc;
	}

	protected StdUserDataArray _dataset;
	
	public abstract double getSigmaThetaForRow(int rowIdx);
	
	protected SigmaThetaCalculator(StdUserDataArray dataset) {
		_dataset = dataset;
	}
	
	protected int findDependentValueColumnIdx(String varName) throws IllegalStateException {
		int columnIdx = -1;
		List<DashDataType<?>> colTypes = _dataset.getDataTypes();
		int idx = 0;
		for (DashDataType<?> colType : colTypes) {
			String colName = colType.getVarName();
			if ( colName.equalsIgnoreCase(varName)) {
				columnIdx = idx;
				break;
			}
			idx += 1;
		}
		if ( columnIdx == -1 ) {
			throw new IllegalArgumentException(varName + " column not found.");
		}
		return columnIdx;
	}
	
	private static class CTDSigmaThetaCalculator extends SigmaThetaCalculator {

		int _tempColumnIdx = -1;
		int _salinityColumnIdx;

		public CTDSigmaThetaCalculator(StdUserDataArray dataset) {
			super(dataset);
			_tempColumnIdx = findDependentValueColumnIdx("ctd_temperature");
			_salinityColumnIdx = findDependentValueColumnIdx("ctd_salinity");
		}
		
		@Override
		public double getSigmaThetaForRow(int rowIdx) {
			if ( ! ( _dataset.isUsableIndex(_tempColumnIdx) && _dataset.isUsableIndex(_salinityColumnIdx))) {
				throw new IllegalStateException("Dependent value is not yet standardized.");
			}
			double temp = ((Double)_dataset.getStdVal(rowIdx, _tempColumnIdx)).doubleValue();
			double sal = ((Double)_dataset.getStdVal(rowIdx, _salinityColumnIdx)).doubleValue();
			double density = fromTempAndSalinity(temp, sal);
			return density;
		}

	}
	
	private static class LabSigmaThetaCalculator extends SigmaThetaCalculator {

		private static final double DEFAULT_LAB_TEMP = 20.0;
		
		int _salinityColumnIdx;
		private double _labTemp = DEFAULT_LAB_TEMP;

		private LabSigmaThetaCalculator(StdUserDataArray dataset) {
			super(dataset);
			try {
				_salinityColumnIdx = findDependentValueColumnIdx("salinity");
			} catch (IllegalArgumentException iae) {
				System.err.println("No bottle salinity found.  Trying ctd_salinity");
				_salinityColumnIdx = findDependentValueColumnIdx("ctd_salinity");
			}
		}
		
		private LabSigmaThetaCalculator(StdUserDataArray dataset, double labTemp) {
			this(dataset);
			_labTemp = labTemp;
		}
		
		@Override
		public double getSigmaThetaForRow(int rowIdx) {
			if ( ! _dataset.isUsableIndex(_salinityColumnIdx)) {
				throw new IllegalStateException("Dependent value is not yet standardized.");
			}
			double sal = ((Double)_dataset.getStdVal(rowIdx, _salinityColumnIdx)).doubleValue();
			double density = fromTempAndSalinity(_labTemp, sal);
			return density;
		}

	}

}
