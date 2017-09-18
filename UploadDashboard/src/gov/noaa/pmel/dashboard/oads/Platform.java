/**
 * 
 */
package gov.noaa.pmel.dashboard.oads;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

/**
 * @author kamb
 *
 */
@Data
@XmlRootElement
public class Platform {

	@XmlElement(name="Name")
	private String _name;
	
	@XmlElement(name="Type")
	private String _type;
}
