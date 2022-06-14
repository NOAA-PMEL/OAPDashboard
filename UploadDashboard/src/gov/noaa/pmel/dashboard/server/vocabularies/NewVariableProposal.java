/**
 * 
 */
package gov.noaa.pmel.dashboard.server.vocabularies;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author kamb
 *
 */
@Data
@Setter(AccessLevel.NONE)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewVariableProposal extends NewVocabularyItemProposal {

    @JsonProperty("column_name")
    private String _columnName;
    
    @JsonProperty("units")
    private String _units;
    
}
