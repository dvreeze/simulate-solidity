/*
 * Copyright 2011-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.simulatesolidity.console

import scala.util.Failure
import scala.util.Random
import scala.util.Success
import scala.util.Try

import eu.cdevreeze.simulatesolidity.contracts.Ballot
import eu.cdevreeze.simulatesolidity.soliditytypes.Address
import eu.cdevreeze.simulatesolidity.soliditytypes.Context

/**
 * Runs the Ballot contract in random order.
 *
 * @author Chris de Vreeze
 */
object BallotRunner {

  private val taskCount = System.getProperty("taskCount", "10000").toInt

  private val addressCount = System.getProperty("addressCount", "250").toInt

  private val random = new Random(System.currentTimeMillis())

  def main(args: Array[String]): Unit = {
    require(args.length >= 2, s"Usage: BallotRunner <proposal 1> <proposal 2> ...")
    val proposalNames = args.toIndexedSeq

    val ballot: Ballot = new Ballot(proposalNames)(makeContext(Address(1)))

    val tasks = (0 until taskCount).toIndexedSeq map { idx =>
      (ctx: Context) => {
        Try {
          if (idx % 10 == 0) {
            println(s"Invariant holds: ${ballot.requireInvariant(ctx)}")
            println(s"Winning proposal so far: ${ballot.winnerName}")
            println(s"Everyone has voted:      ${ballot.voters.values.forall(_.voted)}")
            println(s"Voted proposals so far:  ${ballot.proposals.map(_.voteCount)}")
            println(s"Voting rights (weights): ${ballot.voters.mapValues(_.weight)}")
            println(s"Votes:                   ${ballot.voters.mapValues(_.votedProposalIndexOption.getOrElse(-1))}")
          }

          // Before trying to vote many times, first give most voters the right to vote.
          if (idx > 10 && idx < 1000 && ctx.messageSender == ballot.firstContext.messageSender) {
            (1 to 100) foreach { i =>
              val addr = chooseAddress()
              println(s"$idx tasks have run so far")
              println(s"Calling giveRightToVote for address $addr. Sender: ${ctx.messageSender} (same as chair person)")
              ballot.giveRightToVote(addr)(ctx)
              println(s"Called giveRightToVote for address $addr")
            }
          }

          doNextCall(ballot)(ctx)(proposalNames.size)
        } match {
          case Failure(t) =>
            println(s"Exception thrown. Method aborts, but computing goes on. Exception: $t")
            false
          case Success(b) =>
            true
        }
      }
    }

    tasks foreach { task => task(makeContext(chooseAddress())) }
  }

  private def makeContext(addr: Address): Context = {
    new Context(addr, 0)
  }

  private def doNextCall(ballot: Ballot)(context: => Context)(proposalCount: Int): Boolean = {
    val methodIdx = chooseMethodIndex()

    println()

    methodIdx match {
      case 0 =>
        if (random.nextInt(taskCount / 500) == 0) {
          val addr = chooseAddress()
          println(s"Calling giveRightToVote for address $addr. Sender: ${context.messageSender}")
          ballot.giveRightToVote(addr)(context)
          println(s"Called giveRightToVote for address $addr")
          true
        } else {
          false
        }
      case 1 =>
        if (random.nextInt(taskCount / 1000) == 0) {
          val addr = chooseAddress()
          println(s"Calling delegate for address $addr. Sender: ${context.messageSender}")
          val result = ballot.delegate(addr)(context)
          println(s"Called delegate for address $addr. Result: $result")
          result
        } else {
          false
        }
      case 2 =>
        val proposalIdx = chooseProposalIndex(proposalCount)
        println(s"Calling vote for proposal $proposalIdx. Sender: ${context.messageSender}")
        val result = ballot.vote(proposalIdx)(context)
        println(s"Called vote for proposal $proposalIdx. Result: $result")
        result
      case _ =>
        sys.error(s"No method with index $methodIdx found")
    }
  }

  private def chooseMethodIndex(): Int = {
    random.nextInt(3)
  }

  private def chooseAddress(): Address = {
    val i = random.nextInt(addressCount + 1)
    Address(i + 1)
  }

  private def chooseProposalIndex(proposalCount: Int): Int = {
    random.nextInt(proposalCount)
  }
}
