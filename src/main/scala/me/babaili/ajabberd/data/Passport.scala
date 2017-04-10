package me.babaili.ajabberd.data

import me.babaili.ajabberd.protocol.JID

/**
  * Created by cyq on 27/03/2017.
  */

object Passport {

    def validate(user: String, password: String): Boolean = "" != password && queryUserPassword(user) == password


    def queryUserPassword(user: String): String = {
        user match {
            case "aa" => "bbb"
            case "bb" => "bbb"
            case "cc" => "bbb"
            case _ => ""
        }
    }

    def queryUserJid(user: String): Option[String] = {
        user match {
            case "aa" => Some("aa")
            case "bb" => Some("bb")
            case "cc" => Some("cc")
            case _ => None
        }
    }
}


class Passport  {

}
