@Grab('com.github.albaker:GroovySparql:0.9.0')
import groovy.sparql.*

def word2syl = [:]
new File("epw.cd").splitEachLine("\\\\") { line ->
  def word = line[1]?.toLowerCase()
  def syl = line[7]
  def ns = syl.length() - syl.replaceAll("\\[", "").length()
  word2syl[word] = ns
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
} ORDER BY RAND() LIMIT 1000
"""

/* 
   a list L of words is a Haiku candidate iff one of the following conditions are true:
   - the total sum of syllables in L is <5
   - L starts with a sequence of words with syllable sum 5, and the following words have a sum <7
   - L starts with a sequence of words with syllable sum 5, followed by a sequence of words with syll sum 7, and the following words have syll sum <5

   a list L of words is a Haiku iff the following condition is true:
   - L starts with a sequence of words with syllable sum 5, followed by a sequence of words with syll sum 7, followed by a sequence of words with syll sum 5
*/

def isCandidate(List l) {
  def syll = l.inject {word2syl[it]}
  syll.each { if (it == null) return false }
  
}

def candidates = []
// sparql result variables projected into the closure delegate
sparql.each query, {
  println "$resource $lab"
  lab.split(" ").each{println word2syl[it?.toLowerCase()]}
  
}
