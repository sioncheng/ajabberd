package me.babaili.ajabberd.xmpp

import akka.actor.Actor

import me.babaili.ajabberd.util

/**
  * Created by cyq on 14/03/2017.
  */
class XmppServer extends Actor with util.Logger {

    def receive = {
        case _ =>
            error("what?")
    }
}
