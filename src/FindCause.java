import Handlers.HPOHandler;
import Handlers.OmimHandler;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.util.List;

public class FindCause {

    public static void main(String[] args) throws Exception {

        String symptom = "Hypertelorism";
        Analyzer analyzer = new StandardAnalyzer();

        // Pour Timothée :
        Directory idx = OmimHandler.loadIndex();
        String name = "ocu";
        // Le AND fait direcetement appel au OU, donc la recherche la plus otpi actuellement se fait comme ça :
        OmimHandler.printDocs(OmimHandler.searchSymptomAND(name, analyzer, idx, 1000));

        // Pour Gaëtan :

        /*Directory idx2 = HPOHandler.loadIndex();
        String name2 = "sinusa";
        // Tu obtiens une liste clean de tous les synonymes
        System.out.println("la");
        List<String> Lstr = HPOHandler.getSynonymsByName(name2, analyzer, idx2);
        System.out.println("ici");*/
//        if (!Lstr.isEmpty()){
//            for (String str : Lstr) {
//                System.out.println(str);
//            }
//        }
        // TEST INUTILES POUR VOUS

        /*
        List<Document> res = OmimHandler.searchDiseaseBySymptom(symptom, analyzer, idx, 100);
        for (Document doc : res) {
            System.out.println(doc.get("Name_Disease").split(" ",3)[2].split(";",2)[0]);
        }*/

        /*String name = "Laterally displaced inner canthi";
        List<Document> res = OmimHandler.searchDiseaseBySymptom(name, analyzer, idx, 100);
        for (Document doc : res) {
            System.out.println(doc.get("Name_Disease"));
            System.out.println(doc.get("Body_Parts"));
        }*/


        /*
        // Test of the "addToBasis" function
        List<String> a1 = new ArrayList<>(); List<String> a2 = new ArrayList<>();
        a1.add("a1"); a1.add("Omim"); a1.add("1"); a1.add("s1"); a1.add("s2");
        a2.add("a1"); a2.add("Omim"); a2.add("2"); a2.add("s1"); a2.add("s3");
        OmimHandler.addToBasis(a1, a2);
        System.out.println(a1.toString());*/

        /*Directory idx2 = HPOHandler.loadIndex();
        String name2 = "Urinary incontinence";
        // Tu obtiens une liste clean de tous les synonymes
        System.out.println(HPOHandler.getSynonymsByName(name2, analyzer, idx2).toString());*/

        //String name = "frontal AND ocular AND shallow OR atrophy";
        //name = "frontal";
        //test(name, analyzer, idx, 100);
        //OmimHandler.printDocs(OmimHandler.searchSymptomAND(name, analyzer, idx, 100));
        //OmimHandler.printDocs(OmimHandler.parsedSearchDBS("point* frontal", analyzer, idx, 100));
    }

    public static void test(String name, Analyzer analyzer, Directory index, int hits) throws Exception {
        IndexReader indexReader = DirectoryReader.open(index);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Query q1 = new ComplexPhraseQueryParser("Body_Parts", analyzer).parse('"' + name + '"');
        TopDocs top = indexSearcher.search(q1, hits, Sort.INDEXORDER);
        for (ScoreDoc docu : top.scoreDocs) {
            Document document = indexSearcher.doc(docu.doc);
            System.out.println(document.get("Name_Disease"));
            System.out.println(document.get("Body_Parts"));
            System.out.println();
        }
    }

}
