@Grab('com.github.albaker:GroovySparql:0.9.0')
@Grab("org.twitter4j:twitter4j-core:4.0.4")
import twitter4j.*
import twitter4j.auth.*
import groovy.sparql.*

private static AccessToken loadAccessToken(int useId){
  def lines = 0
  String token = null
  String tokenSecret = null
  new File("twittersecrets.txt").eachLine { line ->
    if (lines == 0) {
    } else if (lines == 1) {
      token = line
    } else if (lines == 2) {
      tokenSecret = line
    }
    lines += 1
  }
  return new AccessToken(token, tokenSecret);
}

def ckey = null
def cpass = null
new File("consumersecrets.txt").eachLine { line, num ->
  if (num == 1) {
    ckey = line
  } else if (num == 2) {
    cpass = line
  }
}

TwitterFactory factory = new TwitterFactory();
AccessToken accessToken = loadAccessToken(0)
twitter = factory.getInstance()
twitter.setOAuthConsumer(ckey, cpass)
twitter.setOAuthAccessToken(accessToken)

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
  if (syllnum == 5 && counter == syll.size()) {
    def haikustring = ""
    println "=========================================================================="
    //    println "Haiku found:"
    counter = 0
    syllnum = 0
    while (syllnum < 5) {
      print l[counter]+" "
      haikustring += (l[counter]+" ")
      syllnum += syll[counter]
      counter += 1
    }
    println ""
    haikustring+= "\n"
    syllnum = 0
    while (syllnum < 7) {
      print l[counter]+" "
      haikustring += (l[counter]+" ")
      syllnum += syll[counter]
      counter += 1
    }
    println ""
    haikustring+= "\n"
    syllnum = 0
    while (syllnum < 5) {
      print l[counter]+" "
      haikustring += (l[counter]+" ")
      syllnum += syll[counter]
      counter += 1
    }
    println "\n==========================================================================\n"
    try {
      def newstatus = twitter.updateStatus("$haikustring\n#biohack16")
      System.out.println("Successfully updated the status to [" + newstatus.getText() + "].");
      sleep(1000*60*10) // wait 10 minutes
    } catch (Exception E) {
      println E.getMessage()
    }
  }
}


def line1 = []
def line2 = []
def line3 = []

//def sparql = new Sparql(endpoint:"http://dbpedia.org/sparql")
//def sparql = new Sparql(endpoint:"http://bio2rdf.org/sparql")
//def sparql = new Sparql(endpoint:"http://rdf.disgenet.org/sparql/")
def sparql = new Sparql(endpoint:"http://sparql.uniprot.org/sparql/")
//def sparql = new Sparql(endpoint:"http://data.linkedmdb.org/sparql")
//def sparql = new Sparql(endpoint:"https://query.wikidata.org/")
// find some resources with labels
//SELECT ?resource ?lab FROM <http://bio2rdf.org/bioportal_resource:bio2rdf.dataset.bioportal.R3> WHERE { 
def query = """
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?resource ?lab WHERE { 
?resource rdfs:label ?lab .
FILTER(LANG(?lab) = "" || LANGMATCHES(LANG(?lab), "en"))
} LIMIT 100000
"""

def candidates = [:]
// sparql result variables projected into the closure delegate
sparql.each query, {
  def words = lab.toLowerCase().tokenize(" ")
  if (isCandidate(words)) {
    candidates[resource] = words
  }
}
def randlist = candidates.keySet().toList()
Collections.shuffle(randlist)
randlist.each { k ->
  def words = candidates[k]
  query = """
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?p ?labp ?o ?labo WHERE { 
<$k> ?p ?o .
?o rdfs:label ?labo .
OPTIONAL {
?p rdfs:label ?labp .
}
FILTER(LANG(?labo) = "" || LANGMATCHES(LANG(?labo), "en"))
} LIMIT 1000
"""
  sparql.each query, {
    def plabel = null
    if (labp == null) {
      plabel = p.substring(p.lastIndexOf("#")+1)
      plabel = plabel.substring(plabel.lastIndexOf("/")+1)
      def tok = ""
      plabel.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])").each {
	tok = tok + it.toLowerCase() + " "
      }
      plabel = tok.trim()
      if (plabel == "subclassof") {
	plabel = "subclass of"
      }
    } else {
      plabel = labp?.toLowerCase()
    }
    def nwords = words + "$plabel $labo".toLowerCase().tokenize(" ")
    if (isCandidate(nwords)) {
      println nwords
      //      println k
    }
  }
}
