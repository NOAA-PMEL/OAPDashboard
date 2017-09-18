/**
 * 
 */
package gov.noaa.pmel.dashboard.util.xml;

import java.io.FileInputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author kamb
 *
 */
public class XViewPath {

	private Document _doc;
	private XPath _xpath;

	public XViewPath(Document xmlDoc) {
		_doc = xmlDoc;
		_xpath = XPathFactory.newInstance().newXPath();
	}
	
	public Node getNode(String xpathExpression) throws XPathExpressionException {
		Node node = null;
		node = (Node)_xpath.compile(xpathExpression).evaluate(_doc, XPathConstants.NODE);
		return node;
	}
	public NodeList getNodes(String xpathExpression) throws XPathExpressionException {
		NodeList nodeList = null;
		nodeList = (NodeList)_xpath.compile(xpathExpression).evaluate(_doc, XPathConstants.NODESET);
		return nodeList;
	}
	public String getValue(String xpathExpression) throws XPathExpressionException {
		String value = null;
		value = (String)_xpath.compile(xpathExpression).evaluate(_doc, XPathConstants.STRING);
		return value;
	}
	
	public static void main(String[] args) {
		String oadsExample = "test-data/OADS.xml";
		String missing = "/Users/kamb/Downloads/oap_metadata_2.xml";
		String minimal = "test-data/CHABA062014_OADS.xml";
		String fname = minimal;
		XViewPath vp = null;
		try ( FileInputStream inStream = new FileInputStream(fname) ) {
			Document doc = XReader.getDOM(inStream);
			vp = new XViewPath(doc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			String expo = vp.getValue("/metadata/datasetId");
			System.out.println(expo);
			Node node = vp.getNode("/metadata/datasetId");
			System.out.println(node);
			NodeList nodes = vp.getNodes("/metadata/datasetId");
			System.out.println(nodes);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
