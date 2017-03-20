package me.babaili.ajabberd.app

import akka.actor.{ActorSystem, Props}
import me.babaili.ajabberd.global.ApplicationContext
import me.babaili.ajabberd.net.TcpListener


/**
  * Created by cyq on 06/03/2017.
  */
object SimpleServerTestApp extends App {

    println("simple server test app")

    //System.setProperty("javax.net.debug", "all")
    //System.setProperty("javax.net.ssl.trustStore", "trustStore")

    val actorSystem = ActorSystem.create("simple-server-test")

    //ApplicationContext.start(actorSystem)

    val listener = actorSystem.actorOf(Props(classOf[TcpListener], "localhost", 5222))


    Thread.sleep(3 * 60 * 1000)

    actorSystem.terminate()


}
