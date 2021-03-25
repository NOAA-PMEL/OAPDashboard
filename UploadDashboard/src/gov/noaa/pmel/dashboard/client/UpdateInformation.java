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

    public DataColumnType newType;
    public ValueUpdater<String> updater;
    public UpdateInformation(DataColumnType newType, ValueUpdater<String> updater) {
        this.newType = newType;
        this.updater = updater;
    }

}
