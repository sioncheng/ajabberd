package me.babaili.ajabberd.xmpp

import me.babaili.ajabberd.protocol.{JID, Packet}

/**
  * Created by cyq on 27/03/2017.
  */
object XmppEvents {

    trait Action
    object IqBind extends Action
    object IqResult extends Action
    object MessageRequest extends Action
    object MessageResponse extends Action

    case class Command(action: Action, uid: String, jid: Option[JID], data: Packet)
    case class Response(action: Action, data: Packet)
    case class JidAssign(jid: JID, uid: String)
    case class ConnectionClosed(jid: JID, uid: String)

}
