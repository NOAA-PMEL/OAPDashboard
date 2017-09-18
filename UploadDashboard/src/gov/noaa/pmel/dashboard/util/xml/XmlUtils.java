/**
 * 
 */
package gov.noaa.pmel.dashboard.util.xml;

/**
 * @author kamb
 *
 */
public class XmlUtils {

	private static String NBSP = "&nbsp;";
	private static String NBTAB = "&nbsp;&nbsp;&nbsp;&nbsp;";
	
	public static String asHtml(String xml) {
		String html = xml;
		html = html.replaceAll("<", "&lt;");
		html = html.replaceAll(">", "&gt;");
		String[] lines = html.split("\\n");
		StringBuilder b = new StringBuilder();
		for (String line : lines) {
			String revised = "";
			boolean done = false;
			do {
				if ( line == null || line.length() == 0 ) {
					done = true;
				} else {
					if ( line.charAt(0) == ' ' ) {
						revised += NBSP;
						line = line.substring(1);
					} else if ( line.charAt(0) == '\t' ) {
						revised += NBTAB;
						line = line.substring(1);
					} else {
						done = true;
					}
				}
			} while ( !done );
			revised += line;
			b.append(revised).append("<br/>");
		}
		html = b.toString();
		return html;
	}

	static String testSp = 
"<one>\n" +
"  <two>\n" +
"     <three/>\n" +
"  </two>\n" +
"</one>";
	
	static String testTab = 
"<one>\n"	+
"		<two>\n"	+
"					<three/>\n"	+
"		</two>\n"	+
"</one>";
	
	public static void main(String[] args) {
		System.out.println(testSp);
		String revised = asHtml(testSp);
		System.out.println(revised);
		System.out.println(testTab);
		revised = asHtml(testTab);
		System.out.println(revised);
	}
}
