package me.babaili.ajabberd.xmpp

import me.babaili.ajabberd.protocol.{JID, Packet, iq, message}
import me.babaili.ajabberd.protocol.extensions.{bind, session}
import akka.actor.Actor
import me.babaili.ajabberd.data.Passport
import me.babaili.ajabberd.global.ApplicationContext
import me.babaili.ajabberd.protocol.message.{Chat, Message}
import me.babaili.ajabberd.util.Logger

import scala.collection.mutable

/**
  * Created by cyq on 27/03/2017.
  */

object XmppServer {
}


class XmppServer extends Actor {

    val logger = new Logger {}
    val connections: scala.collection.mutable.HashMap[JID, ClientActorRefs] =
        new mutable.HashMap[JID, ClientActorRefs]()

    import  XmppEvents._

    def receive = {
        case iqb @ Command(IqBind, uid, Some(jid), data) =>
            //
            logger.info(s"iq bind ${uid} ${jid} ${data}")
            if (!connections.contains(jid.bare)) {
                connections.put(jid.bare, new ClientActorRefs(jid.bare))
            }
            val clientActorRefs = connections.get(jid.bare).get
            val carNew = ClientActorRef(1, jid, sender())
            val car = clientActorRefs.add(carNew)
            if (!car.isEmpty) {
                logger.warn(s"same client info existed ${car}")
            }
            logger.info(s"add client actor ref ${carNew}")
        case msg @ Command(MessageRequest, uid, jid, data) =>
            data match {
                case chat : message.Chat =>
                    logger.info(s"chat ${chat}")
                    val to = chat.to.getOrElse(JID("","",""))
                    if (to.isEmpty) {
                        logger.warn("have no idea to whom")
                    } else {

                        val from = chat.from.getOrElse(jid.getOrElse(JID.EmptyJID))
                        if (from.isEmpty) {
                            logger.warn("have no idea from where")
                        }

                        val clientActorRefs = connections.get(to.bare)
                        if (clientActorRefs.isEmpty) {
                            //
                            logger.warn(s"${to} is not online, will store the message.")
                        } else {
                            val car = clientActorRefs.get.first().get

                            val responseXml = <message type="chat" from={from.toString} to={car.jid.toString}>
                                <body>{chat.body.getOrElse("?")}</body>
                            </message>

                            car.actor ! Response(MessageResponse, Message(responseXml))
                        }

                    }

                case _ =>
                    logger.warn("???????message")
            }
            logger.warn(s"${data.isInstanceOf[message.Chat]}")
            logger.warn(s"${data}")
    }
}
