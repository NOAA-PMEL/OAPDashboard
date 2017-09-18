/**
 * 
 */
package gov.noaa.pmel.dashboard.oads;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.gwt.user.client.rpc.IsSerializable;

import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kamb
 *
 */

@JsonRootName(value="metadata")
@XmlRootElement(name="metadata")
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder={"expoCode", "submissionDateString", "_submitter", "_startDate", "_endDate", 
					"_westernBound", "_easternBound", "_northernBound", "_southernBound", "_variables"
					})
@EqualsAndHashCode(callSuper=true)
public @Data class DashboardOADSMetadata extends DashboardMetadata  implements Serializable, IsSerializable {

	@XmlElement(name="datasetId")
	public String getExpoCode() { return super.getDatasetId(); }
	
//	private Date _submissionDate;
	@JsonGetter("submissiondate")
	@XmlElement(name="submissiondate")
	public String getSubmissionDateString() {  return super.getUploadTimestamp(); }
	public void setSubmissionDateString(String uploadTimeString) { super.setUploadTimestamp(uploadTimeString); }
	@JsonProperty("datasubmitter")
	@XmlElement(name="datasubmitter")
	private Person _submitter;
	
	private List<Person> _investigators;
	private List<Organization> _organizations;
	
	@JsonProperty("startdate")
	@XmlElement(name="startdate")
	private Date _startDate;
	@JsonProperty("enddate")
	@XmlElement(name="enddate")
	private Date _endDate;
	@JsonProperty("westbd")
	@XmlElement(name="westbd")
	private double _westernBound;
	@JsonProperty("eastbd")
	@XmlElement(name="eastbd")
	private double _easternBound;
	@JsonProperty("northbd")
	@XmlElement(name="northbd")
	private double _northernBound;
	@JsonProperty("southbd")
	@XmlElement(name="southbd")
	private double _southernBound;
	
	private Platform _vessel;
	
	@XmlElementRef(type=Variable.class)
	private List<Variable> _variables;
	
	@SuppressWarnings("unused")
	private DashboardOADSMetadata() {
		// For xml binding
	}
	
	public DashboardOADSMetadata(String datasetId) {
		super(datasetId);
		super.setFilename(DashboardUtils.metadataFilename(datasetId));
		initCollections();
	}
	
	private void initCollections() {
		_investigators = new ArrayList<>();
		_organizations = new ArrayList<>();
		_variables = new ArrayList<>();
	}
	
	public boolean addInvestigator(Person pi) {
		return _investigators.add(pi);
	}
	
	public boolean addOrganization(Organization org) {
		return _organizations.add(org);
	}

	public boolean addVariable(Variable var) {
		return _variables.add(var);
	}
	
//	// XXX TODO:
//	public DashboardOADSMetadata(DashboardMetadata metadataInfo, MetadataFileHandler mdataHandler) {
//		this(metadataInfo.getDatasetId());
//		File mdFile = mdataHandler.getMetadataFile(datasetId, filename);
//		assignFromXmlFile(mdFile);
//	}
//
//	private void assignFromXmlFile(File mdFile) {
//		// TODO Auto-generated method stub
//		
//	}

	/**
	 * Create a DsgMetadata object from the data in this object.
	 * Any PI or platform names will be anglicized.  
	 * The version status and QC flag are not assigned.
	 * 
	 * @return
	 *		created DsgMetadata object 
	 */
	public DsgMetadata createDsgMetadata() throws IllegalArgumentException {

		// We cannot create a DsgMetadata object if there are conflicts
		if ( isConflicted() ) {
			throw new IllegalArgumentException("The Metadata contains conflicts");
		}

		DashboardConfigStore confStore;
		try {
			confStore = DashboardConfigStore.get(false);
		} catch ( Exception ex ) {
			throw new RuntimeException("Unexpected failure to get the configuration information");
		}
		DsgMetadata scMData = new DsgMetadata(confStore.getKnownMetadataTypes());
		
		scMData.setDatasetId(datasetId);
		// XXX TODO: OME_FILENAME check
//		scMData.setDatasetName(getExperimentName());

		// Anglicize the platform name for NetCDF/LAS
		Platform vessel = vessel();
		if ( vessel != null ) {
			String platformName = vessel.name();
			scMData.setPlatformName(anglicizeName(platformName));
			// Set the platform type - could be missing
			try {
				scMData.setPlatformType(vessel.type());
			} catch ( Exception ex ) {
				scMData.setPlatformType(null);
			}
		}

		try {
			scMData.setWestmostLongitude(Double.valueOf(westernBound()));
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setWestmostLongitude(null);				
		}

		try {
			scMData.setEastmostLongitude(Double.valueOf(easternBound()));
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setEastmostLongitude(null);
		}

		try {
			scMData.setSouthmostLatitude(Double.valueOf(southernBound()));
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setSouthmostLatitude(null);
		}

		try {
			scMData.setNorthmostLatitude(Double.valueOf(northernBound()));
		} catch (NumberFormatException | NullPointerException ex) {
			scMData.setNorthmostLatitude(null);
		}
		
//		SimpleDateFormat dateParser = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
//		dateParser.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			scMData.setBeginTime(Double.valueOf(startDate().getTime() / 1000.0));
		} catch (Exception ex) {
			scMData.setBeginTime(null);
		}
		try {
			scMData.setEndTime(Double.valueOf(endDate().getTime() / 1000.0));
		} catch (Exception ex) {
			scMData.setEndTime(null);
		}

		StringBuffer piNames = new StringBuffer();
		if ( _investigators != null ) {
			for ( Person investigator : investigators() ) {
				if ( piNames.length() > 0 )
					piNames.append(DsgMetadata.NAMES_SEPARATOR);
				// Anglicize investigator names for NetCDF/LAS
				piNames.append(anglicizeName(investigator.getFullName()));
			}
			scMData.setInvestigatorNames(piNames.toString());
		}

		HashSet<String> usedOrganizations = new HashSet<String>();
		StringBuffer orgGroup = new StringBuffer();
		if ( _organizations != null ) {
			for ( Organization org : organizations() ) {
				if ( org == null )
					continue;
				String orgName = org.name();
				orgName = orgName.trim();
				if ( orgName.isEmpty() )
					continue;
				if ( usedOrganizations.add(orgName) ) {
					if ( orgGroup.length() > 0 )
						orgGroup.append(DsgMetadata.NAMES_SEPARATOR);
					// Anglicize organizations names for NetCDF/LAS
					orgGroup.append(anglicizeName(orgName));
				}
			}
		}
		String allOrgs = orgGroup.toString().trim();
		if ( allOrgs.isEmpty() )
			allOrgs = "unassigned";
		scMData.setOrganizationName(allOrgs);

		return scMData;
	}
	
	// XXX TODO: do we need this?
	private String anglicizeName(String name) {
		return name;
	}

}
