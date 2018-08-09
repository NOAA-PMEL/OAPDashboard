/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import gov.noaa.pmel.dashboard.actions.checker.ProfileDatasetChecker;
import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.dsg.DsgMetadata;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.dsg.DsgNcFile.DsgFileType;
import gov.noaa.pmel.dashboard.handlers.DataFileHandler;
import gov.noaa.pmel.dashboard.handlers.DsgNcFileHandler;
import gov.noaa.pmel.dashboard.oads.DashboardOADSMetadata;
import gov.noaa.pmel.dashboard.oads.OADSMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.DataColumnType;
import lombok.Builder;

/**
 * @author kamb
 *
 */
public class EditFlagsService extends HttpServlet {

    @Builder
    private static class QcChange {
        String profileId;
        String sampleId;
        Character qcFlagValue;
        Double lat;
        Double lon;
        Double depth;
        String date;
        Double dataValue;
    }
    
    private static class UpdateBundle {
        String datasetId;
        String dataVarName;
        String qcFlagName;
        Collection<QcChange> changes;
        
        UpdateBundle(String datasetId) {
            this.datasetId = datasetId;
        }
        UpdateBundle(String datasetId, String dataVarName, String qcFlagName) {
            this(datasetId);
            this.dataVarName = dataVarName;
            this.qcFlagName = qcFlagName;
        }
    }
    
    private static final long serialVersionUID = 1L;
    
    private static Logger logger = LogManager.getLogger(EditFlagsService.class);

    private DataFileHandler _dataFileHandler;
    
    public EditFlagsService() {
        try {
            _dataFileHandler = DashboardConfigStore.get(false).getDataFileHandler();
        } catch (IOException ex) {
            logger.warn("Failed to instantiate DataFileHandler.", ex);
            ex.printStackTrace();
        }
    }
    
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.debug(request.getRemoteAddr() + " : " + request);
        System.out.println(request.getRemoteAddr() + " : " + request);
        response.getOutputStream().write(request.toString().getBytes());
        response.flushBuffer();
    }
    
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        logger.debug(request.getRemoteAddr() + " : " + request);
        String remoteAddr = request.getRemoteAddr();
        
        if ( ! isAcceptedSender(remoteAddr)) {
            response.sendError(SC_FORBIDDEN);
            return;
        }
        
        StringBuilder mbuf = new StringBuilder();
        BufferedReader reader = request.getReader();
        String postMsg;
        JsonObject jsonMsg;
        try {
	        postMsg = reader.lines().collect(Collectors.joining());
        } catch (Exception ex) {
            logger.warn("Error reading POST data.", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading POST data.");
            return;
        } finally {
            reader.close();
        }
        try {
	        jsonMsg = (JsonObject) new JsonParser().parse(postMsg);
	        UpdateBundle update = processJsonMsg(jsonMsg);
	        updateQcFlags(update);
        } catch (JsonParseException jpe) {
            logger.warn("Parse error for msg: "+ postMsg, jpe);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad JSON msg: " + postMsg);
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error updating flags:"+ex.getMessage());
        }
    }

    private boolean isAcceptedSender(String remoteAddr) { // XXX configurable
        return "127.0.0.1".equals(remoteAddr) ||
               "161.55.168.116".equals(remoteAddr);
    }

    private static UpdateBundle processJsonMsg(JsonObject jsonMsg) {
        String datasetId = jsonMsg.get("DATASET_ID").getAsString();
        String dataVarName = jsonMsg.get("DATA_VAR").getAsString();
        String qcFlagName = jsonMsg.get("FLAG_VAR").getAsString();
        UpdateBundle updates = new UpdateBundle(datasetId, dataVarName, qcFlagName);
        Collection<QcChange>changes = new ArrayList<>();
        updates.changes = changes;
        JsonArray edits = (JsonArray)jsonMsg.get("changes");
        for (JsonElement e : edits) {
            JsonObject jo = (JsonObject) e;
            QcChange.QcChangeBuilder qc = QcChange.builder();
            qc.profileId(jo.get("PROFILE").getAsString())
              .dataValue(jo.get(dataVarName).getAsDouble())
              .qcFlagValue(jo.get(qcFlagName).getAsCharacter())
              .sampleId(jo.get("SAMPLE_ID").getAsString())
              .date(jo.get("DATE").getAsString())
              .lat(jo.get("LATITUDE").getAsDouble())
              .lon(jo.get("LONGITUDE").getAsDouble())
              .depth(jo.get("DEPTH").getAsDouble())
              ;
            changes.add(qc.build());
        }
        return updates;
    }
    
    private void updateQcFlags(UpdateBundle update) throws IOException {
        DashboardConfigStore cfg = DashboardConfigStore.get(false);
        String datasetId = update.datasetId;
        DashboardDatasetData ddd = _dataFileHandler.getDatasetDataFromFiles(datasetId, 0, -1);
        StdUserDataArray preStdUsr = cfg.getDashboardDatasetChecker().standardizeDataset(ddd, null);
        int flagColIdx = getColumnIdx(update.qcFlagName, preStdUsr, ddd);
        for (QcChange change : update.changes) {
            Integer rowNum = Integer.valueOf(change.sampleId);
            int rowIdx = rowNum.intValue()-1;
            ArrayList<String> row = ddd.getDataValues().get(rowIdx);
            verifyRow(row, change);
            row.set(flagColIdx, String.valueOf(change.qcFlagValue));
        }
        String msg = "Updated qc flags.";
        _dataFileHandler.saveDatasetDataToFile(ddd, msg);
        _dataFileHandler.saveDatasetInfoToFile(ddd, msg);
        
        // redo the standardization after changes
        StdUserDataArray stdUsr = cfg.getDashboardDatasetChecker().standardizeDataset(ddd, null);
        DsgNcFileHandler dsgHandler = cfg.getDsgNcFileHandler();
        DashboardOADSMetadata oadsMd = OADSMetadata.extractOADSMetadata(stdUsr);
        DsgMetadata dsgMData = oadsMd.createDsgMetadata();
        dsgMData.setVersion("1.0");
        dsgHandler.saveDatasetDsg(dsgMData, stdUsr);
        dsgHandler.flagErddap(true);
    }

    private static int getColumnIdx(String qcFlagName, StdUserDataArray preStdUsr, DashboardDatasetData ddd) {
        DashDataType<?> columnType = preStdUsr.findDataColumn(qcFlagName);
        ArrayList<DataColumnType>ddCols = ddd.getDataColTypes();
        int usrColIdx = -1;
        for (int i = 0; i < ddCols.size(); i++) {
            DataColumnType ddCol = ddCols.get(i);
            if ( columnType.typeNameEquals(ddCol)) {
                usrColIdx = i;
                break;
            }
        }
        return usrColIdx;
    }

    private boolean verifyRow(ArrayList<String> row, QcChange change) throws IllegalArgumentException {
        return true;
    }
    
    public static void main(String[] args) {
        try {
            EditFlagsService efs = new EditFlagsService();
            File updateMsgFile = new File("/Users/kamb/oxy-work/oap_qc_las/data/test/editFlagsX.json");
            String updateMsg = FileUtils.readFileToString(updateMsgFile);
            JsonObject updateJson = (JsonObject) new JsonParser().parse(updateMsg);
            UpdateBundle update = processJsonMsg(updateJson);
            efs.updateQcFlags(update);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }
    }
}
