/**
 * 
 */
package gov.noaa.pmel.dashboard.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.verify.BagVerifier;

/**
 * 
 */
public class Bagit {

    private enum COMP_FORMAT {
        zip,
        tgz
    }
    private static boolean create_package = true;
    
    static void usage(Integer exit) {
        System.out.println("Usage: bagger [options] <bag_path>");
        System.out.println("  -z [filename] : zip file name; Default: [bag_dir_name]_bagit.[comp_format]");
        System.out.println("  -Z : do not create bagit archival package (file).");
//        System.out.println("  -d <dataset_id> : ");
        System.out.println("  -i <path> : path to SubmitInstructions file");
        System.out.println("  -f <fmt> : compression format, either zip or tgz (gzipped tar).  Default zip.");
        System.out.println("  -v : verbose output");
        System.out.println("  If do not create package specified, the bag_path will hold the raw bag.");
        if ( exit != null ) {
            System.exit(exit.intValue());
        }
    }

    private static StandardSupportedAlgorithms getHashAlgorithm() {
        return StandardSupportedAlgorithms.SHA256;
    }

    /**
     * @param bag
     * @param submitInstrFile
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    private static void addSubmitInstructions(Bag bag, File submitInstrFile) throws IOException, NoSuchAlgorithmException {
        File bagDir = bag.getRootDir().toFile();
        File baggedFile = new File(bagDir, submitInstrFile.getName());
        console("Adding submit instructions file: " + submitInstrFile + " to " + bagDir);
        try ( FileInputStream fIn = new FileInputStream(submitInstrFile);
              FileOutputStream fOut = new FileOutputStream(baggedFile)) {
            IOUtils.copy(fIn, fOut);
        }
        File tagMfFile = new File(bag.getRootDir().toFile(), "tagmanifest-sha256.txt");
        Path commentPath = submitInstrFile.toPath();
        String checkSum = Hasher.hash(commentPath, MessageDigest.getInstance("SHA-256"));
        try (FileWriter mfout = new FileWriter(tagMfFile, true)) {
            String manifest = new StringBuilder(checkSum).append("  ").append(submitInstrFile.getName()).toString();
            mfout.write(manifest);
        }
        Manifest tagMf = bag.getTagManifests().iterator().next();
        tagMf.getFileToChecksumMap().put(commentPath, checkSum);
    }

    private static void verify(Bag bag) throws Exception {
        console("Verifying bag : " + bag);
        try ( BagVerifier bv = new BagVerifier(); ) { 
            bv.isComplete(bag, false);
            bv.isValid(bag, false);
        }
    }

    private static String _getBagArchiveName(File bagDir) {
        return bagDir.getName() + "_" + getTimestampStr();
    }
    private static String getBagArchiveFileName(File bagDir, String fileType) {
        return bagDir.getName() + "_bagit." + fileType;
    }
    
    private static final String TIMESTAMP_FORMAT = "yyyyMMdd'T'hhmmss'Z'";
    private static SimpleDateFormat getTimestampFormater() {
        SimpleDateFormat tsFormatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
        tsFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return tsFormatter;
    }
    private static String getTimestampStr() {
        return getTimestampFormater().format(new Date());
    }

    private static File hashit(File archiveFile) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        //Get file input stream for reading the file content
        try ( FileInputStream fis = new FileInputStream(archiveFile); ) {
            //Create byte array to read data in chunks
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;
              
            //Read file data and update in message digest
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
         
        //Get the hash's bytes
        byte[] bytes = digest.digest();
         
        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
         
       String hash = sb.toString();
       
       String hashFileName = archiveFile.getName().substring(0, archiveFile.getName().indexOf('.')) + "-sha256.txt";
       File digestFile = new File(archiveFile.getParentFile(), hashFileName);
       try (FileWriter hashWriter = new FileWriter(digestFile)) {
           hashWriter.write(hash);
       }
//       try { // simpler way using Bagit class
//           String hashy = Hasher.hash(archiveFile.toPath(), digest);
//           System.out.println("hashy: " + hash.compareTo(hashy));
//       } catch (Exception ex) {
//           ex.printStackTrace();
//       }
       return digestFile;
    }
    
    private static File packit(File bagDir, File bagFile, COMP_FORMAT format) throws IOException, CompressorException, ArchiveException {
//        String compressionFormat = ApplicationConfiguration.getProperty("oap.archive_bundle.format", "zip");
        if ( COMP_FORMAT.zip == format ) {
            return zip(bagDir, bagFile);
        } else {
            return tgz(bagDir, bagFile);
        }
    }
    
    private static File zip(File bagDir, File zipFile) throws IOException, CompressorException, ArchiveException {
        File bagArchiveFile = zipFile != null ?
                            zipFile :
                            new File(bagDir.getParent(), bagDir.getName()+"_bagit.zip");  
        console("Packing bag in " + bagArchiveFile);
        try ( FileOutputStream fos = new FileOutputStream(bagArchiveFile);
              ZipOutputStream zipOut = new ZipOutputStream(fos); ) {
            zipDirFiles(bagDir, "", zipOut);
        }
        
        return bagArchiveFile;
    }
    private static void addZipEntry(File file, String entryPath, ZipOutputStream zipos) throws IOException {
        ZipEntry zipE = new ZipEntry(entryPath + file.getName());
        zipE.setTime(file.lastModified());
        zipos.putNextEntry(zipE);
        Files.copy(file.toPath(), zipos);
        zipos.closeEntry();
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
 
    private static File tgz(File bagDir, File bagFile) throws IOException, CompressorException, ArchiveException {
        CompressorStreamFactory csFactoid = new CompressorStreamFactory();
        ArchiveStreamFactory asFactoid = new ArchiveStreamFactory();
        File bagArchiveFile = bagFile != null ?
                bagFile :
                new File(bagDir.getParent(), bagDir.getName()+"_bagit.tar.gz");  
        console("tar-gzipping " + bagDir + " to " + bagArchiveFile);
        FileOutputStream fos = new FileOutputStream(bagArchiveFile);
        BufferedOutputStream bufOS = new BufferedOutputStream(fos);
        GzipCompressorOutputStream gzOS = (GzipCompressorOutputStream) 
                csFactoid.createCompressorOutputStream(CompressorStreamFactory.GZIP, bufOS); 
        try (
            TarArchiveOutputStream tarcOS = (TarArchiveOutputStream)
                asFactoid.createArchiveOutputStream(ArchiveStreamFactory.TAR, gzOS)) {
            addFileToArchive(tarcOS, bagDir, "");
        }
        return bagArchiveFile;
    }
    
    private static void addFileToArchive(TarArchiveOutputStream tOut, File file, String base) throws IOException {
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
    

    /**
     * @param args
     */
    public static void main(String[] args) {
        COMP_FORMAT compressionFormat = COMP_FORMAT.zip;
        try {
            if ( args.length == 0 ) {
                usage(-1);
            }
            String bagDirPath = args[args.length-1];
            File bagDir = new File(bagDirPath);
            String archiveFileName = null;
            String submitInstructionsPath = null;
            for ( int i=0; i < args.length-1; i++ ) {
                switch (args[i]) {
                    case "-z":
                        if ( i < args.length -2 &&
                             ! args[i+1].startsWith("-")) {
                            archiveFileName = args[++i];
                        } else {
                            create_package = true;  // redundant
                        }
                        break;
                    case "-Z":
                        create_package = false;
                        break;
                    case "-i":
                        submitInstructionsPath = args[++i];
                        break;
                    case "-f":
                        compressionFormat = COMP_FORMAT.valueOf(args[++i]);
                        break;
                    case "-v":
                        verbose = true;
                        break;
                    default:
                        System.err.println("Unsupported option: " + args[i]);
                        usage(-2);
                }
            }
            if ( !bagDir.exists() || !bagDir.isDirectory()) {
                System.err.println(bagDir + " doesn't exist or is not a directory");
                usage(3);
            }
            File archiveFile = null;
            if ( archiveFileName != null ) {
                archiveFile = new File(archiveFileName);
            }
            File submitInstrFile = null;
            if ( submitInstructionsPath != null ) {
                submitInstrFile = new File(submitInstructionsPath);
            }
            Path bagPath = bagDir.toPath();
            console("Creating bag at " + bagPath);
            Bag bag = BagCreator.bagInPlace(bagPath, Arrays.asList(getHashAlgorithm()), false);
            if ( submitInstrFile != null ) {
                addSubmitInstructions(bag, submitInstrFile);
            }
            verify(bag);
            if ( create_package ) {
                archiveFile = packit(bagDir, archiveFile, compressionFormat);
                hashit(archiveFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static boolean verbose = false;
    /**
     * @param string
     */
    private static void console(String msg) {
        if ( verbose ) {
            System.out.println(msg);
        }
    }

}
