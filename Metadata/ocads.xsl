<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="/metadata">

    <xsl:variable name='confxml' select='"ocads-conf.xml"' />
    <xsl:variable name='confDoc' select='document($confxml)' />
    <xsl:variable name='title' select='$confDoc//config/option[@name="title"]'/>
    <xsl:variable name='keywords' select='$confDoc//config/option[@name="keywords"]'/>
    <xsl:variable name='website' select='$confDoc//config/option[@name="website"]'/>
    <xsl:variable name='project' select='$confDoc//config/option[@name="project"]'/>

    <html style="overflow-y:scroll;" xmlns="http://www.w3.org/1999/xhtml">

      <head>
        <xsl:comment>Script $Id: a8a0e93a20cebe1f8af4181eb68c3d1c2dd51fe7 $</xsl:comment>

        <title><xsl:value-of select="$title"/>: NCEI Accession <xsl:value-of select="accn"/></title>
        <meta name="keywords">
	  <xsl:attribute name="content">
	    <xsl:value-of select="$keywords"/>
	  </xsl:attribute>
	</meta>

        <style type="text/css">
div.indent {
	padding-left:30px;
	text-indent: -10px;
}
td {
	vertical-align: top;
}
        </style>

      </head>

      <body style="background:#0e2e55 url(/media/images/common/bkg_nodc_dark1_top.jpg) top center no-repeat;">

        <div style="position:relative;padding-left:15px; padding-right:15px; width:945px; height:auto; margin-left:auto; margin-right:auto; border: 0px solid #000000;z-index:1; background-color:#fff;">

          <a href="http://www.ncei.noaa.gov/" target="_blank"><img src="/Images/nceilogo-banner.png"/></a>

          <div style="width:100%; background-color:#ebebeb;"><span style="padding-left:10px; vertical-align:middle;"></span>
            <a target="_blank">
	      <xsl:attribute name="href">
                <xsl:value-of select="$website"/>
              </xsl:attribute>
              <xsl:value-of select="$project"/>
              </a>
	  </div>

          <!--title of the data set-->
          <h2><xsl:value-of select="title"/></h2>


          <!--Investigators-->
          <span style="font-weight:bold">INVESTIGATOR(S): </span><br/>

          <xsl:for-each select="person">

            <div style="width:900px; margin-left: 30px; text-indent:-20px;">

              <strong><xsl:value-of select="name"/></strong>

              {<xsl:value-of select="organization"/>,

              <xsl:if test="deliverypoint1 != ''">
                <xsl:value-of select="deliverypoint1"/>,
              </xsl:if>

              <xsl:if test="city != ''">
                <xsl:value-of select="city"/>,
              </xsl:if>

              <xsl:if test="administrativeArea != ''">
                <xsl:value-of select="administrativeArea"/>,
              </xsl:if>

              <xsl:if test="zip != ''">
                <xsl:value-of select="zip"/>,
              </xsl:if>

              <xsl:value-of select="country"/>}
              <br/>

            </div>

          </xsl:for-each>

          <br/>

          <span style="font-weight:bold">ABSTRACT: </span><xsl:value-of select="abstract"/>
          <br/><br/>

          <span style="font-weight:bold">CITE AS: </span><xsl:value-of select="citation"/>
          <br/><br/>

          <xsl:if test="dataUse != ''">
             <span style="font-weight:bold">DATA USE: </span>
             <xsl:value-of select="dataUse"/>
             <br/><br/>
          </xsl:if>



          <!--Buttons for NCEI landing page and data downloading-->
          <span style="padding-left:250px">
            <a href="{link_landing}" target="_blank"><input type="button" style="background-color:#A9E2F3;" value="NCEI metadata"/></a>
          </span>

          <span style="padding-left:220px">
            <a href="{link_download}" target="_blank"><input type="button" style="background-color:#A9E2F3;" value="Download data"/></a>
          </span>

          <br/><br/>

          <span style="font-weight:bold; padding-left:0px; ">IDENTIFICATION INFORMATION FOR THIS DATA PACKAGE: </span><br/>

          <span style="padding-left:20px; font-weight: bold;">NCEI ACCESSION: </span><xsl:value-of select="accn"/>
          <br/>

          <span style="padding-left:20px; font-weight: bold;">NCEI DOI: </span><xsl:value-of select="doi"/>
          <br/>

          <div class="indent"><span style="font-weight: bold;">EXPOCODE: </span>
            <xsl:if test="expocode != ''">
              <xsl:for-each select="expocode">
                <xsl:apply-templates/>;
              </xsl:for-each>
            </xsl:if>
          </div>

          <div class="indent"><span style="font-weight: bold;">CRUISE ID: </span>
            <xsl:if test="cruiseID != ''">
              <xsl:for-each select="cruiseID">
                <xsl:apply-templates/>;
              </xsl:for-each>
            </xsl:if>
          </div>

          <span style="padding-left:20px; font-weight: bold;">SECTION/LEG: </span>
          <xsl:if test="section != ''">
            <xsl:for-each select="section">
              <xsl:apply-templates/>;
            </xsl:for-each>
          </xsl:if>
          <br/><br/>

          <table style="width:945px">
            <tr><td style="width: 450px;">
                <table style="padding-left:0px; width:430px">
                  <span style="font-weight:bold">TYPES OF STUDY: </span><br/>
                  <span style="padding-left:20px;"></span>
                  <xsl:for-each select="type">
                    <span style="padding-left:5px;"><xsl:apply-templates/>;</span>
                  </xsl:for-each>
                  <br/><br/>
                </table>

                <span style="font-weight:bold;">TEMPORAL COVERAGE: </span> <br/>
                <table style="padding-left:20px; width:500px">
                  <tr>
                    <td style="width: 50%; text-align:left;">START DATE: <xsl:value-of select="startdate"/></td>
                    <td style="width: 50%; text-align:left;">END DATE: <xsl:value-of select="enddate"/></td>
                  </tr>
                </table> <br/>

                <span style="font-weight:bold;">SPATIAL COVERAGE: </span> <br/>
                <table style="padding-left:20px; width:500px">
                  <tr>
                    <td style="padding-left:110px; text-align:left;">NORTH BOUNDARY: <xsl:value-of select="northbd"/></td>
                  </tr>
                </table>

                <table style="padding-left:20px; width:500px">
                  <tr>
                    <td style="width: 50%; text-align:left;">WEST BOUNDARY: <xsl:value-of select="westbd"/></td>
                    <td style="width: 50%; text-align:left;">EAST BOUNDARY: <xsl:value-of select="eastbd"/></td>
                  </tr>
                </table>
                <table style="padding-left:20px; width:500px">
                  <tr>
                    <td style="padding-left:110px; text-align:left;">SOUTH BOUNDARY: <xsl:value-of select="southbd"/></td>
                  </tr>
                </table><br/>

                <table style="padding-left:0px; width:430px">
                  <span style="font-weight:bold">GEOGRAPHIC NAMES: </span><br/>
                  <span style="padding-left:15px;">
                  <xsl:for-each select="geographicName">
                    <span style="padding-left:5px;"><xsl:apply-templates/>;</span>
                  </xsl:for-each>
                  </span><br/>

                  <xsl:if test="locationOrganism != ''">
                    <br/>
                    <span style="font-weight:bold">LOCATION OF ORGANISM COLLECTION: </span><br/>
                    <xsl:for-each select="locationOrganism">
                      <span style="padding-left:5px;"><xsl:apply-templates/>;</span>
                    </xsl:for-each>
                    <br/>
                  </xsl:if>

                  <br/>

                  <span style="font-weight:bold">PLATFORMS: </span><br/>
                  <xsl:if test="platform/name != ''">
                    <div style="padding-left:15px;">
                    <xsl:for-each select="platform">
                      <xsl:choose>
                        <xsl:when test="ID =''">
                          <xsl:value-of select="name"/>;
                        </xsl:when>
                        <xsl:otherwise>
                          <xsl:value-of select="name"/> (ID: <xsl:value-of select="ID"/>);
                        </xsl:otherwise>
                      </xsl:choose>
                    </xsl:for-each><br/>
                    </div>
                  </xsl:if><br/>

                  <span style="font-weight:bold">RESEARCH PROJECT(S): </span><br/>
                  <xsl:if test="researchProject != ''">
                    <span style="padding-left:20px;"></span>
                    <xsl:for-each select="researchProject">
                      <xsl:apply-templates/>;
                    </xsl:for-each>
                  </xsl:if>

                </table>

              </td>

              <!--map or image-->
              <td style="width:450px; height:400px;">
                <a href="{link_img}" target="_blank"><img src="{link_img}" style="max-width:420px; max-height:400px;"/></a>
              </td>

            </tr>
          </table>
          <br/>

          <span style="font-weight:bold;">VARIABLES / PARAMETERS: </span>
          <br/><br/>

          <!-- Dissolved Inorganic Carbon -->
          <xsl:for-each select="variable[internal = '1']">

            <hr/>

            <table width="940">
              <tr>
                <td style="font-weight:bold; font-style:italic; text-align:center;">Dissolved Inorganic Carbon</td>
              </tr>
            </table>
            <br/>
            <table width="940">

              <xsl:if test="abbrev != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold;">Abbreviation: </td>
                  <td style="padding-left:10px; vertical-align:middle;"><xsl:value-of select="abbrev"/></td>
              </tr></xsl:if>


              <xsl:if test="unit != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Unit: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="unit"/></td>
              </tr></xsl:if>

              <xsl:if test="observationType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Observation type: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="observationType"/></td>
              </tr></xsl:if>

              <xsl:if test="insitu != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">In-situ / Manipulation / Response variable: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="insitu"/></td>
              </tr></xsl:if>

              <xsl:if test="measured != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Measured or calculated: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="measured"/></td>
              </tr></xsl:if>

              <xsl:if test="calcMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Calculation method and parameters: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="calcMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="samplingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sampling instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="samplingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="analyzingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Analyzing instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="analyzingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="detailedInfo != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Detailed sampling and analyzing information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="detailedInfo"/></td>
              </tr></xsl:if>

              <xsl:if test="replicate != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Replicate information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="replicate"/></td>
              </tr></xsl:if>

              <!--standard-->
              <xsl:for-each select="standard">

                <xsl:if test="description != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization description: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="description"/></td>
                </tr></xsl:if>

                <xsl:if test="frequency != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization frequency: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="frequency"/></td>
                </tr></xsl:if>

                <xsl:for-each select="crm">

                  <xsl:if test="manufacturer != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">CRM manufacturer: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="manufacturer"/></td>
                  </tr></xsl:if>

                  <xsl:if test="batch != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">CRM batch number: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="batch"/></td>
                  </tr></xsl:if>

                </xsl:for-each>

              </xsl:for-each>

              <!-- poison -->
              <xsl:for-each select="poison">

                <xsl:if test="poisonName != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Poison name: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="poisonName"/></td>
                </tr></xsl:if>

                <xsl:if test="volume != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Poison volume: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="volume"/></td>
                </tr></xsl:if>

                <xsl:if test="correction != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Poison correction: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="correction"/></td>
                </tr></xsl:if>

              </xsl:for-each>

              <xsl:if test="uncertainty != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Uncertainty: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
              </tr></xsl:if>

              <xsl:if test="flag != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Quality flag convention: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="flag"/></td>
              </tr></xsl:if>

              <xsl:if test="methodReference != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Method reference: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="methodReference"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherName != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher name: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherName"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherInstitution != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher institution: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherInstitution"/></td>
              </tr></xsl:if>

            </table>

          </xsl:for-each>










          <!-- Total alkalinity -->

          <xsl:for-each select="variable[internal = '2']">

            <hr/>

            <table width="940"><tr>
                <td style="font-weight:bold; font-style:italic; text-align:center;">Total alkalinity</td>
            </tr></table>

            <br/>

            <table width="940">

              <xsl:if test="abbrev != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Abbreviation: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="abbrev"/></td>
              </tr></xsl:if>


              <xsl:if test="unit != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Unit: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="unit"/></td>
              </tr></xsl:if>


              <xsl:if test="observationType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Observation type: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="observationType"/></td>
              </tr></xsl:if>

              <xsl:if test="insitu != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">In-situ / Manipulation / Response variable: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="insitu"/></td>
              </tr></xsl:if>


              <xsl:if test="measured != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Measured or calculated: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="measured"/></td>
              </tr></xsl:if>

              <xsl:if test="calcMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Calculation method and parameters: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="calcMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="samplingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sampling instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="samplingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="analyzingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Analyzing instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="analyzingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="titrationType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Type of titration: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="titrationType"/></td>
              </tr></xsl:if>

              <xsl:if test="cellType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Cell type (open or closed): </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="cellType"/></td>
              </tr></xsl:if>

              <xsl:if test="curveFitting != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Curve fitting method: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="curveFitting"/></td>
              </tr></xsl:if>

              <xsl:if test="detailedInfo != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Detailed sampling and analyzing information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="detailedInfo"/></td>
              </tr></xsl:if>

              <xsl:if test="replicate != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Replicate information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="replicate"/></td>
              </tr></xsl:if>

              <!--standard-->
              <xsl:for-each select="standard">

                <xsl:if test="description != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization description: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="description"/></td>
                </tr></xsl:if>

                <xsl:if test="frequency != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization frequency: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="frequency"/></td>
                </tr></xsl:if>

                <xsl:for-each select="crm">

                  <xsl:if test="manufacturer != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">CRM manufacturer: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="manufacturer"/></td>
                  </tr></xsl:if>

                  <xsl:if test="batch != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">CRM batch number: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="batch"/></td>
                  </tr></xsl:if>

                </xsl:for-each>

              </xsl:for-each>

              <!-- poison -->
              <xsl:for-each select="poison">

                <xsl:if test="poisonName != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Poison name: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="poisonName"/></td>
                </tr></xsl:if>

                <xsl:if test="volume != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Poison volume: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="volume"/></td>
                </tr></xsl:if>

                <xsl:if test="correction != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Poison correction: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="correction"/></td>
                </tr></xsl:if>

              </xsl:for-each>

              <xsl:if test="blank != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">TA blank correction: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="blank"/></td>
              </tr></xsl:if>

              <xsl:if test="uncertainty != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Uncertainty: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
              </tr></xsl:if>

              <xsl:if test="flag != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Quality flag convention: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="flag"/></td>
              </tr></xsl:if>

              <xsl:if test="methodReference != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Method reference: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="methodReference"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherName != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher name: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherName"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherInstitution != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher institution: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherInstitution"/></td>
              </tr></xsl:if>

            </table>

          </xsl:for-each>










          <!-- pH -->

          <xsl:for-each select="variable[internal = '3']">
            <hr/>

            <table width="940"><tr>
                <td style="font-weight:bold; font-style:italic; text-align:center;">pH</td>
            </tr></table>

            <br/>

            <table width="940">
              <xsl:if test="abbrev != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Abbreviation: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="abbrev"/></td>
              </tr></xsl:if>


              <xsl:if test="phscale != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">pH scale: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="phscale"/></td>
              </tr></xsl:if>


              <xsl:if test="observationType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Observation type: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="observationType"/></td>
              </tr></xsl:if>

              <xsl:if test="insitu != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">In-situ / Manipulation / Response variable: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="insitu"/></td>
              </tr></xsl:if>

              <xsl:if test="measured != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Measured or calculated: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="measured"/></td>
              </tr></xsl:if>

              <xsl:if test="calcMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Calculation method and parameters: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="calcMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="samplingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sampling instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="samplingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="analyzingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Analyzing instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="analyzingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="temperatureMeasure != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Temperature of pH measurement: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureMeasure"/></td>
              </tr></xsl:if>

              <xsl:if test="detailedInfo != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Detailed sampling and analyzing information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="detailedInfo"/></td>
              </tr></xsl:if>

              <xsl:if test="replicate != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Replicate information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="replicate"/></td>
              </tr></xsl:if>

              <!--standard-->
              <xsl:for-each select="standard">

                <xsl:if test="description != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization description: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="description"/></td>
                </tr></xsl:if>

                <xsl:if test="frequency != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization frequency: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="frequency"/></td>
                </tr></xsl:if>

                <xsl:if test="standardphvalues != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">pH standard values: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="standardphvalues"/></td>
                </tr></xsl:if>

                <xsl:if test="temperatureStandardization != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Temperature of standardization: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureStandardization"/></td>
                </tr></xsl:if>

              </xsl:for-each>

              <xsl:if test="temperatureCorrectionMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Temperature correction method: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureCorrectionMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="phReportTemperature != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">At what temperature was pH reported: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="phReportTemperature"/></td>
              </tr></xsl:if>

              <xsl:if test="uncertainty != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Uncertainty: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
              </tr></xsl:if>

              <xsl:if test="flag != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Quality flag convention: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="flag"/></td>
              </tr></xsl:if>

              <xsl:if test="methodReference != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Method reference: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="methodReference"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherName != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher name: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherName"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherInstitution != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher institution: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherInstitution"/></td>
              </tr></xsl:if>

            </table>

          </xsl:for-each>













          <!-- pCO2 autonomous -->

          <xsl:for-each select="variable[internal = '4']">
            <hr/>

            <table width="940"><tr>
                <td style="font-weight:bold; font-style:italic; text-align:center;">pCO2 (fCO2) autonomous</td>
            </tr></table>

            <br/>

            <table width="940">

              <xsl:if test="abbrev != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Abbreviation: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="abbrev"/></td>
              </tr></xsl:if>


              <xsl:if test="unit != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Unit: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="unit"/></td>
              </tr></xsl:if>

              <xsl:if test="observationType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Observation type: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="observationType"/></td>
              </tr></xsl:if>

              <xsl:if test="insitu != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">In-situ / Manipulation / Response variable: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="insitu"/></td>
              </tr></xsl:if>


              <xsl:if test="measured != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Measured or calculated: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="measured"/></td>
              </tr></xsl:if>

              <xsl:if test="calcMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Calculation method and parameters: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="calcMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="samplingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sampling instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="samplingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="locationSeawaterIntake != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Location of seawater intake: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="locationSeawaterIntake"/></td>
              </tr></xsl:if>

              <xsl:if test="depthSeawaterIntake != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Depth of seawater intake: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="depthSeawaterIntake"/></td>
              </tr></xsl:if>

              <xsl:if test="analyzingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Analyzing instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="analyzingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="detailedInfo != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Detailed sampling and analyzing information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="detailedInfo"/></td>
              </tr></xsl:if>

              <!--equilibrator-->
              <xsl:for-each select="equilibrator">

                <xsl:if test="type != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Equilibrator type: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="type"/></td>
                </tr></xsl:if>

                <xsl:if test="volume != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Equilibrator volume: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="volume"/></td>
                </tr></xsl:if>

                <xsl:if test="vented != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Is the equilibrator vented or not: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="vented"/></td>
                </tr></xsl:if>

                <xsl:if test="waterFlowRate != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Water flow rate: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="waterFlowRate"/></td>
                </tr></xsl:if>

                <xsl:if test="gasFlowRate != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Gas flow rate: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="gasFlowRate"/></td>
                </tr></xsl:if>

                <xsl:if test="temperatureEquilibratorMethod != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">How was temperature inside the equilibrator measured: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureEquilibratorMethod"/></td>
                </tr></xsl:if>

                <xsl:if test="pressureEquilibratorMethod != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">How was pressure inside the equilibrator measured: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="pressureEquilibratorMethod"/></td>
                </tr></xsl:if>

                <xsl:if test="dryMethod != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Drying method for gas: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="dryMethod"/></td>
                </tr></xsl:if>

              </xsl:for-each>

              <!--  gas detector for water-->
              <xsl:for-each select="gasDetector">

                <xsl:if test="manufacturer != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">SEA CO2 Gas detector manufacturer : </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="manufacturer"/></td>
                </tr></xsl:if>

                <xsl:if test="model != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">SEA CO2 Gas detector model: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="model"/></td>
                </tr></xsl:if>

                <xsl:if test="resolution != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">SEA CO2 Gas detector resolution: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="resolution"/></td>
                </tr></xsl:if>

                <xsl:if test="uncertainty != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">SEA CO2 Gas detector uncertainty: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
                </tr></xsl:if>

              </xsl:for-each>


              <!--  gas detector for atmosphere-->
              <xsl:for-each select="gasDetectorAtm">

                <xsl:if test="manufacturer != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">ATM CO2 Gas detector manufacturer: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="manufacturer"/></td>
                </tr></xsl:if>

                <xsl:if test="model != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">ATM CO2 Gas detector model: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="model"/></td>
                </tr></xsl:if>

                <xsl:if test="resolution != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">ATM CO2 Gas detector resolution: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="resolution"/></td>
                </tr></xsl:if>

                <xsl:if test="uncertainty != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">ATM CO2 Gas detector uncertainty: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
                </tr></xsl:if>

              </xsl:for-each>


              <!--  standardization -->
              <xsl:for-each select="standardization">

                <xsl:if test="description != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization technique: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="description"/></td>
                </tr></xsl:if>

                <xsl:if test="frequency != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization frequency: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="frequency"/></td>
                </tr></xsl:if>

                <xsl:for-each select="standardgas">

                  <xsl:if test="manufacturer != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">Standard gas manufacturer: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="manufacturer"/></td>
                  </tr></xsl:if>

                  <xsl:if test="concentration != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">Standard gas concentration: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="concentration"/></td>
                  </tr></xsl:if>

                  <xsl:if test="uncertainty != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">Standard gas uncertainty: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
                  </tr></xsl:if>

                </xsl:for-each>

              </xsl:for-each>

              <xsl:if test="waterVaporCorrection != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Water vapor correction method: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="waterVaporCorrection"/></td>
              </tr></xsl:if>

              <xsl:if test="temperatureCorrection != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Temperature correction method: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureCorrection"/></td>
              </tr></xsl:if>

              <xsl:if test="co2ReportTemperature != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">At what temperature was pCO2 reported: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="co2ReportTemperature"/></td>
              </tr></xsl:if>

              <xsl:if test="uncertainty != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Uncertainty: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
              </tr></xsl:if>

              <xsl:if test="flag != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Quality flag convention: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="flag"/></td>
              </tr></xsl:if>

              <xsl:if test="methodReference != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Method reference: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="methodReference"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherName != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher name: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherName"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherInstitution != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher institution: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherInstitution"/></td>
              </tr></xsl:if>

            </table>

          </xsl:for-each>












          <!-- pCO2 discrete -->

          <xsl:for-each select="variable[internal = '5']">

            <hr/>

            <table width="940"><tr>
                <td style="font-weight:bold; font-style:italic; text-align:center;">pCO2 (fCO2) discrete</td>
            </tr></table>

            <br/>

            <table width="940">

              <xsl:if test="abbrev != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Abbreviation: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="abbrev"/></td>
              </tr></xsl:if>



              <xsl:if test="unit != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Unit: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="unit"/></td>
              </tr></xsl:if>

              <xsl:if test="observationType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Observation type: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="observationType"/></td>
              </tr></xsl:if>

              <xsl:if test="insitu != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">In-situ / Manipulation / Response variable: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="insitu"/></td>
              </tr></xsl:if>


              <xsl:if test="measured != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Measured or calculated: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="measured"/></td>
              </tr></xsl:if>

              <xsl:if test="calcMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Calculation method and parameters: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="calcMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="samplingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sampling instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="samplingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="analyzingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Analyzing instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="analyzingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="storageMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Storage method: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="storageMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="seawatervol != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Seawater volume: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="seawatervol"/></td>
              </tr></xsl:if>

              <xsl:if test="headspacevol != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Headspace volume: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="headspacevol"/></td>
              </tr></xsl:if>

              <xsl:if test="temperatureMeasure != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Temperature of measurement: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureMeasure"/></td>
              </tr></xsl:if>

              <xsl:if test="detailedInfo != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Detailed sampling and analyzing information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="detailedInfo"/></td>
              </tr></xsl:if>

              <xsl:if test="replicate != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sample replicate information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="replicate"/></td>
              </tr></xsl:if>

              <!--  gas detector -->
              <xsl:for-each select="gasDetector">

                <xsl:if test="manufacturer != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Gas detector manufacturer: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="manufacturer"/></td>
                </tr></xsl:if>

                <xsl:if test="model != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Gas detector model: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="model"/></td>
                </tr></xsl:if>

                <xsl:if test="resolution != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Gas detector resolution: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="resolution"/></td>
                </tr></xsl:if>

                <xsl:if test="uncertainty != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Gas detector uncertainty: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
                </tr></xsl:if>

              </xsl:for-each>

              <!--  standardization -->
              <xsl:for-each select="standardization">

                <xsl:if test="description != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization technique: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="description"/></td>
                </tr></xsl:if>

                <xsl:if test="frequency != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Standardization frequency: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="frequency"/></td>
                </tr></xsl:if>

                <xsl:if test="temperatureStd != ''"><tr>
                    <td style="width:20%; text-align: right; font-weight:bold; ">Temperature of standardization: </td>
                    <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureStd"/></td>
                </tr></xsl:if>

                <xsl:for-each select="standardgas">

                  <xsl:if test="manufacturer != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">Standard gas manufacturer: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="manufacturer"/></td>
                  </tr></xsl:if>

                  <xsl:if test="concentration != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">Standard gas concentration: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="concentration"/></td>
                  </tr></xsl:if>

                  <xsl:if test="uncertainty != ''"><tr>
                      <td style="width:20%; text-align: right; font-weight:bold; ">Standard gas uncertainty: </td>
                      <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
                  </tr></xsl:if>

                </xsl:for-each>

              </xsl:for-each>

              <xsl:if test="waterVaporCorrection != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Water vapor correction method: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="waterVaporCorrection"/></td>
              </tr></xsl:if>

              <xsl:if test="temperatureCorrection != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Temperature correction method: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="temperatureCorrection"/></td>
              </tr></xsl:if>

              <xsl:if test="co2ReportTemperature != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">At what temperature was pCO2 reported: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="co2ReportTemperature"/></td>
              </tr></xsl:if>

              <xsl:if test="uncertainty != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Uncertainty: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
              </tr></xsl:if>

              <xsl:if test="flag != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Quality flag convention: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="flag"/></td>
              </tr></xsl:if>

              <xsl:if test="methodReference != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Method reference: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="methodReference"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherName != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher name: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherName"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherInstitution != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher institution: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherInstitution"/></td>
              </tr></xsl:if>

            </table>

          </xsl:for-each>













          <!--Variables-->

          <xsl:for-each select="variable[internal ='0']">

            <hr/>

            <table width="940"><tr>
                <td style="font-weight:bold; font-style:italic; text-align:center;"><xsl:value-of select="fullname"/></td>
            </tr></table>

            <br/>

            <table width="940">

              <xsl:if test="abbrev != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Abbreviation: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="abbrev"/></td>
              </tr></xsl:if>


              <xsl:if test="unit != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Unit: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="unit"/></td>
              </tr></xsl:if>

              <xsl:if test="controlledName != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Controlled vocabulary name: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="controlledName"/></td>
              </tr></xsl:if>

              <xsl:if test="observationType != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Observation type: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="observationType"/></td>
              </tr></xsl:if>

              <xsl:if test="insitu != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">In-situ / Manipulation / Response variable: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="insitu"/></td>
              </tr></xsl:if>


              <xsl:if test="measured != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Measured or calculated: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="measured"/></td>
              </tr></xsl:if>

              <xsl:if test="calcMethod != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Calculation method and parameters: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="calcMethod"/></td>
              </tr></xsl:if>

              <xsl:if test="samplingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sampling instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="samplingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="analyzingInstrument != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Analyzing instrument: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="analyzingInstrument"/></td>
              </tr></xsl:if>

              <xsl:if test="sensorManufacturer != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sensor manufacturer: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="sensorManufacturer"/></td>
              </tr></xsl:if>

              <xsl:if test="sensorModel != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sensor model: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="sensorModel"/></td>
              </tr></xsl:if>

              <xsl:if test="sensorResolution != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sensor resolution: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="sensorResolution"/></td>
              </tr></xsl:if>

              <xsl:if test="sensorCalibration != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sensor calibration: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="sensorCalibration"/></td>
              </tr></xsl:if>

              <xsl:if test="sensorDepth != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Sensor depth: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="sensorDepth"/></td>
              </tr></xsl:if>

              <xsl:if test="duration != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Duration: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="duration"/></td>
              </tr></xsl:if>

              <xsl:if test="detailedInfo != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Detailed sampling and analyzing information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="detailedInfo"/></td>
              </tr></xsl:if>

              <xsl:if test="replicate != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Replicate information: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="replicate"/></td>
              </tr></xsl:if>

              <xsl:if test="uncertainty != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Uncertainty: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="uncertainty"/></td>
              </tr></xsl:if>

              <xsl:if test="flag != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Quality flag convention: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="flag"/></td>
              </tr></xsl:if>

              <xsl:if test="methodReference != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Method reference: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="methodReference"/></td>
              </tr></xsl:if>

              <xsl:if test="biologicalSubject != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Biological subject: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="biologicalSubject"/></td>
              </tr></xsl:if>

              <xsl:if test="speciesID != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Species ID: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="speciesID"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherName != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher name: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherName"/></td>
              </tr></xsl:if>

              <xsl:if test="researcherInstitution != ''"><tr>
                  <td style="width:20%; text-align: right; font-weight:bold; ">Researcher institution: </td>
                  <td style="padding-left:10px; vertical-align:middle; "><xsl:value-of select="researcherInstitution"/></td>
              </tr></xsl:if>

            </table>

          </xsl:for-each>

          <hr/>
          <br/>

          <span style="font-weight:bold">DATA PACKAGES RELATED TO THIS ONE:</span><br/>
          <xsl:if test="related/name != ''">
            <span style="padding-left:20px;"></span>
            <xsl:for-each select="related">
              <a href="{link}"><xsl:value-of select="name"/></a>;
            </xsl:for-each>
          </xsl:if>

          <br/><br/>

          <span style="font-weight:bold">PUBLICATIONS DESCRIBING THIS DATA SET: </span><br/>
          <xsl:if test="reference != ''">
            <xsl:for-each select="reference">
              <span style="padding-left:20px;"></span><xsl:apply-templates/><br/>
            </xsl:for-each>
          </xsl:if>

          <br/>

          <span style="font-weight:bold">ADDITIONAL INFORMATION: </span><br/>
          <span style="padding-left:20px;"></span>
          <xsl:if test="suppleInfo != ''">
            <xsl:for-each select="suppleInfo">
              <span style="padding-left:20px;"><xsl:apply-templates/></span><br/>
            </xsl:for-each>
          </xsl:if>
          <br/>

          <span style="font-weight:bold">FUNDING AGENCY: </span><br/>
          <xsl:for-each select="fundingAgency">
            <span style="padding-left:20px;"><xsl:value-of select="agency"/></span><br/>
            <span style="font-style:italic; padding-left:40px;">PROJECT TITLE: </span><xsl:value-of select="title"/><br/>
            <span style="font-style:italic; padding-left:40px;">PROJECT ID: </span><xsl:value-of select="ID"/><br/><br/>
          </xsl:for-each>

          <span style="font-weight:bold">SUBMITTED BY: </span>
          <xsl:for-each select="datasubmitter">
            <span style="padding-left:3px;"><xsl:value-of select="name"/> (<xsl:value-of select="email"/>) </span><br/>
          </xsl:for-each>
          <br/>

          <span style="font-weight:bold">SUBMISSION DATE: </span>
          <xsl:value-of select="submissionDate"/>
          <br/><br/>

          <span style="font-weight:bold">REVISION DATE: </span>
          <xsl:value-of select="revisionDate"/>
          <br/><br/>

          <span style="font-weight:bold">PREVIOUS VERSIONS: </span>
          <xsl:for-each select="old">
            <span style="padding-left:5px;"><a href="{link}" target="_blank"><xsl:value-of select="name"/></a></span>
          </xsl:for-each>
          <br/><br/>

        </div>

        <!-- FOOTER from ../oceans/includes/footlinks.inc -->
        <div style="text-align:center;"><br/>
          <span style="color:white">Last updated on: <xsl:value-of select="update"/></span><br/>
          <span style="color:white"><a href="https://www.commerce.gov/" title="U.S. Department of Commerce" target="_blank" style="color:white;">DOC</a></span>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="http://www.noaa.gov/" title="National Oceanic and Atmospheric Administration" target="_blank"  style="color:white;">NOAA</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="https://www.nesdis.noaa.gov/" title="National Environmental Satellite, Data, and Information Service" target="_blank"  style="color:white;">NESDIS</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="https://www.ncei.noaa.gov/" title="National Centers for Environmental Information" target="_blank"  style="color:white;">NCEI</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="mailto:NCEI.Info@noaa.gov" title="Send mail to NCEI" target="_blank"  style="color:white;">NCEI.Info@noaa.gov</a>
          <br/>

          <a href="/about/disclaimer.html" title="NOAA Disclaimer" target="_blank"  style="color:white;">Disclaimer</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="http://www.noaa.gov/privacy.html" title="NOAA Privacy Policy" target="_blank"  style="color:white;">Privacy Policy</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="http://www.copyright.gov/title17/92chap4.html#403" title="Copyright Notice" target="_blank"  style="color:white;">Copyright Notice</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="http://www.rdc.noaa.gov/~foia/" title="Freedom of Information Act" target="_blank"  style="color:white;">FOIA</a>
          <br/>

          <a href="/survey.html" title="NCEI, Maryland Office, Website Survey" target="_blank"  style="color:white;">Take our survey</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="http://www.cio.noaa.gov/services_programs/info_quality.html" title="Information Quality" target="_blank"  style="color:white;">Info Quality</a>
          <xsl:text>&#160;&#160;|&#160;&#160;</xsl:text>
          <a href="http://www.usa.gov/" title="U.S. Government's Official Web Portal" target="_blank"  style="color:white;">USA.gov</a>
          <br/><br/>

          <a href="https://twitter.com/NOAANCEIocngeo"><img src="/media/images/common/twitter3.gif" alt="Like us on Twitter" width="20" height="20" /></a>
          <a href="http://www.facebook.com/NOAANCEIoceangeo"><img src="/media/images/common/facebook3.gif" alt="Like us on Facebook" width="20" height="20" /></a>
          <a href="/rss/"><img src="/media/images/common/rssfeed-icon2.jpg" alt="RSS feed" width="20" height="20" /></a>
          <br/><br/><br/>
        </div>
        <!-- footer ends-->

      </body>
    </html>

  </xsl:template>

</xsl:stylesheet>
