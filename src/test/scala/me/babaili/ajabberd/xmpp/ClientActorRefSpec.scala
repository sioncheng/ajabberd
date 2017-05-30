package me.babaili.ajabberd.xmpp

import me.babaili.ajabberd.protocol.JID
import org.scalatest.{FunSpecLike, Matchers}

/**
  * Created by chengyongqiao on 30/05/2017.
  */
class ClientActorRefSpec extends FunSpecLike with Matchers{

    describe("given a client actor refs") {
        it("should be able to manage client actor ref") {
            val bareJID = JID("aa@localhost")
            val clientActorRefs = new ClientActorRefs(bareJID)
            clientActorRefs.bareJid should equal(bareJID)
            val jid1 = JID("aa@localhost/jid1")
            val car = clientActorRefs.add(ClientActorRef(1, jid1, null))
            car.isEmpty should be (true)
            val jid2 = JID("aa@localhost/jid2")
            val car2 = clientActorRefs.add(ClientActorRef(2, jid2, null))
            car2.isEmpty should be (true)
            val car3 = clientActorRefs.add(ClientActorRef(2, jid2, null))
            car3.isEmpty should be (false)
            car3.get.jid should equal(jid2)
            val clients = clientActorRefs.clients
            clients.size should be (2)
            clients.+=(ClientActorRef(2, jid2, null))
            clients.size should be (3)
            clientActorRefs.clients.size should be (2)
        }
    }
}
