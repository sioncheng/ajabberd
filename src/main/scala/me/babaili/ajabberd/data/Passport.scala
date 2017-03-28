package me.babaili.ajabberd.data

import me.babaili.ajabberd.protocol.JID

/**
  * Created by cyq on 27/03/2017.
  */

object Passport {
    def queryUserPassword(user: String): String = {
        user match {
            case "aa" => "bbb"
            case _ => ""
        }
    }

    def queryUserJid(user: String): Option[String] = {
        user match {
            case "aa" => Some("jid_aa")
            case _ => None
        }
    }
}


class Passport  {

}
