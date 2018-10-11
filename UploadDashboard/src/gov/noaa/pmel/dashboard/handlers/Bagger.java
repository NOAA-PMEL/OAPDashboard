/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagMetadataException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.verify.BagVerifier;
import gov.loc.repository.bagit.writer.BagWriter;
import gov.noaa.pmel.dashboard.dsg.StdUserDataArray;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore.PropertyNotFoundException;
import gov.noaa.pmel.dashboard.shared.DashboardDataset;
import gov.noaa.pmel.dashboard.shared.DashboardDatasetData;
import gov.noaa.pmel.dashboard.shared.FeatureType;
import gov.noaa.pmel.tws.util.ApplicationConfiguration;
import gov.noaa.pmel.tws.util.FileUtils;
import gov.noaa.pmel.tws.util.StringUtils;

/**
 * @author kamb
 *
 */
public class Bagger implements ArchiveBundler {

    private String _datasetId;
    private FeatureType _featureType;
    private boolean _includeHiddenFiles = false;
    private File _contentRoot;
    private SimpleDateFormat _tsFormatter;
    private DashboardConfigStore _store;

    public static File Bag(String datasetId) throws Exception {
        return Bag(datasetId, "");
    }
    
    public static File Bag(String datasetId, String submitMsg) throws Exception {
        DashboardConfigStore store = DashboardConfigStore.get();
        Bagger bagger = null;
        Path staged = null;
        try {
            bagger = new Bagger(datasetId, false, store);
            staged = bagger.stuffit();
            Bag bag = bagger.bagit(staged);
            bagger.addSubmissionComment(staged, submitMsg);
            bagger.verify(bag);
            File archiveFile = bagger.packit(staged);
            Bagger.hashit(archiveFile);
            return archiveFile;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            if ( bagger != null ) {
                cleanup(staged);
            }
        }
    }
    
    /**
     * @see gov.noaa.pmel.dashboard.handlers.ArchiveBundler#createArchiveFilesBundle(java.lang.String, java.io.File)
     */
    @Override
    public File createArchiveFilesBundle(String stdId, File dataFile) throws Exception {
        return Bag(stdId);
    }

    /**
     * 
     */
    public Bagger(String datasetId, DashboardConfigStore store) throws PropertyNotFoundException {
        this(datasetId, false, store);
    }
    
    public Bagger(String datasetId, boolean includeHiddenFiles, DashboardConfigStore store) throws PropertyNotFoundException {
        _datasetId = datasetId.toUpperCase();
        _includeHiddenFiles = includeHiddenFiles;
        _store = store;
        _contentRoot = _store.getAppContentDir();
        _featureType = _store.getDataFileHandler().getDatasetFromInfoFile(datasetId).getFeatureType();
    }

    /**
     * @return
     * @throws IOException 
     */
    private Path stuffit() throws IOException {
        DataFileHandler dataFiler = _store.getDataFileHandler();
        File dataFile  = dataFiler.datasetDataFile(_datasetId);
        return stuffit(dataFile);
    }
    
    private Path stuffit(File dataFile) throws IOException {
        File rootDir = new File(_contentRoot, "staging");
        File bagRoot = new File(rootDir, _datasetId);
        Path bagPath = bagRoot.toPath();
        if ( bagRoot.exists()) {
            FileUtils.deleteDir(bagRoot);
        }
        bagRoot.mkdirs();
        if ( !bagRoot.exists()) { throw new IllegalStateException("Unable to create bag root dir: " + bagRoot); }
        
        File dataDir = new File(bagRoot, "data");
        dataDir.mkdirs();
        if ( !dataDir.exists()) { throw new IllegalStateException("Unable to create bag data dir: " + dataDir); }
        
        writeFileTo(dataFile, dataDir);
        
//        DataFileHandler dataFiler = _store.getDataFileHandler();
//        File infoFile = dataFiler.datasetInfoFile(_datasetId);
//        writeFileTo(infoFile, dataDir);
        
        File meta = dataDir; // new File(dataDir, "metadata");
        meta.mkdirs();
        if ( !meta.exists()) { throw new IllegalStateException("Unable to create bag metadata dir: " + meta); }
        
        File supl = new File(dataDir, "supplemental");
        supl.mkdirs();
        if ( !supl.exists()) { throw new IllegalStateException("Unable to create bag supplemental dir: " + supl); }
        
        MetadataFileHandler metaFiler = _store.getMetadataFileHandler();
        File metaFile = metaFiler.getMetadataFile(_datasetId);
//        File metaProps = new File(metaFile.getPath()+".properties");
        File metaDir = metaFile.getParentFile();
        for ( File mfile : metaDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String fname = pathname.getName();
                    return ! ( fname.endsWith(".properties") ||
                               pathname.isDirectory());
                }
            })) 
        {
           if (mfile.getName().startsWith("extracted_")) { continue; } // Ignore auto extracted metadata file
           if (mfile.getAbsoluteFile().equals(metaFile.getAbsoluteFile())) {
               writeFileTo(mfile, meta);
           } else {
               writeFileTo(mfile, supl);
           }
        }
        if ( supl.list().length == 0 ) {
           supl.delete();
        }
        if ( !_featureType.equals(FeatureType.OTHER)) {
            addLatLonFile(bagRoot);
        }
        
        return bagPath;
    }

    /**
     * @param metaFile
     * @param root
     * @param root2 
     * @throws FileNotFoundException 
     */
    private void addLatLonFile(File bagRoot) throws FileNotFoundException {
        File lonLatFile = new File(bagRoot, "lonlat.tsv");
        try ( PrintWriter lonLatFileWriter = new PrintWriter(lonLatFile); ) {
//            writeBounds(lonLatFileWriter, metaFile);
            writeAll(lonLatFileWriter);
        }
    }

    /**
     * @param lonLatFile
     * @param metaFile
    private void writeBounds(PrintWriter lonLatFile, File metaFile) {
        // Bounds tags in metadata file
        // <westbd>-124.947</westbd>
        // <eastbd>-124.947</eastbd>
        // <northbd>47.964</northbd>
        // <southbd>47.964</southbd>
    }
     */

    /**
     * @param lonLatFile
     */
    private void writeAll(PrintWriter lonLatFileWriter) {
        DashboardDatasetData ddd = _store.getDataFileHandler().getDatasetDataFromFiles(_datasetId, 0, -1);
        StdUserDataArray stda = new StdUserDataArray(ddd, _store.getKnownUserDataTypes());
        Double[] lats = stda.getSampleLatitudes();
        Double[] lons = stda.getSampleLongitudes();
        for (int idx = 1; idx < stda.getNumSamples(); idx++) {
            lonLatFileWriter.println(lons[idx] + "\t" + lats[idx]);
        }
    }

    /**
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     * 
     */
    private Bag bagit(Path bagDir) throws NoSuchAlgorithmException, IOException {
        StandardSupportedAlgorithms algorithm = getHashAlgorithm();
        Bag bag = BagCreator.bagInPlace(bagDir, Arrays.asList(algorithm), _includeHiddenFiles);
        return bag;
    }
    
    /**
     * @param staged
     * @throws IOException 
     */
    private void addSubmissionComment(Path staged, String submitMsg) throws IOException {
        if ( StringUtils.emptyOrNull(submitMsg)) {
            return;
        }
        File msgFile = new File(staged.toFile(), _datasetId + "_SubmissionComment.txt");
        try (FileWriter fout = new FileWriter(msgFile)) {
            fout.write(submitMsg);
        }
    }

    private static StandardSupportedAlgorithms getHashAlgorithm() {
        return StandardSupportedAlgorithms.SHA256;
    }
    
    private void verify(Bag bag) throws IOException, MissingPayloadManifestException, 
                                        MissingBagitFileException, MissingPayloadDirectoryException, 
                                        FileNotInPayloadDirectoryException, InterruptedException, 
                                        MaliciousPathException, UnsupportedAlgorithmException, 
                                        InvalidBagitFileFormatException, 
                                        CorruptChecksumException, VerificationException {
        BagVerifier bv = new BagVerifier();
        bv.isComplete(bag, _includeHiddenFiles);
        bv.isValid(bag, _includeHiddenFiles);
    }

    /**
     * @param bag
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    private void tossit(Bag bag) throws NoSuchAlgorithmException, IOException {
        File bagDir = new File(_contentRoot, "bags");
        File root = new File(bagDir, _datasetId);
        Path outputPath = root.toPath();
        BagWriter.write(bag, outputPath);
    }

    @SuppressWarnings("resource")
    private File packit(Path bagPath) throws IOException, CompressorException, ArchiveException {
        String compressionFormat = ApplicationConfiguration.getProperty("oap.archive_bundle.format", "zip");
        if ( "zip".equals(compressionFormat)) {
            return zip(bagPath);
        } else {
            return tgz(bagPath);
        }
    }
    
    private File zip(Path bagPath) throws IOException, CompressorException, ArchiveException {
        File bagFile = bagPath.toFile();
        
        String bagArchiveDirName = getBagArchiveName(bagFile.getName());
        File archiveRoot = new File(_contentRoot, "ArchiveBundles/bags/"+_datasetId+"/"+bagArchiveDirName);
        archiveRoot.mkdirs();
        File bagArchiveFile = new File(archiveRoot, _datasetId+"_baggit.zip");
        try ( FileOutputStream fos = new FileOutputStream(bagArchiveFile);
              ZipOutputStream zipOut = new ZipOutputStream(fos); ) {
            zipDirFiles(bagFile, "", zipOut);
        }
        
        return bagArchiveFile;
    }
    
    private static void zipDirFiles(File dir, String dirPath, ZipOutputStream zipos) throws IOException {
        File[] dirFiles = dir.listFiles();
        for (File file : dirFiles) {
            if ( file.isDirectory()) {
                zipDirFiles(file, dirPath+file.getName()+"/", zipos);
            } else {
                addZipEntry(file, dirPath, zipos);
            }
        }
    }
        
    private static void addZipEntry(File file, String entryPath, ZipOutputStream zipos) throws IOException {
        ZipEntry zipE = new ZipEntry(entryPath + file.getName());
        zipE.setTime(file.lastModified());
        zipos.putNextEntry(zipE);
        Files.copy(file.toPath(), zipos);
        zipos.closeEntry();
    }
    private File tgz(Path bagPath) throws IOException, CompressorException, ArchiveException {
        File bagFile = bagPath.toFile();
        CompressorStreamFactory csFactoid = new CompressorStreamFactory();
        ArchiveStreamFactory asFactoid = new ArchiveStreamFactory();
        String bagArchiveDirName = getBagArchiveName(bagFile.getName());
        File archiveRoot = new File(_contentRoot, "ArchiveBundles/bags/"+_datasetId+"/"+bagArchiveDirName);
        archiveRoot.mkdirs();
        File bagArchiveFile = new File(archiveRoot, _datasetId+"_baggit.tar.gz");
        FileOutputStream fos = new FileOutputStream(bagArchiveFile);
        BufferedOutputStream bufOS = new BufferedOutputStream(fos);
        GzipCompressorOutputStream gzOS = (GzipCompressorOutputStream) 
                csFactoid.createCompressorOutputStream(CompressorStreamFactory.GZIP, bufOS); 
        try (
            TarArchiveOutputStream tarcOS = (TarArchiveOutputStream)
                asFactoid.createArchiveOutputStream(ArchiveStreamFactory.TAR, gzOS)) {
            addFileToArchive(tarcOS, bagFile, "");
        }
        return bagArchiveFile;
    }
    
    private void addFileToArchive(TarArchiveOutputStream tOut, File file, String base) throws IOException {
        
        String entryName = base+file.getName();
        TarArchiveEntry tarE = new TarArchiveEntry(file, entryName);
        tOut.putArchiveEntry(tarE);
        if ( file.isFile()) {
            try ( FileInputStream fIn = new FileInputStream(file)) {
                IOUtils.copy(fIn, tOut);
            }
            tOut.closeArchiveEntry();
        } else {
            tOut.closeArchiveEntry();
            for (File child : file.listFiles()) {
                addFileToArchive(tOut, child, entryName+"/");
            }
        }
    }
    
    private static File hashit(File archiveFile) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(archiveFile);
         
        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
          
        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };
         
        //close the stream; We don't need it now.
        fis.close();
         
        //Get the hash's bytes
        byte[] bytes = digest.digest();
         
        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
         
       String hash = sb.toString();
       
       String hashFileName = archiveFile.getName().substring(0, archiveFile.getName().indexOf('.')) + "-sha256.txt";
       File digestFile = new File(archiveFile.getParentFile(), hashFileName);
       try (FileWriter hashWriter = new FileWriter(digestFile)) {
           hashWriter.write(hash);
       }
       return digestFile;
    }

    private void dumpit(Bag bag, File toDir) throws NoSuchAlgorithmException, IOException {
        Path toPath = toDir.toPath();
        BagWriter.write(bag, toPath);
    }
    private String getBagArchiveName(String bagFileName) {
        return bagFileName + "_" + getTimestampStr();
    }
    
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd'T'hhmmss'Z'";
    private SimpleDateFormat getTimestampFormater() {
        if ( _tsFormatter == null ) {
            _tsFormatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
            _tsFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return _tsFormatter;
    }
    
    /**
     * @return
     */
    private String getTimestampStr() {
        return getTimestampFormater().format(new Date());
    }

    /**
     * @param root
     * @return
     * @throws InvalidBagitFileFormatException 
     * @throws UnsupportedAlgorithmException 
     * @throws MaliciousPathException 
     * @throws UnparsableVersionException 
     * @throws IOException 
     * @throws InvalidBagMetadataException 
    private static Bag read(File root) throws InvalidBagMetadataException, IOException, 
                                       UnparsableVersionException, MaliciousPathException, 
                                       UnsupportedAlgorithmException, InvalidBagitFileFormatException {
        BagReader reader = new BagReader();
        Bag bag = reader.read(root.toPath());
        return bag;
    }
     */

    /*
    public File bagInPlace(String datasetId) throws IOException {
        File bagDir = new File(_contentRoot, "staging");
        File root = new File(bagDir, _datasetId);
        if ( root.exists()) {
            FileUtils.deleteDir(root);
        }
        root.mkdirs();
        if ( !root.exists()) { throw new IllegalStateException("Unable to create bag root dir: " + root); }
        
        File data = new File(root, "data");
        data.mkdirs();
        if ( !data.exists()) { throw new IllegalStateException("Unable to create bag data dir: " + data); }
        DataFileHandler dataFiler = _store.getDataFileHandler();
        File dataFile  = dataFiler.datasetDataFile(_datasetId);
        writeFileTo(dataFile, data);
        File infoFile = dataFiler.datasetInfoFile(_datasetId);
        writeFileTo(infoFile, data);
        
        File meta = new File(root, "metadata");
        meta.mkdirs();
        if ( !meta.exists()) { throw new IllegalStateException("Unable to create bag metadata dir: " + meta); }
        
        File supl = new File(meta, "supplemental");
        supl.mkdirs();
        if ( !supl.exists()) { throw new IllegalStateException("Unable to create bag supplemental dir: " + supl); }
        
        MetadataFileHandler metaFiler = _store.getMetadataFileHandler();
        File metaFile = metaFiler.getMetadataFile(_datasetId);
        File metaProps = new File(metaFile.getPath()+".properties");
        File metaDir = metaFile.getParentFile();
        for ( File mfile : metaDir.listFiles()) {
            if (mfile.getName().startsWith("extracted_")) { continue; } // Ignore auto extracted metadata file
           if (mfile.getAbsoluteFile().equals(metaFile.getAbsoluteFile()) || 
               mfile.equals(metaProps))  {
               writeFileTo(mfile, meta);
           } else {
               writeFileTo(mfile, supl);
           }
        }
        
        if ( supl.list().length == 0 ) {
            supl.delete();
        }
        
        return root;
    }
    */
    
    private static void cleanup(Path staging) {
        try {
            if ( staging != null ) {
                FileUtils.deleteDir(staging.toFile());
            }
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    private static void writeFileTo(File inFile, File destDir) throws IOException {
        if ( !destDir.exists()) {
            destDir.mkdirs();
        }
        File destFile = new File(destDir, inFile.getName());
        writeFile(inFile, destFile);
    }
    
    /**
     * @param dataFile
     * @param data
     * @throws IOException 
     */
    private static void writeFile(File inFile, File destFile) throws IOException {
        
        FileChannel cout = null;
        
        if ( ! destFile.getParentFile().exists()) {
            if ( ! destFile.getParentFile().mkdirs()) {
                throw new IOException("Failed to create result directory: " + destFile.getParentFile());
            }
        }
        try ( ReadableByteChannel cin = Channels.newChannel(new FileInputStream(inFile))) {
            java.nio.file.Path path = destFile.toPath();
//            logger.debug("Writing file to: " + path);
            cout = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            cout.transferFrom(cin, 0, inFile.length());
        } finally {
            try { if ( cout != null ) { cout.close(); }} catch (Exception e) {}
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            File configDir = new File("/Users/kamb/tomcat/7/content/OAPUploadDashboard/config");
            ApplicationConfiguration.Initialize(configDir, "oap");
            String datasetId = "PRISM082008";
            File bagArchive = Bagger.Bag(datasetId, "Bagit Test Submission");
            System.out.println("Archive: " + bagArchive.getAbsolutePath());
//            DashboardConfigStore store = DashboardConfigStore.get();
//            Bagger bagger = new Bagger(datasetId, false, store);
////            Path bagPath = new File(store.getAppContentDir(), "bags/"+datasetId).toPath();
////            bagger.packit(bagPath);
//            Path stuff = bagger.stuffit();
//            Bag bag = bagger.bagit(stuff);
//            bagger.verify(bag);
//            bagger.packit(stuff.getParent());
//            bagger.tossit(bag);
//        File bagDir = new File(bagger._contentRoot, "bags");
//        File root = new File(bagDir, datasetId);
//            Bag again = bagger.read(root);
//            bagger.verify(again);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: handle exception
        }

    }
}
