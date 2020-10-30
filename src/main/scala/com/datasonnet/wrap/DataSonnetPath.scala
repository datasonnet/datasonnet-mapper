package com.datasonnet.wrap

/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sjsonnet.Path

case class DataSonnetPath(path: String) extends Path {
  def relativeToString(p: Path): String = p match {
    case other: DataSonnetPath if path.startsWith(other.path) => path.drop(other.path.length)
    case _ => path
  }

  def parent(): Path = DataSonnetPath(path.split('/').dropRight(1).mkString("/"))

  def segmentCount(): Int = path.split('/').length

  def last: String = path.split('/').last

  def /(s: String): Path = DataSonnetPath(path + "/" + s)
}
