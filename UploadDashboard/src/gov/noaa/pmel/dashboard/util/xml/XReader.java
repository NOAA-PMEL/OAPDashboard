/**
 * 
 */
package gov.noaa.pmel.dashboard.util.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

/**
 * @author kamb
 *
 */
public class XReader {

	private File _file;
	
	public XReader(String fileName) throws FileNotFoundException {
		_file = new File(fileName);
		if ( ! _file.exists()) { throw new FileNotFoundException(fileName + " not found."); }
	}
	
	public Document getDocument() throws Exception {
		try ( InputStream inStream = new FileInputStream(_file)) {
			Document doc = getDOM(inStream);
			return doc;
		} catch (IllegalStateException isx) {
			throw new RuntimeException("Error reading file: " + _file.getPath(), isx);
		}
			
	}

	public String getXmlString() throws IOException {
		try ( FileInputStream inStream = new FileInputStream(_file) ) {
			String xml = getXML(inStream);
			return xml;
		} catch (IllegalStateException isx) {
			throw new RuntimeException("Error reading file: " + _file.getPath(), isx);
		}
	}
	public static String getXML(FileInputStream inStream) throws IOException {
		String xml = null;
		FileChannel fChan = inStream.getChannel();
		long available = fChan.size();
		if ( available > Integer.MAX_VALUE) {
			throw new IllegalStateException("File is too large: " + available + " bytes");
		}
		ByteBuffer bBuf = ByteBuffer.allocate((int)available);
		int read = fChan.read(bBuf);
		if ( read != available ) {
			throw new IllegalStateException("Failed to completely read file.");
		}
		xml = new String(bBuf.array());
		return xml;
	}
	public static Document getDOM(InputStream inStream) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		factory.setValidating( true );
		DocumentBuilder parser = factory.newDocumentBuilder();
//		if ( ! parser.isValidating() )
//			throw new Exception( "Available parser is not validating: " +
//					parser.getClass() );
//		parser.setErrorHandler( this );
		System.out.println("DocBuilder: " + parser.getClass() );
		Document dom = parser.parse( inStream );
		return dom;
	}
	
	private static String fname = "test-data/OADS.xml";
	public static String getFileName()
	{
		if ( fname != null ) { return fname; }
		try
		{
			System.out.print("File name: " );
			System.out.flush();
			return new BufferedReader( 
					new InputStreamReader( System.in )).readLine();
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String filename = null;

			if ( args.length == 1 )
				filename = args[ 0 ];
			else
				filename = getFileName();

			XReader xreader = new XReader(filename);
			String xml = xreader.getXmlString();
			System.out.println(xml);
			Document dom = xreader.getDocument();
			System.out.println(dom);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
