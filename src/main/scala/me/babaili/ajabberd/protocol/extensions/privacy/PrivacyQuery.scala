package me.babaili.ajabberd
package protocol
package extensions
package privacy

import scala.xml._


/**
  * Created by sion on 4/4/17.
  */

object PrivacyQuery {
    val tag = Query.tag
    val namespace = "jabber:iq:privacy"

    def apply(xml: Node): PrivacyQuery = new PrivacyQuery(xml)
}

class PrivacyQuery(xml: Node) extends Query(xml) {

}
