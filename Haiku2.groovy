@Grab('com.github.albaker:GroovySparql:0.9.0')
@Grab("org.twitter4j:twitter4j-core:3.0.5")
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import groovy.sparql.*


word2syl = [:]
new File("epw.cd").splitEachLine("\\\\") { line ->
  def word = line[1]?.toLowerCase()
  def syl = line[7]
  def ns = syl.length() - syl.replaceAll("\\[", "").length()
  word2syl[word] = ns
}

word2syl["subclass"] = 2

/* 
   a list L of words is a Haiku candidate iff one of the following conditions are true:
   1 the total sum of syllables in L is <5
   2 L starts with a sequence of words with syllable sum 5, and the following words have a sum <7
   3 L starts with a sequence of words with syllable sum 5, followed by a sequence of words with syll sum 7, and the following words have syll sum <5

   a list L of words is a Haiku iff the following condition is true:
   4 L starts with a sequence of words with syllable sum 5, followed by a sequence of words with syll sum 7, followed by a sequence of words with syll sum 5
*/

def isCandidate(List l) {
  def syll = l.collect {word2syl[it]}
  if (null in syll) { return false }
  def counter = 0
  def syllnum = 0
  while (syllnum < 5 && counter < syll.size()) {
    syllnum += syll[counter]
    counter += 1
  }
  if (syllnum > 5) { return false }
  if (syllnum < 5) { return true } // condition 1 satisfied
  
  syllnum = 0
  while (syllnum < 7 && counter < syll.size()) {
    syllnum += syll[counter]
    counter += 1
  }
  if (syllnum > 7) { return false }
  if (syllnum < 7) { return true } // condition 2 satisfied

  syllnum = 0
  while (syllnum < 5 && counter < syll.size()) {
    syllnum += syll[counter]
    counter += 1
  }
  if (syllnum > 5) { return false }
  if (syllnum == 5) {
    println "Haiku found: $l"
    System.exit(0)
  }
}


def line1 = []
def line2 = []
def line3 = []

//def sparql = new Sparql(endpoint:"http://dbpedia.org/sparql")
def sparql = new Sparql(endpoint:"http://bio2rdf.org/sparql")

// find some resources with labels
def query = """
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?resource ?lab FROM <http://bio2rdf.org/bioportal_resource:bio2rdf.dataset.bioportal.R3> WHERE { 
?resource rdfs:label ?lab .
FILTER(LANG(?lab) = "" || LANGMATCHES(LANG(?lab), "en"))
} ORDER BY RAND() LIMIT 10000
"""

def candidates = [:]
// sparql result variables projected into the closure delegate
sparql.each query, {
  def words = lab.tokenize(" ")
  if (isCandidate(words)) {
    candidates[resource] = words
  }
}

candidates.keySet().each { k ->
  def words = candidates[k]
  query = """
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?p ?labp ?o ?labo FROM <http://bio2rdf.org/bioportal_resource:bio2rdf.dataset.bioportal.R3> WHERE { 
<$k> ?p ?o .
?o rdfs:label ?labo .
OPTIONAL {
?p rdfs:label ?labp .
}
FILTER(LANG(?labo) = "" || LANGMATCHES(LANG(?labo), "en"))
} ORDER BY RAND() LIMIT 1000
"""
  sparql.each query, {
    def plabel = null
    if (labp == null) {
      plabel = p.substring(p.indexOf("#")+1)?.toLowerCase()
      if (plabel == "subclassof") {
	plabel = "subclass of"
      }
    }
    words = words + "$plabel $labo".toLowerCase().tokenize(" ")
    if (isCandidate(words)) {
      println words
    }
  }
}
