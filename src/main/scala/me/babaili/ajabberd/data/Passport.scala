package me.babaili.ajabberd.data

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

    def queryUserJid(user: String): String = {
        user match {
            case "aa" => "jid_aa"
            case _ => ""
        }
    }
}


class Passport  {

}
