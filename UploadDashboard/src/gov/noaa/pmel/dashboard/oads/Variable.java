/**
 * 
 */
package gov.noaa.pmel.dashboard.oads;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * @author kamb
 *
 */
/*
<variable>
  <fullname/>
  <abbrev/>
  <observationType/>
  <insitu/>
  <manipulationMethod/>
  <unit/>
  <measured/>
  <calcMethod/>
  <samplingInstrument/>
  <analyzingInstrument/>
  <storageMethod/>
  <seawatervol/>
  <headspacevol/>
  <temperatureMeasure/>
  <detailedInfo/>
  <replicate/>
  <gasDetector>
     <manufacturer/>
     <model/>
     <resolution/>
     <uncertainty/>
  </gasDetector>
  <standardization>
     <description/>
     <frequency/>
     <temperatureStd/>
     <standardgas>
        <manufacturer/>
        <concentration/>
        <uncertainty/>
     </standardgas>
  </standardization>
  <waterVaporCorrection/>
  <temperatureCorrection/>
  <co2ReportTemperature/>
  <uncertainty/>
  <flag/>
  <methodReference/>
  <researcherName/>
  <researcherInstitution/>
  <internal/>
</variable>
*/

@Data
@XmlRootElement(name="variable")
public class Variable {

	public static enum InSitu {
		InSitu("in-situ observation"),
		ManipCondition("manipulation condition"),
		RespVar("response variable");
		
		private String _value;
		private InSitu(String text) {
			_value = text;
		}
		@Override
		public String toString() {
			return _value;
		}
	}
	
	public static enum Measured {
		Measured,
		Calculated
	}
	@XmlElement(name="fullname")
	@JsonProperty("fullname")
	private String _fullName;
	
	@XmlElement(name="abbrev")
	@JsonProperty("abbrev")
	private String _abbreviation;
	
	@XmlElement(name="observationType")
	@JsonProperty("observationType")
	private String _observationType;
	
	@XmlElement(name="insitu")
	@JsonProperty("insitu")
	private InSitu _inSitu;
	
	@XmlElement(name="manipulationMethod")
	@JsonProperty("manipulationMethod")
	private String _manipulationMethod;
	
	@XmlElement(name="unit")
	@JsonProperty("unit")
	private String _unit;
	
	@XmlElement(name="measured")
	@JsonProperty("measured")
	private Measured _measured;
	
	@XmlElement(name="calcMethod")
	@JsonProperty("calcMethod")
	private String _calcMethod;
	
	@XmlElement(name="samplingInstrument")
	@JsonProperty("samplingInstrument")
	private String _samplingInstrument;

	@XmlElement(name="analyzingInstrument")
	@JsonProperty("analyzingInstrument")
	private String _analyzingInstrument;

	@XmlElement(name="storageMethod")
	@JsonProperty("storageMethod")
	private String _storageMethod;

	@XmlElement(name="seawatervol")
	@JsonProperty("seawatervol")
	private String _seawatervol;

	@XmlElement(name="headspacevol")
	@JsonProperty("headspacevol")
	private String _headspacevol;

	@XmlElement(name="temperatureMeasure")
	@JsonProperty("temperatureMeasure")
	private String _temperatureMeasure;

	@XmlElement(name="detailedInfo")
	@JsonProperty("detailedInfo")
	private String _detailedInfo;

	@XmlElement(name="replicate")
	@JsonProperty("replicate")
	private String _replicate;
	// private Instrument gasDetector;
	// private Standardization standardization;

	@XmlElement(name="waterVaporCorrection")
	@JsonProperty("waterVaporCorrection")
	private String _waterVaporCorrection;

	@XmlElement(name="temperatureCorrection")
	@JsonProperty("temperatureCorrection")
	private String _temperatureCorrection;

	@XmlElement(name="co2ReportTemperature")
	@JsonProperty("co2ReportTemperature")
	private String _co2ReportTemperature;

	@XmlElement(name="uncertainty")
	@JsonProperty("uncertainty")
	private String _uncertainty;

	@XmlElement(name="flag")
	@JsonProperty("flag")
	private String _flag;

	@XmlElement(name="methodReference")
	@JsonProperty("methodReference")
	private String _methodReference;

	@XmlElement(name="researcherName")
	@JsonProperty("researcherName")
	private String _researcherName;

	@XmlElement(name="researcherInstitution")
	@JsonProperty("researcherInstitution")
	private String _researcherInstitution;

	@XmlElement(name="internal")
	@JsonProperty("internal")
	private String _internal;
}
