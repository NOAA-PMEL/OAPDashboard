/**
 * 
 */
package gov.noaa.pmel.dashboard.server.submission.status;

/**
 * @author kamb
 *
 */
public enum StatusMessageFlag {
    
    ACCESSION("a", "accesssion"),
    MESSAGE("m", "message"),
    STATUS("s", "status"),
    VERSION("n", "version");
    
    private String _qpFlag;
    private String _formField;

    private StatusMessageFlag(String qpFlag, String formField) {
        _qpFlag = qpFlag;
        _formField = formField;
    }
    
    public String queryFlag() { return _qpFlag; }
    public String formField() { return _formField; }
}
