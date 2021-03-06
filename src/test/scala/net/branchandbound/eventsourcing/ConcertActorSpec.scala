package net.branchandbound.eventsourcing

import akka.testkit.TestKit
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.Matchers
import akka.testkit.ImplicitSender
import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import java.util.Date
import net.branchandbound.eventsourcing.ConcertProtocol.TicketsBought
import net.branchandbound.eventsourcing.ConcertProtocol.SoldOut

class ConcertActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {
  def this() = this(
    ActorSystem("TestActorSystem", ConfigFactory.parseString(
      """
        |akka.loglevel = "DEBUG"
        |akka.persistence.journal.plugin = "in-memory-journal"
        |akka.actor.debug {
        |   receive = on
        |   autoreceive = on
        |   lifecycle = on
        |}
      """.stripMargin)))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }
   import ConcertActor._
   "A ConcertActor" when {
      val getSalesCmd = GetSalesRecords

      s"receiving '$getSalesCmd'" should {
        s"return empty list of sales" in {
          val concertActor = _system.actorOf(ConcertActor.props("1"))
          concertActor ! CreateConcert(10, 100, new Date)
          concertActor ! getSalesCmd

          expectMsg(List())
        }
      }
      
      val buyCmd = BuyTickets("me", 1)
      s"receiving '$buyCmd'" should {
        s"return Success" in {
          val concertActor = _system.actorOf(ConcertActor.props("1"))
          concertActor ! buyCmd

          expectMsg(TicketsBought("me", 1))
        }
      }

      s"receiving '$buyCmd' and then '$getSalesCmd'" should {
        s"return non-empty list of sales" in {
          val concertActor = _system.actorOf(ConcertActor.props("2"))
          concertActor ! CreateConcert(1, 100, new Date)
          concertActor ! buyCmd
          concertActor ! getSalesCmd
          
          expectMsg(TicketsBought("me", 1))
          expectMsg(Seq(SalesRecord("me", 1, 1)))
        }
      }     
      
      val buyTooMuchCmd = BuyTickets("me", 100)
      s"receiving '$buyTooMuchCmd'" should {
        s"return Success" in {
          val concertActor = _system.actorOf(ConcertActor.props("3"))
          concertActor ! CreateConcert(1, 1, new Date)
          concertActor ! buyTooMuchCmd

          expectMsg(SoldOut("me"))
        }
      }
      
    }
}