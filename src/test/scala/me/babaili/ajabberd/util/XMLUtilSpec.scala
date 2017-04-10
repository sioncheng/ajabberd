package me.babaili.ajabberd.util

import me.babaili.ajabberd.data.Roster
import org.scalatest.{FunSpecLike, Matchers}

import scala.xml.Elem

/**
  * Created by cyq on 10/04/2017.
  */
class XMLUtilSpec extends FunSpecLike with Matchers {

    describe("") {
        it("") {
            var parent:Elem = <query xmlns="jabber:iq:roster"></query>
            val friends = Roster.getFriends("aa")
            if (!friends.isEmpty) {
                println("not empty")
                friends.get.foreach(friend => parent = XMLUtil.addChild(parent,
                    <item jid={friend.toString} name={friend.node} subscription="both"><group>Friends</group></item>))
            }
            println(parent)

            val xml = <iq id="aa" type="result" to="">
                {parent}
            </iq>

            println(xml)
        }
    }
}
