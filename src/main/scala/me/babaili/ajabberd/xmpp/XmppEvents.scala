package me.babaili.ajabberd.xmpp

import me.babaili.ajabberd.protocol.Packet

/**
  * Created by cyq on 27/03/2017.
  */
object XmppEvents {

    trait Action
    object IqBind extends Action
    object IqResult extends Action
    object MessageRequest extends Action
    object MessageResponse extends Action

    case class Command(action: Action, uid: String, data: Packet)
    case class Response(action: Action, data: Packet)
    case class JidAssign(jid: String, uid: String)
    case class ConnectionClosed(jid: String, uid: String)

}
