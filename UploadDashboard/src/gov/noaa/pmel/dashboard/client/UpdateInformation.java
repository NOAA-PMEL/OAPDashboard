/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

import com.google.gwt.cell.client.ValueUpdater;

import gov.noaa.pmel.dashboard.shared.DataColumnType;

/**
 * @author kamb
 *
 */
public class UpdateInformation {

    public int columnIdx;
    public DataColumnType newType;
    public ValueUpdater<String> updater;
    public UpdateInformation(int columnIdx, DataColumnType newType, ValueUpdater<String> updater) {
        this.columnIdx = columnIdx;
        this.newType = newType;
        this.updater = updater;
    }

}
