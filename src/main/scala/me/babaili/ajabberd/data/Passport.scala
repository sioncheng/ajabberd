package me.babaili.ajabberd.data

import me.babaili.ajabberd.protocol.JID

/**
  * Created by cyq on 27/03/2017.
  */

object Passport {
    def queryUserPassword(user: String): String = {
        user match {
            case "aa" => "bbb"
            case "bb" => "bbb"
            case "cc" => "ccc"
            case _ => ""
        }
    }

    def queryUserJid(user: String): Option[String] = {
        user match {
            case "aa" => Some("jid_aa")
            case "bb" => Some("jid_bb")
            case "cc" => Some("jid_cc")
            case _ => None
        }
    }
}


class Passport  {

}
