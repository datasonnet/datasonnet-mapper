package com.datasonnet.wrap

import sjsonnet.Path

case class DataSonnetPath(path: String) extends Path{
  def relativeToString(p: Path): String = p match{
    case other: DataSonnetPath if path.startsWith(other.path) => path.drop(other.path.length)
    case _ => path
  }

  def parent(): Path = DataSonnetPath(path.split('/').dropRight(1).mkString("/"))

  def segmentCount(): Int = path.split('/').length

  def last: String = path.split('/').last

  def /(s: String): Path = DataSonnetPath(path + "/" + s)
}