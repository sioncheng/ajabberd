package me.babaili.ajabberd
package protocol
package extensions
package get

import scala.xml.Node

/**
  * Created by chengyongqiao on 08/04/2017.
  */

//https://xmpp.org/extensions/xep-0049.html

class PrivateQuery(xml: Node) extends Query(xml) {

}

object PrivateQuery {
    val tag = Query.tag
    val namespace = "jabber:iq:private"

    def apply(xml: Node): PrivateQuery = new PrivateQuery(xml)
}

private [ajabberd] object PrivateQueryBuilder extends ExtensionBuilder[PrivateQuery] {
    val tag = PrivateQuery.tag
    val namespace = PrivateQuery.namespace

    def apply(xml: Node) = PrivateQuery(xml)
}