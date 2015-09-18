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

//def sparql = new Sparql(endpoint:"http://dbpedia-live.openlinksw.com/sparql")
//def sparql = new Sparql(endpoint:"http://dbpedia.org/sparql")
def sparql = new Sparql(endpoint:"http://rdf.disgenet.org/sparql/")
//def sparql = new Sparql(endpoint:"http://sparql.uniprot.org/")

def start = "http://linkedlifedata.com/resource/umls/id/C0008049"
//def start = "http://purl.uniprot.org/uniprot/Crisp"

def l = [] // keep list of resources
l << start
def sentences = new LinkedHashSet()

def remember = new TreeSet()

def query = """
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?lab ?p ?labp ?o ?labo WHERE { 
<$start> rdfs:label ?lab .
<$start> ?p ?o .
?o rdfs:label ?labo .
OPTIONAL {
?p rdfs:label ?labp .
}
} LIMIT 500
"""
//FILTER(LANG(?lab) = "en" && LANG(?labo) = "en") .

println query
println "Running SPARQL query"
// sparql result variables projected into the closure delegate
sparql.each query, {
  def plabel = ""
  if (labp == null) {
    def labp = p.toString()
    if (labp.indexOf("#")>-1) {
      plabel = labp.substring(labp.lastIndexOf("#")+1)
    } else if (labp.indexOf("/")>-1) {
      plabel = labp.substring(labp.lastIndexOf("/")+1)
    } else {plabel = ""}
    def lll = []
    if (plabel == plabel.toLowerCase()) {} else { // is camel case
       for (String w : plabel.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
	 lll << w
       }
       plabel = lll.collect { it.toLowerCase() }.join(" ")
    }
  } else {
    plabel = labp
  }
  plabel = plabel?.replaceAll("[^a-zA-Z ]+","")
  labo = labo?.replaceAll("[^a-zA-Z ]+","")
  lab = lab?.replaceAll("[^a-zA-Z ]+","")
  println "$lab $labp [$plabel] $labo"
  def toks = lab.split(" ").collect { it.toLowerCase() }
  def tokp = plabel.split(" ").collect { it.toLowerCase() }
  def toko = labo.split(" ").collect { it.toLowerCase() }
  def ll = toks + tokp + toko
  ll = ll.collect { word2syl[it] }
  //  if (! (null in ll) ) { // found a full s p o path with all words in our dictionary
    //    println "${lab} ${labp} ${labo}"
    remember.add(o.toString())
    sentences << [toks, tokp, toko]
    //  }
}

println remember
remember.each { iri ->
  
  query = """
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?lab ?p ?labp ?o ?labo WHERE { 
<$iri> rdfs:label ?lab .
<$iri> ?p ?o .
OPTIONAL {
?p rdfs:label ?labp .
}
?o rdfs:label ?labo .
} LIMIT 50
"""
  //FILTER(LANG(?lab) = "en" && LANG(?labo) = "en") .
  println query
  sparql.each query, {
  def plabel = ""
  if (labp == null) {
    def labp = p.toString()
    if (labp.indexOf("#")>-1) {
      plabel = labp.substring(labp.lastIndexOf("#")+1)
    } else if (labp.indexOf("/")>-1) {
      plabel = labp.substring(labp.lastIndexOf("/")+1)
    } else {plabel = ""}
  }
  def toks = lab.split(" ").collect { it.toLowerCase() }
  def tokp = plabel.split(" ").collect { it.toLowerCase() }
  def toko = labo.split(" ").collect { it.toLowerCase() }
  def ll = toks + tokp + toko
  ll = ll.collect { word2syl[it] }
  //    if (! (null in ll) ) { // found a full s p o path with all words in our dictionary
    println "${lab} ${labp} ${labo}"
    //      remember.add(o.toString())
    sentences << [toks, tokp, toko]
    //    }
  }
}

remember = [] // remember sentences to remove
sentences.each { sent ->
  def toks = sent[0]
  def tokp = sent[1]
  def toko = sent[2]
  def all = toks + tokp + toko
  def syls = toks.inject(0, {sum, val -> if (word2syl[val]) {sum += word2syl[val]} else { 0 } })
  def sylp = tokp.inject(0, {sum, val -> if (word2syl[val]) { sum += word2syl[val] } else { 0 } })
  def sylo = toko.inject(0, {sum, val -> if (word2syl[val]) {sum += word2syl[val]} else { 0 } } )
  def sylall = all.inject(0, {sum, val -> if (word2syl[val]) { sum += word2syl[val] } else { 0 }})
  println all
  println sylall
}
