package me.babaili.ajabberd.net

import java.util
import javax.security.auth.callback._
import javax.security.sasl.{RealmCallback, Sasl}

import com.typesafe.scalalogging.Logger

/**
  * Created by cyq on 09/03/2017.
  */
class MySaslServer {

    private val logger = Logger("me.babaili.ajabberd.net.MySaslServer")

    private val props = new util.TreeMap[String, String]()
    props.put(Sasl.QOP, "auth")

    private val saslServer = Sasl.createSaslServer("DIGEST-MD5", "xmpp", "localhost",props,new ServerCallbackHandler("localhost"))

    private val token = new Array[Byte](0)

    def evaluateResponse(response: Array[Byte]) : (Boolean, Array[Byte]) = {
        if(response == null) {
            (false, saslServer.evaluateResponse(token))
        } else {
            val newChallenge = saslServer.evaluateResponse(response)
            (saslServer.isComplete(), newChallenge)
        }
    }




    class ServerCallbackHandler(val realm: String) extends CallbackHandler {

        override def handle(callbacks: Array[Callback]) = {
            var i = 0;
            while ( i < callbacks.length) {
                val callback = callbacks(i)
                if (callback.isInstanceOf[NameCallback]) {

                    val ncb = callback.asInstanceOf[NameCallback]

                    ncb.setName("tony")

                } else if (callback.isInstanceOf[PasswordCallback]) {

                    val pcb =callback.asInstanceOf[PasswordCallback]

                    pcb.setPassword("admin1".toCharArray())

                } else if (callback.isInstanceOf[RealmCallback]) {

                    val rcb = callback.asInstanceOf[RealmCallback]

                    rcb.setText(realm)

                } else {

                    throw new UnsupportedCallbackException(callback)

                }

                i = i + 1
            }
        }

    }
}



