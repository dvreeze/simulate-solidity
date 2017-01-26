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

package eu.cdevreeze.simulatesolidity.aspects

import eu.cdevreeze.simulatesolidity.soliditytypes.Address
import eu.cdevreeze.simulatesolidity.soliditytypes.Context

/**
 * Trait to be used by "contract" methods to enforce that only specific senders can successfully invoke
 * a function. It specifies WHO the message sender may be.
 *
 * @author Chris de Vreeze
 */
trait SenderAspects extends ContextAspects {

  final def withRequiredSender[A](allowedSender: Address)(context: Context)(f: () => A) = {
    withRequiredContext(ctx => ctx.messageSender == allowedSender)(context)(f)
  }
}
