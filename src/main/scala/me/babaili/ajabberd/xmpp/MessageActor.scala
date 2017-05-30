package me.babaili.ajabberd.xmpp

import akka.actor.Actor
import me.babaili.ajabberd.protocol.message.Message
import me.babaili.ajabberd.util.Logger

/**
  * Created by chengyongqiao on 30/05/2017.
  */
class MessageActor extends Actor {

    val logger = new Logger{}

    def receive = {
        case message: Message =>
            //
            logger.info(s"got message ${message}")
        case x: Any =>
            //
            logger.error(s"do not understand ${x}")
    }

}
