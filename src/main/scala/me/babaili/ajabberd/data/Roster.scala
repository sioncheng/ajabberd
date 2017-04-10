package me.babaili.ajabberd.data

import me.babaili.ajabberd.protocol.JID

/**
  * Created by cyq on 10/04/2017.
  */
class Roster {

}

object Roster {
    def getFriends(uid: String):Option[List[JID]] = {
        uid match {
            case "aa" => Some(List(JID("bb@localhost"), JID("cc@localhost")))
            case "bb" => Some(List(JID("aa@localhost"), JID("cc@localhost")))
            case "cc" => Some(List(JID("aa@localhost"), JID("bb@localhost")))
            case _ => None
        }
    }

}
