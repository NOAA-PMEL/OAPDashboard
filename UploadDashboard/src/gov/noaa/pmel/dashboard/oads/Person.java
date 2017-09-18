/**
 * 
 */
package gov.noaa.pmel.dashboard.oads;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Data;

/**
 * @author kamb
 *
 */
@XmlRootElement(name="Person")
@XmlType(propOrder={"fullName", "_organization", "_addr1", "_addr2", "_phone", "_email"})
public @Data class Person {
	private String _firstName;
	private String _lastName;
	@XmlElement(name="name")
	public String getFullName() { return _firstName + " " + _lastName; }
	@XmlElement(name="organization")
	private String _organization;
	@XmlElement(name="deliverypoint1")
	private String _addr1;
	@XmlElement(name="deliverypoint2")
	private String _addr2;
	@XmlElement(name="phone")
	private String _phone;
	@XmlElement(name="email")
	private String _email;
}
