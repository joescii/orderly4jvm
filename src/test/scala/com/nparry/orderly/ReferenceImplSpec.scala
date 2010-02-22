/*
 *  Copyright (c) 2010, Nathan Parry
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 * 
 *  3. Neither the name of Nathan Parry nor the names of any
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
 *  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 *  STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *  IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.nparry.orderly

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import net.liftweb.json.JsonAST._
import scala.util.parsing.input.{Reader, StreamReader}


import java.io._
import java.net.URI

class ReferenceImplSpec extends FlatSpec with ShouldMatchers {

  "This implementation" should "produce the same output as the RI for valid input" in {
    ((locateOrderlyInput("referenceImpl/positive_cases") map {
      f=> (f, getExpectedOutput(f)) }).foldLeft(0) { (errorCount, files) =>
        try {
          val ourJson = parseFile(files._1)
          val riJson = jsonFromFile(files._2)
          jsonMatches(ourJson, riJson) match {
            case true => errorCount
            case false => {
              System.err.println("\nInput: " + files._1)
              System.err.println("Our JSON:")
              System.err.println(prettyPrintJSON(ourJson))
              System.err.println("\nRI JSON:")
              System.err.println(prettyPrintJSON(riJson))
              errorCount + 1
            }
          }
        } catch {
          case e:InvalidOrderly => {
            System.err.println("\nInput: " + files._1)
            System.err.println("Parsing failed!")
            errorCount + 1
          }
          case e:Exception => throw e
        }
      }
    ) match {
      case 0 => true
      case count => fail(count + " problems processing positive test cases")
    }
  }

  def jsonMatches(ourJson: JObject, riJson: JObject) = false

  /**
   * Make sure we reject the same cases as the RI. However, we don't try
   * to match the error messages generated by the RI.
   */
  "This implementation" should "reject the same invalid input as the RI" in {
    locateOrderlyInput("referenceImpl/negative_cases") foreach { file =>
      intercept[InvalidOrderly] {
        val json = parseFile(file)
        System.err.println("Successfully parsed " + file + ", this is bad! Parse result was:")
        System.err.println(prettyPrintJSON(json))
      }
    }
  }

  def parseFile(f: File): JObject = OrderlyParser.parse(fileToReader(f))
  def jsonFromFile(f: File): JObject = net.liftweb.json.JsonParser.parse(new FileReader(f)) match {
    case o @ JObject(_) => o
    case _ => throw new Exception("Expected output " + f + " didn't contain an object")
  }

  def locateOrderlyInput(s: String): Array[File] = {
    val a = filesForUri(uriForResourceDir(s)) filter { f => f.getAbsolutePath().endsWith(".orderly") }
    a.length match {
      case 0 => throw new Exception("No test input found in " + s)
      case _ => a
    }
  }

  def getExpectedOutput(f: File): File = {
    val expected = new File(f.getAbsolutePath.replace(".orderly", ".jsonschema"))
    expected.exists() match {
      case false => throw new Exception("Unable to find expected output for " + f)
      case _ => expected
    }
  }

  def uriForResourceDir(s: String): URI = Thread.currentThread().getContextClassLoader().getResources(s).nextElement().toURI()
  def filesForUri(uri: URI): Array[File] = new File(uri).listFiles()
  def fileToReader(f: File): Reader[Char] = StreamReader(new FileReader(f))

  def prettyPrintJSON(json: JValue): String = net.liftweb.json.Printer.pretty(render(json))
}
