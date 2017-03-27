package me.babaili.ajabberd.xmpp

import me.babaili.ajabberd.protocol.{JID, Packet, iq, message}
import me.babaili.ajabberd.protocol.extensions.{bind, session}
import akka.actor.Actor
import me.babaili.ajabberd.data.Passport
import me.babaili.ajabberd.global.ApplicationContext
import me.babaili.ajabberd.util.Logger

/**
  * Created by cyq on 27/03/2017.
  */

object XmppServer {
}


class XmppServer extends Actor {

    val logger = new Logger {}

    import  XmppEvents._

    def receive = {
        case iqb @ Command(IqBind, uid, data) =>
            //
            val iqBind = data.asInstanceOf[iq.IQ]

            iqBind match {
                case iq.Set (_, _, _, Some (request: bind.BindRequest) ) =>
                    val id = iqBind.id.getOrElse ("")
                    val jid = JID (uid, ApplicationContext.getService (), request.resource.getOrElse (id.toString) )

                    val iqXml = <iq type='result' id={id}>
                    <bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'> <jid>{jid.toString ()}</jid> </bind>
                    </iq>

                    val iqResult = iq.Result (iqXml)

                    sender () ! Response (IqResult, iqResult)
                    sender () ! JidAssign (jid.toString (), uid)
                case set @ iq.Set(_, _, _, Some (_: session.Session)) =>
                    val res = set.result(Some(session.Session()))
                    sender () ! Response (IqResult, res)
                case x =>
                    logger.warn(s"what iq? ${x}")
            }
        case msg @ Command(MessageRequest, uid, data) =>
            data match {
                case chat : message.Chat =>
                    logger.warn(s"chat ${chat}")
                case _ =>
                    logger.warn("？？？？ message")
            }
            logger.warn(s"${data.isInstanceOf[message.Chat]}")
            logger.warn(s"${data}")
    }
}
