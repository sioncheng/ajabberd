package me.babaili.ajabberd
package protocol
package extensions
package get


import scala.xml.Node

/**
  * Created by chengyongqiao on 08/04/2017.
  */

//https://xmpp.org/extensions/xep-0012.html

class LastQuery(xml: Node) extends Query(xml){

}

object LastQuery {
    val tag = Query.tag
    val namespace = "jabber:iq:last"

    def apply(xml: Node): LastQuery = new LastQuery(xml)
}

private [ajabberd] object LastQueryBuilder extends ExtensionBuilder[LastQuery] {
    val tag = LastQuery.tag
    val namespace = LastQuery.namespace

    def apply(xml: Node): LastQuery = LastQuery(xml)
}