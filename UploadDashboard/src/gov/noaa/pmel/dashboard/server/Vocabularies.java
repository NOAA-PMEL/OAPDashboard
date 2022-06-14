/**
 * 
 */
package gov.noaa.pmel.dashboard.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.noaa.ncei.oads.xml.v_a0_2_2.BaseVariableType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.OadsMetadataDocumentType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PersonType;
import gov.noaa.ncei.oads.xml.v_a0_2_2.PlatformType;
import gov.noaa.pmel.dashboard.server.model.User;
import gov.noaa.pmel.dashboard.server.util.Notifications;
import gov.noaa.pmel.dashboard.server.vocabularies.NewVariableProposal;
import gov.noaa.pmel.dashboard.server.vocabularies.NewVariableProposal.NewVariableProposalBuilder;
import gov.noaa.pmel.dashboard.server.vocabularies.NewVocabularyItemProposal;
import gov.noaa.pmel.dashboard.server.vocabularies.NewVocabularyItemProposal.NewVocabularyItemProposalBuilder;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.Logging;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public class Vocabularies {
    
	private static final Logger logger = Logging.getLogger(NotificationService.class);
    
//    private static Map<VOCABULARIES, Map<String, String>> existingVocabs;
    private static Set<VOCABULARY> existingVocabs = Collections.synchronizedSet(new HashSet<>());
    private static Map<VOCABULARY, File> existingVocabFiles = new HashMap<>();
    private static Map<VOCABULARY, File> proposedVocabFiles = new HashMap<>();
    
    static enum VOCABULARY {
        NCEI_VARIABLES("variables/ncei") {
            @Override
            Class<? extends NewVocabularyItemProposal> proposalClass() { return NewVariableProposal.class; }
            @Override
            NewVariableProposalBuilder<?,?> builder() { return NewVariableProposal.builder(); }
        },
        NCEI_PLATFORMS("platforms/ncei"),
        NCEI_INSTRUMENTS("instruments/ncei"),
        NCEI_INSTITUTIONS("institutions/ncei");
        
        private String _path;
        Date _lastLoad = null;
        Map<String, String> _existing = null;
        Map<String, NewVocabularyItemProposal> _proposed = null;
        
        private VOCABULARY(String path) {
            _path = path;
        }
        String path() { return _path; }
        Date lastLoad() { return _lastLoad; }
        String fileName() { return this.name().toLowerCase()+".txt"; }
        String proposedFileName() { return this.name().toLowerCase()+".proposed"; }
        synchronized Map<String, String> existing() {
            if ( _existing == null ) { _existing = new HashMap<>(); }
            return _existing;
        }
        synchronized Map<String, NewVocabularyItemProposal> proposed() {
            if ( _proposed == null ) { _proposed = new HashMap<>(); }
            return _proposed;
        }
        @SuppressWarnings("static-method")
        Class<? extends NewVocabularyItemProposal> proposalClass() { return NewVocabularyItemProposal.class; }
        NewVocabularyItemProposal readProposal(String json) throws Exception {
            NewVocabularyItemProposal proposal = new ObjectMapper().readValue(json, proposalClass());
            return proposal;
        }
        NewVocabularyItemProposalBuilder<?,?> builder() { return NewVocabularyItemProposal.builder(); }
    }
    
    public static void checkForVocabularyAdditions(DashboardDataset dataset, 
                                                   OadsMetadataDocumentType metadata) {
        User user = null;
        try {
            user = Users.getUser(dataset.getOwner());
        } catch (DashboardException ex) {
            logger.warn("Exception getting user:"+ex, ex);
        }
        String userId = user != null ? user.username() : "N/A";
        String userEmail = user != null ? user.email() : "N/A";
        checkVariableNames(dataset, metadata, userId, userEmail);
        checkInstitutions(dataset, metadata, userId, userEmail);
        checkInstruments(dataset, metadata, userId, userEmail);
        checkPlatforms(dataset, metadata, userId, userEmail);
    }
    
    public static Map<String, String> getExisting(VOCABULARY vocab) {
        synchronized (vocab) {
            if ( needsReload(vocab)) {
                loadVocabulary(vocab);
            }
        } 
        return vocab.existing();
    }
    public static Map<String, NewVocabularyItemProposal> getProposed(VOCABULARY vocab) {
        synchronized (vocab) {
            if ( needsReload(vocab)) {
                loadVocabulary(vocab);
            }
        } 
        return vocab.proposed();
    }
    /**
     * @param vocab
     * @return
     */
    private static boolean needsReload(VOCABULARY vocab) {
        File existingFile = getExistingVocabularyFile(vocab);
        Date lastMod = new Date(existingFile.lastModified());
        return (vocab.existing().isEmpty()) || 
                lastMod.after(vocab.lastLoad());
    }

    /**
     * @param vocab
     */
    private static void loadVocabulary(VOCABULARY vocab) {
        logger.debug("Loading vocabulary: " + vocab);
        loadExistingVocabulary(vocab);
        loadAlreadyProposed(vocab);
    }

    /**
     * @param dataset
     * @param metadata
     * @param user 
     */
    public static void checkVariableNames(DashboardDataset dataset, 
                                          OadsMetadataDocumentType metadata, 
                                          String userId, String userEmail) {
        Set<NewVocabularyItemProposal> added = new HashSet<>();
        VOCABULARY vocab = VOCABULARY.NCEI_VARIABLES;
        if ( metadata.getVariables() == null ) { return; }
        for (BaseVariableType var : metadata.getVariables()) {
            String varName = var.getFullName();
            String stdName = stdName(varName);
            String keyName = keyName(varName);
            if ( ! getExisting(VOCABULARY.NCEI_VARIABLES).containsKey(keyName)) {
                continue;
            }
            if ( ! getProposed(VOCABULARY.NCEI_VARIABLES).containsKey(keyName)) {
                added.add(NewVariableProposal.builder()
                          .proposedName(stdName)
                          .columnName(var.getDatasetVarName())
                          .recordId(dataset.getRecordId())
                          .userEmail(userEmail)
                          .userId(userId)
                          .build());
            }
        }
        if ( ! added.isEmpty() ) {
            if ( addProposedItems(vocab, added, dataset)) {
                scheduleSendVocabItemsAddedNotification(VOCABULARY.NCEI_VARIABLES, 
                                                        dataset.getOwner(), dataset.getRecordId(), 
                                                        added);
            }
        }
    }
    
    /**
     * @param dataset
     * @param metadata
     */
    private static void checkInstitutions(DashboardDataset dataset, 
                                          OadsMetadataDocumentType metadata, 
                                          String userId, String userEmail) {
        Set<NewVocabularyItemProposal> added = new HashSet<>();
        VOCABULARY vocab = VOCABULARY.NCEI_INSTITUTIONS;
        PersonType submitter = metadata.getDataSubmitter();
        if ( submitter != null ) { 
            List<String> orgs = submitter.getOrganizations();
            if ( orgs != null ) { 
                for (String org : orgs) {
                    if ( ! getExisting(vocab).keySet().contains(keyName(org))) {
                        added.add(vocab.builder()
                                  .proposedName(stdName(org))
                                  .recordId(dataset.getRecordId())
                                  .userId(userId)
                                  .userEmail(userEmail)
                                  .build());
                    }
                }
            }
        }
        if ( metadata.getInvestigators() == null ) { return; }
        for (PersonType pi : metadata.getInvestigators()) {
            List<String> orgs = pi.getOrganizations();
            if ( orgs == null ) { return; }
            for (String org : orgs) {
                if ( ! getExisting(vocab).keySet().contains(keyName(org))) {
                    added.add(vocab.builder()
                              .proposedName(stdName(org))
                              .recordId(dataset.getRecordId())
                              .userId(userId)
                              .userEmail(userEmail)
                              .build());
                }
            }
        }
        if ( ! added.isEmpty() ) {
            if ( addProposedItems(vocab, added, dataset)) {
                scheduleSendVocabItemsAddedNotification(VOCABULARY.NCEI_INSTITUTIONS, 
                                                        dataset.getOwner(), dataset.getRecordId(), 
                                                        added);
            }
        }
    }
    /**
     * @param dataset
     * @param metadata
     */
    private static void checkInstruments(DashboardDataset dataset, 
                                         OadsMetadataDocumentType metadata,
                                         String userId, String userEmail) {
        Set<NewVocabularyItemProposal> added = new HashSet<>();
        VOCABULARY instruments = VOCABULARY.NCEI_INSTRUMENTS;
        if ( metadata.getVariables() == null ) { return; }
        for (BaseVariableType var : metadata.getVariables()) {
            String sampler = var.getSamplingInstrument();
            if ( ! StringUtils.emptyOrNull(sampler) &&
                 ! getExisting(instruments).containsKey(keyName(sampler))) {
                added.add(instruments.builder()
                          .proposedName(stdName(sampler))
                          .recordId(dataset.getRecordId())
                          .userId(userId)
                          .userEmail(userEmail)
                          .build());
            }
            String analyzer = var.getAnalyzingInstrument();
            if ( ! StringUtils.emptyOrNull(analyzer) &&
                 ! getExisting(instruments).containsKey(keyName(analyzer))) {
                added.add(instruments.builder()
                          .proposedName(stdName(analyzer))
                          .recordId(dataset.getRecordId())
                          .userId(userId)
                          .userEmail(userEmail)
                          .build());
            }
        }
        if ( ! added.isEmpty() ) {
            if ( addProposedItems(instruments, added, dataset)) {
                scheduleSendVocabItemsAddedNotification(VOCABULARY.NCEI_INSTRUMENTS, 
                                                        dataset.getOwner(), dataset.getRecordId(), 
                                                        added);
            }
        }
    }
    /**
     * @param dataset
     * @param metadata
     */
    private static void checkPlatforms(DashboardDataset dataset, 
                                       OadsMetadataDocumentType metadata,
                                       String userId, String userEmail) {
        Set<NewVocabularyItemProposal> added = new HashSet<>();
        Map<String, String> existing = VOCABULARY.NCEI_PLATFORMS.existing();
        if ( metadata.getPlatforms() == null ) { return; }
        for ( PlatformType platform : metadata.getPlatforms()) {
            String pname = platform.getName();
            if ( StringUtils.emptyOrNull(pname)) { continue; }
            String pid = platform.getIdentifier().getValue();
            String pkey = platformNameIdString(pname, pid);
            if ( ! existing.containsKey(keyName(pkey))) {
                added.add(VOCABULARY.NCEI_PLATFORMS.builder()
                          .proposedName(stdName(pkey))
                          .recordId(dataset.getRecordId())
                          .userId(userId)
                          .userEmail(userEmail)
                          .build());
            }
        }
        if ( ! added.isEmpty() ) {
            if ( addProposedItems(VOCABULARY.NCEI_PLATFORMS, added, dataset)) {
                scheduleSendVocabItemsAddedNotification(VOCABULARY.NCEI_PLATFORMS, 
                                                        dataset.getOwner(), 
                                                        dataset.getRecordId(), added);
            }
        }
    }

    /**
     * @param pname
     * @param pid
     * @return
     */
    private static String platformNameIdString(String pname, String pid) {
        StringBuilder pniStr = new StringBuilder(pname.trim());
        if ( ! StringUtils.emptyOrNull(pid)) {
            pniStr.append(" (").append(pid.trim()).append(")");
        }
        return pniStr.toString();
    }

    /**
     * @param added
     */
    private static void scheduleSendVocabItemsAddedNotification(final VOCABULARY vocab,
                                                                final String userId, String recordId,
                                                                final Collection<NewVocabularyItemProposal> added) {
        Runnable notifier = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Retriver running.");
                    Thread.sleep(500);
                    sendItemsAddedNotification(vocab, userId, recordId, added);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        new Thread(notifier).start();
    }
    private static void sendItemsAddedNotification(VOCABULARY vocab, 
                                                   String userId, String recordId, 
                                                   Collection<NewVocabularyItemProposal> added) {
        String notificationList = ApplicationConfiguration.getProperty("oap.vocabs.email.list", "linus.kamb@noaa.gov");
        if ( StringUtils.emptyOrNull(notificationList)) {
            return;
        }
        StringBuilder msgBldr = new StringBuilder("New items have been proposed for " + vocab.name() + " by " + 
                                                   userId + " in dataset " + recordId + "\n");
        for (NewVocabularyItemProposal newItem : added) {
            msgBldr.append(" - ")
                   .append(newItem.proposedName())
                   .append("\n");
        }
        Notifications.SendEmailIf("SDIS: New " + vocab + " items proposed.",
                                   msgBldr.toString(), notificationList);
    }

    /**
     * @param existing
     * @throws Exception 
     */
    private static void writeExistingVariables(Map<String, String> existing) throws Exception {
        File existingFile = new File(VOCABULARY.NCEI_VARIABLES.fileName());
        if ( existingFile.exists()) {
            String datestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File backup = new File(existingFile.getParentFile(), existingFile.getName()+"."+datestamp);
            existingFile.renameTo(backup);
        }
        try ( PrintWriter writer = new PrintWriter(new FileWriter(existingFile))) {
            SortedSet<String> sortedKeys = new TreeSet<>(existing.keySet());
            for (String key : sortedKeys ) {
                writer.println(key+"="+existing.get(key));
            }
        }
        
    }

    /**
     * @return
     */
    static void loadExistingVocabulary(VOCABULARY vocab) {
        Map<String, String> existing = vocab.existing();
        existing.clear();
        File vocabFile = getExistingVocabularyFile(vocab);
        logger.info("Loading existing variables from : " + vocabFile.getAbsolutePath());
        try ( BufferedReader reader = new BufferedReader(new FileReader(vocabFile)); ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("[:=]");
                if ( parts.length == 1) {
                    existing.put(keyName(parts[0]), stdName(parts[0]));
                } else {
                    existing.put(keyName(parts[0]), stdName(parts[1]));
                }
            }
            vocab._lastLoad = new Date();
        } catch (Exception ex) {
            logger.warn("Failed to load existing variables", ex);
        }
    }
    static File getExistingVocabularyFile(VOCABULARY vocab) {
        synchronized (existingVocabFiles) {
            if ( ! existingVocabFiles.containsKey(vocab)) {
                File dir = getVocabularyDir(vocab);
                File existingVocabFile = new File(dir, vocab.fileName());
                logger.debug(vocab + " existing file: " + existingVocabFile.getAbsolutePath());
                existingVocabFiles.put(vocab, existingVocabFile);
            }
        }
        return existingVocabFiles.get(vocab);
    }
    /**
    private static void loadProposedVariables() {
        proposedVariableFile = getProposedVariableFile();
        if ( proposedVariableFile.exists()) {
            try ( BufferedReader reader = new BufferedReader(new FileReader(proposedVariableFile)); ) {
                ObjectMapper json = new ObjectMapper();
                Properties vars = new Properties();
                vars.load(reader);
                for (Object key : vars.keySet()) {
                    String compName = (String)key;
                    String varDef = vars.getProperty(compName);
                    NewVariableProposal newV = json.readValue(varDef, NewVariableProposal.class);
                    proposedVariables.put(compName, newV);
                }
            } catch (Exception ex) {
                logger.warn("Failed to read proposed variables file " + proposedVariableFile.getAbsolutePath() +
                            " : " + ex);
            }
        }
    }
     */
    static void loadAlreadyProposed(VOCABULARY vocab) {
        Map<String, NewVocabularyItemProposal> proposed = vocab.proposed();
        proposed.clear();
        File vocabFile = getProposedVocabularyFile(vocab);
        if ( !vocabFile.exists()) {
            logger.info("No proposed items file for " + vocab);
            return;
        }
        logger.info("Loading existing proposals from : " + vocabFile.getAbsolutePath());
        try ( BufferedReader reader = new BufferedReader(new FileReader(vocabFile)); ) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eqIdx = line.indexOf('=');
                if ( eqIdx <= 0 ||
                     eqIdx == line.length()-1) {
                    logger.warn("Bad line (index) in proposed file:" + vocabFile.getAbsolutePath() + " : " + line);
                    continue;
                }
                String key = line.substring(0, eqIdx);
                String json = line.substring(eqIdx+1);
                try {
                    proposed.put(keyName(key), vocab.readProposal(json));
                } catch (Exception ex) {
                    logger.warn("Bad line (content) in proposed file:" + vocabFile.getAbsolutePath() + " : " + line);
                }
            }
        } catch (Exception ex) {
            logger.warn("Exception loading proposed vocab " + vocab, ex);
        }
    }
    static File getProposedVocabularyFile(VOCABULARY vocab) {
        synchronized (proposedVocabFiles) {
            if ( ! proposedVocabFiles.containsKey(vocab)) {
                File dir = getVocabularyDir(vocab);
                File proposedVocabFile = new File(dir, vocab.proposedFileName());
                logger.debug(vocab + " proposed file: " + proposedVocabFile.getAbsolutePath());
                proposedVocabFiles.put(vocab, proposedVocabFile);
            }
        }
        return proposedVocabFiles.get(vocab);
    }
    
    /** Pattern for {@link #getDatasetIDFromName(string)} */
    private static final Pattern lowercaseStripPattern = Pattern.compile("[^\\p{javaLowerCase}\\p{Digit}]+");

    /**
     * Returns the dataset ID for the given dataset / cruise name by converting
     * characters in the name to uppercase and ignoring anything that is not 
     * uppercase of a digit.  The value returned is equivalent to 
     * <pre>name.toUpperCase().replaceAll("[^\p{javaUpperCase}\p{Digit}]+", "")</pre>
     * 
     * @param name
     *      dataset name
     * @return
     *      dataset ID for the given dataset name
     */
    public static String getDatasetIDFromName(String name) {
        return lowercaseStripPattern.matcher(name.trim().toLowerCase()).replaceAll("");
    }

    /**
     * @param varName
     * @return
     */
    public static String keyName(String varName) {
        return varName.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    /**
     * @param varName
     * @return
     */
    public static String stdName(String varName) {
        return varName.trim().replaceAll("\\s+", " ");
    }


    /**
     * @return
    private static File getProposedVariableFile() {
        File dir = getVocabularyDir(VOCABULARIES.NCEI_VARIABLES);
        File proposedVariableFile = new File(dir, "ncei_proposed_additions.txt");
        return proposedVariableFile;
    }
     */

    /**
     * @return
     */
    private static File getVocabulariesRoot() {
        File contentRoot = DashboardConfigStore.getContentRoot();
        File dir = new File(contentRoot, "MetadataEditor/vocabularies");
        String cwd = System.getProperty("user.dir");
        if ( !dir.exists()) {
            if ( ! dir.mkdirs()) {
                logger.warn("Failed to create vocabulary dir: " + dir.getAbsolutePath());
                dir = new File(cwd);
                logger.warn("Using default current working directory: " + dir.getAbsolutePath());
            }
        }
        return dir;
    }
    private static File getVocabularyDir(VOCABULARY vocabulary) {
        File vocabularyRoot = getVocabulariesRoot();
        File dir = new File(vocabularyRoot, vocabulary.path());
        if ( !dir.exists()) {
            if ( ! dir.mkdirs()) {
                throw new RuntimeException("Failed to create vocabulary dir: " + dir.getAbsolutePath());
            }
        }
        return dir;
    }

    /**
     * @param var
     * @param dataset
    static boolean addProposedVariable(BaseVariableType var, DashboardDataset dataset) {
        try {
            return addProposedVariable(stdName(var.getFullName()), var.getDatasetVarName(), var.getUnits(), 
                                       dataset.getRecordId(), dataset.getOwner());
        } catch (Exception ex) {
            logger.warn("Failed to add proposed variable: " + var 
                        + " for dataset: " + dataset.getRecordId(), ex);
            return false;
        }
    }
     */
    
    static boolean itemIsKnown(String itemName, VOCABULARY inVocab) {
        return getExisting(inVocab).containsKey(keyName(itemName)) ||
               getProposed(inVocab).containsKey(keyName(itemName));
    }
    
    static NewVariableProposal getProposedVariableFor(String vFullName) {
        String compName = keyName(vFullName);
        NewVariableProposal newV = (NewVariableProposal)getProposed(VOCABULARY.NCEI_VARIABLES).get(compName);
        return newV;
    }
    static boolean addProposedItems(VOCABULARY vocab, 
                                    Collection<NewVocabularyItemProposal> proposed,
                                    DashboardDataset dataset)                                     
//        throws Exception 
    {
        boolean added = false;
        File existingFile = getExistingVocabularyFile(vocab);
        File proposedFile = getProposedVocabularyFile(vocab);
        Map<String, String> existing = getExisting(vocab);
        Map<String, NewVocabularyItemProposal> alreadyProposed = getProposed(vocab);
        try ( PrintWriter existingWriter = new PrintWriter(new FileWriter(existingFile, true));
              PrintWriter proposedWriter = new PrintWriter(new FileWriter(proposedFile, true)); ) {
            ObjectMapper json = new ObjectMapper();
            for ( NewVocabularyItemProposal proposal : proposed ) {
                String stdName = stdName(proposal.proposedName());
                String keyName = keyName(proposal.proposedName());
                if ( alreadyProposed.containsKey(keyName)) {
                    logger.info("Varible already proposed for " + keyName + ": " + alreadyProposed.get(keyName));
                    continue;
                }
                logger.info("Adding to vocabulary " + vocab + ": " + proposal);
                alreadyProposed.put(keyName, proposal);
                proposedWriter.println(keyName+"="+json.writeValueAsString(proposal));
                existing.put(keyName, stdName);
                existingWriter.println(stdName);
                added = true;
            }
        } catch (Exception ex) {
            logger.warn("Exception writing proposed vocab " + vocab, ex);
            String msg = new StringBuilder()
                    .append("There was an exception adding proposed item to vocabulary ")
                    .append(vocab.name()).append(".\n")
                    .append(proposed.toString()).append(".\n\n")
                    .append(ex.toString()).toString();
            String to = ApplicationConfiguration.getProperty("oap.notification.list", "");
            Notifications.SendEmailIf("SDIS Vocabulary Exception: " + ex, msg, to);
        }
        return added;
    }
    static boolean addProposedVocabularyItem(VOCABULARY vocab, NewVocabularyItemProposal proposal) 
        throws Exception 
    {
        synchronized (vocab) {
            String proposedName = proposal.proposedName();
            String keyName = keyName(proposedName);
            Map<String, NewVocabularyItemProposal> proposed = getProposed(vocab);
            Map<String, String> existing = getExisting(vocab);
            if ( existing.containsKey(keyName)) {
                logger.info("Varible already exists for " + keyName + ": " + proposed.get(keyName));
                return false;
            }
            File proposedFile = getProposedVocabularyFile(vocab);
            if ( ! proposed.containsKey(keyName)) {
                try ( PrintWriter writer = new PrintWriter(new FileWriter(proposedFile, true)); ) {
                    ObjectMapper json = new ObjectMapper();
                    proposed.put(keyName, proposal);
                    writer.println(keyName+"="+json.writeValueAsString(proposal));
                }
            }
            File existingFile = getExistingVocabularyFile(vocab);
            try ( PrintWriter writer = new PrintWriter(new FileWriter(existingFile, true)); ) {
                writer.println(proposedName);
                existing.put(keyName, proposedName);
            }
            return true;
        }
    }
    static boolean addProposedVariable(String vFullName, String vColName, String vUnits,
                                       String recordId, String userId, String userEmail) 
        throws Exception 
    {
        String stdName = stdName(vFullName);
        NewVariableProposal newV = NewVariableProposal.builder()
                    .proposedName(stdName) 
                    .columnName(vColName)
                    .units(vUnits)
                    .recordId(recordId)
                    .userId(userId)
                    .userEmail(userEmail)
                    .build();
        return addProposedVocabularyItem(VOCABULARY.NCEI_VARIABLES, newV);
    }

}
