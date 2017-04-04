package me.babaili.ajabberd
package protocol
package extensions
package privacy



import scala.xml._


/**
  * Created by sion on 4/4/17.
  */

private[ajabberd] object Builder extends ExtensionBuilder[Query]
{
    val tag = Query.tag
    val namespace = "jabber:iq:privacy"

    def apply(xml:Node):Query = {
        println(s"=============== privacy builder ${xml.toString()}")
        PrivacyQuery(xml)
    }
}