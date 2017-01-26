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

package eu.cdevreeze.simulatesolidity.collections

import scala.collection.immutable

/**
 * Functional updater for immutable Maps and immutable IndexedSeq collections.
 *
 * @author Chris de Vreeze
 */
object Updater {

  def updated[K, V](map: Map[K, V], key: K, f: V => V): Map[K, V] = {
    map.updated(key, f(map(key)))
  }

  def updated[K, V](map: Map[K, V], key: K, f: V => V, fallback: K => V): Map[K, V] = {
    map.updated(key, f(map.getOrElse(key, fallback(key))))
  }

  def updated[A](xs: immutable.IndexedSeq[A], idx: Int, f: A => A): immutable.IndexedSeq[A] = {
    xs.updated(idx, f(xs(idx)))
  }
}
