package me.babaili.ajabberd.protocol

import scala.collection.immutable.HashMap

/**
  * Created by chengyongqiao on 11/01/2017.
  */
trait Stanza {
    def tag: String
}

object NilStanza extends Stanza {
    override def tag = ""
}

case class CommonStanza(val tag:String,
                        val attributes:Option[HashMap[String,String]],
                        val children: Option[List[CommonStanza]])

object NilCommonStanza extends CommonStanza("", None, None)

object Stanza {
    val stream: String = "stream:stream"

    val startTls: String = "starttls"

    val proceed: String = "proceed"

    val features: String = "stream:features"

    val mechanism: String = "mechanism"

    val failure: String = "failure"

    val required: String = "required"

    val mechanisms: String = "mechanisms"
}

class Stream(val to:String) extends Stanza {
    override def tag = Stanza.stream
}


class StartTls(val required: Boolean) extends Stanza {
    override def tag = Stanza.startTls
}

class Mechanism(val value: String) extends Stanza {
    override def tag = Stanza.mechanism
}

class Mechanisms(val mechanismList:List[Mechanism]) extends Stanza {
    override def tag = Stanza.mechanism
}


class StreamFeatures(val startTls: StartTls, val mechanisms: Mechanisms) extends Stanza {
    override def tag = Stanza.features
}

class Proceed extends Stanza {
    override def tag = Stanza.proceed
}

class Failure extends Stanza {
    override def tag = Stanza.proceed
}
