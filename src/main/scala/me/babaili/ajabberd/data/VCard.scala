package me.babaili.ajabberd.data

import scala.xml.Elem

/**
  * Created by cyq on 10/04/2017.
  */
class VCard {

}

object VCard {
    def getVcard(uid: String): Option[Elem] = {
        uid match {
            case "aa" =>
                val card = <vCard xmlns='vcard-temp'>
                    <FN>AA Localhost</FN>
                    <N>
                        <FAMILY>Localhost</FAMILY>
                        <GIVEN>AA</GIVEN>
                        <MIDDLE/>
                    </N>
                    <NICKNAME>stpeter</NICKNAME>
                    <URL>http://www.xmpp.org/xsf/people/stpeter.shtml</URL>
                    <BDAY>1966-08-06</BDAY>
                    <ORG>
                        <ORGNAME>XMPP Standards Foundation</ORGNAME>
                        <ORGUNIT/>
                    </ORG>
                    <TITLE>Executive Director</TITLE>
                    <ROLE>Patron Saint</ROLE>
                    <TEL>
                        <WORK/>
                        <VOICE/> <NUMBER>303-308-3282</NUMBER>
                    </TEL>
                    <TEL>
                        <WORK/>
                        <FAX/>
                        <NUMBER/>
                    </TEL>
                    <TEL>
                        <WORK/>
                        <MSG/>
                        <NUMBER/>
                    </TEL>
                    <ADR>
                        <WORK/>
                        <EXTADD>Suite 600</EXTADD>
                        <STREET>1899 Wynkoop Street</STREET>
                        <LOCALITY>Denver</LOCALITY>
                        <REGION>CO</REGION>
                        <PCODE>80202</PCODE>
                        <CTRY>USA</CTRY>
                    </ADR>
                    <TEL>
                        <HOME/>
                        <VOICE/> <NUMBER>303-555-1212</NUMBER>
                    </TEL>
                    <TEL>
                        <HOME/>
                        <FAX/>
                        <NUMBER/>
                    </TEL>
                    <TEL>
                        <HOME/>
                        <MSG/>
                        <NUMBER/>
                    </TEL>
                    <ADR>
                        <HOME/>
                        <EXTADD/>
                        <STREET/>
                        <LOCALITY>Denver</LOCALITY>
                        <REGION>CO</REGION>
                        <PCODE>80209</PCODE>
                        <CTRY>USA</CTRY>
                    </ADR>
                    <EMAIL>
                        <INTERNET/>
                        <PREF/> <USERID>aa@localhost</USERID>
                    </EMAIL>
                    <JABBERID>aa@localhost</JABBERID>
                    <DESC>
                        More information about me is located on my
                        personal website: http://www.saint-andre.com/
                    </DESC>
                </vCard>

                Some(card)
            case "bb" =>
                val card = <vCard xmlns='vcard-temp'>
                    <FN>BB Localhost</FN>
                    <N>
                        <FAMILY>Localhost</FAMILY>
                        <GIVEN>BB</GIVEN>
                        <MIDDLE/>
                    </N>
                    <NICKNAME>stpeter</NICKNAME>
                    <URL>http://www.xmpp.org/xsf/people/stpeter.shtml</URL>
                    <BDAY>1966-08-06</BDAY>
                    <ORG>
                        <ORGNAME>XMPP Standards Foundation</ORGNAME>
                        <ORGUNIT/>
                    </ORG>
                    <TITLE>Executive Director</TITLE>
                    <ROLE>Patron Saint</ROLE>
                    <TEL>
                        <WORK/>
                        <VOICE/> <NUMBER>303-308-3282</NUMBER>
                    </TEL>
                    <TEL>
                        <WORK/>
                        <FAX/>
                        <NUMBER/>
                    </TEL>
                    <TEL>
                        <WORK/>
                        <MSG/>
                        <NUMBER/>
                    </TEL>
                    <ADR>
                        <WORK/>
                        <EXTADD>Suite 600</EXTADD>
                        <STREET>1899 Wynkoop Street</STREET>
                        <LOCALITY>Denver</LOCALITY>
                        <REGION>CO</REGION>
                        <PCODE>80202</PCODE>
                        <CTRY>USA</CTRY>
                    </ADR>
                    <TEL>
                        <HOME/>
                        <VOICE/> <NUMBER>303-555-1212</NUMBER>
                    </TEL>
                    <TEL>
                        <HOME/>
                        <FAX/>
                        <NUMBER/>
                    </TEL>
                    <TEL>
                        <HOME/>
                        <MSG/>
                        <NUMBER/>
                    </TEL>
                    <ADR>
                        <HOME/>
                        <EXTADD/>
                        <STREET/>
                        <LOCALITY>Denver</LOCALITY>
                        <REGION>CO</REGION>
                        <PCODE>80209</PCODE>
                        <CTRY>USA</CTRY>
                    </ADR>
                    <EMAIL>
                        <INTERNET/>
                        <PREF/> <USERID>bb@localhost</USERID>
                    </EMAIL>
                    <JABBERID>bb@localhost</JABBERID>
                    <DESC>
                        More information about me is located on my
                        personal website: http://www.saint-andre.com/
                    </DESC>
                </vCard>

                Some(card)
            case "cc" =>
                val card = <vCard xmlns='vcard-temp'>
                    <FN>CC Localhost</FN>
                    <N>
                        <FAMILY>Localhost</FAMILY>
                        <GIVEN>CC</GIVEN>
                        <MIDDLE/>
                    </N>
                    <NICKNAME>stpeter</NICKNAME>
                    <URL>http://www.xmpp.org/xsf/people/stpeter.shtml</URL>
                    <BDAY>1966-08-06</BDAY>
                    <ORG>
                        <ORGNAME>XMPP Standards Foundation</ORGNAME>
                        <ORGUNIT/>
                    </ORG>
                    <TITLE>Executive Director</TITLE>
                    <ROLE>Patron Saint</ROLE>
                    <TEL>
                        <WORK/>
                        <VOICE/> <NUMBER>303-308-3282</NUMBER>
                    </TEL>
                    <TEL>
                        <WORK/>
                        <FAX/>
                        <NUMBER/>
                    </TEL>
                    <TEL>
                        <WORK/>
                        <MSG/>
                        <NUMBER/>
                    </TEL>
                    <ADR>
                        <WORK/>
                        <EXTADD>Suite 600</EXTADD>
                        <STREET>1899 Wynkoop Street</STREET>
                        <LOCALITY>Denver</LOCALITY>
                        <REGION>CO</REGION>
                        <PCODE>80202</PCODE>
                        <CTRY>USA</CTRY>
                    </ADR>
                    <TEL>
                        <HOME/>
                        <VOICE/> <NUMBER>303-555-1212</NUMBER>
                    </TEL>
                    <TEL>
                        <HOME/>
                        <FAX/>
                        <NUMBER/>
                    </TEL>
                    <TEL>
                        <HOME/>
                        <MSG/>
                        <NUMBER/>
                    </TEL>
                    <ADR>
                        <HOME/>
                        <EXTADD/>
                        <STREET/>
                        <LOCALITY>Denver</LOCALITY>
                        <REGION>CO</REGION>
                        <PCODE>80209</PCODE>
                        <CTRY>USA</CTRY>
                    </ADR>
                    <EMAIL>
                        <INTERNET/>
                        <PREF/> <USERID>cc@localhost</USERID>
                    </EMAIL>
                    <JABBERID>cc@localhost</JABBERID>
                    <DESC>
                        More information about me is located on my
                        personal website: http://www.saint-andre.com/
                    </DESC>
                </vCard>

                Some(card)
            case _ => None
        }
    }
}
