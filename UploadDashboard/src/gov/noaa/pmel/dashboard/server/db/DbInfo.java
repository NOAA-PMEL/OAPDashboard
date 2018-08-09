/**
 * 
 */
package gov.noaa.pmel.dashboard.server.db;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.Value;

/**
 * @author kamb
 *
 */
@Value
public class DbInfo {
	
	@JsonProperty(value="db_id")
	protected Integer _dbId;
	
	@JsonProperty(value="create_time")
	protected Date _createTime;
	
	@JsonProperty(value="modified_time")
	protected Date _modifiedTime;

}
