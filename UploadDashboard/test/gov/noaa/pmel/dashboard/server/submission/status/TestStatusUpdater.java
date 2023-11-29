/**
 * 
 */
package gov.noaa.pmel.dashboard.server.submission.status;

/**
 * @author kamb
 *
 */
public class TestStatusUpdater {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String datasetId = "BEPK93C2C";
        StatusState sstate = StatusState.RECEIVED;
        String message = "Package received by NCEI";
        try {
            StatusUpdater.updateStatusRecord(datasetId, sstate, message);
            StatusUpdater.updateStatusRecord(datasetId, StatusState.INCOMPLETE, "Incomplete info");
            StatusUpdater.updateStatusRecord(datasetId, StatusState.RECEIVED, "Recieved");
            StatusUpdater.updateStatusRecord(datasetId, StatusState.PENDING_INFO, "Pending more info");
            StatusUpdater.updateStatusRecord(datasetId, StatusState.RECEIVED, "Recieved");
            StatusUpdater.updateStatusRecord(datasetId, StatusState.VALIDATED, "Validating your dreams");
            StatusUpdater.updateStatusRecord(datasetId, StatusState.ACCEPTED, "accession:1234567");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}
