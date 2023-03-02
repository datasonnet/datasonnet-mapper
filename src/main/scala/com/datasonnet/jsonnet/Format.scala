package com.datasonnet.jsonnet

/*-
 * Copyright 2019-2022 the original author or authors.
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

/**
  * Minimal re-implementation of Python's `%` formatting logic, since Jsonnet's
  * `%` formatter is basically "do whatever python does", with a link to:
  *
  * - https://docs.python.org/2/library/stdtypes.html#string-formatting
  *
  * Parses the formatted strings into a sequence of literal strings separated
  * by `%` interpolations modelled as structured [[Format.FormatSpec]]s, and
  * use those to decide how to inteprolate the provided Jsonnet [[Val]]s into
  * the final string.
  */
object Format{
  case class FormatSpec(label: Option[String],
                        alternate: Boolean,
                        zeroPadded: Boolean,
                        leftAdjusted: Boolean,
                        blankBeforePositive: Boolean,
                        signCharacter: Boolean,
                        width: Option[Int],
                        precision: Option[Int],
                        conversion: Char)
  import fastparse._, NoWhitespace._
  def integer[_: P]           = P( CharIn("1-9") ~ CharsWhileIn("0-9", 0) | "0" )
  def label[_: P] = P( ("(" ~ CharsWhile(_ != ')').! ~ ")").? )
  def flags[_: P] = P( CharsWhileIn("#0\\- +", 0).! )
  def width[_: P] = P( (integer | "*").!.? )
  def precision[_: P] = P( ("." ~/ integer.!).? )
  def conversion[_: P] = P( CharIn("diouxXeEfFgGcrsa%").! )
  def formatSpec[_: P] = P( label ~ flags ~ width ~ precision ~ CharIn("hlL").? ~ conversion ).map{
    case (label, flags, width, precision, conversion) =>
      FormatSpec(
        label,
        flags.contains('#'),
        flags.contains('0'),
        flags.contains('-'),
        flags.contains(' '),
        flags.contains('+'),
        width.map(_.toInt),
        precision.map(_.toInt),
        conversion.charAt(0)
      )
  }


  def plain[_: P] = P( CharsWhile(_ != '%', 0).! )
  def format[_: P] = P( plain ~ (("%" ~/ formatSpec) ~ plain).rep ~ End)



  def widenRaw(formatted: FormatSpec, txt: String) = widen(formatted, "", "", txt, false, false)
  def widen(formatted: FormatSpec,
            lhs: String,
            mhs: String,
            rhs: String,
            numeric: Boolean,
            signedConversion: Boolean) = {

    val lhs2 =
      if(signedConversion && formatted.blankBeforePositive) " " + lhs
      else if(signedConversion && formatted.signCharacter) "+" + lhs
      else lhs

    val missingWidth = formatted.width.getOrElse(-1) - lhs2.length - mhs.length - rhs.length

    if (missingWidth <= 0) lhs2 + mhs + rhs
    else if (formatted.zeroPadded) {
      if (numeric) lhs2 + mhs + "0" * missingWidth + rhs
      else {
        if (formatted.leftAdjusted) lhs2 + mhs + rhs + " " * missingWidth
        else " " * missingWidth + lhs2 + mhs + rhs
      }
    }
    else if (formatted.leftAdjusted) lhs2 + mhs + rhs + " " * missingWidth
    else " " * missingWidth + lhs2 + mhs + rhs
  }
  def format(s: String,
             values0: Val,
             offset: Int)
            (implicit fileScope: FileScope, evaluator: EvalScope): String = synchronized{
    val values = values0 match{
      case x: Val.Arr => x
      case x: Val.Obj => x
      case x => Val.Arr(Seq(Val.Lazy(x)))
    }
    val (leading, chunks) = fastparse.parse(s, format(_)).get.value
    val output = new StringBuilder
    output.append(leading)
    var i = 0
    for((formatted, literal) <- chunks){
      val cooked0 = formatted.conversion match{
        case '%' => widenRaw(formatted, "%")
        case _ =>

          val value = formatted.label match{
            case None => Materializer(values.cast[Val.Arr].value(i).force)
            case Some(key) =>
              values match{
                case v: Val.Arr => Materializer(v.value(i).force)
                case v: Val.Obj => Materializer(v.value(key, offset))
              }
          }
          i += 1
          value match{
            case ujson.Str(s) => widenRaw(formatted, s)
            case ujson.Num(s) =>
              formatted.conversion match{
                case 'd' | 'i' | 'u' => formatInteger(formatted, s)
                case 'o' => formatOctal(formatted, s)
                case 'x' => formatHexadecimal(formatted, s)
                case 'X' => formatHexadecimal(formatted, s).toUpperCase
                case 'e' => formatExponent(formatted, s).toLowerCase
                case 'E' => formatExponent(formatted, s)
                case 'f' | 'F' => formatFloat(formatted, s)
                case 'g' => formatGeneric(formatted, s).toLowerCase
                case 'G' => formatGeneric(formatted, s)
                case 'c' => widenRaw(formatted, s.toChar.toString)
                case 's' =>
                  if (s.toLong == s) widenRaw(formatted, s.toLong.toString)
                  else widenRaw(formatted, s.toString)
              }
            case ujson.True => widenRaw(formatted, "true")
            case ujson.False => widenRaw(formatted, "false")
            case v => widenRaw(formatted, v.toString)
          }

      }

      output.append(cooked0)
      output.append(literal)


    }

    output.toString()
  }

  def formatInteger(formatted: FormatSpec, s: Double) = {
    val (lhs, rhs) = (if (s < 0) "-" else "", math.abs(s.toInt).toString)
    val rhs2 = precisionPad(lhs, rhs, formatted.precision)
    widen(
      formatted,
      lhs, "", rhs2,
      true, s > 0
    )
  }

  def formatFloat(formatted: FormatSpec, s: Double) = {
    widen(
      formatted,
      if (s < 0) "-" else "", "",
      com.datasonnet.jsonnet.DecimalFormat.format(
        maybeDecimalPoint(formatted, (formatted.precision.getOrElse(6), 0)),
        None,
        math.abs(s)
      ).replace("E", "E+"),
      true,
      s > 0
    )


  }

  def formatOctal(formatted: FormatSpec, s: Double) = {
    val (lhs, rhs) = (if (s < 0) "-" else "", math.abs(s.toInt).toOctalString)
    val rhs2 = precisionPad(lhs, rhs, formatted.precision)
    widen(
      formatted,
      lhs, if (!formatted.alternate || rhs2(0) == '0') "" else "0", rhs2,
      true, s > 0
    )
  }

  def formatHexadecimal(formatted: FormatSpec, s: Double) = {
    val (lhs, rhs) = (if (s < 0) "-" else "", math.abs(s.toInt).toHexString)
    val rhs2 = precisionPad(lhs, rhs, formatted.precision)
    widen(
      formatted,
      lhs, if (!formatted.alternate) "" else "0x", rhs2,
      true, s > 0
    )
  }

  def precisionPad(lhs: String, rhs: String, precision: Option[Int]) = {
    precision match{
      case None => rhs
      case Some(p) =>
        val shortage = p - rhs.length
        if (shortage > 0) "0" * shortage + rhs else rhs
    }
  }

  def formatGeneric(formatted: FormatSpec, s: Double) = {
    val precision = formatted.precision.getOrElse(6)
    val leadingPrecision = math.floor(math.log10(s)).toInt + 1
    val trailingPrecision = math.max(0, precision - leadingPrecision)
    if (s < 0.0001 || math.pow(10, formatted.precision.getOrElse(6): Int) < s) {
      widen(
        formatted,
        if (s < 0) "-" else "", "",
        com.datasonnet.jsonnet.DecimalFormat.format(
          maybeDecimalPoint(formatted, if (formatted.alternate)(precision - 1, 0) else (0, precision - 1)),
          Some(2),
          math.abs(s)
        ).replace("E", "E+"),
        true,
        s > 0
      )
    }
    else {
      widen(
        formatted,
        if (s < 0) "-" else "", "",
        com.datasonnet.jsonnet.DecimalFormat.format(
          maybeDecimalPoint(formatted, if (formatted.alternate) (trailingPrecision, 0) else (0, trailingPrecision)),
          None,
          math.abs(s)
        ).replace("E", "E+"),
        true,
        s > 0
      )
    }

  }

  def formatExponent(formatted: FormatSpec, s: Double) = {
    widen(
      formatted,
      if (s < 0) "-" else "", "",
      com.datasonnet.jsonnet.DecimalFormat.format(
        maybeDecimalPoint(formatted, (formatted.precision.getOrElse(6), 0)),
        Some(2),
        math.abs(s)
      ).replace("E", "E+"),
      true,
      s > 0
    )
  }

  def maybeDecimalPoint(formatted: FormatSpec, fracLengths: (Int, Int)) = {
    if (formatted.precision.contains(0) && !formatted.alternate) None else Some(fracLengths)
  }
}
