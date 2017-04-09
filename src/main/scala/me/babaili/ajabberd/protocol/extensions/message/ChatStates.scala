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

    def unapply(arg: ActiveState): Option[Node] = Some(arg.xml)
}

class ActiveState(xml: Node) extends Extension(xml) {

}

object InactiveState {
    val tag = "inactive"
    val namespace = ChatStates.namespace

    def apply(xml: Node): InactiveState = new InactiveState(xml)

    def unapply(arg: InactiveState): Option[Node] = Some(arg.xml)
}

class InactiveState (xml: Node) extends Extension(xml) {
    
}

object GoneState {
    val tag = "gone"
    val namespace = ChatStates.namespace

    def apply(xml: Node): GoneState = new GoneState(xml)

    def unapply(arg: GoneState): Option[Node] = Some(arg.xml)
}

class GoneState (xml: Node) extends Extension(xml) {

}

object ComposingState {
    val tag = "composing"
    val namespace = ChatStates.namespace

    def apply(xml: Node): ComposingState = new ComposingState(xml)

    def unapply(arg: ComposingState): Option[Node] = Some(arg.xml)
}

class ComposingState (xml: Node) extends Extension(xml) {
    
}

object PausedState {
    val tag = "paused"
    val namespace = ChatStates.namespace

    def apply(xml: Node): PausedState = new PausedState(xml)

    def unapply(arg: PausedState): Option[Node] = Some(arg.xml)
}

class PausedState (xml: Node) extends Extension(xml) {

}
