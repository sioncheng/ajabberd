package me.babaili.ajabberd
package protocol
package extensions
package message

import scala.xml.Node

/**
  * Created by chengyongqiao on 08/04/2017.
  */
class Builders {

}

private [ajabberd] object ActiveStateBuilder extends ExtensionBuilder[ActiveState] {
    val tag = ActiveState.tag
    val namespace = ActiveState.namespace

    def apply(xml: Node) = ActiveState(xml)
}


private [ajabberd] object InactiveStateBuilder extends ExtensionBuilder[InactiveState] {
    val tag = InactiveState.tag
    val namespace = InactiveState.namespace

    def apply(xml: Node) = InactiveState(xml)
}


private [ajabberd] object ComposingStateBuilder extends ExtensionBuilder[ComposingState] {
    val tag = ComposingState.tag
    val namespace = ComposingState.namespace

    def apply(xml: Node) = ComposingState(xml)
}


private [ajabberd] object GoneStateBuilder extends ExtensionBuilder[GoneState] {
    val tag = GoneState.tag
    val namespace = GoneState.namespace

    def apply(xml: Node) = GoneState(xml)
}


private [ajabberd] object PausedStateBuilder extends ExtensionBuilder[PausedState] {
    val tag = PausedState.tag
    val namespace = PausedState.namespace

    def apply(xml: Node) = PausedState(xml)
}