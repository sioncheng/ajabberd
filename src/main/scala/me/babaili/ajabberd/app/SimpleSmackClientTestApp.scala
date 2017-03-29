package me.babaili.ajabberd.app

import javax.net.ssl.{HostnameVerifier, SSLSession}

import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.{ConnectionConfiguration, SASLAuthentication, StanzaListener}
import org.jivesoftware.smack.packet.{Message, Stanza}
import org.jivesoftware.smack.sasl.javax.{SASLDigestMD5Mechanism}
import org.jivesoftware.smack.tcp.{XMPPTCPConnection, XMPPTCPConnectionConfiguration}
import org.slf4j.LoggerFactory

/**
  * Created by cyq on 08/03/2017.
  */
object SimpleSmackClientTestApp extends App {

    val logger = LoggerFactory.getLogger("SimpleSmackClientTestApp")


    /*
    SASLAuthentication.registerSASLMechanism(new SASLMechanism {
        override def checkIfSuccessfulOrThrow() = {
            logger.debug("checkIfSuccessfulOrThrow")
        }

        override def getAuthenticationText = "hello".getBytes()

        override def getName = SASLMechanism.DIGESTMD5

        override def newInstance() = {
            this
        }

        override def getPriority = 100

        override def authenticateInternal(cbh: CallbackHandler) = {
            //
            logger.debug(s"authenticateInternal")
        }
    })*/

    SASLAuthentication.registerSASLMechanism(new SASLDigestMD5Mechanism())

    //new Java7SmackInitializer().initialize()

    val connectionConfiguration = XMPPTCPConnectionConfiguration
        .builder()
        .setServiceName("localhost")
        .setHost("localhost")
        .setPort(5222)
        .setDebuggerEnabled(true)
        .setUsernameAndPassword("aa", "bbb")
        //.setUsernameAndPassword("13764096288","13764096288")
        .setResource("Android")
        .setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
        .setHostnameVerifier(new HostnameVerifier {
            override def verify(s: String, sslSession: SSLSession) = {
                logger.debug(s"s ${s} sslSession ${sslSession.getPeerHost()}")
                true
            }
        })
        .build()

    val trustPath = getClass().getResource("/netty/client/cChat.jks").getPath()
    System.setProperty("javax.net.ssl.trustStore", trustPath)
    //System.setProperty("javax.net.debug", "all")

    val conn = new XMPPTCPConnection(connectionConfiguration)

    conn.connect()

    logger.debug("connected to server")

    //conn.login("aa", "bbb")
    conn.login()

    logger.debug("logon to server")

    val stanzaListener = new StanzaListener {
        override def processPacket(packet: Stanza): Unit = {

        }
    }
    val stanzaFilter = new StanzaFilter {
        override def accept(stanza: Stanza): Boolean = {
            true
        }
    }
    conn.addAsyncStanzaListener( stanzaListener, stanzaFilter);



/*
    val builder = XMPPTCPConnectionConfiguration.builder()
    builder.setHost("localhost")
    builder.setPort(5222)
    builder.setServiceName("localhost")
    builder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
    builder.setCompressionEnabled(true)
    builder.setConnectTimeout(30000)
    builder.setUsernameAndPassword("aa","bbb")
    try {
        TLSUtils.acceptAllCertificates(builder)
    } catch {
        case e: NoSuchAlgorithmException  =>
            e.printStackTrace()
        case e :  KeyManagementException =>
            e.printStackTrace()
    }
    TLSUtils.disableHostnameVerificationForTlsCertificicates(builder)
    val registeredSASLMechanisms = SASLAuthentication.getRegisterdSASLMechanisms()
    registeredSASLMechanisms.values().forEach(new Consumer[String] {
        override def accept(t: String) = SASLAuthentication.blacklistSASLMechanism(t)
    })
    SASLAuthentication.unBlacklistSASLMechanism(SASLPlainMechanism.NAME)
    val conn = new XMPPTCPConnection(builder.build())

    conn.connect()
    */



    val hello = new Message()
    //hello.setFrom("13764096288")
    hello.setTo("13764096288")
    hello.setType(Message.Type.chat)
    hello.setBody("hello")
    hello.setThread("hello-1")

    conn.sendStanza(hello)



    Thread.sleep(20 * 1000)



    conn.disconnect()

    Thread.sleep(2 * 1000)


}
