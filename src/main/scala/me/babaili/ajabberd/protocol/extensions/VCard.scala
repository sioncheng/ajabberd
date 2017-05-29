package me.babaili.ajabberd
package protocol
package extensions


import scala.xml.Node

/**
  * Created by chengyongqiao on 30/04/2017.
  *
  * https://xmpp.org/extensions/xep-0054.html
  */
class VCard(xml: Node)  extends Extension(xml){

}


object VCard {
    val tag = "vCard"
    val namespace = "vcard-temp"

    def apply(xml: Node): VCard = new VCard(xml)
}

private [ajabberd] object VCardBuilder extends ExtensionBuilder[VCard] {

    val tag = VCard.tag
    val namespace = VCard.namespace

    def apply(xml: Node): VCard = new VCard(xml)
}