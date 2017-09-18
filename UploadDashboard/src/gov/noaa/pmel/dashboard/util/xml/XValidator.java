
package gov.noaa.pmel.dashboard.util.xml;

import java.io.*;

import javax.xml.parsers.*;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.*;


/**
 * @author kamb
 *
 */
public class XValidator
	implements org.xml.sax.ErrorHandler
{
    /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

    /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

    /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";

    // default settings

    /** Default parser name. */
    protected static final String DEFAULT_PARSER_NAME = "dom.wrappers.Xerces";

    /** Default namespaces support (true). */
    protected static final boolean DEFAULT_NAMESPACES = true;

    /** Default validation support (false). */
    protected static final boolean DEFAULT_VALIDATION = true;

    /** Default Schema validation support (false). */
    protected static final boolean DEFAULT_SCHEMA_VALIDATION = true;

    /** Default Schema full checking support (false). */
    protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = true;

    /** Default canonical output (false). */
    protected static final boolean DEFAULT_CANONICAL = true;

//	private File _xFile = null;
	private SAXException _exception = null;
	private boolean _error = false;

	boolean namespaces = DEFAULT_NAMESPACES;
	boolean validation = DEFAULT_VALIDATION;
	boolean schemaValidation = DEFAULT_SCHEMA_VALIDATION;
	boolean schemaFullChecking = DEFAULT_SCHEMA_FULL_CHECKING;
	boolean canonical = DEFAULT_CANONICAL;

	private InputStream _in;
//	private InputSource _is;

	public XValidator(InputStream input) {
		_in = input;
	}
	
	@SuppressWarnings("resource")
	public static XValidator FileValidator(File xmlFile) throws FileNotFoundException
	{
		return new XValidator(new FileInputStream(xmlFile));
	}

	public static XValidator StringValidator(String xmlString) throws UnsupportedEncodingException 
	{
		return new XValidator(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));
	}

	private void setup(DOMParser parser)
	{
		// set parser features
		try {
			parser.setFeature(NAMESPACES_FEATURE_ID, namespaces);
		}
		catch (SAXException e) {
			System.err.println("warning: Parser does not support feature ("+NAMESPACES_FEATURE_ID+")");
		}
		try {
			parser.setFeature(VALIDATION_FEATURE_ID, validation);
		}
		catch (SAXException e) {
			System.err.println("warning: Parser does not support feature ("+VALIDATION_FEATURE_ID+")");
		}
		try {
			parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, schemaValidation);
		}
		catch (SAXException e) {
			System.err.println("warning: Parser does not support feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");
		}
		try {
			parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
		}
		catch (SAXException e) {
			System.err.println("warning: Parser does not support feature ("+SCHEMA_FULL_CHECKING_FEATURE_ID+")");
		}
	}


	public boolean validate(boolean useBuilder)
		throws Exception
	{
		return validate();
	}
	public boolean validate()
		throws Exception
	{
        try
        {
			if ( true )
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating( true );
				DocumentBuilder parser = factory.newDocumentBuilder();
				if ( ! parser.isValidating() )
					throw new Exception( "Available parser is not validating: " + parser.getClass() );
				parser.setErrorHandler( this );
				System.out.println("DocBuilder: " + parser.getClass() );
				parser.parse( _in );
			}
//			else
//			{
//				org.apache.xerces.parsers.DOMParser parser = new DOMParser();
//				setup( parser );
//				parser.setErrorHandler( this );
//				System.out.println("Parser: " + parser.getClass() );
//				parser.parse( _is );
//			}

			return ! _error;
        }
        catch ( SAXException ex )
        {
			_exception = ex;
			return false;
		} finally {
			if ( _in != null ) { _in.close(); }
		}
	}

	public SAXException getParseException()
	{
		return _exception;
	}

	public static void usage(boolean exit)
	{
		System.out.println( "USAGE: XValidator <file>" );
		if ( exit )
			System.exit( -1 );
	}

	private static String fname = "test-data/ISO_OADS_v7.xml";
			
	public static String getFileName()
	{
		if ( fname != null ) { return fname; }
		try {
			System.out.print("File name: " );
			System.out.flush();
			return new BufferedReader( new InputStreamReader( System.in )).readLine();
		}
		catch ( Exception ex ) {
			ex.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args)
	{
		try
		{
			String filename = null;

			if ( args.length == 1 )
				filename = args[ 0 ];
			else
				filename = getFileName();

//			System.out.print( "Use DocumentBuilderFactory? (y/n): " );
//			String answer = 
//				new BufferedReader( new InputStreamReader( System.in )).readLine();
//			boolean useFactory = 
//				answer.length() > 0 && 
//				answer.substring(0,1).equalsIgnoreCase( "y" ) ?
//				true : 
//				false;

			File file = new File(filename);
			System.out.println("Validating XML file: " + file.getAbsolutePath() );

			XValidator xv = XValidator.FileValidator(file);

			if ( xv.validate(/*useFactory*/) )
				System.out.println( "File " + filename + 
									" is a valid XML file." );
			else
			{
				System.out.println( "File " + filename + 
									" is NOT a valid XML file." );
				// System.out.println( "Exception: " + xv.getParseException() );
			}
		}
		catch ( Exception ex )
		{
			ex.printStackTrace();
		}
	}

	public void error(SAXParseException exception)
	{
		_error = true;
		System.err.println( "<Error> ["+
				exception.getSystemId() + ":" +
				exception.getLineNumber() + "." +
				exception.getColumnNumber() + "]:" + 
				exception.getMessage() );
		if ( _exception == null )
			_exception = exception;
	}

    public void fatalError(SAXParseException exception)
	{
		_error = true;
		System.err.println( "<Fatal Error> [" +
				exception.getSystemId() + ":" +
				exception.getLineNumber() + "." +
				exception.getColumnNumber() + "]:" + 
				exception.getMessage() );
		if ( _exception == null )
			_exception = exception;
	}

    public void warning(SAXParseException exception)
	{
		_error = true;
		System.err.println( "<Warning> ["+
				exception.getSystemId() + ":" +
				exception.getLineNumber() + "." +
				exception.getColumnNumber() + "]:" + 
				exception.getMessage() );
		if ( _exception == null )
			_exception = exception;
	}
}
