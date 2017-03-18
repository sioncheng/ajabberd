package me.babaili.ajabberd.global

import akka.actor.{ActorRef, ActorSystem, Props}
import me.babaili.ajabberd.xmpp.XmppServer

/**
  * Created by chengyongqiao on 18/03/2017.
  */
object ApplicationContext {

    def start(actorSystem: ActorSystem) = {
        xmppServer = actorSystem.actorOf(Props(classOf[XmppServer]))
    }

    def getXmppServer() = xmppServer

    private var xmppServer: ActorRef = null
}
