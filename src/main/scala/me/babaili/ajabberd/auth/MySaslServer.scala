package me.babaili.ajabberd.auth

import java.util
import javax.security.auth.callback._
import javax.security.sasl.{AuthorizeCallback, RealmCallback, Sasl}

import com.typesafe.scalalogging.Logger
import me.babaili.ajabberd.data.Passport

/**
  * Created by cyq on 09/03/2017.
  */
class MySaslServer {

    private val logger = Logger("me.babaili.ajabberd.auth.MySaslServer")

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

        var defaultUsername = ""

        override def handle(callbacks: Array[Callback]) = {
            var i = 0;
            while ( i < callbacks.length) {
                val callback = callbacks(i)
                logger.debug(s"callback class ${callback.getClass().getName()}")

                if (callback.isInstanceOf[NameCallback]) {

                    val ncb = callback.asInstanceOf[NameCallback]

                    logger.debug(s"name callback ${ncb.getName()} ${ncb.getDefaultName()}")

                   defaultUsername = ncb.getDefaultName()

                    ncb.setName(defaultUsername)

                } else if (callback.isInstanceOf[PasswordCallback]) {

                    val pcb =callback.asInstanceOf[PasswordCallback]

                    pcb.setPassword(Passport.queryUserPassword(defaultUsername).toCharArray())

                    logger.debug(s"password callback ${pcb.getPassword()} ${pcb.isEchoOn()}")

                } else if (callback.isInstanceOf[RealmCallback]) {

                    val rcb = callback.asInstanceOf[RealmCallback]

                    rcb.setText(realm)

                    logger.debug("realm callback")
                } else if(callback.isInstanceOf[AuthorizeCallback]) {
                    val ac = callback.asInstanceOf[AuthorizeCallback]
                    ac.setAuthorized(true)
                } else {

                    logger.debug(s"unsupported callback ${callback.getClass()}")

                    throw new UnsupportedCallbackException(callback)

                }

                i = i + 1
            }
        }

    }
}



