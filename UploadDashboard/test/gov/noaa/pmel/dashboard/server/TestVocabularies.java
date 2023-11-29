/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import gov.noaa.pmel.dashboard.server.vocabularies.NewVariableProposal;

/**
 * @author kamb
 *
 */
public class TestVocabularies {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            NewVariableProposal proposed = Vocabularies.getProposedVariableFor("ocean  viscosity");
            System.out.println("found " + proposed);
            testAddNewVariable("Synthetic Viscosity", "SIN_VIZ", "slime/L", "BEGYXYZ", "kamb", "linus.kamb@noaa.gov");
            proposed = Vocabularies.getProposedVariableFor("synthetic  viscosity");
            System.out.println("found " + proposed);
            testAddNewVariable("Ocean Slammity", "OC_VIZ", "slime/L", "BEGYXYZ", "kamb", "linus.kamb@noaa.gov");
            testAddNewVariable("ocean  viscosity", "OC_VIZ", "slime/L", "BEGYXYZ", "kamb", "linus.kamb@noaa.gov");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 
     */
    private static void testAddNewVariable(String varName, String colName, String units,
                                           String recordId, String userId, String userEmail) 
        throws Exception 
    {
        Vocabularies.addProposedVariable(varName, colName, units, recordId, userId, userEmail);
    }


}
