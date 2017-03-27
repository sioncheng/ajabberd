package me.babaili.ajabberd.global

import akka.actor.{ActorRef, ActorSystem, Props}
import me.babaili.ajabberd.xmpp.{XmppServer}

/**
  * Created by chengyongqiao on 18/03/2017.
  */
object ApplicationContext {

    def start(actorSystem: ActorSystem) = {
        xmppServer = actorSystem.actorOf(Props(classOf[XmppServer]))
    }

    def setService(service: String) = {
        this.service = service
    }


    def getXmppServer() = xmppServer

    def getService() = this.service

    private var xmppServer: ActorRef = null
    private var service: String = ""
}
