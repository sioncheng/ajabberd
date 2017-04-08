package me.babaili.ajabberd
package protocol
package extensions
package message

import scala.xml.Node

/**
  * Created by chengyongqiao on 08/04/2017.
  */

//https://xmpp.org/extensions/xep-0085.html

object ChatStates {
    val namespace = "http://jabber.org/protocol/chatstates"
}

object ActiveState {
    val tag = "active"
    val namespace = ChatStates.namespace

    def apply(xml: Node): ActiveState = new ActiveState(xml)
}

class ActiveState(xml: Node) extends Extension(xml) {

}

object InactiveState {
    val tag = "inactive"
    val namespace = ChatStates.namespace

    def apply(xml: Node): InactiveState = new InactiveState(xml)
}

class InactiveState (xml: Node) extends Extension(xml) {
    
}

object GoneState {
    val tag = "gone"
    val namespace = ChatStates.namespace

    def apply(xml: Node): GoneState = new GoneState(xml)
}

class GoneState (xml: Node) extends Extension(xml) {

}

object ComposingState {
    val tag = "composing"
    val namespace = ChatStates.namespace

    def apply(xml: Node): ComposingState = new ComposingState(xml)
}

class ComposingState (xml: Node) extends Extension(xml) {
    
}

object PausedState {
    val tag = "paused"
    val namespace = ChatStates.namespace

    def apply(xml: Node): PausedState = new PausedState(xml)
}

class PausedState (xml: Node) extends Extension(xml) {

}
