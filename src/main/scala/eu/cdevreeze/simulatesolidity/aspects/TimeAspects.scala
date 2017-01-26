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

import java.time.Instant

import eu.cdevreeze.simulatesolidity.soliditytypes.Context

/**
 * Trait to be used by "contract" methods to enforce WHEN a function may be called.
 *
 * @author Chris de Vreeze
 */
trait TimeAspects extends ContextAspects {

  final def before[A](time: Instant)(context: Context)(f: () => A) = {
    withRequiredContext(ctx => ctx.now.isBefore(time))(context)(f)
  }

  final def notBefore[A](time: Instant)(context: Context)(f: () => A) = {
    withRequiredContext(ctx => !ctx.now.isBefore(time))(context)(f)
  }

  final def after[A](time: Instant)(context: Context)(f: () => A) = {
    withRequiredContext(ctx => ctx.now.isAfter(time))(context)(f)
  }

  final def notAfter[A](time: Instant)(context: Context)(f: () => A) = {
    withRequiredContext(ctx => !ctx.now.isAfter(time))(context)(f)
  }
}
