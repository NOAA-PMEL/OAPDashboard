/**
 * 
 */
package gov.noaa.pmel.dashboard.util.xml;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.*;

import com.kamb.utils.xml.GenXer;

import static org.w3c.dom.Node.*;

/**
 * @author kamb
 *
 */
public class ContentStripper {

	public static Document strip(Document inDoc) throws Exception {
		Element docElem = inDoc.getDocumentElement();
		docElem.normalize();
		strip(docElem);
		return inDoc;
	}
	
	private static void strip(Element parent) {
		NodeList nodeList = parent.getChildNodes();
		int listLength = nodeList.getLength();
		List<Node> remove = new ArrayList<Node>(listLength);
		for (int i = 0; i < listLength; i++) {
			Node node = nodeList.item(i);
			switch (node.getNodeType()) {
				case ELEMENT_NODE:
					strip((Element)node);
					break;
				case TEXT_NODE:
				case CDATA_SECTION_NODE:
					remove.add(node);
					break;
				default:
					System.err.println("Not handling node " + node );
			}
		}
		for (Node child : remove) {
			parent.removeChild(child);
		}
	}

	public static Document strip(InputStream inStream) throws Exception {
		return strip(XReader.getDOM(inStream));
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

			FileInputStream inStream = new FileInputStream(filename);
			Document dom = XReader.getDOM(inStream);
			dom = ContentStripper.strip(dom);
			System.out.println(dom);
			PrintWriter writer = new PrintWriter(filename+".stripped");
			GenXer genX = new GenXer(writer);
			genX.dump(dom);
			genX.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
