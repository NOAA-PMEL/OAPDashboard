/**
 * 
 */
package gov.noaa.pmel.dashboard.server.vocabularies;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * @author kamb
 *
 */
@Data
@SuperBuilder(toBuilder = true)
@Setter(AccessLevel.NONE)
@NoArgsConstructor
@AllArgsConstructor
public class NewVocabularyItemProposal {

    @JsonProperty("proposed_name")
    private String _proposedName;
    
    @JsonProperty("dataset_recordid")
    private String _recordId;
    
    @JsonProperty("user_id")
    private String _userId;
    
    @JsonProperty("user_email")
    private String _userEmail;
    
    @Builder.Default
    @JsonProperty("propose_date")
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss z", timezone="PST")
    private Date _proposedOn = new Date();
    
}
