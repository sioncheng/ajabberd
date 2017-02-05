package me.babaili.ajabberd.util

/**
  * Created by chengyongqiao on 03/02/2017.
  */
object Logger {

    def debug(title: String, message: String): Unit = {
        println(s"[$title]    $message")
    }

    def info(title: String, message: String): Unit = {
        println(s"[$title]    $message")
    }

    def error(title: String, message: String): Unit = {
        println(s"[$title]    $message")
    }

}
