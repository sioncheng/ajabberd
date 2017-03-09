package me.babaili.ajabberd.app

import java.util
import javax.security.auth.callback.{Callback, CallbackHandler}
import javax.security.sasl.Sasl

import scala.collection.immutable.TreeMap

/**
  * Created by cyq on 09/03/2017.
  */
object  JavaSaslTestApp extends App {

    val props = new util.TreeMap[String, String]()
    props.put(Sasl.QOP, "auth")

    val saslServer = Sasl.createSaslServer("DIGEST-MD5", "xmpp", "localhost",props,new ServerCallbackHandler())

    val token = new Array[Byte](0)

    val challenge = saslServer.evaluateResponse(token)

    println(new String(challenge))

}


class ServerCallbackHandler extends CallbackHandler {

    override def handle(callbacks: Array[Callback]) = {

    }

}
