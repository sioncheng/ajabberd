package me.babaili.ajabberd.xmpp

import akka.actor.ActorRef
import me.babaili.ajabberd.protocol.JID

import scala.collection.mutable.ListBuffer

/**
  * Created by chengyongqiao on 30/05/2017.
  */
class ClientActorRefs(val bareJid: JID) {

    var actors: scala.collection.mutable.ListBuffer[ClientActorRef] =
        new ListBuffer[ClientActorRef]()

    def add(clientInfo: ClientActorRef): Option[ClientActorRef] = {
        val car = actors.find(x => x.jid.equals(clientInfo.jid))
        if (!car.isEmpty) {
            actors.-=(car.get)
        }
        actors.+=(clientInfo)
        actors = actors.sortBy( x => x.priority)

        car
    }

    def first(): Option[ClientActorRef] = actors.headOption

    def clients = actors.clone()
}


case class ClientActorRef(priority: Int, jid: JID, actor: ActorRef)