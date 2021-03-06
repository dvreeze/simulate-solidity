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

package eu.cdevreeze.simulatesolidity.soliditytypes

import scala.collection.immutable

/**
 * Script combining several function calls. The "smart contracts" are implicit.
 *
 * @author Chris de Vreeze
 */
final class Script(
    val initialContext: FunctionCallContext,
    val functionCalls: immutable.IndexedSeq[FunctionCall]) {

  /**
   * Invokes all function calls "transactionally". See method FunctionCall.call.
   */
  def run(): HasAccountCollection = {
    val dummyFuncResult: HasAccountCollection = FunctionResult.fromCallContextOnly(initialContext)

    functionCalls.foldLeft(dummyFuncResult) {
      case (funcResult, funcCall) =>
        val newFuncResult = funcCall.call(funcResult.accountCollection)(this)
        newFuncResult
    }
  }
}
